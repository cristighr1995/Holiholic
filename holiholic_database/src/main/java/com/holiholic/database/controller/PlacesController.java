package com.holiholic.database.controller;

import com.holiholic.database.DatabaseManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlacesController {

    @RequestMapping(value = "/getPlaces", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getPlaces(@RequestParam String city)  {
        return new ResponseEntity<>(DatabaseManager.getPlaces(city), HttpStatus.OK);
    }
}
