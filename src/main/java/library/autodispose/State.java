package library.autodispose;

public enum State {
    Created(1),
    Resumed(2),
    Paused(2),
    Destroyed(3);

    private final int value;

    State(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
