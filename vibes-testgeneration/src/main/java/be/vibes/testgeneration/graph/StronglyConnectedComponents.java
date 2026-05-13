package be.vibes.testgeneration.graph;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Computes the strongly connected components (SCCs) of a
 * {@link TransitionSystem} using Tarjan's algorithm.
 *
 * <p>The implementation operates on {@link State} identity directly and
 * therefore works on both plain {@link TransitionSystem}s and any subtype
 * (notably {@code FeaturedTransitionSystem}).
 *
 * <p>The recursion depth is bounded by the number of states, which is small
 * for our SPLs (≤ 275 for HockertyShirts). A recursive implementation is
 * therefore sufficient and keeps the code close to the textbook formulation.
 *
 * <p>The result is returned in reverse topological order of the
 * SCC-condensation DAG, i.e. an SCC always appears after every SCC that can
 * be reached from it. This is a side effect of Tarjan's bottom-up emission
 * order and is convenient for downstream consumers that want to traverse
 * SCCs in a "leaf first" order.
 */
public final class StronglyConnectedComponents {

    private static final Logger LOG = LoggerFactory.getLogger(StronglyConnectedComponents.class);

    private StronglyConnectedComponents() {
        // Utility class.
    }

    /**
     * Returns the SCCs of the given transition system. Every state of the
     * transition system appears in exactly one SCC.
     *
     * @param ts the transition system to analyse
     * @return a list of SCCs; each SCC is the set of states it contains
     */
    public static List<Set<State>> compute(TransitionSystem ts) {
        checkNotNull(ts, "TransitionSystem may not be null");
        Tarjan t = new Tarjan(ts);
        Iterator<State> it = ts.states();
        while (it.hasNext()) {
            State s = it.next();
            if (!t.index.containsKey(s)) {
                t.strongconnect(s);
            }
        }
        return Collections.unmodifiableList(t.result);
    }

    /**
     * Returns the SCC that contains the given state, or {@code null} if the
     * state does not belong to the transition system. This is a convenience
     * around {@link #compute(TransitionSystem)} for callers that only care
     * about one SCC (typically the one containing the initial state).
     */
    public static Set<State> containing(TransitionSystem ts, State state) {
        checkNotNull(state, "State may not be null");
        for (Set<State> scc : compute(ts)) {
            if (scc.contains(state)) {
                return scc;
            }
        }
        return null;
    }

    /**
     * Convenience: is the transition system strongly connected? I.e. does it
     * have exactly one SCC that covers every state?
     */
    public static boolean isStronglyConnected(TransitionSystem ts) {
        List<Set<State>> sccs = compute(ts);
        if (sccs.size() != 1) {
            return false;
        }
        int stateCount = 0;
        Iterator<State> it = ts.states();
        while (it.hasNext()) {
            it.next();
            stateCount++;
        }
        return sccs.get(0).size() == stateCount;
    }

    /** Inner type that holds the mutable state of one Tarjan invocation. */
    private static final class Tarjan {
        final TransitionSystem ts;
        final Map<State, Integer> index = new HashMap<>();
        final Map<State, Integer> lowlink = new HashMap<>();
        final Set<State> onStack = new HashSet<>();
        final Deque<State> stack = new ArrayDeque<>();
        final List<Set<State>> result = new ArrayList<>();
        int nextIndex = 0;

        Tarjan(TransitionSystem ts) {
            this.ts = ts;
        }

        void strongconnect(State v) {
            index.put(v, nextIndex);
            lowlink.put(v, nextIndex);
            nextIndex++;
            stack.push(v);
            onStack.add(v);

            Iterator<Transition> outgoing = ts.getOutgoing(v);
            while (outgoing.hasNext()) {
                State w = outgoing.next().getTarget();
                if (!index.containsKey(w)) {
                    strongconnect(w);
                    lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
                } else if (onStack.contains(w)) {
                    lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
                }
            }

            if (lowlink.get(v).equals(index.get(v))) {
                // v is an SCC root: pop everything down to and including v.
                Set<State> scc = new LinkedHashSet<>();
                State w;
                do {
                    w = stack.pop();
                    onStack.remove(w);
                    scc.add(w);
                } while (!w.equals(v));
                result.add(scc);
                LOG.debug("Emitted SCC of size {}", scc.size());
            }
        }
    }
}
