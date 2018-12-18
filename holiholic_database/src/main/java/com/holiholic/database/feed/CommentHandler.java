package com.holiholic.database.feed;

import com.holiholic.database.database.DatabaseManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* CommentHandler - Handle the comments operations
 *
 */
public class CommentHandler implements ICommentAction {
    private static final Logger LOGGER = Logger.getLogger(CommentHandler.class.getName());
    private final Feed feed;

    CommentHandler(Feed feed) {
        this.feed = feed;
        setLogger();
    }

    /* setLogger - This method should be changed when release application
     *             Sets the logger to print to console (instead of a file)
     *
     *  @return             : void
     */
    private void setLogger() {
        // add logger handler
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);

        LOGGER.addHandler(ch);
        LOGGER.setLevel(Level.ALL);
    }

    /* add - Add a comment to a specific feed item
     *
     *  @return             : success or not
     *  @editField          : contains information about the comment like the title etc
     */
    @Override
    public boolean add(JSONObject editField) {
        try {
            synchronized (DatabaseManager.class) {
                JSONObject feeds = Feed.fetch(feed.getPath(), feed.getCity());
                String uidCurrent = feed.getBody().getString("uidCurrent");
                String uidFeedAuthor = feed.getBody().getString("uidAuthor");
                JSONObject userFeed = feed.fetchUserFeed(feeds.getJSONObject(feed.getType()), uidFeedAuthor);
                String id = feed.getBody().getString(feed.getIdField());
                String comment = editField.getString("comment");

                LOGGER.log(Level.FINE, "User {0} wants to add comment \"{1}\" to the {2} {3} from {4} city",
                           new Object[]{uidCurrent, comment, feed.getType(), id, feed.getCity()});

                if (!userFeed.has(id)) {
                    return false;
                }

                JSONObject newComment = new JSONObject();
                newComment.put("uidCommAuthor", uidCurrent);
                newComment.put("comment", comment);
                newComment.put("timeStamp", editField.getString("timeStamp"));
                newComment.put("commId", DatabaseManager.generateMD5(newComment.toString()));
                newComment.put("authorInformation", DatabaseManager.getUserProfile(uidCurrent));

                JSONObject item = userFeed.getJSONObject(id);
                item.getJSONArray("comments").put(newComment);
                userFeed.put(id, item);
                feeds.getJSONObject(feed.getType()).put(uidFeedAuthor, userFeed);

                return feed.saveFeed(feed.getPath(), feed.getCity(), feeds);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* searchComment - Search for a specific comment in the comments of a feed item
     *
     *  @return             : comment index or -1
     *  @commId             : the id of the comment we search
     *  @comments           : the comments for a specific feed item
     */
    private int searchComment(String commId, JSONArray comments) {
        int commentIndex = -1;
        for (int i = 0; i < comments.length(); i++) {
            if (comments.getJSONObject(i).getString("commId").equals(commId)) {
                commentIndex = i;
                break;
            }
        }
        return commentIndex;
    }

    /* remove - Remove a comment to a specific feed item
     *
     *  @return             : success or not
     *  @editField          : contains information about the comment like the comment id, user id etc
     */
    @Override
    public boolean remove(JSONObject editField) {
        try {
            synchronized (DatabaseManager.class) {
                JSONObject feeds = Feed.fetch(feed.getPath(), feed.getCity());
                String uidCurrent = feed.getBody().getString("uidCurrent");
                String uidFeedAuthor = feed.getBody().getString("uidAuthor");
                JSONObject userFeed = feed.fetchUserFeed(feeds.getJSONObject(feed.getType()), uidFeedAuthor);
                String id = feed.getBody().getString(feed.getIdField());

                LOGGER.log(Level.FINE, "User {0} wants to remove comment {1} of the {2} {3} from {4} city",
                           new Object[]{uidCurrent, editField.getString("commId"), feed.getType(), id, feed.getCity()});

                if (!userFeed.has(id)) {
                    return false;
                }

                JSONObject item = userFeed.getJSONObject(id);
                JSONArray comments = item.getJSONArray("comments");
                int commentIndex = searchComment(editField.getString("commId"), comments);
                if (commentIndex == -1) {
                    return false;
                }

                JSONObject comment = comments.getJSONObject(commentIndex);
                if (!comment.getString("uidCommAuthor").equals(uidCurrent)) {
                    return false;
                }

                comments.remove(commentIndex);
                item.put("comments", comments);
                userFeed.put(id, item);
                feeds.getJSONObject(feed.getType()).put(uidFeedAuthor, userFeed);

                return feed.saveFeed(feed.getPath(), feed.getCity(), feeds);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* edit - Edit a comment to a specific feed item
     *
     *  @return             : success or not
     *  @editField          : contains information about the comment like the new title, user id, comment id etc
     */
    @Override
    public boolean edit(JSONObject editField) {
        try {
            synchronized (DatabaseManager.class) {
                JSONObject feeds = Feed.fetch(feed.getPath(), feed.getCity());
                String uidCurrent = feed.getBody().getString("uidCurrent");
                String uidFeedAuthor = feed.getBody().getString("uidAuthor");
                JSONObject userFeed = feed.fetchUserFeed(feeds.getJSONObject(feed.getType()), uidFeedAuthor);
                String id = feed.getBody().getString(feed.getIdField());

                LOGGER.log(Level.FINE, "User {0} wants to edit comment {1} of the {2} {3} from {4} city to \"{5}\"",
                           new Object[]{uidCurrent, editField.getString("commId"), feed.getType(),
                                        id, feed.getCity(), editField.getString("comment")});

                if (!userFeed.has(id)) {
                    return false;
                }

                JSONObject item = userFeed.getJSONObject(id);
                JSONArray comments = item.getJSONArray("comments");
                int commentIndex = searchComment(editField.getString("commId"), comments);
                if (commentIndex == -1) {
                    return false;
                }

                JSONObject comment = comments.getJSONObject(commentIndex);
                if (!comment.getString("uidCommAuthor").equals(uidCurrent)) {
                    return false;
                }

                comment.put("comment", editField.getString("comment"));
                comments.put(commentIndex, comment);
                item.put("comments", comments);
                userFeed.put(id, item);
                feeds.getJSONObject(feed.getType()).put(uidFeedAuthor, userFeed);

                return feed.saveFeed(feed.getPath(), feed.getCity(), feeds);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}