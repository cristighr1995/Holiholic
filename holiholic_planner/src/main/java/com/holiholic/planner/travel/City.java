package com.holiholic.planner.travel;

import com.holiholic.planner.models.Place;
import org.json.JSONArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* City - Singleton class to provide fast access to places from that city
 *        It will be used also to cache the most frequent cities
 *
 */
public class City {
    // because we have a singleton we will need this instance which will be created only once
    private static City instance;

    public String name;
    public List<Place> places;
    // fast retrieval knowing the id for the desired place instance
    public Map<Integer, Place> placeMappings;
    private Map<Integer, List<Place>> restaurants;

    // The json used to retrieve the places
    public JSONArray jsonPlacesArray;

    public long totalCheckIns = 1;
    public long totalWantToGo = 1;

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

    /* getInstance - Set the city with the places and restaurants nearby
     *
     *  @return       : void
     *  @places       : a list of places from this city
     *  @restaurants  : a list of restaurants from this city
     */
    public void constructInstance(List<Place> places, Map<Integer, List<Place>> restaurants) {
        this.places = places;
        this.restaurants = restaurants;
        this.placeMappings = new HashMap<>();

        for (Place p : this.places) {
            this.placeMappings.put(p.id, p);
        }
    }

    /* getInstance - Get the best restaurant nearby the desired place
     *
     *  @return       : the best restaurant
     *  @placeId      : the id of the place where we want to find the restaurant
     */
    public Place getBestRestaurant(int placeId) {
        List<Place> nearbyRestaurants = restaurants.get(placeId);
        Place bestRestaurant = null;
        double bestRating = 0;
        double currentRating;

        for (Place restaurant : nearbyRestaurants) {
            currentRating = restaurant.rating;

            if (currentRating > bestRating) {
                bestRating = currentRating;
                bestRestaurant = restaurant;
            }
        }

        return bestRestaurant;
    }
}
