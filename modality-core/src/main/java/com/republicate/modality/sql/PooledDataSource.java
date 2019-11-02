package com.republicate.modality.sql;

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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

/**
 * <p>ConnecionPoolDataSource wrapper.</p>
 */

public class PooledDataSource implements DataSource
{
    public PooledDataSource(ConnectionPoolDataSource connectionPoolDataSource)
    {
        this.connectionPoolDataSource = connectionPoolDataSource;
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return connectionPoolDataSource.getPooledConnection().getConnection();
    }

    @Override
    public Connection getConnection(String user, String password) throws SQLException
    {
        return connectionPoolDataSource.getPooledConnection(user, password).getConnection();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException
    {
        logWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException
    {

    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        return 0;
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        throw new SQLFeatureNotSupportedException("not using JDK logging");
    }

    private ConnectionPoolDataSource connectionPoolDataSource;

    private PrintWriter logWriter = null;
}
