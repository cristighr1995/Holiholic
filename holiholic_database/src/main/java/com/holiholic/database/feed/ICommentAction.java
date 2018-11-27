package com.holiholic.database.feed;

import org.json.JSONObject;

/* ICommentAction - Interface for a comment action
 *
 */
public interface ICommentAction {

    boolean add(JSONObject editField);

    boolean remove(JSONObject editField);

    boolean edit(JSONObject editField);
}
