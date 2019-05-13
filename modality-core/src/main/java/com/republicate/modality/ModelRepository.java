package com.republicate.modality;

import com.republicate.modality.config.ConfigurationException;
import org.apache.velocity.tools.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelRepository
{
    protected static Logger logger = LoggerFactory.getLogger("model-repository");

    public static void registerModel(Model model)
    {
        registerModel(null, model);
    }

    public static void registerModel(Object context, Model model)
    {
        if (context == null)
        {
            context = ModelRepository.class.getClassLoader();
        }
        ModelStore store = contextMap.get(context);
        if (store == null)
        {
            store = ContextAttributeStore.getInstance(context);
            if (store == null)
            {
                store = GenericContextStore.getInstance(context);
            }
            contextMap.put(context, store);
        }
        store.put(model);
    }

    public static Model getModel(String modelId)
    {
        return getModel(null, modelId);
    }

    public static Model getModel(Object context, String modelId)
    {
        Model model = null;
        if (context == null)
        {
            context = ModelRepository.class.getClassLoader();
        }
        ModelStore store = contextMap.get(context);
        if (store != null)
        {
            model = store.get(modelId);
        }
        return model;
    }

    /**
     * (global) model id resolution contexts (aka ServletContexts or ClassLoaders)
     */
    private static Map<Object, ModelStore> contextMap = new HashMap<>();

    private static interface ModelStore
    {
        Model get(String id);

        void put(Model model);
    }

    private static class ContextAttributeStore implements ModelStore
    {
        public ContextAttributeStore(Object context)
        {
            this.context = context;
        }

        @Override
        public Model get(String id)
        {
            try
            {
                Object ret = attributeGetter.invoke(context, id);
                if (ret != null && !(ret instanceof Model))
                {
                    throw new RuntimeException("model repository internal error");
                }
                return (Model)ret;
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                logger.error("could net get model id {} in context: {}", id, context, e);
                return null;
            }
        }

        @Override
        public void put(Model model)
        {
            try
            {
                attributeSetter.invoke(context, model.getModelId(), model);
                // also register it in the static repository
                // registerModel(ModelRepository.class.getClassLoader(), model);
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                logger.error("could net get model id {} in context: {}", model.getModelId(), context, e);
            }
        }

        public static ModelStore getInstance(Object context)
        {
            ContextAttributeStore store = new ContextAttributeStore(context);
            store.init();
            if (store.attributeGetter == null || store.attributeSetter == null)
            {
                store = null;
            }
            return store;
        }

        private void init()
        {
            Class contextClass = context.getClass();
            attributeGetter = ClassUtils.findMethod(contextClass, "getAttribute", String.class);
            attributeSetter = ClassUtils.findMethod(contextClass, "setAttribute", String.class, Object.class);
        }

        private Object context;
        private Method attributeGetter = null;
        private Method attributeSetter = null;
    }

    private static class GenericContextStore implements ModelStore
    {
        public GenericContextStore()
        {
        }

        @Override
        public Model get(String id)
        {
            return innerMap.get(id);
        }

        @Override
        public void put(Model model)
        {
            innerMap.put(model.getModelId(), model);
        }

        public static ModelStore getInstance(Object context)
        {
            ModelStore store = staticContexts.get(context);
            if (store == null)
            {
                store = new GenericContextStore();
                staticContexts.put(context, store);
            }
            return store;
        }

        private Map<String, Model> innerMap = new ConcurrentHashMap<>();
        private static Map<Object, ModelStore> staticContexts = new ConcurrentHashMap<>();
    }

}
