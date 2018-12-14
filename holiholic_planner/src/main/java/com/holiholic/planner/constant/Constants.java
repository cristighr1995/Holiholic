package com.holiholic.planner.constant;

import com.holiholic.planner.utils.Interval;

import java.util.Calendar;

/* Constants - All constants will be declared here
 *
 */
public class Constants {
    // Paths
    public final static String GOOGLE_API_PATH = System.getProperty("user.dir") + "/apis/Google_API.txt";
    public final static String UPDATE_ACCESS_KEY_PATH = System.getProperty("user.dir") + "/apis/admin_key.txt";

    // Urls
    public final static String CONTAINS_USER_URL = "http://localhost:8092/containsUser";
    public final static String UPDATE_HISTORY_URL = "http://localhost:8092/updateHistory";
    public final static String GET_PLACES_URL = "http://localhost:8092/getPlaces";
    public final static String GET_MATRIX_URL = "http://localhost:8092/getMatrix";
    public final static String UPDATE_PLANNER_URL = "http://localhost:8092/updatePlanner";

    public final static Calendar DEFAULT_LUNCH_HOUR = Interval.getHour(13, 0, 0);
    public final static Calendar DEFAULT_DINNER_HOUR = Interval.getHour(19, 0, 0);
    // Consider a fixed time to be planned successfully if the planner hour is between interval [time-range, time+range]
    public final static int FIXED_RANGE_ACCEPTANCE = 30;
    public final static int FIXED_ATTRACTION_REWARD = 100;
    public final static int FIXED_RESTAURANT_REWARD = 200;
    public final static double DRIVING_ADJUST_COEFFICIENT = 1.3;    // used to adjust the geometrical duration
    public final static double WALKING_ADJUST_COEFFICIENT = 2.0;
    public final static double ESTIMATED_DRIVING_VELOCITY = 20;     // in kilometers / hour
    public final static double ESTIMATED_WALKING_VELOCITY = 6;
    public final static int RESTAURANTS_SEARCH_RADIUS = 500;        // in meters
}
