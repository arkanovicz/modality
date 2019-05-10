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

import org.apache.velocity.tools.config.ConfigurationUtils;
import org.apache.velocity.tools.config.FactoryConfiguration;
import org.apache.velocity.tools.config.FileFactoryConfiguration;
import org.apache.velocity.tools.view.JeeConfig;
import org.apache.velocity.tools.view.VelocityView;

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class ModalityView extends VelocityView
{
    public static final String MODALITY_TOOLS_DEFAULTS_PATH =
        "/com/republicate/modality/webapp/tools.xml";

    public ModalityView(ServletConfig config)
    {
        super(config);
    }

    public ModalityView(FilterConfig config)
    {
        super(config);
    }

    public ModalityView(ServletContext context)
    {
        super(context);
    }

    public ModalityView(JeeConfig config)
    {
        super(config);
    }

    @Override
    protected FactoryConfiguration getDefaultToolsConfiguration()
    {
        FileFactoryConfiguration defaultTools = (FileFactoryConfiguration)ConfigurationUtils.getDefaultTools();
        defaultTools.read(MODALITY_TOOLS_DEFAULTS_PATH, false);
        return defaultTools;
    }
}
