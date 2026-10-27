package team.terrafirmagreg.modularity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import lombok.val;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.junit.jupiter.api.Test;
import team.terrafirmagreg.modularity.event.EventBus;

class EventBusTest {

    static class Ping extends Event {
        final String text;

        Ping(String text) {
            this.text = text;
        }
    }

    static class Pong extends Ping {
        Pong(String text) {
            super(text);
        }
    }

    static class Flag extends Event implements ICancellableEvent {}

    static class Collector {
        final List<Object> received = new ArrayList<>();

        void onPing(Ping p) {
            received.add(p);
        }
    }

    @Test
    void dispatchByTypeAndSupertype() {
        val bus = new EventBus();
        val got = new ArrayList<Object>();
        bus.addListener(Ping.class, got::add);

        bus.post(new Ping("a"));
        bus.post(new Pong("b"));

        assertEquals(2, got.size());
        assertTrue(got.get(1) instanceof Pong);
    }

    @Test
    void inferredTypeViaMethodRef() {
        val bus = new EventBus();
        val collector = new Collector();
        bus.addListener(collector::onPing);

        bus.post(new Ping("x"));
        assertEquals(1, collector.received.size());
    }

    @Test
    void priorityOrdersHandlers() {
        val bus = new EventBus();
        val order = new ArrayList<String>();
        bus.addListener(EventPriority.LOW, false, Ping.class, p -> order.add("late"));
        bus.addListener(EventPriority.HIGH, false, Ping.class, p -> order.add("early"));
        bus.post(new Ping(""));
        assertEquals(List.of("early", "late"), order);
    }

    @Test
    void cancelledSkipsNormalHandlers() {
        val bus = new EventBus();
        val got = new ArrayList<String>();
        bus.addListener(EventPriority.NORMAL, false, Flag.class, f -> got.add("normal"));
        bus.addListener(EventPriority.NORMAL, true, Flag.class, f -> got.add("canceled"));

        val flag = new Flag();
        flag.setCanceled(true);
        bus.post(flag);
        assertEquals(List.of("canceled"), got);
    }

    @Test
    void subscribeEventObjectPath() {
        val bus = new EventBus();
        val sub = new Object() {
            final List<Ping> received = new ArrayList<>();

            @SubscribeEvent
            void onPing(Ping p) {
                received.add(p);
            }
        };
        bus.subscribe(sub);
        bus.subscribe(sub);

        bus.post(new Ping("s"));
        assertEquals(1, sub.received.size());

        bus.unsubscribe(sub);
        bus.post(new Ping("t"));
        assertEquals(1, sub.received.size());
    }

    @Test
    void subscriptionHandleUnsubscribes() {
        val bus = new EventBus();
        val got = new ArrayList<Ping>();
        val sub = bus.addListener(Ping.class, got::add);

        bus.post(new Ping("1"));
        sub.unsubscribe();
        bus.post(new Ping("2"));
        assertEquals(1, got.size());
    }

    @Test
    void throwingHandlerPropagates_neoforgeSemantics() {
        val bus = new EventBus();
        bus.addListener(Ping.class, p -> {
            throw new RuntimeException("boom");
        });
        assertThrows(RuntimeException.class, () -> bus.post(new Ping("ok")));
    }
}
