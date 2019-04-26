package tool;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.tools.generic.JsonContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ApiClient;

public class LinkedInTool
{
    private static final String clientID = "...";
    private static final String clientSecret = "...";
    private static final String authURI = "https://www.linkedin.com/oauth/v2/authorization";
    private static final String accessTokenURI = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String userEmailAPI = "https://api.linkedin.com/v1/people/~:(email-address)";
    protected static Logger logger = LoggerFactory.getLogger(LinkedInTool.class);

    public String getClientID() { return clientID; }
    public String getAuthURI() { return authURI; }
    
    public String getAccessToken(String code)
    {
        String accessToken = null;
        try
        {
            JsonContent json = ApiClient.post(accessTokenURI,
                                              "grant_type", "authorization_code",
                                              "code", code,
                                              "redirect_uri", "https://mya.froogz.net/oauth/linkedin.vhtml",
                                              "client_id", clientID,
                                              "client_secret", clientSecret);
            accessToken = (String)json.get("access_token");
        }
        catch (Exception e)
        {
            logger.error("[login] [linkedin] could not get access token", e);
        }
        return accessToken;
    }

    public String getUserEmail(String accessToken)
    {
        String email = null;
        try
        {
            Pair<String, String> authHeader = Pair.of("Authorization", "Bearer " + accessToken);
            JsonContent json = ApiClient.get(userEmailAPI, authHeader);
            email = (String)json.get("email");
        }
        catch (Exception e)
        {
            logger.error("[login] [linkedin] could not get email", e);
        }
        return email;
    }
}
