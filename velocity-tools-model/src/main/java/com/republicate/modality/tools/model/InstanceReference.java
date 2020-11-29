package com.republicate.modality.tools.model;

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

import com.republicate.modality.Instance;
import com.republicate.modality.Model;
import com.republicate.modality.util.SlotMap;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class InstanceReference implements Reference, SlotMap
{
    public InstanceReference(Instance instance, ModelTool modelReference)
    {
        this.instance = instance;
        this.modelReference = modelReference;
        this.canWrite = modelReference.getModel().getWriteAccess() == Model.WriteAccess.VTL;
    }

    @Override
    public int size()
    {
        return instance.size();
    }

    @Override
    public boolean isEmpty()
    {
        return instance.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return instance.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return instance.containsValue(value);
    }

    @Override
    public Serializable get(Object key)
    {
        return instance.get(key);
    }

    @Override
    public Serializable put(String key, Serializable value)
    {
        return instance.put(key, value);
    }

    @Override
    public Serializable remove(Object key)
    {
        return instance.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Serializable> m)
    {
        instance.putAll(m);
    }

    @Override
    public void clear()
    {
        instance.clear();
    }

    @Override
    public Set<String> keySet()
    {
        return instance.keySet();
    }

    @Override
    public Collection<Serializable> values()
    {
        return instance.values();
    }

    @Override
    public Set<Entry<String, Serializable>> entrySet()
    {
        return instance.entrySet();
    }

    public Serializable evaluate(String name, Map params)
    {
        try
        {
            return instance.evaluate(name, params);
        }
        catch (SQLException sqle)
        {
            error("could not evaluate instance property {}.{}", instance.getEntity().getName(), name, sqle);
            return null;
        }
    }

    public Serializable evaluate(String name, Serializable... params)
    {
        try
        {
            return instance.evaluate(name, params);
        }
        catch (SQLException sqle)
        {
            error("could not evaluate instance property {}.{}", instance.getEntity().getName(), name, sqle);
            return null;
        }
    }

    public InstanceReference retrieve(String name, Map params)
    {
        try
        {
            Instance inst = instance.retrieve(name, params);
            return inst == null ? null : modelReference.createInstanceReference(inst);
        }
        catch (SQLException sqle)
        {
            error("could not retrieve instance property {}.{}", instance.getEntity().getName(), name, sqle);
            return null;
        }
    }

    public InstanceReference retrieve(String name, Serializable... params)
    {
        try
        {
            Instance inst = instance.retrieve(name, params);
            return inst == null ? null : modelReference.createInstanceReference(inst);
        }
        catch (SQLException sqle)
        {
            error("could not retrieve instance property {}.{}", instance.getEntity().getName(), name, sqle);
            return null;
        }
    }

    public Iterator<InstanceReference> query(String name, Map params)
    {
        try
        {
            return modelReference.createInstanceReferenceIterator(instance.query(name, params));
        }
        catch (SQLException sqle)
        {
            error("could not query instance property {}.{}", instance.getEntity().getName(), name, sqle);
            return null;
        }
    }

    public Iterator<InstanceReference> query(String name, Serializable... params)
    {
        try
        {
            return modelReference.createInstanceReferenceIterator(instance.query(name, params));
        }
        catch (SQLException sqle)
        {
            error("could not query instance property {}.{}", instance.getEntity().getName(), name, sqle);
            return null;
        }
    }

    public long perform(String name, Map params)
    {
        if (canWrite)
        {
            return performImpl(name, params);
        }
        else
        {
            error("instance is read-only");
            return 0;
        }
    }

    protected long performImpl(String name, Map params)
    {
        try
        {
            return instance.perform(name, params);
        }
        catch (SQLException sqle)
        {
            error("could not perform instance action {}.{}", instance.getEntity().getName(), name, sqle);
            return 0;
        }
    }

    public long perform(String name, Serializable... params)
    {
        if (canWrite)
        {
            return performImpl(name, params);
        }
        else
        {
            error("instance is read-only");
            return 0;
        }
    }

    protected long performImpl(String name, Serializable... params)
    {
        try
        {
            return instance.perform(name, params);
        }
        catch (SQLException sqle)
        {
            error("could not perform instance action {}.{}", instance.getEntity().getName(), name, sqle);
            return 0;
        }
    }

    public boolean delete()
    {
        if (canWrite)
        {
            return deleteImpl();
        }
        else
        {
            error("cannot delete read-only instance");
            return false;
        }
    }

    protected boolean deleteImpl()
    {
        try
        {
            instance.delete();
            return true;
        }
        catch (SQLException sqle)
        {
            error("could not delete instance", sqle);
            return false;
        }
    }

    public boolean insert()
    {
        if (canWrite)
        {
            return insertImpl();
        }
        else
        {
            error("cannot insert read-only instance");
            return false;
        }
    }

    protected boolean insertImpl()
    {
        try
        {
            instance.insert();
            return true;
        }
        catch (SQLException sqle)
        {
            error("could not insert instance", sqle);
            return false;
        }
    }

    public boolean update()
    {
        if (canWrite)
        {
            return updateImpl();
        }
        else
        {
            error("cannot update read-only instance");
            return false;
        }
    }

    protected boolean updateImpl()
    {
        try
        {
            instance.update();
            return true;
        }
        catch (SQLException sqle)
        {
            error("could not update instance", sqle);
            return false;
        }
    }

    public boolean upsert()
    {
        if (canWrite)
        {
            return upsertImpl();
        }
        else
        {
            error("cannot insert read-only instance");
            return false;
        }
    }

    protected boolean upsertImpl()
    {
        try
        {
            instance.upsert();
            return true;
        }
        catch (SQLException sqle)
        {
            error("could not upsert instance", sqle);
            return false;
        }
    }

    @Override
    public ModelTool getModelTool()
    {
        return modelReference;
    }

    @Override
    public String toString()
    {
        return instance.toString();
    }

    public boolean isDirty()
    {
        return instance.isDirty();
    }
    
    protected Instance getInstance()
    {
        return instance;
    }

    private Instance instance;

    private ModelTool modelReference;

    private boolean canWrite;
}
