package com.holiholic.follow.database;

import com.holiholic.follow.constant.Constants;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* DatabaseManager - Handle the requests to the database and (pre)process the queries
 *
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    /* setLogger - This method should be changed when release application
     *             Sets the logger to print to console (instead of a file)
     *
     *  @return             : void
     */
    public static void setLogger() {
        // add logger handler
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);

        LOGGER.addHandler(ch);
        LOGGER.setLevel(Level.ALL);
    }

    /* getPeople - Get a list with people to follow for the current user
     *
     *  @return                 : a json string format with the people the user should follow
     *  @md5Key                 : the md5Key for the current user
     */
    public static String getPeople(String md5Key) {
        try {
            return getContentFromURL(Constants.GET_PEOPLE_URL + md5Key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* updatePeople - Save new followed people for the current user in the database using HTTP POST Request
     *
     *  @return                 : true/false (success or not)
     *  @md5KeyFrom             : the md5Key for the current user
     *  @md5KeyTo               : the md5Key for the followed user
     *  @operation              : follow/unfollow
     */
    public static boolean updatePeople(String md5KeyFrom, String md5KeyTo, String operation) {
        try {
            JSONObject followGraphEdge = new JSONObject();
            followGraphEdge.put("md5KeyFrom", md5KeyFrom);
            followGraphEdge.put("md5KeyTo", md5KeyTo);
            followGraphEdge.put("operation", operation);

            StringEntity entity = new StringEntity(followGraphEdge.toString(2),
                                                   ContentType.APPLICATION_JSON);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(Constants.UPDATE_PEOPLE_URL);
            request.setEntity(entity);

            HttpResponse response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            return responseCode == HttpStatus.OK.value();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* getPeople - Get a list with topics to follow for the current user
     *
     *  @return                 : a json string format with the topics the user should follow
     *  @md5Key                 : the md5Key for the current user
     */
    public static String getTopics(String md5Key) {
        try {
            return getContentFromURL(Constants.GET_TOPICS_URL + md5Key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* updateTopics - Save new followed topics for the current user in the database using HTTP POST Request
     *
     *  @return                 : true/false (success or not)
     *  @md5Key                 : the md5Key for the current user
     *  @followedTopics         : a list with topics that user wants to follow
     */
    public static boolean updateTopics(String md5Key, JSONArray followedTopics) {
        try {
            JSONObject topicEntry = new JSONObject();
            topicEntry.put("md5Key", md5Key);
            topicEntry.put("followedTopics", followedTopics);

            StringEntity entity = new StringEntity(topicEntry.toString(2),
                                                   ContentType.APPLICATION_JSON);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(Constants.UPDATE_TOPICS_URL);
            request.setEntity(entity);

            HttpResponse response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            return responseCode == HttpStatus.OK.value();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* getContentFromURL - Returns the content from a http get request
     *
     *  @return             : the content
     *  @strUrl             : the url with the get request
     */
    private static String getContentFromURL(String strUrl) throws IOException {
        String data = null;
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        String line;
        try {
            URL url = new URL(strUrl);
            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url
            urlConnection.connect();
            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            assert iStream != null;
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}
