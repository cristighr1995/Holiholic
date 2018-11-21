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
            String md5KeyFrom = request.getString("md5KeyFrom");
            String md5KeyTo = request.getString("md5KeyTo");
            String operation = follow ? "follow" : "unfollow";

            LOGGER.log(Level.FINE, "New request from {0} to {1} {2}", new Object[]{md5KeyFrom, operation, md5KeyTo});

            return DatabaseManager.updatePeople(md5KeyFrom, md5KeyTo, operation);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* getPeople - This method returns the followers of the current user
     *
     * @return              : the followed people
     */
    public static String getPeople(String md5Key) {
        return DatabaseManager.getPeople(md5Key);
    }
}
