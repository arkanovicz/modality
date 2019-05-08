package com.republicate.modality.webapp.auth;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.republicate.modality.util.TypeUtils;
import com.republicate.modality.webapp.ModalityFilter;

import static org.junit.Assert.*;

public class HTTPBasicAuthTests extends BaseHTTPAuthTests
{
    public static class MyFilterConfig extends MyAbstractFilterConfig
    {
        public MyFilterConfig(ServletContext servletContext)
        {
            super(servletContext);
        }

        @Override
        protected String[][] getConfigValues()
        {
           return  new String[][] {
               { ModalityFilter.MODEL_ID, "model" },
               { AbstractAuthFilter.REALM, "TESTS" },
               { AbstractAuthFilter.PROTECTED_RESOURCES, ".*" },
               { HTTPBasicAuthFilter.USER_BY_CRED_ATTRIBUTE, "user_by_credentials" },
           };
        }
    }

    public static class MyFilter extends HTTPBasicAuthFilter
    {
        public MyFilter()
        {
        }

        @Override
        public void init(FilterConfig filterConfig) throws ServletException
        {
            MyFilterConfig altConfig = new MyFilterConfig(filterConfig.getServletContext());
            super.init(altConfig);
        }
    }

    @Override
    protected Class getFilterClass()
    {
        return MyFilter.class;
    }

    public void testWWWAuthenticate() throws Exception
    {
        String request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n\r\n";
        String response = tester.getResponses(request);
        String expectedWWWAuthenticateHeader = "WWW-Authenticate: Basic realm=\"TESTS\", charset=\"UTF-8\"";
        assertTrue(response.contains(expectedWWWAuthenticateHeader));
    }

    public void testBadAuthenticate() throws Exception
    {
        String request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n" +
                "Authenticate: Basic sqdfsdqfsqdfsdqf\r\n\r\n";
        String response = tester.getResponses(request);
        String expectedWWWAuthenticateHeader = "WWW-Authenticate: Basic realm=\"TESTS\", charset=\"UTF-8\"";
        assertTrue(response.contains(expectedWWWAuthenticateHeader));
    }

    public void testGoodAuthenticate() throws Exception
    {
        String b64 = TypeUtils.base64Encode("nestor:secret");
        String request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n" +
                "Authenticate: Basic " + b64 + "\r\n\r\n";
        String response = tester.getResponses(request);
        assertTrue(response.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(response.contains(GOOD_CONTENT));
    }
}
