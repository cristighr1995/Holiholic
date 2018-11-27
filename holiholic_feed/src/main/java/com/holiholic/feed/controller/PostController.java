package com.holiholic.feed.controller;

import com.holiholic.feed.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PostController {

    @RequestMapping(value = "/getPosts", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getPosts(@RequestParam String uid)  {
        return new ResponseEntity<>(DatabaseManager.getPosts(uid), HttpStatus.OK);
    }

    @RequestMapping(value = "/getPostDetails", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getPostDetails(@RequestParam String city,
                                                 @RequestParam String pid,
                                                 @RequestParam String uidCurrent,
                                                 @RequestParam String uidAuthor)  {
        return new ResponseEntity<>(DatabaseManager.getPostDetails(city,
                                                                   pid,
                                                                   uidCurrent,
                                                                   uidAuthor), HttpStatus.OK);
    }
}
