package be.vibes.testgeneration.mutation;

import be.vibes.fexpression.FExpression;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.ts.Action;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.FeaturedTransitionSystemFactory;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests both mutation operators on a toy graph and on the SVM SPL.
 */
public class MutationOperatorsTest {

    @Test
    public void transitionMissing_toyGraph_producesOneMutantPerTransition() {
        FeaturedTransitionSystem fts = twoStateLoop();
        TransitionMissing op = new TransitionMissing();
        op.generateMutants(fts);

        // Original has 2 transitions => 2 mutants, each missing one.
        assertEquals(2, op.getMutantCount());
        for (FeaturedTransitionSystem mutant : op.getMutants().values()) {
            assertEquals("Mutant must have exactly one less transition than the original",
                    countTransitions(fts) - 1, countTransitions(mutant));
        }
    }

    @Test
    public void transitionMissing_keysAreStableAndUnique() {
        FeaturedTransitionSystem fts = twoStateLoop();
        TransitionMissing op = new TransitionMissing();
        op.generateMutants(fts);
        Set<String> keys = op.getMutants().keySet();
        assertEquals("Keys must be unique", keys.size(), op.getMutantCount());
        for (String key : keys) {
            assertTrue("Key '" + key + "' should start with TM__", key.startsWith("TM__"));
        }
    }

    @Test
    public void transitionMissing_svm_mutantCountMatchesTransitionCount() throws Exception {
        FeaturedTransitionSystem svm = loadSvm();
        int original = countTransitions(svm);

        TransitionMissing op = new TransitionMissing();
        op.generateMutants(svm);
        assertEquals("SVM TransitionMissing must produce one mutant per transition",
                original, op.getMutantCount());

        for (FeaturedTransitionSystem mutant : op.getMutants().values()) {
            assertEquals(original - 1, countTransitions(mutant));
        }
    }

    @Test
    public void actionExchange_toyGraph_underAdjacencyScope_yieldsExpectedCount() {
        FeaturedTransitionSystem fts = twoStateLoop();
        ActionExchange op = new ActionExchange();
        op.generateMutants(fts);

        // Adjacency scope on the toy two-state loop:
        //   t1 = (a, ab, b): outgoing(a) ∪ outgoing(b) \ {ab} = {ba}
        //                    (a, ba, b) is not in the FTS  → 1 mutant
        //   t2 = (b, ba, a): outgoing(b) ∪ outgoing(a) \ {ba} = {ab}
        //                    (b, ab, a) is not in the FTS  → 1 mutant
        // Total: 2 mutants — the same count the old full-Cartesian scope
        // produced on this graph, because |A| = 2 makes the two scopes
        // coincide. The interesting reduction only appears on graphs with
        // more than one distinct outgoing action per state.
        assertEquals(2, op.getMutantCount());
        for (FeaturedTransitionSystem mutant : op.getMutants().values()) {
            // Action exchange preserves transition count (it is a label
            // swap, not a structural removal).
            assertEquals(countTransitions(fts), countTransitions(mutant));
            assertNotEquals(actionMultisetSignature(fts),
                    actionMultisetSignature(mutant));
        }
    }

    @Test
    public void actionExchange_svm_mutantCountMatchesAdjacencyScope() throws Exception {
        FeaturedTransitionSystem svm = loadSvm();
        int transitions = countTransitions(svm);
        int actions = countActions(svm);
        int expectedAdjacency = expectedAexAdjacencyCount(svm);
        int fullCartesian = transitions * (actions - 1);

        ActionExchange op = new ActionExchange(); // no solver: feature-compat filter off
        op.generateMutants(svm);

        assertEquals("SVM AEX mutant count must equal the adjacency-scope expectation",
                expectedAdjacency, op.getMutantCount());
        // Adjacency scope is a strict subset of full Cartesian unless every
        // state happens to expose every action, which is not the case for
        // any of our SPLs.
        assertTrue("Adjacency scope must not exceed |T| × (|A| − 1)",
                op.getMutantCount() <= fullCartesian);

        for (FeaturedTransitionSystem mutant : op.getMutants().values()) {
            assertEquals(transitions, countTransitions(mutant));
        }
    }

