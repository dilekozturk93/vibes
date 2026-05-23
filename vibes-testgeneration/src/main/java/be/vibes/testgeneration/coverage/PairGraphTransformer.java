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
 * <p>Pair-graph construction rules (INIT-less variant, 2026-05-23):
 * <ul>
 *   <li>For each NON-SYNTHETIC transition {@code t} of the original FTS,
 *       the pair graph has a state named {@code "p_<source>_<action>_<target>"}.
 *       Synthetic transitions ({@code __end__}, {@code __balance__N},
 *       {@code __dup__N}) are SKIPPED at this stage — see the rationale
 *       below.</li>
 *   <li>For each ordered pair of non-synthetic original transitions
 *       {@code (t1, t2)} with {@code target(t1) == source(t2)}, the pair
 *       graph has an edge {@code p(t1) -> p(t2)} labelled with
 *       {@code action(t2)}.</li>
 *   <li><strong>No separate INIT vertex.</strong> Instead, the pair
 *       graph's initial state is the <em>canonical initial-pair-vertex</em>
 *       — the lexicographically-first pair-vertex {@code p(t_init)}
 *       where {@code t_init.source == originalFTS.initialState}. The
 *       canonical-start choice is deterministic so re-running the
 *       pipeline on the same input produces the same output.</li>
 * </ul>
 *
 * <p><strong>Note — boundary pairs are KEPT in the graph but EXCLUDED
 * from the coverage metric.</strong> Pair-graph edges where both
 * {@code target(t1) == FTS_initial} and {@code source(t2) == FTS_initial}
 * represent operationally test-case-crossing transition sequences (under
 * standard reset-per-test-case executor semantic, {@code t1} ends one
 * test case at the initial state and {@code t2} starts the next from
 * the initial state — a reset is inserted between them). The strict
 * Edge-Pair coverage definition (Ammann &amp; Offutt 2008) requires
 * consecutive execution within a single test case, so these
 * test-case-crossing pairs are NOT real Edge-Pair coverage targets.
 * However, they CANNOT be removed from the pair-graph: doing so would
 * leave every initial-outgoing pair-vertex with in-degree 0 and every
 * "returns-to-initial" pair-vertex with out-degree 0, recreating the
 * structural balance problem that the INIT removal solved. The boundary
 * exclusion is therefore applied at the COVERAGE METRIC layer — see
 * {@link be.vibes.testgeneration.experiment.MetricsCollector
 * #pairCoveragePercentageOfSuite}, which subtracts boundary pairs from
 * the denominator. Hierholzer's cycle still visits boundary edges
 * (necessary for traversability); the splitter splits at them naturally
 * (the splitter sees {@code target=initial}, closes the trip, starts
 * the next); pair coverage measurement correctly ignores them in both
 * numerator and denominator.
 *
 * <p><em>Edge case — initial self-loops.</em> If {@code FTS_initial}
 * has a self-loop transition {@code t_s} (source = target = initial),
 * the pair {@code (t_s, t_initial_outgoing)} is operationally a
 * within-TC pair (no reset; {@code t_s} ends at initial but the test
 * case continues), yet the metric-level boundary filter would exclude
 * it. None of the five evaluated SPLs contain initial self-loops, so
 * this exclusion is exact for our evaluation; a paper Threats-to-Validity
 * note covers the general case.</p>
 *
 * <p><strong>Why INIT was removed.</strong> The previous design used a
 * dedicated {@code INIT} vertex with edges {@code INIT -> p(t)} for every
 * initial-outgoing transition. That vertex had in-degree 0 by construction
 * (no edges pointing TO {@code INIT}), so in the
 * {@link EulerianBalancer} it always appeared as an unsatisfiable
 * excessIn target — every pairing attempt fell through to a synthetic
 * {@code __balance__} edge. With INIT removed, the role of "where the
 * test starts" is played by a regular pair-vertex {@code p(t_init)},
 * which already has in-edges from {@code p(t')} for every {@code t'}
 * whose target is the FTS initial state. The structural source of every
 * INIT-bound {@code __balance__} disappears; balance is mostly handled
 * via {@code __dup__} (real-path edge doubling).
 *
 * <p>Downstream consumers translate the Hierholzer cycle to an FTS action
 * sequence by:
 * <ol>
 *   <li>PREPENDING the canonical start's underlying transition (the
 *       first transition the tester must execute to reach the cycle's
 *       starting pair-vertex);</li>
 *   <li>For each pair-graph cycle edge, emitting the underlying
 *       transition pointed to by the edge's {@code target} pair-vertex
 *       (via the {@link Result#pairStateToOriginalTransition} side-map).
 *       {@code __dup__N} edges' targets resolve to the underlying real
 *       transition (the same transition the un-suffixed pair-vertex
 *       represents). {@code __balance__N} edges have no target
 *       pair-vertex and are dropped at translation, producing a
 *       discontinuity in the action sequence that downstream splitters
 *       must handle.</li>
 * </ol>
 *
 * <p><strong>Why synthetic transitions are excluded from pair-graph
 * construction.</strong> The coverage metric
 * ({@link be.vibes.testgeneration.experiment.MetricsCollector
 * #pairCoveragePercentageOfSuite}) drops synthetic actions from each
 * test case's walk before forming consecutive pairs. Pairs involving an
 * {@code __end__} would otherwise produce TestCases that contribute
 * nothing to pair coverage; skipping synthetic transitions at
 * construction time aligns the graph with the metric.
 *
 * <p>A Hierholzer Euler cycle on the (balanced) pair graph visits every
 * pair-graph edge exactly once. By construction, the sequence of labels
 * along that cycle is a sequence of original-FTS transitions that covers
 * every contiguous transition pair, which is exactly the
 * all-transition-pairs coverage criterion.
 */
public final class PairGraphTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(PairGraphTransformer.class);

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
        /**
         * Name of the canonical initial pair-vertex (the lexicographically
         * first {@code p(t_init)} for {@code t_init.source == FTS_initial}).
         * Also {@code pairGraph.getInitialState().getName()}.
         */
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
     *
     * @throws IllegalArgumentException if the FTS has no non-synthetic
     *         transition starting at the initial state (no candidate for
     *         the canonical initial pair-vertex)
     */
    public static Result transform(FeaturedTransitionSystem original) {
        checkNotNull(original, "Original FTS may not be null");

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

        // Pick the canonical initial pair-vertex deterministically: the
        // lexicographically-smallest pair-state name among non-synthetic
        // transitions starting at the FTS initial state. This becomes the
        // pair-graph's initial state. If the FTS initial state has
        // multiple outgoings, the other initial-pair-vertices still get
        // their own pair-vertices and are visited naturally by the
        // Hierholzer cycle — they become starts of subsequent test cases
        // after splitAtInitialReturns. Only the canonical one needs the
        // pair-graph's "initial state" marker.
        State originalInitial = original.getInitialState();
        String canonicalStart = null;
        for (Transition t : allOriginalTransitions) {
            if (!t.getSource().equals(originalInitial)) continue;
            String name = pairStateName(t);
            if (canonicalStart == null || name.compareTo(canonicalStart) < 0) {
                canonicalStart = name;
            }
        }
        if (canonicalStart == null) {
            throw new IllegalArgumentException(
                    "FTS initial state '" + originalInitial.getName()
                            + "' has no non-synthetic outgoing transition; "
                            + "cannot construct INIT-less pair graph (no candidate for "
                            + "canonical initial pair-vertex).");
        }

        FeaturedTransitionSystemFactory factory =
                new FeaturedTransitionSystemFactory(canonicalStart);

        // Pre-declare a pair-graph state for every original transition so
        // factory.addTransition can name-resolve them. Also build the
        // pair-state -> original-transition side-map.
        Map<State, Transition> pairStateToOriginalTransition = new HashMap<>();
        Map<Transition, String> originalToPairName = new HashMap<>();
        for (Transition t : allOriginalTransitions) {
            String name = pairStateName(t);
            // canonicalStart was added implicitly via the factory constructor;
            // skip re-adding it to avoid a duplicate-state exception.
            if (!name.equals(canonicalStart)) {
                factory.addState(name);
            }
            originalToPairName.put(t, name);
        }

        // p(t1) -> p(t2) edges for every contiguous transition pair.
        // Boundary pairs (t1.target=initial AND t2.source=initial) are
        // KEPT in the graph — they balance the pair-graph's degree
        // structure. Removing them would leave every initial-outgoing
        // pair-vertex with in-degree 0 and every "returns-to-initial"
        // pair-vertex with out-degree 0, recreating the unsolvable
        // structural imbalance INIT removal solved. The boundary
        // exclusion is applied at the COVERAGE METRIC layer
        // (MetricsCollector.pairCoveragePercentageOfSuite) instead, where
        // the denominator excludes boundary pairs to align with the
        // literature Edge-Pair coverage definition (within-TC only).
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

        LOG.info("Pair graph (INIT-less): {} vertices, {} edges; canonical start = {}",
                allOriginalTransitions.size(), countTransitions(pairGraph), canonicalStart);

        return new Result(pairGraph, pairStateToOriginalTransition, canonicalStart);
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
