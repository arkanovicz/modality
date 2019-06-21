package com.republicate.modality.webapp.example;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class DatabaseInitializer implements ServletContextListener
{
    protected static Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        try
        {
            logger.info("populating database");
            initDatabase(sce.getServletContext());
            logger.info("database populated");
        }
        catch (Exception sqle)
        {
            logger.error("could not initialize database", sqle);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {

    }

    private void initDatabase(ServletContext servletContext) throws Exception
    {
        Model model = new Model().initialize(servletContext.getResource("/WEB-INF/model.xml"));
        model.perform("initBookshelf");
    }
}
