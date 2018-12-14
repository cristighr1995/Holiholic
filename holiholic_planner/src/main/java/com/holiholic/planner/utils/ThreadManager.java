package com.holiholic.planner.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/* ThreadManager - Handle the threads in order to reuse them instead of creating a new executor for each planner
 *                 Also, it is better to implement a Runnable instead of extending the Thread class
 *
 */
public class ThreadManager {
    private static ThreadManager instance;
    private static ThreadPoolExecutor executor;

    // private constructor !!!
    private ThreadManager() {
        // get the number of cores
        int numberOfCores = Runtime.getRuntime().availableProcessors();
        // instantiate the thread pool
        executor = new ThreadPoolExecutor(
                numberOfCores,
                numberOfCores,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        // need a rejection policy when the waiting queue is way to big!
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /* getInstance - Get the instance for the thread manager
     *
     *  @return       : the thread manager instance
     */
    public static ThreadManager getInstance() {
        if (instance == null) {
            //synchronized block to remove overhead
            synchronized (ThreadManager.class) {
                if(instance == null) {
                    // if instance is null, initialize
                    instance = new ThreadManager();
                }
            }
        }

        return instance;
    }

    /* addTask - Add a new task to be executed by the thread manager
     *
     *  @return       : the thread manager instance
     */
    public void addTask(Runnable task) {
        executor.execute(task);
    }
}
