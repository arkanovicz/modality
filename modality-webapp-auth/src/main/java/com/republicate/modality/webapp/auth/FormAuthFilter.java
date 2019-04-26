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

import com.republicate.modality.config.ConfigurationException;
import org.apache.velocity.tools.ToolContext;
import com.republicate.modality.Attribute;
import com.republicate.modality.Instance;
import com.republicate.modality.Model;
import com.republicate.modality.RowAttribute;
import com.republicate.modality.util.SlotHashMap;
import com.republicate.modality.util.SlotMap;
import org.apache.velocity.tools.view.ServletUtils;
import org.apache.velocity.tools.view.VelocityView;
import org.apache.velocity.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>Authentication filter relying on a query returning a user instance whenever his/her credentials
 * are correct, using the <code>&lt;login/&gt;</code> and <code>&lt;login/&gt;</code> placeholder parameters.
 * A typical query would be:</p>
 * <pre><code>&lt;row result="user"&gt;
 *   select * from user where login = &lt;login/&gt; and password = sha1(&lt;password/&gt;
 * &lt;/row&gt;</code></pre>
 * <p>Configuration parameters:</p>
 * <ul>
 *     <li>org.apache.velocity.tools.model.web.<b>model_id</b>&nbsp;model id ; if not provided, the filter
 *     will search for a ModelTool in the application toolbox, and then for the "default" or "model" id.</li>
 *     <li>org.apache.velocity.tools.model.web.<b>user_by_credentials</b>&nbsp;row attribute name to use ;
 *     defaults to <code>user_by_credentials</code>.</li>
 *     <li>org.apache.velocity.tools.model.web.<b>refresh_rate</b>&nbsp;user instance refresh rate in seconds;
 *     defaults to 0 (aka never)</li>
 * </ul>
 * <p>As usual, configuration parameters can be filter's init-params or global context-params.</p>
 */

public class FormAuthFilter extends AbstractFormAuthFilter<Instance>
{
    protected static Logger logger = LoggerFactory.getLogger("auth");

    public static final String MODEL_ID =               "auth.model.model_id";
    public static final String USER_BY_CRED_ATTRIBUTE = "auth.model.user_by_credentials";
    public static final String USER_REFRESH_RATE =      "auth.model.refresh_rate";

    private static final String DEFAULT_MODEL_AUTH_USER_BY_CREDENTIALS = "user_by_credentials";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
        userByCredentialsAttribute = Optional.ofNullable(findConfigParameter(USER_BY_CRED_ATTRIBUTE)).orElse(DEFAULT_MODEL_AUTH_USER_BY_CREDENTIALS);
        modelId = findConfigParameter(MODEL_ID);
    }

    @Override
    protected Instance checkCredentials(String login, String password)
    {
        try
        {
            SlotMap params = new SlotHashMap();
            params.put("login", login);
            params.put("password", password);
            return model.retrieve(userByCredentialsAttribute, params);
        }
        catch (SQLException sqle)
        {
            logger.error("could not check credentials", sqle);
            return null;
        }
    }

    @Override
    protected boolean preFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
    {
        requireModelInit();
        return true;
    }

    protected String displayUser(Instance user)
    {
        String login = user.getString(getLoginField());
        if (login == null && !"login".equals(getLoginField()))
        {
            login = user.getString("login");
        }
        return login != null ? login : String.valueOf(user);
    }

    private void requireModelInit()
    {
        if (model == null)
        {
            synchronized (this)
            {
                if (model == null)
                {
                    // if the model has been initialized via a model tool,
                    // make sure the model tool is ready
                    initModelFromApplicationToolbox();
                    if (model == null)
                    {
                        // No application toolbox, or nothing found within.
                        // Just ask the repository.
                        initModelFromRepository();

                        // Still not available? Try to initialize it ourselves.
                        if (model == null)
                        {
                            initNewModel();

                            // otherwise bail out
                            if (model == null)
                            {
                                throw new RuntimeException("ModelAuthFilter: no model found" + (modelId == null ? "" : " for model id " + modelId));
                            }
                        }
                    }

                    // now check the user_by_credentials attribute
                    Attribute attr = model.getAttribute(userByCredentialsAttribute);
                    if (attr == null)
                    {
                        throw new ConfigurationException("attribute does not exist: " + userByCredentialsAttribute);
                    }
                    if (!(attr instanceof RowAttribute))
                    {
                        throw new ConfigurationException("not a row attribute: " + userByCredentialsAttribute);
                    }
                }
            }
        }
    }

    private void initModelFromApplicationToolbox()
    {
        VelocityView view = ServletUtils.getVelocityView(getConfig());
        if (view.hasApplicationTools())
        {
            Set<String> modelKeys = new HashSet<>();
            Class modelToolClass = null;
            try
            {
                modelToolClass = ClassUtils.getClass("com.republicate.modality.tools.model.ModelTool");
            }
            catch (ClassNotFoundException cnfe) {}
            if (modelToolClass != null)
            {
                // search for a model in application tools
                for (Map.Entry<String, Class> entry : view.getApplicationToolbox().getToolClassMap().entrySet())
                {
                    if (modelToolClass.isAssignableFrom(entry.getValue()))
                    {
                        modelKeys.add(entry.getKey());
                    }
                }
            }
            String modelKey = null;

            if (modelId == null)
            {
                if (modelKeys.size() > 1)
                {
                    throw new RuntimeException("authentication filter cannot choose between several models, please specify " + MODEL_ID + " in configuration parameters");
                }
                else if (!modelKeys.isEmpty())
                {
                    modelId = modelKey = modelKeys.iterator().next();
                }
            }
            else
            {
                if (modelKeys.contains(modelId))
                {
                    modelKey = modelId;
                }
            }

            if (modelKey != null)
            {
                logger.info("Found model id '{}' in application toolbox", modelKey);

                // force model initialization
                view.createContext().get(modelKey);

                // get model
                model = Model.getModel(modelKey);
            }
        }
    }

    protected Model getModel()
    {
        return model;
    }

    private void initModelFromRepository()
    {
        String[] ids =
            modelId == null ?
                new String[] { "default", "model" } :
                new String[] { modelId };
        String foundModel = null;
        for (String id : ids)
        {
            try
            {
                model = Model.getModel(id);
                foundModel = id;
            }
            catch (ConfigurationException e) {}
            if (foundModel != null)
            {
                modelId = foundModel;
                logger.info("Found model id '{}' in model repository", foundModel);
            }
        }
    }

    private void initNewModel()
    {
        try
        {
            Map params = new HashMap();
            params.put(ToolContext.CONTEXT_KEY, getConfig().getServletContext());
            params.put(ToolContext.ENGINE_KEY, ServletUtils.getVelocityView(getConfig()).getVelocityEngine());
            model = new Model().configure(params);
            if (modelId == null)
            {
                model.initialize();
                modelId = model.getModelId();
            }
            else
            {
                model.initialize(modelId);
            }
            logger.info("Configured new model with model id '{}'", modelId);
        }
        catch (ConfigurationException ce)
        {
            logger.error("could not configure and initialize model", ce);
        }
    }

    private String modelId = null;
    private String userByCredentialsAttribute = null;
    private Model model = null;
}
