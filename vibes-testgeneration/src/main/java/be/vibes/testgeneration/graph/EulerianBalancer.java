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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Produces a {@link FeaturedTransitionSystem} in which every state has
 * {@code in_degree == out_degree}, i.e. the necessary condition for an
 * Euler cycle in a directed multigraph.
 *
 * <p>The balancer ASSUMES the input is strongly connected. Strong
 * connectivity is the second necessary condition for an Euler cycle, and
 * checking + repairing it is conceptually a separate concern handled by
 * earlier pipeline stages (post-projection SCC repair). The balancer
 * raises {@link IllegalArgumentException} if it discovers the assumption
 * is violated.
 *
 * <p>Synthetic transitions are added with action names prefixed by
 * {@code __balance__} and feature expression {@link FExpression#trueValue()}.
 * The original transitions (action + fexpr metadata) are copied verbatim.
 *
 * <p>The balancing strategy is a greedy bipartite pairing of
 * "surplus" states (out-degree &gt; in-degree, so they need more incoming
 * edges; equivalently the source side of a needed synthetic edge) with
 * "deficit" states (in-degree &gt; out-degree, the target side). The
 * pairing matches one unit of imbalance per synthetic edge; this is not
 * minimum-weight but is sufficient for Eulerian existence and keeps the
 * synthetic edge count to exactly &Sigma; max(0, out − in) (which equals
 * &Sigma; max(0, in − out) since the totals balance).
 *
 * <p>Each synthetic transition gets a unique action name to avoid VIBeS'
 * {@code (source, action, target)} deduplication, which would otherwise
 * collapse repeated synthetic edges between the same pair into one.
 */
public final class EulerianBalancer {

    private static final Logger LOG = LoggerFactory.getLogger(EulerianBalancer.class);

    /** Prefix marker for synthetic actions added by the balancer. */
    public static final String SYNTHETIC_ACTION_PREFIX = "__balance__";

    private EulerianBalancer() {
        // Utility class.
    }

    /**
     * Returns whether the given action name is a synthetic action that
     * should be excluded from coverage measurement. Two flavours of
     * synthetic action exist in the pipeline:
     *
     * <ul>
     *   <li>{@value #SYNTHETIC_ACTION_PREFIX}N — added by this class
     *       to balance in-degree and out-degree for Hierholzer;</li>
     *   <li>{@code __end__} — added by
     *       {@link be.vibes.testgeneration.conversion.MxeToFtsConverter}
     *       as the FTS counterpart of the ESG-Fx-side {@code "]" -> "["}
     *       back-edge (a "test ends here" marker on mixed-terminal
     *       vertices). It restores strong connectivity at the SPL level.</li>
     * </ul>
     *
     * Both prefixes start with a double underscore, so the check is a
     * single prefix test.
     */
    public static boolean isSyntheticAction(String actionName) {
        return actionName != null
                && (actionName.startsWith(SYNTHETIC_ACTION_PREFIX)
                        || actionName.startsWith("__end__"));
    }

    /**
     * Convenience: returns whether the given action is synthetic.
     */
    public static boolean isSyntheticAction(Action action) {
        return action != null && isSyntheticAction(action.getName());
    }

    /**
     * Balances the given FTS by adding synthetic transitions. Returns a NEW
     * FTS instance; the input is not modified.
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
     * Like {@link #balance} but skips the strong-connectivity precondition
     * check. Intended for callers that know the graph is structured in such
     * a way that balancing will RESTORE strong connectivity (e.g. pair
     * graphs, which have a source-only INIT vertex whose missing incoming
     * edges are exactly what balancing supplies).
     *
     * <p>The caller is responsible for verifying strong connectivity on the
     * result if it matters downstream (Hierholzer requires it).
     */
    public static FeaturedTransitionSystem balanceWithoutPrecheck(FeaturedTransitionSystem fts) {
        checkNotNull(fts, "FTS may not be null");
        return balanceCore(fts);
    }

    private static FeaturedTransitionSystem balanceCore(FeaturedTransitionSystem fts) {

        Map<State, Integer> imbalance = computeImbalance(fts);

        // Copy the input FTS into a fresh factory: every state, every action,
        // and every (action-bearing) transition is preserved verbatim.
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

        // Build two FIFOs of unit-imbalance "slots" — surplus on the source
        // side, deficit on the target side — and zip them.
        Deque<String> surplus = new ArrayDeque<>();
        Deque<String> deficit = new ArrayDeque<>();
        for (Map.Entry<State, Integer> e : imbalance.entrySet()) {
            int delta = e.getValue(); // delta = out - in
            String name = e.getKey().getName();
            if (delta > 0) {
                // Out-heavy: needs |delta| extra incoming. It becomes the
                // TARGET of |delta| synthetic edges.
                for (int i = 0; i < delta; i++) {
                    deficit.add(name);
                }
            } else if (delta < 0) {
                // In-heavy: needs |delta| extra outgoing. It becomes the
                // SOURCE of |delta| synthetic edges.
                for (int i = 0; i < -delta; i++) {
                    surplus.add(name);
                }
            }
        }
        if (surplus.size() != deficit.size()) {
            // This is a defensive check: surplus and deficit totals must be
            // equal in any directed graph (sum of out-degrees == sum of
            // in-degrees == |edges|).
            throw new IllegalStateException(
                    "Imbalance totals do not match: surplus=" + surplus.size()
                            + ", deficit=" + deficit.size());
        }
        int counter = 0;
        while (!surplus.isEmpty()) {
            String src = surplus.poll();
            String tgt = deficit.poll();
            String syntheticAction = SYNTHETIC_ACTION_PREFIX + counter;
            factory.addAction(syntheticAction);
            factory.addTransition(src, syntheticAction, FExpression.trueValue(), tgt);
            counter++;
        }
        LOG.info("Balanced FTS by adding {} synthetic transitions", counter);

        return factory.build();
    }

    /**
     * Returns a map {@code state -> out_degree - in_degree} including only the
     * states that have a non-zero imbalance.
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

    /** Reads an iterator dry and returns its length. */
    private static int count(Iterator<?> it) {
        int n = 0;
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    /**
     * Convenience: returns the list of synthetic transitions in a balanced
     * FTS, in iteration order. Useful for diagnostics and for excluding
     * synthetic edges from coverage measurement.
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
