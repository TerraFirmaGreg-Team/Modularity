package team.terrafirmagreg.modularity.kernel;

import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import net.neoforged.bus.api.Event;
import team.terrafirmagreg.modularity.entry.ListenerEntry;
import team.terrafirmagreg.modularity.entry.SubscriberEntry;
import team.terrafirmagreg.modularity.entry.UnitEntry;
import team.terrafirmagreg.modularity.event.EventBus;
import team.terrafirmagreg.modularity.module.ListenerDecl;
import team.terrafirmagreg.modularity.module.ModuleSpec;

@UtilityClass
final class Subscribers {

    static void subscribe(EventBus bus, UnitEntry<?, ?> entry, Object unit, ModuleSpec spec) {
        bus.subscribe(unit);
        for (Object subscriber : spec.subscribers()) {
            bus.subscribe(subscriber);
        }
        for (ListenerDecl<?> decl : spec.listeners()) {
            listen(bus, decl);
        }
        for (SubscriberEntry component : entry.subscribers()) {
            component.resolve();
            if (component.isPresent()) {
                bus.subscribe(component.require());
            }
        }
        for (ListenerEntry<?> listener : entry.listeners()) {
            listener.listen(bus.raw());
        }
    }

    static void unsubscribe(EventBus bus, UnitEntry<?, ?> entry, Object unit, ModuleSpec spec) {
        bus.unsubscribe(unit);
        for (Object subscriber : spec.subscribers()) {
            bus.unsubscribe(subscriber);
        }
        for (ListenerDecl<?> decl : spec.listeners()) {
            bus.raw().unregister(decl.consumer());
        }
        for (SubscriberEntry component : entry.subscribers()) {
            if (component.isPresent()) {
                bus.unsubscribe(component.require());
            }
        }
        for (ListenerEntry<?> listener : entry.listeners()) {
            listener.unlisten(bus.raw());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> void listen(EventBus bus, ListenerDecl<?> decl) {
        bus.raw().addListener(decl.priority(), decl.receiveCanceled(), (Class<T>) decl.type(), (Consumer<T>)
                decl.consumer());
    }
}
