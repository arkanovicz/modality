package com.republicate.modality.webapp;

import org.apache.velocity.tools.view.JeeConfig;
import org.apache.velocity.tools.view.JeeFilterConfig;
import org.apache.velocity.tools.view.ServletUtils;
import org.apache.velocity.util.ExtProperties;

import javax.servlet.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

public abstract class ModalityFilter implements Filter
{
    public static final String MODALITY_CONFIG_KEY = "modality.config";
    public static final String MODALITY_DEFAULT_CONFIG = "com/republicate/modality/modality.properties";
    public static final String MODALITY_DEFAULT_USER_CONFIG = "/WEB-INF/modality.properties";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        this.config = new JeeFilterConfig(filterConfig);
        initModalityConfig();
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

    private JeeConfig config = null;
    private ExtProperties modalityConfig = null;
}
