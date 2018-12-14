package com.holiholic.planner.planner;

import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.travel.City;
import com.holiholic.planner.utils.*;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* Planner - The main purpose of this class is to calculate multiple itineraries given the city and user preferences
 *           For each request (user) we need to create a new instance of the planner
 *
 */
class Planner {
    private static final Logger LOGGER = Logger.getLogger(Planner.class.getName());
    // best plan starting from a place
    private Map<Integer, List<Place>> plans = new HashMap<>();
    // max scores for each place
    private Map<Integer, Double> maxScores = new HashMap<>();
    private City city;
    private TimeFrame timeFrame;
    private Place start;
    private Meal lunch;
    private Meal dinner;
    private Enums.TravelMode travelMode;
    private double globalMaxScore;
    // The heuristic value is used to calculate the score for the places
    // If closer to 1, means the user is interested in minimizing the distance between places
    // If closer to 0, means the user is interested in maximizing the ratings of the places
    private double heuristicValue;
    // The rewards going from place i to place j at hour h
    private Map<Integer, Map<Integer, Pair<Integer, Double>>> rewards;
    private double[][] durationDriving;
    private double[][] durationWalking;
    private double[][] distanceDriving;
    private double[][] distanceWalking;
    private ThreadManager threadManager;
    private boolean acceptNewTasks = true;
    private int solutionsCount;
    private long startTimeMeasure = 0;

    /* PlaceComparator - Comparator for sorting the places by their reward (descending)
     *
     *  @Class type
     */
    private class PlaceComparator implements Comparator<Place> {
        private Place currentPlace;
        private Calendar currentHour;

        private PlaceComparator(Place currentPlace, Calendar currentHour) {
            this.currentPlace = currentPlace;
            this.currentHour = currentHour;
        }

        // Descending order based on reward
        @Override
        public int compare(Place first, Place second) {
            double firstScore = getReward(currentPlace, first, currentHour);
            double secondScore = getReward(currentPlace, second, currentHour);
            int epsilonError = 100000000;
            return (int)((secondScore - firstScore) * epsilonError);
        }
    }

    // constructor
    Planner(City city, TimeFrame timeFrame, Enums.TravelMode travelMode) {
        this.city = city;
        this.timeFrame = timeFrame;
        this.heuristicValue = 1;
        this.travelMode = travelMode;
        this.threadManager = ThreadManager.getInstance();
        setLogger();
    }

    /* setLogger - Configure the logger
     *
     *  @return             : void
     */
    private void setLogger() {
        // add logger handler
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);

