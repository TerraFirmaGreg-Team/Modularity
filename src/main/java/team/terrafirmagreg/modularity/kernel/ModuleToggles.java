package team.terrafirmagreg.modularity.kernel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;
import team.terrafirmagreg.konf.ConfigFile;
import team.terrafirmagreg.konf.Configs;
import team.terrafirmagreg.modularity.module.ModuleEntry;
import team.terrafirmagreg.modularity.module.ModuleSpec;

final class ModuleToggles {

    private final Path file;
    private @Nullable ConfigFile config;

    ModuleToggles(Path file) {
        this.file = file;
    }

    void open() {
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

    void sync() {
        if (config != null) {
            config.sync();
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
