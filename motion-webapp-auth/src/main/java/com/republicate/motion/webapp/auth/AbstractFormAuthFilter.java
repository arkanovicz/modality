package com.republicate.motion.webapp.auth;

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
import org.apache.velocity.tools.view.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Optional;
import java.util.Set;

/**
 * <p>AuthFilter specialization for form-based logins.</p>
 * <p>Configuration parameters:</p>
 * <ul>
 *     <li><code>auth.form.field.login</code> - login form field name</li>
 *     <li><code>auth.form.<b>field.password</code> - password form field name</li>
 *     <li><code>auth.form.uri.login</code> - log in form URI</li>
 *     <li><code>auth.form.uri.home</code> - public index URI ; defaults to root
 *     resource mapped to '/', typically the welcome file.</li>
 *     <li><code>auth.form.uri.user_home</code> - user home URI : defaults to home URI</li>
 *     <li><code>auth.form.login_redirect</code> - whether to enable redirection towards login URI for protected pages</li>
 *     <li><code>auth.form.success.redirect_get</code> - on success, whether to redirect towards GET request that triggered a redirection towards login</li>
 *     <li><code>auth.form.success.forward_post</code> - on success, whether to forward to POST requests that triggered a redirection towards login</li>
 * </ul>
 * <p>Provided URIs should not include the servlet context path, which will be prefixed to all of them.</p>
 * @param <USER>
 */

public abstract class AbstractFormAuthFilter<USER> extends AbstractSessionAuthFilter<USER>
{
    protected static Logger logger = LoggerFactory.getLogger("auth");

    public static final String LOGIN_FIELD =             "auth.form.field.login";
    public static final String PASSWORD_FIELD =          "auth.form.field.password";
    public static final String LOGIN_URI =               "auth.form.uri.login";
    public static final String HOME_URI =                "auth.form.uri.home";
    public static final String USER_HOME_URI =           "auth.form.uri.user_home";
    public static final String REDIRECT_TOWARDS_LOGIN =  "auth.form.login_redirect";
    public static final String REDIRECT_GET_ON_SUCCESS = "auth.form.success.redirect_get";
    public static final String FORWARD_POST_ON_SUCCESS = "auth.form.success.forward_post";

    private static final String DEFAULT_FORM_LOGIN_FIELD = "login";
    private static final String DEFAULT_FORM_PASSWORD_FIELD = "password";
    private static final String DEFAULT_FORM_LOGIN_URI = "login.vhtml";

