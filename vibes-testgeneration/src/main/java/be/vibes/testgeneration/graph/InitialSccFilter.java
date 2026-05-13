package be.vibes.testgeneration.graph;

import be.vibes.fexpression.FExpression;
import be.vibes.ts.Action;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.FeaturedTransitionSystemFactory;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Restricts a {@link FeaturedTransitionSystem} to the strongly-connected
 * component that contains the initial state.
 *
 * <p>After projection (M4) the product-level FTS frequently fragments into
 * multiple SCCs because the projection drops feature-incompatible
 * transitions. Downstream Euler-cycle generation requires a single
 * strongly-connected component containing the initial state. This filter
 * drops states (and transitions touching them) outside that component.
 *
 * <p>States outside the initial-state SCC fall into two intuitive
 * categories — those reachable from the initial state but with no path
 * back, and those not reachable at all. Both are discarded. Test cases
 * therefore cover whatever the product can actually reach AND return from;
 * the dropped behaviour is documented per-product as the "lost coverage
 * ratio" in the experiment metrics.
 */
public final class InitialSccFilter {

    private static final Logger LOG = LoggerFactory.getLogger(InitialSccFilter.class);

    private InitialSccFilter() {
        // Utility class.
    }

    /**
     * Returns a new {@link FeaturedTransitionSystem} containing only the
     * states in the SCC that contains the initial state, plus the
     * transitions whose source and target both belong to that SCC.
     *
     * @param fts the (possibly fragmented) FTS to filter
     * @return a strongly-connected FTS rooted at the original initial state
     */
    public static FeaturedTransitionSystem keepInitialScc(FeaturedTransitionSystem fts) {
        checkNotNull(fts, "FTS may not be null");
        State initial = fts.getInitialState();
        Set<State> scc = StronglyConnectedComponents.containing(fts, initial);
        if (scc == null || scc.isEmpty()) {
            throw new IllegalStateException(
                    "Initial state " + initial.getName() + " is not in any SCC; "
                            + "this should never happen as Tarjan emits one SCC per state.");
        }
        if (countStates(fts) == scc.size()) {
            // Already strongly connected. Return the original to avoid
            // pointlessly rebuilding the factory.
            return fts;
        }

        FeaturedTransitionSystemFactory factory =
                new FeaturedTransitionSystemFactory(initial.getName());

        // Re-declare states first so addTransition can name-resolve.
        for (State s : scc) {
            factory.addState(s.getName());
        }

        // Preserve action declarations for actions that survive the filter.
        // We only add actions that appear on at least one kept transition;
        // VIBeS' factory tolerates duplicate addActions.
        int kept = 0;
        int dropped = 0;
        Iterator<Transition> transitionIt = fts.transitions();
        while (transitionIt.hasNext()) {
            Transition t = transitionIt.next();
            if (scc.contains(t.getSource()) && scc.contains(t.getTarget())) {
                Action action = t.getAction();
                FExpression fexpr = fts.getFExpression(t);
                if (fexpr == null) {
                    fexpr = FExpression.trueValue();
                }
                factory.addAction(action.getName());
                factory.addTransition(t.getSource().getName(), action.getName(),
                        fexpr, t.getTarget().getName());
                kept++;
            } else {
                dropped++;
            }
        }
        LOG.info("Filtered to initial SCC: kept {} states, {} transitions; dropped {} transitions",
                scc.size(), kept, dropped);
        return factory.build();
    }

    private static int countStates(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<State> it = fts.states();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }
}
