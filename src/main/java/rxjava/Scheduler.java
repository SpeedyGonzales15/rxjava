package main.java.rxjava;

public interface Scheduler {
    void execute(Runnable task);
    void shutdown();
}
