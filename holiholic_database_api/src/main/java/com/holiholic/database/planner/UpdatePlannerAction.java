package com.holiholic.database.planner;

/* UpdatePlannerAction - Handle the update planner operations
 *
 */
public abstract class UpdatePlannerAction {

    /* Factory - Create an instance of update action
     *
     */
    public static class Factory {
        public static UpdatePlannerAction getInstance(String type) {
            switch (type) {
                case "places":
                    return new UpdatePlacesAction();
                case "matrix":
                    return new UpdateMatrixAction();
                default:
                    return null;
            }
        }
    }

    /* execute - Execute the action for a specific city
     *
     *  @return             : success or not
     *  @cityName           : city name
     */
    public abstract boolean execute(String cityName);
}
