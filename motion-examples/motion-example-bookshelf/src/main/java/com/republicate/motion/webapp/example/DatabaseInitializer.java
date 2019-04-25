package com.republicate.motion.webapp.example;

import org.apache.velocity.tools.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class DatabaseInitializer implements ServletContextListener
{
    protected static Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        try
        {
            logger.info("populating database");
            initDatabase(sce.getServletContext());
            logger.info("database populated");
        }
        catch (Exception sqle)
        {
            logger.error("could not initialize database", sqle);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {

    }

    private void initDatabase(ServletContext servletContext) throws Exception
    {
        Model model = new Model().initialize(servletContext.getResource("/WEB-INF/model.xml"));
        model.perform("initBookshelf");
    }
}
