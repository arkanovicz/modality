package com.republicate.modality.tools.velosurf;

import com.republicate.modality.Model;
import com.republicate.modality.config.ConfigHelper;
import com.republicate.modality.tools.model.ModelTool;
import com.republicate.modality.util.AESCryptograph;
import com.republicate.modality.util.Cryptograph;
import com.republicate.modality.velosurf.Velosurf;
import org.apache.velocity.tools.XmlUtils;
import org.apache.velocity.tools.config.ConfigurationException;
import org.apache.velocity.tools.generic.ValueParser;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.InputStreamReader;
import java.net.URL;
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

}
