package library.autodispose.observable;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import library.autodispose.StateController;

final class ObservableConverter<T> implements io.reactivex.ObservableConverter<T, ObservableProxy<T>> {

    @NonNull
    private final StateController controller;

    ObservableConverter(StateController controller) {
        this.controller = controller;
    }

    @Override
    @NonNull
    public ObservableProxy<T> apply(@NonNull Observable<T> upstream) {
        return new ObservableProxyImpl<>(upstream, controller);
    }
}
