package com.holiholic.planner.database;

import com.holiholic.planner.planner.PlanManager;
import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.travel.City;
import com.holiholic.planner.update.action.Action;
import com.holiholic.planner.update.action.ActionFactory;
import com.holiholic.planner.utils.*;
import com.holiholic.planner.utils.Reader;
import org.apache.commons.io.IOUtils;
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

    // the cache that contains the in-memory cities
    private final static Map<String, City> cacheCity = new HashMap<>();
    // the cache that contains the in-memory weather information about cities
    private final static Map<String, WeatherForecastInformation> cacheWeather = new HashMap<>();

    // for each city keep the distance matrix and traffic coefficients
    private final static Map<String, double[][]> cacheDistanceMatrixDriving = new HashMap<>();
    private final static Map<String, double[][]> cacheDistanceMatrixWalking = new HashMap<>();
    private final static Map<String, Map<Integer, Double>> cacheTrafficCoefficients = new HashMap<>();

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

    /* sortPlacesBasedOnPopularity - Given places list, sort based on popularity to send back the response to user
     *
     *  @return             : void
     *  @places             : a list of places in JSONObject format
     */
    private static void sortPlacesBasedOnPopularity(List<Place> places) {
        places.sort((o1, o2) -> {
            if (o1.checkIns != o2.checkIns) {
                // descending
                return Integer.compare(o2.checkIns, o1.checkIns);
            }
            return Double.compare(o2.rating, o1.rating);
        });
    }

    /* filterPlacesByTags - Scan places from the corresponding city and get the places with the desired tags
     *
     *  @return             : the filtered places
     *  @jsonPlacesArray    : the list of places from the city
     *  @tags               : the tags for the places the user wants to visit
     */
    private static List<Place> filterPlacesByTags(List<Place> places, JSONArray tags) {
        List<Place> filteredPlaces = new ArrayList<>();
        Set<String> tagSet = new HashSet<>();

        for (int i = 0; i < tags.length(); i++) {
            tagSet.add(tags.getString(i));
        }

        for (Place place : places) {
            for (String tag : place.tags) {
                if (tagSet.contains(tag)) {
                    filteredPlaces.add(place);
                    break;
                }
            }
        }

        sortPlacesBasedOnPopularity(filteredPlaces);
        return filteredPlaces;
    }

    /* filterPlaceByVisitingHours - Scan places and select only those that can be visited in the period the user selected
     *                              For example, an user can select multiple days
     *
     *  @return             : the filtered places
     *  @places             : the list of places from the city
     *  @visitingInterval   : the interval selected by user (multiple days with different intervals per day)
     */
    private static List<Place> filterPlaceByVisitingHours(List<Place> places, OpeningPeriod visitingInterval) {
        List<Place> filteredPlaces = new ArrayList<>();
        for (Place place : places) {
            if (place.canVisit(visitingInterval)) {
                filteredPlaces.add(place);
            }
        }
        return filteredPlaces;
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

    /* getPlaces - Reads the database and collects all the places from a specific city
    *              This function sorts the places before sending them to user
    *
    *  @return          : a json array with places
    *  @body            : the json containing user's information
    */
    public static String getPlaces(JSONObject body) {
        try {
            String cityName = body.getString("city");
            String uid = body.getString("uid");

            LOGGER.log(Level.FINE, "New request from user {0} to get places recommendation for {1} city",
                       new Object[]{uid, cityName});

            if (!containsUser(uid)) {
                return "[]";
            }

            // decode the city and cache it, to avoid duplicate work
            City city = getCity(cityName);

            List<Place> placesFilteredByTags = filterPlacesByTags(city.places, body.getJSONArray("tags"));
            OpeningPeriod visitingInterval = deserializeOpeningPeriod(body.getJSONArray("visitingInterval"));
            List<Place> openPlaces = filterPlaceByVisitingHours(placesFilteredByTags, visitingInterval);

            return serializeToNetwork(openPlaces);
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    /* serializeToNetwork - Serialize the places to be sent over network
     *
     *  @return             : the json array string with the places information
     *  @places             : the list of places
     */
    private static String serializeToNetwork(List<Place> places) {
        JSONArray result = new JSONArray();
        for (Place place : places) {
            result.put(place.serializeToNetwork());
        }
        return result.toString(2);
    }

    /* fetchArrayFromDatabase - Retrieve from database a json array
     *
     *  @return             : the json array from the database or null
     *  @path               : the path for the database
     */
    public static JSONArray fetchArrayFromDatabase(String path) {
        try {
            InputStream is = new FileInputStream(path);
            String text = IOUtils.toString(is, "UTF-8");
            return new JSONArray(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* decodeRestaurants - Decode restaurants from json format to internal representation
     *
     *  @return         : a map where for each place we store the nearby restaurants
     *  @dbRestaurants  : the format from the database
     */
    public static Map<Integer, List<Place>> decodeRestaurants(JSONArray dbRestaurants) {
        try {
            Map<Integer, List<Place>> restaurants = new HashMap<>();

            for (int i = 0; i < dbRestaurants.length(); i++) {
                JSONObject placeInformation = dbRestaurants.getJSONObject(i);
                JSONArray nearbyRestaurants = placeInformation.getJSONArray("restaurants");
                List<Place> placeRestaurants = new ArrayList<>();

                for (int j = 0; j < nearbyRestaurants.length(); j++) {
                    JSONObject dbRestaurant = nearbyRestaurants.getJSONObject(j);
                    Place restaurant = deserializeRestaurant(dbRestaurant);
                    placeRestaurants.add(restaurant);
                }

                restaurants.put(placeInformation.getInt("id"), placeRestaurants);
            }
            return restaurants;
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* fetchRestaurantsFromDatabase - Loads nearby restaurants for a specific city
     *
     *  @return         : a map where for each place we store the nearby restaurants
     *  @path           : the corresponding file name where we already cache in binary format the map
     */
    private static Map<Integer, List<Place>> fetchRestaurantsFromDatabase(String path) {
        try {
            LOGGER.log(Level.FINE, "Load restaurants from database path: {0}", path);
            return decodeRestaurants(fetchArrayFromDatabase(path));
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* decodePlace - Create Place instance from the database representation
     *
     *  @return             : place instance
     *  @dbPlace            : the database place representation
     */
    private static Place decodePlace(JSONObject dbPlace) {
        try {
            int id = dbPlace.getInt("id");
            String name = dbPlace.getString("name");
            double latitude = dbPlace.getDouble("latitude");
            double longitude = dbPlace.getDouble("longitude");
            int durationVisit = dbPlace.getInt("duration");
            double rating = dbPlace.getDouble("rating");
            int checkIns = dbPlace.getInt("checkIns");
            String imageUrl = dbPlace.getString("imageUrl");
            int wantToGoNumber = dbPlace.getInt("wantToGo");
            JSONArray jsonTagsArray = dbPlace.getJSONArray("tags");
            int parkTime = dbPlace.getInt("parkTime");
            OpeningPeriod openingPeriod = deserializeOpeningPeriod(dbPlace.getJSONArray("openingHours"));

            Place place = new Place(id, name, new GeoPosition(latitude, longitude),
                                    durationVisit, rating, openingPeriod);

            place.checkIns = checkIns;
            place.imageUrl = imageUrl;
            place.wantToGoNumber = wantToGoNumber;
            place.parkTime = parkTime;

            for (int t = 0; t < jsonTagsArray.length(); t++) {
                place.tags.add(jsonTagsArray.getString(t));
            }

            return place;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* fetchCityFromDatabase - Loads the city from a raw json
     *
     *  @return         : void
     *  @city           : the city instance that we want to build
     *  @path           : the corresponding file name for the raw json where we find information about places from city
     */
    private static void fetchCityFromDatabase(City city, String path) {
        try {
            LOGGER.log(Level.FINE, "Load {0} city from database path: {1}", new Object[]{city.name, path});

            JSONArray dbPlaces = fetchArrayFromDatabase(path);
            List<Place> places = new ArrayList<>();

            for (int i = 0; i < dbPlaces.length(); i++) {
                Place place = decodePlace(dbPlaces.getJSONObject(i));
                if (place != null) {
                    places.add(place);
                }
            }

            String restaurantsPath = Constants.DATABASE_PATH + city.name + "_restaurants.json";
            Map<Integer, List<Place>> restaurants = fetchRestaurantsFromDatabase(restaurantsPath);

            city.constructInstance(places, restaurants);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* getCity - Returns the instance of the city and constructs it only the first time
     *
     *  @return       : a city instance
     *  @cityName     : the city where the user wants to go/to visit
     */
    public static City getCity(String cityName) {
        if (cacheCity.containsKey(cityName)) {
            return cacheCity.get(cityName);
        }

        String fileName = Constants.DATABASE_PATH + cityName + ".json";
        City city = null;
        try {
            File f = new File(fileName);
            city = City.getInstance(cityName);

            if (f.getAbsoluteFile().exists()) {
                fetchCityFromDatabase(city, fileName);
                // store the city in the cache
                cacheCity.put(cityName, city);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return city;
    }

    /* isCityCached - Checks if the city instance is cached
     *
     *  @return       : true or false
     *  @cityName     : the city where the user wants to go/to visit
     */
    public static boolean isCityCached(String cityName) {
        return cacheCity.containsKey(cityName);
    }

    /* getWeatherForecastInformation - Returns the instance of the weather forecast
     *
     *  @return       : a weather instance
     *  @cityName     : the city where the user wants to go/to visit
     */
    public static WeatherForecastInformation getWeatherForecastInformation(String cityName) {
        if (cacheWeather.containsKey(cityName)) {
            return cacheWeather.get(cityName);
        }

        WeatherForecastInformation weatherResult = null;

        try {
            String fileName = Constants.DATABASE_PATH + cityName + "_weather.txt";
            Reader.init(new FileInputStream(fileName));

            double temperature = Reader.nextDouble();
            double rainProbability = Reader.nextDouble();
            double snowProbability = Reader.nextDouble();

            weatherResult = new WeatherForecastInformation(temperature, rainProbability, snowProbability);
            cacheWeather.put(cityName, weatherResult);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return weatherResult;
    }

    /* initDistanceMatrix - Reads the distance matrix from file and cache it
     *
     *  @return       : void
     *  @cityName     : the city where the user wants to go/to visit
     *  @modeOfTravel : how the user want to go (driving / walking)
     */
    private static void initDistanceMatrix(String cityName, Enums.TravelMode modeOfTravel) {
        try {
            // example -> bucharest_driving.txt
            String fileName = Constants.DATABASE_PATH
                              + cityName + "_"
                              + Enums.TravelMode.serialize(modeOfTravel) + ".txt";
            Reader.init(new FileInputStream(fileName));
            assert (cacheCity.containsKey(cityName));

            int N = getCity(cityName).places.size();
            double[][] distanceMatrix = new double[N][N];

            while (true) {
                Integer i = Reader.nextInt();
                if (i == null) {
                    break;
                }
                Integer j = Reader.nextInt();
                Double d = Reader.nextDouble();
                distanceMatrix[i][j] = d;
            }

            if (modeOfTravel == Enums.TravelMode.DRIVING) {
                cacheDistanceMatrixDriving.put(cityName, distanceMatrix);
            } else {
                cacheDistanceMatrixWalking.put(cityName, distanceMatrix);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* getDistanceMatrix - Returns the distance matrix from the city given the mode of travel
     *
     *  @return       : distance matrix
     *  @cityName     : the city where the user wants to go/to visit
     *  @modeOfTravel : how the user want to go (driving / walking)
     */
    public static double[][] getDistanceMatrix(String cityName, Enums.TravelMode modeOfTravel) {
        double[][] distanceMatrix = null;

        try {
            if (modeOfTravel == Enums.TravelMode.DRIVING) {
                if (!cacheDistanceMatrixDriving.containsKey(cityName)) {
                    initDistanceMatrix(cityName, modeOfTravel);
                }
                distanceMatrix = cacheDistanceMatrixDriving.get(cityName);
            } else {
                if (!cacheDistanceMatrixWalking.containsKey(cityName)) {
                    initDistanceMatrix(cityName, modeOfTravel);
                }
                distanceMatrix = cacheDistanceMatrixWalking.get(cityName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return distanceMatrix;
    }

    /* initTrafficCoefficients - Reads the traffic coefficients from file and cache them
     *
     *  @return       : void
     *  @cityName     : the city where the user wants to go/to visit
     */
    private static void initTrafficCoefficients(String cityName) {
        Map<Integer, Double> trafficCoefficientsMappings = new HashMap<>();

        try {
            String fileName = Constants.DATABASE_PATH + cityName + "_traffic.txt";
            Reader.init(new FileInputStream(fileName));

            for (int i = 0; i < 24; i++) {
                int hour = Reader.nextInt();
                double coefficient = Reader.nextDouble();
                trafficCoefficientsMappings.put(hour, coefficient);
            }

            // cache the coefficients after successfully read them
            cacheTrafficCoefficients.put(cityName, trafficCoefficientsMappings);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* getTrafficCoefficients - Return the traffic coefficients from a city
     *
     *  @return       : a map where for each hour we have a coefficient
     *  @cityName     : the city where the user wants to go/to visit
     */
    public static Map<Integer, Double> getTrafficCoefficients(String cityName) {
        if (!cacheTrafficCoefficients.containsKey(cityName)) {
            initTrafficCoefficients(cityName);
        }
        return cacheTrafficCoefficients.get(cityName);
    }

    /* updateCity - Write to file the places json array from the city
     *
     *  @return       : void
     *  @city         : the city where the user wants to go/to visit
     */
    public static synchronized void updateCity(City city) {
        try {
            LOGGER.log( Level.FINE, "Update {0} city database", city.name);
            String path = Constants.DATABASE_PATH + city.name + ".json";
            BufferedWriter out = new BufferedWriter(new FileWriter(path), 32768);
            out.write(serializeToNetwork(city.places));
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* updateCity - Serialize a list of itineraries into a json format
     *
     *  @return       : the json string which will be sent to user
     *  @plan         : the final plan
     */
    public static String serializePlan(List<List<Place>> plan) {
        JSONArray response = new JSONArray();
        for (List<Place> itinerary : plan) {
            JSONArray jsonItinerary = new JSONArray();

            for (Place place : itinerary) {
                jsonItinerary.put(place.serializeToPlan());
            }

            response.put(jsonItinerary);
        }

        return response.toString(2);
    }

    /* deserializeHour - Creates a Calendar instance from a coded hour
     *                   Example 0930 means the time 09:30
     *
     *  @return             : the calendar instance of the given hour
     *  @hour               : the serialized hour
     */
    private static Calendar deserializeHour(String hour) {
        int h = Integer.parseInt(hour.substring(0, 2));
        int m = Integer.parseInt(hour.substring(2));
        return Interval.getHour(h, m, 0);
    }

    /* deserializeOpeningPeriod - Creates an OpeningPeriod instance from a json period
     *
     *  @return             : the OpeningPeriod instance of the given json
     *  @jsonOpeningPeriod  : the json from file which contains information about the opening hours for a place
     */
    public static OpeningPeriod deserializeOpeningPeriod(JSONArray jsonOpeningPeriod) {
        // check if the place is non stop
        if (jsonOpeningPeriod.getJSONObject(0).getJSONObject("open").getString("time").equals("0000")) {
            return new OpeningPeriod();
        }

        Set<Integer> closed = new HashSet<>();
        Map<Integer, Interval> intervals = new HashMap<>();
        for (int day = 0; day < 7; day++) {
            closed.add(day);
        }
        for (int i = 0; i < jsonOpeningPeriod.length(); i++) {
            JSONObject period = jsonOpeningPeriod.getJSONObject(i);
            JSONObject open = period.getJSONObject("open");
            JSONObject close = period.getJSONObject("close");
            Calendar start = deserializeHour(open.getString("time"));
            Calendar end = deserializeHour(close.getString("time"));
            int dayOpen = open.getInt("day");
            int dayClose = close.getInt("day");
            // this means the bar is closing after midnight
            if (dayClose != dayOpen) {
                end.add(Calendar.DAY_OF_WEEK, 1);
            }
            intervals.put(dayOpen, new Interval(start, end));
            // erase from closed days
            closed.remove(dayOpen);
        }
        for (int closeDay : closed) {
            Interval closeInterval = new Interval();
            closeInterval.setClosed(true);
            intervals.put(closeDay, closeInterval);
        }

        return new OpeningPeriod(intervals);
    }

    /* deserializeRestaurant - Converts a restaurant into a place
     *
     *  @return             : the place for the restaurant
     *  @restaurantJson     : the restaurant json
     */
    private static Place deserializeRestaurant(JSONObject restaurantJson) {
        Place restaurant = new Place();
        restaurant.type = "restaurant";
        restaurant.name = restaurantJson.getString("name");
        if (restaurantJson.has("rating")) {
            restaurant.rating = restaurantJson.getDouble("rating");
        }
        restaurant.location = new GeoPosition(restaurantJson.getDouble("latitude"),
                                              restaurantJson.getDouble("longitude"));
        if (restaurantJson.has("vicinity")) {
            restaurant.vicinity = restaurantJson.getString("vicinity");
        }
        if (restaurantJson.has("openingHours")) {
            restaurant.openingPeriod = deserializeOpeningPeriod(restaurantJson.getJSONArray("openingHours"));
        } else {
            OpeningPeriod closePeriod = new OpeningPeriod();
            closePeriod.setClosedAllDays();
            restaurant.openingPeriod = closePeriod;
        }
        return restaurant;
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

            LOGGER.log(Level.FINE, "New request from user {0} to save itinerary from {1} city",
                       new Object[]{uid, cityName});
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
                return false;
            }

            Action action = ActionFactory.getAction(body);
            return action.execute();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* syncDatabase - Save in the database the updated json object
     *
     *  @return             : success or not
     *  @path               : the path for the database
     *  @jsonObject         : the object we want to save in the database
     */
    public static boolean syncDatabase(String path, JSONObject jsonObject) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path), 32768);
            out.write(jsonObject.toString(2));
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* syncDatabase - Save in the database the updated json array
     *
     *  @return             : success or not
     *  @path               : the path for the database
     *  @jsonArray          : the array we want to save in the database
     */
    public static boolean syncDatabase(String path, JSONArray jsonArray) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path), 32768);
            out.write(jsonArray.toString(2));
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* updateCacheDistance - Update the cache for distance matrix
     *
     *  @return             : success or not
     *  @cityName           : the current city name
     *  @modeOfTravel       : driving or walking
     *  @distanceMatrix     : the updated distance matrix
     */
    public static boolean updateCacheDistance(String cityName, String modeOfTravel, double[][] distanceMatrix) {
        if (!isCityCached(cityName)) {
            return true;
        }

        try {
            switch (modeOfTravel) {
                case "driving":
                    cacheDistanceMatrixDriving.put(cityName, distanceMatrix);
                    return true;
                case "walking":
                    cacheDistanceMatrixWalking.put(cityName, distanceMatrix);
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
