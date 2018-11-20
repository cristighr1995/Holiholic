package com.holiholic.planner.planner;

import com.holiholic.planner.database.DatabaseManager;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.travel.City;
import com.holiholic.planner.utils.CloneFactory;
import com.holiholic.planner.utils.Enums;
import com.holiholic.planner.utils.GeoPosition;
import com.holiholic.planner.utils.OpeningPeriod;
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

    /* getPlacesListForPlanner - Decode the user request to generate a list of places that he wants to visit
     *
     *  @return             : a list of places (internal representation) that the user wants to visit
     *  @city               : the city instance
     *  @planner            : the planner instance which will be used to generate the actual plan
     *  @placesToPlan       : the json request which contains information about what places to decode
     */
    private static List<Place> getPlacesListForPlanner(City city, Planner planner, JSONArray placesToPlan) {
        List<Place> places = new ArrayList<>();

        for (int i = 0; i < placesToPlan.length(); i++) {
            JSONObject place = placesToPlan.getJSONObject(i);

            int id = place.getInt("id");
            // need this clone to avoid concurrent modifications
            Place p = CloneFactory.clone(planner.getPlaceMappings().get(id));

            if (place.getBoolean("isFixed")) {
                p.fixedTime = place.getString("fixedAt");
            }

            p.durationVisit = place.getInt("duration");
            // update the time in the city place
            int oldTime = city.placeMappings.get(id).durationVisit;
            city.placeMappings.get(id).durationVisit = (oldTime + p.durationVisit) / 2;

            try {
                synchronized (PlanManager.class) {
                    // put the duration directly in the stored json
                    city.jsonPlacesArray.getJSONObject(p.id).put("duration", city.placeMappings.get(id).durationVisit);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            places.add(p);
        }

        return places;
    }

    /* updateCheckIns - Increment the counter for the places that are included in the final plan
     *
     *  @return             : void
     *  @city               : the city instance
     *  @placesToPlan       : the list of places that were included in the final plan
     */
    private static synchronized void updateCheckIns(City city, List<Place> plannedPlaces) {
        for (Place p : plannedPlaces) {
            if (!city.placeMappings.containsKey(p.id)) {
                continue;
            }

            Place cityPlace = city.placeMappings.get(p.id);
            cityPlace.checkIns++;

            // update also the total number of check-ins
            city.totalCheckIns++;

            try {
                // Put the number of check-ins directly in the stored json
                city.jsonPlacesArray.getJSONObject(p.id).put("checkIns", cityPlace.checkIns);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* updateWantToGoNumber - Increment the counter for the desired places before being included in the final plan
     *
     *  @return             : void
     *  @city               : the city instance
     *  @placesToPlan       : the list of places that the user wants to plan
     */
    private static synchronized void updateWantToGoNumber(City city, List<Place> placesToPlan) {
        for (Place p : placesToPlan) {
            if (!city.placeMappings.containsKey(p.id)) {
                continue;
            }

            Place cityPlace = city.placeMappings.get(p.id);
            cityPlace.wantToGoNumber++;

            // update also the total number of want to go
            city.totalWantToGo++;

            try {
                // Put the number of want-to-go directly in the stored json
                city.jsonPlacesArray.getJSONObject(p.id).put("wantToGo", cityPlace.wantToGoNumber);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* decodeStartPlace - Creates an internal representation of the start place that the user chose
     *
     *  @return             : the start place
     *  @startPlace         : the json that the user sent
     */
    private static Place decodeStartPlace(JSONObject startPlace) {
        return new Place(
                -1,
                startPlace.getString("name"),
                new GeoPosition(startPlace.getDouble("latitude"), startPlace.getDouble("longitude")),
                0,
                startPlace.getDouble("rating"),
                null);
    }

    /* generatePlan - Generate the plan given a json request, this method is not exposed!
     *
     *  @return             : the serialized plan
     *  @jsonRequest        : the body of the HTTP POST request
     */
    private static String generatePlan(JSONObject jsonRequest) {
        // get the city from the database
        City city = DatabaseManager.getCity(jsonRequest.getString("city"));
        LOGGER.log(Level.FINE, "New request to generate a plan in city ({0})", city.name);

        // store pointer to preferences
        JSONObject jsonPreferences = jsonRequest.getJSONObject("preferences");

        // get the visiting interval
        OpeningPeriod openingPeriod = DatabaseManager
                                      .deserializeOpeningPeriod(jsonPreferences.getJSONArray("visitingInterval"));

        // get the mode of travel
        String modeOfTravel = jsonPreferences.getString("modeOfTravel");

        // create the planner
        Planner planner = new Planner(city, openingPeriod, Enums.TravelMode.deserialize(modeOfTravel));
        // set preference heuristic
        planner.setPreferenceHeuristic(jsonRequest.getJSONObject("preferences").getDouble("preferenceHeuristic"));

        // set dinner and lunch
        if (jsonPreferences.getBoolean("dinner")) {
            planner.setDinner();
        }
        if (jsonPreferences.getBoolean("lunch")) {
            planner.setLunch();
        }

        // construct the start place from the json request
        planner.setStartPlace(decodeStartPlace(jsonRequest.getJSONObject("startPlace")));

        // get information about what places to visit
        JSONArray jsonPlacesToPlan = jsonRequest.getJSONArray("placesToPlan");
        // decode json into internal representation
        List<Place> placesToPlan = getPlacesListForPlanner(city, planner, jsonPlacesToPlan);

        // update the want-to-go number before the actual plan
        // these are the places that the user is interested in planning
        updateWantToGoNumber(city, placesToPlan);

        LOGGER.log(Level.FINE, "Started generating a plan for city - {0} -", city.name);
        // generate the actual plan that will be returned to user after serialization
        List<List<Place>> plan = planner.getPlan(placesToPlan);

        // update the number of check-ins for each place in the final plan
        for (List<Place> plannedPlaces : plan) {
            updateCheckIns(city, plannedPlaces);
        }

        // after we are done, update the database
        // this will write to file the updates
        DatabaseManager.updateCity(city);

        LOGGER.log(Level.FINE, "Successfully generated plan for city ({0})", city.name);
        LOGGER.log(Level.FINE, "Started serialization of the plan for city ({0})", city.name);
        // serialize the response
        return DatabaseManager.serializePlan(plan);
    }

    /* getPlanFromUserRequest - Given the user request, generate a plan (multiple itineraries) for user
     *
     *  @return             : the serialized plan
     *  @request            : the body of the HTTP POST request
     */
    public static String getPlanFromUserRequest(String request) {
        return generatePlan(new JSONObject(request));
    }
}