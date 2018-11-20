package com.holiholic.database;

import com.holiholic.database.constant.Constants;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* DatabaseManager - Handle the requests to the database
 *
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    /* setLogger - This method should be changed when release application
     *             Sets the logger to print to console (instead of a file)
     *
     *  @return             : void
     */
    static void setLogger() {
        // add logger handler
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);

        LOGGER.addHandler(ch);
        LOGGER.setLevel(Level.ALL);
    }

    /* getUsers - Retrieve users from database
     *
     *  @return             : users in the json format
     */
    private static JSONObject getUsers() {
        try {
            InputStream is = new FileInputStream(Constants.USERS_DB_PATH);
            String text = IOUtils.toString(is, "UTF-8");
            return new JSONObject(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* createProfile - Create a new profile for the current user to be stored in the database
     *
     *  @return             : profile (json format)
     *  @request            : user information
     *  @id                 : the id (unique) for the current user
     */
    private static JSONObject createProfile(JSONObject request, int id) {
        JSONObject profile = new JSONObject();
        profile.put("name", request.getString("name"));
        profile.put("email", request.getString("email"));
        profile.put("imageUrl", request.getString("imageUrl"));
        profile.put("id", id);
        return profile;
    }

    /* updateUsers - Save the users in the database
     *
     *  @return             : true/false (success or not)
     *  @users              : all users (json format)
     */
    private static boolean updateUsers(JSONObject users) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(Constants.USERS_DB_PATH), 32768);
            out.write(users.toString(2));
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* registerUser - Save the new user in the database
     *
     *  @return             : true/false (success or not)
     *  @request            : the json object containing information about the new user
     */
    public static boolean registerUser(JSONObject request) {
        try {
            LOGGER.log(Level.FINE, "Started registration for user {0}", request.getString("md5Key"));

            synchronized (DatabaseManager.class) {
                JSONObject users = getUsers();
                if (users == null) {
                    return false;
                }
                users.put(request.getString("md5Key"), createProfile(request, users.length()));
                if (!updateUsers(users)) {
                    return false;
                }
            }

            LOGGER.log(Level.FINE, "Successfully updated registration for user {0}", request.getString("md5Key"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* containsUser - Check if the current user is already in the database
     *
     *  @return             : true/false (exists or not)
     *  @md5Key             : the unique identifier for the current user
     */
    public static boolean containsUser(String md5Key) {
        JSONObject users = getUsers();
        if (users == null) {
            return false;
        }
        return users.has(md5Key);
    }

    /* fetchTopics - Get in memory json for user topics
     *
     *  @return             : the topics json
     */
    private static JSONObject fetchTopics() {
        try {
            InputStream is = new FileInputStream(Constants.TOPICS_DB_PATH);
            String text = IOUtils.toString(is, "UTF-8");
            return new JSONObject(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* fetchAvailableTopics - Get all available topics to follow
     *
     *  @return             : all available topics (json array format)
     */
    private static JSONArray fetchAvailableTopics() {
        try {
            InputStream is = new FileInputStream(Constants.AVAILABLE_TOPICS_DB_PATH);
            String text = IOUtils.toString(is, "UTF-8");
            return new JSONArray(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* getTopics - Get topics for a specific user
     *             In case the user does not have any topics to follow return empty json array
     *
     *  @return             : topics for a specific user (json in string format) or null in case of error
     *  @md5Key             : unique identifier for the current user
     */
    public static String getTopics(String md5Key) {
        // user does not exists in the system so return null and set error to client
        if (!containsUser(md5Key)) {
            return null;
        }
        JSONObject topics = fetchTopics();
        JSONObject response = new JSONObject();
        response.put("availableTopics", fetchAvailableTopics());
        if (topics.has(md5Key)) {
            response.put("userTopics", topics.getJSONArray(md5Key));
        } else {
            response.put("userTopics", new JSONArray());
        }
        return response.toString(2);
    }

    /* saveTopics - Save in the database the updated topics
     *
     *  @return             : success or not
     *  @topics             : the topics to save in database
     */
    private static boolean saveTopics(JSONObject topics) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(Constants.TOPICS_DB_PATH), 32768);
            out.write(topics.toString(2));
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* updateTopics - Save in the database the updated topics for a specific user given the request
     *
     *  @return             : success or not
     *  @request            : json containing information about the current user and the topics he wants to follow
     */
    public static boolean updateTopics(JSONObject request) {
        try {
            LOGGER.log(Level.FINE, "Started updating topics for user {0}", request.getString("md5Key"));

            String md5Key = request.getString("md5Key");
            JSONArray followedTopics = request.getJSONArray("followedTopics");

            if (!containsUser(md5Key)) {
                return false;
            }

            synchronized (DatabaseManager.class) {
                JSONObject topics = fetchTopics();
                topics.put(md5Key, followedTopics);
                if (!saveTopics(topics)) {
                    return false;
                }
            }

            LOGGER.log(Level.FINE, "Successfully updated topics for user {0}", request.getString("md5Key"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
