package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.FExpression;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.Transition;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Custom Dot renderer for the per-step walkthrough images. Two style
 * decorations on top of the default VIBeS Dot output:
 *
 * <ul>
 *   <li>Transitions in the "bold" set get {@code style=bold} +
 *       {@code color=darkblue} — used in the strongly-connected step to
 *       highlight any edges newly introduced relative to the projected
 *       step (in the current pipeline that set is empty since the SCC
 *       repair only removes; the renderer supports the case for future
 *       extensions).</li>
 *   <li>Transitions whose action is synthetic (per
 *       {@link EulerianBalancer#isSyntheticAction}) get
 *       {@code style=dashed} + {@code color=red} — used in the balanced
 *       step to call out the edges that the Chinese Postman balancer
 *       doubled (or, in the fallback case, the direct synthetic edges).</li>
 * </ul>
 *
 * <p>For doubled transitions whose action is {@code <orig>__dup__N}, the
 * displayed label is the original action name with a trailing
 * {@code (×2)} marker so it reads as "this real action is traversed an
 * extra time".
 */
public final class StyledDotRenderer {

    private StyledDotRenderer() {
    }

    /**
     * Renders the given FTS as Dot, decorating bold-set transitions with a
     * thicker dark-blue style and synthetic transitions with a dashed red
     * style. Bold and dashed sets need not be disjoint; if a transition is
     * in both, bold wins.
     *
     * <p>The bold/dashed sets are matched against
     * {@code (source, action, target)} triples — i.e. structural identity —
     * because {@link Transition} reference equality across two FTS
     * instances is not guaranteed.
     */
    public static String render(FeaturedTransitionSystem fts,
                                Set<Transition> boldSet,
                                boolean autoDashSynthetics) {
        Set<String> boldKeys = new HashSet<>();
        if (boldSet != null) {
            for (Transition t : boldSet) {
                boldKeys.add(transitionKey(t));
            }
        }

        // Assign sequential Dot node IDs in iteration order; initial state
        // gets id 0 so we can decorate it.
        Map<String, String> stateNameToDotId = new LinkedHashMap<>();
        State initial = fts.getInitialState();
        stateNameToDotId.put(initial.getName(), "state0");
        int next = 1;
        Iterator<State> sIt = fts.states();
        while (sIt.hasNext()) {
            State s = sIt.next();
            if (s.equals(initial)) continue;
            stateNameToDotId.put(s.getName(), "state" + next++);
        }

        StringBuilder out = new StringBuilder();
        out.append("digraph G {\nrankdir=LR;\n");
        for (Map.Entry<String, String> entry : stateNameToDotId.entrySet()) {
            String stateName = entry.getKey();
            String dotId = entry.getValue();
            if (stateName.equals(initial.getName())) {
                out.append(dotId).append("[ label = \"").append(stateName)
                        .append("\", style=filled, color=green ];\n");
            } else {
                out.append(dotId).append(" [ label = \"").append(stateName).append("\" ];\n");
            }
        }
        Iterator<Transition> tIt = fts.transitions();
        while (tIt.hasNext()) {
            Transition t = tIt.next();
            String src = stateNameToDotId.get(t.getSource().getName());
            String tgt = stateNameToDotId.get(t.getTarget().getName());
            String labelText = displayLabel(t, fts);
            String key = transitionKey(t);

            String style = "";
            if (boldKeys.contains(key)) {
                style = ", style=bold, color=darkblue, penwidth=2";
            } else if (autoDashSynthetics && EulerianBalancer.isSyntheticAction(t.getAction())) {
                style = ", style=dashed, color=red";
            }

            out.append(src).append(" -> ").append(tgt)
                    .append(" [ label=\" ").append(escape(labelText)).append(" \"")
                    .append(style).append(" ];\n");
        }
        out.append("}\n");
        return out.toString();
    }

    private static String displayLabel(Transition t, FeaturedTransitionSystem fts) {
        String actionName = t.getAction().getName();
        String displayed;
        if (actionName.contains(EulerianBalancer.DUPLICATE_ACTION_INFIX)) {
            // Doubled real action: show base action with a (×2) marker so
            // readers see "we traverse this real action once more".
            String base = EulerianBalancer.stripDuplicateSuffix(actionName);
            displayed = base + " (×2)";
        } else {
            displayed = actionName;
        }
        FExpression fexpr = fts.getFExpression(t);
        String fexprStr;
        if (fexpr == null) {
            fexprStr = "true";
        } else {
            fexprStr = fexpr.applySimplification().toString();
        }
        return displayed + "/" + fexprStr;
    }

    private static String transitionKey(Transition t) {
        return t.getSource().getName() + "|" + t.getAction().getName()
                + "|" + t.getTarget().getName();
    }

    private static String escape(String s) {
        return s.replace("\"", "\\\"").replace("\n", " ");
    }
}
