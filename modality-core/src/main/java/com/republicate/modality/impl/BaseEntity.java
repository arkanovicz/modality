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
import java.sql.Types;
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
        super(name);
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

    protected void addColumn(String name, String sqlName,  int type, Integer size, boolean generated, String typeName) throws SQLException
    {
        addColumn(new Entity.Column(name, sqlName, type, size, generated, typeName));
    }

    protected void addColumn(Entity.Column column) throws SQLException
    {
        Entity.Column previous = columns.put(column.name, column);
        if (previous != null)
        {
            throw new ConfigurationException("column name collision: " + getName() + "." + column.name + " mapped on " + getTable() + "." + previous.sqlName + " and on " + getTable() + "." + column.sqlName);
        }
        column.setIndex(columns.size() - 1);
        column.setReadFilter(getModel().getFilters().getReadFilters().getColumnFilter(sqlName, column.sqlName));
        column.setWriteFilter(getModel().getFilters().getWriteFilters().getColumnFilter(sqlName, column.sqlName));
        columnsMapping.put(column.sqlName, column.name);
    }

    public List<Entity.Column> getPrimaryKey()
    {
        return primaryKey == null ? null : Collections.unmodifiableList(primaryKey);
    }

    public boolean hasPrimaryKey()
    {
        return primaryKey != null && primaryKey.size() > 0;
    }

    public BitSet getPrimaryKeyMask()
    {
        return (BitSet)primaryKeyMask.clone();
    }

    public BitSet getNonPrimaryKeyMask()
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
                ret = getModel().getIdentifiersFilters().getDefaultColumnFilter().apply(sqlColumnName);
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
        BitSet fieldsMask = new BitSet();
        for (int c = 0; c < columnNames.size(); ++c)
        {
            if (source.containsKey(columnNames.get(c)))
            {
                fieldsMask.set(c);
            }
        }
        Action insert = insertPerColumnsMask.computeIfAbsent(fieldsMask, this::generateInsertAction);
        long ret = insert.perform(source);
        boolean used = false;
        if (primaryKey != null && primaryKey.size() > 0)
        {
            for (int i = 0; i < primaryKey.size(); ++i)
            {
                Column keyColumn = primaryKey.get(i);
                if (keyColumn.generated)
                {
                    if (used)
                    {
                        throw new SQLException("several generated keys not supported");
                    }
                    source.put(keyColumn.name, ret);
                    used = true;
                }
            }
        }
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
        if (sqlName == null)
        {
            // entity is not a table
            return;
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
            primaryKey = getSqlPrimaryKey().stream().map(sql -> {
                String colName = columnsMapping.get(sql);
                if (colName == null)
                {
                    getLogger().warn("sqlPrimaryKey column '{}' not found in column mappings for entity {}", sql, getName());
                    return null;
                }
                Column col = columns.get(colName);
                if (col == null)
                {
                    getLogger().warn("sqlPrimaryKey column '{}' (mapped to '{}') not found in columns for entity {}", sql, colName, getName());
                }
                return col;
            }).collect(Collectors.toList());
            if (primaryKey.contains(null))
            {
                getLogger().error("entity {} has invalid sqlPrimaryKey configuration - some columns are missing", getName());
                primaryKey = null;
                return;
            }
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
        }
    }

    private Action generateInsertAction(BitSet columnMask)
    {
        Action insert = new Action("insert", this);
        insert.addQueryPart("INSERT INTO " + quoteIdentifier(getTable()) + "(");
        List<String> params = new ArrayList<>();
        int col = 0;
        for (int i = columnMask.nextSetBit(0); i >= 0; i = columnMask.nextSetBit(i+1))
        {
            Entity.Column column = columns.get(columnNames.get(i));
            if (col++ > 0)
            {
                insert.addQueryPart(", ");
            }
            insert.addQueryPart(quoteIdentifier(column.sqlName));
            params.add(column.name);
        }
        insert.addQueryPart(") VALUES (");
        col = 0;
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
        if (primaryKey.size() == 1 && primaryKey.get(0).generated)
        {
            insert.setGeneratedKeyColumn(sqlPrimaryKey.get(0));
        }
        return insert;
    }

    private void addKeyMapToAttribute(Attribute attribute)
    {
        for (int i = 0; i < sqlPrimaryKey.size(); ++i)
        {
            if (i > 0)
            {
                attribute.addQueryPart(" AND ");
            }
            String sqlPKColName = sqlPrimaryKey.get(i);
            attribute.addQueryPart(quoteIdentifier(sqlPKColName) + " = ");
            attribute.addParameter(translateColumnName(sqlPKColName));
            Column col = getColumn(sqlPKColName);
            if (model.getDriverInfos().isStrictColumnTypes() && model.getDriverInfos().hasColumnMarkers() && !"serial".equals(col.typeName))
            {
                attribute.addQueryPart("::" + col.typeName);
            }
        }
    }

    protected void declareUpstreamJoin(String upstreamAttributeName, Entity pkEntity, List<String> fkColumns)
    {
        List<String> pkColumns = pkEntity.getSqlPrimaryKey();
        if (pkColumns == null || pkColumns.isEmpty())
        {
            getLogger().warn("cannot declare upstream join {}.{}: target entity {} has no primary key", getName(), upstreamAttributeName, pkEntity.getName());
            return;
        }
        if (pkEntity.getTable() == null)
        {
            getLogger().warn("cannot declare upstream join {}.{}: target entity {} has no table", getName(), upstreamAttributeName, pkEntity.getName());
            return;
        }
        Attribute upstreamAttribute = new RowAttribute(upstreamAttributeName, this);
        upstreamAttribute.setResultEntity(pkEntity);
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
        upstreamAttribute.initialize();
    }

    public void declareDownstreamJoin(String downstreamAttributeName, Entity fkEntity, List<String> fkColumns)
    {
        if (sqlPrimaryKey == null || sqlPrimaryKey.isEmpty())
        {
            getLogger().warn("cannot declare downstream join {}.{}: source entity has no primary key", getName(), downstreamAttributeName);
            return;
        }
        if (fkEntity.getTable() == null)
        {
            getLogger().warn("cannot declare downstream join {}.{}: target entity {} has no table", getName(), downstreamAttributeName, fkEntity.getName());
            return;
        }
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
        downstreamAttribute.initialize();
    }

    public void declareExtendedJoin(String joinAttributeName, List<String> leftFKCols, Entity joinEntity, List<String> rightFKCols, Entity rightEntity)
    {
        List<String> rightPK = rightEntity.getSqlPrimaryKey();
        if (rightPK == null || rightPK.isEmpty())
        {
            getLogger().warn("cannot declare extended join {}.{}: right entity {} has no primary key", getName(), joinAttributeName, rightEntity.getName());
            return;
        }
        if (sqlPrimaryKey == null || sqlPrimaryKey.isEmpty())
        {
            getLogger().warn("cannot declare extended join {}.{}: left entity has no primary key", getName(), joinAttributeName);
            return;
        }
        if (rightEntity.getTable() == null || joinEntity.getTable() == null)
        {
            getLogger().warn("cannot declare extended join {}.{}: join or right entity has no table", getName(), joinAttributeName);
            return;
        }
        Attribute extendedJoin = new RowsetAttribute(joinAttributeName, this);
        extendedJoin.setResultEntity(rightEntity);
        extendedJoin.addQueryPart("SELECT " + quoteIdentifier(rightEntity.getTable()) + ".* FROM " +
            quoteIdentifier(joinEntity.getTable()) + " JOIN " + quoteIdentifier(rightEntity.getTable()) + " ON ");
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
        extendedJoin.initialize();
    }

    protected final String quoteIdentifier(String id)
    {
        return getModel().quoteIdentifier(id);
    }

    protected final Serializable filterValue(String columnName, Serializable value) throws SQLException
    {
        if (value != null)
        {
            value = getModel().getFilters().getWriteFilters().filter(value);
            Column column = getColumn(columnName);
            if (column != null)
            {
                value = column.write(value);
                if (getModel().getDriverInfos().isStrictColumnTypes())
                {
                    Class formalClass = SqlUtils.getSqlTypeClass(column.type);
                    if (formalClass == null)
                    {
                        getLogger().warn("unhandled SQL type (falling back to String): {} ", column.typeName);
                        formalClass = String.class;
                    }
                    Class actualClass = value.getClass();
                    if (formalClass != actualClass)
                    {
                        Converter converter = getModel().getConversionHandler().getNeededConverter(formalClass, value.getClass());
                        if (converter != null)
                        {
                            value = converter.convert(value);
                        }
                    }
                    // special mappings
                    if (column.type == Types.OTHER)
                    {
                        switch (getModel().getDriverInfos().getTag())
                        {
                            case "postgresql":
                                value = SqlUtils.getPGObject(column.typeName, value);
                                break;
                        }
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
    private Action update = null;
    private Map<BitSet, Action> insertPerColumnsMask = new HashMap<BitSet, Action>();

    private InstanceBuilder instanceBuilder = null;

    private Map<String, Method> wrappedInstanceGetters = null;

    private Map<String, Pair<Method, Class>> wrappedInstanceSetters = null;

    @FunctionalInterface
    protected interface InstanceBuilder
    {
        Instance create();
    }

    public static class Column implements Serializable
    {
        public Column(String name, String sqlName, int type, Integer size, boolean generated, String typeName)
        {
            this.name = name;
            this.sqlName = sqlName;
            this.type = type;
            this.typeName = typeName;
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
            return value == null ? null : readFilter.apply(value);
        }

        public final Serializable write(Serializable value) throws SQLException
        {
            return writeFilter.apply(value);
        }

        public final String name;
        public final String sqlName;
        public final int type;
        public final String typeName;
        public final Integer size;
        public final boolean generated;
        private int index = -1;
        private boolean keyColumn = false;
        private Filter<Serializable> readFilter = Filter.identity();
        private Filter<Serializable> writeFilter = Filter.identity();
    }
}
