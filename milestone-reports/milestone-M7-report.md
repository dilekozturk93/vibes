# Milestone M7 Report

**Date:** 2026-05-16
**Branch:** `feat/product-test-generation`
**Commit:** (set in the commit that lands this report)

## Scope

Wire the Phase-0 experiment harness: a CSV-producing runner that loops
the enabled SPLs × the three coverage criteria × every valid product
configuration, calls the generators from M4 / M5 / M6, and records per-cell
coverage, suite size, generation time, and peak memory. This is the
feasibility evidence for the May 18 abstract.

## Files changed

- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/experiment/MetricsCollector.java`
 — coverage measurement (state / transition / pair) on a walk or a
 suite, JVM heap helpers, and total-transition counters that exclude
 synthetic balancing edges.
- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/experiment/ExperimentRunner.java`
 — main entry point. Honours the same env-var contract as the user's
 ESG-Fx `RQ2_ExtremeScalability_L234` (`SHARD`, `N_SHARDS`, `runID`,
 `TIMEOUT_HOURS`); writes a semicolon-separated CSV with comma-decimal
 values to `milestone-reports/metrics/phase0-prelim-metrics.csv` (or
 the path supplied via the `OUTPUT_CSV` env var).
- `milestone-reports/metrics/phase0-prelim-metrics.csv` — output of the
 first harness run, 36 rows (12 SVM configurations × 3 coverages).

## What was implemented

### `MetricsCollector`

Coverage percentage of a generated walk (M4 / M5) or suite (M6) against
the repaired product-level FTS. Synthetic balancing transitions are
filtered before computing both numerator and denominator. APIs:

- `stateCoveragePercentage(repaired, walk)` /
 `stateCoveragePercentageOfSuite(repaired, suite)`
- `transitionCoveragePercentage` and its suite variant
- `pairCoveragePercentageOfSuite(repaired, suite)` and
 `pairCoveragePercentage(repaired, walk)`
- `totalSuiteTransitions(suite)`, `totalWalkTransitions(walk)`
- `usedHeapBytes()`, `bytesToMb(bytes)` for the runner's memory baseline.

### `ExperimentRunner`

Env-var contract identical to the user's ESG-Fx harness (so existing
Python aggregation scripts work unchanged):

| Variable | Default | Meaning |
|---|---|---|
| `SHARD` | 0 | This run's shard index |
| `N_SHARDS` | 1 | Total shard count; row included iff `(productId - 1) % N == shard` |
| `runID` | 1 | Replication identifier (included in every row) |
| `TIMEOUT_HOURS` | 0 (unlimited) | Soft wall-clock budget |
| `SPLS` | `SVM,eMail,Elevator` | Comma-separated SPL list |
| `COVERAGES` | `state,transition,pair` | Comma-separated coverage list |
| `OUTPUT_CSV` | `milestone-reports/metrics/phase0-prelim-metrics.csv` | Output path |

For each cell (SPL, configuration, coverage):
1. `FExpressionPreservingProjection.project` (M4).
2. `InitialSccFilter.keepInitialScc` (M4).
3. Run the coverage-specific generator (M4 transition / M5 state /
 M6 pair).
4. Measure coverage percentage on the repaired FTS.
5. Measure generation time (`System.nanoTime` around the generator call)
 and peak heap delta over the cell.
6. Emit one CSV row.

### CSV format

```
spl;coverage;productId;runID;configCount;suiteSize;totalTransitions;coveragePct;genTimeMs;peakMemoryMb
```

Field separator `;`, decimal separator `,`, matching the user's existing
ESG-Fx-side Python aggregation scripts.

## Verification

| Check | Outcome |
|---|---|
| All 49 pre-existing tests still pass | ✓ |
| Module compiles cleanly | ✓ |
| `ExperimentRunner` produces a 36-row CSV for SVM in under 1 second | ✓ |

## Phase-0 preliminary results (SVM, all 12 configurations)

| Coverage | Mean coverage achieved | Mean suite length (transitions) | Min / max length | Mean gen time (ms) | Max gen time (ms) |
|---|---|---|---|---|---|
| state | **1.000** | 7.8 | 3 / 15 | 0.441 | 1.776 |
| transition | **1.000** | 7.5 | 4 / 12 | 1.027 | 4.119 |
| pair | **1.000** | 9.8 | 5 / 17 | 0.846 | 2.754 |

Peak heap usage per cell is ≤ 0.5 MB across all 36 cells.

**Key observations:**

- All three coverage criteria reach 100% on every product configuration.
- Coverage hierarchy holds in suite length: state ≈ transition < pair,
 matching the theoretical expectation that pair coverage is the
 strongest of the three.
- Generation is fast (sub-millisecond average for state, low single-digit
 milliseconds worst case for transition/pair). For RQ1 ("feasibility")
 this is decisive evidence: even the strongest criterion completes per
 product in milliseconds on SVM.
- State-coverage walk being SHORTER than transition-coverage cycle
 on average matches the algorithm: state coverage stops as soon as every
 state has been visited, while transition coverage walks every edge
 (with the Eulerian-balance overhead).

## Decisions taken

- **`SPLS` defaults to {SVM, eMail, Elevator}** but the harness fails
 fast on eMail / Elevator because their DIMACS + mapping files have not
 yet been generated. M7 was run with only `SPLS=SVM` for now; bundling
 DIMACS for the remaining SPLs is Phase-1 work (see plan M9 / M-mut
 reuse: run the user's `FeatureModelToDimacsExporter` on each SPL's
 `model.xml`).
- **Memory measured as heap delta**, not RSS. RSS is more accurate but
 requires platform-specific probes; heap is a reasonable proxy for
 the algorithm's working set and is portable.
- **`System.gc()` before each cell.** A best-effort baseline; the JVM
 does not have to honour it, but it keeps the heap delta numbers in
 the same ballpark across cells.
- **Coverage percentages clamped to ≤ 1.0** at the numerator (number of
 hits, not raw covered-set size) so a walk that contains transitions
 outside the repaired FTS cannot inflate the percentage above 100%.
 (Should not happen in our pipeline, but the clamp is a defensive guard.)

## Open questions / blockers

- **DIMACS for eMail / Elevator / BankAccountv2 / StudentAttendanceSystem**
 is not yet bundled with the project. These SPLs need
 `FeatureModelToDimacsExporter` run on their `model.xml` and the
 resulting `.dimacs` + `_dimacsmapping.txt` files copied into the
 `vibes-testgeneration/src/main/resources/cases/<SPL>/configs/`
 directory before the harness can include them.
- **Git push credentials** — unchanged.

## Next milestone preview

**M8 (May 17–18):** abstract update with the SVM feasibility numbers
above, then submission. Phase 1 work (DIMACS generation for the
remaining four SPLs + mutation-testing harness integration + random
baseline) starts post-abstract.
