package com.holiholic.database.dataStructures;

public class Reactions {
    private Likes likes;
    private Dislikes dislikes;

    public Likes getLikes() {
        return likes;
    }

    public void setLikes(Likes likes) {
        this.likes = likes;
    }

    public Reactions() {
    }

    public Dislikes getDislikes() {
        return dislikes;
    }

    public void setDislikes(Dislikes dislikes) {
        this.dislikes = dislikes;
    }

    public Reactions(Likes likes, Dislikes dislikes) {
        this.likes = likes;
        this.dislikes = dislikes;
    }
}
