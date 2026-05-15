# Automode Milestone M6 Report

**Date:** 2026-05-15
**Branch:** `feat/product-test-generation`
**Commit:** (set in the commit that lands this report)
**Mode:** automode

## Scope

Third and last Phase-0 coverage criterion: all-transition-pairs. This is
the empirical answer to the central paper claim — "what is intractable
at family level becomes tractable at product level" — and so the most
algorithmically demanding of the three generators.

## Files changed

- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/PairGraphTransformer.java`
  — turns an FTS into its pair graph (a.k.a. the "line graph for our
  test-generation purposes"). Builds a `Result` carrying the pair-graph
  FTS plus a side-map from pair-graph states back to original
  transitions, used for translating cycles back into test cases.
- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/TransitionPairCoverageGenerator.java`
  — orchestrator: project → repair → pair-graph → balance →
  Hierholzer → split at synthetics → translate each segment → suite of
  `TestCase`s. The public API returns
  `List<TestCase>` (not a single `TestCase`).
- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/EulerianBalancer.java`
  — adds a `balanceWithoutPrecheck` entry point used by the pair-graph
  pipeline (the pair graph is intentionally not strongly connected
  before balancing because its INIT vertex has out-degree N but in-degree
  0; balancing supplies the missing in-edges).
- `vibes-testgeneration/src/test/java/be/vibes/testgeneration/coverage/TransitionPairCoverageGeneratorTest.java`
  — 6 tests: pair-graph structural cardinalities on toy + SVM, side-map
  correctness, SVM 12-config end-to-end coverage assertion against the
  enumeration of reachable pairs, and a length-comparison vs. transition
  coverage.

## What was implemented

### `PairGraphTransformer`

Construction rules (textbook L=2 of the user's ESG-Fx
`TransformedESGFxGenerator`):

- A distinguished pair-graph state `INIT` represents "no transition yet".
- For each original transition `t`, a pair-graph state
  `p_<source>_<action>_<target>`.
- For each transition `t` starting at the original initial state, a
  pair-graph edge `INIT -> p(t)` labelled `action(t)`.
- For each ordered pair of original transitions `(t1, t2)` with
  `target(t1) == source(t2)`, a pair-graph edge `p(t1) -> p(t2)`
  labelled `action(t2)`.

The result carries an unmodifiable side-map
`Map<State, Transition>` keyed on the pair-graph target state and
valued by the original transition it represents. This is what the
generator uses to translate a Hierholzer cycle back into a sequence
of original-FTS transitions.

### `TransitionPairCoverageGenerator`

Pipeline:

1. project + repair (M4-style).
2. Build pair graph via `PairGraphTransformer.transform`.
3. `EulerianBalancer.balanceWithoutPrecheck` — pair graph is not strongly
   connected before balancing (INIT is source-only). Balancing supplies
   the missing in-edges; we verify post-balance strong connectivity.
4. `HierholzerEulerCycle.compute` on the balanced pair graph.
5. Split the cycle at every synthetic balancing edge into "real segments".
6. Translate each real segment into a `TestCase` against the repaired
   FTS. The public API returns a `List<TestCase>` because synthetic
   edges may teleport between arbitrary pair-states, and a single
   contiguous `TestCase` cannot bridge such jumps.

### Pair-loss bug and fix

The first end-to-end run on SVM exposed a real bug: pair coverage
was incomplete because synthetic balancing edges in the pair graph
can land at non-INIT pair-states. Concretely for SVM config 7
(FreeDrinks off + CancelPurchase on, 9 transitions), the balancer
chose to insert a synthetic edge from `p(pay)` to `p(change)` —
parallel to a real `p(pay) -> p(change)` edge but using a unique
`__balance__N` action. When the cycle was split at this synthetic
edge, the next real segment started at `p(change) -> p(cancel)`,
and the pair `(change, cancel)` was lost from the resulting suite:

- Segment 0 ends with the cycle reaching `p(pay)` again.
- Synthetic edge `p(pay) -> p(change)` is skipped during translation.
- Segment 1 begins with `p(change) -> p(cancel)` — its first
  translated transition is `cancel`, with no preceding `change` in
  the test case.

Fix: when translating a non-first segment whose first pair-graph
edge starts at a real pair-state `p(t_Y)` (i.e. not INIT), prepend
`t_Y` to the resulting test case. The resulting test case starts
mid-FTS at `source(t_Y)` (legal and replayable by VIBeS' executor
after a reset), and the pair `(t_Y, t_X)` carried by the segment's
first pair-graph edge is now present as the first consecutive pair
of that test case.

After the fix every reachable transition pair across all 12 SVM
configurations is covered by some test case in the suite.

## Verification

| Check | Outcome |
|---|---|
| `mvn -pl vibes-testgeneration test` green (49 tests now) | ✓ |
| Toy two-state loop: pair graph has 3 states (INIT + 2 transition-states) and 3 edges | ✓ |
| SVM pair graph state count = |T| + 1 | ✓ |
| SVM pair graph edge count matches structural formula (INIT-outgoing + pair-edge enumeration) | ✓ |
| Pair-graph side-map contains one entry per original transition | ✓ |
| Suite is non-empty for every SVM configuration | ✓ |
| **For each of 12 SVM configurations, every reachable transition pair is covered by some test case in the suite** | ✓ |
| Average suite total length over 12 configs is >= average transition-coverage cycle length | ✓ |

## Decisions taken

- **Public API returns `List<TestCase>`, not a single `TestCase`.**
  The pair-graph balancer adds synthetic edges that can teleport
  between arbitrary pair-states. A single contiguous `TestCase`
  cannot bridge these jumps. Returning a suite is the natural
  representation; the experiment harness aggregates total length
  across the suite. The API asymmetry with M4 / M5 (which return
  a single `TestCase`) is documented at the call site.
- **`balanceWithoutPrecheck` instead of relaxing the existing
  `balance` precondition.** The original
  `EulerianBalancer.balance` requires its input to be strongly
  connected — a useful safety check for the transition / state
  coverage generators where it really should hold. The pair-graph
  pipeline is the legitimate exception (its INIT vertex is
  intentionally source-only pre-balance). Adding a second entry
  point keeps the strong-connectivity invariant for everyone else
  while letting the pair pipeline opt out.
- **No `InitialSccFilter` on the pair graph.** Applying the standard
  SCC repair to the pair graph BEFORE balancing would discard
  everything except the singleton `{INIT}` SCC, since INIT has
  in-degree 0. We bypass it entirely; the balancer restores strong
  connectivity, which we then verify explicitly post-balance.
- **Prepend the "predecessor transition" to non-first segments.**
  Discovered via test failure on SVM config 7 where the pair
  `(change, cancel)` was lost across a synthetic-edge teleport.
  Documented in the algorithm section above. The fix inflates suite
  length by at most (|segments| − 1) transitions but is necessary
  for completeness.

## SVM pair-coverage suite shape (representative)

For SVM config 7 (FreeDrinks off + CancelPurchase on, 9 transitions
in the repaired FTS):

- Pair graph: 10 states (9 + INIT), 9 real edges + 2 synthetic
  (after balancing).
- Hierholzer cycle on balanced pair graph: 11 edges.
- After splitting at the 2 synthetic edges and prepending, 2 test
  cases of total length 11 transitions cover all 9 reachable
  transition pairs.

The aggregated test-suite length is structurally larger than the
all-transitions cycle for the same configuration (which is expected:
pair coverage is the strictly stronger property).

## Open questions / blockers

- **Git push credentials** — unchanged.

## Next milestone preview

**M7 (next, automode):** wire the experiment harness. Loop SVM, eMail,
Elevator × 3 coverage criteria × every valid configuration; capture
coverage %, suite size (test count + total transitions), generation
time (ms), peak memory (MB) into a per-cell CSV row. Adopt the
`SHARD`/`N_SHARDS`/`runID`/`TIMEOUT_HOURS` env var contract from the
user's ESG-Fx-side `RQ2_ExtremeScalability_L234` so the harness drops
straight into the user's existing Python aggregation pipeline.
