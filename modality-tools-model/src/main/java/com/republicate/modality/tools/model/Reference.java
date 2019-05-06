package com.republicate.modality.tools.model;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * <p>Root class for all model objects reference.</p>
 * <p>With Velocity v2.2+, messages can be annoted with the position in the template using the slf4j's MDC mechanism as follow:</p>
 * <ul>
 *     <li>Set `runtime.log.track_location = true` in `velocity.properties`</li>
 *     <li>Set logger the format, for instance with webapp-slf4j-logger :
 *     <code><pre>
 *         &lt;context-param&gt;
 *             &lt;param-name&gt;webapp-slf4j-logger.format&lt;/param-name&gt;
 *             &lt;param-value&gt;%logger [%level] [%ip] %message @%file:%line:%column&lt;/param-value&gt;
 *         &lt;/context-param&gt;
 *     </pre></code>
 *     </li>
 * </ul>
 */

public abstract class Reference
{
    protected abstract ModelTool getModelTool();

    protected void error(String message, Object... arguments)
    {
        // The default implementation just log the error.
        getModelTool().getLogger().error(message, arguments);

        FormattingTuple tuple = MessageFormatter.arrayFormat(message, arguments);
        String msg = tuple.getMessage();
        Throwable err = tuple.getThrowable();
        getModelTool().getLogger().error(msg, err);
        getModelTool().setLastError(msg);

        // TODO - this is a good insertion point for a pluggable error handler
    }

}
