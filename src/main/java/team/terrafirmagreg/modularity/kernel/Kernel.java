package team.terrafirmagreg.modularity.kernel;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.neoforged.bus.api.Event;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.terrafirmagreg.konf.ConfigFile;
import team.terrafirmagreg.modularity.Identifier;
import team.terrafirmagreg.modularity.event.EventBus;
import team.terrafirmagreg.modularity.module.Failure;
import team.terrafirmagreg.modularity.module.Module;
import team.terrafirmagreg.modularity.module.ModuleContext;
import team.terrafirmagreg.modularity.module.ModuleEntry;
import team.terrafirmagreg.modularity.module.ModuleRegistrate;
import team.terrafirmagreg.modularity.module.ModuleSpec;
import team.terrafirmagreg.modularity.module.ModuleStatus;

@Accessors(fluent = true)
public final class Kernel implements AutoCloseable {

    @Getter
    private final String appId;

    @Getter
    private final Path configDir;

    @Getter
    private final EventBus bus;

    private final Logger log;
    private final ModuleRegistrate registrate;
    private final ModuleToggles toggles;
    private final ConfigFiles configs;

    private final Map<String, ModuleEntry<?>> byId = new Object2ObjectLinkedOpenHashMap<>();
    private final List<ModuleEntry<?>> order = new ObjectArrayList<>();
    private final Map<Class<?>, Provided> services = new ConcurrentHashMap<>();
    private boolean loaded;

    private record Provided(Object impl, Identifier owner) {}

    Kernel(KernelBuilder builder) {
        this.appId = builder.appId;
        this.configDir = builder.configDir;
        this.bus = builder.bus;
        this.registrate = builder.registrate != null ? builder.registrate : ModuleRegistrate.of(appId);
        this.log = LoggerFactory.getLogger(appId);
        this.toggles = new ModuleToggles(configDir.resolve("modules.toml"), builder.memoryConfigs);
        this.configs = new ConfigFiles(configDir.resolve("modules"), log, builder.memoryConfigs);
    }

    public static KernelBuilder builder(String appId) {
        return new KernelBuilder(appId);
    }

    @Nullable
    public ModuleEntry<?> entry(String moduleId) {
        return byId.get(moduleId);
    }

    public <T extends Module> Optional<T> module(Class<T> type) {
        return order.stream()
                .flatMap(e -> e.get().stream())
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }

    public Optional<Module> module(String moduleId) {
        ModuleEntry<?> entry = byId.get(moduleId);
        return entry != null ? entry.get().map(Module.class::cast) : Optional.empty();
    }

    public boolean isModuleEnabled(String moduleId) {
        ModuleEntry<?> entry = byId.get(moduleId);
        return entry != null && entry.isEnabled();
    }

