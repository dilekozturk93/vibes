package be.vibes.testgeneration.coverage;

import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.ts.Action;
import be.vibes.ts.State;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import be.vibes.ts.TransitionSystem;
import be.vibes.ts.exception.TransitionSystenExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Uniform-random walk baseline against the coverage-directed generators
 * for RQ2 (mutation score). Each step picks one of the current state's
 * outgoing transitions with equal probability (no damping, no teleport,
 * no coverage goal); the suite stops when its cumulative real-action
 * count meets the supplied action budget.
 *
 * <p>Key design points — agreed with the user, do NOT alter in-place:
 *
 * <ul>
 *   <li><strong>Cut-and-include semantics.</strong> When an in-flight
 *       random walk reaches the configured {@code maxStepsPerCase} ceiling
 *       without returning to the initial state, the walk is truncated and
 *       the truncated prefix IS added to the suite (it represents real
 *       steps the SUT would perform under that random seed). The next
 *       walk then begins. The legacy VIBeS
 *       {@code RandomTestCaseSelector} behaviour (DISCARD non-terminating
 *       walks and retry up to {@code maxNbrTry}) is NOT what the paper's
 *       random baseline wants and is replaced here.</li>
 *
 *   <li><strong>Model-relative step ceiling.</strong> {@code maxStepsPerCase}
 *       is supplied by the caller and is expected to be set as a multiple
 *       of {@code |T|} (e.g. {@code 2 * transitions}) so it scales with
 *       SPL size and only cuts pathological cycles. A fixed magic number
 *       (e.g. 100) would over-cut on large SPLs and overshoot on small
 *       ones.</li>
 *
 *   <li><strong>Per-coverage-level action budget.</strong> The
 *       {@code actionBudget} parameter is the total real-action count the
 *       suite must reach before stopping. The caller must supply it as
 *       the total real-action count of the COMPETITOR suite (e.g. when
 *       comparing against the all-states suite, pass that suite's total
 *       action count). This produces a paper-fair baseline at each
 *       coverage level — random gets exactly the same execution budget
 *       as the suite it is being compared to, no more, no less.</li>
 *
 *   <li><strong>Aborted-walk counter.</strong> The result records how
 *       many walks were truncated at the step ceiling (analogous to the
 *       user's ESG-Fx-side "Safety Limit Hit Count" telemetry).</li>
 * </ul>
 *
 * <p>The supplied seed is forwarded directly to a fresh {@link Random}
 * instance so the result is reproducible.
 *
 * <p>Synthetic balancing transitions (per
 * {@link EulerianBalancer#isSyntheticAction}) never appear on
 * randomly-walked FTSs — those FTSs are unbalanced by construction. The
 * {@code __end__} action and {@code __dup__N}-suffixed actions never
 * appear either on the projected/repaired FTSs this generator is called
 * on. The walk simply picks among the state's outgoing transitions
 * verbatim.
 */
public final class RandomBaselineGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(RandomBaselineGenerator.class);

    private RandomBaselineGenerator() {
        // Utility class.
    }

    /**
     * The output of a single random-baseline run for a given (FTS, budget,
     * step-ceiling, seed) input. Carries the suite of TestCases produced
     * plus the auditing counters needed by the RQ2 CSV writer.
     */
    public static final class Result {
        private final List<TestCase> suite;
        private final int abortedWalks;
        private final int totalRealActions;
        private final int dropoutWalks;
        private final long seed;
        private final int actionBudget;
        private final int maxStepsPerCase;

        Result(List<TestCase> suite, int abortedWalks, int totalRealActions,
               int dropoutWalks, long seed, int actionBudget, int maxStepsPerCase) {
            this.suite = Collections.unmodifiableList(suite);
            this.abortedWalks = abortedWalks;
            this.totalRealActions = totalRealActions;
            this.dropoutWalks = dropoutWalks;
            this.seed = seed;
            this.actionBudget = actionBudget;
            this.maxStepsPerCase = maxStepsPerCase;
        }

        public List<TestCase> getSuite() {
            return suite;
        }

        /**
         * Number of walks in {@link #getSuite()} that were truncated at
         * {@link #getMaxStepsPerCase()} because they did not return to
         * the initial state within the step ceiling. Cut-and-included
         * walks are still part of the suite; this counter is the
         * telemetry side-channel.
         */
        public int getAbortedWalks() {
            return abortedWalks;
        }

        /** Sum of real action steps across all walks in the suite. */
        public int getTotalRealActions() {
            return totalRealActions;
        }

        /**
         * Walks that produced ZERO usable transitions (the initial state
         * had no outgoing transitions on the very first try). On
         * properly-projected-and-repaired product FTSs this is always 0;
         * a non-zero value indicates an upstream pipeline bug.
         */
        public int getDropoutWalks() {
            return dropoutWalks;
        }

        public long getSeed() {
            return seed;
        }

        public int getActionBudget() {
            return actionBudget;
        }

        public int getMaxStepsPerCase() {
            return maxStepsPerCase;
        }
    }

    /**
     * Produces one random-baseline suite for the given product-level FTS,
     * action budget, per-walk step ceiling, and seed. See the class
     * JavaDoc for the design constraints — every one of those points is a
     * deliberate decision, not an artefact, and the implementation below
     * is expected to match them literally.
     *
     * @param ts                product-level repaired FTS — must have at
     *                          least one outgoing transition from its
     *                          initial state.
     * @param idPrefix          prefix prepended to each TestCase id so
     *                          experiment logs are reproducible across
     *                          the four coverage generators.
     * @param actionBudget      total real-action count the suite must
     *                          reach before stopping (paper-fair budget
     *                          matched to the competitor suite).
     *                          Must be {@code > 0}.
     * @param maxStepsPerCase   step ceiling per walk; walks reaching the
     *                          ceiling are CUT and INCLUDED in the suite.
     *                          Must be {@code > 0}; recommended
     *                          {@code 2 * |T|} for the repaired FTS so it
     *                          scales with SPL size.
     * @param seed              {@link Random} seed for reproducibility.
     */
    public static Result generate(TransitionSystem ts, String idPrefix,
                                  int actionBudget, int maxStepsPerCase, long seed) {
        checkNotNull(ts, "TransitionSystem may not be null");
        checkNotNull(idPrefix, "ID prefix may not be null");
        checkArgument(actionBudget > 0, "actionBudget must be > 0; got %s", actionBudget);
        checkArgument(maxStepsPerCase > 0, "maxStepsPerCase must be > 0; got %s", maxStepsPerCase);

        Random random = new Random(seed);
        List<TestCase> suite = new ArrayList<>();
        int abortedWalks = 0;
        int totalRealActions = 0;
        int dropoutWalks = 0;
        int walkIndex = 0;

        State initial = ts.getInitialState();
        while (totalRealActions < actionBudget) {
            WalkResult walk = walkOne(ts, initial, random, maxStepsPerCase, idPrefix + "_random_" + walkIndex);
            walkIndex++;
            if (walk.testCase == null) {
                // Dropout: initial state had no outgoing transitions on the
                // very first attempt — produced no usable transitions. Skip.
                // Increment counter for telemetry; do NOT add to suite.
                dropoutWalks++;
                // Avoid infinite loops on pathological FTSs by aborting the
                // suite if EVERY recent walk has been a dropout. In practice
                // this should never happen on a repaired product FTS (it is
                // strongly connected from initial), but defend anyway.
                if (dropoutWalks > 1000) {
                    LOG.warn("RandomBaselineGenerator: aborting suite after >1000 dropouts "
                                    + "({}). Initial state may have no outgoing transitions on {}.",
                            dropoutWalks, idPrefix);
                    break;
                }
                continue;
            }
            if (walk.aborted) {
                abortedWalks++;
            }
            suite.add(walk.testCase);
            totalRealActions += walk.stepCount;
        }
        LOG.info("RandomBaselineGenerator[{}|seed={}]: budget={}, maxStepsPerCase={}, "
                        + "produced {} test case(s) ({} aborted, {} dropouts), {} total actions",
                idPrefix, seed, actionBudget, maxStepsPerCase,
                suite.size(), abortedWalks, dropoutWalks, totalRealActions);
        return new Result(suite, abortedWalks, totalRealActions, dropoutWalks,
                seed, actionBudget, maxStepsPerCase);
    }

    /**
     * Performs a single uniform-random walk from the initial state up to
     * {@code maxStepsPerCase} or until a return to the initial state,
     * whichever comes first. The truncated-but-non-empty walk is
     * cut-and-included; an empty walk (no outgoing transitions from
     * initial) is returned with {@code testCase == null} so the caller
     * can count it as a dropout.
     */
    private static WalkResult walkOne(TransitionSystem ts, State initial,
                                      Random random, int maxStepsPerCase, String tcId) {
        TestCase tc = new TestCase(tcId);
        State current = initial;
        int steps = 0;
        boolean aborted = false;
        try {
            while (steps < maxStepsPerCase) {
                List<Transition> outgoing = collectOutgoing(ts, current);
                if (outgoing.isEmpty()) {
                    // Sink state — terminate this walk early. Counted as
                    // an abort (it did not return to initial within budget).
                    aborted = true;
                    break;
                }
                Transition next = outgoing.get(random.nextInt(outgoing.size()));
                tc.enqueue(next);
                current = next.getTarget();
                steps++;
                if (current.equals(initial)) {
                    // Natural end of a closed walk; do not mark aborted.
                    break;
                }
            }
            if (steps >= maxStepsPerCase && !current.equals(initial)) {
                aborted = true;
            }
        } catch (TransitionSystenExecutionException e) {
            // Shouldn't happen — collectOutgoing returns only valid
            // continuations of the current state. Defensive: surface as
            // abort with the partial walk preserved.
            LOG.error("RandomBaselineGenerator: enqueue refused after {} step(s) on {}: {}",
                    steps, tcId, e.getMessage());
            aborted = true;
        }
        if (steps == 0) {
            // Dropout — initial had no outgoing transitions at the very
            // first try. Returning null signals the caller to skip rather
            // than add an empty TestCase to the suite.
            return new WalkResult(null, 0, false);
        }
        return new WalkResult(tc, steps, aborted);
    }

    /** Snapshot of outgoing transitions from {@code state} as a list. */
    private static List<Transition> collectOutgoing(TransitionSystem ts, State state) {
        Iterator<Transition> it = ts.getOutgoing(state);
        if (!it.hasNext()) {
            return Collections.emptyList();
        }
        List<Transition> out = new ArrayList<>();
        while (it.hasNext()) {
            out.add(it.next());
        }
        return out;
    }

    /** Helper for {@link #walkOne(TransitionSystem, State, Random, int, String)}. */
    private static final class WalkResult {
        final TestCase testCase;
        final int stepCount;
        final boolean aborted;

        WalkResult(TestCase testCase, int stepCount, boolean aborted) {
            this.testCase = testCase;
            this.stepCount = stepCount;
            this.aborted = aborted;
        }
    }

    /**
     * Convenience: compute the model-relative step ceiling for a given
     * FTS. Returns {@code multiplier × ftsTransitionCount}, with a floor
     * of {@code multiplier × 2} so a degenerate {@code |T|=0} FTS does
     * not produce a zero ceiling (the floor is also a sanity check —
     * such an FTS would dropout immediately anyway). Intended to be
     * called once per product to derive the second argument to
     * {@link #generate(TransitionSystem, String, int, int, long)}.
     */
    public static int modelRelativeMaxSteps(int ftsTransitionCount, int multiplier) {
        checkArgument(multiplier > 0, "multiplier must be > 0; got %s", multiplier);
        return Math.max(multiplier * Math.max(1, ftsTransitionCount), 2 * multiplier);
    }

    /**
     * Returns the action set of the FTS; helper that the harness uses
     * to sanity-check budgets against the modelled action space. Not used
     * inside {@link #generate} directly.
     */
    public static int actionCount(TransitionSystem ts) {
        int n = 0;
        Iterator<Action> it = ts.actions();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }
}
