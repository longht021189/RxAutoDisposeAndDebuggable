package library.autodispose;

public final class AutoDispose {
    private AutoDispose() {}

    public static <T> AutoDisposeConverter<T> autoDispose(StateController controller) {
        return new AutoDisposeConverterImpl<>(controller);
    }
}
