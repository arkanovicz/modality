package com.republicate.modality.webapp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import javax.servlet.http.Cookie;

public class CookieParser
{
    protected static Logger logger = LoggerFactory.getLogger("cookie");

    public static Cookie parseCookie(String setCookie)
    {
        Cookie cookie = null;
        String[] parts = setCookie.split(";");
        for (int i = 0; i < parts.length; ++i)
        {
            String part = parts[i].trim();
            int eq = parts[i].indexOf('=');
            if (eq == -1)
            {
                if (cookie == null)
                {
                    logger.debug("ignoring wrongly formatted Set-Cookie: {}", setCookie);
                    return null;
                }
                else switch (part)
                {
                    case "HttpOnly":
                        cookie.setHttpOnly(true);
                        break;
                    case "Secure":
                        cookie.setSecure(true);
                        break;
                    default:
                        logger.debug("ignoring unknown cookie attribute: {}", part);
                }
            }
            else
            {
                String key = parts[i].substring(0, eq);
                String value = parts[i].substring(eq+1);
                if (i == 0)
                {
                    if (value.startsWith("\"") && value.endsWith("\""))
                    {
                        value = value.substring(1, value.length() - 2);
                    }
                    cookie = new Cookie(key, value);
                }
                else if (cookie == null)
                {
                    logger.debug("ignoring wrongly formatted Set-Cookie: {}", setCookie);
                    return null;
                }
                else switch (key)
                    {
                        case "Expires":
                            Date expires;
                            try
                            {
                                expires = DateUtils.parseDate(value);
                            }
                            catch (DateParseException e)
                            {
                                logger.debug("ignoring wrongly formatted Set-Cookie: {}", setCookie);
                                return null;
                            }
                            int seconds = (int)((expires.getTime() - new Date().getTime()) / 1000);
                            cookie.setMaxAge(seconds);
                            break;
                        case "Max-Age":
                            int maxAge;
                            try
                            {
                                maxAge = Integer.parseInt(value);
                            }
                            catch (NumberFormatException nfe)
                            {
                                logger.debug("ignoring wrongly formatted Set-Cookie: {}", setCookie);
                                return null;
                            }
                            cookie.setMaxAge(maxAge);
                            break;
                        case "Domain":
                            cookie.setDomain(value);
                            break;
                        case "Path":
                            cookie.setPath(value);;
                            break;
                        case "Comment":
                            cookie.setComment(value);
                            break;
                        default:
                            logger.debug("ignoring unknown cookie attribute: {}={}", key, value);
                    }
            }
        }
        return cookie;
    }

}
