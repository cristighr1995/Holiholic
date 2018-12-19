package com.holiholic.places.api;

import java.util.List;

class UrlManager {

    static String getSearchVenuesUrl(String near, List<String> categoryIds) {
        PlacesCredentials placesCredentials = PlacesManager.getPlacesCredentials();
        if (placesCredentials == null) {
            return "";
        }
        return Constants.SEARCH_VENUES_URL + "?near=" + near + serialize(categoryIds) + "&" + placesCredentials.toString();
    }

    static String getVenueDetailsUrl(String placeId) {
        PlacesCredentials placesCredentials = PlacesManager.getPlacesCredentials();
        if (placesCredentials == null) {
            return "";
        }
        return Constants.VENUES_URL + placeId + "?" + placesCredentials.toString();
    }

    private static String serialize(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("&categoryId=");
        builder.append(categoryIds.get(0));
        for (int i = 1; i < categoryIds.size(); i++) {
            builder.append(",").append(categoryIds.get(i));
        }

        return builder.toString();
    }
}
