package com.republicate.modality.filter;

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

import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.util.GlobToRegex;
import org.apache.velocity.tools.ClassUtils;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class Mapper<T extends Serializable>
{
    public Mapper(String configurationPrefix)
    {
        this.configurationPrefix = configurationPrefix;
    }

    protected abstract Logger getLogger();

    public void initialize() throws ConfigurationException
    {
    }

    protected Filter<T> valueToLeaf(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof String)
        {
            Filter<T> ret = null;
            String[] leaves = ((String)value).split(",\\s*");
            for (String strLeaf : leaves)
            {
                Filter<T> leaf = stringToLeaf(strLeaf);
                ret = ret == null ? leaf : composeLeaves(leaf, ret);
            }
            return ret;
        }
        try
        {
            return (Filter<T>)value;
        }
        catch (ClassCastException cce)
        {
            throw new ConfigurationException("cannot convert mapped value to proper type", cce);
        }
    }

    protected Filter<T> stringToLeaf(String value)
    {
        Filter<T> ret = getStockObject(value);
        if (ret == null)
        {
            ret = classnameToLeaf(value);
        }
        return ret;
    }

    protected Filter<T> classnameToLeaf(String clazz)
    {
        try
        {
            T ret = null;
            Class leafClass = ClassUtils.getClass(clazz);
            return classToLeaf(leafClass);
        }
        catch (ClassNotFoundException e)
        {
            throw new ConfigurationException(getConfigurationPrefix() + ": could not find class " + clazz);
        }
    }

    protected Filter<T> classToLeaf(Class leafClass)
    {
        try
        {
            Object obj = leafClass.newInstance();
            return newObjectToLeaf(obj);
        }
        catch (IllegalAccessException | InstantiationException e)
        {
            throw new ConfigurationException(getConfigurationPrefix() + " : could not instanciate class " + leafClass.getName());
        }
    }

    protected Filter<T> newObjectToLeaf(Object obj)
    {
        try
        {
            return (Filter<T>)obj;
        }
        catch (ClassCastException cce)
        {
            throw new ConfigurationException(getConfigurationPrefix() + ": unexpected object class", cce);
        }
    }

    protected abstract Filter<T> composeLeaves(Filter<T> left, Filter<T> right);

    protected abstract void addEntry(String key, Filter<T> leaf);

    public void setMapping(String value)
    {
        Map map = new HashMap();
        String[] parts = value.split(",");
        for (String part : parts)
        {
            int eq = part.indexOf('=');
            if (eq == -1)
            {
                // a single default ?
                Filter<T> leaf = stringToLeaf(part);
                map.put("*", leaf);
                map.put("*.*", leaf);
            }
            else
            {
                String key = part.substring(0, eq);
                String val = part.substring(eq +  1);
                map.put(key, val);
            }
        }
        setMapping(map);
    }

    public void setMapping(Map map)
    {
        if (map == null)
        {
            return;
        }
        for (Map.Entry entry : (Set<Map.Entry>)map.entrySet())
        {
            String key = (String)entry.getKey();
            Filter<T> leaf = valueToLeaf(entry.getValue());
            addEntry(key, leaf);
        }
    }

    public class MappingEntry
    {
        MappingEntry(String pattern)
        {
            this(pattern, null);
        }

        MappingEntry(String pattern, Filter<T> value)
        {
            this.pattern = Pattern.compile(GlobToRegex.toRegex(pattern, "."), Pattern.CASE_INSENSITIVE);
            this.leaf = value;
        }

        public Filter<T> getLeaf()
        {
            return leaf;
        }

        public void setLeaf(Filter<T> leaf)
        {
            this.leaf = leaf;
        }

        public boolean matches(String name)
        {
            return pattern.matcher(name).matches();
        }

        private Pattern pattern;
        private Filter<T> leaf;
    }

    protected String getConfigurationPrefix()
    {
        return configurationPrefix;
    }

    protected void addStockObject(String key, Filter<T> object)
    {
        stockObjects.put(key, object);
    }

    protected Filter<T> getStockObject(String key)
    {
        return stockObjects.get(key);
    }

    private String configurationPrefix;

    private Map<String, Filter<T>> stockObjects = new HashMap<>();

}
