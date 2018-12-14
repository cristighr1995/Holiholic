package com.holiholic.planner.models;

import com.holiholic.planner.utils.Enums;
import com.holiholic.planner.utils.GeoPosition;
import com.holiholic.planner.utils.Interval;
import com.holiholic.planner.utils.TimeFrame;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/* Place - The internal representation model for a place
 *
 */
public class Place implements Comparable<Place> {
    public int id;
    public String name;
    public GeoPosition location;
    public int durationVisit;
    public double rating = 0;
    public TimeFrame timeFrame;             // When is the place open
    public String imageUrl;
    public String vicinity = "";
    public String phone = "";
    public Set<String> tags;
    public String type = "attraction";      // the current place type (starting_point, attraction, restaurant)

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

    private Place(int id,
                 String name,
                 GeoPosition location,
                 int durationVisit,
                 double rating,
                 TimeFrame timeFrame,
                 String imageUrl,
                 String vicinity,
                 String phone,
                 Set<String> tags,
                 String type) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.durationVisit = durationVisit;
        this.rating = rating;
        this.timeFrame = timeFrame;
        this.imageUrl = imageUrl;
        this.vicinity = vicinity;
        this.phone = phone;
        this.tags = tags;
        this.type = type;
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
        copy.location = location.clone();
        copy.durationVisit = durationVisit;
        copy.rating = rating;
        copy.timeFrame = timeFrame != null ? timeFrame.clone() : null;
        copy.plannedHour = plannedHour != null ? (Calendar) plannedHour.clone() : null;
        copy.tags = tags != null ? new HashSet<>(tags) : null;
        copy.imageUrl = imageUrl;
        copy.vicinity = vicinity;
        copy.phone = phone;
        copy.travelMode = travelMode;
        copy.durationToNext = durationToNext;
        copy.distanceToNext = distanceToNext;
        copy.getCarBack = getCarBack;
        copy.carPlaceId = carPlaceId;
        copy.parkHere = parkHere;
        copy.fixedAt = fixedAt;
        copy.waitTime = waitTime;
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
        if (this.fixedAt.equals("anytime") && other.fixedAt.equals("anytime")) {
            return 0;
        } else if (this.fixedAt.equals("anytime")) {
            return 1;
        } else if (other.fixedAt.equals("anytime")) {
            return -1;
        }
        Calendar p1Hour = Interval.getHour(this.fixedAt);
        Calendar p2Hour = Interval.getHour(other.fixedAt);
        return p1Hour.compareTo(p2Hour);
    }

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
        serializedPlace.put("vicinity", vicinity);
        serializedPlace.put("phone", phone);
        serializedPlace.put("tags", serializeTags());
        serializedPlace.put("type", type);
        return serializedPlace;
    }

    /* serializeTags - Serialize the tags into a json format for network
     *
     *  @return       : the serialized tags
     */
    private JSONArray serializeTags() {
        if (tags == null || tags.isEmpty()) {
            return new JSONArray();
        }

        JSONArray result = new JSONArray();

        for (String tag : tags) {
            result.put(tag);
        }

        return result;
    }

    /* deserializeStart - Creates an internal representation of the start place that the user chose
     *
     *  @return             : the start place
     *  @startPlace         : the json that the user sent
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

    private static Place deserialize(JSONObject place) {
        try {
            int id = place.getInt("id");
            String name = place.getString("name");
            double latitude = place.getDouble("latitude");
            double longitude = place.getDouble("longitude");
            int durationVisit = place.getInt("duration");
            double rating = place.getDouble("rating");
            TimeFrame timeFrame = TimeFrame.deserialize(place.getJSONArray("timeFrame"));
            String imageUrl = place.getString("imageUrl");
            String vicinity = place.getString("vicinity");
            String phone = place.getString("phone");
            JSONArray tagsArray = place.getJSONArray("tags");
            Set<String> tags = new HashSet<>();
            for (int i = 0; i < tagsArray.length(); i++) {
                tags.add(tagsArray.getString(i));
            }
            String type = place.getString("type");

            return new Place(id, name, new GeoPosition(latitude, longitude), durationVisit, rating,
                             timeFrame, imageUrl, vicinity, phone, tags, type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
