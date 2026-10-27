package team.terrafirmagreg.modularity.entry;

import java.util.function.Supplier;
import team.terrafirmagreg.modularity.Identifier;

public final class SubscriberEntry extends BaseEntry<Object> {
    private final Supplier<Object> supplier;

    public SubscriberEntry(Identifier id, Supplier<Object> supplier) {
        super(id);
        this.supplier = supplier;
    }

    public void resolve() {
        if (instance() == null) {
            Object resolved = supplier.get();
            if (resolved != null) {
                bind(resolved);
            }
        }
    }
}
