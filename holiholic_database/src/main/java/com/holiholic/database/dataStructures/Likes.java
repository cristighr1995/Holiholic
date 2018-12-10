package com.holiholic.database.dataStructures;

import java.util.Map;

public class Likes {
    private Map<String, Boolean> users;
    private int likeCount;

    public Map<String, Boolean> getUsers() {
        return users;
    }

    public void setUsers(Map<String, Boolean> users) {
        this.users = users;
    }

    public int getCount() {
        return likeCount;
    }

    public void setCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public Likes() {
    }

    public Likes(Map<String, Boolean> users, int likeCount) {
        this.users = users;
        this.likeCount = likeCount;
    }
}
