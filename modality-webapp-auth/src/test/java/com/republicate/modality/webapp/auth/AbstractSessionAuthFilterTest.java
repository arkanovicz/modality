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

import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.easymock.EasyMock.expect;

public class AbstractSessionAuthFilterTest extends BaseAuthFilterTest
{
    protected class AuthSessionFilter extends BaseSessionAuthFilter<String>
    {
        @Override
        protected String authenticate(HttpServletRequest request) throws ServletException
        {
            return request.getParameter("Good-Login");
        }
    }

    private AuthSessionFilter authFilter;


    @Before
    public void setUp() throws Exception
    {
    }

    protected void recordConfig()
    {
        super.recordConfig();
        expect(servletContext.getContextPath()).andReturn("/");
        expect(filterConfig.getInitParameter(BaseSessionAuthFilter.MAX_INACTIVE_INTERVAL)).andReturn("0");
        expect(filterConfig.getInitParameter(BaseSessionAuthFilter.LOGGED_SESSION_KEY)).andReturn("_user_");
        expect(filterConfig.getInitParameter(BaseSessionAuthFilter.DOLOGIN_URI)).andReturn("/login.do");
        expect(filterConfig.getInitParameter(BaseSessionAuthFilter.DOLOGOUT_URI)).andReturn("/logout.do");
        expect(filterConfig.getInitParameter(BaseSessionAuthFilter.REDIRECT_PARAMETER)).andReturn("redirect");
        expect(filterConfig.getInitParameter(BaseSessionAuthFilter.REDIRECT_REFERRER)).andReturn("true");
        expect(filterConfig.getInitParameter(BaseSessionAuthFilter.REDIRECT_SKIP_CHECKS)).andReturn("false");
        expect(filterConfig.getInitParameter(BaseSessionAuthFilter.INVALIDATE_ON_LOGOUT)).andReturn("true");
    }

    @Test
    public void getAuthentifiedUser() throws Exception
    {
        recordConfig();
        expect(request.getParameter("Good-Login")).andAnswer(eval("Nestor"));
        expect(request.getSession(false)).andReturn(null);
        expect(request.getRequestURI()).andAnswer(eval("/some-uri"));
        replayAll();
        authFilter = new AuthSessionFilter();
        authFilter.init(filterConfig);
        authFilter.getAuthentifiedUser(request);
    }

    @Test
    public void processProtectedRequest() throws Exception
    {
        recordConfig();
        String uri = "/login.do";
        expect(request.getRequestURI()).andAnswer(eval(uri));
        expect(request.getSession(false)).andReturn(null);
        expect(request.getRequestURI()).andAnswer(eval(uri));
        expect(request.getParameter("Good-Login")).andAnswer(eval("Nestor"));
        expect(request.getSession()).andReturn(httpSession);
        expect(httpSession.getAttribute("_user_")).andReturn(null);
        expect(httpSession.isNew()).andReturn(true);
        httpSession.setAttribute("_user_", "Nestor");
        httpSession.setMaxInactiveInterval(0);
        expect(request.getParameter("redirect")).andReturn(null);
        expect(request.getHeader("Referer")).andReturn(null);
        expect(request.getRequestURI()).andAnswer(eval(uri));
        expect(request.getRequestURI()).andAnswer(eval(uri));
        response.setStatus(200);
        replayAll();
        authFilter = new AuthSessionFilter();
        authFilter.init(filterConfig);
        authFilter.doFilter(request, response, filterChain);
    }

    @Test
    public void processForbiddenRequest() throws Exception
    {
        recordConfig();
        String uri = "/some-uri";
        expect(request.getRequestURI()).andAnswer(eval(uri));
        expect(request.getSession(false)).andReturn(null);
        expect(request.getRequestURI()).andAnswer(eval(uri));
        expect(request.getParameter("Good-Login")).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval(uri));
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        replayAll();
        authFilter = new AuthSessionFilter();
        authFilter.init(filterConfig);
        authFilter.doFilter(request, response, filterChain);
    }

    @Test
    public void redirectAfterLogin() throws Exception
    {
        recordConfig();
        String uri = "/login.do";
        expect(request.getRequestURI()).andAnswer(eval(uri));
        expect(request.getSession(false)).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval(uri));
        expect(request.getParameter("Good-Login")).andAnswer(eval("Nestor"));
        expect(request.getSession()).andAnswer(eval(httpSession));
        expect(httpSession.getAttribute("_user_")).andAnswer(eval(null));
        expect(httpSession.isNew()).andAnswer(eval(true));
        httpSession.setAttribute("_user_", "Nestor");
        httpSession.setMaxInactiveInterval(0);
        expect(request.getParameter("redirect")).andAnswer(eval(null));
        expect(request.getHeader("Referer")).andAnswer(eval("http://dummy/index.vhtml"));
        expect(request.getRequestURI()).andAnswer(eval(uri));
        expect(request.getHeader("Host")).andAnswer(eval("dummy"));
        response.sendRedirect("/index.vhtml");
        replayAll();
        authFilter = new AuthSessionFilter();
        authFilter.init(filterConfig);
        authFilter.doFilter(request, response, filterChain);
    }
}
