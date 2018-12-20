package com.holiholic.feed.handler;

import org.json.JSONObject;

/* Feed - Handle the questions, guide ads and reviews, posts operations (is abstract!)
 *
 */
public abstract class Feed {
    /* Factory - Creates object for equivalent instance
     *
     */
    public static class Factory {
        /* getInstance - Returns a new instance
         *
         *  @return             : a new Feed instance
         *  @type               : the type of the object
         *  @body               : the request body (from user)
         */
        public static Feed getInstance(String type) {
            switch (type) {
                case "post":
                    return new PostHandler();
                case "question":
                    return new QuestionHandler();
                case "guideAd":
                    return new GuideAdHandler();
                case "guideReview":
                    return new GuideReviewHandler();
                default:
                    return null;
            }
        }
    }

    public abstract boolean add();

    public abstract boolean remove(String id);

    public abstract boolean edit(String id);

    public abstract void setModel(JSONObject model);
}
