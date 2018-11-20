package com.holiholic.database;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HoliholicDatabaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoliholicDatabaseApplication.class, args);
        DatabaseManager.setLogger();
    }
}
