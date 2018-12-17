package com.holiholic.database.controller;

import com.holiholic.database.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PeopleController {

    @RequestMapping(value = "/getPeople", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getPeople(@RequestParam String uid)  {
        return new ResponseEntity<>(DatabaseManager.getPeople(uid), HttpStatus.OK);
    }

    @RequestMapping(value = "/updatePeople", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> updatePeople(@RequestBody String request) {
        boolean result;
        try {
            result = DatabaseManager.updatePeople(new JSONObject(request));
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return new ResponseEntity<>(result, result ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}
