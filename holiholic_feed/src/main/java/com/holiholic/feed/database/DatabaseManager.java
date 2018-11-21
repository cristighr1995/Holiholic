package com.holiholic.feed.database;

import com.holiholic.feed.constant.Constants;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    /* updateQuestion - Updates the content of a question
     *
     *  @return             : success or not
     *  @questionBody       : the question body
     */
    public static boolean updateQuestion(JSONObject questionBody) {
        try {
            StringEntity entity = new StringEntity(questionBody.toString(2),
                                                   ContentType.APPLICATION_JSON);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(Constants.UPDATE_QUESTION_USER_URL);
            request.setEntity(entity);

            HttpResponse response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            return responseCode == HttpStatus.OK.value();
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

    /* getQuestions - Returns a list of questions for a specific city
     *
     *  @return             : the list of questions
     *  @city               : the city where the user wants to see questions
     *  @md5Key             : unique identifier for the current user
     */
    public static String getQuestions(String city, String md5Key) {
        String url = Constants.GET_QUESTION_USER_URL
                     + "city=" + city
                     + "&md5Key=" + md5Key;
        try {
            return getContentFromURL(url);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray().toString(2);
        }
    }

    /* getQuestionDetails - Returns the details for a specific question
     *
     *  @return             : the details for the specific question
     *  @city               : the city where the user wants to see questions
     *  @qid                : the question id
     *  @md5Key             : unique identifier for the current user
     */
    public static String getQuestionDetails(String city, String qid, String md5Key) {
        String url = Constants.GET_QUESTION_DETAILS_USER_URL
                + "city=" + city
                + "&qid=" + qid
                + "&md5Key=" + md5Key;
        try {
            return getContentFromURL(url);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray().toString(2);
        }
    }
}