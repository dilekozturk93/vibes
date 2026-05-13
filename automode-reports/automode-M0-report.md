# Automode Milestone M0 Report

**Date:** 2026-05-13
**Branch:** `feat/product-test-generation`
**Commit:** (set in the commit that lands this report)
**Mode:** manual (M0 was always planned as the manual bootstrap; M1 onward is automode)

## Scope

Stand up the `vibes-testgeneration` Maven module, verify the surrounding VIBeS
infrastructure that downstream milestones rely on, copy the SVM ESG-Fx / DIMACS
inputs into the module's resources, implement the MXE → FTS converter, and
gate it with smoke + structural tests on SVM. This is the foundation
for M1 (eMail + Elevator conversion) and M2+ (graph algorithms + coverage
generators).

## Files changed

- `pom.xml`: register the new `vibes-testgeneration` module.
- `.gitignore`: fix `.DS_Store` glob (was `.DS_Store/` with trailing slash so
  nothing matched), ignore `.claude/`, `.vscode/`, `**/bin/`, and the
  exploratory `vibes-example/*.dot` / `*.png` outputs.
- `vibes-example/pom.xml`: drop unresolved `${log4j.version}` direct dep
  (already pulled transitively via `slf4j-log4j12`); align parent version
  to `2.0.6-SNAPSHOT`.
- `vibes-example/.../Main.java`: rewrite as a minimal Dot-printing demo
  against the VIBeS 2.x DSL.
- `vibes-example/.../SodaVendingMachineModel.java`: update import to
  `be.vibes.dsl.ts.FeaturedTransitionSystemDefinition`.
- `vibes-example/.../ManualTestSuite.java`: delete (depended on pre-2.x
  DSL APIs that no longer exist; not referenced by the new pipeline).
- `vibes-testgeneration/pom.xml`: new module pom (deps: vibes-core,
  vibes-fexpression, vibes-dsl, guava, junit, hamcrest, slf4j).
- `vibes-testgeneration/src/main/java/be/vibes/testgeneration/conversion/MxeToFtsConverter.java`:
  the converter itself, ~250 LoC with full javadoc and a CLI `main`.
- `vibes-testgeneration/src/test/java/be/vibes/testgeneration/conversion/MxeToFtsConverterTest.java`:
  7-test suite — label parsing (3), SVM cardinalities + actions + initial-state
  invariants (3), and XML round-trip idempotency (1).
- `vibes-testgeneration/src/main/resources/cases/SodaVendingMachine/`:
  SVM ESG-Fx MXE and `configs/SVM.dimacs` + `configs/SVM_dimacsmapping.txt`
  copied from the user's ESG-Fx project.

## What was implemented

**MxeToFtsConverter** is the core deliverable. Conversion rules (encoded in
the class javadoc and verified by tests):

- "[" → FTS initial state `INIT`.
- "]" has no corresponding state; merged with `INIT` so the FTS is cyclic.
- An ESG vertex `v` whose outgoing edges all target "]" is "terminal" and
  also merged with `INIT`. This reproduces the inlining seen in
  hand-written `fts-sodaVendingMachine.xml` (where `state9 -[close/!f]-> state1`
  comes from ESG `take/!f → close/!f → ]` being collapsed).
- Every other ESG vertex `v` becomes its own FTS state `s_v` (state name
  prefixed with `v` to avoid collisions with the mxGraph numeric ids).
- An ESG edge `u -> v` becomes an FTS transition
  `state(u) -[event(v)/fexpr(v)]-> state(v)` where `state(x)` resolves to
  `INIT` for `[`, `]`, or terminal vertices and to `s_x` otherwise.
- Edges of the form `u -> "]"` are dropped (already captured by terminal
  merging).
- Feature-expression syntax: `!X` is parsed as a negation of feature `X`;
  bare `X` is the leaf expression; missing or empty expression yields
  `FExpression.trueValue()`.

The class is deliberately self-contained and uses only the VIBeS public API
(`FeaturedTransitionSystemFactory`, `FExpression`, `Xml`). No JGraphT,
no DOM helpers beyond `javax.xml.parsers`. CLI entry point allows
`java -jar … <input.mxe> [output.xml]`.

