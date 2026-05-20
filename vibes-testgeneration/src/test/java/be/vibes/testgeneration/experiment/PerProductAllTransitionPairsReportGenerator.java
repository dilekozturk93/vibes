package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.FExpression;
import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.coverage.PairGraphTransformer;
import be.vibes.testgeneration.coverage.TransitionPairCoverageGenerator;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.testgeneration.graph.InitialSccFilter;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Per-product all-transition-pairs visual report. For each valid product
 * of each MVP SPL it shows:
 * <ol>
 *   <li>the repaired FTS (origin of the pair info, synthetic
 *       <code>__end__</code> dashed-red);</li>
 *   <li>the pair graph BEFORE balancing — INIT vertex source-only,
 *       imbalanced as expected;</li>
 *   <li>the pair graph AFTER balancing — INIT supplied with synthetic
 *       incoming edges (dashed-red) to restore strong connectivity;</li>
 *   <li>the generated test suite — one test case per real Hierholzer
 *       segment, each further split at initial-state returns.</li>
 * </ol>
 * Output: {@code milestone-reports/per-product-all-transition-pairs/<SPL>/}.
 *
 * <p>Run:
 * <pre>
 *   mvn -pl vibes-testgeneration exec:java \
 *       -Dexec.mainClass=be.vibes.testgeneration.experiment.PerProductAllTransitionPairsReportGenerator \
 *       -Dexec.classpathScope=test
 * </pre>
 */
public final class PerProductAllTransitionPairsReportGenerator {

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

    private PerProductAllTransitionPairsReportGenerator() {
    }

    public static void main(String[] args) throws Exception {
        for (SplSpec spec : SPLS) run(spec);
    }

