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

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.util.introspection.IntrospectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A conversion handler adds admissible conversions between Java types whenever Velocity introspection has to map
 * VTL methods and property accessors to Java methods. This implementation is the default Conversion Handler
 * for Velocity.
 *
 * @author <a href="mailto:claude.brisson@gmail.com">Claude Brisson</a>
 * @version $Id: ConversionHandlerImpl.java $
 * @since 2.0
 */

public class ConversionHandlerImpl implements ConversionHandler
{
    protected static Logger logger = LoggerFactory.getLogger("conversion");

    /**
     * standard narrowing and string parsing conversions.
     */
    static Map<Pair<? extends Class, ? extends Class>, Converter> standardConverterMap;

    /**
     * basic toString converter
     */
    static Converter toString;

    /**
     * cache miss converter
     */
    static Converter cacheMiss;

    /**
     * min/max byte/short/int values as long
     */
    static final long minByte = Byte.MIN_VALUE, maxByte = Byte.MAX_VALUE,
        minShort = Short.MIN_VALUE, maxShort = Short.MAX_VALUE,
        minInt = Integer.MIN_VALUE, maxInt = Integer.MAX_VALUE;

    /**
     * min/max long values as double
     */
    static final double minLong = Long.MIN_VALUE, maxLong = Long.MAX_VALUE;

    static DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    static DateFormat isoTimestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * a converters cache map, initialized with the standard narrowing and string parsing conversions.
     */
    Map<Pair<? extends Class, ? extends Class>, Converter> converterCacheMap;

