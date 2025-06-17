package main.java.rxjava;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

public class Observable<T> {

    public interface OnSubscribe<T> {
        void subscribe(Observer<? super T> observer);
    }

    private final OnSubscribe<T> onSubscribe;

    private Scheduler subscribeScheduler;
    private Scheduler observeScheduler;

    private Observable(OnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    public static <T> Observable<T> create(OnSubscribe<T> onSubscribe) {
        return new Observable<>(onSubscribe);
    }

    public Disposable subscribe(Observer<? super T> observer) {
        AtomicBoolean disposed = new AtomicBoolean(false);

        Runnable subscribeTask = () -> {
            if (disposed.get()) return;
            try {
                onSubscribe.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        if (!disposed.get()) {
                            if (observeScheduler != null) {
                                observeScheduler.execute(() -> observer.onNext(item));
                            } else {
                                observer.onNext(item);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (!disposed.get()) {
                            if (observeScheduler != null) {
                                observeScheduler.execute(() -> observer.onError(t));
                            } else {
                                observer.onError(t);
                            }
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (!disposed.get()) {
                            if (observeScheduler != null) {
                                observeScheduler.execute(observer::onComplete);
                            } else {
                                observer.onComplete();
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                if (!disposed.get()) {
                    if (observeScheduler != null) {
                        observeScheduler.execute(() -> observer.onError(t));
                    } else {
                        observer.onError(t);
                    }
                }
            }
        };

        if (subscribeScheduler != null) {
            subscribeScheduler.execute(subscribeTask);
        } else {
            subscribeTask.run();
        }

        return new Disposable() {
            @Override
            public void dispose() {
                disposed.set(true);
            }

            @Override
            public boolean isDisposed() {
                return disposed.get();
            }
        };
    }

    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        return create(observer -> {
            this.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    try {
                        R mapped = mapper.apply(item);
                        observer.onNext(mapped);
                    } catch (Throwable t) {
                        observer.onError(t);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    observer.onError(t);
                }

                @Override
                public void onComplete() {
                    observer.onComplete();
                }
            });
        });
    }

    public Observable<T> filter(Predicate<? super T> predicate) {
        return create(observer -> {
            this.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    try {
                        if (predicate.test(item)) {
                            observer.onNext(item);
                        }
                    } catch (Throwable t) {
                        observer.onError(t);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    observer.onError(t);
                }

                @Override
                public void onComplete() {
                    observer.onComplete();
                }
            });
        });
    }

    public <R> Observable<R> flatMap(Function<? super T, Observable<? extends R>> mapper) {
        return create(observer -> {
            final Object lock = new Object();
            final int[] activeCount = {1}; // 1 — внешний поток
            final boolean[] isOuterCompleted = {false};
            final List<Disposable> disposables = Collections.synchronizedList(new ArrayList<>());

            Disposable outerDisposable = this.subscribe(new Observer<T>() {
                @Override
                public void onNext(T item) {
                    Observable<? extends R> innerObservable;
                    try {
                        innerObservable = mapper.apply(item);
                    } catch (Throwable t) {
                        observer.onError(t);
                        return;
                    }

                    synchronized (lock) {
                        activeCount[0]++;
                    }

                    Disposable innerDisposable = innerObservable.subscribe(new Observer<R>() {
                        @Override
                        public void onNext(R r) {
                            observer.onNext(r);
                        }

                        @Override
                        public void onError(Throwable t) {
                            observer.onError(t);
                        }

                        @Override
                        public void onComplete() {
                            synchronized (lock) {
                                activeCount[0]--;
                                if (activeCount[0] == 0 && isOuterCompleted[0]) {
                                    observer.onComplete();
                                }
                            }
                        }
                    });

                    disposables.add(innerDisposable);
                }

                @Override
                public void onError(Throwable t) {
                    observer.onError(t);
                }

                @Override
                public void onComplete() {
                    synchronized (lock) {
                        isOuterCompleted[0] = true;
                        activeCount[0]--;
                        if (activeCount[0] == 0) {
                            observer.onComplete();
                        }
                    }
                }
            });

            disposables.add(outerDisposable);
        });
    }

    public Observable<T> subscribeOn(Scheduler scheduler) {
        this.subscribeScheduler = scheduler;
        return this;
    }

    public Observable<T> observeOn(Scheduler scheduler) {
        this.observeScheduler = scheduler;
        return this;
    }
}
