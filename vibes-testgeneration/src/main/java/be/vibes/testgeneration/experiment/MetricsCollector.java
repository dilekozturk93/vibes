package be.vibes.testgeneration.experiment;

import be.vibes.testgeneration.graph.EulerianBalancer;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.State;
import be.vibes.ts.TestCase;
import be.vibes.ts.Transition;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Per-cell measurements for the Phase-2 experiment harness: coverage
 * percentage of a generated walk / suite against a reference (repaired
 * product-level) FTS, JVM peak memory, and timing primitives.
 *
 * <p>Coverage measurements ignore synthetic balancing transitions, which
 * are not part of the original FTS' state / transition / pair space and
 * would otherwise pollute the denominator.
 *
 * <p>Memory and timing are exposed as static helpers so the harness can
 * sprinkle them around the existing pipeline calls without paying for an
 * object instance per cell.
 */
public final class MetricsCollector {

    private MetricsCollector() {
        // Utility class.
    }

    // ---------- State coverage ----------

    /**
     * Fraction of states in {@code repaired} that are visited by the
     * given walk. The initial state is always considered visited
     * (every walk starts there).
     */
    public static double stateCoveragePercentage(FeaturedTransitionSystem repaired,
                                                 List<Transition> walk) {
        Set<State> visited = new HashSet<>();
        visited.add(repaired.getInitialState());
        for (Transition t : walk) {
            visited.add(t.getSource());
            visited.add(t.getTarget());
        }
        int total = countStates(repaired);
        if (total == 0) {
            return 1.0;
        }
        return (double) Math.min(visited.size(), total) / (double) total;
    }

    /**
     * Suite variant: aggregate state coverage across every test case.
     * (Tests in the suite may start at different states.)
     */
    public static double stateCoveragePercentageOfSuite(FeaturedTransitionSystem repaired,
                                                        List<TestCase> suite) {
        Set<State> visited = new HashSet<>();
        visited.add(repaired.getInitialState());
        for (TestCase tc : suite) {
            for (Transition t : tc) {
                visited.add(t.getSource());
                visited.add(t.getTarget());
            }
        }
        int total = countStates(repaired);
        if (total == 0) {
            return 1.0;
        }
        return (double) Math.min(visited.size(), total) / (double) total;
    }

    // ---------- Transition coverage ----------

