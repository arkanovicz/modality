package com.republicate.modality.filter;

import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.util.GlobToRegex;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.tools.ClassUtils;
import org.slf4j.Logger;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class ConfigurableFilters<T extends Serializable> extends Filters<Filter<T>>
{

    @Override
    public void initialize() throws ConfigurationException
    {
        setDefaultColumnFilter(getFilter(configuration.getDefaultColumnFilter()));
    }

    /**
     * build filters
     * @param configurationPrefix configuration prefix
     */
    public ConfigurableFilters(String configurationPrefix)
    {
        this.configurationPrefix = configurationPrefix;
    }

    public void addMappings(Object obj)
    {
        if (obj instanceof String)
        {
            addMappings((String)obj);
        }
        else if (obj instanceof Map)
        {
            addMappings((Map<String, String>)obj);
        }
        else
        {
            throw new ConfigurationException("unhandled object class: " + obj.getClass().getName());
        }
    }

    public void addMappings(String value)
    {
        Map<String, Object> map = new HashMap<>();
        String[] parts = value.split("\\s*,\\s*");
        for (String part : parts)
        {
            int eq = part.indexOf('=');
            if (eq == -1)
            {
                // a single default ?
                map.put("*", part);
                map.put("*.*", part);
            }
            else
            {
                String key = part.substring(0, eq).trim();
                String val = part.substring(eq +  1).trim();
                map.put(key, val);
            }
        }
        addMappings(map);

    }

    public void addMappings(Map<String, Object> map)
    {
        map.forEach(this::addMapping);
    }

    public void addMapping(String confKey, Object value)
    {
        if (value instanceof Filter)
        {
            Pair<String, String> key = splitMappingKey(confKey);
            Filter filter = (Filter)value;
            if (key.getRight() == null)
            {
                addTableMapping(confKey, filter);
            }
            else
            {
                addColumnMapping(key.getLeft(), key.getRight(), filter);
            }
        }
        else if (value instanceof String)
        {
            addMapping(confKey, (String)value);
        }
        else
        {
            throw new ConfigurationException("invalid configuration value for key " + configurationPrefix + "." + confKey + ": type not handled: " + value.getClass().getName());
        }
    }

    public void addMapping(String confKey, String value)
    {

        List<String> filters = configuration.stringToLeaf(value);
        Pair<String, String> key = splitMappingKey(confKey);
        if (key.getRight() == null)
        {
            configuration.addTableMapping(key.getLeft(), filters);
        }
        else
        {
            configuration.addColumnMapping(key.getLeft(), key.getRight(), filters);
        }
    }

    private Pair<String, String> splitMappingKey(String confKey)
    {
        int dot = confKey.indexOf('.');
        if (dot == -1)
        {
            return Pair.of(confKey, null);
        }
        else
        {
            int otherDot = confKey.indexOf('.', dot + 1);
            if (otherDot != -1)
            {
                throw new ConfigurationException("invalid configuration key: " + configurationPrefix + "." + confKey);
            }
            String tablePattern = confKey.substring(0, dot);
            String columnPattern = confKey.substring(dot + 1);
            return Pair.of(tablePattern, columnPattern);
        }
    }

    @Override
    public Filter<T> getTableFilter(String table)
    {
        Filter<T> filter = super.getTableFilter(table);
        Filter<T> configuredFilter = getFilter(configuration.getTableFilter(table));
        return aggregate(filter, configuredFilter);
    }

    @Override
    public Filter<T> getColumnFilter(String table, String column)
    {
        Filter<T> filter = super.getColumnFilter(table, column);
        Filter<T> configuredFilter = getFilter(configuration.getColumnFilter(table, column));
        return aggregate(filter, configuredFilter);
    }

    private Filter<T> getFilter(List<String> confEntries)
    {
        Set<String> confSet = new TreeSet<>();
        Set<String> negativeSet = new TreeSet<>();
        confEntries.stream().sorted().distinct().forEach(entry ->
        {
            if (entry.charAt(0) == '-'
                ? negativeSet.add(entry.substring(1))
                : confSet.add(entry));
        });
        confSet.removeAll(negativeSet);
        Filter<T> ret = confSet.stream().map(this::stringToLeaf).reduce(this::aggregate).orElse(Filter.identity());
        return ret;
    }

    /**
     * Aggregate filters by composition.
     * @param left left filter
     * @param right right filter
     * @return composition of left and right
     */
    @Override
    protected Filter<T> aggregate(Filter<T> left, Filter<T> right)
    {
        return left == Filter.identity()
            ? right
            : right == Filter.identity()
            ? left
            : left.compose(right);
    }

    @Override
    protected Filter<T> empty()
    {
        return Filter.identity();
    }

    @Override
    protected Filter<T> stringToLeaf(String value)
    {
        Filter<T> filter = super.stringToLeaf(value);
        if (filter == null)
        {
            Class clazz = null;
            try
            {
                clazz = ClassUtils.getClass(value);
                Object obj = clazz.newInstance();
                if (obj instanceof Filter)
                {
                    filter = (Filter<T>)obj; // may throw a ClassCastException
                }
                else
                {
                    Method stringGetter = ClassUtils.findMethod(obj.getClass(), "get", String.class);
                    final Method getter = stringGetter == null ?
                        ClassUtils.findMethod(obj.getClass(), "get", Object.class) :
                        stringGetter;
                    if (getter == null)
                    {
                        throw new ConfigurationException(configurationPrefix + ": don't know what to do with class " + obj.getClass().getName());
                    }
                    filter = x ->
                    {
                        try
                        {
                            return (T)getter.invoke(obj, x);
                        }
                        catch (IllegalAccessException | InvocationTargetException e)
                        {
                            throw new SQLException("could not apply operator from class " + obj.getClass().getName());
                        }
                    };

                }
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e)
            {
                throw new ConfigurationException("cannot find stock filter for value: " + value);
            }
        }
        return filter;
    }

    /**
     * Configuration keys prefix, used for logging.
     */
    private String configurationPrefix;

    /**
     * Configuration container.
     */
    protected Filters<List<String>> configuration = new Filters<List<String>>()
    {
        /**
         * Aggregate configuration entries by merging them
         * @param left left entries
         * @param right right entries
         * @return concatenated entries
         */
        @Override
        protected List<String> aggregate(List<String> left, List<String> right)
        {
            List<String> agg = new ArrayList<>();
            agg.addAll(left);
            agg.addAll(right);
            return agg;
        }

        @Override
        protected List<String> empty()
        {
            return new ArrayList<String>();
        }

        /**
         * Just split value
         * @param value raw configuration value
         * @return
         */
        @Override
        protected List<String> stringToLeaf(String value)
        {
            return Arrays.asList(value.split("\\*,\\*"));
        }

    };
}
