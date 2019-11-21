package library.autodispose;

import io.reactivex.Observable;

public final class AutoDispose {
    private AutoDispose() {}

    public static <T> AutoDisposeConverter<T> autoDispose(Observable<State> state) {
        return new AutoDisposeConverterImpl<>(state);
    }
}
