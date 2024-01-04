package com.twitch.bot.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobResult;
import com.twitch.bot.aws_technologies.AWS_Credentials;
import com.twitch.bot.aws_technologies.AWS_S3;
import com.twitch.bot.aws_technologies.AWS_Sns;
import com.twitch.bot.aws_technologies.AWS_Transcribe;
import com.twitch.bot.db_utils.TwitchData;
import com.twitch.bot.model.Channel;
import com.twitch.bot.model.Subscriptions;
import com.twitch.bot.model.User;
import com.twitch.bot.sns_model.SnsData;
import com.twitch.bot.twitch_connection.ChannelsData;
import com.twitch.bot.twitch_connection.Connection;
import com.twitch.bot.twitch_connection.Users;

import software.amazon.awssdk.regions.Region;

@CrossOrigin
// ("http://localhost:5173/")
@RestController
@RequestMapping("/")
public class Controller {
    private static final Logger LOG = Logger.getLogger(Controller.class.getName());
    private Connection twitch_connection;
    private Users users;
    TwitchData twitchData;
    HttpHeaders responseHeaders = new HttpHeaders();

    public Controller(ChannelsData channelsData, Connection twitch_connection, Users users, TwitchData twitchData)
            throws Exception {
        this.twitch_connection = twitch_connection;
        this.users = users;
        this.twitchData = twitchData;
        // responseHeaders.add("Access-Control-Allow-Origin", "ORIGIN");
        // responseHeaders.add("Access-Control-Allow-Methods", "PUT, GET, HEAD, POST,
        // DELETE, OPTIONS");
    }

    @GetMapping("/channels")
    public ResponseEntity<Object> getTwitchChannels(@RequestHeader Object userId) throws Exception {
        Boolean isValidUser = users.authenticateUser(Integer.parseInt(userId.toString()));
        if (!isValidUser) {
            return new ResponseEntity<>(responseHeaders, HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(
                twitch_connection.getAllChannels(users.getUserDetails(Integer.parseInt(userId.toString()))),
                responseHeaders, HttpStatus.OK);
    }

    @GetMapping("/twitch_analysis")
    public ResponseEntity<Object> getTwitchAnalysisData(@RequestParam("channel_name") String channelName)
            throws Exception {
        HashMap<String, Object> response = new HashMap<>();
        response.put("twitch_analysis", twitch_connection.getTwitchAnalysisOfAChannelInListOfHashmap(channelName));
        response.put("channel_name", channelName);
        return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
    }

    @GetMapping("/channel_broadcastId")
    public ResponseEntity<Object> getChannelBroadcastId(@RequestParam("channel_name") String channelName)
            throws Exception {
        return new ResponseEntity<>(twitch_connection.getUserBroadcasterId(channelName), responseHeaders,
                HttpStatus.OK);
    }

    @PostMapping("/addChannel")
    public ResponseEntity<Object> subscribeChannel(@RequestParam("channel_name") String channelName) throws Exception {
        twitch_connection.addAndJoinChannel(channelName);
        return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
    }

    @GetMapping("/dummy")
    public ResponseEntity<Object> dummyApi() throws Exception {
        dummy();
        return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
    }

    private void dummy() throws Exception {
        JSONObject credentials = twitchData.getCloudCredentials();
        AWS_Credentials.getRegionInRegionsForm();
        AWS_Credentials.getRegionInRegionForm();
        AWS_Credentials awsCredentials = new AWS_Credentials(credentials.get("access_key").toString(),
                credentials.get("access_id").toString());
        AWS_Sns sns = new AWS_Sns(awsCredentials);
        SnsData data = new SnsData();
        List<Integer> ids = new ArrayList<>();
        ids.add(1);
        data.setUserId(ids);
        data.setChannelId(2);
        data.setChannelName("mail");
        sns.publishSNSMessage(data);
    }

    @DeleteMapping("/removeChannel")
    public ResponseEntity<Object> unSubscribeChannel(@RequestParam("channel_name") String channelName)
            throws Exception {
        twitch_connection.removeAndDeleteChannelData(channelName);
        return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
    }

    @PostMapping("user/authenticate")
    public ResponseEntity<HashMap<String, Object>> authenticateUser(@RequestBody HashMap<String, String> credentials) {
        LOG.log(Level.INFO, "POST /user/authenticate {0}", new Object[] { credentials });
        HashMap<String, Object> response = new HashMap<>();
        try {
            if (!(credentials.containsKey("username") || credentials.containsKey("email"))
                    || !credentials.containsKey("password")) {
                throw new IllegalArgumentException();
            }
            String userName = credentials.get("username");
            String email = credentials.get("email");
            String password = credentials.get("password");

            Boolean isValidUser = (userName != null) ? users.authenticateUser(userName, password, true)
                    : users.authenticateUser(email, password, false);
            if (isValidUser) {
                User user = (userName != null) ? users.getUserDetails(userName, password, true)
                        : users.getUserDetails(email, password, false);
                response.put("user_name", user.getName());
                response.put("email", user.getEmail());
                response.put("user_id", user.getUserId());
                return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
            }

        } catch (IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, "INVALID_BODY");
            return new ResponseEntity<>(responseHeaders, HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getLocalizedMessage());
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("user/register")
    public ResponseEntity<HashMap<String, Object>> register(@RequestBody HashMap<String, String> credentials) {
        LOG.log(Level.INFO, "POST /user/register {0}", new Object[] { credentials });
        HashMap<String, Object> response = new HashMap<>();
        try {
            if (!credentials.containsKey("username") || !credentials.containsKey("email")
                    || !credentials.containsKey("password")) {
                throw new IllegalArgumentException();
            }
            String userName = credentials.get("username");
            String email = credentials.get("email");
            String password = credentials.get("password");

            User user = users.registerUser(userName, password, email);
            response.put("user_name", user.getName());
            response.put("email", user.getEmail());
            response.put("user_id", user.getUserId());
            return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, "INVALID_BODY");
            return new ResponseEntity<>(responseHeaders, HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getLocalizedMessage());
            if (ex.getMessage() != null && ex.getMessage().equals("User Already Present")) {
                return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_ACCEPTABLE);
            } else {
                return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    @GetMapping("user/subscriptions")
    public ResponseEntity<Object> getUserSubscriptions(@RequestHeader Object userId) {
        try {
            Boolean isValidUser = users.authenticateUser(Integer.parseInt(userId.toString()));
            if (isValidUser) {
                User user = users.getUserDetails(Integer.parseInt(userId.toString()));
                return new ResponseEntity<>(users.getUserSubscribedChannels(user), responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getLocalizedMessage());
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("user/subscriptions")
    public ResponseEntity<Object> addUserSubscriptions(@RequestHeader Object userId,
            @RequestParam("channel_id") String channelId) {
        try {
            Subscriptions subscription = users.checkAndAddUserSubscriptions(Integer.parseInt(userId.toString()),
                    Integer.parseInt(channelId));
            if (subscription != null) {
                return new ResponseEntity<>(subscription, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(responseHeaders, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getLocalizedMessage());
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("user/subscriptions")
    public ResponseEntity<Object> deleteUserSubscriptions(@RequestHeader Object userId,
            @RequestParam("channel_id") String channelId) {
        try {
            Boolean isDeleteDone = users.checkAndDeleteUserSubscriptions(Integer.parseInt(userId.toString()),
                    Integer.parseInt(channelId));
            if (isDeleteDone) {
                return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(responseHeaders, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getLocalizedMessage());
            return new ResponseEntity<>(responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
