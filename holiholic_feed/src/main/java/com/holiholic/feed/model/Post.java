package com.holiholic.feed.model;

import com.holiholic.feed.database.DatabaseManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.*;

import static com.holiholic.feed.constant.Constants.DATE_FORMAT;

/* Post - Model for the post object
 *
 */
public class Post {
    private String pid;
    private Date timestamp;
    private String city;
    private String uidAuthor;
    private Content content;
    private Set<String> topics;

    public Post(String pid, Date timestamp, String city, String uidAuthor, Content content, Set<String> topics) {
        this.pid = pid;
        this.timestamp = timestamp;
        this.city = city;
        this.uidAuthor = uidAuthor;
        this.content = content;
        this.topics = topics;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
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

    public Set<String> getTopics() {
        return topics;
    }

    public void setTopics(Set<String> topics) {
        this.topics = topics;
    }

    public JSONObject serialize() {
        JSONObject post = new JSONObject();
        post.put("pid",this.pid);
        post.put("timestamp", DATE_FORMAT.format(this.timestamp));
        post.put("city", this.city);
        post.put("uidAuthor", this.uidAuthor);
        post.put("content",this.content.serialize());
        post.put("topics", new JSONArray(this.topics));
        return post;
    }

    public static Post deserialize(JSONObject postJson) {
        Post post = null;
        JSONArray topics = postJson.getJSONArray("topics");
        Set<String> topicsSet = new HashSet<>();
        for (int i = 0; i < topics.length(); i++) {
            topicsSet.add(topics.getString(i));
        }

        try {
            post = new Post(DatabaseManager.generateMD5(postJson.toString()),
                            DATE_FORMAT.parse(postJson.getString("timestamp")),
                            postJson.getString("city"),
                            postJson.getString("uidAuthor"),
                            Content.deserialize(postJson.getJSONObject("content")),
                            topicsSet);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return post;
    }
}
