package com.holiholic.places.api;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class Places {

    static String getContentFromUrl(String url) {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);
        StringBuilder builder = new StringBuilder();

        try {
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (InputStream stream = entity.getContent()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        builder.append(line);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

        return builder.toString();
    }

    private static JSONArray searchPlaces(String near, String categoryId, int limit) {
        JSONArray places = new JSONArray();

        try {
            String url = UrlManager.getSearchVenuesUrl(near, categoryId, limit);
            JSONObject content = new JSONObject(getContentFromUrl(url));
            places = content.getJSONObject("response").getJSONArray("venues");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return places;
    }

    public static JSONArray getPlaces(String near, PlaceCategory placeCategory) {
        JSONArray searchList = searchPlaces(near, placeCategory.getId(), placeCategory.getLimit());
        JSONArray accumulator = new JSONArray();
        JSONArray places = new JSONArray();
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < searchList.length(); i++) {
            String placeId = searchList.getJSONObject(i).getString("id");
            tasks.add(new PlaceDetailsTask(placeId, i, accumulator, placeCategory));
        }

        ThreadManager.getInstance().invokeAll(tasks);

        // filter null results
        for (int i = 0; i < accumulator.length(); i++) {
            if (accumulator.isNull(i)) {
                continue;
            }
            places.put(accumulator.getJSONObject(i));
        }

        System.out.println("Gathered " + places.length() + " places from category \""
                           + placeCategory.getName() + "\" and topic \"" + placeCategory.getTopic() + "\"");
        return places;
    }
}
