package library.autodispose;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.subjects.BehaviorSubject;
import library.autodispose.error.WrongStateException;

public final class StateController {

    private final BehaviorSubject<State> stateSubject = BehaviorSubject.create();

    public void dispatchState(@NonNull State value) throws WrongStateException {
        State state = stateSubject.getValue();

        if (state != value) {
            if (state != null && value.getValue() < state.getValue()) {
                throw new WrongStateException();
            }

            stateSubject.onNext(value);
        }
    }

    @NonNull
    public Observable<State> getStateObservable() {
        return stateSubject;
    }
}
