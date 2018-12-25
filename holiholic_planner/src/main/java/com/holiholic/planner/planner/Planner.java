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
    private boolean lunch = false;
    private boolean dinner = false;
    private Enums.TravelMode travelMode;
    private double globalMaxScore;
    // The heuristic value is used to calculate the score for the places
    // If closer to 1, means the user is interested in minimizing the distance between places
    // If closer to 0, means the user is interested in maximizing the ratings of the places
    private double heuristicValue;
    // The rewards going from place i to place j at hour h
    private Map<Integer, Map<Integer, Map<Integer, Double>>> rewards;
    private double[][] durationDriving;
    private double[][] durationWalking;
    private double[][] distanceDriving;
    private double[][] distanceWalking;
    private boolean acceptNewTasks = true;
    private int solutionsCount = 0;
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

        Calendar afterVisiting = CloneFactory.clone(hour);
        afterVisiting.add(Calendar.SECOND, currentPlace.durationVisit);

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
                movingHour.add(Calendar.SECOND, place.durationVisit);

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

    /* scheduleFixed - Try to schedule a fixed place because they have higher priority
     *
     *  @return                 : true if can visit the current place before the first fixed place otherwise false
     *  @current                : the current place
     *  @open                   : ids of unvisited places
     *  @solution               : current solution
     *  @hourAtCurrent          : hour at the current place
     *  @score                  : current score
     *  @carPlaceId             : id of the place where is the car parked (if applicable)
     *  @returnDurationToCar    : duration to walk after the car
     *  @fixed                  : fixed places
     */
    private boolean scheduleFixed(Place current, Set<Integer> open, List<Place> solution, Calendar hourAtCurrent,
                                  double score, int carPlaceId, int returnDurationToCar,PriorityQueue<Place> fixed) {
        if (!fixed.isEmpty()) {
            Calendar peekHour = Interval.getHour(fixed.peek().fixedAt);
            int[] duration = getDuration(current, fixed.peek(), carPlaceId);
            int durationToFixed = duration[0];
            int returnDurationWalking = duration[1];
            hourAtCurrent.add(Calendar.SECOND, current.durationVisit);
            hourAtCurrent.add(Calendar.SECOND, durationToFixed);
            hourAtCurrent.add(Calendar.SECOND, -returnDurationToCar);
            hourAtCurrent.add(Calendar.SECOND, returnDurationWalking);

            // if we can visit the next fixed place it means that the current place can be added to the solution
            // otherwise we need to add the fixed place to the solution because we can not select other node
            // !!! keep in mind that other permutations will be computed on other recursive paths
            if (!hourAtCurrent.before(peekHour)) {
                Place next = fixed.poll();

                if (!solution.isEmpty()) {
                    Place lastPlace = solution.get(solution.size() - 1);
                    Calendar lastHour = CloneFactory.clone(lastPlace.plannedHour);
                    lastHour.add(Calendar.SECOND, lastPlace.durationVisit);

                    if (lastPlace.carPlaceId == -1) {
                        carPlaceId = lastPlace.id;
                    } else {
                        carPlaceId = lastPlace.carPlaceId;
                    }

                    duration = getDuration(lastPlace, next, carPlaceId);
                    int durationToNext = duration[0];
                    returnDurationToCar = duration[1];
                    carPlaceId = duration[2];
                    int distanceToNext = duration[3];

                    lastPlace.durationToNext = durationToNext;
                    lastPlace.distanceToNext = distanceToNext;
                    lastHour.add(Calendar.SECOND, durationToNext);
                    peekHour = lastHour;
                }

                assert next != null;
                visit(next.id, open, solution, peekHour, score, carPlaceId, returnDurationToCar, fixed);
                return true;
            }
        }
        return false;
    }

    /* generateItinerary - Add the current solution in possible plans
     *
     *  @return                 : void
     *  @score                  : current score
     *  @solution               : current solution
     *  @verbose                : true if need more printing otherwise false
     */
    private void generateItinerary(double score, List<Place> solution, boolean verbose) {
        if (solution.size() < 2) {
            return;
        }

        if (score > globalMaxScore) {
            synchronized (Planner.class) {
                if (score > globalMaxScore) {
                    int firstPlaceId = solution.get(1).id;
                    List<Place> itinerary = CloneFactory.clone(solution);
                    maxScores.put(firstPlaceId, score);
                    globalMaxScore = score;
                    plans.put(firstPlaceId, itinerary);
                    solutionsCount++;

                    if (verbose) {
                        long estimatedTime = System.nanoTime() - startTimeMeasure;
                        double seconds = (double) estimatedTime / 1000000000.0;
                        StringBuilder logMessage = new StringBuilder();
                        logMessage.append("New solution found in ").append(seconds).append(" seconds having ")
                                  .append(score).append(" score\n");
                        logMessage.append("Itinerary:\n");
                        for (Place place : itinerary) {
                            logMessage.append(serialize(place).toString()).append("\n");
                        }
                        LOGGER.log(Level.FINE, logMessage.toString());
                    }
                }
            }
        }
    }

    /* visitNeighbor - Try to visit the neighbor starting from the current place
     *
     *  @return                 : void
     *  @current                : the current place
     *  @neighbor               : the next place where can go from here
     *  @open                   : ids of unvisited places
     *  @solution               : current solution
     *  @score                  : current score
     *  @hour                   : hour at the current place
     *  @carPlaceId             : id of the place where is the car parked (if applicable)
     *  @returnDurationToCar    : duration to walk after the car
     *  @fixed                  : fixed places
     */
    private void visitNeighbor(Place current, Place neighbor, Set<Integer> open, List<Place> solution, double score,
                               Calendar hour, int carPlaceId, int returnDurationToCar, PriorityQueue<Place> fixed) {
        double reward = getReward(current, neighbor, hour);
        // get the duration to neighbor
        int[] duration = getDuration(current, neighbor, carPlaceId);
        int durationToNext = duration[0];
        int returnDurationWalking = duration[1];
        carPlaceId = duration[2];
        int distanceToNext = duration[3];

        current.durationToNext = durationToNext;
        current.distanceToNext = distanceToNext;
        solution.add(current);

        Calendar temporaryHour = CloneFactory.clone(hour);
        temporaryHour.add(Calendar.SECOND, durationToNext);
        temporaryHour.add(Calendar.SECOND, -returnDurationToCar);
        temporaryHour.add(Calendar.SECOND, returnDurationWalking);

        visit(neighbor.id, open, solution, temporaryHour, score + reward, carPlaceId, returnDurationWalking, fixed);
    }

    /* triggerSolution - A hack function to trigger the solution checking when could include in an itinerary all places
     *                   the user chose and still got time for others!
     *                   Here we can recommend some more places to user in the future!
     *
     *  @return                 : void
     *  @current                : the current place
     *  @open                   : ids of unvisited places
     *  @solution               : current solution
     *  @score                  : current score
     *  @hour                   : hour at the current place
     *  @carPlaceId             : id of the place where is the car parked (if applicable)
     *  @returnDurationToCar    : duration to walk after the car
     *  @fixed                  : fixed places
     */
    private void triggerSolution(Place current, Set<Integer> open, List<Place> solution, double score, Calendar hour,
                                 int carPlaceId, int returnDurationToCar, PriorityQueue<Place> fixed) {
        Calendar currentHour = CloneFactory.clone(hour);
        int nextId = current.id;
        int nextCarPlaceId = carPlaceId;
        int returnDurationWalking = returnDurationToCar;

        solution.add(current);

        if (fixed.isEmpty()) {
            if (travelMode == Enums.TravelMode.DRIVING) {
                if (carPlaceId == current.id) {
                    current.parkHere = true;
                    current.travelMode = Enums.TravelMode.UNKNOWN;
                } else {
                    current.getCarBack = true;
                    current.travelMode = Enums.TravelMode.WALKING;
                    current.carPlaceId = carPlaceId;
                    current.carPlaceName = city.getPlaces().get(carPlaceId).name;
                    current.durationToNext = (int) durationWalking[current.id][carPlaceId];
                    current.distanceToNext = (int) distanceWalking[current.id][carPlaceId];
                }
            }
        }
        else {
            Place neighbor = CloneFactory.clone(fixed.poll());
            nextId = neighbor.id;

            int[] duration = getDuration(current, neighbor, carPlaceId);
            int durationToNext = duration[0];
            returnDurationWalking = duration[1];
            nextCarPlaceId = duration[2];
            int distanceToNext = duration[3];

            currentHour.add(Calendar.SECOND, durationToNext);
            currentHour.add(Calendar.SECOND, -returnDurationToCar);
            currentHour.add(Calendar.SECOND, returnDurationWalking);
            current.durationToNext = durationToNext;
            current.distanceToNext = distanceToNext;
        }

        // A hack to trigger the solution checking
        visit(nextId, open, solution, currentHour, score, nextCarPlaceId, returnDurationWalking, fixed);
    }

    /* visit - This is the brain function which is called recursively to construct a working solution
     *         It uses heuristics to prune solutions that are impossible to be better than the current best
     *
     *  @return                 : void
     *  @id                     : id of the current place
     *  @open                   : a set of ids of unvisited places
     *  @solution               : current solution
     *  @hour                   : the current hour
     *  @cScore                 : current score
     *  @carPlaceId             : the id of the place where is the car parked
     *  @returnDurationToCar    : duration to walk after the car
     *  @fixed                  : fixed places
     */
    void visit(int id, Set<Integer> open, List<Place> solution, Calendar hour, double score, int carPlaceId,
               int returnDurationToCar, PriorityQueue<Place> fixed) {
        // early out if the allocated time frame expired
        if (!acceptNewTasks) {
            return;
        }

        // deep-copy used to load-balance
        Set<Integer> openCopy = CloneFactory.clone(open);
        List<Place> solutionCopy = CloneFactory.clone(solution);
        PriorityQueue<Place> fixedCopy = CloneFactory.clone(fixed);
        Place currentPlace = CloneFactory.clone(city.getPlaces().get(id));
        Calendar currentHour = CloneFactory.clone(hour);

        if (scheduleFixed(currentPlace, openCopy, solutionCopy, CloneFactory.clone(hour), score,
                          carPlaceId, returnDurationToCar, fixedCopy)) {
            return;
        }

        // check if we need to wait some time to plan this place when the user wants
        if (!currentPlace.fixedAt.equals("anytime") && currentPlace.plannedHour == null) {
            Calendar fixedAt = Interval.getHour(currentPlace.fixedAt);
            // if true, it means the user should wait some time to visit the next place when desired
            if (fixedAt.after(currentHour) && currentPlace.canVisit(fixedAt)) {
                currentPlace.waitTime = Interval.getDiff(currentHour, fixedAt, TimeUnit.MINUTES);
                currentHour = fixedAt;
            }
        }

        if (isSolution(openCopy, currentHour, currentPlace, fixedCopy, solutionCopy)) {
            generateItinerary(score, solutionCopy, true);
            return;
        }

        // predict the score for the current solution
        double prediction = predictScore(currentPlace, openCopy, currentHour, fixedCopy);
        if (score + prediction <= globalMaxScore) {
            return;
        }

        if (!currentPlace.canVisit(currentHour)) {
            return;
        }

        // visit the current place
        openCopy.remove(id);
        currentPlace.plannedHour = CloneFactory.clone(currentHour);
        currentHour.add(Calendar.SECOND, currentPlace.durationVisit);

        if (openCopy.isEmpty()) {
            triggerSolution(currentPlace, openCopy, solutionCopy, score, currentHour,
                            carPlaceId, returnDurationToCar, fixed);
            return;
        }

        List<Place> neighbors = new ArrayList<>();
        for (int neighborId : openCopy) {
            neighbors.add(city.getPlaces().get(neighborId));
        }
        // sort based rewards
        neighbors.sort(new PlaceComparator(currentPlace, currentHour));
        for (Place neighbor : neighbors) {
            visitNeighbor(CloneFactory.clone(currentPlace), neighbor, openCopy, CloneFactory.clone(solutionCopy),
                          score, currentHour, carPlaceId, returnDurationToCar, fixedCopy);
        }
    }

    /* createTask - Create a task for each place to be visited from the starting point
     *              Do not call this method from visit method
     *
     *  @return                 : void
     *  @next                   : the place we want to submit
     *  @open                   : a set of ids of unvisited places
     *  @currentSolution        : the current solution (like an accumulator)
     *  @fixedPlaces            : a priority queue which contains the fixed places
     */
    private PlannerTask createTask(Place next, Set<Integer> open, List<Place> solution, PriorityQueue<Place> fixed) {
        try {
            int dayOfWeek = timeFrame.getOpenDays().get(0);
            if (!next.timeFrame.isNonStop() && next.timeFrame.isClosed(dayOfWeek)) {
                return null;
            }

            List<Place> solutionCopy = CloneFactory.clone(solution);
            PriorityQueue<Place> fixedCopy = CloneFactory.clone(fixed);
            Calendar userStartHour = CloneFactory.clone(timeFrame.getInterval(dayOfWeek).getStart());
            Calendar currentHour = null;
            Place startPlaceCopy = CloneFactory.clone(start);
            int durationToNext = getDurationFromStart(next);

            startPlaceCopy.plannedHour = CloneFactory.clone(userStartHour);
            userStartHour.add(Calendar.SECOND, durationToNext);
            startPlaceCopy.durationToNext = durationToNext;
            startPlaceCopy.distanceToNext = getDistanceFromStart(next);
            startPlaceCopy.travelMode = travelMode;

            // place can be visited immediately
            if (next.canVisit(userStartHour)) {
                currentHour = userStartHour;
            } else {
                Calendar placeStartHour = next.timeFrame.getInterval(dayOfWeek).getStart();
                if (timeFrame.canVisit(placeStartHour)) {
                    // start as soon as the place opens
                    currentHour = CloneFactory.clone(placeStartHour);
                }
            }

            if (currentHour != null) {
                solutionCopy.add(startPlaceCopy);
                return new PlannerTask(next.id, CloneFactory.clone(open), solutionCopy, currentHour, 0.0,
                                       next.id, 0, fixedCopy, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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

    /* initMatrix - Get references for distance and duration matrix (from the city instance)
     *
     *  @return                 : void
     */
    private void initMatrix() {
        durationDriving = city.getDurations(Enums.TravelMode.DRIVING);
        durationWalking = city.getDurations(Enums.TravelMode.WALKING);
        distanceDriving = city.getDistances(Enums.TravelMode.DRIVING);
        distanceWalking = city.getDistances(Enums.TravelMode.WALKING);
    }

    /* initRestaurants - Check if need to include meals and get the best restaurants and fix them accordingly
     *
     *  @return                 : void
     *  @places                 : restaurants will be added to the list of places which will be used by the planner
     */
    private void initRestaurants(List<Place> places) {
        if (!lunch && !dinner) {
            return;
        }

        Place topDinner = null;
        Place topLunch;
        List<Place> topRestaurants;
        Calendar hour;

        if (dinner) {
            hour = Constants.DEFAULT_DINNER_HOUR;
            hour.set(Calendar.DAY_OF_WEEK, timeFrame.getOpenDays().get(0));
            topRestaurants = city.getTopRestaurants(5, hour);
            if (topRestaurants != null && !topRestaurants.isEmpty()) {
                topDinner = topRestaurants.get(0);
                places.add(topDinner);
            }
        }
        if (lunch) {
            hour = Constants.DEFAULT_LUNCH_HOUR;
            hour.set(Calendar.DAY_OF_WEEK, timeFrame.getOpenDays().get(0));
            topRestaurants = city.getTopRestaurants(5, hour);
            if (topRestaurants != null && !topRestaurants.isEmpty()) {
                topLunch = topRestaurants.get(0);
                if (topDinner != null && topDinner.id == topLunch.id) {
                    if (topRestaurants.size() >= 2) {
                        topLunch = city.getTopRestaurants(5, hour).get(1);
                    } else {
                        return;
                    }
                }
                places.add(topLunch);
            }
        }

    }

    /* init - Initialize the planner only when the getPlan method is called
     *
     *  @return                 : void
     *  @places                 : places to be visited
     */
    private void init(List<Place> places) {
        initMatrix();
        initRestaurants(places);
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

        Set<Integer> open = new HashSet<>();
        List<PlannerTask> plannerTasks = new ArrayList<>();
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

        // only fixed places
        if (open.isEmpty() && !fixed.isEmpty()) {
            PlannerTask task = createTask(CloneFactory.clone(fixed.poll()), open, new ArrayList<>(), fixed);
            if (task != null) {
                plannerTasks.add(task);
            }
        }
        else {
            // sort descending by rating and by the remaining time for visiting
            places.sort((p1, p2) -> {
                if (p2.rating != p1.rating) {
                    return Double.compare(p2.rating, p1.rating);
                }

                if (p1.isNonStop() && p2.isNonStop()) {
                    return 0;
                } else if (p1.isNonStop()) {
                    return 1;
                } else if (p2.isNonStop()) {
                    return -1;
                }

                int day = timeFrame.getOpenDays().get(0);
                return Interval.compareIntervals(p2.timeFrame.getInterval(day), p1.timeFrame.getInterval(day));
            });

            for (Place place : places) {
                if (!open.contains(place.id)) {
                    continue;
                }
                PlannerTask task = createTask(place, open, new ArrayList<>(), fixed);
                if (task != null) {
                    plannerTasks.add(task);
                }
            }
        }

        ThreadManager.getInstance().invokeAll(plannerTasks, 3, TimeUnit.SECONDS);

        LOGGER.log(Level.FINE, "Finished planning for the city ({0}). Number of solutions found: {1}",
                   new Object[]{city.getName(), solutionsCount});

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
        double reward;

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
                if (current.placeCategory.getTopic().equals("Restaurants")) {
                    reward += Constants.FIXED_RESTAURANT_REWARD;
                } else {
                    reward += Constants.FIXED_ATTRACTION_REWARD;
                }
            }
        }

        return reward;
    }

    /* generateRewards - Generate the rewards
     *
     *  @return                 : void
     *  @places                 : the rewards will be calculated only for this places
     */
    private void generateRewards(List<Place> places) {
        int dayOfWeek = timeFrame.getOpenDays().get(0);
        Calendar movingHour = CloneFactory.clone(timeFrame.getInterval(dayOfWeek).getStart());
        rewards = new HashMap<>();

        for (int hour = 0; hour < 24; hour++) {
            movingHour.add(Calendar.HOUR_OF_DAY, 1);
            rewards.put(movingHour.get(Calendar.HOUR_OF_DAY), new HashMap<>());

            for (int i = 0; i < places.size(); i++) {
                Place current = places.get(i);
                rewards.get(movingHour.get(Calendar.HOUR_OF_DAY)).put(current.id, new HashMap<>());

                for (int j = 0; j < places.size(); j++) {
                    if (i == j) {
                        continue;
                    }

                    Place next = places.get(j);
                    double reward = evaluateReward(current, next, movingHour);
                    rewards.get(movingHour.get(Calendar.HOUR_OF_DAY)).get(current.id).put(next.id, reward);
                }
            }
        }
    }

    /* getReward - Get the reward for going from current place to the next place at the specified hour
     *
     *  @return                 : the reward
     *  @current                : the current place
     *  @next                   : the next place
     *  @hour                   : the current hour
     */
    private double getReward(Place current, Place next, Calendar hour) {
        if (current.id == next.id) {
            return 0;
        }
        return rewards.get(hour.get(Calendar.HOUR_OF_DAY)).get(current.id).get(next.id);
    }

    /* setHeuristicValue - Set the heuristic value
     *
     *  @return                 : void
     *  @pH                     : the value for the heuristic
     */
    void setHeuristicValue(double heuristicValue) {
        this.heuristicValue = heuristicValue;
    }

    /* setLunch - Set the lunch for the current plan
     *
     *  @return                 : void
     */
    void setLunch(boolean lunch) {
        this.lunch = lunch;
    }

    /* setDinner - Set the dinner for the current plan
     *
     *  @return                 : void
     */
    void setDinner(boolean dinner) {
        this.dinner = dinner;
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
        // mathematical distance between two geo points (in meters)
        double distance = GeoPosition.distanceBetweenGeoCoordinates(start.location, nextPlace.location);
        double coefficient = Constants.DRIVING_ADJUST_COEFFICIENT;
        double velocity = Constants.ESTIMATED_DRIVING_VELOCITY;

        if (travelMode == Enums.TravelMode.WALKING) {
            coefficient = Constants.WALKING_ADJUST_COEFFICIENT;
            velocity = Constants.ESTIMATED_WALKING_VELOCITY;
        }

        // transform from kilometers / hour in meters / second
        velocity *= (10.0 / 36.0);
        // distance = velocity * duration (in seconds)
        double duration = distance / velocity;
        // adjust to the correct estimation
        duration *= coefficient;

        return (int) duration;
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
        long timeSpentForPlace;

        for (Place place : itinerary) {
            timeSpentForPlace = 0;
            timeSpentForPlace += place.durationVisit;
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
    static JSONArray serialize(List<List<Place>> plan) {
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
    private static JSONObject serialize(Place place) {
        JSONObject response = new JSONObject();
        response.put("id", place.id);
        response.put("name", place.name);
        response.put("rating", place.rating);
        response.put("duration", place.durationVisit);
        if (place.placeCategory == null) {
            response.put("category", "starting_point");
        } else {
            response.put("category", place.placeCategory.getTopic());
        }
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
