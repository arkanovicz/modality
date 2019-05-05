package com.republicate.modality.tools.velosurf;

import com.republicate.modality.Attribute;
import com.republicate.modality.Entity;
import com.republicate.modality.Instance;
import com.republicate.modality.impl.RowIterator;
import com.republicate.modality.tools.model.InstanceReference;
import com.republicate.modality.tools.model.ModelTool;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.Serializable;
import java.sql.SQLException;

@Deprecated
public class VelosurfInstanceReference extends InstanceReference
{
    public VelosurfInstanceReference(Instance instance, ModelTool modelReference)
    {
        super(instance, modelReference);
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

    @Override
    protected Instance getInstance()
    {
        // to give access to package
        return super.getInstance();
    }

    @Override
    public Serializable get(Object key)
    {
        // values shadow attributes
        // (also mimic Velosurf behavior: search for attributes when inner values are present but null)
        Serializable result = getInstance().get(key);
        if (result == null && key instanceof String)
        {
            try
            {
                String name = (String) key;
                Entity entity = getInstance().getEntity();
                if (entity != null)
                {
                    Attribute attribute = entity.getAttribute(name);
                    if (attribute != null)
                    {
                        switch (attribute.getQueryMethodName())
                        {
                            case "evaluate":
                                result = getInstance().evaluate(name);
                                break;
                            case "retrieve":
                                result = getInstance().retrieve(name);
                                break;
                            case "query":
                                result = (RowIterator) getInstance().query(name);
                                break;
                            case "perform":
                                result = getInstance().perform(name);
                                break;
                            default:
                                error("unhandled attribute verb");
                        }
                    }
                }
            }
            catch (SQLException sqle)
            {
                error("cannot execute attribute {}", key, sqle);
            }
        }
        return result;
    }

}
