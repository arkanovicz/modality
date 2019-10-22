package com.republicate.modality.webapp;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class IndexFilter implements Filter
{

    public static final String INDEX = "index.vhtml";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String uri = request.getRequestURI();
        if (uri.endsWith("/"))
        {
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY); // 301
            response.setHeader("Location", uri + INDEX);
            // why ?
            // response.setHeader("Connection", "close");
        }
        else
        {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void destroy()
    {
    }

    // init() and destroy() can be NOOP.
}