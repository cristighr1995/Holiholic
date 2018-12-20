package com.holiholic.places.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

class PlacesManager {
    private static PlacesCredentials placesCredentials;
    private static Map<String, Integer> days;

    static PlacesCredentials getPlacesCredentials() {
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

    static Map<String, Integer> getDays() {
        if (days != null) {
            return days;
        }

        days = new HashMap<>();
        days.put("Sun", Calendar.SUNDAY);
        days.put("Mon", Calendar.MONDAY);
        days.put("Tue", Calendar.TUESDAY);
        days.put("wed", Calendar.WEDNESDAY);
        days.put("Thu", Calendar.THURSDAY);
        days.put("Fri", Calendar.FRIDAY);
        days.put("Sat", Calendar.SATURDAY);
        return days;
    }
}
