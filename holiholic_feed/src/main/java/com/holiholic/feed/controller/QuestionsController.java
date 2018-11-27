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
                                               @RequestParam String md5Key)  {
        return new ResponseEntity<>(DatabaseManager.getQuestions(city, md5Key), HttpStatus.OK);
    }

    @RequestMapping(value = "/getQuestionDetails", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getQuestionDetails(@RequestParam String city,
                                                     @RequestParam String qid,
                                                     @RequestParam String md5KeyCurrent,
                                                     @RequestParam String md5KeyAuthor)  {
        return new ResponseEntity<>(DatabaseManager.getQuestionDetails(city,
                                                                       qid,
                                                                       md5KeyCurrent,
                                                                       md5KeyAuthor), HttpStatus.OK);
    }
}
