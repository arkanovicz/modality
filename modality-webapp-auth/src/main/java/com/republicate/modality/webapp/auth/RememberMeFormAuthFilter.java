package com.republicate.modality.webapp.auth;

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

import com.republicate.modality.Instance;
import com.republicate.modality.webapp.auth.helpers.RememberMeCookieHandler;
import com.republicate.modality.webapp.auth.helpers.RememberMeCookieHandlerImpl;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.velocity.tools.ClassUtils;
import org.apache.velocity.tools.view.ServletUtils;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <p>Form authentication filter that handles a "remember me" cookie.</p>
 * <p>Convention over configuration: the filter expects to find in the model:</p>
 * <ul>
 *     <li>a root <code>create_remember_me</code> action, which creates a valid <code>remember_me</code>
 *     entry in the database. It takes as input parameters all the key columns of the users entity
 *     plus <code>&lt;secure_key/&gt;</code> and (optionally) <code>&lt;ip/&gt;</code>.</li>
 *     <li>a root <code>check_remember_me</code> row attribute, which takes the same parameters as input
 *     and returns the valid User instance, or null. Its result entity must be the users entity.</li>
 *     <li>an optional but advised root <code>refresh_remember_me</code> action, refreshing the secure key, taking the same parameters.</li>
 *     <li>an optional but advised root <code>reset_remember_me</code> action, taking the same parameters.</li>
 *     <li>an optional root <code>clean_remember_me</code> action, which deletes all expired entries.</li>
 * </ul>
 * <p>Configuration parameters:</p>
 * <ul>
 *     <li><code>auth.cookie.name</code> - name of both the Remember Me cookie and the
 *     corresponding checkbox input name. Defaults to <code>remember_me</code>.</li>
 *     <li><code>auth.cookie.path</code> - path of the Remember Me cookie, defaults to "/" (prepended by the
 *     servlet context path).</li>
 *     <li><code>auth.cookie.max_age</code> - Max age of the Remember Me cookie, defaults to 31536000 (one year).</li>
 *     <li><code>auth.cookie.check_ip</code> - Whether to check request IP, defaults to true.</li>
 *     <li><code>auth.cookie.secure</code> - Whether to use a secure cookie, defaults to true.</li>
 *     <li><code>auth.cookie.handler</code> - either a class name or a living instance of a class extending <code>RememberMeCookieHandler</code> ;
 *     all previous parameters are ignored, next ones are taken into account. The default handler is <code>RememberMeCookieHandlerImpl</code></li>
 *     <li><code>auth.cookie.clean_rate</code> - Rate at which expired entries are cleared from the database,
 *     defaults to 86400 (one day). Note: the cleaning process is triggered by the J2EE server initialization,
 *     and then by the first request that occurs past the refresh rate without server restart.</li>
 *     <li><code>auth.cookie.consider_public_requests</code> - Whether to log in by cookie also on requests towards
 *     public resources. Defaults to true.</li>
 * </ul>
 * </ul>
 * <p>Or, you can forget every configuration param above and define <code></code>
 * to .
 * <p>Typical implementation:</p>
 *
 * <ul>
 *     <li>SQL table:
 * <pre><code>CREATE TABLE remember_me (
 *   us_id INT NOT NULL,
 *   ip VARCHAR(50) NOT NULL,
 *   secure_key TODO NOT NULL,
 *   creation DATETIME NOT NULL,
 *   PRIMARY KEY (us_id, ip, secure_key),
 *   FOREIGN KEY user (us_id) REFERENCES user (us_id)
 *   );</code></pre></li>
 *   <li>Model definition file:
 *   <pre><code>&lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *   &lt;model ...&gt;
 *       ...
 *       &lt;action name="create_remember_me"&gt;
 *           INSERT INTO remember_me (us_id, ip, secure_key, creation) VALUES (&lt;us_id/&gt;, &lt;ip/&gt;, &lt;secure_key/&gt;, NOW());
 *       &lt;/action&gt;
 *
 *       &lt;row name="check_remember_me" result="user"&gt;
 *           SELECT user.*
 *           FROM   remember_me
 *             JOIN user ON user.us_id = remember_me.us_id
 *           WHERE remember_me.us_id = &lt;us_id/&gt;
 *             AND remember_me.ip = &lt;ip/&gt;
 *             AND remember_me.secure_key = &lt;secure_key/&gt;
 *             AND creation >= now() - interval 365 day;
 *       &lt;/row&gt;
 *
 *       &lt;action name="refresh_remember_me"&gt;
 *           UPDATE remember_me
 *           SET remember_me.secure_key = &lt;secure_key/&gt;,
 *               remember_me.creation = now()
 *           WHERE remember_me.us_id = &lt;us_id/&gt;
 *             AND remember_me.ip = &lt;ip/&gt;;
 *       &lt;/action&gt;

 *       &lt;action name="reset_remember_me"&gt;
 *           DELETE FROM remember_me
 *           WHERE remember_me.us_id = &lt;us_id/&gt;
 *             AND remember_me.ip = &lt;ip/&gt;
 *             AND remember_me.secure_key = &lt;secure_key/&gt;
 *             AND creation >= now() - interval 365 day;
 *       &lt;/action&gt;

 *       &lt;action name="clean_remember_me"&gt;&lt;![CDATA[
 *           DELETE FROM remember_me WHERE creation < now() - interval 365 day;]]&gt;
 *       &lt;/action&gt;
 *       ...
 *   &lt;/model&gt;
 *   </code></pre></li>
 *   <p>As a last note: the client application should ask the user for his/her current password when (s)he wants to change it
 *   and this operation should erase the remember_me cookie.</p>
 */

