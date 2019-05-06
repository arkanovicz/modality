package com.republicate.modality.tools.velosurf;

import com.republicate.modality.Entity;
import com.republicate.modality.tools.model.EntityReference;
import com.republicate.modality.tools.model.InstanceReference;
import com.republicate.modality.tools.model.ModelTool;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.Map;

@Deprecated
public class VelosurfEntityReference extends EntityReference
{
    public VelosurfEntityReference(Entity entity, ModelTool modelReference)
    {
        super(entity, modelReference);
    }

    public boolean insert(Map values)
    {
        InstanceReference instance = newInstance(values);
        return instance.insert();
    }
}
