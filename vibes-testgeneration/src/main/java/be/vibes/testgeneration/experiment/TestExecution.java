package be.vibes.testgeneration.experiment;

import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import be.vibes.ts.TransitionSystem;
import be.vibes.ts.exception.TransitionSystenExecutionException;
import be.vibes.ts.execution.TransitionSystemExecutor;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Replays a {@link TestCase} or test suite on a {@link TransitionSystem}
 * via VIBeS' {@link TransitionSystemExecutor} and reports execution
 * metrics: number of transitions executed, wall-clock duration in
 * nanoseconds, and whether the replay completed without refusal.
 *
 * <p>Distinct from {@code FaultDetector}: this class measures the
 * COST of executing a test suite on a given FTS (for RQ1 "test
 * execution time" and RQ3 "total transitions executed" metrics).
 * FaultDetector measures DETECTION — does the mutant FTS refuse some
 * step of the suite? Both wrap the same underlying VIBeS executor but
 * answer different questions.
 *
 * <p>Synthetic transitions (per
 * {@link EulerianBalancer#isSyntheticAction}) are skipped — they have
 * no SUT-level meaning and would distort the cost metric.
 *
 * <p>Timing precision: wall-clock via {@code System.nanoTime()}, which
 * is appropriate for sub-millisecond executor calls. Reported in
 * nanoseconds; helpers convert to ms for human-readable reports.
 */
public final class TestExecution {

    private TestExecution() {
        // Utility class.
    }

    /**
     * Result of executing a single {@link TestCase} on a transition system.
     */
    public static final class ExecutionResult {
        private final int realTransitionsExecuted;
        private final long durationNanos;
        private final boolean completed;
        private final String refusedAtAction;

        ExecutionResult(int realTransitionsExecuted, long durationNanos,
                        boolean completed, String refusedAtAction) {
            this.realTransitionsExecuted = realTransitionsExecuted;
            this.durationNanos = durationNanos;
            this.completed = completed;
            this.refusedAtAction = refusedAtAction;
        }

        /**
         * Number of non-synthetic transitions the executor actually
         * applied (i.e. {@code canExecute} returned true and the executor
         * advanced). Refused steps and synthetic transitions are excluded.
         */
        public int getRealTransitionsExecuted() {
            return realTransitionsExecuted;
        }

        public long getDurationNanos() {
            return durationNanos;
        }

        public double getDurationMillis() {
            return durationNanos / 1_000_000.0;
        }

        /**
         * True iff every non-synthetic transition in the test case was
         * executed without refusal. False iff some step was refused
         * mid-execution (used by {@code FaultDetector.killsDynamic} as
         * the kill signal).
         */
        public boolean isCompleted() {
            return completed;
        }

        /**
         * If completion was refused, the action name at which refusal
         * occurred; {@code null} if {@link #isCompleted()} returned true.
         */
        public String getRefusedAtAction() {
            return refusedAtAction;
        }
    }

    /**
     * Aggregate result over a list of test cases — sums the per-test-case
     * metrics. {@link #getCompletedCount} reports how many test cases
     * executed all the way through.
     */
    public static final class SuiteExecutionResult {
        private final int totalRealTransitions;
        private final long totalDurationNanos;
        private final int totalTestCases;
        private final int completedTestCases;

        SuiteExecutionResult(int totalRealTransitions, long totalDurationNanos,
                             int totalTestCases, int completedTestCases) {
            this.totalRealTransitions = totalRealTransitions;
            this.totalDurationNanos = totalDurationNanos;
            this.totalTestCases = totalTestCases;
            this.completedTestCases = completedTestCases;
        }

        /** Total non-synthetic transitions actually executed across the suite. */
        public int getTotalRealTransitions() {
            return totalRealTransitions;
        }

        public long getTotalDurationNanos() {
            return totalDurationNanos;
        }

        public double getTotalDurationMillis() {
            return totalDurationNanos / 1_000_000.0;
        }

        public int getTotalTestCases() {
            return totalTestCases;
        }

        public int getCompletedTestCases() {
            return completedTestCases;
        }

        public int getRefusedTestCases() {
            return totalTestCases - completedTestCases;
        }
    }

    /**
     * Executes a single test case on the given transition system and
     * returns the per-step metrics.
     *
     * <p>Algorithm: instantiate a {@link TransitionSystemExecutor},
     * reset to the initial state, then for each non-synthetic transition
     * of the test case call {@code canExecute} + {@code execute}. Stop
     * at the first refusal (returning a non-completed result) or run to
     * the end of the test case.
     */
    public static ExecutionResult execute(TestCase testCase, TransitionSystem ts) {
        checkNotNull(testCase, "TestCase may not be null");
        checkNotNull(ts, "TransitionSystem may not be null");

        TransitionSystemExecutor executor = new TransitionSystemExecutor(ts);
        long start = System.nanoTime();
        int realExecuted = 0;
        try {
            executor.reset();
            for (Transition t : testCase) {
                String actionName = t.getAction().getName();
                if (actionName.startsWith(EulerianBalancer.SYNTHETIC_ACTION_PREFIX)) {
                    // __balance__N: should never appear in executable suites
                    // (pair-coverage strips them). Skip + log via FaultDetector;
                    // for execution metrics, also skip without advancing.
                    continue;
                }
                String effectiveActionName;
                boolean countsAsReal;
                if (actionName.contains(EulerianBalancer.DUPLICATE_ACTION_INFIX)) {
                    // __dup__N: real transition under a synthetic label;
                    // execute the base action. A doubled traversal IS a
                    // real step the tester performs, so DO count it.
                    effectiveActionName = EulerianBalancer.stripDuplicateSuffix(actionName);
                    countsAsReal = true;
                } else {
                    // Real action OR __end__ (real FTS transition with
                    // target = initial). Execute via the action name.
                    // For "real cost" reporting, __end__ is NOT a SUT
                    // event the tester performs; exclude from the count
                    // (count it as state-advancement only).
                    effectiveActionName = actionName;
                    countsAsReal = !actionName.startsWith("__end__");
                }
                be.vibes.ts.Action effectiveAction = ts.getAction(effectiveActionName);
                if (effectiveAction == null || !executor.canExecute(effectiveAction)) {
                    long elapsed = System.nanoTime() - start;
                    return new ExecutionResult(realExecuted, elapsed, false,
                            actionName);
                }
                executor.execute(effectiveAction);
                if (countsAsReal) realExecuted++;
            }
        } catch (TransitionSystenExecutionException e) {
            long elapsed = System.nanoTime() - start;
            return new ExecutionResult(realExecuted, elapsed, false,
                    "<executor error: " + e.getMessage() + ">");
        }
        long elapsed = System.nanoTime() - start;
        return new ExecutionResult(realExecuted, elapsed, true, null);
    }

    /**
     * Executes every test case in the suite on the given transition
     * system and aggregates the per-test-case results.
     */
    public static SuiteExecutionResult executeSuite(List<TestCase> suite, TransitionSystem ts) {
        checkNotNull(suite, "Suite may not be null");
        checkNotNull(ts, "TransitionSystem may not be null");
        int totalRealTransitions = 0;
        long totalNanos = 0;
        int completed = 0;
        for (TestCase tc : suite) {
            ExecutionResult r = execute(tc, ts);
            totalRealTransitions += r.getRealTransitionsExecuted();
            totalNanos += r.getDurationNanos();
            if (r.isCompleted()) completed++;
        }
        return new SuiteExecutionResult(totalRealTransitions, totalNanos,
                suite.size(), completed);
    }
}
