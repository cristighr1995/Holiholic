package com.holiholic.guide.controllers;

import com.holiholic.guide.guide.TopicsManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TopicsController {

    @RequestMapping(value = "/getTopics", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getTopics(@RequestParam String md5Key)  {
        return new ResponseEntity<>(TopicsManager.getTopics(md5Key), HttpStatus.OK);
    }

    @RequestMapping(value = "/updateTopics", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> updateTopics(@RequestBody String request)  {
        boolean updateResult;
        try {
            updateResult = TopicsManager.updateTopics(new JSONObject(request));
        } catch (Exception e) {
            e.printStackTrace();
            updateResult = false;
        }
        return new ResponseEntity<>(updateResult, updateResult ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}
