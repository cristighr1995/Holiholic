package com.holiholic.planner.update.action;

import com.holiholic.planner.update.parser.ParserManager;
import org.json.JSONObject;

/* UpdateDistanceAction - Update the distance matrix for a city given the mode of travel
 *
 */
public class UpdateDistanceAction implements Action {
    private JSONObject body;

    // constructor
    UpdateDistanceAction(JSONObject body) {
        this.body = body;
    }

    /* execute - Cache the distances in file
     *
     *  @return             : void
     */
    public boolean execute() {
        return ParserManager.updateDistances(body);
    }
}
