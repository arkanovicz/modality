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
import com.republicate.modality.impl.PostgresqlCopyManager;
import com.republicate.modality.sql.ConnectionWrapper;
import com.republicate.modality.sql.SqlUtils;
import com.republicate.modality.sql.StatementPool;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Arrays;
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
    public long performImpl(Serializable... paramValues) throws SQLException
    {
        // CB TODO - review parameters mapping as in BaseAttribute for redundancy handling
        ConnectionWrapper connection = null;
        Savepoint savepoint = null;
        try
        {
            long changed = 0;
            // support nested transactions using savepoints (CB TODO - add test case)
            connection = StatementPool.getCurrentTransactionConnection(getModel().getModelId());
            if (connection == null)
            {
                connection = getModel().getTransactionConnection();
                StatementPool.setCurrentTransactionConnection(getModel().getModelId(), connection);
            }
            else
            {
                savepoint = connection.setSavepoint();
            }
            connection.enterBusyState();
            int param = 0;
            for (String individualStatement : getStatements())
            {
                if (getModel().getLogger().isTraceEnabled())
                {
                    getModel().getLogger().trace("prepare-{}", individualStatement);
                }
                // Check for postgresql COPY FROM STDIN command
                if (PostgresqlCopyManager.isPostgresqlCopyFromStdin(getModel(), individualStatement, paramValues))
                {
                    if (postgresqlCopyManager == null)
                    {
                        synchronized (this)
                        {
                            if (postgresqlCopyManager == null)
                            {
                                postgresqlCopyManager = new PostgresqlCopyManager(getModel());
                            }
                        }
                    }
                    changed += postgresqlCopyManager.copyFromStdin(individualStatement, paramValues[0]);
                }
                else
                {

                    PreparedStatement statement = connection.prepareStatement(individualStatement);
                    int paramCount = statement.getParameterMetaData().getParameterCount();
                    if (getModel().getLogger().isTraceEnabled())
                    {
                        getModel().getLogger().trace("params-{}", Arrays.asList(paramValues).subList(param, param + paramCount));
                    }
                    for (int i = 1; i <= paramCount; ++i)
                    {
                        statement.setObject(i, paramValues[param++]);
                    }
                    changed += statement.executeUpdate();
                }
            }
            if (savepoint == null)
            {
                connection.commit();
            }
            else
            {
                connection.releaseSavepoint(savepoint);
            }
            return changed;
        }
        catch (SQLException sqle)
        {
            if (connection != null)
            {
                if (savepoint != null)
                {
                    connection.rollback(savepoint);
                }
                else
                {
                    connection.rollback();
                }
            }
            throw sqle;
        }
        finally
        {
            if (connection != null)
            {
                connection.leaveBusyState();
                if (savepoint == null) // means we own the transaction connection
                {
                    StatementPool.resetCurrentTransactionConnection(getModel().getModelId());
                }
            }
        }
    }

    protected List<String> getStatements() throws SQLException
    {
        if (statements == null)
        {
            // if statements are null at this point, this may be a mergeable query
            if (originalQuery == null) throw new SQLException("unhandled case");
            return splitStatements();
        }
        return statements;
    }

    private List<String> splitStatements() throws SQLException
    {
        boolean considerDollar = getModel().getDriverInfos().getTag().equals("postgresql");
        Character quoteChar = getModel().getDriverInfos().getIdentifierQuoteChar();
        return SqlUtils.splitStatements(getQuery(), quoteChar, considerDollar);
    }

    @Override
    public void initialize()
    {
        super.initialize();
        try
        {
            statements = splitStatements();
        }
        catch (SQLException sqle)
        {
            throw new ConfigurationException("unknown error", sqle);
        }
    }

    @Override
    public synchronized void setQuery(String qry)
    {
        super.setQuery(qry);
        statements = null; // triggers recalculation
    }

    private List<String> statements = null;
    private List<List<String>> parameterNamesLists = new ArrayList<>();
}
