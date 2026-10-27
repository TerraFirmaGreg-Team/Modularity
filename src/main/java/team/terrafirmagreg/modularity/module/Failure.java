package team.terrafirmagreg.modularity.module;

import org.jspecify.annotations.Nullable;

public record Failure(
        Kind kind, @Nullable String detail, @Nullable Throwable cause) {

    public enum Kind {
        TOGGLE,

        DEPENDENCY,

        GATE,

        CONSTRUCT,

        DECLARE,

        ENABLE
    }

    public static Failure of(Kind kind, String detail) {
        return new Failure(kind, detail, null);
    }

    public static Failure of(Kind kind, Throwable cause) {
        return new Failure(kind, cause.getMessage(), cause);
    }
}
