package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.FExpression;
import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.coverage.StateCoverageGenerator;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.graph.ShortestPaths;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.testgeneration.product.TestCaseSplitter;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Per-product all-states visual report. For each valid configuration of
 * an SPL it shows the repaired FTS with the state-coverage walk overlaid
 * (greedy transitions bold/darkblue, BFS-reroute transitions dashed-red,
 * unvisited transitions grey), the walk's metrics, and the generated
 * test cases split at initial-state returns. Output:
 * {@code milestone-reports/per-product-all-states/<SPL>/}.
 *
 * <p>Run:
 * <pre>
 *   mvn -pl vibes-testgeneration exec:java \
 *       -Dexec.mainClass=be.vibes.testgeneration.experiment.PerProductAllStatesReportGenerator \
 *       -Dexec.classpathScope=test
 * </pre>
 */
public final class PerProductAllStatesReportGenerator {

    private static final class SplSpec {
        final String name;
        final String mxe;
        final String dimacs;
        final String mapping;
        SplSpec(String n, String m, String d, String map) {
            this.name = n; this.mxe = m; this.dimacs = d; this.mapping = map;
        }
    }

    private static final SplSpec[] SPLS = new SplSpec[] {
            new SplSpec("SVM",
                    "cases/SodaVendingMachine/SVM_ESGFx.mxe",
                    "cases/SodaVendingMachine/configs/SVM.dimacs",
                    "cases/SodaVendingMachine/configs/SVM_dimacsmapping.txt"),
            new SplSpec("eMail",
                    "cases/eMail/eM_ESGFx.mxe",
                    "cases/eMail/configs/eM.dimacs",
                    "cases/eMail/configs/eM_dimacsmapping.txt"),
            new SplSpec("Elevator",
                    "cases/Elevator/El_ESGFx.mxe",
                    "cases/Elevator/configs/El.dimacs",
                    "cases/Elevator/configs/El_dimacsmapping.txt"),
    };

    private enum WalkRole { GREEDY, REROUTE }

    private PerProductAllStatesReportGenerator() {
    }

    public static void main(String[] args) throws Exception {
        for (SplSpec spec : SPLS) run(spec);
    }

