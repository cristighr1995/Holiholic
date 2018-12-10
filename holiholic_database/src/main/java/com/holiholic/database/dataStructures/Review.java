package com.holiholic.database.dataStructures;

import java.util.Date;
import java.util.List;

public class Review {
    private Date timeStamp;
    private List<Comment> comments;
    private String city;
    private String uidGuide;
    private String uidAuthor;
    private User authorInformation;
    private String rid;
    private String type;
    private String title;
    private Reactions reactions;

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

    public String getUidGuide() {
        return uidGuide;
    }

    public void setUidGuide(String uidGuide) {
        this.uidGuide = uidGuide;
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

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
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

    public Review(Date timeStamp, List<Comment> comments, String city, String uidGuide, String uidAuthor, User authorInformation, String rid, String type, String title, Reactions reactions) {
        this.timeStamp = timeStamp;
        this.comments = comments;
        this.city = city;
        this.uidGuide = uidGuide;
        this.uidAuthor = uidAuthor;
        this.authorInformation = authorInformation;
        this.rid = rid;
        this.type = type;
        this.title = title;
        this.reactions = reactions;
    }

    public Review() {
    }
}
