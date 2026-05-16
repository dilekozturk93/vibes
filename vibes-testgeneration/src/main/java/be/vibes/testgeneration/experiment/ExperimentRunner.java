package be.vibes.testgeneration.experiment;

import be.vibes.fexpression.DimacsModel;
import be.vibes.fexpression.configuration.Configuration;
import be.vibes.solver.Sat4JSolverFacade;
import be.vibes.testgeneration.conversion.MxeToFtsConverter;
import be.vibes.testgeneration.coverage.StateCoverageGenerator;
import be.vibes.testgeneration.coverage.TransitionCoverageGenerator;
import be.vibes.testgeneration.coverage.TransitionPairCoverageGenerator;
import be.vibes.testgeneration.graph.InitialSccFilter;
import be.vibes.testgeneration.product.FExpressionPreservingProjection;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Phase-0 experiment harness. Loops every enabled SPL × every coverage
 * criterion × every valid product configuration, generates the test
 * suite, measures coverage / size / time / memory, and emits one CSV row
 * per cell to {@code automode-reports/metrics/phase0-prelim-metrics.csv}
 * (or a path provided via {@code OUTPUT_CSV} env var).
 *
 * <p>Honours the same env-var contract as the user's ESG-Fx-side
 * {@code RQ2_ExtremeScalability_L234} for compatibility with the existing
 * Python aggregation pipeline:
 *
 * <ul>
 *   <li>{@code SHARD} / {@code N_SHARDS} — split configurations across
 *       shards using {@code (configId - 1) % N_SHARDS == SHARD}.</li>
 *   <li>{@code runID} — replication identifier; included in every row.</li>
 *   <li>{@code TIMEOUT_HOURS} — soft wall-clock limit; the harness breaks
 *       out of the loop once exceeded.</li>
 *   <li>{@code SPLS} — comma-separated SPL names to include; defaults to
 *       {@code SVM,eMail,Elevator}.</li>
 *   <li>{@code COVERAGES} — comma-separated coverage names from the
 *       fixed set {@code state}, {@code transition}, {@code pair};
 *       defaults to all three.</li>
 *   <li>{@code OUTPUT_CSV} — destination path; defaults to
 *       {@code automode-reports/metrics/phase0-prelim-metrics.csv}.</li>
 * </ul>
 *
 * <p>The CSV uses {@code ;} as the field separator and {@code ,} as the
 * decimal separator inside numeric values, matching the user's existing
 * downstream Python scripts.
 */