    /**
     * Fraction of original (non-synthetic) transitions in {@code repaired}
     * that are present in {@code walk}.
     */
    public static double transitionCoveragePercentage(FeaturedTransitionSystem repaired,
                                                      List<Transition> walk) {
        Set<String> covered = new HashSet<>();
        for (Transition t : walk) {
            if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                continue;
            }
            covered.add(transitionKey(t));
        }
        Set<String> all = nonSyntheticTransitionKeys(repaired);
        if (all.isEmpty()) {
            return 1.0;
        }
        // Limit numerator to transitions of the repaired FTS so a walk that
        // contains transitions from a slightly different graph variant does
        // not inflate the percentage above 1.0.
        int hits = 0;
        for (String key : covered) {
            if (all.contains(key)) {
                hits++;
            }
        }
        return (double) hits / (double) all.size();
    }

    public static double transitionCoveragePercentageOfSuite(FeaturedTransitionSystem repaired,
                                                             List<TestCase> suite) {
        Set<String> covered = new HashSet<>();
        for (TestCase tc : suite) {
            for (Transition t : tc) {
                if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                    continue;
                }
                covered.add(transitionKey(t));
            }
        }
        Set<String> all = nonSyntheticTransitionKeys(repaired);
        if (all.isEmpty()) {
            return 1.0;
        }
        int hits = 0;
        for (String key : covered) {
            if (all.contains(key)) {
                hits++;
            }
        }
        return (double) hits / (double) all.size();
    }

    // ---------- Pair coverage ----------

    /**
     * Fraction of reachable transition pairs (every {@code (t1, t2)} with
     * {@code target(t1) == source(t2)} in {@code repaired}) that appear
     * consecutively in some test case of the given suite.
     */
    public static double pairCoveragePercentageOfSuite(FeaturedTransitionSystem repaired,
                                                       List<TestCase> suite) {
        Set<String> covered = new HashSet<>();
        for (TestCase tc : suite) {
            List<Transition> walk = new java.util.ArrayList<>();
            for (Transition t : tc) {
                if (EulerianBalancer.isSyntheticAction(t.getAction())) {
                    continue;
                }
                walk.add(t);
            }
            for (int i = 0; i + 1 < walk.size(); i++) {
                covered.add(pairKey(walk.get(i), walk.get(i + 1)));
            }
        }
        Set<String> all = reachablePairKeys(repaired);
        if (all.isEmpty()) {
            return 1.0;
        }
        int hits = 0;
        for (String key : covered) {
            if (all.contains(key)) {
                hits++;
            }
        }
        return (double) hits / (double) all.size();
    }

    /**
     * Pair-coverage variant for a single walk (no segmentation), useful
     * when the generator under test returns one continuous test case.
     */
    public static double pairCoveragePercentage(FeaturedTransitionSystem repaired,
                                                List<Transition> walk) {
        Set<String> covered = new HashSet<>();
        List<Transition> realOnly = new java.util.ArrayList<>();
        for (Transition t : walk) {
            if (!EulerianBalancer.isSyntheticAction(t.getAction())) {
                realOnly.add(t);
            }
        }
        for (int i = 0; i + 1 < realOnly.size(); i++) {
            covered.add(pairKey(realOnly.get(i), realOnly.get(i + 1)));
        }
        Set<String> all = reachablePairKeys(repaired);
        if (all.isEmpty()) {
            return 1.0;
        }
        int hits = 0;
        for (String key : covered) {
            if (all.contains(key)) {
                hits++;
            }
        }
        return (double) hits / (double) all.size();
    }

    // ---------- Suite size ----------

    /**
     * Total non-synthetic transition count across every test case in a
     * suite.
     */
    public static int totalSuiteTransitions(List<TestCase> suite) {
        int n = 0;
        for (TestCase tc : suite) {
            for (Transition t : tc) {
                if (!EulerianBalancer.isSyntheticAction(t.getAction())) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Walk variant: total non-synthetic transition count.
     */
    public static int totalWalkTransitions(List<Transition> walk) {
        int n = 0;
        for (Transition t : walk) {
            if (!EulerianBalancer.isSyntheticAction(t.getAction())) {
                n++;
            }
        }
        return n;
    }

    // ---------- Memory ----------

    /**
     * Peak JVM heap usage in bytes since the last GC, useful as a cheap
     * proxy for per-cell memory cost. The harness calls {@link System#gc()}
     * before starting a cell so the baseline is stable; this returns the
     * delta-style maximum observed during a cell.
     *
     * <p>{@link Runtime#freeMemory} is a snapshot; consumers should sample
     * it repeatedly during the cell and keep the maximum, as done in
     * {@link ExperimentRunner}.
     */
    public static long usedHeapBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    public static double bytesToMb(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    // ---------- Peak heap (MXBean) ----------

    /**
     * Resets the per-memory-pool "peak usage" counter on every HEAP pool.
     * Call this immediately BEFORE the measured phase begins. The matching
     * read is {@link #peakHeapBytes()}, called immediately after the phase
     * ends — the difference between the two snapshots is the peak HEAP
     * occupancy attributable to the phase.
     *
     * <p>This is the same pattern used by the user's ESG-Fx-side
     * {@code AbstractTestPipeline.resetPeakMemoryCounters} (see
     * {@code esg-with-feature-expressions}). The MXBean peak counter
     * survives GC events that {@code Runtime.totalMemory − freeMemory}
     * would erase, so it is the correct primitive for per-phase
     * "high-water-mark" memory.
     */
    public static void resetPeakHeap() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP) {
                pool.resetPeakUsage();
            }
        }
    }

    /**
     * Reads the cumulative peak HEAP occupancy in bytes across all heap
     * pools since the most recent {@link #resetPeakHeap()} call. Returns
     * the sum of {@code getPeakUsage().getUsed()} across pools.
     */
    public static long peakHeapBytes() {
        long total = 0L;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP) {
                MemoryUsage peak = pool.getPeakUsage();
                if (peak != null) {
                    total += peak.getUsed();
                }
            }
        }
        return total;
    }

    /** Convenience: peak HEAP in megabytes since last {@link #resetPeakHeap()}. */
    public static double peakHeapMb() {
        return bytesToMb(peakHeapBytes());
    }

    // ---------- internal helpers ----------

    private static int countStates(FeaturedTransitionSystem fts) {
        int n = 0;
        Iterator<State> it = fts.states();
        while (it.hasNext()) {
            it.next();
            n++;
        }
        return n;
    }

    private static Set<String> nonSyntheticTransitionKeys(FeaturedTransitionSystem fts) {
        Set<String> set = new HashSet<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            Transition t = it.next();
            if (!EulerianBalancer.isSyntheticAction(t.getAction())) {
                set.add(transitionKey(t));
            }
        }
        return set;
    }

    private static Set<String> reachablePairKeys(FeaturedTransitionSystem fts) {
        Set<String> set = new HashSet<>();
        java.util.List<Transition> all = new java.util.ArrayList<>();
        Iterator<Transition> it = fts.transitions();
        while (it.hasNext()) {
            Transition t = it.next();
            if (!EulerianBalancer.isSyntheticAction(t.getAction())) {
                all.add(t);
            }
        }
        for (Transition t1 : all) {
            for (Transition t2 : all) {
                if (t1.getTarget().equals(t2.getSource())) {
                    set.add(pairKey(t1, t2));
                }
            }
        }
        return set;
    }

    private static String transitionKey(Transition t) {
        return t.getSource().getName() + "/" + t.getAction().getName()
                + "/" + t.getTarget().getName();
    }

    private static String pairKey(Transition a, Transition b) {
        return transitionKey(a) + ">>" + transitionKey(b);
    }
}
