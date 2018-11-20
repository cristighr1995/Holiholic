package com.holiholic.login.controllers;

import com.holiholic.login.login.LoginManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    @RequestMapping(value = "/auth", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> auth(@RequestBody String request)  {
        boolean loginResult;
        try {
            loginResult = LoginManager.login(new JSONObject(request));
        } catch (Exception e) {
            e.printStackTrace();
            loginResult = false;
        }
        return new ResponseEntity<>(loginResult, loginResult ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}
