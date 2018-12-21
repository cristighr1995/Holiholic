package com.holiholic.places.api;

import org.json.JSONObject;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        Places.getPlaces("Bucharest", new PlaceCategory("Food", "4d4b7105d754a06374d81259", "Restaurants", 5400));

        //JSONObject obj = Places.buildPlaceInfo("5491a88f498e12011af1a3a7", new PlaceCategory("Food", "4d4b7105d754a06374d81259", "Restaurants", 5400));
    }
}
