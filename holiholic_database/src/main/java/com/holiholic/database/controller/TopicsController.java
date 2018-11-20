package com.holiholic.database.controller;

import com.holiholic.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TopicsController {

    @RequestMapping(value = "/getTopics", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getTopics(@RequestParam String md5Key)  {
        return new ResponseEntity<>(DatabaseManager.getTopics(md5Key), HttpStatus.OK);
    }

    @RequestMapping(value = "/updateTopics", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> updateTopics(@RequestBody String request) {
        boolean registerResult;
        try {
            registerResult = DatabaseManager.updateTopics(new JSONObject(request));
        } catch (Exception e) {
            e.printStackTrace();
            registerResult = false;
        }
        return new ResponseEntity<>(registerResult, registerResult ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}