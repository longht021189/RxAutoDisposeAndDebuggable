package library.autodispose;

import javax.validation.constraints.Size;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;

public final class AutoDispose {
    private AutoDispose() {}

    @NonNull
    public static <T> AutoDisposeConverter<T> autoDispose(@NonNull Observable<State> state) {
        return new AutoDisposeConverterImpl<>(state);
    }

    @NonNull
    public static Observable<State> merge(@Size(min = 2) Observable<State>... states) {
        return merge(states, 0);
    }

    @NonNull
    private static Observable<State> merge(Observable<State>[] states, int index) {
        return states[index].distinctUntilChanged().switchMap(value -> {
            if (index + 1 >= states.length) {
                return Observable.just(value);
            } else {
                return merge(states, index + 1).map(childValue -> {
                    State result = childValue;

                    if (value == State.Destroyed || childValue == State.Destroyed) {
                        result = State.Destroyed;
                    } else if (value == State.Created) {
                        result = State.Created;
                    } else if (value == State.Paused) {
                        if (result != State.Created) {
                            result = State.Paused;
                        }
                    }

                    return result;
                });
            }
        });
    }
}
