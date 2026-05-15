package be.vibes.testgeneration.mutation;

import be.vibes.fexpression.FExpression;
import be.vibes.ts.Action;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.FeaturedTransitionSystemFactory;
import be.vibes.ts.State;
import be.vibes.ts.Transition;

import java.util.Iterator;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility for producing modified copies of a
 * {@link FeaturedTransitionSystem} via {@link FeaturedTransitionSystemFactory}.
 *
 * <p>VIBeS' factory-built FTS is immutable in practice (no public
 * remove-transition API), so mutation operators cannot edit an existing
 * FTS in place — they must rebuild a fresh one. This class centralises
 * that "copy with one tiny change" pattern so the operators stay free of
 * boilerplate.
 */
public final class FtsCloning {

    private FtsCloning() {
        // Utility class.
    }

    /**
     * Returns a deep copy of the given FTS.
     */
    public static FeaturedTransitionSystem copy(FeaturedTransitionSystem fts) {
        return rebuild(fts, t -> true, null);
    }

    /**
     * Returns a copy of the given FTS with one specific transition removed.
     *
     * @param fts the original FTS
     * @param transitionToOmit the transition (identity by reference equality) to drop
     */
    public static FeaturedTransitionSystem withoutTransition(FeaturedTransitionSystem fts,
                                                             Transition transitionToOmit) {
        checkNotNull(transitionToOmit, "Transition to omit may not be null");
        return rebuild(fts, t -> !t.equals(transitionToOmit), null);
    }

    /**
     * Returns a copy of the given FTS in which one specific transition is
     * replaced by a transition with the same source / target / feature
     * expression but a different action label.
     *
     * <p>The replacement action must already exist in the source FTS. The
     * caller picks the swap pair; this method only performs the structural
     * substitution.
     */
    public static FeaturedTransitionSystem withReplacedAction(FeaturedTransitionSystem fts,
                                                              Transition target,
                                                              Action replacementAction) {
        checkNotNull(target, "Target transition may not be null");
        checkNotNull(replacementAction, "Replacement action may not be null");
        return rebuild(fts, t -> !t.equals(target),
                new InsertionSpec(target, replacementAction));
    }

    /**
     * Shared rebuild kernel: walks every state / action / transition of the
     * source, applies {@code keep} to decide whether each transition should
     * be carried over verbatim, and finally inserts the (optional) extra
     * transition described by {@code insertion}.
     */
    private static FeaturedTransitionSystem rebuild(FeaturedTransitionSystem fts,
                                                    Predicate<Transition> keep,
                                                    InsertionSpec insertion) {
        FeaturedTransitionSystemFactory factory =
                new FeaturedTransitionSystemFactory(fts.getInitialState().getName());

        // Replay state declarations so the initial-state name is registered
        // and all isolated states (those without outgoing transitions) are
        // also preserved.
        Iterator<State> stateIt = fts.states();
        while (stateIt.hasNext()) {
            factory.addState(stateIt.next().getName());
        }
        Iterator<Action> actionIt = fts.actions();
        while (actionIt.hasNext()) {
            factory.addAction(actionIt.next().getName());
        }

        Iterator<Transition> transitionIt = fts.transitions();
        while (transitionIt.hasNext()) {
            Transition t = transitionIt.next();
            if (!keep.test(t)) {
                continue;
            }
            FExpression fexpr = fts.getFExpression(t);
            if (fexpr == null) {
                fexpr = FExpression.trueValue();
            }
            factory.addTransition(t.getSource().getName(), t.getAction().getName(),
                    fexpr, t.getTarget().getName());
        }

        if (insertion != null) {
            Transition origin = insertion.original;
            FExpression fexpr = fts.getFExpression(origin);
            if (fexpr == null) {
                fexpr = FExpression.trueValue();
            }
            factory.addTransition(origin.getSource().getName(),
                    insertion.replacementAction.getName(), fexpr,
                    origin.getTarget().getName());
        }

        return factory.build();
    }

    private static final class InsertionSpec {
        final Transition original;
        final Action replacementAction;

        InsertionSpec(Transition original, Action replacementAction) {
            this.original = original;
            this.replacementAction = replacementAction;
        }
    }
}
