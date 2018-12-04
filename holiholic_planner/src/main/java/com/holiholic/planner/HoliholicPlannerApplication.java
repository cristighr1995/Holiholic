package com.holiholic.planner;

import com.holiholic.planner.database.DatabaseManager;
import com.holiholic.planner.update.parser.ParserManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HoliholicPlannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HoliholicPlannerApplication.class, args);
        DatabaseManager.setLogger();
		ParserManager.setLogger();
	}
}
