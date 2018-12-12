package com.holiholic.planner.update.parser;

import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.database.DatabaseManager;
import com.holiholic.planner.planner.PlanManager;
import com.holiholic.planner.travel.City;
import com.holiholic.planner.utils.Enums;
import org.json.*;
import java.io.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* ParserManager - Used to parse HTTP responses from different APIs
 *
 */
public class ParserManager {
    private static final Logger LOGGER = Logger.getLogger(ParserManager.class.getName());

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

    /* getGoogleValue - Parse the json data between two places and return the distance/duration between 2 places
     *
     *  @return             : duration or distance between two places
     *  @type               : duration or distance
     */
    private static int getGoogleValue(String jsonData, String type) {
        int value = Integer.MAX_VALUE;
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            value = jsonObject.getJSONArray("rows")
                    .getJSONObject(0)
                    .getJSONArray("elements")
                    .getJSONObject(0)
                    .getJSONObject(type).getInt("value");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    /* fetchPlacesFromDatabase - Reads the json database with places from file
     *
     *  @return             : the json array with places
     *  @path               : the places database path
     */
    private static JSONArray fetchPlacesFromDatabase(String path) {
        return DatabaseManager.fetchArrayFromDatabase(path);
    }

    /* saveMatrix - Save distance or duration matrix to database
     *
     *  @return             : success or not
     *  @path               : the matrix database path
     *  @matrix             : the updated distance or duration matrix
     */
    private static boolean saveMatrix(String path, double[][] matrix) {
        try {
            FileWriter fw = new FileWriter(path, false);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);

            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[0].length; j++) {
                    if (i == j) {
                        continue;
                    }
                    out.println(i + " " + j + " " + matrix[i][j]);
                }
            }

