package com.republicate.modality.webapp.auth;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.easymock.EasyMock.expect;

public class AbstractSessionAuthFilterTest extends AbstractAuthFilterTest
{
    protected class AuthSessionFilter extends AbstractSessionAuthFilter<String>
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
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.MAX_INACTIVE_INTERVAL)).andReturn("0");
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.LOGGED_SESSION_KEY)).andReturn("_user_");
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.DOLOGIN_URI)).andReturn("/login.do");
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.DOLOGOUT_URI)).andReturn("/logout.do");
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.REDIRECT_PARAMETER)).andReturn("redirect");
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.REDIRECT_REFERRER)).andReturn("true");
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.REDIRECT_SKIP_CHECKS)).andReturn("false");
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.INVALIDATE_ON_LOGOUT)).andReturn("true");
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
