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

import com.republicate.modality.impl.BaseModel;
import com.republicate.modality.sql.ConnectionWrapper;
import com.republicate.modality.sql.PooledStatement;
import com.republicate.modality.sql.StatementPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.function.Function;

public class Model extends BaseModel
{
    public Model()
    {
        super();
    }

    public Model(String modelId)
    {
        super(modelId);
    }

    public Model getModel()
    {
        return this;
    }

    /**
     * Prepare a query.
     *
     * @param query an sql query
     * @return the pooled prepared statement corresponding to the query
     */
    protected PooledStatement prepareQuery(String query) throws SQLException
    {
        checkInitialized();
        return getStatementPool().prepareQuery(query);
    }

    /**
     * Prepare an update query.
     *
     * @param query an sql query
     * @return the pooled prepared statement corresponding to the query
     */
    protected PooledStatement prepareUpdate(String query) throws SQLException
    {
        checkInitialized();
        return getStatementPool().prepareUpdate(query);
    }

    /**
     * Get a transaction connection with manual commit/rollback
     * @return a transaction connection
     * @throws SQLException
     */
    @Override
    protected ConnectionWrapper getTransactionConnection() throws SQLException
    {
        checkInitialized();
        return super.getTransactionConnection();
    }

    /**
     * Perform operations inside a transaction connection
     */
    public void attempt(ModelRunnable operation) throws SQLException
    {
        checkInitialized();
        ConnectionWrapper connection = null;
        try
        {
            connection = getModel().getTransactionConnection();
            connection.enterBusyState();
            StatementPool.setCurrentTransactionConnection(getModelId(), connection);
            operation.run();
            connection.commit();
        }
        catch (SQLException sqle)
        {
            try
            {
                getLogger().error("initiating transaction rollback upon error", sqle);
                if (connection != null)
                {
                    connection.rollback();
                }
            }
            catch (SQLException sqlee)
            {
                getLogger().error("could not rollback", sqlee);
            }
            throw sqle;
        }
        finally
        {
            if (connection != null)
            {
                StatementPool.resetCurrentTransactionConnection(getModelId());
                connection.leaveBusyState();
            }
        }
    }
}
