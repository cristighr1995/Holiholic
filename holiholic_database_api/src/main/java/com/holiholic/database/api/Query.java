package com.holiholic.database.api;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Query {

    public static SelectResult select(List<String> attributes, String tableName, List<DatabasePredicate> predicates) {
        if (tableName == null || tableName.isEmpty()) {
            return null;
        }

        StringBuilder query = new StringBuilder();
        query.append("SELECT ");

        if (attributes == null || attributes.isEmpty()) {
            query.append("*");
        } else {
            query.append(serialize(attributes, ","));
        }

        query.append(" FROM ").append(tableName);

        if (predicates != null && !predicates.isEmpty()) {
            query.append(" WHERE ").append(serialize(predicates, "AND"));
        }

        query.append(";");

        return executeQuery(query.toString());
    }

    public static void insert(String tableName, List<String> values) {
        if (tableName == null || tableName.isEmpty() || values == null || values.isEmpty()) {
            return;
        }

        String query = "INSERT INTO " + tableName + " VALUES (" + serialize(values, ",") + ");";

        ThreadManager.getInstance().addTask(new QueryUpdateTask(query));
    }

    public static void update(String tableName, Map<String, String> attributes, List<DatabasePredicate> predicates) {
        if (tableName == null || tableName.isEmpty() ||
            attributes == null || attributes.isEmpty() ||
            predicates == null || predicates.isEmpty()) {
            return;
        }

        String query = "UPDATE " + tableName + " SET " + serialize(attributes) + " WHERE " + serialize(predicates, "AND") + ";";

        ThreadManager.getInstance().addTask(new QueryUpdateTask(query));
    }

    public static void delete(String tableName, List<DatabasePredicate> predicates) {
        if (tableName == null || tableName.isEmpty() || predicates == null || predicates.isEmpty()) {
            return;
        }

        String query = "DELETE FROM " + tableName + " WHERE " + serialize(predicates, "AND") + ";";

        ThreadManager.getInstance().addTask(new QueryUpdateTask(query));
    }

    private static String serialize(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator = attributes.entrySet().iterator();
        boolean includeSeparator = false;

        while (iterator.hasNext()) {
            if (includeSeparator) {
                builder.append(", ");
            } else {
                includeSeparator = true;
            }

            Map.Entry<String, String> attribute = iterator.next();
            builder.append(attribute.getKey()).append(" = ").append(attribute.getValue());
        }

        return builder.toString();
    }

    private static <T> String serialize(List<T> values, String separator) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        builder.append(values.get(0));
        for (int i = 1; i < values.size(); i++) {
            builder.append(" ").append(separator).append(" ").append(values.get(i));
        }

        return builder.toString();
    }

    private static SelectResult executeQuery(String query) {
        DatabaseConnection connection = new DatabaseConnection();
        Statement statement;
        ResultSet resultSet;

        connection.open();
        System.out.println("Connection to database opened");
        if (connection.isClosed()) {
            return null;
        }

        try {
            statement = connection.getConnection().createStatement();
            resultSet = statement.executeQuery(query);
            System.out.println("Execute \"" + query + "\"");
            System.out.println("Statement result set size: " + resultSet.getFetchSize());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        System.out.println("Return select result");

        return new SelectResult(resultSet, connection, statement);
    }
}
