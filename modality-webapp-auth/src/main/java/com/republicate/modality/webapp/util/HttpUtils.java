package com.republicate.modality.webapp.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

public class HttpUtils
{
    public static String getRealIP(HttpServletRequest request)
    {
        String realIP = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .orElse(request.getRemoteAddr());
        int coma = realIP.indexOf(',');
        if (coma != -1)
        {
            realIP = realIP.substring(0, coma).trim();
        }
        return realIP;
    }

    public static Cookie getCookie(HttpServletRequest request, String cookieName)
    {
        return Arrays.stream(request.getCookies())
            .filter(cookie -> cookie.getName().equals(cookieName))
            .findFirst()
            .orElse(null);
    }

}
