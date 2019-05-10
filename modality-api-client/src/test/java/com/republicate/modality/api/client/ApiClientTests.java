package com.republicate.modality.api.client;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ApiClientTests
{
    protected Logger logger = LoggerFactory.getLogger("api-client-tests");

    public class TestServer implements Runnable
    {
        public static final int PORT = 30412;
        private static final String ERROR_RESPONSE = "HTTP/1.1 500 Internal Error\r\n\r\n";
        private static final String HEADERS =  "HTTP/1.1 200 OK\r\n" +
            "Connection: Keep-Alive\r\n" +
            "Content-Type: application/json\r\n";
        private ServerSocket serverSocket;
        private Map<Pair<String, String>, JsonObject> contents = new ConcurrentHashMap<>();
        private Map<Pair<String, String>, CompletableFuture<Pair<String, JsonObject>>> logs = new ConcurrentHashMap<>();

        Pattern reqPattern = Pattern.compile("^(GET|POST) (/[^ ?]+)(?:\\?(\\S+))? HTTP/1.1\r\n");

        public Future<Pair<String, JsonObject>> addContent(String method, String uri, JsonObject response)
        {
            Pair<String, String> key = Pair.of(method, uri);
            contents.put(key, response);
            CompletableFuture<Pair<String, JsonObject>> ret = new CompletableFuture<>();
            logs.put(key, ret);
            return ret;
        }

        @Override
        public void run()
        {
            Socket socket = null;
            try
            {
                serverSocket = new ServerSocket(PORT);
                // serverSocket.setSoTimeout(5000);
                while (true)
                {
                    socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    StringBuilder received = new StringBuilder();
                    String line;
                    int contentLength = -1;
                    boolean chunked = false;
                    int expectedEmptyLines = -1;
                    boolean sendContinue = false;
                    do
                    {
                        line = in.readLine();
                        if (received.length() == 0)
                        {
                            int sp = line.indexOf(' ');
                            if (sp == -1)
                            {
                                throw new Exception("cannot parse request: " + line);
                            }
                            String method = line.substring(0, sp);
                            switch (method)
                            {
                                case "GET":
                                    expectedEmptyLines = 1;
                                    break;
                                case "POST":
                                    // TODO what about potential trailers?!
                                    expectedEmptyLines = 2;
                                    break;
                                default: throw new Exception("cannot parse request: " + line);
                            }
                        }
                        logger.info(line);
                        if (line == null)
                        {
                            break;
                        }
                        else
                        {
                            received.append(line).append("\r\n");
                            if (line.length() == 0)
                            {
                                --expectedEmptyLines;
                                break;
                            }
                            if (line.startsWith("Content-Length: "))
                            {
                                contentLength = Integer.parseInt(line.substring(16));
                            }
                            else if (line.equals("Expect: 100-continue"))
                            {
                                sendContinue = true;
                            }
                            else if (line.equals("Transfer-Encoding: chunked"))
                            {
                                chunked = true;
                            }
                        }
                    }
                    while (expectedEmptyLines > 0);
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    if (line != null && expectedEmptyLines > 0)
                    {
                        if (sendContinue)
                        {
                            out.print("HTTP/1.1 100 Continue\r\n\r\n");
                            out.flush();
                        }
                        if (chunked)
                        {
                            // read only one chunk
                            line = in.readLine();
                            contentLength = Integer.parseInt(line, 16);
                        }
                        if (contentLength > 0)
                        {
                            char[] buffer = new char[contentLength]; //pas bon pas de content length ?! Parce que c'est pas la version 5 de hc?! => test en remettant le header ?
                            in.read(buffer, 0, contentLength); // only works with 1-byte utf-8 chars, since contentLength refers to bytes
                            received.append(buffer);
                            logger.info(new String(buffer));
                        }
                    }
                    String request = received.toString();
                    Matcher matcher = reqPattern.matcher(request);
                    if (matcher.find())
                    {
                        String method = matcher.group(1);
                        String uri = matcher.group(2);
                        String extra = matcher.group(3);

                        String tmp = uri;
                        if (extra == null)
                        {
                            extra = "";
                        }
                        else
                        {
                            tmp += '?' + extra;
                        }
                        Pair<String, String> key = Pair.of(method, tmp);
                        CompletableFuture<Pair<String, JsonObject>> response = logs.get(key);
                        if (response == null)
                        {
                            throw new Exception("content not found");
                        }

                        JsonObject receivedJson = null;
                        int start = request.indexOf('{');
                        int end = request.lastIndexOf('}');
                        if (start != -1 && end != -1)
                        {
                            String content = request.substring(start, end + 1);
                            Object obj = Jsoner.deserialize(content);
                            if (obj == null || !(obj instanceof JsonObject))
                            {
                                throw new Exception("invalid json object: " + content);
                            }
                            receivedJson = (JsonObject) obj;
                        }
                        else
                        {
                            start = request.indexOf("\r\n\r\n");
                            end = request.lastIndexOf("\r\n\r\n");
                            if (end == start)
                            {
                                end = request.length();
                            }
                            if (start != -1 && end != -1 && start != end && extra.length() == 0)
                            {
                                extra = request.substring(start + 4, end);
                            }
                        }
                        response.complete(Pair.of(extra, receivedJson));
                        out.print(HEADERS);
                        String content = contents.get(key).toJson();
                        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                        out.print("Content-Length: " + bytes.length + "\r\n\r\n");
                        out.print(content);
                        out.print("\r\n\r\n");
                        out.flush();
                        socket.close();
                    }
                    else
                    {
                        throw new Exception("Invalid HTTP request: " + received);
                    }
                }
            }
            catch (Throwable t)
            {
                logs.values().stream().forEach(resp -> resp.obtrudeException(t));
                if (socket != null && socket.isConnected())
                {
                    try
                    {
                        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                        out.print(ERROR_RESPONSE);
                        out.flush();
                    }
                    catch (IOException ioe)
                    {
                        // socket was closed
                    }
                    finally
                    {
                        try
                        {
                            socket.close();
                        }
                        catch (IOException ioe)
                        {
                        }
                    }
                }
            }
        }

        public void close()
        {
            try
            {
                if (serverSocket != null)
                {
                    serverSocket.close();
                }
            }
            catch (IOException ioe)
            {
            }
        }

    }

    protected TestServer server;
    protected Thread serverThread;

    @Before
    public void setUp()
    {
        server = new TestServer();
        serverThread = new Thread(server, "client-api-test-server");
        serverThread.start();
        try
        {
            Thread.currentThread().sleep(100);
        }
        catch (InterruptedException ie)
        {
        }
    }

    @After
    public void tearDown()
    {
        if (serverThread != null && serverThread.isAlive())
        {
            server.close();
            serverThread.interrupt();
            try
            {
                serverThread.join();
            }
            catch(InterruptedException ie) {}
        }
    }

    @Test
    public void testGET() throws Exception
    {
        JsonObject out = new JsonObject();
        out.put("status", "ok");

        Future<Pair<String, JsonObject>> promise = server.addContent("GET", "/check-status?foo=bar", out);

        ApiClient client = new ApiClient();
        JsonObject ret = client.get("http://localhost:" + TestServer.PORT + "/check-status", "foo", "bar");
        assertEquals(out, ret);

        Pair<String, JsonObject> sent = promise.get();
        assertEquals("foo=bar", sent.getKey());
        assertEquals(null, sent.getValue());
    }

    @Test
    public void testPOSTjson() throws Exception
    {
        JsonObject in = new JsonObject();
        in.put("foo", BigDecimal.valueOf(5));
        in.put("bar", "something");
        JsonObject out = new JsonObject();
        out.put("status", "ok");

        Future<Pair<String, JsonObject>> promise = server.addContent("POST", "/check-status", out);

        ApiClient client = new ApiClient();
        JsonObject ret = client.post("http://localhost:" + TestServer.PORT + "/check-status", in);
        assertEquals(out, ret);

        Pair<String, JsonObject> sent = promise.get();
        assertEquals("", sent.getKey());
        assertEquals(in, sent.getValue());
    }

    @Test
    public void testPOSTparams() throws Exception
    {
        JsonObject out = new JsonObject();
        out.put("status", "ok");

        Future<Pair<String, JsonObject>> promise = server.addContent("POST", "/check-status", out);

        ApiClient client = new ApiClient();
        JsonObject ret = client.post("http://localhost:" + TestServer.PORT + "/check-status", "foo", "bar");
        assertEquals(out, ret);

        Pair<String, JsonObject> sent = promise.get();
        assertEquals("foo=bar", sent.getKey());
        assertNull(sent.getValue());
    }
}
