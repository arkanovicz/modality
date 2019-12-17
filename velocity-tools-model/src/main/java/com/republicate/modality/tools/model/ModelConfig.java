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

import com.republicate.modality.Model;
import com.republicate.modality.ModelRepository;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.generic.SafeConfig;
import org.apache.velocity.tools.generic.ValueParser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Base class for model-aware tools.</p>
 *
 */

public class ModelConfig extends SafeConfig
{
    private Model model = null;
    private ToolManager view = null;

    protected Model getModel()
    {
        return model;
    }

    @Override
    protected void configure(ValueParser values)
    {
        String modelId = getModelId(values);
        if (modelId != null)
        {
            model = ModelRepository.getModel(values.get("servletContext"), modelId);
            if (model == null)
            {
                // it means the model hasn't been initialized yet

                // force model initialization
                view.createContext().get(modelId);

                // then try again
                model = ModelRepository.getModel(values.get("servletContext"), modelId);
            }
        }
    }

    protected String getModelId(ValueParser values)
    {
        String modelId = values.getString("model." + Model.MODEL_ID);
        if (modelId == null)
        {
            // if in a web context, try to find a ModelTool
            modelId = getWebappToolboxModelId(values);
        }
        return modelId;
    }

    protected String getWebappToolboxModelId(ValueParser values)
    {
        Object servletContext = values.get("servletContext");
        if (servletContext != null)
        {
            // ok, let's proceed using reflection
            Class servletContextClass = null;
            Class servletUtilsClass = null;
            try
            {
                servletContextClass = Class.forName("javax.servlet.ServletContext");
                servletUtilsClass = Class.forName("org.apache.velocity.tools.view.ServletUtils");
            } catch (ClassNotFoundException cnfe)
            {
            }
            if (servletContext != null && servletUtilsClass != null && servletContextClass.isAssignableFrom(servletContext.getClass()))
            {
                try
                {
                    Method getVelocityView = servletUtilsClass.getMethod("getVelocityView", servletContextClass);
                    view = (ToolManager) getVelocityView.invoke(null, servletContext);
                }
                catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException e)
                {
                    getLog().warn("could not get velocity view");
                }
                if (view != null)
                {
                    List<String> modelTools = view.getApplicationToolbox().getToolClassMap().entrySet().stream()
                        .filter(e -> ModelTool.class.isAssignableFrom(e.getValue()))
                        .map(e -> e.getKey())
                        .collect(Collectors.toList());
                    if (modelTools.size() == 1)
                    {
                        return modelTools.get(0);
                    }
                }
            }
        }
        return null;
    }
}
