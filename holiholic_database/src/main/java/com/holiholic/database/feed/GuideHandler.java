package com.holiholic.database.feed;

import com.holiholic.database.constant.Constants;
import org.json.JSONObject;

import java.util.logging.Logger;

/* GuideHandler - Handle operations for a guide item
 *
 */
public class GuideHandler extends Feed implements IFeedEditable {
    private static final Logger LOGGER = Logger.getLogger(GuideHandler.class.getName());
    private final String path;
    private final String city;
    private final String idField;
    private final String type;
    private final JSONObject body;

    GuideHandler(String city, JSONObject body) {
        this.city = city;
        this.body = body;
        path = Constants.GUIDES_DB_PATH;
        idField = "gid";
        type = "guide";
        setLogger(LOGGER);
        initDatabaseFile();
    }

    @Override
    public void add() {
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
