package com.republicate.motion.model.config;

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

import com.republicate.motion.model.ModelException;

/**
 * Motion Model configuration exceptions thrown to the
 * application layer.
 *
 * @author <a href="mailto:cbrisson@apache.org">Claude Brisson</a>
 * @version $Id:  $
 */

public class ConfigurationException extends ModelException
{
    private static final long serialVersionUID = 3686267961051930733L;

    public ConfigurationException(String exceptionMessage)
    {
        super(exceptionMessage);
    }

    public ConfigurationException(String exceptionMessage, Throwable wrapped)
    {
        super(exceptionMessage, wrapped);
    }

    public ConfigurationException(Throwable wrapped)
    {
        super(wrapped);
    }
}
