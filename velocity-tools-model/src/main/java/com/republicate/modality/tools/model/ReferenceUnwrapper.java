package com.republicate.modality.tools.model;

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

import com.republicate.modality.Entity;
import com.republicate.modality.Instance;
import com.republicate.modality.Model;

/**
 * Static utility to help tools on the java side getting access to model objects from model reference objects
 */

public class ReferenceUnwrapper
{
    /**
     * Private constructor to keep the class static
     */
    private ReferenceUnwrapper()
    {}

    /**
     * Unwrap model
     * @param modelTool given ModelTool
     * @return underlying model
     */
    public static Model unwrap(ModelTool modelTool)
    {
        return modelTool.getModel();
    }

    /**
     * Unwrap entity
     * @param reference given EntityReference
     * @return underlying entity
     */
    public static Entity unwrap(EntityReference reference)
    {
        return reference.getEntity();
    }

    /**
     * Unwrap instance
     * @param reference given InstanceReference
     * @return underlying instance
     */
    public static Instance unwrap(InstanceReference reference)
    {
        return reference.getInstance();
    }
}
