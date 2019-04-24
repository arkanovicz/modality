package tool;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.tools.generic.JsonContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ApiClient;

public class GithubTool
{
    private static final String clientId = "5d1749427d18bac267fe";
    private static final String clientSecret = "2c066317cd563d4d5be3c3cf17dbe54e09a330b5";
    private static final String githubAuthorize = "https://github.com/login/oauth/authorize";
    private static final String githubAccessCode = "https://github.com/login/oauth/access_token";
    private static final String githubPrivateEmails = "https://api.github.com/user/emails";
    protected static Logger logger = LoggerFactory.getLogger(GithubTool.class);

    public String getClientID()
    {
        return clientId;
    }

    public String getAuthorizeURL()
    {
        return "https://github.com/login/oauth/authorize";
    }
    
    public String getAccessToken(String code)
    {
        String accessToken = null;
        try
        {
            JsonContent json = ApiClient.post(githubAccessCode, "client_id", clientId, "client_secret", clientSecret, "code", code);
            accessToken = (String)json.get("access_token");
        }
        catch (Exception e)
        {
            logger.error("[login] [github] could not get access token", e);
        }
        return accessToken;
    }

    public JsonContent getPrivateEmails(String accessToken)
    {
        JsonContent json = null;
        try
        {
            json = ApiClient.get(githubPrivateEmails, "access_token", accessToken);
            
        }
        catch (Exception e)
        {
            logger.error("[login] [github] could not get private emails", e);
        }
        return json;
    }
}
