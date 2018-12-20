package com.holiholic.feed.handler;

import org.json.JSONObject;

public class QuestionHandler extends Feed {
    @Override
    public boolean add() {
        return false;
    }

    @Override
    public boolean remove(String id) {
        return false;
    }

    @Override
    public boolean edit(String id) {
        return false;
    }

    @Override
    public void setModel(JSONObject model) {

    }
}
