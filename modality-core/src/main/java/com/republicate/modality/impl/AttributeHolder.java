package com.republicate.modality.impl;

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

import com.republicate.modality.Action;
import com.republicate.modality.Attribute;
import com.republicate.modality.Entity;
import com.republicate.modality.Instance;
import com.republicate.modality.Model;
import com.republicate.modality.RowAttribute;
import com.republicate.modality.RowsetAttribute;
import com.republicate.modality.ScalarAttribute;
import com.republicate.modality.Transaction;
import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.sql.SqlUtils;
import com.republicate.modality.util.TypeUtils;
import org.slf4j.Logger;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public abstract class AttributeHolder implements Serializable
{
    protected abstract Model getModel();

    protected abstract Logger getLogger();

    private String logName = null;

    public AttributeHolder(String logName)
    {
        this.logName = logName;
    }

    protected void initializeAttributes()
    {
        for (Map.Entry<String, Attribute> entry : attributesMap.entrySet())
        {
            Attribute attribute = entry.getValue();
            if (attribute instanceof Action)
            {
                // promote Action to Transaction if needed
                String query;
                try
                {
                    query = attribute.getQuery();
                }
                catch (SQLException sqle)
                {
                    throw new ConfigurationException("unknown error", sqle);
                }
                if (SqlUtils.hasMultipleStatements(query, getModel().getDriverInfos().getIdentifierQuoteChar()))
                {
                    String key = entry.getKey();
                    Transaction transaction = new Transaction(key, (Action)attribute);
                    entry.setValue(transaction);
                    attribute = transaction;
                }
            }
            attribute.initialize();
        }
    }

    public Attribute getAttribute(String name)
    {
        return attributesMap.get(name); // TODO resolveCase?
    }

    public boolean isCachedAttribute(String name)
    {
        return Optional.ofNullable(getAttribute(name)).map(attr -> attr.getCached()).orElse(false);
    }

    public ScalarAttribute getScalarAttribute(String name)
    {
        Attribute attr = getAttribute(name);
        return attr instanceof ScalarAttribute ? (ScalarAttribute)attr : null;
    }

    public RowAttribute getRowAttribute(String name)
    {
        Attribute attr = getAttribute(name);
        return attr instanceof RowAttribute ? (RowAttribute)attr : null;
    }

    public RowsetAttribute getRowsetAttribute(String name)
    {
        Attribute attr = getAttribute(name);
        return attr instanceof RowsetAttribute ? (RowsetAttribute)attr : null;
    }

    public Action getAction(String name)
    {
        Attribute attr = getAttribute(name);
        return attr instanceof Action ? (Action)attr : null;
    }

    public Serializable evaluate(String name, Serializable... params) throws SQLException
    {
       Attribute attribute = getAttribute(name);
       if (attribute == null)
       {
           throw new SQLException("unknown attribute: " + name);
       }
       if (!(attribute instanceof ScalarAttribute))
       {
           throw new SQLException("not a scalar baseAttribute: " + name);
       }
       return ((ScalarAttribute)attribute).evaluate(params);
    }

    public Serializable evaluate(String name, Map source) throws SQLException
    {
        getLogger().trace("evaluate {}.{}", logName, name);
        Attribute attribute = getAttribute(name);
        if (attribute == null)
        {
            throw new SQLException("unknown attribute: " + name);
        }
        if (!(attribute instanceof ScalarAttribute))
        {
            throw new SQLException("not a scalar attribute: " + name);
        }

        return ((ScalarAttribute)attribute).evaluate(source);
    }

    public Serializable evaluate(String name, Map source, Serializable... params) throws SQLException
    {
        getLogger().trace("evaluate {}.{}", logName, name);
        Attribute attribute = getAttribute(name);
        if (attribute == null)
        {
            throw new SQLException("unknown attribute: " + name);
        }
        if (!(attribute instanceof ScalarAttribute))
        {
            throw new SQLException("not a scalar attribute: " + name);
        }
        return ((ScalarAttribute)attribute).evaluate(source, params);
    }

    public String evaluateString(String name, Map params) throws SQLException
    {
        return TypeUtils.toString(evaluate(name, params));
    }

    public Character evaluateChar(String name, Map params) throws SQLException
    {
        return TypeUtils.toChar(evaluate(name, params));
    }

    public Boolean evaluateBoolean(String name, Map params) throws SQLException
    {
        return TypeUtils.toBoolean(evaluate(name, params));
    }

    public Byte evaluateByte(String name, Map params) throws SQLException
    {
        return TypeUtils.toByte(evaluate(name, params));
    }

    public Short evaluateShort(String name, Map params) throws SQLException
    {
        return TypeUtils.toShort(evaluate(name, params));
    }

    public Integer evaluateInteger(String name, Map params) throws SQLException
    {
        return TypeUtils.toInteger(evaluate(name, params));
    }

    public Long evaluateLong(String name, Map params) throws SQLException
    {
        return TypeUtils.toLong(evaluate(name, params));
    }

    public Float evaluateFloat(String name, Map params) throws SQLException
    {
        return TypeUtils.toFloat(evaluate(name, params));
    }

    public Double evaluateDouble(String name, Map params) throws SQLException
    {
        return TypeUtils.toDouble(evaluate(name, params));
    }

    public Date evaluateDate(String name, Map params) throws SQLException
    {
        return TypeUtils.toDate(evaluate(name, params));
    }

    public Calendar evaluateCalendar(String name, Map params) throws SQLException
    {
        return TypeUtils.toCalendar(evaluate(name, params));
    }

    public String evaluateString(String name, Serializable... params) throws SQLException
    {
        return TypeUtils.toString(evaluate(name, params));
    }

    public Character evaluateChar(String name, Serializable... params) throws SQLException
    {
        return TypeUtils.toChar(evaluate(name, params));
    }

    public Boolean evaluateBoolean(String name, Serializable... params) throws SQLException
    {
        return TypeUtils.toBoolean(evaluate(name, params));
    }

    public Byte evaluateByte(String name, Serializable... params) throws SQLException
    {
        return TypeUtils.toByte(evaluate(name, params));
    }

    public Short evaluateShort(String name, Serializable... params) throws SQLException
    {
        return TypeUtils.toShort(evaluate(name, params));
    }

    public Integer evaluateInteger(String name, Serializable... params) throws SQLException
    {
        return TypeUtils.toInteger(evaluate(name, params));
    }

    public Long evaluateLong(String name, Serializable... params) throws SQLException
    {
        return TypeUtils.toLong(evaluate(name, params));
    }

    public Float evaluateFloat(String name, Serializable... params) throws SQLException
    {
        return TypeUtils.toFloat(evaluate(name, params));
    }

    public Double evaluateDouble(String name, Serializable... params) throws SQLException
    {
        return TypeUtils.toDouble(evaluate(name, params));
    }

    public Date evaluateDate(String name, Serializable... params) throws SQLException
    {
        return TypeUtils.toDate(evaluate(name, params));
    }

    public Calendar evaluateCalendar(String name, Serializable... params) throws SQLException
    {
        return TypeUtils.toCalendar(evaluate(name, params));
    }

// ---    

    public Instance retrieve(String name, Serializable... params) throws SQLException
    {
        getLogger().trace("retrieve {}.{}", logName, name);
        Attribute attribute = getAttribute(name);
        if (attribute == null)
        {
            throw new SQLException("unknown attribute: " + name);
        }
        if (!(attribute instanceof RowAttribute))
        {
            throw new SQLException("not a row attribute: " + name);
        }
        return ((RowAttribute)attribute).retrieve(params);
    }

    public Instance retrieve(String name, Map source) throws SQLException
    {
        getLogger().trace("retrieve {}.{}", logName, name);
        Attribute attribute = getAttribute(name);
        if (attribute == null)
        {
            throw new SQLException("unknown attribute: " + name);
        }
        if (!(attribute instanceof RowAttribute))
        {
            throw new SQLException("not a row attribute: " + name);
        }
        return ((RowAttribute)attribute).retrieve(source);
    }

    public Instance retrieve(String name, Map source, Serializable... params) throws SQLException
    {
        getLogger().trace("retrieve {}.{}", logName, name);
        Attribute attribute = getAttribute(name);
        if (attribute == null)
        {
            throw new SQLException("unknown attribute: " + name);
        }
        if (!(attribute instanceof RowAttribute))
        {
            throw new SQLException("not a row attribute: " + name);
        }
        return ((RowAttribute)attribute).retrieve(source, params);
    }

    public Iterator<Instance> query(String name, Serializable... params) throws SQLException
    {
        getLogger().trace("query {}.{}", logName, name);
        Attribute attribute = getAttribute(name);
        if (attribute == null)
        {
            throw new SQLException("unknown attribute: " + name);
        }
        if (!(attribute instanceof RowsetAttribute))
        {
            throw new SQLException("not a rowset attribute: " + name);
        }
        return ((RowsetAttribute)attribute).query(params);
    }

    public Iterator<Instance> query(String name, Map source) throws SQLException
    {
        getLogger().trace("query {}.{}", logName, name);
        Attribute attribute = getAttribute(name);
        if (attribute == null)
        {
            throw new SQLException("unknown attribute: " + name);
        }
        if (!(attribute instanceof RowsetAttribute))
        {
            throw new SQLException("not a rowset attribute: " + name);
        }
        return ((RowsetAttribute)attribute).query(source);
    }

    public Iterator<Instance> query(String name, Map source, Serializable... params) throws SQLException
    {
        getLogger().trace("query {}.{}", logName, name);
        Attribute attribute = getAttribute(name);
        if (attribute == null)
        {
            throw new SQLException("unknown attribute: " + name);
        }
        if (!(attribute instanceof RowsetAttribute))
        {
            throw new SQLException("not a rowset attribute: " + name);
        }
        return ((RowsetAttribute)attribute).query(source, params);
    }

    public long perform(String name, Serializable... params) throws SQLException
    {
        getLogger().trace("perform {}.{}", logName, name);
        Attribute attribute = getAttribute(name);
        if (attribute == null)
        {
            throw new SQLException("unknown attribute: " + name);
        }
        if (!(attribute instanceof Action))
        {
            throw new SQLException("not an action attribute: " + name);
        }
        return ((Action)attribute).perform(params);
    }

    public long perform(String name, Map source) throws SQLException
    {
        getLogger().trace("perform {}.{}", logName, name);
        Attribute attribute = getAttribute(name);
        if (attribute == null)
        {
            throw new SQLException("unknown attribute: " + name);
        }
        if (!(attribute instanceof Action))
        {
            throw new SQLException("not an action attribute: " + name);
        }
        return ((Action)attribute).perform(source);
    }

    public long perform(String name, Map source, Serializable... params) throws SQLException
    {
        getLogger().trace("perform {}.{}", logName, name);
        Attribute attribute = getAttribute(name);
        if (attribute == null)
        {
            throw new SQLException("unknown attribute: " + name);
        }
        if (!(attribute instanceof Action))
        {
            throw new SQLException("not an action attribute: " + name);
        }
        return ((Action)attribute).perform(source, params);
    }

    protected Entity resolveEntity(String name)
    {
        return getModel().getEntity(name);
    }

    protected void addAttribute(Attribute attribute)
    {
        attributesMap.put(attribute.getName(), attribute);
    }

    public NavigableMap<String, Attribute> getAttributes()
    {
        return Collections.unmodifiableNavigableMap(attributesMap);
    }

    private NavigableMap<String, Attribute> attributesMap = new TreeMap<>();
}
