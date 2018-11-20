package com.holiholic.planner.utils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/* OpeningPeriod - Holds information about the place opening hours
 *
 */
public class OpeningPeriod {
    private Map<Integer, Interval> intervals;
    private Map<Integer, Integer> dayMappings;
    private boolean nonStop = false;

    // default constructor creates a non stop place
    public OpeningPeriod() {
        this.nonStop = true;
    }

    // constructor
    public OpeningPeriod(Map<Integer, Interval> intervals) {
        this.intervals = intervals;
        setDayMappings();
    }

    /* setDayMappings - Maps Calendar day representation to Google day representation
     *
     *  @return             : void
     */
    private void setDayMappings() {
        dayMappings = new HashMap<>();
        dayMappings.put(Calendar.MONDAY, 1);
        dayMappings.put(Calendar.TUESDAY, 2);
        dayMappings.put(Calendar.WEDNESDAY, 3);
        dayMappings.put(Calendar.THURSDAY, 4);
        dayMappings.put(Calendar.FRIDAY, 5);
        dayMappings.put(Calendar.SATURDAY, 6);
        dayMappings.put(Calendar.SUNDAY, 0);
    }

    /* setDayMappings - Makes the current opening period closed for all seven days
     *
     *  @return             : void
     */
    public void setClosedAllDays() {
        setDayMappings();
        this.intervals = new HashMap<>();
        for (int day = 0; day < 7; day++) {
            Interval closeInterval = new Interval();
            closeInterval.setClosed(true);
            this.intervals.put(day, closeInterval);
        }
    }

    /* isClosed - Checks if the place is closed in the given day of the week
     *
     *  @return             : true / false
     *  @dayOfWeek          : the calendar day of the week when to check
     */
    public boolean isClosed(int dayOfWeek) {
        int day = dayMappings.get(dayOfWeek);
        return intervals.get(day).isClosed();
    }

    /* canVisit - Checks if the place can be visited at the given hour
     *
     *  @return             : true / false
     *  @hour               : the hour we want to check
     */
    public boolean canVisit(Calendar hour) {
        if (isNonStop()) {
            return true;
        }
        int day = dayMappings.get(hour.get(Calendar.DAY_OF_WEEK));
        return !intervals.get(day).isClosed() && intervals.get(day).isBetween(hour);
    }

    /* isNonStop - Checks if the place is non stop opened
     *
     *  @return             : true / false
     */
    public boolean isNonStop() {
        return nonStop;
    }

    /* isBetween - Checks if the hour is fitted in the user visiting interval
     *
     *  @return             : true / false
     */
    public boolean isBetween(Calendar hour) {
        return true;
    }

    /* getInterval - Get the interval instance for a specific day
     *
     *  @return             : the interval instance for the given day
     *  @dayOfWeek          : the calendar day of the week
     */
    public Interval getInterval(int dayOfWeek) {
        return intervals.get(dayMappings.get(dayOfWeek));
    }

    /* clone - Deep copy of the current object
     *
     *  @return             : clone of the current object
     */
    @Override
    public OpeningPeriod clone() {
        if (isNonStop()) {
            return new OpeningPeriod();
        }
        Map<Integer, Interval> intervalsClone = new HashMap<>();
        for (Map.Entry<Integer, Interval> interval : intervals.entrySet()) {
            intervalsClone.put(interval.getKey(), (Interval) interval.getValue().clone());
        }
        return new OpeningPeriod(intervalsClone);
    }
}