    private static final String SAVED_REQUEST_SESSION_KEY = "org.apache.velocity.tools.auth.form.saved_request";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
        final String contextPath = getConfig().getServletContext().getContextPath();
        loginField = Optional.ofNullable(findConfigParameter(LOGIN_FIELD)).orElse(DEFAULT_FORM_LOGIN_FIELD);
        passwordField = Optional.ofNullable(findConfigParameter(PASSWORD_FIELD)).orElse(DEFAULT_FORM_PASSWORD_FIELD);
        loginURI = ServletUtils.combinePath(contextPath, Optional.ofNullable(findConfigParameter(LOGIN_URI)).orElse(DEFAULT_FORM_LOGIN_URI));
        homeURI = ServletUtils.combinePath(contextPath, Optional.ofNullable(findConfigParameter(HOME_URI)).orElse(findIndex()));
        userHomeURI = Optional.ofNullable(findConfigParameter(USER_HOME_URI)).map(path -> ServletUtils.combinePath(contextPath, path)).orElse(homeURI);
        redirectTowardsLogin = BooleanUtils.toBoolean(findConfigParameter(REDIRECT_TOWARDS_LOGIN));
        onSuccessRedirectGET = BooleanUtils.toBoolean(findConfigParameter(REDIRECT_GET_ON_SUCCESS));
        onSuccessForwardPOST = BooleanUtils.toBoolean(findConfigParameter(FORWARD_POST_ON_SUCCESS));
    }

    @Override
    protected USER authenticate(HttpServletRequest request)
    {
        if (request.getMethod().equals("POST"))
        {
            String login = request.getParameter(getLoginField());
            String password = request.getParameter(getPasswordField());
            return checkCredentials(login, password);
        }
        else
        {
            return null;
        }
    }

    protected abstract USER checkCredentials(String login, String password);

    /**
     * Check if a specific request needs login protection
     */
    @Override
    protected boolean isProtectedURI(String uri)
    {
        /* never protect the login page itself */
        if (uri.equals(loginURI))
        {
            return false;
        }
        else
        {
            return super.isProtectedURI(uri);
        }
    }

    @Override
    protected void processForbiddenRequest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        if (redirectTowardsLogin)
        {
            String loginURI = getLoginURI();
            if (loginURI != null)
            {
                logger.debug("redirecting unauthorized request {} towards login URI", request.getRequestURI());
                saveRequestIfNeeded(request);
                response.sendRedirect(loginURI);
                return;
            }
            else
            {
                logger.error("cannot redirecting unauthorized request {} towards login URI: no login URI", request.getRequestURI());
            }
        }
        super.processForbiddenRequest(request, response, filterChain);
    }

    @Override
    protected void processPostLoginRequest(USER user, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        // prioritize query string explicit redirection
        String redirection = getQueryStringRedirection(request);
        if (redirection != null)
        {
            logger.debug("redirecting newly logged in user {} towards {}", displayUser(user), redirection);
            response.sendRedirect(redirection);
        }
        else if (redirectTowardsLogin && (onSuccessRedirectGET || onSuccessForwardPOST))
        {
            SavedRequest savedRequest = getSavedRequest(request);
            if (savedRequest != null)
            {
                switch (savedRequest.getMethod())
                {
                    case "GET":
                        logger.debug("redirecting newly logged in user {} towards {}", displayUser(user), savedRequest.getRequestURI());
                        String url = savedRequest.getRequestURI() + Optional.ofNullable(savedRequest.getQueryString()).map(s -> "?" + URLEncoder.encode(s)).orElse("");
                        response.sendRedirect(url);
                        break;
                    case "POST":
                        logger.debug("forwarding newly logged in user {} towards {}", displayUser(user), savedRequest.getRequestURI());
                        ForwardedRequest forwardedRequest = new ForwardedRequest(request, response, savedRequest);
                        request.getRequestDispatcher(savedRequest.getRequestURI()).forward(forwardedRequest, response);
                        break;
                    default:
                        throw new ServletException("unhandled method: " + savedRequest.getMethod());
                }
            }
        }
        else
        {
            super.processPostLoginRequest(user, request, response, filterChain);
        }
    }

    @Override
    protected void processPublicRequest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        // if redirectTowardsLogin is active and user is logged, redirect from login page to index page
        if (redirectTowardsLogin && request.getRequestURI().equals(getLoginURI()))
        {
            USER user = getSessionUser(request.getSession(false));
            if (user != null)
            {
                logger.debug("redirecting already logged in user {} from login page towards {}", displayUser(user), getUserHomeUri());
                response.sendRedirect(getUserHomeUri());
            }
        }
        else
        {
            filterChain.doFilter(request, response);
        }
    }


    @Override
    protected void processPostLogoutRequest(USER user, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        if (redirectTowardsLogin && getLoginURI() != null)
        {
            response.sendRedirect(getLoginURI());
        }
        else
        {
            super.processPostLogoutRequest(user, request, response, filterChain);
        }
    }

    private void saveRequestIfNeeded(HttpServletRequest request)
    {
        String method = request.getMethod();
        if (onSuccessRedirectGET && method.equals("GET") || onSuccessForwardPOST && method.equals("POST"))
        {
            SavedRequest savedRequest = new SavedRequest(request);
            HttpSession session = request.getSession();
            session.setAttribute(SAVED_REQUEST_SESSION_KEY, savedRequest);
        }
    }

    private SavedRequest getSavedRequest(HttpServletRequest request)
    {
        HttpSession session = request.getSession();
        SavedRequest savedRequest = (SavedRequest)session.getAttribute(SAVED_REQUEST_SESSION_KEY);
        if (savedRequest != null)
        {
            session.removeAttribute(SAVED_REQUEST_SESSION_KEY);
        }
        return savedRequest;
    }

    private void clearSavedRequest(HttpServletRequest request)
    {
        HttpSession session = request.getSession();
        session.removeAttribute(SAVED_REQUEST_SESSION_KEY);
    }

    private String findIndex()
    {
        Set<String> landingPages = getConfig().getServletContext().getResourcePaths("/");
        for (String landingPage : landingPages)
        {
            if (landingPage.indexOf('/', 1) == -1)
            {
                return landingPage;
            }
        }
        return null;
    }

    protected String getLoginField()
    {
        return loginField;
    }

    protected String getPasswordField()
    {
        return passwordField;
    }

    protected String getLoginURI()
    {
        return loginURI;
    }

    protected String getHomeURI()
    {
        return homeURI;
    }

    protected String getUserHomeUri()
    {
        return userHomeURI;
    }

    private String loginField = null;
    private String passwordField = null;
    private String loginURI = null;
    private String homeURI = null;
    private String userHomeURI = null;
    private boolean redirectTowardsLogin = false;
    private boolean onSuccessRedirectGET = false;
    private boolean onSuccessForwardPOST = false;

    protected static Cookie parseCookie(String setCookie)
    {
        return new Cookie("TODO", "TODO");
    }

}
