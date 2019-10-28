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


import com.republicate.modality.util.HashMultiMap;
import com.republicate.modality.util.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

/**
 * This class is a pool of PooledPreparedStatements.
 *
 *  @author <a href=mailto:claude.brisson@gmail.com>Claude Brisson</a>
 *
 */
public class StatementPool implements /* Runnable, */ Pool
{
    protected Logger logger = LoggerFactory.getLogger("sql");

    public StatementPool(ConnectionPool connectionPool)
    {
        this(connectionPool, -1);
    }

    /**
     * build a new pool.
     *
     * @param connectionPool connection pool
     */
    public StatementPool(ConnectionPool connectionPool, long connectionsCheckInterval)
    {
        this.connectionPool = connectionPool;
        this.connectionsCheckInterval = connectionsCheckInterval;
    }

    /**
     * get a PooledStatement associated with this query.
     *
     * @param query an SQL query
     * @exception SQLException thrown by the database engine
     * @return a valid statement
     */
    protected synchronized PooledStatement prepareStatement(String query, boolean update) throws SQLException
    {
        logger.trace("prepare-" + query);

        PooledStatement statement = null;
        ConnectionWrapper connection = currentTransactionConnection.get();
        boolean insideTransaction = false;

        if (connection == null)
        {
            List available = statementsMap.get(query);
            for (Iterator it = available.iterator(); it.hasNext(); )
            {
                statement = (PooledStatement) it.next();
                if (statement.isValid())
                {
                    if (!statement.isInUse() && !(connection = statement.getConnection()).isBusy())
                    {
                        // check connection
                        if (!connection.isClosed() && (connectionsCheckInterval < 0 || System.currentTimeMillis() - connection.getLastUse() < connectionsCheckInterval || connection.check()))
                        {
                            statement.notifyInUse();
                            return statement;
                        }
                        else
                        {
                            dropConnection(connection);
                            it.remove();
                        }
                    }
                }
                else
                {
                    it.remove();
                }
            }
            if (count == maxStatements)
            {
                throw new SQLException("Error: Too many opened prepared statements!");
            }
            connection = connectionPool.getConnection();
        }
        else
        {
            insideTransaction = true;
        }

        statement = new PooledStatement(connection,
                update ?
                    connection.prepareStatement(
                            query, connection.getDriverInfos().getLastInsertIdPolicy() == DriverInfos.LastInsertIdPolicy.GENERATED_KEYS ?
                                    Statement.RETURN_GENERATED_KEYS :
                                    Statement.NO_GENERATED_KEYS) :
                    connection.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY));
        if (!insideTransaction)
        {
            statementsMap.put(query, statement);
        }
        statement.notifyInUse();
        return statement;
    }

    public synchronized PooledStatement prepareQuery(String query) throws SQLException
    {
        return prepareStatement(query, false);
    }

    public synchronized PooledStatement prepareUpdate(String query) throws SQLException
    {
        return prepareStatement(query, true);
    }

    /**
     * cycle through statements to check and recycle them.
     * 
     * public void run() {
     *   while (running) {
     *       try {
     *           Thread.sleep(checkDelay);
     *       } catch (InterruptedException e) {}
     *       long now = System.currentTimeMillis();
     *       PooledStatement statement = null;
     *       for (Iterator it=statementsMap.keySet().iterator();it.hasNext();)
     *           for (Iterator jt=statementsMap.get(it.next()).iterator();jt.hasNext();) {
     *               statement = (PooledStatement)jt.next();
     *               if (statement.isInUse() && now-statement.getTagTime() > timeout)
     *                   statement.notifyOver();
     *           }
     *   }
     * }
     */

    /**
     * close all statements.
     */
    public void clear()
    {
        // close all statements
        for(Iterator it = statementsMap.keySet().iterator(); it.hasNext(); )
        {
            for(Iterator jt = statementsMap.get(it.next()).iterator(); jt.hasNext(); )
            {
                try
                {
                    ((PooledStatement)jt.next()).close();
                }
                catch(SQLException e)
                {    // don't care now...
                    logger.error("error while clearing pool", e);
                }
            }
        }
        statementsMap.clear();
    }

    /*
     *  drop all statements relative to a specific connection
     * @param connection the connection
     */
    private void dropConnection(ConnectionWrapper connection)
    {
        for(Iterator it = statementsMap.keySet().iterator(); it.hasNext(); )
        {
            for(Iterator jt = statementsMap.get(it.next()).iterator(); jt.hasNext(); )
            {
                PooledStatement statement = (PooledStatement)jt.next();

                if(statement.getConnection() == connection)
                {
                    try
                    {
                        statement.close();
                    }
                    catch(SQLException sqle) {}
                    statement.setInvalid();
                }
            }
        }
        try
        {
            connection.close();
        }
        catch(SQLException sqle) {}
    }

    /**
     * clear statements on exit.
     */
    protected void finalize()
    {
        clear();
    }

    public static void setCurrentTransactionConnection(ConnectionWrapper connection)
    {
        currentTransactionConnection.set(connection);
    }

    public static void resetCurrentTransactionConnection()
    {
        currentTransactionConnection.remove();
    }

    /**
     * debug - get usage statistics.
     *
     * @return an int array : [nb of statements in use , total nb of statements]
     */
    public int[] getUsageStats()
    {
        int[] stats = new int[] { 0, 0 };

        for(Iterator it = statementsMap.keySet().iterator(); it.hasNext(); )
        {
            for(Iterator jt = statementsMap.get(it.next()).iterator(); jt.hasNext(); )
            {
                if(!((PooledStatement)jt.next()).isInUse())
                {
                    stats[0]++;
                }
            }
        }
        stats[1] = statementsMap.size();
        return stats;
    }

    /**
     * connection pool.
     */
    private ConnectionPool connectionPool;

    /**
     * statements getCount.
     */
    private int count = 0;

    /**
     * map queries -&gt; statements.
     */
    private MultiMap statementsMap = new HashMultiMap();    // query -> PooledStatement

    /**
     * running thread.
     */
    private Thread checkTimeoutThread = null;

    /**
     * true if running.
     */
    private boolean running = true;

    /**
     * connections check interval
     */
    private long connectionsCheckInterval;

    /**
     * check delay.
     */

//  private static final long checkDelay = 30*1000;

    /**
     * after this timeout, statements are recycled even if not closed.
     */
//  private static final long timeout = 60*60*1000;

    /**
     * max number of statements.
     */
    private static final int maxStatements = 50;

    /**
     * current transaction connection
     */
    private static ThreadLocal<ConnectionWrapper> currentTransactionConnection = new ThreadLocal<>();
}
