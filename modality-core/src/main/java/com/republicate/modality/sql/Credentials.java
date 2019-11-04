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

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public final class Credentials
{
    public Credentials() {}

    public String getUser()
    {
        return user;
    }

    public boolean hasCredentials()
    {
        return user != null && password != null;
    }

    public Credentials setUser(String user)
    {
        this.user = user;
        return this;
    }

    public Credentials setPassword(String password)
    {
        this.password = password;
        return this;
    }

    public Connection getConnection(DataSource dataSource) throws SQLException
    {
        if (hasCredentials())
        {
            return dataSource.getConnection(user, password);
        }
        else
        {
            return dataSource.getConnection();
        }
    }

    private String user = null;
    private String password = null;
}
