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
import com.republicate.modality.Model;
import com.republicate.modality.webapp.auth.helpers.SavedRequest;
import org.apache.velocity.tools.view.ServletUtils;
import org.easymock.Capture;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import static org.easymock.EasyMock.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RememberMeFormAuthFilterTests extends BaseFormAuthFilterTests
{
    @BeforeClass
    public static void populateDataSource() throws Exception
    {
        BaseBookshelfTests.populateDataSource();
    }

    protected void recordFilterConfig(boolean redirectTowardsLogin, boolean redirectGetRequests, boolean forwardPostRequests) throws Exception
    {
        super.recordFilterConfig(redirectTowardsLogin, redirectGetRequests, forwardPostRequests);
        expect(filterConfig.getInitParameter(RememberMeFormAuthFilter.COOKIE_HANDLER)).andAnswer(eval(null));
        expect(servletContext.getInitParameter(RememberMeFormAuthFilter.COOKIE_HANDLER)).andAnswer(eval(null));
        expect(filterConfig.getInitParameter(RememberMeFormAuthFilter.COOKIE_NAME)).andAnswer(eval("remember_me"));
        expect(filterConfig.getInitParameter(RememberMeFormAuthFilter.COOKIE_DOMAIN)).andAnswer(eval(null));
        expect(servletContext.getInitParameter(RememberMeFormAuthFilter.COOKIE_DOMAIN)).andAnswer(eval(null));
        expect(filterConfig.getInitParameter(RememberMeFormAuthFilter.COOKIE_PATH)).andAnswer(eval("/"));
        expect(servletContext.getContextPath()).andAnswer(eval("/"));
        expect(filterConfig.getInitParameter(RememberMeFormAuthFilter.COOKIE_MAX_AGE)).andAnswer(eval("10000"));
        expect(filterConfig.getInitParameter(RememberMeFormAuthFilter.COOKIE_SECURE)).andAnswer(eval("false"));
        expect(filterConfig.getInitParameter(RememberMeFormAuthFilter.COOKIE_CLEAN_RATE)).andAnswer(eval("0"));
        expect(filterConfig.getInitParameter(RememberMeFormAuthFilter.COOKIE_CONSIDER_PUBLIC)).andAnswer(eval("true"));
    }

    static Cookie cookie = null;

    private RememberMeFormAuthFilter createFilter()
    {
        return new RememberMeFormAuthFilter()
        {
            @Override
            public void modelInitialized(Model model) throws ServletException
            {
                super.modelInitialized(model);
            }
        };
    }

    @Test
    public void testFormLoginSetcookie() throws Exception
    {
        recordVelocityConfig(true, "user_cred_tools.xml");
        recordFilterConfig(true, true, true);
        recordFilterRequireInit();

        expect(servletContext.getResource("/WEB-INF/user_cred_model.xml")).andAnswer(eval(getResource("user_cred_cookie_model.xml")));
        Capture<Model> model = new Capture<>();
        servletContext.setAttribute(eq("model"), capture(model));
        expect(servletContext.getAttribute("model")).andAnswer(evalCapture(model));

        // GET /index.vhtml : save request and redirect to /login.vhtml
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getCookies()).andAnswer(eval(new Cookie[0]));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getMethod()).andAnswer(eval("GET"));
        Capture<SavedRequest> savedRequest = recordGETRequestCapture("/index.vhtml");
        response.sendRedirect("/login.vhtml");

        // GET /login.vhtml : let go
        expect(request.getRequestURI()).andAnswer(eval("/login.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(null));
        expect(request.getCookies()).andAnswer(eval(new Cookie[0]));
        expect(request.getRequestURI()).andAnswer(eval("/login.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(null));
        filterChain.doFilter(request, response);

        // POST /login.do : authenticate and redirect to /index.vhtml
        Capture<Instance> user = recordSuccessfullLogin();
        expect(request.getParameter("remember_me")).andAnswer(eval("on"));
        expect(request.getHeader("X-Forwarded-For")).andAnswer(eval(null));
        expect(request.getRemoteAddr()).andAnswer(eval("127.0.0.1"));
        expect(request.getHeader("User-Agent")).andAnswer(eval("tests"));
        Capture<Cookie> cookieCapture = new Capture<>();
        response.addCookie(capture(cookieCapture));
        expect(request.getParameter("redirect")).andAnswer(eval(null));
        expect(request.getSession()).andAnswer(eval(session));
        expect(session.getAttribute(eq("org.apache.velocity.tools.auth.form.saved_request"))).andAnswer(evalCapture(savedRequest));
        session.removeAttribute("org.apache.velocity.tools.auth.form.saved_request");
//        expect(request.getRequestURI()).andAnswer(eval("/login.do"));
//        expect(request.getQueryString()).andAnswer(eval(null));
        response.sendRedirect("/index.vhtml");

        // GET /index.vhtml : let go
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(session));
        expect(session.getAttribute("_user_")).andAnswer(evalCapture(user));
        expect(request.getSession()).andAnswer(eval(session));
        expect(session.isNew()).andAnswer(eval(false));
        expect(session.getAttribute("_user_")).andAnswer(evalCapture(user));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getCharacterEncoding()).andAnswer(eval("utf-8"));
        filterChain.doFilter(request, response);

        replayAll();

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity configure now, just to ease tests
        RememberMeFormAuthFilter filter = createFilter();
        filter.init(filterConfig);
        filter.getModel();
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        cookie = cookieCapture.getValue();
    }

    @Test
    public void testLogByCookie() throws Exception
    {
        recordVelocityConfig(true, "user_cred_tools.xml");
        recordFilterConfig(true, true, true);
        recordFilterRequireInit();

        expect(servletContext.getResource("/WEB-INF/user_cred_model.xml")).andAnswer(eval(getResource("user_cred_cookie_model.xml")));
        Capture<Model> model = new Capture<>();
        servletContext.setAttribute(eq("model"), capture(model));
        expect(servletContext.getAttribute("model")).andAnswer(evalCapture(model));

        // GET /index.vhtml : log by cookie
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getCookies()).andAnswer(eval(new Cookie[] { cookie } ));
        expect(request.getHeader("X-Forwarded-For")).andAnswer(eval(null));
        expect(request.getRemoteAddr()).andAnswer(eval("127.0.0.1"));
        expect(request.getHeader("User-Agent")).andAnswer(eval("tests"));
        expect(request.getSession()).andAnswer(eval(session));
        expect(session.isNew()).andAnswer(eval(true));
        Capture<Instance> user = new Capture<>();
        session.setAttribute(eq("_user_"), capture(user));
        session.setMaxInactiveInterval(0);
        expect(request.getHeader("X-Forwarded-For")).andAnswer(eval(null));
        expect(request.getRemoteAddr()).andAnswer(eval("127.0.0.1"));
        expect(request.getHeader("User-Agent")).andAnswer(eval("tests"));
        response.addCookie(anyObject(Cookie.class));
        expect(request.getSession()).andAnswer(eval(session));
        expect(session.isNew()).andAnswer(eval(false)); // TODO - if just logged by cookie, session could still be new
        expect(session.getAttribute("_user_")).andAnswer(evalCapture(user));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getCharacterEncoding()).andAnswer(eval("utf-8"));

        filterChain.doFilter(request, response);

        replayAll();

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity configure now, just to ease tests
        RememberMeFormAuthFilter filter = createFilter();
        filter.init(filterConfig);
        filter.getModel();
        filter.doFilter(request, response, filterChain);

        verifyAll();
    }

}
