package com.holiholic.planner.travel;

import org.json.JSONObject;

public class ItineraryStats {
    private long distance;
    private long duration;
    private double averageRating;
    private int size;

    public ItineraryStats(long distance, long duration, double averageRating, int size) {
        this.distance = distance;
        this.duration = duration;
        this.averageRating = averageRating;
        this.size = size;
    }

    public long getDistance() {
        return distance;
    }

    public long getDuration() {
        return duration;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getSize() {
        return size;
    }

    public JSONObject serialize() {
        JSONObject result = new JSONObject();
        result.put("distance", distance);
        result.put("duration", duration);
        result.put("averageRating", averageRating);
        result.put("size", size);
        return result;
    }
}
