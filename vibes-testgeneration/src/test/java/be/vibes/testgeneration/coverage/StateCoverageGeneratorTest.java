package be.vibes.testgeneration.coverage;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.FExpression;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.graph.ShortestPaths;
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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StateCoverageGeneratorTest {

    private static final String SVM_MXE = "cases/SodaVendingMachine/SVM_ESGFx.mxe";
    private static final String SVM_DIMACS = "cases/SodaVendingMachine/configs/SVM.dimacs";
    private static final String SVM_MAPPING = "cases/SodaVendingMachine/configs/SVM_dimacsmapping.txt";

    // ---- ShortestPaths primitives ----

    @Test
    public void shortestPaths_zeroLengthWhenSourceIsTarget() {
        FeaturedTransitionSystem fts = twoStateLoop();
        State a = fts.getState("a");
        Set<State> targets = new HashSet<>();
        targets.add(a);

        List<Transition> path = ShortestPaths.shortestPathToAny(fts, a, targets);
        assertNotNull(path);
        assertEquals(0, path.size());
    }

    @Test
    public void shortestPaths_pickedNearestTarget() {
        // a -> b -> c -> d -> a (4-cycle). Shortest from a to {c, d}: 2 hops (to c).
        FeaturedTransitionSystemFactory factory = new FeaturedTransitionSystemFactory("a");
        factory.addStates("b", "c", "d");
        factory.addAction("step");
        factory.addTransition("a", "step", FExpression.trueValue(), "b");
        factory.addTransition("b", "step", FExpression.trueValue(), "c");
        factory.addTransition("c", "step", FExpression.trueValue(), "d");
        factory.addTransition("d", "step", FExpression.trueValue(), "a");
        FeaturedTransitionSystem fts = factory.build();

        Set<State> targets = new HashSet<>();
        targets.add(fts.getState("c"));
        targets.add(fts.getState("d"));

        List<Transition> path = ShortestPaths.shortestPathToAny(fts, fts.getState("a"), targets);
        assertNotNull(path);
        assertEquals("Shortest path to {c,d} from a should be 2 hops via b -> c",
                2, path.size());
        assertEquals("c", path.get(path.size() - 1).getTarget().getName());
    }

    @Test
    public void shortestPaths_returnsNullWhenUnreachable() {
        FeaturedTransitionSystem fts = twoStateLoop();
        // Construct a state that does not belong to the FTS by building a
        // separate FTS instance; ShortestPaths must return null since that
        // state cannot be reached from any state of the first FTS.
        FeaturedTransitionSystemFactory other = new FeaturedTransitionSystemFactory("z");
        FeaturedTransitionSystem alienFts = other.build();
        Set<State> targets = new HashSet<>();
        targets.add(alienFts.getState("z"));

        List<Transition> path = ShortestPaths.shortestPathToAny(fts, fts.getState("a"), targets);
        assertThat(path, is(nullValue()));
    }

    // ---- StateCoverageGenerator core algorithm ----

    @Test
    public void coverageWalk_twoStateLoop_visitsBothStatesInOneStep() {
        FeaturedTransitionSystem fts = twoStateLoop();
        List<Transition> walk = StateCoverageGenerator.computeStateCoverageWalk(fts);
        // From the initial state a we just need one step to reach b. Walk
        // should be length 1; both states are visited.
        assertEquals(1, walk.size());
        assertEquals("b", walk.get(0).getTarget().getName());
    }

    @Test
    public void coverageWalk_visitsEveryState() {
        // A more interesting graph with a branch + reroute opportunity.
        FeaturedTransitionSystemFactory factory = new FeaturedTransitionSystemFactory("a");
        factory.addStates("b", "c", "d");
        factory.addAction("ab");
        factory.addAction("bc");
        factory.addAction("ca");
        factory.addAction("ad");
        factory.addAction("da");
        factory.addTransition("a", "ab", FExpression.trueValue(), "b");
        factory.addTransition("b", "bc", FExpression.trueValue(), "c");
        factory.addTransition("c", "ca", FExpression.trueValue(), "a");
        factory.addTransition("a", "ad", FExpression.trueValue(), "d");
        factory.addTransition("d", "da", FExpression.trueValue(), "a");
        FeaturedTransitionSystem fts = factory.build();

        List<Transition> walk = StateCoverageGenerator.computeStateCoverageWalk(fts);
        Set<State> visited = visitedStates(fts, walk);
        assertEquals("Walk must visit every state", countStates(fts), visited.size());
    }

    // ---- End-to-end on SVM ----

    @Test
    public void svm_stateCoverage_walkVisitsAllReachableStates() throws Exception {
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
            int expectedStates = countStates(repaired);

            TestCase tc = StateCoverageGenerator.generate(fts, config, testId);

            Set<State> visited = visitedStates(repaired, toList(tc));
            // initial state is always visited (start of every walk).
            visited.add(repaired.getInitialState());
            assertEquals("Test case " + testId
                            + " must visit every state of the repaired projected FTS",
                    expectedStates, visited.size());
        }
        assertEquals(12, configCount);
    }

    @Test
    public void svm_stateCoverage_isUsuallyShorterThanTransitionCoverage() throws Exception {
        // Heuristic property, NOT an invariant: state coverage walks every
        // state at least once but typically does not need every transition,
        // so it is usually (but not always) shorter than the all-transitions
        // Euler cycle.
        //
        // It can be longer on SVM's smallest projected products because:
        //   - the greedy walk + BFS reroute is not provably optimal (we
        //     pick "first unvisited neighbour" and let BFS bridge the
        //     stuck cases, which can take detours), whereas
        //   - the Hierholzer cycle for transition coverage is exactly
        //     |E| edges in the balanced FTS (plus synthetic edges, which
        //     are folded in too).
        //
        // We measure and report rather than assert; the empirical
        // distribution across the 8 SPLs is one of the answers to RQ3.
        FeaturedTransitionSystem fts = loadSvm();
        Sat4JSolverFacade solver = loadSolver();

        int configCount = 0;
        int stateShorterCount = 0;
        int totalStateLength = 0;
        int totalTransitionLength = 0;
        Iterator<Configuration> configs = solver.getSolutions();
        while (configs.hasNext()) {
            Configuration config = configs.next();
            configCount++;
            String testId = "svm_p" + configCount;

            TestCase stateTc = StateCoverageGenerator.generate(fts, config, testId + "_state");
            TestCase transitionTc =
                    TransitionCoverageGenerator.generate(fts, config, testId + "_trans");

            int stateLen = toList(stateTc).size();
            int transLen = toList(transitionTc).size();
            totalStateLength += stateLen;
            totalTransitionLength += transLen;
            if (stateLen < transLen) {
                stateShorterCount++;
            }
        }
        assertEquals(12, configCount);
        // Empirical sanity: at least the AVERAGE state-coverage walk should
        // be no longer than the average transition-coverage cycle. If this
        // ever fails, the greedy heuristic in StateCoverageGenerator has
        // regressed and warrants investigation.
        assertTrue("On average state coverage should be no longer than transition coverage "
                        + "(avg state=" + (totalStateLength / configCount)
                        + ", avg trans=" + (totalTransitionLength / configCount) + ")",
                totalStateLength <= totalTransitionLength);
        // Lower bound check: state should be strictly shorter at least once.
        assertThat("State coverage should be strictly shorter than transition coverage "
                        + "for at least one configuration",
                stateShorterCount, greaterThan(0));
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
        URL url = StateCoverageGeneratorTest.class.getClassLoader().getResource(SVM_MXE);
        assertThat(url, is(notNullValue()));
        return new MxeToFtsConverter().convert(new File(url.toURI()));
    }

    private static Sat4JSolverFacade loadSolver() throws Exception {
        URL dimacsUrl = StateCoverageGeneratorTest.class.getClassLoader().getResource(SVM_DIMACS);
        URL mappingUrl = StateCoverageGeneratorTest.class.getClassLoader().getResource(SVM_MAPPING);
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

    private static List<Transition> toList(TestCase tc) {
        List<Transition> list = new java.util.ArrayList<>();
        for (Transition t : tc) {
            list.add(t);
        }
        return list;
    }

    private static Set<State> visitedStates(FeaturedTransitionSystem fts,
                                            List<Transition> walk) {
        Set<State> visited = new HashSet<>();
        visited.add(fts.getInitialState());
        for (Transition t : walk) {
            visited.add(t.getTarget());
        }
        return visited;
    }
}
