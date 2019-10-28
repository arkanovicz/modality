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
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <p>Access Control filter skeleton.</p>
 * @param <USER> User class
 */

public abstract class BaseAccessFilter<USER> extends ModalityFilter
{
    protected static Logger logger = LoggerFactory.getLogger("access");

    abstract protected USER getAuthentifiedUser(HttpServletRequest request) throws ServletException;

    abstract protected boolean isGrantedAcess(USER user, HttpServletRequest request);

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
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;

        USER user = getAuthentifiedUser(request);
        if (user != null && isGrantedAcess(user, request))
        {
            logger.debug("user {} allowed towards {}", displayUser(user), request.getRequestURI());
            chain.doFilter(request, response);
        }
        else
        {
            logger.debug("user {} not granted towards {}", displayUser(user), request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Override
    public void destroy()
    {

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
}
