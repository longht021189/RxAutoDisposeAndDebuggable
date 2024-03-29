package library.autodispose.observable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.LambdaConsumerIntrospection;
import io.reactivex.plugins.RxJavaPlugins;
import library.autodispose.State;

public final class ObservableProxyImpl<T> implements ObservableProxy<T> {

    @NonNull
    private final Observable<T> upstream;

    @NonNull
    private final Observable<State> stateObservable;

    public ObservableProxyImpl(
            @NonNull Observable<T> upstream,
            @NonNull Observable<State> state
    ) {
        this.upstream = upstream;
        this.stateObservable = state;
    }

    private boolean filterEvent(Data<T> data) {
        return (data.state == State.Resumed);
    }

    @NotNull
    @Override
    public Disposable subscribe(
            @Nullable Consumer<T> onNext, @Nullable Consumer<Throwable> onError,
            @Nullable Action onComplete, @Nullable Consumer<Disposable> onSubscribe
    ) {
        StackTraceElement[] stackTraceElements =
                Thread.currentThread().getStackTrace();

        ObserverWrapper<T> observer = new ObserverWrapper<>(
                onNext, onError, onComplete, onSubscribe, stackTraceElements);

        Observable<State> state = stateObservable
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

    @NotNull
    @Override
    public ObservableProxy<T> doOnSubscribe(@NotNull Consumer<Disposable> onSubscribe) {
        return new DoOnSubscribe<>(this, onSubscribe);
    }

    private static class Data<T> {
        private final T value;
        private final State state;

        Data(T value, State state) {
            this.value = value;
            this.state = state;
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

    private static class Merged implements Consumer<Disposable> {

        private final Consumer<Disposable> consumer1;

        @Nullable
        private final Consumer<Disposable> consumer2;

        Merged(Consumer<Disposable> consumer1, @Nullable Consumer<Disposable> consumer2) {
            this.consumer1 = consumer1;
            this.consumer2 = consumer2;
        }

        @Override
        public void accept(Disposable disposable) throws Exception {
            consumer1.accept(disposable);

            if (consumer2 != null) {
                consumer2.accept(disposable);
            }
        }
    }

    private static class DoOnSubscribe<T> implements ObservableProxy<T> {

        private final ObservableProxy<T> origin;
        private final Consumer<Disposable> consumer;

        DoOnSubscribe(ObservableProxy<T> origin, Consumer<Disposable> consumer) {
            this.origin = origin;
            this.consumer = consumer;
        }

        @NotNull
        @Override
        public Disposable subscribe(
                @Nullable Consumer<T> onNext, @Nullable Consumer<Throwable> onError,
                @Nullable Action onComplete, @Nullable Consumer<Disposable> onSubscribe
        ) {
            return origin.subscribe(onNext, onError, onComplete, new Merged(consumer, onSubscribe));
        }

        @NotNull
        @Override
        public ObservableProxy<T> doOnSubscribe(@NotNull Consumer<Disposable> onSubscribe) {
            return new DoOnSubscribe<>(this, onSubscribe);
        }
    }
}
