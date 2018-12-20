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
import java.util.regex.Pattern;

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

    private static JSONArray searchPlaces(String near, String categoryId) {
        JSONArray places = new JSONArray();

        try {
            String url = UrlManager.getSearchVenuesUrl(near, categoryId);
            JSONObject content = new JSONObject(getContentFromUrl(url));
            places = content.getJSONObject("response").getJSONArray("venues");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return places;
    }

    private static JSONObject buildHourObject(String time, int day) {
        JSONObject hour = new JSONObject();
        hour.put("time", time);
        hour.put("day", day);
        return hour;
    }

    private static String decodeHour(String hour) {

    }

    private static JSONObject buildDayObject(int day, String renderedTime) {
        String[] hours = renderedTime.split(Pattern.quote(" -"));
        JSONObject dayObject = new JSONObject();
        int index = 0;

        switch (hours[0]) {
            case "Noon":
                dayObject.put(buildHourObject("open", "1200", day));

        }
    }

    private static JSONArray buildPlaceHours(JSONArray timeframes) {
        JSONArray hours = new JSONArray();
        // check if is 24 hours open
        if (timeframes.getJSONObject(0).getJSONArray("open").getJSONObject(0).getString("renderedTime").equals("24 Hours")) {
            JSONObject hour = new JSONObject();
            hour.put("open", buildHourObject("0000", 0));
            hours.put(hour);
            return hours;
        }

        for (int i = 0; i < timeframes.length(); i++) {
            JSONObject timeframe = timeframes.getJSONObject(i);
            String daysInfo = timeframe.getString("days");
            // skip today information
            if (daysInfo.equals("Today")) {
                continue;
            }
            String renderedTime = timeframe.getJSONArray("open").getJSONObject(0).getString("renderedTime");
            String[] days = daysInfo.split(Pattern.quote("-"));
            // just one day information
            if (days.length == 1) {
                hours.put(buildDayObject(PlacesManager.getDays().get(days[0]), renderedTime));
            }
        }
    }

    static JSONObject buildPlaceInfo(String placeId, PlaceCategory placeCategory) {
        JSONObject place = new JSONObject();

        try {
            String url = UrlManager.getVenueDetailsUrl(placeId);
            JSONObject content = new JSONObject(getContentFromUrl(url));
            JSONObject venue = content.getJSONObject("response").getJSONObject("venue");

            if (!venue.has("hours") && !venue.has("popular")) {
                return null;
            }
            if (venue.has("hours")) {
                if (!venue.getJSONObject("hours").getBoolean("isOpen")) {
                    return null;
                }
                place.put("timeFrames", buildPlaceHours(venue.getJSONObject("hours").getJSONArray("timeframes")));
            } else {
                if (!venue.getJSONObject("popular").getBoolean("isOpen")) {
                    return null;
                }
                place.put("timeFrames", buildPlaceHours(venue.getJSONObject("popular").getJSONArray("timeframes")));
            }

            place.put("id", -1);
            place.put("name", venue.getString("name"));
            place.put("latitude", venue.getJSONObject("location").getDouble("lat"));
            place.put("longitude", venue.getJSONObject("location").getDouble("lng"));

            String imagePrefix = venue.getJSONObject("bestPhoto").getString("prefix");
            String imageSuffix = venue.getJSONObject("bestPhoto").getString("suffix");
            place.put("imageUrl", imagePrefix + "original" + imageSuffix);

            double rating = 0;
            if (venue.has("rating")) {
                rating = venue.getDouble("rating");
            }
            place.put("rating", rating);

            JSONObject category = new JSONObject();
            category.put("name", placeCategory.getName());
            category.put("topic", placeCategory.getTopic());
            place.put("category", category);
            place.put("duration", placeCategory.getDuration());

            // decode time frames

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return place;
    }

    public static JSONArray getPlaces(String near, PlaceCategory placeCategory) {
        JSONArray searchList = searchPlaces(near, placeCategory.getId());
        JSONArray places = new JSONArray();
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < searchList.length(); i++) {
            String placeId = searchList.getJSONObject(i).getString("id");
            tasks.add(new PlaceDetailsTask(placeId, i, places, placeCategory));
        }

        ThreadManager.getInstance().invokeAll(tasks);
        return places;
    }
}
