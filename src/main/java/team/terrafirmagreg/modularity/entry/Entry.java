package team.terrafirmagreg.modularity.entry;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import team.terrafirmagreg.modularity.Identifier;

public interface Entry<T> {
    Optional<T> get();

    T require();

    default boolean isPresent() {
        return get().isPresent();
    }

    Identifier id();

    default void ifPresent(Consumer<? super T> action) {
        get().ifPresent(action);
    }

    default <R> Optional<R> map(Function<? super T, ? extends R> mapper) {
        return get().map(mapper);
    }

    default <R> Optional<R> flatMap(Function<? super T, Optional<R>> mapper) {
        return get().flatMap(mapper);
    }

    default Optional<T> filter(Predicate<? super T> predicate) {
        return get().filter(predicate);
    }

    default T orElse(T other) {
        return get().orElse(other);
    }

    default T orElseGet(Supplier<? extends T> supplier) {
        return get().orElseGet(supplier);
    }

    default Stream<T> stream() {
        return get().stream();
    }
}
