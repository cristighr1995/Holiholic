package com.holiholic.planner.utils;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/* Interval - Holds information about the place opening hours for a specific day
 *
 */
public class Interval implements Comparator<Interval> {
    private Calendar start;
    private Calendar end;
    private boolean nonStop;
    private boolean closed;
    private static Map<String, Calendar> hours = new HashMap<>();

    // default constructor creates a non stop place
    Interval() {
        this.nonStop = true;
    }

    // constructor
    public Interval(Calendar start, Calendar end) {
        this.start = start;
        this.end = end;
        if (start == null && end == null) {
            nonStop = true;
        }
    }

    /* getStart - Get the start hour for the current interval
     *
     *  @return             : the start calendar (hour)
     */
    public Calendar getStart() {
        return start;
    }

    /* getEnd - Get the end hour for the current interval
     *
     *  @return             : the end calendar (hour)
     */
    Calendar getEnd() {
        return end;
    }

    /* isNonStop - Checks if the place is non stop opened
     *
     *  @return             : true / false
     */
    private boolean isNonStop() {
        return nonStop;
    }

    /* isClosed - Checks if the place is closed
     *
     *  @return             : true / false
     */
    boolean isClosed() {
        return closed;
    }

    /* setClosed - Close the place in the current interval
     *
     *  @return             : void
     */
    void setClosed() {
        this.closed = true;
        this.nonStop = false;
    }

    /* isBetween - Checks if the given hour is between the current interval
     *
     *  @return             : true / false
     *  @hour               : the hour we want to check
     */
    public boolean isBetween(Calendar hour) {
        if (isNonStop()) {
            return true;
        }

        assert (start != null && hour != null && end != null);
        return start.before(hour) && end.after(hour);
    }

    /* toString - Returns a string representation of the current interval
     *
     *  @return             : the string representation
     */
    @Override
    public String toString() {
        if (isNonStop()) {
            return "Non stop";
        } else if (isClosed()) {
            return "Closed";
        }
        assert (start != null && end != null);
        // the place is opened
        return serialize(start) + " " + serialize(end);
    }

    /* getTotalMinutes - Returns the total minutes until the given hour
     *
     *  @return             : hourOfDay * 60 + minutes
     *  @hour               : the given hour
     */
    private int getTotalMinutes(Calendar hour) {
        return hour.get(Calendar.HOUR_OF_DAY) * 60 + hour.get(Calendar.MINUTE);
    }

    /* compare - Compares two intervals
     *           The non stop places are the greatest and the closed places are the weakest
     *           If neither is non stop or closed, just compare their total minutes
     *
     *  @return             : which one is greater
     *  @first              : the first interval to compare
     *  @second             : the second interval to compare
     */
    @Override
    public int compare(Interval first, Interval second) {
        if ((first.isNonStop() && second.isNonStop()) || (first.isClosed() && second.isClosed())) {
            return 0;
        }
        if (first.isNonStop()) {
            return 1;
        }
        if (second.isNonStop()) {
            return -1;
        }
        if (first.isClosed()) {
            return -1;
        }
        if (second.isClosed()) {
            return 1;
        }

        if (first.getEnd().get(Calendar.DAY_OF_WEEK) == second.getEnd().get(Calendar.DAY_OF_WEEK)) {
           // most late
           int compareResult = second.getEnd().compareTo(first.getEnd());
           if (compareResult != 0) {
               return compareResult;
           }
           // most early
            return first.getStart().compareTo(second.getStart());
        }

        return Integer.compare(second.getEnd().get(Calendar.DAY_OF_WEEK), first.getEnd().get(Calendar.DAY_OF_WEEK));

    }

    /* compareIntervals - Just a static function which compares two interval
     *                    Uses compare function
     *
     *  @return             : which one is greater
     *  @first              : the first interval to compare
     *  @second             : the second interval to compare
     */
    public static int compareIntervals(Interval first, Interval second) {
        return new Interval().compare(first, second);
    }

    /* getHour - Returns a calendar instance for the given parameters
     *
     *  @return             : a corresponding calendar instance
     *  @hourOfDay          : the hour of the day
     *  @minute             : the current minute
     *  @second             : the current second
     */
    private static Calendar getHour(int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        return c;
    }

    /* getHour - Returns a calendar instance for the given parameter
     *
     *  @return             : a corresponding calendar instance
     *  @str                : a string format of the hour (example 21:35)
     */
    public static Calendar getHour(String hour, int dayOfWeek) {
        String key = hour + "-" + dayOfWeek;
        if (hours.containsKey(key)) {
            return CloneFactory.clone(hours.get(key));
        }

        int hourOfDay = Integer.parseInt(hour.substring(0, 2));
        int minutes = Integer.parseInt(hour.substring(2));
        Calendar result = getHour(hourOfDay, minutes);
        result.set(Calendar.DAY_OF_WEEK, dayOfWeek);

        hours.put(key, result);
        return result;
    }

    /* getDiff - Returns the difference between two calendar instance expressed in the given time unit
     *           Internally the difference is expressed in milli seconds
     *
     *  @return             : the difference
     *  @date1              : the first date
     *  @date2              : the second date
     *  @timeUnit           : the time unit we want the result converted
     */
    public static long getDiff(Calendar date1, Calendar date2, TimeUnit timeUnit) {
        long diffInMilliSeconds = date2.getTime().getTime() - date1.getTime().getTime();
        return timeUnit.convert(diffInMilliSeconds, TimeUnit.MILLISECONDS);
    }

    /* serialize - Returns a string representation of the given hour
     *
     *  @return             : the string representation
     *  @hour               : the hour we want to format
     */
    public static String serialize(Calendar hour) {
        return String.format("%02d%02d", hour.get(Calendar.HOUR_OF_DAY), hour.get(Calendar.MINUTE));
    }

    /* serialize - Returns a json object representation of the given hour and day of the week
     *
     *  @return             : the json object representation
     *  @hour               : the hour
     *  @day                : the day for the interval
     */
    private static JSONObject serialize(Calendar hour, int day) {
        JSONObject result = new JSONObject();
        result.put("time", serialize(hour));
        result.put("day", day);
        return result;
    }

    /* serialize - Returns a json format representation of the current interval given the day
     *
     *  @return             : serialization
     *  @day                : the day of the week for the current interval
     */
    JSONObject serialize(int day) {
        JSONObject result = new JSONObject();
        result.put("open", serialize(getStart(), day));
        result.put("close", serialize(getEnd(), day));
        return result;
    }

    public static String toString(Calendar hour) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(hour.getTime());
    }
}
