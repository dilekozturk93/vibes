package be.vibes.testgeneration.product;

import be.vibes.fexpression.FExpression;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.FeaturedTransitionSystemFactory;
import be.vibes.ts.State;
import be.vibes.ts.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Projects a {@link FeaturedTransitionSystem} onto a single product
 * configuration, returning another {@link FeaturedTransitionSystem} in
 * which the original {@link FExpression}s are retained.
 *
 * <p>This is the analog of VIBeS' {@code be.vibes.ts.SimpleProjection},
 * with one key semantic difference: VIBeS' projection erases feature
 * metadata (output is a plain {@link be.vibes.ts.TransitionSystem}); this
 * one preserves it so that the same FTS-typed traversal algorithms can be
 * used at both the SPL and product levels of the pipeline.
 *
 * <p>Rationale (see the approved plan, "Preserving feature expressions in
 * projection"):
 * <ul>
 *   <li>Thesis-level coherence with the user's ESG-Fx work, which adopts
 *       the same convention.</li>
 *   <li>A uniform graph representation (annotated FTS) at both abstraction
 *       levels, so a single algorithm pipeline runs on both.</li>
 *   <li>Traceability — every retained transition still carries the
 *       feature expression that justified its inclusion.</li>
 * </ul>
 *
 * <p>Inclusion rule per transition: include the transition iff
 * {@code fexpr.assign(product).applySimplification().isTrue()}. Transitions
 * with feature expressions that evaluate to false or remain partially
 * unresolved under the given product configuration are dropped. States
 * and actions that do not appear in any kept transition are also dropped;
 * the initial state is always preserved.
 */
public final class FExpressionPreservingProjection {

    private static final Logger LOG = LoggerFactory.getLogger(FExpressionPreservingProjection.class);

    private FExpressionPreservingProjection() {
        // Utility class.
    }

    /**
     * Projects the given FTS onto the given product configuration.
     *
     * @param fts the SPL-level FTS
     * @param product the product configuration to project onto
     * @return a new FTS containing exactly those transitions whose feature
     *         expressions evaluate to true under {@code product}; original
     *         {@link FExpression}s are preserved on kept transitions
     */
    public static FeaturedTransitionSystem project(FeaturedTransitionSystem fts,
                                                   Configuration product) {
        checkNotNull(fts, "FTS may not be null");
        checkNotNull(product, "Configuration may not be null");

        FeaturedTransitionSystemFactory factory =
                new FeaturedTransitionSystemFactory(fts.getInitialState().getName());

        int kept = 0;
        int dropped = 0;
        Iterator<State> stateIt = fts.states();
        while (stateIt.hasNext()) {
            State s = stateIt.next();
            Iterator<Transition> outIt = fts.getOutgoing(s);
            while (outIt.hasNext()) {
                Transition t = outIt.next();
                FExpression original = fts.getFExpression(t);
                FExpression assigned = original.assign(product).applySimplification();
                if (assigned.isTrue()) {
                    String source = t.getSource().getName();
                    String action = t.getAction().getName();
                    String target = t.getTarget().getName();
                    factory.addState(source);
                    factory.addState(target);
                    factory.addAction(action);
                    // Retain the original (un-assigned) feature expression
                    // for traceability. The assigned expression is true by
                    // construction at this product, so the original carries
                    // the strictly-more-information form.
                    factory.addTransition(source, action, original, target);
                    kept++;
                } else {
                    dropped++;
                }
            }
        }
        LOG.info("Projected FTS: kept {} transitions, dropped {}", kept, dropped);
        return factory.build();
    }
}
