package team.terrafirmagreg.modularity.module;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import org.slf4j.Logger;
import team.terrafirmagreg.konf.ConfigFile;
import team.terrafirmagreg.modularity.Identifier;
import team.terrafirmagreg.modularity.event.EventBus;
import team.terrafirmagreg.modularity.kernel.Kernel;

@Accessors(fluent = true)
public final class ModuleContext {
    @Getter
    private final Identifier id;

    @Getter
    private final Logger log;

    @Getter
    private final Kernel kernel;

    private final ModuleEntry<?> entry;

    public ModuleContext(Identifier id, Logger log, Kernel kernel, ModuleEntry<?> entry) {
        this.id = id;
        this.log = log;
        this.kernel = kernel;
        this.entry = entry;
    }

    public EventBus bus() {
        return kernel.bus();
    }

    public <T extends Event> T post(T event) {
        return kernel.post(event);
    }

    public <T extends Event> CompletableFuture<T> postAsync(T event) {
        return kernel.postAsync(event);
    }

    public ConfigFile config() {
        return kernel.config(id, null);
    }

    public ConfigFile config(String name) {
        return kernel.config(id, name);
    }

    public void subscriber(String path, Supplier<Object> supplier) {
        entry.subscriber(path, supplier);
    }

    public void cleanup(Runnable runnable) {
        entry.cleanup(runnable);
    }

    public <T extends Event> void listen(Class<T> type, Consumer<T> listener) {
        listen(EventPriority.NORMAL, false, type, listener);
    }

    public <T extends Event> void listen(
            EventPriority priority, boolean receiveCanceled, Class<T> type, Consumer<T> listener) {
        entry.listener("listener/" + entry.listeners().size(), type, priority, receiveCanceled, listener);
    }

    public Identifier id(String path) {
        return Identifier.of(id.domain(), id.path() + "/" + path);
    }

    public <T> void provide(Class<T> type, T service) {
        kernel.provide(id, type, service);
    }

    public <T> Optional<T> service(Class<T> type) {
        return kernel.service(type);
    }

    public <T> T require(Class<T> type) {
        return kernel.requireService(type);
    }
}
