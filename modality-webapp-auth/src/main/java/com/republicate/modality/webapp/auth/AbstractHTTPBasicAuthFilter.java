package com.republicate.modality.webapp.auth;

import com.republicate.modality.util.TypeUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Filter for HTTP Basic authentication.</p>
 */

public abstract class AbstractHTTPBasicAuthFilter<USER> extends AbstractAuthFilter<USER>
{
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
    }

    @Override
    protected USER authenticate(HttpServletRequest request) throws ServletException
    {
        String authHeader = request.getHeader("Authenticate");
        USER ret = null;
        if (authHeader != null && authHeader.startsWith("Basic "))
        {
            byte[] decrypted = TypeUtils.base64Decode(authHeader.substring(6));
            String clear = new String(decrypted, StandardCharsets.UTF_8);
            int sep = clear.indexOf(':');
            if (sep == -1)
            {
                logger.debug("invalid Basic authotization: {}", clear);
            }
            else
            {
                String login = clear.substring(0, sep);
                String password = clear.substring(sep + 1);
                ret = checkCredentials(login, password);
            }
        }
        else
        {
            logger.debug("invalid Basic authotization: {}", authHeader);
        }
        return ret;
    }

    @Override
    protected void processForbiddenRequest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        logger.debug("unauthorized request towards {}", request.getRequestURI());
        response.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealm() + "\", charset=\"UTF-8\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    protected abstract USER checkCredentials(String login, String password) throws ServletException;
}
