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
         *  @return       : the serialized mode of travel
         */
        public static String serialize(TravelMode modeOfTravel) {
            if (modeOfTravel == DRIVING) {
                return "driving";
            } else if (modeOfTravel == WALKING) {
                return "walking";
            }
            return "unknown";
        }

        /* deserialize - Deserialize a string into a mode of travel
         *
         *  @return       : the corresponding instance
         */
        public static TravelMode deserialize(String modeOfTravel) {
            if (modeOfTravel.equals("driving")) {
                return DRIVING;
            } else if (modeOfTravel.equals("walking")) {
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
}
