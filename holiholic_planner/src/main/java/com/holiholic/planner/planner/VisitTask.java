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
    private List<Place> currentSolution;
    private Calendar hour;
    private double cScore;
    private int carPlaceId;
    private int returningToCarTime;
    private boolean alreadyPlannedLunch;
    private boolean alreadyPlannedDinner;
    private Planner planner;
    private PriorityQueue<Place> fixedPlaces;

    public VisitTask(int id,
                     Set<Integer> open,
                     List<Place> currentSolution,
                     Calendar hour,
                     double cScore,
                     int carPlaceId,
                     int returningToCarTime,
                     boolean alreadyPlannedLunch,
                     boolean alreadyPlannedDinner,
                     Planner planner,
                     PriorityQueue<Place> fixedPlaces) {
        this.id = id;
        this.open = open;
        this.currentSolution = currentSolution;
        this.hour = hour;
        this.cScore = cScore;
        this.carPlaceId = carPlaceId;
        this.returningToCarTime = returningToCarTime;
        this.alreadyPlannedLunch = alreadyPlannedLunch;
        this.alreadyPlannedDinner = alreadyPlannedDinner;
        this.planner = planner;
        this.fixedPlaces = fixedPlaces;
    }

    @Override
    public void run() {
        try {
            planner.visit(id, open, currentSolution, hour, cScore, carPlaceId, returningToCarTime,
                          alreadyPlannedLunch, alreadyPlannedDinner, fixedPlaces);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
