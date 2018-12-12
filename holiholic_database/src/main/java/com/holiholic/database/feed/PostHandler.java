package com.holiholic.database.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holiholic.database.database.DatabaseOperations;
import com.holiholic.database.constant.Constants;
import com.holiholic.database.dataStructures.Post;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

/* PostHandler - Handle operations for a post item
 *
 */
public class PostHandler extends Feed implements IFeedEditable, IDatabaseQueries {
    private static final Logger LOGGER = Logger.getLogger(PostHandler.class.getName());
    private final String path;
    private final String city;
    private final String idField;
    private final String type;
    private final JSONObject body;
    private Post post;

    PostHandler(String city, JSONObject body) {
        this.city = city;
        this.body = body;
        path = Constants.POSTS_DB_PATH;
        idField = "pid";
        type = "post";
        setLogger(LOGGER);
        initDatabaseFile();
    }

    private void unmarshal() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            post = mapper.readValue(body.toString(), Post.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean add() {
        unmarshal();
        // cache

        // db task
        String sql = generateInsertQuery();
        try {
            DatabaseOperations.insert(sql);
        } catch (Exception e) {
            e.printStackTrace();
            return  false;
        }

        return true;
    }

    private String generateInsertQuery() {

        StringBuilder sb = new StringBuilder();
        String sql;
        sql = "INSERT into holiholicdb.Employees values (105, 1555, \"cristi\", \"ghr\");";
        sb.append("INSERT into holiholicdb.Posts values (");
        sb.append()

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
