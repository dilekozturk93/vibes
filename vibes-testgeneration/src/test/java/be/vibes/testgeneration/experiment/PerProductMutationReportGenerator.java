package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.FExpression;
import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.coverage.RandomBaselineGenerator;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
            new SplSpec("BankAccountv2",
                    "cases/BankAccountv2/BAv2_ESGFx.mxe",
                    "cases/BankAccountv2/configs/BAv2.dimacs",
                    "cases/BankAccountv2/configs/BAv2_dimacsmapping.txt"),
            new SplSpec("StudentAttendanceSystem",
                    "cases/StudentAttendanceSystem/SAS_ESGFx.mxe",
                    "cases/StudentAttendanceSystem/configs/SAS.dimacs",
                    "cases/StudentAttendanceSystem/configs/SAS_dimacsmapping.txt"),
    };

    private PerProductMutationReportGenerator() {
    }

    public static void main(String[] args) throws Exception {
        // Allow CLI selection of a subset of SPLs by name; falls back to the
        // full SPLS list when no args are supplied. Example:
        //   mvn ... -Dexec.args="StudentAttendanceSystem"
        if (args.length == 0) {
            for (SplSpec spec : SPLS) run(spec);
            return;
        }
        for (String wanted : args) {
            SplSpec match = null;
            for (SplSpec spec : SPLS) {
                if (spec.name.equals(wanted)) {
                    match = spec;
                    break;
                }
            }
            if (match == null) {
                throw new IllegalArgumentException("Unknown SPL: " + wanted
                        + ". Known: " + java.util.Arrays.stream(SPLS)
                        .map(s -> s.name).reduce((a, b) -> a + ", " + b).orElse(""));
            }
            run(match);
        }
    }

    /**
     * Environment-overridable RQ2 random-baseline seed count. Default 100
     * (paper-fair). Smoke tests can override with e.g.
     * {@code RANDOM_SEED_COUNT=5} so a 5-SPL dry run finishes in seconds.
     */
    private static final int RANDOM_SEED_COUNT =
            intEnv("RANDOM_SEED_COUNT", 100);

    /**
     * Per-random-walk step ceiling expressed as a multiple of the repaired
     * product FTS' transition count. 2× is the user-agreed default (large
     * enough not to truncate sensible walks, small enough to actually cut
     * pathological cycles).
     */
    private static final int RANDOM_MAX_STEPS_MULTIPLIER =
            intEnv("RANDOM_MAX_STEPS_MULTIPLIER", 2);

    /** RunID for RQ2/RQ3 CSV rows. RQ2/RQ3 are deterministic — only one run. */
    private static final int RUN_ID = intEnv("runID", 1);

    /** Directory for RQ2/RQ3 CSV files. RQ1 has its own directory in ExperimentRunner. */
    private static final Path RQ_CSV_DIR = Paths.get(
            System.getenv().getOrDefault("RQ_CSV_DIR", "milestone-reports/metrics"));

    private static int intEnv(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) return defaultValue;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private static void run(SplSpec spec) throws Exception {
        Path outDir = Paths.get("milestone-reports/per-product-mutation/" + spec.name);
        Files.createDirectories(outDir);
        Files.createDirectories(RQ_CSV_DIR);
        File rq2CdCsv = RQ_CSV_DIR.resolve("rq2-coverage-directed.csv").toFile();
        File rq2RbCsv = RQ_CSV_DIR.resolve("rq2-random-baseline.csv").toFile();
        File rq3EffCsv = RQ_CSV_DIR.resolve("rq3-efficiency.csv").toFile();

        FeaturedTransitionSystem fts = loadFts(spec.mxe);
        Sat4JSolverFacade solver = loadSolver(spec.dimacs, spec.mapping);
        // Separate solver + operator instance for ActionExchange — one per
        // SPL, reused across all products. See writeProductSection's notes
        // on the SAT4J thread-leak that motivated this hoisting.
        Sat4JSolverFacade aexSolver = loadSolver(spec.dimacs, spec.mapping);
        ActionExchange aex = new ActionExchange(aexSolver);
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
        // Each operator gets a running OpScores accumulator.
        OpScores aggTm = new OpScores();
        OpScores aggAex = new OpScores();
        OpScores aggSm = new OpScores();
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
                        productCount, ftsFeatures, familyBaseline, aex,
                        rq2CdCsv, rq2RbCsv, rq3EffCsv);
                addOpScores(aggTm, ps.tm);
                addOpScores(aggAex, ps.aex);
                addOpScores(aggSm, ps.sm);
            }

            md.write("---\n\n## " + spec.name + " summary (aggregate over " + productCount
                    + " products)\n\n");
            md.write("**Family-level baseline** (Devroey 2014, ported from VIBeS commit "
                    + "f856c90): " + familyBaseline.size() + " test case(s) generated once "
                    + "for the SPL.\n\n");
            md.write("**Equivalent-mutant treatment:** Inozemtseva &amp; Holmes (2014) — "
                    + "mutant not killed by ANY of the five suites (family + product state + "
                    + "product transition + product pair + random) is conservatively "
                    + "classified equivalent and EXCLUDED from the score denominator. Scores "
                    + "below are **killed / (total &minus; equivalent) = adjusted%**.\n\n");
            md.write("| Operator | Total mutants | Equivalent | Family (Devroey) | "
                    + "Product state-cov | Product transition-cov | Product pair-cov | Random |\n");
            md.write("|---|---|---|---|---|---|---|---|\n");
            writeAggregateRow(md, "TransitionMissing", aggTm);
            writeAggregateRow(md, "ActionExchange", aggAex);
            writeAggregateRow(md, "StateMissing", aggSm);
            md.write("\nTotal products: " + productCount + ".\n");

            html.write("<hr/>\n<h2>" + escapeHtml(spec.name)
                    + " summary (aggregate over " + productCount + " products)</h2>\n");
            html.write("<p><strong>Family-level baseline</strong> (Devroey 2014, ported from "
                    + "VIBeS commit f856c90): " + familyBaseline.size() + " test case(s) "
                    + "generated once for the SPL.</p>\n");
            html.write("<p><strong>Equivalent-mutant treatment:</strong> Inozemtseva &amp; "
                    + "Holmes (2014) — mutant not killed by ANY of five suites (family + "
                    + "product state + product transition + product pair + random) is "
                    + "conservatively classified equivalent and EXCLUDED from the score "
                    + "denominator. Scores: <strong>killed / (total − equivalent) = adjusted%"
                    + "</strong>.</p>\n");
            html.write("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">\n");
            html.write("<tr><th>Operator</th><th>Total mutants</th><th>Equivalent</th>"
                    + "<th>Family (Devroey)</th>"
                    + "<th>Product state-cov</th>"
                    + "<th>Product transition-cov</th>"
                    + "<th>Product pair-cov</th>"
                    + "<th>Random</th></tr>\n");
            writeAggregateRowHtml(html, "TransitionMissing", aggTm);
            writeAggregateRowHtml(html, "ActionExchange", aggAex);
            writeAggregateRowHtml(html, "StateMissing", aggSm);
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
                + "ActionExchange.java) emits one mutant per (transition, neighbourhood-adjacent "
                + "action) pair: for `t = (s, α, d)` the replacement `β` is drawn from "
                + "`OutgoingActions(s) ∪ OutgoingActions(d) \\ {α}` (source- and "
                + "sequentially-adjacent actions), skipping any `β` for which `(s, β, d)` "
                + "already exists in the FTS, and restricted to feature-compatible swaps "
                + "(co-satisfiability of `t`'s feature expression with that of some "
                + "`β`-labelled transition, checked via SAT under the feature model). "
                + "Source state, target state, and feature expression of `t` are preserved.\n");
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
        md.write("6. **Mutation score per criterion** = killed mutants / (total &minus; "
                + "equivalent), per Inozemtseva &amp; Holmes (2014). The equivalent set "
                + "is conservatively defined as mutants surviving all five suites in this "
                + "study (family + product state + product transition + product pair + "
                + "random).\n\n");
        md.write("**Random baseline column.** The \"Random\" column shows a single "
                + "representative random suite (seed 0, action budget matched to the "
                + "product's transition-coverage suite, per-walk step ceiling 2 × "
                + "|T_repaired|, cut-and-include semantics — see "
                + "`RandomBaselineGenerator` JavaDoc). The full 100-seed × 3-budget "
                + "random-baseline distribution per (product × operator) — with "
                + "median / quartiles / extremes and aborted-walk counts — lives in "
                + "`milestone-reports/metrics/rq2-random-baseline.csv` (per-coverage-level "
                + "budget; this column is the legacy display only).\n\n");
        md.write("**Note on coverage saturation.** TransitionMissing and ActionExchange "
                + "mutants on a deterministic FTS are killed precisely when the mutated "
                + "transition is traversed; transition coverage therefore detects them by "
                + "construction. Stronger criteria such as transition-pair coverage cannot "
                + "improve detection for this operator set, though they do increase "
                + "execution cost. This is an inherent property of these mutation operators "
                + "on deterministic models. RQ3 is reframed around the cost dimension under "
                + "this saturation property.\n\n");

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
                + "<code>ActionExchange</code> (one per (transition, neighbourhood-adjacent "
                + "action) pair — replacement drawn from "
                + "<code>OutgoingActions(s) ∪ OutgoingActions(d) \\ {α}</code> for "
                + "<code>t = (s, α, d)</code>, skipping pre-existing triples, restricted to "
                + "feature-compatible swaps via SAT under the feature model).</li>\n");
        html.write("<li><strong>Filter synthetic mutants</strong> — drop mutants whose "
                + "mutation site is on an <code>__end__</code> transition (no SUT meaning).</li>\n");
        html.write("<li><strong>Replay each suite on each mutant</strong> — a TestCase kills "
                + "a mutant iff at least one of its non-synthetic transitions "
                + "<code>(source, action, target)</code> is absent in the mutant.</li>\n");
        html.write("<li><strong>Mutation score</strong> = killed / (total &minus; equivalent), "
                + "per Inozemtseva &amp; Holmes (2014). Equivalent set is mutants surviving all "
                + "five suites (family + product state + product transition + product pair + "
                + "random).</li>\n");
        html.write("<p><strong>Random baseline column.</strong> The &quot;Random&quot; "
                + "column shows one representative random suite (seed 0, action budget "
                + "matched to the transition-coverage suite, step ceiling "
                + "2&nbsp;&times;&nbsp;|T_repaired|, cut-and-include semantics — see "
                + "<code>RandomBaselineGenerator</code> JavaDoc). The full 100-seed "
                + "&times;&nbsp;3-budget distribution per (product &times; operator) — "
                + "with median / quartiles / extremes / aborted-walk count — is in "
                + "<code>milestone-reports/metrics/rq2-random-baseline.csv</code>.</p>\n");
        html.write("</ol>\n");
        html.write("<p><strong>Note on coverage saturation.</strong> TransitionMissing and "
                + "ActionExchange mutants on a deterministic FTS are killed precisely when "
                + "the mutated transition is traversed; transition coverage therefore detects "
                + "them by construction. Stronger criteria such as transition-pair coverage "
                + "cannot improve detection for this operator set, though they do increase "
                + "execution cost. This is an inherent property of these mutation operators "
                + "on deterministic models.</p>\n");
    }

    private static ProductScores writeProductSection(BufferedWriter md, BufferedWriter html,
                                                     SplSpec spec, FeaturedTransitionSystem fts,
                                                     Configuration cfg, int productIndex,
                                                     Set<String> ftsFeatures,
                                                     List<TestCase> familyBaseline,
                                                     ActionExchange aex,
                                                     File rq2CdCsv, File rq2RbCsv,
                                                     File rq3EffCsv) throws Exception {
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
        // ActionExchange is supplied by the caller — one instance per
        // SPL, reused across all products. The operator's
        // feature-compatibility cache is FM-determined and persists
        // across calls so cumulative SAT work stays bounded by
        // |distinct φ| × |actions| instead of |products| × |…|.
        // A per-product fresh solver would otherwise spawn millions of
        // SAT4J Timer threads and crash with "unable to create new
        // native thread" on SPLs with thousands of products (e.g. SAS).
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

        // Random baseline — paper-fair construction:
        //   - per-coverage-level action budget (each random suite matches
        //     the total real-action count of the competitor suite it is
        //     compared against);
        //   - model-relative per-walk step ceiling (2× |T_repaired|) so
        //     only pathological cycles are cut;
        //   - cut-and-include semantics (a walk that hits the step ceiling
        //     is truncated and added to the suite — the LEGACY VIBeS
        //     "discard non-terminating walks" behaviour is wrong for this
        //     paper's baseline);
        //   - reproducible via explicit seed.
        //
        // The MD/HTML "Random" column shows a single representative random
        // suite (seed 0, transition-coverage budget). The full 100-seed
        // × 3-budget ensemble is computed below; its UNION of killed
        // mutants per operator feeds the Inozemtseva equivalence
        // treatment (a mutant is equivalent only if it survives ALL 300
        // random suites in addition to the 4 coverage-directed suites),
        // and the per-seed killed counts feed the rq2-random-baseline.csv
        // median/quartile aggregation.
        int stateSuiteActions = countTestSuiteRealActions(Collections.singletonList(stateTc));
        int transSuiteActions = countTestSuiteRealActions(Collections.singletonList(transTc));
        int pairSuiteActions = countTestSuiteRealActions(pairSuite);
        int familySuiteActions = countTestSuiteRealActions(projectedFamily);
        int repairedTransitions = countTransitions(repaired);
        int randomMaxSteps = RandomBaselineGenerator.modelRelativeMaxSteps(
                repairedTransitions, RANDOM_MAX_STEPS_MULTIPLIER);
        RandomBaselineGenerator.Result representativeRandomResult =
                RandomBaselineGenerator.generate(repaired,
                        spec.name + "_p" + productIndex + "_repr",
                        Math.max(1, transSuiteActions),
                        randomMaxSteps, 0L);
        List<TestCase> randomSuite = representativeRandomResult.getSuite();

        // 100-seed × 3-budget random-baseline ensemble. Two outputs:
        //   (a) per-(operator × budget) per-seed killed-count arrays for
        //       the rq2-random-baseline.csv median/quartile rows;
        //   (b) per-operator UNION of killed mutant keys across all 300
        //       random suites — feeds the equivalence treatment below.
        // The loop is hoisted before equivalence so the equivalent set
        // can be the strict definition: a mutant is equivalent iff it
        // survives every one of the 300 random suites in addition to the
        // four coverage-directed suites.
        int[] randomBudgets = new int[] {
                Math.max(1, stateSuiteActions),
                Math.max(1, transSuiteActions),
                Math.max(1, pairSuiteActions)
        };
        String[] budgetSources = new String[] {"state", "transition", "pair"};
        int[][] tmKilledMatrix = new int[randomBudgets.length][RANDOM_SEED_COUNT];
        int[][] aexKilledMatrix = new int[randomBudgets.length][RANDOM_SEED_COUNT];
        int[][] smKilledMatrix = new int[randomBudgets.length][RANDOM_SEED_COUNT];
        int[] abortedTotals = new int[randomBudgets.length];
        Set<String> tmRandomUnionKilled = new HashSet<>();
        Set<String> aexRandomUnionKilled = new HashSet<>();
        Set<String> smRandomUnionKilled = new HashSet<>();
        for (int bi = 0; bi < randomBudgets.length; bi++) {
            int budget = randomBudgets[bi];
            String src = budgetSources[bi];
            for (int seed = 0; seed < RANDOM_SEED_COUNT; seed++) {
                RandomBaselineGenerator.Result rr = RandomBaselineGenerator.generate(
                        repaired,
                        spec.name + "_p" + productIndex + "_rb_" + src,
                        budget, randomMaxSteps, (long) seed);
                abortedTotals[bi] += rr.getAbortedWalks();
                List<TestCase> rs = rr.getSuite();
                FaultDetector.KillResult tmKr = FaultDetector.scoreSuiteDynamic(rs, tmMutants);
                FaultDetector.KillResult aexKr = FaultDetector.scoreSuiteDynamic(rs, aexMutants);
                FaultDetector.KillResult smKr = FaultDetector.scoreSuiteDynamic(rs, smMutants);
                tmKilledMatrix[bi][seed] = tmKr.getKilled();
                aexKilledMatrix[bi][seed] = aexKr.getKilled();
                smKilledMatrix[bi][seed] = smKr.getKilled();
                // Union-of-killed = mutants NOT in survivors.
                addKilledKeys(tmMutants.keySet(), tmKr.getSurvivors(), tmRandomUnionKilled);
                addKilledKeys(aexMutants.keySet(), aexKr.getSurvivors(), aexRandomUnionKilled);
                addKilledKeys(smMutants.keySet(), smKr.getSurvivors(), smRandomUnionKilled);
            }
        }

        // Uniform execution-based kill check via dynamic replay for every
        // (operator × suite) combination — Parça 1 methodology decision
        // (2026-05-22): consistent kill semantic regardless of operator
        // structure. Earlier static-set check is retained in FaultDetector
        // for sanity-check use only.
        FaultDetector.KillResult tmFamilyState = FaultDetector.scoreSuiteDynamic(projectedFamily, tmMutants);
        FaultDetector.KillResult tmState = FaultDetector.scoreSuiteDynamic(stateSuite, tmMutants);
        FaultDetector.KillResult tmTrans = FaultDetector.scoreSuiteDynamic(transSuite, tmMutants);
        FaultDetector.KillResult tmPair = FaultDetector.scoreSuiteDynamic(pairSuite, tmMutants);
        FaultDetector.KillResult tmRandom = FaultDetector.scoreSuiteDynamic(randomSuite, tmMutants);
        FaultDetector.KillResult aexFamilyState = FaultDetector.scoreSuiteDynamic(projectedFamily, aexMutants);
        FaultDetector.KillResult aexState = FaultDetector.scoreSuiteDynamic(stateSuite, aexMutants);
        FaultDetector.KillResult aexTrans = FaultDetector.scoreSuiteDynamic(transSuite, aexMutants);
        FaultDetector.KillResult aexPair = FaultDetector.scoreSuiteDynamic(pairSuite, aexMutants);
        FaultDetector.KillResult aexRandom = FaultDetector.scoreSuiteDynamic(randomSuite, aexMutants);
        FaultDetector.KillResult smFamilyState = FaultDetector.scoreSuiteDynamic(projectedFamily, smMutants);
        FaultDetector.KillResult smState = FaultDetector.scoreSuiteDynamic(stateSuite, smMutants);
        FaultDetector.KillResult smTrans = FaultDetector.scoreSuiteDynamic(transSuite, smMutants);
        FaultDetector.KillResult smPair = FaultDetector.scoreSuiteDynamic(pairSuite, smMutants);
        FaultDetector.KillResult smRandom = FaultDetector.scoreSuiteDynamic(randomSuite, smMutants);

        // Inozemtseva & Holmes (2014) equivalent-mutant treatment with the
        // random axis upgraded to the 100-seed × 3-budget ensemble's
        // union-of-killed (computed above). A mutant is equivalent iff it
        // survives every coverage-directed suite AND every one of the
        // 300 random suites. This is strictly more inclusive of
        // "non-equivalent" than the legacy single-representative random
        // — fewer mutants classified equivalent, denser equivalence
        // signal, paper-fair under the user-agreed definition.
        Set<String> tmEquivalent = equivalentMutantKeys(tmMutants,
                tmFamilyState, tmState, tmTrans, tmPair, tmRandomUnionKilled);
        Set<String> aexEquivalent = equivalentMutantKeys(aexMutants,
                aexFamilyState, aexState, aexTrans, aexPair, aexRandomUnionKilled);
        Set<String> smEquivalent = equivalentMutantKeys(smMutants,
                smFamilyState, smState, smTrans, smPair, smRandomUnionKilled);

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
        md.write("**Random baseline:** " + randomSuite.size() + " test case(s).\n\n");
        md.write("Scores below: **killed / non-equivalent = adjusted%** "
                + "(Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of "
                + "five suites is equivalent and excluded from denominator).\n\n");
        md.write("| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |\n");
        md.write("|---|---|---|---|---|---|---|---|\n");
        md.write("| TransitionMissing | " + tmMutants.size() + " | "
                + formatEquivalent(tmEquivalent.size(), tmMutants.size()) + " | "
                + formatAdjusted(tmFamilyState.getKilled(), tmMutants.size(), tmEquivalent.size()) + " | "
                + formatAdjusted(tmState.getKilled(), tmMutants.size(), tmEquivalent.size()) + " | "
                + formatAdjusted(tmTrans.getKilled(), tmMutants.size(), tmEquivalent.size()) + " | "
                + formatAdjusted(tmPair.getKilled(), tmMutants.size(), tmEquivalent.size()) + " | "
                + formatAdjusted(tmRandom.getKilled(), tmMutants.size(), tmEquivalent.size()) + " |\n");
        md.write("| ActionExchange | " + aexMutants.size() + " | "
                + formatEquivalent(aexEquivalent.size(), aexMutants.size()) + " | "
                + formatAdjusted(aexFamilyState.getKilled(), aexMutants.size(), aexEquivalent.size()) + " | "
                + formatAdjusted(aexState.getKilled(), aexMutants.size(), aexEquivalent.size()) + " | "
                + formatAdjusted(aexTrans.getKilled(), aexMutants.size(), aexEquivalent.size()) + " | "
                + formatAdjusted(aexPair.getKilled(), aexMutants.size(), aexEquivalent.size()) + " | "
                + formatAdjusted(aexRandom.getKilled(), aexMutants.size(), aexEquivalent.size()) + " |\n");
        md.write("| StateMissing | " + smMutants.size() + " | "
                + formatEquivalent(smEquivalent.size(), smMutants.size()) + " | "
                + formatAdjusted(smFamilyState.getKilled(), smMutants.size(), smEquivalent.size()) + " | "
                + formatAdjusted(smState.getKilled(), smMutants.size(), smEquivalent.size()) + " | "
                + formatAdjusted(smTrans.getKilled(), smMutants.size(), smEquivalent.size()) + " | "
                + formatAdjusted(smPair.getKilled(), smMutants.size(), smEquivalent.size()) + " | "
                + formatAdjusted(smRandom.getKilled(), smMutants.size(), smEquivalent.size()) + " |\n");

        if (!tmEquivalent.isEmpty() || !aexEquivalent.isEmpty() || !smEquivalent.isEmpty()) {
            md.write("\n**Equivalent mutants (not killed by any of the five suites):**\n");
            appendKeyList(md, "TransitionMissing", tmEquivalent);
            appendKeyList(md, "ActionExchange", aexEquivalent);
            appendKeyList(md, "StateMissing", smEquivalent);
        }

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
        html.write("<p><strong>Random baseline:</strong> " + randomSuite.size()
                + " test case(s).</p>\n");
        html.write("<p>Scores: <strong>killed / non-equivalent = adjusted%</strong>. "
                + "Equivalent = mutant not killed by ANY of five suites "
                + "(Inozemtseva &amp; Holmes 2014).</p>\n");
        html.write("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">\n");
        html.write("<tr><th>Operator</th><th>Total</th><th>Equivalent</th>"
                + "<th>Family (Devroey)</th>"
                + "<th>Product state-cov</th>"
                + "<th>Product transition-cov</th>"
                + "<th>Product pair-cov</th>"
                + "<th>Random</th></tr>\n");
        html.write("<tr><td>TransitionMissing</td><td>" + tmMutants.size() + "</td><td>"
                + formatEquivalent(tmEquivalent.size(), tmMutants.size()) + "</td><td>"
                + formatAdjusted(tmFamilyState.getKilled(), tmMutants.size(), tmEquivalent.size()) + "</td><td>"
                + formatAdjusted(tmState.getKilled(), tmMutants.size(), tmEquivalent.size()) + "</td><td>"
                + formatAdjusted(tmTrans.getKilled(), tmMutants.size(), tmEquivalent.size()) + "</td><td>"
                + formatAdjusted(tmPair.getKilled(), tmMutants.size(), tmEquivalent.size()) + "</td><td>"
                + formatAdjusted(tmRandom.getKilled(), tmMutants.size(), tmEquivalent.size()) + "</td></tr>\n");
        html.write("<tr><td>ActionExchange</td><td>" + aexMutants.size() + "</td><td>"
                + formatEquivalent(aexEquivalent.size(), aexMutants.size()) + "</td><td>"
                + formatAdjusted(aexFamilyState.getKilled(), aexMutants.size(), aexEquivalent.size()) + "</td><td>"
                + formatAdjusted(aexState.getKilled(), aexMutants.size(), aexEquivalent.size()) + "</td><td>"
                + formatAdjusted(aexTrans.getKilled(), aexMutants.size(), aexEquivalent.size()) + "</td><td>"
                + formatAdjusted(aexPair.getKilled(), aexMutants.size(), aexEquivalent.size()) + "</td><td>"
                + formatAdjusted(aexRandom.getKilled(), aexMutants.size(), aexEquivalent.size()) + "</td></tr>\n");
        html.write("<tr><td>StateMissing</td><td>" + smMutants.size() + "</td><td>"
                + formatEquivalent(smEquivalent.size(), smMutants.size()) + "</td><td>"
                + formatAdjusted(smFamilyState.getKilled(), smMutants.size(), smEquivalent.size()) + "</td><td>"
                + formatAdjusted(smState.getKilled(), smMutants.size(), smEquivalent.size()) + "</td><td>"
                + formatAdjusted(smTrans.getKilled(), smMutants.size(), smEquivalent.size()) + "</td><td>"
                + formatAdjusted(smPair.getKilled(), smMutants.size(), smEquivalent.size()) + "</td><td>"
                + formatAdjusted(smRandom.getKilled(), smMutants.size(), smEquivalent.size()) + "</td></tr>\n");
        html.write("</table>\n");
        if (!tmEquivalent.isEmpty() || !aexEquivalent.isEmpty() || !smEquivalent.isEmpty()) {
            html.write("<details><summary>Equivalent mutants (not killed by any of the five suites)</summary>\n");
            appendKeyListHtml(html, "TransitionMissing", tmEquivalent);
            appendKeyListHtml(html, "ActionExchange", aexEquivalent);
            appendKeyListHtml(html, "StateMissing", smEquivalent);
            html.write("</details>\n");
        }

        ProductScores ps = new ProductScores();
        fillOpScores(ps.tm, tmMutants.size(), tmEquivalent.size(),
                tmFamilyState, tmState, tmTrans, tmPair, tmRandom);
        fillOpScores(ps.aex, aexMutants.size(), aexEquivalent.size(),
                aexFamilyState, aexState, aexTrans, aexPair, aexRandom);
        fillOpScores(ps.sm, smMutants.size(), smEquivalent.size(),
                smFamilyState, smState, smTrans, smPair, smRandom);

        // ---- RQ2 CSV: coverage-directed mutation scores ----
        writeRq2CoverageDirected(rq2CdCsv, spec, productIndex, "TransitionMissing",
                tmMutants.size(), tmEquivalent.size(),
                tmState, tmTrans, tmPair, tmFamilyState);
        writeRq2CoverageDirected(rq2CdCsv, spec, productIndex, "ActionExchange",
                aexMutants.size(), aexEquivalent.size(),
                aexState, aexTrans, aexPair, aexFamilyState);
        writeRq2CoverageDirected(rq2CdCsv, spec, productIndex, "StateMissing",
                smMutants.size(), smEquivalent.size(),
                smState, smTrans, smPair, smFamilyState);

        // ---- RQ2 CSV: random baseline (writes from the hoisted 100-seed
        // × 3-budget ensemble computed above for the equivalence union).
        // Both writes reuse the same per-seed killed-count matrices; no
        // additional kill checks here.
        for (int bi = 0; bi < randomBudgets.length; bi++) {
            int budget = randomBudgets[bi];
            String src = budgetSources[bi];
            writeRq2RandomBaseline(rq2RbCsv, spec, productIndex, src, "TransitionMissing",
                    tmMutants.size(), tmEquivalent.size(),
                    budget, randomMaxSteps, abortedTotals[bi], tmKilledMatrix[bi]);
            writeRq2RandomBaseline(rq2RbCsv, spec, productIndex, src, "ActionExchange",
                    aexMutants.size(), aexEquivalent.size(),
                    budget, randomMaxSteps, abortedTotals[bi], aexKilledMatrix[bi]);
            writeRq2RandomBaseline(rq2RbCsv, spec, productIndex, src, "StateMissing",
                    smMutants.size(), smEquivalent.size(),
                    budget, randomMaxSteps, abortedTotals[bi], smKilledMatrix[bi]);
        }

        // ---- RQ3 CSV: efficiency = killed / total transitions actually
        // EXECUTED by the suite under a strict reset-per-TestCase
        // executor. Denominator is TestExecution.executeSuite(suite,
        // repaired).getTotalRealTransitions() — the user-original RQ3
        // definition. This was temporarily switched to a static raw-count
        // in fc29817 because the pre-refactor pair pipeline produced
        // suites with mass step-0 refusals (executor reset-per-TC vs.
        // segments starting at non-INIT pair-vertices). With the
        // INIT-less PairGraphTransformer + spec-faithful initial-return
        // splitting, pair TCs start at the FTS initial state and execute
        // end-to-end, so the dynamic denominator is the correct
        // measurement again.
        long stateSuiteCost = TestExecution.executeSuite(
                Collections.singletonList(stateTc), repaired).getTotalRealTransitions();
        long transSuiteCost = TestExecution.executeSuite(
                Collections.singletonList(transTc), repaired).getTotalRealTransitions();
        long pairSuiteCost = TestExecution.executeSuite(
                pairSuite, repaired).getTotalRealTransitions();
        long familySuiteCost = TestExecution.executeSuite(
                projectedFamily, repaired).getTotalRealTransitions();
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "state",
                "TransitionMissing", tmMutants.size(), tmEquivalent.size(),
                tmState.getKilled(), stateSuiteCost);
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "transition",
                "TransitionMissing", tmMutants.size(), tmEquivalent.size(),
                tmTrans.getKilled(), transSuiteCost);
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "pair",
                "TransitionMissing", tmMutants.size(), tmEquivalent.size(),
                tmPair.getKilled(), pairSuiteCost);
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "family-baseline",
                "TransitionMissing", tmMutants.size(), tmEquivalent.size(),
                tmFamilyState.getKilled(), familySuiteCost);
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "state",
                "ActionExchange", aexMutants.size(), aexEquivalent.size(),
                aexState.getKilled(), stateSuiteCost);
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "transition",
                "ActionExchange", aexMutants.size(), aexEquivalent.size(),
                aexTrans.getKilled(), transSuiteCost);
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "pair",
                "ActionExchange", aexMutants.size(), aexEquivalent.size(),
                aexPair.getKilled(), pairSuiteCost);
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "family-baseline",
                "ActionExchange", aexMutants.size(), aexEquivalent.size(),
                aexFamilyState.getKilled(), familySuiteCost);
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "state",
                "StateMissing", smMutants.size(), smEquivalent.size(),
                smState.getKilled(), stateSuiteCost);
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "transition",
                "StateMissing", smMutants.size(), smEquivalent.size(),
                smTrans.getKilled(), transSuiteCost);
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "pair",
                "StateMissing", smMutants.size(), smEquivalent.size(),
                smPair.getKilled(), pairSuiteCost);
        writeRq3Efficiency(rq3EffCsv, spec, productIndex, "family-baseline",
                "StateMissing", smMutants.size(), smEquivalent.size(),
                smFamilyState.getKilled(), familySuiteCost);

        return ps;
    }

    // -------------------- RQ2/RQ3 CSV helpers --------------------

    private static void writeRq2CoverageDirected(File rq2CdCsv, SplSpec spec, int productIndex,
                                                 String operator,
                                                 int totalMutants, int equivalent,
                                                 FaultDetector.KillResult st,
                                                 FaultDetector.KillResult tr,
                                                 FaultDetector.KillResult pr,
                                                 FaultDetector.KillResult fam) throws IOException {
        appendRq2CdRow(rq2CdCsv, spec, productIndex, "state", operator,
                totalMutants, equivalent, st.getKilled());
        appendRq2CdRow(rq2CdCsv, spec, productIndex, "transition", operator,
                totalMutants, equivalent, tr.getKilled());
        appendRq2CdRow(rq2CdCsv, spec, productIndex, "pair", operator,
                totalMutants, equivalent, pr.getKilled());
        appendRq2CdRow(rq2CdCsv, spec, productIndex, "family-baseline", operator,
                totalMutants, equivalent, fam.getKilled());
    }

    private static void appendRq2CdRow(File rq2CdCsv, SplSpec spec, int productIndex,
                                       String coverage, String operator,
                                       int totalMutants, int equivalent, int killed) throws IOException {
        int adjustedDenom = totalMutants - equivalent;
        double scorePct = adjustedDenom <= 0 ? 0.0 : 100.0 * killed / adjustedDenom;
        int survived = adjustedDenom - killed;
        MeasurementCsv.appendRq2CoverageDirectedRow(rq2CdCsv,
                RUN_ID, spec.name, productIndex, coverage, operator,
                totalMutants, equivalent, killed, survived, scorePct);
    }

    private static void writeRq2RandomBaseline(File rq2RbCsv, SplSpec spec, int productIndex,
                                               String coverageBudgetSource, String operator,
                                               int totalMutants, int equivalent,
                                               int budget, int maxStepsPerCase,
                                               int abortedTotal, int[] killed) throws IOException {
        int adjustedDenom = totalMutants - equivalent;
        int[] sorted = killed.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        int killedMin = sorted[0];
        int killedMax = sorted[n - 1];
        int killedMedian = sorted[n / 2];
        int killedP25 = sorted[Math.max(0, (int) Math.floor(n * 0.25))];
        int killedP75 = sorted[Math.min(n - 1, (int) Math.ceil(n * 0.75) - 1)];
        double scoreMin = adjustedDenom <= 0 ? 0.0 : 100.0 * killedMin / adjustedDenom;
        double scoreMax = adjustedDenom <= 0 ? 0.0 : 100.0 * killedMax / adjustedDenom;
        double scoreMedian = adjustedDenom <= 0 ? 0.0 : 100.0 * killedMedian / adjustedDenom;
        double scoreP25 = adjustedDenom <= 0 ? 0.0 : 100.0 * killedP25 / adjustedDenom;
        double scoreP75 = adjustedDenom <= 0 ? 0.0 : 100.0 * killedP75 / adjustedDenom;
        MeasurementCsv.appendRq2RandomBaselineRow(rq2RbCsv,
                RUN_ID, spec.name, productIndex, coverageBudgetSource, operator,
                totalMutants, equivalent, budget, maxStepsPerCase,
                n, abortedTotal,
                killedMin, killedP25, killedMedian, killedP75, killedMax,
                scoreMin, scoreP25, scoreMedian, scoreP75, scoreMax);
    }

    private static void writeRq3Efficiency(File rq3EffCsv, SplSpec spec, int productIndex,
                                           String coverage, String operator,
                                           int totalMutants, int equivalent,
                                           int killed, long totalTransitionsExecuted) throws IOException {
        int adjustedDenom = totalMutants - equivalent;
        double scorePct = adjustedDenom <= 0 ? 0.0 : 100.0 * killed / adjustedDenom;
        int survived = adjustedDenom - killed;
        double efficiency = totalTransitionsExecuted <= 0
                ? 0.0
                : (double) killed / totalTransitionsExecuted;
        MeasurementCsv.appendRq3EfficiencyRow(rq3EffCsv,
                RUN_ID, spec.name, productIndex, coverage, operator,
                totalMutants, equivalent, killed, survived, scorePct,
                totalTransitionsExecuted, efficiency);
    }

    /**
     * Total real-action count of a TestCase suite. Counted:
     * <ul>
     *   <li>Normal action-labelled transitions;</li>
     *   <li>{@code __dup__N}-suffixed transitions — these are Chinese-Postman
     *       edge-doubling labels around a REAL underlying action; executing
     *       one IS a real test step (matches
     *       {@link TestExecution#execute}'s {@code countsAsReal=true}
     *       branch for {@code __dup__}).</li>
     * </ul>
     * Not counted:
     * <ul>
     *   <li>{@code __balance__N} synthetic balancing edges
     *       ({@link EulerianBalancer#SYNTHETIC_ACTION_PREFIX});</li>
     *   <li>{@code __end__} transitions — back-to-INIT synthetic edges
     *       added during ESG→FTS conversion, with no SUT meaning.</li>
     * </ul>
     * Used both as the action budget for the random-baseline ensemble
     * (per coverage level) AND as the RQ3 efficiency denominator
     * (paper-fair "test cost of the suite under a teleport-capable test
     * framework", which is the standard semantic for coverage suites).
     */
    private static int countTestSuiteRealActions(List<TestCase> suite) {
        int n = 0;
        for (TestCase tc : suite) {
            for (Transition t : tc) {
                String name = t.getAction().getName();
                if (name.startsWith(EulerianBalancer.SYNTHETIC_ACTION_PREFIX)) continue;
                if (name.startsWith("__end__")) continue;
                // Normal action OR __dup__N-suffix (real action doubled) — both count.
                n++;
            }
        }
        return n;
    }

    private static void addOpScores(OpScores acc, OpScores delta) {
        acc.total += delta.total;
        acc.equivalent += delta.equivalent;
        acc.killedFamilyState += delta.killedFamilyState;
        acc.killedProductState += delta.killedProductState;
        acc.killedProductTrans += delta.killedProductTrans;
        acc.killedProductPair += delta.killedProductPair;
        acc.killedRandom += delta.killedRandom;
    }

    private static void writeAggregateRow(BufferedWriter md, String label, OpScores agg)
            throws IOException {
        md.write("| " + label + " | " + agg.total + " | "
                + formatEquivalent(agg.equivalent, agg.total) + " | "
                + formatAdjusted(agg.killedFamilyState, agg.total, agg.equivalent) + " | "
                + formatAdjusted(agg.killedProductState, agg.total, agg.equivalent) + " | "
                + formatAdjusted(agg.killedProductTrans, agg.total, agg.equivalent) + " | "
                + formatAdjusted(agg.killedProductPair, agg.total, agg.equivalent) + " | "
                + formatAdjusted(agg.killedRandom, agg.total, agg.equivalent) + " |\n");
    }

    private static void writeAggregateRowHtml(BufferedWriter html, String label, OpScores agg)
            throws IOException {
        html.write("<tr><td>" + label + "</td><td>" + agg.total + "</td><td>"
                + formatEquivalent(agg.equivalent, agg.total) + "</td><td>"
                + formatAdjusted(agg.killedFamilyState, agg.total, agg.equivalent) + "</td><td>"
                + formatAdjusted(agg.killedProductState, agg.total, agg.equivalent) + "</td><td>"
                + formatAdjusted(agg.killedProductTrans, agg.total, agg.equivalent) + "</td><td>"
                + formatAdjusted(agg.killedProductPair, agg.total, agg.equivalent) + "</td><td>"
                + formatAdjusted(agg.killedRandom, agg.total, agg.equivalent) + "</td></tr>\n");
    }

    private static void fillOpScores(OpScores out, int total, int equivalent,
                                     FaultDetector.KillResult family,
                                     FaultDetector.KillResult state,
                                     FaultDetector.KillResult trans,
                                     FaultDetector.KillResult pair,
                                     FaultDetector.KillResult random) {
        out.total = total;
        out.equivalent = equivalent;
        out.killedFamilyState = family.getKilled();
        out.killedProductState = state.getKilled();
        out.killedProductTrans = trans.getKilled();
        out.killedProductPair = pair.getKilled();
        out.killedRandom = random.getKilled();
    }

    /**
     * Returns the set of mutant keys not killed by any of the coverage-
     * directed suites AND not killed by any of the random-baseline
     * ensemble's suites. Inozemtseva &amp; Holmes (2014) equivalent
     * treatment, with the random axis upgraded from a single
     * representative seed to the 100-seed × 3-budget ensemble: a mutant
     * is equivalent iff every one of the 300 random suites failed to
     * kill it (in addition to all four coverage-directed suites failing).
     *
     * @param randomEnsembleKilled  union of mutant keys killed by any
     *                              random suite in the 100-seed × 3-budget
     *                              ensemble. A mutant key OUTSIDE this set
     *                              is therefore surviving the random axis.
     */
    private static Set<String> equivalentMutantKeys(
            Map<String, FeaturedTransitionSystem> mutants,
            FaultDetector.KillResult family,
            FaultDetector.KillResult state,
            FaultDetector.KillResult trans,
            FaultDetector.KillResult pair,
            Set<String> randomEnsembleKilled) {
        Set<String> survivedFamily = new HashSet<>(family.getSurvivors());
        Set<String> survivedState = new HashSet<>(state.getSurvivors());
        Set<String> survivedTrans = new HashSet<>(trans.getSurvivors());
        Set<String> survivedPair = new HashSet<>(pair.getSurvivors());
        Set<String> equivalent = new LinkedHashSet<>();
        for (String key : mutants.keySet()) {
            if (survivedFamily.contains(key)
                    && survivedState.contains(key)
                    && survivedTrans.contains(key)
                    && survivedPair.contains(key)
                    && !randomEnsembleKilled.contains(key)) {
                equivalent.add(key);
            }
        }
        return equivalent;
    }

    /**
     * Accumulates the killed mutant keys for a given suite's KillResult
     * into {@code unionTarget}. Killed = total mutant keys minus the
     * suite's survivor list.
     */
    private static void addKilledKeys(Set<String> allMutantKeys,
                                      List<String> survivors,
                                      Set<String> unionTarget) {
        Set<String> survivorSet = new HashSet<>(survivors);
        for (String key : allMutantKeys) {
            if (!survivorSet.contains(key)) {
                unionTarget.add(key);
            }
        }
    }

    /**
     * Formats killed-by-suite as "killed / (total - equivalent) = pct%"
     * per Inozemtseva &amp; Holmes (2014). When (total - equivalent) is
     * zero (every mutant equivalent), returns "n/a".
     */
    private static String formatAdjusted(int killed, int total, int equivalent) {
        int denom = total - equivalent;
        if (denom <= 0) {
            return "0/0 = n/a";
        }
        return killed + "/" + denom + " = "
                + String.format("%.1f", 100.0 * killed / denom) + "%";
    }

    private static String formatEquivalent(int equivalent, int total) {
        if (total == 0) {
            return "0/0";
        }
        return equivalent + "/" + total + " = "
                + String.format("%.1f", 100.0 * equivalent / total) + "%";
    }

    private static void appendKeyList(BufferedWriter md, String label, Set<String> keys)
            throws IOException {
        if (keys.isEmpty()) return;
        md.write("\n_" + label + "_ (" + keys.size() + "):\n\n");
        for (String key : keys) {
            md.write("- `" + key + "`\n");
        }
    }

    private static void appendKeyListHtml(BufferedWriter html, String label, Set<String> keys)
            throws IOException {
        if (keys.isEmpty()) return;
        html.write("<p><em>" + escapeHtml(label) + "</em> (" + keys.size() + "):</p>\n<ul>\n");
        for (String key : keys) {
            html.write("<li><code>" + escapeHtml(key) + "</code></li>\n");
        }
        html.write("</ul>\n");
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

    /**
     * Per-product per-operator score record. All kill counts are
     * execution-based (FaultDetector.scoreSuiteDynamic) for uniform
     * methodology across operators. {@code equivalent} is the count of
     * mutants not killed by ANY of the five suites — these are
     * conservatively classified as equivalent per Inozemtseva &amp;
     * Holmes (2014) and excluded from the score denominator.
     */
    private static final class OpScores {
        int total;
        int equivalent;
        int killedFamilyState;
        int killedProductState;
        int killedProductTrans;
        int killedProductPair;
        int killedRandom;

        int nonEquivalentDenominator() {
            return total - equivalent;
        }
    }

    private static final class ProductScores {
        OpScores tm = new OpScores();
        OpScores aex = new OpScores();
        OpScores sm = new OpScores();
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
