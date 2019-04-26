package com.republicate.motion.webapp.auth;

import com.republicate.motion.model.Instance;
import org.apache.velocity.tools.view.ServletUtils;
import org.apache.velocity.tools.view.VelocityView;
import org.easymock.Capture;

import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.capture;

public class BaseFormAuthFilterTests extends BaseWebBookshelfTests
{
    protected VelocityView velocityView = null;

    protected void recordVelocityConfig(boolean loadDefaults, String toolbox) throws Exception
    {
        expect(filterConfig.getInitParameter(ServletUtils.SHARED_CONFIG_PARAM)).andAnswer(eval("true"));
        expect(servletContext.getAttribute(ServletUtils.VELOCITY_VIEW_KEY)).andAnswer(eval(null));
        expect(filterConfig.getInitParameter(ServletUtils.ALT_VELOCITY_VIEW_KEY)).andAnswer(eval("com.republicate.motion.webapp.MotionView"));
        expect(filterConfig.getInitParameter(VelocityView.USER_OVERWRITE_KEY)).andAnswer(eval("true"));
        expect(servletContext.getInitParameter(VelocityView.PROPERTIES_KEY)).andAnswer(eval(null));
        expect(filterConfig.getInitParameter(VelocityView.PROPERTIES_KEY)).andAnswer(eval(null));
        expect(servletContext.getResourceAsStream("/WEB-INF/velocity.properties")).andAnswer(eval(null));
        expect(servletContext.getResourceAsStream("/velocimacros.vtl")).andAnswer(eval(null));
        expect(servletContext.getResourceAsStream("/VM_global_library.vm")).andAnswer(eval(null));
        expect(filterConfig.getInitParameter(VelocityView.LOAD_DEFAULTS_KEY)).andAnswer(eval(String.valueOf(loadDefaults)));
        expect(servletContext.getInitParameter(ServletUtils.CONFIGURATION_KEY)).andAnswer(eval(null));
        expect(filterConfig.getInitParameter(ServletUtils.CONFIGURATION_KEY)).andAnswer(eval(null));
        if (toolbox == null)
        {
            expect(servletContext.getResource("/WEB-INF/tools.xml")).andAnswer(eval(null));
        }
        else
        {
            URL tools = getResource(toolbox);
            expect(servletContext.getResource("/WEB-INF/tools.xml")).andAnswer(eval(tools));
        }
        expect(servletContext.getAttribute(ServletUtils.CONFIGURATION_KEY)).andAnswer(eval(null));
        expect(filterConfig.getInitParameter(VelocityView.CLEAN_CONFIGURATION_KEY)).andAnswer(eval("false"));
        servletContext.setAttribute(eq(ServletUtils.VELOCITY_VIEW_KEY), anyObject(VelocityView.class));
    }

    protected void recordFilterConfig()
    {
        recordFilterConfig(false, false, false);
    }

    protected void recordFilterConfig(boolean redirectTowardsLogin)
    {
        recordFilterConfig(redirectTowardsLogin, false, false);
    }

