package team.terrafirmagreg.modularity.module;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import team.terrafirmagreg.modularity.Identifier;

public final class ModuleBuilder<T extends Module> {
    private final ModuleRegistrate owner;
    private final Identifier id;
    private final Supplier<T> factory;

    private final List<String> dependsOn = new ObjectArrayList<>();
    private final List<String> after = new ObjectArrayList<>();
    private final List<String> before = new ObjectArrayList<>();
    private final List<Object> subscribers = new ObjectArrayList<>();
    private final List<ListenerDecl<?>> listeners = new ObjectArrayList<>();
    private final List<BooleanSupplier> gates = new ObjectArrayList<>();
    private boolean alwaysOn;
    private String category = "";
    private String description = "";

    ModuleBuilder(ModuleRegistrate owner, Identifier id, Supplier<T> factory) {
        this.owner = owner;
        this.id = id;
        this.factory = factory;
    }

    public ModuleBuilder<T> dependsOn(ModuleEntry<?>... dependencies) {
        for (ModuleEntry<?> dep : dependencies) {
            dependsOn.add(dep.id().path());
        }
        return this;
    }

    public ModuleBuilder<T> dependsOn(String... ids) {
        dependsOn.addAll(List.of(ids));
        return this;
    }

    public ModuleBuilder<T> after(ModuleEntry<?>... entries) {
        for (ModuleEntry<?> entry : entries) {
            after.add(entry.id().path());
        }
        return this;
    }

    public ModuleBuilder<T> after(String... ids) {
        after.addAll(List.of(ids));
        return this;
    }

    public ModuleBuilder<T> before(ModuleEntry<?>... entries) {
        for (ModuleEntry<?> entry : entries) {
            before.add(entry.id().path());
        }
        return this;
    }

    public ModuleBuilder<T> before(String... ids) {
        before.addAll(List.of(ids));
        return this;
    }

    public ModuleBuilder<T> category(String category) {
        this.category = category;
        return this;
    }

    public ModuleBuilder<T> description(String description) {
        this.description = description;
        return this;
    }

    public ModuleBuilder<T> subscriber(Object subscriber) {
        this.subscribers.add(subscriber);
        return this;
    }

    public <E extends Event> ModuleBuilder<T> listener(Class<E> type, Consumer<E> consumer) {
        listeners.add(new ListenerDecl<>(type, consumer, EventPriority.NORMAL, false));
        return this;
    }

    public <E extends Event> ModuleBuilder<T> listener(
            EventPriority priority, boolean receiveCanceled, Class<E> type, Consumer<E> consumer) {
        listeners.add(new ListenerDecl<>(type, consumer, priority, receiveCanceled));
        return this;
    }

    public ModuleBuilder<T> requires(BooleanSupplier condition) {
        gates.add(condition);
        return this;
    }

    public ModuleBuilder<T> alwaysOn() {
        this.alwaysOn = true;
        return this;
    }

    public ModuleEntry<T> register() {
        ModuleSpec spec = new ModuleSpec(
                dependsOn, after, before, subscribers, listeners, gates, alwaysOn, category, description);
        return owner.add(new ModuleEntry<>(id, spec, factory));
    }
}
