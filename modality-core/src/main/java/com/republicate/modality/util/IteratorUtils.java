package com.republicate.modality.util;

import com.republicate.modality.Instance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public class IteratorUtils
{
    /* *
     * Gets all rows in a list of instances.
     *
     * @return a list of all the rows
     */
    public static List<Instance> toList(Iterator<Instance> iterator)
    {
        List<Instance> ret = new ArrayList<Instance>();
        while (iterator.hasNext())
        {
            ret.add(iterator.next());
        }
        return ret;
    }

    /**
     * Get all rows indexed in a map by a key column
     * @param keyColumn
     * @param iterator
     * @return instances indexed by key column
     */
    public static NavigableMap<Serializable, Instance> toMap(String keyColumn, Iterator<Instance> iterator)
    {
        NavigableMap<Serializable, Instance> ret = new TreeMap<>();
        while (iterator.hasNext())
        {
            Instance instance = iterator.next();
            Serializable key = instance.get(keyColumn);
            Instance previous = ret.put(key, instance);
            if (previous == null)
            {
                throw new RuntimeException("toMap(): column " + keyColumn + " is not a key");
            }
        }
        return ret;
    }

    /**
     * Get all rows grouped by a specific column value
     * @param column
     * @param iterator
     * @return grouped instances
     */
    public static NavigableMap<Serializable, List<Instance>> toGroupsMap(String column, Iterator<Instance> iterator)
    {
        NavigableMap<Serializable, List<Instance>> ret = new TreeMap<>();
        while (iterator.hasNext())
        {
            Instance instance = iterator.next();
            Serializable value = instance.get(column);
            List lst = ret.get(value);
            if (lst == null)
            {
                lst = new ArrayList<Instance>();
                ret.put(value, lst);
            }
            lst.add(instance);
        }
        return ret;
    }
}
