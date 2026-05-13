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

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates a single all-transitions test case for one product configuration.
 *
 * <p>Pipeline: SPL-level FTS + product configuration → projected FTS
 * (feature-expression-preserving) → balanced FTS (in-degree == out-degree
 * everywhere) → Euler cycle (Hierholzer) → {@link TestCase}.
 *
 * <p>The generated test case visits every original (non-synthetic) projected
 * transition at least once and returns to the initial state. Synthetic
 * balancing transitions added by {@link EulerianBalancer} also appear in
 * the cycle; downstream coverage measurement should exclude them via
 * {@link EulerianBalancer#isSyntheticAction(be.vibes.ts.Action)}.
 *
 * <p>For now the generator REQUIRES that the projected FTS is strongly
 * connected. The user's currently bundled SPLs (SVM, eMail, Elevator) and
 * the well-formed SPLs in the user's prior published study all satisfy this.
 * Should that ever fail in practice the generator will throw, and a
 * dedicated SCC-repair step will be slotted in here.
 */
public final class TransitionCoverageGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(TransitionCoverageGenerator.class);

    private TransitionCoverageGenerator() {
        // Utility class.
    }

    /**
     * Generates an all-transitions test case for the given product
     * configuration of the given FTS.
     *
     * @param fts the SPL-level FTS
     * @param product the product configuration to project onto
     * @param testCaseId stable identifier for the produced test case
     * @return a {@link TestCase} containing the Euler cycle of the
     *         projected + balanced product FTS
     */
    public static TestCase generate(FeaturedTransitionSystem fts,
                                    Configuration product,
                                    String testCaseId) {
        checkNotNull(fts, "FTS may not be null");
        checkNotNull(product, "Configuration may not be null");
        checkNotNull(testCaseId, "Test case id may not be null");

        FeaturedTransitionSystem projected =
                FExpressionPreservingProjection.project(fts, product);

        // SCC repair: a projected FTS is typically not strongly connected;
        // keep only the SCC containing the initial state. Whatever falls
        // outside that SCC is unreachable from / unable to return to the
        // initial state under the given product configuration, so it cannot
        // be part of any executable test sequence anyway.
        FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);
        if (!StronglyConnectedComponents.isStronglyConnected(repaired)) {
            throw new IllegalStateException(
                    "Post-repair FTS for testCase '" + testCaseId
                            + "' is still not strongly connected; this should be impossible.");
        }

        FeaturedTransitionSystem balanced = EulerianBalancer.balance(repaired);
        List<Transition> cycle = HierholzerEulerCycle.compute(balanced);

        // Wrap the cycle into an Execution-derived TestCase by enqueueing
        // each transition. Execution invariants are guaranteed by our cycle
        // closure validation, so the exception path below should never fire.
        TestCase testCase = new TestCase(testCaseId);
        try {
            testCase.enqueueAll(cycle);
        } catch (TransitionSystenExecutionException ex) {
            throw new IllegalStateException(
                    "Euler cycle could not be enqueued into TestCase '" + testCaseId
                            + "' — this should not happen given Hierholzer's contiguity invariant.",
                    ex);
        }
        LOG.info("Generated TestCase '{}': {} transitions ({} synthetic)",
                testCaseId, cycle.size(), countSynthetic(cycle));
        return testCase;
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
