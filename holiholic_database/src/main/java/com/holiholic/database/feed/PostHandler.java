package com.holiholic.database.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.holiholic.database.constant.Constants;
import com.holiholic.database.dataStructures.Post;
import com.holiholic.database.database.DatabaseManager;
import com.holiholic.database.database.Query;
import org.json.JSONObject;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/* PostHandler - Handle operations for a post item
 *
 */
public class PostHandler extends Feed {
    private static final Logger LOGGER = Logger.getLogger(PostHandler.class.getName());
    private static final String TABLE_NAME = "holiholicdb.Posts";
    private final JSONObject body;
    private Post post;

    PostHandler(JSONObject body) {
        body.remove("operation");
        body.remove("type");
        this.body = body;
        setLogger(LOGGER);
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
    public void add() {
        unmarshal();
        post.setPid(DatabaseManager.generateMD5(post.toString()));
        Query.insert(TABLE_NAME, getValuesList());
    }

    private List<String> getValuesList() {
        List<String> values = new ArrayList<>();
        values.add("\"" + post.getPid() + "\"");
        values.add("\"" + post.getTimestamp() + "\"");
        values.add("\"" + post.getCity() + "\"");
        values.add("\"" + post.getUidAuthor() + "\"");
        values.add("\"" + post.getContent() + "\"");

        return values;
    }

    @Override
    public void remove() {
    }

    @Override
    public void edit() {
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

    public JSONObject getBody() {
        return body;
    }
    /* getCity - Get the city for the current feed
     *
     *  @return             : the city name for the current feed
     */
    protected  String getCity() {
        return null;
    }

    /* getPath - Get the database path for the current feed
     *
     *  @return             : the database path for the current feed
     */
    protected  String getPath(){
        return null;
    }

    /* getIdField - Get the field id for the current feed
     *
     *  @return             : qid or pid
     */
    protected  String getIdField() {
        return null;
    }

    /* getType - Get the type for the current feed
     *
     *  @return             : question or post
     */
    protected  String getType(){
        return null;
    }

}
