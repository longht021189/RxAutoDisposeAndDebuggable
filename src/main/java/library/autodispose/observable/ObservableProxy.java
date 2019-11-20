package library.autodispose.observable;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import library.autodispose.StateController;

public interface ObservableProxy<T> {

    @NonNull
    Disposable subscribeActual(
            @Nullable Consumer<? super T> onNext,
            @Nullable Consumer<? super Throwable> onError,
            @Nullable Action onComplete,
            @Nullable Consumer<? super Disposable> onSubscribe);

    @NonNull
    Disposable subscribe(
            @NonNull Consumer<? super T> onNext,
            @NonNull Consumer<? super Throwable> onError,
            @NonNull Action onComplete,
            @NonNull Consumer<? super Disposable> onSubscribe);

    @NonNull
    Disposable subscribe(
            @NonNull Consumer<? super T> onNext,
            @NonNull Consumer<? super Throwable> onError,
            @NonNull Action onComplete);

    @NonNull
    Disposable subscribe(
            @NonNull Consumer<? super T> onNext,
            @NonNull Consumer<? super Throwable> onError);

    @NonNull
    Disposable subscribe(
            @NonNull Consumer<? super T> onNext);

    @NonNull
    ObservableProxy<T> dependsOn(
            @NonNull StateController controller);
}
