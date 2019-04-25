package com.republicate.motion.tools.velosurf;

import org.apache.velocity.tools.XmlUtils;
import org.apache.velocity.tools.config.ConfigurationException;
import org.apache.velocity.tools.generic.ValueParser;
import com.republicate.model.Model;
import com.republicate.motion.tools.model.ModelTool;
import com.republicate.model.config.ConfigHelper;
import com.republicate.model.velosurf.Velosurf;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStreamReader;
import java.net.URL;

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
            URL url = new ConfigHelper(params).findURL(credentials);
            try
            {
                DocumentBuilderFactory builderFactory = XmlUtils.createDocumentBuilderFactory();
                builderFactory.setXIncludeAware(true);
                Element doc = builderFactory.newDocumentBuilder().parse(new InputSource(new InputStreamReader(url.openStream()))).getDocumentElement();
                params.put("databaseURL", doc.getAttribute("url"));
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
}
