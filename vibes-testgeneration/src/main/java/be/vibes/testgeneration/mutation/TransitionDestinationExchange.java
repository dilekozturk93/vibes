package be.vibes.testgeneration.mutation;

import be.vibes.fexpression.FExpression;
import be.vibes.solver.ConstraintIdentifier;
import be.vibes.solver.Sat4JSolverFacade;
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
 * "Transition Destination Exchange" mutation operator.
 *
 * <p>For each transition {@code t = (s, α, d)} the operator produces
 * mutants {@code (s, α, d')} — same source, same action, same feature
 * expression, only the target state changes — where the replacement
 * target {@code d'} is drawn from the <strong>combined adjacency
 * pool</strong> (the state-level analogue of
 * {@link ActionExchange}'s action-level pool):
 *
 * <ul>
 *   <li><strong>Source-adjacent:</strong> targets of any outgoing
 *       transition of {@code t.source} — alternative destinations the
 *       source can already reach in one step;</li>
 *   <li><strong>Sequentially-adjacent:</strong> targets of any outgoing
 *       transition of {@code t.target} — destinations the developer
 *       might have meant to reach via one more step ("off-by-one"
 *       redirection fault).</li>
 * </ul>
 *
 * <p>The pool is the union of these two sets, minus {@code d} itself
 * (no identity mutation) and minus any {@code d'} for which
 * {@code (s, α, d')} already exists in the FTS (no no-op mutation that
 * just duplicates an existing edge).
 *
 * <p>If a feature-model solver is provided (see the
 * {@link #TransitionDestinationExchange(Sat4JSolverFacade)} constructor),
 * the operator additionally applies a <strong>feature-compatibility</strong>
 * filter: the replacement {@code d'} is kept iff some transition incident
 * to {@code d'} (incoming or outgoing) in the original FTS has a feature
 * expression co-satisfiable with the feature expression of {@code t}
 * under the feature model. This excludes mutants like "the transition
 * fires under a feature condition where {@code d'} is itself unreachable",
 * which are not realistic faults.
 *
 * <p>The mutant always changes a SINGLE transition's target. This matches
 * the competent-programmer single-point fault hypothesis used by the
 * paper, paralleling {@link ActionExchange}.
 *
 * <p><strong>Why TDE matters for the paper.</strong> Unlike
 * {@link TransitionMissing} and {@link ActionExchange}, where the kill
 * criterion ({@code canExecute} refusal mid-replay) coincides with
 * transition coverage on deterministic FTSs, TDE leaves the original
 * action label intact — replay continues past the mutated site and only
 * diverges if a downstream pair {@code (t, t_next)} cannot fire from
 * {@code d'}. Pair coverage suites exercise EVERY {@code t}-outgoing
 * pair {@code (t, t_{next,i})} as a separate consecutive sequence,
 * giving them combinatorially more opportunities to surface that
 * divergence than transition coverage suites (which fix a single
 * arbitrary follow-up to {@code t}). TDE is therefore the operator
 * where pair coverage's marginal detection advantage over transition
 * coverage is expected to surface.
 */
public class TransitionDestinationExchange extends MutationOperator {

    private static final Logger LOG = LoggerFactory.getLogger(TransitionDestinationExchange.class);

    private static final String NAME = "Transition Destination Exchange";
    private static final String KEY_PREFIX = "TDE";
    private static final String KEY_SEP = "__";

    /**
     * Optional SAT solver for the feature-compatibility filter. When
     * {@code null} the filter is skipped (all syntactically-valid
     * neighbourhood redirections are emitted). For paper experiments
     * pass a solver constructed from the SPL's feature model so the
     * resulting mutants are restricted to feature-compatible
     * redirections.
     */
    private final Sat4JSolverFacade solver;

    /**
     * Per-(φ_t, state d') feature-compatibility cache. The value only
     * depends on the FM-level co-satisfiability of {@code φ_t} with the
     * disjunction of feature expressions on transitions incident to
     * {@code d'}, so it is safe to reuse across products of the same
     * SPL — same FM, same φ pool, same state pool. The cache is NOT
     * cleared by {@link #generateMutants(FeaturedTransitionSystem)} on
     * purpose so the caller can amortise SAT cost across all per-product
     * FTSs of an SPL by reusing a single operator instance (matches the
     * {@link ActionExchange} pattern).
     *
     * <p>Call {@link #clearCompatCache()} when reusing the operator
     * across SPLs with different feature models.
     */
    private final Map<String, Boolean> compatCache = new HashMap<>();

    public void clearCompatCache() {
        compatCache.clear();
    }

    public TransitionDestinationExchange() {
        this(null);
    }

    public TransitionDestinationExchange(Sat4JSolverFacade solver) {
        super(NAME);
        this.solver = solver;
    }

    @Override
    public void generateMutants(FeaturedTransitionSystem fts) {
        // Note: compatCache is intentionally NOT cleared here. See the
        // field's JavaDoc — the cache is FM-determined and is reused
        // across per-product calls within a single SPL.
        super.mutants.clear();

        // Pre-index for O(1) lookups during candidate enumeration:
        //   outgoingTargets(state)         — destination set at this state
        //   transitionsByTouchedState(state) — every transition touching this state
        //                                      (i.e. incoming or outgoing)
        //   existingTriples                — set of (source, action, target)
        Map<State, Set<State>> outgoingTargets = new HashMap<>();
        Map<State, List<Transition>> transitionsByTouchedState = new HashMap<>();
        Set<String> existingTriples = new HashSet<>();
        List<Transition> allTransitions = new ArrayList<>();
        Iterator<Transition> tIt = fts.transitions();
        while (tIt.hasNext()) {
            Transition t = tIt.next();
            allTransitions.add(t);
            outgoingTargets.computeIfAbsent(t.getSource(), k -> new HashSet<>())
                    .add(t.getTarget());
            transitionsByTouchedState.computeIfAbsent(t.getSource(),
                    k -> new ArrayList<>()).add(t);
            transitionsByTouchedState.computeIfAbsent(t.getTarget(),
                    k -> new ArrayList<>()).add(t);
            existingTriples.add(tripleKey(t));
        }

        int skippedExistingTriple = 0;
        int skippedFeatureIncompat = 0;
        for (Transition t : allTransitions) {
            // Combined adjacency pool: targets reachable from source in 1
            // step ∪ targets reachable from current target in 1 step,
            // minus the current target itself.
            Set<State> candidates = new LinkedHashSet<>();
            candidates.addAll(outgoingTargets.getOrDefault(t.getSource(),
                    Collections.emptySet()));
            candidates.addAll(outgoingTargets.getOrDefault(t.getTarget(),
                    Collections.emptySet()));
            candidates.remove(t.getTarget());

            for (State dPrime : candidates) {
                String triple = t.getSource().getName() + "|"
                        + t.getAction().getName() + "|"
                        + dPrime.getName();
                if (existingTriples.contains(triple)) {
                    skippedExistingTriple++;
                    continue;
                }
                if (!isFeatureCompatible(fts, t, dPrime, transitionsByTouchedState)) {
                    skippedFeatureIncompat++;
                    continue;
                }
                String key = mutantKey(t, dPrime);
                FeaturedTransitionSystem mutant =
                        FtsCloning.withReplacedTarget(fts, t, dPrime);
                mutants.put(key, mutant);
            }
        }
        LOG.info("{} produced {} mutants ({} candidates skipped as existing triples, "
                        + "{} skipped as feature-incompatible) from {} transitions",
                NAME, mutants.size(), skippedExistingTriple, skippedFeatureIncompat,
                allTransitions.size());
    }

    /**
     * Feature-compatibility check: is there some transition incident to
     * {@code dPrime} in the FTS (incoming or outgoing) whose feature
     * expression is co-satisfiable with the feature expression of
     * {@code t} under the feature model? If yes, {@code dPrime} is
     * reachable in some product where {@code t} also fires, so the
     * redirection is a realistic fault.
     *
     * <p>If no solver was supplied, returns {@code true} unconditionally
     * (filter disabled). If {@code t}'s feature expression is
     * {@code true} (unconditional), every incident transition is
     * trivially compatible.
     */
    private boolean isFeatureCompatible(FeaturedTransitionSystem fts,
                                        Transition t, State dPrime,
                                        Map<State, List<Transition>> touchedByState) {
        if (solver == null) {
            return true;
        }
        FExpression phiT = fts.getFExpression(t);
        if (phiT == null || phiT.isTrue()) {
            return true;
        }
        String cacheKey = phiT.toString() + "@@" + dPrime.getName();
        Boolean cached = compatCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        boolean result = false;
        List<Transition> incident = touchedByState.getOrDefault(dPrime,
                Collections.emptyList());
        for (Transition tPrime : incident) {
            FExpression phiInc = fts.getFExpression(tPrime);
            FExpression conjunction;
            if (phiInc == null || phiInc.isTrue()) {
                conjunction = phiT;
            } else {
                conjunction = phiT.and(phiInc).applySimplification();
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

    static String mutantKey(Transition t, State replacementTarget) {
        return KEY_PREFIX + KEY_SEP
                + t.getSource().getName() + KEY_SEP
                + t.getAction().getName() + KEY_SEP
                + t.getTarget().getName() + KEY_SEP
                + replacementTarget.getName();
    }
}
