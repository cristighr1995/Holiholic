package com.holiholic.feed.controller;

import com.holiholic.feed.database.DatabaseManager;
import com.holiholic.feed.handler.Feed;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class FeedController {
    private static final Logger LOGGER = Logger.getLogger(FeedController.class.getName());

    /* setLogger - This method should be changed when release application
     *             Sets the logger to print to console (instead of a file)
     *
     *  @return             : void
     */
    public static void setLogger() {
        // add logger handler
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);

        LOGGER.addHandler(ch);
        LOGGER.setLevel(Level.ALL);
    }

    @RequestMapping(value = "/updateFeed", headers = "Content-Type=application/json", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Boolean> updateFeed(@RequestBody String request) {

        try {
            JSONObject requestJson = new JSONObject(request);
            String uid = requestJson.getString("uid");
            String operation = requestJson.getString("operation");
            String type = requestJson.getString("type");

            LOGGER.log(Level.FINE, "New request from {0} to {1} {2}",
                    new Object[]{uid, operation, type});

            // validate uid , return false in case that uid is not valid
            if (DatabaseManager.isNotValid(uid)) {
                return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
            }

            Feed feed = Feed.Factory.getInstance(type);
            if (feed == null) {
                return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            feed.setModel(requestJson.getJSONObject("details"));

            boolean success;
            switch (operation) {
                case "add":
                    success = feed.add();
                    break;
                case "remove":
                    success = feed.remove();
                    break;
                case "edit":
                    success = feed.edit();
                    break;
                default:
                    success = false;
                    break;
            }

            if (success) {
                return new ResponseEntity<>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
    }
}
