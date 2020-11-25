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


import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * various SQL-related helpers.
 *
 *  @author <a href=mailto:claude.brisson@gmail.com>Claude Brisson</a>
 *
 */
public class SqlUtils
{
    protected static Logger logger = LoggerFactory.getLogger("sql");

    // that's crazy to have to code such a method...
    // in Ruby for instance, it's :
    // '*' * n
    private static String stars(int length)
    {
        StringBuilder ret = new StringBuilder(length);
        for(int i = 0; i < length; i++)
        {
            ret.append('*');
        }
        return ret.toString();
    }

    /**
     * get the column nams of a result set
     * @param resultSet result set
     * @return list of columns
     * @throws SQLException
     */
    public static List<String> getColumnNames(ResultSet resultSet) throws SQLException
    {
        List<String> columnNames = new ArrayList<String>();
        ResultSetMetaData meta = resultSet.getMetaData();
        int count = meta.getColumnCount();

        for(int c = 1; c <= count; c++)
        {
            // see http://jira.springframework.org/browse/SPR-3541
            // columnNames.add(meta.getColumnName(c));
            columnNames.add(meta.getColumnLabel(c));
        }
        return columnNames;
    }

    /**
     * java.sql.Types int to class (TODO - move it to a utility class)
     */
    static private Map<Integer, Class> sqlTypeToClass;

    /* CB TODO - a real mapping requires taking precision and scale into account! */
    static
    {
        sqlTypeToClass = new HashMap<Integer, Class>();
        sqlTypeToClass.put(Types.BIGINT, BigInteger.class);
        sqlTypeToClass.put(Types.BIT, Boolean.class);
        sqlTypeToClass.put(Types.BOOLEAN, Boolean.class);
        sqlTypeToClass.put(Types.CHAR, String.class);
        sqlTypeToClass.put(Types.DATE, java.sql.Date.class);
        sqlTypeToClass.put(Types.DECIMAL, Double.class);
        sqlTypeToClass.put(Types.DOUBLE, Double.class);
        sqlTypeToClass.put(Types.FLOAT, Float.class);
        sqlTypeToClass.put(Types.INTEGER, Integer.class);
        sqlTypeToClass.put(Types.LONGNVARCHAR, String.class);
        sqlTypeToClass.put(Types.LONGVARCHAR, String.class);
        sqlTypeToClass.put(Types.NCHAR, String.class);
        sqlTypeToClass.put(Types.NUMERIC, Double.class);
        sqlTypeToClass.put(Types.NVARCHAR, String.class);
        sqlTypeToClass.put(Types.REAL, Float.class);
        sqlTypeToClass.put(Types.ROWID, Long.class);
        sqlTypeToClass.put(Types.SMALLINT, Short.class);
        sqlTypeToClass.put(Types.TIME, java.sql.Time.class);
        sqlTypeToClass.put(Types.TIMESTAMP, java.sql.Timestamp.class);
        sqlTypeToClass.put(Types.TINYINT, Byte.class);
        sqlTypeToClass.put(Types.VARCHAR, String.class);
    }

    public static Class getSqlTypeClass(int type)
    {
        return sqlTypeToClass.get(type);
    }

    /**
     * @param query
     * @param identifierQuoteChar
     * @return
     * @since Modalidy  1.1
     */
    public static List<String> splitStatements(String query, Character identifierQuoteChar)
    {
        return splitStatements(query, identifierQuoteChar, false);
    }

    public enum SplitState
    {
        NORMAL,
        COMMENT,
        IDENTIFIER,
        LITERAL,
        PARENTHESE,
        DOLLAR
    }

    public static List<String> splitStatements(String query, Character identifierQuoteChar, boolean considerDollar)
    {
        List<String> ret = new ArrayList<>();
        Stack<SplitState> state = new Stack<>();
        state.push(SplitState.NORMAL);
        int dollars = 0;
        boolean afterHyphen = false;
        int parLevel = 0;
        StringBuilder currentQuery = new StringBuilder();
        for (int i = 0; i < query.length(); ++i)
        {
            Character c = query.charAt(i);
            currentQuery.append(c); // CB TODO - filter comments?
            switch (state.peek())
            {
                case NORMAL: // normal
                {
                    switch (c)
                    {
                        case '-':
                        {
                            if (afterHyphen)
                            {
                                afterHyphen = false;
                                state.push(SplitState.COMMENT);
                            }
                            else
                            {
                                afterHyphen = true;
                            }
                            break;
                        }
                        case '\'':
                        {
                            state.push(SplitState.LITERAL);
                            break;
                        }
                        case ';':
                        {
                            if (parLevel == 0)
                            {
                                String nextQuery = currentQuery.toString().trim();
                                if (nextQuery.length() > 0)
                                {
                                    ret.add(nextQuery);
                                    currentQuery = new StringBuilder();
                                }
                            }
                            break;
                        }
                        case '(':
                        {
                            ++parLevel;
                            break;
                        }
                        case ')':
                        {
                            --parLevel;
                            break;
                        }
                        case '$':
                            if (considerDollar)
                            {
                                dollars = 1;
                                state.push(SplitState.DOLLAR);
                            }
                            break;
                        default:
                        {
                            if (c == identifierQuoteChar)
                            {
                                state.push(SplitState.IDENTIFIER);
                                break;
                            }
                        }
                    }
                    break;
                }
                case COMMENT:
                {
                    if (c == '\n')
                    {
                        state.pop();
                    }
                    break;
                }
                case IDENTIFIER:
                {
                    if (c == identifierQuoteChar)
                    {
                        state.pop();
                    }
                    break;
                }
                case LITERAL:
                {
                    if (c == '\'')
                    {
                        state.pop();
                    }
                    break;
                }
                case DOLLAR: // (no support for nested $$ blocks - CB TODO)
                    if (c == '$')
                    {
                        if (++dollars == 4)
                        {
                            dollars = 0;
                            state.pop();
                        }
                    }
            }
        }
        String nextQuery = currentQuery.toString().trim();
        if (nextQuery.length() > 0)
        {
            ret.add(nextQuery);
        }
        return ret;
    }

    public static boolean hasMultipleStatements(String query, Character identifierQuoteChar)
    {
        return splitStatements(query, identifierQuoteChar).size() > 1;
    }
}
