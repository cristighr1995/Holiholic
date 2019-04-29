package com.holiholic.planner.database;

import com.holiholic.database.api.DatabasePredicate;
import com.holiholic.database.api.Query;
import com.holiholic.database.api.SelectResult;
import com.holiholic.places.api.PlaceCategory;
import com.holiholic.places.api.Places;
import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.travel.City;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
        String cityName = body.getString("city");

        // refresh database
        deleteOldPlaces(cityName);

        List<PlaceCategory> categories = getPlacesCategories();
        int placesCount = 0;

        for (PlaceCategory category : categories) {
            JSONArray places = Places.getPlaces(cityName, category);

            for (int i = 0; i < places.length(); i++) {
                JSONObject place = places.getJSONObject(i);
                // really important to set the correct id
                place.put("id", placesCount++);

                List<String> values = getValueList(place, cityName);
                if (values == null) {
                    continue;
                }
                Query.insert(Constants.PLACES_TABLE_NAME, values);
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

        city.setPlaces(DatabaseManager.getPlaces(cityName));
        DatabaseManager.cacheCity(city);
        return true;
    }

    /* deleteOldDistances - Clear old values from database
     *
     *  @return             : void
     *  @cityName           : city name
     */
    private void deleteOldPlaces(String cityName) {
        List<DatabasePredicate> predicates = new ArrayList<>();
        predicates.add(new DatabasePredicate("city", "=", "\'" + cityName + "\'"));
        Query.delete(Constants.PLACES_TABLE_NAME, predicates);
    }

    /* getPlacesCategories - Return a list of place categories
     *
     *  @return             : a list of place categories
     */
    private List<PlaceCategory> getPlacesCategories() {
        List<PlaceCategory> categories = new ArrayList<>();
        SelectResult result = Query.select(null, Constants.PLACES_CATEGORIES_TABLE_NAME, null);
        String name, id, topic;
        int duration, limit;

        try {
            while (result.getResultSet().next()) {
                name = result.getResultSet().getString("name");
                id = result.getResultSet().getString("id");
                topic = result.getResultSet().getString("topic");
                duration = result.getResultSet().getInt("duration");
                limit = result.getResultSet().getInt("limit");
                categories.add(new PlaceCategory(name, id, topic, duration, limit));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            result.close();
        }

        return categories;
    }

    /* getValuesList - Return a list of string values to insert into database INSERT query
     *
     *  @return             : list of values to insert
     *  @place              : place in network format
     *  @cityName           : city name
     */
    private List<String> getValueList(JSONObject place, String cityName) {
        List<String> values = new ArrayList<>();

        try {
            values.add("" + place.getInt("id"));
            values.add(DatabaseManager.escape(cityName));
            values.add(DatabaseManager.escape(place.getString("name")));
            values.add(DatabaseManager.escape(place.getString("description")));
            values.add(DatabaseManager.escape(place.getString("imageUrl")));
            values.add("" + place.getDouble("rating"));
            values.add(DatabaseManager.escape(place.getJSONObject("category").getString("name")));
            values.add(DatabaseManager.escape(place.getJSONObject("category").getString("topic")));
            values.add("" + place.getInt("duration"));
            values.add("" + place.getDouble("latitude"));
            values.add("" + place.getDouble("longitude"));
            values.add(DatabaseManager.escape(place.getJSONArray("timeFrames").toString()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return values;
    }
}
