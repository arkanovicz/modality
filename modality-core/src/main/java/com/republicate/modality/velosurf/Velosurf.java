package com.republicate.modality.velosurf;

import com.republicate.modality.Model;
import com.republicate.modality.config.ConfigDigester;
import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.util.AESCryptograph;
import com.republicate.modality.util.Cryptograph;
import com.republicate.modality.util.TypeUtils;
import org.apache.velocity.tools.XmlUtils;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;

@Deprecated
public class Velosurf extends Model
{
    @Override
    public Model initialize(String id, InputSource source) throws ConfigurationException
    {
        String seed = Optional.ofNullable(source.getSystemId())
            .orElse(Optional.ofNullable(getDefinition()).map(url -> url.toString())
            .orElse(Optional.ofNullable(getDatabaseURL())
                .orElse("those are random characters...")));
        initObuscator(seed);
        return super.initialize(id, source);
    }

    @Override
    protected void readDefinition(InputSource source) throws Exception
    {
        if (source == null)
        {
            return;
        }
        DocumentBuilderFactory builderFactory = XmlUtils.createDocumentBuilderFactory();
        builderFactory.setXIncludeAware(true);
        Element doc = builderFactory.newDocumentBuilder().parse(source).getDocumentElement();
        String rootTag = doc.getTagName();
        // support the deprecated 'database' root tag
        if ("database".equals(rootTag))
        {
            getLogger().warn("<database> root tag has been deprecated in favor of <model>");
        }
        else if (!"model".equals(rootTag))
        {
            throw new ConfigurationException("expecting a <model> root tag");
        }
        new VelosurfConfigDigester(doc, this).process();
    }

    protected void initObuscator(String seed)
    {
        obfuscator = new AESCryptograph();
        obfuscator.init(seed);
    }

    public String obfuscate(String clear)
    {
        return TypeUtils.base64Encode(obfuscator.encrypt(clear));
    }

    public String deobfuscate(String obfuscated)
    {
        return obfuscator.decrypt(TypeUtils.base64Decode(obfuscated));
    }

    private Cryptograph obfuscator = null;
}
