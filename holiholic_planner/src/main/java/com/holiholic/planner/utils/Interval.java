package com.holiholic.planner.utils;

import com.holiholic.planner.constant.Constants;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/* Interval - Holds information about the place opening hours for a specific day
 *
 */
public class Interval implements Comparator<Interval> {
    private LocalDateTime start;
    private LocalDateTime end;
    private boolean nonStop;
    private boolean closed;
    private static Map<String, LocalDateTime> hours = new HashMap<>();

    Interval() {
        this.nonStop = true;
    }

    Interval(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
        if (start == null && end == null) {
            nonStop = true;
        }
    }

    /* getStart - Get the start hour for the current interval
     *
     *  @return             : the start LocalDateTime (hour)
     */
    public LocalDateTime getStart() {
        return start;
    }

    /* getEnd - Get the end hour for the current interval
     *
     *  @return             : the end LocalDateTime (hour)
     */
    LocalDateTime getEnd() {
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

    /* isBetween - Checks if the given date time is between the current interval
     *
     *  @return             : true / false
     *  @time               : time to check
     */
    boolean isBetween(LocalDateTime time) {
        if (isNonStop()) {
            return true;
        }

        assert (start != null && time != null && end != null);
        return start.isBefore(time) && end.isAfter(time);
    }

    /* toString - Returns a string representation of the current interval
     *
     *  @return             : the string representation
     */
    @Override
    public String toString() {
        if (isNonStop()) {
            return "24 Hours";
        } else if (isClosed()) {
            return "Closed";
        }
        assert (start != null && end != null);
        return serialize(start) + " " + serialize(end);
    }

    /* compare - Compares two intervals
     *           The non stop places are the greatest and the closed places are the weakest
     *           If neither is non stop or closed, just compare their total minutes
     *
     *  @return             : which one is greater
     *  @i1                 : the first interval to compare
     *  @i2                 : the second interval to compare
     */
    @Override
    public int compare(Interval i1, Interval i2) {
        if ((i1.isNonStop() && i2.isNonStop()) || (i1.isClosed() && i2.isClosed())) {
            return 0;
        }
        if (i1.isNonStop()) {
            return 1;
        }
        if (i2.isNonStop()) {
            return -1;
        }
        if (i1.isClosed()) {
            return -1;
        }
        if (i2.isClosed()) {
            return 1;
        }

        if (i1.getEnd().getDayOfWeek().equals(i2.getEnd().getDayOfWeek())) {
           // most late
           int compareResult = i2.getEnd().compareTo(i1.getEnd());
           if (compareResult != 0) {
               return compareResult;
           }
           // most early
            return i1.getStart().compareTo(i2.getStart());
        }

        return Integer.compare(i2.getEnd().getDayOfWeek().getValue(), i1.getEnd().getDayOfWeek().getValue());

    }

    /* compareIntervals - Just a static function which compares two interval
     *                    Uses compare function
     *
     *  @return             : which one is greater
     *  @i1                 : the first interval to compare
     *  @i2                 : the second interval to compare
     */
    public static int compareIntervals(Interval i1, Interval i2) {
        return new Interval().compare(i1, i2);
    }

    /* getDateTimeFromHour - Returns a LocalDateTime instance
     *
     *  @return             : a corresponding LocalDateTime instance
     *  @hourOfDay          : the hour of the day
     *  @minute             : the current minute
     *  @second             : the current second
     */
    private static LocalDateTime getDateTimeFromHour(int hourOfDay, int minute) {
        return LocalDateTime.now().withHour(hourOfDay).withMinute(minute);
    }

    /* getDateTime - Returns a LocalDateTime using US conventions
     *
     *  @return             : a corresponding LocalDateTime instance
     *  @hour               : hour in format "HHmm""
     *  @dayOfWeek          : day of week in US convention (SUN = 1 ... SAT = 7)
     */
    public static LocalDateTime getDateTime(String hour, int dayOfWeek) {
        String key = hour + "-" + dayOfWeek;
        if (hours.containsKey(key)) {
            return hours.get(key);
        }

        int hourOfDay = Integer.parseInt(hour.substring(0, 2));
        int minutes = Integer.parseInt(hour.substring(2));
        LocalDateTime dateTime = getDateTimeFromHour(hourOfDay, minutes).with(Constants.US_FIELD_DAY_OF_WEEK, dayOfWeek);

        hours.put(key, dateTime);
        return dateTime;
    }

    /* getDiff - Returns the difference between two LocalDateTime instance expressed in the given time unit
     *           Internally the difference is expressed in milli seconds
     *
     *  @return             : the difference
     *  @d1                 : the first date
     *  @d2                 : the second date
     *  @timeUnit           : the time unit we want the result converted
     */
    public static long getDiff(LocalDateTime d1, LocalDateTime d2, TimeUnit timeUnit) {
        return timeUnit.convert(Duration.between(d1, d2).getSeconds(), TimeUnit.SECONDS);
    }

    /* serialize - Returns a string representation of the given date time with only hour and minute
     *
     *  @return             : the string representation
     *  @time               : date time to format
     */
    public static String serialize(LocalDateTime time) {
        return String.format("%02d%02d", time.getHour(), time.getMinute());
    }

    /* serialize - Returns a json object representation of the given time and day of week
     *
     *  @return             : the json object representation
     *  @time               : the time
     *  @day                : the day for the interval
     */
    private static JSONObject serialize(LocalDateTime time, int day) {
        JSONObject result = new JSONObject();
        result.put("time", serialize(time));
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

    /* isInRange - Check if the time is between [reference - range, reference + range]
     *
     *  @return             : true / false
     *  @reference          : reference point
     *  @time               : time to check
     *  @range              : range for interval
     */
    public static boolean isInRange(LocalDateTime reference, LocalDateTime time, int range) {
        LocalDateTime minus = reference.minusSeconds(range);
        LocalDateTime plus = reference.plusSeconds(range);
        return minus.isBefore(time) && plus.isAfter(time);
    }
}
