package team.terrafirmagreg.modularity.kernel;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class DependencyGraph {

    public static List<String> sort(
            List<String> ids, Map<String, List<String>> dependsOn, Map<String, List<String>> after) {
        Map<String, List<String>> edges = new Object2ObjectLinkedOpenHashMap<>(ids.size());
        Object2IntOpenHashMap<String> indegree = new Object2IntOpenHashMap<>(ids.size());
        for (String id : ids) {
            edges.put(id, new ObjectArrayList<>());
            indegree.put(id, 0);
        }

        addEdges(edges, indegree, dependsOn, true);
        addEdges(edges, indegree, after, false);

        Object2IntOpenHashMap<String> order = new Object2IntOpenHashMap<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            order.put(ids.get(i), i);
        }

        List<String> result = new ObjectArrayList<>(ids.size());
        var ready = new ObjectHeapPriorityQueue<String>(Comparator.comparingInt(order::getInt));
        for (String id : ids) {
            if (indegree.getInt(id) == 0) {
                ready.enqueue(id);
            }
        }

        while (!ready.isEmpty()) {
            String next = ready.dequeue();
            result.add(next);
            for (String consumer : edges.get(next)) {
                if (indegree.addTo(consumer, -1) == 1) {
                    ready.enqueue(consumer);
                }
            }
        }

        if (result.size() != ids.size()) {
            List<String> cycle = new ObjectArrayList<>();
            for (String id : ids) {
                if (indegree.getInt(id) > 0) {
                    cycle.add(id);
                }
            }
            throw new IllegalStateException("Dependency cycle between units: " + String.join(" -> ", cycle));
        }
        return result;
    }

    private static void addEdges(
            Map<String, List<String>> edges,
            Object2IntOpenHashMap<String> indegree,
            Map<String, List<String>> relation,
            boolean strict) {
        for (Map.Entry<String, List<String>> e : relation.entrySet()) {
            String consumer = e.getKey();
            if (!edges.containsKey(consumer)) {
                continue;
            }
            for (String dep : e.getValue()) {
                if (!edges.containsKey(dep)) {
                    if (strict) {
                        throw new IllegalStateException(
                                "Module '" + consumer + "' depends on unknown module '" + dep + "'");
                    }
                    continue;
                }
                if (dep.equals(consumer)) {
                    continue;
                }
                if (!edges.get(dep).contains(consumer)) {
                    edges.get(dep).add(consumer);
                    indegree.addTo(consumer, 1);
                }
            }
        }
    }
}
