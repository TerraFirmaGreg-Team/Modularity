package team.terrafirmagreg.modularity.event;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EventBus {

    private final IEventBus bus;
    private final Set<Object> subscribers = ConcurrentHashMap.newKeySet();
    private final Map<ScanKey, Boolean> hasHandlers = new ConcurrentHashMap<>();
    private final Logger log;

    public EventBus() {
        this(LoggerFactory.getLogger(EventBus.class));
    }

    public EventBus(Logger log) {
        this.log = log;
        this.bus = BusBuilder.builder()
                .setExceptionHandler((b, event, listeners, i, t) -> this.log.error(
                        "Event listener threw on {}", event.getClass().getName(), t))
                .build();
        this.bus.start();
    }

    public void subscribe(Object subscriber) {
        boolean statics = subscriber instanceof Class<?>;
        Class<?> scanned = statics ? (Class<?>) subscriber : subscriber.getClass();
        if (!hasSubscribeMethods(scanned, statics)) {
            return;
        }
        if (subscribers.add(subscriber)) {
            bus.register(subscriber);
        }
    }

    public void unsubscribe(Object subscriber) {
        if (subscribers.remove(subscriber)) {
            bus.unregister(subscriber);
        }
    }

    public <T extends Event> Subscription addListener(Consumer<T> listener) {
        bus.addListener(listener);
        return () -> bus.unregister(listener);
    }

    public <T extends Event> Subscription addListener(Class<T> type, Consumer<T> listener) {
        bus.addListener(type, listener);
        return () -> bus.unregister(listener);
    }

    public <T extends Event> Subscription addListener(
            EventPriority priority, boolean receiveCanceled, Class<T> type, Consumer<T> listener) {
        bus.addListener(priority, receiveCanceled, type, listener);
        return () -> bus.unregister(listener);
    }

    public IEventBus raw() {
        return bus;
    }

    public <T extends Event> T post(T event) {
        return bus.post(event);
    }

    private record ScanKey(Class<?> type, boolean statics) {}

    private boolean hasSubscribeMethods(Class<?> type, boolean statics) {
        return hasHandlers.computeIfAbsent(new ScanKey(type, statics), k -> {
            for (Class<?> c = k.type(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(SubscribeEvent.class)
                            && !Modifier.isAbstract(m.getModifiers())
                            && Modifier.isStatic(m.getModifiers()) == k.statics()) {
                        return true;
                    }
                }
            }
            return false;
        });
    }
}
