package com.republicate.modality;

import com.republicate.modality.config.ConfigurationException;
import org.apache.velocity.tools.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ModelRepository
{
    protected static Logger logger = LoggerFactory.getLogger("modality");

    public static void registerModel(Model model)
    {
        registerModel(null, model);
    }

    public static void registerModel(Object context, Model model)
    {
        // find apropriate model store
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

        // store model
        store.put(model);

        // call listeners
        List<Consumer<Model>> listeners = modelListeners.get(model.getModelId());
        if (listeners != null)
        {
            logger.debug("[model-repository] calling {} listeners", listeners.size());
            for (Consumer<Model> listener : listeners)
            {
                listener.accept(model);
            }
        }
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

    public static void addModelListener(String modelId, Consumer<Model> consumer)
    {
        logger.debug("[model-repository] added listener towards model {} : {}", modelId, consumer.getClass().getName());
        List<Consumer<Model>> listeners = modelListeners.get(modelId);
        if (listeners == null)
        {
            synchronized (modelListeners)
            {
                listeners = modelListeners.get(modelId);
                if (listeners == null)
                {
                    listeners = new ArrayList<Consumer<Model>>();
                    modelListeners.put(modelId, listeners);
                }
            }
        }
        synchronized (listeners)
        {
            listeners.add(consumer);
        }
    }

    public static void clear()
    {
        // mainly used for resetting repository before tests
        contextMap.clear();
    }

    /**
     * (global) model id resolution contexts (aka ServletContexts or ClassLoaders)
     */
    private static Map<Object, ModelStore> contextMap = new HashMap<>();

    /**
     * model listeners map
     */
    private static Map<String, List<Consumer<Model>>> modelListeners = new ConcurrentHashMap<>();

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
