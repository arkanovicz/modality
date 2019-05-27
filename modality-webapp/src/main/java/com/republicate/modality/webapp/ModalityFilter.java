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

import org.apache.velocity.tools.view.JeeFilterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * <p>Base J2EE filter providing:</p>
 * <ul>
 *     <li><code>void modelInitialized(Model)</code></li>
 *     <li><code>Model getModel()</code></li>
 *     <li><code>String findConfigParameter(String key)</code></li>
 * </ul>
 */

public abstract class ModalityFilter implements Filter, WebappModelAccessor
{
    protected static Logger logger = LoggerFactory.getLogger("modality");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        this.modelProvider = new WebappModelProvider(new JeeFilterConfig(filterConfig));
        configureModel();
    }

    @Override
    public final WebappModelProvider getModelProvider()
    {
        return modelProvider;
    }

    public ServletContext getServletContext()
    {
        return modelProvider.getServletContext();
    }

    private WebappModelProvider modelProvider = null;
}
