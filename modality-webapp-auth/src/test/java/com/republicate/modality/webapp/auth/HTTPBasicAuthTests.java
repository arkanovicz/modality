package com.republicate.modality.webapp.auth;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.DispatcherType;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.republicate.modality.Model;
import com.republicate.modality.util.TypeUtils;
import com.republicate.modality.webapp.ModalityFilter;

import org.apache.commons.collections4.iterators.IteratorEnumeration;
import org.eclipse.jetty.servlet.ServletTester;

public class HTTPBasicAuthTests extends BaseBookshelfTests
{
    ServletTester tester;

    public static class MyFilterConfig implements FilterConfig
    {
        public MyFilterConfig(ServletContext servletContext)
        {
            this.servletContext = servletContext;
        }

        @Override
        public String getFilterName()
        {
            return HTTPBasicAuthFilter.class.getName();
        }

        @Override
        public ServletContext getServletContext()
        {
            return servletContext;
        }

        @Override
        public String getInitParameter(String name)
        {
            return map.get(name);
        }

        @Override
        public Enumeration<String> getInitParameterNames()
        {
            return new IteratorEnumeration<String>(map.keySet().iterator());
        }

        private ServletContext servletContext;
        private Map<String, String> map = Stream.of(new String[][] {
            { ModalityFilter.MODEL_ID, "model" },
            { AbstractAuthFilter.REALM, "TESTS" },
            { AbstractAuthFilter.PROTECTED_RESOURCES, ".*" },
            { HTTPBasicAuthFilter.USER_BY_CRED_ATTRIBUTE, "user_by_credentials" },
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
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

    private static String GOOD_CONTENT = "this is a protected text";

    public static class EndpointServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        {
            try
            {
                resp.getWriter().println(GOOD_CONTENT);
            }
            catch (IOException e)
            {
                try
                {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }
        }

    }

    /* ------------------------------------------------------------ */
    protected void setUp() throws Exception
    {
        super.setUp();
        BaseBookshelfTests.populateDataSource();
        Model model = new Model().setDataSource(initDataSource()).initialize("model", getResource("user_cred_auth.xml"));

        tester = new ServletTester();
        tester.setContextPath("/");
        EnumSet<DispatcherType> dispatch = EnumSet.of(DispatcherType.REQUEST);
        tester.addFilter(MyFilter.class, "/*", dispatch);
        tester.addServlet(EndpointServlet.class, "/*");
        tester.start();
    }

    /* ------------------------------------------------------------ */
    protected void tearDown() throws Exception
    {
        tester.stop();
        tester=null;
        super.tearDown();
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
