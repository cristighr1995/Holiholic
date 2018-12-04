package com.holiholic.database.feed;

import com.holiholic.database.DatabaseManager;
import com.holiholic.database.constant.Constants;
import org.json.JSONObject;

import java.io.File;
import java.util.logging.Level;
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
            content.put("likes", createEmptyLikesObject());

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
        if (body.has(idField)) {
            return editLikes(operation, react);
        }

        // user wants to rate the guide itself and not a specific post
        return editLikesFromProfile(operation, react);
    }

    private boolean editLikesFromProfile(String operation, String react) {
        try {
            synchronized (DatabaseManager.class) {
                JSONObject feed = fetch(getPath(), getCity());
                String uidCurrent = getBody().getString("uidCurrent");
                String uidGuide = getBody().getString("uidGuide");

                JSONObject likes = feed.getJSONObject("likes");
                JSONObject reactJson = likes.getJSONObject(react);

                LOGGER.log(Level.FINE, "User {0} wants to {1} {2} to the {3} of user {4} from {5} city",
                           new Object[]{uidCurrent, operation, react, getType(), uidGuide, getCity()});

                switch (operation) {
                    case "add":
                        if (reactJson.has(uidCurrent)) {
                            return true;
                        }
                        reactJson.put(uidCurrent, true);
                        break;
                    case "remove":
                        if (!reactJson.has(uidCurrent)) {
                            return true;
                        }
                        reactJson.remove(uidCurrent);
                        break;
                    default:
                        return false;
                }

                likes.put(react, reactJson);
                feed.put("likes", likes);

                return saveFeed(getPath(), getCity(), feed);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
