package be.vibes.testgeneration.coverage;

import be.vibes.fexpression.configuration.Configuration;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.testgeneration.graph.HierholzerEulerCycle;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.graph.StronglyConnectedComponents;
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
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates an all-transitions test suite for one product configuration.
 *
 * <p>Pipeline: SPL-level FTS + product configuration → projected FTS
 * (feature-expression-preserving) → balanced FTS (in-degree == out-degree
 * everywhere) → Euler cycle (Hierholzer) → split into {@link TestCase}s
 * at every visit to the initial state.
 *
 * <p>The generated suite visits every original (non-synthetic) projected
 * transition at least once. Each {@link TestCase} is one round-trip from
 * the initial state back to the initial state — the operational unit a
 * test framework executes with a reset before and after.
 *
 * <p><strong>Why multi-TC, not single-TC (methodology fix 2026-05-24).</strong>
 * Earlier the generator returned the entire Euler cycle as a single
 * {@link TestCase} and a downstream renderer split it at initial-returns
 * <em>for display only</em>. Mutation-detection replay therefore reset
 * the executor only once at suite start, while pair coverage's multi-TC
 * suite reset between every TC. The asymmetry inflated transition-coverage
 * detection on the {@code TransitionDestinationExchange} operator (a TDE
 * mutant whose mutated transition has target = initial is caught mid-cycle
 * in single-TC replay because the next transition tries to fire from the
 * wrong state; in multi-TC replay the same mutated transition is the
 * final step of a TC, the reset wipes the divergence, and the mutant
 * survives — a false-positive 27pt detection advantage measured on SAS
 * in the pilot). Making the API return {@code List<TestCase>} aligns
 * replay semantic with the operational definition already used for
 * display, restoring methodologically fair comparison with pair coverage.
 *
 * <p>For now the generator REQUIRES that the projected FTS is strongly
 * connected. The user's currently bundled SPLs (SVM, eMail, Elevator,
 * BankAccountv2, SAS) all satisfy this. Should that fail, the generator
 * throws.
 */
public final class TransitionCoverageGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(TransitionCoverageGenerator.class);

    private TransitionCoverageGenerator() {
        // Utility class.
    }

    /**
     * Generates an all-transitions test suite for the given product
     * configuration of the given FTS.
     *
     * @param fts the SPL-level FTS
     * @param product the product configuration to project onto
     * @param testCaseBaseId stable identifier prefix; per-trip ids are
     *         {@code <base>_trip<i>}
     * @return a list of {@link TestCase}s — one per initial-return trip
     *         in the Euler cycle of the projected + balanced product FTS
     */
    public static List<TestCase> generate(FeaturedTransitionSystem fts,
                                          Configuration product,
                                          String testCaseBaseId) {
        checkNotNull(fts, "FTS may not be null");
        checkNotNull(product, "Configuration may not be null");
        checkNotNull(testCaseBaseId, "Test case base id may not be null");

        FeaturedTransitionSystem projected =
                FExpressionPreservingProjection.project(fts, product);

        // SCC repair: a projected FTS is typically not strongly connected;
        // keep only the SCC containing the initial state.
        FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);
        if (!StronglyConnectedComponents.isStronglyConnected(repaired)) {
            throw new IllegalStateException(
                    "Post-repair FTS for suite '" + testCaseBaseId
                            + "' is still not strongly connected; this should be impossible.");
        }

        FeaturedTransitionSystem balanced = EulerianBalancer.balance(repaired);
        List<Transition> cycle = HierholzerEulerCycle.compute(balanced);

        // Wrap the cycle into a transient single TestCase so we can reuse
        // TestCaseSplitter, then split at initial-returns into the
        // operational multi-TC suite.
        TestCase fullCycle = new TestCase(testCaseBaseId + "_cycle");
        try {
            fullCycle.enqueueAll(cycle);
        } catch (TransitionSystenExecutionException ex) {
            throw new IllegalStateException(
                    "Euler cycle could not be enqueued into transient TestCase '"
                            + testCaseBaseId + "_cycle' — Hierholzer contiguity violated?",
                    ex);
        }
        List<List<Transition>> trips =
                TestCaseSplitter.splitAtInitialReturns(fullCycle, repaired.getInitialState());

        List<TestCase> suite = new ArrayList<>(trips.size());
        int idx = 0;
        for (List<Transition> trip : trips) {
            if (trip.isEmpty()) {
                idx++;
                continue;
            }
            TestCase tc = new TestCase(testCaseBaseId + "_trip" + idx);
            try {
                tc.enqueueAll(trip);
            } catch (TransitionSystenExecutionException ex) {
                throw new IllegalStateException(
                        "Trip " + idx + " could not be enqueued for suite '"
                                + testCaseBaseId + "' — contiguity broken at split?",
                        ex);
            }
            suite.add(tc);
            idx++;
        }
        LOG.info("Generated transition-coverage suite '{}': {} TestCase(s), "
                        + "{} cycle transitions ({} synthetic)",
                testCaseBaseId, suite.size(), cycle.size(), countSynthetic(cycle));
        return suite;
    }

    /**
     * Returns the set of synthetic-action names present in the test case.
     * Caller-visible so downstream coverage measurement can subtract their
     * contribution from the denominator.
     */
    public static int countSynthetic(List<Transition> cycle) {
        int n = 0;
        for (Transition t : cycle) {
            if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                n++;
            }
        }
        return n;
    }

    private static int countStates(FeaturedTransitionSystem fts) {
        int n = 0;
        java.util.Iterator<State> it = fts.states();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    @SuppressWarnings("unused")
    private static int sccSize(Set<State> scc) {
        return scc == null ? 0 : scc.size();
    }
}
