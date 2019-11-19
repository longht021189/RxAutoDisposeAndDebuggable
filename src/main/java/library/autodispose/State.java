package library.autodispose;

public enum State {
    Created(1, true),
    Resumed(2, true),
    Paused(2, true),
    Destroyed(3, false);

    private final int value;
    private final boolean start;

    State(int value, boolean start) {
        this.value = value;
        this.start = start;
    }

    public int getValue() {
        return value;
    }

    public boolean isStart() {
        return start;
    }
}
