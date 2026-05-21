package be.vibes.testgeneration.coverage.baseline;

import be.vibes.fexpression.FExpression;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.Transition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mutable partial walk used as a candidate during branch-and-bound test
 * generation. Tracks the sequence of transitions taken so far, the set of
 * visited states, the conjunction of feature expressions along the walk
 * (for FTS satisfiability validation), and a score used to rank candidates
 * in the priority queue.
 *
 * <p>Functional equivalent of {@code PartialTestCase} from Devroey 2014's
 * VIBeS implementation ({@code f856c90}). The original extended the old
 * {@code TestCase} interface; we instead compose the data structures
 * directly because the current {@code be.vibes.ts.TestCase} is a concrete
 * class with execution-aware semantics and is not suitable as a partial
 * builder.
 *
 * <p>{@link #copy()} returns a deep copy so the BnB algorithm can branch
 * out one candidate into many without aliasing.
 */
public final class BaselineWalk {

    private final List<Transition> transitions;
    private final Set<State> visitedStates;
    private FExpression accumulatedFExpression;
    private State lastState;
    private int score;

    /**
     * Build an empty walk starting at the given initial state. The initial
     * state is counted as visited from the outset, matching the convention
     * of Devroey's branch-and-bound (which removes the initial state from
     * {@code toVisit} before the main loop).
     */
    public BaselineWalk(State initial) {
        this.transitions = new ArrayList<>();
        this.visitedStates = new LinkedHashSet<>();
        this.visitedStates.add(initial);
        this.accumulatedFExpression = FExpression.trueValue();
        this.lastState = initial;
        this.score = 0;
    }

    private BaselineWalk(BaselineWalk other) {
        this.transitions = new ArrayList<>(other.transitions);
        this.visitedStates = new LinkedHashSet<>(other.visitedStates);
        this.accumulatedFExpression = other.accumulatedFExpression;
        this.lastState = other.lastState;
        this.score = other.score;
    }

    /**
     * Returns a deep copy of this walk. Used by the branch-and-bound to
     * fork a candidate into multiple successors without mutating shared
     * state.
     */
    public BaselineWalk copy() {
        return new BaselineWalk(this);
    }

    /**
     * Appends a transition to the walk. The transition's target becomes
     * the new last state. For FTS walks, the transition's feature
     * expression is conjoined into the accumulated FExpression.
     */
    public void append(Transition transition, FeaturedTransitionSystem fts) {
        transitions.add(transition);
        State target = transition.getTarget();
        visitedStates.add(target);
        lastState = target;
        if (fts != null) {
            FExpression edgeFExpr = fts.getFExpression(transition);
            if (edgeFExpr != null && !edgeFExpr.isTrue()) {
                accumulatedFExpression = accumulatedFExpression.and(edgeFExpr);
            }
        }
    }

    /**
     * LTS variant of {@link #append(Transition, FeaturedTransitionSystem)} —
     * skips feature-expression accumulation.
     */
    public void append(Transition transition) {
        append(transition, null);
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public Set<State> getVisitedStates() {
        return visitedStates;
    }

    public FExpression getAccumulatedFExpression() {
        return accumulatedFExpression;
    }

    public State getLastState() {
        return lastState;
    }

    public int length() {
        return transitions.size();
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BaselineWalk[score=").append(score)
                .append(", length=").append(transitions.size())
                .append(", visited=").append(visitedStates.size())
                .append(", actions=[");
        for (int i = 0; i < transitions.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(transitions.get(i).getAction().getName());
        }
        sb.append("]]");
        return sb.toString();
    }
}
