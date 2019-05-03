package com.republicate.modality.tools.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public abstract class Reference
{
    protected abstract ModelTool getModelReference();

    protected void error(String message, Object... arguments)
    {
        getModelReference().getLogger().error(message, arguments);
    }

}
