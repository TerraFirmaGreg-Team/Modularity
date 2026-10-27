package team.terrafirmagreg.modularity.event;

@FunctionalInterface
public interface Subscription {

    void unsubscribe();
}
