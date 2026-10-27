package team.terrafirmagreg.modularity.module;

import java.util.List;
import java.util.function.BooleanSupplier;

public record ModuleSpec(
        List<String> dependsOn,
        List<String> after,
        List<Object> subscribers,
        List<ListenerDecl<?>> listeners,
        List<BooleanSupplier> gates,
        boolean alwaysOn,
        String category,
        String description) {
    public ModuleSpec {
        dependsOn = List.copyOf(dependsOn);
        after = List.copyOf(after);
        subscribers = List.copyOf(subscribers);
        listeners = List.copyOf(listeners);
        gates = List.copyOf(gates);
        if (category == null) {
            category = "";
        }
        if (description == null) {
            description = "";
        }
    }
}
