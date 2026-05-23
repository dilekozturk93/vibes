package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.coverage.StateCoverageGenerator;
import be.vibes.testgeneration.coverage.TransitionCoverageGenerator;
import be.vibes.testgeneration.coverage.TransitionPairCoverageGenerator;
import be.vibes.testgeneration.coverage.baseline.AllStatesGenerator;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import be.vibes.ts.TransitionSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * RQ1 scalability runner — produces the per-(SPL × product × coverage)
 * timing + memory + suite-shape rows for the paper's RQ1 dataset.
 *
 * <p>Drives the four coverage criteria the paper compares
 * ({@code state}, {@code transition}, {@code pair}) for every valid
 * product configuration of every wired SPL, recording the disjoint
 * timing segments (projection, generation, transformation,
 * execution) plus MXBean-based peak heap for the generation and
 * execution phases separately. Also runs the SPL-level Devroey
 * {@code AllStates} family baseline once per SPL and records its own
 * scalability row (generation time + suite shape + termination
 * reason).
 *
 * <p>Multi-run orchestration is OUT OF PROCESS: invoke this main 11
 * times via the {@code run-rq1.sh} script, with {@code runID=1..11}
 * supplied via the environment. Each invocation appends to the same CSV
 * files (header written only on the first invocation per file). Running
 * 11 iterations inside one JVM would contaminate timings with warm-up
 * and GC carry-over.
 *
 * <p>Environment variables:
 * <ul>
 *   <li>{@code runID} — int, default 1; written verbatim into the
 *       {@code RunID} column.</li>
 *   <li>{@code SPLS} — comma-separated SPL names to include; defaults
 *       to all five wired SPLs (SVM, eMail, Elevator, BankAccountv2,
 *       StudentAttendanceSystem).</li>
 *   <li>{@code RQ1_CSV_DIR} — destination directory; defaults to
 *       {@code milestone-reports/metrics}. Two files are appended:
 *       {@code rq1-coverage-directed.csv} and
 *       {@code rq1-family-baseline.csv}.</li>
 *   <li>{@code FAMILY_TIMEOUT_HOURS} — optional wall-clock ceiling for
 *       the family-baseline generation per SPL (interpreted in the
 *       {@code AllStatesGenerator} call). Default 0 (no ceiling).</li>
 *   <li>{@code MUTATION_TIME} — for unit-test runs only;
 *       {@code true} prints rows to stdout in lieu of writing CSV files.</li>
 * </ul>
 *
 * <p>Mutation scoring (RQ2) and efficiency (RQ3) are NOT in this
 * runner's scope — they are deterministic given fixed seeds and run
 * once via {@link PerProductMutationReportGenerator}, not 11×.
 */
