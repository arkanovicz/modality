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
import com.republicate.modality.Model;

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
