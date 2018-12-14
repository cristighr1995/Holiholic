package com.holiholic.planner.constant;

import com.holiholic.planner.utils.Interval;

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

    // Eating defaults
    public final static Interval defaultLunchInterval = new Interval(Interval.getHour(12, 0, 0),
                                                                     Interval.getHour(15, 0, 0));
    public final static Interval defaultDinnerInterval = new Interval(Interval.getHour(18, 0, 0),
                                                                      Interval.getHour(20, 0, 0));
    public final static int defaultLunchDuration = 90;
    public final static int defaultDinnerDuration = 90;

    // We consider a fixed time to be planned successfully if the time is between interval [time-range, time+range]
    public final static int FIXED_RANGE_ACCEPTANCE = 30;
    public final static int FIXED_TIME_REWARD = 100;

    public final static double drivingCoefficient = 1.3;
    public final static double walkingCoefficient = 2.0;

    // Calculated in km / h
    public final static double drivingMedVelocity = 20;
    public final static double walkingMedVelocity = 6;

    // the value is in meters
    public final static int RESTAURANTS_SEARCH_RADIUS = 500;
}
