package main.java.rxjava.shedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SingleThreadScheduler implements main.java.rxjava.Scheduler {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
