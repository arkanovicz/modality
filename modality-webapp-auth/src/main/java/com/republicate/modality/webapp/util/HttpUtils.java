package com.republicate.modality.webapp.util;

import java.util.Arrays;
import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

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
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
        {
            return null;
        }
        else
        {
            return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(cookieName))
                .findFirst()
                .orElse(null);
        }
    }

}
