package com.holiholic.database.controller;

import com.holiholic.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PostController {

    @RequestMapping(value = "/updatePost", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> updatePost(@RequestBody String request)  {
        boolean result;
        try {
            result = DatabaseManager.updateFeed(new JSONObject(request));
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return new ResponseEntity<>(result, result ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/getPosts", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getPosts(@RequestParam String md5Key)  {
        return new ResponseEntity<>(DatabaseManager.getPosts(md5Key), HttpStatus.OK);
    }

    @RequestMapping(value = "/getPostDetails", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getPostDetails(@RequestParam String city,
                                                 @RequestParam String pid,
                                                 @RequestParam String md5KeyCurrent,
                                                 @RequestParam String md5KeyAuthor)  {
        return new ResponseEntity<>(DatabaseManager.getPostDetails(city,
                                                                   pid,
                                                                   md5KeyCurrent,
                                                                   md5KeyAuthor), HttpStatus.OK);
    }
}