package team.terrafirmagreg.modularity.entry;

import java.util.function.Consumer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import team.terrafirmagreg.modularity.Identifier;

public final class ListenerEntry<T extends Event> extends BaseEntry<Consumer<T>> {
    private final Class<T> type;
    private final EventPriority priority;
    private final boolean receiveCanceled;

    public ListenerEntry(Identifier id, Class<T> type, EventPriority priority, boolean receiveCanceled) {
        super(id);
        this.type = type;
        this.priority = priority;
        this.receiveCanceled = receiveCanceled;
    }

    public void declare(Consumer<T> listener) {
        if (instance() == null) {
            bind(listener);
        }
    }

    public void listen(IEventBus bus) {
        if (isPresent()) {
            bus.addListener(priority, receiveCanceled, type, require());
        }
    }

    public void unlisten(IEventBus bus) {
        if (isPresent()) {
            bus.unregister(require());
        }
    }
}
