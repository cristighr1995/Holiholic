package com.holiholic.planner.utils;

import java.io.Serializable;

/* GeoPosition - Holds information about the place location (latitude,, longitude)
 *
 */
public class GeoPosition implements Serializable {
    private static final long serialVersionUID = 1L;

    public double latitude;
    public double longitude;

    // constructor
    public GeoPosition(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /* clone - Deep copy of the current object
     *
     *  @return             : clone of the current object
     */
    @Override
    public GeoPosition clone() {
        return new GeoPosition(this.latitude, this.longitude);
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

