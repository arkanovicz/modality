package com.republicate.modality.webapp.auth;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.republicate.modality.util.TypeUtils;
import com.republicate.modality.webapp.ModalityFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        String expectedWWWAuthenticateHeader = "WWW-Authenticate: Basic realm=\"TESTS\", charset=\"UTF-8\"";
        assertTrue(response.contains(expectedWWWAuthenticateHeader));
    }

    @Test
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

    @Test
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
