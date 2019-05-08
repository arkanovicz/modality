package com.republicate.modality.webapp.auth;

import com.republicate.modality.Model;
import com.republicate.modality.util.TypeUtils;
import com.republicate.modality.webapp.ModalityFilter;
import org.apache.commons.collections4.iterators.IteratorEnumeration;
import org.eclipse.jetty.servlet.ServletTester;

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

public abstract class BaseHTTPAuthTests extends BaseBookshelfTests
{
    protected ServletTester tester;

    protected static String GOOD_CONTENT = "this is a protected text";

    /* ------------------------------------------------------------ */

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        BaseBookshelfTests.populateDataSource();
        Model model = new Model().setDataSource(initDataSource()).initialize("model", getResource("user_cred_auth.xml"));

        tester = new ServletTester();
        tester.setContextPath("/");
        EnumSet<DispatcherType> dispatch = EnumSet.of(DispatcherType.REQUEST);
        tester.addFilter(getFilterClass(), "/*", dispatch);
        tester.addServlet(EndpointServlet.class, "/*");
        tester.start();
    }
    protected abstract Class getFilterClass();

    @Override
    protected void tearDown() throws Exception
    {
        tester.stop();
        tester=null;
        super.tearDown();
    }

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

    public static abstract class MyAbstractFilterConfig implements FilterConfig
    {
        public MyAbstractFilterConfig(ServletContext servletContext)
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

        protected abstract String[][] getConfigValues();

        private ServletContext servletContext;
        private Map<String, String> map = Stream.of(getConfigValues()).collect(Collectors.toMap(data -> data[0], data -> data[1]));
    }

}