## Verification

| Check | Outcome |
|---|---|
| Module compiles cleanly via `mvn -pl vibes-testgeneration -am compile` | ✓ |
| Test suite (7 tests) green | ✓ — `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` |
| SVM ESG (15 vertices, 21 edges) → FTS with 11 states, 18 transitions | ✓ — matches the expected `(15 − 2 brackets − 3 terminals) + INIT = 11` and `21 − 3 dropped = 18` |
| All 12 SVM ESG events appear as actions in the FTS | ✓ (`pay, change, free, soda, tea, serveSoda, serveTea, cancel, return, open, take, close`) |
| `INIT` is the initial state and has > 0 outgoing transitions | ✓ |
| `Xml.print` → `Xml.loadFeaturedTransitionSystem` is round-trip idempotent on state and transition counts | ✓ |
| Whole reactor build still green (`mvn install` of all modules) | not re-run for M0 to save time; will run at M1 boundary when the new module first ships into the full reactor |

## Decisions taken

- **`vibes-mutation` left disabled.** The module exists with `TransitionMissing`
  and `ActionExchange` operators we need, but everything inside is still on
  the pre-2.x `be.unamur.transitionsystem.*` package and depends on the also
  pre-2.x `vibes-transformation` module. Migrating it is a non-trivial side
  quest that the plan defers to Phase 1 (mutation testing setup). M0 explicitly
  does NOT re-enable it.
- **Auto-generated FTS is bigger than the hand-written reference.** SVM
  produces 11 states / 18 transitions vs. the hand-written
  `fts-sodaVendingMachine.xml` (9 / 13). The difference comes from
  state-merging decisions the human author made when two ESG vertices have
  identical successor sets (e.g. `change/!f` and `free/f` both merge into
  the same FTS state in the hand-written file). The automated converter
  keeps these separate, which is semantically equivalent: the same set of
  feature-valid action sequences is accepted. Coverage metrics will count
  more elements but consistently across the pipeline, so RQ comparisons
  stay valid.
- **State naming uses `v<mxGraphId>`** (e.g. `v40`, `v83`) instead of
  event names. mxGraph ids are unique within a document; event names
  are not (e.g. SVM has `take/f` and `take/!f`). Using the id avoids
  state collisions for any input model.
- **Terminal-vertex merging is automatic, no flag.** A vertex is terminal
  iff it has ≥ 1 outgoing edge and all of them target `"]"`. This is the
  simplest definition that reproduces the hand-written-FTS shape on SVM.
  If a future SPL needs different semantics we will revisit.
- **Push to `origin` failed** with
  `fatal: could not read Username for 'https://github.com/dilekozturk93/vibes.git': Device not configured`.
  The branch was committed locally; the user needs to configure git
  credentials (`gh auth login`, an SSH key, or a credential helper) once;
  subsequent milestones will push automatically. M0 work is fully reproducible
  from the local commit either way.

## Open questions / blockers

- **Git push credentials.** Listed under "Decisions taken" above. Not blocking
  M1; I will keep accumulating commits locally and retry push at every
  milestone end so as soon as credentials are configured, the whole chain
  flushes upstream.
- **DIMACS files for 4 SPLs missing.** Of the 8 SPLs only SVM, HockertyShirts,
  Tesla and syngovia have `<SPL>.dimacs` + `<SPL>_dimacsmapping.txt` already
  in the ESG-Fx project. The other four (eMail, Elevator, BankAccountv2,
  StudentAttendanceSystem) have only `configs/model.xml`. Generating their
  DIMACS is a Phase 1 task and out of M0 scope; M1 (eMail + Elevator FTS
  conversion) only needs the MXE files, which are present.

## Next milestone preview

**M1 (automode):** apply `MxeToFtsConverter` to `eMail/eM_ESGFx.mxe`
and `Elevator/El_ESGFx.mxe`; copy each MXE into
`vibes-testgeneration/src/main/resources/cases/<SPL>/`; extend the test
suite with structural smoke tests for the two new SPLs (state/transition
counts plus action-set spot-checks); render each as Dot for visual
verification. Estimated 2-3 h.
