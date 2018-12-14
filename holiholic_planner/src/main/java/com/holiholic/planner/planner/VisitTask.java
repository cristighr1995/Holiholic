package com.holiholic.planner.planner;

import com.holiholic.planner.models.Place;

import java.util.Calendar;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/* VisitTask - This is used for the load balancing module which starts looking for a solution from a different thread
 *
 */
public class VisitTask implements Runnable {
    private int id;
    private Set<Integer> open;
    private List<Place> solution;
    private Calendar hour;
    private double score;
    private int carPlaceId;
    private int returnDurationToCar;
    private PriorityQueue<Place> fixed;
    private Planner planner;

    VisitTask(int id, Set<Integer> open, List<Place> solution, Calendar hour, double score, int carPlaceId,
              int returnDurationToCar, PriorityQueue<Place> fixed, Planner planner) {
        this.id = id;
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
    public void run() {
        try {
            planner.visit(id, open, solution, hour, score, carPlaceId, returnDurationToCar, fixed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
