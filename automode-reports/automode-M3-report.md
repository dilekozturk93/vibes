# Automode Milestone M3 Report

**Date:** 2026-05-13
**Branch:** `feat/product-test-generation`
**Commit:** (set in the commit that lands this report)
**Mode:** automode

## Scope

Implement the two graph algorithms that turn a strongly-connected FTS into
a deterministic test-generation seed: `EulerianBalancer` adds synthetic
transitions until every state has equal in / out degree, and
`HierholzerEulerCycle` extracts an Euler cycle from a balanced strongly
connected FTS. Together they form the core of every coverage criterion in
the pipeline (transition coverage in M4 uses them directly; state and
transition-pair coverage in M5 / M6 build on top).

## Files changed

- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/EulerianBalancer.java`
  — utility class with `balance(fts)`, `isSyntheticAction(...)`,
  `syntheticTransitions(...)`, and a `SYNTHETIC_ACTION_PREFIX = "__balance__"`
  constant.
- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/HierholzerEulerCycle.java`
  — utility class with `compute(fts)` and `asActionSequence(cycle)`.
- `vibes-testgeneration/src/test/java/be/vibes/testgeneration/graph/HierholzerEulerCycleTest.java`
  — 7 tests covering toy graphs (2-state cycle, 4-state balanced graph,
  unbalanced rejection, disconnected rejection, action-sequence extraction)
  and an end-to-end balancer + Hierholzer run on SVM.

## What was implemented

### EulerianBalancer

Returns a NEW `FeaturedTransitionSystem` instance; the input is untouched.
Algorithm:

1. Reject inputs that are not strongly connected (the second necessary
   condition for an Euler cycle; balancer cannot repair this).
2. Copy every state, action, and transition into a fresh
   `FeaturedTransitionSystemFactory`. Original `FExpression`s are
   preserved verbatim.
3. Compute `delta(v) = out_degree(v) − in_degree(v)` for every state.
4. Greedy bipartite pairing: build a FIFO of "needs outgoing" slots
   (one per unit of in-heaviness) and a FIFO of "needs incoming" slots
   (one per unit of out-heaviness). Pop pairs and add a synthetic
   transition between them.
5. Each synthetic transition gets a unique action name
   `__balance__<N>` (so VIBeS' `(source, action, target)` deduplication
   does not collapse parallel synthetic edges between the same pair) and
   `FExpression.trueValue()`.

`isSyntheticAction(...)` and `syntheticTransitions(...)` let downstream
code recognise and filter synthetic edges (important for coverage
measurement: we don't want synthetic balancing edges inflating the
coverage denominator).

### HierholzerEulerCycle

Standard iterative Hierholzer. Two stacks:

- `vertexStack` — vertices being explored.
- `arrivalStack` — parallel to vertexStack, stores the transition used to
  arrive at each stacked vertex (null for the initial state). Uses
  `LinkedList` rather than `ArrayDeque` because `ArrayDeque` rejects
  null elements.

A separate `usedStack` collects transitions in reverse traversal order;
final result is built by draining it via `pop()`, which yields the
correct forward order.

`verifyCycleClosure` is an in-method postcondition check: the first
transition must start at the initial state, the last must end at it, and
consecutive transitions must form a path. Any violation throws
`IllegalArgumentException` so future bugs surface immediately.

## Verification

| Check | Outcome |
|---|---|
| `mvn -pl vibes-testgeneration test` green (26 tests now) | ✓ — `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0` |
| 2-state cycle → cycle of length 2 covering both edges | ✓ |
| 4-state cycle (two parallel 2-cycles sharing vertex `a`) → cycle of length 4 covering all 4 edges | ✓ |
| Unbalanced graph (single edge a→b) → `IllegalArgumentException` mentioning balance | ✓ |
| Balanced but disconnected (two 2-cycles) → `IllegalArgumentException` mentioning strong connectivity | ✓ |
| `asActionSequence` extracts action names in order | ✓ |
| **End-to-end on SVM**: convert MXE → balancer adds N synthetic edges → Hierholzer produces a cycle that visits every transition (including synthetics) exactly once and returns to INIT | ✓ |
| `isSyntheticAction` correctly identifies `__balance__N`-prefixed actions | ✓ |

## Decisions taken

- **Synthetic action prefix is `__balance__`** (double underscore, double
  trailing). Visually distinct from any human-readable action and lexically
  invalid as a Java identifier, so unlikely to ever collide. Suffix is
  a 0-indexed counter to guarantee VIBeS' `(source, action, target)`
  deduplication never collapses parallel synthetic edges.
- **Balancer keeps original `FExpression`s, sets synthetic edges to
  `FExpression.trueValue()`.** Synthetic edges represent "the pipeline
  is rewiring graph topology for Eulerian existence", not a feature
  constraint, so `true` is the only sensible value.
- **Balancer enforces strong-connectivity precondition at the API.**
  Trying to balance a non-strongly-connected FTS throws. This forces
  callers to think about the order of operations (project → SCC repair →
  balance) rather than discovering it via a silently wrong cycle.
- **`LinkedList` for `arrivalStack`**, not `ArrayDeque`. The initial
  state has no arrival transition; we use `null` as the sentinel.
  `ArrayDeque` rejects null elements; `LinkedList` does not. Caught
  by the first toy-graph test on the first run.

## Open questions / blockers

- **Git push credentials** — unchanged from M0 / M1 / M2.

## Next milestone preview

**M4 (automode):** `FExpressionPreservingProjection` (analog of VIBeS'
`SimpleProjection`, but emits an FTS with retained `FExpression`s) plus
`TransitionCoverageGenerator` (a thin orchestrator: project → balance →
Hierholzer → return `TestCase`). Smoke test enumerates all 12 SVM
configurations, generates one transition-coverage test case per config,
replays it with `TransitionSystemExecutor`, and asserts
`TransitionCoverage.coverage(execution) ≈ 1.0`.
