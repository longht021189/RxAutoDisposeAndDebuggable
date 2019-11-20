package library.autodispose.observable;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.LambdaConsumerIntrospection;
import io.reactivex.plugins.RxJavaPlugins;
import library.autodispose.State;
import library.autodispose.StateController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class ObservableProxyImpl<T> implements ObservableProxy<T> {

    @NonNull
    private final Observable<T> upstream;

    @NonNull
    private final List<StateController> controllers;

    public ObservableProxyImpl(
            @NonNull Observable<T> upstream,
            StateController controller
    ) {
        ArrayList<StateController> list = new ArrayList<>();
        list.add(controller);

        this.upstream = upstream;
        this.controllers = list;
    }

    public ObservableProxyImpl(
            @NonNull Observable<T> upstream,
            StateController controller, List<StateController> controllers
    ) {
        ArrayList<StateController> list = new ArrayList<>();
        list.add(controller);
        list.addAll(controllers);

        this.upstream = upstream;
        this.controllers = list;
    }

    private boolean filterEvent(Data<T> data) {
        return (data.state == State.Resumed);
    }

    @Override
    @NonNull
    public ObservableProxy<T> dependsOn(@NonNull StateController controller) {
        return new ObservableProxyImpl<>(upstream, controller, controllers);
    }

    @Override
    @NonNull
    public Disposable subscribe(@NonNull Consumer<? super T> onNext) {
        return subscribeActual(onNext, null, null, null);
    }

    @Override
    @NonNull
    public Disposable subscribe(
            @NonNull Consumer<? super T> onNext,
            @NonNull Consumer<? super Throwable> onError
    ) {
        return subscribeActual(onNext, onError, null, null);
    }

    @Override
    @NonNull
    public Disposable subscribe(
            @NonNull Consumer<? super T> onNext,
            @NonNull Consumer<? super Throwable> onError,
            @NonNull Action onComplete
    ) {
        return subscribeActual(onNext, onError, onComplete, null);
    }

    @Override
    @NonNull
    public Disposable subscribe(
            @NonNull Consumer<? super T> onNext,
            @NonNull Consumer<? super Throwable> onError,
            @NonNull Action onComplete,
            @NonNull Consumer<? super Disposable> onSubscribe
    ) {
        return subscribeActual(onNext, onError, onComplete, onSubscribe);
    }

    @Override
    @NonNull
    public Disposable subscribeActual(
            @Nullable Consumer<? super T> onNext,
            @Nullable Consumer<? super Throwable> onError,
            @Nullable Action onComplete,
            @Nullable Consumer<? super Disposable> onSubscribe
    ) {
        StackTraceElement[] stackTraceElements =
                Thread.currentThread().getStackTrace();

        ObserverWrapper<T> observer = new ObserverWrapper<>(
                onNext, onError, onComplete, onSubscribe, stackTraceElements);

        Observable<State> state = Observable
                .fromIterable(controllers)
                .map(StateController::getStateObservable)
                .toList()
                .toObservable()
                .flatMap(list -> Observable.combineLatest(list, new Combiner()))
                .distinctUntilChanged()
                .doOnNext(observer::onStateChanged);

        Observable<State> start = state
                .filter(stateValue -> stateValue != State.Destroyed);

        Observable<T> stream = upstream
                .delaySubscription(start);

        Observable
                .combineLatest(stream, state, Data::new)
                .filter(this::filterEvent)
                .map(data -> data.value)
                .subscribe(observer);

        return observer;
    }

    private static class Data<T> {
        private final T value;
        private final State state;

        Data(T value, State state) {
            this.value = value;
            this.state = state;
        }
    }

    private static class Combiner implements Function<Object[], State> {
        @Override
        @NonNull
        public State apply(Object[] objects) {
            return null;
        }
    }

    /**
     * Copy from io.reactivex.internal.observers.LambdaObserver
     */
    private static class ObserverWrapper<T> extends AtomicReference<Disposable> implements Observer<T>, Disposable, LambdaConsumerIntrospection {

        @Nullable
        private final Consumer<? super T> onNext;

        @Nullable
        private final Consumer<? super Throwable> onError;

        @Nullable
        private final Action onComplete;

        @Nullable
        private final Consumer<? super Disposable> onSubscribe;

        @NonNull
        private final StackTraceElement[] stackTraceElements;

        ObserverWrapper(
                @Nullable Consumer<? super T> onNext, @Nullable Consumer<? super Throwable> onError,
                @Nullable Action onComplete, @Nullable Consumer<? super Disposable> onSubscribe,
                @NonNull StackTraceElement[] stackTraceElements
        ) {
            super();

            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
            this.onSubscribe = onSubscribe;
            this.stackTraceElements = stackTraceElements;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                try {
                    if (onSubscribe != null) {
                        onSubscribe.accept(this);
                    }
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    d.dispose();
                    onError(ex);
                }
            }
        }

        @Override
        public void onNext(T t) {
            if (!isDisposed()) {
                try {
                    if (onNext != null) {
                        onNext.accept(t);
                    }
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    get().dispose();
                    onError(e);
                }
            }
        }

        @Override
        public void onError(Throwable error) {
            error.setStackTrace(stackTraceElements);

            if (!isDisposed()) {
                lazySet(DisposableHelper.DISPOSED);
                if (onError != null) {
                    try {
                        onError.accept(error);
                    } catch (Throwable e) {
                        Exceptions.throwIfFatal(e);
                        RxJavaPlugins.onError(new CompositeException(error, e));
                    }
                } else {
                    RxJavaPlugins.onError(error);
                }
            } else {
                RxJavaPlugins.onError(error);
            }
        }

        @Override
        public void onComplete() {
            if (!isDisposed()) {
                lazySet(DisposableHelper.DISPOSED);
                try {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    RxJavaPlugins.onError(e);
                }
            }
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return get() == DisposableHelper.DISPOSED;
        }

        @Override
        public boolean hasCustomOnError() {
            return onError != Functions.ON_ERROR_MISSING;
        }

        synchronized void onStateChanged(@NonNull State state) {
            if (state == State.Destroyed) {
                dispose();
            }
        }
    }
}
