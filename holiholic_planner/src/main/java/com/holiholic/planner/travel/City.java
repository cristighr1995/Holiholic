package com.holiholic.planner.travel;

import com.holiholic.planner.database.DatabaseManager;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.utils.Enums;
import com.holiholic.planner.utils.TimeFrame;

import java.util.*;

/* City - Singleton class to provide fast access to places from that city
 *        It will be used also to cache the most frequent cities
 *
 */
public class City {
    // because we have a singleton we will need this instance which will be created only once
    private static City instance;
    private String name;
    private Map<Integer, Place> places;
    private Map<Integer, Place> restaurants;

    private Map<Enums.TravelMode, double[][]> distance;
    private Map<Enums.TravelMode, double[][]> duration;

    // private constructor !!!
    private City(String name) {
        this.name = name;
        this.distance = new HashMap<>();
        this.duration = new HashMap<>();
    }

    /* getInstance - Get the instance for the city
     *
     *  @return       : the city instance
     *  @cityName     : the city name
     */
    public static City getInstance(String cityName) {
        if (instance == null) {
            //synchronized block to remove overhead
            synchronized (City.class) {
                if(instance == null) {
                    // if instance is null, initialize
                    instance = new City(cityName);
                }
            }
        }

        return instance;
    }

    public String getName() {
        return name;
    }

    public Map<Integer, Place> getPlaces() {
        return places;
    }

    public List<Place> getPlacesAsList() {
        return getPlacesAsList(getPlaces());
    }

    public List<Place> getPlacesAsList(Map<Integer, Place> places) {
        List<Place> placesList = new ArrayList<>();

        for (Map.Entry<Integer, Place> placeEntry : places.entrySet()) {
            placesList.add(placeEntry.getValue());
        }

        return placesList;
    }

    public void setPlaces(Map<Integer, Place> places) {
        this.places = places;
    }

    public Map<Integer, Place> getRestaurants() {
        return restaurants;
    }

    public void setRestaurants(Map<Integer, Place> restaurants) {
        this.restaurants = restaurants;
    }

    public Map<Integer, Place> getFilteredPlaces(Set<String> tags) {
        Map<Integer, Place> filteredPlaces = new HashMap<>();

        for (Map.Entry<Integer, Place> placeEntry : getPlaces().entrySet()) {
            for (String tag : tags) {
                if (placeEntry.getValue().tags.contains(tag)) {
                    filteredPlaces.put(placeEntry.getKey(), placeEntry.getValue());
                    break;
                }
            }
        }

        return filteredPlaces;
    }

    public Map<Integer, Place> getOpenPlaces(TimeFrame timeFrame) {
        return getOpenPlaces(this.places, timeFrame);
    }

    public Map<Integer, Place> getOpenPlaces(Map<Integer, Place> places, TimeFrame timeFrame) {
        Map<Integer, Place> openPlaces = new HashMap<>();

        for (Map.Entry<Integer, Place> placeEntry : places.entrySet()) {
            if (placeEntry.getValue().canVisit(timeFrame)) {
                openPlaces.put(placeEntry.getKey(), placeEntry.getValue());
            }
        }

        return openPlaces;
    }

    public List<Place> getSortedPlaces(Map<Integer, Place> places) {
        List<Place> placesList = getPlacesAsList(places);
        placesList.sort((p1, p2) -> Double.compare(p2.rating, p1.rating));
        return placesList;
    }

    public double[][] getDistances(Enums.TravelMode travelMode) {
        return distance.get(travelMode);
    }

    public double[][] getDurations(Enums.TravelMode travelMode) {
        return duration.get(travelMode);
    }

    private boolean hasDistance(Enums.TravelMode travelMode) {
        return distance.containsKey(travelMode);
    }

    private boolean hasDuration(Enums.TravelMode travelMode) {
        return duration.containsKey(travelMode);
    }

    public boolean hasDistances() {
        return hasDistance(Enums.TravelMode.DRIVING) && hasDistance(Enums.TravelMode.WALKING);
    }

    public boolean hasDurations() {
        return hasDuration(Enums.TravelMode.DRIVING) && hasDuration(Enums.TravelMode.WALKING);
    }

    private void setDistance(Enums.TravelMode travelMode, double[][] distanceMatrix) {
        distance.put(travelMode, distanceMatrix);
    }

    private void setDuration(Enums.TravelMode travelMode, double[][] durationMatrix) {
        duration.put(travelMode, durationMatrix);
    }

    public void setDurations() {
        if (!hasDuration(Enums.TravelMode.DRIVING)) {
            setDuration(Enums.TravelMode.DRIVING,
                        DatabaseManager.getMatrix(this.name, Enums.TravelMode.DRIVING, Enums.TravelInfo.DURATION));
        }
        if (!hasDuration(Enums.TravelMode.WALKING)) {
            setDuration(Enums.TravelMode.WALKING,
                        DatabaseManager.getMatrix(this.name, Enums.TravelMode.WALKING, Enums.TravelInfo.DURATION));
        }
    }

    public void setDistances() {
        if (!hasDistance(Enums.TravelMode.DRIVING)) {
            setDistance(Enums.TravelMode.DRIVING,
                        DatabaseManager.getMatrix(this.name, Enums.TravelMode.DRIVING, Enums.TravelInfo.DISTANCE));
        }
        if (!hasDistance(Enums.TravelMode.WALKING)) {
            setDistance(Enums.TravelMode.WALKING,
                        DatabaseManager.getMatrix(this.name, Enums.TravelMode.WALKING, Enums.TravelInfo.DISTANCE));
        }
    }
}
