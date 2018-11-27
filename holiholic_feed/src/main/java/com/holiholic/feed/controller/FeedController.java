package com.holiholic.feed.controller;

import com.holiholic.feed.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class FeedController {

    @RequestMapping(value = "/updateFeed", headers = "Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> updateFeed(@RequestBody String request) {
        boolean result;
        try {
            result = DatabaseManager.updateFeed(new JSONObject(request));
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return new ResponseEntity<>(result, result ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}
