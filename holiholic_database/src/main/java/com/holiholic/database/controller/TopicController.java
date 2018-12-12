package com.holiholic.database.controller;

import com.holiholic.database.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TopicController {

    @RequestMapping(value = "/getTopics", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getTopics(@RequestParam String uid)  {
        return new ResponseEntity<>(DatabaseManager.getTopics(uid), HttpStatus.OK);
    }

    @RequestMapping(value = "/updateTopics", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> updateTopics(@RequestBody String request) {
        boolean result;
        try {
            result = DatabaseManager.updateTopics(new JSONObject(request));
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return new ResponseEntity<>(result, result ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}
