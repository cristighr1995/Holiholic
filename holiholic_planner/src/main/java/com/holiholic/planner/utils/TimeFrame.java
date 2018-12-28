package com.holiholic.planner.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/* TimeFrame - Holds information about the place opening hours
 *
 */
public class TimeFrame {
    private Map<Integer, Interval> intervals;
    private boolean nonStop = false;

    // default constructor creates a non stop place
    private TimeFrame() {
        this.nonStop = true;
    }

    // constructor
    private TimeFrame(Map<Integer, Interval> intervals) {
        this.intervals = intervals;
    }

    /* setDayMappings - Makes the current opening period closed for all seven days
     *
     *  @return             : void
     */
    public void setClosedAllDays() {
        this.intervals = new HashMap<>();
        for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
            Interval closeInterval = new Interval();
            closeInterval.setClosed();
            this.intervals.put(dayOfWeek, closeInterval);
        }
    }

    /* isClosed - Checks if the place is closed in the given day of the week
     *
     *  @return             : true / false
     *  @dayOfWeek          : the calendar day of the week when to check
     */
    public boolean isClosed(int dayOfWeek) {
        return intervals.get(dayOfWeek).isClosed();
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
        int dayOfWeek = hour.get(Calendar.DAY_OF_WEEK);
        return !intervals.get(dayOfWeek).isClosed() && intervals.get(dayOfWeek).isBetween(hour);
    }

    /* getOpenDays - Get a list of open days
     *
     *  @return             : a list of indexes for each open day
     */
    public List<Integer> getOpenDays() {
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
    public boolean canVisit(TimeFrame timeFrame) {
        if (isNonStop()) {
            return true;
        }

        List<Integer> openDays = timeFrame.getOpenDays();

        for (int dayOfWeek : openDays) {
            if (isClosed(dayOfWeek)) {
                continue;
            }
            Interval placeInterval = getInterval(dayOfWeek);
            Interval periodInterval = timeFrame.getInterval(dayOfWeek);

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

    /* getInterval - Get the interval instance for a specific day
     *
     *  @return             : the interval instance for the given day
     *  @dayOfWeek          : the calendar day of the week
     */
    public Interval getInterval(int dayOfWeek) {
        return intervals.get(dayOfWeek);
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
                result.put(getInterval(dayOfWeek).serialize(dayOfWeek));
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
    private static Calendar deserializeHour(String hour, int dayOfWeek) {
        return Interval.getHour(hour, dayOfWeek);
    }

    /* deserialize - Creates an OpeningPeriod instance from a json period
     *
     *  @return             : the OpeningPeriod instance of the given json
     *  @period             : the json from file which contains information about the opening hours for a place
     */
    public static TimeFrame deserialize(JSONArray timeFrame) {
        // check if the place is non stop
        if (timeFrame.getJSONObject(0).getJSONObject("open").getString("time").equals("0000")) {
            return new TimeFrame();
        }

        Set<Integer> closed = new HashSet<>();
        Map<Integer, Interval> intervals = new HashMap<>();
        for (int day = 1; day <= 7; day++) {
            closed.add(day);
        }

        for (int i = 0; i < timeFrame.length(); i++) {
            JSONObject dayPeriod = timeFrame.getJSONObject(i);
            JSONObject open = dayPeriod.getJSONObject("open");
            JSONObject close = dayPeriod.getJSONObject("close");
            int dayOpen = open.getInt("day");
            int dayClose = close.getInt("day");
            Calendar start = deserializeHour(open.getString("time"), dayOpen);
            Calendar end = deserializeHour(close.getString("time"), dayClose);

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
            closeInterval.setClosed();
            intervals.put(closeDay, closeInterval);
        }

        return new TimeFrame(intervals);
    }
}