    /**
     * Mirrors the adjacency-scope counting logic of {@link ActionExchange}
     * with the feature-compatibility filter disabled (matching the
     * no-solver constructor used in the SVM test):
     *
     * <pre>
     *   for each t = (s, α, d):
     *     candidates = OutgoingActions(s) ∪ OutgoingActions(d) \ {α}
     *     skip β with (s, β, d) already in the FTS
     *     count remaining β
     * </pre>
     */
    private static int expectedAexAdjacencyCount(FeaturedTransitionSystem fts) {
        Map<State, Set<Action>> outgoing = new HashMap<>();
        Set<String> existing = new HashSet<>();
        List<Transition> all = new ArrayList<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            Transition t = it.next();
            all.add(t);
            outgoing.computeIfAbsent(t.getSource(), k -> new HashSet<>()).add(t.getAction());
            existing.add(tripleKey(t.getSource().getName(), t.getAction().getName(),
                    t.getTarget().getName()));
        }
        int count = 0;
        for (Transition t : all) {
            Set<Action> cand = new HashSet<>();
            cand.addAll(outgoing.getOrDefault(t.getSource(), Collections.emptySet()));
            cand.addAll(outgoing.getOrDefault(t.getTarget(), Collections.emptySet()));
            cand.remove(t.getAction());
            for (Action beta : cand) {
                if (!existing.contains(tripleKey(t.getSource().getName(),
                        beta.getName(), t.getTarget().getName()))) {
                    count++;
                }
            }
        }
        return count;
    }

    private static String tripleKey(String src, String act, String tgt) {
        return src + "|" + act + "|" + tgt;
    }

    @Test
    public void mutants_areAllDistinct() throws Exception {
        FeaturedTransitionSystem svm = loadSvm();

        TransitionMissing tm = new TransitionMissing();
        tm.generateMutants(svm);
        Set<String> tmSignatures = new HashSet<>();
        for (FeaturedTransitionSystem mutant : tm.getMutants().values()) {
            tmSignatures.add(transitionSignature(mutant));
        }
        assertEquals("Every TransitionMissing mutant must be structurally distinct",
                tm.getMutantCount(), tmSignatures.size());

        ActionExchange ae = new ActionExchange();
        ae.generateMutants(svm);
        Set<String> aeSignatures = new HashSet<>();
        for (FeaturedTransitionSystem mutant : ae.getMutants().values()) {
            aeSignatures.add(transitionSignature(mutant));
        }
        assertEquals("Every ActionExchange mutant must be structurally distinct",
                ae.getMutantCount(), aeSignatures.size());

        // Mutants of the two operators should also be disjoint (a transition
        // removal cannot equal a label swap that keeps transition count).
        Set<String> intersection = new HashSet<>(tmSignatures);
        intersection.retainAll(aeSignatures);
        assertTrue("TransitionMissing and ActionExchange mutants must not overlap",
                intersection.isEmpty());
    }

    // ---- TransitionDestinationExchange (TDE) ----

    @Test
    public void tde_toyGraph_underAdjacencyScope_yieldsExpectedCount() {
        FeaturedTransitionSystem fts = twoStateLoop();
        TransitionDestinationExchange op = new TransitionDestinationExchange();
        op.generateMutants(fts);

        // Adjacency scope on the toy two-state loop:
        //   t1 = (a, ab, b): outgoingTargets(a) ∪ outgoingTargets(b) \ {b}
        //                    = {b, a} \ {b} = {a}
        //                    (a, ab, a) is not in the FTS → 1 mutant
        //   t2 = (b, ba, a): outgoingTargets(b) ∪ outgoingTargets(a) \ {a}
        //                    = {a, b} \ {a} = {b}
        //                    (b, ba, b) is not in the FTS → 1 mutant
        assertEquals(2, op.getMutantCount());
        for (FeaturedTransitionSystem mutant : op.getMutants().values()) {
            assertEquals("TDE preserves transition count (target swap, not removal)",
                    countTransitions(fts), countTransitions(mutant));
        }
    }

    @Test
    public void tde_keysAreStableAndUnique() {
        FeaturedTransitionSystem fts = twoStateLoop();
        TransitionDestinationExchange op = new TransitionDestinationExchange();
        op.generateMutants(fts);
        Set<String> keys = op.getMutants().keySet();
        assertEquals("Keys must be unique", keys.size(), op.getMutantCount());
        for (String key : keys) {
            assertTrue("Key '" + key + "' should start with TDE__", key.startsWith("TDE__"));
        }
    }

    @Test
    public void tde_svm_mutantCountMatchesAdjacencyScope() throws Exception {
        FeaturedTransitionSystem svm = loadSvm();
        int transitions = countTransitions(svm);
        int expectedAdjacency = expectedTdeAdjacencyCount(svm);

        TransitionDestinationExchange op = new TransitionDestinationExchange(); // no solver
        op.generateMutants(svm);

        assertEquals("SVM TDE mutant count must equal the adjacency-scope expectation",
                expectedAdjacency, op.getMutantCount());

        for (FeaturedTransitionSystem mutant : op.getMutants().values()) {
            assertEquals("TDE preserves transition count",
                    transitions, countTransitions(mutant));
        }
    }

    @Test
    public void tde_mutants_disjoint_from_tm_and_aex() throws Exception {
        FeaturedTransitionSystem svm = loadSvm();

        TransitionMissing tm = new TransitionMissing();
        tm.generateMutants(svm);
        Set<String> tmSignatures = new HashSet<>();
        for (FeaturedTransitionSystem m : tm.getMutants().values()) {
            tmSignatures.add(transitionSignature(m));
        }

        ActionExchange ae = new ActionExchange();
        ae.generateMutants(svm);
        Set<String> aeSignatures = new HashSet<>();
        for (FeaturedTransitionSystem m : ae.getMutants().values()) {
            aeSignatures.add(transitionSignature(m));
        }

        TransitionDestinationExchange tde = new TransitionDestinationExchange();
        tde.generateMutants(svm);
        Set<String> tdeSignatures = new HashSet<>();
        for (FeaturedTransitionSystem m : tde.getMutants().values()) {
            tdeSignatures.add(transitionSignature(m));
        }
        assertEquals("Every TDE mutant must be structurally distinct",
                tde.getMutantCount(), tdeSignatures.size());

        Set<String> tmTde = new HashSet<>(tmSignatures);
        tmTde.retainAll(tdeSignatures);
        assertTrue("TM and TDE mutants must not overlap (TM removes one transition; "
                        + "TDE preserves count via target redirect)",
                tmTde.isEmpty());

        Set<String> aeTde = new HashSet<>(aeSignatures);
        aeTde.retainAll(tdeSignatures);
        assertTrue("AEX and TDE mutants must not overlap (AEX swaps action; TDE swaps "
                        + "target — both preserve count but along orthogonal axes)",
                aeTde.isEmpty());
    }

    /**
     * Mirrors the adjacency-scope counting logic of
     * {@link TransitionDestinationExchange} with the feature-compatibility
     * filter disabled (matching the no-solver constructor):
     *
     * <pre>
     *   for each t = (s, α, d):
     *     candidates = OutgoingTargets(s) ∪ OutgoingTargets(d) \ {d}
     *     skip d' with (s, α, d') already in the FTS
     *     count remaining d'
     * </pre>
     */
    private static int expectedTdeAdjacencyCount(FeaturedTransitionSystem fts) {
        Map<State, Set<State>> outgoingTargets = new HashMap<>();
        Set<String> existing = new HashSet<>();
        List<Transition> all = new ArrayList<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            Transition t = it.next();
            all.add(t);
            outgoingTargets.computeIfAbsent(t.getSource(), k -> new HashSet<>())
                    .add(t.getTarget());
            existing.add(tripleKey(t.getSource().getName(), t.getAction().getName(),
                    t.getTarget().getName()));
        }
        int count = 0;
        for (Transition t : all) {
            Set<State> cand = new HashSet<>();
            cand.addAll(outgoingTargets.getOrDefault(t.getSource(), Collections.emptySet()));
            cand.addAll(outgoingTargets.getOrDefault(t.getTarget(), Collections.emptySet()));
            cand.remove(t.getTarget());
            for (State dPrime : cand) {
                if (!existing.contains(tripleKey(t.getSource().getName(),
                        t.getAction().getName(), dPrime.getName()))) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    public void ftsCloning_withReplacedTargetRedirectsOnlyThatOne() {
        FeaturedTransitionSystem original = twoStateLoop();
        Transition target = original.transitions().next();
        State newTarget = original.getInitialState(); // any valid state ≠ target.target

        FeaturedTransitionSystem redirected =
                FtsCloning.withReplacedTarget(original, target, newTarget);
        assertEquals("Redirect preserves transition count",
                countTransitions(original), countTransitions(redirected));

        // Exactly one transition has been redirected; the rest are verbatim.
        int matchingRedirected = 0;
        Iterator<Transition> it = redirected.transitions();
        while (it.hasNext()) {
            Transition t = it.next();
            if (t.getSource().getName().equals(target.getSource().getName())
                    && t.getAction().getName().equals(target.getAction().getName())
                    && t.getTarget().getName().equals(newTarget.getName())) {
                matchingRedirected++;
            }
        }
        assertEquals("Exactly one redirected transition must appear in the rebuilt FTS",
                1, matchingRedirected);
    }

    @Test
    public void ftsCloning_copyIsStructurallyEqual() {
        FeaturedTransitionSystem original = twoStateLoop();
        FeaturedTransitionSystem clone = FtsCloning.copy(original);
        assertNotEquals("Clone must be a distinct object", System.identityHashCode(original),
                System.identityHashCode(clone));
        assertEquals(transitionSignature(original), transitionSignature(clone));
    }

    @Test
    public void ftsCloning_withoutTransitionDropsOnlyThatOne() {
        FeaturedTransitionSystem original = twoStateLoop();
        Transition target = original.transitions().next();

        FeaturedTransitionSystem reduced = FtsCloning.withoutTransition(original, target);
        assertEquals(countTransitions(original) - 1, countTransitions(reduced));

        Iterator<Transition> rIt = reduced.transitions();
        while (rIt.hasNext()) {
            Transition surviving = rIt.next();
            assertFalse("The omitted transition must not appear in the rebuilt FTS",
                    surviving.getSource().getName().equals(target.getSource().getName())
                            && surviving.getAction().getName().equals(target.getAction().getName())
                            && surviving.getTarget().getName().equals(target.getTarget().getName()));
        }
    }

    // ---- helpers ----

    private static FeaturedTransitionSystem twoStateLoop() {
        FeaturedTransitionSystemFactory factory = new FeaturedTransitionSystemFactory("a");
        factory.addState("b");
        factory.addAction("ab");
        factory.addAction("ba");
        factory.addTransition("a", "ab", FExpression.trueValue(), "b");
        factory.addTransition("b", "ba", FExpression.trueValue(), "a");
        return factory.build();
    }

    private static FeaturedTransitionSystem loadSvm() throws Exception {
        URL url = MutationOperatorsTest.class.getClassLoader()
                .getResource("cases/SodaVendingMachine/SVM_ESGFx.mxe");
        assertThat(url, is(notNullValue()));
        return new MxeToFtsConverter().convert(new File(url.toURI()));
    }

    private static int countTransitions(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    private static int countActions(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Action> it = fts.actions();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    /** Stable signature: sorted list of transition triples. */
    private static String transitionSignature(FeaturedTransitionSystem fts) {
        java.util.List<String> triples = new java.util.ArrayList<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            Transition t = it.next();
            triples.add(t.getSource().getName() + "/" + t.getAction().getName()
                    + "/" + t.getTarget().getName());
        }
        java.util.Collections.sort(triples);
        return String.join("|", triples);
    }

    /** Signature based only on action multiset (independent of source/target). */
    private static String actionMultisetSignature(FeaturedTransitionSystem fts) {
        java.util.List<String> actions = new java.util.ArrayList<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            actions.add(it.next().getAction().getName());
        }
        java.util.Collections.sort(actions);
        return String.join(",", actions);
    }

    /**
     * Validates the test helper for sanity-checking, not a property of the
     * production code: ensures that two distinct mutants would actually
     * produce different transition signatures (defensive against helper bugs).
     */
    @Test
    public void transitionSignature_differsBetweenOriginalAndMissingMutant() {
        FeaturedTransitionSystem original = twoStateLoop();
        Transition omit = original.transitions().next();
        FeaturedTransitionSystem mutant = FtsCloning.withoutTransition(original, omit);
        assertNotEquals(transitionSignature(original), transitionSignature(mutant));
    }
}
