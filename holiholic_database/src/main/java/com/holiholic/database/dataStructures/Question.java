package com.holiholic.database.dataStructures;

import java.util.Date;
import java.util.List;

public class Question {
    private Date timestamp;
    private List<Comment> comments;
    private String city;
    private String author;
    private String uidAuthor;
    private String type;
    private String title;
    private String qid;
    private Reactions reactions;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getUidAuthor() {
        return uidAuthor;
    }

    public void setUidAuthor(String uidAuthor) {
        this.uidAuthor = uidAuthor;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getQid() {
        return qid;
    }

    public void setQid(String qid) {
        this.qid = qid;
    }

    public Reactions getReactions() {
        return reactions;
    }

    public void setReactions(Reactions reactions) {
        this.reactions = reactions;
    }

    public Question() {
    }

    public Question(Date timestamp, List<Comment> comments, String city, String author, String uidAuthor, String type, String title, String qid, Reactions reactions) {
        this.timestamp = timestamp;
        this.comments = comments;
        this.city = city;
        this.author = author;
        this.uidAuthor = uidAuthor;
        this.type = type;
        this.title = title;
        this.qid = qid;
        this.reactions = reactions;
    }
}
