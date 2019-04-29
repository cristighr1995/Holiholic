package com.holiholic.places.api;

public enum PlaceCategoryType {
    STARTING_POINT,
    TOURISTIC_OBJECTIVE,
    UNKNOWN;

    public static PlaceCategoryType deserialize(String placeCategoryType) {
        switch (placeCategoryType) {
            case "startingPoint":
                return STARTING_POINT;
            case "touristicObjective":
                return TOURISTIC_OBJECTIVE;
            default:
                return UNKNOWN;
        }
    }

    public static String serialize(PlaceCategoryType placeCategoryType) {
        switch (placeCategoryType) {
            case STARTING_POINT:
                return "startingPoint";
            case TOURISTIC_OBJECTIVE:
                return "touristicObjective";
            default:
                return "unknown";
        }
    }
}
