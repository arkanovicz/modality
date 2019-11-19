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

import com.republicate.modality.util.Converter;
import com.republicate.modality.util.TypeUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Wrapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WrappingInstance extends Instance implements Wrapper
{
    public WrappingInstance(Entity entity, Object pojo)
    {
        super(entity);
        this.pojo = pojo;
        this.getters = Optional.ofNullable(entity.getWrappedInstanceGetters()).orElse(new HashMap<>());
        this.setters = Optional.ofNullable(entity.getWrappedInstanceSetters()).orElse(new HashMap<>());
    }

    private Serializable callSetter(Pair<Method, Class> setter, Serializable value)
    {
        Serializable ret = null;
        Method method = setter.getLeft();
        Class paramClass = setter.getRight();
        try
        {
            if (value != null && value.getClass() != paramClass && !TypeUtils.isMethodInvocationConvertible(paramClass, value.getClass()))
            {
                Converter converter = getModel().getConversionHandler().getNeededConverter(paramClass, value.getClass());
                if (converter == null)
                {
                    throw new RuntimeException("cannot convert object '" + value + "' of class " + value.getClass().getName() + " to class " + paramClass.getName() + " for setter " + method);
                }
                value = converter.convert(value);
            }
            Object methodRet = method.invoke(pojo, value);
            if (methodRet != null && !(methodRet instanceof Void))
            {
                if (!(methodRet instanceof Serializable))
                {
                    throw new RuntimeException("setter '" + method + "' did return a non-serializable object");
                }
                ret = (Serializable)methodRet;
            }
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException("could not set property using setter " + method, e);
        }
        return ret;
    }

    @Override
    public void readValue(String columnName, Serializable value) throws SQLException
    {
        Pair<Method, Class> setter = setters.get(columnName);
        if (setter == null)
        {
            super.readValue(columnName, value);
        }
        else
        {
            callSetter(setter, value);
        }
    }

    @Override
    public Serializable get(Object key)
    {
        Method getter = getters.get(key);
        if (getter == null)
        {
            return super.get(key);
        }
        else
        {
            try
            {
                return (Serializable)getter.invoke(pojo);
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                throw new RuntimeException("could not get property " + key, e);
            }
        }
    }

    @Override
    protected Serializable putImpl(String key, Serializable value)
    {
        Pair<Method, Class> setter = setters.get(key);
        if (setter == null)
        {
            return super.putImpl(key, value);
        }
        else
        {
            return callSetter(setter, value);
        }
    }

    private Object pojo;
    private Map<String, Method> getters = null;
    private Map<String, Pair<Method, Class>> setters = null;

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        if (isWrapperFor(iface))
        {
            return (T)pojo;
        }
        else
        {
            throw new SQLException("cannot unwrap towards " + iface.getName());
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return iface.isAssignableFrom(pojo.getClass());
    }

    @Override
    public String toString()
    {
        // merge map properties and wrapper properties
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        boolean first = true;
        for (Map.Entry<String, Serializable> entry : entrySet())
        {
            if (first)
            {
                first = false;
            }
            else
            {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        for (Map.Entry<String, Method> entry : getters.entrySet())
        {
            if (first)
            {
                first = false;
            }
            else
            {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append('=');
            String value = null;
            try
            {
                value = String.valueOf(entry.getValue().invoke(pojo));
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                value = e.getClass().getName();
            }
            builder.append(value);
        }
        builder.append('}');
        return builder.toString();
    }


}
