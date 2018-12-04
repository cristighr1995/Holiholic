package com.holiholic.planner.update.action;

import org.json.JSONObject;

/* ActionFactory - Creates a new instance given the command line arguments
 *
 */
public class ActionFactory {

    public static Action getAction(JSONObject body) {
        try {
            switch (body.getString("operation")) {
                case "updateDistances":
                    return new UpdateDistanceAction(body);
                case "updateRestaurants":
                    return new UpdateRestaurantsAction(body);
                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
