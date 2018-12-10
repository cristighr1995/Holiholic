package com.holiholic.database.dataStructures;

import java.util.Map;

public class Dislikes {
    public Map<String, Boolean> getUsers() {
        return users;
    }

    public void setUsers(Map<String, Boolean> users) {
        this.users = users;
    }

    public int getCount() {
        return dislikeCount;
    }

    public void setCount(int dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public Dislikes(Map<String, Boolean> users, int dislikeCount) {
        this.users = users;
        this.dislikeCount = dislikeCount;
    }

    private Map<String, Boolean> users;

    public Dislikes() {
    }

    private int dislikeCount;
}