            bw.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* updateDistances - Download, parse and cache the distance matrix between places
     *
     *  @return                 : void
     *  @body                   : the network json body request
     */
    public static boolean updateDistances(JSONObject body) {
        try {
            String cityName = body.getString("city");
            String travelMode = body.getString("travelMode");
            LOGGER.log(Level.FINE, "Update {0} distance and duration matrix for {0} city",
                       new Object[]{travelMode, cityName});
            JSONArray places = fetchPlacesFromDatabase(DatabaseManager.getDatabasePath(Enums.FileType.PLACES, cityName));
            double[][] durationMatrix = new double[places.length()][places.length()];
            double[][] distanceMatrix = new double[places.length()][places.length()];

            for (int i = 0; i < places.length(); i++) {
                JSONObject place = places.getJSONObject(i);
                for (int j = 0; j < places.length(); j++) {
                    if (i == j)
                        continue;

                    JSONObject next = places.getJSONObject(j);
                    int placeId = place.getInt("id");
                    int nextId = next.getInt("id");
                    String origin = "" + place.getDouble("latitude") + "," + place.getDouble("longitude");
                    String destination = "" + next.getDouble("latitude") + "," + next.getDouble("longitude");
                    String url = URLManager.buildDistanceMatrixURL(origin, destination, travelMode);
                    String urlContent = URLManager.getContentFromURL(url);
                    durationMatrix[placeId][nextId] = getGoogleValue(urlContent, "duration");
                    distanceMatrix[placeId][nextId] = getGoogleValue(urlContent, "distance");
                    LOGGER.log(Level.FINE, "new duration from {0} to {1} is {2} seconds\nnew distance from {3} to {4} is {5} meters",
                            new Object[]{placeId, nextId, durationMatrix[placeId][nextId],
                                         placeId, nextId, distanceMatrix[placeId][nextId]});
                }
            }

            synchronized (DatabaseManager.class) {
                Enums.FileType fileTypeDuration = Enums.FileType.getDuration(Enums.TravelMode.deserialize(travelMode));
                String path = DatabaseManager.getDatabasePath(fileTypeDuration, cityName);
                if (!saveMatrix(path, durationMatrix)) {
                    return false;
                }
                LOGGER.log(Level.FINE, "Saved {0} duration matrix from {0} city in database",
                           new Object[]{travelMode, cityName});

                Enums.FileType fileTypeDistance = Enums.FileType.getDistance(Enums.TravelMode.deserialize(travelMode));
                path = DatabaseManager.getDatabasePath(fileTypeDistance, cityName);
                if (!saveMatrix(path, distanceMatrix)) {
                    return false;
                }
                LOGGER.log(Level.FINE, "Saved {0} distance matrix from {0} city in database",
                           new Object[]{travelMode, cityName});

                return DatabaseManager.updateCacheMatrix(cityName, fileTypeDuration, durationMatrix) &&
                       DatabaseManager.updateCacheMatrix(cityName, fileTypeDistance, distanceMatrix);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* addRestaurantInformation - Add a pair (key, value) to restaurant
     *
     *  @return             : void
     *  @restaurant         : the restaurant where to put information
     *  @key                : the key
     *  @value              : the value
     */
    private static void addRestaurantInformation(JSONObject restaurant,
                                                 String key,
                                                 Object value) {
        try {
            restaurant.put(key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* addRestaurantInformation - Add a pair (key, value) to restaurant, where the value is retrieved from jsonData
     *
     *  @return             : void
     *  @restaurant         : the restaurant where to put information
     *  @key                : the key
     *  @jsonData           : from where to retrieve the value given the key
     */
    private static void addRestaurantInformation(JSONObject restaurant,
                                                 String key,
                                                 JSONObject jsonData) {
        try {
            if (jsonData.has(key)) {
                addRestaurantInformation(restaurant, key, jsonData.get(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
            restaurant.put(key, "");
        }
    }

    /* addRestaurantInformation - Add additional information to a place; Need a new HTTP request to Google
     *
     *  @return             : void
     *  @restaurant         : the restaurant where to put information
     *  @additionalDetails  : the additional information to put
     */
    private static void addRestaurantInformation(JSONObject restaurant,
                                                 JSONObject additionalDetails) {
        try {
            addRestaurantInformation(restaurant, "types", additionalDetails);
            if (additionalDetails.has("opening_hours")) {
                addRestaurantInformation(restaurant, "openingHours",
                        additionalDetails.getJSONObject("opening_hours")
                                .getJSONArray("periods"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* getRestaurant - Decode a request into internal JSON representation of a restaurant
     *
     *  @return             : the restaurant
     *  @urlContent         : json data from Google
     */
    private static JSONObject getRestaurant(JSONObject urlContent) {
        try {
            JSONObject restaurant = new JSONObject();
            addRestaurantInformation(restaurant, "name", urlContent);
            addRestaurantInformation(restaurant, "vicinity", urlContent);
            addRestaurantInformation(restaurant, "rating", urlContent);
            addRestaurantInformation(restaurant, "latitude", urlContent.getJSONObject("geometry")
                                                                       .getJSONObject("location")
                                                                       .getDouble("lat"));
            addRestaurantInformation(restaurant, "longitude", urlContent.getJSONObject("geometry")
                                                                        .getJSONObject("location")
                                                                        .getDouble("lng"));
            String placeId = urlContent.getString("place_id");
            String urlPlaceDetail = URLManager.buildPlaceDetailURL(placeId);
            String placeDetailJson = URLManager.getContentFromURL(urlPlaceDetail);
            addRestaurantInformation(restaurant, new JSONObject(placeDetailJson).getJSONObject("result"));

            return restaurant;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* getRestaurants - Get a list of restaurants given the raw response from Google
     *
     *  @return             : the restaurants nearby a place
     *  @urlContent         : raw json data from Google
     */
    private static JSONArray getRestaurants(String urlContent) {
        try {
            JSONArray result = new JSONArray();
            JSONArray restaurants = new JSONObject(urlContent).getJSONArray("results");
            for (int i = 0; i < restaurants.length(); i++) {
                result.put(getRestaurant(restaurants.getJSONObject(i)));
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* saveRestaurants - Save restaurants in the database and update the city instance if cache
     *
     *  @return             : success or not
     *  @cityName           : the name of the current city
     *  @restaurants        : the updated restaurants for the current city
     */
    private static boolean saveRestaurants(String cityName, JSONArray restaurants) {
        try {
            LOGGER.log(Level.FINE, "Save restaurants from {0} city in database", cityName);
            synchronized (DatabaseManager.class) {
                if (!DatabaseManager.syncDatabase(DatabaseManager.getDatabasePath(Enums.FileType.RESTAURANTS, cityName),
                                                  restaurants)) {
                    return false;
                }

                // if not cached our job is done
                if (!DatabaseManager.isCityCached(cityName)) {
                    return true;
                }

                // otherwise we need to update the city restaurants
                City city = City.getInstance(cityName);
                city.setRestaurants(DatabaseManager.decodeRestaurants(restaurants));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* updateRestaurants - Download, parse and save the restaurants in the databse
     *
     *  @return             : void
     *  @cityName           : the city where to retrieve information
     *  @destinationFileName: where to store the results
     */
    public static boolean updateRestaurants(JSONObject body) {
        try {
            String cityName = body.getString("city");
            LOGGER.log(Level.FINE, "Update restaurants from {0} city", cityName);
            JSONArray places = fetchPlacesFromDatabase(DatabaseManager.getDatabasePath(Enums.FileType.RESTAURANTS, cityName));
            JSONArray updatedRestaurants = new JSONArray();

            for (int i = 0; i < places.length(); i++) {
                JSONObject place = places.getJSONObject(i);
                String url = URLManager.buildRestaurantsURL(String.valueOf(place.getDouble("latitude")),
                                                            String.valueOf(place.getDouble("longitude")),
                                                            Constants.RESTAURANTS_SEARCH_RADIUS);
                String urlContent = URLManager.getContentFromURL(url);
                LOGGER.log(Level.FINE, "Download restaurant from url: {0} for place \"{1}\" from {2} city",
                           new Object[]{url, place.getString("name"), cityName});
                JSONArray restaurants = getRestaurants(urlContent);
                if (restaurants == null) {
                    continue;
                }
                JSONObject placeInformation = new JSONObject();
                placeInformation.put("id", place.getInt("id"));
                placeInformation.put("name", place.getString("name"));
                placeInformation.put("restaurants", restaurants);
                updatedRestaurants.put(placeInformation);
            }

            return saveRestaurants(cityName, updatedRestaurants);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
