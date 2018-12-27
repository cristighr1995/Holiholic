package com.holiholic.planner.models;

import com.holiholic.places.api.PlaceCategory;
import com.holiholic.planner.utils.Enums;
import com.holiholic.planner.utils.GeoPosition;
import com.holiholic.planner.utils.Interval;
import com.holiholic.planner.utils.TimeFrame;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.*;

/* Place - The internal representation model for a place
 *
 */
public class Place implements Comparable<Place> {
    public int id;
    public String name;
    public String description;
    public String imageUrl;
    public double rating = 0;
    public PlaceCategory placeCategory;
    public int durationVisit;
    public GeoPosition location;
    public TimeFrame timeFrame;             // When is the place open

    public Calendar plannedHour;            // When is the place scheduled
    public int durationToNext = 0;
    public int distanceToNext = 0;
    public Enums.TravelMode travelMode;
    public boolean getCarBack;              // True when driving and parked to visit places nearby
    public int carPlaceId = -1;             // The id of the place where parked
    public String carPlaceName = "";        // The name of the place where parked
    public boolean parkHere = false;
    public Enums.MealType mealType = Enums.MealType.UNKNOWN;
    public String fixedAt = "anytime";      // The time when the user wants to visit a place
    public long waitTime = 0;               // how much to wait between visiting 2 places

    // default constructor
    private Place() {}

    // constructor
    private Place(int id, String name, GeoPosition location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }

    public Place(int id, String name, String description, String imageUrl, double rating, PlaceCategory placeCategory,
                 int durationVisit, GeoPosition location, TimeFrame timeFrame) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.placeCategory = placeCategory;
        this.durationVisit = durationVisit;
        this.location = location;
        this.timeFrame = timeFrame;
    }

    /* toString - Returns a string representation of the current object
     *
     *  @return       : the serialized place
     */
    @Override
    public String toString() {
        return serialize().toString();
    }

    /* canVisit - Checks if the place can be visited an the specified hour
     *
     *  @return       : true/false
     *  @hour         : the hour the we want to visit the place
     */
    public boolean canVisit(Calendar hour) {
        return timeFrame.canVisit(hour);
    }

    /* canVisit - Checks if the place can be visited given multiple days interval with each day other constraints
     *
     *  @return       : true/false
     *  @hour         : user interval
     */
    public boolean canVisit(TimeFrame userInterval) {
        return timeFrame.canVisit(userInterval);
    }

    /* canVisit - Checks if the place is non stop
     *
     *  @return       : true/false
     */
    public boolean isNonStop() {
        return timeFrame.isNonStop();
    }

    /* clone - Clone the current object and returns a new copy of it
     *         Need to be very careful when we add a new property because we also need to modify this method !!!!
     *
     *  @return       : a clone of the current object
     */
    @Override
    public Place clone() {
        Place copy = new Place();
        copy.id = id;
        copy.name = name;
        copy.description = description;
        copy.imageUrl = imageUrl;
        copy.rating = rating;
        copy.placeCategory = placeCategory;
        copy.durationVisit = durationVisit;
        copy.location = location.clone();
        copy.timeFrame = timeFrame != null ? timeFrame.clone() : null;
        copy.plannedHour = plannedHour != null ? (Calendar) plannedHour.clone() : null;
        copy.durationToNext = durationToNext;
        copy.distanceToNext = distanceToNext;
        copy.travelMode = travelMode;
        copy.getCarBack = getCarBack;
        copy.carPlaceId = carPlaceId;
        copy.carPlaceName = carPlaceName;
        copy.parkHere = parkHere;
        copy.mealType = mealType;
        copy.fixedAt = fixedAt;
        copy.waitTime = waitTime;
        return copy;
    }

    /* compareTo - Compares two places based on their fixed time
     *             Mostly used for the priority queue for the fixed places
     *
     *  @return       : the order of the places (ascending based on their fixed time)
     */
    @Override
    public int compareTo(@Nullable Place other) {
        if (other == null) {
            return -1;
        }
        if (this.fixedAt.equals("anytime") && other.fixedAt.equals("anytime")) {
            return 0;
        } else if (this.fixedAt.equals("anytime")) {
            return 1;
        } else if (other.fixedAt.equals("anytime")) {
            return -1;
        }
        Calendar p1Hour = Interval.getHour(this.fixedAt, 1);
        Calendar p2Hour = Interval.getHour(other.fixedAt, 1);
        return p1Hour.compareTo(p2Hour);
    }

    /* serialize - Serialize the place into a json object format
     *             This is general information about a place
     *
     *  @return       : the serialized place
     */
    public JSONObject serialize() {
        JSONObject serializedPlace = new JSONObject();
        serializedPlace.put("id", id);
        serializedPlace.put("name", name);
        serializedPlace.put("latitude", location.latitude);
        serializedPlace.put("longitude", location.longitude);
        serializedPlace.put("duration", durationVisit);
        serializedPlace.put("rating", rating);
        serializedPlace.put("timeFrame", timeFrame.serialize());
        serializedPlace.put("imageUrl", imageUrl);
        serializedPlace.put("description", description);
        serializedPlace.put("category", placeCategory.getTopic());
        return serializedPlace;
    }

    /* deserializeStart - Creates an internal representation of the start place that the user chose
     *
     *  @return             : the start place
     *  @place              : information about the start place
     */
    public static Place deserializeStart(JSONObject place) {
        try {
            int id = -1;
            String name = place.getString("name");
            double latitude = place.getDouble("latitude");
            double longitude = place.getDouble("longitude");
            return new Place(id, name, new GeoPosition(latitude, longitude));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
