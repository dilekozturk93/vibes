package be.vibes.testgeneration.coverage;

import be.vibes.fexpression.FExpression;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.FeaturedTransitionSystemFactory;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Turns a {@link FeaturedTransitionSystem} into its <em>pair graph</em>,
 * the directed graph whose vertices represent the transitions of the
 * original FTS and whose edges represent contiguous transition pairs.
 *
 * <p>This is the FTS analog of the ESG-Fx-side
 * {@code TransformedESGFxGenerator} used in the user's prior published
 * study for triple-coverage. The construction here is the L=2 case
 * (pairs); higher coverage levels would iterate the same construction.
 *
 * <p>Pair-graph construction rules:
 * <ul>
 *   <li>A distinguished state {@code INIT} represents "no transition
 *       executed yet" — the starting point of any test case.</li>
 *   <li>For each NON-SYNTHETIC transition {@code t} of the original FTS,
 *       the pair graph has a state named {@code "p_<source>_<action>_<target>"}.
 *       Synthetic transitions ({@code __end__}, {@code __balance__N},
 *       {@code __dup__N}) are SKIPPED at this stage — see the rationale
 *       below.</li>
 *   <li>For each non-synthetic original transition {@code t} whose source
 *       is the original initial state, the pair graph has an edge
 *       {@code INIT -> p(t)} labelled with {@code action(t)}.</li>
 *   <li>For each ordered pair of non-synthetic original transitions
 *       {@code (t1, t2)} with {@code target(t1) == source(t2)}, the pair
 *       graph has an edge {@code p(t1) -> p(t2)} labelled with
 *       {@code action(t2)}.</li>
 * </ul>
 *
 * <p><strong>Why synthetic transitions are excluded from pair-graph
 * construction.</strong> The coverage metric
 * ({@link be.vibes.testgeneration.experiment.MetricsCollector
 * #pairCoveragePercentageOfSuite}) drops synthetic actions from each
 * test case's walk before forming consecutive pairs. Pairs involving an
 * {@code __end__} (e.g. {@code (real, __end__)} or
 * {@code (__end__, real)}) are therefore worth zero in the denominator,
 * yet the un-filtered pair-graph construction would generate pair-graph
 * edges for them. Translating those edges back produces TestCases like
 * {@code [__end__, real]} that contribute nothing to pair coverage but
 * still cost a test case slot and a setup/teardown — visible in early
 * reports as repeated single-action test cases like "open mailbox" alone,
 * each coming from a {@code p(__end___from_X) -> p(open_mailbox)} edge.
 * Skipping synthetic transitions at construction time aligns the graph
 * with the metric.</p>
 *
 * <p>A Hierholzer Euler cycle on the (balanced, SCC-repaired) pair graph
 * visits every pair-graph edge exactly once. By construction, the
 * sequence of labels along that cycle is a sequence of original-FTS
 * transitions that covers every contiguous transition pair, which is
 * exactly the all-transition-pairs coverage criterion of RQ3.
 *
 * <p>The {@link Result#pairStateToOriginalTransition} side-map lets
 * downstream code translate a pair-graph cycle back into the
 * corresponding sequence of original-FTS transitions: the
 * {@link Transition#getTarget()} of each pair-graph cycle edge is the
 * pair-graph state whose {@link Transition} we executed.
 */
public final class PairGraphTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(PairGraphTransformer.class);

    private static final String INIT_NAME = "INIT";

    private PairGraphTransformer() {
        // Utility class.
    }

    /**
     * The (pair graph, side-map, initial state name) triple produced by
     * {@link #transform}.
     */
    public static final class Result {
        public final FeaturedTransitionSystem pairGraph;
        public final Map<State, Transition> pairStateToOriginalTransition;
        public final String initialStateName;

        Result(FeaturedTransitionSystem pairGraph,
               Map<State, Transition> pairStateToOriginalTransition,
               String initialStateName) {
            this.pairGraph = pairGraph;
            this.pairStateToOriginalTransition =
                    Collections.unmodifiableMap(pairStateToOriginalTransition);
            this.initialStateName = initialStateName;
        }
    }

    /**
     * Builds the pair graph of the given (post-projection, SCC-repaired)
     * FTS and returns it along with the side-map used to translate
     * pair-graph traversals back into original-FTS transition sequences.
     */
    public static Result transform(FeaturedTransitionSystem original) {
        checkNotNull(original, "Original FTS may not be null");

        FeaturedTransitionSystemFactory factory = new FeaturedTransitionSystemFactory(INIT_NAME);

        // Index NON-SYNTHETIC original transitions by source state. Synthetic
        // transitions (__end__ et al.) are filtered out at construction time
        // — see the class JavaDoc for the rationale.
        Map<State, java.util.List<Transition>> outgoingBySource = new HashMap<>();
        Iterator<Transition> tIt = original.transitions();
        java.util.List<Transition> allOriginalTransitions = new java.util.ArrayList<>();
        while (tIt.hasNext()) {
            Transition t = tIt.next();
            if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                continue;
            }
            allOriginalTransitions.add(t);
            outgoingBySource.computeIfAbsent(t.getSource(),
                    k -> new java.util.ArrayList<>()).add(t);
        }

        // Pre-declare a pair-graph state for every original transition so
        // factory.addTransition can name-resolve them. Also build the
        // pair-state -> original-transition side-map.
        Map<State, Transition> pairStateToOriginalTransition = new HashMap<>();
        Map<Transition, String> originalToPairName = new HashMap<>();
        for (Transition t : allOriginalTransitions) {
            String name = pairStateName(t);
            factory.addState(name);
            originalToPairName.put(t, name);
        }

        // INIT -> p(t) edges for every original transition starting at the
        // original initial state.
        State originalInitial = original.getInitialState();
        for (Transition t : allOriginalTransitions) {
            if (!t.getSource().equals(originalInitial)) {
                continue;
            }
            String actionName = t.getAction().getName();
            factory.addAction(actionName);
            factory.addTransition(INIT_NAME, actionName, FExpression.trueValue(),
                    originalToPairName.get(t));
        }

        // p(t1) -> p(t2) edges for every contiguous transition pair.
        for (Transition t1 : allOriginalTransitions) {
            java.util.List<Transition> nextOptions = outgoingBySource.get(t1.getTarget());
            if (nextOptions == null) {
                continue;
            }
            for (Transition t2 : nextOptions) {
                String actionName = t2.getAction().getName();
                factory.addAction(actionName);
                factory.addTransition(originalToPairName.get(t1), actionName,
                        FExpression.trueValue(), originalToPairName.get(t2));
            }
        }

        FeaturedTransitionSystem pairGraph = factory.build();

        // Populate the side-map AFTER build(), so we get references to the
        // factory-managed State instances rather than locally-built ones.
        for (Transition t : allOriginalTransitions) {
            State pairState = pairGraph.getState(originalToPairName.get(t));
            pairStateToOriginalTransition.put(pairState, t);
        }

        LOG.info("Pair graph: {} states ({} transitions + INIT), {} edges (pairs)",
                allOriginalTransitions.size() + 1, allOriginalTransitions.size(),
                countTransitions(pairGraph));

        return new Result(pairGraph, pairStateToOriginalTransition, INIT_NAME);
    }

    /**
     * Returns the canonical pair-graph state name for the given original
     * transition. Exposed package-private for tests.
     */
    static String pairStateName(Transition t) {
        return "p_" + t.getSource().getName() + "_" + t.getAction().getName()
                + "_" + t.getTarget().getName();
    }

    private static int countTransitions(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }
}
