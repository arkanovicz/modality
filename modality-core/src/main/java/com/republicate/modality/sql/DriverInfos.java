package com.republicate.modality.sql;

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

import com.republicate.modality.config.ConfigurationException;
import com.republicate.modality.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * <p>Contains specific description and behaviour of jdbc drivers.</p>
 *
 * <p>Main sources:</p>
 * <ul>
 * <li>http://www.schemaresearch.com/products/srtransport/doc/modules/jdbcconf.html
 * <li>http://db.apache.org/torque/ and org.apache.torque.adapter classes
 * </ul>
 *
 *  @author <a href="mailto:claude.brisson@gmail.com">Claude Brisson</a>
 */
public class DriverInfos implements Constants, Serializable
{
    protected static Logger logger = LoggerFactory.getLogger("sql");

    /*
     * Constructors
     */

    public DriverInfos()
    {
    }

    public void setDefaults(DriverInfos other)
    {
        setTag(Optional.ofNullable(getTag()).orElse(other.getTag()));
        setCatalog(Optional.ofNullable(getCatalog()).orElse(other.getCatalog()));
        setPingQuery(Optional.ofNullable(getPingQuery()).orElse(other.getPingQuery()));
        setTablesCaseSensitivity(Optional.ofNullable(getTablesCaseSensitivity()).orElse(other.getTablesCaseSensitivity()));
        setSchemaQuery(Optional.ofNullable(getSchemaQuery()).orElse(other.getSchemaQuery()));
        setLastInsertIdPolicy(Optional.ofNullable(getLastInsertIdPolicyString()).orElse(Optional.ofNullable(other.getLastInsertIdPolicyString()).orElse("none")));
        setStrictColumnTypes(Optional.ofNullable(isStrictColumnTypes()).orElse(Optional.ofNullable(other.isStrictColumnTypes()).orElse(false)));
        setColumnMarkers(Optional.ofNullable(hasColumnMarkers()).orElse(Optional.ofNullable(other.hasColumnMarkers()).orElse(false)));
        Pattern ignoreTablesPattern = Optional.ofNullable(getIgnoreTablesPattern()).orElse(other.getIgnoreTablesPattern());
        setIgnoreTablesPattern(ignoreTablesPattern == null ? null : ignoreTablesPattern.toString());
        Character idQuoteChar = Optional.of(getIdentifierQuoteChar()).orElse(other.getIdentifierQuoteChar());
        setIdentifierQuoteChar(idQuoteChar == null ? null : String.valueOf(idQuoteChar));
    }

    public void log()
    {
        logger.info("driver tag: {}", getTag());
        logger.info("driver catalog: {}", getCatalog());
        logger.info("driver ping query: {}", getPingQuery());
        logger.info("driver tables case sensivity: {}", getTablesCaseSensitivity());
        logger.info("driver schema query: {}", getSchemaQuery());
        logger.info("driver last insert id policy: {}", getLastInsertIdPolicy());
        logger.info("driver strict column types: {}", isStrictColumnTypes());
        logger.info("driver column markers: {}", hasColumnMarkers());
        logger.info("driver ignore tables pattern: {}", getIgnoreTablesPattern());
        logger.info("driver identifier quote char: <{}>", getIdentifierQuoteChar());
    }

    /*
     * Getters and setters
     */

