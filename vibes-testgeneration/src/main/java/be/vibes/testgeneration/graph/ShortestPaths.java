package be.vibes.testgeneration.graph;

import be.vibes.ts.State;
import be.vibes.ts.Transition;
import be.vibes.ts.TransitionSystem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * BFS-based shortest-path queries on a {@link TransitionSystem}. All
 * transitions are treated as unit-weight edges, so BFS yields true
 * shortest paths in transition count (equivalent to Dijkstra on a
 * uniformly-weighted graph but without the priority-queue overhead).
 *
 * <p>The primary entry point is
 * {@link #shortestPathToAny(TransitionSystem, State, Set)}, which finds
 * the closest target in a set rather than a single named state. This is
 * what state-coverage generation needs: at each "stuck" point, reroute
 * to the nearest state that has not yet been visited.
 */
public final class ShortestPaths {

    private ShortestPaths() {
        // Utility class.
    }

    /**
     * Returns the shortest path (as an ordered list of transitions) from
     * {@code source} to any state in {@code targets}, or {@code null} if no
     * target is reachable from {@code source}.
     *
     * <p>The returned list is empty iff {@code source} is itself in
     * {@code targets} (zero-length path).
     */
    public static List<Transition> shortestPathToAny(TransitionSystem ts,
                                                     State source,
                                                     Set<State> targets) {
        checkNotNull(ts, "TransitionSystem may not be null");
        checkNotNull(source, "Source state may not be null");
        checkNotNull(targets, "Target set may not be null");
        if (targets.isEmpty()) {
            return null;
        }
        if (targets.contains(source)) {
            return Collections.emptyList();
        }

        // BFS with predecessor tracking. The predecessor map remembers the
        // transition we used to first arrive at each visited state.
        Map<State, Transition> arrival = new HashMap<>();
        Set<State> visited = new HashSet<>();
        Deque<State> queue = new ArrayDeque<>();
        queue.offer(source);
        visited.add(source);

        State found = null;
        while (!queue.isEmpty() && found == null) {
            State current = queue.poll();
            Iterator<Transition> outs = ts.getOutgoing(current);
            while (outs.hasNext()) {
                Transition t = outs.next();
                State next = t.getTarget();
                if (visited.contains(next)) {
                    continue;
                }
                visited.add(next);
                arrival.put(next, t);
                if (targets.contains(next)) {
                    found = next;
                    break;
                }
                queue.offer(next);
            }
        }

        if (found == null) {
            return null;
        }

        // Reconstruct path by walking backwards through arrival.
        List<Transition> path = new ArrayList<>();
        State cursor = found;
        while (!cursor.equals(source)) {
            Transition t = arrival.get(cursor);
            path.add(t);
            cursor = t.getSource();
        }
        Collections.reverse(path);
        return path;
    }
}
