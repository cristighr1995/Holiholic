package com.holiholic.planner.database;

import com.holiholic.planner.planner.PlanManager;
import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.travel.City;
import com.holiholic.planner.utils.*;
import com.holiholic.planner.utils.Reader;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
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
    private static void sortPlacesBasedOnPopularity(List<JSONObject> places) {
        places.sort((o1, o2) -> {
            int checkIns1 = o1.getInt("checkIns");
            int checkIns2 = o2.getInt("checkIns");

            if (checkIns1 != checkIns2) {
                // descending
                return checkIns2 - checkIns1;
            }

            double rating1 = o1.getDouble("rating");
            double rating2 = o2.getDouble("rating");
            return Double.compare(rating2, rating1);
        });
    }

    /* getPlacesBasedOnTags - Scan places from the corresponding city and get the places with the desired tags
     *
     *  @return             : a json array string with places
     *  @jsonPlacesArray    : the list of places from the city
     *  @tags               : the tags for the places the user wants to visit
     */
    private static String getPlacesBasedOnTags(JSONArray jsonPlacesArray, Set<String> tags) {
        JSONArray response = new JSONArray();
        List<JSONObject> places = new ArrayList<>();

        try {
            for (int i = 0; i < jsonPlacesArray.length(); i++) {
                JSONObject jsonPlace = jsonPlacesArray.getJSONObject(i);
                JSONArray jsonPlaceTags = jsonPlace.getJSONArray("tags");

                for (int j = 0; j < jsonPlaceTags.length(); j++) {
                    String tag = jsonPlaceTags.getString(j);

                    if (tags.contains(tag)) {
                        places.add(jsonPlace);
                        break;
                    }
                }
            }
            // sort the places based on their rating or popularity
            sortPlacesBasedOnPopularity(places);
            // put the sorted places in the response
            response.put(places);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response.toString(2);
    }

    /* getPlaces - Reads the database and collects all the places from a specific city
    *              This function sorts the places before sending them to user
    *
    *  @return       : a json array with places
    *  @request      : the json containing user's information
    */
    public static String getPlacesHttpRequest(String request) {
        String response = null;

        try {
            LOGGER.log(Level.FINE, "New request to process places recommendations");

            JSONObject jsonRequest = new JSONObject(request);
            String cityName = jsonRequest.getString("city");

            // decode the city and cache it, to avoid duplicate work
            City city = getCity(cityName);

            assert (city.jsonPlacesArray != null);

            Set<String> tags = new HashSet<>();
            JSONArray jsonTags = jsonRequest.getJSONArray("tags");
            for (int i = 0; i < jsonTags.length(); i++) {
                tags.add(jsonTags.getString(i));
            }

            response = getPlacesBasedOnTags(city.jsonPlacesArray, tags);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    /* loadRestaurantsFromFile - Loads nearby restaurants for a specific city
     *
     *  @return       : a map where for each place we store the nearby restaurants
     *  @fileName     : the corresponding file name where we already cache in binary format the map
     */
    private static Map<Integer, List<Place>> loadRestaurantsFromFile(String fileName) {
        Map<Integer, List<Place>> restaurants = null;

        try {
            LOGGER.log(Level.FINE, "Started loading restaurants from file - {0} -", fileName);

            restaurants = new HashMap<>();
            InputStream is = new FileInputStream(fileName);
            String jsonText = IOUtils.toString(is, "UTF-8");
            JSONArray jsonRestaurants = new JSONArray(jsonText);

            for (int i = 0; i < jsonRestaurants.length(); i++) {
                JSONObject jsonPlaceInformation = jsonRestaurants.getJSONObject(i);
                JSONArray jsonPlaceRestaurants = jsonPlaceInformation.getJSONArray("restaurants");
                List<Place> placeRestaurants = new ArrayList<>();

                for (int j = 0; j < jsonPlaceRestaurants.length(); j++) {
                    JSONObject jsonRestaurant = jsonPlaceRestaurants.getJSONObject(j);
                    Place restaurant = deserializeRestaurant(jsonRestaurant);
                    placeRestaurants.add(restaurant);
                }

                restaurants.put(jsonPlaceInformation.getInt("id"), placeRestaurants);
            }

            LOGGER.log(Level.FINE, "Successfully retrieved restaurants from file - {0} -", fileName);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return restaurants;
    }

    /* loadCityFromFile - Loads the city from a raw json
     *
     *  @return       : void
     *  @city         : the city instance that we want to build
     *  @fileName     : the corresponding file name for the raw json where we find information about places from city
     */
    private static void loadCityFromFile(City city, String fileName) {
        try {
            LOGGER.log(Level.FINE, "Start loading city - {0} - from file {1}", new Object[]{city.name, fileName});
            List<Place> places = new ArrayList<>();

            InputStream is = new FileInputStream(fileName);
            String jsonText = IOUtils.toString(is, "UTF-8");

            JSONArray jsonArray = new JSONArray(jsonText);
            // save also this array for future manipulation
            city.jsonPlacesArray = jsonArray;
            int jsonArrayLength = jsonArray.length();

            for (int i = 0; i < jsonArrayLength; i++) {
                JSONObject jsonPlace = jsonArray.getJSONObject(i);

                int id = jsonPlace.getInt("id");
                String name = jsonPlace.getString("name");
                double latitude = jsonPlace.getDouble("latitude");
                double longitude = jsonPlace.getDouble("longitude");
                int durationVisit = jsonPlace.getInt("duration");
                double rating = jsonPlace.getDouble("rating");
                int checkIns = jsonPlace.getInt("checkIns");
                String imageUrl = jsonPlace.getString("imageUrl");
                int wantToGoNumber = jsonPlace.getInt("wantToGo");
                JSONArray jsonTagsArray = jsonPlace.getJSONArray("tags");
                int parkTime = jsonPlace.getInt("parkTime");
                OpeningPeriod openingPeriod = deserializeOpeningPeriod(jsonPlace.getJSONArray("openingHours"));

                Place place = new Place(id, name, new GeoPosition(latitude, longitude),
                                        durationVisit, rating, openingPeriod);
                place.checkIns = checkIns;
                place.imageUrl = imageUrl;
                place.wantToGoNumber = wantToGoNumber;
                place.parkTime = parkTime;

                for (int t = 0; t < jsonTagsArray.length(); t++) {
                    place.tags.add(jsonTagsArray.getString(t));
                }

                places.add(place);
            }

            LOGGER.log(Level.FINE, "Successfully retrieved places for city - {0} -", city.name);

            String restaurantsFileName = Constants.DATABASE_PATH + city.name + "_restaurants.json";
            Map<Integer, List<Place>> restaurants = loadRestaurantsFromFile(restaurantsFileName);

            city.constructInstance(places, restaurants);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
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

        File f;
        City city = null;
        String fileName = Constants.DATABASE_PATH + cityName + ".json";

        try {
            f = new File(fileName);
            city = City.getInstance(cityName);

            if (f.getAbsoluteFile().exists()) {
                loadCityFromFile(city, fileName);
                // store the city in the cache
                cacheCity.put(cityName, city);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return city;
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
            LOGGER.log( Level.FINE, "Started updating the city ({0}) database", city.name);
            String fileName = Constants.DATABASE_PATH + city.name + ".json";
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName), 32768);
            out.write(city.jsonPlacesArray.toString(2));
            out.close();
            LOGGER.log( Level.FINE, "Successfully updated the city ({0}) database", city.name);
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
                jsonItinerary.put(place.serialize());
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
}
