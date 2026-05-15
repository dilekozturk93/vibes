package be.vibes.testgeneration.mutation;

import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "Transition Missing" mutation operator: produces one mutant
 * {@link FeaturedTransitionSystem} per transition of the original, with
 * that transition removed.
 *
 * <p>This is the FTS analog of the ESG-Fx-side {@code EdgeOmitter} used in
 * the user's prior published study and is one of the two operators
 * referenced by the ICTSS abstract.
 *
 * <p>Mutant keys are formatted as {@code "TM_<source>__<action>__<target>"},
 * which is stable across runs and easy to grep for in experiment logs.
 */
public class TransitionMissing extends MutationOperator {

    private static final Logger LOG = LoggerFactory.getLogger(TransitionMissing.class);

    private static final String NAME = "Transition Missing";
    private static final String KEY_PREFIX = "TM";
    private static final String KEY_SEP = "__";

    public TransitionMissing() {
        super(NAME);
    }

    @Override
    public void generateMutants(FeaturedTransitionSystem fts) {
        // Snapshot the original transitions so the per-mutant rebuild cannot
        // interact with iteration order.
        List<Transition> originalTransitions = new ArrayList<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            originalTransitions.add(it.next());
        }

        for (Transition t : originalTransitions) {
            String key = mutantKey(t);
            FeaturedTransitionSystem mutant = FtsCloning.withoutTransition(fts, t);
            mutants.put(key, mutant);
        }
        LOG.info("{} produced {} mutants on input with {} transitions",
                NAME, mutants.size(), originalTransitions.size());
    }

    /** Builds the stable key used for the mutant containing this transition omitted. */
    static String mutantKey(Transition t) {
        return KEY_PREFIX + KEY_SEP
                + t.getSource().getName() + KEY_SEP
                + t.getAction().getName() + KEY_SEP
                + t.getTarget().getName();
    }
}