    private static void run(SplSpec spec) throws Exception {
        Path outDir = Paths.get("milestone-reports/per-product-all-states/" + spec.name);
        Files.createDirectories(outDir);

        FeaturedTransitionSystem fts = loadFts(spec.mxe);
        Sat4JSolverFacade solver = loadSolver(spec.dimacs, spec.mapping);
        Set<String> ftsFeatures = collectFeatureNames(fts);

        Path mdPath = outDir.resolve(spec.name + "-per-product-all-states-report.md");
        Path htmlPath = outDir.resolve(spec.name + "-per-product-all-states-report.html");

        try (BufferedWriter md = new BufferedWriter(new FileWriter(mdPath.toFile()));
             BufferedWriter html = new BufferedWriter(new FileWriter(htmlPath.toFile()))) {

            writeHtmlHeader(html, spec.name);
            String title = "Per-Product All-States Coverage — " + spec.name;
            md.write("# " + title + "\n\n");
            html.write("<h1>" + escapeHtml(title) + "</h1>\n");

            writeAlgorithmSection(md, html);

            md.write("---\n\n## Products\n\n");
            html.write("<hr/>\n<h2>Products</h2>\n");
            md.write("Each product below shows the repaired FTS with the state-coverage "
                    + "walk overlaid. **Legend:**\n\n"
                    + "- **darkblue solid bold** — real transition picked by the **greedy** "
                    + "phase (target was unvisited at selection time);\n"
                    + "- **darkorange solid bold** — real transition picked as part of a "
                    + "**BFS-reroute** shortest path (the greedy phase was stuck at a state "
                    + "with no unvisited neighbour);\n"
                    + "- **red dashed bold** — synthetic transition (`__end__`) that the walk "
                    + "happens to traverse; same convention as in the all-transitions report;\n"
                    + "- **light grey** — real transition not in the walk; **faint dashed red** "
                    + "— synthetic transition not in the walk. All transitions shown are "
                    + "present in the projected FTS exactly as drawn — none are synthesized "
                    + "for this report; the colour only encodes which phase of the algorithm "
                    + "picked them.\n\n"
                    + "Test cases below are obtained by splitting the walk at every visit to "
                    + "the initial state — `__end__` and `__balance__N` are hidden from the "
                    + "displayed action sequence, `__dup__N` is stripped (the latter two never "
                    + "appear in a state-coverage walk since no balancing is performed).\n\n");
            html.write("<p>Each product below shows the repaired FTS with the state-coverage "
                    + "walk overlaid. <strong>Legend:</strong></p>\n<ul>\n"
                    + "<li><strong>darkblue solid bold</strong> — real transition picked by "
                    + "the <strong>greedy</strong> phase;</li>\n"
                    + "<li><strong>darkorange solid bold</strong> — real transition picked as "
                    + "part of a <strong>BFS-reroute</strong> shortest path;</li>\n"
                    + "<li><strong>red dashed bold</strong> — synthetic transition "
                    + "(<code>__end__</code>) traversed by the walk;</li>\n"
                    + "<li><strong>light grey</strong> — real transition not in the walk; "
                    + "<strong>faint dashed red</strong> — synthetic not in the walk.</li>\n"
                    + "</ul>\n"
                    + "<p>All transitions shown are present in the projected FTS exactly as "
                    + "drawn — none are synthesized for this report; the colour only encodes "
                    + "which phase of the algorithm picked them.</p>\n");

            int productIndex = 0;
            Iterator<Configuration> configs = solver.getSolutions();
            while (configs.hasNext()) {
                Configuration cfg = configs.next();
                productIndex++;
                writeProductSection(md, html, spec, outDir, fts, cfg, productIndex, ftsFeatures);
            }
            md.write("---\n\nTotal products: " + productIndex + ".\n");
            html.write("<hr/>\n<p>Total products: " + productIndex + ".</p>\n");
            writeHtmlFooter(html);
        }
        System.out.println(spec.name + " per-product all-states report -> " + mdPath);
    }

