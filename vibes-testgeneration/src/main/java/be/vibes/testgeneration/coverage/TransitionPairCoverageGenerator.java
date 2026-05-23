package be.vibes.testgeneration.coverage;

import be.vibes.fexpression.configuration.Configuration;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.testgeneration.graph.HierholzerEulerCycle;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.testgeneration.product.TestCaseSplitter;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import be.vibes.ts.exception.TransitionSystenExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates an all-transition-pairs test suite for one product configuration.
 *
 * <p>Pipeline (INIT-less variant, 2026-05-23):
 * <ol>
 *   <li>{@link FExpressionPreservingProjection#project} — project SPL FTS
 *       onto the configuration.</li>
 *   <li>{@link InitialSccFilter#keepInitialScc} — drop states that cannot
 *       return to the FTS initial state.</li>
 *   <li>{@link PairGraphTransformer#transform} — build the INIT-less pair
 *       graph (vertex per non-synthetic FTS transition, edges for
 *       contiguous pairs, canonical initial pair-vertex chosen as the
 *       lexicographically-smallest {@code p(t_init)}).</li>
 *   <li>{@link EulerianBalancer#balanceWithoutPrecheck} — pair imbalanced
 *       vertices via Chinese-Postman shortest-path doublings
 *       ({@code __dup__N}). When no real path exists between an excessOut
 *       and excessIn pair (e.g. across an SCC disconnect produced by
 *       {@code __end__} filtering — known residual issue), fall through
 *       to a direct synthetic {@code __balance__N} edge.</li>
 *   <li>{@link HierholzerEulerCycle#compute} — extract a single Euler
 *       cycle that visits every pair-graph edge exactly once.</li>
 *   <li>Translate the cycle to an FTS action sequence: PREPEND the
 *       canonical start's underlying transition (so the test case starts
 *       at the FTS initial state and covers the
 *       (start_vertex_transition, first_emitted_action) pair), then for
 *       each cycle edge emit the underlying transition pointed to by the
 *       edge's target pair-vertex. {@code __dup__N} edges' targets
 *       resolve naturally to the underlying real transition (the
 *       suffixed and un-suffixed pair-edges share a target pair-vertex).
 *       {@code __balance__N} edges have no underlying transition and
 *       are dropped — they leave a discontinuity in the action sequence
 *       at the bridge point.</li>
 *   <li>{@link TestCaseSplitter#splitAtInitialReturns} — split the
 *       single concatenated TestCase at every visit to the FTS initial
 *       state, producing one trip-shaped TestCase per round-trip.
 *       Trips after a state-1 return are guaranteed to start at the FTS
 *       initial state and therefore executable end-to-end. Trips
 *       immediately after a dropped {@code __balance__N} (the rare
 *       SCC-disconnect case) may begin mid-FTS — this is the documented
 *       Threats-to-Validity residual.</li>
 *   <li>Optional dedup: TestCases whose action-name sequences are
 *       identical to a previously-emitted TestCase are dropped. The
 *       underlying pair coverage of the suite is preserved (the
 *       deduplicated copies cover the same pairs).</li>
 * </ol>
 *
 * <p>Each pair-graph edge corresponds to one contiguous transition pair
 * in the original FTS, so the resulting suite covers every reachable
 * transition pair at least once.
 */
public final class TransitionPairCoverageGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(TransitionPairCoverageGenerator.class);

    private TransitionPairCoverageGenerator() {
        // Utility class.
    }

    /**
     * Generates an all-transition-pairs test SUITE for the given product
     * configuration.
     */
    public static List<TestCase> generate(FeaturedTransitionSystem fts,
                                          Configuration product,
                                          String testCaseBaseId) {
        return generateWithTimings(fts, product, testCaseBaseId, null);
    }

    /**
     * Per-sub-phase timings produced by
     * {@link #generateWithTimings(FeaturedTransitionSystem, Configuration, String, Timings)}.
     * Disjoint segments — their sum exactly equals the wall-clock time
     * of the method.
     */
    public static final class Timings {
        public long projectionAndRepairNanos;
        public long pairGraphConstructionNanos;
        public long balancingNanos;
        public long hierholzerEulerNanos;
        public long translationAndDedupeNanos;

        /**
         * "Transformation" in the RQ1 sense — everything between
         * pair-graph construction and the test cases: balancing,
         * Hierholzer Euler cycle, and the translation + split + dedup.
         * Disjoint from {@link #pairGraphConstructionNanos}.
         */
        public long transformationNanos() {
            return balancingNanos + hierholzerEulerNanos + translationAndDedupeNanos;
        }

        public long testGenTotalNanos() {
            return projectionAndRepairNanos + pairGraphConstructionNanos
                    + balancingNanos + hierholzerEulerNanos
                    + translationAndDedupeNanos;
        }
    }

    /**
     * Same as {@link #generate} but, if {@code timings} is non-null,
     * records per-sub-phase nanosecond timings into it.
     */
    public static List<TestCase> generateWithTimings(FeaturedTransitionSystem fts,
                                                     Configuration product,
                                                     String testCaseBaseId,
                                                     Timings timings) {
        checkNotNull(fts, "FTS may not be null");
        checkNotNull(product, "Configuration may not be null");
        checkNotNull(testCaseBaseId, "Test case base id may not be null");

        long t0 = System.nanoTime();
        FeaturedTransitionSystem projected =
                FExpressionPreservingProjection.project(fts, product);
        FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);
        long t1 = System.nanoTime();
        if (timings != null) timings.projectionAndRepairNanos = t1 - t0;

        long t2 = System.nanoTime();
        PairGraphTransformer.Result pgResult = PairGraphTransformer.transform(repaired);
        long t3 = System.nanoTime();
        if (timings != null) timings.pairGraphConstructionNanos = t3 - t2;

        long t4 = System.nanoTime();
        FeaturedTransitionSystem pairGraphBalanced =
                EulerianBalancer.balanceWithoutPrecheck(pgResult.pairGraph);
        long t5 = System.nanoTime();
        if (timings != null) timings.balancingNanos = t5 - t4;

        long t6 = System.nanoTime();
        List<Transition> pairCycle = HierholzerEulerCycle.compute(pairGraphBalanced);
        long t7 = System.nanoTime();
        if (timings != null) timings.hierholzerEulerNanos = t7 - t6;

        // ---- Translate pair-cycle to trips, inline ----
        long t8 = System.nanoTime();
        State canonicalStart = pairGraphBalanced.getInitialState();
        // Resolve the canonical start back to the side-map. The balanced
        // graph carries the same state-name set as the pre-balance pair
        // graph, so we look up by name.
        State canonicalStartInPg = pgResult.pairGraph.getState(canonicalStart.getName());
        Transition prefixTransition =
                pgResult.pairStateToOriginalTransition.get(canonicalStartInPg);
        if (prefixTransition == null) {
            throw new IllegalStateException(
                    "Canonical start pair-vertex '" + canonicalStart.getName()
                            + "' has no entry in the side-map; PairGraphTransformer mapping is incomplete.");
        }
        State ftsInitial = repaired.getInitialState();

        // Trip-building loop. Two trip-boundary triggers:
        //   (a) Natural: the current transition's target is the FTS initial
        //       state (a tester would reset here for the next test case).
        //   (b) Discontinuity: a __balance__N edge in the cycle was just
        //       skipped, so the next emission's source is not the previous
        //       emission's target. We must close the current trip and start
        //       the next one mid-FTS — that mid-FTS trip is the known
        //       Threats-to-Validity residual on SPLs whose pair graph has
        //       SCC disconnects under __end__ filtering.
        List<TestCase> testCases = new ArrayList<>();
        int tripCounter = 0;
        TestCase currentTrip = new TestCase(testCaseBaseId + "_trip" + tripCounter);
        boolean discontinuityPending = false;
        int balanceSkipped = 0;
        int midFtsTrips = 0;
        try {
            Transition prefixResolved = findInRepaired(repaired, prefixTransition);
            currentTrip.enqueue(prefixResolved);
            if (prefixResolved.getTarget().equals(ftsInitial)) {
                testCases.add(currentTrip);
                tripCounter++;
                currentTrip = new TestCase(testCaseBaseId + "_trip" + tripCounter);
            }
            for (Transition pairEdge : pairCycle) {
                String label = pairEdge.getAction().getName();
                if (label.startsWith(EulerianBalancer.SYNTHETIC_ACTION_PREFIX)) {
                    balanceSkipped++;
                    if (!isEmptyTc(currentTrip)) {
                        testCases.add(currentTrip);
                        tripCounter++;
                        currentTrip = new TestCase(testCaseBaseId + "_trip" + tripCounter);
                    }
                    discontinuityPending = true;
                    continue;
                }
                Transition underlying = pgResult.pairStateToOriginalTransition.get(pairEdge.getTarget());
                if (underlying == null) {
                    throw new IllegalStateException(
                            "Pair-graph target state " + pairEdge.getTarget().getName()
                                    + " has no entry in the side-map; mapping is incomplete.");
                }
                Transition resolved = findInRepaired(repaired, underlying);
                if (discontinuityPending) {
                    // First transition of a post-__balance__ trip. It
                    // starts wherever the __balance__'s target pair-vertex
                    // represents — likely mid-FTS, not necessarily at the
                    // initial state.
                    if (!resolved.getSource().equals(ftsInitial)) {
                        midFtsTrips++;
                    }
                    discontinuityPending = false;
                }
                currentTrip.enqueue(resolved);
                if (resolved.getTarget().equals(ftsInitial)) {
                    testCases.add(currentTrip);
                    tripCounter++;
                    currentTrip = new TestCase(testCaseBaseId + "_trip" + tripCounter);
                }
            }
            if (!isEmptyTc(currentTrip)) {
                testCases.add(currentTrip);
            }
        } catch (TransitionSystenExecutionException ex) {
            throw new IllegalStateException(
                    "Translated pair-graph cycle could not be enqueued into trip '"
                            + currentTrip.getId() + "': " + ex.getMessage(), ex);
        }
        if (midFtsTrips > 0) {
            LOG.warn("Pair-coverage suite for '{}': {} trip(s) begin mid-FTS "
                            + "(after __balance__N discontinuity) — known residual on SPLs whose "
                            + "pair graph has SCC disconnects under __end__ filtering",
                    testCaseBaseId, midFtsTrips);
        }
        LOG.debug("Pair cycle translated for '{}': prefix + {} cycle edges "
                        + "(of which {} __balance__ skipped) -> {} trips ({} mid-FTS)",
                testCaseBaseId, pairCycle.size(), balanceSkipped, testCases.size(), midFtsTrips);

        // ---- Dedup by action sequence ----
        List<TestCase> deduped = dedupeByActionSequence(testCases);
        long t9 = System.nanoTime();
        if (timings != null) timings.translationAndDedupeNanos = t9 - t8;

        LOG.info("Pair-coverage suite for '{}': cycle {} edges (incl prefix), "
                        + "{} trips before dedup, {} after dedup",
                testCaseBaseId, pairCycle.size() + 1, testCases.size(), deduped.size());
        return deduped;
    }

    /**
     * Drops TestCases whose action-name sequences duplicate an earlier
     * TestCase's. Preserves first-occurrence ordering. Synthetic-action
     * prefixes are normalised before comparing so
     * {@code action__dup__N} compares as {@code action}.
     */
    private static List<TestCase> dedupeByActionSequence(List<TestCase> raw) {
        List<TestCase> out = new ArrayList<>(raw.size());
        Set<String> seen = new HashSet<>();
        for (TestCase tc : raw) {
            StringBuilder key = new StringBuilder();
            for (Transition t : tc) {
                String name = t.getAction().getName();
                if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                    if (name.contains(EulerianBalancer.DUPLICATE_ACTION_INFIX)) {
                        name = EulerianBalancer.stripDuplicateSuffix(name);
                    } else {
                        // pure synthetic (__balance__N, __end__): ignore
                        continue;
                    }
                }
                key.append(name).append('');
            }
            if (seen.add(key.toString())) {
                out.add(tc);
            }
        }
        return out;
    }

    /**
     * Resolves a reference Transition back to the equivalent instance in
     * {@code repaired} so the TestCase carries Transition objects owned
     * by the FTS the caller will use for execution / coverage measurement.
     */
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

    private static boolean isEmptyTc(TestCase tc) {
        return !tc.iterator().hasNext();
    }
}
