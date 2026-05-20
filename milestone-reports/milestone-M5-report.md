# Milestone M5 Report

**Date:** 2026-05-15
**Branch:** `feat/product-test-generation`
**Commit:** (set in the commit that lands this report)

## Scope

Add the second coverage criterion: all-states (state coverage). Unlike
all-transitions (M4), state coverage requires every state of the product
FTS to be visited at least once but does NOT require every transition to
be used. The expected outcome is shorter test cases — a key data point
for RQ3 ("does increasing coverage level improve fault detection
efficiency?").

## Files changed

- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/ShortestPaths.java`
 — BFS-based shortest-path helper with one public entry point,
 `shortestPathToAny(ts, source, targets)`, returning either the path
 as a `List<Transition>` or `null` if no target is reachable.
- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/StateCoverageGenerator.java`
 — orchestrator: project → repair → greedy walk with BFS reroute on
 "stuck" → wrap as `TestCase`.
- `vibes-testgeneration/src/test/java/be/vibes/testgeneration/coverage/StateCoverageGeneratorTest.java`
 — 7 tests: BFS primitives (zero-length, nearest-target, unreachable),
 walk algorithm (toy graphs), SVM end-to-end across 12 configurations,
 and a length-comparison vs. transition coverage.

## What was implemented

### `ShortestPaths`

BFS with predecessor tracking; treats all transitions as unit-weight
edges. The interesting choice was to query against a *set* of targets
(not a single state) so the caller does not have to BFS-once-per-state
when looking for "any uncovered state from here". That's the structural
need of `StateCoverageGenerator` and matches how the ESG-Fx-side
`EulerCycleGeneratorForEventCoverage` uses Dijkstra reactively.

`shortestPathToAny` returns:
- `[]` (empty) when the source is itself in the target set
- `List<Transition>` of the shortest path otherwise
- `null` when no target is reachable from the source

### `StateCoverageGenerator`

Algorithm (the body of `computeStateCoverageWalk`):

1. `visited = {initial}`, `remaining = states - {initial}`, `walk = []`.
2. While `remaining` is non-empty:
 - Look at the outgoing transitions of the current state. Pick the
 first one whose target is unvisited. Append, advance, mark
 visited.
 - If no outgoing edge leads to an unvisited state: call
 `ShortestPaths.shortestPathToAny(fts, current, remaining)`,
 append the entire path, and mark every state along it as visited.
3. Wrap the walk into a `TestCase` via `enqueueAll`.

Pre-pipeline (same as `TransitionCoverageGenerator`):
project → `InitialSccFilter.keepInitialScc` → run the walk algorithm.

The walk is NOT closed back to the initial state. State coverage does
not require it, and forcing a return would only pad the cycle without
covering any extra states.

## Verification

| Check | Outcome |
|---|---|
| `mvn -pl vibes-testgeneration test` green (43 tests now) | ✓ — `Tests run: 43, Failures: 0, Errors: 0, Skipped: 0` |
| `ShortestPaths` returns `[]` when source = target | ✓ |
| `ShortestPaths` picks nearest target from a set (4-cycle, target {c,d} → length 2) | ✓ |
| `ShortestPaths` returns `null` when no target is reachable | ✓ |
| 2-state loop: state-coverage walk is length 1 (one hop reaches every state) | ✓ |
| 5-state branch graph: every state visited | ✓ |
| **SVM end-to-end**: for each of the 12 valid configurations, the walk visits every state of the repaired projected FTS | ✓ |
| Average state-coverage walk length over the 12 SVM configs is ≤ average transition-coverage cycle length | ✓ |
| At least one SVM configuration produces a strictly shorter state-coverage walk than its transition-coverage cycle | ✓ |

## Decisions taken

- **Greedy walk + BFS reroute, NOT Hierholzer-with-state-coverage-cutoff.**
 Two alternatives considered:
 1. Build the balanced Euler cycle (as in M4) and trim it at the moment
 every state has been visited. Pro: easy. Con: still requires
 balancing the graph and running Hierholzer, plus the cycle visits
 states in an order determined by Hierholzer's edge order, not by
 state coverage; the trim point may be late.
 2. Greedy walk + reactive BFS reroute (what we shipped). Pro: a much
 more direct algorithm that exits as soon as every state has been
 visited, mirroring the user's ESG-Fx-side
 `EulerCycleGeneratorForEventCoverage`. Con: not provably optimal.
 Chose 2 for methodological alignment with the prior published study.

- **BFS, not Dijkstra-with-priority-queue.** Every transition is a
 unit-weight edge, so BFS is exact and faster.

- **Walk is open (not closed back to initial).** State coverage doesn't
 require returning to start. Closing the cycle would pad the test
 case length without covering any extra states.

- **Length-comparison test is empirical, not invariant.** The first run
 exposed a real observation: for SVM config 10, the state-coverage walk
 was 15 transitions while the transition-coverage cycle was 14. The
 greedy walk is not optimal; it can take detours that, in graphs where
 `|E| ≈ |V|` (sparse projected products), exceed the Hierholzer cycle.
 We assert "on average state ≤ transition" and "at least one config
 strictly shorter", rather than "always shorter", so the test reflects
 the actual mathematical property of the heuristic.

## SVM length comparison (state coverage vs. transition coverage)

Empirical numbers from the test run:

| Configuration | state cov walk | trans cov cycle | delta |
|---|---|---|---|
| svm_p1.. p12 (averaged) | ≤ ≤ | ≤ | state ≤ trans on average |

(Individual per-config numbers can be extracted from
`vibes-testgeneration/target/surefire-reports/` after a test run with
`-Dexec.args` style logging if needed. The aggregate property is what
the test enforces.)

For SVM config 10 specifically: state=15, trans=14. The greedy walk
took one extra reroute via a previously-visited state. This is a
worth-mentioning observation in the paper's "Threats to Validity" or
"Limitations" section — the state-coverage heuristic is not always
strictly shorter than transition coverage, and reviewers comparing the
two coverages on small SPLs may see counter-intuitive numbers.

## Open questions / blockers

- **Git push credentials** — unchanged.

## Next milestone preview

**M6 (next):** `PairGraphTransformer` plus
`TransitionPairCoverageGenerator`. The transformer turns an FTS into its
"pair graph" — a state in the pair graph represents an ordered pair of
transitions `(t1, t2)` of the original where `target(t1) = source(t2)`,
and an edge from `(t1, t2)` to `(t2, t3)` exists iff that triple is
contiguous in the original. Hierholzer on the pair graph yields a cycle
that covers every transition pair. This is the analog of the ESG-Fx
`TransformedESGFxGenerator`. Transition-pair coverage is the strongest
of the three RQ3 criteria and the operational answer to "intractable at
family level" — at product level it becomes tractable.
