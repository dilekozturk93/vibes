package be.vibes.testgeneration.conversion;

import be.vibes.dsl.io.Xml;
import be.vibes.fexpression.FExpression;
import be.vibes.ts.Action;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Smoke + structural tests for {@link MxeToFtsConverter} on the Soda Vending
 * Machine ESG-Fx model bundled under
 * {@code src/main/resources/cases/SodaVendingMachine/}.
 *
 * <p>The ESG has 15 vertices (13 events + the "[" / "]" markers) and 21
 * edges. Three event vertices (return/c, take/f, close/!f) have only "]"
 * as a successor and are therefore merged with INIT during conversion.
 * The resulting FTS has 11 states (INIT + 10 event vertices) and
 * 18 transitions (21 ESG edges minus the 3 edges feeding ").
 */
public class MxeToFtsConverterTest {

    private static final String SVM_MXE_RESOURCE = "cases/SodaVendingMachine/SVM_ESGFx.mxe";
    private static final String EMAIL_MXE_RESOURCE = "cases/eMail/eM_ESGFx.mxe";
    private static final String ELEVATOR_MXE_RESOURCE = "cases/Elevator/El_ESGFx.mxe";

    @Test
    public void parseEventLabel_negatedFeature_producesNegation() {
        MxeToFtsConverter.EventLabel label = MxeToFtsConverter.parseEventLabel("pay/!f");
        assertEquals("pay", label.action);
        assertThat(label.fexpr.applySimplification(),
                is(equalTo(FExpression.featureExpr("f").not().applySimplification())));
    }

    @Test
    public void parseEventLabel_plainFeature_producesLeaf() {
        MxeToFtsConverter.EventLabel label = MxeToFtsConverter.parseEventLabel("soda/s");
        assertEquals("soda", label.action);
        assertThat(label.fexpr.applySimplification(),
                is(equalTo(FExpression.featureExpr("s").applySimplification())));
    }

    @Test
    public void parseEventLabel_missingFeature_producesTrue() {
        MxeToFtsConverter.EventLabel label = MxeToFtsConverter.parseEventLabel("reset");
        assertEquals("reset", label.action);
        assertTrue(label.fexpr.applySimplification().isTrue());
    }

    @Test
    public void convert_svm_producesExpectedCardinalities() throws Exception {
        FeaturedTransitionSystem fts = convertSvm();
        // After bisimulation reduction the SVM FTS matches Devroey's
        // hand-written canonical: 9 states, 13 transitions. (free/f and
        // change/!f collapse to one state; serveSoda/s and serveTea/t
        // collapse to another.)
        assertEquals("FTS state count", 9, countStates(fts));
        assertEquals("FTS transition count", 13, countTransitions(fts));
    }

    @Test
    public void convert_svm_actionSetMatchesEsgEvents() throws Exception {
        FeaturedTransitionSystem fts = convertSvm();
        Set<String> actions = new HashSet<>();
        Iterator<Action> it = fts.actions();
        while (it.hasNext()) {
            actions.add(it.next().getName());
        }
        // Every event in the SVM ESG (less the bracket markers) must appear as
        // an action in the converted FTS.
        assertThat(actions, containsInAnyOrder(
                "pay", "change", "free",
                "soda", "tea", "serveSoda", "serveTea",
                "cancel", "return",
                "open", "take", "close"));
    }

    @Test
    public void convert_svm_initialStateExists() throws Exception {
        FeaturedTransitionSystem fts = convertSvm();
        State init = fts.getInitialState();
        assertThat(init, is(notNullValue()));
        assertEquals("state1", init.getName());
        // INIT must have outgoing transitions (the start of any test run).
        assertThat("INIT outgoing transition count",
                countOutgoing(fts, init), greaterThan(0));
    }

    @Test
    public void convert_svm_writeAndReloadIsIdempotent() throws Exception {
        FeaturedTransitionSystem fts = convertSvm();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Xml.print(fts, baos);
        FeaturedTransitionSystem reloaded =
                Xml.loadFeaturedTransitionSystem(new ByteArrayInputStream(baos.toByteArray()));
        assertEquals(countStates(fts), countStates(reloaded));
        assertEquals(countTransitions(fts), countTransitions(reloaded));
    }

    @Test
    public void convert_emailSpl_producesExpectedCardinalities() throws Exception {
        FeaturedTransitionSystem fts = convertResource(EMAIL_MXE_RESOURCE);
        // After bisimulation reduction: terminal vertices collapse into the
        // initial state (state1), and equivalent event vertices share a
        // single FTS state. SVM has 9, eMail 11, Elevator 14 states
        // post-minimization.
        // After the mixed-terminal __end__ fix: 11 states unchanged, but
        // 2 synthetic __end__ transitions are added (one per mixed-terminal
        // class that has at least one ]-edge in the original ESG), bringing
        // the transition count from 22 to 24.
        assertEquals("eMail FTS state count", 11, countStates(fts));
        assertEquals("eMail FTS transition count", 24, countTransitions(fts));
        assertEquals("state1", fts.getInitialState().getName());
        assertThat("eMail INIT outgoing transition count",
                countOutgoing(fts, fts.getInitialState()), greaterThan(0));
    }

    @Test
    public void convert_elevatorSpl_producesExpectedCardinalities() throws Exception {
        FeaturedTransitionSystem fts = convertResource(ELEVATOR_MXE_RESOURCE);
        // Elevator's raw ESG has 19 event vertices but heavy structural
        // duplication (many press/release pairs with identical successor
        // sets); bisimulation reduction collapses these aggressively.
        // After the mixed-terminal __end__ fix: 14 states unchanged, but
        // 7 synthetic __end__ transitions are added (one per mixed-terminal
        // class with a ]-edge — fewer than the 10 raw ]-edges in the ESG
        // because the corresponding ESG vertices collapse into 7 classes
        // under bisimulation). Transition count rises from 42 to 49.
        assertEquals("Elevator FTS state count", 14, countStates(fts));
        assertEquals("Elevator FTS transition count", 49, countTransitions(fts));
        assertEquals("state1", fts.getInitialState().getName());
        assertThat("Elevator INIT outgoing transition count",
                countOutgoing(fts, fts.getInitialState()), greaterThan(0));
    }

    private FeaturedTransitionSystem convertSvm() throws Exception {
        return convertResource(SVM_MXE_RESOURCE);
    }

    private FeaturedTransitionSystem convertResource(String resourcePath) throws Exception {
        URL url = getClass().getClassLoader().getResource(resourcePath);
        assertThat("Resource must be on the classpath: " + resourcePath, url, is(notNullValue()));
        File mxeFile = new File(url.toURI());
        return new MxeToFtsConverter().convert(mxeFile);
    }

    private static int countStates(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<State> it = fts.states();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
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

    private static int countOutgoing(FeaturedTransitionSystem fts, State state) {
        int n = 0;
        Iterator<Transition> it = fts.getOutgoing(state);
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }
}
