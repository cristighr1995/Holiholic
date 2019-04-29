package com.holiholic.places.api;

import org.json.JSONObject;

public class PlaceCategory {
    private String name;
    private String id;
    private String topic;
    private int duration;
    private int limit;
    private PlaceCategoryType type;

    public PlaceCategory(PlaceCategoryType type) {
        this.type = type;
    }

    public PlaceCategory(String name, String topic) {
        this.name = name;
        this.topic = topic;
        this.type = PlaceCategoryType.TOURISTIC_OBJECTIVE;
    }

    public PlaceCategory(String name, String id, String topic, int duration, int limit) {
        this(name, topic);
        this.id = id;
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

    public JSONObject serialize() {
        JSONObject result = new JSONObject();
        result.put("type", PlaceCategoryType.serialize(type));

        if (!isStartingPoint()) {
            assert (name != null && topic != null);

            result.put("name", name);
            result.put("topic", topic);
        }

        return result;
    }

    public boolean isStartingPoint() {
        return type == PlaceCategoryType.STARTING_POINT;
    }

    public static PlaceCategory deserialize(JSONObject serializedPlaceCategory) {
        try {
            PlaceCategoryType type = PlaceCategoryType.deserialize(serializedPlaceCategory.getString("type"));
            if (type == PlaceCategoryType.STARTING_POINT) {
                return new PlaceCategory(type);
            }

            String name = serializedPlaceCategory.getString("name");
            String topic = serializedPlaceCategory.getString("topic");

            return new PlaceCategory(name, topic);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
