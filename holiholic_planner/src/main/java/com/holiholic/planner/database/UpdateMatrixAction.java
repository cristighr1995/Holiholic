package com.holiholic.planner.database;

import com.holiholic.planner.travel.City;
import org.json.JSONObject;

/* UpdateMatrixAction - Updates the distance and duration matrix in the database
 *
 */
class UpdateMatrixAction extends UpdateAction {

    /* execute - Make the request to the database module and cache the results if applicable
     *
     *  @return             : success or not
     *  @body               : the request body
     */
    @Override
    boolean execute(JSONObject body) {
        // make the call to update the database
        super.execute(body);

        // cache the results if applicable
        String cityName = body.getString("city");

        if (!DatabaseManager.isCityCached(cityName)) {
            return true;
        }

        City city = DatabaseManager.getCity(cityName);
        if (city == null) {
            // there must be an error somewhere
            return false;
        }
        city.setDurations();
        city.setDistances();
        DatabaseManager.cacheCity(city);
        return true;
    }
}
