package be.vibes.testgeneration.mutation;

import be.vibes.ts.Action;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "Action Exchange" mutation operator: for each transition and each
 * alternative action in the FTS' action alphabet, produce a mutant in
 * which the transition's action label is replaced by the alternative.
 * Source state, target state, and feature expression are preserved.
 *
 * <p>This corresponds to the second operator named in the ICTSS abstract.
 * Its ESG-Fx analog is roughly between an edge re-labelling and the
 * "EdgeRedirector" operator, but applied to the action carried by the
 * edge rather than its endpoints.
 *
 * <p>The mutant count is {@code |T| × (|A| − 1)} where {@code T} is the
 * set of transitions and {@code A} is the set of actions of the original
 * FTS. For SVM with 18 transitions and 12 actions this yields 18 × 11 = 198
 * mutants.
 *
 * <p>Mutant keys are
 * {@code "AEX_<source>__<originalAction>__<newAction>__<target>"} so that
 * each mutation site is uniquely addressable in experiment logs.
 */
public class ActionExchange extends MutationOperator {

    private static final Logger LOG = LoggerFactory.getLogger(ActionExchange.class);

    private static final String NAME = "Action Exchange";
    private static final String KEY_PREFIX = "AEX";
    private static final String KEY_SEP = "__";

    public ActionExchange() {
        super(NAME);
    }

    @Override
    public void generateMutants(FeaturedTransitionSystem fts) {
        List<Transition> originalTransitions = new ArrayList<>();
        Iterator<Transition> tIt = fts.transitions();
        while (tIt.hasNext()) {
            originalTransitions.add(tIt.next());
        }

        List<Action> allActions = new ArrayList<>();
        Iterator<Action> aIt = fts.actions();
        while (aIt.hasNext()) {
            allActions.add(aIt.next());
        }

        for (Transition t : originalTransitions) {
            Action originalAction = t.getAction();
            for (Action replacement : allActions) {
                if (replacement.equals(originalAction)) {
                    continue;
                }
                String key = mutantKey(t, replacement);
                FeaturedTransitionSystem mutant =
                        FtsCloning.withReplacedAction(fts, t, replacement);
                mutants.put(key, mutant);
            }
        }
        LOG.info("{} produced {} mutants from {} transitions and {} actions",
                NAME, mutants.size(), originalTransitions.size(), allActions.size());
    }

    static String mutantKey(Transition t, Action replacement) {
        return KEY_PREFIX + KEY_SEP
                + t.getSource().getName() + KEY_SEP
                + t.getAction().getName() + KEY_SEP
                + replacement.getName() + KEY_SEP
                + t.getTarget().getName();
    }
}
