package com.holiholic.feed.handler;

import com.holiholic.database.api.DatabasePredicate;
import com.holiholic.database.api.Query;
import com.holiholic.feed.model.FeedItem;
import org.json.JSONObject;

import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.holiholic.feed.constant.Constants.*;

/* FeedItemHandler - Handle operations for post and question items
 *
 */
public class FeedItemHandler extends Feed {
    private static final Logger LOGGER = Logger.getLogger(FeedItemHandler.class.getName());
    private FeedItem feedItem;
    private String TABLE_NAME;

    public FeedItemHandler() {
        setLogger();
    }

    @Override
    public boolean add() {
        try {
            Query.insert(feedItem.getTableName(), getValuesList());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean remove(String id) {
        try {
            Query.delete(feedItem.getTableName(), Arrays.asList(new DatabasePredicate("id", "=", "\"" + id + "\"")));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean edit(String id) {
        try {
            Map<String, String> attributes = getAttributesMap();
            Query.update(feedItem.getTableName(), attributes, Arrays.asList(new DatabasePredicate("id", "=", "\"" + id + "\"")));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void setModel(JSONObject model) {
        feedItem = FeedItem.deserialize(model);
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
        values.add("\"" + feedItem.getId() + "\"");
        values.add("\'" + DATE_FORMAT.format(feedItem.getTimestamp()) + "\'");
        values.add("\"" + feedItem.getCity() + "\"");
        values.add("\"" + feedItem.getUidAuthor() + "\"");
        values.add("\'" + feedItem.getContent().serialize().toString() + "\'");
        return values;
    }

    /* getAttributesMap - This method constructs the map of attributes for the update query
     *
     * @ return             : Map<String, String>
     */
    private Map<String, String> getAttributesMap() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("city", "\"" + feedItem.getCity() + "\"");
        attributes.put("content", "\'" + feedItem.getContent().serialize().toString() + "\'");

        return attributes;
    }
}