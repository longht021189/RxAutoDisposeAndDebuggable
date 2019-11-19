package library.autodispose;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

import java.util.Stack;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

final class ObservableProxyImpl<T> implements ObservableProxy<T> {

    @NonNull
    private final Observable<T> upstream;

    @NonNull
    private final StateController controller;

    ObservableProxyImpl(
            @NonNull Observable<T> upstream,
            @NonNull StateController controller
    ) {
        this.upstream = upstream;
        this.controller = controller;
    }

    private Observable<T> nextStream(boolean isStart, AtomicReference<ObservableEmitter<State>> emitterReference) {
        if (isStart) {
            return upstream.doOnComplete(() -> {
                ObservableEmitter<State> emitter = emitterReference.get();

                if (emitter != null && !emitter.isDisposed()) {
                    emitter.onComplete();
                }
            });
        } else {
            return Observable.empty();
        }
    }

    private boolean filterEvent(Data<T> data) {
        return (data.state == State.Resumed);
    }

    @Override
    public Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Action onComplete, Consumer<? super Disposable> onSubscribe) {
        return subscribeInternal(onNext, onError, onComplete, onSubscribe);
    }

    @Override
    public Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Action onComplete) {
        return subscribeInternal(onNext, onError, onComplete, null);
    }

    @Override
    public Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
        return subscribeInternal(onNext, onError, null, null);
    }

    @Override
    public Disposable subscribe(Consumer<? super T> onNext) {
        return subscribeInternal(onNext, null, null, null);
    }

    private Disposable subscribeInternal(
            @Nullable Consumer<? super T> onNext,
            @Nullable Consumer<? super Throwable> onError,
            @Nullable Action onComplete,
            @Nullable Consumer<? super Disposable> onSubscribe
    ) {
        StackTraceElement[] stackTraceElements =
                Thread.currentThread().getStackTrace();

        SubscribeConsumer subscribeConsumer =
                new SubscribeConsumer(onSubscribe);

        ErrorConsumer errorConsumer =
                new ErrorConsumer(onError, stackTraceElements);

        AtomicReference<Disposable>
                disposableReference = new AtomicReference<>();

        AtomicReference<ObservableEmitter<State>>
                emitterReference = new AtomicReference<>();

        Observable<State> state1 = Observable
                .create(emitter -> {
                    emitterReference.set(emitter);

                    controller
                            .getStateObservable()
                            .subscribe(new StateObserver(emitter, disposableReference));
                });

        Observable<State> state = state1
                .distinctUntilChanged()
                .doOnNext(subscribeConsumer::onStateChanged);

        Observable<T> stream = state
                .map(State::isStart)
                .distinctUntilChanged()
                .switchMap(isStart -> nextStream(isStart, emitterReference));

        return Observable
                .combineLatest(stream, state, Data::new)
                .filter(this::filterEvent)
                .map(data -> data.value)
                .doFinally(() -> {
                    Disposable disposable = disposableReference.get();

                    if (disposable != null && !disposable.isDisposed()) {
                        disposable.dispose();
                    }
                })
                .subscribe(new NextConsumer<>(onNext), errorConsumer
                        , new CompleteAction(onComplete), subscribeConsumer);
    }

    private static class Data<T> {
        private final T value;
        private final State state;

        Data(T value, State state) {
            this.value = value;
            this.state = state;
        }
    }

    private static class NextConsumer<T> implements Consumer<T> {
        @Nullable
        private final Consumer<? super T> onNext;

        NextConsumer(@Nullable Consumer<? super T> onNext) {
            this.onNext = onNext;
        }

        @Override
        public void accept(T t) throws Exception {
            if (onNext != null) {
                onNext.accept(t);
            }
        }
    }

    private static class SubscribeConsumer implements Consumer<Disposable> {
        @Nullable
        private final Consumer<? super Disposable> onSubscribe;

        private final CompositeDisposable disposables = new CompositeDisposable();

        SubscribeConsumer(@Nullable Consumer<? super Disposable> onSubscribe) {
            this.onSubscribe = onSubscribe;
        }

        synchronized void onStateChanged(@NonNull State state) {
            if (state == State.Destroyed) {
                disposables.dispose();
            }
        }

        @Override
        public synchronized void accept(Disposable disposable) throws Exception {
            if (disposables.isDisposed()) {
                disposable.dispose();
                return;
            }

            disposables.add(disposable);

            if (onSubscribe != null) {
                onSubscribe.accept(disposable);
            }
        }
    }

    private static class ErrorConsumer implements Consumer<Throwable> {
        @Nullable
        private final Consumer<? super Throwable> onError;

        @NonNull
        private final StackTraceElement[] stackTraceElements;

        ErrorConsumer(@Nullable Consumer<? super Throwable> onError, @NonNull StackTraceElement[] stackTraceElements) {
            this.onError = onError;
            this.stackTraceElements = stackTraceElements;
        }

        @Override
        public void accept(Throwable throwable) throws Exception {
            throwable.setStackTrace(stackTraceElements);

            if (onError != null) {
                onError.accept(throwable);
            } else {
                throw new Exception(throwable);
            }
        }
    }

    private static class CompleteAction implements Action {
        @Nullable
        private final Action onComplete;

        CompleteAction(Action onComplete) {
            this.onComplete = onComplete;
        }

        @Override
        public void run() throws Exception {
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    private static class StateObserver implements Observer<State> {
        @NonNull
        private final ObservableEmitter<State> emitter;

        @NonNull
        private final AtomicReference<Disposable> disposableReference;

        StateObserver(
                @NonNull ObservableEmitter<State> emitter,
                @NonNull AtomicReference<Disposable> disposableReference
        ) {
            this.emitter = emitter;
            this.disposableReference = disposableReference;
        }

        @Override
        public void onSubscribe(Disposable d) {
            disposableReference.set(d);
        }

        @Override
        public void onNext(State state) {
            if (!emitter.isDisposed()) {
                emitter.onNext(state);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!emitter.isDisposed()) {
                emitter.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!emitter.isDisposed()) {
                emitter.onComplete();
            }
        }
    }
}
