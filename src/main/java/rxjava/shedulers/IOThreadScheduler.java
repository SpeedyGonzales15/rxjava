package main.java.rxjava.shedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IOThreadScheduler implements main.java.rxjava.Scheduler {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void execute(Runnable task) {
        executor.execute(task);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
