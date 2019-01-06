package com.holiholic.database.api;

import java.sql.ResultSet;
import java.sql.Statement;

public class SelectResult {
    private ResultSet resultSet;
    private DatabaseConnection databaseConnection;
    private Statement statement;

    public SelectResult(ResultSet resultSet, DatabaseConnection databaseConnection, Statement statement) {
        this.resultSet = resultSet;
        this.databaseConnection = databaseConnection;
        this.statement = statement;
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }

    public Statement getStatement() {
        return statement;
    }

    public void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        databaseConnection.close();
    }
}
