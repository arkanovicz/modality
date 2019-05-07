package com.republicate.modality.webapp.auth;

import com.republicate.modality.util.TypeUtils;

import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;

public abstract class AbstractBasicAuthFilter<USER> extends AbstractHeaderAuthFilter<USER>
{
    @Override
    USER authorize(String headerValue) throws ServletException
    {
        USER ret = null;
        byte[] decrypted = TypeUtils.base64Decode(headerValue);
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
        return ret;
    }

    protected abstract USER checkCredentials(String login, String password) throws ServletException;
}
