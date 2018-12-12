package com.holiholic.planner.update.parser;

import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.utils.Reader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

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
     *  @travelMode         : the mode of travel between places
     */
    static String buildDistanceMatrixURL(String origin,
                                         String destination,
                                         String travelMode) {
        return "https://maps.googleapis.com/maps/api/distancematrix/json?"
                + "origins=" + origin
                + "&destinations=" + destination
                + "&mode=" + travelMode
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
