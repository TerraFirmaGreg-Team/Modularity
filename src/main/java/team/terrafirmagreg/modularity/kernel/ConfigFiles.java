package team.terrafirmagreg.modularity.kernel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import team.terrafirmagreg.konf.ConfigFile;
import team.terrafirmagreg.konf.Configs;
import team.terrafirmagreg.modularity.Identifier;

final class ConfigFiles {

    private final Path modulesDir;
    private final Logger log;
    private final Map<Path, ConfigFile> open = new ConcurrentHashMap<>();

    ConfigFiles(Path modulesDir, Logger log) {
        this.modulesDir = modulesDir;
        this.log = log;
    }

    ConfigFile open(Identifier moduleId, @Nullable String name) {
        Path file = name == null
                ? modulesDir.resolve(moduleId.path() + ".toml")
                : modulesDir.resolve(moduleId.path()).resolve(name + ".toml");
        return open.computeIfAbsent(file, f -> {
            try {
                Files.createDirectories(f.getParent());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create config directory for " + f, e);
            }
            return Configs.toml(f);
        });
    }

    void syncAll() {
        for (ConfigFile file : open.values()) {
            file.sync();
        }
    }

    void reloadAll() {
        for (ConfigFile file : open.values()) {
            file.reload();
        }
    }

    void closeAll() {
        for (ConfigFile file : open.values()) {
            try {
                file.close();
            } catch (Throwable t) {
                log.warn("Failed to close config {}", file.path(), t);
            }
        }
        open.clear();
    }
}
