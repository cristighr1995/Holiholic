package com.holiholic.database.database;

public class DatabasePredicate {
    private String attribute;
    private String operation; // ex: “=”, “<=”, “IS NOT”
    private String value;

    public DatabasePredicate() {
    }

    public DatabasePredicate(String attribute, String operation, String value) {
        this.attribute = attribute;
        this.operation = operation;
        this.value = value;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
