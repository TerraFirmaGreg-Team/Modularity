package team.terrafirmagreg.modularity.kernel;

import java.util.Map;
import java.util.function.BooleanSupplier;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.terrafirmagreg.modularity.module.Failure;
import team.terrafirmagreg.modularity.module.Module;
import team.terrafirmagreg.modularity.module.ModuleContext;
import team.terrafirmagreg.modularity.module.ModuleEntry;
import team.terrafirmagreg.modularity.module.ModuleSpec;

@UtilityClass
final class UnitPipeline {

    static <T extends Module> void activate(
            ModuleEntry<T> entry, Map<String, ModuleEntry<?>> byId, ModuleToggles toggles, Kernel kernel, Logger log) {
        String id = entry.id().path();
        entry.clearFailure();
        ModuleSpec spec = entry.spec();
        if (!spec.alwaysOn() && !toggles.enabled(entry)) {
            log.info("Module '{}' disabled by modules.toml", id);
            entry.fail(Failure.of(Failure.Kind.TOGGLE, "disabled in modules.toml"));
            return;
        }
        for (String dep : spec.dependsOn()) {
            ModuleEntry<?> parent = byId.get(dep);
            if (parent == null || !parent.isEnabled()) {
                log.warn("Module '{}' disabled: dependency '{}' is disabled or missing", id, dep);
                entry.fail(Failure.of(Failure.Kind.DEPENDENCY, dep));
                return;
            }
        }
        for (BooleanSupplier gate : spec.gates()) {
            boolean pass;
            try {
                pass = gate.getAsBoolean();
            } catch (Throwable t) {
                pass = false;
            }
            if (!pass) {
                log.info("Module '{}' disabled: gate condition rejected", id);
                entry.fail(Failure.of(Failure.Kind.GATE, "requires() rejected"));
                return;
            }
        }

        T unit;
        ModuleContext ctx;
        try {
            unit = entry.construct();
            ctx = new ModuleContext(entry.id(), LoggerFactory.getLogger(kernel.appId() + "." + id), kernel, entry);
            entry.attach(ctx, unit);
            unit.attach(ctx);
        } catch (Throwable t) {
            entry.fail(Failure.of(Failure.Kind.CONSTRUCT, t));
            log.error("Module '{}' failed to construct — disabling", id, t);
            return;
        }
        try {
            if (entry.claimDeclaration()) {
                unit.declare(ctx);
            }
        } catch (Throwable t) {
            entry.fail(Failure.of(Failure.Kind.DECLARE, t));
            log.error("Module '{}' threw during declaration — disabling", id, t);
            return;
        }
        Subscribers.subscribe(kernel.bus(), entry, unit, spec);
        try {
            unit.onEnable(ctx);
        } catch (Throwable t) {
            Subscribers.unsubscribe(kernel.bus(), entry, unit, spec);
            kernel.dropServices(entry.id());
            entry.fail(Failure.of(Failure.Kind.ENABLE, t));
            log.error("Module '{}' threw during activation — rolled back to disabled", id, t);
            return;
        }
        kernel.post(new ModuleEvent.Enabled(entry.id(), unit));
    }
}
