package com.republicate.model.impl;

import com.republicate.model.Entity;
import com.republicate.model.Instance;
import com.republicate.model.Model;

public class InstanceProducer
{
    protected InstanceProducer(Model model, Entity resultEntity)
    {
        this.model = model;
        this.resultEntity = resultEntity;
    }

    protected InstanceProducer(Model model)
    {
        this(model, null);
    }

    protected InstanceProducer(Entity resultEntity)
    {
        this(resultEntity.getModel(), resultEntity);
    }

    public Model getModel()
    {
        return model;
    }

    public Entity getResultEntity()
    {
        return resultEntity;
    }

    protected void setResultEntity(Entity resultEntity)
    {
        this.resultEntity = resultEntity;
    }

    public Instance newResultInstance()
    {
        return resultEntity == null ?
            new Instance(getModel()) :
            resultEntity.newInstance();
    }

    private Model model = null;
    private Entity resultEntity = null;
}
