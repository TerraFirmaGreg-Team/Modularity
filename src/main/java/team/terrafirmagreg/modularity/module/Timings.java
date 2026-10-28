package team.terrafirmagreg.modularity.module;

public record Timings(long constructNanos, long declareNanos, long subscribeNanos, long enableNanos) {

    public static final Timings ZERO = new Timings(0, 0, 0, 0);

    public long totalNanos() {
        return constructNanos + declareNanos + subscribeNanos + enableNanos;
    }

    public enum Phase {
        CONSTRUCT,
        DECLARE,
        SUBSCRIBE,
        ENABLE
    }
}
