package be.vibes.testgeneration.graph;

import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.TransitionSystem;
import be.vibes.ts.TransitionSystemFactory;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link StronglyConnectedComponents}. Covers a handful of toy
 * graphs (single state, two-state cycle, disjoint components, DAG-shaped
 * non-trivial example) and a real SPL — the SVM FTS produced by the M0
 * converter — which is expected to be strongly connected by construction
 * (all dead-end paths in the ESG were rewired back to INIT).
 */
public class StronglyConnectedComponentsTest {

    @Test
    public void singleState_isOneScc() {
        TransitionSystemFactory factory = new TransitionSystemFactory("only");
        TransitionSystem ts = factory.build();

        List<Set<State>> sccs = StronglyConnectedComponents.compute(ts);

        assertEquals(1, sccs.size());
        assertEquals(1, sccs.get(0).size());
        assertTrue(StronglyConnectedComponents.isStronglyConnected(ts));
    }

    @Test
    public void twoStateCycle_isOneScc() {
        TransitionSystemFactory factory = new TransitionSystemFactory("a");
        factory.addState("b");
        factory.addAction("toB");
        factory.addAction("toA");
        factory.addTransition("a", "toB", "b");
        factory.addTransition("b", "toA", "a");
        TransitionSystem ts = factory.build();

        List<Set<State>> sccs = StronglyConnectedComponents.compute(ts);
        assertEquals(1, sccs.size());
        assertEquals(2, sccs.get(0).size());
        assertTrue(StronglyConnectedComponents.isStronglyConnected(ts));
    }

    @Test
    public void linearChain_eachStateIsOwnScc() {
        TransitionSystemFactory factory = new TransitionSystemFactory("a");
        factory.addState("b");
        factory.addState("c");
        factory.addAction("step");
        factory.addTransition("a", "step", "b");
        factory.addTransition("b", "step", "c");
        TransitionSystem ts = factory.build();

        List<Set<State>> sccs = StronglyConnectedComponents.compute(ts);

        assertEquals(3, sccs.size());
        for (Set<State> scc : sccs) {
            assertEquals(1, scc.size());
        }
        assertFalse(StronglyConnectedComponents.isStronglyConnected(ts));
    }

    @Test
    public void disjointCycles_areSeparateSccs() {
        // Two independent 2-cycles {a, b} and {c, d} that share no transitions.
        TransitionSystemFactory factory = new TransitionSystemFactory("a");
        factory.addStates("b", "c", "d");
        factory.addAction("ab");
        factory.addAction("ba");
        factory.addAction("cd");
        factory.addAction("dc");
        factory.addTransition("a", "ab", "b");
        factory.addTransition("b", "ba", "a");
        factory.addTransition("c", "cd", "d");
        factory.addTransition("d", "dc", "c");
        TransitionSystem ts = factory.build();

        List<Set<State>> sccs = StronglyConnectedComponents.compute(ts);

        assertEquals(2, sccs.size());
        for (Set<State> scc : sccs) {
            assertEquals(2, scc.size());
        }
    }

    @Test
    public void cycleWithDanglingTail_keepsTailAsSeparateScc() {
        // Cycle {a, b} plus a state c that a points to but never returns.
        TransitionSystemFactory factory = new TransitionSystemFactory("a");
        factory.addStates("b", "c");
        factory.addAction("ab");
        factory.addAction("ba");
        factory.addAction("toC");
        factory.addTransition("a", "ab", "b");
        factory.addTransition("b", "ba", "a");
        factory.addTransition("a", "toC", "c");
        TransitionSystem ts = factory.build();

        List<Set<State>> sccs = StronglyConnectedComponents.compute(ts);

        assertEquals(2, sccs.size());
        Set<Integer> sizes = new HashSet<>();
        for (Set<State> scc : sccs) {
            sizes.add(scc.size());
        }
        assertThat(sizes, containsInAnyOrder(1, 2));
        assertFalse(StronglyConnectedComponents.isStronglyConnected(ts));
    }

    @Test
    public void containing_findsSccForKnownState() {
        TransitionSystemFactory factory = new TransitionSystemFactory("a");
        factory.addState("b");
        factory.addAction("ab");
        factory.addAction("ba");
        factory.addTransition("a", "ab", "b");
        factory.addTransition("b", "ba", "a");
        TransitionSystem ts = factory.build();

        Set<State> scc = StronglyConnectedComponents.containing(ts, ts.getState("a"));
        assertThat(scc, is(notNullValue()));
        assertEquals(2, scc.size());
    }

    @Test
    public void containing_returnsNullForForeignState() {
        TransitionSystemFactory factory = new TransitionSystemFactory("a");
        TransitionSystem ts = factory.build();

        TransitionSystemFactory other = new TransitionSystemFactory("alien");
        TransitionSystem otherTs = other.build();

        assertNull(StronglyConnectedComponents.containing(ts, otherTs.getState("alien")));
    }

    @Test
    public void svmConvertedFts_isStronglyConnected() throws Exception {
        // SVM is well-behaved: every termination flows through a
        // fully-terminal vertex (return/c, take/f, close/!f), all of which
        // the converter merges into INIT. Result: a single SCC at SPL level.
        FeaturedTransitionSystem fts = convertResource("cases/SodaVendingMachine/SVM_ESGFx.mxe");
        assertTrue("SVM converted FTS must be strongly connected by construction",
                StronglyConnectedComponents.isStronglyConnected(fts));
        List<Set<State>> sccs = StronglyConnectedComponents.compute(fts);
        assertEquals(1, sccs.size());
    }

    @Test
    public void emailConvertedFts_hasNonTrivialSccDecomposition() throws Exception {
        // eMail and Elevator have "mixed terminal" vertices (vertices with
        // BOTH a "]" successor and at least one non-"]" successor). The current
        // converter drops u -> "]" edges, which leaves some states with no path
        // back to INIT and therefore breaks SPL-level strong connectivity.
        // This is expected and acceptable; the EulerianBalancer (M3) and the
        // projection-time SCC repair handle it. We only assert here that the
        // algorithm runs and reports a non-empty decomposition.
        FeaturedTransitionSystem fts = convertResource("cases/eMail/eM_ESGFx.mxe");
        List<Set<State>> sccs = StronglyConnectedComponents.compute(fts);
        assertTrue("eMail decomposition must cover at least one state", sccs.size() >= 1);
        assertTotalStatesCovered(fts, sccs);
    }

    @Test
    public void elevatorConvertedFts_hasNonTrivialSccDecomposition() throws Exception {
        FeaturedTransitionSystem fts = convertResource("cases/Elevator/El_ESGFx.mxe");
        List<Set<State>> sccs = StronglyConnectedComponents.compute(fts);
        assertTrue("Elevator decomposition must cover at least one state", sccs.size() >= 1);
        assertTotalStatesCovered(fts, sccs);
    }

    private static void assertTotalStatesCovered(FeaturedTransitionSystem fts, List<Set<State>> sccs) {
        int stateCount = 0;
        Iterator<State> it = fts.states();
        while (it.hasNext()) {
            it.next();
            stateCount++;
        }
        int sum = 0;
        for (Set<State> scc : sccs) {
            sum += scc.size();
        }
        assertEquals("Every state must belong to exactly one SCC", stateCount, sum);
    }

    private FeaturedTransitionSystem convertResource(String resourcePath) throws Exception {
        URL url = getClass().getClassLoader().getResource(resourcePath);
        assertThat("Resource must be on the classpath: " + resourcePath, url, is(notNullValue()));
        File mxeFile = new File(url.toURI());
        return new MxeToFtsConverter().convert(mxeFile);
    }
}