    private static void writeAlgorithmSection(BufferedWriter md, BufferedWriter html)
            throws IOException {
        md.write("## How the all-states test case is built\n\n");
        md.write("Given an SPL-level FTS plus one product configuration, "
                + "[`StateCoverageGenerator.generate(fts, config, id)`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/"
                + "StateCoverageGenerator.java) runs five steps. The algorithm is the analog "
                + "of the ESG-Fx-side `EulerCycleGeneratorForEventCoverage` (event coverage on "
                + "an ESG = state coverage on its FTS conversion); the structural objective is "
                + "different from all-transitions, so the pipeline diverges after the SCC repair.\n\n");
        md.write("**Step 1 — Project onto the product.** Same as all-transitions: "
                + "`FExpressionPreservingProjection.project(fts, config)` keeps every "
                + "transition whose feature expression evaluates true under the product, "
                + "then a forward BFS drops states unreachable from the initial state.\n\n");
        md.write("**Step 2 — Repair strong connectivity.** Same as all-transitions: "
                + "`InitialSccFilter.keepInitialScc(projected)` keeps the SCC containing the "
                + "initial state. State coverage requires only that every state in the "
                + "repaired FTS be reachable AND able to return to wherever the walk decides "
                + "to keep going — strong connectivity guarantees both at once.\n\n");
        md.write("**Step 3 — Greedy walk.** Starting at the initial state, at every step "
                + "pick any outgoing transition whose target has not yet been visited. "
                + "Implementation in "
                + "[`pickUnvisitedNeighbour(...)`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/"
                + "StateCoverageGenerator.java) — first match wins; iteration order is the "
                + "FTS's natural outgoing-transition order, which is stable across runs given "
                + "the deterministic VIBeS state-name ordering. Walk extends, every newly-"
                + "visited target enters the `visited` set.\n\n");
        md.write("**Step 4 — BFS reroute when stuck.** When `pickUnvisitedNeighbour` returns "
                + "`null` (every outgoing of the current state goes to a state we've already "
                + "visited), the walk is stuck. "
                + "[`ShortestPaths.shortestPathToAny(fts, current, remaining)`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/"
                + "ShortestPaths.java) runs a BFS from the current state and returns the path "
                + "to the **nearest** state in the still-uncovered set. The path is appended "
                + "to the walk; every state along the path is marked visited (the reroute "
                + "incidentally covers intermediates too). Strong connectivity from step 2 "
                + "guarantees that a path always exists.\n\n");
        md.write("**Step 5 — Repeat until covered, then wrap.** Steps 3+4 alternate until "
                + "every state in the repaired FTS is in `visited`; the final transition list "
                + "is enqueued into `be.vibes.ts.TestCase`. For display the cycle is split at "
                + "initial-state returns via `TestCaseSplitter.splitAtInitialReturns(...)`. "
                + "Synthetics (`__end__`) are hidden in the rendered sequence; `__dup__` and "
                + "`__balance__` cannot occur (the state-coverage pipeline performs no "
                + "balancing).\n\n");
        md.write("**Why BFS over Dijkstra.** All transitions are unit-weight (no edge cost "
                + "distinguishes them at this layer), so BFS yields the optimal shortest path "
                + "without paying for a priority queue. If we ever want to bias the walk "
                + "(e.g. prefer paths that exercise more `__dup__` candidates, or paths that "
                + "discharge dangerous feature expressions first), we'd switch to weighted "
                + "Dijkstra — the API in `ShortestPaths` is structured to accept a weight "
                + "function in a follow-up.\n\n");
        md.write("**Coverage claim.** Step 5 terminates iff every state is visited, and step "
                + "5 always terminates because (a) strong connectivity from step 2 guarantees "
                + "BFS finds a path to any uncovered state, and (b) every iteration removes "
                + "at least one state from `remaining`. State coverage is therefore **100% on "
                + "the repaired FTS** by construction.\n\n");
        md.write("**Why this is generally shorter than all-transitions.** A walk that visits "
                + "every state is bounded below by `|V| - 1` transitions (visit-once tree); "
                + "an Euler cycle visiting every transition is bounded below by `|E|`. In "
                + "real FTSs `|E|` is typically 1.5–4× `|V|`, so state-coverage walks are "
                + "shorter — but they leave many edges un-exercised, weakening mutation "
                + "detection. That trade-off is what RQ3 quantifies.\n\n");

        html.write("<h2>How the all-states test case is built</h2>\n<ol>\n");
        html.write("<li><strong>Project onto the product.</strong> "
                + "<code>FExpressionPreservingProjection.project</code> keeps every transition "
                + "whose feature expression evaluates true under the configuration; a forward "
                + "BFS then drops states unreachable from the initial.</li>\n");
        html.write("<li><strong>Repair strong connectivity.</strong> "
                + "<code>InitialSccFilter.keepInitialScc</code> isolates the SCC containing "
                + "the initial state.</li>\n");
        html.write("<li><strong>Greedy walk.</strong> Starting at the initial state, repeatedly "
                + "pick any outgoing transition whose target has not yet been visited.</li>\n");
        html.write("<li><strong>BFS reroute when stuck.</strong> If every outgoing of the "
                + "current state goes to a visited state, "
                + "<code>ShortestPaths.shortestPathToAny</code> runs a BFS from the current "
                + "state to the nearest state still uncovered, and the resulting path is "
                + "appended to the walk. Intermediate states along the path are also marked "
                + "visited.</li>\n");
        html.write("<li><strong>Repeat until covered, then wrap.</strong> Steps 3 and 4 "
                + "alternate until every state is in the visited set; the transition list is "
                + "enqueued into <code>TestCase</code>, then "
                + "<code>TestCaseSplitter.splitAtInitialReturns</code> splits the walk at "
                + "every visit to the initial state for display.</li>\n");
        html.write("</ol>\n");
        html.write("<p><strong>Coverage claim:</strong> step 5 terminates iff every state is "
                + "visited; strong connectivity guarantees termination. State coverage is "
                + "therefore <strong>100% on the repaired FTS</strong> by construction. The "
                + "walk is typically shorter than an all-transitions Euler cycle (lower-bound "
                + "<code>|V|-1</code> vs <code>|E|</code>) but exercises fewer edges — the "
                + "fundamental RQ3 trade-off.</p>\n");
    }

