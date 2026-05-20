# Revision: M0 — Canonical MXE-to-FTS Converter

**Date:** 2026-05-18
**Branch:** `feat/product-test-generation`
**Commit:** (set in the commit that lands this report)
**Trigger:** User reviewed first SVM PNG and pointed out two issues
against Devroey's published canonical FTS:
(1) `soda/s` and `tea/t` transitions duplicated because `free/f` and
`change/!f` were kept as separate FTS states despite having identical
outgoing behaviour, and (2) state names like `v40` (mxGraph vertex IDs)
rather than the conventional sequential `state1, state2, …`.

## Scope

Replace the naive 1-vertex-per-state converter with one that produces
the bisimulation-minimized canonical FTS, with sequential
BFS-order state naming. SVM now matches Devroey's hand-written
reference exactly (9 states, 13 transitions).

## What changed

### `MxeToFtsConverter.buildFts` (replaced)

New pipeline:

1. **Terminal detection** — unchanged (vertex whose outgoing edges all
 go to `]`).
2. **Initial partition** — `INIT_CLASS = {[`,`]`, all terminals}`;
 every other vertex starts in a single "OTHER" class.
3. **Partition refinement** (textbook Paige-Tarjan): for each class,
 compute each member's outgoing signature as the sorted multiset of
 `(action, simplified_fexpr, target_class)`. If members of one class
 disagree on signature, split. Iterate until stable.
4. **Sequential naming** — BFS from the INIT class assigns
 `state1, state2, …` in visitation order.
5. **FTS emission** — one state per class, one transition per distinct
 `(class, action, fexpr, target_class)` outgoing of a representative.
 `FeaturedTransitionSystemFactory.addTransition` already deduplicates
 identical triples within a class.

The reduction is a STANDARD forward bisimulation. The resulting FTS is
behaviourally equivalent to the raw one: it accepts the same set of
feature-valid action sequences and exposes the same product slices
under any configuration. Coverage metrics measured against the reduced
FTS are therefore strictly comparable to the raw-FTS metrics, just on
a smaller denominator.

### State naming

`state1` is always the initial state. `state2..stateN` are assigned in
BFS order starting from `state1`. Outgoing-edge iteration order
follows the source MXE so the naming is deterministic across runs.

### Cardinality changes (post-revision)

| SPL | States (old → new) | Transitions (old → new) |
|---|---|---|
| SVM | 11 → **9** | 18 → **13** |
| eMail | 16 → **11** | 31 → **22** |
| Elevator | 20 → **14** | 70 → **42** |

SVM now matches Devroey's hand-written FTS exactly (the user supplied
that as the reference). The merge pairs in SVM are
`{change/!f, free/f}` (both go to `cancel/soda/tea`) and
`{serveSoda/s, serveTea/t}` (both go to `take/f, open/!f`). Elevator's
much bigger reduction reflects its many press/release symmetries.

### Tests updated

5 assertions had to be refreshed:

- `MxeToFtsConverterTest.convert_svm_producesExpectedCardinalities`:
 11→9 states, 18→13 transitions.
- `MxeToFtsConverterTest.convert_svm_initialStateExists`: initial name
 `"INIT"` → `"state1"`.
- `MxeToFtsConverterTest.convert_emailSpl_producesExpectedCardinalities`:
 16→11, 31→22; initial name update.
- `MxeToFtsConverterTest.convert_elevatorSpl_producesExpectedCardinalities`:
 20→14, 42 transitions; initial name update.
- `HierholzerEulerCycleTest.balancerThenHierholzer_onSvm_producesValidCycle`:
 `cycle.size() > 17` → `> 12` (SVM cycle is shorter post-reduction).

Tests that derive their assertions DYNAMICALLY (M4 / M5 / M6 coverage
generators, mutation operator counts, M7 ExperimentRunner) needed
NO changes — they read the current cardinalities from the FTS each
run, so they tracked the new numbers automatically.

## Verification

| Check | Outcome |
|---|---|
| `mvn -pl vibes-testgeneration test` green | ✓ — `Tests run: 49, Failures: 0, Errors: 0, Skipped: 0` |
| SVM Dot/PNG matches Devroey's canonical structure visually | ✓ |
| All 12 SVM configurations still reach 100% coverage under every coverage criterion | ✓ |
| Pair-coverage suite still covers every reachable pair after reduction | ✓ |

### Phase-0 metrics summary (post-revision)

| Coverage | Mean coverage | Mean suite length | Min / Max | Mean gen time (ms) | Max gen time (ms) |
|---|---|---|---|---|---|
| state | 1.000 | 7.8 | 3 / 17 | 0.661 | 3.591 |
| transition | 1.000 | 7.2 | 4 / 11 | 0.930 | 3.495 |
| pair | 1.000 | 9.4 | 5 / 16 | 0.776 | 2.850 |

Numbers are slightly lower than the pre-revision summary (transition
7.5→7.2, pair 9.8→9.4 mean) because the FTS denominator shrank with
bisimulation reduction, NOT because coverage degraded. All cells
still hit 100% coverage.

## Decisions taken

- **Bisimulation reduction is the default**, not an opt-in flag. The
 raw 1-vertex-per-state output was a structural defect the user
 rejected on inspection; no legacy use case keeps it alive. Removing
 the option avoids two parallel behaviours each requiring its own
 tests.
- **Forward bisimulation only.** We minimize by future behaviour
 (outgoing signature). The tradeoff: incoming behaviour (which
 transition arrived at a state) is forgotten. This matches the
 reference FTS — state 3 in Devroey's canonical SVM is reachable
 both via `change/!f` and `free/f`, and the canonical does NOT
 distinguish them.
- **Sequential state naming, not event-based.** Multiple ESG vertices
 can share an event (e.g. SVM has `take/f` and `take/!f` as
 separate vertices); event-based names would collide. Sequential
 `stateN` is what Devroey uses and is robust.
- **Class identity through ESG vertex IDs internally; sequential names
 only at the FTS surface.** Keeps the partition-refinement
 implementation simple while presenting the canonical names to
 downstream code.

## Open questions / blockers

- **Git push credentials** — unchanged from earlier milestones.

## Files changed

- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/conversion/MxeToFtsConverter.java`
 — `buildFts` replaced; helper methods `detectTerminalVertices`,
 `partitionRefine`, `signatureOf`, `assignSequentialStateNames` added.
- `vibes-testgeneration/src/test/java/be/vibes/testgeneration/conversion/MxeToFtsConverterTest.java`
 — cardinality + initial-state-name assertions updated for all three
 MVP SPLs.
- `vibes-testgeneration/src/test/java/be/vibes/testgeneration/graph/HierholzerEulerCycleTest.java`
 — SVM cycle-length lower bound updated (17 → 12).
- `milestone-reports/figures/SodaVendingMachine.{dot,png}`,
 `eMail.{dot,png}`, `Elevator.{dot,png}` — regenerated.
- `milestone-reports/metrics/phase0-prelim-metrics.csv` — regenerated
 with new (slightly smaller) suite-size numbers.

## Next

Phase 1 work resumes from here: DIMACS generation for the four SPLs
that ship only `model.xml`, mutation-testing harness integration, and
the random baseline. None of those are affected by this revision
beyond reading the new (canonical) FTS as input.
