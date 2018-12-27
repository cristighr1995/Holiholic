package com.holiholic.planner.planner;

import com.holiholic.planner.models.Place;

import java.util.Calendar;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.Callable;

/* PlannerTask - This is used for the load balancing module which starts looking for a solution from a different thread
 *
 */
public class PlannerTask implements Callable<Boolean> {
    private Place current;
    private Set<Integer> open;
    private List<Place> solution;
    private Calendar hour;
    private double score;
    private int carPlaceId;
    private int returnDurationToCar;
    private PriorityQueue<Place> fixed;
    private Planner planner;

    PlannerTask(Place current, Set<Integer> open, List<Place> solution, Calendar hour, double score, int carPlaceId,
                int returnDurationToCar, PriorityQueue<Place> fixed, Planner planner) {
        this.current = current;
        this.open = open;
        this.solution = solution;
        this.hour = hour;
        this.score = score;
        this.carPlaceId = carPlaceId;
        this.returnDurationToCar = returnDurationToCar;
        this.fixed = fixed;
        this.planner = planner;
    }

    @Override
    public Boolean call() {
        try {
            planner.visit(current, open, solution, hour, score, carPlaceId, returnDurationToCar, fixed);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
