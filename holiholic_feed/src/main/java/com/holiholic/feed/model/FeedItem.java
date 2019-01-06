package com.holiholic.feed.model;

import com.holiholic.feed.database.DatabaseManager;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.*;

import static com.holiholic.feed.constant.Constants.DATE_FORMAT;
import static com.holiholic.feed.constant.Constants.POSTS_TABLE_NAME;
import static com.holiholic.feed.constant.Constants.QUESTIONS_TABLE_NAME;

/* FeedItem - Model for the post object
 *
 */
public class FeedItem {
    private String id;
    private String tableName;
    private Date timestamp;
    private String city;
    private String uidAuthor;
    private Content content;

    public FeedItem(String id, String tableName, Date timestamp, String city, String uidAuthor, Content content) {
        this.id = id;
        this.tableName = tableName;
        this.timestamp = timestamp;
        this.city = city;
        this.uidAuthor = uidAuthor;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUidAuthor() {
        return uidAuthor;
    }

    public void setUidAuthor(String uidAuthor) {
        this.uidAuthor = uidAuthor;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public JSONObject serialize() {
        JSONObject feedItem = new JSONObject();
        feedItem.put("id",this.id);
        feedItem.put("timestamp", DATE_FORMAT.format(this.timestamp));
        feedItem.put("city", this.city);
        feedItem.put("uidAuthor", this.uidAuthor);
        feedItem.put("content",this.content.serialize());
        return feedItem;
    }

    public static FeedItem deserialize(JSONObject feedItemJson) {
        FeedItem feedItem = null;
        String tableName = "";
        switch (feedItemJson.getString("type")) {
            case "post":
                tableName = POSTS_TABLE_NAME;
                break;
            case "question":
                tableName = QUESTIONS_TABLE_NAME;
                break;
            default:
                break;
        }

        try {
            feedItem = new FeedItem(DatabaseManager.generateMD5(feedItemJson.toString()),
                            tableName,
                            DATE_FORMAT.parse(feedItemJson.getString("timestamp")),
                            feedItemJson.getString("city"),
                            feedItemJson.getString("uidAuthor"),
                            Content.deserialize(feedItemJson.getJSONObject("content")));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return feedItem;
    }
}
