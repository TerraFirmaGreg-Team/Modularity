package team.terrafirmagreg.modularity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import lombok.val;
import org.junit.jupiter.api.Test;
import team.terrafirmagreg.modularity.kernel.DependencyGraph;

class DependencyGraphTest {

    @Test
    void declarationOrderWithoutRelations() {
        val sorted = DependencyGraph.sort(List.of("b", "a", "c"), Map.of(), Map.of());
        assertEquals(List.of("b", "a", "c"), sorted);
    }

    @Test
    void dependsOnOrdersBeforeDependent() {
        val sorted = DependencyGraph.sort(List.of("a", "b"), Map.of("b", List.of("a")), Map.of());
        assertEquals(List.of("a", "b"), sorted);
    }

    @Test
    void afterIsSoftOrdering() {
        val sorted = DependencyGraph.sort(List.of("a", "b"), Map.of(), Map.of("a", List.of("b")));
        assertEquals(List.of("b", "a"), sorted);
    }

    @Test
    void afterIgnoresUnknownIds() {
        val sorted = DependencyGraph.sort(List.of("a"), Map.of(), Map.of("a", List.of("missing")));
        assertEquals(List.of("a"), sorted);
    }

    @Test
    void dependsOnUnknownIdFails() {
        val e = assertThrows(
                IllegalStateException.class,
                () -> DependencyGraph.sort(List.of("a"), Map.of("a", List.of("missing")), Map.of()));
        assertTrue(e.getMessage().contains("missing"));
    }

    @Test
    void cycleFailsWithMembers() {
        val e = assertThrows(
                IllegalStateException.class,
                () -> DependencyGraph.sort(List.of("a", "b"), Map.of("a", List.of("b"), "b", List.of("a")), Map.of()));
        assertTrue(e.getMessage().contains("a"), e.getMessage());
        assertTrue(e.getMessage().contains("b"), e.getMessage());
    }
}
