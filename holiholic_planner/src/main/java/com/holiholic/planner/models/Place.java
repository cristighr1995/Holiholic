package com.holiholic.planner.models;

import com.holiholic.planner.utils.Enums;
import com.holiholic.planner.utils.GeoPosition;
import com.holiholic.planner.utils.Interval;
import com.holiholic.planner.utils.OpeningPeriod;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

/* Place - The internal representation model for a place
 *
 */
public class Place implements Serializable, Comparable<Place> {
    private static final long serialVersionUID = 1L;

    public int id;                          // The id for internal representation
    public String name;
    public GeoPosition location;
    public int durationVisit;               // The duration for visiting in the interior
    public double rating = 0;               // The google rating
    public OpeningPeriod openingPeriod;
    public Calendar plannedHour;            // The time in the final plan when it's better to visit this place
    public Integer durationToNext;          // The time needed to reach next place
    public Enums.TravelMode modeOfTravel;  // The mode to go to next place (walking, driving etc.)
    public boolean getCarBack;              // For each place we need to know if we need to go back after the car
    public int carPlaceId = -1;             // For each place we need to know where we left the car, to return back
    public String carPlaceName = "";        // For each place we need to know the actual name of the place where we
                                            // left the car (will be used to create the response to user

    // this field is used only if we use the Vehicle mode of travel
    public boolean parkHere = false;
    public boolean eatLunch = false;
    public boolean eatDinner = false;
    public Enums.MealType mealType = Enums.MealType.UNKNOWN;
    public int parkTime;                    // The time needed for parking the vehicle (calculated in seconds)
    public int checkIns = 0;                // The actual number of times this place was chosen for the plan
    public int wantToGoNumber = 0;          // The number of times a place was chosen in the recommendation part
                                            // but not sure if the was chosen for plan or not
    public String imageUrl;                 // the image url for displaying the image

    public String vicinity = "";            // The address (if available)
    public String phoneNumber = "";         // The phone number (if available)
    public String fixedTime = "anytime";    // The time when the user wants to visit a place
    // It will be an exact hour if the user selects to fix a place
    public long needToWait = 0;             // By default if the user does not fix a place it should not wait between
                                            // places, but if we don't have time to put anything between, the user needs
                                            // to wait this amount of time

    public Set<String> tags;                // types of attractions (local, art, architecture, fun etc)
    public String type = "attraction";      // the current place type (starting_point, attraction, restaurant)
    public int distanceToNext = 0;          // the distance (in meters) until the next place

    // default constructor
    public Place() {}

    // constructor
    public Place(int id,
                 String name,
                 GeoPosition location,
                 int durationVisit,
                 double rating,
                 OpeningPeriod openingPeriod) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.durationVisit = durationVisit;
        this.rating = rating;
        this.openingPeriod = openingPeriod;
        this.tags = new HashSet<>();
    }

    /* toString - Returns a string representation of the current object
     *
     *  @return       : the serialized place
     */
    @Override
    public String toString() {
        return serializeToPlan().toString();
    }

    /* serializeToPlan - Serialize the place into a json format which is used for plan representation
     *
     *  @return       : the serialized place
     */
    public JSONObject serializeToPlan() {
        JSONObject response = new JSONObject();
        response.put("id", id);
        response.put("name", name);
        response.put("rating", rating);
        response.put("duration", durationVisit);
        response.put("type", type);
        response.put("modeOfTravel", Enums.TravelMode.serialize(modeOfTravel));
        response.put("timeToNext", durationToNext);
        response.put("distanceToNext", distanceToNext);
        response.put("plannedHour", Interval.serializeHour(plannedHour));
        response.put("getCarBack", getCarBack);
        response.put("parkHere", parkHere);
        response.put("carPlaceId", carPlaceId);
        response.put("carPlaceName", carPlaceName);
        response.put("parkTime", parkTime);
        response.put("mealType", Enums.MealType.serialize(mealType));
        response.put("latitude", location.latitude);
        response.put("longitude", location.longitude);
        return response;
    }

    /* serializeToNetwork - Serialize the place into a json format for network
     *
     *  @return       : the serialized place
     */
    public JSONObject serializeToNetwork() {
        JSONObject response = new JSONObject();
        response.put("id", id);
        response.put("name", name);
        response.put("rating", rating);
        response.put("duration", durationVisit);
        response.put("type", type);
        response.put("parkTime", parkTime);
        response.put("latitude", location.latitude);
        response.put("longitude", location.longitude);
        response.put("checkIns", checkIns);
        response.put("wantToGo", wantToGoNumber);
        response.put("imageUrl", imageUrl);
        response.put("openingHours", openingPeriod.serialize());
        response.put("tags", serializeTags());
        return response;
    }

    /* serializeTags - Serialize the tags into a json format for network
     *
     *  @return       : the serialized tags
     */
    private JSONArray serializeTags() {
        JSONArray result = new JSONArray();
        for (String tag : tags) {
            result.put(tag);
        }
        return result;
    }

    /* canVisit - Checks if the place can be visited an the specified hour
     *
     *  @return       : true/false
     *  @hour         : the hour the we want to visit the place
     */
    public boolean canVisit(Calendar hour) {
        return openingPeriod.canVisit(hour);
    }

    /* canVisit - Checks if the place can be visited given multiple days interval with each day other constraints
     *
     *  @return       : true/false
     *  @hour         : user interval
     */
    public boolean canVisit(OpeningPeriod userInterval) {
        return openingPeriod.canVisit(userInterval);
    }

    /* canVisit - Checks if the place is non stop
     *
     *  @return       : true/false
     */
    public boolean isNonStop() {
        return openingPeriod.isNonStop();
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
        copy.location = location.clone();
        copy.durationVisit = durationVisit;
        copy.rating = rating;
        copy.openingPeriod = openingPeriod != null ? openingPeriod.clone() : null;
        copy.plannedHour = plannedHour != null ? (Calendar) plannedHour.clone() : null;
        copy.tags = tags != null ? new HashSet<>(tags) : null;
        copy.checkIns = checkIns;
        copy.imageUrl = imageUrl;
        copy.vicinity = vicinity;
        copy.phoneNumber = phoneNumber;
        copy.wantToGoNumber = wantToGoNumber;
        copy.modeOfTravel = modeOfTravel;
        copy.durationToNext = durationToNext;
        copy.distanceToNext = distanceToNext;
        copy.getCarBack = getCarBack;
        copy.parkTime = parkTime;
        copy.carPlaceId = carPlaceId;
        copy.parkHere = parkHere;
        copy.eatLunch = eatLunch;
        copy.eatDinner = eatDinner;
        copy.fixedTime = fixedTime;
        copy.needToWait = needToWait;
        copy.type = type;
        copy.carPlaceName = carPlaceName;
        copy.mealType = mealType;
        return copy;
    }

    /* compareTo - Compares two places based on their fixed time
     *             Mostly used for the priority queue for the fixed places
     *
     *  @return       : the order of the places (ascending based on their fixed time)
     */
    @Override
    public int compareTo(Place other) {
        if (this.fixedTime.equals("anytime") && other.fixedTime.equals("anytime")) {
            return 0;
        } else if (this.fixedTime.equals("anytime")) {
            return 1;
        } else if (other.fixedTime.equals("anytime")) {
            return -1;
        }
        Calendar p1Hour = Interval.getHour(this.fixedTime);
        Calendar p2Hour = Interval.getHour(other.fixedTime);
        return p1Hour.compareTo(p2Hour);
    }
}
