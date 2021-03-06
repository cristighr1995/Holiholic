package com.holiholic.planner.travel;

import com.holiholic.places.api.PlaceCategory;
import com.holiholic.planner.database.DatabaseManager;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.utils.Enums;
import com.holiholic.planner.utils.TimeFrame;

import java.time.LocalDateTime;
import java.util.*;

/* City - Singleton class to provide fast access to places from that city
 *        It will be used also to cache the most frequent cities
 *
 */
public class City {
    private String name;
    private Map<Integer, Place> places;
    private Map<Enums.TravelMode, double[][]> distance;
    private Map<Enums.TravelMode, double[][]> duration;

    public City(String name) {
        this.name = name;
        this.distance = new HashMap<>();
        this.duration = new HashMap<>();
    }

    /* getName - Get the city name
     *
     *  @return         : the city name
     */
    public String getName() {
        return name;
    }

    /* getPlaces - Get the places from the city
     *
     *  @return         : places
     */
    public Map<Integer, Place> getPlaces() {
        return places;
    }

    /* getPlacesAsList - Get the places in a list format
     *
     *  @return         : places
     */
    public List<Place> getPlacesAsList() {
        return getPlacesAsList(getPlaces());
    }

    /* getPlacesAsList - Get the places in a list format
     *
     *  @return         : places
     *  @places         : smaller amount of places
     */
    List<Place> getPlacesAsList(Map<Integer, Place> places) {
        List<Place> placesList = new ArrayList<>();

        for (Map.Entry<Integer, Place> placeEntry : places.entrySet()) {
            placesList.add(placeEntry.getValue());
        }

        return placesList;
    }

    /* setPlaces - Set places for this city
     *
     *  @return         : void
     *  @places         : places
     */
    public void setPlaces(Map<Integer, Place> places) {
        this.places = places;
    }

    /* getFilteredPlaces - Filter places by tags
     *
     *  @return         : filtered places
     *  @categories     : place categories
     */
    public Map<Integer, Place> getFilteredPlaces(Set<String> categories) {
        if (categories.size() == 1) {
            if (categories.iterator().next().equals("All")) {
                return this.places;
            }
        }
        Map<Integer, Place> filteredPlaces = new HashMap<>();

        for (Map.Entry<Integer, Place> placeEntry : getPlaces().entrySet()) {
            PlaceCategory placeCategory = placeEntry.getValue().placeCategory;
            if (placeCategory == null || placeCategory.isStartingPoint()) {
                continue;
            }

            if (categories.contains(placeCategory.getTopic())) {
                filteredPlaces.put(placeEntry.getKey(), placeEntry.getValue());
            }
        }

        return filteredPlaces;
    }

    /* getOpenPlaces - Get all open places in the specified time frame
     *
     *  @return             : filtered places
     *  @timeFrame          : the time frame when to check open places
     */
    public Map<Integer, Place> getOpenPlaces(TimeFrame timeFrame) {
        return getOpenPlaces(this.places, timeFrame);
    }

    /* getOpenPlaces - Get all open places in the specified time frame
     *
     *  @return             : filtered places
     *  @places             : places to filter
     *  @timeFrame          : the time frame when to check open places
     */
    public Map<Integer, Place> getOpenPlaces(Map<Integer, Place> places, TimeFrame timeFrame) {
        Map<Integer, Place> openPlaces = new HashMap<>();

        for (Map.Entry<Integer, Place> placeEntry : places.entrySet()) {
            if (placeEntry.getValue().canVisit(timeFrame)) {
                openPlaces.put(placeEntry.getKey(), placeEntry.getValue());
            }
        }

        return openPlaces;
    }

    /* getSortedPlaces - Sort places based on their rating
     *
     *  @return             : sorted places
     *  @places             : places
     */
    public List<Place> getSortedPlaces(Map<Integer, Place> places) {
        List<Place> placesList = getPlacesAsList(places);
        placesList.sort((p1, p2) -> Double.compare(p2.rating, p1.rating));
        return placesList;
    }

    /* getDistances - Get distance matrix based on the travel mode
     *
     *  @return             : distance matrix
     *  @travelMode         : travel mode
     */
    public double[][] getDistances(Enums.TravelMode travelMode) {
        return distance.get(travelMode);
    }

