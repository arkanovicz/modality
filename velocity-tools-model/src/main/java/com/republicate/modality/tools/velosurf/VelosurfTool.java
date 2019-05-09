package com.republicate.modality.tools.velosurf;

import com.republicate.modality.Entity;
import com.republicate.modality.Instance;
import com.republicate.modality.Model;
import com.republicate.modality.config.ConfigHelper;
import com.republicate.modality.tools.model.EntityReference;
import com.republicate.modality.tools.model.InstanceReference;
import com.republicate.modality.tools.model.ModelTool;
import com.republicate.modality.velosurf.Velosurf;
import org.apache.velocity.tools.XmlUtils;
import org.apache.velocity.tools.config.ConfigurationException;
import org.apache.velocity.tools.generic.ValueParser;
import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilderFactory;

@Deprecated
public class VelosurfTool extends ModelTool
{
    @Override
    protected Model createModel()
    {
        return new Velosurf();
    }

    @Override
    protected void configure(ValueParser params)
    {
        String credentials = params.getString("credentials");
        if (credentials != null)
        {
            ValueParser newparams = new ValueParser()
            {{
                setReadOnly(false);
            }};
            newparams.putAll(params);
            params = newparams;
            params.remove("credentials");
            URL url = new ConfigHelper(params).findURL(credentials);
            try
            {
                DocumentBuilderFactory builderFactory = XmlUtils.createDocumentBuilderFactory();
                builderFactory.setXIncludeAware(true);
                Element doc = builderFactory.newDocumentBuilder().parse(new InputSource(new InputStreamReader(url.openStream()))).getDocumentElement();
                params.put("database", doc.getAttribute("url"));
                params.put("credentials.user", doc.getAttribute("user"));
                params.put("credentials.password", doc.getAttribute("password"));
            }
            catch (Exception e)
            {
                throw new ConfigurationException("could not read credentials from URL " + url, e);
            }
        }
        super.configure(params);
    }

    public String obfuscate(String clear)
    {
        return ((Velosurf)getModel()).obfuscate(clear);
    }

    public String deobfuscate(String obfuscated)
    {
        return ((Velosurf)getModel()).deobfuscate(obfuscated);

    }

    @Override
    protected EntityReference createEntityReference(Entity entity)
    {
        return new VelosurfEntityReference(entity, this);
    }

    @Override
    protected InstanceReference createInstanceReference(Instance instance)
    {
        return new VelosurfInstanceReference(instance, this);
    }

    @Override
    public Iterator<InstanceReference> createInstanceReferenceIterator(Iterator<Instance> query)
    {
        return new VelosurfInstanceReferenceIterator(query);
    }

    @Override
    protected Logger getLogger() // give package access to logger
    {
        return getLog();
    }

    public void setError(String message)
    {
        setLastError(message);
    }

    public String getError()
    {
        return getLastError();
    }

    public void clearError()
    {
        clearLastError();
    }

    public class VelosurfInstanceReferenceIterator extends ModelTool.InstanceReferenceIterator
    {
        public VelosurfInstanceReferenceIterator(Iterator<Instance> iterator)
        {
            super(iterator);
        }

        // TODO - some of those methods could be useful in RowIterator and/or ModelTool.InstanceReferenceIterator

        public List<InstanceReference> getRows()
        {
            List<InstanceReference> ret = new ArrayList<>();
            while (hasNext())
            {
                ret.add(next());
            }
            return ret;
        }

        public List<Serializable> getScalars()
        {
            List<Serializable> ret = new ArrayList<>();
            while (hasNext())
            {
                InstanceReference instance = next();
                Serializable value = instance.entrySet().iterator().next().getValue();
                ret.add(value);
            }
            return ret;
        }

        public Set<Serializable> getSet()
        {
            Set<Serializable> ret = new TreeSet<>();
            while (hasNext())
            {
                InstanceReference instance = next();
                Serializable value = instance.entrySet().iterator().next().getValue();
                ret.add(value);
            }
            return ret;
        }

        public Map<Serializable, Serializable> getMap()
        {
            throw new UnsupportedOperationException("not supported");
        }

        public Map<Serializable, InstanceReference> getInstanceMap()
        {
            Map<Serializable, InstanceReference> ret = new HashMap<>();
            while (hasNext())
            {
                VelosurfInstanceReference instance = (VelosurfInstanceReference)next();
                Serializable key[] = instance.getInstance().getPrimaryKey();
                if (key != null && key.length == 1)
                {
                    ret.put(key[0], instance);
                }
                else
                {
                    try
                    {
                        throw new SQLException("wrong key size");
                    }
                    catch (SQLException sqle)
                    {
                        error("cannot get instance map", sqle);
                        return null;
                    }
                }
            }
            return ret;
        }
    }

    public Object put(String key, Object value)
    {
        try
        {
            throw new SQLFeatureNotSupportedException("ModelTool is read-only");
        }
        catch (SQLException sqle)
        {
            error("cannot set value", sqle);
            return null;
        }
    }
}
