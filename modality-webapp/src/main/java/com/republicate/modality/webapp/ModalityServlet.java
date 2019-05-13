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

import com.republicate.modality.Model;
import org.apache.velocity.tools.view.JeeServletConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public abstract class ModalityServlet extends HttpServlet
{
    protected static Logger logger = LoggerFactory.getLogger("modality");

    public void init(ServletConfig servletConfig) throws ServletException
    {
        this.config = new WebappModelConfig((new JeeServletConfig(servletConfig)));
        config.initModalityConfig();
    }

    protected Model getModel() throws ServletException
    {
        requireModelInit();
        return model;
    }

    protected void requireModelInit() throws ServletException
    {
        if (model == null)
        {
            synchronized (this)
            {
                if (model == null)
                {
                    initModel();
                }
            }
        }
    }

    protected void initModel() throws ServletException
    {
        model = config.initModel();
    }

    protected final String findConfigParameter(String key)
    {
        return config.findConfigParameter(key);
    }

    protected final WebappModelConfig getConfig()
    {
        return config;
    }

    private WebappModelConfig config = null;
    private Model model = null;
}
