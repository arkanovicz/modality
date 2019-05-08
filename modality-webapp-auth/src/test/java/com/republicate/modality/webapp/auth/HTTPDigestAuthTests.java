package com.republicate.modality.webapp.auth;

import com.republicate.modality.webapp.ModalityFilter;
import com.republicate.modality.webapp.util.Digester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import static org.junit.Assert.*;

public class HTTPDigestAuthTests extends BaseHTTPAuthTests
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
                { HTTPDigestAuthFilter.USER_BY_LOGIN, "user_by_login" },
                { HTTPDigestAuthFilter.DIGEST_BY_LOGIN, "digest_by_login" },
            };
        }
    }

    public static class MyFilter extends HTTPDigestAuthFilter
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
        return HTTPDigestAuthTests.MyFilter.class;
    }

    static Pattern checkResp = Pattern.compile("WWW-Authenticate: Digest realm=\"TESTS\", qop=\"auth,auth-int\", nonce=\"(\\w{32})\", domain=\"/\", uri=\"/\", algorithm=MD5-sess");

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    @Test
    public void testWWWAuthenticate() throws Exception
    {
        String request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n\r\n";
        String response = tester.getResponses(request);
        Matcher matcher = checkResp.matcher(response);
        assertTrue(matcher.find());
    }

    @Test
    public void testGoodAuthenticate() throws Exception
    {
        String request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n\r\n";
        String response = tester.getResponses(request);
        Matcher matcher = checkResp.matcher(response);
        assertTrue(matcher.find());
        String nonce = matcher.group(1);

        String ha1 = Digester.toHexMD5String("nestor:TESTS:secret");
        String ha2 = Digester.toHexMD5String("GET:/");
        String resp = Digester.toHexMD5String(ha1 + ':' + nonce + ':' + ha2);

        request =
            "GET / HTTP/1.1\r\n" +
            "Host: test\r\n" +
            "Authenticate: Digest realm=\"TESTS\", uri=\"/\", nonce=\"" + nonce + "\", username=\"nestor\", response=\"" + resp + "\"\r\n" +
            "\r\n";
        response = tester.getResponses(request);
        assertTrue(response.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(response.contains(GOOD_CONTENT));
    }

    @Test
    public void testGoodAuthenticate_MD5() throws Exception
    {
        String request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n\r\n";
        String response = tester.getResponses(request);
        Matcher matcher = checkResp.matcher(response);
        assertTrue(matcher.find());
        String nonce = matcher.group(1);

        String ha1 = Digester.toHexMD5String("nestor:TESTS:secret");
        String ha2 = Digester.toHexMD5String("GET:/");
        String resp = Digester.toHexMD5String(ha1 + ':' + nonce + ':' + ha2);

        request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n" +
                "Authenticate: Digest realm=\"TESTS\", uri=\"/\", nonce=\"" + nonce + "\", username=\"nestor\", response=\"" + resp + "\", algorithm=MD5\r\n" +
                "\r\n";
        response = tester.getResponses(request);
        assertTrue(response.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(response.contains(GOOD_CONTENT));
    }

    @Test
    public void testGoodAuthenticate_MD5sess() throws Exception
    {
        String request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n\r\n";
        String response = tester.getResponses(request);
        Matcher matcher = checkResp.matcher(response);
        assertTrue(matcher.find());
        String nonce = matcher.group(1);
        String cnonce = "sdkfjrutieoprozksiruebdfcvtezekd";

        String ha1 = Digester.toHexMD5String(Digester.toHexMD5String("nestor:TESTS:secret") + ':' + nonce + ':' + cnonce);
        String ha2 = Digester.toHexMD5String("GET:/");
        String resp = Digester.toHexMD5String(ha1 + ':' + nonce + ':' + ha2);

        request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n" +
                "Authenticate: Digest realm=\"TESTS\", uri=\"/\", nonce=\"" + nonce + "\", username=\"nestor\", response=\"" + resp + "\", algorithm=MD5-sess, cnonce=\"" + cnonce + "\"\r\n" +
                "\r\n";
        response = tester.getResponses(request);
        assertTrue(response.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(response.contains(GOOD_CONTENT));
    }

    @Test
    public void testGoodAuthenticate_auth() throws Exception
    {
        String request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n\r\n";
        String response = tester.getResponses(request);
        Matcher matcher = checkResp.matcher(response);
        assertTrue(matcher.find());
        String nonce = matcher.group(1);
        String cnonce = "sdkfjrutieoprozksiruebdfcvtezekd";

        String ha1 = Digester.toHexMD5String(Digester.toHexMD5String("nestor:TESTS:secret") + ':' + nonce + ':' + cnonce);
        String ha2 = Digester.toHexMD5String("GET:/");
        String resp = Digester.toHexMD5String(ha1 + ':' + nonce + ":00000001:" + cnonce + ":auth:" + ha2);

        request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n" +
                "Authenticate: Digest realm=\"TESTS\", uri=\"/\", nonce=\"" + nonce + "\", username=\"nestor\", response=\"" + resp + "\", algorithm=MD5-sess, cnonce=\"" + cnonce + "\", nc=00000001, qop=auth\r\n" +
                "\r\n";
        response = tester.getResponses(request);
        assertTrue(response.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(response.contains(GOOD_CONTENT));
    }

    /*
    Broken, server does not see GET request content ?!
    @Test
    public void testGoodAuthenticate_authInt() throws Exception
    {
        String request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n\r\n";
        String response = tester.getResponses(request);
        Matcher matcher = checkResp.matcher(response);
        assertTrue(matcher.find());
        String nonce = matcher.group(1);
        String cnonce = "sdkfjrutieoprozksiruebdfcvtezekd";

        String ha1 = Digester.toHexMD5String(Digester.toHexMD5String("nestor:TESTS:secret") + ':' + nonce + ':' + cnonce);
        String ha2 = Digester.toHexMD5String("GET:/:" + Digester.toHexMD5String("some body text"));
        String resp = Digester.toHexMD5String(ha1 + ':' + nonce + ":00000001:" + cnonce + ":auth:" + ha2);

        request =
            "GET / HTTP/1.1\r\n" +
                "Host: test\r\n" +
                "Authenticate: Digest realm=\"TESTS\", uri=\"/\", nonce=\"" + nonce + "\", username=\"nestor\", response=\"" + resp + "\", algorithm=MD5-sess, cnonce=\"" + cnonce + "\", nc=00000001, qop=auth-int\r\n" +
                "\r\n" +
                "some body text\r\n" +
                "\r\n";
        response = tester.getResponses(request);
        assertTrue(response.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(response.contains(GOOD_CONTENT));
    }
     */

}
