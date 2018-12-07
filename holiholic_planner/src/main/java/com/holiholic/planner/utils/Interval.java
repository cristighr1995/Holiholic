package com.holiholic.planner.utils;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/* Interval - Holds information about the place opening hours for a specific day
 *
 */
public class Interval implements Comparator<Interval>, Serializable {
    private static final long serialVersionUID = 1L;

    private Calendar start;
    private Calendar end;
    private boolean nonStop;
    private boolean closed;

    // default constructor creates a non stop place
    public Interval() {
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

    /* setClosed - Close or open a place in the current interval
     *             Right now this method only closes a place so be careful!
     *
     *  @return             : void
     *  @closed             : true / false
     */
    public void setClosed(boolean closed) {
        this.closed = closed;
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

        assert (start != null && hour != null);

        int startHour = start.get(Calendar.HOUR_OF_DAY);
        int startMinute = start.get(Calendar.MINUTE);
        int currentHour = hour.get(Calendar.HOUR_OF_DAY);
        int currentMinute = hour.get(Calendar.MINUTE);

        int startTotalMinutes = startHour * 60 + startMinute;
        int currentTotalMinutes = currentHour * 60 + currentMinute;

        int startDay = start.get(Calendar.DAY_OF_YEAR);
        int currentDay = hour.get(Calendar.DAY_OF_YEAR);

        if (end == null) {
            return startTotalMinutes <= currentTotalMinutes || startDay < currentDay;
        }

        int endHour = end.get(Calendar.HOUR_OF_DAY);
        int endMinute = end.get(Calendar.MINUTE);
        int endTotalMinutes = endHour * 60 + endMinute;

        return startTotalMinutes <= currentTotalMinutes && currentTotalMinutes <= endTotalMinutes;
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
        return serializeHour(start) + " " + serializeHour(end);
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

        assert (first.getEnd() != null && second.getEnd() != null);

        Calendar firstStart = first.getStart();
        Calendar firstEnd = first.getEnd();

        Calendar secondStart = second.getStart();
        Calendar secondEnd = second.getEnd();

        int fsTotal = getTotalMinutes(firstStart);
        int feTotal = getTotalMinutes(firstEnd);

        int ssTotal = getTotalMinutes(secondStart);
        int seTotal = getTotalMinutes(secondEnd);

        int firstEndDay = firstEnd.get(Calendar.DAY_OF_YEAR);
        int secondEndDay = secondEnd.get(Calendar.DAY_OF_YEAR);

        int minutesForEntireDay = 23 * 60 + 59;

        // Add an entire day to the one who is closing in the night (example at 3AM in the night)
        if (firstEndDay > secondEndDay) {
            feTotal += minutesForEntireDay;
        }
        if (firstEndDay < secondEndDay) {
            seTotal += minutesForEntireDay;
        }

        // Chose the most late
        if (feTotal > seTotal) {
            return -1;
        }
        if (feTotal < seTotal) {
            return 1;
        }

        // Chose the most early
        return Integer.compare(ssTotal, fsTotal);

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
    public static Calendar getHour(int hourOfDay, int minute, int second) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        return c;
    }

    /* getHour - Returns a calendar instance for the given parameter
     *
     *  @return             : a corresponding calendar instance
     *  @str                : a string format of the hour (example 21:35)
     */
    public static Calendar getHour(String str) {
        String[] hour = str.split(":");
        return getHour(Integer.parseInt(hour[0]), Integer.parseInt(hour[1]), 0);
    }

    /* getDiff - Returns the difference between two calendar instance expressed in the given time unit
     *           Internally the difference is expresed in milli seconds
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

    /* serializeHour - Returns a string representation of the given hour
     *
     *  @return             : the string representation
     *  @hour               : the hour we want to format
     */
    public static String serializeHour(Calendar hour) {
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
        result.put("time", serializeHour(hour));
        result.put("day", day);
        return result;
    }

    /* clone - Deep copy of the current object
     *
     *  @return             : clone of the current object
     */
    @Override
    public Object clone() {
        if (isNonStop()) {
            return new Interval();
        }
        if (isClosed()) {
            Interval closedInterval = new Interval();
            closedInterval.setClosed(true);
            return closedInterval;
        }
        assert (start != null && end != null);
        Interval intervalClone = new Interval((Calendar) start.clone(), (Calendar) end.clone());
        intervalClone.nonStop = this.nonStop;
        intervalClone.closed = this.closed;
        return intervalClone;
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
}
