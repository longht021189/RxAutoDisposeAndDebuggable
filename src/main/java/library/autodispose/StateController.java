package library.autodispose;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.subjects.BehaviorSubject;
import library.autodispose.error.ClosedException;

public final class StateController {

    private final BehaviorSubject<State> stateSubject = BehaviorSubject.create();

    public void dispatchState(@NonNull State value) throws ClosedException {
        State state = stateSubject.getValue();

        if (state == null || value.getValue() >= state.getValue()) {
            stateSubject.onNext(value);
        } else if (state == State.Destroyed) {
            throw new ClosedException();
        }
    }

    @NonNull
    Observable<State> getStateObservable() {
        return stateSubject;
    }
}
