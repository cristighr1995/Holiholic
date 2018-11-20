package com.holiholic.database.controller;

import com.holiholic.database.DatabaseManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class LoginController {

    @RequestMapping(value = "/containsUser", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Boolean> containsUser(@RequestParam String md5Key)  {
        return new ResponseEntity<>(DatabaseManager.containsUser(md5Key), HttpStatus.OK);
    }

    @RequestMapping(value = "/registerUser", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> registerUser(@RequestBody String request) {
        boolean registerResult;
        try {
            registerResult = DatabaseManager.registerUser(new JSONObject(request));
        } catch (Exception e) {
            e.printStackTrace();
            registerResult = false;
        }
        return new ResponseEntity<>(registerResult, registerResult ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}
