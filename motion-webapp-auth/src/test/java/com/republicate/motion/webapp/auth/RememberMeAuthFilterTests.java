package com.republicate.motion.webapp.auth;

import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Test;

public class RememberMeAuthFilterTests extends BaseFormAuthFilterTests
{

    @Mock(MockType.STRICT)
    protected RememberMeCookieHandler rememberMeCookieHandler;

    @Override
    protected void recordFilterConfig()
    {
        super.recordFilterConfig();
        /* TODO - how to pass an object?
        expect(filterConfig.getInitParameter(RememberMeFormAuthFilter.COOKIE_HANDLER)).andAnswer(eval(rememberMeCookieHandler));
         */

    }

    @Test
    public void testCookieCreation() throws Exception
    {
        /*
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
        */
    }
}