    private static void run(SplSpec spec) throws Exception {
        Path outDir = Paths.get("milestone-reports/per-product-all-transition-pairs/" + spec.name);
        Files.createDirectories(outDir);

        FeaturedTransitionSystem fts = loadFts(spec.mxe);
        Sat4JSolverFacade solver = loadSolver(spec.dimacs, spec.mapping);
        Set<String> ftsFeatures = collectFeatureNames(fts);

        Path mdPath = outDir.resolve(spec.name + "-per-product-all-transition-pairs-report.md");
        Path htmlPath = outDir.resolve(spec.name + "-per-product-all-transition-pairs-report.html");

        try (BufferedWriter md = new BufferedWriter(new FileWriter(mdPath.toFile()));
             BufferedWriter html = new BufferedWriter(new FileWriter(htmlPath.toFile()))) {

            writeHtmlHeader(html, spec.name);
            String title = "Per-Product All-Transition-Pairs Coverage — " + spec.name;
            md.write("# " + title + "\n\n");
            html.write("<h1>" + escapeHtml(title) + "</h1>\n");

            writeAlgorithmSection(md, html);

            md.write("---\n\n## Products\n\n");
            html.write("<hr/>\n<h2>Products</h2>\n");
            md.write("Three FTS images per product: (a) the repaired original FTS — source "
                    + "of the pair info; (b) the pair graph BEFORE balancing — INIT is "
                    + "source-only and the graph is not Eulerian; (c) the pair graph AFTER "
                    + "balancing — synthetic edges (`__balance__N`) added back to INIT shown "
                    + "as dashed-red. Generated test cases are listed underneath, split at "
                    + "every visit to the original repaired FTS's initial state. Synthetic "
                    + "actions are hidden / `__dup__N` is stripped, same convention as the "
                    + "other reports.\n\n");
            html.write("<p>Three FTS images per product: (a) the repaired original FTS — "
                    + "source of the pair info; (b) the pair graph BEFORE balancing — INIT "
                    + "source-only and the graph is not Eulerian; (c) the pair graph AFTER "
                    + "balancing — synthetic <code>__balance__N</code> edges to INIT shown "
                    + "dashed-red. Generated test cases are listed underneath.</p>\n");

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
        System.out.println(spec.name + " per-product all-transition-pairs report -> " + mdPath);
    }

    private static void writeAlgorithmSection(BufferedWriter md, BufferedWriter html)
            throws IOException {
        md.write("## How the all-transition-pairs test suite is built\n\n");
        md.write("**Why a fresh transformation, not `vibes-transformation`.** "
                + "The legacy `vibes-transformation` module (not in the root pom) is an I/O "
                + "and format-conversion layer (AUT &harr; LTS, XML readers, Dot printers, "
                + "structural pruning) operating on the obsolete `be.unamur.transitionsystem.*` "
                + "namespace; it does not contain anything for pair-graph / k-tuple coverage "
                + "transformation and is dormant. The L=2 pair graph here is the FTS analog of "
                + "the ESG-Fx-side `TransformedESGFxGenerator` (from the user's prior published "
                + "study); we re-implement against the current `be.vibes.ts.*` types directly.\n\n");
        md.write("**Reduction insight.** The pair-coverage problem on FTS *F* reduces to the "
                + "edge-coverage problem on a transformed FTS *P(F)* — the *pair graph*. A "
                + "Hierholzer Euler cycle on *P(F)* visits every edge of *P(F)* exactly once, "
                + "which by construction means every contiguous transition pair of *F* is "
                + "exercised in the resulting test sequence.\n\n");
        md.write("**Step 1 — Project onto the product.** Same as the other criteria: "
                + "`FExpressionPreservingProjection.project(fts, config)` keeps transitions "
                + "whose feature expression evaluates true; reachability filter drops "
                + "unreachable states.\n\n");
        md.write("**Step 2 — Repair strong connectivity on the original FTS.** Same as the "
                + "other criteria: `InitialSccFilter.keepInitialScc(projected)`.\n\n");
        md.write("**Step 3 — Build the pair graph.** "
                + "[`PairGraphTransformer.transform(repaired)`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/"
                + "PairGraphTransformer.java) produces a pair graph *P(F)* whose:\n\n"
                + "- **node** = original transition (named `p_<source>_<action>_<target>`), "
                + "plus a synthetic `INIT` node representing \"no transition executed yet\";\n"
                + "- **edge** `INIT -> p(t)` (labelled `action(t)`) for every original "
                + "transition `t` starting at the original initial state;\n"
                + "- **edge** `p(t1) -> p(t2)` (labelled `action(t2)`) for every ordered "
                + "pair `(t1, t2)` with `target(t1) == source(t2)`.\n\n"
                + "Complexity: enumerate via an outgoing-by-source index on the original FTS, "
                + "so the construction is *O(|T| + sum_s out_deg(s))* rather than *O(|T|^2)*. "
                + "The transformer also returns a side-map `pairStateToOriginalTransition` "
                + "used in step 6 to translate the pair-graph cycle back to original "
                + "transitions.\n\n");
        md.write("**Step 4 — Balance (no SCC precheck).** The pair graph is intentionally "
                + "not strongly connected at this point: INIT has out-degree N (one per "
                + "original initial-state transition) and in-degree 0. Running the usual "
                + "`InitialSccFilter` here would discard everything except INIT. "
                + "[`EulerianBalancer.balanceWithoutPrecheck(pairGraph)`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/"
                + "EulerianBalancer.java) skips the SCC check and supplies the missing "
                + "INIT-incoming edges as synthetic `__balance__N` edges — exactly enough to "
                + "make every pair-graph state in-balanced AND restore strong connectivity. "
                + "The balanced pair graph is verified to be strongly connected after this "
                + "step; if it isn't, the projection has produced an unrepaired pair-graph "
                + "fragment (does not happen on the three MVP SPLs).\n\n");
        md.write("**Step 5 — Hierholzer Euler cycle on the balanced pair graph.** "
                + "[`HierholzerEulerCycle.compute(balanced)`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/"
                + "HierholzerEulerCycle.java) returns one contiguous cycle visiting every "
                + "pair-graph edge exactly once. By the reduction insight above, every "
                + "contiguous transition pair of the original FTS is covered.\n\n");
        md.write("**Step 6 — Split + translate back.** Synthetic `__balance__N` edges in the "
                + "pair-graph cycle cannot be replayed in the original FTS (they encode "
                + "\"teleport back to INIT\"), so the cycle is split at every synthetic edge. "
                + "Each non-synthetic segment is translated: each pair-graph edge "
                + "`p(t1) -> p(t2)` corresponds to executing `t2` in the original FTS (the "
                + "side-map gives us `t2` from the edge's target pair-state). One subtlety: "
                + "the FIRST pair-graph edge of a non-INIT-starting segment carries a real "
                + "pair `(t_Y, t_X)` that would otherwise be split across two test cases by "
                + "the synthetic teleport. To preserve coverage we PREPEND `t_Y` to the "
                + "segment (the test case then starts mid-FTS at `source(t_Y)` — legal as long "
                + "as the executor is reset between test cases).\n\n");
        md.write("**Step 7 — Wrap into a List<TestCase>.** Unlike all-transitions and "
                + "all-states (which return one TestCase), this generator returns a *suite* "
                + "because the pair graph naturally yields multiple test cases (one per "
                + "real segment between synthetic teleports). For display each TestCase is "
                + "additionally split at every visit to the original repaired FTS's initial "
                + "state via `TestCaseSplitter.splitAtInitialReturns(...)` — same operational "
                + "test-case semantics as the other two reports.\n\n");
        md.write("**Coverage claim.** Hierholzer visits every pair-graph edge exactly once → "
                + "every contiguous original transition pair is covered, by construction. "
                + "The pair-graph reachable-pair count is the denominator; pairs that exist "
                + "only via dropped transitions (post-projection) are NOT in the denominator, "
                + "which is the correct semantics for product-level coverage.\n\n");

        html.write("<h2>How the all-transition-pairs test suite is built</h2>\n");
        html.write("<p><strong>Why a fresh transformation, not <code>vibes-transformation</code>"
                + ".</strong> The legacy <code>vibes-transformation</code> module (not in the "
                + "root pom) is an I/O / format-conversion layer for the obsolete "
                + "<code>be.unamur.transitionsystem.*</code> namespace; it does not contain "
                + "anything for pair-graph / k-tuple coverage transformation. We re-implement "
                + "directly against the current <code>be.vibes.ts.*</code> types.</p>\n");
        html.write("<p><strong>Reduction:</strong> pair-coverage on FTS <em>F</em> &equiv; "
                + "edge-coverage on the pair graph <em>P(F)</em>. A Hierholzer cycle on "
                + "<em>P(F)</em> covers every original pair.</p>\n<ol>\n");
        html.write("<li><strong>Project</strong> via <code>FExpressionPreservingProjection.project</code>.</li>\n");
        html.write("<li><strong>Repair SCC</strong> via <code>InitialSccFilter.keepInitialScc</code>.</li>\n");
        html.write("<li><strong>Pair graph</strong> via <code>PairGraphTransformer.transform</code> — "
                + "nodes = original transitions plus INIT; edges = INIT&rarr;p(t) for initial-state "
                + "outgoings and p(t1)&rarr;p(t2) for every contiguous pair.</li>\n");
        html.write("<li><strong>Balance (no SCC precheck)</strong> via <code>EulerianBalancer.balanceWithoutPrecheck</code> — "
                + "supplies the missing INIT-incoming as synthetic <code>__balance__N</code> edges.</li>\n");
        html.write("<li><strong>Hierholzer Euler cycle</strong> on the balanced pair graph.</li>\n");
        html.write("<li><strong>Split + translate back.</strong> Cycle is split at synthetic edges; "
                + "each pair-graph edge <code>p(t1)&rarr;p(t2)</code> translates to executing "
                + "<code>t2</code> in the original FTS. The first edge of each non-INIT segment "
                + "gets <code>t_Y</code> prepended to preserve the otherwise-split pair.</li>\n");
        html.write("<li><strong>Wrap into List&lt;TestCase&gt;.</strong> One test case per real "
                + "segment; each further split at original-FTS initial returns for display.</li>\n");
        html.write("</ol>\n");
        html.write("<p><strong>Coverage claim:</strong> Hierholzer visits every pair-graph "
                + "edge exactly once &rarr; every contiguous original transition pair is "
                + "covered, by construction.</p>\n");
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
        PairGraphTransformer.Result pgResult = PairGraphTransformer.transform(repaired);
        FeaturedTransitionSystem pairGraph = pgResult.pairGraph;
        FeaturedTransitionSystem pairGraphBalanced;
        Exception balanceErr = null;
        try {
            pairGraphBalanced = EulerianBalancer.balanceWithoutPrecheck(pairGraph);
        } catch (Exception ex) {
            pairGraphBalanced = null;
            balanceErr = ex;
        }

        List<TestCase> suite;
        Exception genErr = null;
        try {
            suite = TransitionPairCoverageGenerator.generate(
                    fts, cfg, spec.name + "_p" + productIndex + "_pair");
        } catch (Exception ex) {
            suite = Collections.emptyList();
            genErr = ex;
        }

        // Render the three PNGs.
        Path png1 = renderStyled(repaired, outDir,
                spec.name + "-product" + productIndex + "-repaired", true);
        Path png2 = renderStyled(pairGraph, outDir,
                spec.name + "-product" + productIndex + "-pairgraph-raw", true);
        Path png3 = pairGraphBalanced != null
                ? renderStyled(pairGraphBalanced, outDir,
                        spec.name + "-product" + productIndex + "-pairgraph-balanced", true)
                : null;

        // Pair counts: edges in raw pair graph excluding the INIT-out
        // contribution give the original-pair count. Actually every edge in
        // the raw pair graph corresponds to either an INIT-out (one per
        // original initial-state transition) or a real pair (one per
        // contiguous pair in the original FTS). Both are denominator
        // contributions to coverage.
        int rawPairEdges = countTransitions(pairGraph);
        int balancedEdges = pairGraphBalanced == null ? 0 : countTransitions(pairGraphBalanced);
        int syntheticAdded = pairGraphBalanced == null ? 0
                : balancedEdges - rawPairEdges;
        int suiteSize = suite.size();
        int totalRealSteps = 0;
        for (TestCase tc : suite) totalRealSteps += countTestCaseLength(tc);

        md.write("\n### Product " + productIndex + "\n\n");
        md.write("**Selected features:** " + featuresLine + "\n\n");
        md.write("**Repaired FTS:** " + countStates(repaired) + " states, "
                + countTransitions(repaired) + " transitions ("
                + countRealTransitions(repaired) + " real / "
                + countEnd(repaired) + " `__end__`).\n\n");
        md.write("**Pair graph (raw):** " + countStates(pairGraph)
                + " nodes (incl. INIT), " + rawPairEdges + " edges (every edge = one "
                + "contiguous transition pair in the original FTS; the subset starting at "
                + "INIT correspond to pairs `(start, t)` for any original initial-state "
                + "outgoing `t`).\n\n");
        if (pairGraphBalanced != null) {
            md.write("**Pair graph (balanced):** " + countStates(pairGraphBalanced)
                    + " nodes, " + balancedEdges + " edges (" + syntheticAdded
                    + " synthetic `__balance__N` added to restore in-balance at INIT).\n\n");
        } else {
            md.write("**Pair graph (balanced):** could not balance: `" + balanceErr + "`.\n\n");
        }

        md.write("![Repaired FTS — product " + productIndex + "]("
                + png1.getFileName() + ")\n\n");
        md.write("![Pair graph (raw) — product " + productIndex + "]("
                + png2.getFileName() + ")\n\n");
        if (png3 != null) {
            md.write("![Pair graph (balanced) — product " + productIndex + "]("
                    + png3.getFileName() + ")\n\n");
        }

        if (suite != null && !suite.isEmpty()) {
            md.write("**Generated test suite** — " + suiteSize
                    + " test case(s) total (" + totalRealSteps
                    + " real step(s); pair-graph cycle has " + balancedEdges
                    + " edge(s) total, " + syntheticAdded
                    + " synthetic dropped at translation).\n\n");
            int caseIndex = 0;
            for (TestCase tc : suite) {
                List<List<Transition>> trips =
                        TestCaseSplitter.splitAtInitialReturns(tc, repaired.getInitialState());
                List<List<String>> renderedTrips = new ArrayList<>();
                for (List<Transition> trip : trips) {
                    List<String> actions = TestCaseSplitter.renderTripActions(trip);
                    List<String> normalized = new ArrayList<>(actions.size());
                    for (String a : actions) normalized.add(normalize(a));
                    if (!normalized.isEmpty()) renderedTrips.add(normalized);
                }
                if (renderedTrips.isEmpty()) {
                    continue;
                }
                if (renderedTrips.size() == 1) {
                    caseIndex++;
                    String seq = String.join(" -> ", renderedTrips.get(0));
                    md.write("- **test case " + caseIndex + "**: `" + seq + "`\n");
                } else {
                    for (List<String> t : renderedTrips) {
                        caseIndex++;
                        String seq = String.join(" -> ", t);
                        md.write("- **test case " + caseIndex + "**: `" + seq + "`\n");
                    }
                }
            }
            md.write("\n");
        } else {
            md.write("**Generated test suite:** could not generate: `" + genErr + "`.\n\n");
        }

        // HTML mirror
        html.write("<h3>Product " + productIndex + "</h3>\n");
        html.write("<p><strong>Selected features:</strong> "
                + escapeHtml(featuresLine) + "</p>\n");
        html.write("<p><strong>Repaired FTS:</strong> " + countStates(repaired) + " states, "
                + countTransitions(repaired) + " transitions ("
                + countRealTransitions(repaired) + " real / "
                + countEnd(repaired) + " <code>__end__</code>).</p>\n");
        html.write("<p><strong>Pair graph (raw):</strong> " + countStates(pairGraph)
                + " nodes (incl. INIT), " + rawPairEdges + " edges.</p>\n");
        if (pairGraphBalanced != null) {
            html.write("<p><strong>Pair graph (balanced):</strong> "
                    + countStates(pairGraphBalanced) + " nodes, "
                    + balancedEdges + " edges (" + syntheticAdded
                    + " synthetic <code>__balance__N</code> added).</p>\n");
        } else {
            html.write("<p><strong>Pair graph (balanced):</strong> could not balance: <code>"
                    + escapeHtml(String.valueOf(balanceErr)) + "</code></p>\n");
        }
        html.write("<img src=\"" + escapeHtml(png1.getFileName().toString())
                + "\" alt=\"Repaired FTS — product " + productIndex + "\"/>\n");
        html.write("<img src=\"" + escapeHtml(png2.getFileName().toString())
                + "\" alt=\"Pair graph (raw) — product " + productIndex + "\"/>\n");
        if (png3 != null) {
            html.write("<img src=\"" + escapeHtml(png3.getFileName().toString())
                    + "\" alt=\"Pair graph (balanced) — product " + productIndex + "\"/>\n");
        }
        if (suite != null && !suite.isEmpty()) {
            html.write("<p><strong>Generated test suite</strong> — " + suiteSize
                    + " test case(s) total (" + totalRealSteps + " real step(s); pair-graph "
                    + "cycle has " + balancedEdges + " edge(s) total, " + syntheticAdded
                    + " synthetic dropped at translation).</p>\n<ul>\n");
            int caseIndex = 0;
            for (TestCase tc : suite) {
                List<List<Transition>> trips =
                        TestCaseSplitter.splitAtInitialReturns(tc, repaired.getInitialState());
                List<List<String>> renderedTrips = new ArrayList<>();
                for (List<Transition> trip : trips) {
                    List<String> actions = TestCaseSplitter.renderTripActions(trip);
                    List<String> normalized = new ArrayList<>(actions.size());
                    for (String a : actions) normalized.add(normalize(a));
                    if (!normalized.isEmpty()) renderedTrips.add(normalized);
                }
                for (List<String> t : renderedTrips) {
                    caseIndex++;
                    List<String> escaped = new ArrayList<>(t.size());
                    for (String a : t) escaped.add(escapeHtml(a));
                    String seq = String.join(" &rarr; ", escaped);
                    html.write("<li><strong>test case " + caseIndex + "</strong>: <code>"
                            + seq + "</code></li>\n");
                }
            }
            html.write("</ul>\n");
        } else {
            html.write("<p><strong>Generated test suite:</strong> could not generate: <code>"
                    + escapeHtml(String.valueOf(genErr)) + "</code></p>\n");
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

    private static int countTestCaseLength(TestCase tc) {
        int n = 0;
        Iterator<Transition> it = tc.iterator();
        while (it.hasNext()) { it.next(); n++; }
        return n;
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private static FeaturedTransitionSystem loadFts(String r) throws Exception {
        URL u = PerProductAllTransitionPairsReportGenerator.class
                .getClassLoader().getResource(r);
        return new MxeToFtsConverter().convert(new File(u.toURI()));
    }

    private static Sat4JSolverFacade loadSolver(String d, String m) throws Exception {
        URL du = PerProductAllTransitionPairsReportGenerator.class
                .getClassLoader().getResource(d);
        URL mu = PerProductAllTransitionPairsReportGenerator.class
                .getClassLoader().getResource(m);
        return new Sat4JSolverFacade(DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mu.toURI()), new File(du.toURI())));
    }

    private static void writeHtmlHeader(BufferedWriter out, String name) throws IOException {
        out.write("<!DOCTYPE html>\n<html lang=\"en\"><head>\n<meta charset=\"UTF-8\"/>\n");
        out.write("<title>Per-Product All-Transition-Pairs — " + escapeHtml(name) + "</title>\n");
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
