package com.holiholic.database.feed;

import org.json.JSONObject;

/* FeedEditHandler - Handler for the edit operations of a feed item
 *
 */
public class FeedEditHandler implements IFeedEditable {
    private final Feed feed;

    FeedEditHandler(Feed feed) {
        this.feed = feed;
    }

    @Override
    public boolean editTitle(JSONObject body, String title) {
        return feed.editTitle(body, title);
    }

    @Override
    public boolean editComment(JSONObject body, JSONObject editField) {
        return feed.editComment(body, editField);
    }

    @Override
    public boolean editLikes(JSONObject body, JSONObject editField) {
        return feed.editLikes(body, editField);
    }
}
