package com.republicate.modality.webapp;

import com.republicate.modality.Model;
import com.republicate.modality.ModelRepository;
import com.republicate.modality.config.ConfigurationException;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.view.JeeConfig;
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
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class WebappModelConfig
{
    protected static Logger logger = LoggerFactory.getLogger("model-init");

    public static final String MODALITY_CONFIG_KEY = "modality.config";
    public static final String MODALITY_DEFAULT_CONFIG = "com/republicate/modality/modality.properties";
    public static final String MODALITY_DEFAULT_USER_CONFIG = "/WEB-INF/modality.properties";

    public static final String MODEL_ID = "auth.model.model_id";

    public WebappModelConfig(JeeConfig config)
    {
        this.webConfig = config;
    }

    public JeeConfig getWebConfig()
    {
        return webConfig;
    }

    public String findConfigParameter(String key)
    {
        String ret = webConfig.findInitParameter(key);
        if (ret == null)
        {
            ret = modalityConfig.getString(key);
        }
        return ret;
    }

    public ServletContext getServletContext()
    {
        return getWebConfig().getServletContext();
    }

    //
    // Modality Initialization
    //  - default modality.properties file
    // - user provided mododality.properties file
    //

    public void initModalityConfig() throws ServletException
    {
        initModalityDefaultConfig();
        initModalityUserConfig();
        modelId = findConfigParameter(MODEL_ID);
    }

    private void initModalityDefaultConfig() throws ServletException
    {
        modalityConfig = new ExtProperties();
        InputStream is = ServletUtils.getInputStream(MODALITY_DEFAULT_CONFIG, webConfig.getServletContext());
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
        String modalityConfigPath = getWebConfig().findInitParameter(MODALITY_CONFIG_KEY);
        boolean mandatory = modalityConfigPath != null;
        if (modalityConfigPath == null)
        {
            modalityConfigPath = MODALITY_DEFAULT_USER_CONFIG;
        }
        if (modalityConfigPath != null)
        {
            InputStream is = ServletUtils.getInputStream(modalityConfigPath, webConfig.getServletContext());
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
                    throw new ServletException("could not read modality configuration file: " + modalityConfigPath, ioe);
                }
                modalityConfig.putAll(userProps);
            }
        }
    }

    //
    // Model Initialization
    // - application toolbox model
    // - ModelRepository toolbox
    //


    protected Model initModel() throws ServletException
    {
        // if the model has been initialized via a model tool,
        // make sure the model tool is ready
        Model model = initModelFromApplicationToolbox();
        if (model == null)
        {
            // No application toolbox, or nothing found within.
            // Just ask the repository.
            model = initModelFromRepository();

            // Still not available? Try to initialize it ourselves.
            if (model == null)
            {
                model = initNewModel();

                // otherwise bail out
                if (model == null)
                {
                    throw new RuntimeException("ModelAuthFilter: no model found" + (modelId == null ? "" : " for model id " + modelId));
                }
            }
        }
        return model;
    }

    private Model initModelFromApplicationToolbox()
    {
        Model model = null;
        VelocityView view = ServletUtils.getVelocityView(getWebConfig());
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
                model = ModelRepository.getModel(getWebConfig().getServletContext(), modelKey);
            }
        }
        return model;
    }

    private Model initModelFromRepository()
    {
        Model model = null;
        String[] ids =
            modelId == null ?
                new String[] { "default", "model" } :
                new String[] { modelId };
        for (String id : ids)
        {
            model = ModelRepository.getModel(id);
            if (model != null)
            {
                modelId = id;
                logger.info("Found model id '{}' in model repository", id);
                break;
            }
        }
        return model;
    }

    private Model initNewModel()
    {
        Model model = null;
        try
        {
            Map params = new HashMap();
            params.put(ToolContext.CONTEXT_KEY, getWebConfig().getServletContext());
            params.put(ToolContext.ENGINE_KEY, ServletUtils.getVelocityView(getWebConfig()).getVelocityEngine());
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
        return model;
    }

    private String modelId = null;
    private ExtProperties modalityConfig = null;
    private JeeConfig webConfig;
    
}
