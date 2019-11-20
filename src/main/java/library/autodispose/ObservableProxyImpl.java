package library.autodispose;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

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

        Observable<State> state = controller
                .getStateObservable()
                .distinctUntilChanged()
                .doOnNext(subscribeConsumer::onStateChanged);

        Observable<State> start = state
                .filter(stateValue -> stateValue != State.Destroyed);

        Observable<T> stream = upstream
                .delaySubscription(start);

        return Observable
                .combineLatest(stream, state, Data::new)
                .filter(this::filterEvent)
                .map(data -> data.value)
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
}
