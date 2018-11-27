package com.holiholic.follow.follow;

import com.holiholic.follow.database.DatabaseManager;
import org.json.JSONObject;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* PeopleManager - Handle the operations focused on people
 *
 */
public class PeopleManager {
    private static final Logger LOGGER = Logger.getLogger(PeopleManager.class.getName());

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

    /* updatePeople - This method adds new followers to the current user or deletes followers
     *
     * @return              : true and false only when encountered an error
     * @request             : the body for the request
     * @follow              : add or remove link between the two users
     */
    public static boolean updatePeople(JSONObject request, boolean follow) {
        try {
            String uidFrom = request.getString("uidFrom");
            String uidTo = request.getString("uidTo");
            String operation = follow ? "follow" : "unfollow";

            LOGGER.log(Level.FINE, "New request from {0} to {1} {2}", new Object[]{uidFrom, operation, uidTo});

            return DatabaseManager.updatePeople(uidFrom, uidTo, operation);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* getPeople - This method returns the followers of the current user
     *
     * @return              : the followed people
     */
    public static String getPeople(String uid) {
        return DatabaseManager.getPeople(uid);
    }
}
