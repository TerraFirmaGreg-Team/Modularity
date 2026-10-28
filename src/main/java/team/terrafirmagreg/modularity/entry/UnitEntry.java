package team.terrafirmagreg.modularity.entry;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import team.terrafirmagreg.modularity.Identifier;

public abstract class UnitEntry<T, C> extends BaseEntry<T> {
    private final Supplier<T> factory;
    private final List<SubscriberEntry> subscribers = new ObjectArrayList<>();
    private final List<ListenerEntry<?>> listeners = new ObjectArrayList<>();
    private final List<Runnable> cleanups = new ObjectArrayList<>();
    private boolean componentsDeclared;
    private C context;

    protected UnitEntry(Identifier id, Supplier<T> factory) {
        super(id);
        this.factory = factory;
    }

    public SubscriberEntry subscriber(String path, Supplier<Object> supplier) {
        SubscriberEntry entry = new SubscriberEntry(Identifier.of(id().domain(), id().path() + "/" + path), supplier);
        subscribers.add(entry);
        return entry;
    }

    public <T extends Event> ListenerEntry<T> listener(
            String path, Class<T> type, EventPriority priority, boolean receiveCanceled, Consumer<T> consumer) {
        ListenerEntry<T> entry = new ListenerEntry<>(
                Identifier.of(id().domain(), id().path() + "/" + path), type, priority, receiveCanceled);
        entry.declare(consumer);
        listeners.add(entry);
        return entry;
    }

    public List<SubscriberEntry> subscribers() {
        return subscribers;
    }

    public List<ListenerEntry<?>> listeners() {
        return listeners;
    }

    public void cleanup(Runnable runnable) {
        cleanups.add(runnable);
    }

    public List<Runnable> cleanups() {
        return cleanups;
    }

    public boolean claimDeclaration() {
        if (componentsDeclared) {
            return false;
        }
        componentsDeclared = true;
        return true;
    }

    public void resetDeclaration() {
        componentsDeclared = false;
        subscribers.clear();
        listeners.clear();
        cleanups.clear();
    }

    public Optional<C> context() {
        return Optional.ofNullable(context);
    }

    public T construct() {
        return factory.get();
    }

    public void attach(C ctx, T unit) {
        this.context = ctx;
        bind(unit);
    }

    @Override
    public void unbind() {
        super.unbind();
        this.context = null;
    }
}
