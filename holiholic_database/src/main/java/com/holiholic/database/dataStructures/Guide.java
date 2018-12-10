package com.holiholic.database.dataStructures;

import java.util.Date;
import java.util.List;

public class Guide {
    private Date timestamp;
    private List<Comment> comments;
    private String city;
    private String phone;
    private String email;
    private String description;
    private String uidAuthor;
    private User authorInformation;
    private String type;
    private String title;
    private String gid;
    private Reactions reactions;

    public Guide() {
    }

    public Guide(Date timestamp, List<Comment> comments, String city, String phone, String email, String description, String uidAuthor, User authorInformation, String type, String title, String gid, Reactions reactions) {
        this.timestamp = timestamp;
        this.comments = comments;
        this.city = city;
        this.phone = phone;
        this.email = email;
        this.description = description;
        this.uidAuthor = uidAuthor;
        this.authorInformation = authorInformation;
        this.type = type;
        this.title = title;
        this.gid = gid;
        this.reactions = reactions;
    }

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public Reactions getReactions() {
        return reactions;
    }

    public void setReactions(Reactions reactions) {
        this.reactions = reactions;
    }
}
