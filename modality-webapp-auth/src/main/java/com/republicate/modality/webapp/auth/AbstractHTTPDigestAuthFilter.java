package com.republicate.modality.webapp.auth;

import com.republicate.modality.util.SlotHashMap;
import com.republicate.modality.util.SlotMap;
import com.republicate.modality.webapp.auth.helpers.NonceStore;
import com.republicate.modality.webapp.util.Digester;
import com.republicate.modality.webapp.util.HttpUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * <p>Filter for HTTP Digest authentication.</p>
 * <p>Configuration parameters:</p>
 * <ul>
 *     <li><code>auth.digest.domain</code> - authentication domain, '/' by default.</li>
 *     <li><code>auth.digest.algorithm</code> - digest algorithm, can be 'MD5' or 'MD5-sess'. If left unspecified,
 *     the filter will return 'MD5-sess' by default in the WWW-Authenticate header, but will confirm to the algorithm
 *     asked by the client, if any.</li>
 * </ul>
 * @param <USER>
 */

public abstract class AbstractHTTPDigestAuthFilter<USER> extends AbstractAuthFilter<USER>
{
    // config parameters keys

    public static final String DOMAIN = "auth.digest.domain";
    public static final String ALGORITHM = "auth.digest.algorithm";

    // default values

    private static final String DEFAULT_DOMAIN = "/";
    private static final String DEFAULT_ALGORITHM = "MD5-sess";

    /** corect response request attribute */
    private static final String CORRECT_RESPONSE = "com.republicate.webapp.auth.digest.correct";

