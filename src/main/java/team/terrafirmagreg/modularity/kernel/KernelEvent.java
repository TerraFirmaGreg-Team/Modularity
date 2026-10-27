package team.terrafirmagreg.modularity.kernel;

import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.neoforged.bus.api.Event;
import team.terrafirmagreg.modularity.Identifier;

@Getter
@Accessors(fluent = true)
public sealed class KernelEvent extends Event {

    private final String appId;

    private KernelEvent(String appId) {
        this.appId = appId;
    }

    @Getter
    @Accessors(fluent = true)
    public static final class Loaded extends KernelEvent {
        private final List<Identifier> enabled;
        private final List<Identifier> disabled;

        public Loaded(String appId, List<Identifier> enabled, List<Identifier> disabled) {
            super(appId);
            this.enabled = List.copyOf(enabled);
            this.disabled = List.copyOf(disabled);
        }
    }

    public static final class Reloaded extends KernelEvent {
        public Reloaded(String appId) {
            super(appId);
        }
    }

    public static final class Unloaded extends KernelEvent {
        public Unloaded(String appId) {
            super(appId);
        }
    }
}
