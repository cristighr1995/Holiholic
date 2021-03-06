package com.holiholic.planner.controllers;

import com.holiholic.planner.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlacesController {

    @RequestMapping(value = "/getPlaces", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> getPlaces(@RequestBody String body)  {
        try {
            return new ResponseEntity<>(DatabaseManager.getPlaces(new JSONObject(body)), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("[]", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/getAvailableCities", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getAvailableCities()  {
        try {
            return new ResponseEntity<>(DatabaseManager.getAvailableCitiesSerialized(), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("[]", HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/cacheItineraries", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> cacheItineraries(@RequestParam String cityName)  {
        try {
            return new ResponseEntity<>(DatabaseManager.cacheItineraries(cityName), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
    }
}


