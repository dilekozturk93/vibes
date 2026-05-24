package be.vibes.testgeneration.coverage;

import be.vibes.fexpression.configuration.Configuration;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.testgeneration.graph.HierholzerEulerCycle;
import be.vibes.testgeneration.graph.InitialSccFilter;
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
 * Generates an all-transition-pairs test suite for one product configuration.
 *
 * <p>Pipeline (INIT-less, __end__-aware variant, 2026-05-24):
 * <ol>
 *   <li>{@link FExpressionPreservingProjection#project} — project SPL FTS
 *       onto the configuration.</li>
 *   <li>{@link InitialSccFilter#keepInitialScc} — drop states that cannot
 *       return to the FTS initial state.</li>
 *   <li>{@link PairGraphTransformer#transform} — build the INIT-less pair
 *       graph. Vertices include {@code __end__} transitions (the
 *       MxeToFtsConverter back-to-INIT rewiring for mixed-terminal ESG
 *       vertices); only balancer artefacts ({@code __balance__N},
 *       {@code __dup__N}) are filtered. Canonical initial pair-vertex is
 *       the lexicographically-smallest {@code p(t_init)}.</li>
 *   <li>{@link EulerianBalancer#balanceWithoutPrecheck} — pair imbalanced
 *       vertices via Chinese-Postman shortest-path doublings
 *       ({@code __dup__N}). With {@code __end__} now in the pair graph,
 *       every "returns-to-initial" pair-vertex has at least one outgoing
 *       edge (to some {@code p(t_initial_outgoing)}) and every
 *       "initial-outgoing" pair-vertex has at least one incoming edge
 *       (from some {@code p(t_*_end)}), so the structural imbalance that
 *       previously forced {@code __balance__N} bridges on
 *       all-mixed-terminal SPLs (Elevator) disappears.</li>
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
 *   <li>Split the action sequence at every FTS-initial return, producing
 *       one TestCase per round-trip. Trips are bounded by initial-return
 *       transitions ONLY — including {@code __end__} (whose target is
 *       always the FTS initial state), which is therefore a natural
 *       split point. Mid-trip {@code __balance__N} discontinuities are
 *       extremely rare now that {@code __end__} is in the pair graph;
 *       any residual is sub-split LOCALLY per the VIBeS TestCase
 *       contiguity invariant. Sub-splits are counted separately and
 *       logged at WARN.</li>
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

        // ---- Translate pair-cycle to a flat action sequence, then split ----
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

        // Phase 1 — build the flat action sequence as List<Transition>
        // (bypasses TestCase contiguity check). __balance__N edges contribute
        // NO entry, leaving a discontinuity in the sequence at the bridge
        // point. __dup__N edges contribute their underlying real transition
        // (target pair-vertex resolves via the side-map to the underlying
        // transition, suffix is irrelevant for execution). __end__ edges
        // contribute the __end__ transition itself — a real FTS transition
        // whose target is the initial state, which Phase 2 then uses as a
        // trip-boundary split point.
        List<Transition> sequence = new ArrayList<>(pairCycle.size() + 1);
        sequence.add(findInRepaired(repaired, prefixTransition));
        int balanceSkipped = 0;
        for (Transition pairEdge : pairCycle) {
            String label = pairEdge.getAction().getName();
            if (label.startsWith(EulerianBalancer.SYNTHETIC_ACTION_PREFIX)) {
                balanceSkipped++;
                continue;
            }
            Transition underlying = pgResult.pairStateToOriginalTransition.get(pairEdge.getTarget());
            if (underlying == null) {
                throw new IllegalStateException(
                        "Pair-graph target state " + pairEdge.getTarget().getName()
                                + " has no entry in the side-map; mapping is incomplete.");
            }
            sequence.add(findInRepaired(repaired, underlying));
        }

        // Phase 2 — split at FTS-initial returns ONLY (splitAtInitialReturns
        // logic, applied directly to the List<Transition>). This is the
        // spec-faithful trip boundary: a trip is a closed walk from initial
        // back to initial, ending with the transition that returned the
        // walk to initial.
        List<List<Transition>> trips = new ArrayList<>();
        List<Transition> current = new ArrayList<>();
        for (Transition t : sequence) {
            current.add(t);
            if (t.getTarget().equals(ftsInitial)) {
                trips.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            trips.add(current); // incomplete final trip
        }

        // Phase 3 — wrap each trip in a TestCase. TestCase.enqueue enforces
        // source/target contiguity; with __end__ now in the pair graph,
        // the only remaining source of mid-trip __balance__N discontinuity
        // is a pair-graph SCC partition unrelated to mixed-terminal
        // structure (extremely rare; not yet observed on the 5 evaluated
        // SPLs). If a discontinuity does land mid-trip, the TestCase
        // invariant requires source/target contiguity, so we sub-split
        // locally. Sub-splits are counted separately so any residual is
        // observable.
        List<TestCase> testCases = new ArrayList<>(trips.size());
        int tripIdx = 0;
        int contiguitySubsplits = 0;
        int midFtsSubsplitTrips = 0;
        for (List<Transition> trip : trips) {
            if (trip.isEmpty()) {
                tripIdx++;
                continue;
            }
            int subIdx = 0;
            TestCase tc = new TestCase(tripId(testCaseBaseId, tripIdx, subIdx, false));
            Transition previous = null;
            try {
                for (Transition t : trip) {
                    if (previous != null && !t.getSource().equals(previous.getTarget())) {
                        // Sub-split forced by TestCase contiguity invariant.
                        // Close current sub-trip, open a new one.
                        if (!isEmptyTc(tc)) {
                            testCases.add(tc);
                        }
                        subIdx++;
                        contiguitySubsplits++;
                        if (!t.getSource().equals(ftsInitial)) {
                            midFtsSubsplitTrips++;
                        }
                        tc = new TestCase(tripId(testCaseBaseId, tripIdx, subIdx, true));
                    }
                    tc.enqueue(t);
                    previous = t;
                }
            } catch (TransitionSystenExecutionException ex) {
                throw new IllegalStateException(
                        "Unexpected contiguity violation building trip '" + tc.getId()
                                + "' (TestCase invariant) on transition with source="
                                + (previous == null ? "<first>" : previous.getTarget().getName())
                                + ": " + ex.getMessage(), ex);
            }
            if (!isEmptyTc(tc)) {
                testCases.add(tc);
            }
            tripIdx++;
        }
        if (contiguitySubsplits > 0) {
            LOG.warn("Pair-coverage suite for '{}': {} contiguity sub-split(s) "
                            + "({} of which begin mid-FTS) — TestCase invariant forced sub-splitting "
                            + "at __balance__N discontinuities mid-trip. Known residual on SPLs "
                            + "whose pair graph has SCC disconnects under __end__ filtering.",
                    testCaseBaseId, contiguitySubsplits, midFtsSubsplitTrips);
        }
        LOG.debug("Pair cycle translated for '{}': prefix + {} cycle edges "
                        + "(of which {} __balance__ skipped) -> {} trips ({} contiguity sub-splits)",
                testCaseBaseId, pairCycle.size(), balanceSkipped,
                testCases.size(), contiguitySubsplits);

        // No dedup. Every Hierholzer cycle visit is unique by cycle
        // POSITION even when two trips share the same action sequence,
        // and the cross-TC boundary pair (TC_k_last, TC_{k+1}_first)
        // depends on suite ORDER — dropping a duplicate-action-sequence
        // trip would also drop the (one and only) boundary pair adjacent
        // to it. Pair coverage measurement is sensitive to those
        // boundaries, so the suite carries every trip the cycle produced.
        long t9 = System.nanoTime();
        if (timings != null) timings.translationAndDedupeNanos = t9 - t8;

        LOG.info("Pair-coverage suite for '{}': cycle {} edges (incl prefix), {} trips",
                testCaseBaseId, pairCycle.size() + 1, testCases.size());
        return testCases;
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

    /**
     * TestCase id with optional "_sub" suffix for contiguity-forced
     * sub-splits. {@code subIdx == 0 && !forceSub} produces the base id
     * (no suffix); positive {@code subIdx} or {@code forceSub} adds the
     * sub-index suffix so traces can identify mid-trip sub-splits.
     */
    private static String tripId(String baseId, int tripIdx, int subIdx, boolean forceSub) {
        String base = baseId + "_trip" + tripIdx;
        if (subIdx == 0 && !forceSub) {
            return base;
        }
        return base + "_sub" + subIdx;
    }
}
