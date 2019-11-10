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
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Node : For speed considerations, matching columns are calculated for each known column at configuration time. It means that
 * unknown result set column names will only be applied the <b>default</b> column filter, aka *.*
 * @param <T>
 */

public abstract class ColumnMapper<T extends Serializable> extends TableMapper<T>
{
    public ColumnMapper(String configurationPrefix)
    {
        super(configurationPrefix);
    }

    @Override
    protected void addEntry(String key, Filter<T> leaf)
    {
        int dot = key.indexOf('.');
        if (dot == -1)
        {
            super.addEntry(key, leaf);
        }
        else
        {
            int otherDot = key.indexOf('.', dot + 1);
            if (otherDot != -1)
            {
                throw new ConfigurationException("invalid mappingEntry key: " + getConfigurationPrefix() + "." + key);
            }
            String tablePattern = key.substring(0, dot).replaceAll("\b_\b", "*");
            String columnPattern = key.substring(dot + 1).replaceAll("\b_\b", "*");
            MappingEntry mappingEntry = new MappingEntry(columnPattern, leaf);
            addColumnMapping(tablePattern, columnPattern, mappingEntry);
        }
    }

    protected void addColumnMapping(String tablePattern, String columnPattern, MappingEntry mappingEntry)
    {
        Pair<MappingEntry, Map<String, MappingEntry>> pair = columnsMapping.get(tablePattern);
        if (pair == null)
        {
            pair = Pair.of(new MappingEntry(tablePattern), new HashMap<String, MappingEntry>());
            columnsMapping.put(tablePattern, pair);
        }
        Map<String, MappingEntry> colsMap = pair.getRight();
        MappingEntry prevEntry = colsMap.put(columnPattern, mappingEntry);
        if (prevEntry != null)
        {
            getLogger().warn("overriding " + getConfigurationPrefix() + ".{}.{}", tablePattern, columnPattern);
        }

        if ("*".equals(columnPattern) && "*".equals(tablePattern))
        {
            setDefaultColumnLeaf(mappingEntry.getLeaf());
        }
    }
    
    /* needed ?
    public List<MappingEntry> getColumnsMapping(String table)
    {
        List<MappingEntry> ret = new ArrayList<>();
        for (Pair<MappingEntry, Map<String, MappingEntry>> pair : columnsMapping.values())
        {
            if (pair.getLeft().matches(table))
            {
                ret.addAll(pair.getRight().values());
            }
        }
        return ret;
    }
    */

    public Filter<T> getColumnEntry(String table, String column)
    {
        Filter<T> ret = null;
        for (MappingEntry entry : getTablesMapping().values())
        {
            if (entry.matches(table))
            {
                if (ret == null)
                {
                    ret = entry.getLeaf();
                }
                else
                {
                    ret = composeLeaves(entry.getLeaf(), ret);
                }
            }
        }
        for (Pair<MappingEntry, Map<String, MappingEntry>> pair : columnsMapping.values())
        {
            if (pair.getLeft().matches(table))
            {
                for (MappingEntry entry : pair.getRight().values())
                {
                    if (entry.matches(column))
                    {
                        if (ret == null)
                        {
                            ret = entry.getLeaf();
                        }
                        else
                        {
                            ret = composeLeaves(entry.getLeaf(), ret);
                        }
                    }
                }
            }
        }
        return ret;
    }

    public Filter<T> getDefaultColumnLeaf()
    {
        return defaultColumnLeaf;
    }

    protected void setDefaultColumnLeaf(Filter<T> defaultColumnLeaf)
    {
        this.defaultColumnLeaf = defaultColumnLeaf;
    }

    private Filter<T> defaultColumnLeaf = null;

    private Map<String, Pair<MappingEntry, Map<String, MappingEntry>>> columnsMapping = new HashMap<>();
}
