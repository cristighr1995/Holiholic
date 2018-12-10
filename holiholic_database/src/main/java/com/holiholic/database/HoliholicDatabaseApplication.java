package com.holiholic.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holiholic.database.dataStructures.Comment;
import com.holiholic.database.dataStructures.Post;
import com.holiholic.database.dataStructures.Reactions;
import com.holiholic.database.dataStructures.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

@SpringBootApplication
public class HoliholicDatabaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoliholicDatabaseApplication.class, args);
        DatabaseManager.setLogger();
       /* Post post = new Post( null,
                            null,
                            "Bucharest",
                            "617289210210110201012021",
                            null,
                            "ahshajskakskaksakskas",
                            "post",
                            "New post",
                            null
                            );
        ObjectMapper mapper = new ObjectMapper();

        String jsonStr = null;
        Post result = null;
        try {
            jsonStr = mapper.writeValueAsString(post);
            result = mapper.readValue(jsonStr, Post.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(jsonStr);
        System.out.println(result);*/
    }

}
