package com.holiholic.feed.database;

import com.holiholic.feed.constant.Constants;
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
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
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

    /* isNotValid - Checks if the user id is valid
     *
     */
    public static boolean isNotValid(String uid) {
        return false;
    }

    /* generateMD5 - Generates an md5 key for a plain text
     *
     *  @return             : the md5 key
     *  @plain              : the plain text we want to hash
     */
    public static String generateMD5(String plain) {
        String hash = null;
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(plain.getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            hash = bigInt.toString(16);
            // pad with zeros to have full 32 characters
            if (hash.length() != 32) {
                int diff = 32 - hash.length();
                StringBuilder sb = new StringBuilder();
                for (int d = 0; d < diff; d++) {
                    sb.append("0");
                }
                hash = sb.append(hash).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return hash;
    }

//    /* updateQuestion - Updates the content of a feed item
//     *
//     *  @return             : success or not
//     *  @body               : the request body
//     */
//    public static boolean updateFeed(JSONObject body) {
//        return postContentToURL(body, Constants.UPDATE_FEED_URL);
//    }
//
//    /* getQuestions - Returns a list of questions for a specific city
//     *
//     *  @return             : the list of questions
//     *  @city               : the city where the user wants to see questions
//     *  @uid                : unique identifier for the current user
//     */
//    public static String getQuestions(String city, String uid) {
//        String url = Constants.GET_QUESTIONS_URL
//                     + "?city=" + city
//                     + "&uid=" + uid;
//        try {
//            return getContentFromURL(url);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new JSONArray().toString(2);
//        }
//    }
//
//    /* getQuestionDetails - Returns the details for a specific question
//     *
//     *  @return             : the details for the specific question
//     *  @city               : the city where the user wants to see questions
//     *  @qid                : the question id
//     *  @uidCurrent         : unique identifier for the current user
//     *  @uidAuthor          : unique identifier for the author of the question
//     */
//    public static String getQuestionDetails(String city,
//                                            String qid,
//                                            String uidCurrent,
//                                            String uidAuthor) {
//        String url = Constants.GET_QUESTION_DETAILS_URL
//                     + "?city=" + city
//                     + "&qid=" + qid
//                     + "&uidCurrent=" + uidCurrent
//                     + "&uidAuthor=" + uidAuthor;
//        try {
//            return getContentFromURL(url);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new JSONArray().toString(2);
//        }
//    }
//
//    /* getGuideProfile - Returns the profile for a guide
//     *
//     *  @return             : the profile for a guide
//     *  @city               : the city where the user wants to see the guide
//     *  @uid                : unique identifier for the current user
//     *  @uidGuide           : unique identifier for the guide
//     */
//    public static String getGuideProfile(String city, String uid, String uidGuide) {
//        String url = Constants.GET_GUIDE_PROFILE_URL
//                     + "?city=" + city
//                     + "&uid=" + uid
//                     + "&uidGuide=" + uidGuide;
//        try {
//            return getContentFromURL(url);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new JSONArray().toString(2);
//        }
//    }
//
//    /* getGuideProfilePostDetails - Returns the details for a specific post from guide profile
//     *
//     *  @return             : the details for the specific post from guide profile
//     *  @city               : the city where the user wants to see guide
//     *  @gpid               : the guide post id
//     *  @uidCurrent         : unique identifier for the current user
//     *  @uidAuthor          : unique identifier for the author of the post
//     *  @uidGuide           : unique identifier for the guide
//     */
//    public static String getGuideProfilePostDetails(String city,
//                                                    String gpid,
//                                                    String uidCurrent,
//                                                    String uidAuthor,
//                                                    String uidGuide) {
//        String url = Constants.GET_GUIDE_PROFILE_POST_DETAILS_URL
//                     + "?city=" + city
//                     + "&gpid=" + gpid
//                     + "&uidCurrent=" + uidCurrent
//                     + "&uidAuthor=" + uidAuthor
//                     + "&uidGuide=" + uidGuide;
//        try {
//            return getContentFromURL(url);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new JSONArray().toString(2);
//        }
//    }
//
//    /* getPosts - Returns a list of posts
//     *
//     *  @return             : the list of post
//     *  @uid                : unique identifier for the current user
//     */
//    public static String getPosts(String uid) {
//        String url = Constants.GET_POSTS_URL
//                     + "?uid=" + uid;
//        try {
//            return getContentFromURL(url);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new JSONArray().toString(2);
//        }
//    }
//
//    /* getPostDetails - Returns the details for a specific post
//     *
//     *  @return             : the details for the specific post
//     *  @city               : the city where the post was posted
//     *  @pid                : the post id
//     *  @uidCurrent         : unique identifier for the current user
//     *  @uidAuthor          : unique identifier for the author of the post
//     */
//    public static String getPostDetails(String city,
//                                        String pid,
//                                        String uidCurrent,
//                                        String uidAuthor) {
//        String url = Constants.GET_POST_DETAILS_URL
//                     + "?city=" + city
//                     + "&pid=" + pid
//                     + "&uidCurrent=" + uidCurrent
//                     + "&uidAuthor=" + uidAuthor;
//        try {
//            return getContentFromURL(url);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new JSONArray().toString(2);
//        }
//    }
//    /* getGuides - Returns a list of guides for a specific city
//     *
//     *  @return             : the list of guides
//     *  @city               : the city where the user wants to see guides
//     *  @uid                : unique identifier for the current user
//     */
//    public static String getGuides(String city, String uid) {
//        String url = Constants.GET_GUIDES_URL
//                     + "?city=" + city
//                     + "&uid=" + uid;
//        try {
//            return getContentFromURL(url);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new JSONArray().toString(2);
//        }
//    }
//
//    /* getGuideDetails - Returns the details for a specific guide
//     *
//     *  @return             : the details for the specific guide
//     *  @city               : the city where the user wants to see guides
//     *  @gid                : the guide id
//     *  @uidCurrent         : unique identifier for the current user
//     *  @uidAuthor          : unique identifier for the author of the post
//     */
//    public static String getGuideDetails(String city,
//                                         String gid,
//                                         String uidCurrent,
//                                         String uidAuthor) {
//        String url = Constants.GET_GUIDE_DETAILS_URL
//                     + "?city=" + city
//                     + "&qid=" + gid
//                     + "&uidCurrent=" + uidCurrent
//                     + "&uidAuthor=" + uidAuthor;
//        try {
//            return getContentFromURL(url);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new JSONArray().toString(2);
//        }
//    }
//    /* getContentFromURL - Returns the content from a http get request
//    *
//    *  @return             : the content
//    *  @strUrl             : the url with the get request
//    */
//    private static String getContentFromURL(String strUrl) throws IOException {
//        String data = null;
//        InputStream iStream = null;
//        HttpURLConnection urlConnection = null;
//        String line;
//        try {
//            URL url = new URL(strUrl);
//            // Creating an http connection to communicate with url
//            urlConnection = (HttpURLConnection) url.openConnection();
//            // Connecting to url
//            urlConnection.connect();
//            // Reading data from url
//            iStream = urlConnection.getInputStream();
//
//            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
//            StringBuilder sb = new StringBuilder();
//
//            while ((line = br.readLine()) != null) {
//                sb.append(line);
//            }
//
//            data = sb.toString();
//            br.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            assert iStream != null;
//            iStream.close();
//            urlConnection.disconnect();
//        }
//        return data;
//    }
//
//    /* postContentToURL - Create a post request given the body and the url
//     *
//     *  @return             : success or not
//     *  @body               : the body of the HTTP POST request
//     *  @strUrl             : the url for the HTTP POST request
//     */
//    private static boolean postContentToURL(JSONObject body, String strUrl) {
//        try {
//            StringEntity entity = new StringEntity(body.toString(2),
//                                                   ContentType.APPLICATION_JSON);
//
//            HttpClient httpClient = HttpClientBuilder.create().build();
//            HttpPost request = new HttpPost(strUrl);
//            request.setEntity(entity);
//
//            HttpResponse response = httpClient.execute(request);
//            int responseCode = response.getStatusLine().getStatusCode();
//            return responseCode == HttpStatus.OK.value();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
}
