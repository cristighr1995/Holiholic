package com.holiholic.database.dataStructures;

import java.util.Map;

public class Questions {
    private Map<String, Map<String, Question>> question;
    private int questionCount;

    public Questions() {
    }

    public Map<String, Map<String, Question>> getQuestions() {
        return question;
    }

    public void setQuestions(Map<String, Map<String, Question>> question) {
        this.question = question;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public Questions(Map<String, Map<String, Question>> question, int questionCount) {
        this.question = question;
        this.questionCount = questionCount;
    }
}
