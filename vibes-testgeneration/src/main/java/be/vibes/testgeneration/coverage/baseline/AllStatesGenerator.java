package be.vibes.testgeneration.coverage.baseline;

import be.vibes.fexpression.FExpression;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.solver.ConstraintIdentifier;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import be.vibes.ts.TransitionSystem;
import be.vibes.ts.exception.TransitionSystenExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Family-level all-states coverage-driven test generator: Devroey et al.,
 * SPLC 2014, Algorithm 3 (branch-and-bound with accessibility-matrix
 * heuristic).
 *
 * <p>Port-with-reference of {@code AllStatesTestCaseGenerator} from the
 * 2014 VIBeS implementation (commit {@code f856c90}, deleted in the 2018
 * refactor). The algorithm semantics are preserved; the surrounding
 * scaffolding (TestCaseFactory pattern, mutable-TestCase interface, abstract
 * WrapUp/Validator hierarchy) has been simplified to match the current
 * {@code be.vibes.ts.*} API.
 *
 * <p><strong>Algorithm 3 (paper, restated):</strong>
 * <ol>
 *   <li>Initialise {@code toVisit ← S \ {initial}}.</li>
 *   <li>Initialise the candidate priority queue with one walk per
 *       outgoing transition of the initial state. Each candidate carries
 *       its score (the number of states in {@code toVisit} reachable from
 *       the walk's last state, per the Warshall accessibility matrix).</li>
 *   <li>While {@code toVisit} is not empty and candidates exist:
 *     <ol type="a">
 *       <li>Pop the highest-score candidate {@code c}.</li>
 *       <li>If {@code c.lastState == initial} (the walk has closed),
 *           validate {@code c}'s accumulated feature expression against the
 *           feature model. If valid AND the walk visits some previously-
 *           uncovered state, accept it as a test case and remove its
 *           visited states from {@code toVisit}.</li>
 *       <li>Otherwise, branch out {@code c} by appending each outgoing
 *           transition of {@code c.lastState}. For each successor, recompute
 *           the score; keep it in the queue if its accumulated feature
 *           expression remains satisfiable.</li>
 *     </ol>
 *   </li>
 *   <li>Return the accumulated test set.</li>
 * </ol>
 *
 * <p><strong>FTS validity check.</strong> A walk's feature expression is
 * the conjunction of its transitions' feature expressions; valid iff
 * SAT-solvable under the feature model. Paper §3.1's "Simplification for
 * large models" defers this SAT call to walk-completion time (only when
 * the candidate returns to the initial state), saving ~99% of SAT calls
 * during branching. We implement that simplification.
 *
 * <p>For LTS input (no feature model), the validity check is a no-op
 * (always returns true).
 */
