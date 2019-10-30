package com.republicate.modality.webapp.access;

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

import com.republicate.modality.webapp.ModalityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.republicate.modality.webapp.auth.BaseAuthFilter.PROTECTED_RESOURCES;

/**
 * <p>Access Control filter skeleton.</p>
 * @param <USER> User class
 */

public abstract class BaseAccessFilter<USER> extends ModalityFilter
{
    protected static Logger logger = LoggerFactory.getLogger("access");

    public static String WHITELIST = "access.whitelist";

    abstract protected USER getAuthentifiedUser(HttpServletRequest request) throws ServletException;

    abstract protected boolean isGrantedAcess(USER user, HttpServletRequest request);

    protected boolean isProtectedURI(String uri)
    {
        if (protectedResources != null)
        {
            return protectedResources.matcher(uri).matches();
        }
        else
        {
            return true;
        }
    }

    protected void processPublicRequest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        filterChain.doFilter(request, response);
    }

    protected boolean isWhitelistedURI(String uri)
    {
        if (whitelistedResources != null)
        {
            return whitelistedResources.matcher(uri).matches();
        }
        else
        {
            return false;
        }
    }

    protected void processGrantedAccessRequest(USER user, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        filterChain.doFilter(request, response);
    }

    protected void processForbiddenRequest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
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

    /* Filter interface */

    /**
     * <p>Filter initialization.</p>
     * <p>Child classes should start with <code>super.configure(filterConfig);</code>.</p>
     * <p>Once parent has been initialized, <code>getModelProvider()</code> returns a JeeConfig.</p>
     * @param filterConfig filter config
     * @throws ServletException in case of error
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);

        // read auth.protected regex
        String protectedResourcesPattern = findConfigParameter(PROTECTED_RESOURCES);
        if (protectedResourcesPattern != null)
        {
            try
            {
                protectedResources = Pattern.compile(protectedResourcesPattern);
            }
            catch (PatternSyntaxException pse)
            {
                throw new ServletException("could not configure protected resources pattern", pse);
            }
        }

        // read access.whitelist regex
        String whitelistPattern = findConfigParameter(WHITELIST);
        if (whitelistPattern != null)
        {
            try
            {
                whitelistedResources = Pattern.compile(whitelistPattern);
            }
            catch (PatternSyntaxException pse)
            {
                throw new ServletException("could not configure whitelist pattern", pse);
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;

        String uri = request.getRequestURI();

        if (isProtectedURI(uri))
        {
            USER user = getAuthentifiedUser(request);
            if (user == null)
            {
                // the authentication filter should have catched it
                logger.debug("user not logged, not granting access towards protected resource {}", request.getRequestURI());
                processForbiddenRequest(request, response, chain);
            }
            else
            {
                if (isWhitelistedURI(uri))
                {
                    logger.debug("user {} granted access towards whitelisted resource {}", displayUser(user), request.getRequestURI());
                    processPublicRequest(request, response, chain);
                }
                else
                {
                    if (isGrantedAcess(user, request))
                    {
                        logger.debug("user {} granted access towards {}", displayUser(user), request.getRequestURI());
                        processGrantedAccessRequest(user, request, response, chain);
                    }
                    else
                    {
                        // the authentication filter should have catched it
                        logger.debug("user {} was denied access towards protected resource {}", displayUser(user), request.getRequestURI());
                        processForbiddenRequest(request, response, chain);
                    }
                }
            }
        }
        else
        {
            // not logging anything
            processPublicRequest(request, response, chain);
        }
    }

    @Override
    public void destroy()
    {

    }

    /**
     * protected resources
     */
    private Pattern protectedResources = null;

    /**
     * whitelisted resources
     */
    private Pattern whitelistedResources = null;
}
