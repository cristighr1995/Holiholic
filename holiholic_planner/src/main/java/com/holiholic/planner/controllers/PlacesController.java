package com.holiholic.planner.controllers;

import com.holiholic.planner.database.DatabaseManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlacesController {

    @RequestMapping(value = "/places", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> getPlacesRecommendation(@RequestBody String request)  {
        return new ResponseEntity<>(DatabaseManager.getPlacesHttpRequest(request), HttpStatus.OK);
    }
}


