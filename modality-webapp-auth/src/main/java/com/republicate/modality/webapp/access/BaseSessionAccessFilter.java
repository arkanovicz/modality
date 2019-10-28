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

import org.apache.commons.lang3.math.NumberUtils;

import java.util.Optional;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static com.republicate.modality.webapp.auth.BaseSessionAuthFilter.LOGGED_SESSION_KEY;

/**
 *
 * @param <USER> User class
 */

public abstract class BaseSessionAccessFilter<USER> extends BaseAccessFilter<USER>
{
    private static final String DEFAULT_LOGGED_SESSION_KEY = "logged_user";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
        loggedSessionKey = Optional.ofNullable(findConfigParameter(LOGGED_SESSION_KEY)).orElse(DEFAULT_LOGGED_SESSION_KEY);
    }

    protected USER getAuthentifiedUser(HttpServletRequest request) throws ServletException
    {
        HttpSession session = request.getSession(false);
        if (session == null)
        {
            return null;
        }
        USER user = (USER)session.getAttribute(loggedSessionKey);
        return user;
    }

    private String loggedSessionKey = null;
}
