package com.holiholic.places.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.Callable;

public class PlaceDetailsTask implements Callable<Boolean> {
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
            JSONObject place = Places.buildPlaceInfo(placeId, placeCategory);
            places.put(index, place);
        } catch (Exception e) {
            e.printStackTrace();
            result = Boolean.FALSE;
        }
        return result;
    }
}
