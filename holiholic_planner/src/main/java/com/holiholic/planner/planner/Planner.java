package com.holiholic.planner.planner;

import com.holiholic.planner.constant.Constants;
import com.holiholic.planner.database.DatabaseManager;
import com.holiholic.planner.models.Place;
import com.holiholic.planner.travel.City;
import com.holiholic.planner.utils.*;
import javafx.util.Pair;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/* Planner - The main purpose of this class is to calculate multiple itineraries given the city and user preferences
 *           For each request (user) we need to create a new instance of the planner
 *
 */
public class Planner {
    private static final Logger LOGGER = Logger.getLogger(Planner.class.getName());

    // Key = Place id, Value = The place reference
    private Map<Integer, Place> placeMappings;

    // for each place we store the best plan starting with that place
    private Map<Integer, List<Place>> planMappings = new HashMap<>();
    // for each place we store the current max score for the plan
    private Map<Integer, Double> maxScoreMappings = new HashMap<>();

    private City city;
    // The time the user wants to spend in the current city
    private OpeningPeriod openingPeriod;
    private Place startPlace;

    private Map<Integer, Integer> placesToPlanMappings;

    // Eating preferences
    private Meal lunch;
    private Meal dinner;

    // The way the user wants to travel between places
    // It can be driving | walking | bicycling | transit (we can not use transit which is public transportation because
    // it is paid)
    private Enums.TravelMode modeOfTravel;

    private double maxScore;
    // Preference heuristic
    // It is used in calculating the score for the places
    // If closer to 1, means the user is interested in minimizing the distance between places
    // If closer to 0, means the user is interested in maximizing the ratings of the places
    private double pH;

    // The rewards going from place i to place j at hour h
    private double[][][] rewards;

    private double[][] durationDriving;
    private double[][] durationWalking;
    private double[][] distanceDriving;
    private double[][] distanceWalking;

    // The map used for calculating the traffic coefficients
    private Map<Integer, Double> trafficCoefficients;

    // Information about weather, it will be calculated real-time using the city of the user
    private WeatherForecastInformation weatherForecastInformation;
    // This rate is used to adjust the duration between places
    // For example if it's raining, it will take longer to move between places
    private double weatherRate = 1.0;

    private ThreadManager threadManager;
    private boolean acceptNewTasks = true;
    private int numberOfSolutions;
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

