package com.holiholic.places.api;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
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
import java.text.SimpleDateFormat;
import java.util.*;

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
                    System.out.println("New request to \"" + url + "\"");
                    System.out.println("Response:");
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

    private static String[] split(String string, String pattern) {
        return Iterables.toArray(Splitter.on(pattern).omitEmptyStrings().split(string), String.class);
    }

    private static JSONObject buildHourObject(String time, int day) {
        JSONObject hour = new JSONObject();
        hour.put("time", time);
        hour.put("day", day);
        return hour;
    }

    private static JSONObject formatHour(String time, int day) {
        try {
            SimpleDateFormat displayFormat = new SimpleDateFormat("HHmm");
            SimpleDateFormat parseFormat = new SimpleDateFormat("hh:mm a");
            Date date;
            switch (time) {
                case "Noon":
                    return buildHourObject("1200", day);
                case "Midnight":
                    return buildHourObject("0000", day);
                default:
                    date = parseFormat.parse(time);
                    return buildHourObject(displayFormat.format(date), day);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static JSONObject buildDayObject(int day, String renderedTime) {
        try {
            String[] hours = split(renderedTime, "\u2013");
            JSONObject dayObject = new JSONObject();
            JSONObject open, close;

            if (hours[0].equals("24 Hours")) {
                open = formatHour("00:01 AM", day);
                close = formatHour("11:59 PM", day);
                dayObject.put("open", open);
                dayObject.put("close", close);
                return dayObject;
            }

            open = formatHour(hours[0], day);
            close = formatHour(hours[1], day);

            if (open == null || close == null) {
                return null;
            }

            // the close time is the next day
            SimpleDateFormat displayFormat = new SimpleDateFormat("HHmm");
            Date openDate = displayFormat.parse(open.getString("time"));
            Date closeDate = displayFormat.parse(close.getString("time"));

            if (closeDate.before(openDate)) {
                close.put("day", (day % 7) + 1);
            }

            dayObject.put("open", open);
            dayObject.put("close", close);

            return dayObject;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String mergeRenderedTime(JSONArray open) {
        if (open.length() == 1) {
            return open.getJSONObject(0).getString("renderedTime");
        }

        String start = split(open.getJSONObject(0).getString("renderedTime"), "\u2013")[0];
        String end = split(open.getJSONObject(open.length() - 1).getString("renderedTime"), "\u2013")[1];
        return start + "\u2013" + end;
    }

    private static JSONArray buildPlaceHours(JSONArray timeFrames) {
        JSONArray hours = new JSONArray();
        JSONObject timeFrame;
        String daysInfo, renderedTime;
        String[] intervals, days;
        int start, end;

        try {
            // 24 hours open
            if (timeFrames.getJSONObject(0).getJSONArray("open").getJSONObject(0)
                    .getString("renderedTime").equals("24 Hours")) {
                JSONObject hour = new JSONObject();
                hour.put("open", buildHourObject("0000", 0));
                hours.put(hour);
                return hours;
            }

            for (int i = 0; i < timeFrames.length(); i++) {
                timeFrame = timeFrames.getJSONObject(i);
                daysInfo = timeFrame.getString("days");
                // skip today information
                if (daysInfo.equals("Today")) {
                    continue;
                }
                renderedTime = mergeRenderedTime(timeFrame.getJSONArray("open"));
                intervals = split(daysInfo, ", ");
                System.out.println("Decode " + daysInfo + " interval with " + renderedTime + " time");

                if (renderedTime.equals("None")) {
                    continue;
                }

                for (String interval : intervals) {
                    days = split(interval, "\u2013");
                    // just one day information
                    if (days.length == 1) {
                        try {
                            hours.put(buildDayObject(PlacesManager.getDays().get(days[0]), renderedTime));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        start = PlacesManager.getDays().get(days[0]);
                        end = PlacesManager.getDays().get(days[1]);

                        if (start < end) {
                            for (int day = start; day <= end; day++) {
                                hours.put(buildDayObject(day, renderedTime));
                            }
                        } else {
                            for (int day = start; day <= Calendar.SATURDAY; day++) {
                                hours.put(buildDayObject(day, renderedTime));
                            }
                            for (int day = Calendar.SUNDAY; day <= end; day++) {
                                hours.put(buildDayObject(day, renderedTime));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return hours;
    }

    private static JSONObject getPlaceDetails(String placeId, PlaceCategory placeCategory) {
        JSONObject place = new JSONObject();
        try {
            String url = UrlManager.getVenueDetailsUrl(placeId);
            JSONObject content = new JSONObject(Places.getContentFromUrl(url));
            JSONObject venue = content.getJSONObject("response").getJSONObject("venue");
            JSONArray timeFrames;

            System.out.println("Build information about \"" + venue.getString("name") + "\"");

            if (!venue.has("hours") && !venue.has("popular")) {
                return null;
            }
            if (venue.has("hours")) {
                timeFrames = buildPlaceHours(venue.getJSONObject("hours").getJSONArray("timeframes"));
            } else {
                timeFrames = buildPlaceHours(venue.getJSONObject("popular").getJSONArray("timeframes"));
            }
            if (timeFrames == null) {
                return null;
            }
            place.put("timeFrames", timeFrames);

            place.put("id", -1);
            place.put("name", venue.getString("name"));
            place.put("latitude", venue.getJSONObject("location").getDouble("lat"));
            place.put("longitude", venue.getJSONObject("location").getDouble("lng"));

            String description = "";
            if (venue.has("description")) {
                description = venue.getString("description");
            }
            place.put("description", description);

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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        System.out.println(place.toString());
        return place;
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
        System.out.println("Search for venues from " + placeCategory.getName() + " category");
        JSONArray searchList = searchPlaces(near, placeCategory.getId(), placeCategory.getLimit());
        JSONArray places = new JSONArray();

        for (int i = 0; i < searchList.length(); i++) {
            String placeId = searchList.getJSONObject(i).getString("id");
            JSONObject placeDetails = getPlaceDetails(placeId, placeCategory);
            if (placeDetails != null) {
                placeDetails.put("id", i);
                places.put(placeDetails);
            }
        }

        System.out.println("Gathered " + places.length() + " places from category \""
                           + placeCategory.getName() + "\" and topic \"" + placeCategory.getTopic() + "\"");
        return places;
    }

    private static double getDistance(JSONObject response, String type) {
        return response.getJSONArray("rows").getJSONObject(0).getJSONArray("elements").getJSONObject(0)
               .getJSONObject(type).getInt("value");
    }

    private static double[] getDistance(String origin, String destination, String type) {
        double[] distance = new double[2];
        Arrays.fill(distance, Integer.MAX_VALUE);

        try {
            String url = UrlManager.getDistanceMatrixUrl(origin, destination, type);
            JSONObject response = new JSONObject(getContentFromUrl(url));
            distance[0] = getDistance(response, "distance");
            distance[1] = getDistance(response, "duration");
        } catch (Exception e) {
            System.out.println("Got ZERO_RESULTS");
        }

        return distance;
    }

    private static String getLocation(JSONObject place) {
        return place.getDouble("latitude") + "," + place.getDouble("longitude");
    }

    public static Map<String, double[][]> getDistances(JSONArray places) {
        Map<String, double[][]> distances = new HashMap<>();
        try {
            int placesLength = places.length();
            double[][] distanceDriving = new double[placesLength][placesLength];
            double[][] durationDriving = new double[placesLength][placesLength];
            double[][] distanceWalking = new double[placesLength][placesLength];
            double[][] durationWalking = new double[placesLength][placesLength];
            double[] distanceResponse;
            String origin, destination;

            for (int i = 0; i < placesLength; i++) {
                for (int j = 0; j < placesLength; j++) {
                    if (i == j) {
                        continue;
                    }

                    origin = getLocation(places.getJSONObject(i));
                    destination = getLocation(places.getJSONObject(j));

                    distanceResponse = getDistance(origin, destination, "driving");
                    distanceDriving[i][j] = distanceResponse[0];
                    durationDriving[i][j] = distanceResponse[1];

                    distanceResponse = getDistance(origin, destination, "walking");
                    distanceWalking[i][j] = distanceResponse[0];
                    durationWalking[i][j] = distanceResponse[1];
                }
            }

            distances.put("distance_driving", distanceDriving);
            distances.put("duration_driving", durationDriving);
            distances.put("distance_walking", distanceWalking);
            distances.put("duration_walking", durationWalking);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return distances;
    }

}
