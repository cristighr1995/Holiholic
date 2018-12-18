package com.holiholic.database.database;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Query {
    public static ResultSet select(Set<String> attributes, String tableName, List<DatabasePredicate> predicates) {
        return null;
    }

    public static void insert(String tableName, List<String> values) {

        StringBuilder valuesBuilder = new StringBuilder();
        valuesBuilder.append(values.get(0));
        for( int i = 1; i < values.size(); i++) {
            valuesBuilder.append(", " + values.get(i));
        }

        String query = "INSERT INTO " + tableName + " VALUES (" + valuesBuilder.toString() + ");";

        Runnable worker = new QueryVisitTask(query);

        QueryThreadManager.getInstance().addTask(worker);
    }

    public static void update(String tableName, Map<String, String> attributes, List<DatabasePredicate> predicates){
    }

    public static void delete(String tableName, List<DatabasePredicate> predicates) {}

}
