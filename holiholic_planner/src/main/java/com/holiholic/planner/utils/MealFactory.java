package com.holiholic.planner.utils;

/* MealFactory - Creates an instance of a Meal
 *
 */
public class MealFactory {
    /* getInstance - Get an instance of a Meal
     *
     *  @return             : the meal instance
     *  @mealType           : the meal type
     *  @interval           : the interval to plan the meal
     *  @duration           : how much to stay at the restaurant (expressed in minutes)
     */
    public static Meal getInstance(Enums.MealType mealType,
                                   Interval interval,
                                   int duration) {
        switch (mealType) {
            case LUNCH:
                return new Meal(interval, duration);
            case DINNER:
                return new Meal(interval, duration);
            default:
                return null;
        }
    }
}
