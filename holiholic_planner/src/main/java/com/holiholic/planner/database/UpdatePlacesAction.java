package com.holiholic.planner.database;

import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.travel.City;
import org.json.JSONObject;

import java.util.Map;

/* UpdatePlacesAction - Updates the places in the database
 *
 */
class UpdatePlacesAction extends UpdateAction {

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
        String url = Constants.GET_PLACES_URL + "?city=" + cityName;
        Map<Integer, Place> places = DatabaseManager.getPlaces(url);

        if (!DatabaseManager.isCityCached(cityName)) {
            return true;
        }

        City city = DatabaseManager.getCity(cityName);
        if (city == null) {
            // there must be an error somewhere
            return false;
        }
        city.setPlaces(places);
        DatabaseManager.cacheCity(city);
        return true;
    }
}
