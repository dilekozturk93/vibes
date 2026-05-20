package be.vibes.testgeneration.coverage;

import be.vibes.fexpression.configuration.Configuration;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.testgeneration.graph.HierholzerEulerCycle;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.graph.StronglyConnectedComponents;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import be.vibes.ts.exception.TransitionSystenExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates an all-transition-pairs test case for one product configuration.
 *
 * <p>Pipeline:
 *
 * <ol>
 *   <li>{@link FExpressionPreservingProjection#project} — project SPL FTS
 *       onto the configuration.</li>
 *   <li>{@link InitialSccFilter#keepInitialScc} — drop states that
 *       cannot return to the initial state.</li>
 *   <li>{@link PairGraphTransformer#transform} — build the pair graph.</li>
 *   <li>{@link InitialSccFilter#keepInitialScc} (again, on the pair
 *       graph) — drop pair-graph states that cannot return to its INIT.</li>
 *   <li>{@link EulerianBalancer#balance} — make every pair-graph state
 *       have equal in / out degree.</li>
 *   <li>{@link HierholzerEulerCycle#compute} — extract an Euler cycle
 *       that visits every pair-graph edge exactly once.</li>
 *   <li>Translate the pair-graph cycle back to a sequence of original
 *       (repaired) FTS transitions and wrap as {@link TestCase}.</li>
 * </ol>
 *
 * <p>Each pair-graph edge corresponds to a contiguous transition pair in
 * the original FTS, so the resulting test case covers every reachable
 * transition pair at least once.
 *
 * <p>Synthetic balancing edges in the pair graph (introduced by
 * {@link EulerianBalancer}) cannot be translated back to original
 * transitions; they are skipped during translation and contribute only
 * to the cycle length, not to the test case length. The resulting
 * {@link TestCase} may therefore contain fewer transitions than the
 * pair-graph cycle length.
 */
public final class TransitionPairCoverageGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(TransitionPairCoverageGenerator.class);

    private TransitionPairCoverageGenerator() {
        // Utility class.
    }

    /**
     * Generates an all-transition-pairs test SUITE for the given product
     * configuration.
     *
     * <p>Returns a {@link List} of {@link TestCase}s, not a single
     * {@code TestCase}. The pair graph contains a source-only {@code INIT}
     * vertex whose missing incoming edges are supplied as synthetic
     * balancing edges by {@link EulerianBalancer}. The resulting
     * Hierholzer cycle naturally splits at every synthetic edge: each
     * "real segment" of the cycle starts at INIT in the pair graph and
     * corresponds to a contiguous, replayable test case in the original
     * FTS. Synthetic edges themselves do not appear in any returned
     * test case.
     *
     * <p>For RQ metrics ("total test length", "test count"), aggregate
     * across the returned list.
     */
    public static List<TestCase> generate(FeaturedTransitionSystem fts,
                                          Configuration product,
                                          String testCaseBaseId) {
        checkNotNull(fts, "FTS may not be null");
        checkNotNull(product, "Configuration may not be null");
        checkNotNull(testCaseBaseId, "Test case base id may not be null");

        FeaturedTransitionSystem projected =
                FExpressionPreservingProjection.project(fts, product);
        FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);

        PairGraphTransformer.Result pgResult = PairGraphTransformer.transform(repaired);

        // The pair graph is intentionally NOT strongly connected before
        // balancing: its INIT vertex has out-degree N (one per original
        // initial-state transition) but in-degree 0, so applying the usual
        // InitialSccFilter would discard everything except INIT.
        // EulerianBalancer.balanceWithoutPrecheck supplies the missing
        // INIT-incoming edges as synthetic balancing edges, which restores
        // strong connectivity. We verify that explicitly below.
        FeaturedTransitionSystem pairGraphBalanced =
                EulerianBalancer.balanceWithoutPrecheck(pgResult.pairGraph);
        if (!StronglyConnectedComponents.isStronglyConnected(pairGraphBalanced)) {
            throw new IllegalStateException(
                    "Pair graph for '" + testCaseBaseId
                            + "' is not strongly connected after balancing; "
                            + "the projected FTS may have an unrepaired-fragment in its pair structure.");
        }

        List<Transition> pairCycle = HierholzerEulerCycle.compute(pairGraphBalanced);

        // Split the pair-graph cycle at synthetic edges and translate each
        // real segment into a TestCase against the repaired FTS.
        List<List<Transition>> realSegments = splitAtSyntheticEdges(pairCycle);
        List<TestCase> testCases = new ArrayList<>(realSegments.size());
        int segIndex = 0;
        int totalTransitions = 0;
        for (List<Transition> segment : realSegments) {
            if (segment.isEmpty()) {
                continue;
            }
            List<Transition> translated = translateSegment(segment, repaired,
                    pgResult.pairStateToOriginalTransition);
            String id = testCaseBaseId + "_seg" + (segIndex++);
            TestCase tc = new TestCase(id);
            try {
                tc.enqueueAll(translated);
            } catch (TransitionSystenExecutionException ex) {
                throw new IllegalStateException(
                        "Translated pair-graph segment could not be enqueued into TestCase '"
                                + id + "': " + ex.getMessage(), ex);
            }
            testCases.add(tc);
            totalTransitions += translated.size();
        }
        // Dedupe at the action-sequence level: two TestCases whose translated
        // action sequences are identical exercise the same action-pair set
        // even if they correspond to different transition-level pairs (e.g.
        // 'send_email from state2->state1' vs 'send_email from state3->state1'
        // — same action label, different state path). Under action-pair
        // coverage (the standard in the user's prior ESG-Fx work and the
        // semantically meaningful criterion for SUT testing), only one of
        // them is needed; the others are operationally redundant.
        // Transition-level uniqueness is preserved in the underlying FTS;
        // this dedup only drops surplus copies from the suite.
        List<TestCase> dedupedCases = dedupeByActionSequence(testCases);
        LOG.info("Generated pair-coverage suite for '{}': {} pair-graph edges -> "
                        + "{} test cases ({} after action-sequence dedup), {} total transitions",
                testCaseBaseId, pairCycle.size(), testCases.size(),
                dedupedCases.size(), totalTransitions);
        return dedupedCases;
    }

    /**
     * Drops TestCases whose action-name sequences duplicate an earlier
     * TestCase's. Preserves the first-occurrence ordering. Synthetic actions
     * (already-stripped __dup__ suffixes etc.) are normalised before comparing.
     */
    private static List<TestCase> dedupeByActionSequence(List<TestCase> raw) {
        List<TestCase> out = new ArrayList<>(raw.size());
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        for (TestCase tc : raw) {
            StringBuilder key = new StringBuilder();
            for (Transition t : tc) {
                if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                    continue;
                }
                String name = t.getAction().getName();
                if (name.contains(EulerianBalancer.DUPLICATE_ACTION_INFIX)) {
                    name = EulerianBalancer.stripDuplicateSuffix(name);
                }
                key.append(name).append("");
            }
            if (seenKeys.add(key.toString())) {
                out.add(tc);
            }
        }
        return out;
    }

    /**
     * Splits a pair-graph Euler cycle at every synthetic balancing edge.
     * Each returned sub-list is a maximal stretch of non-synthetic edges
     * (a "real segment") in their original cyclic order.
     */
    private static List<List<Transition>> splitAtSyntheticEdges(List<Transition> cycle) {
        List<List<Transition>> segments = new ArrayList<>();
        List<Transition> current = new ArrayList<>();
        for (Transition t : cycle) {
            if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                if (!current.isEmpty()) {
                    segments.add(current);
                    current = new ArrayList<>();
                }
            } else {
                current.add(t);
            }
        }
        if (!current.isEmpty()) {
            segments.add(current);
        }
        return segments;
    }

    /**
     * Translates a single (non-empty, synthetic-free) pair-graph cycle
     * segment back to a sequence of original (repaired) FTS transitions.
     *
     * <p>For each pair-graph edge in the segment, look up its target state
     * (a pair-graph state representing "we just executed original
     * transition X") in the side-map to find X.
     *
     * <p>Non-first segments are arrived at via a synthetic balancing
     * edge in the pair graph; the synthetic edge can land at ANY
     * pair-state, not only INIT. The pair represented by the segment's
     * first pair-graph edge ({@code (t_Y, t_X)} where {@code t_Y} owns
     * the segment's first source pair-state and {@code t_X} owns its
     * target) is therefore "split" across the synthetic teleport and
     * would otherwise not appear consecutively in any test case. To
     * preserve coverage we PREPEND {@code t_Y} to such a segment. The
     * resulting test case starts mid-FTS at {@code source(t_Y)}; that's
     * legal and replayable as long as the executor is reset between
     * test cases of the suite.
     *
     * <p>Re-resolves transition identity against {@code repaired} so the
     * resulting {@link Transition}s belong to the FTS instance the
     * caller will use for execution / coverage measurement.
     */
    private static List<Transition> translateSegment(List<Transition> segment,
                                                     FeaturedTransitionSystem repaired,
                                                     Map<State, Transition> sideMap) {
        List<Transition> result = new ArrayList<>(segment.size() + 1);

        // If the segment's first pair-graph edge starts at a real
        // pair-state p(t_Y) (i.e. not INIT), prepend t_Y so that the
        // pair (t_Y, t_X) carried by the first pair-graph edge appears
        // as a consecutive pair in the resulting test case.
        State firstSource = segment.get(0).getSource();
        Transition prefix = sideMap.get(firstSource);
        if (prefix != null) {
            result.add(findInRepaired(repaired, prefix));
        }

        for (Transition pairEdge : segment) {
            Transition originalTransition = sideMap.get(pairEdge.getTarget());
            if (originalTransition == null) {
                throw new IllegalStateException(
                        "Pair-graph target state " + pairEdge.getTarget().getName()
                                + " has no entry in the side-map; mapping is incomplete.");
            }
            Transition resolved = findInRepaired(repaired, originalTransition);
            result.add(resolved);
        }
        return result;
    }

    private static Transition findInRepaired(FeaturedTransitionSystem repaired,
                                             Transition reference) {
        State src = repaired.getState(reference.getSource().getName());
        State tgt = repaired.getState(reference.getTarget().getName());
        if (src == null || tgt == null) {
            return reference;
        }
        java.util.Iterator<Transition> it =
                repaired.getTransitions(src, repaired.getAction(reference.getAction().getName()), tgt);
        if (it.hasNext()) {
            return it.next();
        }
        return reference;
    }
}
