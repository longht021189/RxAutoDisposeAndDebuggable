package library.autodispose;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.ReplaySubject;

import java.util.concurrent.atomic.AtomicInteger;

class ObservableProxyImpl<T> implements ObservableProxy<T> {

    @NonNull
    private final Observable<T> upstream;

    @NonNull
    private final ReplaySubject<ObserverState> stateSubject;

    ObservableProxyImpl(@NonNull Observable<T> upstream, @NonNull ReplaySubject<ObserverState> stateSubject) {
        this.upstream = upstream;
        this.stateSubject = stateSubject;
    }

    @Override
    public Disposable subscribe(
            @Nullable Consumer<? super T> onNext,
            @Nullable Consumer<? super Throwable> onError,
            @Nullable Action onComplete,
            @Nullable Consumer<? super Disposable> onSubscribe
    ) {
        // Thread.currentThread().getStackTrace();
        final AtomicInteger stateValue = new AtomicInteger(0);

        Observable check = stateSubject
                .map(ObserverState::getValue)
                .map(value -> {

                });

        Observable stream = stateSubject
                .map(state -> {
                    boolean isStart;

                    switch (state) {
                        case Created:
                        case Resumed:
                        case Paused:
                            isStart = true;
                            break;

                        default:
                            isStart = false;
                            break;
                    }

                    return isStart;
                })
                .distinctUntilChanged()
                .switchMap(isStart -> {
                    Observable next;

                    if (isStart) {
                        next = upstream;
                    } else {
                        next = Observable.empty();
                    }

                    return next;
                });

        return stateSubject
                .flatMap(state -> {
                    switch (state) {
                        case Created:
                            break;

                        case Resumed:
                            break;

                        case Paused:
                            break;

                        case Destroyed:
                            break;
                    }

                    return upstream;
                })
                .subscribe();
    }

    private static class FunctionProxy<T> implements Function<ObserverState, Observable<T>> {

        @NonNull
        private final Observable<T> upstream;

        FunctionProxy(@NonNull Observable<T> upstream) {
            this.upstream = upstream;
        }

        @Override
        public Observable<T> apply(ObserverState state) {
            return null;
        }
    }
}