    private static void writeProductSection(BufferedWriter md, BufferedWriter html,
                                            SplSpec spec, Path outDir,
                                            FeaturedTransitionSystem fts,
                                            Configuration cfg,
                                            int productIndex,
                                            Set<String> ftsFeatures) throws Exception {
        String featuresLine = formatFeatures(cfg, ftsFeatures);

        FeaturedTransitionSystem projected = FExpressionPreservingProjection.project(fts, cfg);
        FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);

        AnnotatedWalk annotated = retraceWalk(repaired);
        List<TestCase> suite = StateCoverageGenerator.generate(
                fts, cfg, spec.name + "_p" + productIndex + "_state");

        Set<String> greedyKeys = transitionKeys(annotated, WalkRole.GREEDY);
        Set<String> rerouteKeys = transitionKeys(annotated, WalkRole.REROUTE);

        String pngBase = spec.name + "-product" + productIndex + "-walk";
        Path pngPath = renderWalk(repaired, outDir, pngBase, greedyKeys, rerouteKeys);

        int totalStates = countStates(repaired);
        int visitedStates = visitedStateCount(annotated, repaired.getInitialState());
        int walkLen = annotated.transitions.size();
        int greedy = greedyKeys.size();
        int reroute = annotated.rerouteSegmentCount;

        md.write("\n### Product " + productIndex + "\n\n");
        md.write("**Selected features:** " + featuresLine + "\n\n");
        md.write("**Repaired FTS:** " + totalStates + " states, "
                + countTransitions(repaired) + " transitions ("
                + countRealTransitions(repaired) + " real / "
                + countEnd(repaired) + " `__end__`).\n\n");
        md.write("**State-coverage walk:** " + walkLen + " transition step(s) — "
                + greedy + " unique greedy edge(s), " + reroute
                + " BFS-reroute segment(s). State coverage on the repaired FTS: **"
                + visitedStates + "/" + totalStates + " = "
                + String.format("%.1f", 100.0 * visitedStates / totalStates) + "%**.\n\n");
        md.write("![Walk overlay — product " + productIndex + "]("
                + pngPath.getFileName() + ")\n\n");

        List<List<String>> trips = new ArrayList<>();
        for (TestCase tc : suite) {
            trips.addAll(renderTrips(tc, repaired.getInitialState()));
        }
        md.write("**Generated test cases** (" + trips.size()
                + " trip(s) from initial back to initial, hidden synthetics removed):\n\n");
        for (int i = 0; i < trips.size(); i++) {
            String seq = trips.get(i).isEmpty()
                    ? "(empty)"
                    : String.join(" -> ", trips.get(i));
            md.write("- **test case " + (i + 1) + "**: `" + seq + "`\n");
        }
        md.write("\n");

