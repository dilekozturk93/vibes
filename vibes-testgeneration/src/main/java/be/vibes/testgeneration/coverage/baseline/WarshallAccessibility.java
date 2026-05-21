package be.vibes.testgeneration.coverage.baseline;

import be.vibes.ts.State;
import be.vibes.ts.Transition;
import be.vibes.ts.TransitionSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Boolean accessibility matrix computed via the modified Warshall algorithm
 * of Devroey et al., SPLC 2014, Algorithm 1.
 *
 * <p>Functional equivalent of {@code WarshallScoreComputor} from the
 * original VIBeS commit {@code f856c90}; rewritten against the
 * {@code be.vibes.ts.*} API and stripped of the abstract {@code
 * ScoreComputor} interface (callers query the matrix directly).
 *
 * <p>Paper Algorithm 1 propagates feature expressions cell-by-cell; for
 * scalability the original implementation (see paper §3.1, "Simplification
 * for large models") instead uses a boolean accessibility matrix and defers
 * feature-expression satisfiability to test-case validation time. We preserve
 * that simplification — the matrix entry {@code M[from][to]} is simply
 * "is {@code to} reachable from {@code from} along some path of length ≤ L?"
 * where {@code L} is the number of Warshall iterations applied.
 *
 * <p>Initialisation rule (matches the original): every transition
 * {@code s → t} contributes an entry {@code M[s][t] = true}, EXCEPT
 * transitions whose target is the initial state. Excluding back-edges to
 * INIT keeps the matrix from over-counting trivially-closeable walks; the
 * algorithm picks back-to-INIT transitions explicitly during the score-zero
 * close phase, not via the matrix.
 *
 * <p>Iteration cost is O(|S|³) per Warshall pass; {@code iterate(k)} runs
 * the closure {@code k} times. The paper's recommendation is {@code k = 5}
 * for the case studies (sufficient depth for most reachability), but the
 * underlying transitive closure converges at {@code k = ⌈log₂(|S|)⌉}.
 */
public final class WarshallAccessibility {

    private static final Logger LOG = LoggerFactory.getLogger(WarshallAccessibility.class);

    private final Map<State, Set<State>> matrix = new HashMap<>();
    private final Set<State> allStates = new HashSet<>();
    private int iterations = 0;

    private WarshallAccessibility() {
    }

    /**
     * Build a matrix initialised with one-hop reachability for the given
     * transition system. Caller is expected to call {@link #iterate(int)}
     * to extend the closure depth before querying.
     */
    public static WarshallAccessibility forSystem(TransitionSystem ts) {
        WarshallAccessibility w = new WarshallAccessibility();
        // Snapshot every state — including states with no outgoing seed
        // entry — so the iteration considers them as potential transitive
        // targets (j) as well as potential intermediate steps (k).
        Iterator<State> sIt = ts.states();
        while (sIt.hasNext()) {
            w.allStates.add(sIt.next());
        }
        State initial = ts.getInitialState();
        Iterator<Transition> tIt = ts.transitions();
        while (tIt.hasNext()) {
            Transition t = tIt.next();
            if (t.getTarget().equals(initial)) {
                // Per the original WarshallScoreComputor.initilise: do NOT
                // seed entries that go back to the initial state. The score
                // heuristic uses the matrix to estimate how many NEW states
                // a walk would unlock; back-edges to INIT are not new state
                // unlocks.
                continue;
            }
            w.put(t.getSource(), t.getTarget(), true);
        }
        return w;
    }

    /**
     * Extends the closure depth by running the boolean Warshall iteration
     * {@code times} times. Each iteration: for every (i, j, k) triple,
     * {@code M[i][j] |= (M[i][k] AND M[k][j])}.
     */
    public void iterate(int times) {
        for (int i = 0; i < times; i++) {
            iterateOnce();
        }
    }

    private void iterateOnce() {
        LOG.debug("Running Warshall iteration {}", iterations + 1);
        // i, j, k must range over ALL states of the TS, not just those
        // with seed entries — a state with no outgoing edges (but reachable
        // as a target) is still a valid j; conversely, a state with no
        // incoming seed entries is still a valid intermediate k once
        // transitive entries land on it during iteration.
        for (State k : allStates) {
            for (State i : allStates) {
                if (!get(i, k)) continue;
                for (State j : allStates) {
                    if (get(k, j)) {
                        put(i, j, true);
                    }
                }
            }
        }
        iterations++;
    }

    /**
     * Returns {@code true} iff there is a path from {@code from} to
     * {@code to} of length at most {@link #getIterations()} + 1 in the
     * transition system the matrix was built from.
     */
    public boolean isAccessible(State from, State to) {
        return get(from, to);
    }

    /**
     * Counts how many states of {@code candidates} are accessible from
     * {@code from}. This is the score-heuristic Devroey 2014 uses to rank
     * candidate walks during the branch-and-bound: a higher count means the
     * walk's last state has more uncovered targets within reach.
     */
    public int countAccessible(State from, Set<State> candidates) {
        int score = 0;
        for (State c : candidates) {
            if (get(from, c)) {
                score++;
            }
        }
        return score;
    }

    public int getIterations() {
        return iterations;
    }

    private boolean get(State from, State to) {
        Set<State> set = matrix.get(from);
        return set != null && set.contains(to);
    }

    private void put(State from, State to, boolean accessible) {
        if (accessible) {
            matrix.computeIfAbsent(from, k -> new HashSet<>()).add(to);
        } else {
            Set<State> set = matrix.get(from);
            if (set != null) set.remove(to);
        }
    }
}
