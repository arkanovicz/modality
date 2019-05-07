package com.republicate.modality.webapp.auth.helpers;

import com.republicate.modality.Attribute;
import com.republicate.modality.Instance;
import com.republicate.modality.Model;
import com.republicate.modality.RowAttribute;
import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.util.SlotHashMap;
import com.republicate.modality.util.SlotMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import javax.servlet.ServletException;

public class CredentialsCheckerImpl implements CredentialsChecker<Instance>
{
    protected static Logger logger = LoggerFactory.getLogger("auth");

    public CredentialsCheckerImpl(String userByCredentialsAttribute)
    {
        this.userByCredentialsAttribute = userByCredentialsAttribute;
    }

    @Override
    public CredentialsChecker<Instance> setModel(Model model) throws ServletException
    {
        this.model = model;

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
        return this;
    }

    @Override
    public Instance checkCredentials(String login, String password) throws ServletException
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

    public Model getModel()
    {
        return model;
    }

    private Model model = null;
    private String userByCredentialsAttribute = null;
}
