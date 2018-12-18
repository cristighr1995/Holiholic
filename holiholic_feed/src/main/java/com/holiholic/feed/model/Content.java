package com.holiholic.feed.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* Content - Model for the content object
 *
 */
public class Content {
    private String description;
    private List<String> images;
    private String recommendation;
    private String checkIn;

    public Content(String description, List<String> images, String recommendation, String checkIn) {
        this.description = description;
        this.images = images;
        this.recommendation = recommendation;
        this.checkIn = checkIn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getRecommendations() {
        return recommendation;
    }

    public void setRecommendations(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getCheckin() {
        return checkIn;
    }

    public void setCheckin(String checkIn) {
        this.checkIn = checkIn;
    }

    public JSONObject serialize() {
        JSONObject content = new JSONObject();
        content.put("description", this.description);
        content.put("images", new JSONArray(this.images));
        content.put("recommendation", this.recommendation);
        content.put("checkIn", this.checkIn);
        return content;
    }

    public static Content deserialize(JSONObject content) {
        JSONArray images = content.getJSONArray("images");
        List<String> imagesList = new ArrayList<>();
        for (int i = 0; i < images.length(); i++) {
            imagesList.add(images.getString(i));
        }
        return new Content(
                            content.getString("description"),
                            imagesList,
                            content.getString("recommendation"),
                            content.getString("checkIn"));
    }
}
