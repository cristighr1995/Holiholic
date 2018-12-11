package com.holiholic.planner.utils;

/* Enums - All enumerations will be declared here
 *
 */
public class Enums {
    /* TravelMode - Modes of traveling between places
     *
     */
    public enum TravelMode {
        DRIVING, WALKING, UNKNOWN;

        /* serialize - Serialize the mode of travel in a string format
         *
         *  @return         : the serialized mode of travel
         *  @travelMode     : the travel mode
         */
        public static String serialize(TravelMode travelMode) {
            if (travelMode == DRIVING) {
                return "driving";
            } else if (travelMode == WALKING) {
                return "walking";
            }
            return "unknown";
        }

        /* deserialize - Deserialize a string into a mode of travel
         *
         *  @return       : the corresponding instance
         */
        public static TravelMode deserialize(String travelMode) {
            if (travelMode.equals("driving")) {
                return DRIVING;
            } else if (travelMode.equals("walking")) {
                return WALKING;
            }
            return UNKNOWN;
        }
    }

    /* MealType - Meal types can be Lunch or Dinner
     *
     */
    public enum MealType {
        LUNCH, DINNER, UNKNOWN;

        /* serialize - Serialize the meal type in a string format
         *
         *  @return       : the serialized meal type
         */
        public static String serialize(MealType mealType) {
            if (mealType == LUNCH) {
                return "lunch";
            } else if (mealType == DINNER) {
                return "dinner";
            }
            return "unknown";
        }

        /* deserialize - Deserialize a string into a meal type
         *
         *  @return       : the corresponding instance
         */
        public static MealType deserialize(String meal) {
            if (meal.equals("lunch")) {
                return LUNCH;
            } else if (meal.equals("dinner")) {
                return DINNER;
            }
            return UNKNOWN;
        }
    }

    public enum FileType {
        PLACES,
        RESTAURANTS,
        TRAFFIC_COEFFICIENTS,
        WEATHER_INFO,
        DISTANCE_DRIVING,
        DISTANCE_WALKING,
        DURATION_DRIVING,
        DURATION_WALKING;

        public static FileType[] getFileTypes(String travelMode) {
            return getFileTypes(TravelMode.deserialize(travelMode));
        }

        public static FileType[] getFileTypes(TravelMode travelMode) {
            switch (travelMode) {
                case DRIVING:
                    return new FileType[]{DURATION_DRIVING, DISTANCE_DRIVING};
                case WALKING:
                    return new FileType[]{DURATION_WALKING, DISTANCE_WALKING};
                default:
                    return null;
            }
        }

        public static FileType getDuration(TravelMode travelMode) {
            switch (travelMode) {
                case DRIVING:
                    return DURATION_DRIVING;
                case WALKING:
                    return DURATION_WALKING;
                default:
                    return null;
            }
        }

        public static FileType getDistance(TravelMode travelMode) {
            switch (travelMode) {
                case DRIVING:
                    return DISTANCE_DRIVING;
                case WALKING:
                    return DISTANCE_WALKING;
                default:
                    return null;
            }
        }

        public static String serialize(FileType fileType) {
            switch (fileType) {
                case PLACES:
                    return "places";
                case RESTAURANTS:
                    return "restaurants";
                case WEATHER_INFO:
                    return "weather information";
                case TRAFFIC_COEFFICIENTS:
                    return "traffic coefficients";
                case DURATION_DRIVING:
                    return "duration driving";
                case DURATION_WALKING:
                    return "duration_walking";
                case DISTANCE_DRIVING:
                    return "distance driving";
                case DISTANCE_WALKING:
                    return "distance walking";
                default:
                    return null;
            }
        }
    }
}
