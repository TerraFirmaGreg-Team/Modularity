package team.terrafirmagreg.modularity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import team.terrafirmagreg.modularity.kernel.Kernel;
import team.terrafirmagreg.modularity.module.Module;
import team.terrafirmagreg.modularity.module.ModuleContext;
import team.terrafirmagreg.modularity.module.ModuleRegistrate;

class ModuleConfigTest {

    @TempDir
    Path dir;

    static class Configured extends Module {
        static int retries = -1;

        @Override
        public void declare(ModuleContext ctx) {
            retries = ctx.config()
                    .value("retries", 3)
                    .range(0, 10)
                    .comment("Retry count")
                    .get();
            ctx.config("extra").value("note", "hi").get();
        }
    }

    @Test
    void perModuleConfigFiles() throws Exception {
        val reg = ModuleRegistrate.create("testapp-cfg");
        reg.entry("alpha", Configured::new).register();

        val kernel =
                Kernel.builder("testapp-cfg").configDir(dir).registrate(reg).build();
        kernel.load();

        val file = dir.resolve("modules").resolve("alpha.toml");
        assertTrue(Files.exists(file));
        assertEquals(3, Configured.retries);
        assertTrue(Files.readString(file).contains("retries = 3"));

        val extra = dir.resolve("modules").resolve("alpha").resolve("extra.toml");
        assertTrue(Files.exists(extra));
        assertTrue(Files.readString(extra).contains("note = \"hi\""));

        kernel.unload();
    }
}
