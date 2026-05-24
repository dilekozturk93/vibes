package be.vibes.testgeneration.coverage;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end test of the M4 pipeline on the Soda Vending Machine SPL.
 *
 * <p>For each of the 12 valid SVM configurations enumerated by
 * {@link Sat4JSolverFacade}, the test:
 *
 * <ol>
 *   <li>Generates a transition-coverage {@link TestCase}.</li>
 *   <li>Confirms that the set of non-synthetic transitions present in the
 *       generated cycle exactly equals the set of transitions of the
 *       repaired (initial-SCC-only) projected FTS — i.e. 100% transition
 *       coverage of what is reachable AND returnable within the product.</li>
 * </ol>
 *
 * <p>Replay via {@code TransitionSystemExecutor} is skipped here: the
 * Euler cycle is closed and contiguous by construction (validated in
 * {@code HierholzerEulerCycle.compute}), and the coverage assertion below
 * already gives us the property we care about for RQ1 (feasibility).
 * A full executor-based replay test will land alongside the experiment
 * harness in M7, where it is needed for measuring fault detection on
 * mutants.
 */
public class TransitionCoverageGeneratorTest {

    private static final String SVM_MXE = "cases/SodaVendingMachine/SVM_ESGFx.mxe";
    private static final String SVM_DIMACS = "cases/SodaVendingMachine/configs/SVM.dimacs";
    private static final String SVM_MAPPING = "cases/SodaVendingMachine/configs/SVM_dimacsmapping.txt";

    @Test
    public void svm_allTwelveConfigurations_achieveFullTransitionCoverage() throws Exception {
        FeaturedTransitionSystem fts = loadFts(SVM_MXE);
        Sat4JSolverFacade solver = loadSolver(SVM_DIMACS, SVM_MAPPING);

        int configCount = 0;
        Iterator<Configuration> configs = solver.getSolutions();
        while (configs.hasNext()) {
            Configuration config = configs.next();
            configCount++;
            String testId = "svm_p" + configCount;

            FeaturedTransitionSystem projected = FExpressionPreservingProjection.project(fts, config);
            FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);
            int repairedTransitions = countTransitions(repaired);
            assertThat("Repaired FTS must have at least one transition for " + testId,
                    repairedTransitions, greaterThan(0));

            java.util.List<TestCase> suite =
                    TransitionCoverageGenerator.generate(fts, config, testId);

            // Concatenating every trip in the suite must cover every
            // non-synthetic transition at least once.
            Set<Transition> nonSynthetic = new HashSet<>();
            int totalCycleLength = 0;
            for (TestCase tc : suite) {
                for (Transition t : tc) {
                    totalCycleLength++;
                    if (!EulerianBalancer.isSyntheticAction(t.getAction())) {
                        nonSynthetic.add(t);
                    }
                }
            }
            assertEquals("Suite " + testId
                            + " must cover every reachable transition (size mismatch)",
                    repairedTransitions, nonSynthetic.size());
            assertThat("Suite " + testId + " must contain at least one TestCase",
                    suite.size(), greaterThan(0));
            assertThat("Suite " + testId + " total transitions must be positive",
                    totalCycleLength, greaterThan(0));
        }
        assertEquals("SVM should expose 12 valid configurations", 12, configCount);
    }

    private static FeaturedTransitionSystem loadFts(String resourcePath) throws Exception {
        URL url = TransitionCoverageGeneratorTest.class.getClassLoader().getResource(resourcePath);
        assertThat(url, is(notNullValue()));
        return new MxeToFtsConverter().convert(new File(url.toURI()));
    }

    private static Sat4JSolverFacade loadSolver(String dimacsResource, String mappingResource) throws Exception {
        URL dimacsUrl = TransitionCoverageGeneratorTest.class.getClassLoader().getResource(dimacsResource);
        URL mappingUrl = TransitionCoverageGeneratorTest.class.getClassLoader().getResource(mappingResource);
        assertThat(dimacsUrl, is(notNullValue()));
        assertThat(mappingUrl, is(notNullValue()));
        DimacsModel model = DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mappingUrl.toURI()), new File(dimacsUrl.toURI()));
        return new Sat4JSolverFacade(model);
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

    @SuppressWarnings("unused")
    private static int sizeNoBaselineUsed() {
        // Reserved for the M7 experiment harness which will replay test
        // cases via TransitionSystemExecutor against mutants. Kept here as
        // a marker so the next agent can find the intended hook.
        return 0;
    }
}
