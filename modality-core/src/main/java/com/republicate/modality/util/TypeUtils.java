package com.republicate.modality.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeUtils
{
    protected static Logger logger = LoggerFactory.getLogger("sql-types");

    public static String toString(Object value)
    {
        return value == null ? null : value.toString();
    }

    public static Character toChar(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Character)
        {
            return (Character)value;
        }
        if (value instanceof Boolean)
        {
            return ((Boolean)value).booleanValue()
                ? 't'
                : 'f';
        }
        if (value instanceof String)
        {
            switch (((String) value).length())
            {
                case 0:
                    logger.warn("empty string cannot be converted to char, returning null");
                    return null;
                case 1:
                    return ((String)value).charAt(0);
                default:
                    logger.warn("string '{}' truncated when converted to char", value);
                    return ((String)value).charAt(0);
            }
        }
        logger.warn("class {} cannot be converted to char, returning null", value.getClass().getName());
        return null;
    }

    public static Boolean toBoolean(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Boolean)
        {
            return (Boolean)value;
        }
        if (value instanceof String)
        {
            String str = (String)value;
            if ("true".equals(str))
            {
                return true;
            }
            if ("false".equals(str))
            {
                return false;
            }
            try
            {
                value = Long.valueOf(str);
            }
            catch (NumberFormatException nfe)
            {
                logger.warn("string '{}' cannot be converted to boolean, returning false", value);
                return false;
            }
        }
        if (value instanceof Number)
        {
            return ((Number)value).longValue() != 0l;
        }
        logger.warn("class {} cannot be converted to boolean, returning false", value.getClass().getName());
        return false;
    }

    public static Byte toByte(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number)value).byteValue();
        }
        if (value instanceof String)
        {
            try
            {
                return Byte.valueOf((String)value);
            }
            catch (NumberFormatException nfe)
            {
                logger.warn("string '{}'  cannot be converted to byte, returning null", value);
                return null;
            }
        }
        logger.warn("class {} cannot be converted to byte, returning null", value.getClass().getName());
        return null;
    }


    public static Short toShort(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number)value).shortValue();
        }
        if (value instanceof String)
        {
            try
            {
                return Short.valueOf((String)value);
            }
            catch (NumberFormatException nfe)
            {
                logger.warn("string '{}'  cannot be converted to short, returning null", value);
                return null;
            }
        }
        logger.warn("class {} cannot be converted to short, returning null", value.getClass().getName());
        return null;
    }

    public static Integer toInteger(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number)value).intValue();
        }
        if (value instanceof String)
        {
            try
            {
                return Integer.valueOf((String)value);
            }
            catch (NumberFormatException nfe)
            {
                logger.warn("string '{}'  cannot be converted to integer, returning null", value);
                return null;
            }
        }
        logger.warn("class {} cannot be converted to integer, returning null", value.getClass().getName());
        return null;
    }

    public static Long toLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number)value).longValue();
        }
        if (value instanceof String)
        {
            try
            {
                return Long.valueOf((String)value);
            }
            catch (NumberFormatException nfe)
            {
                logger.warn("string '{}'  cannot be converted to long, returning null", value);
                return null;
            }
        }
        logger.warn("class {} cannot be converted to long, returning null", value.getClass().getName());
        return null;
    }

    public static Float toFloat(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number)value).floatValue();
        }
        if (value instanceof String)
        {
            try
            {
                return Float.valueOf((String)value);
            }
            catch (NumberFormatException nfe)
            {
                logger.warn("string '{}'  cannot be converted to float, returning null", value);
                return null;
            }
        }
        logger.warn("class {} cannot be converted to float, returning null", value.getClass().getName());
        return null;
    }

    public static Double toDouble(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number)value).doubleValue();
        }
        if (value instanceof String)
        {
            try
            {
                return Double.valueOf((String)value);
            }
            catch (NumberFormatException nfe)
            {
                logger.warn("string '{}'  cannot be converted to double, returning null", value);
                return null;
            }
        }
        logger.warn("class {} cannot be converted to double, returning null", value.getClass().getName());
        return null;
    }

    public static Date toDate(Object value)
    {
        if (value == null || value instanceof Date)
        {
            return (Date)value;
        }
        if (value instanceof Calendar)
        {
            return ((Calendar)value).getTime();
        }
        logger.warn("class {} cannot be converted to date, returning null", value.getClass().getName());
        return null;
    }

    public static Calendar toCalendar(Object value)
    {
        if (value == null || value instanceof Calendar)
        {
            return (Calendar)value;
        }
        if (value instanceof Date)
        {
            // CB TODO - use model locale
            Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTime((Date)value);
            return calendar;
        }
        logger.warn("class {} cannot be converted to calendar, returning null", value.getClass().getName());
        return null;
    }

    public static byte[] toBytes(Object value)
    {
        if (value == null || value instanceof byte[])
        {
            return (byte[])value;
        }
        return String.valueOf(value).getBytes(StandardCharsets.UTF_8);
    }

    public static String base64Encode(Object value)
    {
        if (value == null)
        {
            return null;
        }
        byte[] decoded = toBytes(value);
        byte[] encoded = Base64.encodeBase64URLSafe(decoded);
        return new String(encoded, StandardCharsets.UTF_8);
    }

    public static byte[] base64Decode(Object value)
    {
        if (value == null)
        {
            return null;
        }
        String encoded = toString(value);
        return Base64.decodeBase64(encoded);

    }

    /**
     * boxing helper maps for standard types
     */
    private static Map<Class, Class> boxingMap, unboxingMap;

    static
    {
        boxingMap = new HashMap<Class, Class>();
        boxingMap.put(Boolean.TYPE, Boolean.class);
        boxingMap.put(Character.TYPE, Character.class);
        boxingMap.put(Byte.TYPE, Byte.class);
        boxingMap.put(Short.TYPE, Short.class);
        boxingMap.put(Integer.TYPE, Integer.class);
        boxingMap.put(Long.TYPE, Long.class);
        boxingMap.put(Float.TYPE, Float.class);
        boxingMap.put(Double.TYPE, Double.class);

        unboxingMap = new HashMap<Class, Class>();
        for (Map.Entry<Class,Class> entry : (Set<Map.Entry<Class,Class>>)boxingMap.entrySet())
        {
            unboxingMap.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * returns boxed type (or input type if not a primitive type)
     * @param clazz input class
     * @return boxed class
     */
    public static Class getBoxedClass(Class clazz)
    {
        Class boxed = boxingMap.get(clazz);
        return boxed == null ? clazz : boxed;
    }

    /**
     * returns unboxed type (or input type if not successful)
     * @param clazz input class
     * @return unboxed class
     */
    public static Class getUnboxedClass(Class clazz)
    {
        Class unboxed = unboxingMap.get(clazz);
        return unboxed == null ? clazz : unboxed;
    }

    /**
     * Determines whether a type represented by a class object is
     * convertible to another type represented by a class object using a
     * method invocation conversion, treating object types of primitive
     * types as if they were primitive types (that is, a Boolean actual
     * parameter type matches boolean primitive formal type). This behavior
     * is because this method is used to determine applicable methods for
     * an actual parameter list, and primitive types are represented by
     * their object duals in reflective method calls.
     *
     * @param formal the formal parameter type to which the actual
     * parameter type should be convertible
     * @param actual the actual parameter type.
     * @return true if either formal type is assignable from actual type,
     * or formal is a primitive type and actual is its corresponding object
     * type or an object type of a primitive type that can be converted to
     * the formal type.
     */
    public static boolean isMethodInvocationConvertible(Class formal, Class actual)
    {
        /* if it's a null, it means the arg was null */
        if (actual == null)
        {
            return !formal.isPrimitive();
        }

        /* Check for identity or widening reference conversion */
        if (formal.isAssignableFrom(actual))
        {
            return true;
        }

        /* 2.0: Since MethodMap's comparison functions now use this method with potentially reversed arguments order,
         * actual can be a primitive type. */

        /* Check for boxing */
        if (!formal.isPrimitive() && actual.isPrimitive())
        {
            Class boxed = boxingMap.get(actual);
            if (boxed != null && boxed == formal || formal.isAssignableFrom(boxed)) return true;
        }

        if (formal.isPrimitive())
        {
            if (actual.isPrimitive())
            {
                /* check for widening primitive conversion */
                if (formal == Short.TYPE && actual == Byte.TYPE)
                    return true;
                if (formal == Integer.TYPE && (
                    actual == Byte.TYPE || actual == Short.TYPE))
                    return true;
                if (formal == Long.TYPE && (
                    actual == Byte.TYPE || actual == Short.TYPE || actual == Integer.TYPE))
                    return true;
                if (formal == Float.TYPE && (
                    actual == Byte.TYPE || actual == Short.TYPE || actual == Integer.TYPE ||
                        actual == Long.TYPE))
                    return true;
                if (formal == Double.TYPE && (
                    actual == Byte.TYPE || actual == Short.TYPE || actual == Integer.TYPE ||
                        actual == Long.TYPE || actual == Float.TYPE))
                    return true;
            }
            else
            {
                /* Check for unboxing with widening primitive conversion. */
                if (formal == Boolean.TYPE && actual == Boolean.class)
                    return true;
                if (formal == Character.TYPE && actual == Character.class)
                    return true;
                if (formal == Byte.TYPE && actual == Byte.class)
                    return true;
                if (formal == Short.TYPE && (actual == Short.class || actual == Byte.class))
                    return true;
                if (formal == Integer.TYPE && (actual == Integer.class || actual == Short.class ||
                    actual == Byte.class))
                    return true;
                if (formal == Long.TYPE && (actual == Long.class || actual == Integer.class ||
                    actual == Short.class || actual == Byte.class))
                    return true;
                if (formal == Float.TYPE && (actual == Float.class || actual == Long.class ||
                    actual == Integer.class || actual == Short.class || actual == Byte.class))
                    return true;
                if (formal == Double.TYPE && (actual == Double.class || actual == Float.class ||
                    actual == Long.class || actual == Integer.class || actual == Short.class ||
                    actual == Byte.class))
                    return true;
            }
        }
        return false;

    }


    /**
     * Check if two values represent the same value even with different types
     * @param left left value
     * @param right right value
     * @return true if both values will map to the same value in the database
     */
    public static boolean sameValues(Serializable left, Serializable right)
    {
        if (left == null)
        {
            return right == null;
        }
        if (right == null)
        {
            return left == null;
        }
        if (left instanceof Number && right instanceof Number)
        {
            return ((Number)left).doubleValue() == ((Number)right).doubleValue();
        }
        // resort to string comparison
        return String.valueOf(left).equals(String.valueOf(right));
    }
}
