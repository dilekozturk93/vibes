# Automode Milestone M2 Report

**Date:** 2026-05-13
**Branch:** `feat/product-test-generation`
**Commit:** (set in the commit that lands this report)
**Mode:** automode

## Scope

Provide the first piece of graph-algorithm infrastructure for the pipeline:
Tarjan's strongly-connected-components on a VIBeS `TransitionSystem`.
Validate on toy graphs and the three MVP SPLs; report SPL-level SCC
structure so M3 (`EulerianBalancer`) knows what it has to repair.

## Files changed

- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/StronglyConnectedComponents.java`:
  utility class with three entry points — `compute(ts)`, `containing(ts, state)`,
  `isStronglyConnected(ts)`. Recursive textbook Tarjan; result is an
  unmodifiable list in reverse topological order of the SCC-condensation.
- `vibes-testgeneration/src/test/java/be/vibes/testgeneration/graph/StronglyConnectedComponentsTest.java`:
  10 tests covering trivial graphs (single state), classic shapes
  (2-state cycle, linear chain, disjoint cycles, cycle-with-tail) and the
  three MVP SPLs.
- `vibes-testgeneration/src/test/java/be/vibes/testgeneration/graph/SccDiagnostic.java`:
  small `main`-only helper to print SCC stats for the MVP SPLs.
  Not a test, lives under `src/test/java` because it depends on the test-only
  log4j-slf4j-impl binding.

## What was implemented

Single utility class with no internal state and three public methods.
Tarjan is run inside a private `Tarjan` instance per call so the class
itself is thread-safe. Recursion depth is bounded by the state count
(≤ 275 for HockertyShirts), so the recursive form is fine; we don't need
an explicit stack-based reformulation.

`containing(ts, state)` returns `null` for foreign states (those that
don't belong to the transition system passed in). `isStronglyConnected`
is the shortcut sanity check we'll want from a lot of pipeline stages.

## Verification

| Check | Outcome |
|---|---|
| `mvn -pl vibes-testgeneration test` green (19 tests now) | ✓ — `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0` |
| Toy: single state → 1 SCC of size 1 | ✓ |
| Toy: two-state cycle → 1 SCC of size 2 | ✓ |
| Toy: linear chain a→b→c → 3 singleton SCCs | ✓ |
| Toy: disjoint cycles → 2 separate SCCs of size 2 | ✓ |
| Toy: cycle-with-dangling-tail → {2-cycle, singleton-tail} | ✓ |
| `containing(ts, knownState)` returns the right SCC | ✓ |
| `containing(ts, foreignState)` returns `null` | ✓ |
| SVM converted FTS is strongly connected (1 SCC of size 11) | ✓ |
| eMail and Elevator FTSs have a well-defined decomposition (every state in exactly one SCC) | ✓ |

## Decisions taken

- **Recursive Tarjan, not iterative.** The largest SPL has 275 ESG vertices,
  which maps to ≤ 275 states in our FTS. Default Java stack handles that
  comfortably. Iterative formulation would add code complexity for no
  measurable benefit.
- **Result is an unmodifiable `List`.** Defensive copy at the call site
  is the caller's responsibility. The `SccDiagnostic` helper hit this
  immediately when it tried to sort the result; the diagnostic was fixed
  by wrapping in `new ArrayList<>(...)` rather than relaxing the API.
- **SPL-level strong-connectivity tests downgraded for eMail / Elevator.**
  Earlier in M2 the new asserts caught a real issue: those two SPLs are
  NOT strongly connected at the SPL level. This is expected per the
  plan — strong connectivity is required after projection, and the
  EulerianBalancer (M3) is responsible for repair. The tests now assert
  only that every state belongs to some SCC; the actual structure is
  documented below.

## SPL-level SCC structure (diagnostic output)

| SPL | FTS states | SCC count | Largest SCC | Singletons | Sizes |
|---|---|---|---|---|---|
| SVM | 11 | **1** | 11 | 0 | `[11]` |
| eMail | 16 | 2 | 14 | 0 | `[14, 2]` |
| Elevator | 20 | 17 | 2 | 14 | `[2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]` |

Notes:

- **SVM is strongly connected at the SPL level by construction.** Every
  terminating path goes through a fully-terminal vertex (return/c,
  take/f, close/!f), all of which the converter merges into INIT.
- **eMail has a 14-state main SCC plus an isolated 2-state cycle.**
  The small cycle likely corresponds to a self-contained loop in the
  ESG (e.g. "enter email body" can self-loop). It's not reachable from
  / back to INIT — `EulerianBalancer` will need to wire it in.
- **Elevator is heavily fragmented (17 SCCs).** 14 of its 20 states are
  singletons. The natural ESG shape of an elevator interaction has
  many "press X / release X / event" terminal-leaning paths that the
  converter cannot stitch into cycles on its own. This is the
  most demanding case for M3.

## Open questions / blockers

- **Git push credentials** — same as M0 / M1. Continuing local-only.

## Next milestone preview

**M3 (automode):** `EulerianBalancer` (in / out degree equalisation with
synthetic transitions) + `HierholzerEulerCycle` (destructive DFS giving
an Euler cycle). Plus unit tests on toy graphs and on the SVM FTS (which
is strongly connected so the balancer only has to add a few self-loops).
M3 also unblocks M4, which is the first end-to-end transition coverage
generator.
