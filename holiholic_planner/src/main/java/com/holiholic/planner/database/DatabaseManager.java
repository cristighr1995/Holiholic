package com.holiholic.planner.database;

import com.holiholic.planner.planner.PlanManager;
import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.travel.City;
import com.holiholic.planner.utils.*;
import com.holiholic.planner.utils.Reader;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
        String url = Constants.CONTAINS_USER_URL + "?uid=" + uid;
        try {
            return Boolean.parseBoolean(getContentFromURL(url));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* getPlaces - Get the places from the database making a HTTP GET and deserialize places
     *
     *  @return             : places
     *  @url                : the url for the GET request
     */
    private static Map<Integer, Place> getPlaces(String url) {
        try {
            String rawPlaces = getContentFromURL(url);
            return deserializePlaces(rawPlaces);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* deserializePlaces - Deserialize places from raw string format
     *
     *  @return             : places
     *  @rawPlaces          : raw string (json) containing places information
     */
    private static Map<Integer, Place> deserializePlaces(String rawPlaces) {
        try {
            JSONArray placesArray = new JSONArray(rawPlaces);
            Map<Integer, Place> places = new HashMap<>();

            for (int i = 0; i < placesArray.length(); i++) {
                Place place = Place.deserialize(placesArray.getJSONObject(i));
                if (place == null) {
                    continue;
                }
                places.put(place.id, place);
            }

            return places;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
     *  @tags               : desired tags
     *  @timeFrame          : the time frame to search for open places
     */
    private static JSONArray filterPlaces(City city, Set<String> tags, TimeFrame timeFrame) {
        return serializePlaces(city.getSortedPlaces(city.getOpenPlaces(city.getFilteredPlaces(tags), timeFrame)));
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
            String cityName = body.getString("city");

            if (!containsUser(uid)) {
                LOGGER.log(Level.FINE, "Invalid request from user {0} to get places recommendation for {1} city",
                           new Object[]{uid, cityName});
                return "[]";
            }

            Set<String> tags = new HashSet<>();
            JSONArray bodyTags = body.getJSONArray("tags");
            for (int t = 0; t < bodyTags.length(); t++) {
                tags.add(bodyTags.getString(t));
            }
            TimeFrame period = TimeFrame.deserialize(body.getJSONArray("timeFrame"));

            LOGGER.log(Level.FINE, "New request from user {0} to get places recommendation for {1} city",
                       new Object[]{uid, cityName});

            if (isCityCached(cityName)) {
                return filterPlaces(cities.get(cityName), tags, period).toString(2);
            }

            String url = Constants.GET_PLACES_URL + "?city=" + cityName + "&uid=" + uid;
            Map<Integer, Place> places = getPlaces(url);

            City city = City.getInstance(cityName);
            city.setPlaces(places);
            cities.put(cityName, city);

            return filterPlaces(city, tags, period).toString(2);
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

    /* isCityCached - Checks if the city instance is cached
     *
     *  @return       : true or false
     *  @cityName     : the city where the user wants to go/to visit
     */
    private static boolean isCityCached(String cityName) {
        return cities.containsKey(cityName);
    }

    /* getContentFromURL - Returns the content from a http get request
     *
     *  @return             : the content
     *  @strUrl             : the url with the get request
     */
    private static String getContentFromURL(String strUrl) throws IOException {
        String data = null;
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        String line;
        try {
            URL url = new URL(strUrl);
            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url
            urlConnection.connect();
            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            assert iStream != null;
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /* updateHistory - Save a plan into a specific user history
     *
     *  @return             : success or not
     *  @body               : the network json body request
     */
    public static boolean updateHistory(JSONObject body) {
        try {
            String cityName = body.getString("city");
            String uid = body.getString("uid");
            LOGGER.log(Level.FINE, "User {0} wants to save itinerary from {1} city", new Object[]{uid, cityName});
            return postContentToURL(body, Constants.UPDATE_HISTORY_URL);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* postContentToURL - Create a post request given the body and the url
     *
     *  @return             : success or not
     *  @body               : the body of the HTTP POST request
     *  @strUrl             : the url for the HTTP POST request
     */
    private static boolean postContentToURL(JSONObject body, String strUrl) {
        try {
            StringEntity entity = new StringEntity(body.toString(2),
                                                   ContentType.APPLICATION_JSON);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(strUrl);
            request.setEntity(entity);

            HttpResponse response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            return responseCode == HttpStatus.OK.value();
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
            String operation = body.getString("operation");
            String cityName = body.getString("city");
            LOGGER.log(Level.FINE, "New request to update {0} database for {1} city", new Object[]{operation, cityName});
            return postContentToURL(body, Constants.UPDATE_PLANNER_URL);
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
        String url = Constants.GET_MATRIX_URL
                     + "?city=" + cityName
                     + "&travelMode=" + Enums.TravelMode.serialize(travelMode)
                     + "travelInfo=" + Enums.TravelInfo.serialize(travelInfo);

        try {
            JSONObject response = new JSONObject(getContentFromURL(url));
            int N = response.getInt("dimension");
            double[][] matrix = new double[N][N];
            JSONArray responseMatrix = response.getJSONArray("matrix");

            for (int i = 0; i < responseMatrix.length(); i++) {
                JSONObject edge = responseMatrix.getJSONObject(i);
                matrix[edge.getInt("from")][edge.getInt("to")] = edge.getDouble("value");
            }

            return matrix;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
