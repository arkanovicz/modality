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
import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.filter.Filter;
import com.republicate.modality.sql.SqlUtils;
import com.republicate.modality.util.Converter;
import org.apache.commons.lang3.tuple.Pair;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class BaseEntity extends AttributeHolder
{
    public BaseEntity(String name, Model model)
    {
        this.model = model;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String getTable() { return sqlName; }

    public void setTable(String table)
    {
        this.sqlName = table;
    }

    protected void addColumn(String name, String sqlName,  int type, Integer size, boolean generated) throws SQLException
    {
        addColumn(new Entity.Column(name, sqlName, type, size, generated));
    }

    protected void addColumn(Entity.Column column) throws SQLException
    {
        Entity.Column previous = columns.put(column.name, column);
        if (previous != null)
        {
            throw new ConfigurationException("column name collision: " + getName() + "." + column.name + " mapped on " + getTable() + "." + previous.sqlName + " and on " + getTable() + "." + column.sqlName);
        }
        column.setIndex(columns.size() - 1);
        Optional.ofNullable(getModel().getFilters().getReadFilters().getColumnEntry(sqlName, column.sqlName)).ifPresent(filter -> column.setReadFilter(filter));
        Optional.ofNullable(getModel().getFilters().getWriteFilters().getColumnEntry(sqlName, column.sqlName)).ifPresent(filter -> column.setWriteFilter(filter));
        columnsMapping.put(column.sqlName, column.name);
    }

    public List<Entity.Column> getPrimaryKey()
    {
        return Collections.unmodifiableList(primaryKey);
    }

    public boolean hasPrimaryKey()
    {
        return primaryKey != null && primaryKey.size() > 0;
    }

    public BitSet getPrimaryKeyMask()
    {
        return (BitSet)primaryKeyMask.clone();
    }

    public BitSet getNonKeyMask()
    {
        BitSet ret = (BitSet)primaryKeyMask.clone();
        ret.flip(0, columns.size());
        return ret;
    }

    protected List<String> getSqlPrimaryKey()
    {
        return sqlPrimaryKey != null ? Collections.unmodifiableList(sqlPrimaryKey) : null;
    }

    protected void setSqlPrimaryKey(String ... sqlPrimaryKey) // receives sql names
    {
        this.sqlPrimaryKey = Arrays.asList(sqlPrimaryKey);

        // now we can initialize entity internal attributes
        initialize();
    }

    public Collection<Entity.Column> getColumns()
    {
        return Collections.unmodifiableCollection(columns.values());
    }

    public List<String> getColumnNames()
    {
        return Collections.unmodifiableList(columnNames);
    }

    public Entity.Column getColumn(int index)
    {
        return columns.get(columnNames.get(index));
    }

    public Entity.Column getColumn(String columnName)
    {
        return columns.get(columnName);
    }

    public String getColumnName(int index)
    {
        return columnNames.get(index);
    }

    public String translateColumnName(String sqlColumnName)
    {
        String ret = columnsMapping.get(sqlColumnName);
        if (ret == null)
        {
            try
            {
                ret = getModel().getIdentifiers().getDefaultColumnLeaf().apply(sqlColumnName);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        return ret;
    }

    public void delete(Map source) throws SQLException
    {
        delete.perform(source);
    }

    public void insert(Map source) throws SQLException
    {
        insert.perform(source);
    }

    public void update(Map source) throws SQLException
    {
        update.perform(source);
    }

    public Model getModel()
    {
        return model;
    }

    public Instance newInstance()
    {
        return instanceBuilder.create();
    }

    public Instance newInstance(Map values)
    {
        Instance instance = instanceBuilder.create();
        instance.putAll(values);
        return instance;
    }

    protected Map<String, Method> getWrappedInstanceGetters()
    {
        return wrappedInstanceGetters;
    }

    protected Map<String, Pair<Method, Class>> getWrappedInstanceSetters()
    {
        return wrappedInstanceSetters;
    }

    /**
     * initialization
     */

    protected void initialize()
    {
        initializeAttributes();
        // TODO - lazy initialization
        if (sqlName == null)
        {
            sqlName = name;
        }
        columnNames = columns.values().stream().map(x -> x.name).collect(Collectors.toList());

        String tableIdentifier = quoteIdentifier(getTable());

        iterateAttribute = new RowsetAttribute("iterate", this);
        iterateAttribute.setResultEntity((Entity)this);
        iterateAttribute.addQueryPart("SELECT * FROM " + tableIdentifier);
        iterateAttribute.initialize();

        countAttribute = new ScalarAttribute("getCount", this);
        countAttribute.addQueryPart("SELECT COUNT(*) FROM " + tableIdentifier);
        countAttribute.initialize();

        if (sqlPrimaryKey != null && sqlPrimaryKey.size() > 0)
        {
            primaryKey = getSqlPrimaryKey().stream().map(sql -> columns.get(columnsMapping.get(sql))).collect(Collectors.toList());
            primaryKeyMask = new BitSet();
            primaryKey.stream().forEach(col -> { col.setKeyColumn(); primaryKeyMask.set(col.getIndex()); });

            fetchAttribute = new RowAttribute("retrieve", this);
            fetchAttribute.setResultEntity((Entity)this);
            fetchAttribute.addQueryPart("SELECT * FROM " + tableIdentifier + " WHERE ");
            addKeyMapToAttribute(fetchAttribute);
            fetchAttribute.initialize();

            delete = new Action("delete", this);
            delete.addQueryPart("DELETE FROM " + tableIdentifier + " WHERE ");
            addKeyMapToAttribute(delete);
            delete.initialize();

            update = new UpdateAction(this);
            update.addQueryPart("UPDATE " + tableIdentifier + " SET ");
            update.addParameter(UpdateAction.DYNAMIC_PART);
            update.addQueryPart(" WHERE ");
            addKeyMapToAttribute(update);
            update.initialize();

            insert = new Action("insert", this);
            insert.addQueryPart("INSERT INTO " + tableIdentifier + "(");
            int nonGeneratedColumns = 0;
            List<String> params = new ArrayList<>();
            for (Entity.Column column : columns.values())
            {
                if (!column.generated)
                {
                    if (nonGeneratedColumns++ > 0)
                    {
                        insert.addQueryPart(", ");
                    }
                    insert.addQueryPart(quoteIdentifier(column.sqlName));
                    params.add(column.name);
                }
            }
            insert.addQueryPart(") VALUES (");
            int col = 0;
            for (String param : params)
            {
                if (col++ > 0)
                {
                    insert.addQueryPart(", ");
                }
                insert.addParameter(param);
            }
            insert.addQueryPart(")");
            insert.initialize();
        }
    }

    private void addKeyMapToAttribute(Attribute attribute)
    {
        for (int i = 0; i < sqlPrimaryKey.size(); ++i)
        {
            if (i > 0)
            {
                attribute.addQueryPart(" AND ");
            }
            attribute.addQueryPart(quoteIdentifier(sqlPrimaryKey.get(i)) + " = ");
            attribute.addParameter(getColumn(i).name);
        }
    }

    protected void declareUpstreamJoin(String upstreamAttributeName, Entity pkEntity, List<String> fkColumns)
    {
        Attribute upstreamAttribute = new RowAttribute(upstreamAttributeName, this);
        upstreamAttribute.setResultEntity(pkEntity);
        List<String> pkColumns = pkEntity.getSqlPrimaryKey();
        upstreamAttribute.addQueryPart("SELECT * FROM " + quoteIdentifier(pkEntity.getTable()) + " WHERE ");
        for (int col = 0; col < pkColumns.size(); ++ col)
        {
            if (col > 0)
            {
                upstreamAttribute.addQueryPart(" AND ");
            }
            upstreamAttribute.addQueryPart(quoteIdentifier(pkColumns.get(col)) + " = ");
            upstreamAttribute.addParameter(translateColumnName(fkColumns.get(col)));
        }
        addAttribute(upstreamAttribute);
    }

    public void declareDownstreamJoin(String downstreamAttributeName, Entity fkEntity, List<String> fkColumns)
    {
        Attribute downstreamAttribute = new RowsetAttribute(downstreamAttributeName, this);
        downstreamAttribute.setResultEntity(fkEntity);
        downstreamAttribute.addQueryPart("SELECT * FROM " + quoteIdentifier(fkEntity.getTable()) + " WHERE ");
        for (int col = 0; col < sqlPrimaryKey.size(); ++ col)
        {
            if (col > 0)
            {
                downstreamAttribute.addQueryPart(" AND ");
            }
            downstreamAttribute.addQueryPart(quoteIdentifier(fkColumns.get(col)) + " = ");
            downstreamAttribute.addParameter(translateColumnName(sqlPrimaryKey.get(col)));
        }
        addAttribute(downstreamAttribute);
    }

    public void declareExtendedJoin(String joinAttributeName, List<String> leftFKCols, Entity joinEntity, List<String> rightFKCols, Entity rightEntity)
    {
        List<String> rightPK = rightEntity.getSqlPrimaryKey();
        Attribute extendedJoin = new RowsetAttribute(joinAttributeName, this);
        extendedJoin.setResultEntity(rightEntity);
        extendedJoin.addQueryPart("SELECT " + quoteIdentifier(rightEntity.getTable()) + ".* FROM " +
            quoteIdentifier(joinEntity.getTable()) + "JOIN " + quoteIdentifier(rightEntity.getTable()) + " ON ");
        for (int col = 0; col < rightPK.size(); ++col)
        {
            if (col > 0)
            {
                extendedJoin.addQueryPart(" AND ");
            }
            extendedJoin.addQueryPart(quoteIdentifier(rightEntity.getTable()) + "." + quoteIdentifier(rightPK.get(col)) + " = " + quoteIdentifier(joinEntity.getTable()) + "." + quoteIdentifier(rightFKCols.get(col)));
        }
        extendedJoin.addQueryPart(" WHERE ");
        for (int col = 0; col < sqlPrimaryKey.size(); ++col)
        {
            if (col > 0)
            {
                extendedJoin.addQueryPart(" AND ");
            }
            extendedJoin.addQueryPart(quoteIdentifier(leftFKCols.get(col)) + " = ");
            extendedJoin.addParameter(translateColumnName(sqlPrimaryKey.get(col)));
        }
        addAttribute(extendedJoin);
    }

    protected final String quoteIdentifier(String id)
    {
        return getModel().quoteIdentifier(id);
    }

    protected final Serializable filterValue(String columnName, Serializable value) throws SQLException
    {
        if (value != null)
        {
            Filter<Serializable> typeFilter = getModel().getFilters().getWriteFilters().getTypeEntry(value.getClass());
            if (typeFilter != null)
            {
                value = typeFilter.apply(value);
            }
            Column column = getColumn(columnName);
            if (column != null)
            {
                value = column.write(value);
                if (getModel().getDriverInfos().isStrictColumnTypes())
                {
                    Class clazz = SqlUtils.getSqlTypeClass(column.type);
                    if (clazz == null)
                    {
                        throw new SQLException("unhandled SQL type: " + column.type);
                    }
                    Converter converter = getModel().getConversionHandler().getNeededConverter(clazz, value.getClass());
                    if (converter != null)
                    {
                        value = converter.convert(value);
                    }
                }
            }
        }
        return value;
    }

    public boolean hasColumn(String key)
    {
        return columns.containsKey(key);
    }

    protected void setInstanceBuilder(InstanceBuilder builder)
    {
        setInstanceBuilder(builder, null);
    }

    protected void setInstanceBuilder(InstanceBuilder builder, PropertyDescriptor[] properties)
    {
        this.instanceBuilder = builder;
        if (properties != null)
        {
            wrappedInstanceGetters = new HashMap<String, Method>();
            wrappedInstanceSetters = new HashMap<String, Pair<Method, Class>>();
            for (PropertyDescriptor descriptor : properties)
            {
                String name = descriptor.getName();
                Optional.ofNullable(descriptor.getReadMethod()).ifPresent(getter -> wrappedInstanceGetters.put(name, getter));
                Optional.ofNullable(descriptor.getWriteMethod()).ifPresent(setter -> wrappedInstanceSetters.put(name, Pair.of(setter, setter.getParameterTypes()[0])));
            }
        }
    }

    protected ScalarAttribute getCountAttribute()
    {
        return countAttribute;
    }
    protected RowAttribute getFetchAttribute()
    {
        return fetchAttribute;
    }
    protected RowsetAttribute getIterateAttribute()
    {
        return iterateAttribute;
    }
    private String name = null;
    private String sqlName = null;
    private Model model = null;
    private Map<String, String> columnsMapping = new HashMap<>();
    private LinkedHashMap<String, Entity.Column> columns = new LinkedHashMap<>();
    private List<String> columnNames = null; // redundant with 'columns' field, but needed for random access

    private List<String> sqlPrimaryKey = null;
    private List<Entity.Column> primaryKey = null;
    private BitSet primaryKeyMask = null;
    private ScalarAttribute countAttribute = null;
    private RowAttribute fetchAttribute = null;
    private RowsetAttribute iterateAttribute = null;

    private Action delete = null;

    private Action insert = null;

    private Action update = null;

    private InstanceBuilder instanceBuilder = null;

    private Map<String, Method> wrappedInstanceGetters = null;

    private Map<String, Pair<Method, Class>> wrappedInstanceSetters = null;

    @FunctionalInterface
    protected interface InstanceBuilder
    {
        Instance create();
    }

    public static class Column
    {
        public Column(String name, String sqlName, int type, Integer size, boolean generated)
        {
            this.name = name;
            this.sqlName = sqlName;
            this.type = type;
            this.size = size;
            this.generated = generated;
        }

        public int getIndex()
        {
            return index;
        }

        protected void setIndex(int index)
        {
            this.index = index;
        }

        /**
         * Whether column is the primary key, or part of the primary key
         * @return
         */
        public boolean isKeyColumn()
        {
            return keyColumn;
        }

        protected void setKeyColumn()
        {
            keyColumn = true;
        }

        protected void setReadFilter(Filter<Serializable> readFilter)
        {
            this.readFilter = readFilter;
        }

        protected void setWriteFilter(Filter<Serializable> writeFilter)
        {
            this.writeFilter = writeFilter;
        }

        public final Serializable read(Serializable value) throws SQLException
        {
            return readFilter.apply(value);
        }

        public final Serializable write(Serializable value) throws SQLException
        {
            return writeFilter.apply(value);
        }

        public final String name;
        public final String sqlName;
        public final int type;
        public final Integer size;
        public final boolean generated;
        private int index = -1;
        private boolean keyColumn = false;
        private Filter<Serializable> readFilter = Filter.identity();
        private Filter<Serializable> writeFilter = Filter.identity();
    }
}