        html.write("<h3>Product " + productIndex + "</h3>\n");
        html.write("<p><strong>Selected features:</strong> "
                + escapeHtml(featuresLine) + "</p>\n");
        html.write("<p><strong>Repaired FTS:</strong> " + totalStates + " states, "
                + countTransitions(repaired) + " transitions ("
                + countRealTransitions(repaired) + " real / "
                + countEnd(repaired) + " <code>__end__</code>).</p>\n");
        html.write("<p><strong>State-coverage walk:</strong> " + walkLen
                + " transition step(s) — " + greedy + " unique greedy edge(s), "
                + reroute + " BFS-reroute segment(s). State coverage on the repaired FTS: "
                + "<strong>" + visitedStates + "/" + totalStates + " = "
                + String.format("%.1f", 100.0 * visitedStates / totalStates) + "%</strong>.</p>\n");
        html.write("<img src=\"" + escapeHtml(pngPath.getFileName().toString())
                + "\" alt=\"Walk overlay — product " + productIndex + "\"/>\n");
        html.write("<p><strong>Generated test cases</strong> (" + trips.size()
                + " trip(s) from initial back to initial, hidden synthetics removed):</p>\n<ul>\n");
        for (int i = 0; i < trips.size(); i++) {
            List<String> escaped = new ArrayList<>(trips.get(i).size());
            for (String a : trips.get(i)) escaped.add(escapeHtml(a));
            String seq = escaped.isEmpty()
                    ? "(empty)"
                    : String.join(" &rarr; ", escaped);
            html.write("<li><strong>test case " + (i + 1) + "</strong>: <code>"
                    + seq + "</code></li>\n");
        }
        html.write("</ul>\n");
    }

    // ---------- Walk re-tracing ----------

    /**
     * Replays the state-coverage walk in lock-step with
     * {@link StateCoverageGenerator}'s logic and annotates each transition
     * as either {@link WalkRole#GREEDY} (picked because its target was
     * unvisited) or {@link WalkRole#REROUTE} (picked as part of a BFS
     * shortest-path back to the nearest uncovered state). The walk is
     * computed twice (once here, once when generating the actual TestCase);
     * the duplication is the cost of keeping
     * {@link StateCoverageGenerator}'s public API minimal.
     */
    private static AnnotatedWalk retraceWalk(FeaturedTransitionSystem repaired) {
        State start = repaired.getInitialState();
        Set<State> allStates = new LinkedHashSet<>();
        Iterator<State> sIt = repaired.states();
        while (sIt.hasNext()) allStates.add(sIt.next());

        Set<State> visited = new HashSet<>();
        visited.add(start);
        Set<State> remaining = new LinkedHashSet<>(allStates);
        remaining.remove(start);

        List<Transition> walk = new ArrayList<>();
        List<WalkRole> roles = new ArrayList<>();
        State current = start;
        int rerouteSegments = 0;

        while (!remaining.isEmpty()) {
            Transition next = pickUnvisited(repaired, current, visited);
            if (next != null) {
                walk.add(next);
                roles.add(WalkRole.GREEDY);
                current = next.getTarget();
                visited.add(current);
                remaining.remove(current);
                continue;
            }
            List<Transition> path = ShortestPaths.shortestPathToAny(repaired, current, remaining);
            if (path == null) {
                throw new IllegalStateException(
                        "Cannot reroute from " + current.getName() + " — FTS not strongly connected?");
            }
            for (Transition t : path) {
                walk.add(t);
                roles.add(WalkRole.REROUTE);
                visited.add(t.getTarget());
                remaining.remove(t.getTarget());
            }
            current = path.get(path.size() - 1).getTarget();
            rerouteSegments++;
        }
        return new AnnotatedWalk(walk, roles, rerouteSegments);
    }

    private static Transition pickUnvisited(FeaturedTransitionSystem fts,
                                            State current, Set<State> visited) {
        Iterator<Transition> outs = fts.getOutgoing(current);
        while (outs.hasNext()) {
            Transition t = outs.next();
            if (!visited.contains(t.getTarget())) return t;
        }
        return null;
    }

    private static Set<String> transitionKeys(AnnotatedWalk a, WalkRole filter) {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < a.transitions.size(); i++) {
            if (a.roles.get(i) == filter) {
                keys.add(transitionKey(a.transitions.get(i)));
            }
        }
        return keys;
    }

    private static int visitedStateCount(AnnotatedWalk a, State initial) {
        Set<State> visited = new HashSet<>();
        visited.add(initial);
        for (Transition t : a.transitions) {
            visited.add(t.getSource());
            visited.add(t.getTarget());
        }
        return visited.size();
    }

    private static final class AnnotatedWalk {
        final List<Transition> transitions;
        final List<WalkRole> roles;
        final int rerouteSegmentCount;
        AnnotatedWalk(List<Transition> ts, List<WalkRole> rs, int rsc) {
            this.transitions = ts;
            this.roles = rs;
            this.rerouteSegmentCount = rsc;
        }
    }

    // ---------- Dot rendering with role-aware styling ----------

    /**
     * Renders the FTS in Dot with three styles on transitions: GREEDY
     * (darkblue bold), REROUTE (red dashed), unvisited (grey). Edges that
     * appear with both roles (rare — only if the same edge is used in
     * both phases for different visits) are shown as GREEDY since the
     * greedy contribution dominates intent.
     */
    private static Path renderWalk(FeaturedTransitionSystem fts, Path outDir,
                                   String basename,
                                   Set<String> greedyKeys,
                                   Set<String> rerouteKeys) throws Exception {
        Path dot = outDir.resolve(basename + ".dot");
        Path png = outDir.resolve(basename + ".png");
        try (PrintStream out = new PrintStream(dot.toFile())) {
            out.println(renderDot(fts, greedyKeys, rerouteKeys));
        }
        Process p = new ProcessBuilder("dot", "-Tpng", dot.toString(), "-o", png.toString())
                .inheritIO().start();
        p.waitFor();
        return png;
    }

    private static String renderDot(FeaturedTransitionSystem fts,
                                    Set<String> greedyKeys,
                                    Set<String> rerouteKeys) {
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
            String key = transitionKey(t);
            boolean synthetic = EulerianBalancer.isSyntheticAction(t.getAction());
            String style;
            if (synthetic && (greedyKeys.contains(key) || rerouteKeys.contains(key))) {
                // Synthetic edge that the walk did traverse — keep the dashed-red
                // convention used in the all-transitions report so the visual
                // language stays consistent across reports.
                style = ", style=dashed, color=red, penwidth=2";
            } else if (greedyKeys.contains(key)) {
                style = ", style=bold, color=darkblue, penwidth=2";
            } else if (rerouteKeys.contains(key)) {
                // Real transition used as part of a BFS reroute path —
                // distinct color, NOT dashed (the transition is genuinely
                // in the projected FTS; dashing it would falsely suggest
                // it was synthesized).
                style = ", style=bold, color=darkorange, penwidth=2";
            } else if (synthetic) {
                // Synthetic edge present in the FTS but not in the walk —
                // still flag with dashed-red but at lower weight.
                style = ", style=dashed, color=\"#cc8888\"";
            } else {
                style = ", color=\"#bbbbbb\", fontcolor=\"#888888\"";
            }
            out.append(src).append(" -> ").append(tgt)
                    .append(" [ label=\" ").append(escape(displayLabel(t, fts))).append(" \"")
                    .append(style).append(" ];\n");
        }
        out.append("}\n");
        return out.toString();
    }

    private static String displayLabel(Transition t, FeaturedTransitionSystem fts) {
        String actionName = t.getAction().getName();
        FExpression fexpr = fts.getFExpression(t);
        String fx = fexpr == null ? "true" : fexpr.applySimplification().toString();
        return normalize(actionName) + "/" + fx;
    }

    private static String transitionKey(Transition t) {
        return t.getSource().getName() + "|" + t.getAction().getName()
                + "|" + t.getTarget().getName();
    }

    // ---------- Test-case rendering ----------

    private static List<List<String>> renderTrips(TestCase tc, State initial) {
        List<List<String>> out = new ArrayList<>();
        for (List<Transition> trip : TestCaseSplitter.splitAtInitialReturns(tc, initial)) {
            List<String> actions = TestCaseSplitter.renderTripActions(trip);
            List<String> normalized = new ArrayList<>(actions.size());
            for (String a : actions) normalized.add(normalize(a));
            if (!normalized.isEmpty()) out.add(normalized);
        }
        return out;
    }

    // ---------- Helpers ----------

    private static Set<String> collectFeatureNames(FeaturedTransitionSystem fts) {
        Set<String> names = new HashSet<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            FExpression fexpr = fts.getFExpression(it.next());
            if (fexpr == null) continue;
            for (Feature f : fexpr.getFeatures()) names.add(f.getName());
        }
        return names;
    }

    private static String formatFeatures(Configuration cfg, Set<String> ftsFeatures) {
        TreeSet<String> sel = new TreeSet<>(), desel = new TreeSet<>();
        for (Feature f : cfg.getFeatures()) {
            if (!ftsFeatures.contains(f.getName())) continue;
            (cfg.isSelected(f) ? sel : desel).add(f.getName());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("selected = {").append(String.join(", ", sel)).append("}");
        if (!desel.isEmpty()) sb.append(", deselected = {").append(String.join(", ", desel)).append("}");
        return sb.toString();
    }

    private static int countStates(FeaturedTransitionSystem fts) {
        int n = 0; Iterator<State> it = fts.states(); while (it.hasNext()) { it.next(); n++; } return n;
    }

    private static int countTransitions(FeaturedTransitionSystem fts) {
        int n = 0; Iterator<Transition> it = fts.transitions(); while (it.hasNext()) { it.next(); n++; } return n;
    }

    private static int countRealTransitions(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            if (!EulerianBalancer.isSyntheticAction(it.next().getAction())) n++;
        }
        return n;
    }

    private static int countEnd(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            if (it.next().getAction().getName().startsWith("__end__")) n++;
        }
        return n;
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private static String escape(String s) {
        return s.replace("\"", "\\\"").replace("\n", " ");
    }

    private static FeaturedTransitionSystem loadFts(String r) throws Exception {
        URL u = PerProductAllStatesReportGenerator.class.getClassLoader().getResource(r);
        return new MxeToFtsConverter().convert(new File(u.toURI()));
    }

    private static Sat4JSolverFacade loadSolver(String d, String m) throws Exception {
        URL du = PerProductAllStatesReportGenerator.class.getClassLoader().getResource(d);
        URL mu = PerProductAllStatesReportGenerator.class.getClassLoader().getResource(m);
        return new Sat4JSolverFacade(DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mu.toURI()), new File(du.toURI())));
    }

    private static void writeHtmlHeader(BufferedWriter out, String name) throws IOException {
        out.write("<!DOCTYPE html>\n<html lang=\"en\"><head>\n<meta charset=\"UTF-8\"/>\n");
        out.write("<title>Per-Product All-States — " + escapeHtml(name) + "</title>\n");
        out.write("<style>\nbody { font-family: -apple-system, BlinkMacSystemFont, sans-serif; "
                + "max-width: 1080px; margin: 2em auto; padding: 0 1em; line-height: 1.55; color: #222; }\n"
                + "h1 { border-bottom: 2px solid #333; padding-bottom: 0.3em; }\n"
                + "h2 { margin-top: 2em; color: #444; border-bottom: 1px solid #ddd; padding-bottom: 0.2em; }\n"
                + "h3 { margin-top: 1.6em; color: #555; }\n"
                + "code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; "
                + "font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 0.95em; }\n"
                + "img { max-width: 100%; border: 1px solid #ccc; padding: 4px; background: white; "
                + "display: block; margin: 1em auto; }\n"
                + "ul { padding-left: 1.4em; } hr { border: 0; border-top: 1px solid #ccc; margin: 2.5em 0; }\n"
                + "</style>\n</head><body>\n");
    }

    private static void writeHtmlFooter(BufferedWriter out) throws IOException {
        out.write("</body></html>\n");
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
