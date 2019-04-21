package com.holiholic.planner.planner;

import com.holiholic.planner.models.Place;

import java.time.LocalDateTime;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.Callable;

/* PlannerTask - This is used for the load balancing module which starts looking for a solution from a different thread
 *
 */
public class PlannerTask implements Callable<Boolean> {
    private Place current;
    private Set<Place> open;
    private List<Place> solution;
    private LocalDateTime time;
    private double score;
    private int carPlaceId;
    private int returnDurationToCar;
    private PriorityQueue<Place> fixed;
    private Planner planner;

    PlannerTask(Place current, Set<Place> open, List<Place> solution, LocalDateTime time, double score, int carPlaceId,
                int returnDurationToCar, PriorityQueue<Place> fixed, Planner planner) {
        this.current = current;
        this.open = open;
        this.solution = solution;
        this.time = time;
        this.score = score;
        this.carPlaceId = carPlaceId;
        this.returnDurationToCar = returnDurationToCar;
        this.fixed = fixed;
        this.planner = planner;
    }

    @Override
    public Boolean call() {
        try {
            planner.visit(current, open, solution, time, score, carPlaceId, returnDurationToCar, fixed);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
