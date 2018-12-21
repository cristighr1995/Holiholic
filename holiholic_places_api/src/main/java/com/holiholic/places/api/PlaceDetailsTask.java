package com.holiholic.places.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

class PlaceDetailsTask implements Callable<Boolean> {
    private String placeId;
    private int index;
    private JSONArray places;
    private PlaceCategory placeCategory;

    PlaceDetailsTask(String placeId, int index, JSONArray places, PlaceCategory placeCategory) {
        this.placeId = placeId;
        this.index = index;
        this.places = places;
        this.placeCategory = placeCategory;
    }

    @Override
    public Boolean call() {
        Boolean result = Boolean.TRUE;
        try {
            Instant start = Instant.now();
            JSONObject place = Places.buildPlaceInfo(placeId, placeCategory);
            places.put(index, place);
            Instant end = Instant.now();
            Duration timeElapsed = Duration.between(start, end);
            System.out.println("Built place \"" + placeId + "\" in " + timeElapsed.toMillis() + " milliseconds");
        } catch (Exception e) {
            e.printStackTrace();
            result = Boolean.FALSE;
        }
        return result;
    }
}
