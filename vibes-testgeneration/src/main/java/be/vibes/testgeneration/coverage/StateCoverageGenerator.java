package be.vibes.testgeneration.coverage;

import be.vibes.fexpression.configuration.Configuration;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.graph.ShortestPaths;
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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates a single all-states test case for one product configuration.
 *
 * <p>State coverage requires that every state of the product FTS be
 * visited at least once. Unlike all-transitions coverage, it does NOT
 * require every edge to be used. The resulting test cases are therefore
 * typically shorter, which is exactly the trade-off RQ3 asks about.
 *
 * <p>Algorithm (analog of the ESG-Fx-side
 * {@code EulerCycleGeneratorForEventCoverage}):
 *
 * <ol>
 *   <li>Project the SPL FTS onto the configuration and keep the SCC that
 *       contains the initial state (same first two steps as the
 *       all-transitions generator).</li>
 *   <li>Walk from the initial state, greedily picking outgoing
 *       transitions to states that have not yet been visited.</li>
 *   <li>When no outgoing transition leads to an unvisited state, run a
 *       BFS from the current state to find the nearest unvisited state
 *       (a unit-weight Dijkstra) and append that path to the walk.</li>
 *   <li>Repeat until every state in the repaired FTS has been visited.</li>
 * </ol>
 *
 * <p>The result is a walk (not necessarily a cycle) that starts at the
 * initial state. Subsequent stages (mutation testing, coverage
 * measurement) can replay it via VIBeS' executor exactly as they would
 * any other {@link TestCase}.
 */
public final class StateCoverageGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(StateCoverageGenerator.class);

    private StateCoverageGenerator() {
        // Utility class.
    }

    /**
     * Generates an all-states test suite for the given product configuration.
     *
     * <p>Returns {@code List<TestCase>} split at every initial-return of
     * the walk. State-coverage walks usually do not transit the initial
     * state mid-walk (the greedy phase prefers unvisited targets), so the
     * resulting list is typically singleton. The split is applied
     * unconditionally to align replay semantic with
     * {@link TransitionCoverageGenerator} and
     * {@link TransitionPairCoverageGenerator} — every coverage suite uses
     * the same "TC = initial-return trip" semantic and the same
     * executor-reset boundaries, eliminating the methodology asymmetry
     * documented in 2026-05-24's TDE pilot.
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

        List<Transition> walk = computeStateCoverageWalk(repaired);

        TestCase fullWalk = new TestCase(testCaseBaseId + "_walk");
        try {
            fullWalk.enqueueAll(walk);
        } catch (TransitionSystenExecutionException ex) {
            throw new IllegalStateException(
                    "Walk could not be enqueued into transient TestCase '"
                            + testCaseBaseId + "_walk' — non-contiguous walk?",
                    ex);
        }
        List<List<Transition>> trips =
                TestCaseSplitter.splitAtInitialReturns(fullWalk, repaired.getInitialState());

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
                                + testCaseBaseId + "'.",
                        ex);
            }
            suite.add(tc);
            idx++;
        }
        LOG.info("Generated state-coverage suite '{}': {} TestCase(s), {} walk transitions",
                testCaseBaseId, suite.size(), walk.size());
        return suite;
    }

    /**
     * Computes a walk through the given (strongly connected, balanced or
     * not) FTS that visits every state at least once. Greedy first,
     * BFS-reroute when no unvisited neighbour is available.
     */
    static List<Transition> computeStateCoverageWalk(FeaturedTransitionSystem fts) {
        State start = fts.getInitialState();

        Set<State> allStates = new LinkedHashSet<>();
        Iterator<State> stateIt = fts.states();
        while (stateIt.hasNext()) {
            allStates.add(stateIt.next());
        }

        Set<State> visited = new HashSet<>();
        visited.add(start);
        Set<State> remaining = new LinkedHashSet<>(allStates);
        remaining.remove(start);

        List<Transition> walk = new ArrayList<>();
        State current = start;
        int rerouteCount = 0;

        while (!remaining.isEmpty()) {
            Transition next = pickUnvisitedNeighbour(fts, current, visited);
            if (next != null) {
                walk.add(next);
                current = next.getTarget();
                visited.add(current);
                remaining.remove(current);
                continue;
            }

            // Stuck: no outgoing transition leads to an unvisited state.
            // BFS to the nearest state in `remaining`.
            List<Transition> path = ShortestPaths.shortestPathToAny(fts, current, remaining);
            if (path == null) {
                throw new IllegalStateException(
                        "State coverage walk stuck at " + current.getName()
                                + " with " + remaining.size() + " state(s) still uncovered; "
                                + "no BFS path to any of them. The FTS may not be strongly connected.");
            }
            walk.addAll(path);
            for (Transition t : path) {
                visited.add(t.getTarget());
                remaining.remove(t.getTarget());
            }
            current = path.get(path.size() - 1).getTarget();
            rerouteCount++;
        }

        LOG.debug("State-coverage walk: {} transitions, {} BFS reroutes",
                walk.size(), rerouteCount);
        return walk;
    }

    /**
     * Picks any outgoing transition of {@code current} whose target has
     * not yet been visited, or {@code null} if every outgoing transition
     * goes to an already-visited state.
     */
    private static Transition pickUnvisitedNeighbour(FeaturedTransitionSystem fts,
                                                     State current,
                                                     Set<State> visited) {
        Iterator<Transition> outs = fts.getOutgoing(current);
        while (outs.hasNext()) {
            Transition t = outs.next();
            if (!visited.contains(t.getTarget())) {
                return t;
            }
        }
        return null;
    }
}
