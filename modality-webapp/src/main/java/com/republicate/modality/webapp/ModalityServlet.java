package com.republicate.modality.webapp;

import com.republicate.modality.Model;
import com.republicate.modality.config.ConfigurationException;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.view.JeeConfig;
import org.apache.velocity.tools.view.JeeFilterConfig;
import org.apache.velocity.tools.view.JeeServletConfig;
import org.apache.velocity.tools.view.ServletUtils;
import org.apache.velocity.tools.view.VelocityView;
import org.apache.velocity.util.ClassUtils;
import org.apache.velocity.util.ExtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public abstract class ModalityServlet extends HttpServlet
{
    protected static Logger logger = LoggerFactory.getLogger("modality");

    public static final String MODALITY_CONFIG_KEY = "modality.config";
    public static final String MODALITY_DEFAULT_CONFIG = "com/republicate/modality/modality.properties";
    public static final String MODALITY_DEFAULT_USER_CONFIG = "/WEB-INF/modality.properties";

    public static final String MODEL_ID = "auth.model.model_id";

    @Override
    public void init(ServletConfig servletConfig) throws ServletException
    {
        this.config = new JeeServletConfig(servletConfig);
        initModalityConfig();
        modelId = findConfigParameter(MODEL_ID);
    }

    protected final JeeConfig getConfig()
    {
        return config;
    }

    protected final String findConfigParameter(String key)
    {
        // first search config
        String value = getConfig().findInitParameter(key);

        // then Modality properties
        if (value == null)
        {
            value = modalityConfig.getString(key);
        }

        return value;
    }

    private void initModalityConfig() throws ServletException
    {
        initModalityDefaultConfig();
        initModalityUserConfig();
    }

    private void initModalityDefaultConfig() throws ServletException
    {
        modalityConfig = new ExtProperties();
        InputStream is = ServletUtils.getInputStream(MODALITY_DEFAULT_CONFIG, config.getServletContext());
        if (is == null)
        {
            throw new ServletException("could not find default modality configuration file: " + MODALITY_DEFAULT_CONFIG);
        }
        try
        {
            modalityConfig.load(is);
        } catch (IOException ioe)
        {
            throw new ServletException("could configure default modality configuration file", ioe);
        }
    }

    private void initModalityUserConfig() throws ServletException
    {
        String modalityConfigPath = getConfig().findInitParameter(MODALITY_CONFIG_KEY);
        boolean mandatory = modalityConfigPath != null;
        if (modalityConfigPath == null)
        {
            modalityConfigPath = MODALITY_DEFAULT_USER_CONFIG;
        }
        if (modalityConfigPath != null)
        {
            InputStream is = ServletUtils.getInputStream(modalityConfigPath, config.getServletContext());
            if (is == null)
            {
                if (mandatory)
                {
                    throw new ServletException("could not find modality configuration file: " + modalityConfigPath);
                }
            }
            else
            {
                ExtProperties userProps = new ExtProperties();
                try
                {
                    userProps.load(is);
                }
                catch (IOException ioe)
                {
                    throw new ServletException("could not read modality config file: " + modalityConfigPath, ioe);
                }
                modalityConfig.putAll(userProps);
            }
        }
    }

    protected void requireModelInit() throws ServletException
    {
        if (model == null)
        {
            synchronized (this)
            {
                if (model == null)
                {
                    initModel();
                }
            }
        }
    }

    protected void initModel() throws ServletException
    {
        // if the model has been initialized via a model tool,
        // make sure the model tool is ready
        initModelFromApplicationToolbox();
        if (model == null)
        {
            // No application toolbox, or nothing found within.
            // Just ask the repository.
            initModelFromRepository();

            // Still not available? Try to initialize it ourselves.
            if (model == null)
            {
                initNewModel();

                // otherwise bail out
                if (model == null)
                {
                    throw new RuntimeException("ModelAuthFilter: no model found" + (modelId == null ? "" : " for model id " + modelId));
                }
            }
        }
    }

    private void initModelFromApplicationToolbox()
    {
        VelocityView view = ServletUtils.getVelocityView(getConfig());
        if (view.hasApplicationTools())
        {
            Set<String> modelKeys = new HashSet<>();
            Class modelToolClass = null;
            try
            {
                modelToolClass = ClassUtils.getClass("com.republicate.modality.tools.model.ModelTool");
            }
            catch (ClassNotFoundException cnfe) {}
            if (modelToolClass != null)
            {
                // search for a model in application tools
                for (Map.Entry<String, Class> entry : view.getApplicationToolbox().getToolClassMap().entrySet())
                {
                    if (modelToolClass.isAssignableFrom(entry.getValue()))
                    {
                        modelKeys.add(entry.getKey());
                    }
                }
            }
            String modelKey = null;

            if (modelId == null)
            {
                if (modelKeys.size() > 1)
                {
                    throw new RuntimeException("authentication filter cannot choose between several models, please specify " + MODEL_ID + " in configuration parameters");
                }
                else if (!modelKeys.isEmpty())
                {
                    modelId = modelKey = modelKeys.iterator().next();
                }
            }
            else
            {
                if (modelKeys.contains(modelId))
                {
                    modelKey = modelId;
                }
            }

            if (modelKey != null)
            {
                logger.info("Found model id '{}' in application toolbox", modelKey);

                // force model initialization
                view.createContext().get(modelKey);

                // get model
                model = Model.getModel(modelKey);
            }
        }
    }

    private void initModelFromRepository()
    {
        String[] ids =
            modelId == null ?
                new String[] { "default", "model" } :
                new String[] { modelId };
        String foundModel = null;
        for (String id : ids)
        {
            try
            {
                model = Model.getModel(id);
                foundModel = id;
            }
            catch (ConfigurationException e) {}
            if (foundModel != null)
            {
                modelId = foundModel;
                logger.info("Found model id '{}' in model repository", foundModel);
            }
        }
    }

    private void initNewModel()
    {
        try
        {
            Map params = new HashMap();
            params.put(ToolContext.CONTEXT_KEY, getConfig().getServletContext());
            params.put(ToolContext.ENGINE_KEY, ServletUtils.getVelocityView(getConfig()).getVelocityEngine());
            model = new Model().configure(params);
            if (modelId == null)
            {
                model.initialize();
                modelId = model.getModelId();
            }
            else
            {
                model.initialize(modelId);
            }
            logger.info("Configured new model with model id '{}'", modelId);
        }
        catch (ConfigurationException ce)
        {
            logger.error("could not configure and initialize model", ce);
        }
    }

    protected Model getModel()
    {
        return model;
    }

    private String modelId = null;
    private Model model = null;

    private JeeConfig config = null;
    private ExtProperties modalityConfig = null;
}
