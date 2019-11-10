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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class TableMapper<T extends Serializable> extends Mapper<T>
{
    public TableMapper(String configurationPrefix)
    {
        super(configurationPrefix);
    }

    @Override
    protected void addEntry(String pattern, Filter<T> leaf)
    {
        if ("_".equals(pattern))
        {
            pattern = "*";
        }
        MappingEntry mappingEntry = new MappingEntry(pattern, leaf);
        addTableEntry(pattern, mappingEntry);
    }

    protected void addTableEntry(String pattern, MappingEntry mappingEntry)
    {
        MappingEntry prev = tablesMapping.put(pattern, mappingEntry);
        if (prev != null)
        {
            getLogger().warn("overriding " + getConfigurationPrefix() + ".{}", pattern);
        }
    }

    public Filter<T> getTableEntry(String table)
    {
        Filter<T> ret = null;
        for (MappingEntry mappingEntry : tablesMapping.values())
        {
            if (mappingEntry.matches(table))
            {
                if (ret == null)
                {
                    ret = mappingEntry.getLeaf();
                }
                else
                {
                    ret = composeLeaves(mappingEntry.getLeaf(), ret);
                }
            }
        }
        return ret;
    }

    protected Map<String, MappingEntry> getTablesMapping()
    {
        return tablesMapping;
    }

    private Map<String, MappingEntry> tablesMapping = new HashMap<>();
}
