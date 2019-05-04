package com.republicate.modality;

import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.impl.AttributeHolder;
import com.republicate.modality.sql.ConnectionWrapper;
import com.republicate.modality.sql.SqlUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Transaction extends Action
{
    public Transaction(String name, AttributeHolder parent)
    {
        super(name, parent);
    }

    public Transaction(String name, Action action)
    {
        super(name, action.getParent());
        setParameterNames(action.getParameterNames());
        try
        {
            setQuery(action.getQuery());
        }
        catch (SQLException sqle)
        {
            throw new ConfigurationException("unknown error", sqle);
        }
    }

    @Override
    public int performImpl(Serializable... paramValues) throws SQLException
    {
        ConnectionWrapper connection = null;
        try
        {
            int changed = 0;
            connection = getModel().getTransactionConnection();
            connection.enterBusyState();
            int param = 0;
            for (String individualStatement : getStatements())
            {
                PreparedStatement statement = connection.prepareStatement(individualStatement);
                int paramCount = statement.getParameterMetaData().getParameterCount();
                for (int i = 1; i <= paramCount; ++i)
                {
                    statement.setObject(i, paramValues[param++]);
                }
                changed += statement.executeUpdate();
            }
            connection.commit();
            return changed;
        }
        catch (SQLException sqle)
        {
            if (connection != null)
            {
                connection.rollback();
            }
            throw sqle;
        }
        finally
        {
            if (connection != null)
            {
                connection.leaveBusyState();
            }
        }
    }

    protected List<String> getStatements() throws SQLException
    {
        return statements;
    }

    @Override
    protected void initialize()
    {
        try
        {
            statements = SqlUtils.splitStatements(getQuery(), getModel().getDriverInfos().getIdentifierQuoteChar());
        }
        catch (SQLException sqle)
        {
            throw new ConfigurationException("unknown error", sqle);
        }
    }

    private List<String> statements = new ArrayList<>();
    private List<List<String>> parameterNamesLists = new ArrayList<>();
}
