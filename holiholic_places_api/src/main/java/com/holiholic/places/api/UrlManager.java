package com.holiholic.places.api;

class UrlManager {

    static String getSearchVenuesUrl(String near, String categoryId) {
        PlacesCredentials placesCredentials = PlacesManager.getPlacesCredentials();
        if (placesCredentials == null) {
            return "";
        }
        return Constants.SEARCH_VENUES_URL
               + "?near=" + near
               + "&categoryId=" + categoryId
               + "&" + placesCredentials.toString();
    }

    static String getVenueDetailsUrl(String placeId) {
        PlacesCredentials placesCredentials = PlacesManager.getPlacesCredentials();
        if (placesCredentials == null) {
            return "";
        }
        return Constants.VENUES_URL + placeId + "?" + placesCredentials.toString();
    }
}
