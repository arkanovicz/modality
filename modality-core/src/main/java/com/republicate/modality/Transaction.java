package com.republicate.modality;

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
