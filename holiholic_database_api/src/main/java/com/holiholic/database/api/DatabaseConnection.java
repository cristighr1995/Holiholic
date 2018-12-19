package com.holiholic.database.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;

class DatabaseConnection {
    private Connection connection;

    void open() {
        try {
            DatabaseCredentials credentials = getDatabaseCredentials();

            if (credentials == null) {
                return;
            }

            Class.forName(credentials.getJdbcDriver());
            connection = DriverManager.getConnection(credentials.getDatabaseUrl(),
                                                     credentials.getUsername(),
                                                     credentials.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DatabaseCredentials getDatabaseCredentials() {
        if (!new File(Constants.CREDENTIALS_CONFIG_PATH).exists()) {
            System.err.println("Database credentials path \"" + Constants.CREDENTIALS_CONFIG_PATH + "\" is invalid.");
            return null;
        }

        try {
            return new ObjectMapper().readValue(new File(Constants.CREDENTIALS_CONFIG_PATH), DatabaseCredentials.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Connection getConnection() {
        return connection;
    }

    boolean isClosed() {
        try {
            return connection == null || connection.isClosed();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
