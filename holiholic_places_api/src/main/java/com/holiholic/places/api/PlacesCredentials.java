package com.holiholic.places.api;

public class PlacesCredentials {
    private String clientId;
    private String clientSecret;
    private String version;
    private String googleApiKey;

    public PlacesCredentials() {

    }

    public PlacesCredentials(String clientId, String clientSecret, String version, String googleApiKey) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.version = version;
        this.googleApiKey = googleApiKey;
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

    public String getGoogleApiKey() {
        return googleApiKey;
    }

    public void setGoogleApiKey(String googleApiKey) {
        this.googleApiKey = googleApiKey;
    }

    @Override
    public String toString() {
        return "client_id=" + clientId + "&client_secret=" + clientSecret + "&v=" + version;
    }
}