        @Override
        public int compare(Place first, Place second) {
            double firstScore = getReward(currentPlace, first, currentHour);
            double secondScore = getReward(currentPlace, second, currentHour);
            // Need to be in descending order base on the score
            int epsilonError = 100000000;
            return (int)((secondScore - firstScore) * epsilonError);
        }
    }

    // constructor
    Planner(City city, OpeningPeriod openingPeriod, Enums.TravelMode modeOfTravel) {
        this.city = city; // we need to clone the city!!!
        this.openingPeriod = openingPeriod;
        this.pH = 1;
        this.modeOfTravel = modeOfTravel;
        // map ids to places
        buildPlaceMappings();
        // used for logging
        setLogger();
        // get the instance of thread manager to reuse existing threads
        threadManager = ThreadManager.getInstance();
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

    /* buildPlaceMappings - Map the place id to a place instance
     *
     *  @return             : void
     */
    private void buildPlaceMappings() {
        placeMappings = new HashMap<>();
        for (Place p : city.places) {
            placeMappings.put(p.id, p);
        }
    }

    /* getPlaceMappings - Getter for placeMappings
     *
     *  @return             : the place mappings
     */
    Map<Integer, Place> getPlaceMappings() {
        return placeMappings;
    }

    /* isTourOver - Checks if the tour is over
     *
     *  @return             : true/false
     *  @hour               : the hour when we want to check
     *  @currentPlace       : after visiting the current place, check if the tour is over
     */
    private boolean isTourOver(Calendar hour, Place currentPlace) {
        if (!openingPeriod.isBetween(hour)) {
            return true;
        }

        Calendar afterVisiting = CloneFactory.clone(hour);
        int durationMinutes = currentPlace.durationVisit;
        afterVisiting.add(Calendar.MINUTE, durationMinutes);

        return !openingPeriod.isBetween(afterVisiting);
    }

    /* solutionContainsPlace - Checks if a solution contains a place
     *
     *  @return             : true/false
     *  @solution           : the solution where we want to search
     *  @place              : the place we want to find
     */
    private boolean solutionContainsPlace(List<Place> solution, Place place) {
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
     *  @currentPlace       : the current place where we are
     *  @fixedPlaces        : a priority queue which contains the fixed places
     *  @currentSolution    : the current solution
     */
    private boolean isSolution(Set<Integer> open,
                               Calendar hour,
                               Place currentPlace,
                               PriorityQueue<Place> fixedPlaces,
                               List<Place> currentSolution) {
        if (isTourOver(hour, currentPlace)) {
            return true;
        }

        for (Integer id : open) {
            Place place = placeMappings.get(id);
            if (place.canVisit(hour)) {
                return false;
            }
        }

        if (!fixedPlaces.isEmpty()) {
            return false;
        }

        return solutionContainsPlace(currentSolution, currentPlace) || !currentPlace.canVisit(hour);
    }

    /* predictScore - A greedy score that will be used for pruning
     *
     *  @return             : the predicted score for the current solution
     *  @currentPlace       : the current place used as a reference
     *  @open               : a set of ids of the unvisited place
     *  @hour               : the hour of visiting the current place
     *  @fixedPlaces        : a priority queue which contains the fixed places
     */
    private double predictScore(Place currentPlace,
                                Set<Integer> open,
                                Calendar hour,
                                PriorityQueue<Place> fixedPlaces) {
        double prediction;
        double maxReward = 0;
        double trafficCoefficient;
        int count = 0;
        int timeToNextPlace;
        int durationInMinutes;

        List<Place> placesToVisit = new ArrayList<>();
        Calendar copyHour = CloneFactory.clone(hour);
        Place last = currentPlace;

        for (int i : open) {
            placesToVisit.add(placeMappings.get(i));
        }

        Collections.addAll(placesToVisit, fixedPlaces.toArray(new Place[0]));
        placesToVisit.sort(new PlaceComparator(currentPlace, copyHour));

        for (Place p : placesToVisit) {
            if (p.canVisit(copyHour)) {
                durationInMinutes = p.durationVisit;
                copyHour.add(Calendar.MINUTE, durationInMinutes);

                // consider the traffic congestion at the current hour
                trafficCoefficient = trafficCoefficients.get(copyHour.get(Calendar.HOUR_OF_DAY));

                if (modeOfTravel == Enums.TravelMode.DRIVING) {
                    timeToNextPlace = (int) Math.min(durationWalking[last.id][p.id],
                                                     durationDriving[last.id][p.id] * trafficCoefficient
                                                     + p.parkTime);
                } else {
                    timeToNextPlace = (int) durationWalking[last.id][p.id];
                }

                timeToNextPlace *= weatherRate;

                // get the reward for the current place
                maxReward = Math.max(maxReward, getReward(last, p, copyHour));
                // add the time for arriving to next place
                copyHour.add(Calendar.SECOND, timeToNextPlace);

                count++;
                last = p;
            }
        }

        // approximate the score considering the maxReward for all the places we can visit
        prediction = count * maxReward;
        return prediction;
    }

    /* getDurationToNeighbor - Calculates the time to get from current place to the neighbor
     *                         It considers if it is better to just drive or walk to next place and return after the car
     *
     *  @return             : a 3-element array (durationToNeighbor, returnTimeWalking, nextCarPlaceId)
     *  @currentPlace       : the current place used as a reference
     *  @neighbor           : the next place that we want to visit
     *  @carPlaceId         : the id where the car is right now
     *  @currentHour        : the current time
     */
    private int[] getDurationToNeighbor(Place currentPlace,
                                        Place neighbor,
                                        int carPlaceId,
                                        Calendar currentHour) {
        int durationToNeighbor;
        int returningTimeWalking = 0;
        int nextCarPlaceId = carPlaceId;
        int distanceToNeighbor;

        // the duration to neighbor from start place is approximate
        if (currentPlace.id == -1) {
            durationToNeighbor = getDurationFromStart(neighbor);
            distanceToNeighbor = getDistanceFromStart(neighbor);
            currentPlace.parkHere = false;
            if (modeOfTravel == Enums.TravelMode.DRIVING) {
                nextCarPlaceId = neighbor.id;
            }
            currentPlace.modeOfTravel = modeOfTravel;
        } else {
            // If the user selected driving, we need to take in consideration if it's closer to walk instead
            // of drive and find parking slot
            if (modeOfTravel == Enums.TravelMode.DRIVING) {
                if (carPlaceId == currentPlace.id) {
                    currentPlace.parkHere = true;
                }

                double durationDrivingValue, durationWalkingValue;
                double distanceDrivingValue;
                // we also need to take into account the traffic for driving duration
                // the traffic will raise the normal duration with a coefficient
                double trafficCoefficient = trafficCoefficients.get(currentHour.get(Calendar.HOUR_OF_DAY));

                // If we have the car right to the current place
                if (carPlaceId == currentPlace.id) {
                    durationDrivingValue = durationDriving[currentPlace.id][neighbor.id] * trafficCoefficient
                                        + neighbor.parkTime;
                    distanceDrivingValue = distanceDriving[currentPlace.id][neighbor.id];
                } else {
                    // Or we need to get back where we have the car and continue from there
                    durationDrivingValue = durationWalking[currentPlace.id][carPlaceId]
                                           + durationDriving[carPlaceId][neighbor.id] * trafficCoefficient
                                           + neighbor.parkTime;
                    distanceDrivingValue = distanceWalking[currentPlace.id][carPlaceId]
                                           + distanceDriving[carPlaceId][neighbor.id];
                }

                // Or just walk to the next place
                durationWalkingValue = durationWalking[currentPlace.id][neighbor.id]
                                       + durationWalking[neighbor.id][carPlaceId];

                if (durationDrivingValue < durationWalkingValue) {
                    // it means that we were walking to next place
                    // and need to remind the user to get back for the car
                    if (carPlaceId != currentPlace.id) {
                        currentPlace.getCarBack = true;
                        currentPlace.carPlaceId = carPlaceId;
                        currentPlace.carPlaceName = placeMappings.get(carPlaceId).name;
                    }

                    nextCarPlaceId = neighbor.id;
                    durationToNeighbor = (int) durationDrivingValue;
                    distanceToNeighbor = (int) distanceDrivingValue;
                    currentPlace.modeOfTravel = Enums.TravelMode.DRIVING;
                } else {
                    // The actual duration is without taking into consideration the returning time for the car
                    // We will count that time later
                    durationToNeighbor = (int) durationWalking[currentPlace.id][neighbor.id];
                    distanceToNeighbor = (int) distanceWalking[currentPlace.id][neighbor.id];
                    currentPlace.modeOfTravel = Enums.TravelMode.WALKING;
                    returningTimeWalking = (int) durationWalking[neighbor.id][carPlaceId];
                }
            } else {
                durationToNeighbor = (int) durationWalking[currentPlace.id][neighbor.id];
                distanceToNeighbor = (int) distanceWalking[currentPlace.id][neighbor.id];
                currentPlace.modeOfTravel = Enums.TravelMode.WALKING;
            }
        }

        durationToNeighbor *= weatherRate;

        return new int[]{durationToNeighbor, returningTimeWalking, nextCarPlaceId, distanceToNeighbor};
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
            Calendar peekFixedHour = Interval.getHour(fixedPlaces.peek().fixedTime);
            Place currentPlaceCopy = CloneFactory.clone(currentPlace);

            // after we visit the place we can go to the next place
            currentPlaceHour.add(Calendar.MINUTE, currentPlace.durationVisit);

            // parse the estimated duration
            int[] estimatedDuration = getDurationToNeighbor(currentPlaceCopy, fixedPlaces.peek(),
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

                    int[] durationResults = getDurationToNeighbor(lastPlace, next, carPlaceId, lastPlaceAvailableTime);
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
        if (!currentPlace.fixedTime.equals("anytime") && currentPlace.plannedHour == null) {
            Calendar fixedTime = Interval.getHour(currentPlace.fixedTime);
            // if true, it means the user should wait some time to visit the next place when desired
            if (fixedTime.after(currentHour) && currentPlace.canVisit(fixedTime)) {
                currentPlace.needToWait = Interval.getDiff(currentHour, fixedTime, TimeUnit.MINUTES);
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

            if (cScore > maxScore) {
                // to avoid concurrent modification we should synchronize this part
                synchronized (Planner.class) {
                    // update the max score
                    maxScoreMappings.put(firstPlannedPlace.id, cScore);
                    maxScore = cScore;

                    List<Place> currentPlan = new ArrayList<>(currentSolution);

                    // trigger to finish at last place
                    currentPlan.get(currentPlan.size() - 1).durationToNext = null;
                    planMappings.put(firstPlannedPlace.id, currentPlan);
                    numberOfSolutions++;

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
        if (cScore + prediction <= maxScore) {
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
                int[] durationResults = getDurationToNeighbor(currentPlaceCopy, neighbor, carPlaceId, currentHour);
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
                    if (modeOfTravel == Enums.TravelMode.DRIVING) {
                        if (carPlaceId == currentPlace.id) {
                            currentPlace.parkHere = true;
                        } else {
                            currentPlace.getCarBack = true;
                            currentPlace.carPlaceId = carPlaceId;
                            currentPlace.carPlaceName = placeMappings.get(carPlaceId).name;
                        }
                    }
                }
                else {
                    Place neighbor = CloneFactory.clone(fixedPlacesCopy.poll());
                    nextId = neighbor.id;

                    int[] durationResults = getDurationToNeighbor(currentPlace, neighbor, carPlaceId, currentHour);
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
            Calendar userStartHour = CloneFactory.clone(openingPeriod.getInterval(currentDayOfWeek).getStart());

            try {
                Place startPlaceCopy = CloneFactory.clone(startPlace);
                // set the start place the very first hour of the visiting interval
                startPlaceCopy.plannedHour = CloneFactory.clone(userStartHour);
                // get the estimated time to reach from start place to next place
                int timeInSecondsToNextPlace = getDurationFromStart(nextPlace);
                // add duration to next place
                userStartHour.add(Calendar.SECOND, timeInSecondsToNextPlace);

                startPlaceCopy.durationToNext = timeInSecondsToNextPlace / 60;
                startPlaceCopy.distanceToNext = getDistanceFromStart(nextPlace);
                startPlaceCopy.modeOfTravel = modeOfTravel;
                startPlaceCopy.type = "starting_point";

                // if can plan the place at the start of the interval
                if (nextPlace.canVisit(userStartHour)) {
                    currentHour = CloneFactory.clone(userStartHour);
                } else {
                    Calendar nextPlaceStartHour = nextPlace.openingPeriod.getInterval(currentDayOfWeek).getStart();
                    if (openingPeriod.isBetween(nextPlaceStartHour)) {
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
        for (Place p : places) {
            maxScoreMappings.put(p.id, Double.NEGATIVE_INFINITY);
        }
    }

    /* getPlan - Calculates the itineraries for the places that the user wants to visit
     *
     *  @return                 : multiple possible itineraries
     *  @placesToPlan           : a list of places the user wants to visit
     */
    List<List<Place>> getPlan(List<Place> placesToPlan) {
        // Store the positions for the placesToPlan
        placesToPlanMappings = new HashMap<>();
        for (int i = 0; i < placesToPlan.size(); i++) {
            placesToPlanMappings.put(placesToPlan.get(i).id, i);
            placeMappings.put(placesToPlan.get(i).id, placesToPlan.get(i));
        }

        // Cache weather information
        weatherForecastInformation = DatabaseManager.getWeatherForecastInformation(city.name);
        weatherRate = getWeatherRate();
        // Cache traffic coefficients
        cacheTrafficCoefficients();
        // Cache distances because we will use them intensely when getting the rewards !!!
        cacheDistanceMatrix();
        // Cache rewards
        cacheRewards();
        // initialize the max score for each place
        initMaxScores(placesToPlan);
        // set or reset the maxScore to -Inf
        maxScore = Double.NEGATIVE_INFINITY;
        // start measuring time
        startTimeMeasure = System.nanoTime();

        List<Place> currentSolution = new ArrayList<>();
        Set<Integer> open = new HashSet<>();

        // get fixed places
        PriorityQueue<Place> fixedPlaces = new PriorityQueue<>((p1, p2) -> {
            Calendar p1Hour = Interval.getHour(p1.fixedTime);
            Calendar p2Hour = Interval.getHour(p2.fixedTime);
            return p1Hour.compareTo(p2Hour);
        });

        for (Place p : placesToPlan) {
            if (!p.fixedTime.equals("anytime")) {
                fixedPlaces.add(p);
            } else {
                open.add(p.id);
            }
        }

        // if we have only fixedPlaces
        if (open.isEmpty() && !fixedPlaces.isEmpty()) {
            Place p = CloneFactory.clone(fixedPlaces.poll());
            submitPlaceToPlanner(p, open, currentSolution, fixedPlaces);
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
                   new Object[]{city.name, numberOfSolutions});

        return sortItineraries();
    }

    /* sortItineraries - Sorts the plan that will be sent to user
     *
     *  @return                 : sorted itineraries based on their score (descending)
     */
    private List<List<Place>> sortItineraries() {
        List<List<Place>> sortedPlans = new ArrayList<>();
        List<Pair<Double, List<Place>>> plans = new ArrayList<>();

        for (Map.Entry<Integer, List<Place>> pair : planMappings.entrySet()) {
            double score = maxScoreMappings.get(pair.getKey());
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
     *  @currentPlace           : the current place used as a reference
     *  @nextPlace              : the place where the user can go
     *  @hour                   : the current hour for visiting the current place
     */
    private double evaluateReward(Place currentPlace, Place nextPlace, Calendar hour) {
        Integer position = placesToPlanMappings.get(nextPlace.id);
        // early out if a place is not supposed to be planned
        if (position == null) {
            return 0;
        }

        double distance;
        double trafficCoefficient = trafficCoefficients.get(hour.get(Calendar.HOUR_OF_DAY));
        int placesToPlanSize = placesToPlanMappings.size();
        int placePosition = position;

        if (modeOfTravel == Enums.TravelMode.DRIVING) {
            distance = Math.min(durationWalking[currentPlace.id][nextPlace.id],
                                // traffic and parking time contribution to distance
                                durationDriving[currentPlace.id][nextPlace.id] * trafficCoefficient
                                + nextPlace.parkTime);
        } else {
            distance = durationWalking[currentPlace.id][nextPlace.id];
        }

        // weather contribution to distance
        distance *= weatherRate;

        // the number of components <1 that contribute to the popularity
        double numberOfComponents = 3;
        double checkIns = ((double) nextPlace.checkIns) / ((double) city.totalCheckIns);
        double wanToGoNumber = ((double) nextPlace.wantToGoNumber) / ((double) city.totalWantToGo);

        // we also need to scale correctly the popularity between [0, 1]
        double popularity = (nextPlace.rating / Constants.maxRating + checkIns + wanToGoNumber) / numberOfComponents;
        double positionPreference = ((double) (placesToPlanSize - placePosition)) / ((double) placesToPlanSize);

        // the reward for the place
        double reward = (1 / distance) * pH // distance contribution
                        + popularity * (1 - pH) // popularity contribution
                        + Constants.placePositionRate * positionPreference; // place position contribution

        if (!currentPlace.fixedTime.equals("anytime")) {
            Calendar fixedTime = Interval.getHour(currentPlace.fixedTime);
            Calendar minus = CloneFactory.clone(hour);
            Calendar plus = CloneFactory.clone(hour);
            minus.add(Calendar.MINUTE, -Constants.fixedTimeIntervalRange);
            plus.add(Calendar.MINUTE, Constants.fixedTimeIntervalRange);
            Interval i = new Interval(minus, plus);

            if (i.isBetween(fixedTime)) {
                reward += Constants.scoreFixedTime;
            }
        }

        return reward;
    }

    /* cacheRewards - Cache the rewards for fast retrieval
     *
     *  @return                 : void
     */
    private void cacheRewards() {
        int numberOfPlaces = city.places.size();
        rewards = new double[numberOfPlaces][numberOfPlaces][24];
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        Calendar startHour = CloneFactory.clone(openingPeriod.getInterval(dayOfWeek).getStart());

        for (int h = 0; h < 24; h++) {
            startHour.add(Calendar.HOUR_OF_DAY, h);
            for (int i = 0; i < numberOfPlaces; i++) {
                Place currentPlace = placeMappings.get(i);
                for (int j = i + 1; j < numberOfPlaces; j++) {
                    Place nextPlace = placeMappings.get(j);
                    rewards[i][j][h] = evaluateReward(currentPlace, nextPlace, startHour);
                    rewards[j][i][h] = evaluateReward(nextPlace, currentPlace, startHour);
                }
            }
        }
    }

    /* cacheDistanceMatrix - Cache the distance matrix
     *
     *  @return                 : void
     */
    private void cacheDistanceMatrix() {
        durationDriving = DatabaseManager.getDurationMatrix(city.name, Enums.TravelMode.DRIVING);
        durationWalking = DatabaseManager.getDurationMatrix(city.name, Enums.TravelMode.WALKING);

        distanceDriving = DatabaseManager.getDistanceMatrix(city.name, Enums.TravelMode.DRIVING);
        distanceWalking = DatabaseManager.getDistanceMatrix(city.name, Enums.TravelMode.WALKING);
    }

    /* cacheTrafficCoefficients - Cache the traffic coefficients
     *
     *  @return                 : void
     */
    private void cacheTrafficCoefficients() {
        trafficCoefficients = DatabaseManager.getTrafficCoefficients(city.name);
    }

    /* getWeatherRate - Calculates the weather rate which is used to adjust the duration between two places
     *
     *  @return                 : the weather rate
     */
    private double getWeatherRate() {
        double rate = 1.0;
        // using the third simple rule we calculate how to raise the rate
        rate += weatherForecastInformation.rainProbability * Constants.maxWeatherRainThreshold / 100.0;
        // also for snow
        rate += weatherForecastInformation.snowProbability * Constants.maxWeatherSnowThreshold / 100.0;
        return rate;
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

    /* setPreferenceHeuristic - Setter for the preference heuristic
     *
     *  @return                 : void
     *  @pH                     : the value for the preference heuristic
     */
    void setPreferenceHeuristic(double pH) {
        this.pH = pH;
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
                lastPlace.eatLunch = true;
            } else if (mealType == Enums.MealType.DINNER) {
                lastPlace.eatDinner = true;
            }

            // add the place to the current solution
            currentSolution.add(restaurant);
            return true;
        }

        return false;
    }

    /* setStartPlace - Set the start place for this planner
     *
     *  @return                 : void
     *  @startPlace             : the start place instance we want to set
     */
    void setStartPlace(Place startPlace) {
        this.startPlace = startPlace;
    }

    /* getDurationFromStart - Approximate the duration to get from the start place to the next place
     *
     *  @return                 : the duration
     *  @nextPlace              : the next place to visit starting from startPlace
     */
    private int getDurationFromStart(Place nextPlace) {
        // get mathematical distance between two geo points
        double distanceInMeters = GeoPosition.distanceBetweenGeoCoordinates(startPlace.location, nextPlace.location);

        // this coefficient is used to calculate from distance in meters to seconds
        double coefficient = Constants.drivingCoefficient;
        double medVelocity = Constants.drivingMedVelocity;

        if (modeOfTravel == Enums.TravelMode.WALKING) {
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
        return (int) GeoPosition.distanceBetweenGeoCoordinates(startPlace.location, nextPlace.location);
    }
}
