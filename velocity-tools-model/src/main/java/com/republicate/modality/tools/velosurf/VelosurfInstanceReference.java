package com.republicate.modality.tools.velosurf;

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

import com.republicate.modality.Attribute;
import com.republicate.modality.Entity;
import com.republicate.modality.Instance;
import com.republicate.modality.impl.RowIterator;
import com.republicate.modality.tools.model.InstanceReference;
import com.republicate.modality.tools.model.ModelTool;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.Serializable;
import java.sql.SQLException;

@Deprecated
public class VelosurfInstanceReference extends InstanceReference
{
    public VelosurfInstanceReference(Instance instance, ModelTool modelReference)
    {
        super(instance, modelReference);
    }

    @Override
    protected Instance getInstance()
    {
        // to give access to package
        return super.getInstance();
    }

    @Override
    public Serializable get(Object key)
    {
        // values shadow attributes
        // (also mimic Velosurf behavior: search for attributes when inner values are present but null)
        Serializable result = getInstance().get(key);
        if (result == null && key instanceof String)
        {
            try
            {
                String name = (String) key;
                Entity entity = getInstance().getEntity();
                if (entity != null)
                {
                    Attribute attribute = entity.getAttribute(name);
                    if (attribute != null)
                    {
                        switch (attribute.getQueryMethodName())
                        {
                            case "evaluate":
                                result = getInstance().evaluate(name);
                                break;
                            case "retrieve":
                                result = getInstance().retrieve(name);
                                break;
                            case "query":
                                result = (RowIterator) getInstance().query(name);
                                break;
                            case "perform":
                                result = getInstance().perform(name);
                                break;
                            default:
                                error("unhandled attribute verb");
                        }
                    }
                }
            }
            catch (SQLException sqle)
            {
                error("cannot execute attribute {}", key, sqle);
            }
        }
        return result;
    }

}
