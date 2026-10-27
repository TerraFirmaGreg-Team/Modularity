package team.terrafirmagreg.modularity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.val;
import net.neoforged.bus.api.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import team.terrafirmagreg.modularity.kernel.Kernel;
import team.terrafirmagreg.modularity.kernel.KernelEvent;
import team.terrafirmagreg.modularity.kernel.ModuleEvent;
import team.terrafirmagreg.modularity.module.Failure;
import team.terrafirmagreg.modularity.module.Module;
import team.terrafirmagreg.modularity.module.ModuleContext;
import team.terrafirmagreg.modularity.module.ModuleRegistrate;

class KernelTest {

    @TempDir
    Path dir;

    static final List<String> calls = new ArrayList<>();

    static class Alpha extends Module {
        @Override
        public void declare(ModuleContext ctx) {
            calls.add("alpha.declare");
        }

        @Override
        public void onEnable(ModuleContext ctx) {
            calls.add("alpha.enable");
        }

        @Override
        public void onReload(ModuleContext ctx) {
            calls.add("alpha.reload");
        }

        @Override
        public void onDisable(ModuleContext ctx) {
            calls.add("alpha.disable");
        }
    }

    static class Beta extends Module {
        @Override
        public void onEnable(ModuleContext ctx) {
            calls.add("beta.enable");
        }
    }

    static class Boom extends Module {
        @Override
        public void onEnable(ModuleContext ctx) {
            throw new RuntimeException("boom");
        }
    }

    static class Gated extends Module {}

    static class Msg extends Event {
        final String text;

        Msg(String text) {
            this.text = text;
        }
    }

    static class Subbed extends Module {
        final List<Object> got = new ArrayList<>();

        @Override
        public void declare(ModuleContext ctx) {
            ctx.listen(Msg.class, got::add);
        }
    }

    static class Provider extends Module {
        @Override
        public void onEnable(ModuleContext ctx) {
            ctx.provide(String.class, "hello-service");
        }
    }

    static class SecondProvider extends Module {
        @Override
        public void onEnable(ModuleContext ctx) {
            ctx.provide(String.class, "other");
        }
    }

    static class SpecListener extends Module {}

    private Kernel kernel(ModuleRegistrate reg) {
        return Kernel.builder("testapp").configDir(dir).registrate(reg).build();
    }

    @Test
    void activationFollowsDependencyOrder() {
        calls.clear();
        val reg = ModuleRegistrate.create("testapp-order");
        val alpha = reg.entry("alpha", Alpha::new).register();
        reg.entry("beta", Beta::new).dependsOn(alpha).register();

        val kernel =
                Kernel.builder("testapp-order").configDir(dir).registrate(reg).build();
        kernel.load();

        assertEquals(List.of("alpha.declare", "alpha.enable", "beta.enable"), calls);
        assertTrue(kernel.isModuleEnabled("alpha"));
        assertTrue(kernel.isModuleEnabled("beta"));
    }

    @Test
    void togglesMaterializeInToml() throws Exception {
        calls.clear();
        val reg = ModuleRegistrate.create("testapp-toml");
        reg.entry("alpha", Alpha::new).category("base").description("Core bits").register();
        reg.entry("beta", Beta::new).register();

        kernel(reg).load();

        val text = Files.readString(dir.resolve("modules.toml"));
        assertTrue(text.contains("enabled = true"), text);
        assertTrue(text.contains("base — Core bits"), text);

        val reg2 = ModuleRegistrate.create("testapp-toml2");
        reg2.entry("alpha", Alpha::new).register();
        reg2.entry("beta", Beta::new).register();
        Files.writeString(dir.resolve("modules.toml"), """
                [alpha]
                enabled = true
                [beta]
                enabled = false
                """);
        calls.clear();
        kernel(reg2).load();
        assertEquals(List.of("alpha.declare", "alpha.enable"), calls);
    }

    @Test
    void dependencyCascadeDisablesDependents() throws Exception {
        calls.clear();
        val reg = ModuleRegistrate.create("testapp-cascade");
        reg.entry("alpha", Alpha::new).register();
        reg.entry("beta", Beta::new).dependsOn("alpha").register();

        Files.writeString(dir.resolve("modules.toml"), "[alpha]\nenabled = false\n");

        val kernel = kernel(reg);
        kernel.load();
        assertFalse(kernel.isModuleEnabled("alpha"));
        assertFalse(kernel.isModuleEnabled("beta"));
        assertTrue(calls.isEmpty());
    }

    @Test
    void gateRejectsModule() {
        val reg = ModuleRegistrate.create("testapp-gate");
        reg.entry("gated", Gated::new).requires(() -> false).register();
        val kernel = kernel(reg);
        kernel.load();
        assertFalse(kernel.isModuleEnabled("gated"));
    }

    @Test
    void onEnableThrowRollsBack() {
        val reg = ModuleRegistrate.create("testapp-boom");
        val boom = reg.entry("boom", Boom::new).register();
        val kernel = kernel(reg);
        kernel.load();
        assertFalse(kernel.isModuleEnabled("boom"));
        assertTrue(boom.get().isEmpty());
        assertThrows(IllegalStateException.class, boom::require);
    }

    @Test
    void subscribersReceivePostedEvents() {
        val reg = ModuleRegistrate.create("testapp-bus");
        reg.entry("sub", Subbed::new).register();
        val kernel = kernel(reg);
        kernel.load();

        kernel.post(new Msg("hello"));
        val sub = kernel.module(Subbed.class).orElseThrow();
        assertEquals(1, sub.got.size());
        assertEquals("hello", ((Msg) sub.got.get(0)).text);
    }

