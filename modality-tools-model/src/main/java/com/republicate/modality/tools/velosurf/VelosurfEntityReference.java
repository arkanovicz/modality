package com.republicate.modality.tools.velosurf;

import com.republicate.modality.Entity;
import com.republicate.modality.tools.model.EntityReference;
import com.republicate.modality.tools.model.ModelTool;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

@Deprecated
public class VelosurfEntityReference extends EntityReference
{
    public VelosurfEntityReference(Entity entity, ModelTool modelReference)
    {
        super(entity, modelReference);
    }

    @Override
    protected void error(String message, Object... arguments)
    {
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, arguments);
        String msg = tuple.getMessage();
        Throwable err = tuple.getThrowable();
        VelosurfTool modelTool = (VelosurfTool)getModelReference();
        modelTool.getLogger().error(msg, err);
        modelTool.setError(msg);

    }
}
