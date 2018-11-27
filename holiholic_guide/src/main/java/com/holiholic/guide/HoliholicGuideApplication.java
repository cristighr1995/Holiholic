package com.holiholic.guide;

import com.holiholic.guide.database.DatabaseManager;
import com.holiholic.guide.guide.PeopleManager;
import com.holiholic.guide.guide.TopicsManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HoliholicGuideApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoliholicGuideApplication.class, args);
        DatabaseManager.setLogger();
        PeopleManager.setLogger();
        TopicsManager.setLogger();
    }
}
