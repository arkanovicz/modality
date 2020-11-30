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

import com.republicate.modality.Entity;
import com.republicate.modality.Instance;
import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.sql.RowValues;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>BaseAttribute interface</p>
 * @author Claude Brisson
 * @version $Revision: $
 * @since 3.1
 */

public abstract class BaseAttribute extends InstanceProducer implements Serializable
{
    public BaseAttribute(String name, AttributeHolder parent)
    {
        super(parent.getModel());
        this.parent = parent;
        this.attributeName = name;
    }

    protected void initialize()
    {
        if (query != null)
        {
            query = query.trim();
        }
        if (resultEntityName != null)
        {
            Entity entity = parent.resolveEntity(resultEntityName);
            if (entity == null)
            {
                throw new ConfigurationException("attribute " + getName() + " result entity not found: " + resultEntityName);
            }
            setResultEntity(entity);
        }
        if (parameterNames.size() > 0)
        {
            paramMapping = new int[parameterNames.size()];
            Map<String, Integer> paramOrders = new HashMap<String, Integer>();
            int paramIndex = 0;
            for (int i = 0; i < parameterNames.size(); ++i)
            {
                String paramName = parameterNames.get(i);
                Integer order = paramOrders.get(paramName);
                if (order == null)
                {
                    paramOrders.put(paramName, order = paramIndex++);
                }
                paramMapping[i] = order;
            }
        }

    }

    public String getName()
    {
        return attributeName;
    }

    public AttributeHolder getParent()
    {
        return parent;
    }

    protected void setResultEntity(String entityName)
    {
        this.resultEntityName = entityName;
    }

    public List<String> getParameterNames()
    {
        return Collections.unmodifiableList(parameterNames);
    }

    protected void addQueryPart(String queryPart)
    {
        query = query + queryPart;
    }

    protected void addParameter(String paramName)
    {
        parameterNames.add(paramName);
        query = query + "?";
    }

    protected boolean getCached()
    {
        return cached;
    }

    protected Serializable[] getParamValues(RowValues source) throws SQLException
    {
        Serializable[] paramValues = new Serializable[parameterNames.size()];
        for (int i = 0; i < paramValues.length; ++i)
        {
            paramValues[i] = source.get(parameterNames.get(i));
        }
        return paramValues;
    }

    protected Serializable[] getParamValues(Serializable[] rawParamValues) throws SQLException
    {
        Serializable[] paramValues = new Serializable[parameterNames.size()];
        Entity entity = (Entity)Optional.ofNullable(getParent()).filter(x -> x instanceof Entity).orElse(null);
        if (entity == null)
        {
            for (int i = 0; i < paramValues.length; ++i)
            {
                paramValues[i] = rawParamValues[paramMapping[i]];
            }
        }
        else
        {
            for (int i = 0; i < paramValues.length; ++i)
            {
                String paramName = parameterNames.get(i);
                paramValues[i] = entity.filterValue(paramName, rawParamValues[paramMapping[i]]);
            }
        }
        return paramValues;
    }

    protected Serializable[] getParamValues(Map source) throws SQLException
    {
        Serializable[] paramValues = new Serializable[parameterNames.size()];
        Entity parentEntity = (Entity)Optional.ofNullable(getParent()).filter(x -> x instanceof Entity).orElse(null);
        Entity sourceEntity = Optional.ofNullable(source).filter(x -> x instanceof Instance).map(i -> ((Instance)i).getEntity()).orElse(null);
        if (parentEntity == null && sourceEntity == null)
        {
            for (int i = 0; i < paramValues.length; ++i)
            {
                paramValues[i] = (Serializable)source.get(parameterNames.get(i));
            }
        }
        else if (parentEntity == null || sourceEntity == null || parentEntity == sourceEntity)
        {
            Entity entity = parentEntity == null ? sourceEntity : parentEntity;
            for (int i = 0; i < paramValues.length; ++i)
            {
                String paramName = parameterNames.get(i);
                paramValues[i] = entity.filterValue(paramName, (Serializable)source.get(paramName));
            }
        }
        else
        {
            for (int i = 0; i < paramValues.length; ++i)
            {
                String paramName = parameterNames.get(i);
                Serializable value = (Serializable)source.get(paramName);
                paramValues[i] = parentEntity.hasColumn(paramName) ?
                    parentEntity.filterValue(paramName, value) :
                    sourceEntity.filterValue(paramName, value);
            }
        }
        return paramValues;
    }

    private static int bitsBefore(BitSet bitset, int i)
    {
        BitSet mask = new BitSet();
        mask.set(0, i);
        mask.and(bitset);
        return mask.cardinality();
    }

