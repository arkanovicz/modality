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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.stream.Collectors;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.AnsiFormat;
import com.diogonunes.jcolor.Attribute;

interface Colorizer
{
    AnsiFormat blue = new AnsiFormat(Attribute.BRIGHT_BLUE_TEXT());
    AnsiFormat green = new AnsiFormat(Attribute.BRIGHT_GREEN_TEXT());
    AnsiFormat red = new AnsiFormat(Attribute.BRIGHT_RED_TEXT());
    AnsiFormat bold = new AnsiFormat(Attribute.BOLD());

    default String blue(String str)
    {
        return Ansi.colorize(str, blue);
    }
    default String green(String str)
    {
        return Ansi.colorize(str, green);
    }
    default String red(String str)
    {
        return Ansi.colorize(str, red);
    }
    default String bold(String str)
    {
        return Ansi.colorize(str, bold);
    }


}

public class BaseBookshelfTests implements Colorizer
{
    protected static Logger logger = LoggerFactory.getLogger("tests");

    protected static DataSource getDataSource() throws Exception
    {
        BasicDataSource ds = new BasicDataSource();
        ds.setUrl("jdbc:hsqldb:.;hsqldb.sqllog=3");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    protected static synchronized void populateDataSource(String sqlFile) throws Exception
    {
        DataSource ds = getDataSource();
        Connection connection = ds.getConnection();
        Statement statement = connection.createStatement();
        String sql = IOUtils.toString(getResourceReader(sqlFile));
        for (String command : sql.split(";"))
        {
            if (command.trim().length() == 0) continue;
            // System.err.println("Running ["+command+"]");
            statement.executeUpdate(command);
            // System.err.println("Done.");
        }
        statement.close();
        connection.close();
    }

    @AfterClass
    public static void clearDataSource() throws Exception
    {
        DataSource ds = getDataSource();
        Connection connection = ds.getConnection();
        Statement statement = connection.createStatement();
        // Under hsqldb, next statement has for effect to just *empty* the default schema.
        statement.executeUpdate("DROP SCHEMA PUBLIC CASCADE;");
        statement.close();
        connection.close();
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
                    case "jdbc/test-data-source": return getDataSource();
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

    /*
     * Print current test name in log
     */

    @Before
    public void setUp() throws Exception
    {
        String niceName = formatTestName(name.getMethodName());
        logger.info(bold("******************************** " + this.getClass().getSimpleName() + " > " + niceName + " ********************************"));
        // TODO - CB - Resetting the database between each test doesn't work for now, we ought to refresh webapp db connections,
        // otherwise Modality reverse enginering fails randomly.
        // resetDatabase();
    }

    private String formatTestName(String methodName) throws Exception
    {
        String parts[] = methodName.split("(?<=[a-z])(?=[A-Z0-9])|(?<=[a-z0-9])(?=[A-Z])|(?:^test)?_");
        String niceName = Arrays.stream(parts).filter(p -> !p.isEmpty()).collect(Collectors.joining(" "));
        return niceName;
    }

    @Rule
    public TestName name = new TestName();

}

