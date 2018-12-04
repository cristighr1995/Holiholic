package com.holiholic.database.feed;

import com.holiholic.database.DatabaseManager;
import com.holiholic.database.constant.Constants;
import org.json.JSONObject;

import java.io.File;
import java.util.logging.Logger;

/* GuideProfile - Handle operations for a guide profile
 *
 */
public class GuideProfile extends Feed implements IFeedEditable {
    private static final Logger LOGGER = Logger.getLogger(GuideProfile.class.getName());
    private String path;
    private final String city;
    private final String idField;
    private final String type;
    private final JSONObject body;

    GuideProfile(String city, JSONObject body) {
        this.city = city;
        this.body = body;
        idField = "gpid";
        type = "guideProfile";
        setLogger(LOGGER);
        initPath();
    }

    private void initPath() {
        try {
            path = Constants.GUIDE_PROFILE_DB_PATH + body.getString("uidGuide") + "_";
            initFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initFile() {
        synchronized (DatabaseManager.class) {
            String fullPath = path + city.toLowerCase() + ".json";
            File f = new File(fullPath);
            if (f.exists() && !f.isDirectory()) {
                return;
            }

            // populate empty file
            JSONObject content = new JSONObject();
            content.put(type + "Count", 0);
            content.put(type, new JSONObject());

            DatabaseManager.syncDatabase(fullPath, content);
        }
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