    protected void recordFilterConfig(boolean redirectTowardsLogin, boolean redirectGetRequests, boolean forwardPostRequests)
    {
        expect(filterConfig.getInitParameter(AbstractAuthFilter.MOTION_CONFIG_KEY)).andAnswer(eval(null));
        expect(filterConfig.getServletContext()).andAnswer(eval(servletContext));
        expect(servletContext.getInitParameter(AbstractAuthFilter.MOTION_CONFIG_KEY)).andAnswer(eval(null));
        expect(filterConfig.getServletContext()).andAnswer(eval(servletContext));
        expect(filterConfig.getInitParameter(AbstractAuthFilter.PROTECTED_RESOURCES)).andAnswer(eval(".*"));
        expect(filterConfig.getServletContext()).andAnswer(eval(servletContext)).anyTimes();
        expect(servletContext.getContextPath()).andReturn("/");
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.MAX_INACTIVE_INTERVAL)).andAnswer(eval("0"));
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.LOGGED_SESSION_KEY)).andAnswer(eval("_user_"));
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.DOLOGIN_URI)).andAnswer(eval("/login.do"));
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.DOLOGOUT_URI)).andAnswer(eval("/logout.do"));
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.REDIRECT_PARAMETER)).andAnswer(eval("redirect"));
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.REDIRECT_REFERRER)).andAnswer(eval("true"));
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.REDIRECT_SKIP_CHECKS)).andAnswer(eval("false"));
        expect(filterConfig.getInitParameter(AbstractSessionAuthFilter.INVALIDATE_ON_LOGOUT)).andAnswer(eval("true"));
        expect(servletContext.getContextPath()).andReturn("/");
        expect(filterConfig.getInitParameter(AbstractFormAuthFilter.LOGIN_FIELD)).andAnswer(eval("login"));
        expect(filterConfig.getInitParameter(AbstractFormAuthFilter.PASSWORD_FIELD)).andAnswer(eval("password"));
        expect(filterConfig.getInitParameter(AbstractFormAuthFilter.LOGIN_URI)).andAnswer(eval("/login.vhtml"));
        expect(filterConfig.getInitParameter(AbstractFormAuthFilter.HOME_URI)).andAnswer(eval("/index.vhtml"));
        expect(servletContext.getResourcePaths("/")).andAnswer(eval(new HashSet<String>(Arrays.asList("/index.vhtml"))));
        expect(filterConfig.getInitParameter(AbstractFormAuthFilter.USER_HOME_URI)).andAnswer(eval("/index.vhtml"));
        expect(filterConfig.getInitParameter(AbstractFormAuthFilter.REDIRECT_TOWARDS_LOGIN)).andAnswer(eval(String.valueOf(redirectTowardsLogin)));
        expect(filterConfig.getInitParameter(AbstractFormAuthFilter.REDIRECT_GET_ON_SUCCESS)).andAnswer(eval(String.valueOf(redirectGetRequests)));
        expect(filterConfig.getInitParameter(AbstractFormAuthFilter.FORWARD_POST_ON_SUCCESS)).andAnswer(eval(String.valueOf(forwardPostRequests)));
        expect(filterConfig.getInitParameter(FormAuthFilter.USER_BY_CRED_ATTRIBUTE)).andAnswer(eval("user_by_credentials"));
        expect(filterConfig.getInitParameter(FormAuthFilter.MODEL_ID)).andAnswer(eval(null));
        expect(servletContext.getInitParameter(FormAuthFilter.MODEL_ID)).andAnswer(eval(null));
        expect(filterConfig.getInitParameter(ServletUtils.SHARED_CONFIG_PARAM)).andAnswer(eval("true"));
        expect(servletContext.getAttribute(ServletUtils.VELOCITY_VIEW_KEY)).andAnswer(() -> velocityView);
        expect(filterConfig.getInitParameter(ServletUtils.SHARED_CONFIG_PARAM)).andAnswer(eval("true"));
        expect(servletContext.getAttribute(ServletUtils.VELOCITY_VIEW_KEY)).andAnswer(() -> velocityView);
    }

    protected Capture<Instance> recordSuccessfullLogin() throws Exception
    {
        expect(request.getRequestURI()).andAnswer(eval("/login.do"));
        expect(request.getSession(false)).andAnswer(eval(null));
        expect(request.getRequestURI()).andAnswer(eval("/login.do"));
        expect(request.getParameter("login")).andAnswer(eval("Nestor"));
        expect(request.getParameter("password")).andAnswer(eval("secret"));
        expect(request.getSession()).andAnswer(eval(session));
        expect(session.getAttribute("_user_")).andAnswer(eval(null));
        Capture<Instance> user = new Capture<>();
        session.setAttribute(eq("_user_"), capture(user));
        session.setMaxInactiveInterval(0);
        return user;
    }

    protected Capture<SavedRequest> recordGETRequestCapture(String uri) throws Exception
    {
        expect(request.getMethod()).andAnswer(eval("GET"));
        expect(request.getRequestURI()).andAnswer(eval(uri));
        expect(request.getQueryString()).andAnswer(eval(null));
        expect(request.getSession()).andAnswer(eval(session));
        Capture<SavedRequest> savedRequest = new Capture<SavedRequest>();
        session.setAttribute(eq("org.apache.velocity.tools.auth.form.saved_request"), capture(savedRequest));
        return savedRequest;
    }

    protected Capture<SavedRequest> recordPOSTRequestCapture(String uri) throws Exception
    {
        Enumeration<String> emptyEnumeration = new Enumeration<String>()
        {
            @Override
            public boolean hasMoreElements()
            {
                return false;
            }

            @Override
            public String nextElement()
            {
                return null;
            }
        };

        expect(request.getMethod()).andAnswer(eval("POST"));
        expect(request.getRequestURI()).andAnswer(eval(uri));
        expect(request.getQueryString()).andAnswer(eval(null));
        expect(request.getContextPath()).andAnswer(eval(""));
        expect(request.getServletPath()).andAnswer(eval(uri));
        expect(request.getPathInfo()).andAnswer(eval(null));
        expect(request.getPathTranslated()).andAnswer(eval(null));
        expect(request.getRequestURL()).andAnswer(eval(new StringBuffer("http://model-tests" + uri)));
        expect(request.getAttributeNames()).andAnswer(eval(emptyEnumeration));
        expect(request.getHeaderNames()).andAnswer(eval(emptyEnumeration));
        expect(request.getInputStream()).andAnswer(eval(null));
        expect(request.getContentType()).andAnswer(eval("whatever/whatever"));
        expect(request.getSession()).andAnswer(eval(session));
        Capture<SavedRequest> savedRequest = new Capture<SavedRequest>();
        session.setAttribute(eq("org.apache.velocity.tools.auth.form.saved_request"), capture(savedRequest));
        return savedRequest;
    }

}
