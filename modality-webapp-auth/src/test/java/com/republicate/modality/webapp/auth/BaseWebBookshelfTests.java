package com.republicate.modality.webapp.auth;

import org.easymock.Capture;
import org.easymock.EasyMockRule;
import org.easymock.IAnswer;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class BaseWebBookshelfTests extends BaseBookshelfTests
{
    protected static Logger logger = LoggerFactory.getLogger("webapp-mock");

    protected static int logline = 0;

    protected static void logMockCall(Object value)
    {
        String caller = null;
        String mocked = null;
        String test = null;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement line : stackTrace)
        {
            String at = line.toString();
            if (at.contains("logMockCall"))
            {
                continue;
            }
            if (mocked == null)
            {
                if (at.startsWith("com.sun.proxy"))
                {
                    int dot = at.lastIndexOf('.');
                    int par = at.indexOf('(', dot + 1);
                    mocked = at.substring(dot + 1, par) + "()";
                }
            }
            if (at.startsWith("com.republicate"))
            {
                if (at.contains("Test"))
                {
                    if (test == null && !at.contains(".answer(") && !(at.contains("$")))
                    {
                        test = at.replaceAll("\\b[a-z]+\\.|\\(.*\\)", "");
                    }
                }
                else if (caller == null)
                {
                    caller = at;
                }
            }
        }
        logger.trace("[{}-{}] mocked {} called from {}, returning {}", test, String.format("%03d", ++logline), mocked, caller, value);
        // good place for a conditional breakpoint
        int foo = 0;
        // note: good other breakpoint: throw AssertionErrorWrapper
    }

    /**
     * Unique point of passage for non-void calls
     * @param value value to return
     * @param <T> type of returned value
     * @return value
     */
    protected static <T> IAnswer<T> eval(final T value)
    {
        return () ->
        {
            logMockCall(value);
            return value;
        };
    }

    protected static <T> IAnswer<T> evalCapture(final Capture<T> value)
    {
        return () ->
        {
            logMockCall(value.getValue());
            return value.getValue();
        };
    }

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Mock
    protected FilterConfig filterConfig;

    @Mock(MockType.STRICT)
    protected ServletContext servletContext;

    @Mock(MockType.STRICT)
    protected HttpServletRequest request;

    @Mock(MockType.STRICT)
    protected HttpServletResponse response;

    @Mock(MockType.STRICT)
    protected FilterChain filterChain;

    @Mock(MockType.STRICT)
    protected HttpSession session;

    @Mock(MockType.STRICT)
    protected RequestDispatcher requestDispatcher;

    protected void replayAll()
    {
        replay(filterConfig, servletContext, request, response, filterChain, session);
    }

    protected void verifyAll()
    {
        verify(filterConfig, servletContext, request, response, filterChain, session);
    }
}
