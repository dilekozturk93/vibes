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
import be.vibes.testgeneration.mutation.FaultDetector;
import be.vibes.testgeneration.mutation.MutationOperator;
import be.vibes.testgeneration.mutation.StateMissing;
import be.vibes.testgeneration.mutation.TransitionMissing;
import be.vibes.testgeneration.coverage.baseline.AllStatesGenerator;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.fexpression.FExpression;
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

        // Family-level (SPL-level) all-states baseline per Devroey 2014.
        // Computed ONCE per SPL; per-product kill check filters each
        // family-level test case's transitions to those whose feature
        // expression is satisfied by the product configuration.
        // Reload the solver afterwards because AllStatesGenerator adds
        // and removes SAT constraints during walk validation.
        System.out.println("Running family-level baseline for " + spec.name + "...");
        Sat4JSolverFacade baselineSolver = loadSolver(spec.dimacs, spec.mapping);
        List<TestCase> familyBaseline = AllStatesGenerator.generateForFts(
                fts, baselineSolver, spec.name + "_family");
        System.out.println("  -> " + familyBaseline.size() + " family-level test case(s)");

        Path mdPath = outDir.resolve(spec.name + "-per-product-mutation-report.md");
        Path htmlPath = outDir.resolve(spec.name + "-per-product-mutation-report.html");

        // Aggregate stats across all products for the summary table.
        int totalTmReal = 0;
        int totalTmKilled_familyState = 0;
        int totalTmKilled_state = 0, totalTmKilled_trans = 0, totalTmKilled_pair = 0;
        int totalAexReal = 0;
        int totalAexKilled_familyState = 0;
        int totalAexKilled_state = 0, totalAexKilled_trans = 0, totalAexKilled_pair = 0;
        int totalSmReal = 0;
        int totalSmKilled_familyState = 0;
        int totalSmKilled_state = 0, totalSmKilled_trans = 0, totalSmKilled_pair = 0;
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
                ProductScores ps = writeProductSection(md, html, spec, fts, cfg,
                        productCount, ftsFeatures, familyBaseline);
                totalTmReal += ps.tmTotal;
                totalTmKilled_familyState += ps.tmKilledFamilyState;
                totalTmKilled_state += ps.tmKilledState;
                totalTmKilled_trans += ps.tmKilledTrans;
                totalTmKilled_pair += ps.tmKilledPair;
                totalAexReal += ps.aexTotal;
                totalAexKilled_familyState += ps.aexKilledFamilyState;
                totalAexKilled_state += ps.aexKilledState;
                totalAexKilled_trans += ps.aexKilledTrans;
                totalAexKilled_pair += ps.aexKilledPair;
                totalSmReal += ps.smTotal;
                totalSmKilled_familyState += ps.smKilledFamilyState;
                totalSmKilled_state += ps.smKilledState;
                totalSmKilled_trans += ps.smKilledTrans;
                totalSmKilled_pair += ps.smKilledPair;
            }

            md.write("---\n\n## " + spec.name + " summary (aggregate over " + productCount
                    + " products)\n\n");
            md.write("**Family-level baseline** (Devroey 2014, ported from VIBeS commit "
                    + "f856c90): " + familyBaseline.size() + " test case(s) generated once "
                    + "for the SPL, projected per-product via fexpr-filtering before "
                    + "kill-checking.\n\n");
            md.write("| Operator | Mutants | Family state-cov (Devroey) | Product state-cov | "
                    + "Product transition-cov | Product pair-cov |\n");
            md.write("|---|---|---|---|---|---|\n");
            md.write("| TransitionMissing | " + totalTmReal + " | "
                    + formatScore(totalTmKilled_familyState, totalTmReal) + " | "
                    + formatScore(totalTmKilled_state, totalTmReal) + " | "
                    + formatScore(totalTmKilled_trans, totalTmReal) + " | "
                    + formatScore(totalTmKilled_pair, totalTmReal) + " |\n");
            md.write("| ActionExchange | " + totalAexReal + " | "
                    + formatScore(totalAexKilled_familyState, totalAexReal) + " | "
                    + formatScore(totalAexKilled_state, totalAexReal) + " | "
                    + formatScore(totalAexKilled_trans, totalAexReal) + " | "
                    + formatScore(totalAexKilled_pair, totalAexReal) + " |\n");
            md.write("| StateMissing (dynamic) | " + totalSmReal + " | "
                    + formatScore(totalSmKilled_familyState, totalSmReal) + " | "
                    + formatScore(totalSmKilled_state, totalSmReal) + " | "
                    + formatScore(totalSmKilled_trans, totalSmReal) + " | "
                    + formatScore(totalSmKilled_pair, totalSmReal) + " |\n");
            md.write("\nTotal products: " + productCount + ".\n");

            html.write("<hr/>\n<h2>" + escapeHtml(spec.name)
                    + " summary (aggregate over " + productCount + " products)</h2>\n");
            html.write("<p><strong>Family-level baseline</strong> (Devroey 2014, ported from "
                    + "VIBeS commit f856c90): " + familyBaseline.size() + " test case(s) "
                    + "generated once for the SPL, projected per-product via fexpr-filtering "
                    + "before kill-checking.</p>\n");
            html.write("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">\n");
            html.write("<tr><th>Operator</th><th>Mutants</th>"
                    + "<th>Family state-cov (Devroey)</th>"
                    + "<th>Product state-cov</th>"
                    + "<th>Product transition-cov</th>"
                    + "<th>Product pair-cov</th></tr>\n");
            html.write("<tr><td>TransitionMissing</td><td>" + totalTmReal + "</td><td>"
                    + formatScore(totalTmKilled_familyState, totalTmReal) + "</td><td>"
                    + formatScore(totalTmKilled_state, totalTmReal) + "</td><td>"
                    + formatScore(totalTmKilled_trans, totalTmReal) + "</td><td>"
                    + formatScore(totalTmKilled_pair, totalTmReal) + "</td></tr>\n");
            html.write("<tr><td>ActionExchange</td><td>" + totalAexReal + "</td><td>"
                    + formatScore(totalAexKilled_familyState, totalAexReal) + "</td><td>"
                    + formatScore(totalAexKilled_state, totalAexReal) + "</td><td>"
                    + formatScore(totalAexKilled_trans, totalAexReal) + "</td><td>"
                    + formatScore(totalAexKilled_pair, totalAexReal) + "</td></tr>\n");
            html.write("<tr><td>StateMissing (dynamic)</td><td>" + totalSmReal + "</td><td>"
                    + formatScore(totalSmKilled_familyState, totalSmReal) + "</td><td>"
                    + formatScore(totalSmKilled_state, totalSmReal) + "</td><td>"
                    + formatScore(totalSmKilled_trans, totalSmReal) + "</td><td>"
                    + formatScore(totalSmKilled_pair, totalSmReal) + "</td></tr>\n");
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
                                                     Set<String> ftsFeatures,
                                                     List<TestCase> familyBaseline) throws Exception {
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
        StateMissing sm = new StateMissing();
        sm.generateMutants(repaired);

        Map<String, FeaturedTransitionSystem> tmMutants = filterRealMutants(tm.getMutants(), "TM");
        Map<String, FeaturedTransitionSystem> aexMutants = filterRealMutants(aex.getMutants(), "AEX");
        Map<String, FeaturedTransitionSystem> smMutants = filterRealMutants(sm.getMutants(), "SM");

        List<TestCase> stateSuite = Collections.singletonList(stateTc);
        List<TestCase> transSuite = Collections.singletonList(transTc);

        // Project the family-level baseline onto this product: keep family
        // test-case transitions whose SPL-level feature expression is
        // satisfied by the product configuration. This emulates "a tester
        // runs Devroey's SPL-level suite on this product — only the
        // applicable steps execute".
        List<TestCase> projectedFamily = projectFamilySuite(familyBaseline, fts, cfg);

        FaultDetector.KillResult tmFamilyState = FaultDetector.scoreSuite(projectedFamily, tmMutants);
        FaultDetector.KillResult tmState = FaultDetector.scoreSuite(stateSuite, tmMutants);
        FaultDetector.KillResult tmTrans = FaultDetector.scoreSuite(transSuite, tmMutants);
        FaultDetector.KillResult tmPair = FaultDetector.scoreSuite(pairSuite, tmMutants);
        FaultDetector.KillResult aexFamilyState = FaultDetector.scoreSuite(projectedFamily, aexMutants);
        FaultDetector.KillResult aexState = FaultDetector.scoreSuite(stateSuite, aexMutants);
        FaultDetector.KillResult aexTrans = FaultDetector.scoreSuite(transSuite, aexMutants);
        FaultDetector.KillResult aexPair = FaultDetector.scoreSuite(pairSuite, aexMutants);
        // StateMissing changes execution semantics — use dynamic replay.
        FaultDetector.KillResult smFamilyState = FaultDetector.scoreSuiteDynamic(projectedFamily, smMutants);
        FaultDetector.KillResult smState = FaultDetector.scoreSuiteDynamic(stateSuite, smMutants);
        FaultDetector.KillResult smTrans = FaultDetector.scoreSuiteDynamic(transSuite, smMutants);
        FaultDetector.KillResult smPair = FaultDetector.scoreSuiteDynamic(pairSuite, smMutants);

        md.write("\n### Product " + productIndex + "\n\n");
        md.write("**Selected features:** " + featuresLine + "\n\n");
        md.write("**Repaired FTS:** " + countStates(repaired) + " states, "
                + countTransitions(repaired) + " transitions ("
                + countRealTransitions(repaired) + " real / "
                + countEnd(repaired) + " `__end__`).\n\n");
        md.write("**Family baseline projected to this product:** "
                + projectedFamily.size() + " test case(s) (of "
                + familyBaseline.size() + " family-level), "
                + countTestSuiteTransitions(projectedFamily) + " real step(s) applicable.\n\n");
        md.write("| Operator | Real mutants | Family state-cov (Devroey) | "
                + "Product state-cov | Product transition-cov | Product pair-cov |\n");
        md.write("|---|---|---|---|---|---|\n");
        md.write("| TransitionMissing | " + tmMutants.size() + " | "
                + formatScore(tmFamilyState.getKilled(), tmMutants.size()) + " | "
                + formatScore(tmState.getKilled(), tmMutants.size()) + " | "
                + formatScore(tmTrans.getKilled(), tmMutants.size()) + " | "
                + formatScore(tmPair.getKilled(), tmMutants.size()) + " |\n");
        md.write("| ActionExchange | " + aexMutants.size() + " | "
                + formatScore(aexFamilyState.getKilled(), aexMutants.size()) + " | "
                + formatScore(aexState.getKilled(), aexMutants.size()) + " | "
                + formatScore(aexTrans.getKilled(), aexMutants.size()) + " | "
                + formatScore(aexPair.getKilled(), aexMutants.size()) + " |\n");
        md.write("| StateMissing (dynamic) | " + smMutants.size() + " | "
                + formatScore(smFamilyState.getKilled(), smMutants.size()) + " | "
                + formatScore(smState.getKilled(), smMutants.size()) + " | "
                + formatScore(smTrans.getKilled(), smMutants.size()) + " | "
                + formatScore(smPair.getKilled(), smMutants.size()) + " |\n");

        // Surviving mutants (escaped detection by any criterion) — these are
        // the interesting ones for paper analysis.
        appendSurvivors(md, "TransitionMissing — survived family-level state coverage (Devroey)", tmFamilyState.getSurvivors());
        appendSurvivors(md, "TransitionMissing — survived product state coverage", tmState.getSurvivors());
        appendSurvivors(md, "TransitionMissing — survived product transition coverage", tmTrans.getSurvivors());
        appendSurvivors(md, "TransitionMissing — survived product pair coverage", tmPair.getSurvivors());
        appendSurvivors(md, "ActionExchange — survived family-level state coverage (Devroey)", aexFamilyState.getSurvivors());
        appendSurvivors(md, "ActionExchange — survived product state coverage", aexState.getSurvivors());
        appendSurvivors(md, "ActionExchange — survived product transition coverage", aexTrans.getSurvivors());
        appendSurvivors(md, "ActionExchange — survived product pair coverage", aexPair.getSurvivors());

        // HTML
        html.write("<h3>Product " + productIndex + "</h3>\n");
        html.write("<p><strong>Selected features:</strong> "
                + escapeHtml(featuresLine) + "</p>\n");
        html.write("<p><strong>Repaired FTS:</strong> " + countStates(repaired) + " states, "
                + countTransitions(repaired) + " transitions ("
                + countRealTransitions(repaired) + " real / "
                + countEnd(repaired) + " <code>__end__</code>).</p>\n");
        html.write("<p><strong>Family baseline projected to this product:</strong> "
                + projectedFamily.size() + " test case(s) (of "
                + familyBaseline.size() + " family-level), "
                + countTestSuiteTransitions(projectedFamily) + " real step(s) applicable.</p>\n");
        html.write("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">\n");
        html.write("<tr><th>Operator</th><th>Real mutants</th>"
                + "<th>Family state-cov (Devroey)</th>"
                + "<th>Product state-cov</th>"
                + "<th>Product transition-cov</th>"
                + "<th>Product pair-cov</th></tr>\n");
        html.write("<tr><td>TransitionMissing</td><td>" + tmMutants.size() + "</td><td>"
                + formatScore(tmFamilyState.getKilled(), tmMutants.size()) + "</td><td>"
                + formatScore(tmState.getKilled(), tmMutants.size()) + "</td><td>"
                + formatScore(tmTrans.getKilled(), tmMutants.size()) + "</td><td>"
                + formatScore(tmPair.getKilled(), tmMutants.size()) + "</td></tr>\n");
        html.write("<tr><td>ActionExchange</td><td>" + aexMutants.size() + "</td><td>"
                + formatScore(aexFamilyState.getKilled(), aexMutants.size()) + "</td><td>"
                + formatScore(aexState.getKilled(), aexMutants.size()) + "</td><td>"
                + formatScore(aexTrans.getKilled(), aexMutants.size()) + "</td><td>"
                + formatScore(aexPair.getKilled(), aexMutants.size()) + "</td></tr>\n");
        html.write("<tr><td>StateMissing (dynamic)</td><td>" + smMutants.size() + "</td><td>"
                + formatScore(smFamilyState.getKilled(), smMutants.size()) + "</td><td>"
                + formatScore(smState.getKilled(), smMutants.size()) + "</td><td>"
                + formatScore(smTrans.getKilled(), smMutants.size()) + "</td><td>"
                + formatScore(smPair.getKilled(), smMutants.size()) + "</td></tr>\n");
        html.write("</table>\n");
        appendSurvivorsHtml(html, "TransitionMissing — survived family-level state coverage (Devroey)", tmFamilyState.getSurvivors());
        appendSurvivorsHtml(html, "TransitionMissing — survived product state coverage", tmState.getSurvivors());
        appendSurvivorsHtml(html, "TransitionMissing — survived product transition coverage", tmTrans.getSurvivors());
        appendSurvivorsHtml(html, "TransitionMissing — survived product pair coverage", tmPair.getSurvivors());
        appendSurvivorsHtml(html, "ActionExchange — survived family-level state coverage (Devroey)", aexFamilyState.getSurvivors());
        appendSurvivorsHtml(html, "ActionExchange — survived product state coverage", aexState.getSurvivors());
        appendSurvivorsHtml(html, "ActionExchange — survived product transition coverage", aexTrans.getSurvivors());
        appendSurvivorsHtml(html, "ActionExchange — survived product pair coverage", aexPair.getSurvivors());

        ProductScores ps = new ProductScores();
        ps.tmTotal = tmMutants.size();
        ps.tmKilledFamilyState = tmFamilyState.getKilled();
        ps.tmKilledState = tmState.getKilled();
        ps.tmKilledTrans = tmTrans.getKilled();
        ps.tmKilledPair = tmPair.getKilled();
        ps.aexTotal = aexMutants.size();
        ps.aexKilledFamilyState = aexFamilyState.getKilled();
        ps.aexKilledState = aexState.getKilled();
        ps.aexKilledTrans = aexTrans.getKilled();
        ps.aexKilledPair = aexPair.getKilled();
        ps.smTotal = smMutants.size();
        ps.smKilledFamilyState = smFamilyState.getKilled();
        ps.smKilledState = smState.getKilled();
        ps.smKilledTrans = smTrans.getKilled();
        ps.smKilledPair = smPair.getKilled();
        return ps;
    }

    /**
     * STRICT projection of a family-level test suite onto a product
     * configuration: stop at the first transition whose feature
     * expression is NOT satisfied by the configuration, keep only the
     * prefix up to (not including) that drop, discard the entire suffix.
     *
     * <p>Semantic: "a tester runs Devroey's SPL-level suite on this
     * specific product — execution halts at the first refused step;
     * subsequent steps would not run regardless of their feasibility
     * because the SUT is in an undefined state after a failed transition".
     *
     * <p>This is the realistic family-vs-product comparison for RQ2.
     * The earlier "lenient" variant (split-and-keep-suffix) over-credited
     * the family-level baseline by salvaging post-drop suffixes that
     * could not have actually executed without a fresh test-case reset
     * the family-level pipeline does not provide.
     */
    private static List<TestCase> projectFamilySuite(List<TestCase> familySuite,
                                                     FeaturedTransitionSystem fts,
                                                     be.vibes.fexpression.configuration.Configuration cfg) {
        List<TestCase> projected = new java.util.ArrayList<>(familySuite.size());
        for (TestCase familyTc : familySuite) {
            TestCase projectedTc = new TestCase(familyTc.getId() + "_proj");
            int kept = 0;
            try {
                for (Transition t : familyTc) {
                    FExpression fexpr = fts.getFExpression(t);
                    if (fexpr == null || fexpr.assign(cfg).applySimplification().isTrue()) {
                        projectedTc.enqueue(t);
                        kept++;
                    } else {
                        // STRICT: stop at first drop. Discard the suffix.
                        break;
                    }
                }
            } catch (be.vibes.ts.exception.TransitionSystenExecutionException e) {
                // Contiguity violation while replaying — also a halt.
            }
            if (kept > 0) {
                projected.add(projectedTc);
            }
        }
        return projected;
    }

    private static int countTestSuiteTransitions(List<TestCase> suite) {
        int n = 0;
        for (TestCase tc : suite) {
            Iterator<Transition> it = tc.iterator();
            while (it.hasNext()) { it.next(); n++; }
        }
        return n;
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

    private static String formatScore(int killed, int total) {
        if (total == 0) {
            return "n/a";
        }
        return killed + "/" + total + " = "
                + String.format("%.1f", 100.0 * killed / total) + "%";
    }

    private static final class ProductScores {
        int tmTotal, tmKilledFamilyState, tmKilledState, tmKilledTrans, tmKilledPair;
        int aexTotal, aexKilledFamilyState, aexKilledState, aexKilledTrans, aexKilledPair;
        int smTotal, smKilledFamilyState, smKilledState, smKilledTrans, smKilledPair;
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
