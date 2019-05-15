package com.republicate.modality.api.server;

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

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.view.JeeConfig;
import org.apache.velocity.tools.view.JeeServletConfig;
import org.apache.velocity.tools.view.ServletUtils;
import org.apache.velocity.tools.view.VelocityViewServlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Servlet which allows serving Json VTL templates.</p>
 * <p>When a filesystem path element starts with <code>_</code>, the API servlet matches this path element to the corresponding
 * requested URI path element and defines a request attribute named after the path element.</p>
 * <p>For instance:</p>
 * <ul>
 *     <li>Request: <code>GET /api/book/25/authors</code></li>
 *     <li>Template: <code>/api/book/_book_id/authors/GET.json</code></li>
 *     <li>Request attributes: <code>book_id = 25</code></li>
 * </ul>
 * <p>Configuration parameters:</p>
 * <ul>
 *     <li><code>api.server.content_type</code> - content type; defaults to <code>application/json;charset=UTF-8</code>.</li>
 *     <li><code>api.server.error_template</code> - path of error template ; no default. It should be set to a template
 *     which is not directly accessible, as <code>/WEB-INF/error.json</code>.</li>
 * </ul>
 */

public class ApiServlet extends VelocityViewServlet
{
    public static final String CONTENT_TYPE = "api.server.content_type";
    public static final String ERROR_TEMPLATE = "api.server.error_template";

    private static final String DEFAULT_CONTENT_TYPE = "application/json;charset=UTF-8";

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        JeeConfig servletConfig = new JeeServletConfig(config);
        contentType = Optional.ofNullable(servletConfig.findInitParameter(CONTENT_TYPE)).orElse(DEFAULT_CONTENT_TYPE);
        errorTemplate = servletConfig.findInitParameter(ERROR_TEMPLATE);
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        doRequest(request, response);
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        doRequest(request, response);
    }

    /**
     * <p>Init request. By default, all methods other than GET are protected by a global lock.</p>
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException
     */
    @Override
    protected void initRequest(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        super.initRequest(request, response);
        lockResources(request);
    }

    @Override
    protected void setContentType(HttpServletRequest request, HttpServletResponse response)
    {
        response.setContentType(contentType);
    }

    /**
     * Lock request resources.
     * @param request
     */
    protected void lockResources(HttpServletRequest request)
    {
        // Global lock.
        // Warning: recursive calls (aka json VTL templates initiating loopback API calls)
        // will generate deadlocks. To avoid those, override lockResources() and unlockResources()
        // to provide a finer locking mechanism.
        if (request.getMethod().equals("GET"))
        {
            lock.readLock().lock();
        }
        else
        {
            lock.writeLock().lock();
        }
    }

    @Override
    protected Template getTemplate(HttpServletRequest request, HttpServletResponse response)
    {
        String path = ServletUtils.getPath(request);
        String[] elements = path.split("/");
        File target = new File(getServletContext().getRealPath("/"));
        String resourcePath = "/";
        for (String element : elements)
        {
            if (element.length() == 0 || ".".equals(element))
            {
                continue;
            }
            File sub = new File(target, element);
            if (sub.exists())
            {
                if (!element.startsWith("_"))
                {
                    target = sub;
                    resourcePath += '/' + element;
                }
                else
                {
                    getLog().error("could not map '" + element + "' on " + target.getAbsolutePath());
                    target = null;
                    break;
                }
            }
            else
            {
                File[] placeholders = target.listFiles((dir, name) -> name.startsWith("_"));
                if (placeholders.length == 1)
                {
                    target = placeholders[0];
                    resourcePath += '/' + placeholders[0].getName();
                    String attribute = placeholders[0].getName().substring(1);
                    request.setAttribute(attribute, element);
                }
                else
                {
                    getLog().error("could not map '" + element + "' on " + target.getAbsolutePath());
                    target = null;
                    break;
                }
            }
        }
        if (target == null)
        {
            throw new ResourceNotFoundException("Could not map request path onto API: " + path);
        }
        resourcePath += "/" + request.getMethod() + ".json";
        return super.getTemplate(resourcePath);
    }

    /**
     * Unlock request resources.
     * @param request
     */
    protected void unlockResources(HttpServletRequest request)
    {
        if (request.getMethod().equals("GET"))
        {
            lock.readLock().unlock();
        }
        else
        {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected void error(HttpServletRequest request, HttpServletResponse response, Throwable e)
    {
        String path = ServletUtils.getPath(request);
        getLog().error("Error processing a template for path '{}'", path, e);
        if (response.isCommitted())
        {
            getLog().error("An error occured but the response headers have already been sent.");
            return;
        }
        try
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (errorTemplate != null)
            {
                Context errorContext = createContext(request, response);
                fillContext(errorContext, request);
                errorContext.put("path", path);
                while (e != null && e instanceof MethodInvocationException)
                {
                    // we want the real cause
                    e = e.getCause();
                }
                errorContext.put("error", e);

                // get the template
                Template template = handleRequest(request, response, errorContext);

                // merge the template and context into the response
                mergeTemplate(template, errorContext, response);
            }
        }
        catch (Exception e2)
        {
            // clearly something is quite wrong.
            // let's log the new exception then give up and
            // throw a runtime exception that wraps the first one
            String msg = "Exception while printing error screen";
            getLog().error(msg, e2);
            throw new RuntimeException(msg, e);
        }
    }


    protected void requestCleanup(HttpServletRequest request,
                                  HttpServletResponse response,
                                  Context context)
    {
        super.requestCleanup(request, response, context);
        unlockResources(request);
    }

    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private String contentType = null;
    private String errorTemplate = null;

}

