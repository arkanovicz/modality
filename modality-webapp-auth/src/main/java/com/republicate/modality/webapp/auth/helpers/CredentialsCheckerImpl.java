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

    public CredentialsCheckerImpl(String userByCredentials)
    {
        this.userByCredentials = userByCredentials;
    }

    @Override
    public CredentialsChecker<Instance> setModel(Model model) throws ServletException
    {
        this.model = model;

        userByCredentialsAttribute = model.getRowAttribute(userByCredentials);
        if (userByCredentialsAttribute == null)
        {
            throw new ConfigurationException("attribute does not exist: " + userByCredentials);
        }
        return this;
    }

    @Override
    public Instance checkCredentials(String realm, String login, String password) throws ServletException
    {
        try
        {
            SlotMap params = new SlotHashMap();
            params.put("realm", realm);
            params.put("login", login);
            params.put("password", password);
            return getModel().retrieve(userByCredentials, params);
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
    private String userByCredentials = null;
    private RowAttribute userByCredentialsAttribute = null;
}
