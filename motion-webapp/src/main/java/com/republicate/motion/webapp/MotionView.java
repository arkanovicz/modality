package com.republicate.motion.webapp;

import org.apache.velocity.tools.config.ConfigurationUtils;
import org.apache.velocity.tools.config.FactoryConfiguration;
import org.apache.velocity.tools.config.FileFactoryConfiguration;
import org.apache.velocity.tools.view.JeeConfig;
import org.apache.velocity.tools.view.VelocityView;

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class MotionView extends VelocityView
{
    public static final String MOTION_TOOLS_DEFAULTS_PATH =
        "/com/republicate/motion/webapp/tools.xml";

    public MotionView(ServletConfig config)
    {
        super(config);
    }

    public MotionView(FilterConfig config)
    {
        super(config);
    }

    public MotionView(ServletContext context)
    {
        super(context);
    }

    public MotionView(JeeConfig config)
    {
        super(config);
    }

    @Override
    protected FactoryConfiguration getDefaultToolsConfiguration()
    {
        FileFactoryConfiguration defaultTools = (FileFactoryConfiguration)ConfigurationUtils.getDefaultTools();
        defaultTools.read(MOTION_TOOLS_DEFAULTS_PATH, false);
        return defaultTools;
    }
}
