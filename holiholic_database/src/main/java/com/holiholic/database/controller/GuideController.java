package com.holiholic.database.controller;

import com.holiholic.database.DatabaseManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class GuideController {

    @RequestMapping(value = "/getGuides", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getGuides(@RequestParam String city,
                                               @RequestParam String uid)  {
        return new ResponseEntity<>(DatabaseManager.getGuides(city, uid), HttpStatus.OK);
    }

    @RequestMapping(value = "/getGuideDetails", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getGuideDetails(@RequestParam String city,
                                                     @RequestParam String gid,
                                                     @RequestParam String uidCurrent,
                                                     @RequestParam String uidAuthor)  {
        return new ResponseEntity<>(DatabaseManager.getGuideDetails(city,
                gid,
                uidCurrent,
                uidAuthor), HttpStatus.OK);
    }
}
