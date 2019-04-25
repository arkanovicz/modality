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

import org.apache.velocity.tools.model.Instance;
import org.apache.velocity.tools.model.Model;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RememberMeCookieHandler
{
    default RememberMeCookieHandler setModel(Model model) throws ServletException
    {
        // nop
        return this;
    }

    void setRememberMe(Instance user, HttpServletRequest request, HttpServletResponse response) throws ServletException;

    Instance getRememberMe(HttpServletRequest request) throws ServletException;

    default void resetRememberMe(Instance user, HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        // nop
    }

    default void refreshRememberMe(Instance user, HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        resetRememberMe(user, request, response);
        setRememberMe(user, request, response);
    }
}
