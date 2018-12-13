package com.holiholic.planner.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

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
        for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
            Interval closeInterval = new Interval();
            closeInterval.setClosed(true);
            this.intervals.put(dayOfWeek, closeInterval);
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

    /* getOpenDays - Get a list of open days
     *
     *  @return             : a list of indexes for each open day
     */
    private List<Integer> getOpenDays() {
        List<Integer> openDays = new ArrayList<>();
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            if (isNonStop() || !isClosed(dayOfWeek)) {
                openDays.add(dayOfWeek);
            }
        }
        return openDays;
    }

    /* canVisit - Check if the current period (visiting interval) can be visited in the given period
     *            For example each place has an opening period which consist of daily visiting Interval
     *            The period parameter can be the day(s) the user wants to visit
     *            So an use case would be when the user wants to check the availability of a place for Monday and Friday
     *
     *  @return             : true or false
     *  @period             : the period we want to check if it is between the current opening period
     */
    public boolean canVisit(OpeningPeriod period) {
        if (isNonStop()) {
            return true;
        }

        List<Integer> openDays = period.getOpenDays();

        for (int dayOfWeek : openDays) {
            if (isClosed(dayOfWeek)) {
                continue;
            }
            Interval placeInterval = getInterval(dayOfWeek);
            Interval periodInterval = period.getInterval(dayOfWeek);

            if (placeInterval.getStart().before(periodInterval.getStart())
                && periodInterval.getStart().before(placeInterval.getEnd())) {
                return true;
            }
            if (placeInterval.getStart().before(periodInterval.getEnd())
                && periodInterval.getEnd().before(placeInterval.getEnd())) {
                return true;
            }
            if (periodInterval.getStart().before(placeInterval.getStart())
                && placeInterval.getStart().before(periodInterval.getEnd())) {
                return true;
            }
        }
        return false;
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

    /* serializeNonStopPeriod - Returns a json format representation for a non stop place
     *
     *  @return             : serialization
     */
    private JSONObject serializeNonStopPeriod() {
        JSONObject result = new JSONObject();
        JSONObject nonStopObject = new JSONObject();
        nonStopObject.put("time", "0000");
        nonStopObject.put("day", 0);
        result.put("open", nonStopObject);
        return result;
    }

    /* serialize - Returns a json format representation for the current opening period
     *
     *  @return             : serialization
     */
    public JSONArray serialize() {
        JSONArray result = new JSONArray();

        if (isNonStop()) {
            result.put(serializeNonStopPeriod());
            return result;
        }

        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            if (!isClosed(dayOfWeek)) {
                result.put(getInterval(dayOfWeek).serialize(dayMappings.get(dayOfWeek)));
            }
        }

        return result;
    }

    /* deserializeHour - Creates a Calendar instance from a coded hour
     *                   Example 0930 means the time 09:30
     *
     *  @return             : the calendar instance of the given hour
     *  @hour               : the serialized hour
     */
    public static Calendar deserializeHour(String hour) {
        int h = Integer.parseInt(hour.substring(0, 2));
        int m = Integer.parseInt(hour.substring(2));
        return Interval.getHour(h, m, 0);
    }

    /* deserialize - Creates an OpeningPeriod instance from a json period
     *
     *  @return             : the OpeningPeriod instance of the given json
     *  @period             : the json from file which contains information about the opening hours for a place
     */
    public static OpeningPeriod deserialize(JSONArray period) {
        // check if the place is non stop
        if (period.getJSONObject(0).getJSONObject("open").getString("time").equals("0000")) {
            return new OpeningPeriod();
        }

        Set<Integer> closed = new HashSet<>();
        Map<Integer, Interval> intervals = new HashMap<>();
        for (int day = 0; day < 7; day++) {
            closed.add(day);
        }

        for (int i = 0; i < period.length(); i++) {
            JSONObject dayPeriod = period.getJSONObject(i);
            JSONObject open = dayPeriod.getJSONObject("open");
            JSONObject close = dayPeriod.getJSONObject("close");
            Calendar start = deserializeHour(open.getString("time"));
            Calendar end = deserializeHour(close.getString("time"));
            int dayOpen = open.getInt("day");
            int dayClose = close.getInt("day");
            // set the correct day
            start.set(Calendar.DAY_OF_WEEK, dayOpen + 1);
            end.set(Calendar.DAY_OF_WEEK, dayClose + 1);

            // this means the bar is closing after midnight
            if (dayClose != dayOpen) {
                end.add(Calendar.DAY_OF_WEEK, 1);
            }
            intervals.put(dayOpen, new Interval(start, end));
            // erase from closed days
            closed.remove(dayOpen);
        }
        for (int closeDay : closed) {
            Interval closeInterval = new Interval();
            closeInterval.setClosed(true);
            intervals.put(closeDay, closeInterval);
        }

        return new OpeningPeriod(intervals);
    }
}
