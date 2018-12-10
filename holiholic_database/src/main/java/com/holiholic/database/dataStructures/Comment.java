package com.holiholic.database.dataStructures;

import java.util.Date;
import java.util.List;

public class Comment {
    private Date timestamp;
    private String uidCommAuthor;
    private String commId;
    private String comment;
    private List<Comment> comments;

    public Comment() {
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getUidCommAuthor() {
        return uidCommAuthor;
    }

    public void setUidCommAuthor(String uidCommAuthor) {
        this.uidCommAuthor = uidCommAuthor;
    }

    public String getCommId() {
        return commId;
    }

    public void setCommId(String commId) {
        this.commId = commId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public Comment(Date timestamp, String uidCommAuthor, String commId, String comment, List<Comment> comments) {
        this.timestamp = timestamp;
        this.uidCommAuthor = uidCommAuthor;
        this.commId = commId;
        this.comment = comment;
        this.comments = comments;
    }
}
