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

import com.republicate.json.Json;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class implements a basic API client around Apache HTTP client.
 */

// TODO cookieStore ? credentialsProvider ?
// CookieStore cookieStore = new BasicCookieStore();
// CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

// TODO  client.close();

public class ApiClient implements Closeable
{
    private static final int TIMEOUT = 30000; // 30 sec
    protected Logger logger = LoggerFactory.getLogger("api-client");

    public ApiClient()
    {
        client = HttpClients.custom()
            .setSSLSocketFactory(new SSLConnectionSocketFactory(
                SSLContexts.createSystemDefault(),
                new String[] { "TLSv1.2" },
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier()))
            .setConnectionTimeToLive(1, TimeUnit.MINUTES)
            .setDefaultSocketConfig(SocketConfig.custom()
                .setSoTimeout(TIMEOUT)
                .build())
            .setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .setCookieSpec(CookieSpecs.STANDARD)
                .build())
            .build();
    }

    public Json.Object get(String url, String ... params) throws IOException
    {
        return get(url, null, params);
    }

    public Json.Object get(String url, Pair<String, String> header, String ... params) throws IOException
    {
        return get(url, header, paramsToMap(params));
    }

    public Json.Object get(String url, Map<String, String> params) throws IOException
    {
        return get(url, null, params);
    }

    public Json.Object get(String url, Pair<String, String> header, Map<String, String> params) throws IOException
    {
        HttpGet req = null;
        try
        {
            StringBuilder  paramsString = new StringBuilder();
            for (Map.Entry<String, String> param : params.entrySet())
            {
                paramsString.append(paramsString.length() > 0 ? '&' : '?');
                paramsString.append(param.getKey()).append('=').append(param.getValue());
            }
            String paramsUrl = url + paramsString.toString();
            req = new HttpGet(paramsUrl);
            return submit(req, header);
        }
        finally
        {
            req.releaseConnection();
        }
    }

    public Json.Object post(String url, String ... params) throws IOException
    {
        return post(url, null, params);
    }

    public Json.Object post(String url, Pair<String, String> header, String ... params) throws IOException
    {
        return post(url, header, paramsToMap(params));
    }

    public Json.Object post(String url, Pair<String, String> header, Map<String, String> params) throws IOException
    {
        HttpPost req = null;
        try
        {
            req = new HttpPost(url);
            req.setHeader("Accept", "*/*");
            if (header != null)
            {
                req.setHeader(header.getLeft(), header.getRight());
            }
            List<NameValuePair> reqParams = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> param : params.entrySet())
            {
                reqParams.add(new BasicNameValuePair(param.getKey(), param.getValue()));

            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(reqParams, "UTF-8");
            entity.setContentType(ContentType.APPLICATION_FORM_URLENCODED.toString());
            return submit(req, header, entity);
        }
        finally
        {
            closeRequest(req);
        }
    }

    public Json.Object post(String url, Json.Object params) throws IOException
    {
        return post(url, null, params);
    }

    public Json.Object post(String url, Pair<String, String> header, Json.Object params) throws IOException
    {
        HttpPost req = null;
        try
        {
            req = new HttpPost(url);
            req.setHeader("Accept", "*/*");
            EntityTemplate entity = new EntityTemplate(outputstream ->
            {
                Writer writer = new OutputStreamWriter(outputstream, StandardCharsets.UTF_8);
                params.toString(writer);
                writer.flush();
            });
            entity.setContentType(ContentType.APPLICATION_JSON.toString());
            return submit(req, header, entity);
        }
        finally
        {
            closeRequest(req);
        }
    }

    protected Json.Object submit(HttpEntityEnclosingRequestBase req, HttpEntity httpEntity) throws IOException
    {
        return submit(req, null, httpEntity);
    }

    protected Json.Object submit(HttpEntityEnclosingRequestBase req, Pair<String, String> header, HttpEntity httpEntity) throws IOException
    {
        if (httpEntity != null)
        {
            req.setEntity(httpEntity);
        }
        return submit(req, header);
    }

    protected Json.Object submit(HttpRequestBase req, Pair<String, String> header) throws IOException
    {
        if (header != null)
        {
            req.setHeader(header.getKey(), header.getValue());
        }
        return submit(req);
    }

    protected Json.Object submit(HttpRequestBase req) throws IOException
    {
        req.setHeader("Accept", "*/*");
        Json.Object ret = null;
        HttpResponse resp = client.execute(req);
        StatusLine statusLine = resp.getStatusLine();
        int status = statusLine.getStatusCode();
        if (status >= 200 && status < 300)
        {
            HttpEntity entity = resp.getEntity();
            if (entity == null)
            {
                throw new ClientProtocolException("Response is empty");
            }
            Charset charset = getCharset(entity);
            Reader body = new InputStreamReader(entity.getContent(), charset);
            ContentType contentType = ContentType.get(entity);
            ret = entityToJson(body, contentType, charset).asObject(); // will throw if result is an array
        }
        else
        {
            HttpEntity entity = resp.getEntity();
            if (entity == null)
            {
                throw new ClientProtocolException("Response is empty");
            }
            String body = EntityUtils.toString(entity);
            logger.error("error body = " + body);

        }
        return ret;
    }

    protected Json entityToJson(Reader body, ContentType contentType, Charset charset) throws IOException
    {
        Json ret = null;
        switch (contentType.getMimeType())
        {
            case "application/json":
                ret = jsonEntityToJson(body);
                break;
            case "application/x-www-form-urlencoded":
                ret = formEntityToJson(body, charset);
                break;
            case "text/xml":
                ret = xmlEntityToJson(body);
                break;
            default:
                throw new ClientProtocolException("unhandled content type: " + contentType);
        }
        return ret;
    }

    protected Json jsonEntityToJson(Reader body) throws IOException
    {
        Json ret = null;
        Object rawJson = null;
        try
        {
            if (logger.isTraceEnabled())
            {
                // Debugging version
                String got = IOUtils.toString(body);
                logger.trace("body: {}", got);
                rawJson = Json.parse(got);
            }
            else
            {
                rawJson = Json.parse(body);
            }
        }
        catch (IOException ioe)
        {
            throw new ClientProtocolException("Invalid json", ioe);
        }
        if (rawJson instanceof Json)
        {
            ret = (Json) rawJson;
        }
        else
        {
            ret = new Json.Object();
            ret.asObject().put("root", (Serializable)rawJson);
        }
        return ret;
    }

    protected Json formEntityToJson(Reader body) throws IOException
    {
        return formEntityToJson(body, StandardCharsets.UTF_8);
    }

    protected Json formEntityToJson(Reader body, Charset charset) throws IOException
    {
        Json.Object ret = null;
        String bodyContent = IOUtils.toString(body);
        ret = new Json.Object();
        String keyValuePairs[] = bodyContent.split("&");
        for (String keyValue : keyValuePairs)
        {
            String kv[] = keyValue.split("=");
            if (kv.length != 2)
            {
                throw new ClientProtocolException("Expecting key-value pair: " + keyValue);
            }
            String key = URLDecoder.decode(kv[0], charset.name());
            String value = URLDecoder.decode(kv[1], charset.name());
            Object previous = ret.put(key, value);
            if (previous != null)
            {
                throw new ClientProtocolException("Unsupported redundant values in response for key: " + key);
            }
        }
        return ret;
    }

    protected Json xmlEntityToJson(Reader body) throws IOException
    {
        // An attribute-less XML content can easily be converted into json
        // or, using 'attribute' and 'content' keys.
        // In any way, we need more informations.
        // Subclasses can inherit and provide the implementation they expect.
        throw new IOException("default implementation does not know how to convert XML into Json");
    }

    protected Charset getCharset(HttpEntity entity)
    {
        Charset charset = null;
        ContentType contentType = ContentType.get(entity);
        if (contentType != null)
        {
            charset = contentType.getCharset();
            if (charset == null)
            {
                final ContentType defaultContentType = ContentType.getByMimeType(contentType.getMimeType());
                charset = defaultContentType != null ? defaultContentType.getCharset() : null;
            }
        }
        if (charset == null)
        {
            charset = StandardCharsets.UTF_8;
        }
        return charset;
    }

    protected Map<String, String> paramsToMap(String... params) throws IOException
    {
        if ((params.length % 2) != 0)
        {
            throw new IOException("expecting an even number of params as 'key,value,key,value,...'");
        }
        Map<String, String> map = new HashMap<>();
        for (int p = 0; p < params.length; p += 2)
        {
            map.put(params[p], params[p + 1]);
        }
        return map;
    }

    protected void closeRequest(HttpRequestBase req) throws IOException
    {
        if (req != null && !req.isAborted())
        {
            req.abort();
            req.releaseConnection();
        }
    }

    private CloseableHttpClient client = null;

    @Override
    public void close() throws IOException
    {
        client.close();
    }
}
