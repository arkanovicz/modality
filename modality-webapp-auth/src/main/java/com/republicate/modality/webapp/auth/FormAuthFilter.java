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

import com.republicate.modality.Attribute;
import com.republicate.modality.Instance;
import com.republicate.modality.RowAttribute;
import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.util.SlotHashMap;
import com.republicate.modality.util.SlotMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    public static final String USER_BY_CRED_ATTRIBUTE = "auth.model.user_by_credentials";
    public static final String USER_REFRESH_RATE =      "auth.model.refresh_rate";

    private static final String DEFAULT_MODEL_AUTH_USER_BY_CREDENTIALS = "user_by_credentials";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
        userByCredentialsAttribute = Optional.ofNullable(findConfigParameter(USER_BY_CRED_ATTRIBUTE)).orElse(DEFAULT_MODEL_AUTH_USER_BY_CREDENTIALS);
    }

    @Override
    protected boolean preFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException
    {
        requireModelInit();
        return true;
    }

    protected void initModel() throws ServletException
    {
        super.initModel();

        // now check the user_by_credentials attribute
        Attribute attr = getModel().getAttribute(userByCredentialsAttribute);
        if (attr == null)
        {
            throw new ConfigurationException("attribute does not exist: " + userByCredentialsAttribute);
        }
        if (!(attr instanceof RowAttribute))
        {
            throw new ConfigurationException("not a row attribute: " + userByCredentialsAttribute);
        }

    }

    @Override
    protected Instance checkCredentials(String login, String password)
    {
        try
        {
            SlotMap params = new SlotHashMap();
            params.put("login", login);
            params.put("password", password);
            return getModel().retrieve(userByCredentialsAttribute, params);
        }
        catch (SQLException sqle)
        {
            logger.error("could not check credentials", sqle);
            return null;
        }
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

    private String userByCredentialsAttribute = null;
}
