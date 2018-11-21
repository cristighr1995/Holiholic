package com.holiholic.database;

import com.holiholic.database.constant.Constants;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
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
            LOGGER.log(Level.FINE, "Started updating the users database");

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

            LOGGER.log(Level.FINE, "Successfully updated the users database");
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
                questionBody.put("likes", 0);
                // set an id
                questionBody.put("id", questionsCount);
                userQuestions.put(String.valueOf(questionsCount), questionBody);

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
                if (!questions.getJSONObject("questions").has(md5KeyCurrent)) {
                    return true;
                }

                JSONObject userQuestions = questions.getJSONObject("questions").getJSONObject(md5KeyCurrent);
                String qid = String.valueOf(questionBody.getInt("qid"));

                if (!userQuestions.has(qid)) {
                    return true;
                }

                userQuestions.remove(qid);
                // update questions
                questions.put("questionsCount", questionsCount - 1);
                questions.getJSONObject("questions").put(md5KeyCurrent, userQuestions);

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
               if (!questions.getJSONObject("questions").has(md5KeyCurrent)) {
                   return false;
               }

               JSONObject userQuestions = questions.getJSONObject("questions").getJSONObject(md5KeyCurrent);
               String qid = String.valueOf(questionBody.getInt("qid"));

               if (!userQuestions.has(qid)) {
                   return false;
               }

               JSONObject question = userQuestions.getJSONObject(qid);
               question.put("title", title);
               userQuestions.put(qid, question);
               questions.getJSONObject("questions").put(md5KeyCurrent, userQuestions);

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
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getQuestions(String city, String md5Key) {
        return null;
    }

    public static String getQuestionDetails(String city, String qid, String md5Key) {
        return null;
    }
}
