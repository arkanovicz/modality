package com.republicate.modality.tools.model;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.config.ConfigurationUtils;
import org.apache.velocity.tools.config.FactoryConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class MultiModelsTests extends BaseBookshelfTests
{
    public @Test void testTwoModels()
    {
        ToolManager manager = new ToolManager();
        manager.setVelocityEngine(createVelocityEngine((String)null));
        FactoryConfiguration config = ConfigurationUtils.find(ModelTool.MODEL_TOOLS_DEFAULTS_PATH);
        FactoryConfiguration modelConfig = ConfigurationUtils.find("two_models_tools.xml");
        config.addConfiguration(modelConfig);
        manager.configure(config);
        Context context = manager.createContext();
        Object obj = context.get("model_1");
        assertNotNull(obj);
        assertTrue(obj instanceof ModelTool);
        ModelTool model_1 = (ModelTool)obj;
        obj = context.get("model_2");
        assertNotNull(obj);
        assertTrue(obj instanceof ModelTool);
        ModelTool model_2 = (ModelTool)obj;
        Object rst1 = model_1.get("authors_count");
        assertTrue(rst1 instanceof Number);
        Object rst2 = model_2.get("count_authors");
        assertTrue(rst2 instanceof Number);
        assertEquals(2l, rst1);
        assertEquals(2l, rst2);
    }

    @BeforeClass
    public static void populateDataSource() throws Exception
    {
        BaseBookshelfTests.populateDataSource();
    }

}
