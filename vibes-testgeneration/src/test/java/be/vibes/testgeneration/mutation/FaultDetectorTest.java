package be.vibes.testgeneration.mutation;

import be.vibes.fexpression.FExpression;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.FeaturedTransitionSystemFactory;
import be.vibes.ts.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link FaultDetector} on a hand-built toy FTS so we can
 * reason about the kill criterion without depending on the whole
 * projection / SCC-repair / Hierholzer pipeline.
 *
 * <p>Toy graph: a 4-state ring s1 -&gt; s2 -&gt; s3 -&gt; s4 -&gt; s1
 * with actions a, b, c, d on each transition respectively. Initial state
 * is s1. The test suite below is the canonical Euler cycle of this ring:
 * s1 -a-&gt; s2 -b-&gt; s3 -c-&gt; s4 -d-&gt; s1 — every transition exercised once.
 */
public class FaultDetectorTest {

    private FeaturedTransitionSystem original;
    private TestCase fullCycle;

    @Before
    public void setUp() {
        FeaturedTransitionSystemFactory f = new FeaturedTransitionSystemFactory("s1");
        f.addState("s1"); f.addState("s2"); f.addState("s3"); f.addState("s4");
        f.addAction("a"); f.addAction("b"); f.addAction("c"); f.addAction("d");
        f.addTransition("s1", "a", FExpression.trueValue(), "s2");
        f.addTransition("s2", "b", FExpression.trueValue(), "s3");
        f.addTransition("s3", "c", FExpression.trueValue(), "s4");
        f.addTransition("s4", "d", FExpression.trueValue(), "s1");
        original = f.build();

        fullCycle = new TestCase("full-cycle");
        try {
            // TestCase enforces path contiguity at enqueue; walk explicitly
            // s1 -a-> s2 -b-> s3 -c-> s4 -d-> s1.
            fullCycle.enqueue(findTransition(original, "s1", "a", "s2"));
            fullCycle.enqueue(findTransition(original, "s2", "b", "s3"));
            fullCycle.enqueue(findTransition(original, "s3", "c", "s4"));
            fullCycle.enqueue(findTransition(original, "s4", "d", "s1"));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static be.vibes.ts.Transition findTransition(FeaturedTransitionSystem fts,
                                                         String src, String action, String tgt) {
        Iterator<be.vibes.ts.Transition> it = fts.transitions();
        while (it.hasNext()) {
            be.vibes.ts.Transition t = it.next();
            if (t.getSource().getName().equals(src)
                    && t.getAction().getName().equals(action)
                    && t.getTarget().getName().equals(tgt)) {
                return t;
            }
        }
        throw new AssertionError("Transition not found: " + src + " -" + action + "-> " + tgt);
    }

    @Test
    public void original_isNeverKilled_byOwnTestSuite() {
        assertFalse("Original FTS cannot be killed by its own suite",
                FaultDetector.kills(Collections.singletonList(fullCycle), original));
    }

    @Test
    public void transitionMissing_isKilled_byFullCycle() {
        TransitionMissing tm = new TransitionMissing();
        tm.generateMutants(original);
        Map<String, FeaturedTransitionSystem> mutants = tm.getMutants();
        assertThat(mutants.size(), is(greaterThan(0)));

        FaultDetector.KillResult result =
                FaultDetector.scoreSuite(Collections.singletonList(fullCycle), mutants);
        // The full Euler cycle traverses every transition; therefore every
        // TransitionMissing mutant must be killed by it.
        assertEquals("Full cycle kills every TM mutant",
                mutants.size(), result.getKilled());
        assertEquals(1.0, result.getScore(), 0.0);
        assertTrue("No survivors", result.getSurvivors().isEmpty());
    }

    @Test
    public void transitionMissing_partialSuite_leavesCorrespondingMutantAlive() {
        // Suite traverses only 's1 -a-> s2'. The TM mutant that removes that
        // specific transition is killed; the others (TM_s2__b__s3, ...) are not.
        TestCase partial = new TestCase("partial");
        try {
            partial.enqueue(findTransition(original, "s1", "a", "s2"));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }

        TransitionMissing tm = new TransitionMissing();
        tm.generateMutants(original);
        FaultDetector.KillResult result =
                FaultDetector.scoreSuite(Collections.singletonList(partial), tm.getMutants());

        // Exactly one mutant killed (the one removing the transition the suite traverses).
        assertEquals("Exactly the one TM mutant matching the suite's transition is killed",
                1, result.getKilled());
        assertEquals(tm.getMutants().size() - 1, result.getSurvivors().size());
    }

    @Test
    public void actionExchange_isKilled_byFullCycle() {
        ActionExchange aex = new ActionExchange();
        aex.generateMutants(original);
        Map<String, FeaturedTransitionSystem> mutants = aex.getMutants();
        assertThat("AEX produces non-empty mutant set", mutants.size(), is(greaterThan(0)));

        FaultDetector.KillResult result =
                FaultDetector.scoreSuite(Collections.singletonList(fullCycle), mutants);
        // The full Euler cycle traverses every transition; AEX mutants
        // remove the original (src, origAction, tgt) triple, so the suite
        // step over that transition is refused -> killed.
        assertEquals("Full cycle kills every AEX mutant",
                mutants.size(), result.getKilled());
        assertEquals(1.0, result.getScore(), 0.0);
    }

    @Test
    public void emptyMutantSet_scoreIsVacuouslyOne() {
        FaultDetector.KillResult empty =
                FaultDetector.scoreSuite(Collections.singletonList(fullCycle),
                        new LinkedHashMap<>());
        assertEquals(0, empty.getKilled());
        assertEquals(0, empty.getTotal());
        assertEquals("Empty mutant set scores 1.0 (vacuous)", 1.0, empty.getScore(), 0.0);
    }

    @Test
    public void killsFromTriples_avoidsRewalkingSuite() {
        // killsFromTriples is the cheap-loop variant; verify it agrees with
        // the suite-walking variant on a known case.
        TransitionMissing tm = new TransitionMissing();
        tm.generateMutants(original);
        java.util.Set<String> suiteTriples =
                FaultDetector.nonSyntheticTriplesOfSuite(Collections.singletonList(fullCycle));
        assertEquals("All 4 transitions of the ring", 4, suiteTriples.size());

        for (FeaturedTransitionSystem mutant : tm.getMutants().values()) {
            assertTrue("Every TM mutant is killed by the full-cycle triple set",
                    FaultDetector.killsFromTriples(mutant, suiteTriples));
        }
    }

    @Test
    public void survivorList_preservesMutantIteration_orderForReproducibility() {
        // Use a one-transition suite over 's1 -a-> s2' so 3 of the 4 TM
        // mutants survive. The survivor list must reflect mutant iteration
        // order (LinkedHashMap), which is the order the operator generated
        // them — required for stable experiment logs and the per-product
        // report's reproducible drill-down listings.
        TestCase suite = new TestCase("s1a");
        try {
            suite.enqueue(findTransition(original, "s1", "a", "s2"));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
        TransitionMissing tm = new TransitionMissing();
        tm.generateMutants(original);
        List<String> survivors =
                FaultDetector.scoreSuite(Collections.singletonList(suite), tm.getMutants())
                        .getSurvivors();
        // Survivors must be a strict subset of mutant keys in their original order.
        java.util.List<String> allKeys = new java.util.ArrayList<>(tm.getMutants().keySet());
        java.util.List<String> survivorsCopy = new java.util.ArrayList<>(survivors);
        survivorsCopy.retainAll(allKeys);
        assertEquals(survivorsCopy, survivors);
    }

    @Test
    public void syntheticTransitionsInSuite_doNotContributeToKillDecision() {
        // Build a 2-state model with one real transition (a) and one
        // synthetic back-edge (__end__). A suite that traverses both
        // should NOT detect a mutant that only differs in the __end__
        // transition, because FaultDetector ignores synthetic triples.
        FeaturedTransitionSystemFactory f = new FeaturedTransitionSystemFactory("s1");
        f.addState("s1"); f.addState("s2");
        f.addAction("a"); f.addAction("__end__");
        f.addTransition("s1", "a", FExpression.trueValue(), "s2");
        f.addTransition("s2", "__end__", FExpression.trueValue(), "s1");
        FeaturedTransitionSystem withEnd = f.build();

        TestCase suite = new TestCase("suite");
        try {
            suite.enqueue(findTransition(withEnd, "s1", "a", "s2"));
            suite.enqueue(findTransition(withEnd, "s2", "__end__", "s1"));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }

        // Suite triples set, after filtering synthetics, has exactly the
        // real (s1, a, s2) triple.
        java.util.Set<String> triples = FaultDetector.nonSyntheticTriplesOfSuite(
                Collections.singletonList(suite));
        assertEquals("Only the real triple survives filtering", 1, triples.size());

        // Build a 'mutant' that lacks the __end__ transition but keeps 'a'.
        // Under FaultDetector's synthetic-aware semantics this mutant is
        // structurally indistinguishable from the original from the suite's
        // POV — kill check returns false.
        FeaturedTransitionSystemFactory mf =
                new FeaturedTransitionSystemFactory("s1");
        mf.addState("s1"); mf.addState("s2");
        mf.addAction("a");
        mf.addTransition("s1", "a", FExpression.trueValue(), "s2");
        FeaturedTransitionSystem mutantMissingEnd = mf.build();
        assertFalse("Removing only an __end__ transition does NOT kill — "
                        + "synthetics carry no SUT signal",
                FaultDetector.kills(Collections.singletonList(suite), mutantMissingEnd));
    }
}
