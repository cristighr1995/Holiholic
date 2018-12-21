package com.holiholic.places.api;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ThreadManager {
    private static ThreadManager instance;
    private static ThreadPoolExecutor executor;

    private ThreadManager() {
        int numberOfCores = Runtime.getRuntime().availableProcessors();
        executor = new ThreadPoolExecutor(numberOfCores, numberOfCores, 0L, TimeUnit.MILLISECONDS,
                                          new LinkedBlockingQueue<Runnable>());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    static ThreadManager getInstance() {
        if (instance == null) {
            synchronized (ThreadManager.class) {
                if(instance == null) {
                    instance = new ThreadManager();
                }
            }
        }
        return instance;
    }

    void invokeAll(List<Callable<Boolean>> tasks) {
        try {
            executor.invokeAll(tasks, 400 * tasks.size(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
