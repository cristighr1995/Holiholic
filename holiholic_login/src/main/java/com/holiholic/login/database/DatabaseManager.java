package com.holiholic.login.database;

import com.holiholic.login.constant.Constants;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* DatabaseManager - Handle the requests to the database and (pre)process the queries
 *
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

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
    }

    /* generateMD5 - Generates an md5 key for a plain text
     *
     *  @return             : the md5 key
     *  @plain              : the plain text we want to hash
     */
    public static String generateMD5(String plain) {
        String hash = null;

        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(plain.getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            hash = bigInt.toString(16);

            // pad with zeros to have full 32 characters
            if (hash.length() != 32) {
                int diff = 32 - hash.length();
                StringBuilder sb = new StringBuilder();
                for (int d = 0; d < diff; d++) {
                    sb.append("0");
                }

                hash = sb.append(hash).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return hash;
    }

    /* registerUser - Save a new user in the database using HTTP POST Request
     *
     *  @return             : true/false (success or not)
     *  @jsonProfile        : the current profile (json format)
     */
    public static boolean registerUser(JSONObject jsonProfile) {
        try {
            StringEntity entity = new StringEntity(jsonProfile.toString(2),
                                                   ContentType.APPLICATION_JSON);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(Constants.REGISTER_USER_URL);
            request.setEntity(entity);

            HttpResponse response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            return responseCode == HttpStatus.OK.value();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /* containsUser - Check if the current user is already in the database
     *
     *  @return             : true/false (existing or not)
     *  @uid                : the unique identifier for the current user
     */
    public static boolean containsUser(String uid) {
        String url = Constants.CONTAINS_USER_URL + uid;
        try {
            return Boolean.parseBoolean(getContentFromURL(url));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
}
