package com.republicate.modality.velosurf;

import com.republicate.modality.config.ConfigDigester;
import com.republicate.modality.config.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.Optional;

@Deprecated
public class VelosurfConfigDigester extends ConfigDigester
{
    protected static Logger logger = LoggerFactory.getLogger("velosurf");

    public VelosurfConfigDigester(Element doc, Object bean)
    {
        super(doc, bean);
    }

    @Override
    protected String getElementName(Element element)
    {
        String name = super.getElementName(element);
        switch (name)
        {
            case "entity":
            {
                logger.warn("<entity> tags are deprecated: use <xxx> tags with 'xxx' being the entity name, like in <book>...</book>");
                name = Optional.ofNullable(element.getAttribute("name"))
                    .orElseThrow(() -> new ConfigurationException("entity without name"));
                element.removeAttribute("name");
                break;
            }
            case "attribute":
            {
                logger.warn("<attribute> tags are deprecated: use <scalar name=...>, <row name=...>, <rowset name=...>");
                String result = Optional.ofNullable(element.getAttribute("result"))
                    .orElseThrow(() -> new ConfigurationException("attribute without result type"));
                int slash = result.indexOf('/');
                if (slash != -1)
                {
                    name = result.substring(0, slash);
                    element.setAttribute("result", result.substring(slash + 1));
                }
                break;
            }
            default: // nop
        }
        return name;
    }
}
