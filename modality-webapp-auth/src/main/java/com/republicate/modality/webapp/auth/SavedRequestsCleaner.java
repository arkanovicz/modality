package com.republicate.modality.webapp.auth;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import static com.republicate.modality.webapp.auth.BaseFormAuthFilter.SAVED_REQUEST_SESSION_KEY;

@WebListener
public class SavedRequestsCleaner implements HttpSessionListener
{
    /**
     * Receives notification that a session has been created.
     *
     * @param se the HttpSessionEvent containing the session
     */
    @Override
    public void sessionCreated(HttpSessionEvent se)
    {
        BaseAuthFilter.logger.debug("session activated: {}", se.getSession().getId());
    }

    /**
     * Receives notification that a session is about to be invalidated.
     *
     * @param se the HttpSessionEvent containing the session
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent se)
    {
        BaseAuthFilter.logger.debug("session passivated: {}", se.getSession().getId());
        se.getSession().removeAttribute(SAVED_REQUEST_SESSION_KEY);
    }
}
