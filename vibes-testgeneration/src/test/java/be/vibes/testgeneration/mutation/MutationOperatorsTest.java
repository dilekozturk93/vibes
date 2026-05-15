package be.vibes.testgeneration.mutation;

import be.vibes.fexpression.FExpression;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.ts.Action;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.FeaturedTransitionSystemFactory;
import be.vibes.ts.Transition;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
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
    public void actionExchange_toyGraph_producesTimesTransitionsAndActionsMinusOne() {
        FeaturedTransitionSystem fts = twoStateLoop();
        ActionExchange op = new ActionExchange();
        op.generateMutants(fts);

        // 2 transitions × (2 actions − 1) = 2 mutants total: each transition
        // has its label swapped with the other action.
        assertEquals(2, op.getMutantCount());
        for (FeaturedTransitionSystem mutant : op.getMutants().values()) {
            // After action exchange the transition count is unchanged.
            assertEquals(countTransitions(fts), countTransitions(mutant));
            // Each mutant's action multiset must differ from the original's
            // multiset, because we swapped the label of at least one
            // transition while keeping every other label.
            assertNotEquals(actionMultisetSignature(fts),
                    actionMultisetSignature(mutant));
        }
    }

    @Test
    public void actionExchange_svm_mutantCountMatchesFormula() throws Exception {
        FeaturedTransitionSystem svm = loadSvm();
        int transitions = countTransitions(svm);
        int actions = countActions(svm);

        ActionExchange op = new ActionExchange();
        op.generateMutants(svm);
        assertEquals("SVM ActionExchange mutant count must equal |T| * (|A| - 1)",
                transitions * (actions - 1), op.getMutantCount());

        for (FeaturedTransitionSystem mutant : op.getMutants().values()) {
            // Transition count unchanged; the mutation is a label swap, not
            // a structural removal.
            assertEquals(transitions, countTransitions(mutant));
        }
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
