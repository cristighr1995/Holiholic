package com.holiholic.planner.database;

import com.holiholic.database.api.DatabasePredicate;
import com.holiholic.database.api.Query;
import com.holiholic.database.api.SelectResult;
import com.holiholic.places.api.PlaceCategory;
import com.holiholic.planner.planner.PlanManager;
import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.planner.Planner;
import com.holiholic.planner.travel.AvailableCity;
import com.holiholic.planner.travel.City;
import com.holiholic.planner.travel.Itinerary;
import com.holiholic.planner.travel.ItineraryStats;
import com.holiholic.planner.utils.*;
import com.holiholic.planner.utils.Reader;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.sql.ResultSet;
import java.time.LocalDateTime;
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

    // cache itineraries to reduce the number of database queries
    private final static Map<String, Itinerary> itineraries = new HashMap<>();

    // cache PlaceCategory to reduce redundant calls to database used in their deserialization / construction
    private final static Map<String, PlaceCategory> placeCategories = new HashMap<>();

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

    /* filterPlaces - Filter places from a city having specific tags
     *
     *  @return             : filtered places ready to be sent over network
     *  @city               : the city instance
     *  @categories         : places categories names
     */
    private static JSONArray filterPlaces(City city, Set<String> categories) {
        return serializePlaces(city.getSortedPlaces(city.getFilteredPlaces(categories)));
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
            TimeFrame timeFrame = null;

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

            boolean openOnly = body.getBoolean("openOnly");
            if (openOnly) {
                if (!body.has("timeFrame")) {
                    LOGGER.log(Level.FINE, "Invalid request from user {0} to get places recommendation for {1} city",
                               new Object[]{uid, cityName});
                    return "[]";
                }
                timeFrame = TimeFrame.deserialize(body.getJSONArray("timeFrame"));
            }

            LOGGER.log(Level.FINE, "New request from user {0} to get places recommendation for {1} city",
                       new Object[]{uid, cityName});

            if (isCityCached(cityName)) {
                if (openOnly) {
                    return filterPlaces(cities.get(cityName), placeCategories, timeFrame).toString(2);
                }
                return filterPlaces(cities.get(cityName), placeCategories).toString(2);
            }

            Map<Integer, Place> places = getPlaces(cityName);

            City city = new City(cityName);
            city.setPlaces(places);
            cities.put(cityName, city);

            if (openOnly) {
                return filterPlaces(city, placeCategories, timeFrame).toString(2);
            }
            return filterPlaces(city, placeCategories).toString(2);
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    /* getAvailableCities - Reads the database and collects all available cities
     *
     *  @return          : a list with available cities
     */
    private static List<AvailableCity> getAvailableCities() {
        // Query: SELECT * from Cities
        SelectResult result = Query.select(null, Constants.CITIES_TABLE_NAME, null);
        List<AvailableCity> availableCities;

        try {
            int placesCount;
            String city, country, imageUrl, description;
            ResultSet resultSet = result.getResultSet();

            availableCities = new ArrayList<>();

            while (resultSet.next()) {
                city = resultSet.getString("city");
                country = resultSet.getString("country");
                placesCount = resultSet.getInt("placesCount");
                imageUrl = resultSet.getString("imageUrl");
                description = resultSet.getString("description");

                availableCities.add(new AvailableCity(city, country, placesCount, imageUrl, description));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            result.close();
        }

        return availableCities;
    }

    /* getAvailableCities - Get available cities in serialized format
     *
     *  @return          : a json array with available cities
     */
    public static String getAvailableCitiesSerialized() {
        LOGGER.log(Level.FINE, "New request to get available cities");

        List<AvailableCity> availableCities = getAvailableCities();
        if (availableCities == null) {
            return "[]";
        }

        JSONArray response = new JSONArray();

        for (AvailableCity city : availableCities) {
            response.put(city.serialize());
        }

        return response.toString(2);
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
     *  @dimension          : dimension of matrix
     */
    public static double[][] getMatrix(String cityName, Enums.TravelMode travelMode, Enums.TravelInfo travelInfo,
                                       int dimention) {
        double[][] matrix = new double[dimention][dimention];
        List<DatabasePredicate> predicates = new ArrayList<>();
        predicates.add(new DatabasePredicate("city", "=", "\'" + cityName + "\'"));
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

    public static String generateHash(String text) {
        return DigestUtils.md5Hex(text);
    }

    /* escape - Escape a string into database format
     *
     *  @return             : the escaped string
     *  @string             : string to escape
     */
    public static String escape(String string) {
        // escape one quote
        string = string.replace("\'", "\\\'");
        // remove double quotes
        string = string.replace("\\\"", "");
        return "\'" + string + "\'";

    }

    public static void savePlan(String cityName, List<List<Place>> plan) {
        for (List<Place> places : plan) {
            Itinerary itinerary = new Itinerary(cityName, places);
            String itineraryId = itinerary.getId();

            if (itineraries.containsKey(itineraryId)) {
                LOGGER.log(Level.FINE, "Itinerary {0} already in cache.", itineraryId);
                continue;
            }

            itineraries.put(itineraryId, itinerary);
            Query.insert(Constants.CALCULATED_ITINERARIES_TABLE_NAME, itinerary.getValuesList());
            LOGGER.log(Level.FINE, "Saved itinerary {0} in the database.", itineraryId);
        }
    }

    public static PlaceCategory getPlaceCategory(String topic, String name) {
        String placeCategoryLookupKey = topic + " - " + name;

        if (placeCategories.containsKey(placeCategoryLookupKey)) {
            return placeCategories.get(placeCategoryLookupKey);
        }

        SelectResult result = Query.select("SELECT * from "
                                           + Constants.PLACES_CATEGORIES_TABLE_NAME
                                           + " WHERE topic = \'" + topic + "\' and"
                                           + " name = " + "\'" + name + "\';");
        String id;
        int duration, limit;

        try {
            ResultSet resultSet = result.getResultSet();
            if (resultSet.next()) {
                name = resultSet.getString("name");
                id = resultSet.getString("id");
                duration = resultSet.getInt("duration");
                limit = resultSet.getInt("limit");

                PlaceCategory placeCategory = new PlaceCategory(name, id, topic, duration, limit);
                placeCategories.put(placeCategoryLookupKey, placeCategory);

                return placeCategory;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public static boolean cacheItineraries(String cityName) {
        LOGGER.log(Level.FINE, "New request to cache itineraries for {0} city ", cityName);

        List<DatabasePredicate> predicates = new ArrayList<>();
        predicates.add(new DatabasePredicate("city", "=", "\'" + cityName + "\'"));
        SelectResult result = Query.select(null, Constants.CALCULATED_ITINERARIES_TABLE_NAME, predicates);

        try {
            String id;
            LocalDateTime timestamp;
            List<Place> places;
            ItineraryStats stats;
            ResultSet resultSet = result.getResultSet();

            while (resultSet.next()) {
                id = resultSet.getString("id");
                timestamp = resultSet.getTimestamp("timestamp").toLocalDateTime();
                places = Planner.deserializePlacesFromItinerary(new JSONArray(resultSet.getString("itinerary")));

                if (places == null) {
                    return false;
                }

                stats = ItineraryStats.deserialize(new JSONObject(resultSet.getString("stats")));

                if (stats == null) {
                    return false;
                }

                Itinerary itinerary = new Itinerary(id, cityName, timestamp, places, stats);
                itineraries.put(id, itinerary);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            result.close();
        }

        return true;
    }
}
