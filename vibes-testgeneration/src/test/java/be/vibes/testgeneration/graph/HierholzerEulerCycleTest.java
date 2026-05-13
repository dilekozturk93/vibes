package be.vibes.testgeneration.graph;

import be.vibes.fexpression.FExpression;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.FeaturedTransitionSystemFactory;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HierholzerEulerCycleTest {

    @Test
    public void singleTwoStateCycle_isEulerCycle() {
        FeaturedTransitionSystemFactory factory = new FeaturedTransitionSystemFactory("a");
        factory.addState("b");
        factory.addAction("ab");
        factory.addAction("ba");
        factory.addTransition("a", "ab", FExpression.trueValue(), "b");
        factory.addTransition("b", "ba", FExpression.trueValue(), "a");
        FeaturedTransitionSystem fts = factory.build();

        List<Transition> cycle = HierholzerEulerCycle.compute(fts);
        assertEquals(2, cycle.size());
        assertCoversEveryTransition(fts, cycle);
        assertEquals("a", cycle.get(0).getSource().getName());
        assertEquals("a", cycle.get(cycle.size() - 1).getTarget().getName());
    }

    @Test
    public void balancedFourState_isEulerCycle() {
        // Two parallel two-cycles sharing the start vertex 'a'. Balanced:
        // a has in=out=2, b/c each in=out=1.
        FeaturedTransitionSystemFactory factory = new FeaturedTransitionSystemFactory("a");
        factory.addStates("b", "c");
        factory.addAction("ab");
        factory.addAction("ba");
        factory.addAction("ac");
        factory.addAction("ca");
        factory.addTransition("a", "ab", FExpression.trueValue(), "b");
        factory.addTransition("b", "ba", FExpression.trueValue(), "a");
        factory.addTransition("a", "ac", FExpression.trueValue(), "c");
        factory.addTransition("c", "ca", FExpression.trueValue(), "a");
        FeaturedTransitionSystem fts = factory.build();

        List<Transition> cycle = HierholzerEulerCycle.compute(fts);
        assertEquals(4, cycle.size());
        assertCoversEveryTransition(fts, cycle);
    }

    @Test
    public void unbalancedGraph_isRejected() {
        FeaturedTransitionSystemFactory factory = new FeaturedTransitionSystemFactory("a");
        factory.addStates("b");
        factory.addAction("ab");
        // Single edge a->b: a has out=1 in=0, b has out=0 in=1. Unbalanced.
        // Also not strongly connected — both preconditions fail, but verifying
        // is-balanced runs first.
        factory.addTransition("a", "ab", FExpression.trueValue(), "b");
        FeaturedTransitionSystem fts = factory.build();

        try {
            HierholzerEulerCycle.compute(fts);
            fail("Expected IllegalArgumentException for unbalanced FTS");
        } catch (IllegalArgumentException expected) {
            assertTrue("error message should mention balance",
                    expected.getMessage().toLowerCase().contains("balance"));
        }
    }

    @Test
    public void balancedButNotStronglyConnected_isRejected() {
        // Two disjoint two-cycles: each is locally balanced, but the FTS is
        // not strongly connected as a whole. Hierholzer must reject because
        // there is no single Euler cycle.
        FeaturedTransitionSystemFactory factory = new FeaturedTransitionSystemFactory("a");
        factory.addStates("b", "c", "d");
        factory.addAction("ab");
        factory.addAction("ba");
        factory.addAction("cd");
        factory.addAction("dc");
        factory.addTransition("a", "ab", FExpression.trueValue(), "b");
        factory.addTransition("b", "ba", FExpression.trueValue(), "a");
        factory.addTransition("c", "cd", FExpression.trueValue(), "d");
        factory.addTransition("d", "dc", FExpression.trueValue(), "c");
        FeaturedTransitionSystem fts = factory.build();

        try {
            HierholzerEulerCycle.compute(fts);
            fail("Expected IllegalArgumentException for disconnected FTS");
        } catch (IllegalArgumentException expected) {
            assertTrue("error message should mention strong connectivity",
                    expected.getMessage().toLowerCase().contains("strongly connected"));
        }
    }

    @Test
    public void asActionSequence_extractsActionNamesInOrder() {
        FeaturedTransitionSystemFactory factory = new FeaturedTransitionSystemFactory("a");
        factory.addState("b");
        factory.addAction("ab");
        factory.addAction("ba");
        factory.addTransition("a", "ab", FExpression.trueValue(), "b");
        factory.addTransition("b", "ba", FExpression.trueValue(), "a");
        FeaturedTransitionSystem fts = factory.build();

        List<Transition> cycle = HierholzerEulerCycle.compute(fts);
        List<String> actions = HierholzerEulerCycle.asActionSequence(cycle);
        assertEquals(2, actions.size());
        // The exact ordering depends on FTS iteration order, but the action
        // multiset must be exactly {ab, ba}.
        Set<String> distinct = new HashSet<>(actions);
        assertEquals(2, distinct.size());
        assertTrue(distinct.contains("ab"));
        assertTrue(distinct.contains("ba"));
    }

    @Test
    public void balancerThenHierholzer_onSvm_producesValidCycle() throws Exception {
        FeaturedTransitionSystem svm = loadSvm();
        FeaturedTransitionSystem balanced = EulerianBalancer.balance(svm);

        // Every state must be balanced post-balancer.
        Iterator<State> it = balanced.states();
        while (it.hasNext()) {
            State s = it.next();
            int in = countIterator(balanced.getIncoming(s));
            int out = countIterator(balanced.getOutgoing(s));
            assertEquals("state " + s.getName() + " must be balanced", in, out);
        }

        List<Transition> cycle = HierholzerEulerCycle.compute(balanced);
        assertCoversEveryTransition(balanced, cycle);
        // SVM has 18 original transitions plus however many synthetic edges
        // the balancer adds; the cycle length matches the total transition
        // count of the balanced FTS.
        int totalTransitions = countIterator(balanced.transitions());
        assertEquals(totalTransitions, cycle.size());
        assertThat("Cycle should include at least every original SVM transition",
                cycle.size(), greaterThan(17));
    }

    @Test
    public void balancer_isSyntheticAction_recognisesPrefix() {
        assertTrue(EulerianBalancer.isSyntheticAction("__balance__0"));
        assertTrue(EulerianBalancer.isSyntheticAction("__balance__42"));
        assertEquals(false, EulerianBalancer.isSyntheticAction("pay"));
        assertEquals(false, EulerianBalancer.isSyntheticAction((String) null));
    }

    private static FeaturedTransitionSystem loadSvm() throws Exception {
        URL url = HierholzerEulerCycleTest.class.getClassLoader()
                .getResource("cases/SodaVendingMachine/SVM_ESGFx.mxe");
        assertThat(url, is(notNullValue()));
        return new MxeToFtsConverter().convert(new File(url.toURI()));
    }

    private static void assertCoversEveryTransition(FeaturedTransitionSystem fts,
                                                    List<Transition> cycle) {
        Set<Transition> covered = new HashSet<>(cycle);
        assertEquals("Every transition must be covered exactly once",
                cycle.size(), covered.size());
        int totalTransitions = countIterator(fts.transitions());
        assertEquals("Cycle must cover every transition",
                totalTransitions, covered.size());
    }

    private static int countIterator(Iterator<?> it) {
        int n = 0;
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }
}
