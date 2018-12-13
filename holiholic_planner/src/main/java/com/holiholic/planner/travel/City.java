package com.holiholic.planner.travel;

import com.holiholic.planner.models.Place;
import com.holiholic.planner.utils.OpeningPeriod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* City - Singleton class to provide fast access to places from that city
 *        It will be used also to cache the most frequent cities
 *
 */
public class City {
    // because we have a singleton we will need this instance which will be created only once
    private static City instance;
    private String name;
    private Map<String, Place> places;
    private Map<String, Place> restaurants;

    // private constructor !!!
    private City(String name) {
        this.name = name;
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

    public Map<String, Place> getPlaces() {
        return places;
    }

    public void setPlaces(Map<String, Place> places) {
        this.places = places;
    }

    public Map<String, Place> getRestaurants() {
        return restaurants;
    }

    public void setRestaurants(Map<String, Place> restaurants) {
        this.restaurants = restaurants;
    }

    public Map<String, Place> getFilteredPlaces(Set<String> tags) {
        return null;
    }

    public Map<String, Place> getOpenPlaces(OpeningPeriod period) {
        return getOpenPlaces(this.places, period);
    }

    public Map<String, Place> getOpenPlaces(Map<String, Place> places, OpeningPeriod period) {
        return null;
    }

    public List<Place> getSortedPlaces(Map<String, Place> places) {
        return null;
    }
}