    protected Serializable[] getParamValues(Map source, Serializable[] additionalParams) throws SQLException
    {
        try
        {
            Serializable[] paramValues = new Serializable[parameterNames.size()];
            Entity parentEntity = (Entity)Optional.ofNullable(getParent()).filter(x -> x instanceof Entity).orElse(null);
            Entity sourceEntity = Optional.ofNullable(source).filter(x -> x instanceof Instance).map(i -> ((Instance)i).getEntity()).orElse(null);
            BitSet sourcedParams = new BitSet();
            BitSet providedParams = new BitSet();
            if (parentEntity == null && sourceEntity == null)
            {
                for (int i = 0; i < paramValues.length; ++i)
                {
                    String paramName = parameterNames.get(i);
                    if (source.containsKey(paramName))
                    {
                        paramValues[i] = (Serializable)source.get(paramName);
                        sourcedParams.set(paramMapping[i]);
                    }
                    else
                    {
                        int paramOrder = paramMapping[i];
                        paramValues[i] = additionalParams[paramOrder - bitsBefore(sourcedParams, paramOrder)];
                        providedParams.set(paramOrder);
                    }
                }
            }
            else if (parentEntity == null || sourceEntity == null || parentEntity == sourceEntity)
            {
                Entity entity = parentEntity == null ? sourceEntity : parentEntity;
                for (int i = 0; i < paramValues.length; ++i)
                {
                    Serializable value;
                    String paramName = parameterNames.get(i);
                    if (source.containsKey(paramName))
                    {
                        value = (Serializable)source.get(paramName);
                        sourcedParams.set(paramMapping[i]);
                    }
                    else
                    {
                        int paramOrder = paramMapping[i];
                        value = additionalParams[paramOrder - bitsBefore(sourcedParams, paramOrder)];
                        providedParams.set(paramOrder);
                    }
                    paramValues[i] = entity.filterValue(paramName, value);
                }
            }
            else
            {
                for (int i = 0; i < paramValues.length; ++i)
                {
                    String paramName = parameterNames.get(i);
                    Serializable value;
                    if (source.containsKey(paramName))
                    {
                        value = (Serializable)source.get(paramName);
                        sourcedParams.set(paramMapping[i]);
                    }
                    else
                    {
                        int paramOrder = paramMapping[i];
                        value = additionalParams[paramOrder - bitsBefore(sourcedParams, paramOrder)];
                        providedParams.set(paramOrder);
                    }
                    paramValues[i] = parentEntity.hasColumn(paramName) ?
                        parentEntity.filterValue(paramName, value) :
                        sourceEntity.filterValue(paramName, value);
                }
            }
            if (providedParams.cardinality() != additionalParams.length)
            {
                throw new SQLException("too many parameters provided: got " + additionalParams.length + ", used " + providedParams.cardinality());
            }
            return paramValues;
        }
        catch (ArrayIndexOutOfBoundsException aioobe)
        {
            throw new SQLException("too few parameters provided: got " + additionalParams.length + ", needed more");
        }
    }

    public String getQuery() throws SQLException
    {
        return query;
    }

    /**
     * @since Modality 1.1
     */
    public void mergeQuery(Map<String, ?> context) throws SQLException // CB TODO - synchronizations? concurrent execution vs. merging? Escaping?
    {
        if (originalQuery == null)
        {
            synchronized (this)
            {
                if (originalQuery == null)
                {
                    originalQuery = getQuery();
                }
            }
        }
        StringBuilder mergedQuery = new StringBuilder();
        Matcher matcher = mergeLexer.matcher(originalQuery);
        int pos = 0;
        while (matcher.find())
        {
            if (matcher.start() > pos) mergedQuery.append(originalQuery.substring(pos, matcher.start()));
            String reference = matcher.group().substring(1);
            String value = String.valueOf(context.get(reference));
            if (value == null) throw new SQLException("Undefined reference: @" + reference);
            mergedQuery.append(value);
            pos = matcher.end();
        }
        mergedQuery.append(originalQuery.substring(pos));
        setQuery(mergedQuery.toString());
        initialize();
    }

    private String originalQuery = null;

    private Pattern mergeLexer = Pattern.compile("@\\w+");

    protected void setParameterNames(List<String> parameterNames)
    {
        this.parameterNames = parameterNames;
    }

    protected void setQuery(String query)
    {
        this.query = query;
    }

    protected void setCached(boolean cached)
    {
        this.cached = cached;
    }

    private boolean cached = false;
    private AttributeHolder parent = null;
    private String resultEntityName = null;
    private String attributeName = null;
    private String query = "";
    protected List<String> parameterNames = new ArrayList<>();
    protected int paramMapping[] = null;
}
