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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import javax.sql.DataSource;

/**
 * <p>Standalone data source, aka connections provider.</p>
 * <p>When using the BasicDataSource from *inside* a webapp (i.e. *not* using JNDI):</p>
 * <ul>
 *     <li>If the JDBC driver livrary is present both in the container libraries and in the webapp libraries,
 *     the driver will be automatically registered.</li>
 *     <li>If the JDBC driver library is only present in the webapp libraries, the application *is* responsible
 *     for registering it with the DriverManager, which boils down to calling Class.forName(driverClass).</li>
 *     <li>If the JDBC driver library is present only in the container libraries, its use in a BasicDataSource
 *     is impossible, you'll have to resort to configure a JNDI data source.</li>
 * </ul>
 * <p>It is considered a best practice to always use JNDI in a webapp, as there is a good chance to create memory
 * leaks when the webapp is reloaded otherwise.</p>
 */

public class BasicDataSource implements DataSource
{
    public BasicDataSource(String databaseURL)
    {
        this.databaseURL = databaseURL;
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        // When using the BasicDatasource in a webapp, the following call will automatically register
        // a driver present in the webapp classpath (at the condition that the driver is *also* present
        // in the container classpath).
        DriverManager.getDrivers();
        return DriverManager.getConnection(databaseURL);
    }

    @Override
    public Connection getConnection(String user, String password) throws SQLException
    {
        // When using the BasicDatasource in a webapp, the following call will automatically register
        // a driver present in the webapp classpath (at the condition that the driver is *also* present
        // in the container classpath).
        DriverManager.getDrivers();
        return DriverManager.getConnection(databaseURL, user, password);
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

    private String databaseURL;

    private PrintWriter logWriter = null;
}
