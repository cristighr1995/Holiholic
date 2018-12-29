package com.holiholic.planner.utils;

import java.util.List;
import java.util.concurrent.Callable;
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

    private ThreadManager() {
        int numberOfCores = Runtime.getRuntime().availableProcessors();
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
                    instance = new ThreadManager();
                }
            }
        }

        return instance;
    }

    /* invokeAll - Execute a list of tasks and wait for their execution
     *
     *  @return         : void
     *  @tasks          : tasks to execute
     *  @limit          : duration limit
     *  @timeUnit       : time unit for duration limit
     */
    public void invokeAll(List<? extends Callable<Boolean>> tasks, int limit, TimeUnit timeUnit) {
        try {
            executor.invokeAll(tasks, limit, timeUnit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
