package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.FExpression;
import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.coverage.TransitionCoverageGenerator;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.testgeneration.graph.HierholzerEulerCycle;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Per-product all-transitions visual report. For each valid configuration of
 * an SPL it shows two PNGs side by side (projected FTS, then balanced FTS
 * with Chinese-Postman doubled edges dashed-red) plus the generated
 * all-transitions test case as a single action sequence. Output lives at
 * {@code milestone-reports/per-product-all-transitions/<SPL>/}.
 *
 * <p>The header section explains the M4 algorithm in five steps with
 * pointers to the implementing classes — included on every SPL report so
 * the artefact is self-contained.
 *
 * <p>Run:
 * <pre>
 *   mvn -pl vibes-testgeneration exec:java \
 *       -Dexec.mainClass=be.vibes.testgeneration.experiment.PerProductAllTransitionsReportGenerator \
 *       -Dexec.classpathScope=test
 * </pre>
 */
public final class PerProductAllTransitionsReportGenerator {

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

    private PerProductAllTransitionsReportGenerator() {}

    public static void main(String[] args) throws Exception {
        for (SplSpec spec : SPLS) run(spec);
    }

    private static void run(SplSpec spec) throws Exception {
        Path outDir = Paths.get("milestone-reports/per-product-all-transitions/" + spec.name);
        Files.createDirectories(outDir);

        FeaturedTransitionSystem fts = loadFts(spec.mxe);
        Sat4JSolverFacade solver = loadSolver(spec.dimacs, spec.mapping);
        Set<String> ftsFeatures = collectFeatureNames(fts);

        Path mdPath = outDir.resolve(spec.name + "-per-product-all-transitions-report.md");
        Path htmlPath = outDir.resolve(spec.name + "-per-product-all-transitions-report.html");

        try (BufferedWriter md = new BufferedWriter(new FileWriter(mdPath.toFile()));
             BufferedWriter html = new BufferedWriter(new FileWriter(htmlPath.toFile()))) {

            writeHtmlHeader(html, spec.name);
            String title = "Per-Product All-Transitions Coverage — " + spec.name;
            md.write("# " + title + "\n\n");
            html.write("<h1>" + escapeHtml(title) + "</h1>\n");

            writeAlgorithmSection(md, html);

            md.write("---\n\n## Products\n\n");
            html.write("<hr/>\n<h2>Products</h2>\n");
            md.write("Each product below shows the projected FTS (left, synthetic `__end__` "
                    + "transitions dashed-red) and the balanced FTS (right, Chinese-Postman "
                    + "doubled transitions `<action>__dup__N` dashed-red). The generated "
                    + "all-transitions test case is listed underneath, segmented at every "
                    + "synthetic action (the action sequence between two synthetic boundaries "
                    + "is one self-contained sub-walk on real SUT events).\n\n");
            html.write("<p>Each product below shows the projected FTS (synthetic <code>__end__</code> "
                    + "dashed-red) and the balanced FTS (Chinese-Postman doubled "
                    + "<code>&lt;action&gt;__dup__N</code> dashed-red). The generated "
                    + "all-transitions test case is listed underneath, segmented at every "
                    + "synthetic action.</p>\n");

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
        System.out.println(spec.name + " per-product all-transitions report -> " + mdPath);
    }

    private static void writeAlgorithmSection(BufferedWriter md, BufferedWriter html)
            throws IOException {
        md.write("## How the all-transitions test case is built (M4 pipeline)\n\n");
        md.write("Given an SPL-level FTS plus one product configuration, "
                + "the generator runs five steps. All five live in the "
                + "`vibes-testgeneration` module; the orchestrator is "
                + "[`TransitionCoverageGenerator.generate(...)`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/"
                + "TransitionCoverageGenerator.java).\n\n");
        md.write("**Step 1 — Project onto the product.** "
                + "[`FExpressionPreservingProjection.project(fts, config)`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/product/"
                + "FExpressionPreservingProjection.java) keeps every transition `t` whose "
                + "feature expression evaluates true under the product "
                + "(`fts.getFExpression(t).assign(config).applySimplification().isTrue()`); "
                + "the original (un-assigned) `FExpression` is preserved on the kept transition "
                + "for traceability. A forward BFS from the initial state then drops states "
                + "unreachable from it. Output: a product-level `FeaturedTransitionSystem`.\n\n");
        md.write("**Step 2 — Repair strong connectivity.** "
                + "[`InitialSccFilter.keepInitialScc(projected)`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/"
                + "InitialSccFilter.java) runs "
                + "[Tarjan's SCC]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/"
                + "StronglyConnectedComponents.java), keeps the SCC containing the initial state, "
                + "and drops every other state (and its transitions). The result is strongly "
                + "connected by construction — the precondition for any Eulerian-cycle algorithm. "
                + "In the three MVP SPLs this step is currently a no-op (the projection already "
                + "produced a single SCC reachable from initial); we still run it as an invariant "
                + "check and to keep the pipeline robust for larger SPLs.\n\n");
        md.write("**Step 3 — Balance for an Euler cycle.** "
                + "[`EulerianBalancer.balance(repaired)`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/"
                + "EulerianBalancer.java) makes the graph Eulerian by enforcing "
                + "`in-degree == out-degree` at every state. The approach is a directed "
                + "**Chinese Postman**: for each pair of imbalanced states `(u, v)` "
                + "(`u` has excess outgoing, `v` has excess incoming) it finds a shortest path "
                + "of real transitions from `v` to `u` via BFS, then **doubles** every "
                + "transition along that path. Doubled transitions get a unique action name "
                + "`<original>__dup__N` so they survive VIBeS' dedup but their semantic action is "
                + "the original — coverage measurement strips the suffix. Where no real path "
                + "exists (e.g. the pair-graph from M6) the balancer falls back to a direct "
                + "synthetic `__balance__N` edge.\n\n");
        md.write("**Step 4 — Trace the Euler cycle.** "
                + "[`HierholzerEulerCycle.compute(balanced)`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/"
                + "HierholzerEulerCycle.java) walks the balanced graph using "
                + "Hierholzer's algorithm: DFS until a sub-cycle closes, splice in additional "
                + "sub-cycles from unvisited transitions, repeat. The output is one contiguous "
                + "sequence of transitions that visits every edge of the balanced graph exactly "
                + "once and returns to the initial state.\n\n");
        md.write("**Step 5 — Wrap into a TestCase.** The cycle is enqueued into "
                + "`be.vibes.ts.TestCase`. Synthetic actions (`__end__`, `__balance__N`, "
                + "`<action>__dup__N`) remain in the test case so the executor can use them "
                + "as test-case boundary markers (everything between two synthetics is one "
                + "real-SUT sub-walk); they are filtered before coverage measurement via "
                + "`EulerianBalancer.isSyntheticAction(...)`.\n\n");
        md.write("**Coverage claim (by construction).** Every real transition in the projected "
                + "FTS appears in the balanced FTS (balancing only adds, never removes). "
                + "The Hierholzer cycle visits every transition of the balanced graph exactly "
                + "once. Therefore the cycle's real (non-synthetic) transitions cover **100% "
                + "of the projected FTS' real transitions**. This is a structural invariant, "
                + "not an empirical observation.\n\n");

        html.write("<h2>How the all-transitions test case is built (M4 pipeline)</h2>\n");
        html.write("<p>Given an SPL-level FTS plus one product configuration, the generator "
                + "runs five steps. All five live in <code>vibes-testgeneration</code>; the "
                + "orchestrator is <code>TransitionCoverageGenerator.generate(...)</code>.</p>\n");
        html.write("<ol>\n");
        html.write("<li><strong>Project onto the product.</strong> "
                + "<code>FExpressionPreservingProjection.project</code> keeps every transition "
                + "whose feature expression evaluates true under the configuration; a forward "
                + "BFS from the initial state then drops unreachable states.</li>\n");
        html.write("<li><strong>Repair strong connectivity.</strong> "
                + "<code>InitialSccFilter.keepInitialScc</code> (uses "
                + "<code>StronglyConnectedComponents</code> for Tarjan) isolates the SCC "
                + "containing the initial state.</li>\n");
        html.write("<li><strong>Balance for an Euler cycle.</strong> "
                + "<code>EulerianBalancer.balance</code> runs a directed Chinese Postman: "
                + "for each pair of imbalanced states it finds a shortest path of real "
                + "transitions and doubles them (label suffix <code>__dup__N</code>); "
                + "fallback to direct <code>__balance__N</code> when no path exists.</li>\n");
        html.write("<li><strong>Trace the Euler cycle.</strong> "
                + "<code>HierholzerEulerCycle.compute</code> walks the balanced graph with "
                + "Hierholzer's algorithm, returning one contiguous transition sequence that "
                + "visits every edge of the balanced graph exactly once.</li>\n");
        html.write("<li><strong>Wrap into a TestCase.</strong> The cycle is enqueued; "
                + "synthetic actions remain as test-case boundary markers and are filtered "
                + "from coverage measurement via "
                + "<code>EulerianBalancer.isSyntheticAction</code>.</li>\n");
        html.write("</ol>\n");
        html.write("<p><strong>Coverage claim (by construction).</strong> Balancing only adds "
                + "edges, Hierholzer visits every edge of the balanced graph exactly once, "
                + "therefore the cycle's real (non-synthetic) transitions cover 100% of the "
                + "projected FTS' real transitions — a structural invariant, not an empirical "
                + "observation.</p>\n");
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
        FeaturedTransitionSystem balanced;
        Exception balanceError = null;
        try {
            balanced = EulerianBalancer.balance(repaired);
        } catch (Exception ex) {
            balanced = null;
            balanceError = ex;
        }

        // Render projected (synthetic __end__ dashed-red) and balanced (additionally
        // __dup__N / __balance__N dashed-red).
        String projBase = spec.name + "-product" + productIndex + "-projected";
        String balBase = spec.name + "-product" + productIndex + "-balanced";
        Path projPng = renderStyled(projected, outDir, projBase, true);
        Path balPng = balanced != null
                ? renderStyled(balanced, outDir, balBase, true)
                : null;

        // Generate the all-transitions test case via the M4 orchestrator.
        TestCase tc;
        Exception genError = null;
        try {
            tc = TransitionCoverageGenerator.generate(
                    fts, cfg, spec.name + "_p" + productIndex + "_trans");
        } catch (Exception ex) {
            tc = null;
            genError = ex;
        }

        int realCount = countRealTransitions(repaired);
        int allCount = countTransitions(repaired);
        int realInTc = tc == null ? 0 : countRealTransitions(tc);
        int dupInTc = tc == null ? 0 : countDuplicates(tc);
        int fbInTc = tc == null ? 0 : countFallback(tc);
        int endInTc = tc == null ? 0 : countEnd(tc);
        Set<String> coveredKeys = tc == null ? Collections.emptySet() : coveredRealKeys(tc);
        Set<String> allKeys = realTransitionKeys(repaired);
        int hits = 0;
        for (String k : coveredKeys) {
            if (allKeys.contains(k)) hits++;
        }
        double pct = allKeys.isEmpty() ? 1.0 : (double) hits / (double) allKeys.size();

        md.write("\n### Product " + productIndex + "\n\n");
        md.write("**Selected features:** " + featuresLine + "\n\n");
        md.write("**Projected FTS:** " + countStates(projected) + " states, "
                + countTransitions(projected) + " transitions ("
                + countRealTransitions(projected) + " real / "
                + countEnd(projected) + " `__end__`).\n\n");
        md.write("**Repaired FTS (after `InitialSccFilter`):** " + countStates(repaired)
                + " states, " + allCount + " transitions ("
                + realCount + " real / " + countEnd(repaired) + " `__end__`).\n\n");
        if (balanced != null) {
            md.write("**Balanced FTS:** " + countStates(balanced) + " states, "
                    + countTransitions(balanced) + " transitions ("
                    + countRealTransitions(balanced) + " real / "
                    + countEnd(balanced) + " `__end__` / "
                    + countDuplicates(balanced) + " `__dup__` / "
                    + countFallback(balanced) + " `__balance__`).\n\n");
        } else {
            md.write("**Balanced FTS:** could not balance: `" + balanceError + "`.\n\n");
        }

        md.write("![Projected FTS — product " + productIndex + "]("
                + projPng.getFileName() + ")\n\n");
        if (balPng != null) {
            md.write("![Balanced FTS — product " + productIndex + "]("
                    + balPng.getFileName() + ")\n\n");
        }

        if (tc != null) {
            List<List<String>> segments = splitAtSynthetics(tc);
            md.write("**All-transitions test case (`" + tc.getId() + "`)** — "
                    + countTestCaseLength(tc) + " step(s) total ("
                    + realInTc + " real / "
                    + endInTc + " `__end__` / "
                    + dupInTc + " `__dup__` / "
                    + fbInTc + " `__balance__`). Real-transition coverage on the "
                    + "repaired FTS: **" + hits + "/" + allKeys.size() + " = "
                    + String.format("%.1f", pct * 100.0) + "%**.\n\n");
            md.write("Sub-walks between synthetic boundaries:\n\n");
            for (int i = 0; i < segments.size(); i++) {
                String seq = segments.get(i).isEmpty()
                        ? "(empty)"
                        : String.join(" -> ", segments.get(i));
                md.write("- **sub-walk " + (i + 1) + "**: `" + seq + "`\n");
            }
            md.write("\nFull cycle (synthetic actions shown verbatim):\n\n");
            md.write("```\n" + fullSequenceWithLabels(tc) + "\n```\n\n");
        } else {
            md.write("**All-transitions test case:** could not generate: `"
                    + genError + "`.\n\n");
        }

        html.write("<h3>Product " + productIndex + "</h3>\n");
        html.write("<p><strong>Selected features:</strong> "
                + escapeHtml(featuresLine) + "</p>\n");
        html.write("<p><strong>Projected FTS:</strong> " + countStates(projected) + " states, "
                + countTransitions(projected) + " transitions ("
                + countRealTransitions(projected) + " real / "
                + countEnd(projected) + " <code>__end__</code>).</p>\n");
        html.write("<p><strong>Repaired FTS:</strong> " + countStates(repaired) + " states, "
                + allCount + " transitions (" + realCount + " real / "
                + countEnd(repaired) + " <code>__end__</code>).</p>\n");
        if (balanced != null) {
            html.write("<p><strong>Balanced FTS:</strong> " + countStates(balanced) + " states, "
                    + countTransitions(balanced) + " transitions ("
                    + countRealTransitions(balanced) + " real / "
                    + countEnd(balanced) + " <code>__end__</code> / "
                    + countDuplicates(balanced) + " <code>__dup__</code> / "
                    + countFallback(balanced) + " <code>__balance__</code>).</p>\n");
        } else {
            html.write("<p><strong>Balanced FTS:</strong> could not balance: <code>"
                    + escapeHtml(String.valueOf(balanceError)) + "</code></p>\n");
        }

        html.write("<img src=\"" + escapeHtml(projPng.getFileName().toString())
                + "\" alt=\"Projected FTS — product " + productIndex + "\"/>\n");
        if (balPng != null) {
            html.write("<img src=\"" + escapeHtml(balPng.getFileName().toString())
                    + "\" alt=\"Balanced FTS — product " + productIndex + "\"/>\n");
        }

        if (tc != null) {
            List<List<String>> segments = splitAtSynthetics(tc);
            html.write("<p><strong>All-transitions test case (<code>"
                    + escapeHtml(tc.getId()) + "</code>)</strong> — "
                    + countTestCaseLength(tc) + " step(s) total ("
                    + realInTc + " real / " + endInTc + " <code>__end__</code> / "
                    + dupInTc + " <code>__dup__</code> / "
                    + fbInTc + " <code>__balance__</code>). "
                    + "Real-transition coverage on the repaired FTS: <strong>"
                    + hits + "/" + allKeys.size() + " = "
                    + String.format("%.1f", pct * 100.0) + "%</strong>.</p>\n");
            html.write("<p>Sub-walks between synthetic boundaries:</p>\n<ul>\n");
            for (int i = 0; i < segments.size(); i++) {
                String seq = segments.get(i).isEmpty()
                        ? "(empty)"
                        : String.join(" &rarr; ", segments.get(i));
                html.write("<li><strong>sub-walk " + (i + 1) + "</strong>: <code>"
                        + escapeHtml(seq) + "</code></li>\n");
            }
            html.write("</ul>\n");
            html.write("<p>Full cycle (synthetic actions shown verbatim):</p>\n");
            html.write("<pre>" + escapeHtml(fullSequenceWithLabels(tc)) + "</pre>\n");
        } else {
            html.write("<p><strong>All-transitions test case:</strong> could not generate: "
                    + "<code>" + escapeHtml(String.valueOf(genError)) + "</code></p>\n");
        }
    }

    private static Path renderStyled(FeaturedTransitionSystem fts, Path outDir,
                                     String basename, boolean autoDashSynthetics)
            throws Exception {
        Path dot = outDir.resolve(basename + ".dot");
        Path png = outDir.resolve(basename + ".png");
        try (PrintStream out = new PrintStream(dot.toFile())) {
            out.println(StyledDotRenderer.render(fts, Collections.emptySet(), autoDashSynthetics));
        }
        Process p = new ProcessBuilder("dot", "-Tpng", dot.toString(), "-o", png.toString())
                .inheritIO().start();
        p.waitFor();
        return png;
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

    private static List<List<String>> splitAtSynthetics(TestCase tc) {
        List<List<String>> segments = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (Transition t : tc) {
            if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                if (!current.isEmpty()) {
                    segments.add(current);
                    current = new ArrayList<>();
                }
            } else {
                current.add(normalize(t.getAction().getName()));
            }
        }
        if (!current.isEmpty()) segments.add(current);
        if (segments.isEmpty()) segments.add(new ArrayList<>());
        return segments;
    }

    private static String fullSequenceWithLabels(TestCase tc) {
        List<String> parts = new ArrayList<>();
        for (Transition t : tc) {
            String name = t.getAction().getName();
            if (name.contains(EulerianBalancer.DUPLICATE_ACTION_INFIX)) {
                String base = EulerianBalancer.stripDuplicateSuffix(name);
                parts.add(normalize(base) + "(dup)");
            } else {
                parts.add(normalize(name));
            }
        }
        return String.join(" -> ", parts);
    }

    /**
     * Collapses any embedded whitespace (newlines, tabs, multiple spaces) in
     * an action name down to a single space. Some MXE files carry literal
     * newlines inside event labels (the user authors them in mxGraph with
     * Shift-Enter for visual word-wrap); displaying these verbatim breaks the
     * "a -&gt; b -&gt; c" formatting. This is a pure-rendering normalization;
     * the underlying FTS keeps the raw action name (and so does coverage
     * measurement, so no semantic drift).
     */
    private static String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private static int countTestCaseLength(TestCase tc) {
        int n = 0;
        Iterator<Transition> it = tc.iterator();
        while (it.hasNext()) { it.next(); n++; }
        return n;
    }

    private static int countStates(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<State> it = fts.states();
        while (it.hasNext()) { it.next(); n++; }
        return n;
    }

    private static int countTransitions(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) { it.next(); n++; }
        return n;
    }

