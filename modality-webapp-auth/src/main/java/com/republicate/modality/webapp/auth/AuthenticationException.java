package com.republicate.modality.webapp.auth;

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

public class AuthenticationException extends Exception
{
    /**
     * Default constructor.
     */
    public AuthenticationException()
    {
    }

    /**
     * Constructor with message
     *
     * @param message error message
     *
     * @see java.lang.Throwable#getMessage
     */
    public AuthenticationException(String message)
    {
        super(message);
    }

    /**
     * Constructor with message and cause
     *
     * @param message error message
     * @param cause cause
     *
     * @see java.lang.Throwable#getMessage
     */
    public AuthenticationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    private static final long serialVersionUID = 7781661985930778427L;
}
