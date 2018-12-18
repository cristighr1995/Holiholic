package com.holiholic.database.api;

public class DatabasePredicate {
    private String attribute;
    private String operation;
    private String value;

    public DatabasePredicate(String attribute, String operation, String value) {
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getOperation() {
        return operation;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return attribute + " " + operation + " " + value;
    }
}
