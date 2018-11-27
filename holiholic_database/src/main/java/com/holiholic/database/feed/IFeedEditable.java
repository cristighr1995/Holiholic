package com.holiholic.database.feed;

import org.json.JSONObject;

/* IFeedEditable - Interface for edit operations for a feed item
 *
 */
public interface IFeedEditable {

    boolean editTitle(JSONObject body, String title);

    boolean editComment(JSONObject body, JSONObject editField);

    boolean editLikes(JSONObject body, JSONObject editField);
}
