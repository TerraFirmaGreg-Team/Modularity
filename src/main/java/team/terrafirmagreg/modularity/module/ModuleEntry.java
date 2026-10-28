package team.terrafirmagreg.modularity.module;

import java.util.Optional;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import team.terrafirmagreg.modularity.Identifier;
import team.terrafirmagreg.modularity.entry.UnitEntry;
import team.terrafirmagreg.modularity.module.Timings.Phase;

public final class ModuleEntry<T extends Module> extends UnitEntry<T, ModuleContext> {
    @Getter
    @Accessors(fluent = true)
    private final ModuleSpec spec;

    @Getter
    @Accessors(fluent = true)
    @Nullable
    private Failure failure;

    private final long[] phaseNanos = new long[Phase.values().length];

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

    public PhaseTimer phase(Phase phase) {
        return new PhaseTimer(phase);
    }

    public final class PhaseTimer implements AutoCloseable {
        private final Phase phase;
        private final long start = System.nanoTime();

        private PhaseTimer(Phase phase) {
            this.phase = phase;
        }

        @Override
        public void close() {
            phaseNanos[phase.ordinal()] = System.nanoTime() - start;
        }
    }

    public Timings timings() {
        return new Timings(phaseNanos[0], phaseNanos[1], phaseNanos[2], phaseNanos[3]);
    }

    public ModuleStatus status() {
        return new ModuleStatus(id(), isEnabled(), failure, timings());
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
