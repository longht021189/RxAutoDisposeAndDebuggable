package library.autodispose;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;

public class ObservableConverter<T> implements io.reactivex.ObservableConverter<T, ObservableProxy<T>> {

    @Override
    @NonNull
    public ObservableProxy<T> apply(@NonNull Observable<T> upstream) {
        return new ObservableProxyImpl<>(upstream);
    }
}
