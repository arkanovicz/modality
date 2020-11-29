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

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * <p>Root class for all model objects reference.</p>
 * <p>With Velocity v2.2+, messages can be annoted with the position in the template using the slf4j's MDC mechanism as follow:</p>
 * <ul>
 *     <li>Set `runtime.log.track_location = true` in `velocity.properties`</li>
 *     <li>Set logger the format, for instance with webapp-slf4j-logger :
 *     <pre><code>
 *         &lt;context-param&gt;
 *             &lt;param-name&gt;webapp-slf4j-logger.format&lt;/param-name&gt;
 *             &lt;param-value&gt;%logger [%level] [%ip] %message @%file:%line:%column&lt;/param-value&gt;
 *         &lt;/context-param&gt;
 *     </code></pre>
 *     </li>
 * </ul>
 */

public interface Reference
{
    ModelTool getModelTool();

    default void error(String message, Object... arguments)
    {
        // The default implementation log the error and set the thread-local error, adding the throwable message at end
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, arguments);
        String msg = tuple.getMessage();
        Throwable err = tuple.getThrowable();

        if (err == null)
        {
            getModelTool().getLog().error(msg);
        }
        else
        {
            getModelTool().getLog().error(msg, err);
            StringBuilder appender = new StringBuilder();
            appender.append(msg).append(" - ").append(err.getMessage());
            getModelTool().setLastError(appender.toString());
        }

        // TODO - this is a good insertion point for a pluggable error handler
    }

}
