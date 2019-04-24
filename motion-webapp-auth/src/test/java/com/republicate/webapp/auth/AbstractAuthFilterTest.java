package com.republicate.webapp.auth;

import static org.easymock.EasyMock.*;

import com.republicate.webapp.BaseWebappMockTest;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;

public class AbstractAuthFilterTest extends BaseWebappMockTest
{
    protected class AuthFilter extends AbstractAuthFilter<String>
    {
        @Override
        protected String authenticate(HttpServletRequest request)
        {
            return request.getParameter("Good-Login");
        }
    }

    private AuthFilter authFilter;

    @Test
    public void getAuthentifiedUser() throws Exception
    {
        expect(filterConfig.getInitParameter(AbstractAuthFilter.PROTECTED_RESOURCES)).andAnswer(eval(".*"));
        expect(request.getRequestURI()).andAnswer(eval("/some-uri"));
        expect(request.getParameter("Good-Login")).andAnswer(eval("Nestor"));
        replayAll();
        authFilter = new AuthFilter();
        authFilter.init(filterConfig);
        authFilter.getAuthentifiedUser(request);
    }

    @Test
    public void processProtectedRequest() throws Exception
    {
        expect(filterConfig.getInitParameter(AbstractAuthFilter.PROTECTED_RESOURCES)).andAnswer(eval(".*"));
        expect(request.getRequestURI()).andAnswer(eval("/some-uri"));
        expect(request.getParameter("Good-Login")).andAnswer(eval("Nestor"));
        expect(request.getRequestURI()).andAnswer(eval("/some-uri"));
        filterChain.doFilter(request, response);
        replayAll();
        authFilter = new AuthFilter();
        authFilter.init(filterConfig);
        authFilter.doFilter(request, response, filterChain);
    }

    @Test
    public void processForbiddenRequest() throws Exception
    {
        expect(filterConfig.getInitParameter(AbstractAuthFilter.PROTECTED_RESOURCES)).andAnswer(eval(".*"));
        expect(request.getRequestURI()).andAnswer(eval("/some-uri"));
        expect(request.getParameter("Good-Login")).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/some-uri"));
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        replayAll();
        authFilter = new AuthFilter();
        authFilter.init(filterConfig);
        authFilter.doFilter(request, response, filterChain);
    }
}