public class RememberMeFormAuthFilter extends FormAuthFilter
{
    public static final String COOKIE_NAME =            "auth.cookie.name";
    public static final String COOKIE_DOMAIN =          "auth.cookie.domain";
    public static final String COOKIE_PATH =            "auth.cookie.path";
    public static final String COOKIE_MAX_AGE =         "auth.cookie.max_age";
    public static final String COOKIE_CHECK_UA =        "auth.cookie.check.user_agent";
    public static final String COOKIE_CHECK_IP =        "auth.cookie.check.ip";
    public static final String COOKIE_HANDLER =         "auth.cookie.handler";
    public static final String COOKIE_CLEAN_RATE =      "auth.cookie.clean_rate";
    public static final String COOKIE_CONSIDER_PUBLIC = "auth.cookie.consider_public_requests";
    public static final String COOKIE_SECURE =          "auth.cookie.secure";

    private static final String DEFAULT_COOKIE_NAME =       "remember_me";
    private static final String DEFAULT_COOKIE_PATH =       "/";
    private static final int    DEFAULT_COOKIE_MAX_AGE =    31536000; // one year
    private static final int    DEFAULT_COOKIE_CLEAN_RATE = 86400; // one day

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
        Object handler = findConfigParameter(COOKIE_HANDLER);
        if (handler != null)
        {
            if (handler instanceof String)
            {
                try
                {
                    Class handlerClass = ClassUtils.getClass((String) handler);
                    rememberMeCookieHandler = (RememberMeCookieHandlerImpl)handlerClass.newInstance();
                }
                catch (ClassNotFoundException cnfe)
                {
                    throw new ServletException("invalid remember_me cookie handler class name: " + handler, cnfe);
                }
                catch (InstantiationException | IllegalAccessException e)
                {
                    throw new ServletException("could not create a new instance of the remember_me cookie handler class: " + handler, e);
                }
                catch (ClassCastException cce)
                {
                    throw new ServletException("could not create a new instance of the remember_me cookie handler class: " + handler, cce);
                }
            }
            else
            {
                try
                {
                    rememberMeCookieHandler = (RememberMeCookieHandlerImpl)handler;
                }
                catch (ClassCastException cce)
                {
                    throw new ServletException("provided instance cannot be cast to RememberMeCookieHandler", cce);
                }
            }
        }
        else
        {
            String cookieName = Optional.ofNullable(findConfigParameter(COOKIE_NAME)).orElse(DEFAULT_COOKIE_NAME);
            String cookieDomain = findConfigParameter(COOKIE_DOMAIN);
            String cookiePath = Optional.ofNullable(findConfigParameter(COOKIE_PATH)).orElse(ServletUtils.combinePath(getConfig().getServletContext().getContextPath(), DEFAULT_COOKIE_PATH));
            int cookieMaxAge = NumberUtils.toInt(findConfigParameter(COOKIE_MAX_AGE), DEFAULT_COOKIE_MAX_AGE);
            boolean cookieSecure = Optional.ofNullable(BooleanUtils.toBooleanObject(findConfigParameter(COOKIE_SECURE))).orElse(true);
            boolean checkUserAgent = Optional.ofNullable(BooleanUtils.toBooleanObject(findConfigParameter(COOKIE_CHECK_UA))).orElse(true);
            boolean checkIP = BooleanUtils.toBoolean(findConfigParameter(COOKIE_CHECK_IP));
            rememberMeCookieHandler = new RememberMeCookieHandlerImpl(cookieName, cookieDomain, cookiePath, cookieMaxAge, cookieSecure, checkUserAgent, checkIP);
        }
        cleanRate = NumberUtils.toInt(findConfigParameter(COOKIE_CLEAN_RATE), DEFAULT_COOKIE_CLEAN_RATE);
        considerPublicRequests = Optional.ofNullable(BooleanUtils.toBooleanObject(findConfigParameter(COOKIE_CONSIDER_PUBLIC))).orElse(true);
    }

    @Override
    protected void initModel() throws ServletException
    {
        super.initModel();
        rememberMeCookieHandler.setModel(getModel());
    }

    @Override
    protected void processForbiddenRequest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        // second chance using cookie
        Instance user = checkRememberMeCookie(request, response);
        if (user != null)
        {
            processProtectedRequest(user, request, response, filterChain);
        }
        else
        {
            super.processForbiddenRequest(request, response, filterChain);
        }
    }

    /**
     * Also check cookie on public pages
     * @param request
     * @return
     * @throws ServletException
     */
    @Override
    protected void processPublicRequest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        if (considerPublicRequests)
        {
            Instance logged = getSessionUser(request.getSession(false));
            if (logged == null)
            {
                logged = checkRememberMeCookie(request, response);
                // TODO - don't stay on loggin page if user just go logged in by cookie
            }
        }
        super.processPublicRequest(request, response, filterChain);
    }

    @Override
    protected void processPostLoginRequest(Instance user, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        getModel(); // force model initialization
        rememberMeCookieHandler.setRememberMe(user, request, response);
        super.processPostLoginRequest(user, request, response, filterChain);
    }

    @Override
    protected void processPostLogoutRequest(Instance user, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        getModel(); // force model initialization
        rememberMeCookieHandler.resetRememberMe(user, request, response);
        super.processPostLogoutRequest(user, request, response, filterChain);
    }

    protected Instance checkRememberMeCookie(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        getModel(); // force model initialization
        Instance user = rememberMeCookieHandler.getRememberMe(request);
        if (user != null)
        {
            HttpSession session = request.getSession();
            if (checkSessionUser(user, session))
            {
                setSessionUser(user, session);
                rememberMeCookieHandler.refreshRememberMe(user, request, response);
                logger.debug("user logged in by cookie: {}", displayUser(user));
                return user;
            } else
            {
                logger.debug("invalid cookie for user {}", displayUser(user));
            }
        }
        return user;
    }

    protected RememberMeCookieHandler getCookieHandler()
    {
        return rememberMeCookieHandler;
    }

    private RememberMeCookieHandler rememberMeCookieHandler = null;
    private boolean considerPublicRequests = true;
    private int cleanRate;
}
