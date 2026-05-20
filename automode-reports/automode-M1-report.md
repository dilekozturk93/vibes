# Automode Milestone M1 Report

> ⚠ **Two clarifications carried in from later milestones:**
>
> 1. **Cardinalities below predate [M0-rev](automode-M0-rev-report.html).**
>    The 11/18, 16/31, 20/70 numbers come from the initial naive
>    1-vertex-per-state converter. After bisimulation reduction the
>    canonical FTS sizes are 9/13 (SVM), 11/22 (eMail), 14/42
>    (Elevator).
>
> 2. **"Terminal vertices merged into INIT" uses a strict definition.**
>    A vertex counts as terminal **only if every outgoing edge goes
>    to `]`**. Vertices that have at least one non-`]` successor are
>    NOT counted here even though they may also have an edge to `]`
>    — those edges are silently dropped while the vertex itself stays
>    as its own FTS state. This is why eMail reports "2" terminals
>    (despite 4 events touching `]`) and Elevator reports "0"
>    (despite 10 events touching `]`): in eMail only 2 events have
>    `]` as their sole successor, and in Elevator every event has at
>    least one non-`]` continuation.
>
>    Dropped `]`-edges are reflected in the `FTS transitions` column:
>    `FTS transitions = ESG edges − (edges to ])`.

**Date:** 2026-05-13
**Branch:** `feat/product-test-generation`
**Commit:** (set in the commit that lands this report)
**Mode:** automode

## Scope

Extend the M0 MXE → FTS converter to the remaining two Phase-0 SPLs
(eMail and Elevator), validate cardinalities via expanded JUnit tests,
and render Dot + PNG visualisations for all three MVP SPLs (SVM, eMail,
Elevator). This finalises the input layer of the pipeline so that M2
(strongly-connected-components) can start working on real FTS instances
rather than just toy graphs.

## Files changed

- `vibes-testgeneration/src/main/resources/cases/eMail/eM_ESGFx.mxe`:
  copied from the ESG-Fx project.
- `vibes-testgeneration/src/main/resources/cases/Elevator/El_ESGFx.mxe`:
  copied from the ESG-Fx project.
- `vibes-testgeneration/src/test/java/be/vibes/testgeneration/conversion/MxeToFtsConverterTest.java`:
  two new test methods (`convert_emailSpl_producesExpectedCardinalities`,
  `convert_elevatorSpl_producesExpectedCardinalities`) plus a shared
  `convertResource(String)` helper.
- `automode-reports/figures/SodaVendingMachine.{dot,png}`,
  `automode-reports/figures/eMail.{dot,png}`,
  `automode-reports/figures/Elevator.{dot,png}`: Dot + Graphviz PNG
  for each MVP SPL.

## What was implemented

The converter itself did not change — M0 already covered the algorithm.
M1 only added inputs, test cases and visual artefacts. The cardinalities
that come out of the converter for the three MVP SPLs are:

| SPL | ESG vertices (events) | ESG edges | Terminal vertices merged into INIT | FTS states | FTS transitions |
|---|---|---|---|---|---|
| SVM | 13 | 21 | 3 | 11 | 18 |
| eMail | 17 | 35 | 2 | 16 | 31 |
| Elevator | 19 | 80 | 0 | 20 | 70 |

The math checks out in each case: `FTS_states = (events − terminals) + 1`
(the `+ 1` is INIT), `FTS_transitions = ESG_edges − edges_to_]`.

The Elevator model has no fully-terminal vertices: every event has at
least one non-"]" successor. The 10 dropped edges all come from "mixed"
vertices that can both continue and terminate. This is sensible for an
elevator model where most states can both lead to further events and
end the session (e.g. pressing the alarm).

## Verification

| Check | Outcome |
|---|---|
| `mvn -pl vibes-testgeneration test` green (9 tests) | ✓ — `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` |
| eMail Dot renders with `dot -Tpng` (no syntax errors) | ✓ — `eMail.png` ~281 kB, 92-line `.dot` |
| Elevator Dot renders with `dot -Tpng` | ✓ — `Elevator.png` ~792 kB, 126-line `.dot` |
| SVM Dot still renders identically | ✓ — `SodaVendingMachine.png` ~115 kB |
| Round-trip (XML write → XML load) still idempotent for SVM (regression) | ✓ — covered by the unchanged M0 test |

Visual spot-check: the SVM PNG shows the expected structure — `pay/!f`
and `free/f` leave `INIT`, the two parallel chains converge on
`soda/tea/cancel` outgoing edges (with the duplication noted in the M0
report), and `take/f`, `take/!f` and `close/!f` close the cycle back to
`INIT`.

## Decisions taken

- **Cardinality assertions are exact, not bounded.** Tests assert the
  state and transition counts as exact integers. This is intentional:
  if the converter algorithm changes in a way that alters the counts,
  the test should fail loudly and force a deliberate update to the
  expected value (with a clear comment explaining the new arithmetic).
- **Test inputs are bundled under `src/main/resources/cases/<SPL>/`**
  rather than referenced from the sibling ESG-Fx project. This keeps
  `vibes-testgeneration` self-contained and reproducible; the cost is
  ~30 kB of MXE per SPL bundled in the jar.
- **PNGs are committed under `automode-reports/figures/`** rather than
  inside the Maven module. They're documentation artefacts, not source
  inputs, and don't belong on the classpath. Future paper figures can
  also live here.

## Open questions / blockers

- **Git push credentials** — same as M0: pushes still fail with
  `Device not configured`. Continuing local-only.

## Next milestone preview

**M2 (automode):** implement `StronglyConnectedComponents` (Tarjan's
algorithm) on `TransitionSystem` plus an SVM-product-level smoke test
that reports SCC structure of each of the 12 valid SVM configurations.
This is the first piece of graph algorithm infrastructure and the
prerequisite for M3 (Eulerian balancing + Hierholzer).
