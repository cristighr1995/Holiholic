package com.holiholic.database.api;

import java.sql.Statement;

class QueryUpdateTask implements Runnable {
    private String query;

    QueryUpdateTask(String query) {
        this.query = query;
    }

    @Override
    public void run() {
        executeUpdate(query);
    }

    private void executeUpdate(String query) {
        DatabaseConnection connection = new DatabaseConnection();
        Statement statement = null;
        connection.open();

        System.out.println("Connection to database opened");

        if (connection.isClosed()) {
            return;
        }

        try {
            statement = connection.getConnection().createStatement();
            System.out.println("Execute \"" + query + "\"");
            System.out.println("Statement result: " + statement.executeUpdate(query));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            connection.close();
        }

        System.out.println("Connection to database closed");
    }
}
