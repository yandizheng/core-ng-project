package core.framework.util;

/**
 * @author neo
 */
public final class StopWatch {
    private long start;

    public StopWatch() {
        reset();
    }

    public void reset() {
        start = System.nanoTime();
    }

    public long elapsedTime() {
        long end = System.nanoTime();
        return end - start;
    }
}
