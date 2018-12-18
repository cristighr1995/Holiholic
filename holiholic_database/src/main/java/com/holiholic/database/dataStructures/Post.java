package com.holiholic.database.dataStructures;


import org.json.JSONObject;

public class Post {
    private String pid;
    private String timestamp;
    private String city;
    private String uidAuthor;
    private String content;

    public Post() {
    }

    public Post(String pid, String timestamp, String city, String uidAuthor, String content) {
        this.pid = pid;
        this.timestamp = timestamp;
        this.city = city;
        this.uidAuthor = uidAuthor;
        this.content = content;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUidAuthor() {
        return uidAuthor;
    }

    public void setUidAuthor(String uidAuthor) {
        this.uidAuthor = uidAuthor;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Post{" +
                "timestamp='" + timestamp + '\'' +
                ", city='" + city + '\'' +
                ", uidAuthor='" + uidAuthor + '\'' +
                ", content=" + content +
                '}';
    }
}
