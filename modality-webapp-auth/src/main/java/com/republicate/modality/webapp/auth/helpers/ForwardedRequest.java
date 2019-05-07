package com.republicate.modality.webapp.auth.helpers;

import com.republicate.modality.webapp.util.CookieParser;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

public class ForwardedRequest extends HttpServletRequestWrapper
{
    protected static Logger logger = LoggerFactory.getLogger("auth");

    public ForwardedRequest(HttpServletRequest forwardedRequest, HttpServletResponse response, SavedRequest savedRequest)
    {
        super(forwardedRequest);
        this.savedRequest = savedRequest;
        additionalCookies = response.getHeaders("Set-Cookie").stream().map(CookieParser::parseCookie).collect(Collectors.toList());
    }

    @Override
    public Cookie[] getCookies()
    {
        return (Cookie[])ArrayUtils.addAll(super.getCookies(), additionalCookies.toArray());
    }

    @Override
    public long getDateHeader(String name)
    {
        return Optional.ofNullable(getHeader(name)).map(value -> Long.valueOf(value)).orElse(-1l);
    }

    @Override
    public String getHeader(String name)
    {
        return savedRequest.getHeaders().get(name);
    }

    @Override
    public Enumeration<String> getHeaderNames()
    {
        return savedRequest.getHeaders().keys();
    }

    @Override
    public Enumeration<String> getHeaders(String name)
    {
        final String value = savedRequest.getHeaders().get(name);
        return new Enumeration<String>()
        {
            @Override
            public boolean hasMoreElements()
            {
                return first-- > 0;
            }

            @Override
            public String nextElement()
            {
                return value;
            }
            private int first = 1;
        };
    }

    @Override
    public int getIntHeader(String name)
    {
        return Optional.ofNullable(getHeader(name)).map(value -> Integer.valueOf(value)).orElse(-1);
    }

    @Override
    public String getPathInfo()
    {
        return savedRequest.getPathInfo();
    }

    @Override
    public String getPathTranslated()
    {
        return savedRequest.getPathTranslated();
    }

    @Override
    public String getContextPath()
    {
        return savedRequest.getContextPath();
    }

    @Override
    public String getQueryString()
    {
        return savedRequest.getQueryString();
    }

    @Override
    public String getRequestURI()
    {
        return savedRequest.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL()
    {
        return savedRequest.getRequestURL();
    }

    @Override
    public String getServletPath()
    {
        return savedRequest.getServletPath();
    }

    @Override
    public Object getAttribute(String name)
    {
        return savedRequest.getAttributes().get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames()
    {
        return savedRequest.getAttributes().keys();
    }

    @Override
    public int getContentLength()
    {
        return savedRequest.getBody().length;
    }

    @Override
    public long getContentLengthLong()
    {
        return getContentLength();
    }

    @Override
    public String getContentType()
    {
        return getHeader("Content-Type");
    }

    @Override
    public ServletInputStream getInputStream()
    {
        byte[] body = Optional.ofNullable(savedRequest.getBody()).orElse(new byte[0]);
        final ByteArrayInputStream buffer = new ByteArrayInputStream(body);
        return new ServletInputStream()
        {
            @Override
            public boolean isFinished()
            {
                return buffer.available() == 0;
            }

            @Override
            public boolean isReady()
            {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener)
            {
                throw new NotImplementedException("not implemented");
            }

            @Override
            public int read() throws IOException
            {
                return buffer.read();
            }
        };
    }

    @Override
    public String getParameter(String name)
    {
        return Optional.ofNullable(savedRequest.getParameters().get(name)).map(arr -> arr[0]).orElse(null);
    }

    @Override
    public Map<String, String[]> getParameterMap()
    {
        return savedRequest.getParameters();
    }

    @Override
    public Enumeration<String> getParameterNames()
    {
        return Collections.enumeration(savedRequest.getParameters().keySet());
    }

    @Override
    public String[] getParameterValues(String name)
    {
        return savedRequest.getParameters().get(name);
    }

    @Override
    public BufferedReader getReader()
    {
        try
        {
            byte[] body = Optional.ofNullable(savedRequest.getBody()).orElse(new byte[0]);
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), getCharacterEncoding()));
        }
        catch (UnsupportedEncodingException uee)
        {
            logger.error("could not get request reader", uee);
            return null;
        }
    }

    @Override
    public ServletRequest getRequest()
    {
        throw new NotImplementedException("saved requests cannot be unwrapped");
    }

    @Override
    public boolean isAsyncSupported()
    {
        return false;
    }

    @Override
    public void removeAttribute(String name)
    {
        savedRequest.getAttributes().remove(name);
    }

    private SavedRequest savedRequest;
    private List<Cookie> additionalCookies = null;
}
