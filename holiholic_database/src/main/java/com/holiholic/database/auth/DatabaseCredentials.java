package com.holiholic.database.auth;

public class DatabaseCredentials {
    private String username;
    private String password;
    private String dbUrl;
    private String jdbcDriver;

    public DatabaseCredentials(String username, String password, String dbUrl, String jdbcDriver) {
        this.username = username;
        this.password = password;
        this.dbUrl = dbUrl;
        this.jdbcDriver = jdbcDriver;
    }

    public DatabaseCredentials() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }
}
