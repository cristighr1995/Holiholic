package com.holiholic.planner.utils;

import com.holiholic.planner.models.Place;

import java.util.*;

/* CloneFactory - Creates deep copies of objects
 *              - This is really important when dealing with load-balancing module,
 *                because we need to deep-copy the context
 *              - We can create shallow copies like this:
 *                1) List<MyObject> cloneList = new ArrayList<>(listToClone);
 *                2) List<MyObject> cloneList (ArrayList<MyObject>) listToClone.clone();
 *              - To create a deep copy using the clone method we need to create explicitly a new object instance
 *
 */
public class CloneFactory {
    public static Set<Integer> clone(Set<Integer> set) {
        return new HashSet<>(set);
    }

    public static List<Place> clone(List<Place> list) {
        List<Place> cloneList = new ArrayList<>();
        for (Place place : list) {
            cloneList.add(place.clone());
        }
        return cloneList;
    }

    public static PriorityQueue<Place> clone(PriorityQueue<Place> pq) {
        PriorityQueue<Place> pqClone = new PriorityQueue<>();
        for (Place place : pq) {
            pqClone.add(place.clone());
        }
        return pqClone;
    }

    public static Place clone(Place place) {
        return place.deepClone();
    }
}
