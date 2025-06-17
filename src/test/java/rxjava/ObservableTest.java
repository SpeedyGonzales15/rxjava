package test.java.rxjava;

import main.java.rxjava.Disposable;
import main.java.rxjava.Observable;
import main.java.rxjava.Observer;
import main.java.rxjava.Scheduler;
import main.java.rxjava.shedulers.ComputationScheduler;
import main.java.rxjava.shedulers.IOThreadScheduler;
import main.java.rxjava.shedulers.SingleThreadScheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ObservableTest {

    private Scheduler ioScheduler;
    private Scheduler computationScheduler;
    private Scheduler singleScheduler;

    @Before
    public void setUp() {
        ioScheduler = new IOThreadScheduler();
        computationScheduler = new ComputationScheduler(4);
        singleScheduler = new SingleThreadScheduler();
    }

    @After
    public void tearDown() {
        ioScheduler.shutdown();
        computationScheduler.shutdown();
        singleScheduler.shutdown();
    }

    @Test
    public void testBasicObservable() {
        AtomicInteger sum = new AtomicInteger(0);
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable<Integer> observable = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onComplete();
        });

        observable.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                sum.addAndGet(item);
            }

            @Override
            public void onError(Throwable t) {
                fail("Should not call onError");
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }
        });

        assertEquals(3, sum.get());
        assertTrue(completed.get());
    }

    @Test
    public void testMapOperator() {
        Observable<Integer> source = Observable.create(emitter -> {
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        AtomicInteger result = new AtomicInteger(0);

        source.map(x -> x * 10)
              .subscribe(new Observer<Integer>() {
                  @Override
                  public void onNext(Integer item) {
                      result.addAndGet(item);
                  }

                  @Override
                  public void onError(Throwable t) {
                      fail("No error expected");
                  }

                  @Override
                  public void onComplete() {}
              });

        assertEquals(50, result.get());
    }

    @Test
    public void testFilterOperator() {
        Observable<Integer> source = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        });

        AtomicInteger count = new AtomicInteger(0);

        source.filter(x -> x % 2 == 1)
              .subscribe(new Observer<Integer>() {
                  @Override
                  public void onNext(Integer item) {
                      count.incrementAndGet();
                  }

                  @Override
                  public void onError(Throwable t) {
                      fail("No error expected");
                  }

                  @Override
                  public void onComplete() {}
              });

        assertEquals(2, count.get());
    }

    @Test
    public void testErrorHandling() {
        Observable<Integer> source = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onError(new RuntimeException("Test error"));
        });

        AtomicBoolean errorCalled = new AtomicBoolean(false);

        source.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {}

            @Override
            public void onError(Throwable t) {
                errorCalled.set(true);
                assertEquals("Test error", t.getMessage());
            }

            @Override
            public void onComplete() {
                fail("Should not complete");
            }
        });

        assertTrue(errorCalled.get());
    }

    @Test
    public void testDisposable() throws InterruptedException {
        Observable<Integer> source = Observable.create(emitter -> {
            for (int i = 0; i < 10; i++) {
                emitter.onNext(i);
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }
            emitter.onComplete();
        });

        AtomicInteger count = new AtomicInteger(0);

        Disposable[] disposableHolder = new Disposable[1];

        disposableHolder[0] = source.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                count.incrementAndGet();
                if (count.get() == 3) {
                    disposableHolder[0].dispose();
                }
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {
                fail("Should not complete due to dispose");
            }
        });

        Thread.sleep(100);

        assertEquals(3, count.get());
        assertTrue(disposableHolder[0].isDisposed());
    }
}
