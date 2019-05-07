package com.republicate.modality.webapp.auth;

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

import com.republicate.modality.Action;
import com.republicate.modality.Entity;
import com.republicate.modality.Instance;
import com.republicate.modality.Model;
import com.republicate.modality.RowAttribute;
import com.republicate.modality.util.AESCryptograph;
import com.republicate.modality.util.Cryptograph;
import com.republicate.modality.util.SlotHashMap;
import com.republicate.modality.util.SlotMap;
import com.republicate.modality.util.TypeUtils;
import com.republicate.modality.webapp.util.HttpUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RememberMeCookieHandlerImpl implements RememberMeCookieHandler
{
    protected static Logger logger = LoggerFactory.getLogger("remember-me");

    public RememberMeCookieHandlerImpl(String name, String domain, String path, int maxAge, boolean secure, boolean checkUserAgent, boolean checkIP)
    {
        this.cookieName = name;
        this.cookieDomain = domain;
        this.cookiePath = path;
        this.cookieMaxAge = maxAge;
        this.cookieSecure = secure;
        this.checkUserAgent = checkUserAgent;
        this.checkIP = checkIP;
    }

    @Override
    public RememberMeCookieHandler setModel(Model model) throws ServletException
    {
        doCreateCookie = model.getAction("create_remember_me");
        if (doCreateCookie == null)
        {
            throw new ServletException("required action 'create_remember_me' does not exist or is not an action");
        }
        doCheckCookie = model.getRowAttribute("check_remember_me");
        if (doCheckCookie == null)
        {
            throw new ServletException("required row attribute 'check_remember_me' does not exist or is not a row attribute");
        }
        Entity users = doCheckCookie.getResultEntity();
        if (users == null || !users.hasPrimaryKey())
        {
            throw new ServletException("'check_remember_me' attribute doesn't have any resulting instance, or it doesn't have any primary key");
        }
        usersPrimaryKey = users.getPrimaryKey().stream().map(col -> col.name).collect(Collectors.toList());
        doRefreshCookie = model.getAction("refresh_remember_me"); // may be null
        if (doRefreshCookie == null)
        {
            logger.warn("action 'refresh_remember_me' does not exist: resorting to reset + create");
        }
        doResetCookie = model.getAction("reset_remember_me"); // may be null
        if (doResetCookie == null)
        {
            logger.warn("no reset_remember_me action provided");
        }
        doCleanCookies = model.getAction("clean_remember_me"); // may be null
        cryptograph = new AESCryptograph();
        String seed = getCryptographSeed(model);
        cryptograph.init(seed);
        return this;
    }

    private String getCryptographSeed(Model model)
    {
        return Optional.ofNullable(
            model.getDefinition()).map(x -> String.valueOf(x)).
            orElse(Optional.ofNullable(model.getDatabaseURL()).filter(x -> x.length() >= 16).
                orElse("sixteen chars..."));

    }

    @Override
    public Instance getRememberMe(HttpServletRequest request) throws ServletException
    {
        Instance user = null;
        String value = getCookieValue(request);
        if (value != null)
        {
            String ip = HttpUtils.getRealIP(request);
            String ua = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
            user = getRememberMe(value, ip, ua);
        }
        return user;
    }

    private Instance getRememberMe(String rememberMeValue, String requestIp, String requestUA) throws ServletException
    {
        String decrypted;
        try
        {
            byte[] bytes = TypeUtils.base64Decode(rememberMeValue);
            decrypted = cryptograph.decrypt(bytes);
        }
        catch (Exception e)
        {
            logger.debug("remember_me cookie decryption failed", e);
            return null;
        }
        // the expected format is: userKey#ip#ua#secureKey
        String[] parts = decrypted.split("#");
        if (parts.length != 3 + usersPrimaryKey.size())
        {
            logger.debug("wrong remember_me cookie value format");
            return null;
        }

        String userKey = parts[0];
        String ip = parts[1];
        String ua = parts[2];
        String secureKey = parts[3];

        if (checkIP && !ip.equals(requestIp)
            || checkUserAgent && !ua.equals(requestUA))
        {
            logger.debug("mismatch in remember_me cookie IP or UA");
            return null;
        }

        Serializable[] pk = userKey.split("\\|");
        if (pk.length != usersPrimaryKey.size())
        {
            logger.debug("wrong number of user key values");
            return null;
        }
        SlotMap params = buildParams(pk, ip, ua, secureKey);
        try
        {
            return doCheckCookie.retrieve(params);
        }
        catch (SQLException sqle)
        {
            throw new ServletException("could not check remember_me cookie", sqle);
        }
    }

    @Override
    public void setRememberMe(Instance user, HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        String rememberMe = request.getParameter(cookieName);
        if (rememberMe != null)
        {
            String ip = HttpUtils.getRealIP(request);
            String ua = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
            String secureKey = generateSecureKey();

            Serializable[] pk = user.getPrimaryKey();
            if (pk == null)
            {
                throw new ServletException("could not get user primary key for user" + user);
            }
            Cookie cookie = createCookie(pk, ip, ua, secureKey);
            response.addCookie(cookie);

            SlotMap params = buildParams(pk, ip, ua, secureKey);
            try
            {
                doCreateCookie.perform(params);
            } catch (SQLException sqle)
            {
                throw new ServletException("could create remember_me cookie", sqle);
            }
        }
    }

    @Override
    public void resetRememberMe(Instance user, HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        if (doResetCookie == null)
        {
            RememberMeCookieHandler.super.resetRememberMe(user, request, response);
        }
        else
        {
            manageCookie(user, request, response, doResetCookie, 0);
        }
    }

    @Override
    public void refreshRememberMe(Instance user, HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        if (doRefreshCookie == null)
        {
            RememberMeCookieHandler.super.refreshRememberMe(user, request, response);
        }
        else
        {
            manageCookie(user, request, response, doRefreshCookie, cookieMaxAge);
        }
    }

    private void manageCookie(Instance user, HttpServletRequest request, HttpServletResponse response, Action action, int maxAge) throws ServletException
    {
        Serializable[] pk = user.getPrimaryKey();
        if (pk == null)
        {
            throw new ServletException("could not get user primary key for user" + user);
        }
        String ip = HttpUtils.getRealIP(request);
        String ua = Optional.ofNullable(request.getHeader("User-Agent")).orElse("");
        String secureKey = generateSecureKey();
        Cookie cookie = createCookie(pk, ip, ua, secureKey);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);

        SlotMap params = buildParams(pk, ip, ua, secureKey);
        try
        {
            action.perform(params);
        }
        catch (SQLException sqle)
        {
            throw new ServletException("could not call action " + action.getName(), sqle);
        }

    }

    private String getCookieValue(HttpServletRequest request)
    {
        return Optional.ofNullable(HttpUtils.getCookie(request, cookieName))
            .map(cookie -> cookie.getValue())
            .orElse(null);
    }

    private SlotMap buildParams(Serializable[] pk, String ip, String ua, String secureKey)
    {
        SlotMap params = new SlotHashMap();
        IntStream.range(0, pk.length).forEach(i -> params.put(usersPrimaryKey.get(i), pk[i]));
        params.put("ip", ip);
        params.put("secure_key", secureKey);
        return params;
    }

    private Cookie createCookie(Serializable[] pk, String ip, String ua, String secureKey)
    {
        String userKey = Arrays.stream(pk).map(String::valueOf).collect(Collectors.joining("|"));
        StringBuilder cleanCookie = new StringBuilder();
        cleanCookie.append(userKey)
            .append('#').append(ip)
            .append('#').append(ua)
            .append('#').append(secureKey);
        String encrypted = TypeUtils.base64Encode(cryptograph.encrypt(cleanCookie.toString()));
        Cookie cookie = new Cookie(cookieName, encrypted);
        if (cookieDomain != null)
        {
            cookie.setDomain(cookieDomain);
        }
        cookie.setPath(cookiePath);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(cookieMaxAge);
        cookie.setSecure(cookieSecure);
        return cookie;
    }

    static private final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final String generateSecureKey()
    {
        return RandomStringUtils.random(15, characters);
    }

    // config
    private String cookieName;
    private String cookieDomain;
    private String cookiePath;
    private int cookieMaxAge;
    private boolean cookieSecure;
    private boolean checkUserAgent;
    private boolean checkIP;
    private Cryptograph cryptograph;

    // init
    private RowAttribute doCheckCookie = null;
    private List<String> usersPrimaryKey = null;
    private Action doCreateCookie = null;
    private Action doResetCookie = null;
    private Action doRefreshCookie = null;
    private Action doCleanCookies = null;

}
