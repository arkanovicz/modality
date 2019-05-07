package com.republicate.modality.webapp.auth;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.velocity.tools.view.ServletUtils;

import java.util.Optional;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>AuthFilter specialization for header-based authorization processes.</p>
 * <p>Configuration parameters:</p>
 * <ul>
 *     <li><code>auth.header.name</code> - name of authorization header, defaults to Authorization</li>
 * </ul>
 */


public abstract class AbstractHeaderAuthFilter<USER> extends AbstractAuthFilter<USER>
{
    // config parameters keys

    public static final String HEADER_NAME = "auth.header.name";

    // default values
    private static final String DEFAULT_HEADER_NAME = "Authorization";

    /**
     * <p>Filter initialization.</p>
     * <p>Child classes should start with <code>super.init(filterConfig);</code>.</p>
     * <p>Once parent has been initialized, <code>getConfig()</code> returns a JeeConfig.</p>
     * @param filterConfig filter config
     * @throws ServletException in case of error
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
        headerName = Optional.ofNullable(findConfigParameter(HEADER_NAME)).orElse(DEFAULT_HEADER_NAME);
    }

    @Override
    protected USER authenticate(HttpServletRequest request) throws ServletException
    {
        String headerValue = request.getHeader(getHeaderName());
        return headerValue == null ? null : authorize(headerValue);
    }

    abstract USER authorize(String headerValue) throws ServletException;

    protected String getHeaderName()
    {
        return headerName;
    }

    private String headerName = null;
}
