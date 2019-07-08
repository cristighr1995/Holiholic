package com.holiholic.feed;

import com.holiholic.feed.controller.FeedController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HoliholicFeedApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoliholicFeedApplication.class, args);
        FeedController.setLogger();
    }
}
