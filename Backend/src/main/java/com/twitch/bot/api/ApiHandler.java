package com.twitch.bot.api;

import org.springframework.stereotype.Component;
import com.twitch.bot.db_utils.TwitchData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.context.annotation.DependsOn;

@Component
@DependsOn({"twitchData"})
public class ApiHandler {
    private static final Logger LOG = Logger.getLogger(ApiHandler.class.getName());
    TwitchData twitchData;

    public enum APIKEYTYPE {
        CHATGPTAPIKEY
    }

    public enum PATH {
        CONNECT("irc.twitch.tv", 6667),
        OAUTH_TOKEN("oauth2/token", 0),
        OAUTH_VALIDATE("oauth2/validate", 0),
        GET_USERS("helix/users", 0),
        GET_CHANNEL("helix/channels", 0),
        CLIPS("helix/clips", 0),
        GET_STREAMS("helix/streams", 0);

        private String path;
        private Integer ip;

        private PATH(String path, Integer ip) {
            this.path = path;
            this.ip = ip;
        }

    }

    public ApiHandler(TwitchData twitchData) throws Exception {
        this.twitchData = twitchData;
        JSONObject credentials = twitchData.getTwitchCredentials();
        this.clientId = credentials.getString("client_id");
        this.clientSecret = credentials.getString("client_secret");
        this.authorization_domain = HTTPS + "id.twitch.tv";
        this.domain = HTTPS + "api.twitch.tv";
        this.connectionTimeOut = 5000;
        this.socketTimeOut = 5000;
        this.userName = credentials.getString("user_name");
        this.accessToken = credentials.getString("access_token");
        this.refreshToken = credentials.getString("refresh_token");
    }

    private JSONObject params;
    private JSONObject headers;
    private JSONObject body;
    private String domain;
    private String authorization_domain;
    private String path;
    private Integer connectionTimeOut;
    private Integer socketTimeOut;
    private String accessToken;
    private String refreshToken;
    private String clientId;
    private String clientSecret;
    private boolean isConnectionRunning = false;
    private BufferedWriter twitch_writer;
    private BufferedReader twitch_reader;
    private String userName;

    private static final String HTTPS = "https://";
    private static final String SLASH = "/";

    public JSONObject getParams() {
        return params;
    }

    public ApiHandler setParams(JSONObject params) {
        this.params = params;
        return this;
    }

    public JSONObject getHeaders() {
        return headers;
    }

    public ApiHandler setHeaders(JSONObject headers) {
        this.headers = headers;
        return this;
    }

    public JSONObject getBody() {
        return body;
    }

    public ApiHandler setBody(JSONObject body) {
        this.body = body;
        return this;
    }

    public String getPath() {
        return path;
    }

    public ApiHandler setPath(PATH pathValue) {
        this.path = pathValue.path;
        return this;
    }

    public ApiHandler setConnectionTimeOut(Integer connectionTimeOut) {
        this.connectionTimeOut = connectionTimeOut;
        return this;
    }

    public ApiHandler setSocketTimeOut(Integer socketTimeOut) {
        this.socketTimeOut = socketTimeOut;
        return this;
    }

    public BufferedWriter getTwitch_writer() {
        return twitch_writer;
    }

    public BufferedReader getTwitch_reader() {
        return twitch_reader;
    }

    private void sendAccessTokenAndRefreshTokenToMongoDB(String accessToken, String refreshToken) {
        twitchData.setTwitchCredentials(new JSONObject().put("access_token", accessToken).put("refresh_token", refreshToken).put("client_id", clientId));
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    private String validateAndUpdateAccessToken() throws Exception {
        String result = "";
        int status;
        HttpGet httpGet = new HttpGet(authorization_domain + SLASH + PATH.OAUTH_VALIDATE.path);

        // Headers Part
        httpGet.setHeader("Authorization", "Bearer " + accessToken);

        // Timeout Part
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeOut)
                .setSocketTimeout(300000).build();
        httpGet.setConfig(requestConfig);

        // httpGet.setURI(uriBuilder.build());

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(httpGet)) {

            status = response.getStatusLine().getStatusCode();
            result = EntityUtils.toString(response.getEntity());
        }

