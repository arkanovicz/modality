package com.republicate.modality.webapp.auth;

import com.republicate.modality.Instance;
import com.republicate.modality.util.SlotHashMap;
import com.republicate.modality.util.SlotMap;
import com.republicate.modality.webapp.auth.helpers.CredentialsChecker;
import com.republicate.modality.webapp.auth.helpers.CredentialsCheckerImpl;

import java.sql.SQLException;
import java.util.Optional;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

/**
 * <p>Basic Authentication filter.</p>
 * <p>Configuration parameters:</p>
 * <ul>
 *     <li>auth.model.<b>user_by_credentials</b>&nbsp;row attribute name to use ;
 *     defaults to <code>user_by_credentials</code>.</li>
 * </ul>
 * <p>As usual, configuration parameters can be filter's init-params or global context-params, or inside <code>modality.properties</code>.</p>
 */


public class BasicAuthFilter extends AbstractBasicAuthFilter<Instance>
{
    public static final String USER_BY_CRED_ATTRIBUTE = "auth.model.user_by_credentials";

    private static final String DEFAULT_MODEL_AUTH_USER_BY_CREDENTIALS = "user_by_credentials";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
        String userByCredentialsAttribute = Optional.ofNullable(findConfigParameter(USER_BY_CRED_ATTRIBUTE)).orElse(DEFAULT_MODEL_AUTH_USER_BY_CREDENTIALS);
        credentialsChecker = new CredentialsCheckerImpl(userByCredentialsAttribute);
    }

    @Override
    protected Instance checkCredentials(String login, String password) throws ServletException
    {
        getModel(); // force model initialization
        return credentialsChecker.checkCredentials(login, password);
    }

    private CredentialsChecker<Instance> credentialsChecker = null;
}
