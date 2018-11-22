package com.holiholic.database;

import com.holiholic.database.constant.Constants;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* DatabaseManager - Handle the requests to the database
 *
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    /* setLogger - This method should be changed when release application
     *             Sets the logger to print to console (instead of a file)
     *
     *  @return             : void
     */
    static void setLogger() {
        // add logger handler
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);

        LOGGER.addHandler(ch);
        LOGGER.setLevel(Level.ALL);
    }

    /* generateMD5 - Generates an md5 key for a plain text
     *
     *  @return             : the md5 key
     *  @plain              : the plain text we want to hash
     */
    public static String generateMD5(String plain) {
        String hash = null;
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(plain.getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            hash = bigInt.toString(16);
            // pad with zeros to have full 32 characters
            if (hash.length() != 32) {
                int diff = 32 - hash.length();
                StringBuilder sb = new StringBuilder();
                for (int d = 0; d < diff; d++) {
                    sb.append("0");
                }
                hash = sb.append(hash).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return hash;
    }

    /* getUsers - Retrieve users from database
     *
     *  @return             : users in the json format
     */
    private static JSONObject getUsers() {
        try {
            InputStream is = new FileInputStream(Constants.USERS_DB_PATH);
            String text = IOUtils.toString(is, "UTF-8");
            return new JSONObject(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* createProfile - Create a new profile for the current user to be stored in the database
     *
     *  @return             : profile (json format)
     *  @request            : user information
     *  @id                 : the id (unique) for the current user
     */
    private static JSONObject createProfile(JSONObject request, int id) {
        JSONObject profile = new JSONObject();
        profile.put("name", request.getString("name"));
        profile.put("email", request.getString("email"));
        profile.put("imageUrl", request.getString("imageUrl"));
        profile.put("id", id);
        return profile;
    }

    /* updateUsers - Save the users in the database
     *
     *  @return             : true/false (success or not)
     *  @users              : all users (json format)
     */
    private static boolean updateUsers(JSONObject users) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(Constants.USERS_DB_PATH), 32768);
            out.write(users.toString(2));
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* registerUser - Save the new user in the database
     *
     *  @return             : true/false (success or not)
     *  @request            : the json object containing information about the new user
     */
    public static boolean registerUser(JSONObject request) {
        try {
            LOGGER.log(Level.FINE, "Started registration for user {0}", request.getString("md5Key"));

            synchronized (DatabaseManager.class) {
                JSONObject users = getUsers();
                if (users == null) {
                    return false;
                }
                users.put(request.getString("md5Key"), createProfile(request, users.length()));
                if (!updateUsers(users)) {
                    return false;
                }
            }

            LOGGER.log(Level.FINE, "Successfully updated registration for user {0}", request.getString("md5Key"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* containsUser - Check if the current user is already in the database
     *
     *  @return             : true/false (exists or not)
     *  @md5Key             : the unique identifier for the current user
     */
    public static boolean containsUser(String md5Key) {
        JSONObject users = getUsers();
        if (users == null) {
            return false;
        }
        return users.has(md5Key);
    }

    /* fetchQuestions - Get questions for a specific city from the database
     *
     *  @return             : questions for the city
     *  @city               : the city the user wants to see questions
     */
    private static JSONObject fetchQuestions(String city) {
        try {
            InputStream is = new FileInputStream(Constants.QUESTIONS_DB_PATH + city.toLowerCase() + ".json");
            String text = IOUtils.toString(is, "UTF-8");
            return new JSONObject(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* saveQuestions - Saves in the database the updates for the question
     *
     *  @return             : success or not
     *  @city               : the city the user wants to see questions
     *  @questions          : the updated questions
     */
    private static boolean saveQuestions(String city, JSONObject questions) {
        try {
            String filename = Constants.QUESTIONS_DB_PATH + city + ".json";
            BufferedWriter out = new BufferedWriter(new FileWriter(filename), 32768);
            out.write(questions.toString(2));
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* updateQuestion - Updates a specific question
     *
     *  @return             : success or not
     *  @questionBody       : the question body (json format)
     */
    public static boolean updateQuestion(JSONObject questionBody) {
        try {
            String operation = questionBody.getString("operation");
            String city = questionBody.getString("city");
            LOGGER.log(Level.FINE, "Started {0} question operation from city {2}", new Object[]{operation, city});

            switch (operation) {
                case "add":
                    return addQuestion(questionBody);
                case "remove":
                    return removeQuestion(questionBody);
                case "edit":
                    return editQuestion(questionBody);
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* createEmptyLikesObject - Create an empty json object for the likes (reacts)
     *                          In the future we can add more than just 2 reactions
     *
     *  @return             : the react json
     */
    private static JSONObject createEmptyLikesObject() {
        JSONObject likes = new JSONObject();
        likes.put("like", new JSONObject());
        likes.put("dislike", new JSONObject());
        return likes;
    }

    /* addQuestion - Add a question in the database
     *
     *  @return             : success or not
     *  @questionBody       : the question body (json format)
     */
    private static boolean addQuestion(JSONObject questionBody) {
        String md5KeyQuestionAuthor = questionBody.getString("md5KeyQuestionAuthor");
        if (!containsUser(md5KeyQuestionAuthor)) {
            return false;
        }

        try {
            synchronized (DatabaseManager.class) {
                String city = questionBody.getString("city");
                JSONObject questions = fetchQuestions(city);
                int questionsCount = questions.getInt("questionsCount");
                JSONObject userQuestions;
                if (questions.getJSONObject("questions").has(md5KeyQuestionAuthor)) {
                    userQuestions = questions.getJSONObject("questions").getJSONObject(md5KeyQuestionAuthor);
                } else {
                    userQuestions = new JSONObject();
                }

                // remove operation
                questionBody.remove("operation");
                // empty list of comments
                questionBody.put("comments", new JSONArray());
                // zero likes
                questionBody.put("likes", createEmptyLikesObject());
                // set an id
                questionBody.put("qid", generateMD5(questionBody.toString()));
                userQuestions.put(questionBody.getString("qid"), questionBody);

                // update questions
                questions.put("questionsCount", questionsCount + 1);
                questions.getJSONObject("questions").put(md5KeyQuestionAuthor, userQuestions);

                return saveQuestions(city, questions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* addQuestion - Remove a question from the database
     *
     *  @return             : success or not
     *  @questionBody       : the question body (json format)
     */
    private static boolean removeQuestion(JSONObject questionBody) {
        try {
            synchronized (DatabaseManager.class) {
                String city = questionBody.getString("city");
                JSONObject questions = fetchQuestions(city);
                int questionsCount = questions.getInt("questionsCount");
                String md5KeyCurrent = questionBody.getString("md5KeyCurrent");
                String md5KeyQuestionAuthor = questionBody.getString("md5KeyQuestionAuthor");

                if (!md5KeyCurrent.equals(md5KeyQuestionAuthor)) {
                    return false;
                }
                if (!questions.getJSONObject("questions").has(md5KeyQuestionAuthor)) {
                    return true;
                }

                JSONObject userQuestions = questions.getJSONObject("questions").getJSONObject(md5KeyQuestionAuthor);
                String qid = questionBody.getString("qid");

                if (!userQuestions.has(qid)) {
                    return true;
                }

                userQuestions.remove(qid);
                // update questions
                questions.put("questionsCount", questionsCount - 1);
                questions.getJSONObject("questions").put(md5KeyQuestionAuthor, userQuestions);

                return saveQuestions(city, questions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* editQuestionTitle - Edits the title of a question
     *
     *  @return             : success or not
     *  @questionBody       : the question body (json format)
     *  @title              : the new title for the question
     */
    private static boolean editQuestionTitle(JSONObject questionBody, String title) {
       try {
           synchronized (DatabaseManager.class) {
               String city = questionBody.getString("city");
               JSONObject questions = fetchQuestions(city);
               String md5KeyCurrent = questionBody.getString("md5KeyCurrent");
               String md5KeyQuestionAuthor = questionBody.getString("md5KeyQuestionAuthor");

               if (!md5KeyCurrent.equals(md5KeyQuestionAuthor)) {
                   return false;
               }
               if (!questions.getJSONObject("questions").has(md5KeyQuestionAuthor)) {
                   return false;
               }

               JSONObject userQuestions = questions.getJSONObject("questions").getJSONObject(md5KeyQuestionAuthor);
               String qid = questionBody.getString("qid");

               if (!userQuestions.has(qid)) {
                   return false;
               }

               JSONObject question = userQuestions.getJSONObject(qid);
               question.put("title", title);
               userQuestions.put(qid, question);
               questions.getJSONObject("questions").put(md5KeyQuestionAuthor, userQuestions);

               return saveQuestions(city, questions);
           }
       } catch (Exception e) {
           e.printStackTrace();
           return false;
       }
    }

    /* addQuestionComment - Add a comment to a specific question
     *
     *  @return             : success or not
     *  @questionBody       : the question body (json format)
     *  @editField          : contains information about the comment like time stamp, author, comment etc
     */
    private static boolean addQuestionComment(JSONObject questionBody, JSONObject editField) {
        try {
            synchronized (DatabaseManager.class) {
                String city = questionBody.getString("city");
                JSONObject questions = fetchQuestions(city);
                String md5KeyCurrent = questionBody.getString("md5KeyCurrent");
                String md5KeyQuestionAuthor = questionBody.getString("md5KeyQuestionAuthor");

                if (!questions.getJSONObject("questions").has(md5KeyQuestionAuthor)) {
                    return false;
                }

                JSONObject userQuestions = questions.getJSONObject("questions").getJSONObject(md5KeyQuestionAuthor);
                String qid = questionBody.getString("qid");

                if (!userQuestions.has(qid)) {
                    return false;
                }

                JSONObject newComment = new JSONObject();
                newComment.put("md5KeyCommAuthor", md5KeyCurrent);
                newComment.put("comment", editField.getString("comment"));
                newComment.put("timeStamp", editField.getString("timeStamp"));
                newComment.put("commId", generateMD5(newComment.toString()));

                JSONObject question = userQuestions.getJSONObject(qid);
                question.getJSONArray("comments").put(newComment);
                userQuestions.put(qid, question);
                questions.getJSONObject("questions").put(md5KeyQuestionAuthor, userQuestions);

                return saveQuestions(city, questions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* removeQuestionComment - Remove a comment from a specific question
     *
     *  @return             : success or not
     *  @questionBody       : the question body (json format)
     *  @editField          : contains information about the comment like comment id, user that wants to remove etc
     */
    private static boolean removeQuestionComment(JSONObject questionBody, JSONObject editField) {
        try {
            synchronized (DatabaseManager.class) {
                String city = questionBody.getString("city");
                JSONObject questions = fetchQuestions(city);
                String md5KeyCurrent = questionBody.getString("md5KeyCurrent");
                String md5KeyQuestionAuthor = questionBody.getString("md5KeyQuestionAuthor");

                if (!questions.getJSONObject("questions").has(md5KeyQuestionAuthor)) {
                    return false;
                }

                JSONObject userQuestions = questions.getJSONObject("questions").getJSONObject(md5KeyQuestionAuthor);
                String qid = questionBody.getString("qid");

                if (!userQuestions.has(qid)) {
                    return false;
                }

                JSONObject question = userQuestions.getJSONObject(qid);
                JSONArray comments = question.getJSONArray("comments");

                int commentIndex = -1;
                for (int i = 0; i < comments.length(); i++) {
                    if (comments.getJSONObject(i).getString("commId").equals(editField.getString("commId"))) {
                        commentIndex = i;
                        break;
                    }
                }
                if (commentIndex == -1) {
                    return false;
                }

                JSONObject comment = comments.getJSONObject(commentIndex);
                if (!comment.getString("md5KeyCommAuthor").equals(md5KeyCurrent)) {
                    return false;
                }

                comments.remove(commentIndex);
                question.put("comments", comments);
                userQuestions.put(qid, question);
                questions.getJSONObject("questions").put(md5KeyQuestionAuthor, userQuestions);

                return saveQuestions(city, questions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* editQuestionCommentContent - Edit the message of a specific comment
     *
     *  @return             : success or not
     *  @questionBody       : the question body (json format)
     *  @editField          : contains information about the comment like comment id, author, new message etc
     */
    private static boolean editQuestionCommentContent(JSONObject questionBody, JSONObject editField) {
        try {
            synchronized (DatabaseManager.class) {
                String city = questionBody.getString("city");
                JSONObject questions = fetchQuestions(city);
                String md5KeyCurrent = questionBody.getString("md5KeyCurrent");
                String md5KeyQuestionAuthor = questionBody.getString("md5KeyQuestionAuthor");

                if (!questions.getJSONObject("questions").has(md5KeyQuestionAuthor)) {
                    return false;
                }

                JSONObject userQuestions = questions.getJSONObject("questions").getJSONObject(md5KeyQuestionAuthor);
                String qid = questionBody.getString("qid");

                if (!userQuestions.has(qid)) {
                    return false;
                }

                JSONObject question = userQuestions.getJSONObject(qid);
                JSONArray comments = question.getJSONArray("comments");

                int commentIndex = -1;
                for (int i = 0; i < comments.length(); i++) {
                    if (comments.getJSONObject(i).getString("commId").equals(editField.getString("commId"))) {
                        commentIndex = i;
                        break;
                    }
                }
                if (commentIndex == -1) {
                    return false;
                }

                JSONObject comment = comments.getJSONObject(commentIndex);
                if (!comment.getString("md5KeyCommAuthor").equals(md5KeyCurrent)) {
                    return false;
                }

                comment.put("comment", editField.getString("comment"));
                comments.put(commentIndex, comment);
                question.put("comments", comments);
                userQuestions.put(qid, question);
                questions.getJSONObject("questions").put(md5KeyQuestionAuthor, userQuestions);

                return saveQuestions(city, questions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* editQuestionComment - Edit the comment of a specific comment
     *
     *  @return             : success or not
     *  @questionBody       : the question body (json format)
     *  @editField          : contains information about the operation (add/remove/edit comment) and edit information
     */
    private static boolean editQuestionComment(JSONObject questionBody, JSONObject editField) {
        try {
            switch (editField.getString("operation")) {
                case "add":
                    return addQuestionComment(questionBody, editField);
                case "remove":
                    return removeQuestionComment(questionBody, editField);
                case "edit":
                    return editQuestionCommentContent(questionBody, editField);
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* editQuestionLikes - Edit the likes of a specific question
     *
     *  @return             : success or not
     *  @questionBody       : the question body (json format)
     *  @editField          : contains information about the operation (add/remove reacts) and edit information
     */
    private static boolean editQuestionLikes(JSONObject questionBody, JSONObject editField) {
        try {
            synchronized (DatabaseManager.class) {
                String city = questionBody.getString("city");
                JSONObject questions = fetchQuestions(city);
                String md5KeyCurrent = questionBody.getString("md5KeyCurrent");
                String md5KeyQuestionAuthor = questionBody.getString("md5KeyQuestionAuthor");

                if (!questions.getJSONObject("questions").has(md5KeyQuestionAuthor)) {
                    return false;
                }

                JSONObject userQuestions = questions.getJSONObject("questions").getJSONObject(md5KeyQuestionAuthor);
                String qid = questionBody.getString("qid");

                if (!userQuestions.has(qid)) {
                    return false;
                }

                JSONObject question = userQuestions.getJSONObject(qid);
                JSONObject likes = question.getJSONObject("likes");
                String operation = editField.getString("operation");
                String react = editField.getString("react");
                JSONObject reactJson = likes.getJSONObject(react);

                switch (operation) {
                    case "add":
                        if (reactJson.has(md5KeyCurrent)) {
                            return true;
                        }
                        reactJson.put(md5KeyCurrent, true);
                        break;
                    case "remove":
                        if (!reactJson.has(md5KeyCurrent)) {
                            return true;
                        }
                        reactJson.remove(md5KeyCurrent);
                        break;
                    default:
                        return false;
                }

                likes.put(react, reactJson);
                question.put("likes", likes);
                userQuestions.put(qid, question);
                questions.getJSONObject("questions").put(md5KeyQuestionAuthor, userQuestions);

                return saveQuestions(city, questions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* editQuestion - Edits a question - the user can do multiple edits like:
     *                change the title,
     *                add/remove/edit comments,
     *                like/dislike
     *
     *  @return             : success or not
     *  @questionBody       : the question body (json format)
     *  @title              : the new title for the question
     */
    private static boolean editQuestion(JSONObject questionBody) {
        try {
            JSONObject editField = questionBody.getJSONObject("editField");
            assert editField.length() == 1;
            String field = editField.keys().next();

            switch (field) {
                case "title":
                    return editQuestionTitle(questionBody, editField.getString(field));
                case "comments":
                    return editQuestionComment(questionBody, editField.getJSONObject(field));
                case "likes":
                    return editQuestionLikes(questionBody, editField.getJSONObject(field));
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* getQuestions - Get a list of questions for a specific city
     *                Each question has only the last comment
     *
     *  @return             : a list of questions (json string format)
     *  @city               : the requested city
     *  @md5KeyCurrent      : unique identifier for the current user
     */
    public static String getQuestions(String city, String md5KeyCurrent) {
        try {
            if (!containsUser(md5KeyCurrent)) {
                return new JSONArray().toString(2);
            }

            JSONObject questions = fetchQuestions(city).getJSONObject("questions");
            JSONArray result = new JSONArray();
            // for each author
            for (String author : questions.keySet()) {
                // for each question
                for (String qid : questions.getJSONObject(author).keySet()) {
                    JSONObject question = questions.getJSONObject(author).getJSONObject(qid);
                    // display only the last comment
                    JSONArray comments = question.getJSONArray("comments");
                    JSONObject comment = comments.getJSONObject(comments.length() - 1);
                    question.remove("comments");
                    question.put("comments", new JSONArray().put(comment));
                    // display only the number of reactions
                    JSONObject likes = question.getJSONObject("likes");
                    // for each react store only the count
                    for (String react : likes.keySet()) {
                        likes.put(react, likes.getJSONObject(react).length());
                    }
                    question.put("likes", likes);
                    result.put(question);
                }
            }
            return result.toString(2);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray().toString(2);
        }
    }

    /* getQuestionDetails - Get details for a specific question
     *
     *  @return                 : the details for a question (json string format)
     *  @city                   : the requested city
     *  @qid                    : unique identifier for the question
     *  @md5KeyCurrent          : unique identifier for the current user
     *  @md5KeyQuestionAuthor   : the id for the user who wrote the question
     */
    public static String getQuestionDetails(String city,
                                            String qid,
                                            String md5KeyCurrent,
                                            String md5KeyQuestionAuthor) {
        try {
            if (!containsUser(md5KeyCurrent)) {
                return new JSONArray().toString(2);
            }

            JSONObject questions = fetchQuestions(city).getJSONObject("questions");
            if (!questions.has(md5KeyQuestionAuthor)) {
                return new JSONArray().toString(2);
            }

            JSONObject questionsAuthor = questions.getJSONObject(md5KeyQuestionAuthor);
            if (!questionsAuthor.has(qid)) {
                return new JSONArray().toString(2);
            }

            return questionsAuthor.getJSONObject(qid).toString(2);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray().toString(2);
        }
    }

    /* fetchTopics - Get in memory json for user topics
     *
     *  @return             : the topics json
     */
    private static JSONObject fetchTopics() {
        try {
            InputStream is = new FileInputStream(Constants.TOPICS_DB_PATH);
            String text = IOUtils.toString(is, "UTF-8");
            return new JSONObject(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* fetchAvailableTopics - Get all available topics to follow
     *
     *  @return             : all available topics (json array format)
     */
    private static JSONArray fetchAvailableTopics() {
        try {
            InputStream is = new FileInputStream(Constants.AVAILABLE_TOPICS_DB_PATH);
            String text = IOUtils.toString(is, "UTF-8");
            return new JSONArray(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* getTopics - Get topics for a specific user
     *             In case the user does not have any topics to follow return empty json array
     *
     *  @return             : topics for a specific user (json in string format)
     *  @md5Key             : unique identifier for the current user
     */
    public static String getTopics(String md5Key) {
        JSONObject topics = fetchTopics();
        JSONObject response = new JSONObject();
        response.put("availableTopics", fetchAvailableTopics());
        assert topics != null;
        if (topics.has(md5Key)) {
            response.put("userTopics", topics.getJSONArray(md5Key));
        } else {
            response.put("userTopics", new JSONArray());
        }
        return response.toString(2);
    }

    /* saveTopics - Save in the database the updated topics
     *
     *  @return             : success or not
     *  @topics             : the topics to save in database
     */
    private static boolean saveTopics(JSONObject topics) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(Constants.TOPICS_DB_PATH), 32768);
            out.write(topics.toString(2));
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* updateTopics - Save in the database the updated topics for a specific user given the request
     *
     *  @return             : success or not
     *  @request            : json containing information about the current user and the topics he wants to follow
     */
    public static boolean updateTopics(JSONObject request) {
        try {
            LOGGER.log(Level.FINE, "Started updating topics for user {0}", request.getString("md5Key"));

            String md5Key = request.getString("md5Key");
            JSONArray followedTopics = request.getJSONArray("followedTopics");

            if (!containsUser(md5Key)) {
                return false;
            }

            synchronized (DatabaseManager.class) {
                JSONObject topics = fetchTopics();
                assert topics != null;
                topics.put(md5Key, followedTopics);
                if (!saveTopics(topics)) {
                    return false;
                }
            }

            LOGGER.log(Level.FINE, "Successfully updated topics for user {0}", request.getString("md5Key"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* fetchGraph - Get in memory json follow graph
     *
     *  @return             : the follow graph (json format)
     */
    private static JSONObject fetchGraph() {
        try {
            InputStream is = new FileInputStream(Constants.PEOPLE_DB_PATH);
            String text = IOUtils.toString(is, "UTF-8");
            return new JSONObject(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* getTopics - Get following people for a specific user
     *             In case the user does not have any following people return empty json array
     *
     *  @return             : following people for a specific user (json in string format)
     *  @md5Key             : unique identifier for the current user
     */
    public static String getPeople(String md5Key) {
        JSONObject followGraph = fetchGraph();
        assert followGraph != null;
        if (followGraph.has(md5Key)) {
            return followGraph.getJSONArray(md5Key).toString(2);
        }
        return new JSONArray().toString(2);
    }

    /* updatePeople - Save in the database the updated edge between two users given the request
     *
     *  @return             : success or not
     *  @followGraphEdge    : json containing information about the edge (from, to) and operation
     */
    public static boolean updatePeople(JSONObject followGraphEdge) {
        try {
            String from = followGraphEdge.getString("md5KeyFrom");
            String to = followGraphEdge.getString("md5KeyTo");
            String operation = followGraphEdge.getString("operation");

            LOGGER.log(Level.FINE, "Started {0} operation from user {1} to user {2}", new Object[]{operation,
                                                                                      from,
                                                                                      to});
            if (!containsUser(from) || !containsUser(to)) {
                return false;
            }
            switch (operation) {
                case "follow":
                    return addFollowGraphEdge(from, to);
                case "unfollow":
                    return removeFollowGraphEdge(from, to);
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* removeFollowGraphEdge - Removes the edge between from and to
     *
     *  @return             : success or not
     *  @from               : unique identifier for the current user
     *  @to                 : unique identifier for the user he wants to unfollow
     */
    private static boolean removeFollowGraphEdge(String from, String to) {
        try {
            synchronized (DatabaseManager.class) {
                JSONObject followGraph = fetchGraph();
                JSONArray followers;
                assert followGraph != null;
                if (followGraph.has(from)) {
                    followers = followGraph.getJSONArray(from);
                } else {
                    followers = new JSONArray();
                }

                int indexToRemove = -1;
                for (int i = 0; i < followers.length(); i++) {
                    if (followers.getString(i).equals(to)) {
                        indexToRemove = i;
                        break;
                    }
                }
                if (indexToRemove != -1) {
                    followers.remove(indexToRemove);
                }

                followGraph.put(from, followers);
                if (!saveFollowGraph(followGraph)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* addFollowGraphEdge - Adds an edge between from and to
     *
     *  @return             : success or not
     *  @from               : unique identifier for the current user
     *  @to                 : unique identifier for the user he wants to follow
     */
    private static boolean addFollowGraphEdge(String from, String to) {
        try {
            synchronized (DatabaseManager.class) {
                JSONObject followGraph = fetchGraph();
                JSONArray followers;
                assert followGraph != null;
                if (followGraph.has(from)) {
                    followers = followGraph.getJSONArray(from);
                } else {
                    followers = new JSONArray();
                }

                for (int i = 0; i < followers.length(); i++) {
                    if (followers.getString(i).equals(to)) {
                        return true;
                    }
                }

                followers.put(to);
                followGraph.put(from, followers);
                if (!saveFollowGraph(followGraph)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* saveFollowGraph - Save in the database the updated follow graph
     *
     *  @return             : success or not
     *  @followGraph        : the follow graph to save in database
     */
    private static boolean saveFollowGraph(JSONObject followGraph) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(Constants.PEOPLE_DB_PATH), 32768);
            out.write(followGraph.toString(2));
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
