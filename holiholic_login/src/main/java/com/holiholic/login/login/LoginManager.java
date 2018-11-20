package com.holiholic.login.login;

import com.holiholic.login.database.DatabaseManager;
import org.json.JSONObject;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* LoginManager - Handle the login operations
 *
 */
public class LoginManager {
    private static final Logger LOGGER = Logger.getLogger(LoginManager.class.getName());

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

    /* login - Checks if a user exists in the system, if not, will save the profile in the database
     *
     *  @return             : true and false only when encountered an error
     */
    public static boolean login(JSONObject request) {
        try {
            LOGGER.log(Level.FINE, "New request to authenticate from {0}", request.getString("email"));

            String md5key = DatabaseManager.generateMD5(request.getString("email"));
            if (!DatabaseManager.containsUser(md5key)) {
                request.put("md5Key", md5key);
                return registerUser(request);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /* registerUser - Save the new user in the database
     *
     *  @return             : true and false only when encountered an error
     *  @jsonProfile        : the profile of the current user (json format)
     */
    private static boolean registerUser(JSONObject jsonProfile) {
        return DatabaseManager.registerUser(jsonProfile);
    }
}
