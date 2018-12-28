package com.holiholic.planner.database;

import com.holiholic.database.api.DatabasePredicate;
import com.holiholic.database.api.Query;
import com.holiholic.database.api.SelectResult;
import com.holiholic.places.api.Places;
import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.travel.City;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* UpdateDistanceTimeMatrixAction - Updates the distance and duration matrix in the database
 *
 */
class UpdateDistanceTimeMatrixAction extends UpdateAction {

    /* execute - Make the request to the database module and cache the results if applicable
     *
     *  @return             : success or not
     *  @body               : the request body
     */
    @Override
    boolean execute(JSONObject body) {
        String cityName = body.getString("city");

        deleteOldDistances(cityName);
        Map<String, double[][]> distances = Places.getDistances(getPlaces(cityName));
        double[][] distanceDriving = distances.get("distance_driving");
        double[][] distanceWalking = distances.get("distance_walking");
        double[][] durationDriving = distances.get("duration_driving");
        double[][] durationWalking = distances.get("duration_walking");
        int placesLength = distanceDriving.length;

        for (int i = 0; i < placesLength; i++) {
            for (int j = 0; j < placesLength; j++) {
                if (i == j) {
                    continue;
                }

                List<String> valuesDriving = getValuesList(cityName, "driving",
                        durationDriving[i][j], distanceDriving[i][j], i, j);
                List<String> valuesWalking = getValuesList(cityName, "walking",
                        durationWalking[i][j], distanceWalking[i][j], i, j);

                Query.insert(Constants.PLACES_DISTANCES_TABLE_NAME, valuesDriving);
                Query.insert(Constants.PLACES_DISTANCES_TABLE_NAME, valuesWalking);
            }
        }

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

    /* escape - Escape a string into database format
     *
     *  @return             : the escaped string
     *  @string             : string to escape
     */
    private String escape(String string) {
        return "\'" + string + "\'";
    }

    /* getValuesList - Return a list of string values to insert into database INSERT query
     *
     *  @return             : list of values to insert
     *  @cityName           : city name
     *  @travelMode         : travel mode
     *  @duration           : duration to get between from and to
     *  @distance           : distance to get between from and to
     *  @from               : start
     *  @to                 : destination
     */
    private List<String> getValuesList(String cityName, String travelMode,
                                       double duration, double distance, int from, int to) {
        List<String> values = new ArrayList<>();
        values.add(escape(cityName));
        values.add(escape(travelMode));
        values.add("" + duration);
        values.add("" + distance);
        values.add("" + from);
        values.add("" + to);
        return values;
    }

    /* deleteOldDistances - Clear old values from database
     *
     *  @return             : void
     *  @cityName           : city name
     */
    private void deleteOldDistances(String cityName) {
        List<DatabasePredicate> predicates = new ArrayList<>();
        predicates.add(new DatabasePredicate("city", "=", "\'" + cityName + "\'"));
        Query.delete(Constants.PLACES_DISTANCES_TABLE_NAME, predicates);
    }

    /* getPlaces - Get a list of places only with latitude and longitude fields used to make requests
     *
     *  @return             : void
     *  @cityName           : city name
     */
    private JSONArray getPlaces(String cityName) {
        JSONArray places = new JSONArray();
        try {
            List<String> attributes = new ArrayList<>();
            List<DatabasePredicate> predicates = new ArrayList<>();
            attributes.add("latitude");
            attributes.add("longitude");
            predicates.add(new DatabasePredicate("city", "=", "\'" + cityName + "\'"));
            SelectResult result = Query.select(attributes, Constants.PLACES_TABLE_NAME, predicates);
            ResultSet resultSet = result.getResultSet();
            JSONObject place;

            while (resultSet.next()) {
                place = new JSONObject();
                place.put("latitude", resultSet.getString("latitude"));
                place.put("longitude", resultSet.getString("longitude"));
                places.put(place);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return places;
    }
}
