package com.holiholic.database.controller;

import com.holiholic.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class QuestionsController {

    @RequestMapping(value = "/updateQuestion", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> updateQuestion(@RequestBody String request)  {
        boolean result;
        try {
            result = DatabaseManager.updateQuestion(new JSONObject(request));
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return new ResponseEntity<>(result, result ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

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
                                                     @RequestParam String md5KeyQuestionAuthor)  {
        return new ResponseEntity<>(DatabaseManager.getQuestionDetails(city,
                                                                       qid,
                                                                       md5KeyCurrent,
                                                                       md5KeyQuestionAuthor), HttpStatus.OK);
    }
}
