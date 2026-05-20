package be.vibes.testgeneration.graph;

import be.vibes.fexpression.FExpression;
import be.vibes.ts.Action;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.FeaturedTransitionSystemFactory;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Produces a {@link FeaturedTransitionSystem} in which every state has
 * {@code in_degree == out_degree}, i.e. the necessary condition for an
 * Euler cycle in a directed multigraph. The strategy is the directed
 * Chinese Postman approach: instead of inserting fresh synthetic edges
 * between arbitrary imbalanced vertices, the balancer finds shortest
 * paths in the real graph and "doubles" the transitions along those
 * paths. Each doubled transition is materialised as a new FTS transition
 * carrying the original action name plus a {@code __dup__N} suffix so it
 * is a distinct entry from VIBeS' factory's perspective. Downstream
 * coverage code recognises the suffix via {@link #isSyntheticAction} and
 * treats these transitions as synthetic for hit-counting purposes;
 * test-case display strips the suffix so the action sequence reads as a
 * sequence of real actions (with some real actions appearing more than
 * once when a path was doubled).
 *
 * <p>Compared to the previous "direct synthetic edge" strategy this
 * preserves contiguity of the Hierholzer cycle: every traversal is along
 * an edge that exists in the original FTS (possibly visited more than
 * once), so the resulting test case is a single, contiguous, executable
 * action sequence with NO teleporting between unrelated states.
 *
 * <p>The balancer ASSUMES the input is strongly connected. Strong
 * connectivity is the second necessary condition for an Euler cycle, and
 * checking + repairing it is conceptually a separate concern handled by
 * earlier pipeline stages (post-projection SCC repair). The balancer
 * raises {@link IllegalArgumentException} if it discovers the assumption
 * is violated.
 */
public final class EulerianBalancer {

    private static final Logger LOG = LoggerFactory.getLogger(EulerianBalancer.class);

    /**
     * Legacy prefix used when the balancer inserted direct synthetic edges
     * between imbalanced pairs. Kept as a class constant so any older
     * persisted artefact that still contains transitions with this prefix
     * is still recognised by {@link #isSyntheticAction}.
     */
    public static final String SYNTHETIC_ACTION_PREFIX = "__balance__";

    /**
     * Infix marker for transitions added by edge-doubling. Transitions
     * carry an action name like {@code originalAction__dup__N}; everything
     * up to {@code __dup__} is the original action name and is restored
     * when the test case is rendered.
     */
    public static final String DUPLICATE_ACTION_INFIX = "__dup__";

    private EulerianBalancer() {
        // Utility class.
    }

    /**
     * Returns whether the given action name is synthetic and should be
     * filtered out of coverage measurement. Three flavours are recognised:
     *
     * <ul>
     *   <li>{@value #SYNTHETIC_ACTION_PREFIX}N — legacy direct synthetic
     *       balancing edge;</li>
     *   <li>{@code __end__} — back-to-INIT synthetic added by
     *       {@link be.vibes.testgeneration.conversion.MxeToFtsConverter}
     *       for mixed-terminal ESG vertices (the FTS counterpart of
     *       ESG-Fx's {@code "]" -> "["} edge);</li>
     *   <li>{@code <action>__dup__N} — edge-doubling balance transition;
     *       the suffix-stripped prefix is the real action that's being
     *       duplicated for Eulerian balance.</li>
     * </ul>
     */
    public static boolean isSyntheticAction(String actionName) {
        if (actionName == null) {
            return false;
        }
        return actionName.startsWith(SYNTHETIC_ACTION_PREFIX)
                || actionName.startsWith("__end__")
                || actionName.contains(DUPLICATE_ACTION_INFIX);
    }

    public static boolean isSyntheticAction(Action action) {
        return action != null && isSyntheticAction(action.getName());
    }

    /**
     * If {@code actionName} is a duplicate-balance label, returns the
     * original action name (everything before {@code __dup__}); otherwise
     * returns {@code actionName} unchanged. Useful when displaying test
     * sequences so doubled transitions read as the real action.
     */
    public static String stripDuplicateSuffix(String actionName) {
        if (actionName == null) {
            return null;
        }
        int idx = actionName.indexOf(DUPLICATE_ACTION_INFIX);
        return idx < 0 ? actionName : actionName.substring(0, idx);
    }

    /**
     * Balances the given FTS by doubling shortest-path transitions
     * between imbalanced pairs (Chinese Postman). Returns a NEW FTS
     * instance; the input is not modified.
     *
     * @param fts a strongly-connected featured transition system
     * @return a balanced FTS with {@code in_degree == out_degree} at every state
     * @throws IllegalArgumentException if the input is not strongly connected
     */
    public static FeaturedTransitionSystem balance(FeaturedTransitionSystem fts) {
        checkNotNull(fts, "FTS may not be null");
        if (!StronglyConnectedComponents.isStronglyConnected(fts)) {
            throw new IllegalArgumentException(
                    "EulerianBalancer requires a strongly connected input; "
                            + "repair the FTS first (e.g. drop unreachable states or rewire sinks).");
        }
        return balanceCore(fts);
    }

    /**
     * Like {@link #balance} but skips the strong-connectivity precondition.
     * Used by callers (notably the transition-pair coverage pipeline)
     * whose graphs have a structurally-source-only INIT vertex that the
     * balancer is expected to repair.
     */
    public static FeaturedTransitionSystem balanceWithoutPrecheck(FeaturedTransitionSystem fts) {
        checkNotNull(fts, "FTS may not be null");
        return balanceCore(fts);
    }

    private static FeaturedTransitionSystem balanceCore(FeaturedTransitionSystem fts) {

        Map<State, Integer> imbalance = computeImbalance(fts);

        // Copy the input FTS into a fresh factory verbatim.
        FeaturedTransitionSystemFactory factory =
                new FeaturedTransitionSystemFactory(fts.getInitialState().getName());

        Iterator<State> stateIt = fts.states();
        while (stateIt.hasNext()) {
            factory.addState(stateIt.next().getName());
        }
        Iterator<Action> actionIt = fts.actions();
        while (actionIt.hasNext()) {
            factory.addAction(actionIt.next().getName());
        }
        Iterator<Transition> transitionIt = fts.transitions();
        while (transitionIt.hasNext()) {
            Transition t = transitionIt.next();
            FExpression fexpr = fts.getFExpression(t);
            if (fexpr == null) {
                fexpr = FExpression.trueValue();
            }
            factory.addTransition(t.getSource().getName(), t.getAction().getName(),
                    fexpr, t.getTarget().getName());
        }

        // Build excess-out (delta < 0: needs extra outgoing) and excess-in
        // (delta > 0: needs extra incoming) queues. Pair them up greedily
        // and double the shortest path from each excess-out source to its
        // matched excess-in target.
        Deque<State> excessOut = new ArrayDeque<>();
        Deque<State> excessIn = new ArrayDeque<>();
        for (Map.Entry<State, Integer> e : imbalance.entrySet()) {
            int delta = e.getValue();
            if (delta > 0) {
                for (int i = 0; i < delta; i++) {
                    excessIn.add(e.getKey());
                }
            } else if (delta < 0) {
                for (int i = 0; i < -delta; i++) {
                    excessOut.add(e.getKey());
                }
            }
        }
        if (excessOut.size() != excessIn.size()) {
            throw new IllegalStateException(
                    "Imbalance totals do not match: excessOut=" + excessOut.size()
                            + ", excessIn=" + excessIn.size());
        }

        int dupCounter = 0;
        int fallbackCounter = 0;
        while (!excessOut.isEmpty()) {
            State src = excessOut.poll();
            State tgt = excessIn.poll();
            List<Transition> path = shortestPath(fts, src, tgt);
            if (path == null) {
                // No real path from src to tgt. This happens when the input
                // is not strongly connected — e.g. the pair-graph pipeline
                // calls balanceWithoutPrecheck on a graph whose INIT is
                // source-only. Fall back to inserting a direct synthetic
                // edge (the previous strategy) so the result is still
                // Eulerian. These edges DO break test-sequence contiguity
                // and have to be split / filtered downstream; they are
                // marked with the legacy SYNTHETIC_ACTION_PREFIX.
                String syntheticAction = SYNTHETIC_ACTION_PREFIX + fallbackCounter;
                fallbackCounter++;
                factory.addAction(syntheticAction);
                factory.addTransition(src.getName(), syntheticAction,
                        FExpression.trueValue(), tgt.getName());
                continue;
            }
            // Double each transition on the path with a unique duplicate
            // action label so VIBeS' (source, action, target) dedup does
            // not collapse parallel doublings of the same edge.
            for (Transition t : path) {
                String dupAction = t.getAction().getName() + DUPLICATE_ACTION_INFIX + dupCounter;
                dupCounter++;
                factory.addAction(dupAction);
                FExpression fexpr = fts.getFExpression(t);
                if (fexpr == null) {
                    fexpr = FExpression.trueValue();
                }
                factory.addTransition(t.getSource().getName(), dupAction, fexpr,
                        t.getTarget().getName());
            }
        }
        LOG.info("Balanced FTS: doubled {} real transition(s) via Chinese Postman; "
                        + "inserted {} fallback synthetic edge(s) where no real path existed",
                dupCounter, fallbackCounter);

        return factory.build();
    }

    /**
     * BFS shortest path of transitions from {@code source} to {@code target}
     * along the existing edges of {@code fts}. Returns an empty list when
     * {@code source.equals(target)} and {@code null} when no path exists.
     */
    static List<Transition> shortestPath(FeaturedTransitionSystem fts, State source, State target) {
        if (source.equals(target)) {
            return new ArrayList<>();
        }
        Map<State, Transition> arrival = new HashMap<>();
        Set<State> visited = new HashSet<>();
        Deque<State> queue = new ArrayDeque<>();
        queue.add(source);
        visited.add(source);
        State found = null;
        while (!queue.isEmpty() && found == null) {
            State v = queue.poll();
            Iterator<Transition> outs = fts.getOutgoing(v);
            while (outs.hasNext()) {
                Transition t = outs.next();
                State w = t.getTarget();
                if (visited.contains(w)) {
                    continue;
                }
                visited.add(w);
                arrival.put(w, t);
                if (w.equals(target)) {
                    found = w;
                    break;
                }
                queue.offer(w);
            }
        }
        if (found == null) {
            return null;
        }
        List<Transition> path = new ArrayList<>();
        State cursor = found;
        while (!cursor.equals(source)) {
            Transition t = arrival.get(cursor);
            path.add(t);
            cursor = t.getSource();
        }
        java.util.Collections.reverse(path);
        return path;
    }

    /**
     * Returns the imbalance map: state → out_degree - in_degree. Only the
     * states with non-zero delta are included.
     */
    static Map<State, Integer> computeImbalance(FeaturedTransitionSystem fts) {
        Map<State, Integer> imbalance = new HashMap<>();
        Iterator<State> it = fts.states();
        while (it.hasNext()) {
            State s = it.next();
            int out = count(fts.getOutgoing(s));
            int in = count(fts.getIncoming(s));
            int delta = out - in;
            if (delta != 0) {
                imbalance.put(s, delta);
            }
        }
        return imbalance;
    }

    private static int count(Iterator<?> it) {
        int n = 0;
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    /**
     * Returns the list of synthetic transitions in a balanced FTS, in
     * iteration order. Used by diagnostics and by coverage measurement
     * for filtering.
     */
    public static List<Transition> syntheticTransitions(FeaturedTransitionSystem balanced) {
        List<Transition> result = new ArrayList<>();
        Iterator<Transition> it = balanced.transitions();
        while (it.hasNext()) {
            Transition t = it.next();
            if (isSyntheticAction(t.getAction())) {
                result.add(t);
            }
        }
        return result;
    }
}
