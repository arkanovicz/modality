package com.republicate.modality.webapp.auth;

import com.republicate.modality.model.Instance;
import com.republicate.modality.model.Model;
import org.apache.velocity.tools.view.ServletUtils;

import org.easymock.Capture;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;

import static org.easymock.EasyMock.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FormAuthFilterTests extends BaseFormAuthFilterTests
{
    @BeforeClass
    public static void populateDataSource() throws Exception
    {
        BaseBookshelfTests.populateDataSource();
    }

    @Test
    public void testModelFromRepositoryForbiddenAccess() throws Exception
    {
        recordVelocityConfig(false, null);
        recordFilterConfig();

        // GET /index.vhtml, expect 403
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getSession(false)).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        expect(request.getRequestURI()).andAnswer(eval("/index.vhtml"));
        response.sendError(403);

        replayAll();

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity init now, just to ease tests
        new Model().setDataSource(initDataSource()).initialize("model", getResource("user_cred_model.xml"));
        FormAuthFilter filter = new FormAuthFilter();
        filter.init(filterConfig);
        filter.doFilter(request, response, filterChain);
    }

    @Test
    public void testModelFromToolboxAllowedAccess() throws Exception
    {
        recordVelocityConfig(true, "user_cred_tools.xml");
        recordFilterConfig(false);
        expect(servletContext.getResource("/WEB-INF/user_cred_model.xml")).andAnswer(eval(getResource("user_cred_model.xml")));
        recordSuccessfullLogin();
        expect(request.getParameter("redirect")).andAnswer(eval(null));
        expect(request.getParameter("redirect")).andAnswer(eval(null));
        expect(request.getHeader("Referer")).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/login.do"));
        expect(request.getHeader("Referer")).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/login.do"));
        response.setStatus(200);

        replayAll();

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity init now, just to ease tests
        FormAuthFilter filter = new FormAuthFilter();
        filter.init(filterConfig);
        filter.doFilter(request, response, filterChain);
    }

    @Test
    public void testRedirectTowardsAndFromLoginUsingReferrer() throws Exception
    {
        recordVelocityConfig(true, "user_cred_tools.xml");
        recordFilterConfig(true);
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

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity init now, just to ease tests
        FormAuthFilter filter = new FormAuthFilter();
        filter.init(filterConfig);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);
    }

    @Test
    public void testRedirectTowardsAndFromLoginUsingSavedGETRequest() throws Exception
    {
        recordVelocityConfig(true, "user_cred_tools.xml");
        recordFilterConfig(true, true, true);
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

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity init now, just to ease tests
        FormAuthFilter filter = new FormAuthFilter();
        filter.init(filterConfig);
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

        velocityView = ServletUtils.getVelocityView(filterConfig); // force Velocity init now, just to ease tests
        FormAuthFilter filter = new FormAuthFilter();
        filter.init(filterConfig);
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        verifyAll();
    }

}
