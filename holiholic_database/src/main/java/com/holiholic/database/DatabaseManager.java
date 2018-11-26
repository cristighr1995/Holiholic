package com.holiholic.database;

import com.holiholic.database.constant.Constants;
import com.holiholic.database.feed.Feed;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
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

    /* getUsers - Retrieve users from database
     *
     *  @return             : users in the json format
     */
    private static JSONObject getUsers() {
        return fetchObjectFromDatabase(Constants.USERS_DB_PATH);
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
        return syncDatabase(Constants.USERS_DB_PATH, users);
    }

    /* registerUser - Save the new user in the database
     *
     *  @return             : true/false (success or not)
     *  @request            : the json object containing information about the new user
     */
    public static boolean registerUser(JSONObject request) {
        try {
            LOGGER.log(Level.FINE, "New request to register user {0}", request.getString("md5Key"));
            synchronized (DatabaseManager.class) {
                JSONObject users = getUsers();
                if (users == null) {
                    return false;
                }
                users.put(request.getString("md5Key"), createProfile(request, users.length()));
                return updateUsers(users);
            }
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

    /* updateFeed - Updates a specific question or post from the feed
     *
     *  @return             : success or not
     *  @body               : the request body (json format)
     */
    public static boolean updateFeed(JSONObject body) {
        try {
            String operation = body.getString("operation");
            String city = body.getString("city");
            String type = body.getString("type");

            LOGGER.log(Level.FINE, "New request to {0} {1} in {2} city",
                       new Object[]{operation, type, city});

            Feed feed = Feed.Factory.getInstance(city, type, body);
            if (feed == null) {
                return false;
            }

            switch (operation) {
                case "add":
                    return feed.add(body);
                case "remove":
                    return feed.remove(body);
                case "edit":
                    return feed.edit(body);
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* getQuestions - Get a list of questions for a specific city
     *                Each question has only the last comment
     *
     *  @return             : a list of questions (json string format)
     *  @city               : the requested city
     *  @md5Key             : unique identifier for the current user
     */
    public static String getQuestions(String city, String md5Key) {
        return Feed.getQuestions(md5Key, Constants.QUESTIONS_DB_PATH, city, "question", LOGGER).toString(2);
    }

    /* getPosts - Get a list of posts
     *            Each post has only the last comment
     *
     *  @return             : a list of posts (json string format)
     *  @md5Key             : current user id
     */
    public static String getPosts(String md5Key) {
        return Feed.getPosts(md5Key, Constants.POSTS_DB_PATH, "post", LOGGER).toString(2);
    }

    /* fetchCities - Get a list of available cities for the application
     *
     *  @return             : a list of cities
     */
    public static JSONArray fetchCities() {
        return fetchArrayFromDatabase(Constants.CITIES_DB_PATH);
    }

    /* getQuestionDetails - Get details for a specific question
     *
     *  @return                 : the details for a question (json string format)
     *  @city                   : the requested city
     *  @qid                    : unique identifier for the question
     *  @md5KeyCurrent          : unique identifier for the current user
     *  @md5KeyQuestionAuthor   : the id for the user who wrote the question
     */
    public static String getQuestionDetails(String city,
                                            String qid,
                                            String md5KeyCurrent,
                                            String md5KeyAuthor) {
        return Feed.getDetails(city, qid, "question", Constants.QUESTIONS_DB_PATH, md5KeyCurrent, md5KeyAuthor, LOGGER);
    }

    /* getPostDetails - Get details for a specific post
     *
     *  @return                 : the details for a post (json string format)
     *  @city                   : the requested city
     *  @pid                    : unique identifier for the post
     *  @md5KeyCurrent          : unique identifier for the current user
     *  @md5KeyQuestionAuthor   : the id for the user who wrote the post
     */
    public static String getPostDetails(String city,
                                        String pid,
                                        String md5KeyCurrent,
                                        String md5KeyAuthor) {
        return Feed.getDetails(city, pid, "post", Constants.POSTS_DB_PATH, md5KeyCurrent, md5KeyAuthor, LOGGER);
    }

    /* fetchTopics - Get in memory json for user topics
     *
     *  @return             : the topics json
     */
    private static JSONObject fetchTopics() {
        return fetchObjectFromDatabase(Constants.TOPICS_DB_PATH);
    }

    /* fetchAvailableTopics - Get all available topics to follow
     *
     *  @return             : all available topics (json array format)
     */
    private static JSONArray fetchAvailableTopics() {
        return fetchArrayFromDatabase(Constants.AVAILABLE_TOPICS_DB_PATH);
    }

    /* getTopics - Get topics for a specific user
     *             In case the user does not have any topics to follow return empty json array
     *
     *  @return             : topics for a specific user (json in string format)
     *  @md5Key             : unique identifier for the current user
     */
    public static String getTopics(String md5Key) {
        LOGGER.log(Level.FINE, "New request from user {0} to get topics", md5Key);
        JSONObject topics = fetchTopics();
        JSONObject response = new JSONObject();
        response.put("availableTopics", fetchAvailableTopics());
        assert topics != null;
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
        return syncDatabase(Constants.TOPICS_DB_PATH, topics);
    }

    /* updateTopics - Save in the database the updated topics for a specific user given the request
     *
     *  @return             : success or not
     *  @request            : json containing information about the current user and the topics he wants to follow
     */
    public static boolean updateTopics(JSONObject request) {
        try {
            String md5Key = request.getString("md5Key");
            LOGGER.log(Level.FINE, "New request from user {0} to update his topics", md5Key);
            JSONArray followedTopics = request.getJSONArray("followedTopics");

            if (!containsUser(md5Key)) {
                return false;
            }

            synchronized (DatabaseManager.class) {
                JSONObject topics = fetchTopics();
                assert topics != null;
                topics.put(md5Key, followedTopics);
                return saveTopics(topics);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* fetchGraph - Get in memory json follow graph
     *
     *  @return             : the follow graph (json format)
     */
    private static JSONObject fetchGraph() {
        return fetchObjectFromDatabase(Constants.PEOPLE_DB_PATH);
    }

    /* getTopics - Get following people for a specific user
     *             In case the user does not have any following people return empty json array
     *
     *  @return             : following people for a specific user (json in string format)
     *  @md5Key             : unique identifier for the current user
     */
    public static String getPeople(String md5Key) {
        LOGGER.log(Level.FINE, "New request from user {0} to get people", md5Key);
        JSONObject followGraph = fetchGraph();
        assert followGraph != null;
        if (followGraph.has(md5Key)) {
            return followGraph.getJSONArray(md5Key).toString(2);
        }
        return new JSONArray().toString(2);
    }

    /* updatePeople - Save in the database the updated edge between two users given the request
     *
     *  @return             : success or not
     *  @followGraphEdge    : json containing information about the edge (from, to) and operation
     */
    public static boolean updatePeople(JSONObject followGraphEdge) {
        try {
            String from = followGraphEdge.getString("md5KeyFrom");
            String to = followGraphEdge.getString("md5KeyTo");
            String operation = followGraphEdge.getString("operation");
            LOGGER.log(Level.FINE, "New request from user {0} to {1} user {2}", new Object[]{operation, from, to});

            if (!containsUser(from) || !containsUser(to)) {
                return false;
            }
            switch (operation) {
                case "follow":
                    return addFollowGraphEdge(from, to);
                case "unfollow":
                    return removeFollowGraphEdge(from, to);
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* removeFollowGraphEdge - Removes the edge between from and to
     *
     *  @return             : success or not
     *  @from               : unique identifier for the current user
     *  @to                 : unique identifier for the user he wants to unfollow
     */
    private static boolean removeFollowGraphEdge(String from, String to) {
        try {
            synchronized (DatabaseManager.class) {
                JSONObject followGraph = fetchGraph();
                JSONArray followers;
                assert followGraph != null;
                if (followGraph.has(from)) {
                    followers = followGraph.getJSONArray(from);
                } else {
                    followers = new JSONArray();
                }

                int indexToRemove = -1;
                for (int i = 0; i < followers.length(); i++) {
                    if (followers.getString(i).equals(to)) {
                        indexToRemove = i;
                        break;
                    }
                }
                if (indexToRemove != -1) {
                    followers.remove(indexToRemove);
                }

                followGraph.put(from, followers);
                return saveFollowGraph(followGraph);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* addFollowGraphEdge - Adds an edge between from and to
     *
     *  @return             : success or not
     *  @from               : unique identifier for the current user
     *  @to                 : unique identifier for the user he wants to follow
     */
    private static boolean addFollowGraphEdge(String from, String to) {
        try {
            synchronized (DatabaseManager.class) {
                JSONObject followGraph = fetchGraph();
                JSONArray followers;
                assert followGraph != null;
                if (followGraph.has(from)) {
                    followers = followGraph.getJSONArray(from);
                } else {
                    followers = new JSONArray();
                }

                for (int i = 0; i < followers.length(); i++) {
                    if (followers.getString(i).equals(to)) {
                        return true;
                    }
                }

                followers.put(to);
                followGraph.put(from, followers);
                return saveFollowGraph(followGraph);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* saveFollowGraph - Save in the database the updated follow graph
     *
     *  @return             : success or not
     *  @followGraph        : the follow graph to save in database
     */
    private static boolean saveFollowGraph(JSONObject followGraph) {
        return syncDatabase(Constants.PEOPLE_DB_PATH, followGraph);
    }

    /* syncDatabase - Save in the database the updated json object
     *
     *  @return             : success or not
     *  @path               : the path for the database
     *  @jsonObject         : the object we want to save in the database
     */
    public static boolean syncDatabase(String path, JSONObject jsonObject) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path), 32768);
            out.write(jsonObject.toString(2));
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* fetchObjectFromDatabase - Retrieve from database a json object
     *
     *  @return             : the json object from the database or null
     *  @path               : the path for the database
     */
    public static JSONObject fetchObjectFromDatabase(String path) {
        try {
            InputStream is = new FileInputStream(path);
            String text = IOUtils.toString(is, "UTF-8");
            return new JSONObject(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* fetchArrayFromDatabase - Retrieve from database a json array
     *
     *  @return             : the json array from the database or null
     *  @path               : the path for the database
     */
    private static JSONArray fetchArrayFromDatabase(String path) {
        try {
            InputStream is = new FileInputStream(path);
            String text = IOUtils.toString(is, "UTF-8");
            return new JSONArray(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
