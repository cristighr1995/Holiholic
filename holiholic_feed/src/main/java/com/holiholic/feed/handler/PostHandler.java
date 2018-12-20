package com.holiholic.feed.handler;

import com.holiholic.database.api.DatabasePredicate;
import com.holiholic.database.api.Query;
import com.holiholic.feed.model.Post;
import org.json.JSONObject;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.holiholic.feed.constant.Constants.*;

/* PostHandler - Handle operations for a post item
 *
 */
public class PostHandler extends Feed {
    private static final Logger LOGGER = Logger.getLogger(PostHandler.class.getName());
    private Post post;

    public PostHandler() {
        setLogger();
    }

    @Override
    public boolean add() {
        try {
            Query.insert(POSTS_TABLE_NAME, getValuesList());
            for (String topic : post.getTopics()) {
                Query.insert(TOPICS_TABLE_NAME, Arrays.asList("\"" + topic + "\"", "\"post\"", "\"" + post.getPid() + "\""));
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean remove(String id) {
        try {
            Query.delete(POSTS_TABLE_NAME, Arrays.asList(new DatabasePredicate("pid", "=", "\"" + id + "\"")));
            Query.delete(TOPICS_TABLE_NAME, Arrays.asList(new DatabasePredicate("parentId", "=", "\"" + id + "\"")));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean edit(String id) {
        try {
            Map<String, String> attributes = getAttributesMap();
            Query.update(POSTS_TABLE_NAME, attributes, Arrays.asList(new DatabasePredicate("pid", "=", "\"" + id + "\"")));
            Map<String, Boolean> oldTopics = getOldTopicsList(id);
            for (String topic : post.getTopics()) {
                if (!oldTopics.containsKey(topic)) {
                    Query.insert(TOPICS_TABLE_NAME, Arrays.asList("\"" + topic + "\"", "\"post\"", "\"" + id + "\""));
                    oldTopics.remove(topic);
                }
            }

            for (Map.Entry<String, Boolean> entry : oldTopics.entrySet()) {
                Query.delete(TOPICS_TABLE_NAME, Arrays.asList(new DatabasePredicate("name","=", "\"" + entry.getKey()+ "\""),
                                                              new DatabasePredicate("parentId", "=", "\"" + id + "\"")));
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void setModel(JSONObject model) {
        post = Post.deserialize(model);
    }

    /* setLogger - This method should be changed when release application
     *             Sets the logger to print to console (instead of a file)
     *
     *  @return             : void
     */
    void setLogger() {
        // add logger handler
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);

        LOGGER.addHandler(ch);
        LOGGER.setLevel(Level.ALL);
    }

    /* getValuesList - This method constructs the list of values for the insert query
     *
     * @ return             : List<String>
     */
    private List<String> getValuesList() {
        List<String> values = new ArrayList<>();
        values.add("\"" + post.getPid() + "\"");
        values.add("\'" + DATE_FORMAT.format(post.getTimestamp()) + "\'");
        values.add("\"" + post.getCity() + "\"");
        values.add("\"" + post.getUidAuthor() + "\"");
        values.add("\'" + post.getContent().serialize().toString() + "\'");
        return values;
    }

    /* getAttributesMap - This method constructs the map of attributes for the update query
     *
     * @ return             : Map<String, String>
     */
    private Map<String, String> getAttributesMap() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("city", "\"" + post.getCity() + "\"");
        attributes.put("content", "\'" + post.getContent().serialize().toString() + "\'");

        return attributes;
    }

    /* getOldTopicsList - This method constructs a list with old topics
     *
     * @ return             : List<String>
     */
    private Map<String, Boolean> getOldTopicsList(String id) {
        ResultSet rs = Query.select(Arrays.asList("name"), TOPICS_TABLE_NAME, Arrays.asList(new DatabasePredicate("parentType", "=", "\"" + "post" + "\""),
                new DatabasePredicate("parentId", "=", "\"" + id + "\"")));

        Map<String, Boolean> topics = new HashMap<>();
        while (true) {
            try {
                if (!rs.next()) break;
                String topic = rs.getString("name");
                topics.put(topic, true);
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        return topics;
    }
}