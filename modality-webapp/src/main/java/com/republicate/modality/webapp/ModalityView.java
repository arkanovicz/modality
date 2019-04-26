package com.republicate.modality.webapp;

import org.apache.velocity.tools.config.ConfigurationUtils;
import org.apache.velocity.tools.config.FactoryConfiguration;
import org.apache.velocity.tools.config.FileFactoryConfiguration;
import org.apache.velocity.tools.view.JeeConfig;
import org.apache.velocity.tools.view.VelocityView;

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class ModalityView extends VelocityView
{
    public static final String MODALITY_TOOLS_DEFAULTS_PATH =
        "/com/republicate/modality/webapp/tools.xml";

    public ModalityView(ServletConfig config)
    {
        super(config);
    }

    public ModalityView(FilterConfig config)
    {
        super(config);
    }

    public ModalityView(ServletContext context)
    {
        super(context);
    }

    public ModalityView(JeeConfig config)
    {
        super(config);
    }

    @Override
    protected FactoryConfiguration getDefaultToolsConfiguration()
    {
        FileFactoryConfiguration defaultTools = (FileFactoryConfiguration)ConfigurationUtils.getDefaultTools();
        defaultTools.read(MODALITY_TOOLS_DEFAULTS_PATH, false);
        return defaultTools;
    }
}
