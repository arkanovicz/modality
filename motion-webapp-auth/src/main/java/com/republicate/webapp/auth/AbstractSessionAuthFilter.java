package com.republicate.webapp.auth;

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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.velocity.tools.view.JeeConfig;
import org.apache.velocity.tools.view.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;


/**
 * <p>AuthFilter specialization for session-based login processes.</p>
 * <p>The filter handles two <i>actions</i>: login and logout.</p>
 * <p>The filter should map all protected resources URIs (including the lougout action URI),
 * as long as the login action URI.</p>
 * <p>Configuration parameters:</p>
 * <ul>
 *     <li><code>auth.session_key</code> - session key under which the object
 *     representing the logged user is stored.</li>
 *     <li><code>auth.session.max_inactive_interval</code> - max session inactive
 *     interval for logged users</li>
 *     <li><code>auth.session.uri.dologin</code> - login action URI (should not include servlet context path)</li>
 *     <li><code>auth.session.uri.dologout</code> - logout action URI (should not include servlet context path)</li>
 *     <li><code>auth.session.redirect.parameter</code> - name of query string
 *     parameter in login & logout actions to take into account (takes precedence over referrer redirection
 *     if activated).
 *     <li><code>auth.session.redirect.referrer</code> - whether to
 *     redirect newly logged users to login action referrer ; defaults to <code>false</code>.</li>
 *     <li><code>auth.session.redirect.skip_checks</code> - allow to skip hostname checks
 *     when doing redirects (for misconfigured or non-rewritring proxies).</li>
 *     <li><code>auth.session.invalidate_on_logout</code> - invalidate sessions at logout ;
 *     defaults to <code>true</code>.</li>
 * </ul>
 *
 * <p>All URIs are prefixed with the servlet context path.</p>
 *
 */

public abstract class AbstractSessionAuthFilter<USER> extends AbstractAuthFilter<USER>
{
    protected static Logger logger = LoggerFactory.getLogger("auth");

    // config parameters keys

    public static final String LOGGED_SESSION_KEY =    "auth.session.logged_key";
    public static final String MAX_INACTIVE_INTERVAL = "auth.session.max_inactive_interval";
    public static final String DOLOGIN_URI =           "auth.session.uri.dologin";
    public static final String DOLOGOUT_URI =          "auth.session.uri.dologout";
    public static final String REDIRECT_PARAMETER =    "auth.session.redirect.parameter";
    public static final String REDIRECT_REFERRER =     "auth.session.redirect.referrer";
    public static final String REDIRECT_SKIP_CHECKS =  "auth.session.redirect.skip_checks";
    public static final String INVALIDATE_ON_LOGOUT =  "auth.session.invalidate_on_logout";

    // default values
    private static final String DEFAULT_LOGGED_SESSION_KEY = "logged";
    private static final String DEFAULT_DOLOGIN_URI = "login.do";
    private static final String DEFAULT_DOLOGOUT_URI = "logout.do";

