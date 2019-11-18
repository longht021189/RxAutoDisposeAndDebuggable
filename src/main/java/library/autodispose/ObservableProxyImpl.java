package library.autodispose;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;

class ObservableProxyImpl<T> implements ObservableProxy<T> {

    @NonNull
    private final Observable<T> upstream;

    private BehaviorSubject<ObserverState> stateSubject = BehaviorSubject.create();

    ObservableProxyImpl(@NonNull Observable<T> upstream) {
        this.upstream = upstream;
    }

    @Override
    public Disposable subscribe(
            @Nullable Consumer<? super T> onNext,
            @Nullable Consumer<? super Throwable> onError,
            @Nullable Action onComplete,
            @Nullable Consumer<? super Disposable> onSubscribe
    ) {
        // Thread.currentThread().getStackTrace();
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
