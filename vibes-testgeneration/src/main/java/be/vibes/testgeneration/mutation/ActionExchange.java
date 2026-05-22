package be.vibes.testgeneration.mutation;

import be.vibes.fexpression.FExpression;
import be.vibes.solver.ConstraintIdentifier;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.ts.Action;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Action Exchange" mutation operator (scope-restricted variant,
 * 2026-05-22 decision).
 *
 * <p>For each transition {@code t = (s, α, d)} the operator produces
 * mutants {@code (s, β, d)} — same source, same target, same feature
 * expression, only the action label changes — where the replacement
 * action {@code β} is drawn from the <strong>combined adjacency
 * pool</strong>:
 *
 * <ul>
 *   <li><strong>Source-adjacent:</strong> actions that appear on any
 *       outgoing transition of {@code t.source};</li>
 *   <li><strong>Sequentially-adjacent:</strong> actions that appear on
 *       any outgoing transition of {@code t.target}.</li>
 * </ul>
 *
 * <p>The pool is the union of these two sets, minus {@code α} (no
 * identity mutation) and minus any {@code β} for which {@code (s, β, d)}
 * already exists in the FTS (no no-op mutation that just duplicates an
 * existing edge).
 *
 * <p>If a feature-model solver is provided (see the
 * {@link #ActionExchange(Sat4JSolverFacade)} constructor), the operator
 * additionally applies a <strong>feature-compatibility</strong> filter:
 * the replacement {@code β} is kept iff some transition labelled
 * {@code β} in the original FTS has a feature expression
 * co-satisfiable with the feature expression of {@code t} under the
 * feature model. This excludes mutants like "β fires under a feature
 * condition where β never actually fires in the original SPL", which
 * are arguably not realistic faults.
 *
 * <p>The mutant always changes a SINGLE transition. There is no
 * pairwise swap variant — this matches the competent-programmer
 * single-point fault hypothesis used by the paper.
 *
 * <p>Earlier versions of this class generated the full Cartesian
 * product {@code |T| × (|A| − 1)}, which was inconsistent with the
 * paper's RQ2 statement and scaled poorly (BankAccountv2 produced
 * 285k mutants under the old scope). The current scope-restricted
 * version drops the count substantially while preserving the operator's
 * methodological alignment with the Devroey 2014 fault model.
 */
public class ActionExchange extends MutationOperator {

    private static final Logger LOG = LoggerFactory.getLogger(ActionExchange.class);

    private static final String NAME = "Action Exchange";
    private static final String KEY_PREFIX = "AEX";
    private static final String KEY_SEP = "__";

    /**
     * Optional SAT solver for the feature-compatibility filter. When
     * {@code null} the filter is skipped (all syntactically-valid
     * neighbourhood swaps are emitted). For paper experiments pass a
     * solver constructed from the SPL's feature model so the resulting
     * mutants are restricted to feature-compatible swaps.
     */
    private final Sat4JSolverFacade solver;

    /**
     * Per-(φ_t, action β) feature-compatibility cache. Same φ_t × β pair
     * recurs across many transitions in large SPLs (e.g. BankAccountv2
     * emits a few dozen distinct feature expressions over the full
     * action set, but tens of thousands of mutant candidates) — without
     * a cache the SAT pass dominates runtime. Cleared at the start of
     * each {@link #generateMutants(FeaturedTransitionSystem)} call.
     */
    private final Map<String, Boolean> compatCache = new HashMap<>();

    public ActionExchange() {
        this(null);
    }

    public ActionExchange(Sat4JSolverFacade solver) {
        super(NAME);
        this.solver = solver;
    }

    @Override
    public void generateMutants(FeaturedTransitionSystem fts) {
        compatCache.clear();
        // Pre-index for O(1) lookups during the per-transition candidate
        // enumeration:
        //   outgoingActions(state)         — action set at this state
        //   transitionsByAction(action)    — every transition with this action
        //   existingTriples                — set of (source, action, target)
        Map<State, Set<Action>> outgoingActions = new HashMap<>();
        Map<Action, List<Transition>> transitionsByAction = new HashMap<>();
        Set<String> existingTriples = new HashSet<>();
        List<Transition> allTransitions = new ArrayList<>();
        Iterator<Transition> tIt = fts.transitions();
        while (tIt.hasNext()) {
            Transition t = tIt.next();
            allTransitions.add(t);
            outgoingActions.computeIfAbsent(t.getSource(), k -> new HashSet<>())
                    .add(t.getAction());
            transitionsByAction.computeIfAbsent(t.getAction(), k -> new ArrayList<>())
                    .add(t);
            existingTriples.add(tripleKey(t));
        }

        int skippedExistingTriple = 0;
        int skippedFeatureIncompat = 0;
        for (Transition t : allTransitions) {
            // Combined adjacency pool: source-out ∪ target-out, minus self.
            Set<Action> candidates = new LinkedHashSet<>();
            candidates.addAll(outgoingActions.getOrDefault(t.getSource(), Collections.emptySet()));
            candidates.addAll(outgoingActions.getOrDefault(t.getTarget(), Collections.emptySet()));
            candidates.remove(t.getAction());

            for (Action beta : candidates) {
                String tripleKey = t.getSource().getName() + "|"
                        + beta.getName() + "|"
                        + t.getTarget().getName();
                if (existingTriples.contains(tripleKey)) {
                    skippedExistingTriple++;
                    continue;
                }
                if (!isFeatureCompatible(fts, t, beta, transitionsByAction)) {
                    skippedFeatureIncompat++;
                    continue;
                }
                String key = mutantKey(t, beta);
                FeaturedTransitionSystem mutant =
                        FtsCloning.withReplacedAction(fts, t, beta);
                mutants.put(key, mutant);
            }
        }
        LOG.info("{} produced {} mutants ({} candidates skipped as existing triples, "
                        + "{} skipped as feature-incompatible) from {} transitions",
                NAME, mutants.size(), skippedExistingTriple, skippedFeatureIncompat,
                allTransitions.size());
    }

    /**
     * Feature-compatibility check: is there some transition labelled
     * {@code β} in the FTS whose feature expression is co-satisfiable
     * with the feature expression of {@code t} under the feature model?
     *
     * <p>If no solver was supplied at construction time, returns
     * {@code true} unconditionally (filter disabled).
     *
     * <p>If {@code t}'s feature expression is {@code true} (unconditional
     * transition), every β-transition is trivially compatible.
     */
    private boolean isFeatureCompatible(FeaturedTransitionSystem fts,
                                        Transition t, Action beta,
                                        Map<Action, List<Transition>> byAction) {
        if (solver == null) {
            return true;
        }
        FExpression phiT = fts.getFExpression(t);
        if (phiT == null || phiT.isTrue()) {
            return true;
        }
        String cacheKey = phiT.toString() + "@@" + beta.getName();
        Boolean cached = compatCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        boolean result = false;
        List<Transition> betaTransitions = byAction.getOrDefault(beta, Collections.emptyList());
        for (Transition tPrime : betaTransitions) {
            FExpression phiBeta = fts.getFExpression(tPrime);
            FExpression conjunction;
            if (phiBeta == null || phiBeta.isTrue()) {
                conjunction = phiT;
            } else {
                conjunction = phiT.and(phiBeta).applySimplification();
            }
            if (isSatisfiable(conjunction)) {
                result = true;
                break;
            }
        }
        compatCache.put(cacheKey, result);
        return result;
    }

    private boolean isSatisfiable(FExpression fexpr) {
        if (fexpr == null || fexpr.isTrue()) {
            return true;
        }
        ConstraintIdentifier id = null;
        try {
            id = solver.addConstraint(fexpr);
            return solver.isSatisfiable();
        } catch (Exception e) {
            return false;
        } finally {
            if (id != null) {
                try {
                    solver.removeConstraint(id);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String tripleKey(Transition t) {
        return t.getSource().getName() + "|"
                + t.getAction().getName() + "|"
                + t.getTarget().getName();
    }

    static String mutantKey(Transition t, Action replacement) {
        return KEY_PREFIX + KEY_SEP
                + t.getSource().getName() + KEY_SEP
                + t.getAction().getName() + KEY_SEP
                + replacement.getName() + KEY_SEP
                + t.getTarget().getName();
    }
}
