package tool;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.velocity.tools.generic.JsonContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ApiClient;

public class FacebookTool
{
    private static final String clientID = "...";
    private static final String clientSecret = "...";
    private static final String loginURL = "http://www.facebook.com/dialog/oauth";
    private static final String accessTokenURL = "https://graph.facebook.com/oauth/access_token";
    private static final String graphURL = "https://graph.facebook.com/me";

    protected static Logger logger = LoggerFactory.getLogger(FacebookTool.class);

    public String getClientID()
    {
        return clientID;
    }

    public String getLoginURL()
    {
        return loginURL;
    }


    private static final String urlencode(String str) throws UnsupportedEncodingException
    {
        return URLEncoder.encode(str, "UTF-8");
    }
    
    public String getAccessToken(String code)
    {
        String accessToken = null;
        try
        {
            JsonContent json = ApiClient.get(accessTokenURL, "client_id", clientID, "redirect_uri", urlencode("https://mya.froogz.net/oauth/facebook.vhtml"), "client_secret", clientSecret, "code", code);
            accessToken = (String)json.get("access_token");
        }
        catch (Exception e)
        {
            logger.error("[login] [facebook] could not get access token", e);
        }
        return accessToken;
    }

    public String getUserEmail(String accessToken)
    {
        String email = null;
        try
        {
            JsonContent json = ApiClient.get(graphURL, "fields", "email", "access_token", urlencode(accessToken));
            email = (String)json.get("email");
        }
        catch (Exception e)
        {
            logger.error("[login] [facebook] could not get email", e);
        }
        return email;
    }
}
