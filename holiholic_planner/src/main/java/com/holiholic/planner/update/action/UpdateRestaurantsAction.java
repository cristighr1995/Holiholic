package com.holiholic.planner.update.action;

import com.holiholic.planner.update.parser.ParserManager;
import org.json.JSONObject;

/* UpdateRestaurantsAction - Update the restaurants from a city
 *
 */
public class UpdateRestaurantsAction implements Action {
    private JSONObject body;

    // constructor
    UpdateRestaurantsAction(JSONObject body) {
        this.body = body;
    }

    /* execute - Cache the restaurants in file
     *
     *  @return             : void
     */
    public boolean execute() {
        return ParserManager.updateRestaurants(body);
    }
}
