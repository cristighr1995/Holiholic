package com.holiholic.feed.controller;

import com.holiholic.feed.database.DatabaseManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class GuideReviewController {

//    @RequestMapping(value = "/getGuideProfile", method = RequestMethod.GET)
//    @ResponseBody
//    public ResponseEntity<String> getGuideProfile(@RequestParam String city,
//                                                  @RequestParam String uid,
//                                                  @RequestParam String uidGuide)  {
//        return new ResponseEntity<>(DatabaseManager.getGuideProfile(city, uid, uidGuide), HttpStatus.OK);
//    }
//
//    @RequestMapping(value = "/getGuideProfilePostDetails", method = RequestMethod.GET)
//    @ResponseBody
//    public ResponseEntity<String> getGuideProfilePostDetails(@RequestParam String city,
//                                                             @RequestParam String gpid,
//                                                             @RequestParam String uidCurrent,
//                                                             @RequestParam String uidAuthor,
//                                                             @RequestParam String uidGuide)  {
//        return new ResponseEntity<>(DatabaseManager.getGuideProfilePostDetails(city,
//                                                                               gpid,
//                                                                               uidCurrent,
//                                                                               uidAuthor,
//                                                                               uidGuide), HttpStatus.OK);
//    }
}
