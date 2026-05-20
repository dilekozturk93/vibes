package be.vibes.testgeneration.product;

import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.ts.State;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a long Euler-cycle test case into smaller "trip" test cases —
 * one per round-trip from the initial state back to the initial state.
 *
 * <p>Rationale: a test case in the operational sense is a single execution
 * starting from the system's reset / initial state and running until the
 * system returns to that state. Hierholzer's algorithm produces one
 * contiguous cycle that visits the initial state many times (every Eulerian
 * cycle on a strongly-connected graph does); each pass through the initial
 * state is a natural test-case boundary. The cycle is the union of all
 * these trips strung together; splitting recovers the per-trip view.
 *
 * <p>Display rules applied alongside the split:
 * <ul>
 *   <li>{@code __end__} and {@code __balance__N} transitions are HIDDEN
 *       from the displayed action sequence. They are synthetic "return to
 *       initial" markers and do not correspond to any real SUT event the
 *       tester would perform. They still cause a test-case boundary because
 *       their target is the initial state.</li>
 *   <li>{@code <action>__dup__N} transitions show their BASE action name.
 *       A doubled traversal is a real test step that happens to share the
 *       action label with another transition (the second time the tester
 *       performs the action); the {@code __dup__} suffix is implementation
 *       detail of how the balancer ensures unique edge identity in the FTS.</li>
 * </ul>
 *
 * <p>Note: this class only changes how a TestCase is RENDERED. The underlying
 * VIBeS {@link TestCase} object and {@link Transition} objects are unmodified
 * — coverage measurement, mutation testing, and CSV metrics all continue to
 * see the raw Euler cycle including every synthetic action.
 */
public final class TestCaseSplitter {

    private TestCaseSplitter() {
    }

    /**
     * Splits the given cycle at every visit to the initial state. Each
     * trip starts at the initial state and ends with the transition that
     * returned the cycle to the initial state (that transition is included
     * in the trip — it is the observable last action that brought the SUT
     * back to reset). Empty trips (e.g. a lone {@code __end__} not preceded
     * by any real action) are omitted.
     *
     * @param cycle a closed Euler-cycle TestCase produced by one of the
     *              coverage generators
     * @param initial the initial state of the projected / repaired FTS
     * @return list of trips, each a contiguous slice of the cycle
     */
    public static List<List<Transition>> splitAtInitialReturns(TestCase cycle, State initial) {
        List<List<Transition>> trips = new ArrayList<>();
        List<Transition> current = new ArrayList<>();
        for (Transition t : cycle) {
            current.add(t);
            if (t.getTarget().equals(initial)) {
                if (!current.isEmpty()) {
                    trips.add(current);
                }
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            // Tail that did not close at initial — only happens if the cycle
            // is not a proper closed Euler tour (shouldn't, given balancing).
            trips.add(current);
        }
        return trips;
    }

    /**
     * Renders a trip as a display-friendly list of action names, with
     * {@code __end__} / {@code __balance__N} hidden and {@code __dup__N}
     * suffixes stripped. The returned list may be empty if every transition
     * in the trip was a hidden synthetic.
     */
    public static List<String> renderTripActions(List<Transition> trip) {
        List<String> out = new ArrayList<>();
        for (Transition t : trip) {
            String name = t.getAction().getName();
            if (isHiddenSynthetic(name)) {
                continue;
            }
            if (name.contains(EulerianBalancer.DUPLICATE_ACTION_INFIX)) {
                name = EulerianBalancer.stripDuplicateSuffix(name);
            }
            out.add(name);
        }
        return out;
    }

    /**
     * True for synthetic actions that should be HIDDEN from the displayed
     * test sequence: {@code __end__} (mixed-terminal back-to-INIT) and
     * {@code __balance__N} (direct synthetic fallback). Doubled actions
     * ({@code __dup__N}) are NOT hidden — they're real actions the tester
     * performs a second time.
     */
    public static boolean isHiddenSynthetic(String actionName) {
        return actionName.startsWith("__end__")
                || actionName.startsWith(EulerianBalancer.SYNTHETIC_ACTION_PREFIX);
    }
}
