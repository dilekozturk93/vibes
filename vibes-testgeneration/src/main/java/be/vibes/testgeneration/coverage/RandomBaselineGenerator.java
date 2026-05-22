package be.vibes.testgeneration.coverage;

import be.vibes.dsl.selection.Random;
import be.vibes.ts.TestCase;
import be.vibes.ts.TestSet;
import be.vibes.ts.TransitionSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wraps VIBeS' {@code be.vibes.dsl.selection.Random.randomSelection}
 * into a List&lt;TestCase&gt;-returning generator with the same shape as
 * {@link StateCoverageGenerator}, {@link TransitionCoverageGenerator},
 * and {@link TransitionPairCoverageGenerator}. This provides the
 * "random baseline" suite for RQ2 against the coverage-directed
 * generators and is required by the Inozemtseva &amp; Holmes (2014)
 * equivalent-mutant treatment (a mutant not killed by ANY suite is
 * conservatively equivalent — random is one of the five suites in our
 * union).
 *
 * <p>Operates on the REPAIRED product-level FTS (post
 * FExpressionPreservingProjection + InitialSccFilter) so the random
 * walks are guaranteed strongly-connected and feature-feasible on the
 * product. This is consistent with how the coverage generators are
 * called.
 *
 * <p>Suite-size policy for paper fairness: by default we match the
 * number of test cases of the all-states baseline (paper convention,
 * Devroey 2014 Table 3 column "r-N" where N matches the all-states
 * suite size). Caller can override via the explicit-arity overload.
 */
public final class RandomBaselineGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(RandomBaselineGenerator.class);

    /** Default number of random test cases. Matches Devroey 2014 r-5 baseline for SVM. */
    public static final int DEFAULT_NBR_TEST_CASES = 5;

    /** Default max length per test case. Matches VIBeS' RandomTestCaseSelector default. */
    public static final int DEFAULT_MAX_LENGTH = 100;

    private RandomBaselineGenerator() {
    }

    /**
     * Generates a default-sized random baseline suite ({@value DEFAULT_NBR_TEST_CASES}
     * test cases, each up to {@value DEFAULT_MAX_LENGTH} transitions).
     */
    public static List<TestCase> generate(TransitionSystem ts, String idPrefix) {
        return generate(ts, idPrefix, DEFAULT_NBR_TEST_CASES, DEFAULT_MAX_LENGTH);
    }

    /**
     * Generates a random baseline suite with the given number of test
     * cases and max length per test case. Delegates to VIBeS'
     * {@link Random#randomSelection(TransitionSystem, int, int)} which
     * uses {@code RandomTestCaseSelector} under the hood — uniform
     * random next-transition pick at each state, no feature-model
     * validation (the caller has already projected onto a product).
     */
    public static List<TestCase> generate(TransitionSystem ts, String idPrefix,
                                          int nbrTestCases, int maxLength) {
        checkNotNull(ts, "TransitionSystem may not be null");
        checkNotNull(idPrefix, "ID prefix may not be null");
        TestSet set = Random.randomSelection(ts, nbrTestCases, maxLength);
        List<TestCase> out = new ArrayList<>(nbrTestCases);
        int i = 0;
        Iterator<TestCase> it = set.iterator();
        while (it.hasNext()) {
            // Re-label test cases with our id prefix so experiment logs
            // are reproducible across the four coverage generators.
            TestCase original = it.next();
            TestCase relabelled = new TestCase(idPrefix + "_random_" + i);
            try {
                for (be.vibes.ts.Transition t : original) {
                    relabelled.enqueue(t);
                }
            } catch (be.vibes.ts.exception.TransitionSystenExecutionException e) {
                LOG.warn("Could not relabel random test case (contiguity?): {}", e.getMessage());
                out.add(original);
                i++;
                continue;
            }
            out.add(relabelled);
            i++;
        }
        LOG.info("RandomBaselineGenerator: generated {} random test case(s) of max length {}",
                out.size(), maxLength);
        return out;
    }
}
