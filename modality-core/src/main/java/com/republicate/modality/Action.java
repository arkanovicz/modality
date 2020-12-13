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

import com.republicate.modality.impl.AttributeHolder;
import com.republicate.modality.impl.PostgresqlCopyManager;
import com.republicate.modality.sql.ConnectionWrapper;
import com.republicate.modality.sql.PooledStatement;
import com.republicate.modality.sql.StatementPool;

import java.io.Serializable;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

public class Action extends Attribute
{
    public Action(String name, AttributeHolder parent)
    {
        super(name, parent);

    }

    public long perform(Serializable... params) throws SQLException
    {
        return performImpl(getParamValues(params));
    }

    public long perform(Map source) throws SQLException
    {
        return performImpl(getParamValues(source));
    }

    public long perform(Map source, Serializable... params) throws SQLException
    {
        return performImpl(getParamValues(source, params));
    }

    protected long performImpl(Serializable... paramValues) throws SQLException
    {
        long ret = 0;
        PooledStatement statement = null;
        String query = getQuery();
        // Check for postgresql COPY FROM STDIN command
        if (PostgresqlCopyManager.isPostgresqlCopyFromStdin(getModel(), query, paramValues))
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
            return postgresqlCopyManager.copyFromStdin(query, paramValues[0]);
        }
        else
        {
            try
            {

                statement = getModel().prepareUpdate(getQuery());
                statement.getConnection().enterBusyState();
                ret = statement.executeUpdate(paramValues);
                if (ret == 1 && generatedKeyColumn != null)
                {
                    ret = statement.getLastInsertID(generatedKeyColumn);
                }
            }
            finally
            {
                if (statement != null)
                {
                    statement.notifyOver();
                    statement.getConnection().leaveBusyState();
                }
            }
            return ret;
        }
    }

    @Override
    public String getQueryMethodName()
    {
        return "perform";
    }

    public void setGeneratedKeyColumn(String generatedKeyColumn)
    {
        this.generatedKeyColumn = generatedKeyColumn;
    }

    private String generatedKeyColumn = null;

    // for postgresql COPY FROM STDIN
    protected PostgresqlCopyManager postgresqlCopyManager = null;

}
