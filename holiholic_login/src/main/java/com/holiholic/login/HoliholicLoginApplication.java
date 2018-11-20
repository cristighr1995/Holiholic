package com.holiholic.login;

import com.holiholic.login.database.DatabaseManager;
import com.holiholic.login.login.LoginManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HoliholicLoginApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoliholicLoginApplication.class, args);
        DatabaseManager.setLogger();
        LoginManager.setLogger();
    }
}
