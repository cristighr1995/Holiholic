package com.holiholic.places.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

class PlacesManager {
    private static PlacesCredentials placesCredentials;

    public static PlacesCredentials getPlacesCredentials() {
        if (placesCredentials != null) {
            return placesCredentials;
        }

        if (!new File(Constants.CREDENTIALS_PLACES_PATH).exists()) {
            System.err.println("Places credentials path \"" + Constants.CREDENTIALS_PLACES_PATH + "\" is invalid.");
            return null;
        }

        try {
            placesCredentials = new ObjectMapper().readValue(new File(Constants.CREDENTIALS_PLACES_PATH),
                                                             PlacesCredentials.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return placesCredentials;
    }
}