        LOGGER.addHandler(ch);
        LOGGER.setLevel(Level.ALL);
    }

    /* isTourOver - Checks if the tour is over
     *
     *  @return             : true/false
     *  @hour               : the hour when we want to check
     *  @currentPlace       : after visiting the current place, check if the tour is over
     */
    private boolean isTourOver(Calendar hour, Place currentPlace) {
        if (!timeFrame.canVisit(hour)) {
            return true;
        }

        int durationMinutes = currentPlace.durationVisit;
        Calendar afterVisiting = CloneFactory.clone(hour);
        afterVisiting.add(Calendar.MINUTE, durationMinutes);

        return !timeFrame.canVisit(afterVisiting);
    }

    /* contains - Checks if a solution contains a place
     *
     *  @return             : true/false
     *  @solution           : the solution where we want to search
     *  @place              : the place we want to find
     */
    private boolean contains(List<Place> solution, Place place) {
        for (Place p : solution) {
            if (p.id == place.id) {
                return true;
            }
        }
        return false;
    }

    /* isSolution - Checks if the current state is a solution
     *
     *  @return             : true/false
     *  @open               : a set of ids that we didn't visit yet
     *  @hour               : the current hour
     *  @current            : the current place
     *  @fixed              : priority queue which contains the fixed places
     *  @solution           : the current solution
     */
    private boolean isSolution(Set<Integer> open, Calendar hour, Place current,
                               PriorityQueue<Place> fixed, List<Place> solution) {
        if (isTourOver(hour, current)) {
            return true;
        }

        for (int id : open) {
            if (city.getPlaces().get(id).canVisit(hour)) {
                return false;
            }
        }

        if (!fixed.isEmpty()) {
            return false;
        }

        return contains(solution, current) || !current.canVisit(hour);
    }

    /* predictScore - A greedy score that will be used for pruning
     *
     *  @return             : the predicted score for the current solution
     *  @current            : the current place
     *  @open               : a set of ids of the unvisited place
     *  @hour               : the hour of visiting the current place
     *  @fixed              : a priority queue which contains the fixed places
     */
    private double predictScore(Place current, Set<Integer> open, Calendar hour, PriorityQueue<Place> fixed) {
        double maxReward = 0;
        double durationToNext;
        int count = 0;
        List<Place> places = new ArrayList<>();
        Calendar movingHour = CloneFactory.clone(hour);
        Place last = current;

        for (int i : open) {
            places.add(city.getPlaces().get(i));
        }

        Collections.addAll(places, fixed.toArray(new Place[0]));
        places.sort(new PlaceComparator(current, movingHour));

        for (Place place : places) {
            if (place.canVisit(movingHour)) {
                movingHour.add(Calendar.MINUTE, place.durationVisit);

                if (travelMode == Enums.TravelMode.DRIVING) {
                    durationToNext = Math.min(durationWalking[last.id][place.id], durationDriving[last.id][place.id]);
                } else {
                    durationToNext = (int) durationWalking[last.id][place.id];
                }

                maxReward = Math.max(maxReward, getReward(last, place, movingHour));
                movingHour.add(Calendar.SECOND, (int) durationToNext);

                count++;
                last = place;
            }
        }

        return count * maxReward;
    }

    /* getDuration - Calculates the duration to get from current place to next place
     *               It considers if it is better to just drive or (park, walk to next place and return later after car)
     *
     *  @return             : an array with information about the travel from current to next
     *  @current            : the current place
     *  @next               : the next place we want to visit
     *  @carPlaceId         : the id where the car is right now
     */
    private int[] getDuration(Place current, Place next, int carPlaceId) {
        int durationToNext;
        int returningTimeWalking = 0;
        int nextCarPlaceId = carPlaceId;
        int distanceToNext;

        // the duration to neighbor from start place
        if (current.id == -1) {
            durationToNext = getDurationFromStart(next);
            distanceToNext = getDistanceFromStart(next);
            current.parkHere = false;
            if (travelMode == Enums.TravelMode.DRIVING) {
                nextCarPlaceId = next.id;
            }
            current.travelMode = travelMode;
        } else {
            // if user selected driving, take into consideration if it's closer to walk instead of driving
            if (travelMode == Enums.TravelMode.DRIVING) {
                if (carPlaceId == current.id) {
                    current.parkHere = true;
                }

                double durationDrivingValue, durationWalkingValue, distanceDrivingValue;

                if (carPlaceId == current.id) {
                    durationDrivingValue = durationDriving[current.id][next.id];
                    distanceDrivingValue = distanceDriving[current.id][next.id];
                } else {
                    // calculate duration to get where the car is parked and continue from there
                    durationDrivingValue = durationWalking[current.id][carPlaceId] + durationDriving[carPlaceId][next.id];
                    distanceDrivingValue = distanceWalking[current.id][carPlaceId] + distanceDriving[carPlaceId][next.id];
                }

                // or just walk to the next place
                durationWalkingValue = durationWalking[current.id][next.id] + durationWalking[next.id][carPlaceId];

                if (durationDrivingValue < durationWalkingValue) {
                    // walk to next place and remind user to get the car back
                    if (carPlaceId != current.id) {
                        current.getCarBack = true;
                        current.carPlaceId = carPlaceId;
                        current.carPlaceName = city.getPlaces().get(carPlaceId).name;
                    }

                    nextCarPlaceId = next.id;
                    durationToNext = (int) durationDrivingValue;
                    distanceToNext = (int) distanceDrivingValue;
                    current.travelMode = Enums.TravelMode.DRIVING;
                } else {
                    // the actual duration is without taking into consideration the returning time for the car
                    // it will be considered later
                    durationToNext = (int) durationWalking[current.id][next.id];
                    distanceToNext = (int) distanceWalking[current.id][next.id];
                    current.travelMode = Enums.TravelMode.WALKING;
                    returningTimeWalking = (int) durationWalking[next.id][carPlaceId];
                }
            } else {
                durationToNext = (int) durationWalking[current.id][next.id];
                distanceToNext = (int) distanceWalking[current.id][next.id];
                current.travelMode = Enums.TravelMode.WALKING;
            }
        }

        return new int[]{durationToNext, returningTimeWalking, nextCarPlaceId, distanceToNext};
    }

    /* includeMeal - Add a nearby restaurant in plan
     *
     *  @return                 : 2-element boolean arary with the updated alreadyPlanned parameters
     *  @alreadyPlannedLunch:   : if already planned lunch
     *  @alreadyPlannedDinner   : if already planned dinner
     *  @currentHour            : the current hour
     *  @currentPlace           : the current place
     *  @currentSolution        : the current solution
     */
    private boolean[] includeMeal(boolean alreadyPlannedLunch,
                                  boolean alreadyPlannedDinner,
                                  Calendar currentHour,
                                  Place currentPlace,
                                  List<Place> currentSolution) {
        boolean success;
        if (isLunchIncluded()) {
            if (!alreadyPlannedLunch) {
                // try to plan the restaurant
                success = addRestaurantToItinerary(Enums.MealType.LUNCH, currentHour,
                                                   currentPlace, currentSolution);
                if (success) {
                    // set the restaurant meal type
                    currentSolution.get(currentSolution.size() - 1).mealType = Enums.MealType.LUNCH;
                    alreadyPlannedLunch = true;
                }
            }
        }
        if (isDinnerIncluded()) {
            if (!alreadyPlannedDinner) {
                // try to plan the restaurant
                success = addRestaurantToItinerary(Enums.MealType.DINNER, currentHour,
                                                   currentPlace, currentSolution);
                if (success) {
                    // set the restaurant meal type
                    currentSolution.get(currentSolution.size() - 1).mealType = Enums.MealType.DINNER;
                    alreadyPlannedDinner = true;
                }
            }
        }

        return new boolean[]{alreadyPlannedLunch, alreadyPlannedDinner};
    }

    /* visit - This is the brain function which is called recursively to construct a working solution
     *         It uses heuristics to prune solutions that are impossible to be better than the current best
     *
     *  @return                 : void
     *  @id                     : the id of the current place
     *  @open                   : a set of ids of unvisited places
     *  @currentSolution        : the current solution (like an accumulator)
     *  @hour                   : the current time
     *  @cScore                 : current score
     *  @carPlaceId             : the id of the place where is the car parked
     *  @returningToCarTime     : the time for getting back to where is the car from the current place
     *  @alreadyPlannedLunch    : if planned lunch
     *  @alreadyPlannedDinner   : if planned dinner
     *  @fixedPlaces            : a priority queue which contains the fixed places
     */
    void visit(int id,
               Set<Integer> open,
               List<Place> currentSolution,
               Calendar hour,
               double cScore,
               int carPlaceId,
               int returningToCarTime,
               boolean alreadyPlannedLunch,
               boolean alreadyPlannedDinner,
               PriorityQueue<Place> fixedPlaces) {
        // early out if the time frame expired
        if (!acceptNewTasks) {
            return;
        }

        // deep-copy used to load-balance
        Set<Integer> openCopy = CloneFactory.clone(open);
        List<Place> currentSolutionCopy = CloneFactory.clone(currentSolution);
        PriorityQueue<Place> fixedPlacesCopy = CloneFactory.clone(fixedPlaces);

        // get the current place
        Place currentPlace = CloneFactory.clone(placeMappings.get(id));
        // clone the current hour
        Calendar currentHour = CloneFactory.clone(hour);

        // we have fixed places (these places have a higher priority)
        if (!fixedPlacesCopy.isEmpty()) {
            Calendar currentPlaceHour = CloneFactory.clone(currentHour);
            Calendar peekFixedHour = Interval.getHour(fixedPlaces.peek().fixedAt);
            Place currentPlaceCopy = CloneFactory.clone(currentPlace);

            // after we visit the place we can go to the next place
            currentPlaceHour.add(Calendar.MINUTE, currentPlace.durationVisit);

            // parse the estimated duration
            int[] estimatedDuration = getDuration(currentPlaceCopy, fixedPlaces.peek(),
                                                            carPlaceId, currentPlaceHour);
            int durationToNeighbor = estimatedDuration[0];
            int currentReturningTimeWalking = estimatedDuration[1];

            // add the results
            currentPlaceHour.add(Calendar.SECOND, durationToNeighbor);
            currentPlaceHour.add(Calendar.SECOND, -returningToCarTime);
            currentPlaceHour.add(Calendar.SECOND, currentReturningTimeWalking);

            // if we can visit the next fixed place it means that the current place can be added to the solution
            // otherwise we need to add the fixed place to the solution because we can not select other node
            // !!! keep in mind that other permutations will be computed on other recursive paths
            if (!currentPlaceHour.before(peekFixedHour)) {
                Place next = CloneFactory.clone(fixedPlacesCopy.poll());
                Calendar nextHour = peekFixedHour;

                if (!currentSolutionCopy.isEmpty()) {
                    Place lastPlace = currentSolutionCopy.get(currentSolutionCopy.size() - 1);

                    Calendar lastPlaceAvailableTime = CloneFactory.clone(lastPlace.plannedHour);
                    lastPlaceAvailableTime.add(Calendar.MINUTE, lastPlace.durationVisit);

                    if (lastPlace.carPlaceId == -1) {
                        carPlaceId = lastPlace.id;
                    } else {
                        carPlaceId = lastPlace.carPlaceId;
                    }

                    int[] durationResults = getDuration(lastPlace, next, carPlaceId, lastPlaceAvailableTime);
                    int durationToNext = durationResults[0];
                    int retToCarTime = durationResults[1];
                    int nextCarId = durationResults[2];
                    int distanceToNext = durationResults[3];

                    carPlaceId = nextCarId;
                    returningToCarTime = retToCarTime;

                    lastPlace.durationToNext = durationToNext / 60;
                    lastPlace.distanceToNext = distanceToNext;

                    lastPlaceAvailableTime.add(Calendar.SECOND, durationToNext);
                    nextHour = lastPlaceAvailableTime;
                }

                visit(next.id, openCopy, currentSolutionCopy, nextHour, cScore, carPlaceId, returningToCarTime,
                      alreadyPlannedLunch, alreadyPlannedDinner, fixedPlacesCopy);
                return;
            }
        }

        // check if we need to wait some time to plan this place when the user wants
        if (!currentPlace.fixedAt.equals("anytime") && currentPlace.plannedHour == null) {
            Calendar fixedTime = Interval.getHour(currentPlace.fixedAt);
            // if true, it means the user should wait some time to visit the next place when desired
            if (fixedTime.after(currentHour) && currentPlace.canVisit(fixedTime)) {
                currentPlace.waitTime = Interval.getDiff(currentHour, fixedTime, TimeUnit.MINUTES);
                currentHour = fixedTime;
            }
        }

        // check for solution to exit recursion
        if (isSolution(openCopy, currentHour, currentPlace, fixedPlacesCopy, currentSolutionCopy)) {
            Place firstPlannedPlace = null;

            if (!currentSolutionCopy.isEmpty() && currentSolutionCopy.size() >= 2) {
                firstPlannedPlace = currentSolutionCopy.get(1);
            }
            if (firstPlannedPlace == null) {
                return;
            }

            if (cScore > globalMaxScore) {
                // to avoid concurrent modification we should synchronize this part
                synchronized (Planner.class) {
                    // update the max score
                    maxScores.put(firstPlannedPlace.id, cScore);
                    globalMaxScore = cScore;

                    List<Place> currentPlan = new ArrayList<>(currentSolution);
                    plans.put(firstPlannedPlace.id, currentPlan);
                    solutionsCount++;

                    // ... the code being measured ...
                    long estimatedTime = System.nanoTime() - startTimeMeasure;
                    double seconds = (double) estimatedTime / 1000000000.0;

                    LOGGER.log(Level.FINE, "Got a solution in {0} seconds", seconds);
                    LOGGER.log(Level.FINE, "Solution score: {0}", cScore);
                    StringBuilder sbLog = new StringBuilder();
                    sbLog.append("Print the solution:").append("\n");
                    for (Place p : currentPlan) {
                        sbLog.append(p.toString()).append("\n");
                    }
                    sbLog.append("===================================\n\n");
                    LOGGER.log(Level.FINE, sbLog.toString());
                }
            }
            return;
        }

        // Very IMPORTANT
        // Using a prediction of the score we won't consider the tour if the score will be less the maximum score so far
        double prediction = predictScore(currentPlace, openCopy, currentHour, fixedPlacesCopy);
        if (cScore + prediction <= globalMaxScore) {
            return;
        }

        if (currentPlace.canVisit(currentHour)) {
            // add the place to current solution
            openCopy.remove(id);

            currentPlace.plannedHour = CloneFactory.clone(currentHour);

            // get the duration of visiting this place
            currentHour.add(Calendar.MINUTE, currentPlace.durationVisit);

            // get and sort the neighbors based on preferences
            List<Place> neighbors = new ArrayList<>();
            for (int i : openCopy) {
                neighbors.add(placeMappings.get(i));
            }

            neighbors.sort(new PlaceComparator(currentPlace, currentHour));

            // try to add each neighbor to current solution
            for (Place neighbor : neighbors) {
                Place currentPlaceCopy = CloneFactory.clone(currentPlace);
                List<Place> currentSolutionCopyOfCopy = CloneFactory.clone(currentSolutionCopy);

                // get the reward for visiting the neighbor
                double placeScore = getReward(currentPlace, neighbor, currentHour);

                // get the duration to neighbor
                int[] durationResults = getDuration(currentPlaceCopy, neighbor, carPlaceId, currentHour);
                int durationToNext = durationResults[0];
                int currentReturningTimeWalking = durationResults[1];
                int nextCarPlaceId = durationResults[2];
                int distanceToNext = durationResults[3];

                // set the current place the time to travel to next place
                currentPlaceCopy.durationToNext = durationToNext / 60;
                currentPlaceCopy.distanceToNext = distanceToNext;
                // add to solution
                currentSolutionCopyOfCopy.add(currentPlaceCopy);

                // after visiting the place we need to check if the user wants to take lunch or dinner
                boolean[] mealResponse = includeMeal(alreadyPlannedLunch, alreadyPlannedDinner, currentHour,
                                                     currentPlaceCopy, currentSolutionCopyOfCopy);
                // update meal parameters
                alreadyPlannedLunch = mealResponse[0];
                alreadyPlannedDinner = mealResponse[1];

                Calendar currentHourCopy = CloneFactory.clone(currentHour);
                // add the time to the next place at the current hour
                currentHourCopy.add(Calendar.SECOND, durationToNext);
                currentHourCopy.add(Calendar.SECOND, -returningToCarTime);
                currentHourCopy.add(Calendar.SECOND, currentReturningTimeWalking);

                // recurse to solution
                visit(neighbor.id, openCopy, currentSolutionCopyOfCopy, currentHourCopy,
                      cScore + placeScore, nextCarPlaceId, currentReturningTimeWalking,
                      alreadyPlannedLunch, alreadyPlannedDinner, fixedPlacesCopy);
            }

            if (neighbors.isEmpty()) {
                int nextId = currentPlace.id;
                Calendar currentHourCopy = CloneFactory.clone(currentHour);
                int nextCarPlaceId = carPlaceId;
                int currentReturningTimeWalking = returningToCarTime;

                currentSolutionCopy.add(currentPlace);

                if (fixedPlacesCopy.isEmpty()) {
                    if (travelMode == Enums.TravelMode.DRIVING) {
                        if (carPlaceId == currentPlace.id) {
                            currentPlace.parkHere = true;
                        } else {
                            currentPlace.getCarBack = true;
                            currentPlace.carPlaceId = carPlaceId;
                            currentPlace.carPlaceName = placeMappings.get(carPlaceId).name;
                            currentPlace.durationToNext = (int) durationWalking[currentPlace.id][carPlaceId] / 60;
                            currentPlace.distanceToNext = (int) distanceWalking[currentPlace.id][carPlaceId];
                        }
                    }
                }
                else {
                    Place neighbor = CloneFactory.clone(fixedPlacesCopy.poll());
                    nextId = neighbor.id;

                    int[] durationResults = getDuration(currentPlace, neighbor, carPlaceId, currentHour);
                    int durationToNext = durationResults[0];
                    currentReturningTimeWalking = durationResults[1];
                    nextCarPlaceId = durationResults[2];
                    int distanceToNext = durationResults[3];

                    currentHourCopy.add(Calendar.SECOND, durationToNext);
                    currentHourCopy.add(Calendar.SECOND, -returningToCarTime);
                    currentHourCopy.add(Calendar.SECOND, currentReturningTimeWalking);
                    currentPlace.durationToNext = durationToNext / 60;
                    currentPlace.distanceToNext = distanceToNext;
                }

                // A hack to trigger the solution checking
                visit(nextId, openCopy, currentSolutionCopy, currentHourCopy, cScore, nextCarPlaceId,
                      currentReturningTimeWalking, alreadyPlannedLunch, alreadyPlannedDinner, fixedPlacesCopy);
            }
        }
    }

    /* submitPlaceToPlanner - Submit a task with each starting point
     *
     *  @return                 : void
     *  @nextPlace              : the place we want to submit
     *  @open                   : a set of ids of unvisited places
     *  @currentSolution        : the current solution (like an accumulator)
     *  @fixedPlaces            : a priority queue which contains the fixed places
     */
    private void submitPlaceToPlanner(Place nextPlace,
                                      Set<Integer> open,
                                      List<Place> currentSolution,
                                      PriorityQueue<Place> fixedPlaces) {
        try {
            int currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
            // early out if the place is closed
            if (!nextPlace.openingPeriod.isNonStop() && nextPlace.openingPeriod.isClosed(currentDayOfWeek)) {
                return;
            }

            Set<Integer> openCopy = CloneFactory.clone(open);
            List<Place> currentSolutionCopy = CloneFactory.clone(currentSolution);
            PriorityQueue<Place> fixedPlacesCopy = CloneFactory.clone(fixedPlaces);
            Calendar currentHour = null;
            Calendar userStartHour = CloneFactory.clone(timeFrame.getInterval(currentDayOfWeek).getStart());

            try {
                Place startPlaceCopy = CloneFactory.clone(start);
                // set the start place the very first hour of the visiting interval
                startPlaceCopy.plannedHour = CloneFactory.clone(userStartHour);
                // get the estimated time to reach from start place to next place
                int timeInSecondsToNextPlace = getDurationFromStart(nextPlace);
                // add duration to next place
                userStartHour.add(Calendar.SECOND, timeInSecondsToNextPlace);

                startPlaceCopy.durationToNext = timeInSecondsToNextPlace / 60;
                startPlaceCopy.distanceToNext = getDistanceFromStart(nextPlace);
                startPlaceCopy.travelMode = travelMode;
                startPlaceCopy.type = "starting_point";

                // if can plan the place at the start of the interval
                if (nextPlace.canVisit(userStartHour)) {
                    currentHour = CloneFactory.clone(userStartHour);
                } else {
                    Calendar nextPlaceStartHour = nextPlace.openingPeriod.getInterval(currentDayOfWeek).getStart();
                    if (timeFrame.isBetween(nextPlaceStartHour)) {
                        // otherwise we need to start when the place opens
                        // for example if we can start at 09:00 but the place opens at 11:00, we will start at 11:00
                        currentHour = CloneFactory.clone(nextPlaceStartHour);
                    }
                }
                if (currentHour != null) {
                    currentSolutionCopy.add(startPlaceCopy);

                    // start a new task for each possible next place
                    Runnable worker = new VisitTask(nextPlace.id, openCopy, currentSolutionCopy, currentHour, 0.0,
                                                    nextPlace.id, 0, false, false, this, fixedPlacesCopy);
                    threadManager.addTask(worker);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* awaitTermination - Wait for executor to finish tasks considering the number of places in the plan
     *
     *  @return                 : void
     *  @nrPlacesToPlan         : number of places in the plan
     */
    private void awaitTermination(int nrPlacesToPlan) {
        Thread waiter = new Thread(() -> {
            try {
                int timeToWait = 10 * (nrPlacesToPlan / 5);
                LOGGER.log( Level.FINE, "Accepting tasks for {0} seconds", timeToWait);
                // wait a time frame
                Thread.sleep(timeToWait * 1000);
                LOGGER.log( Level.FINE, "No longer accepting tasks for the city ({0})", city.name);
                // no longer accept tasks
                acceptNewTasks = false;
                LOGGER.log( Level.FINE, "Wait for finishing remaining tasks for the city ({0})", city.name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        try {
            waiter.start();
            waiter.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* initMaxScores - Initialize max score map for each place, this is the score considering the solution starts
     *                 with that place
     *
     *  @return                 : void
     *  @places                 : a list of places
     */
    private void initMaxScores(List<Place> places) {
        globalMaxScore = Double.NEGATIVE_INFINITY;

        for (Place place : places) {
            maxScores.put(place.id, Double.NEGATIVE_INFINITY);
        }
    }

    private void initMatrix() {
        durationDriving = city.getDurations(Enums.TravelMode.DRIVING);
        durationWalking = city.getDurations(Enums.TravelMode.WALKING);
        distanceDriving = city.getDistances(Enums.TravelMode.DRIVING);
        distanceWalking = city.getDistances(Enums.TravelMode.WALKING);
    }

    private void init(List<Place> places) {
        initMatrix();
        generateRewards(places);
        initMaxScores(places);
        startTimeMeasure = System.nanoTime();
    }

    /* getPlan - Generate possible itineraries
     *
     *  @return                 : possible itineraries
     *  @places                 : a list of places the user wants to visit
     */
    List<List<Place>> getPlan(List<Place> places) {
        init(places);

        List<Place> currentSolution = new ArrayList<>();
        Set<Integer> open = new HashSet<>();
        PriorityQueue<Place> fixed = new PriorityQueue<>((p1, p2) -> {
            Calendar p1Hour = Interval.getHour(p1.fixedAt);
            Calendar p2Hour = Interval.getHour(p2.fixedAt);
            return p1Hour.compareTo(p2Hour);
        });

        for (Place place : places) {
            if (!place.fixedAt.equals("anytime")) {
                fixed.add(place);
            } else {
                open.add(place.id);
            }
        }

        // if we have only fixedPlaces
        if (open.isEmpty() && !fixed.isEmpty()) {
            Place p = CloneFactory.clone(fixed.poll());
            submitPlaceToPlanner(p, open, currentSolution, fixed);
        }
        else {
            // Sort the places by rating and by the remaining time for visiting
            placesToPlan.sort((first, second) -> {
                if (second.rating != first.rating) {
                    return Double.compare(second.rating, first.rating);
                }

                if (first.isNonStop() && second.isNonStop()) {
                    return 0;
                } else if (first.isNonStop()) {
                    return 1;
                } else if (second.isNonStop()) {
                    return -1;
                }

                int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                return Interval.compareIntervals(second.openingPeriod.getInterval(day),
                                                 first.openingPeriod.getInterval(day));
            });

            for (Place p : placesToPlan) {
                if (!open.contains(p.id)) {
                    continue;
                }
                submitPlaceToPlanner(p, open, currentSolution, fixedPlaces);
            }
        }

        try {
            awaitTermination(placesToPlan.size());
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOGGER.log(Level.FINE, "Finished planning for the city ({0}). Number of solutions found: {1}",
                   new Object[]{city.name, solutionsCount});

        return sortItineraries();
    }

    /* sortItineraries - Sorts the plan that will be sent to user
     *
     *  @return                 : sorted itineraries based on their score (descending)
     */
    private List<List<Place>> sortItineraries() {
        List<List<Place>> sortedPlans = new ArrayList<>();
        List<Pair<Double, List<Place>>> plans = new ArrayList<>();

        for (Map.Entry<Integer, List<Place>> pair : this.plans.entrySet()) {
            double score = maxScores.get(pair.getKey());
            List<Place> placesList = pair.getValue();
            plans.add(new Pair<>(score, placesList));
        }

        plans.sort((p1, p2) -> p2.getKey().compareTo(p1.getKey()));

        for (Pair<Double, List<Place>> pair : plans) {
            sortedPlans.add(pair.getValue());
        }

        return sortedPlans;
    }

    /* evaluateReward - Evaluate the score for going from current place to next place at a given hour
     *
     *  @return                 : the reward for going from current place to next place at that hour
     *                            considering the distance between and the rating of the next place
     *  @current                : the current place
     *  @next                   : the next place
     *  @hour                   : the current hour
     */
    private double evaluateReward(Place current, Place next, Calendar hour) {
        double distance;
        double reward = 0;

        if (travelMode == Enums.TravelMode.DRIVING) {
            distance = Math.min(durationWalking[current.id][next.id], durationDriving[current.id][next.id]);
        } else {
            distance = durationWalking[current.id][next.id];
        }

        reward = (1 / distance) * heuristicValue + next.rating * (1 - heuristicValue);

        if (!current.fixedAt.equals("anytime")) {
            Calendar minus = CloneFactory.clone(hour);
            Calendar plus = CloneFactory.clone(hour);
            minus.add(Calendar.MINUTE, -Constants.FIXED_RANGE_ACCEPTANCE);
            plus.add(Calendar.MINUTE, Constants.FIXED_RANGE_ACCEPTANCE);
            Interval range = new Interval(minus, plus);

            if (range.isBetween(Interval.getHour(current.fixedAt))) {
                reward += Constants.FIXED_TIME_REWARD;
            }
        }

        return reward;
    }

    /* generateRewards - Generate the rewards
     *
     *  @return                 : void
     */
    private void generateRewards(List<Place> places) {
        int dayOfWeek = timeFrame.getOpenDays().iterator().next();
        Calendar movingHour = CloneFactory.clone(timeFrame.getInterval(dayOfWeek).getStart());
        rewards = new HashMap<>();

        for (int hour = 0; hour < 24; hour++) {
            movingHour.add(Calendar.HOUR_OF_DAY, hour);
            rewards.put(movingHour.get(Calendar.HOUR_OF_DAY), new HashMap<>());

            for (int i = 0; i < places.size(); i++) {
                for (int j = i + 1; j < places.size(); j++) {
                    Place current = places.get(i);
                    Place next = places.get(j);
                    double reward = evaluateReward(current, next, movingHour);
                    double inverseReward = evaluateReward(next, current, movingHour);

                    rewards.get(movingHour.get(Calendar.HOUR_OF_DAY)).put(current.id, new Pair<>(next.id, reward));
                    rewards.get(movingHour.get(Calendar.HOUR_OF_DAY)).put(next.id, new Pair<>(current.id, inverseReward));
                }
            }
        }
    }

    /* getReward - Get the cached reward between two places at a given hour
     *
     *  @return                 : the cached reward for going from current place to next place at that hour
     *                            considering also the minutes left for visiting the next place
     *  @currentPlace           : the current place used as a reference
     *  @nextPlace              : the place where the user can go
     *  @hour                   : the current hour for visiting the current place
     */
    private double getReward(Place currentPlace, Place nextPlace, Calendar hour) {
        int cid = currentPlace.id;
        int nid = nextPlace.id;
        int hourIdx = hour.get(Calendar.HOUR_OF_DAY);
        return rewards[cid][nid][hourIdx];
    }

    /* setHeuristicValue - Setter for the heuristic value
     *
     *  @return                 : void
     *  @pH                     : the value for the heuristic
     */
    void setHeuristicValue(double heuristicValue) {
        this.heuristicValue = heuristicValue;
    }

    /* setLunch - Set default lunch interval and duration to include in plan
     *
     *  @return                 : void
     */
    void setLunch() {
        setLunch(Constants.defaultLunchInterval, Constants.defaultLunchDuration);
    }

    /* setLunch - Set custom lunch interval and duration to include in plan
     *
     *  @return                 : void
     *  @interval               : the interval when to include the lunch
     *  @durationInMinutes      : how much to last the lunch
     */
    void setLunch(Interval interval, int durationInMinutes) {
        lunch = MealFactory.getInstance(Enums.MealType.LUNCH, interval, durationInMinutes);
    }

    /* setDinner - Set default dinner interval and duration to include in plan
     *
     *  @return                 : void
     */
    void setDinner() {
        setDinner(Constants.defaultDinnerInterval, Constants.defaultDinnerDuration);
    }

    /* setLunch - Set custom dinner interval and duration to include in plan
     *
     *  @return                 : void
     *  @interval               : the interval when to include the dinner
     *  @durationInMinutes      : how much to last the dinner
     */
    void setDinner(Interval interval, int durationInMinutes) {
        dinner = MealFactory.getInstance(Enums.MealType.DINNER, interval, durationInMinutes);
    }

    /* isLunchIncluded - Check if need to include lunch
     *
     *  @return                 : true / false
     */
    private boolean isLunchIncluded() {
        return lunch != null;
    }

    /* isDinnerIncluded - Check if need to include dinner
     *
     *  @return                 : true / false
     */
    private boolean isDinnerIncluded() {
        return dinner != null;
    }

    /* getMeal - Get an instance corresponding to the meal type
     *
     *  @return                 : the meal instance
     */
    private Meal getMeal(Enums.MealType mealType) {
        switch (mealType) {
            case LUNCH:
                return lunch;
            case DINNER:
                return dinner;
            default:
                    return null;
        }
    }

    /* addRestaurantToItinerary - Include a restaurant in current solution
     *
     *  @return                 : true / false (if operation is successful)
     *  @mealType               : LUNCH or DINNER
     *  @currentHour            : the current hour
     *  @currentPlace           : the current place where the user should be
     *  @currentSolution        : the solution so far
     */
    private boolean addRestaurantToItinerary(Enums.MealType mealType,
                                             Calendar currentHour,
                                             Place currentPlace,
                                             List<Place> currentSolution) {
        Meal meal = getMeal(mealType);
        if (meal == null) {
            return false;
        }
        int lastRestaurantIndex = currentSolution.size() - 1;
        Place lastPlace = currentSolution.get(lastRestaurantIndex);

        if (meal.interval.isBetween(currentHour)) {
            // get the best restaurant based on rating
            Place restaurant = city.getBestRestaurant(currentPlace.id);
            // plan restaurant hour and duration
            restaurant.plannedHour = CloneFactory.clone(currentHour);
            restaurant.durationVisit = meal.duration;
            // add the duration to the current hour
            currentHour.add(Calendar.MINUTE, meal.duration);

            if (mealType == Enums.MealType.LUNCH) {
                lastPlace.lunch = true;
            } else if (mealType == Enums.MealType.DINNER) {
                lastPlace.dinner = true;
            }

            // add the place to the current solution
            currentSolution.add(restaurant);
            return true;
        }

        return false;
    }

    /* setStart - Set the start place for this planner
     *
     *  @return                 : void
     *  @start                  : the start place instance we want to set
     */
    void setStart(Place start) {
        this.start = start;
    }

    /* getDurationFromStart - Approximate the duration to get from the start place to the next place
     *
     *  @return                 : the duration
     *  @nextPlace              : the next place to visit starting from startPlace
     */
    private int getDurationFromStart(Place nextPlace) {
        // get mathematical distance between two geo points
        double distanceInMeters = GeoPosition.distanceBetweenGeoCoordinates(start.location, nextPlace.location);

        // this coefficient is used to calculate from distance in meters to seconds
        double coefficient = Constants.drivingCoefficient;
        double medVelocity = Constants.drivingMedVelocity;

        if (travelMode == Enums.TravelMode.WALKING) {
            coefficient = Constants.walkingCoefficient;
            medVelocity = Constants.walkingMedVelocity;
        }

        // transform from km/h in m/s
        medVelocity *= (10.0 / 36.0);

        // distance = velocity * time;
        // calculated in seconds
        double timeToNext = distanceInMeters / medVelocity;

        // adjust to the correct estimation
        timeToNext *= coefficient;

        return (int) timeToNext;
    }

    /* getDistanceFromStart - Approximate the distance to get from the start place to the next place
     *
     *  @return                 : the distance
     *  @nextPlace              : the next place to visit starting from startPlace
     */
    private int getDistanceFromStart(Place nextPlace) {
        return (int) GeoPosition.distanceBetweenGeoCoordinates(start.location, nextPlace.location);
    }

    /* getAverageRating - Returns the average rating for an itinerary
     *
     *  @return         : average rating
     *  @itinerary      : the itinerary for the user
     */
    private static double getAverageRating(List<Place> itinerary) {
        if (itinerary == null || itinerary.isEmpty()) {
            return 0;
        }

        double totalRating = 0;
        for (Place place : itinerary) {
            totalRating += place.rating;
        }
        return totalRating / itinerary.size();
    }

    /* getTotalDuration - Returns the total duration for an itinerary
     *
     *  @return         : total duration
     *  @itinerary      : the itinerary for the user
     */
    private static long getTotalDuration(List<Place> itinerary) {
        if (itinerary == null || itinerary.isEmpty()) {
            return 0;
        }

        long totalTimeSpent = 0;
        long timeSpentForPlace = 0;

        for (Place place : itinerary) {
            timeSpentForPlace = 0;
            timeSpentForPlace += place.durationVisit;
            if (place.parkHere) {
                timeSpentForPlace += place.parkTime;
            }
            timeSpentForPlace += place.durationToNext;
            totalTimeSpent += timeSpentForPlace;
        }

        return totalTimeSpent;
    }

    /* getTotalDistance - Returns the total distance for an itinerary
     *
     *  @return         : total distance
     *  @itinerary      : the itinerary for the user
     */
    private static long getTotalDistance(List<Place> itinerary) {
        if (itinerary == null || itinerary.isEmpty()) {
            return 0;
        }

        long totalDistance = 0;
        for (Place place : itinerary) {
            totalDistance += place.distanceToNext;
        }
        return totalDistance;
    }

    /* getStats - Returns statistics for an itinerary
     *
     *  @return         : statistics
     *  @itinerary      : the itinerary for the user
     */
    private static JSONObject getStats(List<Place> itinerary) {
        JSONObject result = new JSONObject();
        result.put("distance", getTotalDistance(itinerary));
        result.put("duration", getTotalDuration(itinerary));
        result.put("averageRating", getAverageRating(itinerary));
        result.put("size", itinerary.size());
        return result;
    }

    /* serialize - Serialize a list of itineraries into a json format
     *
     *  @return       : the json string which will be sent to user
     *  @plan         : the final plan
     */
    public static JSONArray serialize(List<List<Place>> plan) {
        JSONArray response = new JSONArray();
        for (List<Place> itinerary : plan) {
            JSONObject itineraryInfo = new JSONObject();
            JSONArray route = new JSONArray();

            itineraryInfo.put("stats", getStats(itinerary));
            for (Place place : itinerary) {
                route.put(serialize(place));
            }
            itineraryInfo.put("route", route);
            response.put(itineraryInfo);
        }

        return response;
    }

    /* serializeToPlan - Serialize the place into a json format which is used for plan representation
     *
     *  @return       : the serialized place
     */
    public static JSONObject serialize(Place place) {
        JSONObject response = new JSONObject();
        response.put("id", place.id);
        response.put("name", place.name);
        response.put("rating", place.rating);
        response.put("duration", place.durationVisit);
        response.put("type", place.type);
        response.put("travelMode", Enums.TravelMode.serialize(place.travelMode));
        response.put("durationToNext", place.durationToNext);
        response.put("distanceToNext", place.distanceToNext);
        response.put("plannedHour", Interval.serializeHour(place.plannedHour));
        response.put("getCarBack", place.getCarBack);
        response.put("parkHere", place.parkHere);
        response.put("carPlaceId", place.carPlaceId);
        response.put("carPlaceName", place.carPlaceName);
        response.put("mealType", Enums.MealType.serialize(place.mealType));
        response.put("latitude", place.location.latitude);
        response.put("longitude", place.location.longitude);
        return response;
    }
}
