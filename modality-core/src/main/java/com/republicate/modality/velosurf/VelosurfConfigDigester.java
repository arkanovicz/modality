package com.republicate.modality.velosurf;

import com.republicate.modality.config.ConfigDigester;
import com.republicate.modality.config.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.HashMap;
import java.util.Map;
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
                name = Optional.ofNullable(element.getAttribute("name"))
                    .orElseThrow(() -> new ConfigurationException("entity without name"));
                logger.warn("<entity name=\"{}\">...</entity> tag is deprecated: use <{}>...</{}>", name, name, name);
                element.removeAttribute("name");
                handleDeprecatedAttributes("entity", element, deprecatedEntityAttributes);
                break;
            }
            case "attribute":
            {
                String result = Optional.ofNullable(element.getAttribute("result"))
                    .orElseThrow(() -> new ConfigurationException("attribute without result type"));
                int slash = result.indexOf('/');
                if (slash != -1)
                {
                    name = result.substring(0, slash);
                    String resultEntity = result.substring(slash + 1);
                    element.setAttribute("result", resultEntity);
                    String attrName = Optional.ofNullable(element.getAttribute("name")).orElse(null);
                    logger.warn("<attribute name=\"{}\" result=\"{}/{}\">...</attribute> tag is deprecated: use <{} name=\"{}\" result=\"{}\">...</attribute>", attrName, name, resultEntity, name, attrName, resultEntity);
                }
                else
                {
                    name = result;
                    element.removeAttribute("result");
                    String attrName = Optional.ofNullable(element.getAttribute("name")).orElse(null);
                    logger.warn("<attribute name=\"{}\" result=\"{}\">...</attribute> tag is deprecated: use <{} name=\"{}\">...</attribute>", attrName, name, name, attrName);
                }
                handleDeprecatedAttributes("attribute", element, deprecatedAttributeAttributes);
                break;
            }
            default: // nop
        }
        return name;
    }

    private void handleDeprecatedAttributes(String nature, Element element, Map<String, String> deprecationMap)
    {
        for (Map.Entry<String, Object> attribute : getAttributesMap(element).entrySet())
        {
            String attrName = attribute.getKey();
            if (deprecationMap.containsKey(attrName))
            {
                String replacement = deprecationMap.get(attrName);
                if (replacement == null)
                {
                    logger.warn("<{}> attribute {} is deprecated", nature, attrName);
                    element.removeAttribute(attrName);
                }
                else
                {
                    logger.warn("<{}> attribute {} is deprecated, use {}", nature, attrName, replacement);
                    String value = element.getAttribute(attrName);
                    element.removeAttribute(attrName);
                    element.setAttribute(replacement, value);
                }
            }
        }
    }

    private static Map<String, String> deprecatedEntityAttributes = new HashMap<String, String>();
    private static Map<String, String> deprecatedAttributeAttributes = new HashMap<String, String>();
    static
    {
        deprecatedEntityAttributes.put("caching", null);
        deprecatedEntityAttributes.put("read-only", null);
        deprecatedEntityAttributes.put("obfuscate", null);
        deprecatedEntityAttributes.put("localize", null);

        deprecatedAttributeAttributes.put("caching", "cached");
    }
}
