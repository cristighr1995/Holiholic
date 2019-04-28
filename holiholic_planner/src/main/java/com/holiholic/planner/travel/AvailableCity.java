package com.holiholic.planner.travel;

import org.json.JSONObject;

public class AvailableCity {
    public String cityName;
    public String country;
    public int placesCount;
    public String imageUrl;
    public String description;

    public AvailableCity(String cityName, String country, int placesCount) {
        this.cityName = cityName;
        this.country = country;
        this.placesCount = placesCount;
        this.imageUrl = "Not available";
        this.description = "Not available";
    }

    public AvailableCity(String cityName, String country, int placesCount, String imageUrl, String description) {
        this(cityName, country, placesCount);
        this.imageUrl = "Not available";
        this.description = "Not available";
    }

    public JSONObject serialize() {
        JSONObject result = new JSONObject();
        result.put("cityName", cityName);
        result.put("country", country);
        result.put("placesCount", placesCount);
        result.put("imageUrl", imageUrl);
        result.put("description", description);
        return result;
    }
}
