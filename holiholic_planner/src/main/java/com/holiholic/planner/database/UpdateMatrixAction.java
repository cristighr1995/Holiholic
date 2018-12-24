package com.holiholic.planner.database;

import com.holiholic.database.api.DatabasePredicate;
import com.holiholic.database.api.Query;
import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.travel.City;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
        String cityName = body.getString("city");

        // refresh database
        deleteOldDistances(cityName);

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

    private void deleteOldDistances(String cityName) {
        List<DatabasePredicate> predicates = new ArrayList<>();
        predicates.add(new DatabasePredicate("city", "=", "\'" + cityName + "\'"));
        Query.delete(Constants.PLACES_TABLE_NAME, predicates);
    }
}
