package com.holiholic.database.feed;

import org.json.JSONArray;
import org.json.JSONObject;
import com.holiholic.database.database.DatabaseManager;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* Feed - Handle the questions, guides and posts operations (is abstract!)
 *
 */
public abstract class Feed {
    private Logger LOGGER;

    /* Factory - Creates object for PostHandler or QuestionHandler
     *
     */
    public static class Factory {
        /* getInstance - Returns a new instance of a PostHandler or QuestionHandler
         *
         *  @return             : a new Feed instance
         *  @city               : where
         *  @type               : question or post
         *  @body               : the request body (from user)
         */
        public static Feed getInstance(String city, String type, JSONObject body) {
            switch (type) {
                case "post":
                    return new PostHandler(city, body);
                case "question":
                    return new QuestionHandler(city, body);
                case "guide":
                    return new GuideHandler(city, body);
                case "review":
                    return new GuideProfileHandler(city, body);
                default:
                    return null;
            }
        }
    }

    /* setLogger - This method should be changed when release application
     *             Sets the logger to print to console (instead of a file)
     *
     *  @return             : void
     */
    void setLogger(final Logger LOGGER) {
        // add logger handler
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);

        LOGGER.addHandler(ch);
        LOGGER.setLevel(Level.ALL);

        this.LOGGER = LOGGER;
    }

    /* fetch - Get feed items for a specific city from the database
     *         Can be used also from outside if the parameters are known
     *
     *  @return             : feed items for the city
     *  @path               : the database path
     *  @city               : the city the user wants to see feed items
     */
    static JSONObject fetch(String path, String city) {
        return DatabaseManager.fetchObjectFromDatabase(getDatabaseFullPath(path, city));
    }

    /* getFeed - Get feed (questions, guides or posts)
     *
     *  @return             : an array of object with feeds
     *  @uid                : the current user id
     *  @city               : the city the user wants to see feed items
     *  @type               : question, guide or post
     *  @LOGGER             : logger instance to print useful information
     *  @feeds              : cached in memory feeds
     */
    private static JSONArray getFeed(String uid,
                                     String city,
                                     String type,
                                     Logger LOGGER,
                                     JSONObject feeds) {
        LOGGER.log(Level.FINE, "New request from user {0} to get {1}s from {2} city",
                   new Object[]{uid, type, city});
        try {
            if (!DatabaseManager.containsUser(uid)) {
                return new JSONArray();
            }

            JSONArray result = new JSONArray();
            // for each author
            for (String author : feeds.keySet()) {
                // for each feed item
                for (String id : feeds.getJSONObject(author).keySet()) {
                    JSONObject feed = feeds.getJSONObject(author).getJSONObject(id);
                    // display only the last comment
                    JSONArray comments = feed.getJSONArray("comments");
                    if (comments.length() >= 2) {
                        JSONObject comment = comments.getJSONObject(comments.length() - 1);
                        feeds.remove("comments");
                        feeds.put("comments", new JSONArray().put(comment));
                    }
                    // display only the number of reactions
                    JSONObject likes = feed.getJSONObject("likes");
                    // for each react store only the count
                    for (String react : likes.keySet()) {
                        likes.put(react, likes.getJSONObject(react).length());
                    }
                    feed.put("likes", likes);
                    result.put(feed);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    /* getFeed - Get feed (questions, guides or posts)
     *
     *  @return             : an array of object with feeds
     *  @uid                : the current user id
     *  @path               : the database path
     *  @city               : the city the user wants to see feed items
     *  @type               : question, guide or post
     *  @LOGGER             : logger instance to print useful information
     */
    private static JSONArray getFeed(String uid,
                                     String path,
                                     String city,
                                     String type,
                                     Logger LOGGER) {
        try {
            return getFeed(uid, city, type, LOGGER, fetch(path, city).getJSONObject(type));
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    /* getQuestions - Get a list of questions for a specific city
     *                Each question has only the last comment
     *
     *  @return             : a list of questions (json string format)
     *  @uid                : current user id
     *  @path               : the database path
     *  @city               : the requested city
     *  @type               : question
     *  @LOGGER             : logger to print useful information
     */
    public static JSONArray getQuestions(String uid,
                                         String path,
                                         String city,
                                         String type,
                                         Logger LOGGER) {
        return getFeed(uid, path, city, type, LOGGER);
    }

    /* getGuideProfile - Get a list of posts for a guide and also the reactions for his profile
     *                   Each post has only the last comment
     *
     *  @return             : a list of posts from the guide profile and his reactions (json object format)
     *  @uid                : current user id
     *  @path               : the database path
     *  @city               : the requested city
     *  @type               : review
     *  @LOGGER             : logger to print useful information
     */
    public static JSONObject getGuideProfile(String uid,
                                             String path,
                                             String city,
                                             String type,
                                             Logger LOGGER) {
        try {
            JSONObject review = fetch(path, city);
            JSONArray guidePosts = getFeed(uid, city, type, LOGGER, review.getJSONObject(type));

            JSONObject result = new JSONObject();
            result.put("guidePosts", guidePosts);
            result.put("likes", review.getJSONObject("likes"));

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    /* getGuides - Get a list of guides for a specific city
     *
     *  @return             : a list of guides (json string format)
     *  @uid                : current user id
     *  @path               : the database path
     *  @city               : the requested city
     *  @type               : guide
     *  @LOGGER             : logger to print useful information
     */
    public static JSONArray getGuides(String uid,
                                      String path,
                                      String city,
                                      String type,
                                      Logger LOGGER) {
        return getFeed(uid, path, city, type, LOGGER);
    }

    /* getPosts - Get a list of posts
     *            Each post has only the last comment
     *
     *  @return             : a list of posts (json string format)
     *  @uid                : current user id
     *  @path               : the database path
     *  @type               : post
     *  @LOGGER             : logger to print useful information
     */
    public static JSONArray getPosts(String uid,
                                     String path,
                                     String type,
                                     Logger LOGGER) {
        JSONArray cities = getAvailableCities();
        JSONArray result = new JSONArray();
        JSONArray people = DatabaseManager.fetchPeople(uid);
        Set<String> followingIds = new HashSet<>();
        for (int i = 0; i < people.length(); i++) {
            followingIds.add(people.getString(i));
        }
        // current user should also see his own posts
        followingIds.add(uid);

        // merge cities
        for (int i = 0; i < cities.length(); i++) {
            JSONArray cityFeed = getFeed(uid, path, cities.getString(i), type, LOGGER);
            for (int j = 0; j < cityFeed.length(); j++) {
                JSONObject item = cityFeed.getJSONObject(j);
                // only posts from people the user is following
                if (followingIds.contains(item.getString("uidAuthor"))) {
                    result.put(item);
                }
            }
        }
        return result;
    }

    /* getAvailableCities - Get a list of available cities for the application
     *
     *  @return             : a list with cities
     */
    private static JSONArray getAvailableCities() {
        return DatabaseManager.fetchCities();
    }

    /* getDetails - Get details about a specific feed item
     *
     *  @return             : the details json string format
     *  @city               : the city the user wants to see feed items
     *  @id                 : the id of the feed item
     *  @type               : question, guide or post
     *  @path               : the database path
     *  @uidCurrent         : the id of the current user
     *  @uidAuthor          : the id of the author of the feed item
     *  @LOGGER             : the logger used to print useful information
     */
    public static String getDetails(String city,
                                    String id,
                                    String type,
                                    String path,
                                    String uidCurrent,
                                    String uidAuthor,
                                    Logger LOGGER) {
        LOGGER.log(Level.FINE, "New request from user {0} to get {1} {2} details from {3} city",
                   new Object[]{uidCurrent, type, id, city});
        try {
            if (!DatabaseManager.containsUser(uidCurrent)) {
                return new JSONArray().toString(2);
            }

            JSONObject feed = fetch(path, city).getJSONObject(type);
            if (!feed.has(uidAuthor)) {
                return new JSONArray().toString(2);
            }

            JSONObject userFeed = feed.getJSONObject(uidAuthor);
            if (!userFeed.has(id)) {
                return new JSONArray().toString(2);
            }

            return userFeed.getJSONObject(id).toString(2);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray().toString(2);
        }
    }

    /* createEmptyLikesObject - Create an empty json object for the likes (reacts)
     *                          In the future we can add more than just 2 reactions
     *
     *  @return             : the react json
     */
    JSONObject createEmptyLikesObject() {
        JSONObject likes = new JSONObject();
        likes.put("like", new JSONObject());
        likes.put("dislike", new JSONObject());
        return likes;
    }

    /* containsUser - Checks if a user is in the system
     *
     *  @return             : true or false
     *  @userId             : the user id
     */
    private boolean containsUser(String userId) {
        return DatabaseManager.containsUser(userId);
    }

    /* fetchUserFeed - Returns a json object with all the feeds for a specific user
     *
     *  @return             : feeds (json format)
     *  @feed               : the whole json from memory
     *  @uidAuthor          : the user id
     */
    JSONObject fetchUserFeed(JSONObject feed, String uidAuthor) {
        JSONObject userFeed = null;
        try {
            if (feed.has(uidAuthor)) {
                userFeed = feed.getJSONObject(uidAuthor);
            } else {
                userFeed = new JSONObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
         return userFeed;
    }

    /* createDatabaseEntity - Creates an entity object to be saved in the database
     *
     *  @return             : the entity
     *  @body               : the body (from network) for the feed item
     *  @idField            : qid or pid
     */
    private JSONObject createDatabaseEntity(JSONObject body, String idField) {
        // remove operation
        body.remove("operation");
        // empty list of comments
        body.put("comments", new JSONArray());
        // zero likes
        body.put("likes", createEmptyLikesObject());
        // set an id
        body.put(idField, DatabaseManager.generateMD5(body.toString()));
        // add user information for display
        body.put("authorInformation", DatabaseManager.getUserProfile(body.getString("uidAuthor")));

        // the body here is modified because the method param is passed by value
        return body;
    }

    /* getDatabaseFullPath - Construct the database merging the early constructed path prefix and the city extension
     *
     *  @return             : success or not
     *  @pathPrefix         : the prefix path for the database
     *  @city               : the city for the current action
     */
    public static String getDatabaseFullPath(String pathPrefix, String city) {
        return pathPrefix + city + ".json";
    }

    /* createDatabaseEmptyFile - Creates an object which is used for the database to initialize an empty database
     *
     *  @return             : the json object
     */
    JSONObject createDatabaseEmptyFile() {
        JSONObject content = new JSONObject();
        content.put(getType() + "Count", 0);
        content.put(getType(), new JSONObject());
        return content;
    }

    /* initDatabaseFile - Checks if the database file already exists and if not creates an empty file
     *                    For example creates and empty "posts_bucharest.json" file
     *
     *  @return             : void
     */
    void initDatabaseFile() {
        try {
            DatabaseManager.initDatabaseFileObject(getDatabaseFullPath(getPath(), getCity()), createDatabaseEmptyFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* saveFeed - Saves in the database the updates for the feeds
     *
     *  @return             : success or not
     *  @path               : the path for the database
     *  @city               : the city the user wants to see feeds
     *  @feed               : the updated feed
     */
    boolean saveFeed(String path, String city, JSONObject feed) {
        return DatabaseManager.syncDatabase(getDatabaseFullPath(path, city), feed);
    }

    /* getCity - Get the city for the current feed
     *
     *  @return             : the city name for the current feed
     */
    protected abstract String getCity();

    /* getPath - Get the database path for the current feed
     *
     *  @return             : the database path for the current feed
     */
    protected abstract String getPath();

    /* getIdField - Get the field id for the current feed
     *
     *  @return             : qid or pid
     */
    protected abstract String getIdField();

    /* getType - Get the type for the current feed
     *
     *  @return             : question or post
     */
    protected abstract String getType();

    /* getBody - Get the network body request for the current feed
     *
     *  @return             : the network body request
     */
    protected abstract JSONObject getBody();

    /* add - Adds a new feed item (is abstract)
     *
     *  @return             : success or not
     */
    public abstract boolean add();

    /* add - Adds a new feed item (this is the actual implementation)
     *
     *  @return             : success or not
     */
//    public boolean add() {
//        String uidAuthor = getBody().getString("uidAuthor");
//        if (!containsUser(uidAuthor)) {
//            return false;
//        }
//
//        // create the body which is saved in the database
//        JSONObject entity = createDatabaseEntity(getBody(), getIdField());
//
//        LOGGER.log(Level.FINE, "User {0} wants to add a {1} in {2} city",
//                   new Object[]{uidAuthor, getType(), getCity()});
//
//        try {
//            synchronized (DatabaseManager.class) {
//                JSONObject feed = fetch(getPath(), getCity());
//                int count = feed.getInt(getType() + "Count");
//                JSONObject userFeed = fetchUserFeed(feed.getJSONObject(getType()), uidAuthor);
//                userFeed.put(entity.getString(getIdField()), entity);
//                feed.put(getType() + "Count", ++count);
//                feed.getJSONObject(getType()).put(uidAuthor, userFeed);
//                return saveFeed(getPath(), getCity(), feed);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

    /* remove - Remove a feed item (is abstract)
     *
     *  @return             : success or not
     */
    public abstract boolean remove(JSONObject body);

    /* remove - Remove a feed item (this is the actual implementation)
     *
     *  @return             : success or not
     */
    boolean remove() {
        try {
            synchronized (DatabaseManager.class) {
                JSONObject feed = fetch(getPath(), getCity());
                int count = feed.getInt(getType() + "Count");
                String uidCurrent = getBody().getString("uidCurrent");
                String uidAuthor = getBody().getString("uidAuthor");
                String id = getBody().getString(getIdField());

                LOGGER.log(Level.FINE, "User {0} wants to remove {1} with id {2} from {3} city",
                           new Object[]{uidCurrent, getType(), id, getCity()});

                if (!uidCurrent.equals(uidAuthor)) {
                    return false;
                }

                JSONObject userFeed = fetchUserFeed(feed.getJSONObject(getType()), uidAuthor);
                if (!userFeed.has(id)) {
                    return true;
                }

                userFeed.remove(id);
                feed.put(getType() + "Count", --count);
                feed.getJSONObject(getType()).put(uidAuthor, userFeed);

                return saveFeed(getPath(), getCity(), feed);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* edit - Edit a feed item
     *
     *  @return             : success or not
     *  @body               : the network body request
     */
    public boolean edit(JSONObject body) {
        try {
            JSONObject editField = body.getJSONObject("editField");
            assert editField.length() == 1;
            String field = editField.keys().next();
            FeedEditHandler feedEditHandler = new FeedEditHandler(this);

            switch (field) {
                case "title":
                    return feedEditHandler.editTitle(body, editField.getString(field));
                case "comments":
                    return feedEditHandler.editComment(body, editField.getJSONObject(field));
                case "likes":
                    return feedEditHandler.editLikes(body, editField.getJSONObject(field));
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* editTitle - Edit the title of a feed item (is abstract)
     *
     *  @return             : success or not
     *  @body               : the network body request
     *  @title              : the new title
     */
    public abstract boolean editTitle(JSONObject body, String title);

    /* editTitle - Edit the title of a feed item (this is the actual implementation)
     *
     *  @return             : success or not
     *  @title              : the new title
     */
    boolean editTitle(String title) {
        try {
            synchronized (DatabaseManager.class) {
                JSONObject feed = fetch(getPath(), getCity());
                String uidCurrent = getBody().getString("uidCurrent");
                String uidAuthor = getBody().getString("uidAuthor");
                String id = getBody().getString(getIdField());

                LOGGER.log(Level.FINE, "User {0} wants to edit title of the {1} {2} from {3} city to \"{4}\"",
                           new Object[]{uidCurrent, getType(), id, getCity(), title});

                if (!uidCurrent.equals(uidAuthor)) {
                    return false;
                }

                JSONObject userFeed = fetchUserFeed(feed.getJSONObject(getType()), uidAuthor);
                if (!userFeed.has(id)) {
                    return false;
                }

                JSONObject item = userFeed.getJSONObject(id);
                item.put("title", title);
                userFeed.put(id, item);
                feed.getJSONObject(getType()).put(uidAuthor, userFeed);

                return saveFeed(getPath(), getCity(), feed);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* editComment - Edit a comment of a feed item (is abstract)
     *
     *  @return             : success or not
     *  @body               : the network body request
     *  @editField          : contains information about the operation parameters
     */
    public abstract boolean editComment(JSONObject body, JSONObject editField);

    /* editComment - Edit a comment of a feed item (this is the actual implementation)
     *
     *  @return             : success or not
     *  @editField          : contains information about the operation parameters
     */
    boolean editComment(JSONObject editField) {
        CommentHandler comment = new CommentHandler(this);
        try {
            switch (editField.getString("operation")) {
                case "add":
                    return comment.add(editField);
                case "remove":
                    return comment.remove(editField);
                case "edit":
                    return comment.edit(editField);
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* editLikes - Edit the reactions of a feed item (is abstract)
     *
     *  @return             : success or not
     *  @body               : the network body request
     *  @editField          : contains information about the operation parameters
     */
    public abstract boolean editLikes(JSONObject body, JSONObject editField);

    /* editLikes - Edit the reactions of a feed item (is abstract)
     *
     *  @return             : success or not
     *  @operation          : add or remove
     *  @react              : like/dislike etc
     */
    boolean editLikes(String operation, String react) {
        try {
            synchronized (DatabaseManager.class) {
                JSONObject feed = fetch(getPath(), getCity());
                String uidCurrent = getBody().getString("uidCurrent");
                String uidAuthor = getBody().getString("uidAuthor");
                JSONObject userFeed = fetchUserFeed(feed.getJSONObject(getType()), uidAuthor);
                String id = getBody().getString(getIdField());

                if (!userFeed.has(id)) {
                    return false;
                }

                JSONObject item = userFeed.getJSONObject(id);
                JSONObject likes = item.getJSONObject("likes");
                JSONObject reactJson = likes.getJSONObject(react);

                LOGGER.log(Level.FINE, "User {0} wants to {1} {2} to the {3} {4} from {5} city",
                           new Object[]{uidCurrent, operation, react, getType(), id, getCity()});

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
                item.put("likes", likes);
                userFeed.put(id, item);
                feed.getJSONObject(getType()).put(uidAuthor, userFeed);

                return saveFeed(getPath(), getCity(), feed);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
