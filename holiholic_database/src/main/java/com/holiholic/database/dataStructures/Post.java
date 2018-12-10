package com.holiholic.database.dataStructures;

import java.util.Date;
import java.util.List;

public class Post {
    private Date timeStamp;
    private List<Comment> comments;
    private String city;
    private String uidAuthor;
    private User authorInformation;
    private String pid;
    private String type;
    private String title;
    private Reactions reactions;

    public Post() {
    }

    public Post(Date timeStamp, List<Comment> comments, String city, String uidAuthor, User authorInformation, String pid, String type, String title, Reactions reactions) {
        this.timeStamp = timeStamp;
        this.comments = comments;
        this.city = city;
        this.uidAuthor = uidAuthor;
        this.authorInformation = authorInformation;
        this.pid = pid;
        this.type = type;
        this.title = title;
        this.reactions = reactions;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
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

    public String getUidAuthor() {
        return uidAuthor;
    }

    public void setUidAuthor(String uidAuthor) {
        this.uidAuthor = uidAuthor;
    }

    public User getAuthorInformation() {
        return authorInformation;
    }

    public void setAuthorInformation(User authorInformation) {
        this.authorInformation = authorInformation;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
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

    public Reactions getReactions() {
        return reactions;
    }

    public void setReactions(Reactions reactions) {
        this.reactions = reactions;
    }
}
