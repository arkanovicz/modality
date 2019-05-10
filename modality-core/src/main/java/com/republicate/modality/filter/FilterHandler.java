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
import org.apache.velocity.tools.ClassUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

public abstract class FilterHandler <T extends Serializable> extends TypeMapper<Filter<T>>
{
    public FilterHandler(String configurationPrefix)
    {
        super(configurationPrefix);
        setDefaultColumnLeaf(Filter.identity());
    }

    @Override
    protected Filter<T> composeLeaves(Filter<T> left, Filter<T> right)
    {
        return left.compose(right);
    }

    protected Filter<T> newObjectToLeaf(Object obj)
    {
        Filter<T> ret = null;
        if (ret instanceof Filter)
        {
            return super.newObjectToLeaf(obj);
        }
        else
        {
            Method stringGetter = ClassUtils.findMethod(obj.getClass(), "get", String.class);
            final Method getter = stringGetter == null ?
                ClassUtils.findMethod(obj.getClass(), "get", Object.class) :
                stringGetter;
            if (getter == null)
            {
                throw new ConfigurationException(getConfigurationPrefix() + ": don't know what to do with class " + obj.getClass().getName());
            }
            return x ->
            {
                try
                {
                    return (T)getter.invoke(obj, x);
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                    throw new SQLException("could not apply operator from class " + obj.getClass().getName());
                }
            };
        }
    }
}
