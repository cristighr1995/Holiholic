package com.holiholic.database.controller;

import com.holiholic.database.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class HistoryController {

    @RequestMapping(value = "/updateHistory", headers = "Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> updateHistory(@RequestBody String body) {
        try {
            return new ResponseEntity<>(DatabaseManager.updateHistory(new JSONObject(body)), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
    }
}
