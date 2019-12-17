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

import org.apache.velocity.tools.view.JeeConfig;
import org.apache.velocity.tools.view.ServletUtils;
import org.apache.velocity.util.ExtProperties;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class WebappModalityConfig
{
    public static final String MODALITY_USER_CONFIG_KEY = "modality.config";
    public static final String MODALITY_DEFAULT_CONFIG = "com/republicate/modality/modality.properties";
    public static final String MODALITY_DEFAULT_USER_CONFIG = "/WEB-INF/modality.properties";

    public WebappModalityConfig(JeeConfig config)
    {
        this.webConfig = config;
    }

    public ServletContext getServletContext()
    {
        return getWebConfig().getServletContext();
    }

    public String findConfigParameter(String key)
    {
        String ret = webConfig.findInitParameter(key);
        if (ret == null)
        {
            ret = modalityConfig.getString(key);
        }
        return ret;
    }

    //
    // Modality Initialization
    //  - default modality.properties file
    // - user provided mododality.properties file
    //

    public void configure() throws ServletException
    {
        loadDefaultConfig();
        loadUserConfig();
    }

    public JeeConfig getWebConfig()
    {
        return webConfig;
    }


    private void loadDefaultConfig() throws ServletException
    {
        modalityConfig = new ExtProperties();
        InputStream is = ServletUtils.getInputStream(MODALITY_DEFAULT_CONFIG, webConfig.getServletContext());
        if (is == null)
        {
            throw new ServletException("could not find default modality configuration file: " + MODALITY_DEFAULT_CONFIG);
        }
        try
        {
            modalityConfig.load(is);
        } catch (IOException ioe)
        {
            throw new ServletException("could configure default modality configuration file", ioe);
        }
    }

    private void loadUserConfig() throws ServletException
    {
        String modalityConfigPath = getWebConfig().findInitParameter(MODALITY_USER_CONFIG_KEY);
        boolean mandatory = modalityConfigPath != null;
        if (modalityConfigPath == null)
        {
            modalityConfigPath = MODALITY_DEFAULT_USER_CONFIG;
        }
        if (modalityConfigPath != null)
        {
            InputStream is = ServletUtils.getInputStream(modalityConfigPath, webConfig.getServletContext());
            if (is == null)
            {
                if (mandatory)
                {
                    throw new ServletException("could not find modality configuration file: " + modalityConfigPath);
                }
            }
            else
            {
                ExtProperties userProps = new ExtProperties();
                try
                {
                    userProps.load(is);
                }
                catch (IOException ioe)
                {
                    throw new ServletException("could not read modality configuration file: " + modalityConfigPath, ioe);
                }
                modalityConfig.putAll(userProps);
            }
        }
    }


    private JeeConfig webConfig = null;
    private ExtProperties modalityConfig = null;
}
