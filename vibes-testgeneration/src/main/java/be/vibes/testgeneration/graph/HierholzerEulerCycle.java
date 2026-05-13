package be.vibes.testgeneration.graph;

import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import be.vibes.ts.TransitionSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Extracts an Euler cycle from a balanced + strongly-connected directed
 * graph using Hierholzer's algorithm.
 *
 * <p>The algorithm assumes the graph satisfies the necessary and sufficient
 * conditions for the existence of an Euler cycle in a directed multigraph:
 * (1) every vertex has {@code in_degree == out_degree}, and (2) every vertex
 * with at least one edge belongs to a single strongly connected component.
 * Both preconditions are validated and an {@link IllegalArgumentException}
 * is raised on violation, so callers do not get silently incorrect cycles.
 *
 * <p>Returned cycle is a {@code List<Transition>} in traversal order; the
 * source of the first transition equals the target of the last (a closed
 * cycle starting and ending at the initial state of the FTS).
 *
 * <p>Implementation is the standard iterative formulation (single stack of
 * vertices being explored, one stack of transitions output in reverse).
 * Each transition is consumed at most once, so the algorithm is
 * {@code O(|E|)} in time.
 */
public final class HierholzerEulerCycle {

    private static final Logger LOG = LoggerFactory.getLogger(HierholzerEulerCycle.class);

    private HierholzerEulerCycle() {
        // Utility class.
    }

    /**
     * Computes an Euler cycle on the given balanced + strongly connected FTS,
     * starting and ending at the FTS' initial state.
     *
     * @param fts the balanced FTS to traverse
     * @return the Euler cycle as a list of transitions in traversal order
     * @throws IllegalArgumentException if the precondition (balanced +
     *         strongly connected) is violated
     */
    public static List<Transition> compute(FeaturedTransitionSystem fts) {
        checkNotNull(fts, "FTS may not be null");
        verifyBalanced(fts);
        if (!StronglyConnectedComponents.isStronglyConnected(fts)) {
            throw new IllegalArgumentException(
                    "HierholzerEulerCycle requires a strongly connected input");
        }

        // Build a mutable copy of the outgoing-transition lists: each state
        // points to a deque of remaining outgoing transitions, and we pop
        // from this deque as we consume each edge.
        Map<State, Deque<Transition>> remaining = new HashMap<>();
        Iterator<State> stateIt = fts.states();
        while (stateIt.hasNext()) {
            State s = stateIt.next();
            Deque<Transition> outs = new ArrayDeque<>();
            Iterator<Transition> tIt = fts.getOutgoing(s);
            while (tIt.hasNext()) {
                outs.add(tIt.next());
            }
            remaining.put(s, outs);
        }

        State start = fts.getInitialState();
        Deque<State> vertexStack = new ArrayDeque<>();
        // Transitions output during the inverse Hierholzer build. The cycle
        // is constructed in reverse and reversed at the end.
        Deque<Transition> usedStack = new ArrayDeque<>();
        // Track which transition we used to arrive at each currently-stacked
        // vertex. The initial state has no arrival transition; we use null
        // as the sentinel — hence LinkedList (which permits null elements)
        // rather than ArrayDeque (which does not).
        Deque<Transition> arrivalStack = new LinkedList<>();

        vertexStack.push(start);
        arrivalStack.push(null);

        while (!vertexStack.isEmpty()) {
            State v = vertexStack.peek();
            Deque<Transition> outs = remaining.get(v);
            if (outs != null && !outs.isEmpty()) {
                Transition next = outs.poll();
                vertexStack.push(next.getTarget());
                arrivalStack.push(next);
            } else {
                vertexStack.pop();
                Transition arrival = arrivalStack.pop();
                if (arrival != null) {
                    usedStack.push(arrival);
                }
            }
        }

        // usedStack contains transitions in reverse traversal order.
        List<Transition> cycle = new ArrayList<>(usedStack.size());
        while (!usedStack.isEmpty()) {
            cycle.add(usedStack.pop());
        }
        // usedStack.pop() iterates LIFO; the Deque was a push-on-arrival
        // stack so reversing once via the pop sequence already gives the
        // desired traversal order.
        verifyCycleClosure(cycle, start);

        LOG.info("Generated Euler cycle: {} transitions starting and ending at {}",
                cycle.size(), start.getName());
        return Collections.unmodifiableList(cycle);
    }

    /**
     * Convenience: returns the action-name sequence corresponding to an
     * Euler cycle. Useful for replay via
     * {@code be.vibes.ts.execution.TransitionSystemExecutor.execute(...)}.
     */
    public static List<String> asActionSequence(List<Transition> cycle) {
        List<String> actions = new ArrayList<>(cycle.size());
        for (Transition t : cycle) {
            actions.add(t.getAction().getName());
        }
        return actions;
    }

    private static void verifyBalanced(TransitionSystem ts) {
        Iterator<State> it = ts.states();
        while (it.hasNext()) {
            State s = it.next();
            int out = count(ts.getOutgoing(s));
            int in = count(ts.getIncoming(s));
            checkArgument(in == out,
                    "State %s violates Eulerian balance: in=%s out=%s", s.getName(), in, out);
        }
    }

    private static int count(Iterator<?> it) {
        int n = 0;
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    private static void verifyCycleClosure(List<Transition> cycle, State start) {
        if (cycle.isEmpty()) {
            return;
        }
        State first = cycle.get(0).getSource();
        State last = cycle.get(cycle.size() - 1).getTarget();
        checkArgument(first.equals(start),
                "Euler cycle does not start at expected vertex (start=%s, actual=%s)",
                start.getName(), first.getName());
        checkArgument(last.equals(start),
                "Euler cycle does not return to start (start=%s, last target=%s)",
                start.getName(), last.getName());
        // Consecutive transitions must form a path.
        for (int i = 1; i < cycle.size(); i++) {
            State prevTarget = cycle.get(i - 1).getTarget();
            State currSource = cycle.get(i).getSource();
            checkArgument(prevTarget.equals(currSource),
                    "Euler cycle is not contiguous at position %s: prev target=%s, curr source=%s",
                    i, prevTarget.getName(), currSource.getName());
        }
    }
}
