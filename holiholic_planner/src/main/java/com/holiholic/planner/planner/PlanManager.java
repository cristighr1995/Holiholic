package com.holiholic.planner.planner;

import com.holiholic.planner.database.DatabaseManager;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.travel.City;
import com.holiholic.planner.utils.CloneFactory;
import com.holiholic.planner.utils.Enums;
import com.holiholic.planner.utils.TimeFrame;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* PlanManager - Deals with handling the request between clients and actual planner class
 *
 */
public class PlanManager {
    private static final Logger LOGGER = Logger.getLogger(PlanManager.class.getName());

    /* setLogger - Configure the logger
     *
     *  @return             : void
     */
    public static void setLogger() {
        // add logger handler
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);

        LOGGER.addHandler(ch);
        LOGGER.setLevel(Level.ALL);
    }

    /* getPlaces - Decode the user request to generate a list of places that he wants to visit
     *
     *  @return             : a list of places (internal representation) that the user wants to visit
     *  @city               : the city instance
     *  @placesFromRequest  : the json request which contains information about what places to decode
     */
    private static List<Place> getPlaces(City city, JSONArray placesFromRequest) {
        List<Place> places = new ArrayList<>();

        for (int i = 0; i < placesFromRequest.length(); i++) {
            JSONObject placeInfo = placesFromRequest.getJSONObject(i);
            int id = placeInfo.getInt("id");
            // need this clone to avoid concurrent modifications
            Place place = CloneFactory.clone(city.getPlaces().get(id));

            if (placeInfo.getBoolean("isFixed")) {
                place.fixedAt = placeInfo.getString("fixedAt");
            }

            place.durationVisit = placeInfo.getInt("duration");
            // update the time in the city place
            int oldTime = city.getPlaces().get(id).durationVisit;

            synchronized (PlanManager.class) {
                city.getPlaces().get(id).durationVisit = (oldTime + place.durationVisit) / 2;
            }

            places.add(place);
        }

        return places;
    }

    /* getPlan - Generate the plan given a json request, this method is not exposed!
     *
     *  @return             : the serialized plan
     *  @body               : the body of the HTTP POST request
     */
    public static String getPlan(JSONObject body) {
        try {
            String cityName = body.getString("city");
            String uid = body.getString("uid");

            if (!DatabaseManager.containsUser(uid)) {
                LOGGER.log(Level.FINE, "User {0} does not exist in the system and can not generate a plan in {1} city",
                           new Object[]{uid, cityName});
                return "[]";
            }

            JSONObject preferences = body.getJSONObject("preferences");
            TimeFrame timeFrame = TimeFrame.deserialize(preferences.getJSONArray("timeFrame"));

            if (timeFrame.getOpenDays().isEmpty()) {
                LOGGER.log(Level.FINE, "Invalid request from user {0} to generate a plan in {1} city, because time frame is missing",
                           new Object[]{uid, cityName});
                return "[]";
            }

            LOGGER.log(Level.FINE, "New request from user {0} to generate a plan in {1} city",
                       new Object[]{uid, cityName});

            City city = DatabaseManager.getCity(cityName);
            Enums.TravelMode travelMode = Enums.TravelMode.deserialize(preferences.getString("travelMode"));
            double heuristicValue = preferences.getDouble("heuristicValue");
            boolean dinner = preferences.getBoolean("dinner");
            boolean lunch = preferences.getBoolean("lunch");
            Place start = Place.deserializeStart(body.getJSONObject("start"));
            List<Place> places = getPlaces(city, body.getJSONArray("places"));

            if (!city.hasDurations()) {
                city.setDurations();
            }
            if (!city.hasDistances()) {
                city.setDistances();
            }

            // create the planner
            Planner planner = new Planner(city, timeFrame, travelMode);
            planner.setHeuristicValue(heuristicValue);
            planner.setStart(start);

            if (lunch) {
                planner.setLunch();
            }
            if (dinner) {
                planner.setDinner();
            }

            LOGGER.log(Level.FINE, "Generate a plan for user {0} in {1} city having {2} places",
                       new Object[]{uid, cityName, places.size()});

            return Planner.serialize(planner.getPlan(places)).toString(2);
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }
}
