package team.terrafirmagreg.modularity.module;

import java.util.function.Consumer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;

public record ListenerDecl<T extends Event>(
        Class<T> type, Consumer<T> consumer, EventPriority priority, boolean receiveCanceled) {}
