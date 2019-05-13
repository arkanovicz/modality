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

import com.republicate.modality.webapp.BaseWebappMockTest;
import com.republicate.modality.webapp.ModalityFilter;
import com.republicate.modality.webapp.WebappModelConfig;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.easymock.EasyMock.expect;

public class BaseAuthFilterTest extends BaseWebappMockTest
{
    protected class AuthFilter extends BaseAuthFilter<String>
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
        expect(filterConfig.getInitParameter(WebappModelConfig.MODALITY_CONFIG_KEY)).andAnswer(eval(null));
        expect(servletContext.getInitParameter(WebappModelConfig.MODALITY_CONFIG_KEY)).andAnswer(eval(null));
        expect(servletContext.getResourceAsStream("/WEB-INF/modality.properties")).andAnswer(eval(null));
        expect(filterConfig.getInitParameter(WebappModelConfig.MODEL_ID)).andAnswer(eval(null));
        expect(servletContext.getInitParameter(WebappModelConfig.MODEL_ID)).andAnswer(eval(null));
        expect(filterConfig.getInitParameter(BaseAuthFilter.REALM)).andAnswer(eval("TESTS"));
        expect(filterConfig.getInitParameter(BaseAuthFilter.PROTECTED_RESOURCES)).andAnswer(eval(".*"));
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
