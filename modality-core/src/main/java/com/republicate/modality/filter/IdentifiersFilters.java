package com.republicate.modality.filter;

import com.republicate.modality.config.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.tools.ClassUtils;
import org.atteo.evo.inflector.English;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static org.atteo.evo.inflector.English.MODE.ENGLISH_CLASSICAL;

public class IdentifiersFilters extends ConfigurableFilters<String>
{
    public IdentifiersFilters()
    {
        super("identifiers.mapping");
        addStockFilter("lowercase", x -> x.toLowerCase(Locale.ROOT));
        addStockFilter("uppercase", x -> x.toUpperCase(Locale.ROOT));
        addStockFilter("snake_to_camel", IdentifiersFilters::snakeToCamel);
        addStockFilter("plural_en", x -> enInflector.getPlural(x));
    }

    private static final String[] prefixes = { "plural", "getPlural" };

    private static final English enInflector = new English(ENGLISH_CLASSICAL);

    public static String snakeToCamel(String snake)
    {
        snake = snake.toLowerCase(Locale.ROOT);
        String[] parts = snake.split("_");
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String part : parts)
        {
            if (part.length() > 0)
            {
                builder.append(first ? part : StringUtils.capitalize(part));
                first = false;
            }
        }
        return builder.length() == 0 ? "_" : builder.toString();
    }

    public void setInflector(String inflector)
    {
        if (inflector == null || inflector.length() == 0 || inflector.equals("none"))
        {
            this.inflector = Filter.identity();
        }
        else
        {
            try
            {
                Class pluralizerClass = ClassUtils.getClass(inflector);
                Method method = null;
                for (String prefix : prefixes)
                {
                    try
                    {
                        method = ClassUtils.findSetter(prefix, pluralizerClass, x -> x == String.class);
                    }
                    catch (NoSuchMethodException nsme)
                    {
                    }
                }
                if (method == null)
                {
                    throw new ConfigurationException("invalid inflector: " + inflector);
                }
                final Method m = method;
                final Object o = pluralizerClass.newInstance();
                this.inflector = x ->
                {
                    try
                    {
                        return (String)m.invoke(o, x);
                    }
                    catch (IllegalAccessException | InvocationTargetException e)
                    {
                        throw new SQLException("could not apply inflector from class " + o.getClass().getName());
                    }
                };
            }
            catch (Exception e)
            {
                throw new ConfigurationException("could not instanciate inflector", e);
            }
        }
    }

    public String pluralize(String word) throws SQLException
    {
        return inflector.apply(word);
    }

    @Override
    protected Filter<String> stringToLeaf(String leaf)
    {
        Filter<String> ret = null;
        if (leaf.startsWith("/"))
        {
            String[] parts = leaf.substring(1).split("/");
            if (parts.length != 2)
            {
                throw new ConfigurationException("invalid regex replacement rule, expecting /search/replace/ :" + leaf);
            }
            final Pattern pattern = Pattern.compile(parts[0], Pattern.CASE_INSENSITIVE);
            final String rep = parts[1];
            return x -> pattern.matcher(x).replaceAll(rep);
        }
        return super.stringToLeaf(leaf);
    }

    public String transformTableName(String sqlTable) throws SQLException
    {
        Filter<String> filter = getTableFilter(sqlTable);
        return filter == null ? sqlTable : filter.apply(sqlTable);
    }

    public String transformColumnName(String sqlTable, String sqlColumn) throws SQLException
    {
        Filter<String> filter = getColumnFilter(sqlTable, sqlColumn);
        return filter == null ? sqlColumn : filter.apply(sqlColumn);
    }

    public String transformColumnName(String sqlColumn) throws SQLException
    {
        return getDefaultColumnFilter().apply(sqlColumn);
    }

    /**
     * Declare setMapping(String) as a synonym for addMappings(String), to allow <code>identifiers.mapping</code> in
     * configuration files
     * @param mappings identifier mappings
     */
    public void setMapping(String mappings)
    {
        addMappings(mappings);
    }

    /**
     * Declare setMapping(Map) as a synonym for addMappings(Map), to allow <code>identifiers.mapping</code> in
     * configuration files
     * @param mappings identifier mappings
     */
    public void setMapping(Map mappings)
    {
        addMappings(mappings);
    }

    private Filter<String> inflector = Filter.identity();

}
