package com.holiholic.planner.travel;

import com.holiholic.planner.database.DatabaseManager;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.planner.Planner;
import org.json.JSONArray;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Itinerary {
    private String id;
    private String cityName;
    private LocalDateTime timestamp;
    private List<Place> places;
    private ItineraryStats stats;

    private JSONArray serializedPlaces;
    private DateTimeFormatter formatter;

    public Itinerary(String cityName, List<Place> places) {
        this.cityName = cityName;
        this.timestamp = LocalDateTime.now();
        this.places = places;
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    public String getId() {
        if (id != null) {
            return id;
        }

        id = DatabaseManager.generateHash(getSerializedPlaces().toString());
        return id;
    }

    public String getCityName() {
        return cityName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public List<Place> getPlaces() {
        return places;
    }

    public ItineraryStats getStats() {
        if (stats != null) {
            return stats;
        }

        stats = Planner.getStats(places);
        return stats;
    }

    public JSONArray getSerializedPlaces() {
        if (serializedPlaces != null) {
            return serializedPlaces;
        }

        serializedPlaces = new JSONArray();

        for (Place place : places) {
            serializedPlaces.put(Planner.serialize(place));
        }

        return serializedPlaces;
    }

    public List<String> getValuesList() {
        List<String> values = new ArrayList<>();
        values.add(DatabaseManager.escape(getId()));
        values.add(DatabaseManager.escape(cityName));
        values.add(DatabaseManager.escape(timestamp.format(formatter)));
        values.add(DatabaseManager.escape(getSerializedPlaces().toString(2)));
        values.add(DatabaseManager.escape(getStats().serialize().toString(2)));
        return values;
    }
}
