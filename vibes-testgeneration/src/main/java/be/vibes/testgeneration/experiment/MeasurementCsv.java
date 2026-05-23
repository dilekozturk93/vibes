package be.vibes.testgeneration.experiment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * CSV writers for RQ1 (scalability), RQ2 (mutation score), and RQ3
 * (efficiency). One CSV per concern; each writer appends a single row per
 * call and writes the header on first call against an empty file.
 *
 * <p>The on-disk format mirrors the user's ESG-Fx-side
 * {@code TestPipelineMeasurementWriter_*} writers exactly so the existing
 * Python aggregation pipeline can consume both bodies of work:
 * <ul>
 *   <li>field separator {@code ;};</li>
 *   <li>decimal separator {@code ,} via
 *       {@link DecimalFormatSymbols} pinned to {@link Locale#ROOT};</li>
 *   <li>{@link DecimalFormat} pattern {@code "#.##"} for all
 *       {@code double} fields;</li>
 *   <li>append-on-first-write — if the destination file is empty, write
 *       the header and the data row; otherwise append the data row only.</li>
 * </ul>
 *
 * <p>Domain terminology is FTS-native ("FTS States", "FTS Transitions"),
 * not ESG-Fx ("Vertices", "Edges"): the CSV format mechanics match the
 * reference, the column names match the model formalism used in this
 * project.
 *
 * <p>Each method is independently callable so per-row writes never share
 * a {@link BufferedWriter} between RQ files — keeps locking simple under
 * concurrent shard execution.
 */
public final class MeasurementCsv {

    private MeasurementCsv() {
        // Utility class.
    }

    /** Comma-decimal {@link DecimalFormat} matching the ESG-Fx writers. */
    private static DecimalFormat commaDecimalFormatter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#.##", symbols);
    }

    /** Append {@code row} to {@code file}; if the file is empty, write {@code header} first. */
    private static void appendRow(File file, String header, String row) throws IOException {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0L) {
                writer.write(header);
            }
            writer.write(row);
        }
    }

    // ====================================================================
    // RQ1 — coverage-directed (11-run, append, RunID-keyed)
    // ====================================================================

    /**
     * One row per (SPL × product × coverage criterion × runID). Records
     * the disjoint timing segments (projection, generation,
     * transformation, execution), per-phase peak memory, suite shape,
     * coverage achieved, and the wall-clock sentinel so post-hoc analysis
     * can verify {@code segmentSum ≈ wallClock} per row.
     *
     * <p>Coverage types: {@code state}, {@code transition}, {@code pair}.
     * Family-level baseline scalability has its own
     * {@link #appendRq1FamilyBaselineRow} call so its column shape
     * (no per-product fields) does not pollute this one.
     *
     * <p>Time columns are disjoint by construction (see
     * {@code TransitionPairCoverageGenerator.Timings} and the harness):
     * for {@code state} and {@code transition}, transformationTimeMs is
     * always 0; for {@code pair}, testGenTimeMs excludes
     * transformationTimeMs (i.e. it is pair-graph-construction only),
     * so {@code projection + testGen + transformation + execution} adds
     * each phase exactly once.
     */
    public static void appendRq1CoverageDirectedRow(File file,
                                                    int runId, String splName, String coverageType,
                                                    int productIndex,
                                                    int ftsStates, int ftsTransitions,
                                                    int testCaseCount, int totalRealActions,
                                                    double coveragePct,
                                                    double projectionTimeMs,
                                                    double testGenTimeMs,
                                                    double transformationTimeMs,
                                                    double testExecutionTimeMs,
                                                    double testGenPeakMemoryMb,
                                                    double testExecutionPeakMemoryMb,
                                                    double wallClockTimeMs) throws IOException {
        DecimalFormat df = commaDecimalFormatter();
        double segmentSumMs = projectionTimeMs + testGenTimeMs
                + transformationTimeMs + testExecutionTimeMs;
        String header = "RunID;SPL Name;Coverage Type;Product Index;FTS States;FTS Transitions;"
                + "Test Cases;Total Test Actions;Coverage(%);"
                + "Projection Time(ms);Test Generation Time(ms);Transformation Time(ms);"
                + "Test Execution Time(ms);Test Generation Peak Memory(MB);"
                + "Test Execution Peak Memory(MB);Wall Clock Time(ms);Segment Sum Time(ms)\n";
        String row = runId + ";" + splName + ";" + coverageType + ";" + productIndex + ";"
                + ftsStates + ";" + ftsTransitions + ";"
                + testCaseCount + ";" + totalRealActions + ";"
                + df.format(coveragePct) + ";"
                + df.format(projectionTimeMs) + ";" + df.format(testGenTimeMs) + ";"
                + df.format(transformationTimeMs) + ";" + df.format(testExecutionTimeMs) + ";"
                + df.format(testGenPeakMemoryMb) + ";" + df.format(testExecutionPeakMemoryMb) + ";"
                + df.format(wallClockTimeMs) + ";" + df.format(segmentSumMs) + "\n";
        appendRow(file, header, row);
    }

    /**
     * One row per (SPL × runID) — family-level baseline (Devroey AllStates
     * generated once for the SPL FTS). Captures generation time + suite
     * size + whether the run hit a wall-clock / OOM ceiling so RQ1 can
     * report scalability ceilings explicitly.
     */
    public static void appendRq1FamilyBaselineRow(File file,
                                                  int runId, String splName,
                                                  int ftsStates, int ftsTransitions,
                                                  int familyTestCaseCount,
                                                  int familyTotalRealActions,
                                                  double generationTimeMs,
                                                  double generationPeakMemoryMb,
                                                  double wallClockTimeMs,
                                                  String terminationReason) throws IOException {
        DecimalFormat df = commaDecimalFormatter();
        String header = "RunID;SPL Name;FTS States;FTS Transitions;"
                + "Family Test Cases;Family Total Actions;"
                + "Generation Time(ms);Generation Peak Memory(MB);"
                + "Wall Clock Time(ms);Termination Reason\n";
        String row = runId + ";" + splName + ";" + ftsStates + ";" + ftsTransitions + ";"
                + familyTestCaseCount + ";" + familyTotalRealActions + ";"
                + df.format(generationTimeMs) + ";" + df.format(generationPeakMemoryMb) + ";"
                + df.format(wallClockTimeMs) + ";" + terminationReason + "\n";
        appendRow(file, header, row);
    }

    // ====================================================================
    // RQ2 — mutation score (single-run, append-safe, deterministic)
    // ====================================================================

    /**
     * One row per (SPL × product × coverage type × operator). Records the
     * mutation score of a single coverage-directed suite (state /
     * transition / pair) or the family baseline (projected onto the
     * product) against the operator's mutant set.
     *
     * <p>Random baseline uses its own writer — its 100-seed aggregation
     * needs distinct columns (median / IQR / spread).
     */
    public static void appendRq2CoverageDirectedRow(File file,
                                                    int runId, String splName,
                                                    int productIndex, String coverageType,
                                                    String operator,
                                                    int totalMutants, int equivalentCount,
                                                    int killedCount, int survivedCount,
                                                    double mutationScorePct) throws IOException {
        DecimalFormat df = commaDecimalFormatter();
        String header = "RunID;SPL Name;Product Index;Coverage Type;Operator;"
                + "Total Mutants;Equivalent Count;Killed Count;Survived Count;"
                + "Mutation Score(%)\n";
        String row = runId + ";" + splName + ";" + productIndex + ";" + coverageType + ";"
                + operator + ";" + totalMutants + ";" + equivalentCount + ";"
                + killedCount + ";" + survivedCount + ";"
                + df.format(mutationScorePct) + "\n";
        appendRow(file, header, row);
    }

    /**
     * One row per (SPL × product × coverage budget source × operator) —
     * the {@code coverageBudgetSource} is the coverage type whose suite's
     * total-action-count was used as the random-walk action budget
     * (per-coverage-level matching, paper-fair). 100 seeds (configurable);
     * row reports median + quartile spread + observed extremes of the
     * mutation score distribution as well as the aborted-walk counter
     * (analogue of the ESG-Fx-side "Safety Limit Hit" metric).
     */
    public static void appendRq2RandomBaselineRow(File file,
                                                  int runId, String splName,
                                                  int productIndex, String coverageBudgetSource,
                                                  String operator,
                                                  int totalMutants, int equivalentCount,
                                                  int actionBudget, int maxStepsPerCase,
                                                  int seedCount, int abortedWalksTotal,
                                                  int killedMin, int killedP25, int killedMedian,
                                                  int killedP75, int killedMax,
                                                  double mutationScoreMinPct, double mutationScoreP25Pct,
                                                  double mutationScoreMedianPct,
                                                  double mutationScoreP75Pct, double mutationScoreMaxPct) throws IOException {
        DecimalFormat df = commaDecimalFormatter();
        String header = "RunID;SPL Name;Product Index;Coverage Budget Source;Operator;"
                + "Total Mutants;Equivalent Count;Action Budget;Max Steps Per Case;"
                + "Seed Count;Aborted Walks Total;"
                + "Killed Min;Killed P25;Killed Median;Killed P75;Killed Max;"
                + "Mutation Score Min(%);Mutation Score P25(%);Mutation Score Median(%);"
                + "Mutation Score P75(%);Mutation Score Max(%)\n";
        String row = runId + ";" + splName + ";" + productIndex + ";" + coverageBudgetSource + ";"
                + operator + ";" + totalMutants + ";" + equivalentCount + ";"
                + actionBudget + ";" + maxStepsPerCase + ";" + seedCount + ";" + abortedWalksTotal + ";"
                + killedMin + ";" + killedP25 + ";" + killedMedian + ";" + killedP75 + ";" + killedMax + ";"
                + df.format(mutationScoreMinPct) + ";" + df.format(mutationScoreP25Pct) + ";"
                + df.format(mutationScoreMedianPct) + ";"
                + df.format(mutationScoreP75Pct) + ";" + df.format(mutationScoreMaxPct) + "\n";
        appendRow(file, header, row);
    }

    // ====================================================================
    // RQ3 — efficiency (single-run, append-safe, deterministic)
    // ====================================================================

    /**
     * One row per (SPL × product × coverage type × operator) — RQ3
     * efficiency = killed-non-equivalent mutants / total real transitions
     * executed by the suite. Numerator from FaultDetector; denominator
     * from TestExecution.executeSuite.
     */
    public static void appendRq3EfficiencyRow(File file,
                                              int runId, String splName,
                                              int productIndex, String coverageType,
                                              String operator,
                                              int totalMutants, int equivalentCount,
                                              int killedCount, int survivedCount,
                                              double mutationScorePct,
                                              long totalTransitionsExecuted,
                                              double efficiencyKilledPerTransition) throws IOException {
        DecimalFormat df = commaDecimalFormatter();
        String header = "RunID;SPL Name;Product Index;Coverage Type;Operator;"
                + "Total Mutants;Equivalent Count;Killed Count;Survived Count;"
                + "Mutation Score(%);Total Transitions Executed;Efficiency (killed/transition)\n";
        String row = runId + ";" + splName + ";" + productIndex + ";" + coverageType + ";"
                + operator + ";" + totalMutants + ";" + equivalentCount + ";"
                + killedCount + ";" + survivedCount + ";"
                + df.format(mutationScorePct) + ";"
                + totalTransitionsExecuted + ";"
                + df.format(efficiencyKilledPerTransition) + "\n";
        appendRow(file, header, row);
    }
}
