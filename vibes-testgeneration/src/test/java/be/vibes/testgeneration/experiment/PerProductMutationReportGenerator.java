package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.FExpression;
import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.coverage.StateCoverageGenerator;
import be.vibes.testgeneration.coverage.TransitionCoverageGenerator;
import be.vibes.testgeneration.coverage.TransitionPairCoverageGenerator;
import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.mutation.ActionExchange;
import be.vibes.testgeneration.mutation.MutationOperator;
import be.vibes.testgeneration.mutation.TransitionMissing;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Per-product mutation testing report. For each product configuration of
 * each MVP SPL it generates state / transition / pair test suites, applies
 * the {@link TransitionMissing} and {@link ActionExchange} mutation
 * operators to the repaired FTS, and reports per-criterion mutation
 * scores (killed / total) plus the list of surviving mutants per
 * criterion. Output:
 * {@code milestone-reports/per-product-mutation/<SPL>/}.
 *
 * <p>Synthetic mutants — i.e. mutants whose mutation site is on a
 * synthetic ({@code __end__}) transition — are filtered out before
 * scoring. Mutating an {@code __end__} edge produces a model that
 * differs from the original only at the synthetic boundary, which has
 * no SUT-level meaning, so counting it as a kill or escape would
 * distort the score.
 *
 * <p>Run:
 * <pre>
 *   mvn -pl vibes-testgeneration exec:java \
 *       -Dexec.mainClass=be.vibes.testgeneration.experiment.PerProductMutationReportGenerator \
 *       -Dexec.classpathScope=test
 * </pre>
 */
public final class PerProductMutationReportGenerator {

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

    private PerProductMutationReportGenerator() {
    }

    public static void main(String[] args) throws Exception {
        for (SplSpec spec : SPLS) run(spec);
    }

