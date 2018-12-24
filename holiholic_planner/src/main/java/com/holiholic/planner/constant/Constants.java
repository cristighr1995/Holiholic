package com.holiholic.planner.constant;

import com.holiholic.planner.utils.Interval;

import java.util.Calendar;

/* Constants - All constants will be declared here
 *
 */
public class Constants {
    // Paths
    public final static String UPDATE_ACCESS_KEY_PATH = System.getProperty("user.dir") + "/apis/admin_key.txt";

    // Urls
    private final static String DATABASE_URL = "http://localhost:8092/";
    public final static String CONTAINS_USER_URL = DATABASE_URL + "containsUser";
    public final static String UPDATE_HISTORY_URL = DATABASE_URL + "updateHistory";
    public final static String GET_PLACES_URL = DATABASE_URL + "getPlaces";
    public final static String GET_MATRIX_URL = DATABASE_URL + "getMatrix";

    // Table names
    private final static String DATABASE_INSTANCE_NAME = "holiholicdb";
    public final static String PLACES_TABLE_NAME = DATABASE_INSTANCE_NAME + ".Places";
    public final static String PLACES_CATEGORIES_TABLE_NAME = DATABASE_INSTANCE_NAME + ".PlacesCategories";

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
}
