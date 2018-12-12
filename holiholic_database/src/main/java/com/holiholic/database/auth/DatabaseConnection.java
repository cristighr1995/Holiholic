package com.holiholic.database.auth;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static Connection connection;

    private static DbCredentials getDbCredentials() {
        ObjectMapper objectMapper = new ObjectMapper();
        DbCredentials credentials = null;
        try {
            credentials = objectMapper.readValue(new File("db/auth/dbCredentials.json"), DbCredentials.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return credentials;
    }

    private static void init() {
        DbCredentials credentials = getDbCredentials();
        try {
            Class.forName(credentials.getJdbcDriver());
            System.out.println("Connecting to database...");
            connection = DriverManager.getConnection(credentials.getDbUrl(),
                                                    credentials.getUsername(),
                                                    credentials.getPassword());
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        if (connection == null) {
            synchronized (DatabaseConnection.class) {
                if (connection == null) {
                    init();
                }
            }
        }
        return connection;
    }
}
