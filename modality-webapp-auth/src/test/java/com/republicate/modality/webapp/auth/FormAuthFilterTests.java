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
import com.republicate.modality.webapp.auth.helpers.ForwardedRequest;
import com.republicate.modality.webapp.auth.helpers.SavedRequest;
import org.apache.velocity.tools.view.ServletUtils;
import org.easymock.Capture;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;

import javax.servlet.ServletException;

import static org.easymock.EasyMock.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FormAuthFilterTests extends BaseFormAuthFilterTests
{
    @BeforeClass
    public static void populateDataSource() throws Exception
    {
        BaseBookshelfTests.populateDataSource();
    }

    private FormAuthFilter createFilter()
    {
        return new FormAuthFilter()
        {
            @Override
            public void modelInitialized(Model model) throws ServletException
            {
                super.modelInitialized(model);
            }
        };
    }

    @Test
    public void testModelFromRepositoryForbiddenAccess() throws Exception
    {
        recordVelocityConfig(false, null);
        recordFilterConfig();
        recordFilterRequireInit();

        // GET /index.vhtml, expect 403
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        response.sendError(403);

        replayAll();

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity configure now, just to ease tests
        new Model().setDataSource(initDataSource()).initialize(getResource("user_cred_model.xml"));
        FormAuthFilter filter = createFilter();
        filter.init(filterConfig);
        filter.getModel();
        filter.doFilter(request, response, filterChain);
    }
/*
    @Test
    public void testModelFromToolboxAllowedAccess() throws Exception
    {
        recordVelocityConfig(true, "user_cred_tools.xml");
        recordFilterConfig(false);
        recordFilterRequireInit();

        expect(servletContext.getResource("/WEB-INF/user_cred_model.xml")).andAnswer(eval(getResource("user_cred_model.xml")));
        recordSuccessfullLogin();
        expect(request.getParameter("redirect")).andAnswer(eval(null));
        expect(request.getSession(false)).andAnswer(eval(session));
        session.removeAttribute("org.apache.velocity.tools.auth.form.saved_request");
        expect(request.getParameter("redirect")).andAnswer(eval(null));
        expect(request.getHeader("Referer")).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/login.do"));
        expect(request.getHeader("Referer")).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/login.do"));
        response.sendRedirect("/index.vhtml");

        replayAll();

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity configure now, just to ease tests
        FormAuthFilter filter = createFilter();
        filter.configure(filterConfig);
        filter.initModel();
        filter.doFilter(request, response, filterChain);
    }

    @Test
    public void testRedirectTowardsAndFromLoginUsingReferrer() throws Exception
    {
        recordVelocityConfig(true, "user_cred_tools.xml");
        recordFilterConfig(true);
        recordFilterRequireInit();

        expect(servletContext.getResource("/WEB-INF/user_cred_model.xml")).andAnswer(eval(getResource("user_cred_model.xml")));

        // GET /index.vhtml, expect 302 towards /login.vhtml
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getMethod()).andAnswer(eval("GET"));
        response.sendRedirect("/login.vhtml");

        // GET /login.vhtml
        expect(request.getRequestURI()).andAnswer(eval("/login.vhtml"));
        expect(request.getRequestURI()).andAnswer(eval("/login.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(null));
        filterChain.doFilter(request, response);

        // POST /login.do
        Capture<Instance> user = recordSuccessfullLogin();
        expect(request.getParameter("redirect")).andAnswer(eval(null));
        expect(request.getSession(false)).andAnswer(eval(session));
        session.removeAttribute("org.apache.velocity.tools.auth.form.saved_request");
        expect(request.getParameter("redirect")).andAnswer(eval(null));
        // we're cheating, here, because the real referrer is login.vhtml
        // but we're just testing referrer redirection
        expect(request.getHeader("Referer")).andAnswer(eval("/index.vhtml"));
        expect(request.getRequestURI()).andAnswer(eval("/login.do"));
        response.sendRedirect("/index.vhtml");

        // GET /index.vhtml, expect 200
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(session));
        expect(session.getAttribute("_user_")).andAnswer(evalCapture(user));
        response.setStatus(200);

        replayAll();

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity configure now, just to ease tests
        FormAuthFilter filter = createFilter();
        filter.configure(filterConfig);
        filter.initModel();
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
    }

    @Test
    public void testRedirectTowardsAndFromLoginUsingSavedGETRequest() throws Exception
    {
        recordVelocityConfig(true, "user_cred_tools.xml");
        recordFilterConfig(true, true, true);

        recordFilterRequireInit();
        expect(servletContext.getResource("/WEB-INF/user_cred_model.xml")).andAnswer(eval(getResource("user_cred_model.xml")));

        // GET /index.vhtml : save request and redirect to /login.vhtml
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getMethod()).andAnswer(eval("GET"));
        Capture<SavedRequest> savedRequest = recordGETRequestCapture("/index.vhtml");
        response.sendRedirect("/login.vhtml");

        // GET /login.vhtml : let go
        expect(request.getRequestURI()).andAnswer(eval("/login.vhtml"));
        expect(request.getRequestURI()).andAnswer(eval("/login.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(null));
        filterChain.doFilter(request, response);

        // POST /login.do : authenticate and redirect to /index.vhtml
        Capture<Instance> user = recordSuccessfullLogin();
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
        filterChain.doFilter(request, response);

        replayAll();

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity configure now, just to ease tests
        FormAuthFilter filter = createFilter();
        filter.configure(filterConfig);
        filter.initModel();
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
    }

    @Test
    public void testRedirectTowardsLoginThenForwardUsingSavedPOSTRequest() throws Exception
    {
        recordVelocityConfig(true, "user_cred_tools.xml");
        recordFilterConfig(true, true, true);
        recordFilterRequireInit();

        expect(servletContext.getResource("/WEB-INF/user_cred_model.xml")).andAnswer(eval(getResource("user_cred_model.xml")));

        // POST /index.do : save request and redirect to /login.vhtml
        expect(request.getRequestURI()).andAnswer(eval("/index.do"));
        expect(request.getSession(false)).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/index.do"));
        expect(request.getRequestURI()).andAnswer(eval("/index.do"));
        expect(request.getMethod()).andAnswer(eval("POST"));
        Capture<SavedRequest> savedRequest = recordPOSTRequestCapture("/index.do");
        response.sendRedirect("/login.vhtml");

        // POST /login.do : authenticate then forward to /index.do
        recordSuccessfullLogin();
        expect(request.getParameter("redirect")).andAnswer(eval(null));
        expect(request.getSession()).andAnswer(eval(session));
        expect(session.getAttribute(eq("org.apache.velocity.tools.auth.form.saved_request"))).andAnswer(evalCapture(savedRequest));
        session.removeAttribute("org.apache.velocity.tools.auth.form.saved_request");
        expect(response.getHeaders("Set-Cookie")).andAnswer(eval(new ArrayList<String>()));
        expect(request.getRequestDispatcher("/index.do")).andAnswer(eval(requestDispatcher));
        requestDispatcher.forward(anyObject(ForwardedRequest.class), eq(response));

        replayAll();

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity configure now, just to ease tests
        FormAuthFilter filter = createFilter();
        filter.configure(filterConfig);
        filter.initModel();
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        verifyAll();
    }
*/
}
