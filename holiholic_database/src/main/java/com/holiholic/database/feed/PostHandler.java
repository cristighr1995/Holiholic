package com.holiholic.database.feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.holiholic.database.constant.Constants;
import com.holiholic.database.dataStructures.Post;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

/* PostHandler - Handle operations for a post item
 *
 */
public class PostHandler extends Feed implements IFeedEditable {
    private static final Logger LOGGER = Logger.getLogger(PostHandler.class.getName());
    private final String path;
    private final String city;
    private final String idField;
    private final String type;
    private final JSONObject body;
    private final Post post;

    PostHandler(String city, JSONObject body) {
        this.city = city;
        this.body = body;
        path = Constants.POSTS_DB_PATH;
        idField = "pid";
        type = "post";
        setLogger(LOGGER);
        this.post = unmarshal(body);
        initDatabaseFile();
    }

    private Post unmarshal(JSONObject body) {
        Post post = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            post = mapper.readValue(body.toString(), Post.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return post;
    }

    @Override
    public boolean add(JSONObject body) {
        return add();
    }

    @Override
    public boolean remove(JSONObject body) {
        return remove();
    }

    @Override
    public boolean editTitle(JSONObject body, String title) {
        return editTitle(title);
    }

    @Override
    public boolean editComment(JSONObject body, JSONObject editField) {
        return editComment(editField);
    }

    @Override
    public boolean editLikes(JSONObject body, JSONObject editField) {
        String operation = editField.getString("operation");
        String react = editField.getString("react");
        return editLikes(operation, react);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public String getIdField() {
        return idField;
    }

    @Override
    public String getType() {
        return type;
    }

    public JSONObject getBody() {
        return body;
    }
}