public final class AllStatesGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(AllStatesGenerator.class);

    /** Default Warshall closure depth, matches the original 2014 default. */
    private static final int DEFAULT_WARSHALL_DEPTH = 5;

    private AllStatesGenerator() {
    }

    /**
     * Generate a family-level all-states-covering test suite for an LTS
     * (no feature model). Validity check always returns true.
     */
    public static List<TestCase> generateForLts(TransitionSystem ts, String testIdPrefix) {
        return generate(ts, null, null, testIdPrefix, DEFAULT_WARSHALL_DEPTH);
    }

    /**
     * Generate a family-level all-states-covering test suite for an FTS
     * with a feature model. Walks whose accumulated feature expression is
     * unsatisfiable under {@code solver}'s feature model are dropped.
     */
    public static List<TestCase> generateForFts(FeaturedTransitionSystem fts,
                                                Sat4JSolverFacade solver,
                                                String testIdPrefix) {
        checkNotNull(solver, "Solver may not be null for FTS generation");
        return generate(fts, fts, solver, testIdPrefix, DEFAULT_WARSHALL_DEPTH);
    }

    /**
     * Generate a family-level all-states-covering test suite. Internal
     * variant taking explicit parameters; prefer the LTS / FTS façade
     * methods.
     */
    public static List<TestCase> generate(TransitionSystem ts,
                                          FeaturedTransitionSystem ftsOrNull,
                                          Sat4JSolverFacade solverOrNull,
                                          String testIdPrefix,
                                          int warshallDepth) {
        checkNotNull(ts, "Transition system may not be null");
        checkNotNull(testIdPrefix, "Test case ID prefix may not be null");

        State initial = ts.getInitialState();
        Set<State> toVisit = collectStates(ts);
        toVisit.remove(initial);

        WarshallAccessibility accessibility = WarshallAccessibility.forSystem(ts);
        accessibility.iterate(warshallDepth);

        // Highest score at the head — PriorityQueue is min-heap by default.
        PriorityQueue<BaselineWalk> candidates =
                new PriorityQueue<>(Comparator.comparingInt(BaselineWalk::getScore).reversed());

        // Seed the queue with one candidate per outgoing transition of
        // the initial state (matching the original algorithm's seeding).
        addSuccessors(new BaselineWalk(initial), initial, ts, ftsOrNull,
                solverOrNull, accessibility, toVisit, candidates, new ArrayList<>());

        List<TestCase> result = new ArrayList<>();
        while (!toVisit.isEmpty() && !candidates.isEmpty()) {
            BaselineWalk best = candidates.poll();
            addSuccessors(best, best.getLastState(), ts, ftsOrNull,
                    solverOrNull, accessibility, toVisit, candidates, result);
        }

        // Assign stable IDs in walk-completion order so experiment logs
        // are reproducible.
        List<TestCase> labelled = new ArrayList<>(result.size());
        for (int i = 0; i < result.size(); i++) {
            TestCase old = result.get(i);
            TestCase relabelled = new TestCase(testIdPrefix + "_tc" + i);
            try {
                for (Transition t : old) {
                    relabelled.enqueue(t);
                }
            } catch (TransitionSystenExecutionException e) {
                throw new IllegalStateException(
                        "Could not relabel test case (cycle non-contiguous?): " + e.getMessage(), e);
            }
            labelled.add(relabelled);
        }

        if (!toVisit.isEmpty()) {
            LOG.warn("AllStatesGenerator could not cover {} state(s): {}",
                    toVisit.size(), toVisit);
        }
        LOG.info("AllStatesGenerator: produced {} test case(s) covering {}/{} states",
                labelled.size(), collectStates(ts).size() - toVisit.size(),
                collectStates(ts).size());
        return labelled;
    }

    /**
     * For each outgoing transition of {@code from}, fork the walk and
     * either accept it as a completed test case (if it closes at initial
     * and is valid) or push it into the candidate queue (if it's still
     * extending and remains feasible).
     */
    private static void addSuccessors(BaselineWalk walk,
                                      State from,
                                      TransitionSystem ts,
                                      FeaturedTransitionSystem ftsOrNull,
                                      Sat4JSolverFacade solverOrNull,
                                      WarshallAccessibility accessibility,
                                      Set<State> toVisit,
                                      PriorityQueue<BaselineWalk> candidates,
                                      List<TestCase> result) {
        State initial = ts.getInitialState();
        Iterator<Transition> outs = ts.getOutgoing(from);
        while (outs.hasNext()) {
            Transition t = outs.next();
            BaselineWalk fork = walk.copy();
            fork.append(t, ftsOrNull);

            if (t.getTarget().equals(initial)) {
                // Walk closes — validate and accept if it covers new states.
                if (!isValid(fork, solverOrNull)) {
                    LOG.debug("Closed walk rejected — unsatisfiable: {}", fork);
                    continue;
                }
                Set<State> newlyVisited = new HashSet<>(fork.getVisitedStates());
                newlyVisited.retainAll(toVisit);
                if (newlyVisited.isEmpty()) {
                    LOG.debug("Closed walk does not progress, dropping: {}", fork);
                    continue;
                }
                TestCase tc = walkToTestCase(fork, "candidate");
                result.add(tc);
                toVisit.removeAll(newlyVisited);
                LOG.debug("Closed walk accepted, removed {} state(s) from toVisit",
                        newlyVisited.size());
            } else {
                // Walk extends — score it and queue it (if still feasible).
                if (!isValid(fork, solverOrNull)) {
                    LOG.debug("Extending walk rejected — unsatisfiable so far: {}", fork);
                    continue;
                }
                int score = scoreOf(fork, accessibility, toVisit);
                fork.setScore(score);
                candidates.add(fork);
            }
        }
    }

    private static int scoreOf(BaselineWalk walk,
                               WarshallAccessibility accessibility,
                               Set<State> toVisit) {
        // Paper Algorithm 2: score = number of toVisit states reachable from
        // the walk's last state. We add the number of toVisit states ALREADY
        // covered by the walk (Devroey's `Sets.intersection` enhancement in
        // AllStatesTestCaseGenerator.computeScore): this rewards walks that
        // have already gathered uncovered states, biasing the priority queue
        // toward completing productive walks rather than starting new ones.
        int reachable = accessibility.countAccessible(walk.getLastState(), toVisit);
        int alreadyCovered = 0;
        for (State s : walk.getVisitedStates()) {
            if (toVisit.contains(s)) {
                alreadyCovered++;
            }
        }
        return reachable + alreadyCovered;
    }

    /**
     * FTS satisfiability check via SAT solver. For LTS, always true.
     */
    private static boolean isValid(BaselineWalk walk, Sat4JSolverFacade solver) {
        if (solver == null) {
            return true;
        }
        FExpression fexpr = walk.getAccumulatedFExpression();
        if (fexpr == null || fexpr.isTrue()) {
            return true;
        }
        ConstraintIdentifier id = null;
        try {
            id = solver.addConstraint(fexpr);
            return solver.isSatisfiable();
        } catch (Exception e) {
            // ConstraintSolvingException covers SolverInitializationException;
            // SolverFatalErrorException is unchecked-flavoured here. Treat any
            // SAT failure as "walk currently infeasible" rather than aborting
            // the entire generation pass.
            LOG.debug("Constraint solving failed, treating walk as invalid: {}", e.getMessage());
            return false;
        } finally {
            if (id != null) {
                try {
                    solver.removeConstraint(id);
                } catch (Exception e) {
                    LOG.warn("Constraint cleanup failed: {}", e.getMessage());
                }
            }
        }
    }

    private static TestCase walkToTestCase(BaselineWalk walk, String id) {
        TestCase tc = new TestCase(id);
        try {
            for (Transition t : walk.getTransitions()) {
                tc.enqueue(t);
            }
        } catch (TransitionSystenExecutionException e) {
            throw new IllegalStateException(
                    "BaselineWalk produced a non-contiguous transition sequence: "
                            + e.getMessage(), e);
        }
        return tc;
    }

    private static Set<State> collectStates(TransitionSystem ts) {
        Set<State> out = new HashSet<>();
        Iterator<State> it = ts.states();
        while (it.hasNext()) out.add(it.next());
        return out;
    }
}
