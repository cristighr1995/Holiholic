package com.holiholic.planner.controllers;

import com.holiholic.planner.planner.PlanManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlannerController {

    @RequestMapping(value = "/getPlan", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> getPlan(@RequestBody String request)  {
        try {
            return new ResponseEntity<>(PlanManager.getPlan(new JSONObject(request)), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("[]", HttpStatus.BAD_REQUEST);
        }
    }
}
