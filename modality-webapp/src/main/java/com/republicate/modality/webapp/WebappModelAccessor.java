package com.republicate.modality.webapp;

import com.republicate.modality.Model;

import javax.servlet.ServletException;

public interface WebappModelAccessor
{
    WebappModelProvider getModelProvider();

    default void configureModel() throws ServletException
    {
        getModelProvider().configure();
    }

    default Model getModel() throws ServletException
    {
        Model model = getModelProvider().getModel(false);
        if (model == null)
        {
            synchronized(this)
            {
                model = getModelProvider().getModel(true);
                modelInitialized(model);
            }
        }
        return model;
    }

    default void modelInitialized(Model model) throws ServletException
    {
        // default implementation does nothing
    }

    default String findConfigParameter(String key)
    {
        return getModelProvider().findConfigParameter(key);
    }
}
