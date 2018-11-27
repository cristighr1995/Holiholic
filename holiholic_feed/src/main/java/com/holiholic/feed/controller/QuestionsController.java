package com.holiholic.feed.controller;

import com.holiholic.feed.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class QuestionsController {

    @RequestMapping(value = "/getQuestions", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getQuestions(@RequestParam String city,
                                               @RequestParam String uid)  {
        return new ResponseEntity<>(DatabaseManager.getQuestions(city, uid), HttpStatus.OK);
    }

    @RequestMapping(value = "/getQuestionDetails", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getQuestionDetails(@RequestParam String city,
                                                     @RequestParam String qid,
                                                     @RequestParam String uidCurrent,
                                                     @RequestParam String uidAuthor)  {
        return new ResponseEntity<>(DatabaseManager.getQuestionDetails(city,
                                                                       qid,
                                                                       uidCurrent,
                                                                       uidAuthor), HttpStatus.OK);
    }
}
