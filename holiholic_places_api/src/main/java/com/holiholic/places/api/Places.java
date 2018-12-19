package com.holiholic.places.api;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

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

    public static JSONArray getPlaces(String near, List<String> categoryIds) {
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
}
