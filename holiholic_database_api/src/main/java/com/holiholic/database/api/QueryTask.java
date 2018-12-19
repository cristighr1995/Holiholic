package com.holiholic.database.api;

class QueryTask implements Runnable {
    private String query;

    QueryTask(String query) {
        this.query = query;
    }

    @Override
    public void run() {
        Query.executeUpdate(query);
    }
}
