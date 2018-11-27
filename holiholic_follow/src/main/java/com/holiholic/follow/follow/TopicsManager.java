package com.holiholic.follow.follow;

import com.holiholic.follow.database.DatabaseManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* TopicsManager - Handle the operations focused on topics
 *
 */
public class TopicsManager {
    private static final Logger LOGGER = Logger.getLogger(TopicsManager.class.getName());

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

    /* updateTopics - This method adds new topics to the current user
     *
     * @return              : true and false only when encountered an error
     */
    public static boolean updateTopics(JSONObject jsonObject) {
        try {
            String uid = jsonObject.getString("uid");
            JSONArray followedTopics = jsonObject.getJSONArray("followedTopics");
            LOGGER.log(Level.FINE, "New request from {0} to follow {1} topics", new Object[]{uid,
                                                                                followedTopics.toString()});
            return DatabaseManager.updateTopics(uid, followedTopics);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* getTopics - This method returns the followed topics of the current user
     *
     * @return              : the followed topics
     */
    public static String getTopics(String uid) {
        return DatabaseManager.getTopics(uid);
    }
}