    static
    {
        standardConverterMap = new HashMap<>();

        cacheMiss = new Converter<Serializable>()
        {
            @Override
            public Serializable convert(Serializable o)
            {
                return o;
            }
        };

        /* number -> boolean */
        Converter<Boolean> numberToBool = new Converter<Boolean>()
        {
            @Override
            public Boolean convert(Serializable o)
            {
                return o == null ? null : ((Number) o).intValue() != 0;
            }
        };
        standardConverterMap.put(Pair.of(Boolean.class, Byte.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Short.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Integer.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Long.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Float.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Double.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Number.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Byte.TYPE), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Short.TYPE), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Integer.TYPE), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Long.TYPE), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Float.TYPE), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.class, Double.TYPE), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Byte.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Short.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Integer.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Long.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Float.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Double.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Number.class), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Byte.TYPE), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Short.TYPE), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Integer.TYPE), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Long.TYPE), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Float.TYPE), numberToBool);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Double.TYPE), numberToBool);

        /* character -> boolean */
        Converter<Boolean> charToBoolean = new Converter<Boolean>()
        {
            @Override
            public Boolean convert(Serializable o)
            {
                return o == null ? null : ((Character) o).charValue() != 0;
            }
        };
        standardConverterMap.put(Pair.of(Boolean.class, Character.class), charToBoolean);
        standardConverterMap.put(Pair.of(Boolean.class, Character.TYPE), charToBoolean);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Character.class), charToBoolean);
        standardConverterMap.put(Pair.of(Boolean.TYPE, Character.TYPE), charToBoolean);

        /* string -> boolean */
        Converter<Boolean> stringToBoolean = new Converter<Boolean>()
        {
            @Override
            public Boolean convert(Serializable o)
            {
                return Boolean.valueOf(String.valueOf(o)); // what about 'on', '1', ... + toLowerCase() ?
            }
        };
        standardConverterMap.put(Pair.of(Boolean.class, String.class), stringToBoolean);
        standardConverterMap.put(Pair.of(Boolean.TYPE, String.class), stringToBoolean);

        /* narrowing towards byte */
        Converter<Byte> narrowingToByte = new Converter<Byte>()
        {
            @Override
            public Byte convert(Serializable o)
            {
                if (o == null) return null;
                long l = ((Number)o).longValue();
                if (l < minByte || l > maxByte)
                {
                    throw new NumberFormatException("value out of range for byte type: " + l);
                }
                return ((Number) o).byteValue();
            }
        };
        standardConverterMap.put(Pair.of(Byte.class, Short.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.class, Integer.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.class, Long.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.class, Float.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.class, Double.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.class, Number.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.class, Short.TYPE), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.class, Integer.TYPE), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.class, Long.TYPE), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.class, Float.TYPE), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.class, Double.TYPE), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Short.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Integer.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Long.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Float.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Double.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Number.class), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Short.TYPE), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Integer.TYPE), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Long.TYPE), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Float.TYPE), narrowingToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Double.TYPE), narrowingToByte);

        /* string to byte */
        Converter<Byte> stringToByte = new Converter<Byte>()
        {
            @Override
            public Byte convert(Serializable o)
            {
                return Byte.valueOf(String.valueOf(o));
            }
        };
        standardConverterMap.put(Pair.of(Byte.class, String.class), stringToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, String.class), stringToByte);

        /* narrowing towards short */
        Converter<Short> narrowingToShort = new Converter<Short>()
        {
            @Override
            public Short convert(Serializable o)
            {
                if (o == null) return null;
                long l = ((Number)o).longValue();
                if (l < minShort || l > maxShort)
                {
                    throw new NumberFormatException("value out of range for short type: " + l);
                }
                return ((Number) o).shortValue();
            }
        };
        standardConverterMap.put(Pair.of(Short.class, Integer.class), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.class, Long.class), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.class, Float.class), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.class, Double.class), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.class, Number.class), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.class, Integer.TYPE), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.class, Long.TYPE), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.class, Float.TYPE), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.class, Double.TYPE), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, Integer.class), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, Long.class), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, Float.class), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, Double.class), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, Number.class), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, Integer.TYPE), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, Long.TYPE), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, Float.TYPE), narrowingToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, Double.TYPE), narrowingToShort);

        /* string to short */
        Converter<Short> stringToShort = new Converter<Short>()
        {
            @Override
            public Short convert(Serializable o)
            {
                return Short.valueOf(String.valueOf(o));
            }
        };
        standardConverterMap.put(Pair.of(Short.class, String.class), stringToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, String.class), stringToShort);

        /* narrowing towards int */
        Converter<Integer> narrowingToInteger = new Converter<Integer>()
        {
            @Override
            public Integer convert(Serializable o)
            {
                if (o == null) return null;
                long l = ((Number)o).longValue();
                if (l < minInt || l > maxInt)
                {
                    throw new NumberFormatException("value out of range for integer type: " + l);
                }
                return ((Number) o).intValue();
            }
        };
        standardConverterMap.put(Pair.of(Integer.class, Long.class), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.class, Float.class), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.class, Double.class), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.class, Number.class), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.class, Long.TYPE), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.class, Float.TYPE), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.class, Double.TYPE), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.TYPE, Long.class), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.TYPE, Float.class), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.TYPE, Double.class), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.TYPE, Number.class), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.TYPE, Long.TYPE), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.TYPE, Float.TYPE), narrowingToInteger);
        standardConverterMap.put(Pair.of(Integer.TYPE, Double.TYPE), narrowingToInteger);

        /* widening towards Integer */
        Converter<Integer> wideningToInteger = new Converter<Integer>()
        {
            @Override
            public Integer convert(Serializable o)
            {
                if (o == null) return null;
                return ((Number) o).intValue();
            }
        };
        standardConverterMap.put(Pair.of(Integer.class, Short.class), wideningToInteger);
        standardConverterMap.put(Pair.of(Integer.class, Short.TYPE), wideningToInteger);

        /* string to int */
        Converter<Integer> stringToInteger = new Converter<Integer>()
        {
            @Override
            public Integer convert(Serializable o)
            {
                return Integer.valueOf(String.valueOf(o));
            }
        };
        standardConverterMap.put(Pair.of(Integer.class, String.class), stringToInteger);
        standardConverterMap.put(Pair.of(Integer.TYPE, String.class), stringToInteger);

        /* narrowing towards long */
        Converter<Long> narrowingToLong = new Converter<Long>()
        {
            @Override
            public Long convert(Serializable o)
            {
                if (o == null) return null;
                double d = ((Number)o).doubleValue();
                if (d < minLong || d > maxLong)
                {
                    throw new NumberFormatException("value out of range for long type: " + d);
                }
                return ((Number) o).longValue();
            }
        };
        standardConverterMap.put(Pair.of(Long.class, Float.class), narrowingToLong);
        standardConverterMap.put(Pair.of(Long.class, Double.class), narrowingToLong);
        standardConverterMap.put(Pair.of(Long.class, Number.class), narrowingToLong);
        standardConverterMap.put(Pair.of(Long.class, Float.TYPE), narrowingToLong);
        standardConverterMap.put(Pair.of(Long.class, Double.TYPE), narrowingToLong);
        standardConverterMap.put(Pair.of(Long.TYPE, Float.class), narrowingToLong);
        standardConverterMap.put(Pair.of(Long.TYPE, Double.class), narrowingToLong);
        standardConverterMap.put(Pair.of(Long.TYPE, Number.class), narrowingToLong);
        standardConverterMap.put(Pair.of(Long.TYPE, Float.TYPE), narrowingToLong);
        standardConverterMap.put(Pair.of(Long.TYPE, Double.TYPE), narrowingToLong);

        /* widening towards Long */
        Converter<Long> wideningToLong = new Converter<Long>()
        {
            @Override
            public Long convert(Serializable o)
            {
                if (o == null) return null;
                return ((Number) o).longValue();
            }
        };
        standardConverterMap.put(Pair.of(Long.class, Short.class), wideningToLong);
        standardConverterMap.put(Pair.of(Long.class, Integer.class), wideningToLong);
        standardConverterMap.put(Pair.of(Long.class, Short.TYPE), wideningToLong);
        standardConverterMap.put(Pair.of(Long.class, Integer.TYPE), wideningToLong);

        /* string to long */
        Converter<Long> stringToLong = new Converter<Long>()
        {
            @Override
            public Long convert(Serializable o)
            {
                return Long.valueOf(String.valueOf(o));
            }
        };
        standardConverterMap.put(Pair.of(Long.class, String.class), stringToLong);
        standardConverterMap.put(Pair.of(Long.TYPE, String.class), stringToLong);

        /* narrowing towards float */
        Converter<Float> narrowingToFloat = new Converter<Float>()
        {
            @Override
            public Float convert(Serializable o)
            {
                return o == null ? null : ((Number) o).floatValue();
            }
        };
        standardConverterMap.put(Pair.of(Float.class, Double.class), narrowingToFloat);
        standardConverterMap.put(Pair.of(Float.class, Number.class), narrowingToFloat);
        standardConverterMap.put(Pair.of(Float.class, Double.TYPE), narrowingToFloat);
        standardConverterMap.put(Pair.of(Float.TYPE, Double.class), narrowingToFloat);
        standardConverterMap.put(Pair.of(Float.TYPE, Number.class), narrowingToFloat);
        standardConverterMap.put(Pair.of(Float.TYPE, Double.TYPE), narrowingToFloat);

        /* exact towards Float */
        Converter<Float> toFloat = new Converter<Float>()
        {
            @Override
            public Float convert(Serializable o)
            {
                if (o == null) return null;
                return ((Number) o).floatValue();
            }
        };
        standardConverterMap.put(Pair.of(Float.class, Short.class), toFloat);
        standardConverterMap.put(Pair.of(Float.class, Integer.class), toFloat);
        standardConverterMap.put(Pair.of(Float.class, Long.class), toFloat);
        standardConverterMap.put(Pair.of(Float.class, Short.TYPE), toFloat);
        standardConverterMap.put(Pair.of(Float.class, Integer.TYPE), toFloat);
        standardConverterMap.put(Pair.of(Float.class, Long.TYPE), toFloat);

        /* string to float */
        Converter<Float> stringToFloat = new Converter<Float>()
        {
            @Override
            public Float convert(Serializable o)
            {
                return Float.valueOf(String.valueOf(o));
            }
        };
        standardConverterMap.put(Pair.of(Float.class, String.class), stringToFloat);
        standardConverterMap.put(Pair.of(Float.TYPE, String.class), stringToFloat);

        /* exact or widening towards Double */
        Converter<Double> toDouble = new Converter<Double>()
        {
            @Override
            public Double convert(Serializable o)
            {
                if (o == null) return null;
                return ((Number) o).doubleValue();
            }
        };
        standardConverterMap.put(Pair.of(Double.class, Short.class), toDouble);
        standardConverterMap.put(Pair.of(Double.class, Integer.class), toDouble);
        standardConverterMap.put(Pair.of(Double.class, Long.class), toDouble);
        standardConverterMap.put(Pair.of(Double.class, Float.class), toDouble);
        standardConverterMap.put(Pair.of(Double.class, Number.class), toDouble);
        standardConverterMap.put(Pair.of(Double.class, Short.TYPE), toDouble);
        standardConverterMap.put(Pair.of(Double.class, Integer.TYPE), toDouble);
        standardConverterMap.put(Pair.of(Double.class, Long.TYPE), toDouble);
        standardConverterMap.put(Pair.of(Double.class, Float.TYPE), toDouble);
        standardConverterMap.put(Pair.of(Double.TYPE, Number.class), toDouble);

        /* string to double */
        Converter<Double> stringToDouble = new Converter<Double>()
        {
            @Override
            public Double convert(Serializable o)
            {
                return Double.valueOf(String.valueOf(o));
            }
        };
        standardConverterMap.put(Pair.of(Double.class, String.class), stringToDouble);
        standardConverterMap.put(Pair.of(Double.TYPE, String.class), stringToDouble);

        /* boolean to byte */
        Converter<Byte> booleanToByte = new Converter<Byte>()
        {
            @Override
            public Byte convert(Serializable o)
            {
                return o == null ? null : (Boolean) o ? (byte)1 : (byte)0;
            }
        };
        standardConverterMap.put(Pair.of(Byte.class, Boolean.class), booleanToByte);
        standardConverterMap.put(Pair.of(Byte.class, Boolean.TYPE), booleanToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Boolean.class), booleanToByte);
        standardConverterMap.put(Pair.of(Byte.TYPE, Boolean.TYPE), booleanToByte);

        /* boolean to short */
        Converter<Short> booleanToShort = new Converter<Short>()
        {
            @Override
            public Short convert(Serializable o)
            {
                return o == null ? null : (Boolean) o ? (short)1 : (short)0;
            }
        };
        standardConverterMap.put(Pair.of(Short.class, Boolean.class), booleanToShort);
        standardConverterMap.put(Pair.of(Short.class, Boolean.TYPE), booleanToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, Boolean.class), booleanToShort);
        standardConverterMap.put(Pair.of(Short.TYPE, Boolean.TYPE), booleanToShort);

        /* boolean to integer */
        Converter<Integer> booleanToInteger = new Converter<Integer>()
        {
            @Override
            public Integer convert(Serializable o)
            {
                return o == null ? null : (Boolean) o ? (Integer)1 : (Integer)0;
            }
        };
        standardConverterMap.put(Pair.of(Integer.class, Boolean.class), booleanToInteger);
        standardConverterMap.put(Pair.of(Integer.class, Boolean.TYPE), booleanToInteger);
        standardConverterMap.put(Pair.of(Integer.TYPE, Boolean.class), booleanToInteger);
        standardConverterMap.put(Pair.of(Integer.TYPE, Boolean.TYPE), booleanToInteger);

        /* boolean to lonf */
        Converter<Long> booleanToLong = new Converter<Long>()
        {
            @Override
            public Long convert(Serializable o)
            {
                return o == null ? null : ((Boolean)o).booleanValue() ? 1L : 0L;
            }
        };
        standardConverterMap.put(Pair.of(Long.class, Boolean.class), booleanToLong);
        standardConverterMap.put(Pair.of(Long.class, Boolean.TYPE), booleanToLong);
        standardConverterMap.put(Pair.of(Long.TYPE, Boolean.class), booleanToLong);
        standardConverterMap.put(Pair.of(Long.TYPE, Boolean.TYPE), booleanToLong);

        /* to string */
        toString = new Converter<String>()
        {
            @Override
            public String convert(Serializable o)
            {
                return String.valueOf(o);
            }
        };

        /* string to locale */
        Converter<Locale> stringToLocale = new Converter<Locale>()
        {
            @Override
            public Locale convert(Serializable o)
            {
                return o == null ? null : LocaleUtils.toLocale((String)o);
            }
        };
        standardConverterMap.put(Pair.of(Locale.class, String.class), stringToLocale);

        /* conversions of date/time types towards sql types */

        Converter<java.sql.Date> longToSqlDate =
                new Converter<java.sql.Date>()
                {
                    public java.sql.Date convert(Serializable o)
                    {
                        return new java.sql.Date((Long)o);
                    }
                };
        standardConverterMap.put(Pair.of(java.sql.Date.class, Long.class), longToSqlDate);
        standardConverterMap.put(Pair.of(java.sql.Date.class, Long.TYPE), longToSqlDate);
        standardConverterMap.put(Pair.of(java.sql.Date.class, java.util.Date.class),
                new Converter<java.sql.Date>()
                {
                    public java.sql.Date convert(Serializable o)
                    {
                        return new java.sql.Date(((java.util.Date)o).getTime());
                    }
                });
        standardConverterMap.put(Pair.of(java.sql.Date.class, Calendar.class),
                new Converter<java.sql.Date>()
                {
                    public java.sql.Date convert(Serializable o)
                    {
                        return new java.sql.Date(((Calendar)o).getTime().getTime());
                    }
                });

        Converter<java.sql.Time> longToSqlTime =
                new Converter<java.sql.Time>()
                {
                    public java.sql.Time convert(Serializable o)
                    {
                        return new java.sql.Time((Long)o);
                    }
                };

        standardConverterMap.put(Pair.of(java.sql.Time.class, Long.class), longToSqlTime);
        standardConverterMap.put(Pair.of(java.sql.Time.class, Long.TYPE), longToSqlTime);

        Converter<java.sql.Timestamp> longToSqlTimestamp =
                new Converter<java.sql.Timestamp>()
                {
                    public java.sql.Timestamp convert(Serializable o)
                    {
                        return new java.sql.Timestamp((Long)o);
                    }
                };
        standardConverterMap.put(Pair.of(java.sql.Timestamp.class, Long.class), longToSqlTimestamp);
        standardConverterMap.put(Pair.of(java.sql.Timestamp.class, Long.TYPE), longToSqlTimestamp);
        standardConverterMap.put(Pair.of(java.sql.Timestamp.class, java.util.Date.class),
                new Converter<java.sql.Timestamp>()
                {
                    public java.sql.Timestamp convert(Serializable o)
                    {
                        return new java.sql.Timestamp(((java.util.Date)o).getTime());
                    }
                });
        standardConverterMap.put(Pair.of(java.sql.Timestamp.class, Calendar.class),
                new Converter<java.sql.Timestamp>()
                {
                    public java.sql.Timestamp convert(Serializable o)
                    {
                        return new java.sql.Timestamp(((Calendar)o).getTime().getTime());
                    }
                });

        /* Conversion between date and calendar types */

        standardConverterMap.put(Pair.of(java.util.Date.class, Calendar.class),
                new Converter<java.util.Date>()
                {
                    public java.util.Date convert(Serializable o)
                    {
                        return ((Calendar)o).getTime();
                    }
                });

        /* TODO - Date -> Calendar requires more work... */

        /* Conversion from sql date/time types */

        Converter<Long> sqlDateToLong =
                new Converter<Long>()
                {
                    public Long convert(Serializable o)
                    {
                        return ((java.sql.Date)o).getTime();
                    }
                };
        standardConverterMap.put(Pair.of(Long.class, java.sql.Date.class), longToSqlDate);
        standardConverterMap.put(Pair.of(Long.TYPE, java.sql.Date.class), longToSqlDate);
        Converter<Long> sqlTimeToLong =
                new Converter<Long>()
                {
                    public Long convert(Serializable o)
                    {
                        return ((java.sql.Time)o).getTime();
                    }
                };

        standardConverterMap.put(Pair.of(Long.class, java.sql.Time.class), longToSqlTime);
        standardConverterMap.put(Pair.of(Long.TYPE, java.sql.Time.class), longToSqlTime);

        Converter<Long> sqlTimestampToLong =
                new Converter<Long>()
                {
                    public Long convert(Serializable o)
                    {
                        return ((java.sql.Timestamp)o).getTime();
                    }
                };
        standardConverterMap.put(Pair.of(java.sql.Timestamp.class, Long.class), longToSqlTimestamp);
        standardConverterMap.put(Pair.of(java.sql.Timestamp.class, Long.TYPE), longToSqlTimestamp);
        standardConverterMap.put(Pair.of(java.sql.Timestamp.class, java.util.Date.class),
                new Converter<java.sql.Timestamp>()
                {
                    public java.sql.Timestamp convert(Serializable o)
                    {
                        return new java.sql.Timestamp(((java.util.Date)o).getTime());
                    }
                });
        standardConverterMap.put(Pair.of(java.sql.Timestamp.class, Calendar.class),
                new Converter<java.sql.Timestamp>()
                {
                    public java.sql.Timestamp convert(Serializable o)
                    {
                        return new java.sql.Timestamp(((Calendar)o).getTime().getTime());
                    }
                });

        /* String to date/time types, default to ISO format */

        Converter<java.sql.Date> stringToDate = new Converter<java.sql.Date>()
        {
          public java.sql.Date convert(Serializable o)
          {
              try
              {
                  return new java.sql.Date(isoDateFormat.parse(String.valueOf(o)).getTime());
              }
              catch (ParseException pe)
              {
                  logger.warn("could not parse '{}' into an iso date", o);
                  return null;
              }
          }
        };
        standardConverterMap.put(Pair.of(java.sql.Date.class, String.class), stringToDate);

        Converter<java.sql.Timestamp> stringToTimestamp = new Converter<java.sql.Timestamp>()
        {
            public java.sql.Timestamp convert(Serializable o)
            {
                try
                {
                    return new java.sql.Timestamp(isoTimestampFormat.parse(String.valueOf(o)).getTime());
                }
                catch (ParseException pe)
                {
                    logger.warn("could not parse '{}' into an iso timestamp", o);
                    return null;
                }
            }
        };
        standardConverterMap.put(Pair.of(java.sql.Timestamp.class, String.class), stringToTimestamp);
    }

    /**
     * Constructor
     */
    public ConversionHandlerImpl()
    {
        converterCacheMap = new ConcurrentHashMap<>();
    }

    /**
     * Check to see if the conversion can be done using an explicit conversion
     * @param actual found argument type
     * @param formal expected formal type
     * @return null if no conversion is needed, or the appropriate Converter object
     * @since 2.0
     */
    @Override
    public boolean isExplicitlyConvertible(Class formal, Class actual, boolean possibleVarArg)
    {
        /*
         * for consistency, we also have to check standard implicit convertibility
         * since it may not have been checked before by the calling code
         */
        if (formal == actual ||
            IntrospectionUtils.isMethodInvocationConvertible(formal, actual, possibleVarArg) ||
            getNeededConverter(formal, actual) != null)
        {
            return true;
        }

        /* Check var arg */
        if (possibleVarArg && formal.isArray())
        {
            if (actual.isArray())
            {
                actual = actual.getComponentType();
            }
            return isExplicitlyConvertible(formal.getComponentType(), actual, false);
        }
        return false;
    }


    /**
     * Returns the appropriate Converter object needed for an explicit conversion
     * Returns null if no conversion is needed.
     *
     * @param actual found argument type
     * @param formal expected formal type
     * @return null if no conversion is needed, or the appropriate Converter object
     * @since 2.0
     */
    @Override
    public Converter getNeededConverter(final Class formal, final Class actual)
    {
        Pair<Class, Class> key = Pair.of(formal, actual);

        /* first check for a standard conversion */
        Converter converter = standardConverterMap.get(key);
        if (converter == null)
        {
            /* then the converters cache map */
            converter = converterCacheMap.get(key);
            if (converter == null)
            {
                /* check for conversion towards string */
                if (formal == String.class)
                {
                    converter = toString;
                }
                /* check for String -> Enum constant conversion */
                else if (formal.isEnum() && actual == String.class)
                {
                    converter = new Converter()
                    {
                        @Override
                        public Serializable convert(Serializable o)
                        {
                            return Enum.valueOf((Class<Enum>) formal, (String) o);
                        }
                    };
                }

                converterCacheMap.put(key, converter == null ? cacheMiss : converter);
            }
        }
        return converter == cacheMiss ? null : converter;
    }

    /**
     * Add the given converter to the handler.
     *
     * @param formal expected formal type
     * @param actual provided argument type
     * @param converter converter
     * @since 2.0
     */
    @Override
    public void addConverter(Class formal, Class actual, Converter converter)
    {
        Pair<Class, Class> key = Pair.of(formal, actual);
        converterCacheMap.put(key, converter);
        if (formal.isPrimitive())
        {
            key = Pair.of(IntrospectionUtils.getBoxedClass(formal), actual);
            converterCacheMap.put(key, converter);
        }
        else
        {
            Class unboxedFormal = IntrospectionUtils.getUnboxedClass(formal);
            if (unboxedFormal != formal)
            {
                key = Pair.of(unboxedFormal, actual);
                converterCacheMap.put(key, converter);
            }
        }
    }


}