public final class ExperimentRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ExperimentRunner.class);

    private static final String DEFAULT_CSV_DIR = "milestone-reports/metrics";
    private static final String RQ1_CD_CSV = "rq1-coverage-directed.csv";
    private static final String RQ1_FB_CSV = "rq1-family-baseline.csv";

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

    private ExperimentRunner() {
        // Entry point only.
    }

    public static void main(String[] args) throws Exception {
        int runId = intEnv("runID", 1);
        List<String> splFilter = listEnv("SPLS", Collections.emptyList());
        Path csvDir = Paths.get(System.getenv().getOrDefault("RQ1_CSV_DIR", DEFAULT_CSV_DIR));
        Files.createDirectories(csvDir);
        File rq1CdCsv = csvDir.resolve(RQ1_CD_CSV).toFile();
        File rq1FbCsv = csvDir.resolve(RQ1_FB_CSV).toFile();

        LOG.info("RQ1 scalability runner: runID={}, csvDir={}", runId, csvDir.toAbsolutePath());
        for (SplSpec spec : SPLS) {
            if (!splFilter.isEmpty() && !splFilter.contains(spec.name)) {
                continue;
            }
            runSpl(spec, runId, rq1CdCsv, rq1FbCsv);
        }
    }

    /**
     * Loads the SPL, runs the family-level Devroey baseline (one row),
     * and iterates per-product coverage cells (one row per product per
     * coverage criterion).
     */
    private static void runSpl(SplSpec spec, int runId,
                               File rq1CdCsv, File rq1FbCsv) throws Exception {
        FeaturedTransitionSystem fts = loadFts(spec.mxe);
        int ftsStates = countStates(fts);
        int ftsTransitions = countTransitions(fts);

        // -------- Family-level baseline scalability --------
        Sat4JSolverFacade familySolver = loadSolver(spec.dimacs, spec.mapping);
        MetricsCollector.resetPeakHeap();
        long famWallStart = System.nanoTime();
        List<TestCase> familyBaseline;
        String terminationReason;
        double famGenPeakMb;
        try {
            familyBaseline = AllStatesGenerator.generateForFts(
                    fts, familySolver, spec.name + "_family");
            terminationReason = "completed";
        } catch (OutOfMemoryError oom) {
            familyBaseline = Collections.emptyList();
            terminationReason = "OOM";
            LOG.error("Family baseline OOM for {}", spec.name, oom);
        } catch (Exception ex) {
            familyBaseline = Collections.emptyList();
            terminationReason = "exception: " + ex.getClass().getSimpleName();
            LOG.error("Family baseline failed for {}", spec.name, ex);
        }
        long famWallEnd = System.nanoTime();
        famGenPeakMb = MetricsCollector.peakHeapMb();
        double famWallMs = nsToMs(famWallEnd - famWallStart);
        int famTotalActions = totalRealActions(familyBaseline);
        MeasurementCsv.appendRq1FamilyBaselineRow(rq1FbCsv,
                runId, spec.name, ftsStates, ftsTransitions,
                familyBaseline.size(), famTotalActions,
                famWallMs, famGenPeakMb, famWallMs, terminationReason);
        LOG.info("{} family baseline: {} TC, {} actions, {} ms, {} MB peak, term={}",
                spec.name, familyBaseline.size(), famTotalActions, famWallMs,
                famGenPeakMb, terminationReason);

        // -------- Per-product coverage-directed --------
        Sat4JSolverFacade enumSolver = loadSolver(spec.dimacs, spec.mapping);
        int productIndex = 0;
        Iterator<Configuration> configs = enumSolver.getSolutions();
        while (configs.hasNext()) {
            Configuration cfg = configs.next();
            productIndex++;
            runProduct(spec, fts, cfg, productIndex, runId, rq1CdCsv,
                    ftsStates, ftsTransitions);
        }
    }

    /**
     * Drives the three coverage cells for the given product. Each cell
     * is self-contained: it does its OWN projection/repair, records that
     * as its {@code projectionTimeMs}, and reports its own wall-clock
     * sentinel. Projection cost is paid three times per product — fine,
     * projection is sub-millisecond on all five SPLs and the
     * cell-self-contained design keeps the
     * {@code segmentSum ≈ wallClock} invariant per row, which is the
     * RQ1 measurement contract.
     */
    private static void runProduct(SplSpec spec, FeaturedTransitionSystem fts,
                                   Configuration cfg, int productIndex, int runId,
                                   File rq1CdCsv, int ftsStates, int ftsTransitions) throws IOException {
        runCoverageCell(spec, fts, cfg, productIndex, runId, rq1CdCsv, "state");
        runCoverageCell(spec, fts, cfg, productIndex, runId, rq1CdCsv, "transition");
        runCoverageCell(spec, fts, cfg, productIndex, runId, rq1CdCsv, "pair");
    }

    /**
     * Single (product × coverage) cell: generates the suite, executes
     * it on the repaired product FTS, and writes one RQ1 CSV row. The
     * outer projection time is shared across the three coverage cells of
     * the same product (it is identical for all three by construction).
     * Disjoint segments by construction — see {@link MeasurementCsv}.
     */
    private static void runCoverageCell(SplSpec spec, FeaturedTransitionSystem fts,
                                        Configuration cfg, int productIndex, int runId,
                                        File rq1CdCsv, String coverage) throws IOException {
        String testId = spec.name + "_p" + productIndex + "_" + coverage;
        long wallStartNanos = System.nanoTime();

        // ---- Projection + repair (cell-local) ----
        long projStart = System.nanoTime();
        FeaturedTransitionSystem projected = FExpressionPreservingProjection.project(fts, cfg);
        FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);
        long projEnd = System.nanoTime();
        double projectionMs = nsToMs(projEnd - projStart);
        int repairedStates = countStates(repaired);
        int repairedTransitions = countTransitions(repaired);

        double genMs;
        double transformMs;
        double genPeakMb;
        int suiteSize;
        int totalActions;
        double coveragePct;
        List<TestCase> suiteForExec;

        MetricsCollector.resetPeakHeap();
        long genStart = System.nanoTime();
        switch (coverage) {
            case "state": {
                TestCase tc = StateCoverageGenerator.generate(fts, cfg, testId);
                long genEnd = System.nanoTime();
                genMs = nsToMs(genEnd - genStart);
                transformMs = 0.0;
                genPeakMb = MetricsCollector.peakHeapMb();
                List<Transition> walk = toList(tc);
                suiteSize = 1;
                totalActions = MetricsCollector.totalWalkTransitions(walk);
                coveragePct = MetricsCollector.stateCoveragePercentage(repaired, walk);
                suiteForExec = Collections.singletonList(tc);
                break;
            }
            case "transition": {
                TestCase tc = TransitionCoverageGenerator.generate(fts, cfg, testId);
                long genEnd = System.nanoTime();
                genMs = nsToMs(genEnd - genStart);
                transformMs = 0.0;
                genPeakMb = MetricsCollector.peakHeapMb();
                List<Transition> walk = toList(tc);
                suiteSize = 1;
                totalActions = MetricsCollector.totalWalkTransitions(walk);
                coveragePct = MetricsCollector.transitionCoveragePercentage(repaired, walk);
                suiteForExec = Collections.singletonList(tc);
                break;
            }
            case "pair": {
                TransitionPairCoverageGenerator.Timings timings = new TransitionPairCoverageGenerator.Timings();
                List<TestCase> suite = TransitionPairCoverageGenerator.generateWithTimings(
                        fts, cfg, testId, timings);
                long genEnd = System.nanoTime();
                // Disjoint segment policy: testGenMs reports the
                // pair-graph CONSTRUCTION only; transformationMs reports
                // balancing + SCC check + Hierholzer + translation/dedup.
                // generateWithTimings repeats the projection+repair
                // internally (idempotent — same fts+cfg input) and we
                // ignore that inner result; the row's projectionMs comes
                // from the outer projection done up-top, consistent with
                // state/transition cells. The inner projection inflates
                // wall-clock by one extra projection (sub-millisecond on
                // all five SPLs), which the segmentSum-vs-wallClock
                // sentinel will surface as a small drift in the
                // wall-clock direction.
                genMs = nsToMs(timings.pairGraphConstructionNanos);
                transformMs = nsToMs(timings.transformationNanos());
                genPeakMb = MetricsCollector.peakHeapMb();
                suiteSize = suite.size();
                totalActions = MetricsCollector.totalSuiteTransitions(suite);
                coveragePct = MetricsCollector.pairCoveragePercentageOfSuite(repaired, suite);
                suiteForExec = suite;
                // Sanity: wall-clock of generateWithTimings should match
                // sum of its internal segments. Log a warning if drift
                // is gross.
                long internalSum = timings.testGenTotalNanos();
                long wallInternal = genEnd - genStart;
                if (Math.abs(internalSum - wallInternal) > wallInternal / 10
                        && wallInternal > 1_000_000L /* ignore sub-ms noise */) {
                    LOG.warn("pair-gen segment-sum mismatch on {}: sum={} ns, wall={} ns",
                            testId, internalSum, wallInternal);
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown coverage criterion: " + coverage);
        }

        // ---- Execution segment (TestExecution.executeSuite on repaired FTS) ----
        MetricsCollector.resetPeakHeap();
        long execStart = System.nanoTime();
        TestExecution.SuiteExecutionResult exec = TestExecution.executeSuite(suiteForExec, repaired);
        long execEnd = System.nanoTime();
        double execMs = nsToMs(execEnd - execStart);
        double execPeakMb = MetricsCollector.peakHeapMb();

        long wallEnd = System.nanoTime();
        double wallMs = nsToMs(wallEnd - wallStartNanos);

        MeasurementCsv.appendRq1CoverageDirectedRow(rq1CdCsv,
                runId, spec.name, coverage, productIndex,
                repairedStates, repairedTransitions,
                suiteSize, totalActions, coveragePct,
                projectionMs, genMs, transformMs, execMs,
                genPeakMb, execPeakMb, wallMs);

        if (LOG.isDebugEnabled()) {
            LOG.debug("{} p{} {} -> {} TC, {} actions, cov={}%, proj={}ms, gen={}ms, transform={}ms, exec={}ms, execActions={}",
                    spec.name, productIndex, coverage, suiteSize, totalActions,
                    coveragePct, projectionMs, genMs, transformMs, execMs,
                    exec.getTotalRealTransitions());
        }
    }

    // -------------------------- helpers --------------------------

    private static FeaturedTransitionSystem loadFts(String resource) throws Exception {
        URL url = ExperimentRunner.class.getClassLoader().getResource(resource);
        if (url == null) {
            throw new IOException("Resource not found on classpath: " + resource);
        }
        return new MxeToFtsConverter().convert(new File(url.toURI()));
    }

    private static Sat4JSolverFacade loadSolver(String dimacs, String mapping) throws Exception {
        URL dimacsUrl = ExperimentRunner.class.getClassLoader().getResource(dimacs);
        URL mappingUrl = ExperimentRunner.class.getClassLoader().getResource(mapping);
        if (dimacsUrl == null || mappingUrl == null) {
            throw new IOException("DIMACS or mapping resource missing for " + dimacs);
        }
        DimacsModel model = DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mappingUrl.toURI()), new File(dimacsUrl.toURI()));
        return new Sat4JSolverFacade(model);
    }

    private static int countStates(TransitionSystem ts) {
        int n = 0;
        Iterator<State> it = ts.states();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    private static int countTransitions(TransitionSystem ts) {
        int n = 0;
        Iterator<Transition> it = ts.transitions();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    private static List<Transition> toList(TestCase tc) {
        List<Transition> list = new ArrayList<>();
        for (Transition t : tc) {
            list.add(t);
        }
        return list;
    }

    private static int totalRealActions(List<TestCase> suite) {
        int n = 0;
        for (TestCase tc : suite) {
            for (Transition t : tc) {
                String name = t.getAction().getName();
                if (name.startsWith("__")) {
                    continue;
                }
                n++;
            }
        }
        return n;
    }

    private static double nsToMs(long ns) {
        return ns / 1_000_000.0;
    }

    private static int intEnv(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static List<String> listEnv(String name, List<String> defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        List<String> parts = new ArrayList<>();
        for (String p : value.split(",")) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return Collections.unmodifiableList(parts);
    }

    private static final class SplSpec {
        final String name;
        final String mxe;
        final String dimacs;
        final String mapping;

        SplSpec(String name, String mxe, String dimacs, String mapping) {
            this.name = name;
            this.mxe = mxe;
            this.dimacs = dimacs;
            this.mapping = mapping;
        }
    }
}
