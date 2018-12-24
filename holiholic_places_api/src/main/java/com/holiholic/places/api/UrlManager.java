package com.holiholic.places.api;

class UrlManager {

    static String getSearchVenuesUrl(String near, String categoryId, int limit) {
        PlacesCredentials placesCredentials = PlacesManager.getPlacesCredentials();
        if (placesCredentials == null) {
            return "";
        }
        return Constants.SEARCH_VENUES_URL
               + "?near=" + near
               + "&categoryId=" + categoryId
               + "&limit=" + limit
               + "&" + placesCredentials.toString();
    }

    static String getVenueDetailsUrl(String placeId) {
        PlacesCredentials placesCredentials = PlacesManager.getPlacesCredentials();
        if (placesCredentials == null) {
            return "";
        }
        return Constants.VENUES_URL + placeId + "?" + placesCredentials.toString();
    }

    static String getDistanceMatrixUrl(String origin, String destination, String travelMode) {
        PlacesCredentials placesCredentials = PlacesManager.getPlacesCredentials();
        if (placesCredentials == null) {
            return "";
        }

        return Constants.DISTANCE_MATRIX_URL
               + "?origins=" + origin
               + "&destinations=" + destination
               + "&mode=" + travelMode
               + "&language=en-EN"
               + "&key=" + placesCredentials.getGoogleApiKey();
    }
}
