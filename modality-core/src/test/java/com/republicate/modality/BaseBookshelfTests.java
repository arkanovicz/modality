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

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.app.VelocityEngine;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

public class BaseBookshelfTests
{
    protected VelocityEngine createVelocityEngine(String propertiesFile)
    {
        VelocityEngine engine = propertiesFile == null ? new VelocityEngine() : new VelocityEngine(propertiesFile);
        engine.init();
        return engine;
    }

    protected VelocityEngine createVelocityEngine(Properties properties)
    {
        VelocityEngine engine = properties == null ? new VelocityEngine() : new VelocityEngine(properties);
        engine.init();
        return engine;
    }


    protected static DataSource initDataSource() throws Exception
    {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl("jdbc:hsqldb:.;hsqldb.sqllog=3");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    private static boolean dataSourcePopulated = false;

    protected static synchronized void populateDataSource() throws Exception
    {
        if (dataSourcePopulated)
        {
            return;
        }
        DataSource ds = initDataSource();
        Connection connection = ds.getConnection();
        Statement statement = connection.createStatement();
        String sql = IOUtils.toString(getResourceReader("bookshelf.sql"));
        for (String command : sql.split(";"))
        {
            if (command.trim().length() == 0) continue;
            // System.err.println("Running ["+command+"]");
            statement.executeUpdate(command);
            // System.err.println("Done.");
        }
        statement.close();
        connection.close();
        dataSourcePopulated = true;
    }

    protected static URL getResource(String name)
    {
        return BasicModelToolTests.class.getClassLoader().getResource(name);
    }

    protected static Reader getResourceReader(String name)
    {
        return new InputStreamReader(BasicModelToolTests.class.getClassLoader().getResourceAsStream(name), StandardCharsets.UTF_8);
    }

    public static class TestJNDIContext extends InitialContext
    {
        public TestJNDIContext() throws NamingException
        {}

        @Override
        public Object lookup(String name) throws NamingException
        {
            try
            {
                // System.err.println("@@@ looking for " + name);
                switch (name)
                {
                    case "java:comp/env": return this;
                    case "jdbc/test-data-source": return initDataSource();
                    default: return null;
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }
    }

    /*
     * A dummy jndi mechanism
     */

    static TestJNDIContext jndiContext;
    static
    {
        try
        {
            // define system property after singleton creation to avoid infinite loop
            jndiContext = new TestJNDIContext();
            System.setProperty("java.naming.factory.initial", JNDIContextFactory.class.getName());
        }
        catch (NamingException ne)
        {
            ne.printStackTrace();
        }
    }

    public static class JNDIContextFactory implements InitialContextFactory
    {
        @Override
        public javax.naming.Context getInitialContext(Hashtable<?, ?> environment) throws NamingException
        {
            return jndiContext;
        }
    }


}
