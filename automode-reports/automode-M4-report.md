# Automode Milestone M4 Report

**Date:** 2026-05-13
**Branch:** `feat/product-test-generation`
**Commit:** (set in the commit that lands this report)
**Mode:** automode

## Scope

First end-to-end coverage criterion: all-transitions. The pipeline now takes
an SPL FTS plus a product configuration, projects (preserving feature
expressions), keeps the initial-state SCC, balances, runs Hierholzer, and
wraps the cycle into a `TestCase`. Tested on every valid configuration of
SVM.

## Files changed

- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/product/FExpressionPreservingProjection.java`
  — analog of VIBeS' `SimpleProjection` but emits `FeaturedTransitionSystem`
  with retained `FExpression`s.
- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/InitialSccFilter.java`
  — keeps the SCC that contains the initial state and drops everything else.
- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/TransitionCoverageGenerator.java`
  — orchestrator: project → repair → balance → Hierholzer → TestCase.
- `vibes-testgeneration/src/test/java/be/vibes/testgeneration/coverage/TransitionCoverageGeneratorTest.java`
  — end-to-end SVM test: 12 valid configs from SAT, each produces a test
  case that covers every reachable transition.
- `vibes-testgeneration/src/main/resources/cases/SodaVendingMachine/configs/SVM_dimacsmapping.txt`
  — reformatted from the user's `ID:FeatureName` (colon-separated, header
  row) layout into VIBeS' `id featureName` (space-separated, no header).
  This is the format VIBeS' `DimacsModel.createFromTvlParserGeneratedFiles`
  expects.

## What was implemented

### `FExpressionPreservingProjection`

For each transition in the SPL FTS, evaluate
`fexpr.assign(product).applySimplification()`; if the result is true,
include the transition in the projected FTS with its **original** feature
expression retained (not the simplified `TRUE`). The original carries
strictly more information (which feature choice justified the transition)
and is what we want for traceability and for the uniform-data-structure
property promised in the plan.

### `InitialSccFilter`

After projection, the product-level FTS frequently fragments. Strategy:
compute Tarjan SCCs of the projected FTS, find the SCC that contains the
initial state, drop all other states and the transitions touching them.
What survives is by construction strongly connected and rooted at the
original initial state — the prerequisite for `EulerianBalancer`.

### `TransitionCoverageGenerator`

Pure orchestration; no new algorithms. The five steps are:

1. `project = FExpressionPreservingProjection.project(fts, config)`
2. `repaired = InitialSccFilter.keepInitialScc(project)`
3. `balanced = EulerianBalancer.balance(repaired)`
4. `cycle = HierholzerEulerCycle.compute(balanced)`
5. `testCase = new TestCase(id); testCase.enqueueAll(cycle); return testCase;`

## Verification

| Check | Outcome |
|---|---|
| `mvn -pl vibes-testgeneration test` green | ✓ — `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0` |
| 12 valid SVM configurations enumerated by `Sat4JSolverFacade` | ✓ |
| For each of the 12 configs, the generated TestCase covers every transition of the repaired (initial-SCC) FTS | ✓ |
| Synthetic balancing actions identified via `EulerianBalancer.isSyntheticAction(...)` are filtered before coverage assertion | ✓ |
| First SVM config originally had 7 states across 3 SCCs (pre-repair); SCC repair brought it down to a strongly-connected sub-FTS the balancer accepts | ✓ |

## Decisions taken

- **`InitialSccFilter` as a separate utility class**, not folded into
  `TransitionCoverageGenerator`. State-coverage (M5) and transition-pair
  coverage (M6) will both need it; carving it out keeps each coverage
  generator at the level of "compose existing utilities" rather than
  re-implementing the repair step.
- **Dropped behaviour outside the initial SCC is silently discarded.**
  An explicit "lost coverage ratio" metric belongs in the experiment
  harness (M7), not in the generator. The plan notes this and we honour
  it here.
- **Tested via cycle-set comparison rather than `TransitionSystemExecutor`
  replay.** The Hierholzer post-condition guarantees a closed contiguous
  cycle; the existence of a path matching the cycle in the underlying
  FTS is implicit. Reusing the cycle's transition objects against the
  repaired FTS' transition set gives an equivalent, simpler assertion.
  A full executor-based replay belongs alongside mutation testing in M7
  (where it actually matters — replaying on a mutant might fail mid-cycle
  and that failure is the signal we want).
- **Mapping file format conversion is committed**, not done at runtime.
  Future SPLs added to the module will follow the same convention: the
  `configs/<SPL>_dimacsmapping.txt` file is space-separated with no
  header. A small one-shot converter could be added later if it becomes
  a frequent operation.

## Open questions / blockers

- **Git push credentials** — unchanged.

## Next milestone preview

**M5 (automode):** `StateCoverageGenerator`. A walk through the projected
+ SCC-repaired FTS that visits every state at least once, using Dijkstra
to reroute toward the nearest uncovered state when the natural traversal
exhausts uncovered neighbours. This is the analog of the ESG-Fx
`EulerCycleGeneratorForEventCoverage`. State coverage produces shorter
test cases than transition coverage (it doesn't need every edge) so it
also pressure-tests the assumption that "shorter Euler-based tests still
beat random walks" — relevant for RQ3.
