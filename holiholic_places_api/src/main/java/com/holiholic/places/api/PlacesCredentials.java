package com.holiholic.places.api;

public class PlacesCredentials {
    private String clientId;
    private String clientSecret;
    private String version;

    public PlacesCredentials() {

    }

    public PlacesCredentials(String clientId, String clientSecret, String version) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.version = version;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "&client_id=" + clientId + "&client_secret=" + clientSecret + "&v=" + version;
    }
}