    private static int countRealTransitions(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            if (!EulerianBalancer.isSyntheticAction(it.next().getAction())) n++;
        }
        return n;
    }

    private static int countRealTransitions(TestCase tc) {
        int n = 0;
        for (Transition t : tc) {
            if (!EulerianBalancer.isSyntheticAction(t.getAction())) n++;
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

    private static int countEnd(TestCase tc) {
        int n = 0;
        for (Transition t : tc) {
            if (t.getAction().getName().startsWith("__end__")) n++;
        }
        return n;
    }

    private static int countDuplicates(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            if (it.next().getAction().getName()
                    .contains(EulerianBalancer.DUPLICATE_ACTION_INFIX)) n++;
        }
        return n;
    }

    private static int countDuplicates(TestCase tc) {
        int n = 0;
        for (Transition t : tc) {
            if (t.getAction().getName()
                    .contains(EulerianBalancer.DUPLICATE_ACTION_INFIX)) n++;
        }
        return n;
    }

    private static int countFallback(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            if (it.next().getAction().getName()
                    .startsWith(EulerianBalancer.SYNTHETIC_ACTION_PREFIX)) n++;
        }
        return n;
    }

    private static int countFallback(TestCase tc) {
        int n = 0;
        for (Transition t : tc) {
            if (t.getAction().getName()
                    .startsWith(EulerianBalancer.SYNTHETIC_ACTION_PREFIX)) n++;
        }
        return n;
    }

    private static Set<String> realTransitionKeys(FeaturedTransitionSystem fts) {
        Set<String> keys = new HashSet<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            Transition t = it.next();
            if (EulerianBalancer.isSyntheticAction(t.getAction())) continue;
            keys.add(t.getSource().getName() + "|" + t.getAction().getName()
                    + "|" + t.getTarget().getName());
        }
        return keys;
    }

    private static Set<String> coveredRealKeys(TestCase tc) {
        Set<String> keys = new HashSet<>();
        for (Transition t : tc) {
            if (EulerianBalancer.isSyntheticAction(t.getAction())) continue;
            keys.add(t.getSource().getName() + "|" + t.getAction().getName()
                    + "|" + t.getTarget().getName());
        }
        return keys;
    }

    private static FeaturedTransitionSystem loadFts(String r) throws Exception {
        URL u = PerProductAllTransitionsReportGenerator.class
                .getClassLoader().getResource(r);
        return new MxeToFtsConverter().convert(new File(u.toURI()));
    }

    private static Sat4JSolverFacade loadSolver(String d, String m) throws Exception {
        URL du = PerProductAllTransitionsReportGenerator.class
                .getClassLoader().getResource(d);
        URL mu = PerProductAllTransitionsReportGenerator.class
                .getClassLoader().getResource(m);
        return new Sat4JSolverFacade(DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mu.toURI()), new File(du.toURI())));
    }

    private static void writeHtmlHeader(BufferedWriter out, String name) throws IOException {
        out.write("<!DOCTYPE html>\n<html lang=\"en\"><head>\n<meta charset=\"UTF-8\"/>\n");
        out.write("<title>Per-Product All-Transitions — " + escapeHtml(name) + "</title>\n");
        out.write("<style>\nbody { font-family: -apple-system, BlinkMacSystemFont, sans-serif; "
                + "max-width: 1080px; margin: 2em auto; padding: 0 1em; line-height: 1.55; color: #222; }\n"
                + "h1 { border-bottom: 2px solid #333; padding-bottom: 0.3em; }\n"
                + "h2 { margin-top: 2em; color: #444; border-bottom: 1px solid #ddd; padding-bottom: 0.2em; }\n"
                + "h3 { margin-top: 1.6em; color: #555; }\n"
                + "code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; "
                + "font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 0.95em; }\n"
                + "pre { background: #f4f4f4; padding: 10px 12px; border-radius: 4px; "
                + "overflow-x: auto; font-family: ui-monospace, Menlo, Consolas, monospace; "
                + "font-size: 0.9em; }\n"
                + "img { max-width: 100%; border: 1px solid #ccc; padding: 4px; background: white; "
                + "display: block; margin: 1em auto; }\n"
                + "ul { padding-left: 1.4em; }\n"
                + "hr { border: 0; border-top: 1px solid #ccc; margin: 2.5em 0; }\n"
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
