package com.republicate.modality.webapp;

import org.apache.velocity.tools.view.JeeConfig;
import org.apache.velocity.tools.view.JeeFilterConfig;
import org.apache.velocity.tools.view.ServletUtils;
import org.apache.velocity.util.ExtProperties;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
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
        String modalityConfigPath = Optional.ofNullable(getConfig().findInitParameter(MODALITY_CONFIG_KEY)).orElse(MODALITY_DEFAULT_CONFIG);
        InputStream is = ServletUtils.getInputStream(modalityConfigPath, config.getServletContext());
        // make it mandatory for now
        if (is == null)
        {
            throw new ServletException("could not find modality configuration file: " +modalityConfigPath);
        }
        Reader rd = new InputStreamReader(is, StandardCharsets.UTF_8); // TODO make it configurable
        Properties props = new Properties();
        try
        {
            props.load(rd);
        } catch (IOException ioe)
        {
            throw new ServletException("could not read modality config file: " + modalityConfigPath, ioe);
        }
        modalityConfig = ExtProperties.convertProperties(props);
    }


    private JeeConfig config = null;
    private ExtProperties modalityConfig = null;
}
