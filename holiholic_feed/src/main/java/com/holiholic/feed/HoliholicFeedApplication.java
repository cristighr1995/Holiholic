package com.holiholic.feed;

import com.holiholic.feed.database.DatabaseManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HoliholicFeedApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoliholicFeedApplication.class, args);
        DatabaseManager.setLogger();
    }
}
