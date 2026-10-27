package team.terrafirmagreg.modularity.kernel;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.neoforged.bus.api.Event;
import team.terrafirmagreg.modularity.Identifier;
import team.terrafirmagreg.modularity.module.Module;

@Getter
@Accessors(fluent = true)
public sealed class ModuleEvent extends Event {

    private final Identifier id;
    private final Module module;

    private ModuleEvent(Identifier id, Module module) {
        this.id = id;
        this.module = module;
    }

    public static final class Enabled extends ModuleEvent {
        public Enabled(Identifier id, Module module) {
            super(id, module);
        }
    }

    public static final class Disabled extends ModuleEvent {
        public Disabled(Identifier id, Module module) {
            super(id, module);
        }
    }
}
