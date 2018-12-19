package com.holiholic.places.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.Callable;

public class PlaceDetailsTask implements Callable<Boolean> {
    private String placeId;
    private int index;
    private JSONArray places;

    PlaceDetailsTask(String placeId, int index, JSONArray places) {
        this.placeId = placeId;
        this.index = index;
        this.places = places;
    }

    @Override
    public Boolean call() {
        Boolean result = Boolean.TRUE;
        try {
            JSONObject place = Places.buildPlaceInfo(placeId);
            places.put(index, place);
        } catch (Exception e) {
            e.printStackTrace();
            result = Boolean.FALSE;
        }
        return result;
    }
}
