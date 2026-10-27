package team.terrafirmagreg.modularity.entry;

public abstract class Unit<C> {
    private C context;

    public final void attach(C context) {
        this.context = context;
    }

    public C ctx() {
        return context;
    }
}
