package com.republicate.modality.webapp;

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

import org.easymock.EasyMockRule;
import org.easymock.IAnswer;
import org.easymock.Mock;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.easymock.EasyMock.replay;

public class BaseWebappMockTest
{
    protected static Logger logger = LoggerFactory.getLogger("webapp-mock");
    /**
     * Unique point of passage for non-void calls
     * @param value value to return
     * @param <T> type of returned value
     * @return value
     */
    protected static <T> IAnswer<T> eval(final T value)
    {
        return new IAnswer<T>()
        {
            public T answer() throws Throwable
            {
                String caller = null;
                String mocked = null;
                String test = null;
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                for (StackTraceElement line : stackTrace)
                {
                    String at = line.toString();
                    if (mocked == null)
                    {
                        if (at.startsWith("com.sun.proxy"))
                        {
                            int dot = at.lastIndexOf('.');
                            dot = at.lastIndexOf('.', dot - 1);
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
                // good place for a breakpoint
                logger.trace("XXX [{}] mocked {} called from {}, returning {}", test, mocked, caller, value);
                // other very useful one: exception breakpoint at org.easymock.internal.AssertionErrorWrapper
                return value;
            }
        };
    }

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Mock
    protected FilterConfig filterConfig;

    @Mock
    protected ServletContext servletContext;

    @Mock
    protected HttpServletRequest request;

    @Mock
    protected HttpServletResponse response;

    @Mock
    protected FilterChain filterChain;

    @Mock
    protected HttpSession httpSession;

    protected void replayAll()
    {
        replay(filterConfig, servletContext, request, response, filterChain, httpSession);
    }


}
