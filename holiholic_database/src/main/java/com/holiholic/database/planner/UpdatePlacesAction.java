package com.holiholic.database.planner;

/* UpdatePlacesAction - Updates the places in the database
 *
 */
class UpdatePlacesAction extends UpdatePlannerAction {

    /* execute - Call API to retrieve places online and save them in the database
     *
     *  @return             : success or not
     *  @body               : the request body
     */
    @Override
    public boolean execute(String cityName) {
        return true;
    }
}
