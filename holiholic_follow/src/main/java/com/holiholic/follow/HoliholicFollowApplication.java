package com.holiholic.follow;

import com.holiholic.follow.database.DatabaseManager;
import com.holiholic.follow.follow.PeopleManager;
import com.holiholic.follow.follow.TopicsManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HoliholicFollowApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoliholicFollowApplication.class, args);
        DatabaseManager.setLogger();
        PeopleManager.setLogger();
        TopicsManager.setLogger();
    }
}
