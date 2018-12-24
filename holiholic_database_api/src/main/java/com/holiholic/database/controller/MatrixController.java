package com.holiholic.database.controller;

import com.holiholic.database.DatabaseManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class MatrixController {

    @RequestMapping(value = "/getMatrix", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getMatrix(@RequestParam String city,
                                            @RequestParam String travelMode,
                                            @RequestParam String travelInfo)  {
        return new ResponseEntity<>(DatabaseManager.getMatrix(city,travelMode, travelMode), HttpStatus.OK);
    }
}
