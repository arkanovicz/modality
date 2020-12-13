package com.republicate.modality.impl;

import com.republicate.modality.Model;
import com.republicate.modality.config.ConfigHelper;
import com.republicate.modality.filter.IdentifiersFilters;
import com.republicate.modality.sql.ConnectionWrapper;
import com.republicate.modality.sql.NonPositionedParameter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostgresqlCopyManager
{
    protected Model model;

    private static final Pattern copyFromStdin = Pattern.compile("^COPY\\s+\"?(\\w+)\"?(?:\\s*\\([^)]+\\)\\s*|\\s+)\\s*FROM\\s+STDIN\\s+.*$");
    private static final String COPY_MANAGER_CLASS = "org.postgresql.copy.CopyManager";
    private static final String BASE_CONNECTION_CLASS = "org.postgresql.core.BaseConnection";
    private static Class copyManagerClass = null;
    private static Constructor copyManager_Ctor = null;
    private static Method copyManager_copyIn = null;
    protected ConfigHelper helper = new ConfigHelper();

    public interface ResourceURLProvider extends Function<String, URL>, NonPositionedParameter
    {
        // implementing classes needs a
        //   public URL apply(String tableName);
        // method
    }

    public PostgresqlCopyManager(Model model)
    {
        this.model = model;
    }

    public static boolean isPostgresqlCopyFromStdin(Model model, String qry, Serializable params[])
    {
        return model.getDriverInfos().getTag().equals("postgresql")
            && (
                params.length == 1 && params[0] != null && (params[0] instanceof String || params[0] instanceof URL)
                ||
                Arrays.stream(params).anyMatch(param -> param != null && param instanceof ResourceURLProvider)
                )
            && copyFromStdin.matcher(qry).matches();
    }

    public long copyFromStdin(String query, Serializable param) throws SQLException
    {
        ConnectionWrapper wrapper = null;
        if (copyManagerClass == null)
        {
            synchronized (getClass())
            {
                if (copyManagerClass == null)
                {
                    try
                    {
                        copyManagerClass = Class.forName(COPY_MANAGER_CLASS);
                        Class baseConnectionClass = Class.forName(BASE_CONNECTION_CLASS);
                        Class params[] = { baseConnectionClass };
                        copyManager_Ctor = copyManagerClass.getConstructor(params);
                        copyManager_copyIn = copyManagerClass.getMethod("copyIn", String.class, Reader.class);
                    }
                    catch (ClassNotFoundException | NoSuchMethodException e)
                    {
                        throw new SQLException("Could not load postgresql CopyManager class", e);
                    }
                }
            }
        }
        boolean ownsTransaction = false;
        try
        {
            wrapper = model.getCurrentTransactionConnection();
            if (wrapper == null)
            {
                // always use a transaction
                // CB TODO - there should be a getConnectionPool().borrowConnection()
                // that would gather the two next lines here and elsewhere (small concurrency issue)
                wrapper = ((BaseModel)model).getTransactionConnection();
                ownsTransaction = true;
            }
            wrapper.enterBusyState();

            // instantiate copy manager
            Object copyManager = null;
            try
            {
                Connection connection = wrapper.unwrap();
                Object ctorArgs[] = {connection};
                copyManager = copyManager_Ctor.newInstance(ctorArgs);
            }
            catch (IllegalAccessException | InstantiationException | InvocationTargetException e)
            {
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof SQLException) throw (SQLException)cause;
                throw new SQLException("could not instantiate copy manager", e);
            }

            // get resource reader
            Reader reader = null;
            try
            {
                if (param instanceof ResourceURLProvider)
                {
                    Matcher matcher = copyFromStdin.matcher(query);
                    if (!matcher.matches()) throw new SQLException("unhandled case");
                    String tableName = matcher.group(1);
                    param = ((ResourceURLProvider)param).apply(tableName);
                }
                else if (param instanceof String)
                {
                    param = helper.findURL((String)param, model.getServletContext(), false);
                }
                else if (!(param instanceof URL))
                {
                    throw new SQLException("Expecting a path or URL parameter");
                }
                if (param == null)
                {
                    throw new SQLException("Could not find resource: " + param);
                }
                reader = new InputStreamReader(((URL) param).openStream(), StandardCharsets.UTF_8);
            }
            catch (IOException e)
            {
                throw new SQLException("could not get URL reader", e);
            }

            // copy
            long affected = -1;
            try
            {
                affected = ((Number)copyManager_copyIn.invoke(copyManager, query, reader)).longValue();
                if (ownsTransaction)
                {
                    wrapper.commit();
                }
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof SQLException) throw (SQLException)cause;
                throw new SQLException("could not copy in", e);
            }
            if (ownsTransaction)
            {
                wrapper.commit();
            }
            return affected;
        }
        catch (SQLException sqle)
        {
            if (ownsTransaction)
            {
                wrapper.rollback();
            }
            throw sqle;
        }
        finally
        {
            if (wrapper != null)
            {
                wrapper.leaveBusyState();
            }
        }
    }
}