    private final String[] qopValues = { "auth", "auth-int" };

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        super.init(filterConfig);
        domain = Optional.ofNullable(findConfigParameter(DOMAIN)).orElse("/");
        algorithm = findConfigParameter(ALGORITHM);
    }

    protected USER authenticate(HttpServletRequest request) throws ServletException
    {
        String authHeader = request.getHeader("Authenticate");
        USER ret = null;
        if (authHeader != null && authHeader.startsWith("Basic "))
        {
            try
            {
                SlotMap authParams = getAuthenticationParams(authHeader);
                String response = (String)authParams.get("response");
                String nonce = (String)authParams.get("nonce");
                Integer nc = (Integer)authParams.get("nc");
                String uri = (String)authParams.get("uri");

                // if URI match
                if (request.getRequestURI().equals(uri))
                {
                    // and if resonse is correct
                    Pair<String, USER> correct = calcExpectedResponse(authParams, request);
                    if (correct.getLeft().equals(response))
                    {
                        // mark it
                        request.setAttribute(CORRECT_RESPONSE, true);
                        if (nonceStore.checkNonce(nonce, nc))
                        {
                            // then check nonce
                            ret = correct.getRight();
                        }
                    }
                }
            }
            catch (AuthenticationException ae)
            {
                logger.debug("Digest authentication failed", ae);
            }
        }
        return ret;
    }

    private SlotMap getAuthenticationParams(String headerValue) throws AuthenticationException
    {
        if (!headerValue.startsWith("Digest "))
        {
            throw new AuthenticationException("invalid Digest authentication: " + headerValue);
        }
        SlotMap ret = new SlotHashMap();
        boolean hasQOP = false;
        int nc = 0;
        String[] parts = headerValue.substring(7).split(", *");
        for (String part : parts)
        {
            int eq = part.indexOf('=');
            if (eq == -1)
            {
                throw new AuthenticationException("invalid Digest authentication: no value for: " + part);
            }
            String key = part.substring(0, eq);
            String value = part.substring(eq + 1);
            if (value.startsWith("\"") && value.endsWith("\""))
            {
                value = value.substring(1, value.length() - 2);
            }
            switch (key)
            {
                case "realm":
                    if (!getRealm().equals(value))
                    {
                        throw new AuthenticationException("invalid Digest authentication: invalid realm: " + value);
                    }
                    ret.put(key, value);
                    break;
                case "qop":
                    if (!Arrays.asList(qopValues).contains(value))
                    {
                        throw new AuthenticationException("invalid Digest authentication: invalid qop: " + value);
                    }
                    ret.put(key, value);
                    hasQOP = true;
                    break;
                case "algorithm":
                    if (algorithm != null && !algorithm.equals(value))
                    {
                        throw new AuthenticationException("invalid Digest authentication: (invalid algorithm: " + value);
                    }
                    break;
                case "domain": // not checked for now - TODO
                case "URI":
                case "nonce":
                case "cnonce":
                case "username":
                case "response":
                    ret.put(key, value);
                    break;
                case "nc":
                    try
                    {
                        nc = Integer.parseInt(value, 16);
                    }
                    catch (NumberFormatException nfe)
                    {
                        throw new AuthenticationException("invalid Digest authentication: invalid nc: " + part);
                    }
                    ret.put(key, nc);
                    break;
                default:
                    logger.debug("ignoring Digest authentication param: {}", part);
            }
        }
        // check required params
        String[] requiredParams = hasQOP ?
            new String[] { "username", "response", "nonce", "cnonce", "nc" }
            : new String[] { "username", "response", "nonce" };
        for (String param : requiredParams)
        {
            if (!ret.containsKey(param))
            {
                throw new AuthenticationException("invalid Digest authentication: missing authaurization param: " + param);
            }
        }
        return ret;
    }

    private Pair<String, USER> calcExpectedResponse(SlotMap authParams, HttpServletRequest request) throws AuthenticationException
    {
        String username = (String) authParams.get("username");
        String md5 = getUserRealmPasswordMD5(username);
        String qop = (String)authParams.get("qop");
        if (md5 == null)
        {
            throw new AuthenticationException("no user found");
        }
        String HA1 = calcHA1(authParams, md5);
        String HA2 = calcHA2(authParams, request);
        String nonce = (String)authParams.get("nonce");
        String response;
        if (qop == null)
        {
            String concat = HA1 + ':' + nonce + ':' + HA2;
            response = Digester.toHexMD5String(concat);
        }
        else
        {
            String cnonce = (String)authParams.get("cnonce");
            int nc = (Integer)authParams.get("nc");
            String concat = HA1 + ':' + nonce + ':' + nc + ':' + cnonce + ':' + qop + ':' + HA2;
            response = Digester.toHexMD5String(concat);
        }
        USER user = getUserInstance(username);
        return Pair.of(response, user);
    }

    protected abstract String getUserRealmPasswordMD5(String login);

    protected abstract USER getUserInstance(String login);

    private String calcHA1(SlotMap authParams, String md5) throws AuthenticationException
    {
        String algorithm = Optional.ofNullable((String)authParams.get("algorithm")).orElse(DEFAULT_ALGORITHM);
        String ha1;
        if (algorithm == null || algorithm.equals("MD5"))
        {
            ha1 = md5;
        }
        else
        {
            String nonce = (String)authParams.get("nonce");
            String cnonce = (String)authParams.get("cnonce");
            String a1 = md5 + ':' + nonce + ':' + cnonce;
            ha1 = Digester.toHexMD5String(a1);
        }
        return ha1;
    }

    private String calcHA2(SlotMap authParams, HttpServletRequest request) throws AuthenticationException
    {
        String qop = (String)authParams.get("qop");
        String ha2;
        if (qop == null || "auth".equals(qop))
        {
            String a2 = request.getMethod() + ':' + request.getRequestURI();
            ha2 = Digester.toHexMD5String(a2);
        }
        else if ("auth-int".equals(qop))
        {
            byte[] body;
            try
            {
                body = HttpUtils.toByteArray(request.getInputStream());
            }
            catch (IOException ioe)
            {
                throw new AuthenticationException("could not read body", ioe);
            }
            String a2 = request.getMethod() + ':' + request.getRequestURI() + ':' + Digester.toHexMD5String(body);
            ha2 = Digester.toHexMD5String(a2);
        }
        else
        {
            throw new AuthenticationException("invalid qop:" + qop);
        }
        return ha2;
    }

    @Override
    protected void processForbiddenRequest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        logger.debug("unauthorized request towards {}", request.getRequestURI());
        StringBuilder wwwAuthenticate = new StringBuilder();
        wwwAuthenticate.append("Digest ").append("realm=\"").append(getRealm()).append("\"");
        wwwAuthenticate.append(", qop=\"auth,auth-int\""); // TODO - parametrize
        if (request.getAttribute(CORRECT_RESPONSE) != null)
        {
            wwwAuthenticate.append(", stale=true");
        }
        String nonce = nonceStore.newNonce();
        wwwAuthenticate.append(", nonce=\"").append(nonce).append("\"");
        wwwAuthenticate.append(", domain=\"").append(domain).append("\"");
        wwwAuthenticate.append(", uri=\"").append(request.getRequestURI()).append("\"");
        wwwAuthenticate.append(", algorithm=").append(DEFAULT_ALGORITHM);
        response.addHeader("WWW-Authenticate", wwwAuthenticate.toString());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private String domain = null;
    private String algorithm = null;
    private NonceStore nonceStore = null;
}

