package be.vibes.testgeneration.coverage;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.FExpression;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.FeaturedTransitionSystemFactory;
import be.vibes.ts.State;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransitionPairCoverageGeneratorTest {

    private static final String SVM_MXE = "cases/SodaVendingMachine/SVM_ESGFx.mxe";
    private static final String SVM_DIMACS = "cases/SodaVendingMachine/configs/SVM.dimacs";
    private static final String SVM_MAPPING = "cases/SodaVendingMachine/configs/SVM_dimacsmapping.txt";

    // ---- PairGraphTransformer structural properties ----

    @Test
    public void pairGraphTransform_twoStateLoop_hasExpectedStructure() {
        // Two-state loop: a -[ab]-> b -[ba]-> a. Original has 2 transitions.
        // INIT-less pair graph (2026-05-23): 2 vertices (one per transition),
        // edges:
        //   p(ab) -> p(ba)  (action ba)
        //   p(ba) -> p(ab)  (action ab)
        // Total 2 pair-graph edges. Canonical initial state =
        // lexicographically-smallest p(t_init) for t_init.source = "a";
        // here only "a -[ab]-> b" qualifies → "p_a_ab_b".
        FeaturedTransitionSystem fts = twoStateLoop();
        PairGraphTransformer.Result r = PairGraphTransformer.transform(fts);

        assertEquals("pair-graph state count = |original T| (no separate INIT)",
                2, countStates(r.pairGraph));
        assertEquals("pair-graph edge count = 2 (p(ab)->p(ba), p(ba)->p(ab))",
                2, countTransitions(r.pairGraph));
        assertEquals("p_a_ab_b", r.initialStateName);
    }

    @Test
    public void pairGraphTransform_svm_hasExpectedCardinality() throws Exception {
        // Original SVM family FTS has E transitions and S states. INIT-less
        // pair graph has E states (one per non-synthetic transition) and the
        // number of pair-graph edges equals
        //   sum over t1 of |{t2 : source(t2) == target(t1)}|   (pair edges)
        // (no INIT-outgoing edges any more).
        FeaturedTransitionSystem svm = loadSvm();
        int e = countTransitions(svm);

        PairGraphTransformer.Result r = PairGraphTransformer.transform(svm);
        assertEquals(e, countStates(r.pairGraph));

        // Edge count matches the formula computed structurally below.
        int expectedPairEdges = expectedPairEdgeCount(svm);
        assertEquals(expectedPairEdges, countTransitions(r.pairGraph));
    }

    @Test
    public void pairGraphTransform_sideMap_pointsToOriginalTransitions() {
        FeaturedTransitionSystem fts = twoStateLoop();
        PairGraphTransformer.Result r = PairGraphTransformer.transform(fts);

        Set<Transition> originalTransitions = new HashSet<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            originalTransitions.add(it.next());
        }
        assertEquals("side-map must contain one entry per original transition",
                originalTransitions.size(), r.pairStateToOriginalTransition.size());
        for (Transition t : r.pairStateToOriginalTransition.values()) {
            assertTrue("side-map values must be original-FTS transitions",
                    originalTransitions.contains(t));
        }
    }

    // ---- End-to-end on SVM ----

    @Test
    public void svm_pairCoverage_isGeneratedForEveryConfiguration() throws Exception {
        FeaturedTransitionSystem fts = loadSvm();
        Sat4JSolverFacade solver = loadSolver();

        int configCount = 0;
        Iterator<Configuration> configs = solver.getSolutions();
        while (configs.hasNext()) {
            Configuration config = configs.next();
            configCount++;
            String testId = "svm_p" + configCount;

            List<TestCase> suite = TransitionPairCoverageGenerator.generate(fts, config, testId);
            assertThat("Pair-coverage suite " + testId + " must contain at least one test case",
                    suite.size(), greaterThan(0));
            int totalLen = 0;
            for (TestCase tc : suite) {
                totalLen += toList(tc).size();
            }
            assertThat("Pair-coverage suite " + testId + " total transitions",
                    totalLen, greaterThan(0));
        }
        assertEquals(12, configCount);
    }

    @Test
    public void svm_pairCoverage_coversEveryReachableTransitionPair() throws Exception {
        FeaturedTransitionSystem fts = loadSvm();
        Sat4JSolverFacade solver = loadSolver();

        int configCount = 0;
        Iterator<Configuration> configs = solver.getSolutions();
        while (configs.hasNext()) {
            Configuration config = configs.next();
            configCount++;
            String testId = "svm_p" + configCount;

            FeaturedTransitionSystem projected = FExpressionPreservingProjection.project(fts, config);
            FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);

            Set<TransitionPairKey> expectedPairs = enumerateReachablePairs(repaired);

            // The INIT-less suite is split at FTS-initial returns
            // (TestCaseSplitter.splitAtInitialReturns inside the
            // generator). Boundary pairs — (last action of trip k,
            // first action of trip k+1) — are real operationally-
            // covered pairs (after reset, the tester executes the next
            // trip starting at initial), so pair coverage of the SUITE
            // is computed by concatenating all trips and counting
            // consecutive pairs in the flattened sequence.
            List<TestCase> suite = TransitionPairCoverageGenerator.generate(fts, config, testId);
            java.util.List<Transition> flattened = new java.util.ArrayList<>();
            for (TestCase tc : suite) flattened.addAll(toList(tc));
            Set<TransitionPairKey> coveredPairs = consecutivePairs(flattened);

            for (TransitionPairKey expected : expectedPairs) {
                assertTrue("Test " + testId + " suite must cover pair "
                                + expected.first + " -> " + expected.second,
                        coveredPairs.contains(expected));
            }
        }
        assertEquals(12, configCount);
    }

    @Test
    public void svm_pairCoverage_totalLengthIsAtLeastTransitionCoverage() throws Exception {
        // The pair-coverage suite visits every contiguous transition pair,
        // which is a stricter property than visiting every transition.
        // The TOTAL length of the suite should therefore be >= the
        // all-transitions Euler cycle length on average across all 12
        // SVM configurations.
        FeaturedTransitionSystem fts = loadSvm();
        Sat4JSolverFacade solver = loadSolver();

        int configCount = 0;
        int totalPair = 0;
        int totalTrans = 0;
        Iterator<Configuration> configs = solver.getSolutions();
        while (configs.hasNext()) {
            Configuration config = configs.next();
            configCount++;
            String testId = "svm_p" + configCount;

            List<TestCase> pairSuite =
                    TransitionPairCoverageGenerator.generate(fts, config, testId + "_pair");
            int pairLen = 0;
            for (TestCase tc : pairSuite) {
                pairLen += toList(tc).size();
            }
            TestCase transTc =
                    TransitionCoverageGenerator.generate(fts, config, testId + "_trans");
            totalPair += pairLen;
            totalTrans += toList(transTc).size();
        }
        assertEquals(12, configCount);
        assertTrue("Average pair-coverage total length should be >= average transition-coverage length "
                        + "(avg pair=" + (totalPair / configCount)
                        + ", avg trans=" + (totalTrans / configCount) + ")",
                totalPair >= totalTrans);
    }

    // ---- helpers ----

    private static FeaturedTransitionSystem twoStateLoop() {
        FeaturedTransitionSystemFactory factory = new FeaturedTransitionSystemFactory("a");
        factory.addState("b");
        factory.addAction("ab");
        factory.addAction("ba");
        factory.addTransition("a", "ab", FExpression.trueValue(), "b");
        factory.addTransition("b", "ba", FExpression.trueValue(), "a");
        return factory.build();
    }

    private static FeaturedTransitionSystem loadSvm() throws Exception {
        URL url = TransitionPairCoverageGeneratorTest.class.getClassLoader().getResource(SVM_MXE);
        assertThat(url, is(notNullValue()));
        return new MxeToFtsConverter().convert(new File(url.toURI()));
    }

    private static Sat4JSolverFacade loadSolver() throws Exception {
        URL dimacsUrl = TransitionPairCoverageGeneratorTest.class.getClassLoader().getResource(SVM_DIMACS);
        URL mappingUrl = TransitionPairCoverageGeneratorTest.class.getClassLoader().getResource(SVM_MAPPING);
        assertThat(dimacsUrl, is(notNullValue()));
        assertThat(mappingUrl, is(notNullValue()));
        DimacsModel model = DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mappingUrl.toURI()), new File(dimacsUrl.toURI()));
        return new Sat4JSolverFacade(model);
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

    private static int countTransitions(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    /**
     * Counts the expected number of pair-graph edges for the given FTS
     * under the INIT-less construction: one per (t1, t2) contiguous-pair.
     */
    private static int expectedPairEdgeCount(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        java.util.List<Transition> all = new java.util.ArrayList<>();
        while (it.hasNext()) {
            all.add(it.next());
        }
        for (Transition t1 : all) {
            for (Transition t2 : all) {
                if (t1.getTarget().equals(t2.getSource())) {
                    n++; // p(t1) -> p(t2)
                }
            }
        }
        return n;
    }

    private static List<Transition> toList(TestCase tc) {
        List<Transition> list = new java.util.ArrayList<>();
        for (Transition t : tc) {
            list.add(t);
        }
        return list;
    }

    private static Set<TransitionPairKey> consecutivePairs(List<Transition> walk) {
        Set<TransitionPairKey> pairs = new HashSet<>();
        for (int i = 0; i + 1 < walk.size(); i++) {
            Transition a = walk.get(i);
            Transition b = walk.get(i + 1);
            pairs.add(new TransitionPairKey(a, b));
        }
        return pairs;
    }

    private static Set<TransitionPairKey> enumerateReachablePairs(FeaturedTransitionSystem fts) {
        java.util.List<Transition> all = new java.util.ArrayList<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            all.add(it.next());
        }
        Set<TransitionPairKey> pairs = new HashSet<>();
        for (Transition t1 : all) {
            for (Transition t2 : all) {
                if (t1.getTarget().equals(t2.getSource())) {
                    pairs.add(new TransitionPairKey(t1, t2));
                }
            }
        }
        return pairs;
    }

    /** Equality / hash keyed on (source, action, target) triples of both transitions. */
    private static final class TransitionPairKey {
        final String first;
        final String second;

        TransitionPairKey(Transition a, Transition b) {
            this.first = a.getSource().getName() + "/" + a.getAction().getName()
                    + "/" + a.getTarget().getName();
            this.second = b.getSource().getName() + "/" + b.getAction().getName()
                    + "/" + b.getTarget().getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TransitionPairKey)) return false;
            TransitionPairKey other = (TransitionPairKey) o;
            return first.equals(other.first) && second.equals(other.second);
        }

        @Override
        public int hashCode() {
            return first.hashCode() * 31 + second.hashCode();
        }
    }
}
