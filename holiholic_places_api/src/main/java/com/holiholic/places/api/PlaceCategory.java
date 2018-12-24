package com.holiholic.places.api;

public class PlaceCategory {
    private String name;
    private String id;
    private String topic;
    private int duration;
    private int limit;

    public PlaceCategory(String name, String id, String topic, int duration, int limit) {
        this.name = name;
        this.id = id;
        this.topic = topic;
        this.duration = duration;
        this.limit = limit;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public int getDuration() {
        return duration;
    }

    public int getLimit() {
        return limit;
    }
}
