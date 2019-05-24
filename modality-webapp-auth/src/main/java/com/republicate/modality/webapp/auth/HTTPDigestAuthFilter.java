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

import com.republicate.modality.Instance;
import com.republicate.modality.Model;
import com.republicate.modality.RowAttribute;
import com.republicate.modality.ScalarAttribute;
import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.util.SlotHashMap;
import com.republicate.modality.util.SlotMap;

import java.sql.SQLException;
import java.util.Optional;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * <p>HTTP Digest Authentication filter.</p>
 * <p>Configuration parameters:</p>
 * <ul>
 *     <li><code>auth.model.digest_by_login</code> - scalar attribute returning MD5(login:realm:password). Defaults to
 *     'digest_by_login'. Receives the parameters 'login' and 'realm'.</li>
 *     <li><code>auth.model.user_by_login</code> - row attribute returning the user instance. Defaults to
 *     'user_by_login'. Receives the parameters 'login' and 'realm'.</li>
 * </ul>
 * <p>As usual, configuration parameters can be filter's configure-params or global context-params, or inside <code>modality.properties</code>.</p>
 */

public class HTTPDigestAuthFilter extends BaseHTTPDigestAuthFilter<Instance>
{
    // configuration keys

    public static final String DIGEST_BY_LOGIN = "auth.model.digest_by_login";
    public static final String USER_BY_LOGIN =   "auth.model.user_by_login";

    // default values

    private static final String DEFAULT_DIGEST_BY_LOGIN = "digest_by_login";
    private static final String DEFAULT_USER_BY_LOGIN =   "user_by_login";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);

    }

    @Override
    public void modelInitialized(Model model) throws ServletException
    {
        super.modelInitialized(model);

        String digestByLogin = Optional.ofNullable(findConfigParameter(DIGEST_BY_LOGIN)).orElse(DEFAULT_DIGEST_BY_LOGIN);
        digestByLoginAttribute = getModel().getScalarAttribute(digestByLogin);
        if (digestByLoginAttribute == null)
        {
            throw new ConfigurationException("attribute does not exist: " + digestByLogin);
        }

        String userByLogin = Optional.ofNullable(findConfigParameter(USER_BY_LOGIN)).orElse(DEFAULT_USER_BY_LOGIN);
        userByLoginAttribute = getModel().getRowAttribute(userByLogin);
        if (userByLoginAttribute == null)
        {
            throw new ConfigurationException("attribute does not exist: " + userByLogin);
        }
    }

    @Override
    protected String getUserRealmPasswordMD5(String login) throws AuthenticationException
    {
        try
        {
            getModel(); // force initialization
            SlotMap params = new SlotHashMap();
            params.put("realm", getRealm());
            params.put("login", login);
            return digestByLoginAttribute.getString(params);
        }
        catch (SQLException | ServletException e)
        {
            logger.error("could not get user digest", e);
            return null;
        }
    }

    @Override
    protected Instance getUserInstance(String login) throws AuthenticationException
    {
        try
        {
            getModel(); // force initialization
            SlotMap params = new SlotHashMap();
            params.put("realm", getRealm());
            params.put("login", login);
            return userByLoginAttribute.retrieve(params);
        }
        catch (SQLException | ServletException e)
        {
            logger.error("could not get user instance", e);
            return null;
        }
    }

    private ScalarAttribute digestByLoginAttribute = null;
    private RowAttribute userByLoginAttribute = null;
}
