package team.terrafirmagreg.modularity.module;

import team.terrafirmagreg.modularity.entry.Unit;

public abstract class Module extends Unit<ModuleContext> {

    public void declare(ModuleContext ctx) {}

    public void onEnable(ModuleContext ctx) {}

    public void onReload(ModuleContext ctx) {}

    public void onDisable(ModuleContext ctx) {}
}
