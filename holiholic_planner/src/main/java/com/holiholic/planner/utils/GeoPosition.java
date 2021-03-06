package com.holiholic.planner.utils;

/* GeoPosition - Holds information about the place location (latitude,, longitude)
 *
 */
public class GeoPosition {
    public double latitude;
    public double longitude;

    public GeoPosition(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /* distanceBetweenGeoCoordinates - Calculates the mathematical distance between two geo points
     *
     *  @return             : the distance expressed in meters
     *  @origin             : start place
     *  @destination        : destination place
     */
    public static double distanceBetweenGeoCoordinates(GeoPosition origin, GeoPosition destination) {
        // calculated in meters
        double earthRadius = 6371000;
        double dLat = Math.toRadians(destination.latitude - origin.latitude);
        double dLng = Math.toRadians(destination.longitude - origin.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                   + Math.cos(Math.toRadians(origin.latitude))
                   * Math.cos(Math.toRadians(destination.latitude))
                   * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}

