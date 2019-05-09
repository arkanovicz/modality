package com.republicate.velocity.tools.api;

import com.github.cliftonlabs.json_simple.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
import org.apache.velocity.tools.generic.JsonContent;
import org.apache.velocity.tools.generic.JsonTool;
import org.apache.velocity.tools.generic.XmlTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class implements a basic API client around Apache HTTP client.
 */

// TODO cookieStore ? credentialsProvider ?
// CookieStore cookieStore = new BasicCookieStore();
// CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

// TODO  client.close();

public class ApiClient
{
    protected static Logger logger = LoggerFactory.getLogger(ApiClient.class);
    private static CloseableHttpClient client = HttpClients.custom()
        .setSSLSocketFactory(new SSLConnectionSocketFactory(
            SSLContexts.createSystemDefault(),
            new String[] { "TLSv1.2" },
            null,
            SSLConnectionSocketFactory.getDefaultHostnameVerifier()))
        .setConnectionTimeToLive(1, TimeUnit.MINUTES)
        .setDefaultSocketConfig(SocketConfig.custom()
            .setSoTimeout(5000)
            .build())
        .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectTimeout(5000)
            .setSocketTimeout(5000)
            .setCookieSpec(CookieSpecs.STANDARD)
            .build())
        .build();

    protected static JsonContent submit(HttpRequestBase req) throws IOException
    {
        JsonContent ret = null;
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
            String body = EntityUtils.toString(entity);
            logger.trace("[api-client] response body = " + body);
            ContentType contentType = ContentType.get(entity);
            Charset charset = contentType.getCharset();
            switch (contentType.getMimeType())
            {
                case "application/json":
                {
                    ret = (JsonContent)new JsonTool().parse(body).root();
                    break;
                }
                case "application/x-www-form-urlencoded":
                case "text/html":
                {
                    JsonObject obj = new JsonObject();
                    String keyValuePairs[] = body.split("&");
                    for (String keyValue : keyValuePairs)
                    {
                        String kv[] = keyValue.split("=");
                        if (kv.length != 2)
                        {
                            throw new ClientProtocolException("Expecting key-value pair: " + keyValue);
                        }
                        String key = URLDecoder.decode(kv[0], charset.name());
                        String value = URLDecoder.decode(kv[1], charset.name());
                        Object previous = obj.put(key, value);
                        if (previous != null)
                        {
                            throw new ClientProtocolException("Unsupported redundant values in response for key: " + key);
                        }
                    }
                    ret = new JsonContent(obj);
                    break;
                }
                case "text/xml":
                {
                    // hack for linkedin - TODO: use LinkedIn v2 API & json
                    JsonObject obj = new JsonObject();
                    XmlTool xml = new XmlTool();
                    xml.parse(body);
                    obj.put("email", xml.getFirst().getFirst().getText());
                    ret = new JsonContent(obj);
                    break;
                }
                default:
                    throw new ClientProtocolException("Unsupported content type: " + contentType.getMimeType());
            }
        }
        else
        {
            HttpEntity entity = resp.getEntity();
            if (entity == null) throw new ClientProtocolException("Response is empty");
            String body = EntityUtils.toString(entity);
            logger.debug("[api-client] error body = " + body);

        }
        return ret;
    }

    public static JsonContent get(String url, String ... params) throws IOException, UnsupportedEncodingException
    {
        return get(url, null, params);
    }

    public static JsonContent get(String url, Pair<String, String> header, String ... params) throws IOException, UnsupportedEncodingException
    {
        HttpGet req = null;
        try
        {
            StringBuilder  paramsString = new StringBuilder();
            for (int p = 0; p < params.length; p += 2)
            {
                if (paramsString.length() > 0) paramsString.append('&'); else paramsString.append('?');
                paramsString.append(params[p]).append('=').append(params[p + 1]);
            }
            String paramsUrl = url + paramsString.toString();
            req = new HttpGet(paramsUrl);
            req.setHeader("Accept", "*/*");
            if (header != null) req.setHeader(header.getLeft(), header.getRight());
            return submit(req);
        }
        finally
        {
            req.releaseConnection();
        }
    }

    public static JsonContent post(String url, String ... params) throws IOException, UnsupportedEncodingException
    {
        return post(url, null, params);
    }

    public static JsonContent post(String url, Pair<String, String> header, String ... params) throws IOException, UnsupportedEncodingException
    {
        HttpPost req = null;
        try
        {
            req = new HttpPost(url);
            req.setHeader("Accept", "*/*");
            req.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            if (header != null) req.setHeader(header.getLeft(), header.getRight());

            List<NameValuePair> reqParams = new ArrayList<NameValuePair>();
            for (int p = 0; p < params.length; p += 2)
            {
                reqParams.add(new BasicNameValuePair(params[p], params[p + 1]));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(reqParams, "UTF-8");
            req.setEntity(entity);
            return submit(req);
        }
        finally
        {
            req.releaseConnection();
        }
    }

    public static JsonContent post(String url, JsonContent params) throws IOException, UnsupportedEncodingException
    {
        HttpPost req = null;
        try
        {
            req = new HttpPost(url);
            req.setHeader("Accept", "*/*");
            EntityTemplate entity = new EntityTemplate(outputstream ->
            {
                outputstream.write(params.toString().getBytes(StandardCharsets.UTF_8));
                outputstream.flush();
            });
            entity.setContentType(ContentType.APPLICATION_JSON.toString());
            req.setEntity(entity);
            return submit(req);
        }
        finally
        {
            req.releaseConnection();
        }
    }
}