    public String getTag()
    {
        return tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public String getCatalog()
    {
        return catalog;
    }

    public void setCatalog(String catalog)
    {
        this.catalog = catalog;
    }

    public String getPingQuery()
    {
        return pingQuery;
    }

    public void setPingQuery(String query)
    {
        this.pingQuery = pingQuery;
    }

    public CaseSensitivity getTablesCaseSensitivity()
    {
        return tablesCaseSensitivity;
    }

    public void setTablesCaseSensitivity(CaseSensitivity tablesCaseSensitivity)
    {
        this.tablesCaseSensitivity = tablesCaseSensitivity;
        switch (tablesCaseSensitivity)
        {
            case LOWERCASE: filterTableName = t -> t.toLowerCase(Locale.ROOT); break;
            case UPPERCASE: filterTableName = t -> t.toUpperCase(Locale.ROOT); break;
            default: filterTableName = t -> t;
        }
    }

    public String getSchemaQuery()
    {
        return schemaQuery;
    }

    public void setSchemaQuery(String schemaQuery)
    {
        this.schemaQuery = schemaQuery;
    }

    public LastInsertIdPolicy getLastInsertIdPolicy()
    {
        return lastInsertIdPolicy;
    }

    public String getLastInsertIdPolicyString()
    {
        if (lastInsertIdPolicy == null)
        {
            return null;
        }
        switch (lastInsertIdPolicy)
        {
            case NONE: return "none";
            case GENERATED_KEYS: return "generated_keys";
            case METHOD: return "method:" + getLastInsertIdMethod();
            case QUERY: return "query:" + getLastInsertIdQuery();
            default: return null;
        }
    }

    static Pattern classMethodPattern = Pattern.compile("((?:\\w+\\.)*\\w+\\.\\w)(?:\\(\\))?");

    public void setLastInsertIdPolicy(String policy)
    {
        policy = policy.trim();
        if (policy.startsWith("query:"))
        {
            lastInsertIdPolicy = LastInsertIdPolicy.QUERY;
            lastInsertIdQuery = policy.substring(6).trim();
        }
        else if (policy.startsWith("method:"))
        {
            lastInsertIdPolicy = LastInsertIdPolicy.METHOD;
            lastInsertIdMethod = policy.substring(7).trim();
        }
        else
        {
            lastInsertIdPolicy = LastInsertIdPolicy.valueOf(policy.toUpperCase());
            if (lastInsertIdPolicy == LastInsertIdPolicy.RETURNING)
            {
                throw new ConfigurationException("'returning' last insert id policy is not yet supported");
            }
        }
    }

    public String getLastInsertIdQuery()
    {
        return lastInsertIdQuery;
    }

    public String getLastInsertIdMethod()
    {
        return lastInsertIdMethod;
    }

    public Boolean isStrictColumnTypes()
    {
        return strictColumnTypes;
    }

    public void setStrictColumnTypes(boolean strictColumnTypes)
    {
        this.strictColumnTypes = strictColumnTypes;
    }

    public Pattern getIgnoreTablesPattern()
    {
        return ignoreTablesPattern;
    }

    public void setIgnoreTablesPattern(String pattern)
    {
        if (pattern != null && pattern.length() > 0)
        {
            ignoreTablesPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }
        else
        {
            ignoreTablesPattern = null;
        }
    }

    public Character getIdentifierQuoteChar()
    {
        return identifierQuoteChar;
    }

    public void setIdentifierQuoteChar(String identifierQuoteChar)
    {
        identifierQuoteChar = identifierQuoteChar.trim();
        if (identifierQuoteChar.length() > 0)
        {
            this.identifierQuoteChar = identifierQuoteChar.charAt(0);
        }
    }

    public Boolean hasColumnMarkers()
    {
        return columnMarkers;
    }

    public void setColumnMarkers(boolean columnMarkers)
    {
        this.columnMarkers = columnMarkers;
    }

    /*
     * Operations
     */

    public String getTableName(String entityName)
    {
        return filterTableName.apply(entityName);
    }

    public String quoteIdentifier(String id)
    {
        if (identifierQuoteChar == ' ')
        {
            return id;
        }
        else
        {
            return identifierQuoteChar + id + identifierQuoteChar;
        }
    }

    public String getDescribeEnumQuery()
    {
        return describeEnumQuery;
    }

    public String getDescribeEnumPattern()
    {
        return describeEnumPattern;
    }

    public void setDescribeEnum(String describeEnum)
    {
        int sep = describeEnum.indexOf('|');
        if (sep == -1)
        {
            describeEnumQuery = describeEnum;
            describeEnumPattern = null;
        }
        else
        {
            describeEnumQuery = describeEnum.substring(0, sep);
            describeEnumPattern = describeEnum.substring(sep + 1);
        }
    }

    /**
     * Check whether to ignore or not this table.
     *
     * @param name table name
     * @return whether to ignore this table
     */
    public boolean ignoreTable(String name)
    {
        return ignoreTablesPattern != null && ignoreTablesPattern.matcher(name).matches();
    }

    /**
     * Driver-specific value filtering
     *
     * @param value value to be filtered
     * @return filtered value
     */
    public Object filterValue(Object value)
    {
        if(value instanceof Calendar && "mysql".equals(tag))
        {
            value = ((Calendar)value).getTime();
        }
        return value;
    }

    /*
     * Members
     */

    /** jdbc tag of the database vendor */
    private String tag = "unknown";

    private String catalog = null;

    /** ping SQL query */
    private String pingQuery = null;

    /** case-sensivity */
    public enum CaseSensitivity { UNKNOWN, SENSITIVE, LOWERCASE, UPPERCASE }
    private CaseSensitivity tablesCaseSensitivity = null;
    private UnaryOperator<String> filterTableName = t -> t;

    /** SQL query to set the current schema */
    private String schemaQuery = null;

    /** ID generation method */
    public enum LastInsertIdPolicy { NONE, GENERATED_KEYS, RETURNING, QUERY, METHOD }
    private LastInsertIdPolicy lastInsertIdPolicy = null;
    private String lastInsertIdQuery = null;
    private String lastInsertIdMethod = null;

    /** whether the JDBC driver is strict about column types */
    private Boolean strictColumnTypes = null;

    /** ignore tables matching this pattern */
    private Pattern ignoreTablesPattern = null;

    /** quoteIdentifier quote character */
    private Character identifierQuoteChar = null;

    /** whether driver supports ::varchar etc... */
    private Boolean columnMarkers = null;

    /** sql query to get enum values */
    private String describeEnumQuery = null;
    private String describeEnumPattern = null;
}
