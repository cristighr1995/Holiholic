package com.holiholic.database.dataStructures;

public class User {

    private String imageUrl;
    private String name;
    private String email;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User(String imageUrl, String name, String email) {
        this.imageUrl = imageUrl;
        this.name = name;
        this.email = email;
    }
}
