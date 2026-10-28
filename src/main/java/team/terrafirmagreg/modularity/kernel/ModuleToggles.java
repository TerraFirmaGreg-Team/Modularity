package team.terrafirmagreg.modularity.kernel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import team.terrafirmagreg.konf.ConfigFile;
import team.terrafirmagreg.konf.Configs;
import team.terrafirmagreg.konf.InvalidConfigException;
import team.terrafirmagreg.konf.format.ConfigFormat;
import team.terrafirmagreg.modularity.module.ModuleEntry;
import team.terrafirmagreg.modularity.module.ModuleSpec;

@Slf4j
final class ModuleToggles {

    private final Path file;
    private final boolean memory;
    private @Nullable ConfigFile config;

    ModuleToggles(Path file, boolean memory) {
        this.file = file;
        this.memory = memory;
    }

    void open() {
        if (memory) {
            config = Configs.memory(ConfigFormat.TOML);
            return;
        }
        try {
            Files.createDirectories(file.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create config directory for " + file, e);
        }
        config = Configs.toml(file);
    }

    boolean enabled(ModuleEntry<?> entry) {
        if (config == null) {
            return true;
        }
        return config.value(entry.id().path() + ".enabled", true)
                .comment(commentOf(entry.spec()))
                .get();
    }

    void reload() {
        if (config != null) {
            config.reload();
        }
    }

    void set(String modulePath, boolean enabled) {
        if (config != null) {
            config.value(modulePath + ".enabled", true).set(enabled);
        }
    }

    void sync() {
        if (config != null) {
            config.sync();
            try {
                config.verify();
            } catch (InvalidConfigException e) {
                log.warn("Unknown keys in {}: {}", file, e.getMessage());
            }
        }
    }

    void close() {
        if (config != null) {
            config.close();
            config = null;
        }
    }

    private static String commentOf(ModuleSpec spec) {
        if (spec.category().isEmpty()) {
            return spec.description();
        }
        return spec.description().isEmpty() ? spec.category() : spec.category() + " — " + spec.description();
    }
}
