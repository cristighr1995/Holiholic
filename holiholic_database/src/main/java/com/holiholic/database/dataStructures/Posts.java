package com.holiholic.database.dataStructures;

import java.util.Map;

public class Posts {
    private Map<String, Map<String, Post>> post;
    private int postCount;

    public Map<String, Map<String, Post>> getPost() {
        return post;
    }

    public void setPost(Map<String, Map<String, Post>> post) {
        this.post = post;
    }

    public int getPostCount() {
        return postCount;
    }

    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    public Posts() {
    }

    public Posts(Map<String, Map<String, Post>> post, int postCount) {
        this.post = post;
        this.postCount = postCount;
    }
}
