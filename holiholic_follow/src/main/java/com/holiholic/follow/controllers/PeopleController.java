package com.holiholic.follow.controllers;

import com.holiholic.follow.follow.PeopleManager;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PeopleController {

    @RequestMapping(value = "/getPeople", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getPeople(@RequestParam String uid)  {
        return new ResponseEntity<>(PeopleManager.getPeople(uid), HttpStatus.OK);
    }

    @RequestMapping(value = "/updatePeople", headers="Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> updatePeople(@RequestParam boolean follow,
                                                @RequestBody String request)  {
        boolean updateResult;
        try {
            updateResult = PeopleManager.updatePeople(new JSONObject(request), follow);
        } catch (Exception e) {
            e.printStackTrace();
            updateResult = false;
        }
        return new ResponseEntity<>(updateResult, updateResult ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}