    private static void run(SplSpec spec) throws Exception {
        Path outDir = Paths.get("milestone-reports/per-product-mutation/" + spec.name);
        Files.createDirectories(outDir);

        FeaturedTransitionSystem fts = loadFts(spec.mxe);
        Sat4JSolverFacade solver = loadSolver(spec.dimacs, spec.mapping);
        Set<String> ftsFeatures = collectFeatureNames(fts);

        Path mdPath = outDir.resolve(spec.name + "-per-product-mutation-report.md");
        Path htmlPath = outDir.resolve(spec.name + "-per-product-mutation-report.html");

        // Aggregate stats across all products for the summary table.
        int totalTmReal = 0, totalTmKilled_state = 0, totalTmKilled_trans = 0, totalTmKilled_pair = 0;
        int totalAexReal = 0, totalAexKilled_state = 0, totalAexKilled_trans = 0, totalAexKilled_pair = 0;
        int productCount = 0;

        try (BufferedWriter md = new BufferedWriter(new FileWriter(mdPath.toFile()));
             BufferedWriter html = new BufferedWriter(new FileWriter(htmlPath.toFile()))) {

            writeHtmlHeader(html, spec.name);
            String title = "Per-Product Mutation Report — " + spec.name;
            md.write("# " + title + "\n\n");
            html.write("<h1>" + escapeHtml(title) + "</h1>\n");

            writeAlgorithmSection(md, html);

            md.write("---\n\n## Products\n\n");
            html.write("<hr/>\n<h2>Products</h2>\n");

            Iterator<Configuration> configs = solver.getSolutions();
            while (configs.hasNext()) {
                Configuration cfg = configs.next();
                productCount++;
                ProductScores ps = writeProductSection(md, html, spec, fts, cfg, productCount, ftsFeatures);
                totalTmReal += ps.tmTotal;
                totalTmKilled_state += ps.tmKilledState;
                totalTmKilled_trans += ps.tmKilledTrans;
                totalTmKilled_pair += ps.tmKilledPair;
                totalAexReal += ps.aexTotal;
                totalAexKilled_state += ps.aexKilledState;
                totalAexKilled_trans += ps.aexKilledTrans;
                totalAexKilled_pair += ps.aexKilledPair;
            }

            md.write("---\n\n## " + spec.name + " summary (aggregate over " + productCount
                    + " products)\n\n");
            md.write("| Operator | Mutants | State-cov kills | Transition-cov kills | Pair-cov kills |\n");
            md.write("|---|---|---|---|---|\n");
            md.write("| TransitionMissing | " + totalTmReal + " | "
                    + formatScore(totalTmKilled_state, totalTmReal) + " | "
                    + formatScore(totalTmKilled_trans, totalTmReal) + " | "
                    + formatScore(totalTmKilled_pair, totalTmReal) + " |\n");
            md.write("| ActionExchange | " + totalAexReal + " | "
                    + formatScore(totalAexKilled_state, totalAexReal) + " | "
                    + formatScore(totalAexKilled_trans, totalAexReal) + " | "
                    + formatScore(totalAexKilled_pair, totalAexReal) + " |\n");
            md.write("\nTotal products: " + productCount + ".\n");

            html.write("<hr/>\n<h2>" + escapeHtml(spec.name)
                    + " summary (aggregate over " + productCount + " products)</h2>\n");
            html.write("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">\n");
            html.write("<tr><th>Operator</th><th>Mutants</th><th>State-cov kills</th>"
                    + "<th>Transition-cov kills</th><th>Pair-cov kills</th></tr>\n");
            html.write("<tr><td>TransitionMissing</td><td>" + totalTmReal + "</td><td>"
                    + formatScore(totalTmKilled_state, totalTmReal) + "</td><td>"
                    + formatScore(totalTmKilled_trans, totalTmReal) + "</td><td>"
                    + formatScore(totalTmKilled_pair, totalTmReal) + "</td></tr>\n");
            html.write("<tr><td>ActionExchange</td><td>" + totalAexReal + "</td><td>"
                    + formatScore(totalAexKilled_state, totalAexReal) + "</td><td>"
                    + formatScore(totalAexKilled_trans, totalAexReal) + "</td><td>"
                    + formatScore(totalAexKilled_pair, totalAexReal) + "</td></tr>\n");
            html.write("</table>\n");
            html.write("<p>Total products: " + productCount + ".</p>\n");
            writeHtmlFooter(html);
        }
        System.out.println(spec.name + " per-product mutation report -> " + mdPath);
    }

    private static void writeAlgorithmSection(BufferedWriter md, BufferedWriter html)
            throws IOException {
        md.write("## How mutation scores are computed\n\n");
        md.write("**Why a fresh mutation module, not `vibes-mutation`.** "
                + "The legacy `vibes-mutation` module (commented out in the root pom) is on "
                + "the old `be.unamur.transitionsystem.*` + `be.unamur.fts.fexpression.*` "
                + "namespaces and transitively depends on `vibes-transformation` (also "
                + "dormant) plus a non-existent `vibes-execution` module. Re-vivifying all "
                + "three for just the two operators referenced in the ICTSS abstract "
                + "(TransitionMissing, ActionExchange) would have been disproportionate; we "
                + "re-implement against the current `be.vibes.ts.*` types in "
                + "`vibes-testgeneration/.../mutation/`.\n\n");
        md.write("**Pipeline per product:**\n\n");
        md.write("1. **Project + repair.** Same as the coverage reports — "
                + "`FExpressionPreservingProjection.project` followed by "
                + "`InitialSccFilter.keepInitialScc` gives the product-level repaired FTS "
                + "(the system under test for this product).\n");
        md.write("2. **Generate test suites.** Three independent generators produce one "
                + "suite per criterion: `StateCoverageGenerator.generate` (one TestCase, "
                + "greedy + BFS reroute), `TransitionCoverageGenerator.generate` (one "
                + "TestCase, Chinese-Postman + Hierholzer Euler cycle), and "
                + "`TransitionPairCoverageGenerator.generate` (suite of TestCases via "
                + "pair-graph Hierholzer, deduped by action sequence).\n");
        md.write("3. **Generate mutants.** "
                + "[`TransitionMissing`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/"
                + "TransitionMissing.java) emits one mutant per transition (the transition is "
                + "removed). [`ActionExchange`]"
                + "(../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/"
                + "ActionExchange.java) emits one mutant per (transition, alternative-action) "
                + "pair (the transition's action label is swapped to the alternative).\n");
        md.write("4. **Filter synthetic mutants.** A mutant whose mutation site is on a "
                + "synthetic transition (`__end__`) is dropped from the denominator. The SUT "
                + "doesn't have such a transition; whether a test suite happens to 'kill' "
                + "such a mutant is not a meaningful signal about real fault detection.\n");
        md.write("5. **Replay test suite on each mutant.** A TestCase **kills** a mutant iff "
                + "at least one of its non-synthetic transitions `(source, action, target)` "
                + "is NOT present in the mutant. For TransitionMissing this happens whenever "
                + "the suite traverses the removed transition; for ActionExchange whenever "
                + "the suite traverses the mutated transition (the original `(s, a_orig, t)` "
                + "triple is gone — replaced by `(s, a_new, t)`).\n");
        md.write("6. **Mutation score per criterion** = killed mutants / total real mutants. "
                + "Higher is better. The central RQ2 claim is that the score monotonically "
                + "increases with the coverage criterion's strictness (state &le; transition "
                + "&le; transition-pair).\n\n");
        md.write("**Note on kill semantics.** Strict definition: the test suite kills the "
                + "mutant iff running the suite on the mutant produces a different observable "
                + "behaviour from running it on the original (e.g. a transition refused mid-"
                + "execution, or an extra transition fired). For both operators, this is "
                + "equivalent to the cheaper static check used here — \"some real transition "
                + "in the test case is not in the mutant\" — because both operators only "
                + "modify the FTS's transition set, not its execution semantics.\n\n");

        html.write("<h2>How mutation scores are computed</h2>\n");
        html.write("<p><strong>Why a fresh mutation module, not <code>vibes-mutation</code>"
                + ".</strong> The legacy module is on the obsolete "
                + "<code>be.unamur.transitionsystem.*</code> namespace, commented out in the "
                + "root pom, and depends on the dormant <code>vibes-transformation</code> "
                + "plus a non-existent <code>vibes-execution</code>. We re-implement the two "
                + "operators referenced in the ICTSS abstract against the current "
                + "<code>be.vibes.ts.*</code> types.</p>\n<ol>\n");
        html.write("<li><strong>Project + repair</strong> via "
                + "<code>FExpressionPreservingProjection.project</code> + "
                + "<code>InitialSccFilter.keepInitialScc</code>.</li>\n");
        html.write("<li><strong>Generate test suites</strong> via the three coverage "
                + "generators (state, transition, pair).</li>\n");
        html.write("<li><strong>Generate mutants</strong> via "
                + "<code>TransitionMissing</code> (one per transition, removed) and "
                + "<code>ActionExchange</code> (one per (transition, alternative-action) "
                + "pair, action swapped).</li>\n");
        html.write("<li><strong>Filter synthetic mutants</strong> — drop mutants whose "
                + "mutation site is on an <code>__end__</code> transition (no SUT meaning).</li>\n");
        html.write("<li><strong>Replay each suite on each mutant</strong> — a TestCase kills "
                + "a mutant iff at least one of its non-synthetic transitions "
                + "<code>(source, action, target)</code> is absent in the mutant.</li>\n");
        html.write("<li><strong>Mutation score</strong> = killed / total. RQ2 claim: score "
                + "increases with the coverage criterion's strictness (state &le; transition "
                + "&le; pair).</li>\n");
        html.write("</ol>\n");
    }

    private static ProductScores writeProductSection(BufferedWriter md, BufferedWriter html,
                                                     SplSpec spec, FeaturedTransitionSystem fts,
                                                     Configuration cfg, int productIndex,
                                                     Set<String> ftsFeatures) throws Exception {
        String featuresLine = formatFeatures(cfg, ftsFeatures);
        FeaturedTransitionSystem projected = FExpressionPreservingProjection.project(fts, cfg);
        FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);

        TestCase stateTc = StateCoverageGenerator.generate(
                fts, cfg, spec.name + "_p" + productIndex + "_state");
        TestCase transTc = TransitionCoverageGenerator.generate(
                fts, cfg, spec.name + "_p" + productIndex + "_trans");
        List<TestCase> pairSuite = TransitionPairCoverageGenerator.generate(
                fts, cfg, spec.name + "_p" + productIndex + "_pair");

        TransitionMissing tm = new TransitionMissing();
        tm.generateMutants(repaired);
        ActionExchange aex = new ActionExchange();
        aex.generateMutants(repaired);

        Map<String, FeaturedTransitionSystem> tmMutants = filterRealMutants(tm.getMutants(), "TM");
        Map<String, FeaturedTransitionSystem> aexMutants = filterRealMutants(aex.getMutants(), "AEX");

        List<TestCase> stateSuite = Collections.singletonList(stateTc);
        List<TestCase> transSuite = Collections.singletonList(transTc);

        KillResult tmState = scoreSuite(tmMutants, stateSuite);
        KillResult tmTrans = scoreSuite(tmMutants, transSuite);
        KillResult tmPair = scoreSuite(tmMutants, pairSuite);
        KillResult aexState = scoreSuite(aexMutants, stateSuite);
        KillResult aexTrans = scoreSuite(aexMutants, transSuite);
        KillResult aexPair = scoreSuite(aexMutants, pairSuite);

        md.write("\n### Product " + productIndex + "\n\n");
        md.write("**Selected features:** " + featuresLine + "\n\n");
        md.write("**Repaired FTS:** " + countStates(repaired) + " states, "
                + countTransitions(repaired) + " transitions ("
                + countRealTransitions(repaired) + " real / "
                + countEnd(repaired) + " `__end__`).\n\n");
        md.write("| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |\n");
        md.write("|---|---|---|---|---|\n");
        md.write("| TransitionMissing | " + tmMutants.size() + " | "
                + formatScore(tmState.killed, tmMutants.size()) + " | "
                + formatScore(tmTrans.killed, tmMutants.size()) + " | "
                + formatScore(tmPair.killed, tmMutants.size()) + " |\n");
        md.write("| ActionExchange | " + aexMutants.size() + " | "
                + formatScore(aexState.killed, aexMutants.size()) + " | "
                + formatScore(aexTrans.killed, aexMutants.size()) + " | "
                + formatScore(aexPair.killed, aexMutants.size()) + " |\n");

        // Surviving mutants (escaped detection by any criterion) — these are
        // the interesting ones for paper analysis.
        appendSurvivors(md, "TransitionMissing — survived state coverage", tmState.survivors);
        appendSurvivors(md, "TransitionMissing — survived transition coverage", tmTrans.survivors);
        appendSurvivors(md, "TransitionMissing — survived pair coverage", tmPair.survivors);
        appendSurvivors(md, "ActionExchange — survived state coverage", aexState.survivors);
        appendSurvivors(md, "ActionExchange — survived transition coverage", aexTrans.survivors);
        appendSurvivors(md, "ActionExchange — survived pair coverage", aexPair.survivors);

        // HTML
        html.write("<h3>Product " + productIndex + "</h3>\n");
        html.write("<p><strong>Selected features:</strong> "
                + escapeHtml(featuresLine) + "</p>\n");
        html.write("<p><strong>Repaired FTS:</strong> " + countStates(repaired) + " states, "
                + countTransitions(repaired) + " transitions ("
                + countRealTransitions(repaired) + " real / "
                + countEnd(repaired) + " <code>__end__</code>).</p>\n");
        html.write("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">\n");
        html.write("<tr><th>Operator</th><th>Real mutants</th>"
                + "<th>State-cov</th><th>Transition-cov</th><th>Pair-cov</th></tr>\n");
        html.write("<tr><td>TransitionMissing</td><td>" + tmMutants.size() + "</td><td>"
                + formatScore(tmState.killed, tmMutants.size()) + "</td><td>"
                + formatScore(tmTrans.killed, tmMutants.size()) + "</td><td>"
                + formatScore(tmPair.killed, tmMutants.size()) + "</td></tr>\n");
        html.write("<tr><td>ActionExchange</td><td>" + aexMutants.size() + "</td><td>"
                + formatScore(aexState.killed, aexMutants.size()) + "</td><td>"
                + formatScore(aexTrans.killed, aexMutants.size()) + "</td><td>"
                + formatScore(aexPair.killed, aexMutants.size()) + "</td></tr>\n");
        html.write("</table>\n");
        appendSurvivorsHtml(html, "TransitionMissing — survived state coverage", tmState.survivors);
        appendSurvivorsHtml(html, "TransitionMissing — survived transition coverage", tmTrans.survivors);
        appendSurvivorsHtml(html, "TransitionMissing — survived pair coverage", tmPair.survivors);
        appendSurvivorsHtml(html, "ActionExchange — survived state coverage", aexState.survivors);
        appendSurvivorsHtml(html, "ActionExchange — survived transition coverage", aexTrans.survivors);
        appendSurvivorsHtml(html, "ActionExchange — survived pair coverage", aexPair.survivors);

        ProductScores ps = new ProductScores();
        ps.tmTotal = tmMutants.size();
        ps.tmKilledState = tmState.killed;
        ps.tmKilledTrans = tmTrans.killed;
        ps.tmKilledPair = tmPair.killed;
        ps.aexTotal = aexMutants.size();
        ps.aexKilledState = aexState.killed;
        ps.aexKilledTrans = aexTrans.killed;
        ps.aexKilledPair = aexPair.killed;
        return ps;
    }

    private static void appendSurvivors(BufferedWriter md, String label, List<String> survivors)
            throws IOException {
        if (survivors.isEmpty()) {
            return;
        }
        md.write("\n**" + label + "** (" + survivors.size() + "):\n\n");
        for (String s : survivors) {
            md.write("- `" + s + "`\n");
        }
    }

    private static void appendSurvivorsHtml(BufferedWriter html, String label, List<String> survivors)
            throws IOException {
        if (survivors.isEmpty()) {
            return;
        }
        html.write("<p><strong>" + escapeHtml(label) + "</strong> ("
                + survivors.size() + "):</p>\n<ul>\n");
        for (String s : survivors) {
            html.write("<li><code>" + escapeHtml(s) + "</code></li>\n");
        }
        html.write("</ul>\n");
    }

    /**
     * A mutant is REAL iff its mutation key encodes a site whose action is
     * non-synthetic. For TransitionMissing the key is
     * {@code TM_<src>__<action>__<tgt>}; the action portion is checked.
     * For ActionExchange the key is
     * {@code AEX_<src>__<origAction>__<newAction>__<tgt>}; both actions are
     * checked.
     */
    private static Map<String, FeaturedTransitionSystem> filterRealMutants(
            Map<String, FeaturedTransitionSystem> raw, String operatorPrefix) {
        Map<String, FeaturedTransitionSystem> out = new LinkedHashMap<>();
        for (Map.Entry<String, FeaturedTransitionSystem> e : raw.entrySet()) {
            if (!isSyntheticMutantKey(e.getKey(), operatorPrefix)) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    private static boolean isSyntheticMutantKey(String key, String operatorPrefix) {
        // Format: TM_<src>__<action>__<tgt> or AEX_<src>__<a1>__<a2>__<tgt>.
        // The action(s) are between the __'s. Cheapest correct check: scan
        // for any synthetic substring in the key.
        return key.contains("__end__")
                || key.contains(EulerianBalancer.SYNTHETIC_ACTION_PREFIX)
                || key.contains(EulerianBalancer.DUPLICATE_ACTION_INFIX);
    }

    /**
     * For each mutant, replays every TestCase in the suite. The mutant is
     * killed iff at least one TestCase's non-synthetic transitions are not
     * all present in the mutant (i.e. some {@code (source, action, target)}
     * triple of the suite is absent from the mutant). Returns the
     * {@code killed} count and the list of survivor keys.
     */
    private static KillResult scoreSuite(Map<String, FeaturedTransitionSystem> mutants,
                                         List<TestCase> suite) {
        // Pre-compute the set of (source, action, target) triples exercised
        // by the suite. This makes the per-mutant check O(1) per triple in
        // the mutant's transition set, rather than O(|suite|) for each
        // mutant transition.
        Set<String> suiteTriples = new HashSet<>();
        for (TestCase tc : suite) {
            for (Transition t : tc) {
                if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                    continue;
                }
                suiteTriples.add(tripleKey(t.getSource().getName(),
                        t.getAction().getName(), t.getTarget().getName()));
            }
        }

        int killed = 0;
        List<String> survivors = new ArrayList<>();
        for (Map.Entry<String, FeaturedTransitionSystem> e : mutants.entrySet()) {
            if (isKilled(e.getValue(), suiteTriples)) {
                killed++;
            } else {
                survivors.add(e.getKey());
            }
        }
        return new KillResult(killed, survivors);
    }

    /**
     * Returns true iff some triple exercised by the suite is NOT a transition
     * of the mutant. Equivalent to "the suite's execution on the mutant would
     * fail (refused transition)" for both TransitionMissing and ActionExchange.
     */
    private static boolean isKilled(FeaturedTransitionSystem mutant, Set<String> suiteTriples) {
        Set<String> mutantTriples = new HashSet<>();
        Iterator<Transition> it = mutant.transitions();
        while (it.hasNext()) {
            Transition t = it.next();
            mutantTriples.add(tripleKey(t.getSource().getName(),
                    t.getAction().getName(), t.getTarget().getName()));
        }
        for (String triple : suiteTriples) {
            if (!mutantTriples.contains(triple)) {
                return true;
            }
        }
        return false;
    }

    private static String tripleKey(String src, String action, String tgt) {
        return src + "|" + action + "|" + tgt;
    }

    private static String formatScore(int killed, int total) {
        if (total == 0) {
            return "n/a";
        }
        return killed + "/" + total + " = "
                + String.format("%.1f", 100.0 * killed / total) + "%";
    }

    private static final class ProductScores {
        int tmTotal, tmKilledState, tmKilledTrans, tmKilledPair;
        int aexTotal, aexKilledState, aexKilledTrans, aexKilledPair;
    }

    private static final class KillResult {
        final int killed;
        final List<String> survivors;
        KillResult(int killed, List<String> survivors) {
            this.killed = killed;
            this.survivors = survivors;
        }
    }

    // ---------- Helpers (shared with other generators) ----------

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

    private static FeaturedTransitionSystem loadFts(String r) throws Exception {
        URL u = PerProductMutationReportGenerator.class.getClassLoader().getResource(r);
        return new MxeToFtsConverter().convert(new File(u.toURI()));
    }

    private static Sat4JSolverFacade loadSolver(String d, String m) throws Exception {
        URL du = PerProductMutationReportGenerator.class.getClassLoader().getResource(d);
        URL mu = PerProductMutationReportGenerator.class.getClassLoader().getResource(m);
        return new Sat4JSolverFacade(DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mu.toURI()), new File(du.toURI())));
    }

    private static void writeHtmlHeader(BufferedWriter out, String name) throws IOException {
        out.write("<!DOCTYPE html>\n<html lang=\"en\"><head>\n<meta charset=\"UTF-8\"/>\n");
        out.write("<title>Per-Product Mutation — " + escapeHtml(name) + "</title>\n");
        out.write("<style>\nbody { font-family: -apple-system, BlinkMacSystemFont, sans-serif; "
                + "max-width: 1080px; margin: 2em auto; padding: 0 1em; line-height: 1.55; color: #222; }\n"
                + "h1 { border-bottom: 2px solid #333; padding-bottom: 0.3em; }\n"
                + "h2 { margin-top: 2em; color: #444; border-bottom: 1px solid #ddd; padding-bottom: 0.2em; }\n"
                + "h3 { margin-top: 1.6em; color: #555; }\n"
                + "code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; "
                + "font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 0.95em; }\n"
                + "table { border-collapse: collapse; margin: 1em 0; }\n"
                + "th { background: #eee; text-align: left; }\n"
                + "td { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 0.93em; }\n"
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
