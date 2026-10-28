package team.terrafirmagreg.modularity.kernel;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;
import team.terrafirmagreg.modularity.Identifier;
import team.terrafirmagreg.modularity.event.EventBus;
import team.terrafirmagreg.modularity.module.ModuleRegistrate;

public final class KernelBuilder {

    final String appId;

    Path configDir;
    EventBus bus;
    boolean memoryConfigs;

    @Nullable
    ModuleRegistrate registrate;

    KernelBuilder(String appId) {
        Identifier.of(appId, "_");
        this.appId = appId;
        this.configDir = Path.of("config", appId);
        this.bus = new EventBus();
    }

    public KernelBuilder configDir(Path configDir) {
        this.configDir = configDir;
        return this;
    }

    public KernelBuilder bus(EventBus bus) {
        this.bus = bus;
        return this;
    }

    public KernelBuilder memoryConfigs() {
        return memoryConfigs(true);
    }

    public KernelBuilder memoryConfigs(boolean memoryConfigs) {
        this.memoryConfigs = memoryConfigs;
        return this;
    }

    public KernelBuilder registrate(ModuleRegistrate registrate) {
        this.registrate = registrate;
        return this;
    }

    public Kernel build() {
        return new Kernel(this);
    }
}
