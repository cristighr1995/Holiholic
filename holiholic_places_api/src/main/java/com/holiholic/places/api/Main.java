package com.holiholic.places.api;

import org.json.JSONObject;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        // Places.getPlaces("Bucharest", Arrays.asList("4bf58dd8d48988d181941735"));

        JSONObject obj = Places.buildPlaceInfo("4dfc6d43fa76c83b6e7ca607");
    }
}
