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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class Places {

    private static String getContentFromUrl(String url) {
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

    private static JSONArray searchPlaces(String near, List<String> categoryIds) {
        JSONArray places = new JSONArray();

        try {
            String url = UrlManager.getSearchVenuesUrl(near, categoryIds);
            JSONObject content = new JSONObject(getContentFromUrl(url));
            places = content.getJSONObject("response").getJSONArray("venues");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return places;
    }

    static JSONObject buildPlaceInfo(String placeId) {
        JSONObject place = new JSONObject();

        try {
            String url = UrlManager.getVenueDetailsUrl(placeId);
            JSONObject content = new JSONObject(getContentFromUrl(url));
            JSONObject venue = content.getJSONObject("response").getJSONObject("venue");
            place.put("id", -1);
            place.put("name", venue.getString("name"));
            place.put("latitude", venue.getJSONObject("location").getDouble("lat"));
            place.put("longitude", venue.getJSONObject("location").getDouble("lng"));
            place.put("type", "attraction");

            String imagePrefix = venue.getJSONObject("bestPhoto").getString("prefix");
            String imageSuffix = venue.getJSONObject("bestPhoto").getString("suffix");
            place.put("imageUrl", imagePrefix + "original" + imageSuffix);

            double rating = 0;
            if (venue.has("rating")) {
                rating = venue.getDouble("rating");
            }
            place.put("rating", rating);

            JSONArray tags = new JSONArray();
            JSONArray categories = venue.getJSONArray("categories");
            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.getJSONObject(i);
                JSONObject tag = new JSONObject();
                tag.put("id", category.getString("id"));
                tag.put("name", category.getString("name"));
                tags.put(tag);
            }
            place.put("tags", tags);

            // decode time frames

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return place;
    }

    public static JSONArray getPlaces(String near, String categoryId) {
        return getPlaces(near, Arrays.asList(categoryId));
    }

    public static JSONArray getPlaces(String near, List<String> categoryIds) {
        JSONArray searchList = searchPlaces(near, categoryIds);
        JSONArray places = new JSONArray();
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < searchList.length(); i++) {
            String placeId = searchList.getJSONObject(i).getString("id");
            tasks.add(new PlaceDetailsTask(placeId, i, places));
        }

        ThreadManager.getInstance().invokeAll(tasks);
        return places;
    }
}
