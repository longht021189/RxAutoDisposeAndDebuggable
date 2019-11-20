package library.autodispose;

import io.reactivex.ObservableConverter;
import library.autodispose.observable.ObservableProxy;

public interface AutoDisposeConverter<T> extends ObservableConverter<T, ObservableProxy<T>> {
}
