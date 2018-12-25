package com.holiholic.planner.database;

import com.holiholic.database.api.DatabasePredicate;
import com.holiholic.database.api.Query;
import com.holiholic.database.api.SelectResult;
import com.holiholic.places.api.PlaceCategory;
import com.holiholic.planner.planner.PlanManager;
import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.travel.City;
import com.holiholic.planner.utils.*;
import com.holiholic.planner.utils.Reader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* DatabaseManager - Handle the requests to the database and (pre)process the queries
 *
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    // after the first call of retrieving information from database for a city, the result is cached in this map
    private final static Map<String, City> cities = new HashMap<>();

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

        PlanManager.setLogger();
    }

    /* containsUser - Check if the current user is in the system
     *
     *  @return             : true or false
     *  @uid                : the current user id
     */
    public static boolean containsUser(String uid) {
        return true;
    }

    /* getPlaces - Get the places from the database making a HTTP GET and deserialize places
     *
     *  @return             : places
     *  @cityName           : city
     */
    static Map<Integer, Place> getPlaces(String cityName) {
        Map<Integer, Place> places = new HashMap<>();
        Place place;
        List<DatabasePredicate> predicates = new ArrayList<>();
        predicates.add(new DatabasePredicate("city", "=", "\'" + cityName + "\'"));
        SelectResult result = Query.select(null, Constants.PLACES_TABLE_NAME, predicates);

        try {
            int id, duration;
            String name, description, imageUrl, categoryName, categoryTopic;
            double rating, latitude, longitude;
            JSONArray timeFrames;
            ResultSet resultSet = result.getResultSet();
            while (resultSet.next()) {
                id = resultSet.getInt("id");
                name = resultSet.getString("name");
                description = resultSet.getString("description");
                imageUrl = resultSet.getString("imageUrl");
                rating = resultSet.getDouble("rating");
                categoryName = resultSet.getString("categoryName");
                categoryTopic = resultSet.getString("categoryTopic");
                duration = resultSet.getInt("duration");
                latitude = resultSet.getDouble("latitude");
                longitude = resultSet.getDouble("longitude");
                timeFrames = new JSONArray(resultSet.getString("timeFrames"));

                place = new Place(id, name, description, imageUrl, rating, new PlaceCategory(categoryName, categoryTopic),
                                  duration, new GeoPosition(latitude, longitude), TimeFrame.deserialize(timeFrames));
                places.put(id, place);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            result.close();
        }

        return places;
    }

    /* serializePlaces - Serialize a list of places to be sent over network
     *
     *  @return             : serialized places into a json array format
     *  @places             : list of places to be serialized
     */
    private static JSONArray serializePlaces(List<Place> places) {
        JSONArray serializedPlaces = new JSONArray();

        for (Place place : places) {
            serializedPlaces.put(place.serialize());
        }

        return serializedPlaces;
    }

    /* filterPlaces - Filter places from a city that can visited in the allocated time frame and have specific tags
     *
     *  @return             : filtered places ready to be sent over network
     *  @city               : the city instance
     *  @categories         : places categories names
     *  @timeFrame          : the time frame to search for open places
     */
    private static JSONArray filterPlaces(City city, Set<String> categories, TimeFrame timeFrame) {
        return serializePlaces(city.getSortedPlaces(city.getOpenPlaces(city.getFilteredPlaces(categories), timeFrame)));
    }

    /* getPlaces - Reads the database and collects all the places from a specific city
    *              This function sorts the places before sending them to user
    *
    *  @return          : a json array with places
    *  @body            : the json containing user's information
    */
    public static String getPlaces(JSONObject body) {
        try {
            String uid = body.getString("uid");
            String cityName = body.getString("city").toLowerCase();

            if (!containsUser(uid)) {
                LOGGER.log(Level.FINE, "Invalid request from user {0} to get places recommendation for {1} city",
                           new Object[]{uid, cityName});
                return "[]";
            }

            Set<String> placeCategories = new HashSet<>();
            JSONArray categories = body.getJSONArray("categories");
            for (int t = 0; t < categories.length(); t++) {
                placeCategories.add(categories.getString(t));
            }
            TimeFrame timeFrame = TimeFrame.deserialize(body.getJSONArray("timeFrame"));

            LOGGER.log(Level.FINE, "New request from user {0} to get places recommendation for {1} city",
                       new Object[]{uid, cityName});

            if (isCityCached(cityName)) {
                return filterPlaces(cities.get(cityName), placeCategories, timeFrame).toString(2);
            }

            Map<Integer, Place> places = getPlaces(cityName);

            City city = City.getInstance(cityName);
            city.setPlaces(places);
            cities.put(cityName, city);

            return filterPlaces(city, placeCategories, timeFrame).toString(2);
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    /* getCity - Returns the instance of the city
     *
     *  @return       : a city instance
     *  @cityName     : the city where the user wants to go/to visit
     */
    public static City getCity(String cityName) {
        if (cities.containsKey(cityName)) {
            return cities.get(cityName);
        }
        return null;
    }

    /* cacheCity - Stores in cache a new or updated city
     *
     *  @return         : void
     *  @city           : the city instance
     */
    static void cacheCity(City city) {
        cities.put(city.getName(), city);
    }

    /* isCityCached - Checks if the city instance is cached
     *
     *  @return       : true or false
     *  @cityName     : the city where the user wants to go/to visit
     */
    static boolean isCityCached(String cityName) {
        return cities.containsKey(cityName);
    }

    /* updateHistory - Save a plan into a specific user history
     *
     *  @return             : success or not
     *  @body               : the network json body request
     */
    public static boolean updateHistory(JSONObject body) {
        try {
            String cityName = body.getString("city").toLowerCase();
            String uid = body.getString("uid");
            LOGGER.log(Level.FINE, "User {0} wants to save itinerary from {1} city", new Object[]{uid, cityName});
            // TODO save history in database
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* isKeyAuthorized - Checks if the current access key is authorized to update the planner database
     *
     *  @return             : success or not
     *  @accessKey          : the current access key
     */
    private static boolean isKeyAuthorized(String accessKey) {
        try {
            Reader.init(new FileInputStream(Constants.UPDATE_ACCESS_KEY_PATH));
            return Reader.readLine().equals(accessKey);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* updatePlanner - Updates the planner database like distances, weather, restaurants etc
     *
     *  @return             : success or not
     *  @body               : the body of the HTTP POST request
     */
    public static boolean updatePlanner(JSONObject body) {
        try {
            if (!isKeyAuthorized(body.getString("accessKey"))) {
                LOGGER.log(Level.FINE, "Unauthorized key to update planner database");
                return false;
            }
            String type = body.getString("type");
            String cityName = body.getString("city").toLowerCase();
            LOGGER.log(Level.FINE, "New request to update {0} database for {1} city", new Object[]{type, cityName});

            UpdateAction action = UpdateAction.Factory.getInstance(type);
            if (action == null) {
                LOGGER.log(Level.FINE, "Invalid type to update the database");
                return false;
            }

            return action.execute(body);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* getMatrix - Get and deserialize matrix from the database
     *
     *  @return             : matrix
     *  @cityName           : city instance
     *  @travelMode         : driving or walking
     *  @travelInfo         : duration or distance
     */
    public static double[][] getMatrix(String cityName, Enums.TravelMode travelMode, Enums.TravelInfo travelInfo) {
        int dimension = 0;
        List<DatabasePredicate> predicates = new ArrayList<>();
        predicates.add(new DatabasePredicate("city", "=", "\'" + cityName + "\'"));
        SelectResult resultCount = Query.select(Collections.singletonList("count(*)"), Constants.PLACES_TABLE_NAME, predicates);
        try {
            if (resultCount.getResultSet().next()) {
                dimension = resultCount.getResultSet().getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            resultCount.close();
        }

        if (dimension == 0) {
            return null;
        }

        double[][] matrix = new double[dimension][dimension];
        predicates.add(new DatabasePredicate("travelMode", "=", "\'" + Enums.TravelMode.serialize(travelMode) + "\'"));
        SelectResult result = Query.select(null, Constants.PLACES_DISTANCES_TABLE_NAME, predicates);
        try {
            ResultSet resultSet = result.getResultSet();
            int from, to;
            double value;
            while (resultSet.next()) {
                from = resultSet.getInt("from");
                to = resultSet.getInt("to");
                value = resultSet.getDouble(Enums.TravelInfo.serialize(travelInfo));
                matrix[from][to] = value;
            }
        } catch (Exception e) {
            e.printStackTrace();
            matrix = null;
        } finally {
            result.close();
        }
        return matrix;
    }
}
