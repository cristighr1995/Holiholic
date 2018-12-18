package com.holiholic.feed.handler;

import api.Query;
import com.holiholic.feed.model.Post;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    public boolean remove() {
        return false;
    }

    @Override
    public boolean edit() {
        return false;
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
}
