package com.holiholic.database.dataStructures;

public class User {

    private String imageUrl;
    private String name;
    private String email;
    private int uid;

    public User(String imageUrl, String name, String email, int uid) {
        this.imageUrl = imageUrl;
        this.name = name;
        this.email = email;
        this.uid = uid;
    }

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

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }
}
