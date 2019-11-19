package com.republicate.modality.filter;

import com.republicate.modality.util.Cryptograph;
import com.republicate.modality.util.TypeUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ValueFilters extends ConfigurableFilters<Serializable>
{
    public ValueFilters(String configurationPrefix)
    {
        super(configurationPrefix);
        addStockFilter("lowercase", x -> String.valueOf(x).toLowerCase(Locale.ROOT));
        addStockFilter("uppercase", x -> String.valueOf(x).toUpperCase(Locale.ROOT));
        addStockFilter("calendar_to_date", x -> x instanceof Calendar ? TypeUtils.toDate(x) : x);
        addStockFilter("date_to_calendar", x -> x instanceof java.sql.Date ? TypeUtils.toCalendar(x) : x);
        addStockFilter("number_to_boolean", x -> x instanceof Number ? ((Number)x).longValue() != 0 : x);
        addStockFilter("raw_obfuscate", x -> cryptograph.encrypt(TypeUtils.toString(x)));
        addStockFilter("raw_deobfuscate", x -> cryptograph.decrypt(TypeUtils.toBytes(x)));
        addStockFilter("obfuscate", x -> TypeUtils.base64Encode(cryptograph.encrypt(String.valueOf(x))));
        addStockFilter("deobfuscate", x -> cryptograph.decrypt(TypeUtils.base64Decode(x)));
        addStockFilter("deobfuscate_strings", x -> x instanceof String ? cryptograph.decrypt(TypeUtils.base64Decode(x)) : x);
        addStockFilter("base64_encode", x -> TypeUtils.base64Encode(x));
        addStockFilter("base64_decode", x -> TypeUtils.base64Decode(x));
        addStockFilter("mask", x -> null);
        addStockFilter("no_html", x -> StringUtils.indexOfAny(String.valueOf(x), "<>\"") == -1 ? x : error("invalid character"));
        addStockFilter("escape_html", x -> new HtmlEscaped(x));
    }

    @Override
    public void addMapping(String key, Object value)
    {
        Class clazz = null;
        try
        {
            clazz = ClassUtils.getClass(key);
        }
        catch (ClassNotFoundException cnfe) {}
        if (clazz != null)
        {
            if (value instanceof String)
            {
                addTypeMapping(clazz, stringToLeaf((String) value));
            }
            else if (value instanceof Filter)
            {
                addTypeMapping(clazz, (Filter)value);
            }
        }
        else
        {
            super.addMapping(key, value);
        }
    }

    @Override
    public void addMapping(String confKey, String value)
    {
        Class clazz = null;
        try
        {
            clazz = ClassUtils.getClass(confKey);
        }
        catch (ClassNotFoundException cnfe) {}
        if (clazz != null)
        {
            addTypeMapping(clazz, stringToLeaf(value));
        }
        else
        {
            if (value.contains("obfuscate"))
            {
                needsCryptograph = true;
            }
            super.addMapping(confKey, value);
        }

    }

    /**
     * Add a type mapping
     * @param cls target class
     * @param functor applied functor
     */
    protected void addTypeMapping(Class cls, Filter<Serializable> functor)
    {
        Filter<Serializable> previous = typesMappings.put(cls, functor);
        if (previous != null)
        {
            logger.warn("overwriting type mapping for class {}", cls.getName());
        }
    }

    /**
     * Add type mappings
     * @param mappings to add
     */
    protected void addTypeMappings(Map<Class, Filter<Serializable>> mappings)
    {
        mappings.entrySet().stream().forEach(entry -> addTypeMapping(entry.getKey(), entry.getValue()));
    }

    /**
     * Applies type mappings filters
     * @param value  value to be filtered
     * @return resulting value
     */
    public Serializable filter(Serializable value) throws SQLException
    {
        // CB TODO - this operation may pose performance problems
        Class clazz = value.getClass();
        Filter<Serializable> filter = null;
        // apply the most specific one, and cache result for intermediate classes
        List<Class> tries = null;
        while (clazz != null)
        {
            filter = typesMappings.get(clazz);
            if (filter != null)
            {
                break;
            }
            if (tries == null)
            {
                tries = new ArrayList<>();
            }
            tries.add(clazz);
            clazz = clazz.getSuperclass();
        }
        if (filter == null)
        {
            filter = Filter.identity();
        }
        if (tries != null)
        {
            for (Class tried : tries)
            {
                typesMappings.put(tried, filter);
            }
        }
        return filter.apply(value);
    }

    public void setCryptograph(Cryptograph cryptograph)
    {
        this.cryptograph = cryptograph;
    }

    // CB TODO - replace with reflection on the presence of setCryptograph, or with a subclass
    public boolean needsCryptograph()
    {
        return needsCryptograph;
    }

    @Override
    protected Filter<Serializable> stringToLeaf(String value)
    {
        if (value.contains("obfuscate"))
        {
            needsCryptograph = true;
        }
        return super.stringToLeaf(value);
    }

    protected static Serializable error(String message) throws SQLException
    {
        throw new SQLException(message);
    }

    public static class HtmlEscaped implements Serializable
    {
        public HtmlEscaped(Serializable value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return Optional.ofNullable(value).map(x -> StringEscapeUtils.escapeHtml4(String.valueOf(x))).orElse("");
        }

        private Serializable value;
    }

    private Map<Class, Filter<Serializable>> typesMappings = new ConcurrentHashMap<>();

    private Cryptograph cryptograph = null;
    private boolean needsCryptograph = false;

}

