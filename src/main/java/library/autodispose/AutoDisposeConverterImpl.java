package library.autodispose;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import library.autodispose.observable.ObservableProxy;
import library.autodispose.observable.ObservableProxyImpl;

final class AutoDisposeConverterImpl<T> implements AutoDisposeConverter<T> {

    @NonNull
    private final Observable<State> state;

    AutoDisposeConverterImpl(Observable<State> state) {
        this.state = state;
    }

    @Override
    @NonNull
    public ObservableProxy<T> apply(@NonNull Observable<T> upstream) {
        return new ObservableProxyImpl<>(upstream, state);
    }
}
