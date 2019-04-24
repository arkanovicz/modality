package tool;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.tools.generic.JsonContent;
import org.apache.velocity.tools.generic.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ApiClient;

public class GoogleTool
{
    private static final String clientID = "421767446751-n74g702jmqvinqtd6j61cn6jehs4n89d.apps.googleusercontent.com";
    private static final String clientSecret = "TT-9njC-1vG6iRGfRmJ83M6V";
    private static final String projectID = "froogz-a5d62";
    private static final String authURI = "https://accounts.google.com/o/oauth2/auth";
    private static final String tokenURI = "https://www.googleapis.com/oauth2/v4/token";

    protected static Logger logger = LoggerFactory.getLogger(GoogleTool.class);

    public String getClientID() { return clientID; }
    public String getAuthURI() { return authURI; }


    private static final String urlencode(String str) throws UnsupportedEncodingException
    {
        return URLEncoder.encode(str, "UTF-8");
    }

    public String getUserEmail(String code)
    {
        String userEmail = null;
        try
        {
            JsonContent json = ApiClient.post(tokenURI, "client_id", clientID, "redirect_uri", "https://mya.froogz.net/oauth/google.vhtml", "client_secret", clientSecret, "code", code, "grant_type", "authorization_code");
            String idToken = (String)json.get("id_token");
            String parts[] = idToken.split("\\.");
            String payload = StringUtils.newStringUtf8(Base64.decodeBase64(parts[1]));
            JsonContent infos = (JsonContent)new JsonTool().parse(payload).root();
            userEmail = (String)infos.get("email");
        }
        catch (Exception e)
        {
            logger.error("[login] [google] could not get user email", e);
        }
        return userEmail;
    }
}
