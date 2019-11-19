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

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Filter that implements the *correct* behavior for welcome files, aka REDIRECT 301.</p>
 * <p>Neither Apache or J2EE welcome-file is apropriate, since they resort on forwarding.</p>
 * <p><b>If</b> you rely on nginx or apache to serve static files in front of your J2EE container
 * using the <code>ajp</code> protocol on port 8009, then your configuration file should look like (for apache):</p>
 * <pre><code>
 *     ProxyPassMatch ^/(.*\.vhtml|.*\.do|)$ ajp://localhost:8009/$1
 *
 *     ProxyPassReverse / ajp://localhost:8009/
 *
 *     &lt;Proxy *&gt;
 *         AllowOverride All
 *         Order deny,allow
 *         Allow from all
 *     &lt;/Proxy&gt;
 * </code></pre>
 * <p>Then, for the J2EE container to pick your filter, you must add a filter-mapping towards <code>/*</code>,
 * not <code>/</code>, it would otherwise replace the default content container servlet.</p>
 */

public class IndexFilter implements Filter
{

    // TO TODO make it configurable
    public static final String INDEX = "index.vhtml";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String uri = request.getRequestURI();
        if (uri.endsWith("/"))
        {
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY); // 301
            response.setHeader("Location", uri + INDEX);
            // why ?
            // response.setHeader("Connection", "close");
        }
        else
        {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void destroy()
    {
    }

    // init() and destroy() can be NOOP.
}
