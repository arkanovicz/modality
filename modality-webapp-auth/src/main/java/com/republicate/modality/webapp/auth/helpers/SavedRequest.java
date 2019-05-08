package com.republicate.modality.webapp.auth.helpers;

import com.republicate.modality.webapp.util.HttpUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import static com.republicate.modality.webapp.util.HttpUtils.toByteArray;

public class SavedRequest
{
    protected static Logger logger = LoggerFactory.getLogger("auth");

    public SavedRequest(HttpServletRequest request)
    {
        // save enough info for a redirect
        method = request.getMethod();
        scheme = request.getScheme();
        serverName = request.getServerName();
        serverPort = request.getServerPort();
        requestURI = request.getRequestURI();
        queryString = request.getQueryString();

        // - headers
        headers = new Hashtable<String, String>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements())
        {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            String previousValue = headers.put(headerName, headerValue);
            if (previousValue != null)
            {
                logger.warn("saved request cannot handle redundant headers (header: {})", headerName);
            }
        }

        // redundant with headers, but handy
        contentType = request.getContentType();
        characterEncoding = request.getCharacterEncoding();

        // attributes
        attributes = new Hashtable<String, Object>();
        Enumeration<String> attributeNames = request.getAttributeNames();
        while (attributeNames.hasMoreElements())
        {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = request.getAttribute(attributeName);
            attributes.put(attributeName, attributeValue);
        }

        if (method.equals("POST"))
        {
            // save enough info for a forward, that is:
            // - paths
            contextPath = request.getContextPath();
            servletPath = request.getServletPath();
            pathInfo = request.getPathInfo();
            pathTranslated = request.getPathTranslated();

            // - body
            try
            {
                ServletInputStream servletInputStream = request.getInputStream();
                if (servletInputStream != null)
                {
                    body = HttpUtils.toByteArray(request.getInputStream());
                }
            }
            catch (IOException ioe)
            {
                logger.error("could not save request content", ioe);
            }
        }
    }

    public String getMethod()
    {
        return method;
    }

    public Hashtable<String, String> getHeaders()
    {
        return headers;
    }

    public String getPathInfo()
    {
        return pathInfo;
    }

    public String getPathTranslated()
    {
        return pathTranslated;
    }

    public String getContextPath()
    {
        return contextPath;
    }

    public String getQueryString()
    {
        return queryString;
    }

    public String getRequestURI()
    {
        return requestURI;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getCharacterEncoding()
    {
        return characterEncoding;
    }

    public StringBuffer getRequestURL()
    {
        StringBuffer url = new StringBuffer();
        url.append(scheme).append("://").append(serverName);
        if ( (!"http".equals(scheme)) || (serverPort != 80) && (!"https".equals(scheme)) || (serverPort != 443) )
        {
            url.append(':').append(serverPort);
        }
        url.append(requestURI);
        return url;
    }

    public String getServletPath()
    {
        return servletPath;
    }

    public Hashtable<String, Object> getAttributes()
    {
        return attributes;
    }

    public byte[] getBody()
    {
        return body;
    }

    public Map<String, String[]> getParameters()
    {
        Map<String, String[]> parameters = new Hashtable<>();
        if (queryString != null)
        {
            addtoParameters(parameters, queryString.split("&"));
        }
        if (getContentType().startsWith("application/x-www-form-urlencoded"))
        {
            String charset = getCharacterEncoding();
            String decoded;
            try
            {
                decoded = URLDecoder.decode(new String(body, charset));
            }
            catch (UnsupportedEncodingException uee)
            {
                logger.error("could not parse request body", uee);
                decoded = "";
            }
            addtoParameters(parameters, decoded.split("&"));
        }
        return parameters;
    }

    private void addtoParameters(Map<String, String[]> parameters, String[] params)
    {
        Arrays.stream(params).map(param ->
            {
                int eq = param.indexOf('=');
                switch (eq)
                {
                    case -1: return Pair.of(param, "");
                    case 0: return Pair.of("", "");
                    default: return Pair.of(param.substring(0, eq), param.substring(eq + 1));
                }
            }
        ).forEachOrdered(pair ->
        {
            String[] values = Optional.ofNullable(parameters.get(pair.getKey()))
                .map(arr -> Arrays.copyOf(arr, arr.length + 1))
                .orElse(new String[1]);
            values[values.length - 1] = pair.getValue();
            parameters.put(pair.getKey(), values);
        });

    }

    // some setters...
    // a filter inheriting from AbstractAuthFormFilter can override getSavedRequest

    public void setRequestURI(String uri)
    {
        this.requestURI = uri;
    }

    public void setQueryString(String queryString)
    {
        this.queryString = queryString;
    }

    private String method = null;
    private String scheme = null;
    private int serverPort = 0;
    private String serverName = null;
    private String requestURI = null;
    private String queryString = null;
    private String contextPath = null;
    private String servletPath = null;
    private String pathInfo = null;
    private String pathTranslated = null;
    private String contentType = null;
    private String characterEncoding = null;
    private Hashtable<String, Object> attributes = null;
    private Hashtable<String, String> headers = null;
    private byte[] body = null;
}
