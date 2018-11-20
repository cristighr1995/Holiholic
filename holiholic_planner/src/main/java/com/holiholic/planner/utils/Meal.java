package com.holiholic.planner.utils;

/* Meal - Holds information about the meal if included
 *
 */
public class Meal {
    public Interval interval;
    public int duration;

    // constructor
    public Meal(Interval interval, int duration) {
        this.interval = interval;
        this.duration = duration;
    }
}
