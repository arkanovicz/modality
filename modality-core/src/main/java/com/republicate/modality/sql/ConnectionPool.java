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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.sql.DataSource;

/**
 *  Connection pool.
 *
 *  @author <a href="mailto:claude.brisson@gmail.com">Claude Brisson</a>
 */
public class ConnectionPool implements Serializable
{
    private static final int VALIDATION_TIMEOUT = 1; // 1s is long enough

    protected static Logger logger = LoggerFactory.getLogger("sql");
    private static Random randomizer = new Random();

    /**
     * Constructor.
     *
     * @throws SQLException
     */
    public ConnectionPool(DataSource dataSource, Credentials credentials, DriverInfos driverInfos, String schema, boolean autocommit, int max, boolean checkConnections) throws SQLException
    {
        this.dataSource = dataSource;
        this.credentials = credentials;
        this.driverInfos = driverInfos;
        this.schema = schema;
        this.autocommit = autocommit;
        this.max = max;
        this.checkConnections = checkConnections;
    }

    public ConnectionPool(DataSource dataSource, Credentials credentials, DriverInfos driverInfos, String schema, boolean autocommit, int max) throws SQLException
    {
        this(dataSource, credentials, driverInfos, schema, autocommit, max, true);
    }

    public ConnectionPool(DataSource dataSource, Credentials credentials, DriverInfos driverInfos, String schema, boolean autocommit) throws SQLException
    {
        this(dataSource, credentials, driverInfos, schema, autocommit, 50, true);
    }

    public ConnectionPool(DataSource dataSource, Credentials credentials, DriverInfos driverInfos, String schema) throws SQLException
    {
        this(dataSource, credentials, driverInfos, schema, true, 50, true);
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setSchema(String schema)
    {
        this.schema = schema;
    }

    /**
     * Get a connection.
     * @return a connection
     * @throws SQLException
     */
    public synchronized ConnectionWrapper getConnection() throws SQLException
    {
        for(Iterator it = connections.iterator(); it.hasNext(); )
        {
            ConnectionWrapper c = (ConnectionWrapper)it.next();
            if (!c.isBusy())
            {
                if(c.isClosed() || checkConnections && !c.isValid(VALIDATION_TIMEOUT))
                {
                    it.remove();
                }
                else
                {
                    return c;
                }
            }
        }
        if(connections.size() == max)
        {
            logger.warn("Connection pool: max number of connections reached! ");

            // return a busy connection...
            return connections.get(randomizer.nextInt(connections.size()));
        }

        ConnectionWrapper newconn = createConnection();

        connections.add(newconn);
        return newconn;
    }

    /**
     * Create a connection.
     *
     * @return connection
     * @throws SQLException
     */
    private ConnectionWrapper createConnection() throws SQLException
    {
        logger.info("Creating a new connection{}.", schema != null && schema.length() > 0 ? " on schema " + schema : "");

        Connection connection = credentials.getConnection(dataSource);

        // schema
        if(schema != null && schema.length() > 0)
        {
            String schemaQuery = driverInfos.getSchemaQuery();

            if(schemaQuery != null)
            {
                schemaQuery = schemaQuery.replace("$schema", schema);
                Statement stmt = connection.createStatement();
                stmt.executeUpdate(schemaQuery);
                stmt.close();
            }
            else
            {
                connection.setSchema(schema);
            }
        }

        // autocommit
        connection.setAutoCommit(autocommit);
        return new ConnectionWrapper(driverInfos, connection);
    }

/*
    private String getSchema(Connection connection) throws SQLException
    {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("select sys_context('userenv','current_schema') from dual");
        rs.next();
        return rs.getString(1);
    }*/

    /**
     * clear all connections.
     */
    public void clear()
    {
        for(Iterator it = connections.iterator(); it.hasNext(); )
        {
            ConnectionWrapper c = (ConnectionWrapper)it.next();

            try
            {
                c.close();
            }
            catch(SQLException sqle) {}
        }
    }

    private DataSource dataSource;

    private Credentials credentials;

    /** optional schema */
    private String schema = null;

    /** infos on the driverInfos */
    private DriverInfos driverInfos = null;

    /** autocommit flag */
    private boolean autocommit = true;

    /** list of all connections */
    private List<ConnectionWrapper> connections = new ArrayList<>();

    /** Maximum number of connections. */
    private int max;

    /** whether to check connections */
    private boolean checkConnections;
}
