package tool;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.tools.generic.JsonContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ApiClient;

public class TwitterTool
{
    private static final String apiUrl = "https://api.twitter.com";
    private static final String callbackUrl = "https://mya.froogz.net/oauth/twitter.vhtml";
    private static final String consumerKey = "rJyJj0vqkV2UKEPVdxm5XH52k";
    private static final String consumerSecret = "WULOtDnsPvpHIlvUWiAGrWa2CMRn77TLNrPU32GwHIVKEXNkbd";
    private static Encoder base64Encoder = Base64.getUrlEncoder();
    private static final ChallengeTool nonceGenerator = new ChallengeTool();
    protected static Logger logger = LoggerFactory.getLogger(TwitterTool.class);

    private static final String urlencode(String str) throws UnsupportedEncodingException
    {
        return URLEncoder.encode(str, "UTF-8");
    }

    public JsonContent requestToken()
    {
        JsonContent json = null;
        try
        {
            String path = "/oauth/request_token";
            String url = apiUrl + path;
        
            SortedMap<String, String> oauthParams = new TreeMap<String, String>();
            oauthParams.put("oauth_callback", urlencode(callbackUrl));
            oauthParams.put("oauth_consumer_key", urlencode(consumerKey));
            oauthParams.put("oauth_nonce", urlencode(nonceGenerator.toString()));
            oauthParams.put("oauth_signature_method", "HMAC-SHA1");
            oauthParams.put("oauth_timestamp", String.valueOf(new Date().getTime()/1000));
            oauthParams.put("oauth_version", "1.0");

            StringBuilder oauthParamsBaseSign = new StringBuilder();
            for (Map.Entry<String, String> param : oauthParams.entrySet())
            {
                if (oauthParamsBaseSign.length() > 0) oauthParamsBaseSign.append('&');
                oauthParamsBaseSign.append(param.getKey()).append('=').append(param.getValue());
            }

            HmacUtils hmacSha1Signer = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, consumerSecret + "&");
            StringBuilder toBeSigned = new StringBuilder();
            toBeSigned.append("POST").append('&');
            toBeSigned.append(urlencode(apiUrl + path)).append('&');
            toBeSigned.append(urlencode(oauthParamsBaseSign.toString()));
            String signature = base64Encoder.encodeToString(hmacSha1Signer.hmac(toBeSigned.toString()));
            oauthParams.put("oauth_signature", urlencode(signature));

            StringBuilder authHeaderValue = new StringBuilder();
            for (Map.Entry<String, String> param : oauthParams.entrySet())
            {
                if (authHeaderValue.length() == 0) authHeaderValue.append("OAuth ");
                else authHeaderValue.append(", ");
                authHeaderValue.append(param.getKey()).append("=\"").append(param.getValue()).append("\"");
            }

            Pair<String, String> authHeader = Pair.of("Authorization", authHeaderValue.toString());
            json = ApiClient.post(url, authHeader);
            boolean valid = Boolean.valueOf((String)json.get("oauth_callback_confirmed"));
            if (!valid) throw new Exception("oauth_callback_confirmed is false");
        }
        catch (Exception e)
        {
            logger.error("[login] [twitter] could not get twitter login url", e);
        }
        return json;
    }

    public JsonContent enableAccess(String oauthToken, String oauthSecret, String oauthVerifier)
    {
        JsonContent json = null;
        try
        {
            String path = "/oauth/access_token";
            String url = apiUrl + path;
        
            SortedMap<String, String> oauthParams = new TreeMap<String, String>();
            oauthParams.put("oauth_callback", urlencode(callbackUrl));
            oauthParams.put("oauth_consumer_key", urlencode(consumerKey));
            oauthParams.put("oauth_nonce", urlencode(nonceGenerator.toString()));
            oauthParams.put("oauth_signature_method", "HMAC-SHA1");
            oauthParams.put("oauth_token", urlencode(oauthToken));
            oauthParams.put("oauth_timestamp", String.valueOf(new Date().getTime()/1000));
            oauthParams.put("oauth_verifier", urlencode(oauthVerifier));
            oauthParams.put("oauth_version", "1.0");

            StringBuilder oauthParamsBaseSign = new StringBuilder();
            for (Map.Entry<String, String> param : oauthParams.entrySet())
            {
                if (oauthParamsBaseSign.length() > 0) oauthParamsBaseSign.append('&');
                oauthParamsBaseSign.append(param.getKey()).append('=').append(param.getValue());
            }

            HmacUtils hmacSha1Signer = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, consumerSecret + "&" + oauthSecret);
            StringBuilder toBeSigned = new StringBuilder();
            toBeSigned.append("POST").append('&');
            toBeSigned.append(urlencode(apiUrl + path)).append('&');
            toBeSigned.append(urlencode(oauthParamsBaseSign.toString()));
            String signature = base64Encoder.encodeToString(hmacSha1Signer.hmac(toBeSigned.toString()));
            oauthParams.put("oauth_signature", urlencode(signature));

            StringBuilder authHeaderValue = new StringBuilder();
            for (Map.Entry<String, String> param : oauthParams.entrySet())
            {
                if (authHeaderValue.length() == 0) authHeaderValue.append("OAuth ");
                else authHeaderValue.append(", ");
                authHeaderValue.append(param.getKey()).append("=\"").append(param.getValue()).append("\"");
            }

            Pair<String, String> authHeader = Pair.of("Authorization", authHeaderValue.toString());
            json = ApiClient.post(url, authHeader);
        }
        catch (Exception e)
        {
            logger.error("[login] [twitter] could not get twitter access token", e);
        }
        return json;
    }

    public JsonContent getCredentials(String oauthToken, String oauthSecret)
    {
        JsonContent json = null;
        try
        {
            String path = "/1.1/account/verify_credentials.json";
            String url = apiUrl + path;
            
            SortedMap<String, String> oauthParams = new TreeMap<String, String>();
            oauthParams.put("oauth_callback", urlencode(callbackUrl));
            oauthParams.put("oauth_consumer_key", urlencode(consumerKey));
            oauthParams.put("oauth_nonce", urlencode(nonceGenerator.toString()));
            oauthParams.put("oauth_signature_method", "HMAC-SHA1");
            oauthParams.put("oauth_token", urlencode(oauthToken));
            oauthParams.put("oauth_timestamp", String.valueOf(new Date().getTime()/1000));
            oauthParams.put("oauth_version", "1.0");

            SortedMap<String, String> params = new TreeMap<String, String>();
            params.put("include_entities", "false");
            params.put("skip_status", "true");
            params.put("include_email", "true");

            SortedMap<String, String> signParams = new TreeMap<String, String>();
            signParams.putAll(oauthParams);
            signParams.putAll(params);

            StringBuilder oauthParamsBaseSign = new StringBuilder();
            for (Map.Entry<String, String> param : signParams.entrySet())
            {
                if (oauthParamsBaseSign.length() > 0) oauthParamsBaseSign.append('&');
                oauthParamsBaseSign.append(param.getKey()).append('=').append(param.getValue());
            }

            HmacUtils hmacSha1Signer = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, consumerSecret + "&" + oauthSecret);
            StringBuilder toBeSigned = new StringBuilder();
            toBeSigned.append("GET").append('&');
            toBeSigned.append(urlencode(apiUrl + path)).append('&');
            toBeSigned.append(urlencode(oauthParamsBaseSign.toString()));
            String signature = base64Encoder.encodeToString(hmacSha1Signer.hmac(toBeSigned.toString()));
            oauthParams.put("oauth_signature", urlencode(signature));

            StringBuilder authHeaderValue = new StringBuilder();
            for (Map.Entry<String, String> param : oauthParams.entrySet())
            {
                if (authHeaderValue.length() == 0) authHeaderValue.append("OAuth ");
                else authHeaderValue.append(", ");
                authHeaderValue.append(param.getKey()).append("=\"").append(param.getValue()).append("\"");
            }

            Pair<String, String> authHeader = Pair.of("Authorization", authHeaderValue.toString());
            json = ApiClient.get(url, authHeader, "include_entities", "false", "skip_status", "true", "include_email", "true"); // CB TODO - avoid params redundancy (and factorize code!!!)
        }
        catch (Exception e)
        {
            logger.error("[login] [twitter] could not get twitter access token", e);
        }
        return json;
    }

}
