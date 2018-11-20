import org.apache.commons.io.IOUtils;
import org.json.*;

import java.io.*;
import java.net.*;
import java.util.*;

/* Reader - Fast reader class
 *
 */
class Reader {
    private static BufferedReader reader;
    private static StringTokenizer tokenizer;

    /* init - Initialize the Reader
     *
     *  @return             : void
     *  @input              : where to read (can be used also with System.in)
     */
    static void init(InputStream input) {
        reader = new BufferedReader(new InputStreamReader(input));
        tokenizer = new StringTokenizer("");
    }

    /* next - Returns the next word from file
     *
     *  @return             : next word
     */
    private static String next() throws IOException {
        while ( ! tokenizer.hasMoreTokens() ) {
            // check for eof
            String line = reader.readLine();
            if (line == null)
                return null;

            tokenizer = new StringTokenizer(line);
        }
        return tokenizer.nextToken();
    }

    /* readLine - Returns the whole line from a file
     *
     *  @return             : next word
     */
    static String readLine() throws IOException {
        return reader.readLine();
    }

    /* nextInt - Returns the next integer from file
     *
     *  @return             : next int
     */
    static Integer nextInt() throws IOException {
        String s = next();
        return s == null ? null : Integer.parseInt(s);
    }

    /* nextDouble - Returns the next double from file
     *
     *  @return             : next double
     */
    static Double nextDouble() throws IOException {
        String s = next();
        return s == null ? null : Double.parseDouble(s);
    }
}

/* Constants - The main constants used for this package
 *
 */
class Constants {
    final static String DATABASE_PATH = System.getProperty("user.dir") + "/../places_db/";
    final static String GOOGLE_API_PATH = System.getProperty("user.dir") + "/../apis/Google_API.txt";
    final static int RESTAURANTS_SEARCH_RADIUS = 500; // the value is in meters
}

/* ParserManager - Used to parse HTTP responses from different APIs
 *
 */
class ParserManager {
    /* getGoogleDistance - Parse the json data between two places and return the duration between 2 places
     *
     *  @return             : duration in seconds between two places
     */
    private static int getGoogleDistance(String jsonData) {
        int durationInSeconds = Integer.MAX_VALUE;
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            durationInSeconds = jsonObject.getJSONArray("rows")
                    .getJSONObject(0)
                    .getJSONArray("elements")
                    .getJSONObject(0)
                    .getJSONObject("duration").getInt("value");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return durationInSeconds;
    }

