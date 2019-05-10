package com.republicate.modality.webapp.util;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    // commons-io IOUtils.toByteArray() method *manually* shaded here
    public static byte[] toByteArray(final InputStream input) throws IOException
    {
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            byte[] buffer = new byte[8196];
            int n;
            while (-1 != (n = input.read(buffer)))
            {
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        }
    }
}
