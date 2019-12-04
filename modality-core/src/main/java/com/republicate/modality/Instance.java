package com.republicate.modality;

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

import com.republicate.modality.filter.Filter;
import com.republicate.modality.filter.ValueFilters;
import com.republicate.modality.sql.RowValues;
import com.republicate.modality.util.ChainedMap;
import com.republicate.modality.util.SlotTreeMap;
import com.republicate.modality.util.TypeUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class Instance extends SlotTreeMap
{
    public Instance(Model model)
    {
        this.model = model;
        this.dirtyFlags = new BitSet();
        this.canWrite = model.getWriteAccess() != Model.WriteAccess.NONE;
    }

    protected Instance(Entity entity)
    {
        this(entity.getModel());
        this.entity = entity;
    }

    public void readValues(RowValues values) throws SQLException
    {
        ValueFilters filters = getModel().getFilters().getReadFilters();
        for (String key : values.keySet())
        {
            Serializable value = values.get(key);
            if (value != null)
            {
                value = filters.filter(value);
            }
            readValue(key, values.get(key));
        }
        setClean();
        persisted = lookupPersisted();
    }

    public void readValue(String key, Serializable value) throws SQLException
    {
        // get column if any
        String colName;
        if (entity == null)
        {
            colName = getModel().getIdentifiersFilters().transformColumnName(key);
        }
        else
        {
            colName = entity.translateColumnName(key);
            if (value != null)
            {
                Entity.Column column = entity.getColumn(colName);
                if (column != null)
                {
                    // filter value
                    value = entity.getColumn(colName).read(value);
                }
            }
        }
        // always put the value, even null
        super.put(colName, value);
    }

    public Serializable evaluate(String name, Map params) throws SQLException
    {
        return entity.evaluate(name, params == null ? (Map)this : new ChainedMap(this, params));
    }

    public Serializable evaluate(String name, Serializable... params) throws SQLException
    {
        boolean doCache = params.length == 0 && entity.isCachedAttribute(name);
        if (doCache && containsKey(name))
        {
            return get(name);
        }
        Serializable ret = entity.evaluate(name, (Map)this, params);
        if (doCache)
        {
            putImpl(name, ret);
        }
        return ret;
    }

    public Instance retrieve(String name, Map params) throws SQLException
    {
        return entity.retrieve(name, params == null ? (Map)this : new ChainedMap(this, params));
    }

    public Instance retrieve(String name, Serializable... params) throws SQLException
    {
        boolean doCache = params.length == 0 && entity.isCachedAttribute(name);
        if (doCache && containsKey(name))
        {
            return (Instance)get(name);
        }
        Instance ret = entity.retrieve(name, (Map)this, params);
        if (doCache)
        {
            putImpl(name, ret);
        }
        return ret;
    }

    public Iterator<Instance> query(String name, Map params) throws SQLException
    {
        return entity.query(name, params == null ? (Map)this : new ChainedMap(this, params));
    }

    public Iterator<Instance> query(String name, Serializable... params) throws SQLException
    {
        return entity.query(name, (Map)this, params);
    }

    public long perform(String name, Map params) throws SQLException
    {
        if (!canWrite)
        {
            throw new SQLException("instance is read-only");
        }
        return entity.perform(name, params == null ? (Map)this : new ChainedMap(this, params));
    }

    public long perform(String name, Serializable... params) throws SQLException
    {
        if (!canWrite)
        {
            throw new SQLException("instance is read-only");
        }
        return entity.perform(name, (Map)this, params);
    }

    public String getString(String name)
    {
        return TypeUtils.toString(get(name));
    }

    public Boolean getBoolean(String name)
    {
        return TypeUtils.toBoolean(get(name));
    }

    public Short getShort(String name)
    {
        return TypeUtils.toShort(get(name));
    }

    public Integer getInteger(String name)
    {
        return TypeUtils.toInteger(get(name));
    }

    public Long getLong(String name)
    {
        return TypeUtils.toLong(get(name));
    }

    public Float getFloat(String name)
    {
        return TypeUtils.toFloat(get(name));
    }

    public Double getDouble(String name)
    {
        return TypeUtils.toDouble(get(name));
    }

    public final Model getModel()
    {
        return model;
    }

    public final Entity getEntity()
    {
        return entity;
    }

    protected void setClean()
    {
        dirtyFlags.clear();
    }

    public Serializable[] getPrimaryKey()
    {
        List<Entity.Column> pk = entity.getPrimaryKey();
        if (pk == null)
        {
            return null;
        }
        Serializable[] ret = new Serializable[pk.size()];
        int col = 0;
        for (Entity.Column column : pk)
        {
            ret[col++] = get(column.name);
        }
        return ret;
    }

    public BitSet getDirtyFlags()
    {
        return dirtyFlags;
    }

    public boolean isDirty()
    {
        return dirtyFlags.cardinality() > 0;
    }

    public void refresh() throws SQLException
    {
        ensurePersisted();
        Instance myself = getEntity().fetch(getPrimaryKey());
        super.putAll(myself);
    }

    public void delete() throws SQLException
    {
        if (!canWrite)
        {
            throw new SQLException("instance is read-only");
        }
        ensurePersisted();
        entity.delete(this);
        persisted = false;
    }

    public void insert() throws SQLException
    {
        if (!canWrite)
        {
            throw new SQLException("instance is read-only");
        }
        ensureNotPersisted();
        entity.insert(this);
        persisted = lookupPersisted();
        // the next call is necessary for the following use case:
        // $book.put(...)
        // $book.delete()
        // $bok.insert()
        setClean();
    }

    public void update() throws SQLException
    {
        if (!canWrite)
        {
            throw new SQLException("instance is read-only");
        }
        ensurePersisted();
        if (!isDirty())
        {
            return;
        }
        entity.update(this);
    }

    public void upsert() throws SQLException
    {
        if (!canWrite)
        {
            throw new SQLException("instance is read-only");
        }

        Serializable[] pk;
        if (lookupPersisted())
        {
            // TODO - some vendors support upsert
            Instance prev = entity.fetch(getPrimaryKey());
            if (prev == null)
            {
                persisted = false;
                insert();
            }
            else
            {
                prev.putAll(this);
                prev.update();
                persisted = true;
                setClean();
            }
        }
        else
        {
            throw new SQLException("cannot upsert instance: no or invalid primary key: " + this);
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends Serializable> map)
    {
        Serializable[] pk = null;
        if (persisted)
        {
            pk = getPrimaryKey();
        }
        super.putAll(map);
        if (persisted)
        {
            // the persisted flag became false at this point if PK changed
            entity.getNonPrimaryKeyMask().stream()
                .filter(col ->
                {
                    String colName = entity.getColumn(col).name;
                    return !Objects.equals(get(colName), map.get(colName));
                })
                .forEach(col -> dirtyFlags.set(col));
        }
    }

    protected Serializable putImpl(String key, Serializable value)
    {
        return super.put(key, value);
    }

    @Override
    public final Serializable put(String key, Serializable value)
    {
        Serializable ret = putImpl(key, value);
        if (persisted)
        {
            Entity.Column column = entity.getColumn(key);
            if (column != null)
            {
                if (column.isKeyColumn())
                {
                    if (!TypeUtils.sameValues(ret, value))
                    {
                        persisted = false;
                    }
                }
                else
                {
                    dirtyFlags.set(column.getIndex());
                }
            }
        }
        return ret;
    }

    /**
     * Test equality of two instances.
     * @param o other instance
     * @return equality status
     */
    @Override
    public boolean equals(final Object o)
    {
        return (o instanceof Instance)
            && getEntity() == ((Instance)o).getEntity()
            && super.equals(o);
    }

    private void ensurePersisted()
    {
        if (!persisted)
        {
            throw new IllegalStateException("instance must be persisted");
        }
    }

    private void ensureNotPersisted()
    {
        if (persisted)
        {
            throw new IllegalStateException("instance must not be persisted");
        }
    }

    private boolean lookupPersisted()
    {
        List<Entity.Column> pkCols = entity == null ? null : entity.getPrimaryKey();
        return pkCols != null && pkCols.size() > 0 && pkCols.stream().noneMatch(Objects::isNull);
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        out.writeObject(model.getModelId());
        out.writeObject(entity == null ? null : entity.getName());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        String modelId = (String)in.readObject();
        String entityName = (String)in.readObject();
        ModelRepository.getModel(modelId);
        if (model == null)
        {
            // lazy initialization
            ModelRepository.addModelListener(modelId, new LazyModelSetter(entityName));
            return;
        }
        if (entityName != null)
        {
            entity = model.getEntity(entityName);
            if (entity == null)
            {
                throw new IOException("could not de-serialize instance: entity '" + entityName + "' not found");
            }
        }
    }

    protected void setModel(Model model)
    {
        this.model = model;
    }

    protected void setEntity(Entity entity)
    {
        this.entity = entity;
    }

    private transient Model model = null;

    private transient Entity entity = null;

    private BitSet dirtyFlags = null;

    private boolean canWrite = false;

    private boolean persisted = false;

    private static final long serialVersionUID = -6234576437555893893L;

    private class LazyModelSetter implements Consumer<Model>
    {
        public LazyModelSetter(String entityName)
        {
            this.entityName = entityName;
        }

        @Override
        public void accept(Model model)
        {
            setModel(model);
            if (entityName != null)
            {
                setEntity(model.getEntity(entityName));
            }
        }

        private String entityName;
    }
}
