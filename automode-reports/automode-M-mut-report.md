# Automode Milestone M-mut Report

**Date:** 2026-05-15
**Branch:** `feat/product-test-generation`
**Commit:** (set in the commit that lands this report)
**Mode:** automode

## Scope

Port the two mutation operators referenced in the ICTSS abstract
(TransitionMissing and ActionExchange) from the user's ESG-Fx project into
the new `vibes-testgeneration` module, working directly on the VIBeS
`FeaturedTransitionSystem` API. Slotted earlier than the original plan
(M9) at the user's explicit request after they chose option C
("port logic from ESG-Fx project") over reviving the old
`vibes-mutation` module (which is still on the pre-2.x API and would have
required a non-trivial migration).

## Files changed

- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/MutationOperator.java`
  — abstract base class modelled after the ESG-Fx
  `tr.edu.iyte.esgfx.mutationtesting.mutationoperators.MutationOperator`.
  Holds the mutant map and the operator's human-readable name; concrete
  subclasses override `generateMutants(FeaturedTransitionSystem)`.
- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/FtsCloning.java`
  — utility that builds modified copies of an FTS via
  `FeaturedTransitionSystemFactory`. Three entry points: `copy(fts)`,
  `withoutTransition(fts, t)`, `withReplacedAction(fts, t, a)`. Centralises
  the rebuild-with-one-change pattern so operators stay short.
- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/TransitionMissing.java`
  — FTS analog of ESG-Fx's `EdgeOmitter`. One mutant per transition; the
  transition is dropped, everything else preserved.
- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/ActionExchange.java`
  — FTS analog of vibes-mutation's `ActionExchange`. For each
  `(transition, alternative_action)` pair, produce a mutant where the
  transition's label is swapped to the alternative action; source state,
  target state, and feature expression preserved.
- `vibes-testgeneration/src/test/java/be/vibes/testgeneration/mutation/MutationOperatorsTest.java`
  — 9 tests covering toy graph (2-state loop) and SVM cardinalities,
  key uniqueness, mutant distinctness, and the cloning utility's
  copy / drop / replace primitives.

## What was implemented

### `MutationOperator` (base class)

Closely mirrors the ESG-Fx-side hierarchy:
- `protected String name` + `protected Map<String, FeaturedTransitionSystem> mutants`
- `public abstract void generateMutants(FeaturedTransitionSystem fts)`
- Read-only accessors: `getName()`, `getMutants()` (unmodifiable view),
  `getMutantCount()`.

Validation of whether a mutant is "valid" (e.g. still strongly connected
post-mutation) is intentionally NOT performed here. The ESG-Fx pipeline
also produces both valid and invalid mutants and lets the downstream
fault-detection harness sort them out. We mirror that decision so the
methodology is unchanged from the prior published study.

### `FtsCloning`

Three operations parameterised by a single shared `rebuild` kernel:

- `copy(fts)` — identity rebuild; needed for the toy-graph round-trip test
  and as a primitive used by mutation operators if they ever need to
  decouple from the input identity-wise.
- `withoutTransition(fts, t)` — rebuild skipping one transition.
- `withReplacedAction(fts, t, a)` — rebuild skipping the target transition
  AND adding a new transition with the same source / target / feature
  expression but the replacement action. This is the structural primitive
  the `ActionExchange` operator needs.

### `TransitionMissing`

Snapshots the original transition list (so iteration order does not
interact with the per-mutant rebuilds), then for each transition calls
`FtsCloning.withoutTransition(fts, t)`. Mutant key format:
`TM__<source>__<action>__<target>`. Stable across runs (depends only on
source/action/target names) and grep-friendly.

### `ActionExchange`

For each transition × every other action in the FTS alphabet, generates a
mutant with `FtsCloning.withReplacedAction(fts, t, replacement)`. The
total mutant count is `|T| × (|A| − 1)`. For SVM (18 transitions,
12 actions) that yields 198 mutants. Mutant key format:
`AEX__<source>__<originalAction>__<newAction>__<target>`.

## Verification

| Check | Outcome |
|---|---|
| `mvn -pl vibes-testgeneration test` green (36 tests now) | ✓ — `Tests run: 36, Failures: 0, Errors: 0, Skipped: 0` |
| Toy graph: TransitionMissing produces 2 mutants from 2 transitions, each with 1 fewer transition | ✓ |
| TransitionMissing mutant keys are unique and use the `TM__` prefix | ✓ |
| SVM: TransitionMissing produces exactly 18 mutants (= |T|) | ✓ |
| Toy graph: ActionExchange produces 2 mutants (= 2 × 1) | ✓ |
| SVM: ActionExchange produces exactly 198 mutants (= 18 × 11) | ✓ |
| Every TransitionMissing mutant is structurally distinct from every other | ✓ |
| Every ActionExchange mutant is structurally distinct from every other | ✓ |
| TransitionMissing and ActionExchange mutant sets are disjoint (different cardinalities for the transition list) | ✓ |
| `FtsCloning.copy` produces a structurally-equal but distinct object | ✓ |
| `FtsCloning.withoutTransition` drops only the named transition | ✓ |

## Decisions taken

- **Operators live in `be.vibes.testgeneration.mutation`**, not in the
  legacy `vibes-mutation` module. The legacy module is still on the
  pre-2.x API and depends on the also-disabled `vibes-transformation`
  module; reviving it would have been larger than this whole port.
  Putting the operators inside `vibes-testgeneration` also keeps the new
  pipeline self-contained.
- **No "validate the mutant" step in the operators themselves.** Whether
  a mutant is "viable" (e.g. still strongly connected, still reachable
  from the initial state) is a question the experiment harness decides
  on, not the operator. We match the user's ESG-Fx convention here.
- **`ActionExchange` enumerates every `(transition, alternative_action)`
  pair**, not just one swap per transition (which is what vibes-mutation's
  selection-strategy-driven version does). This matches the ESG-Fx-style
  "enumerate every mutation site" approach and makes the mutant-set
  reproducible across runs (no RNG, no strategy choice).
- **Mutant keys include human-readable state and action names** rather
  than opaque integer IDs. Easier to grep and to diff between runs;
  cost is a few extra bytes per CSV row.
- **No mutant validity check (strong-connectivity etc.) here.** Adding
  one would let us mark mutants up front as "definitely cannot be killed
  by any test" (= equivalent in our setting), but that's a property of
  the *coverage criterion + test suite* pair, not of the operator. The
  experiment harness will handle this in M7.

## SVM mutant inventory

For the SVM SPL (M0 conversion: 11 states, 18 transitions, 12 actions),
running both operators against the SPL-level FTS yields:

| Operator | Mutants |
|---|---|
| TransitionMissing | 18 |
| ActionExchange | 198 |
| **Total** | **216** |

A typical mutation-testing run will also apply each operator to each
*projected product* FTS (= per-configuration mutants). For SVM with 12
valid configurations, an unfiltered upper bound on the per-product
mutant count is in the same order of magnitude (less because projected
FTSs are smaller).

## Open questions / blockers

- **Git push credentials** — same as M0–M4. Continuing local-only;
  please configure `gh auth login` (or equivalent) when convenient.

## Next milestone preview

**M5 (next, automode):** `StateCoverageGenerator` — Hierholzer + reactive
Dijkstra reroute toward the nearest uncovered state when the natural
traversal exhausts uncovered neighbours. Analog of the ESG-Fx-side
`EulerCycleGeneratorForEventCoverage`. State coverage produces shorter
test cases than transition coverage (it does not need every edge), so
the resulting test suites are the natural lower-cost baseline for RQ3.
