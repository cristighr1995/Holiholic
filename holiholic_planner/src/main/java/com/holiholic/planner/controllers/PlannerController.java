package com.holiholic.planner.controllers;

import com.holiholic.planner.planner.PlanManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlannerController {

    @RequestMapping(value = "/planner", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> generatePlan(@RequestBody String request)  {
        return new ResponseEntity<>(PlanManager.getPlanFromUserRequest(request), HttpStatus.OK);
    }
}
