package be.vibes.testgeneration.mutation;

import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.ts.Action;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import be.vibes.ts.execution.TransitionSystemExecutor;
import be.vibes.ts.exception.TransitionSystenExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides whether a test suite kills a mutant.
 *
 * <h3>Which method to use</h3>
 *
 * <p>For paper experiments and per-product mutation reports use
 * {@link #killsDynamic(List, FeaturedTransitionSystem)} or
 * {@link #scoreSuiteDynamic(List, java.util.Map)} on EVERY operator —
 * uniform execution-based replay via {@link TransitionSystemExecutor}.
 * This is the methodology agreed for Parça 1 (2026-05-22 decision):
 * uniform replay-based detection across all operators ensures a
 * consistent kill semantic regardless of operator structure.
 *
 * <p>The static set-based variants ({@link #kills(List, FeaturedTransitionSystem)}
 * and {@link #scoreSuite(List, java.util.Map)}) are retained for
 * sanity-checking and microbenchmarks only. They are provably
 * equivalent to dynamic replay for {@link TransitionMissing} and
 * {@link ActionExchange} (neither operator changes execution
 * semantics) but NOT for {@link StateMissing} or any future operator
 * that perturbs the executor's reachable state space. Calling them
 * from per-product reports is no longer recommended — prefer the
 * Dynamic suffix for methodological uniformity.
 *
 * <h3>Kill criterion</h3>
 *
 * <p>A test suite {@code K} kills a mutant {@code M} iff some
 * non-synthetic {@code (source, action, target)} triple exercised by
 * {@code K} is NOT a transition of {@code M}:
 *
 * <pre>
 *   kills(K, M)  &equiv;  &exist; t &isin; K, &not;synthetic(t.action)
 *                         &and; (t.source, t.action, t.target) &notin; transitions(M)
 * </pre>
 *
 * <p>This is a static, set-based check. For the two mutation operators
 * currently in scope ({@link TransitionMissing} and
 * {@link ActionExchange}) it is provably equivalent to the standard
 * dynamic kill definition (i.e. replaying the suite on {@code M} via the
 * executor produces different observable behaviour from replaying on the
 * original), because both operators only modify the FTS's transition
 * set and never alter the execution semantics of remaining transitions:
 *
 * <ul>
 *   <li>{@link TransitionMissing} removes {@code (s, &alpha;, t)}. A suite
 *       step that traverses the removed transition finds no matching
 *       outgoing on {@code M} &rarr; refused mid-execution &rarr; killed.
 *       Statically: the triple is absent from {@code M}.</li>
 *   <li>{@link ActionExchange} replaces {@code (s, &alpha;, t)} with
 *       {@code (s, &beta;, t)}. A suite step that traverses the original
 *       triple finds no outgoing of {@code s} labelled {@code &alpha;} with
 *       target {@code t} &rarr; refused mid-execution &rarr; killed.
 *       Statically: the original triple is absent from {@code M}.</li>
 * </ul>
 *
 * <p><strong>Operators outside this scope</strong> (e.g. a hypothetical
 * {@code TransitionAdd}, {@code StateMissing}, {@code WrongInitialState})
 * DO modify execution semantics — extra transitions can be fired, states
 * can be missing, or the executor can diverge from step 1. The static
 * check is INSUFFICIENT for those; dynamic executor-based replay via
 * VIBeS' {@code TransitionSystemExecutor.canExecute(Action)} is required.
 * That variant is left as future work and would live alongside
 * {@link #kills(List, FeaturedTransitionSystem)} as e.g.
 * {@code killsDynamic(...)} with the same return shape.
 *
 * <h3>Synthetic transitions</h3>
 *
 * <p>Transitions whose action is synthetic (per
 * {@link EulerianBalancer#isSyntheticAction(be.vibes.ts.Action)}) are
 * skipped both when collecting suite triples and when forming the
 * mutant's triple set, because synthetic transitions are not real SUT
 * events and their presence / absence carries no fault-detection signal.
 */
public final class FaultDetector {

    private static final Logger LOG = LoggerFactory.getLogger(FaultDetector.class);

    private FaultDetector() {
    }

    /**
     * Dynamic kill check via {@link TransitionSystemExecutor} replay.
     * Required for operators whose mutation changes the execution semantics
     * of the FTS, not just its transition set: {@link StateMissing}
     * (removed state cannot be reached), {@code WrongInitialState}
     * (replay diverges from step 1), {@code TransitionAdd} (non-determinism
     * in the executor), {@code TransitionDestinationExchange} (executor
     * advances to a different state).
     *
     * <p>Algorithm: for each TestCase in the suite, instantiate a fresh
     * executor on {@code mutant}, walk the test case action by action
     * via {@code canExecute(action)} + {@code execute(action)}. If any
     * step is refused (cannot execute) the mutant is killed. If the
     * executor reaches an end state different from what the original
     * would have, that is also a divergence and kills.
     *
     * <p>Synthetic actions in the test case are SKIPPED (treated as
     * test-case boundary markers, not real SUT events).
     */
    public static boolean killsDynamic(List<TestCase> suite, FeaturedTransitionSystem mutant) {
        for (TestCase tc : suite) {
            TransitionSystemExecutor executor = new TransitionSystemExecutor(mutant);
            try {
                executor.reset();
            } catch (TransitionSystenExecutionException e) {
                return true;
            }
            for (Transition t : tc) {
                String actionName = t.getAction().getName();
                String effectiveActionName;

                if (actionName.startsWith(EulerianBalancer.SYNTHETIC_ACTION_PREFIX)) {
                    // __balance__N — pair-graph balancing fallback synthetic.
                    // Pair-coverage's splitAtSyntheticEdges strips these
                    // before they enter executable suites, so encountering
                    // one here means an upstream invariant was violated.
                    // Skip with a warn — neither advance nor kill.
                    LOG.warn("Unexpected __balance__ action in dynamic replay: {} "
                            + "(this should not occur in executable test cases)",
                            actionName);
                    continue;
                } else if (actionName.contains(EulerianBalancer.DUPLICATE_ACTION_INFIX)) {
                    // __dup__N — duplicate of a real transition (same source /
                    // target / fexpr, only the label is synthetic to survive
                    // VIBeS' dedup). Execute the BASE action so the executor
                    // advances through the real path.
                    effectiveActionName = EulerianBalancer.stripDuplicateSuffix(actionName);
                } else {
                    // Plain real action OR __end__ (which is also a real FTS
                    // transition added by MxeToFtsConverter for mixed-terminal
                    // states; target is the initial state). Either way the
                    // mutant should still carry it (synthetic-site mutants
                    // are filtered upstream), so execute normally.
                    effectiveActionName = actionName;
                }

                Action mutantAction;
                try {
                    mutantAction = mutant.getAction(effectiveActionName);
                } catch (RuntimeException e) {
                    mutantAction = null;
                }
                if (mutantAction == null) {
                    // The action does not exist in the mutant at all (the
                    // entire action was dropped — possible with TM when it
                    // was the action's sole transition, or with SM when the
                    // sole user of the action was the removed state).
                    return true;
                }
                try {
                    if (!executor.canExecute(mutantAction)) {
                        return true;
                    }
                    executor.execute(mutantAction);
                } catch (TransitionSystenExecutionException e) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Same as {@link #scoreSuite(List, java.util.Map)} but uses the
     * dynamic replay check. Use this overload for operators that change
     * execution semantics.
     */
    public static KillResult scoreSuiteDynamic(List<TestCase> suite,
                                               java.util.Map<String, FeaturedTransitionSystem> mutants) {
        int killed = 0;
        java.util.List<String> survivors = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, FeaturedTransitionSystem> e : mutants.entrySet()) {
            if (killsDynamic(suite, e.getValue())) {
                killed++;
            } else {
                survivors.add(e.getKey());
            }
        }
        return new KillResult(killed, mutants.size(), survivors);
    }

    /**
     * Returns {@code true} iff at least one non-synthetic transition
     * exercised by some TestCase in {@code suite} is absent from
     * {@code mutant}'s transition set.
     */
    public static boolean kills(List<TestCase> suite, FeaturedTransitionSystem mutant) {
        Set<String> mutantTriples = nonSyntheticTriples(mutant);
        for (TestCase tc : suite) {
            for (Transition t : tc) {
                if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                    continue;
                }
                if (!mutantTriples.contains(tripleKey(t))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Convenience: count how many of the given mutants are killed by the
     * given suite, and return the keys of the survivors (escaped) in the
     * iteration order of {@code mutants}. Survivor keys are useful for
     * the paper's drill-down analysis on which specific mutations slip
     * past a weak coverage criterion.
     */
    public static KillResult scoreSuite(List<TestCase> suite,
                                        Map<String, FeaturedTransitionSystem> mutants) {
        // Pre-collect the suite's non-synthetic triples once so per-mutant
        // checks reduce to a set difference.
        Set<String> suiteTriples = nonSyntheticTriplesOfSuite(suite);
        int killed = 0;
        List<String> survivors = new ArrayList<>();
        for (Map.Entry<String, FeaturedTransitionSystem> e : mutants.entrySet()) {
            if (killsFromTriples(e.getValue(), suiteTriples)) {
                killed++;
            } else {
                survivors.add(e.getKey());
            }
        }
        return new KillResult(killed, mutants.size(), survivors);
    }

    /**
     * Specialised variant of {@link #kills(List, FeaturedTransitionSystem)}
     * that takes a pre-collected suite-triple set. Avoids re-walking the
     * suite for every mutant when scoring many mutants against the same
     * suite — the common case in mutation-testing experiments.
     */
    public static boolean killsFromTriples(FeaturedTransitionSystem mutant,
                                           Set<String> suiteTriples) {
        Set<String> mutantTriples = nonSyntheticTriples(mutant);
        for (String triple : suiteTriples) {
            if (!mutantTriples.contains(triple)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the set of non-synthetic {@code (source, action, target)}
     * triple keys exercised by the suite. Exposed so callers that score
     * the same suite against many mutants can pay the suite-walk cost
     * once.
     */
    public static Set<String> nonSyntheticTriplesOfSuite(List<TestCase> suite) {
        Set<String> out = new HashSet<>();
        for (TestCase tc : suite) {
            for (Transition t : tc) {
                if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                    continue;
                }
                out.add(tripleKey(t));
            }
        }
        return out;
    }

    private static Set<String> nonSyntheticTriples(FeaturedTransitionSystem fts) {
        Set<String> out = new HashSet<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            Transition t = it.next();
            if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                continue;
            }
            out.add(tripleKey(t));
        }
        return out;
    }

    private static String tripleKey(Transition t) {
        return t.getSource().getName() + "|" + t.getAction().getName()
                + "|" + t.getTarget().getName();
    }

    /**
     * Result triple: how many mutants were killed by the suite, the total
     * mutant count (= killed + survivors.size()), and the keys of mutants
     * that escaped detection.
     */
    public static final class KillResult {
        private final int killed;
        private final int total;
        private final List<String> survivors;

        public KillResult(int killed, int total, List<String> survivors) {
            this.killed = killed;
            this.total = total;
            this.survivors = survivors;
        }

        public int getKilled() {
            return killed;
        }

        public int getTotal() {
            return total;
        }

        /**
         * Score = killed / total, in [0, 1]. Returns 1.0 when there are no
         * mutants (vacuously perfect) so summary aggregations do not need to
         * special-case empty operators.
         */
        public double getScore() {
            return total == 0 ? 1.0 : (double) killed / (double) total;
        }

        public List<String> getSurvivors() {
            return survivors;
        }
    }
}
