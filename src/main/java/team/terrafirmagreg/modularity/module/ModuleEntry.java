package team.terrafirmagreg.modularity.module;

import java.util.Optional;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import team.terrafirmagreg.modularity.Identifier;
import team.terrafirmagreg.modularity.entry.UnitEntry;

public final class ModuleEntry<T extends Module> extends UnitEntry<T, ModuleContext> {
    @Getter
    @Accessors(fluent = true)
    private final ModuleSpec spec;

    @Nullable
    private Failure failure;

    ModuleEntry(Identifier id, ModuleSpec spec, Supplier<T> factory) {
        super(id, factory);
        this.spec = spec;
    }

    public void fail(@Nullable Failure failure) {
        this.failure = failure;
        gate(false);
    }

    public void clearFailure() {
        this.failure = null;
    }

    public ModuleStatus status() {
        return new ModuleStatus(id(), isEnabled(), failure);
    }

    public <S> void provide(Class<S> type, S impl) {
        context()
                .orElseThrow(() -> new IllegalStateException("Module '" + id() + "' has no context"))
                .kernel()
                .provide(id(), type, impl);
    }

    public <S> Optional<S> service(Class<S> type) {
        return context().flatMap(ctx -> ctx.kernel().serviceOf(id(), type));
    }
}
