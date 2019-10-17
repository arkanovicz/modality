package com.republicate.modality.filter;

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

import com.republicate.modality.util.Cryptograph;
import com.republicate.modality.util.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Locale;

public class ValueFilterHandler extends FilterHandler<Serializable>
{
    protected static Logger logger = LoggerFactory.getLogger("modality");

    public ValueFilterHandler(String configurationPrefix)
    {
        super(configurationPrefix);
        addStockObject("lowercase", x -> String.valueOf(x).toLowerCase(Locale.ROOT));
        addStockObject("uppercase", x -> String.valueOf(x).toUpperCase(Locale.ROOT));
        addStockObject("calendar_to_date", x -> x != null && x instanceof Calendar ? TypeUtils.toDate(x) : x);
        addStockObject("date_to_calendar", x -> x != null && x instanceof java.sql.Date ? TypeUtils.toCalendar(x) : x);
        addStockObject("number_to_boolean", x -> x != null && x instanceof Number ? ((Number)x).longValue() != 0 : x);
        addStockObject("raw_obfuscate", x -> x != null ? cryptograph.encrypt(TypeUtils.toString(x)) : null);
        addStockObject("raw_deobfuscate", x -> x != null ? cryptograph.decrypt(TypeUtils.toBytes(x)) : null);
        addStockObject("obfuscate", x -> x != null ? TypeUtils.base64Encode(cryptograph.encrypt(String.valueOf(x))) : null);
        addStockObject("deobfuscate", x -> x != null ? cryptograph.decrypt(TypeUtils.base64Decode(x)) : null);
        addStockObject("deobfuscate_strings", x -> x != null && x instanceof String ? cryptograph.decrypt(TypeUtils.base64Decode(x)) : x);
        addStockObject("base64_encode", x -> TypeUtils.base64Encode(x));
        addStockObject("base64_decode", x -> TypeUtils.base64Decode(x));
        addStockObject("mask", x -> null);
    }

    @Override
    protected Filter<Serializable> getStockObject(String key)
    {
        if (key.contains("obfuscate"))
        {
            needsCryptograph = true;
        }
        return super.getStockObject(key);
    }

    @Override
    protected Logger getLogger()
    {
        return logger;
    }

    public void setCryptograph(Cryptograph cryptograph)
    {
        this.cryptograph = cryptograph;
    }

    public boolean needsCryptograph()
    {
        return needsCryptograph;
    }

    private Cryptograph cryptograph = null;
    private boolean needsCryptograph = false;
}