    /**
     * <p>Filter initialization.</p>
     * <p>Child classes should start with <code>super.init(cilterConfig);</code>.</p>
     * <p>Once parent has been initialized, <code>getConfig()</code> returns a JeeConfig.</p>
     * @param filterConfig filter config
     * @throws ServletException in case of error
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
        String contextPath = getConfig().getServletContext().getContextPath();
        maxInactiveInterval = NumberUtils.toInt(findConfigParameter(MAX_INACTIVE_INTERVAL), 0);
        loggedSessionKey = Optional.ofNullable(findConfigParameter(LOGGED_SESSION_KEY)).orElse(DEFAULT_LOGGED_SESSION_KEY);
        doLoginURI = ServletUtils.combinePath(contextPath, Optional.ofNullable(findConfigParameter(DOLOGIN_URI)).orElse(DEFAULT_DOLOGIN_URI));
        doLogoutURI = ServletUtils.combinePath(contextPath,Optional.ofNullable(findConfigParameter(DOLOGOUT_URI)).orElse(DEFAULT_DOLOGOUT_URI));
        redirectParameter = findConfigParameter(REDIRECT_PARAMETER);
        redirectReferrer = BooleanUtils.toBoolean(findConfigParameter(REDIRECT_REFERRER));
        redirectSkipChecks = BooleanUtils.toBoolean(findConfigParameter(REDIRECT_SKIP_CHECKS));
        invalidateSessionOnLogout = Optional.ofNullable(BooleanUtils.toBoolean(findConfigParameter(INVALIDATE_ON_LOGOUT))).orElse(true);
    }

    @Override
    protected USER getAuthentifiedUser(HttpServletRequest request) throws ServletException
    {
        // check session
        HttpSession session = request.getSession(false);
        USER user = getSessionUser(session);
        if (user != null)
        {
            return user;
        }
        else
        {   // check login
            String uri = request.getRequestURI();
            if (uri.equals(getDoLoginURI()))
            {
                user = super.getAuthentifiedUser(request); // will call authenticate()
                if (user == null)
                {
                    logger.info("failed login with params {}", request.getParameterMap());
                }
                return user;
            }
            return null;
        }
    }

    @Override
    protected void processProtectedRequest(USER user, HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpSession session = request.getSession();
        if (checkSessionUser(user, session))
        {
            setSessionUser(user, session);
            logger.debug("user logged in: {}", displayUser(user));
            processPostLoginRequest(user, request, response, chain);
        }
        else if (request.getRequestURI().equals(getDoLogoutURI()))
        {
            // handle logout requests
            clearSessionUser(user, session);
            if (invalidateSessionOnLogout)
            {
                session.invalidate();
            }
            logger.debug("user logged out: {}", displayUser(user));
            processPostLogoutRequest(user, request, response, chain);
        }
        else
        {
            // proceed to protected resources
            super.processProtectedRequest(user, request, response, chain);
        }
    }

    /**
     * Answers a login request after the user has been successfully logged in.
     * @param user
     * @param request
     * @param response
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    protected void processPostLoginRequest(USER user, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        String redirection = getRedirection(request);
        if (redirection != null)
        {
            // redirection after login
            logger.debug("logged in user redirected towards {}: {}", redirection, displayUser(user));
            response.sendRedirect(redirection);
        }
        else
        {
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    /**
     * Answers a non-redirected logout request after the user has been logged out.
     * @param user
     * @param request
     * @param response
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    protected void processPostLogoutRequest(USER user, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        String redirection = getQueryStringRedirection(request);
        if (redirection != null)
        {
            // redirection after login
            logger.debug("logged out user redirected towards {}: {}", redirection, displayUser(user));
            response.sendRedirect(redirection);
        }
        else
        {
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    protected boolean checkSessionUser(USER user, HttpSession session) throws ServletException
    {
        USER prev = null;
        if (!session.isNew())
        {
            prev = getSessionUser(session);
        }

        if (prev == null)
        {
            // handle newly logged users
            return true;
        }
        else if (prev != user)
        {
            // something got horribly wrong
            throw new ServletException("auth: invalid session state");
        }
        return false;
    }

    protected USER getSessionUser(HttpSession session) throws ServletException
    {
        if (session == null)
        {
            return null;
        }
        USER user = (USER)session.getAttribute(loggedSessionKey);
        return user;
    }

    protected void setSessionUser(USER user, HttpSession session) throws ServletException
    {
        session.setAttribute(loggedSessionKey, user);
        session.setMaxInactiveInterval(maxInactiveInterval);
    }

    protected void clearSessionUser(USER user, HttpSession session) throws ServletException
    {
        session.removeAttribute(loggedSessionKey);
    }

    protected final String getQueryStringRedirection(HttpServletRequest request)
    {
        if (getRedirectParameter() != null)
        {
            return validateRedirection(request, request.getParameter(redirectParameter));
        }
        return null;
    }

    protected final String getReferrerRedirection(HttpServletRequest request) throws IOException
    {
        if (getRedirectReferrer())
        {
            return validateRedirection(request, request.getHeader("Referer"));
        }
        return null;
    }

    protected String getRedirection(HttpServletRequest request) throws IOException
    {
        return Optional.ofNullable(getQueryStringRedirection(request))
            .orElse(getReferrerRedirection(request));
    }

    private String validateRedirection(HttpServletRequest request, String redirection)
    {
        if (redirection == null || redirectSkipChecks || redirection.startsWith("/") || !redirection.contains("://"))
        {
            // null or relative or validation disabled
            return redirection;
        }
        URL url = null;
        try
        {
            url = new URL(redirection);
        }
        catch (MalformedURLException mue)
        {
            logger.warn("not honoring redirect request towards {}: invalid URL", redirection);
        }

        // check that hostnames do match
        String requestHost = request.getHeader("Host");
        String referrerHost = url.getHost();
        if (requestHost.equals(referrerHost))
        {
            String path = url.getPath();
            String query = url.getQuery();
            String anchor = url.getRef();
            StringBuilder dest = new StringBuilder(path);
            if (query != null)
            {
                dest.append('&').append(query);
            }
            if (anchor != null)
            {
                dest.append('#').append(anchor);
            }
            return dest.toString();
        }
        else
        {
            logger.warn("not honoring redirect request towards {}: hostnames {} and {} do not match", redirection, requestHost, referrerHost);
            return null;
        }
    }

    protected String getDoLoginURI()
    {
        return doLoginURI;
    }

    protected String getDoLogoutURI()
    {
        return doLogoutURI;
    }

    protected String getRedirectParameter()
    {
        return redirectParameter;
    }

    protected boolean getRedirectReferrer()
    {
        return redirectReferrer;
    }

    /**
     * <p>What to display in the logs for a user, like in the logs.</p>
     * <p>A child class can change it to return something like
     * <code>return user.getString('login');</code>.</p>
     * @param user target user
     * @return string representation for the user
     */
    protected String displayUser(USER user)
    {
        // user.getString
        return String.valueOf(user);
    }

    private JeeConfig config = null;
    private int maxInactiveInterval = 0;
    private String loggedSessionKey = null;
    private String doLoginURI = null;
    private String doLogoutURI = null;
    private String redirectParameter = null;
    private boolean redirectReferrer = false;
    private boolean redirectSkipChecks = false;
    private boolean invalidateSessionOnLogout = false;

    // for oauth
    // ...
}