    /* getDurations - Get duration matrix based on the travel mode
     *
     *  @return             : duration matrix
     *  @travelMode         : travel mode
     */
    public double[][] getDurations(Enums.TravelMode travelMode) {
        return duration.get(travelMode);
    }

    /* hasDistance - Check if the distance matrix is cached
     *
     *  @return             : true or false
     *  @travelMode         : travel mode
     */
    private boolean hasDistance(Enums.TravelMode travelMode) {
        return distance.containsKey(travelMode);
    }

    /* hasDuration - Check if the duration matrix is cached
     *
     *  @return             : true or false
     *  @travelMode         : travel mode
     */
    private boolean hasDuration(Enums.TravelMode travelMode) {
        return duration.containsKey(travelMode);
    }

    /* hasDistances - Check if the distance matrix is cached for both modes of travel
     *
     *  @return             : true or false
     */
    public boolean hasDistances() {
        return hasDistance(Enums.TravelMode.DRIVING) && hasDistance(Enums.TravelMode.WALKING);
    }

    /* hasDurations - Check if the duration matrix is cached for both modes of travel
     *
     *  @return             : true or false
     */
    public boolean hasDurations() {
        return hasDuration(Enums.TravelMode.DRIVING) && hasDuration(Enums.TravelMode.WALKING);
    }

    /* setDistance - Set a distance matrix based on the travel mode
     *
     *  @return             : void
     *  @travelMode         : travel mode
     *  @distanceMatrix     : distance matrix
     */
    private void setDistance(Enums.TravelMode travelMode, double[][] distanceMatrix) {
        distance.put(travelMode, distanceMatrix);
    }

    /* setDuration - Set a duration matrix based on the travel mode
     *
     *  @return             : void
     *  @travelMode         : travel mode
     *  @distanceMatrix     : duration matrix
     */
    private void setDuration(Enums.TravelMode travelMode, double[][] durationMatrix) {
        duration.put(travelMode, durationMatrix);
    }

    /* setDurations - Set duration matrix for both modes of travel
     *
     *  @return             : void
     */
    public void setDurations() {
        if (!hasDuration(Enums.TravelMode.DRIVING)) {
            setDuration(Enums.TravelMode.DRIVING, DatabaseManager.getMatrix(this.name, Enums.TravelMode.DRIVING,
                    Enums.TravelInfo.DURATION, places.size()));
        }
        if (!hasDuration(Enums.TravelMode.WALKING)) {
            setDuration(Enums.TravelMode.WALKING, DatabaseManager.getMatrix(this.name, Enums.TravelMode.WALKING,
                    Enums.TravelInfo.DURATION, places.size()));
        }
    }

    /* setDistances - Set distance matrix for both modes of travel
     *
     *  @return             : void
     */
    public void setDistances() {
        if (!hasDistance(Enums.TravelMode.DRIVING)) {
            setDistance(Enums.TravelMode.DRIVING, DatabaseManager.getMatrix(this.name, Enums.TravelMode.DRIVING,
                    Enums.TravelInfo.DISTANCE, places.size()));
        }
        if (!hasDistance(Enums.TravelMode.WALKING)) {
            setDistance(Enums.TravelMode.WALKING, DatabaseManager.getMatrix(this.name, Enums.TravelMode.WALKING,
                    Enums.TravelInfo.DISTANCE, places.size()));
        }
    }

    /* getTopRestaurants - Get the top restaurants from this city which are open
     *
     *  @return             : list with restaurants, already sorted
     *  @limit              : the limit for the restaurants count
     *  @time               : restaurants open at this time
     */
    public List<Place> getTopRestaurants(int limit, LocalDateTime time) {
        // min heap
        PriorityQueue<Place> pq = new PriorityQueue<>(Comparator.comparingDouble(p -> p.rating));

        for (Map.Entry<Integer, Place> placeEntry : getPlaces().entrySet()) {
            if (placeEntry.getValue().placeCategory.getTopic().equals("Restaurants") &&
                placeEntry.getValue().canVisit(time)) {
                if (pq.size() < limit) {
                    pq.add(placeEntry.getValue());

                } else {
                    if (!pq.isEmpty() && pq.peek().rating < placeEntry.getValue().rating) {
                        pq.poll();
                        pq.add(placeEntry.getValue());
                    }
                }
            }
        }

        List<Place> topRestaurants = new ArrayList<>(pq);
        // reverse to higher rating
        Collections.reverse(topRestaurants);

        return topRestaurants;
    }
}
