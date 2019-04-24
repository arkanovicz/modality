package com.republicate.webapp.auth;

import org.apache.commons.lang3.tuple.Pair;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

public class SavedRequest
{
    public SavedRequest(HttpServletRequest request)
    {
        // save enough info for a redirect
        method = request.getMethod();
        requestURI = request.getRequestURI();
        queryString = request.getQueryString();
        if (method.equals("POST"))
        {
            // save enough info for a forward, that is:
            // - paths
            contextPath = request.getContextPath();
            servletPath = request.getServletPath();
            pathInfo = request.getPathInfo();
            pathTranslated = request.getPathTranslated();
            requestURL = request.getRequestURL();
            // - attributes
            attributes = new Hashtable<String, Object>();
            Enumeration<String> attributeNames = request.getAttributeNames();
            while (attributeNames.hasMoreElements())
            {
                String attributeName = attributeNames.nextElement();
                Object attributeValue = request.getAttribute(attributeName);
                attributes.put(attributeName, attributeValue);
            }
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
                    AbstractFormAuthFilter.logger.warn("saved request cannot handle redundant headers (header: {})", headerName);
                }
            }
            // - body
            try
            {
                ServletInputStream servletInputStream = request.getInputStream();
                if (servletInputStream != null)
                {
                    body = toByteArray(request.getInputStream());
                }
            }
            catch (IOException ioe)
            {
                AbstractFormAuthFilter.logger.error("could not save request content", ioe);
            }
            // - and some pre-parsing
            parameters = new HashMap<>();
            if (queryString != null)
            {
                addtoParameters(queryString.split("&"));
            }
            if (request.getContentType().startsWith("application/x-www-form-urlencoded"))
            {
                String charset = request.getCharacterEncoding();
                String decoded;
                try
                {
                    decoded = URLDecoder.decode(new String(body, charset));
                }
                catch (UnsupportedEncodingException uee)
                {
                    AbstractFormAuthFilter.logger.error("could not parse request body", uee);
                    decoded = "";
                }
                addtoParameters(decoded.split("&"));
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

    public StringBuffer getRequestURL()
    {
        return requestURL;
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
        return parameters;
    }

    private void addtoParameters(String[] params)
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

    // IOUtils.toByteArray() method *manually* shaded here
    private byte[] toByteArray(final InputStream input) throws IOException
    {
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            byte[] buffer = new byte[8196];
            int n;
            while (-1 != (n = input.read(buffer)))
            {
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        }
    }

    private String method = null;
    private String requestURI = null;
    private String queryString = null;
    private String contextPath = null;
    private String servletPath = null;
    private String pathInfo = null;
    private String pathTranslated = null;
    private StringBuffer requestURL = null;
    private Hashtable<String, Object> attributes = null;
    private Hashtable<String, String> headers = null;
    private Map<String, String[]> parameters = null;
    private byte[] body = null;
}
