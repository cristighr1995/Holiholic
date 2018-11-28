package com.holiholic.planner.controllers;

import com.holiholic.planner.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UpdateDatabaseController {

    @RequestMapping(value = "/updatePlanner", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> updatePlanner(@RequestBody String request)  {
        try {
            return new ResponseEntity<>(DatabaseManager.updatePlanner(new JSONObject(request)), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
    }
}
