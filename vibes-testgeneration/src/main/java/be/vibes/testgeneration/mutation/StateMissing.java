package be.vibes.testgeneration.mutation;

import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "State Missing" mutation operator: produces one mutant FTS per
 * non-initial state of the original, with that state and all transitions
 * incident to it (incoming + outgoing) removed.
 *
 * <p>This is the third operator in the {@link MutationOperator} set used
 * by the ICTSS paper, corresponding to Devroey et al.'s (2014 SPLC §4.1)
 * "faulty state" fault class. The other two are {@link TransitionMissing}
 * (faulty transition) and {@link ActionExchange} (faulty action). Together
 * they cover the three fault categories Devroey 2014 uses for fault
 * seeding.
 *
 * <p>The initial state is NEVER mutated — removing it would leave the
 * FTS without an initial state, which is structurally invalid and the
 * algorithms downstream would reject. (vibes-mutation's StateMissing
 * implementation throws when the strategy selects the initial state;
 * the wrong-initial-state fault class is covered by a separate
 * {@code WrongInitialState} operator.)
 *
 * <p>Unlike {@link TransitionMissing} and {@link ActionExchange},
 * StateMissing changes the execution semantics of the FTS (not just its
 * transition set): a test that would have visited the removed state
 * cannot replay correctly on the mutant. The static triple-set kill
 * check in {@code FaultDetector.kills(...)} is INSUFFICIENT for
 * StateMissing mutants — use {@code FaultDetector.killsDynamic(...)},
 * which replays via {@code TransitionSystemExecutor} and detects
 * refused mid-execution steps.
 *
 * <p>Mutant keys are formatted as {@code "SM_<state>"}, stable across
 * runs and grep-friendly in experiment logs.
 */
public class StateMissing extends MutationOperator {

    private static final Logger LOG = LoggerFactory.getLogger(StateMissing.class);

    private static final String NAME = "State Missing";
    private static final String KEY_PREFIX = "SM";
    private static final String KEY_SEP = "_";

    public StateMissing() {
        super(NAME);
    }

    @Override
    public void generateMutants(FeaturedTransitionSystem fts) {
        State initial = fts.getInitialState();
        // Snapshot states up-front — the per-mutant rebuild creates a new
        // FTS and we must not interact with iteration order on the original.
        List<State> candidates = new ArrayList<>();
        Iterator<State> sIt = fts.states();
        while (sIt.hasNext()) {
            State s = sIt.next();
            if (s.equals(initial)) {
                continue;
            }
            candidates.add(s);
        }

        for (State victim : candidates) {
            String key = mutantKey(victim);
            FeaturedTransitionSystem mutant = FtsCloning.withoutState(fts, victim);
            mutants.put(key, mutant);
        }
        LOG.info("{} produced {} mutants from {} non-initial states",
                NAME, mutants.size(), candidates.size());
    }

    static String mutantKey(State state) {
        return KEY_PREFIX + KEY_SEP + state.getName();
    }
}
