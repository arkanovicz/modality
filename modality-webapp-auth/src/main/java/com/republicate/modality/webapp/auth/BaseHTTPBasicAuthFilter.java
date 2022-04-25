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

import com.republicate.modality.util.ConversionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Filter for HTTP Basic authentication.</p>
 */

public abstract class BaseHTTPBasicAuthFilter<USER> extends BaseAuthFilter<USER>
{
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
    }

    @Override
    protected USER authenticate(HttpServletRequest request) throws ServletException
    {
        String authHeader = request.getHeader("Authenticate");
        USER ret = null;
        if (authHeader != null && authHeader.startsWith("Basic "))
        {
            byte[] decrypted = ConversionUtils.base64Decode(authHeader.substring(6));
            String clear = new String(decrypted, StandardCharsets.UTF_8);
            int sep = clear.indexOf(':');
            if (sep == -1)
            {
                logger.debug("invalid Basic authentication: {}", clear);
            }
            else
            {
                String login = clear.substring(0, sep);
                String password = clear.substring(sep + 1);
                ret = checkCredentials(login, password);
            }
        }
        else
        {
            logger.debug("invalid Basic authentication: {}", authHeader);
        }
        return ret;
    }

    @Override
    protected void processForbiddenRequest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        logger.debug("unauthorized request towards {}", request.getRequestURI());
        response.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealm() + "\", charset=\"UTF-8\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    protected abstract USER checkCredentials(String login, String password) throws ServletException;
}
