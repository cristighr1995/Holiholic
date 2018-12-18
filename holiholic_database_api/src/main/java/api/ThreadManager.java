package api;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ThreadManager {
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
                new LinkedBlockingQueue<Runnable>());
        // need a rejection policy when the waiting queue is way to big!
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /* getInstance - Get the instance for the query thread manager
     *
     *  @return       : the thread manager instance
     */
    static ThreadManager getInstance() {
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

    /* addTask - Add a new task to be executed by the query thread manager
     *
     *  @return       : the query thread manager instance
     */
    void addTask(Runnable task) {
        executor.execute(task);
    }
}