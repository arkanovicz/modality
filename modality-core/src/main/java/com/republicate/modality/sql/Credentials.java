package com.republicate.modality.sql;

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

    public void setUser(String user)
    {
        this.user = user;
    }

    public void setPassword(String password)
    {
        this.password = password;
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
