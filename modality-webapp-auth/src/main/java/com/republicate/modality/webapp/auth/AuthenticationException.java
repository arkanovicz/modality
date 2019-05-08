package com.republicate.modality.webapp.auth;

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
