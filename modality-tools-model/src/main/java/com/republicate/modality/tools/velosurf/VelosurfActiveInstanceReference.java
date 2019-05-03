package com.republicate.modality.tools.velosurf;

import com.republicate.modality.Instance;
import com.republicate.modality.tools.model.ActiveInstanceReference;
import com.republicate.modality.tools.model.InstanceReference;
import com.republicate.modality.tools.model.ModelTool;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

@Deprecated
public class VelosurfActiveInstanceReference extends ActiveInstanceReference
{
    public VelosurfActiveInstanceReference(Instance instance, ModelTool modelReference)
    {
        super(instance, modelReference);
    }

    @Override
    protected void handleError(String message, Object... arguments)
    {
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, arguments);
        String msg = tuple.getMessage();
        Throwable err = tuple.getThrowable();
        logger.error(msg, err);
        ((VelosurfTool)getModelReference()).setError(message);
    }
}
