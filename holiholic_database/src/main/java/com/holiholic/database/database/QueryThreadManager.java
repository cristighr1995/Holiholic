package com.holiholic.database.database;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/* DatabaseThreadManager - Handle the threads in order to reuse them instead of creating a new executor for each query
 *                 Also, it is better to implement a Runnable instead of extending the Thread class
 *
 */
public class QueryThreadManager {
    private static QueryThreadManager instance;
    private static ThreadPoolExecutor executor;

    // private constructor !!!
    private QueryThreadManager() {
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

    /* getInstance - Get the instance for the query thread manager
     *
     *  @return       : the thread manager instance
     */
    public static QueryThreadManager getInstance() {
        if (instance == null) {
            //synchronized block to remove overhead
            synchronized (QueryThreadManager.class) {
                if(instance == null) {
                    // if instance is null, initialize
                    instance = new QueryThreadManager();
                }
            }
        }

        return instance;
    }

    /* addTask - Add a new task to be executed by the query thread manager
     *
     *  @return       : the query thread manager instance
     */
    public void addTask(Runnable task) {
        executor.execute(task);
    }
}