package com.republicate.motion.webapp;

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

public abstract class MotionFilter implements Filter
{
    public static final String MOTION_CONFIG_KEY = "motion.config";
    public static final String MOTION_DEFAULT_CONFIG = "com/republicate/motion/motion.properties";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        this.config = new JeeFilterConfig(filterConfig);
        initMotionConfig();
    }

    protected final JeeConfig getConfig()
    {
        return config;
    }

    protected final String findConfigParameter(String key)
    {
        // first search config
        String value = getConfig().findInitParameter(key);

        // then Motion properties
        if (value == null)
        {
            value = motionConfig.getString(key);
        }

        return value;
    }

    private void initMotionConfig() throws ServletException
    {
        String motionConfigPath = Optional.ofNullable(getConfig().findInitParameter(MOTION_CONFIG_KEY)).orElse(MOTION_DEFAULT_CONFIG);
        InputStream is = ServletUtils.getInputStream(motionConfigPath, config.getServletContext());
        // make it mandatory for now
        if (is == null)
        {
            throw new ServletException("could not find motion configuration file: " +motionConfigPath);
        }
        Reader rd = new InputStreamReader(is, StandardCharsets.UTF_8); // TODO make it configurable
        Properties props = new Properties();
        try
        {
            props.load(rd);
        } catch (IOException ioe)
        {
            throw new ServletException("could not read motion config file: " + motionConfigPath, ioe);
        }
        motionConfig = ExtProperties.convertProperties(props);
    }


    private JeeConfig config = null;
    private ExtProperties motionConfig = null;
}