    public void setEnabled(String moduleId, boolean enabled) {
        if (!loaded) {
            throw new IllegalStateException("Kernel is not loaded");
        }
        ModuleEntry<?> entry = entry(moduleId);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown module '" + moduleId + "'");
        }
        if (entry.spec().alwaysOn()) {
            throw new IllegalStateException("Module '" + moduleId + "' is alwaysOn and cannot be toggled");
        }
        toggles.set(moduleId, enabled);
        if (entry.isEnabled() == enabled) {
            return;
        }
        if (enabled) {
            UnitPipeline.activate(entry, byId, toggles, this, log);
            recoverDependents();
        } else {
            disableCascade(entry);
        }
    }

    public boolean applyToggles() {
        if (!loaded) {
            throw new IllegalStateException("Kernel is not loaded");
        }
        toggles.reload();
        List<ModuleEntry<?>> toDisable = new ObjectArrayList<>();
        List<ModuleEntry<?>> toEnable = new ObjectArrayList<>();
        for (ModuleEntry<?> e : order) {
            if (e.spec().alwaysOn()) {
                continue;
            }
            boolean want = toggles.enabled(e);
            if (e.isEnabled() != want) {
                (want ? toEnable : toDisable).add(e);
            }
        }
        toDisable.forEach(e -> setEnabled(e.id().path(), false));
        toEnable.forEach(e -> setEnabled(e.id().path(), true));
        return !(toDisable.isEmpty() && toEnable.isEmpty());
    }

    public List<ModuleEntry<?>> modules() {
        return List.copyOf(order);
    }

    public ConfigFile config(Identifier moduleId, @Nullable String name) {
        return configs.open(moduleId, name);
    }

    public <T extends Event> T post(T event) {
        return bus.post(event);
    }

    public <T extends Event> CompletableFuture<T> postAsync(T event) {
        return bus.postAsync(event);
    }

    public List<ModuleStatus> report() {
        return order.stream().map(ModuleEntry::status).toList();
    }

    public Optional<ModuleStatus> status(String moduleId) {
        return Optional.ofNullable(byId.get(moduleId)).map(ModuleEntry::status);
    }

    public String reportText() {
        StringBuilder sb = new StringBuilder();
        sb.append("%-24s %-8s %-12s %s%n".formatted("id", "enabled", "failure", "total ms"));
        for (ModuleStatus s : report()) {
            sb.append("%-24s %-8s %-12s %.2f%n"
                    .formatted(
                            s.id().path(),
                            s.enabled(),
                            s.failure() != null ? s.failure().kind() : "-",
                            s.timings().totalNanos() / 1_000_000.0));
        }
        return sb.toString();
    }

    public <S> void provide(Identifier owner, Class<S> type, S impl) {
        Provided previous = services.compute(type, (t, p) -> p != null ? p : new Provided(impl, owner));
        if (previous.impl() != impl) {
            throw new IllegalStateException("Service '" + type.getSimpleName() + "' already provided");
        }
    }

    @SuppressWarnings("unchecked")
    public <S> Optional<S> service(Class<S> type) {
        return Optional.ofNullable(services.get(type)).map(p -> (S) p.impl());
    }

    @SuppressWarnings("unchecked")
    public <S> Optional<S> serviceOf(Identifier owner, Class<S> type) {
        Provided p = services.get(type);
        return p != null && p.owner().equals(owner) ? Optional.of((S) p.impl()) : Optional.empty();
    }

    public <S> S requireService(Class<S> type) {
        return service(type)
                .orElseThrow(() -> new IllegalStateException("Service '" + type.getSimpleName() + "' is not provided"));
    }

    public void dropServices(Identifier owner) {
        services.values().removeIf(p -> p.owner().equals(owner));
    }

    public void load() {
        if (loaded) {
            return;
        }
        loaded = true;

        if (registrate.entries().isEmpty()) {
            log.warn("No modules registered for app '{}'", appId);
            return;
        }
        registrate.freeze();

        for (ModuleEntry<?> entry : registrate.entries()) {
            byId.put(entry.id().path(), entry);
        }
        for (String id : DependencyGraph.sort(new ObjectArrayList<>(byId.keySet()), edges(true), edges(false))) {
            order.add(byId.get(id));
        }

        toggles.open();
        for (ModuleEntry<?> entry : order) {
            UnitPipeline.activate(entry, byId, toggles, this, log);
        }

        List<Identifier> enabled = order.stream()
                .filter(ModuleEntry::isEnabled)
                .map(ModuleEntry::id)
                .toList();
        List<Identifier> disabled =
                order.stream().filter(e -> !e.isEnabled()).map(ModuleEntry::id).toList();
        log.info("Kernel loaded for '{}': enabled {}; disabled {}", appId, enabled, disabled);

        toggles.sync();
        configs.syncAll();
        bus.post(new KernelEvent.Loaded(appId, enabled, disabled));
    }

    public void reload() {
        configs.reloadAll();
        for (ModuleEntry<?> entry : order) {
            Module module = entry.get().orElse(null);
            if (module == null) {
                continue;
            }
            ModuleContext ctx = contextOf(entry);
            try {
                module.onReload(ctx);
                bus.post(new ModuleEvent.Reloaded(entry.id(), module));
            } catch (Throwable t) {
                log.error("Module '{}' threw during reload", entry.id().path(), t);
            }
        }
        bus.post(new KernelEvent.Reloaded(appId));
    }

    public void unload() {
        for (int i = order.size() - 1; i >= 0; i--) {
            deactivate(order.get(i), null);
        }
        configs.closeAll();
        toggles.close();
        services.clear();
        order.clear();
        byId.clear();
        loaded = false;
        bus.post(new KernelEvent.Unloaded(appId));
    }

    @Override
    public void close() {
        if (loaded) {
            unload();
        }
    }

    private void deactivate(ModuleEntry<?> entry, @Nullable Failure failure) {
        Module module = entry.instance();
        if (module != null) {
            ModuleContext ctx = contextOf(entry);
            if (entry.isEnabled()) {
                try {
                    module.onDisable(ctx);
                } catch (Throwable t) {
                    log.error("Module '{}' threw during disable", entry.id().path(), t);
                }
                for (int i = entry.cleanups().size() - 1; i >= 0; i--) {
                    try {
                        entry.cleanups().get(i).run();
                    } catch (Throwable t) {
                        log.error("Module '{}' cleanup threw", entry.id().path(), t);
                    }
                }
                bus.post(new ModuleEvent.Disabled(entry.id(), module));
            }
            Subscribers.unsubscribe(bus, entry, module, entry.spec());
            entry.unbind();
            entry.resetDeclaration();
        }
        if (failure != null) {
            entry.fail(failure);
        }
    }

    private void disableCascade(ModuleEntry<?> target) {
        String targetId = target.id().path();
        for (int i = order.size() - 1; i >= 0; i--) {
            ModuleEntry<?> e = order.get(i);
            if (e.isEnabled() && dependsTransitivelyOn(e, targetId)) {
                deactivate(e, Failure.of(Failure.Kind.DEPENDENCY, targetId));
            }
        }
        deactivate(target, Failure.of(Failure.Kind.TOGGLE, "disabled via setEnabled"));
    }

    private void recoverDependents() {
        for (ModuleEntry<?> e : order) {
            Failure failure = e.failure();
            if (!e.isEnabled() && failure != null && failure.kind() == Failure.Kind.DEPENDENCY) {
                UnitPipeline.activate(e, byId, toggles, this, log);
            }
        }
    }

    private boolean dependsTransitivelyOn(ModuleEntry<?> entry, String targetId) {
        for (String dep : entry.spec().dependsOn()) {
            if (dep.equals(targetId)) {
                return true;
            }
            ModuleEntry<?> parent = byId.get(dep);
            if (parent != null && dependsTransitivelyOn(parent, targetId)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, List<String>> edges(boolean strict) {
        Map<String, List<String>> edges = new Object2ObjectOpenHashMap<>();
        for (ModuleEntry<?> entry : byId.values()) {
            ModuleSpec spec = entry.spec();
            edges.put(entry.id().path(), new ObjectArrayList<>(strict ? spec.dependsOn() : spec.after()));
        }
        if (!strict) {
            for (ModuleEntry<?> entry : byId.values()) {
                String id = entry.id().path();
                for (String target : entry.spec().before()) {
                    List<String> deps = edges.get(target);
                    if (deps != null) {
                        deps.add(id);
                    }
                }
            }
        }
        return edges;
    }

    private static ModuleContext contextOf(ModuleEntry<?> entry) {
        return entry.context()
                .orElseThrow(
                        () -> new IllegalStateException("Module '" + entry.id() + "' is bound but has no context"));
    }
}
