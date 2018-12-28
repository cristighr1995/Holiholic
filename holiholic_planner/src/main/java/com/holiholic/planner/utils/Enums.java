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
            switch (travelMode) {
                case DRIVING:
                    return "driving";
                case WALKING:
                    return "walking";
                default:
                    return "unknown";
            }
        }

        /* deserialize - Deserialize a string into a mode of travel
         *
         *  @return       : the corresponding instance
         */
        public static TravelMode deserialize(String travelMode) {
            switch (travelMode) {
                case "driving":
                    return DRIVING;
                case "walking":
                    return WALKING;
                default:
                    return UNKNOWN;
            }
        }
    }

    /* MealType - Meal types can be Lunch or Dinner
     *
     */
    public enum MealType {
        BREAKFAST, LUNCH, DINNER, UNKNOWN;

        /* serialize - Serialize the meal type in a string format
         *
         *  @return       : the serialized meal type
         */
        public static String serialize(MealType mealType) {
            switch (mealType) {
                case BREAKFAST:
                    return "breakfast";
                case LUNCH:
                    return "lunch";
                case DINNER:
                    return "dinner";
                default:
                    return "unknown";
            }
        }
    }

    public enum TravelInfo {
        DURATION, DISTANCE;

        /* serialize - Serialize the travel info in a string format
         *
         *  @return         : the serialized travel info
         *  @travelInfo     : the travel info
         */
        public static String serialize(TravelInfo travelInfo) {
            switch (travelInfo) {
                case DURATION:
                    return "duration";
                case DISTANCE:
                    return "distance";
                default:
                    return "unknown";
            }
        }
    }
}
