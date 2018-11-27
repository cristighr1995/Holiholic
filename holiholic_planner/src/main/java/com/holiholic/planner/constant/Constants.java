package com.holiholic.planner.constant;

import com.holiholic.planner.utils.Interval;

/* Constants - All constants will be declared here
 *
 */
public class Constants {
    public final static String CONTAINS_USER_URL = "http://localhost:8092/containsUser";
    public final static String UPDATE_HISTORY_URL = "http://localhost:8092/updateHistory";
    public final static String DATABASE_PATH = System.getProperty("user.dir") + "/db/";

    // Eating defaults
    public final static Interval defaultLunchInterval = new Interval(Interval.getHour(12, 0, 0),
                                                                     Interval.getHour(15, 0, 0));
    public final static Interval defaultDinnerInterval = new Interval(Interval.getHour(18, 0, 0),
                                                                      Interval.getHour(20, 0, 0));
    public final static int defaultLunchDuration = 90;
    public final static int defaultDinnerDuration = 90;

    // We consider a fixed time to be planned successfully if the time is between interval [time-range, time+range]
    public final static int fixedTimeIntervalRange = 30;

    public final static int scoreFixedTime = 100;

    public final static double maxWeatherRainThreshold = 0.65;
    public final static double maxWeatherSnowThreshold = 0.9;

    public final static double maxRating = 5.0;
    public final static double placePositionRate = 0.2;

    public final static double drivingCoefficient = 1.3;
    public final static double walkingCoefficient = 2.0;

    // Calculated in km / h
    public final static double drivingMedVelocity = 20;
    public final static double walkingMedVelocity = 6;
}
