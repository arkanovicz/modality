package com.republicate.modality.webapp.auth;

import com.republicate.modality.webapp.BaseWebappMockTest;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.easymock.EasyMock.expect;

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

    protected void recordConfig()
    {
        expect(filterConfig.getServletContext()).andAnswer(eval(servletContext)).anyTimes();
        expect(filterConfig.getInitParameter(AbstractAuthFilter.MODALITY_CONFIG_KEY)).andAnswer(eval(null));
        expect(servletContext.getInitParameter(AbstractAuthFilter.MODALITY_CONFIG_KEY)).andAnswer(eval(null));
        expect(servletContext.getResourceAsStream("/WEB-INF/modality.properties")).andAnswer(eval(null));
        expect(filterConfig.getInitParameter(AbstractAuthFilter.PROTECTED_RESOURCES)).andAnswer(eval(".*"));
    }

    @Test
    public void getAuthentifiedUser() throws Exception
    {
        recordConfig();
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
        recordConfig();
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
        recordConfig();
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