public final class ExperimentRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ExperimentRunner.class);

    private static final String DEFAULT_OUTPUT = "automode-reports/metrics/phase0-prelim-metrics.csv";
    private static final List<String> DEFAULT_SPLS = Arrays.asList("SVM", "eMail", "Elevator");
    private static final List<String> DEFAULT_COVERAGES = Arrays.asList("state", "transition", "pair");

    private static final String CSV_HEADER =
            "spl;coverage;productId;runID;configCount;suiteSize;totalTransitions;coveragePct;genTimeMs;peakMemoryMb";

    private ExperimentRunner() {
        // Entry point only.
    }

    public static void main(String[] args) throws Exception {
        int shard = intEnv("SHARD", 0);
        int nShards = intEnv("N_SHARDS", 1);
        int runId = intEnv("runID", 1);
        int timeoutHours = intEnv("TIMEOUT_HOURS", 0);
        long timeoutNanos = timeoutHours > 0
                ? timeoutHours * 60L * 60L * 1_000_000_000L
                : Long.MAX_VALUE;

        List<String> spls = listEnv("SPLS", DEFAULT_SPLS);
        List<String> coverages = listEnv("COVERAGES", DEFAULT_COVERAGES);
        Path output = Paths.get(System.getenv().getOrDefault("OUTPUT_CSV", DEFAULT_OUTPUT));

        Files.createDirectories(output.getParent());
        long start = System.nanoTime();
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(output))) {
            csv.println(CSV_HEADER);
            for (String spl : spls) {
                if (System.nanoTime() - start > timeoutNanos) {
                    LOG.warn("Timeout reached before SPL {} could be processed", spl);
                    break;
                }
                runSpl(spl, coverages, shard, nShards, runId, csv);
                csv.flush();
            }
        }
        LOG.info("Wrote phase-0 metrics to {}", output.toAbsolutePath());
    }

    private static void runSpl(String spl, List<String> coverages,
                               int shard, int nShards, int runId,
                               PrintWriter csv) throws Exception {
        SplResources resources = SplResources.forName(spl);
        FeaturedTransitionSystem fts = loadFts(resources.mxeResource);
        Sat4JSolverFacade solver = loadSolver(resources.dimacsResource, resources.mappingResource);

        int productIndex = 0;
        Iterator<Configuration> configs = solver.getSolutions();
        while (configs.hasNext()) {
            Configuration config = configs.next();
            productIndex++;
            if ((productIndex - 1) % nShards != shard) {
                continue;
            }
            for (String coverage : coverages) {
                Result r = runCell(fts, config, spl, coverage, productIndex);
                csv.println(formatRow(spl, coverage, productIndex, runId, r));
            }
        }
    }

    /**
     * Generates a test suite for one (FTS, product config, coverage) cell
     * and measures its size, coverage, generation time, and peak memory.
     */
    private static Result runCell(FeaturedTransitionSystem fts,
                                  Configuration config,
                                  String spl,
                                  String coverage,
                                  int productIndex) {
        FeaturedTransitionSystem projected = FExpressionPreservingProjection.project(fts, config);
        FeaturedTransitionSystem repaired = InitialSccFilter.keepInitialScc(projected);

        String testId = spl + "_p" + productIndex + "_" + coverage;

        // Memory baseline (best-effort; not a hard sandbox).
        System.gc();
        long memoryBefore = MetricsCollector.usedHeapBytes();
        long peak = memoryBefore;

        long t0 = System.nanoTime();
        Result r = new Result();
        switch (coverage) {
            case "state": {
                TestCase tc = StateCoverageGenerator.generate(fts, config, testId);
                long t1 = System.nanoTime();
                peak = Math.max(peak, MetricsCollector.usedHeapBytes());
                List<Transition> walk = toList(tc);
                r.suiteSize = 1;
                r.totalTransitions = MetricsCollector.totalWalkTransitions(walk);
                r.coveragePct = MetricsCollector.stateCoveragePercentage(repaired, walk);
                r.genTimeMs = nsToMs(t1 - t0);
                break;
            }
            case "transition": {
                TestCase tc = TransitionCoverageGenerator.generate(fts, config, testId);
                long t1 = System.nanoTime();
                peak = Math.max(peak, MetricsCollector.usedHeapBytes());
                List<Transition> walk = toList(tc);
                r.suiteSize = 1;
                r.totalTransitions = MetricsCollector.totalWalkTransitions(walk);
                r.coveragePct = MetricsCollector.transitionCoveragePercentage(repaired, walk);
                r.genTimeMs = nsToMs(t1 - t0);
                break;
            }
            case "pair": {
                List<TestCase> suite = TransitionPairCoverageGenerator.generate(fts, config, testId);
                long t1 = System.nanoTime();
                peak = Math.max(peak, MetricsCollector.usedHeapBytes());
                r.suiteSize = suite.size();
                r.totalTransitions = MetricsCollector.totalSuiteTransitions(suite);
                r.coveragePct = MetricsCollector.pairCoveragePercentageOfSuite(repaired, suite);
                r.genTimeMs = nsToMs(t1 - t0);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown coverage criterion: " + coverage);
        }
        r.peakMemoryMb = MetricsCollector.bytesToMb(Math.max(0, peak - memoryBefore));
        return r;
    }

    // ---------- formatting + env helpers ----------

    private static String formatRow(String spl, String coverage, int productId, int runId, Result r) {
        // Decimal separator: comma. Field separator: semicolon.
        return spl + ";" + coverage + ";" + productId + ";" + runId + ";"
                + 1 + ";" // configCount = 1 per row (one row per product/coverage)
                + r.suiteSize + ";"
                + r.totalTransitions + ";"
                + commaDecimal(r.coveragePct) + ";"
                + commaDecimal(r.genTimeMs) + ";"
                + commaDecimal(r.peakMemoryMb);
    }

    private static String commaDecimal(double v) {
        return String.format(Locale.US, "%.4f", v).replace('.', ',');
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

    // ---------- resource loading ----------

    private static FeaturedTransitionSystem loadFts(String resource) throws Exception {
        URL url = ExperimentRunner.class.getClassLoader().getResource(resource);
        if (url == null) {
            throw new IOException("Resource not found on classpath: " + resource);
        }
        return new MxeToFtsConverter().convert(new File(url.toURI()));
    }

    private static Sat4JSolverFacade loadSolver(String dimacsResource, String mappingResource) throws Exception {
        URL dimacsUrl = ExperimentRunner.class.getClassLoader().getResource(dimacsResource);
        URL mappingUrl = ExperimentRunner.class.getClassLoader().getResource(mappingResource);
        if (dimacsUrl == null || mappingUrl == null) {
            throw new IOException("DIMACS or mapping resource missing for " + dimacsResource);
        }
        DimacsModel model = DimacsModel.createFromTvlParserGeneratedFiles(
                new File(mappingUrl.toURI()), new File(dimacsUrl.toURI()));
        return new Sat4JSolverFacade(model);
    }

    private static List<Transition> toList(TestCase tc) {
        List<Transition> list = new ArrayList<>();
        for (Transition t : tc) {
            list.add(t);
        }
        return list;
    }

    // ---------- per-row data class ----------

    private static final class Result {
        int suiteSize;
        int totalTransitions;
        double coveragePct;
        double genTimeMs;
        double peakMemoryMb;
    }

    /**
     * Resource locator. M7 only has SVM bundled with DIMACS + mapping; the
     * other two MVP SPLs ship only MXE so far, so attempting to run them
     * via this harness raises a clear error until Phase 1 adds their
     * DIMACS files.
     */
    private static final class SplResources {
        final String mxeResource;
        final String dimacsResource;
        final String mappingResource;

        SplResources(String mxe, String dimacs, String mapping) {
            this.mxeResource = mxe;
            this.dimacsResource = dimacs;
            this.mappingResource = mapping;
        }

        static SplResources forName(String spl) {
            switch (spl) {
                case "SVM":
                    return new SplResources(
                            "cases/SodaVendingMachine/SVM_ESGFx.mxe",
                            "cases/SodaVendingMachine/configs/SVM.dimacs",
                            "cases/SodaVendingMachine/configs/SVM_dimacsmapping.txt");
                case "eMail":
                    return new SplResources(
                            "cases/eMail/eM_ESGFx.mxe",
                            "cases/eMail/configs/eM.dimacs",
                            "cases/eMail/configs/eM_dimacsmapping.txt");
                case "Elevator":
                    return new SplResources(
                            "cases/Elevator/El_ESGFx.mxe",
                            "cases/Elevator/configs/El.dimacs",
                            "cases/Elevator/configs/El_dimacsmapping.txt");
                default:
                    throw new IllegalArgumentException("Unknown SPL: " + spl);
            }
        }
    }
}
