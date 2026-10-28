package team.terrafirmagreg.modularity.module;

import org.jspecify.annotations.Nullable;
import team.terrafirmagreg.modularity.Identifier;

public record ModuleStatus(
        Identifier id, boolean enabled, @Nullable Failure failure, Timings timings) {}
