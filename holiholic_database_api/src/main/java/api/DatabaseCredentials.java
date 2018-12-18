package api;

public class DatabaseCredentials {
    private String username;
    private String password;
    private String databaseUrl;
    private String jdbcDriver;

    public DatabaseCredentials() {}

    public DatabaseCredentials(String username, String password, String databaseUrl, String jdbcDriver) {
        this.username = username;
        this.password = password;
        this.databaseUrl = databaseUrl;
        this.jdbcDriver = jdbcDriver;
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

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }
}
