package com.holiholic.planner.constant;

/* Constants - All constants will be declared here
 *
 */
public class Constants {
    // Paths
    public final static String UPDATE_ACCESS_KEY_PATH = System.getProperty("user.dir") + "/apis/admin_key.txt";

    // Table names
    private final static String DATABASE_INSTANCE_NAME = "holiholicdb";
    public final static String PLACES_TABLE_NAME = DATABASE_INSTANCE_NAME + ".Places";
    public final static String PLACES_CATEGORIES_TABLE_NAME = DATABASE_INSTANCE_NAME + ".PlacesCategories";
    public final static String PLACES_DISTANCES_TABLE_NAME = DATABASE_INSTANCE_NAME + ".PlacesDistances";

    public final static String DEFAULT_LUNCH_HOUR = "1300";
    public final static String DEFAULT_DINNER_HOUR = "1900";
    // Consider a fixed time to be planned successfully if the planner hour is between interval [time-range, time+range]
    public final static int FIXED_RANGE_ACCEPTANCE = 30;
    public final static int FIXED_ATTRACTION_REWARD = 100;
    public final static int FIXED_RESTAURANT_REWARD = 200;
    public final static double DRIVING_ADJUST_COEFFICIENT = 1.3;    // used to adjust the geometrical duration
    public final static double WALKING_ADJUST_COEFFICIENT = 2.0;
    public final static double ESTIMATED_DRIVING_VELOCITY = 20;     // in kilometers / hour
    public final static double ESTIMATED_WALKING_VELOCITY = 6;
}
