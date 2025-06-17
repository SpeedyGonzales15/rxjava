package main.java.rxjava.shedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComputationScheduler implements main.java.rxjava.Scheduler {

    private final ExecutorService executor;

    public ComputationScheduler(int threads) {
        this.executor = Executors.newFixedThreadPool(threads);
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
