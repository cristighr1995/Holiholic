package com.holiholic.planner.models;

import com.holiholic.places.api.PlaceCategory;
import com.holiholic.planner.utils.Enums;
import com.holiholic.planner.utils.GeoPosition;
import com.holiholic.planner.utils.TimeFrame;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

/* Place - The internal representation model for a place
 *
 */
public class Place implements Comparable<Place> {
    public int id;
    public String name;
    private String description;
    private String imageUrl;
    public double rating = 0;
    public PlaceCategory placeCategory;
    public int durationVisit;
    public GeoPosition location;
    public TimeFrame timeFrame;             // When is the place open

    // default values before planning
    public LocalDateTime plannedHour;       // When is the place scheduled
    public int durationToNext = 0;
    public int distanceToNext = 0;
    public Enums.TravelMode travelMode = Enums.TravelMode.UNKNOWN;
    public boolean getCarBack = false;      // True when driving and parked to visit places nearby
    public int carPlaceId = -1;             // The id of the place where parked
    public String carPlaceName = "";        // The name of the place where parked
    public boolean parkHere = false;
    public Enums.MealType mealType = Enums.MealType.UNKNOWN;
    public String fixedAt = "anytime";      // The time when the user wants to visit a place
    public LocalDateTime fixedTime;
    public long waitTime = 0;               // how much to wait between visiting 2 places
    public boolean interior = false;        // specify if user wants to enter place

    private Place() {

    }

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

    /* canVisit - Checks if the place can be visited an the given time
     *
     *  @return       : true/false
     *  @time         : time to check if the place can be visited
     */
    public boolean canVisit(LocalDateTime time) {
        return !interior || timeFrame.canVisit(time);
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

    /* copy - Returns a new reference making a deep copy of the current object
     *
     *  @return       : copy of the current object
     */
    public Place copy() {
        Place other = new Place();
        other.id = id;
        other.name = name;
        other.description = description;
        other.imageUrl = imageUrl;
        other.rating = rating;
        other.placeCategory = placeCategory;
        other.durationVisit = durationVisit;
        other.location = location;
        other.timeFrame = timeFrame;
        other.plannedHour = plannedHour;
        other.durationToNext = durationToNext;
        other.distanceToNext = distanceToNext;
        other.travelMode = travelMode;
        other.getCarBack = getCarBack;
        other.carPlaceId = carPlaceId;
        other.carPlaceName = carPlaceName;
        other.parkHere = parkHere;
        other.mealType = mealType;
        other.fixedAt = fixedAt;
        other.fixedTime = fixedTime;
        other.waitTime = waitTime;
        other.interior = interior;
        return other;
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
        return this.fixedTime.compareTo(other.fixedTime);
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

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        Place other = (Place) o;
        return this.id == other.id;
    }
}