        if (status == 401) {
            String oauth_data = generateAccessToken();
            JSONObject oauth_json = new JSONObject(oauth_data);
            sendAccessTokenAndRefreshTokenToMongoDB(oauth_json.getString("access_token"),
                    oauth_json.getString("refresh_token"));
            result = new JSONObject().put("status", "OAUTH_UPDATED_SUCCESSFUL").toString();
        } else if (status == 200) {
            result = new JSONObject().put("status", "OAUTH_FETCHED_SUCCESSFUL").toString();
        } else {
            LOG.log(Level.SEVERE, "Issue in Oauth ::: status ::: " + status + " ::: response ::: " + result);
            throw new Exception("Issue in Oauth Generation");
        }
        return result;
    }

    private String generateAccessToken() throws Exception {
        String result = "";
        int status;
        HttpPost httpPost = new HttpPost(authorization_domain + SLASH + PATH.OAUTH_TOKEN.path);

        // Headers Part
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

        URIBuilder uriBuilder = new URIBuilder(httpPost.getURI());
        httpPost.setURI(uriBuilder.build());

        // Body Part
        JSONObject oauth_body = new JSONObject();
        oauth_body.put("client_id", clientId);
        oauth_body.put("client_secret", clientSecret);
        oauth_body.put("grant_type", "refresh_token");
        oauth_body.put("refresh_token", refreshToken);
        httpPost.setEntity(new StringEntity(jsonToHttpSupportedString(oauth_body)));

        // Timeout Part
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeOut)
                .setSocketTimeout(300000).build();
        httpPost.setConfig(requestConfig);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(httpPost)) {

            status = response.getStatusLine().getStatusCode();
            result = EntityUtils.toString(response.getEntity());
        }

        if (status != 200) {
            LOG.log(Level.SEVERE, "OAUTH TOKEN REFRESHING FAILED");
            throw new Exception("Oauth Generation Failed");
        }
        return result;
    }

    public String jsonToHttpSupportedString(JSONObject json) {
        String result = "";
        Iterator<String> jsonIter = json.keys();
        while (jsonIter.hasNext()) {
            String key = jsonIter.next();
            result += key + "=" + json.get(key).toString();
            if (jsonIter.hasNext()) {
                result += "&";
            }
        }
        return result;
    }

    public boolean CONNECT() throws Exception {
        if (!isConnectionEstablishedAndRunning()) {
            validateAndUpdateAccessToken();
            try {
                @SuppressWarnings("resource")
                Socket socketConnection = new Socket(PATH.CONNECT.path, PATH.CONNECT.ip);
                this.twitch_writer = new BufferedWriter(new OutputStreamWriter(socketConnection.getOutputStream()));
                this.twitch_reader = new BufferedReader(new InputStreamReader(socketConnection.getInputStream()));

                this.twitch_writer.write("PASS " + "oauth:" + accessToken + "\r\n");
                this.twitch_writer.write("NICK " + userName + "\r\n");
                this.twitch_writer.write("CAP REQ :twitch.tv/commands \r\n");
                this.twitch_writer.write("CAP REQ :twitch.tv/membership \r\n");
                this.twitch_writer.flush();

                String currentLine = "";
                while ((currentLine = this.twitch_reader.readLine()) != null) {
                    if (currentLine.indexOf("004") >= 0) {
                        LOG.log(Level.INFO, "Connected >> " + userName + " ~ irc.twitch.tv");
                        break;
                    } else {
                        LOG.log(Level.INFO, currentLine);
                    }
                }
                isConnectionRunning = true;
                return isConnectionEstablishedAndRunning();
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Exception is " + ex);
            }
        }
        return isConnectionEstablishedAndRunning();
    }

    public boolean isConnectionEstablishedAndRunning() {
        return isConnectionRunning;
    }

    public String POST() throws Exception {
        validateAndUpdateAccessToken();
        checkRequestQuality();
        String result = "";
        HttpPost httpPost = new HttpPost(domain + SLASH + path);

        // Headers Part
        if (!headers.isEmpty()) {
            for (String key : headers.keySet()) {
                httpPost.setHeader(key, headers.getString(key));
            }
        }

        // parameters part
        URIBuilder uriBuilder = new URIBuilder(httpPost.getURI());
        for (String key : params.keySet()) {
            uriBuilder.addParameter(key, params.getString(key));
        }

        httpPost.setURI(uriBuilder.build());

        // Body Part
        httpPost.setEntity(new StringEntity(body.toString()));

        // Timeout Part
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeOut)
                .setSocketTimeout(socketTimeOut).build();
        httpPost.setConfig(requestConfig);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(httpPost)) {

            result = EntityUtils.toString(response.getEntity());
        }

        clear();
        return result;
    }

    public String GET() throws Exception {
        validateAndUpdateAccessToken();
        checkRequestQuality();
        String result = "";
        HttpGet httpGet = new HttpGet(domain + SLASH + path);

        // Headers Part
        if (!headers.isEmpty()) {
            for (String key : headers.keySet()) {
                httpGet.setHeader(key, headers.getString(key));
            }
        }

        // parameters part
        URIBuilder uriBuilder = new URIBuilder(httpGet.getURI());
        for (String key : params.keySet()) {
            uriBuilder.addParameter(key, params.getString(key));
        }

        // Timeout Part
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeOut)
                .setSocketTimeout(socketTimeOut).build();
        httpGet.setConfig(requestConfig);

        httpGet.setURI(uriBuilder.build());

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(httpGet)) {

            result = EntityUtils.toString(response.getEntity());
        }

        clear();
        return result;
    }

    public String PUT() throws Exception {
        validateAndUpdateAccessToken();
        checkRequestQuality();
        String result = "";
        HttpPut httpPut = new HttpPut(domain + SLASH + path);

        // Headers Part
        if (!headers.isEmpty()) {
            for (String key : headers.keySet()) {
                httpPut.setHeader(key, headers.getString(key));
            }
        }

        // parameters part
        URIBuilder uriBuilder = new URIBuilder(httpPut.getURI());
        for (String key : params.keySet()) {
            uriBuilder.addParameter(key, params.getString(key));
        }

        // Body Part
        httpPut.setEntity(new StringEntity(body.toString()));

        // Timeout Part
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeOut)
                .setSocketTimeout(socketTimeOut).build();
        httpPut.setConfig(requestConfig);

        httpPut.setURI(uriBuilder.build());

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(httpPut)) {

            result = EntityUtils.toString(response.getEntity());
        }

        clear();
        return result;
    }

    public String DELETE() throws Exception {
        validateAndUpdateAccessToken();
        checkRequestQuality();
        String result = "";
        HttpDelete httpDelete = new HttpDelete(domain + SLASH + path);

        // Headers Part
        if (!headers.isEmpty()) {
            for (String key : headers.keySet()) {
                httpDelete.setHeader(key, headers.getString(key));
            }
        }

        // parameters part
        URIBuilder uriBuilder = new URIBuilder(httpDelete.getURI());
        for (String key : params.keySet()) {
            uriBuilder.addParameter(key, params.getString(key));
        }

        // Timeout Part
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectionTimeOut)
                .setSocketTimeout(socketTimeOut).build();
        httpDelete.setConfig(requestConfig);

        httpDelete.setURI(uriBuilder.build());

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(httpDelete)) {

            result = EntityUtils.toString(response.getEntity());
        }

        clear();
        return result;
    }

    private void checkRequestQuality() throws Exception {
        if (params == null) {
            params = new JSONObject();
        }
        if (headers == null) {
            headers = new JSONObject();
        }
        if (body == null) {
            body = new JSONObject();
        }
        if (path == null) {
            path = "";
        }

        if (!(headers.has("Content-Type") && path.equals(PATH.CONNECT.path))) {
            headers.put("Content-Type", "application/json");
        }
        if (!(headers.has("Authorization") && path.equals(PATH.CONNECT.path))) {
            headers.put("Authorization", "Bearer " + accessToken);
        }
        if(headers.has("set_client_id")){
            headers.put(headers.getString("set_client_id"), this.clientId);
            headers.remove("set_client_id");
        }
    }

    private void clear() {
        params = null;
        headers = null;
        body = null;
        path = null;
        socketTimeOut = 5000;
        connectionTimeOut = 5000;
    }
}