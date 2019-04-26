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

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class SlotTreeMap extends TreeMap<String,Serializable> implements SlotMap
{
    public SlotTreeMap() {}

    public SlotTreeMap(java.util.Comparator<java.lang.String> comparator)
    {
        super(comparator);
    }

    public SlotTreeMap(Map<? extends String, ? extends Serializable> m)
    {
        super(m);
    }
}
