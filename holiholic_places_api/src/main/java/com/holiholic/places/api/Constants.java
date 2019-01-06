package com.holiholic.places.api;

class Constants {
    final static String CREDENTIALS_PLACES_PATH = System.getProperty("user.dir") + "/places_api_credentials.json";
    final static String VENUES_URL = "https://api.foursquare.com/v2/venues/";
    final static String SEARCH_VENUES_URL = VENUES_URL + "search";
    final static String DISTANCE_MATRIX_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";
}