    /* getPlaces - Reads the json database with places from file
     *
     *  @return             : the json array with places
     *  @fileName           : the path for the file
     */
    private static JSONArray getPlaces(String fileName) {
        JSONArray jsonArray = null;
        try {
            InputStream is = new FileInputStream(fileName);
            String jsonText = IOUtils.toString(is, "UTF-8");

            jsonArray = new JSONArray(jsonText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    /* cacheDistances - Download, parse and cache the distance matrix between places
     *
     *  @return             : void
     *  @cityName           : the city to update distances
     *  @modeOfTravel       : what to update (driving / walking)
     *  @destinationFileName: where to save the results
     */
    static void cacheDistances(String cityName,
                               String modeOfTravel,
                               String destinationFileName) {
        String fileName = Constants.DATABASE_PATH + cityName + ".json";
        JSONArray places = getPlaces(fileName);

        try {
            FileWriter fw = new FileWriter(destinationFileName, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);

            for (int i = 0; i < places.length(); i++) {
                JSONObject place = places.getJSONObject(i);
                for (int j = 0; j < places.length(); j++) {
                    if (i == j)
                        continue;
                    JSONObject next = places.getJSONObject(j);
                    String origin = "" + place.getDouble("latitude") + "," + place.getDouble("longitude");
                    String destination = "" + next.getDouble("latitude") + "," + next.getDouble("longitude");
                    String url = URLManager.buildDistanceMatrixURL(origin, destination, modeOfTravel);

                    try {
                        String data = URLManager.getContentFromURL(url);
                        int timeDistance = getGoogleDistance(data);
                        // save timeDistance
                        String line = place.getInt("id") + " " + next.getInt("id") + " " + ((double) timeDistance);
                        // write to file
                        out.println(line);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
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
     *  @jsonData           : raw json data from Google
     */
    private static JSONObject getRestaurant(JSONObject jsonData) {
        JSONObject restaurant = null;
        try {
            restaurant = new JSONObject();
            addRestaurantInformation(restaurant, "name", jsonData);
            addRestaurantInformation(restaurant, "vicinity", jsonData);
            addRestaurantInformation(restaurant, "rating", jsonData);
            addRestaurantInformation(restaurant, "latitude", jsonData.getJSONObject("geometry")
                                                                  .getJSONObject("location")
                                                                  .getDouble("lat"));
            addRestaurantInformation(restaurant, "longitude", jsonData.getJSONObject("geometry")
                                                                   .getJSONObject("location")
                                                                   .getDouble("lng"));
            String placeId = jsonData.getString("place_id");

            try {
                String urlPlaceDetail = URLManager.buildPlaceDetailURL(placeId);
                String placeDetailJson = URLManager.getContentFromURL(urlPlaceDetail);
                addRestaurantInformation(restaurant, new JSONObject(placeDetailJson).getJSONObject("result"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return restaurant;
    }

    /* getRestaurants - Get a list of restaurants given the raw response from Google
     *
     *  @return             : the restaurants nearby a place
     *  @jsonData           : raw json data from Google
     */
    private static JSONArray getRestaurants(String jsonData) {
        JSONArray result = new JSONArray();
        JSONArray restaurants = new JSONObject(jsonData).getJSONArray("results");
        for (int i = 0; i < restaurants.length(); i++) {
            JSONObject restaurant = getRestaurant(restaurants.getJSONObject(i));
            result.put(restaurant);
        }
        return result;
    }

    /* cacheRestaurants - Download, parse and cache the restaurants
     *
     *  @return             : void
     *  @cityName           : the city where to retrieve information
     *  @destinationFileName: where to store the results
     */
    static void cacheRestaurants(String cityName, String destinationFileName) {
        String fileName = Constants.DATABASE_PATH + cityName + ".json";
        JSONArray places = getPlaces(fileName);

        try {
            JSONArray result = new JSONArray();
            for (int i = 0; i < places.length(); i++) {
                JSONObject place = places.getJSONObject(i);
                String url = URLManager.buildRestaurantsURL(String.valueOf(place.getDouble("latitude")),
                                                            String.valueOf(place.getDouble("longitude")),
                                                            Constants.RESTAURANTS_SEARCH_RADIUS);
                try {
                    String data = URLManager.getContentFromURL(url);
                    JSONArray restaurants = getRestaurants(data);
                    JSONObject placeInformation = new JSONObject();
                    placeInformation.put("id", place.getInt("id"));
                    placeInformation.put("name", place.getString("name"));
                    placeInformation.put("restaurants", restaurants);
                    result.put(placeInformation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(destinationFileName), 32768);
                out.write(result.toString(2));
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/* URLManager - Used to construct URLs and to download raw content
 *
 */
class URLManager {
    /* getGoogleApiKey - Reads the google api key from file
     *
     *  @return             : the key
     */
    private static String getGoogleApiKey() {
        String key = null;
        try {
            Reader.init(new FileInputStream(Constants.GOOGLE_API_PATH));
            key = Reader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return key;
    }

    /* getContentFromURL - Returns the content from a http get request
     *
     *  @return             : the content
     *  @strUrl             : the url with the get request
     */
    static String getContentFromURL(String strUrl) throws IOException {
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

    /* buildDistanceMatrixURL - Construct a url for downloading distance information between two places
     *
     *  @return             : the Google url
     *  @origin             : the origin parameter for the url with (latitude,longitude) format
     *  @destination        : the destination parameter for the url
     *  @modeOfTravel       : the mode of travel between places
     */
    static String buildDistanceMatrixURL(String origin,
                                         String destination,
                                         String modeOfTravel) {
        return "https://maps.googleapis.com/maps/api/distancematrix/json?"
               + "origins=" + origin
               + "&destinations=" + destination
               + "&mode=" + modeOfTravel
               + "&language=en-EN"
               + "&key=" + getGoogleApiKey();
    }

    /* buildRestaurantsURL - Construct a url for downloading restaurant information nearby a place, given the coordinates
     *
     *  @return             : the Google url
     *  @latitude           : the latitude of the place
     *  @longitude          : the longitude of the place
     *  @searchRadius       : the search radius for a restaurant nearby
     */
    static String buildRestaurantsURL(String latitude,
                                      String longitude,
                                      int searchRadius) {
        return "https://maps.googleapis.com/maps/api/place/nearbysearch/json?"
               + "location=" + latitude + "," + longitude
               + "&radius=" + searchRadius
               + "&type=restaurant"
               + "&sensor=true"
               + "&key=" + getGoogleApiKey();
    }

    /* buildPlaceDetailURL - Construct a url for downloading additional information for a place
     *
     *  @return             : the Google url
     *  @placeId            : the Google Place Id
     */
    static String buildPlaceDetailURL(String placeId) {
        return "https://maps.googleapis.com/maps/api/place/details/json?"
               + "placeid=" + placeId
               + "&key=" + getGoogleApiKey();
    }
}

/* Action - Interface to implement for future commands
 *
 */
interface Action {
    void execute();
}

/* UpdateDistanceAction - Update the distance matrix for a city given the mode of travel
 *
 */
class UpdateDistanceAction implements Action {
    private String cityName;
    private String modeOfTravel;
    private String destinationFileName;

    // constructor
    UpdateDistanceAction(String[] args) {
        String city = args[1];
        String mode = args[2];
        String destination = Constants.DATABASE_PATH + city + "_" + mode + ".txt";
        configure(city, mode, destination);
    }

    /* configure - Set the class variables
     *
     *  @return             : void
     *  @cityName           : the city name
     *  @modeOfTravel       : the mode of travel between places
     *  @destinationFileName: where to save the results
     */
    private void configure(String cityName, String modeOfTravel, String destinationFileName) {
        this.cityName = cityName;
        this.modeOfTravel = modeOfTravel;
        this.destinationFileName = destinationFileName;
    }

    /* execute - Cache the distances in file
     *
     *  @return             : void
     */
    public void execute() {
        try {
            System.out.println("Started downloading distance information for (" + cityName + ", " + modeOfTravel + ")");
            ParserManager.cacheDistances(cityName, modeOfTravel, destinationFileName);
            System.out.println("Finished successfully downloading information");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/* UpdateRestaurantsAction - Update the restaurants from a city
 *
 */
class UpdateRestaurantsAction implements Action {
    private String cityName;
    private String destinationFileName;

    // constructor
    UpdateRestaurantsAction(String[] args) {
        this.cityName = args[1];
        this.destinationFileName = Constants.DATABASE_PATH + cityName + "_restaurants.json";
    }

    /* execute - Cache the restaurants in file
     *
     *  @return             : void
     */
    public void execute() {
        try {
            System.out.println("Started downloading restaurants information for (" + cityName + ")");
            ParserManager.cacheRestaurants(cityName, destinationFileName);
            System.out.println("Finished successfully downloading information");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/* ActionFactory - Creates a new instance given the command line arguments
 *
 */
class ActionFactory {
    static Action getAction(String[] args) {
        if (args == null || args.length == 0)
            return null;

        if (args[0].equals("updateDistance")) {
            return new UpdateDistanceAction(args);
        } else if (args[0].equals("updateRestaurants")){
            return new UpdateRestaurantsAction(args);
        }
        return null;
    }
}

public class UpdateManager {
    public static void main(String[] args) {
        Action action = ActionFactory.getAction(args);
        action.execute();
    }
}