    @Test
    void reloadAndUnloadOrderAndEvents() {
        calls.clear();
        val reg = ModuleRegistrate.create("testapp-life");
        val alpha = reg.entry("alpha", Alpha::new).register();
        reg.entry("beta", Beta::new).dependsOn(alpha).register();
        val kernel = kernel(reg);
        kernel.load();

        val events = new ArrayList<KernelEvent>();
        kernel.bus().addListener(KernelEvent.class, events::add);

        kernel.reload();
        assertTrue(calls.contains("alpha.reload"));

        calls.clear();
        kernel.unload();
        assertEquals(List.of("alpha.disable"), calls);
        assertTrue(events.stream().anyMatch(e -> e instanceof KernelEvent.Reloaded));
        assertTrue(events.stream().anyMatch(e -> e instanceof KernelEvent.Unloaded));
    }

    @Test
    void loadAfterUnloadRestarts() {
        calls.clear();
        val reg = ModuleRegistrate.create("testapp-restart");
        reg.entry("alpha", Alpha::new).register();
        val kernel = kernel(reg);
        kernel.load();
        kernel.unload();
        calls.clear();

        kernel.load();
        assertEquals(List.of("alpha.declare", "alpha.enable"), calls);
        assertTrue(kernel.isModuleEnabled("alpha"));
    }

    @Test
    void reportRecordsFailureKinds() throws Exception {
        calls.clear();
        Files.writeString(dir.resolve("modules.toml"), "off.enabled = false\n");
        val reg = ModuleRegistrate.create("testapp-status");
        reg.entry("boom", Boom::new).register();
        reg.entry("needy", Alpha::new).dependsOn("boom").register();
        reg.entry("off", Alpha::new).register();
        val kernel = kernel(reg);
        kernel.load();

        assertEquals(
                Failure.Kind.ENABLE,
                kernel.status("boom").orElseThrow().failure().kind());
        assertEquals(
                Failure.Kind.DEPENDENCY,
                kernel.status("needy").orElseThrow().failure().kind());
        assertEquals(
                Failure.Kind.TOGGLE,
                kernel.status("off").orElseThrow().failure().kind());
    }

    @Test
    void moduleEventsOnLoadAndUnload() {
        calls.clear();
        val reg = ModuleRegistrate.create("testapp-mev");
        reg.entry("alpha", Alpha::new).register();
        val kernel = kernel(reg);

        val enabled = new ArrayList<ModuleEvent.Enabled>();
        kernel.bus().addListener(ModuleEvent.Enabled.class, enabled::add);
        kernel.load();
        assertEquals("alpha", enabled.get(0).id().path());

        val disabled = new ArrayList<ModuleEvent.Disabled>();
        kernel.bus().addListener(ModuleEvent.Disabled.class, disabled::add);
        kernel.unload();
        assertEquals("alpha", disabled.get(0).id().path());
    }

    @Test
    void servicesProvideRequireAndOwnership() {
        val reg = ModuleRegistrate.create("testapp-svc");
        reg.entry("provider", Provider::new).register();
        reg.entry("other", Alpha::new).register();
        val kernel = kernel(reg);
        kernel.load();

        assertEquals("hello-service", kernel.requireService(String.class));
        assertTrue(kernel.entry("provider").service(String.class).isPresent());
        assertTrue(kernel.entry("other").service(String.class).isEmpty());

        kernel.unload();
        assertTrue(kernel.service(String.class).isEmpty());
    }

    @Test
    void duplicateServiceThrowsAndGates() {
        val reg = ModuleRegistrate.create("testapp-dupsvc");
        reg.entry("p1", Provider::new).register();
        reg.entry("p2", SecondProvider::new).after("p1").register();
        val kernel = kernel(reg);
        kernel.load();

        assertFalse(kernel.isModuleEnabled("p2"));
        assertEquals(
                Failure.Kind.ENABLE, kernel.status("p2").orElseThrow().failure().kind());
        assertEquals("hello-service", kernel.requireService(String.class));
    }

    @Test
    void alwaysOnIgnoresToggleAndWritesNoKey() throws Exception {
        calls.clear();
        val reg = ModuleRegistrate.create("testapp-alwayson");
        reg.entry("core", Alpha::new).alwaysOn().register();
        reg.entry("beta", Beta::new).register();

        Files.writeString(dir.resolve("modules.toml"), """
                [core]
                enabled = false
                [beta]
                enabled = false
                """);
        val kernel = kernel(reg);
        kernel.load();
        assertTrue(kernel.isModuleEnabled("core"));
        assertFalse(kernel.isModuleEnabled("beta"));

        val reg2 = ModuleRegistrate.create("testapp-alwayson2");
        reg2.entry("core", Alpha::new).alwaysOn().register();
        reg2.entry("beta", Beta::new).register();
        Files.delete(dir.resolve("modules.toml"));
        val kernel2 = kernel(reg2);
        kernel2.load();
        assertTrue(kernel2.isModuleEnabled("core"));
        val text = Files.readString(dir.resolve("modules.toml"));
        assertFalse(text.contains("[core]"), text);
        assertTrue(text.contains("[beta]"), text);
    }

    @Test
    void specListenerReceivesAndDrops() {
        val got = new ArrayList<Msg>();
        val reg = ModuleRegistrate.create("testapp-speclis");
        reg.entry("mod", SpecListener::new).listener(Msg.class, got::add).register();
        val kernel = kernel(reg);
        kernel.load();
        kernel.post(new Msg("a"));
        kernel.unload();
        kernel.post(new Msg("b"));
        assertEquals(1, got.size());
    }

    @Test
    void invalidAppIdRejected() {
        assertThrows(IllegalArgumentException.class, () -> Kernel.builder("Bad App!"));
    }
}
