package com.holiholic.planner.database;

import org.json.JSONObject;

/* UpdateAction - Handle the update planner database operations
 *
 */
abstract class UpdateAction {

    /* Factory - Create an instance of update action
     *
     */
    static class Factory {
        static UpdateAction getInstance(String type) {
            switch (type) {
                case "places":
                    return new UpdatePlacesAction();
                case "distanceTimeMatrix":
                    return new UpdateDistanceTimeMatrixAction();
                default:
                    return null;
            }
        }
    }

    /* execute - Make the request to the database module
     *
     *  @return             : success or not
     *  @body               : the request body
     */
    abstract boolean execute(JSONObject body);
}
