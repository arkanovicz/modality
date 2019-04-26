package com.republicate.modality.model;

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

/**
 *  Base class for Modality Model runtime exceptions thrown to the
 * application layer.
 *
 * @author <a href="mailto:cbrisson@apache.org">Claude Brisson</a>
 * @version $Id:  $
 */
public class ModelException extends RuntimeException
{
    private static final long serialVersionUID = 4219808691463960079L;

    /**
     * @param exceptionMessage The message to register.
     */
    public ModelException(final String exceptionMessage)
    {
        super(exceptionMessage);
    }

    /**
     * @param exceptionMessage The message to register.
     * @param wrapped A throwable object that caused the Exception.
     */
    public ModelException(final String exceptionMessage, final Throwable wrapped)
    {
        super(exceptionMessage, wrapped);
    }

    /**
     * @param wrapped A throwable object that caused the Exception.
     */
    public ModelException(final Throwable wrapped)
    {
        super(wrapped);
    }

    /**
     *  returns the wrapped Throwable that caused this
     *  MethodInvocationException to be thrown
     *
     *  @return Throwable thrown by method invocation
     *  @deprecated Use {@link java.lang.RuntimeException#getCause()}
     */
    public Throwable getWrappedThrowable()
    {
        return getCause();
    }

}
