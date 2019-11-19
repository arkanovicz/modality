package com.republicate.modality.filter;

/**
 * <p>Generic container for values & identifiers mappings, handling three distinct mappings:</p>
 * <ul>
 *     <li>A <i>tables mappings</i> list from patterns (matched against table names) to functors</li>
 *     <li>A <i>columns mapping</i> list from pairs of patterns (matched against table and column names) to functors</li>
 * </ul>
 */

import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.util.GlobToRegex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 *
 * @param <V> Type of mapped values (String for configuration, Filter&lt;T&gt; afterwards)
 */
public abstract class Filters<V>
{
    protected static Logger logger = LoggerFactory.getLogger("modality");

    public void initialize() throws ConfigurationException
    {
    }

    /**
     * Returns empty value
     * @return empty value
     */
    protected abstract V empty();

    /**
     * Abstract function to aggregate filters
     * @param left left filter
     * @param right right filter
     * @return aggregated filter
     */
    protected abstract V aggregate(V left, V right);

    protected V stringToLeaf(String value)
    {
        return stockFilters.get(value);
    }

    protected Pattern getPattern(String key)
    {
        return Pattern.compile(GlobToRegex.toRegex(key, "."), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Add a table mapping
     * @param key glob pattern of table name
     * @param value mapped filter(s)
     */
    public void addTableMapping(String key, V value)
    {
        tablesMappings.merge(getPattern(key), value, this::aggregate);
    }

    /**
     * Add a column mapping
     * @param tableKey glob pattern of table name
     * @param columnKey glob pattern of column name
     * @param value mapped filter(s)
     */
    public void addColumnMapping(String tableKey, String columnKey, V value)
    {
        Pattern tablePattern = getPattern(tableKey), columnPattern = getPattern(columnKey);
        Map<Pattern, V> childMap = columnsMappings.computeIfAbsent(tablePattern, k -> new TreeMap<>(stringComparator));
        childMap.merge(columnPattern, value, this::aggregate);
        if ("*".equals(tableKey) && "*".equals(columnKey))
        {
            defaultColumnFilter = value;
        }
    }

    public Filters<V> addStockFilter(String name, V value)
    {
        V prev = stockFilters.put(name, value);
        if (prev != null)
        {
            logger.warn("overriding stock filter {}", name);
        }
        return this;
    }

    public V getTableFilter(String table)
    {
        return tablesMappings.entrySet().stream()
            .filter(entry -> entry.getKey().matcher(table).matches())
            .map(entry -> entry.getValue())
            .reduce(this::aggregate)
            .orElse(empty());
    }

    public V getColumnFilter(String table, String column)
    {
        return columnsMappings.entrySet().stream()
            .filter(entry -> entry.getKey().matcher(table).matches())
            .flatMap(entry -> entry.getValue().entrySet().stream())
            .filter(entry -> entry.getKey().matcher(column).matches())
            .map(entry -> entry.getValue())
            .reduce(this::aggregate)
            .orElse(empty());
    }

    public V getDefaultColumnFilter()
    {
        return defaultColumnFilter;
    }

    private Comparator<Pattern> stringComparator = (o1, o2) -> String.valueOf(o1).compareTo(String.valueOf(o2));

    /**
     * Table mappings list, indexed by key string representation
     */
    private Map<Pattern, V> tablesMappings = new TreeMap<>(stringComparator);

    /**
     * Column mappings list, indexed by key string representation
     */
    private Map<Pattern, Map<Pattern, V>> columnsMappings = new TreeMap<>(stringComparator);

    /**
     * Stock filters by name
     */
    private Map<String, V> stockFilters = new TreeMap<>();

    /**
     * Default columns filtering
     */
    private V defaultColumnFilter = empty();
}
