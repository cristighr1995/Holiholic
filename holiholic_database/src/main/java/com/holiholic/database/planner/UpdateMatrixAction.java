package com.holiholic.database.planner;

/* UpdateMatrixAction - Updates the distance and duration matrix in the database
 *
 */
class UpdateMatrixAction extends UpdatePlannerAction {

    /* execute - Call API to get duration and distance between places for a specific city
     *
     *  @return             : success or not
     *  @cityName           : city name
     */
    @Override
    public boolean execute(String cityName) {
        return true;
    }
}
