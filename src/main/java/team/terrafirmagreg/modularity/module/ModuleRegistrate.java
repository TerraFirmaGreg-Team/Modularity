package team.terrafirmagreg.modularity.module;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import team.terrafirmagreg.modularity.Identifier;

@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModuleRegistrate {
    private static final Map<String, ModuleRegistrate> BY_APP = new Object2ObjectOpenHashMap<>();

    @Getter
    private final String appId;

    private final Map<String, ModuleEntry<?>> entries = new Object2ObjectLinkedOpenHashMap<>();
    private boolean frozen;

    public static synchronized ModuleRegistrate create(String appId) {
        return BY_APP.computeIfAbsent(appId, ModuleRegistrate::new);
    }

    @Nullable
    public static synchronized ModuleRegistrate of(String appId) {
        return BY_APP.get(appId);
    }

    public <T extends Module> ModuleBuilder<T> entry(String id, Supplier<T> factory) {
        if (frozen) {
            throw new IllegalStateException("ModuleRegistrate for '" + appId + "' is already frozen");
        }
        if ("modules".equals(id)) {
            throw new IllegalArgumentException("Module id 'modules' is reserved (config directory name)");
        }
        return new ModuleBuilder<>(this, Identifier.of(appId, id), factory);
    }

    <T extends Module> ModuleEntry<T> add(ModuleEntry<T> entry) {
        String key = entry.id().path();
        if (entries.putIfAbsent(key, entry) != null) {
            throw new IllegalStateException("Duplicate module id '" + key + "' for app '" + appId + "'");
        }
        return entry;
    }

    public Collection<ModuleEntry<?>> entries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    @Nullable
    public ModuleEntry<?> entry(String id) {
        return entries.get(id);
    }

    public void freeze() {
        this.frozen = true;
    }
}
