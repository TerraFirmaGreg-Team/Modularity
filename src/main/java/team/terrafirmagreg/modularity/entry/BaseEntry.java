package team.terrafirmagreg.modularity.entry;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import team.terrafirmagreg.modularity.Identifier;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntry<T> implements Entry<T> {
    @Getter
    @Accessors(fluent = true)
    private final Identifier id;

    private boolean gated = true;
    private T bound;

    public void gate(boolean allowed) {
        this.gated = allowed;
    }

    public boolean isEnabled() {
        return gated;
    }

    public void bind(T value) {
        if (this.bound != null) {
            throw new IllegalStateException("Entry '" + id + "' is already bound");
        }
        this.bound = value;
    }

    public void unbind() {
        this.bound = null;
    }

    @Nullable
    public T instance() {
        return bound;
    }

    @Override
    public Optional<T> get() {
        return gated && bound != null ? Optional.of(bound) : Optional.empty();
    }

    @Override
    public T require() {
        return get().orElseThrow(() ->
                new IllegalStateException("Entry '" + id + "' is " + (gated ? "not bound yet" : "disabled")));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + "]";
    }
}
