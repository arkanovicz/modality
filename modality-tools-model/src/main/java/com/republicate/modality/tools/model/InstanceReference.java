package com.republicate.modality.tools.model;

import com.republicate.modality.Instance;
import com.republicate.modality.util.SlotMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class InstanceReference extends Reference implements SlotMap
{
    public InstanceReference(Instance instance, ModelTool modelReference)
    {
        this.instance = instance;
        this.modelReference = modelReference;
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
        error("cannot change read-only instance");
        return null;
    }

    protected Serializable putImpl(String key, Serializable value)
    {
        try
        {
            return instance.put(key, value);

        }
        catch (Exception e)
        {
            error("could not set instance field {} to value {}", key, String.valueOf(value));
            return null;
        }
    }

    @Override
    public Serializable remove(Object key)
    {
        error("cannot change read-only instance");
        return null;
    }

    protected Serializable removeImpl(Object key)
    {
        return instance.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Serializable> m)
    {
        error("cannot change read-only instance");
    }

    public void putAllImpl(Map<? extends String, ? extends Serializable> m)
    {
        try
        {
            instance.putAll(m);
        }
        catch (Exception e)
        {
            error("could not set instance fields to values {}", m, e);
        }
    }

    @Override
    public void clear()
    {
        error("cannot change read-only instance");
    }

    public void clearImpl()
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

    public int perform(String name, Map params)
    {
        error("instance is read-only");
        return 0;
    }

    protected int performImpl(String name, Map params)
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

    public int perform(String name, Serializable... params)
    {
        error("instance is read-only");
        return 0;
    }

    protected int performImpl(String name, Serializable... params)
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
        error("cannot delete read-only instance");
        return false;
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
        error("cannot insert read-only instance");
        return false;
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
        error("cannot update read-only instance");
        return false;
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
        error("cannot insert read-only instance");
        return false;
    }

    @Override
    protected ModelTool getModelReference()
    {
        return modelReference;
    }

    @Override
    public String toString()
    {
        return instance.toString();
    }

    protected Instance getInstance()
    {
        return instance;
    }

    private Instance instance;

    private ModelTool modelReference;
}
