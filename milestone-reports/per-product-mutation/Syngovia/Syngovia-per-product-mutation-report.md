# Per-Product Mutation Report — Syngovia

## How mutation scores are computed

**Why a fresh mutation module, not `vibes-mutation`.** The legacy `vibes-mutation` module (commented out in the root pom) is on the old `be.unamur.transitionsystem.*` + `be.unamur.fts.fexpression.*` namespaces and transitively depends on `vibes-transformation` (also dormant) plus a non-existent `vibes-execution` module. Re-vivifying all three for just the two operators referenced in the ICTSS abstract (TransitionMissing, ActionExchange) would have been disproportionate; we re-implement against the current `be.vibes.ts.*` types in `vibes-testgeneration/.../mutation/`.

**Pipeline per product:**

1. **Project + repair.** Same as the coverage reports — `FExpressionPreservingProjection.project` followed by `InitialSccFilter.keepInitialScc` gives the product-level repaired FTS (the system under test for this product).
2. **Generate test suites.** Three independent generators produce one suite per criterion: `StateCoverageGenerator.generate` (one TestCase, greedy + BFS reroute), `TransitionCoverageGenerator.generate` (one TestCase, Chinese-Postman + Hierholzer Euler cycle), and `TransitionPairCoverageGenerator.generate` (suite of TestCases via pair-graph Hierholzer, deduped by action sequence).
3. **Generate mutants.** [`TransitionMissing`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/TransitionMissing.java) emits one mutant per transition (the transition is removed). [`ActionExchange`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/ActionExchange.java) emits one mutant per (transition, neighbourhood-adjacent action) pair: for `t = (s, α, d)` the replacement `β` is drawn from `OutgoingActions(s) ∪ OutgoingActions(d) \ {α}` (source- and sequentially-adjacent actions), skipping any `β` for which `(s, β, d)` already exists in the FTS, and restricted to feature-compatible swaps (co-satisfiability of `t`'s feature expression with that of some `β`-labelled transition, checked via SAT under the feature model). Source state, target state, and feature expression of `t` are preserved.
4. **Filter synthetic mutants.** A mutant whose mutation site is on a synthetic transition (`__end__`) is dropped from the denominator. The SUT doesn't have such a transition; whether a test suite happens to 'kill' such a mutant is not a meaningful signal about real fault detection.
5. **Replay test suite on each mutant.** A TestCase **kills** a mutant iff at least one of its non-synthetic transitions `(source, action, target)` is NOT present in the mutant. For TransitionMissing this happens whenever the suite traverses the removed transition; for ActionExchange whenever the suite traverses the mutated transition (the original `(s, a_orig, t)` triple is gone — replaced by `(s, a_new, t)`).
6. **Mutation score per criterion** = killed mutants / (total &minus; equivalent), per Inozemtseva &amp; Holmes (2014). The equivalent set is conservatively defined as mutants surviving all five suites in this study (family + product state + product transition + product pair + random).

**Random baseline column.** The "Random" column shows a single representative random suite (seed 0, action budget matched to the product's transition-coverage suite, per-walk step ceiling 2 × |T_repaired|, cut-and-include semantics — see `RandomBaselineGenerator` JavaDoc). The full 100-seed × 3-budget random-baseline distribution per (product × operator) — with median / quartiles / extremes and aborted-walk counts — lives in `milestone-reports/metrics/rq2-random-baseline.csv` (per-coverage-level budget; this column is the legacy display only).

**Note on coverage saturation vs. TransitionDestinationExchange.** TransitionMissing, ActionExchange, and StateMissing mutants on a deterministic FTS are killed precisely when the mutated transition is traversed (mid-replay `canExecute` refusal at the mutation site); transition coverage therefore detects them by construction and stronger criteria add execution cost without detection. [`TransitionDestinationExchange`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/TransitionDestinationExchange.java) (TDE) breaks this saturation: it leaves the original action label intact so replay continues past the mutated site, and divergence surfaces only on the SUBSEQUENT step when the executor — now at the wrong target state — tries to fire the expected next transition. Pair coverage exercises EVERY t-outgoing pair as a separate consecutive sequence, giving it combinatorially more opportunities to expose that divergence than transition coverage (which fixes one arbitrary follow-up to t). The detection gap on TDE is RQ3's primary signal that pair coverage's marginal cost buys actual detection.

**Note on test-case granularity (methodology-fairness fix 2026-05-24).** All three coverage generators (`StateCoverageGenerator`, `TransitionCoverageGenerator`, `TransitionPairCoverageGenerator`) return `List<TestCase>` split at every initial-return of the walk. Each TC is one round-trip from the initial state, and the executor RESETS between TCs. Earlier the state and transition generators returned a single `TestCase` and the executor reset only once at suite start; pair coverage's multi-TC suite reset between every TC. That asymmetry inflated transition-coverage's TDE detection (a TDE mutant whose mutated transition has target = initial was caught mid-cycle in single-TC replay because the next transition tried to fire from the wrong state; in multi-TC replay the same mutated transition is the final step of a TC, the reset wipes the divergence, and the mutant survives — measured as a false +27pt transition-coverage advantage on SAS in the v2 pilot). Aligning all generators on the same TC = initial-return-trip semantic restores fair comparison.

---

## Products


### Product 1

**Selected features:** selected = {MMR, ab, as, bl, br, cli, clop, fip, ftp, iar, inl, layg, mfa, pat, pb, rel, sa, ser, sp, srl, sso, swl, tl, tob, tog, tr, vi, vip, wfl, wlb}

**Repaired FTS:** 74 states, 271 transitions (270 real / 1 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 106 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 270 | 0/270 = 0.0% | 0/270 = 0.0% | 100/270 = 37.0% | 270/270 = 100.0% | 270/270 = 100.0% | 111/270 = 41.1% |
| ActionExchange | 1964 | 0/1964 = 0.0% | 0/1964 = 0.0% | 876/1964 = 44.6% | 1964/1964 = 100.0% | 1964/1964 = 100.0% | 718/1964 = 36.6% |
| StateMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 73/73 = 100.0% | 73/73 = 100.0% | 73/73 = 100.0% | 48/73 = 65.8% |
| TransitionDestinationExchange | 1277 | 558/1277 = 43.7% | 0/719 = 0.0% | 306/719 = 42.6% | 667/719 = 92.8% | 719/719 = 100.0% | 319/719 = 44.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (558):

- `TDE__state30__Hide Graphics__state45__state44`
- `TDE__state30__Hide Graphics__state45__state46`
- `TDE__state61__Remove from 
Favorites__state1__state4`
- `TDE__state61__Remove from 
Favorites__state1__state3`
- `TDE__state61__Remove from 
Favorites__state1__state2`
- `TDE__state47__Bone Removal Head__state45__state66`
- `TDE__state47__Bone Removal Head__state45__state65`
- `TDE__state47__Bone Removal Head__state45__state63`
- `TDE__state47__Bone Removal Head__state45__state62`
- `TDE__state39__select instance
from Instances list__state58__state39`
- `TDE__state47__Bone Removal Leg__state45__state66`
- `TDE__state47__Bone Removal Leg__state45__state65`
- `TDE__state47__Bone Removal Leg__state45__state63`
- `TDE__state47__Bone Removal Leg__state45__state62`
- `TDE__state60__Discard Changes__state1__state4`
- `TDE__state60__Discard Changes__state1__state3`
- `TDE__state60__Discard Changes__state1__state2`
- `TDE__state47__Lock Reference
Lines__state45__state66`
- `TDE__state47__Lock Reference
Lines__state45__state65`
- `TDE__state47__Lock Reference
Lines__state45__state63`
- `TDE__state47__Lock Reference
Lines__state45__state62`
- `TDE__state30__Align Timepoints__state45__state44`
- `TDE__state30__Align Timepoints__state45__state46`
- `TDE__state58__Drag data
into Patient
tab__state1__state58`
- `TDE__state58__Drag data
into Patient
tab__state1__state14`
- `TDE__state58__Drag data
into Patient
tab__state1__state13`
- `TDE__state58__Drag data
into Patient
tab__state1__state41`
- `TDE__state58__Drag data
into Patient
tab__state1__state4`
- `TDE__state58__Drag data
into Patient
tab__state1__state3`
- `TDE__state58__Drag data
into Patient
tab__state1__state2`
- `TDE__state44__Save and Send__state1__state59`
- `TDE__state44__Save and Send__state1__state60`
- `TDE__state44__Save and Send__state1__state4`
- `TDE__state44__Save and Send__state1__state3`
- `TDE__state44__Save and Send__state1__state2`
- `TDE__state39__Drag data
into Patient
tab__state1__state58`
- `TDE__state39__Drag data
into Patient
tab__state1__state14`
- `TDE__state39__Drag data
into Patient
tab__state1__state13`
- `TDE__state39__Drag data
into Patient
tab__state1__state39`
- `TDE__state39__Drag data
into Patient
tab__state1__state41`
- `TDE__state39__Drag data
into Patient
tab__state1__state4`
- `TDE__state39__Drag data
into Patient
tab__state1__state3`
- `TDE__state39__Drag data
into Patient
tab__state1__state2`
- `TDE__state24__LayoutGallery icon__state35__state33`
- `TDE__state26__Delete__state1__state26`
- `TDE__state26__Delete__state1__state14`
- `TDE__state26__Delete__state1__state13`
- `TDE__state26__Delete__state1__state39`
- `TDE__state26__Delete__state1__state41`
- `TDE__state26__Delete__state1__state40`
- `TDE__state26__Delete__state1__state4`
- `TDE__state26__Delete__state1__state3`
- `TDE__state26__Delete__state1__state2`
- `TDE__state62__Save and Send__state1__state45`
- `TDE__state62__Save and Send__state1__state60`
- `TDE__state62__Save and Send__state1__state4`
- `TDE__state62__Save and Send__state1__state3`
- `TDE__state62__Save and Send__state1__state2`
- `TDE__state47__Bone Removal Auto__state45__state66`
- `TDE__state47__Bone Removal Auto__state45__state65`
- `TDE__state47__Bone Removal Auto__state45__state63`
- `TDE__state47__Bone Removal Auto__state45__state62`
- `TDE__state26__Add into 
Demo List__state1__state26`
- `TDE__state26__Add into 
Demo List__state1__state14`
- `TDE__state26__Add into 
Demo List__state1__state13`
- `TDE__state26__Add into 
Demo List__state1__state39`
- `TDE__state26__Add into 
Demo List__state1__state41`
- `TDE__state26__Add into 
Demo List__state1__state40`
- `TDE__state26__Add into 
Demo List__state1__state4`
- `TDE__state26__Add into 
Demo List__state1__state3`
- `TDE__state26__Add into 
Demo List__state1__state2`
- `TDE__state45__Save and Send__state1__state60`
- `TDE__state45__Save and Send__state1__state4`
- `TDE__state45__Save and Send__state1__state3`
- `TDE__state45__Save and Send__state1__state2`
- `TDE__state63__Save and Send__state1__state45`
- `TDE__state63__Save and Send__state1__state60`
- `TDE__state63__Save and Send__state1__state4`
- `TDE__state63__Save and Send__state1__state3`
- `TDE__state63__Save and Send__state1__state2`
- `TDE__state47__Structure Isolation__state45__state66`
- `TDE__state47__Structure Isolation__state45__state65`
- `TDE__state47__Structure Isolation__state45__state63`
- `TDE__state47__Structure Isolation__state45__state62`
- `TDE__state65__Save and Pause__state1__state45`
- `TDE__state65__Save and Pause__state1__state60`
- `TDE__state65__Save and Pause__state1__state4`
- `TDE__state65__Save and Pause__state1__state3`
- `TDE__state65__Save and Pause__state1__state2`
- `TDE__state59__Time curve__state45__state73`
- `TDE__state30__Hide Lines__state45__state44`
- `TDE__state30__Hide Lines__state45__state46`
- `TDE__state59__Time ROI__state45__state73`
- `TDE__state46__Save and Pause__state1__state61`
- `TDE__state46__Save and Pause__state1__state60`
- `TDE__state46__Save and Pause__state1__state4`
- `TDE__state46__Save and Pause__state1__state3`
- `TDE__state46__Save and Pause__state1__state2`
- `TDE__state47__Home Position__state45__state66`
- `TDE__state47__Home Position__state45__state65`
- `TDE__state47__Home Position__state45__state63`
- `TDE__state47__Home Position__state45__state62`
- `TDE__state68__Rename Layout__state1__state4`
- `TDE__state68__Rename Layout__state1__state3`
- `TDE__state68__Rename Layout__state1__state2`
- `TDE__state26__Drag data
into Patient
tab__state1__state26`
- `TDE__state26__Drag data
into Patient
tab__state1__state14`
- `TDE__state26__Drag data
into Patient
tab__state1__state13`
- `TDE__state26__Drag data
into Patient
tab__state1__state39`
- `TDE__state26__Drag data
into Patient
tab__state1__state41`
- `TDE__state26__Drag data
into Patient
tab__state1__state40`
- `TDE__state26__Drag data
into Patient
tab__state1__state4`
- `TDE__state26__Drag data
into Patient
tab__state1__state3`
- `TDE__state26__Drag data
into Patient
tab__state1__state2`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state14`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state13`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state4`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state3`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state2`
- `TDE__state66__Save and Pause__state1__state64`
- `TDE__state66__Save and Pause__state1__state60`
- `TDE__state66__Save and Pause__state1__state4`
- `TDE__state66__Save and Pause__state1__state3`
- `TDE__state66__Save and Pause__state1__state2`
- `TDE__state59__Show Vessel__state45__state73`
- `TDE__state73__drag icon to the
Favorite Tools 
Panel__state1__state77`
- `TDE__state73__drag icon to the
Favorite Tools 
Panel__state1__state60`
- `TDE__state73__drag icon to the
Favorite Tools 
Panel__state1__state4`
- `TDE__state73__drag icon to the
Favorite Tools 
Panel__state1__state3`
- `TDE__state73__drag icon to the
Favorite Tools 
Panel__state1__state2`
- `TDE__state47__Save as Series__state45__state66`
- `TDE__state47__Save as Series__state45__state65`
- `TDE__state47__Save as Series__state45__state63`
- `TDE__state47__Save as Series__state45__state62`
- `TDE__state30__Align__state45__state44`
- `TDE__state30__Align__state45__state46`
- `TDE__state78__export
findings__state1__state4`
- `TDE__state78__export
findings__state1__state3`
- `TDE__state78__export
findings__state1__state2`
- `TDE__state26__double-click
to assign default 
workflow__state41__state26`
- `TDE__state26__double-click
to assign default 
workflow__state41__state39`
- `TDE__state77__Add to Favorites__state1__state4`
- `TDE__state77__Add to Favorites__state1__state3`
- `TDE__state77__Add to Favorites__state1__state2`
- `TDE__state47__Shutters On&Off__state45__state66`
- `TDE__state47__Shutters On&Off__state45__state65`
- `TDE__state47__Shutters On&Off__state45__state63`
- `TDE__state47__Shutters On&Off__state45__state62`
- `TDE__state30__Print Layout__state45__state44`
- `TDE__state30__Print Layout__state45__state46`
- `TDE__state47__Punch__state45__state66`
- `TDE__state47__Punch__state45__state65`
- `TDE__state47__Punch__state45__state63`
- `TDE__state47__Punch__state45__state62`
- `TDE__state58__Correct__state1__state58`
- `TDE__state58__Correct__state1__state14`
- `TDE__state58__Correct__state1__state13`
- `TDE__state58__Correct__state1__state41`
- `TDE__state58__Correct__state1__state4`
- `TDE__state58__Correct__state1__state3`
- `TDE__state58__Correct__state1__state2`
- `TDE__state47__Radial Sliced
Ranges__state45__state66`
- `TDE__state47__Radial Sliced
Ranges__state45__state65`
- `TDE__state47__Radial Sliced
Ranges__state45__state63`
- `TDE__state47__Radial Sliced
Ranges__state45__state62`
- `TDE__state30__Print Image__state45__state44`
- `TDE__state30__Print Image__state45__state46`
- `TDE__state47__Fit to Acquisition
Size__state64__state45`
- `TDE__state47__Fit to Acquisition
Size__state64__state66`
- `TDE__state47__Fit to Acquisition
Size__state64__state65`
- `TDE__state47__Fit to Acquisition
Size__state64__state63`
- `TDE__state47__Fit to Acquisition
Size__state64__state62`
- `TDE__state30__Print Stack__state45__state44`
- `TDE__state30__Print Stack__state45__state46`
- `TDE__state24__Minimize__state1__state34`
- `TDE__state24__Minimize__state1__state33`
- `TDE__state24__Minimize__state1__state10`
- `TDE__state24__Minimize__state1__state31`
- `TDE__state24__Minimize__state1__state36`
- `TDE__state24__Minimize__state1__state35`
- `TDE__state24__Minimize__state1__state30`
- `TDE__state24__Minimize__state1__state4`
- `TDE__state24__Minimize__state1__state3`
- `TDE__state24__Minimize__state1__state2`
- `TDE__state47__Region Growing__state67__state45`
- `TDE__state47__Region Growing__state67__state66`
- `TDE__state47__Region Growing__state67__state65`
- `TDE__state47__Region Growing__state67__state64`
- `TDE__state47__Region Growing__state67__state63`
- `TDE__state47__Region Growing__state67__state62`
- `TDE__state24__Layout Gallery__state33__state35`
- `TDE__state39__Correct__state1__state58`
- `TDE__state39__Correct__state1__state14`
- `TDE__state39__Correct__state1__state13`
- `TDE__state39__Correct__state1__state39`
- `TDE__state39__Correct__state1__state41`
- `TDE__state39__Correct__state1__state4`
- `TDE__state39__Correct__state1__state3`
- `TDE__state39__Correct__state1__state2`
- `TDE__state47__Zoom&Pan__state45__state66`
- `TDE__state47__Zoom&Pan__state45__state65`
- `TDE__state47__Zoom&Pan__state45__state63`
- `TDE__state47__Zoom&Pan__state45__state62`
- `TDE__state39__double-click
to assign default 
workflow__state41__state58`
- `TDE__state39__double-click
to assign default 
workflow__state41__state39`
- `TDE__state10__Minimize__state1__state5`
- `TDE__state10__Minimize__state1__state7`
- `TDE__state10__Minimize__state1__state6`
- `TDE__state10__Minimize__state1__state9`
- `TDE__state10__Minimize__state1__state8`
- `TDE__state10__Minimize__state1__state4`
- `TDE__state10__Minimize__state1__state3`
- `TDE__state10__Minimize__state1__state2`
- `TDE__state58__double-click
to assign default 
workflow__state41__state58`
- `TDE__state47__Edit Result__state45__state66`
- `TDE__state47__Edit Result__state45__state65`
- `TDE__state47__Edit Result__state45__state63`
- `TDE__state47__Edit Result__state45__state62`
- `TDE__state75__Replace__state1__state4`
- `TDE__state75__Replace__state1__state3`
- `TDE__state75__Replace__state1__state2`
- `TDE__state34__Change View__state1__state53`
- `TDE__state34__Change View__state1__state52`
- `TDE__state34__Change View__state1__state4`
- `TDE__state34__Change View__state1__state3`
- `TDE__state34__Change View__state1__state2`
- `TDE__state24__Configuration 
Panel__state1__state34`
- `TDE__state24__Configuration 
Panel__state1__state33`
- `TDE__state24__Configuration 
Panel__state1__state10`
- `TDE__state24__Configuration 
Panel__state1__state31`
- `TDE__state24__Configuration 
Panel__state1__state36`
- `TDE__state24__Configuration 
Panel__state1__state35`
- `TDE__state24__Configuration 
Panel__state1__state30`
- `TDE__state24__Configuration 
Panel__state1__state4`
- `TDE__state24__Configuration 
Panel__state1__state3`
- `TDE__state24__Configuration 
Panel__state1__state2`
- `TDE__state47__Fit to Segment__state64__state45`
- `TDE__state47__Fit to Segment__state64__state66`
- `TDE__state47__Fit to Segment__state64__state65`
- `TDE__state47__Fit to Segment__state64__state63`
- `TDE__state47__Fit to Segment__state64__state62`
- `TDE__state47__Bone Opacity__state45__state66`
- `TDE__state47__Bone Opacity__state45__state65`
- `TDE__state47__Bone Opacity__state45__state63`
- `TDE__state47__Bone Opacity__state45__state62`
- `TDE__state26__Correct__state1__state26`
- `TDE__state26__Correct__state1__state14`
- `TDE__state26__Correct__state1__state13`
- `TDE__state26__Correct__state1__state39`
- `TDE__state26__Correct__state1__state41`
- `TDE__state26__Correct__state1__state40`
- `TDE__state26__Correct__state1__state4`
- `TDE__state26__Correct__state1__state3`
- `TDE__state26__Correct__state1__state2`
- `TDE__state47__Curved 
Ranges__state45__state66`
- `TDE__state47__Curved 
Ranges__state45__state65`
- `TDE__state47__Curved 
Ranges__state45__state63`
- `TDE__state47__Curved 
Ranges__state45__state62`
- `TDE__state59__NextStudy&
PreviousStudy__state45__state73`
- `TDE__state47__Table Removal__state67__state45`
- `TDE__state47__Table Removal__state67__state66`
- `TDE__state47__Table Removal__state67__state65`
- `TDE__state47__Table Removal__state67__state64`
- `TDE__state47__Table Removal__state67__state63`
- `TDE__state47__Table Removal__state67__state62`
- `TDE__state19__Add search 
condition field__state18__state17`
- `TDE__state39__Print__state1__state58`
- `TDE__state39__Print__state1__state14`
- `TDE__state39__Print__state1__state13`
- `TDE__state39__Print__state1__state39`
- `TDE__state39__Print__state1__state41`
- `TDE__state39__Print__state1__state4`
- `TDE__state39__Print__state1__state3`
- `TDE__state39__Print__state1__state2`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state14`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state13`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state4`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state3`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state2`
- `TDE__state75__Insert before__state1__state4`
- `TDE__state75__Insert before__state1__state3`
- `TDE__state75__Insert before__state1__state2`
- `TDE__state26__Import__state1__state26`
- `TDE__state26__Import__state1__state14`
- `TDE__state26__Import__state1__state13`
- `TDE__state26__Import__state1__state39`
- `TDE__state26__Import__state1__state41`
- `TDE__state26__Import__state1__state40`
- `TDE__state26__Import__state1__state4`
- `TDE__state26__Import__state1__state3`
- `TDE__state26__Import__state1__state2`
- `TDE__state17__Remove search 
condition field__state19__state17`
- `TDE__state58__Print__state1__state58`
- `TDE__state58__Print__state1__state14`
- `TDE__state58__Print__state1__state13`
- `TDE__state58__Print__state1__state41`
- `TDE__state58__Print__state1__state4`
- `TDE__state58__Print__state1__state3`
- `TDE__state58__Print__state1__state2`
- `TDE__state17__Add search 
condition field__state18__state17`
- `TDE__state47__Scroll__state45__state66`
- `TDE__state47__Scroll__state45__state65`
- `TDE__state47__Scroll__state45__state63`
- `TDE__state47__Scroll__state45__state62`
- `TDE__state47__Spine 
Ranges__state45__state66`
- `TDE__state47__Spine 
Ranges__state45__state65`
- `TDE__state47__Spine 
Ranges__state45__state63`
- `TDE__state47__Spine 
Ranges__state45__state62`
- `TDE__state25__Asterisk to add
to favorite 
workflows__state1__state38`
- `TDE__state25__Asterisk to add
to favorite 
workflows__state1__state4`
- `TDE__state25__Asterisk to add
to favorite 
workflows__state1__state3`
- `TDE__state25__Asterisk to add
to favorite 
workflows__state1__state2`
- `TDE__state16__Abort search__state1__state26`
- `TDE__state16__Abort search__state1__state4`
- `TDE__state16__Abort search__state1__state3`
- `TDE__state16__Abort search__state1__state2`
- `TDE__state47__Vascular 
Ranges__state45__state66`
- `TDE__state47__Vascular 
Ranges__state45__state65`
- `TDE__state47__Vascular 
Ranges__state45__state63`
- `TDE__state47__Vascular 
Ranges__state45__state62`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state53`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state52`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state4`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state3`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state2`
- `TDE__state10__Configuration 
Panel__state1__state5`
- `TDE__state10__Configuration 
Panel__state1__state7`
- `TDE__state10__Configuration 
Panel__state1__state6`
- `TDE__state10__Configuration 
Panel__state1__state9`
- `TDE__state10__Configuration 
Panel__state1__state8`
- `TDE__state10__Configuration 
Panel__state1__state4`
- `TDE__state10__Configuration 
Panel__state1__state3`
- `TDE__state10__Configuration 
Panel__state1__state2`
- `TDE__state26__Print__state1__state26`
- `TDE__state26__Print__state1__state14`
- `TDE__state26__Print__state1__state13`
- `TDE__state26__Print__state1__state39`
- `TDE__state26__Print__state1__state41`
- `TDE__state26__Print__state1__state40`
- `TDE__state26__Print__state1__state4`
- `TDE__state26__Print__state1__state3`
- `TDE__state26__Print__state1__state2`
- `TDE__state39__Import__state1__state58`
- `TDE__state39__Import__state1__state14`
- `TDE__state39__Import__state1__state13`
- `TDE__state39__Import__state1__state39`
- `TDE__state39__Import__state1__state41`
- `TDE__state39__Import__state1__state4`
- `TDE__state39__Import__state1__state3`
- `TDE__state39__Import__state1__state2`
- `TDE__state59__Assisted 
Perpendicular__state45__state73`
- `TDE__state47__Clip Box__state45__state66`
- `TDE__state47__Clip Box__state45__state65`
- `TDE__state47__Clip Box__state45__state63`
- `TDE__state47__Clip Box__state45__state62`
- `TDE__state47__Clip Plane__state45__state66`
- `TDE__state47__Clip Plane__state45__state65`
- `TDE__state47__Clip Plane__state45__state63`
- `TDE__state47__Clip Plane__state45__state62`
- `TDE__state47__Visual Alignment__state45__state66`
- `TDE__state47__Visual Alignment__state45__state65`
- `TDE__state47__Visual Alignment__state45__state63`
- `TDE__state47__Visual Alignment__state45__state62`
- `TDE__state47__Correlated Cursors__state45__state66`
- `TDE__state47__Correlated Cursors__state45__state65`
- `TDE__state47__Correlated Cursors__state45__state63`
- `TDE__state47__Correlated Cursors__state45__state62`
- `TDE__state24__Online 
Help__state1__state34`
- `TDE__state24__Online 
Help__state1__state33`
- `TDE__state24__Online 
Help__state1__state10`
- `TDE__state24__Online 
Help__state1__state31`
- `TDE__state24__Online 
Help__state1__state36`
- `TDE__state24__Online 
Help__state1__state35`
- `TDE__state24__Online 
Help__state1__state30`
- `TDE__state24__Online 
Help__state1__state4`
- `TDE__state24__Online 
Help__state1__state3`
- `TDE__state24__Online 
Help__state1__state2`
- `TDE__state24__Close__state1__state34`
- `TDE__state24__Close__state1__state33`
- `TDE__state24__Close__state1__state10`
- `TDE__state24__Close__state1__state31`
- `TDE__state24__Close__state1__state36`
- `TDE__state24__Close__state1__state35`
- `TDE__state24__Close__state1__state30`
- `TDE__state24__Close__state1__state4`
- `TDE__state24__Close__state1__state3`
- `TDE__state24__Close__state1__state2`
- `TDE__state47__Anatomy Visualizer__state45__state66`
- `TDE__state47__Anatomy Visualizer__state45__state65`
- `TDE__state47__Anatomy Visualizer__state45__state63`
- `TDE__state47__Anatomy Visualizer__state45__state62`
- `TDE__state62__Save and Pause__state1__state45`
- `TDE__state62__Save and Pause__state1__state60`
- `TDE__state62__Save and Pause__state1__state4`
- `TDE__state62__Save and Pause__state1__state3`
- `TDE__state62__Save and Pause__state1__state2`
- `TDE__state47__Windowing__state45__state66`
- `TDE__state47__Windowing__state45__state65`
- `TDE__state47__Windowing__state45__state63`
- `TDE__state47__Windowing__state45__state62`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state53`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state52`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state4`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state3`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state2`
- `TDE__state39__Delete__state1__state58`
- `TDE__state39__Delete__state1__state14`
- `TDE__state39__Delete__state1__state13`
- `TDE__state39__Delete__state1__state39`
- `TDE__state39__Delete__state1__state41`
- `TDE__state39__Delete__state1__state4`
- `TDE__state39__Delete__state1__state3`
- `TDE__state39__Delete__state1__state2`
- `TDE__state43__confirm__state1__state4`
- `TDE__state43__confirm__state1__state3`
- `TDE__state43__confirm__state1__state2`
- `TDE__state58__Delete__state1__state58`
- `TDE__state58__Delete__state1__state14`
- `TDE__state58__Delete__state1__state13`
- `TDE__state58__Delete__state1__state41`
- `TDE__state58__Delete__state1__state4`
- `TDE__state58__Delete__state1__state3`
- `TDE__state58__Delete__state1__state2`
- `TDE__state10__Online 
Help__state1__state5`
- `TDE__state10__Online 
Help__state1__state7`
- `TDE__state10__Online 
Help__state1__state6`
- `TDE__state10__Online 
Help__state1__state9`
- `TDE__state10__Online 
Help__state1__state8`
- `TDE__state10__Online 
Help__state1__state4`
- `TDE__state10__Online 
Help__state1__state3`
- `TDE__state10__Online 
Help__state1__state2`
- `TDE__state26__Export__state1__state26`
- `TDE__state26__Export__state1__state14`
- `TDE__state26__Export__state1__state13`
- `TDE__state26__Export__state1__state39`
- `TDE__state26__Export__state1__state41`
- `TDE__state26__Export__state1__state40`
- `TDE__state26__Export__state1__state4`
- `TDE__state26__Export__state1__state3`
- `TDE__state26__Export__state1__state2`
- `TDE__state58__Import__state1__state58`
- `TDE__state58__Import__state1__state14`
- `TDE__state58__Import__state1__state13`
- `TDE__state58__Import__state1__state41`
- `TDE__state58__Import__state1__state4`
- `TDE__state58__Import__state1__state3`
- `TDE__state58__Import__state1__state2`
- `TDE__state47__Clip Plane Slab__state45__state66`
- `TDE__state47__Clip Plane Slab__state45__state65`
- `TDE__state47__Clip Plane Slab__state45__state63`
- `TDE__state47__Clip Plane Slab__state45__state62`
- `TDE__state73__Save and Pause__state1__state77`
- `TDE__state73__Save and Pause__state1__state60`
- `TDE__state73__Save and Pause__state1__state4`
- `TDE__state73__Save and Pause__state1__state3`
- `TDE__state73__Save and Pause__state1__state2`
- `TDE__state47__Edit Bone Removal__state45__state66`
- `TDE__state47__Edit Bone Removal__state45__state65`
- `TDE__state47__Edit Bone Removal__state45__state63`
- `TDE__state47__Edit Bone Removal__state45__state62`
- `TDE__state47__Bone Removal Body__state45__state66`
- `TDE__state47__Bone Removal Body__state45__state65`
- `TDE__state47__Bone Removal Body__state45__state63`
- `TDE__state47__Bone Removal Body__state45__state62`
- `TDE__state47__Parallel Ranges__state45__state66`
- `TDE__state47__Parallel Ranges__state45__state65`
- `TDE__state47__Parallel Ranges__state45__state63`
- `TDE__state47__Parallel Ranges__state45__state62`
- `TDE__state45__Save and Pause__state1__state60`
- `TDE__state45__Save and Pause__state1__state4`
- `TDE__state45__Save and Pause__state1__state3`
- `TDE__state45__Save and Pause__state1__state2`
- `TDE__state63__Save and Pause__state1__state45`
- `TDE__state63__Save and Pause__state1__state60`
- `TDE__state63__Save and Pause__state1__state4`
- `TDE__state63__Save and Pause__state1__state3`
- `TDE__state63__Save and Pause__state1__state2`
- `TDE__state74__Load Data__state1__state4`
- `TDE__state74__Load Data__state1__state3`
- `TDE__state74__Load Data__state1__state2`
- `TDE__state47__Automatic 
Registration__state45__state66`
- `TDE__state47__Automatic 
Registration__state45__state65`
- `TDE__state47__Automatic 
Registration__state45__state63`
- `TDE__state47__Automatic 
Registration__state45__state62`
- `TDE__state2__Delete the search 
criteria in the Quick
Search field__state8__state10`
- `TDE__state44__Save and Pause__state1__state59`
- `TDE__state44__Save and Pause__state1__state60`
- `TDE__state44__Save and Pause__state1__state4`
- `TDE__state44__Save and Pause__state1__state3`
- `TDE__state44__Save and Pause__state1__state2`
- `TDE__state47__Radial 
Ranges__state45__state66`
- `TDE__state47__Radial 
Ranges__state45__state65`
- `TDE__state47__Radial 
Ranges__state45__state63`
- `TDE__state47__Radial 
Ranges__state45__state62`
- `TDE__state10__Close__state1__state5`
- `TDE__state10__Close__state1__state7`
- `TDE__state10__Close__state1__state6`
- `TDE__state10__Close__state1__state9`
- `TDE__state10__Close__state1__state8`
- `TDE__state10__Close__state1__state4`
- `TDE__state10__Close__state1__state3`
- `TDE__state10__Close__state1__state2`
- `TDE__state46__Save and Send__state1__state61`
- `TDE__state46__Save and Send__state1__state60`
- `TDE__state46__Save and Send__state1__state4`
- `TDE__state46__Save and Send__state1__state3`
- `TDE__state46__Save and Send__state1__state2`
- `TDE__state73__Save and Send__state1__state77`
- `TDE__state73__Save and Send__state1__state60`
- `TDE__state73__Save and Send__state1__state4`
- `TDE__state73__Save and Send__state1__state3`
- `TDE__state73__Save and Send__state1__state2`
- `TDE__state64__Save and Send__state1__state60`
- `TDE__state64__Save and Send__state1__state4`
- `TDE__state64__Save and Send__state1__state3`
- `TDE__state64__Save and Send__state1__state2`
- `TDE__state58__Export__state1__state58`
- `TDE__state58__Export__state1__state14`
- `TDE__state58__Export__state1__state13`
- `TDE__state58__Export__state1__state41`
- `TDE__state58__Export__state1__state4`
- `TDE__state58__Export__state1__state3`
- `TDE__state58__Export__state1__state2`
- `TDE__state30__Custom text__state45__state44`
- `TDE__state30__Custom text__state45__state46`
- `TDE__state65__Save and Send__state1__state45`
- `TDE__state65__Save and Send__state1__state60`
- `TDE__state65__Save and Send__state1__state4`
- `TDE__state65__Save and Send__state1__state3`
- `TDE__state65__Save and Send__state1__state2`
- `TDE__state39__Export__state1__state58`
- `TDE__state39__Export__state1__state14`
- `TDE__state39__Export__state1__state13`
- `TDE__state39__Export__state1__state39`
- `TDE__state39__Export__state1__state41`
- `TDE__state39__Export__state1__state4`
- `TDE__state39__Export__state1__state3`
- `TDE__state39__Export__state1__state2`
- `TDE__state30__Full text__state45__state44`
- `TDE__state30__Full text__state45__state46`
- `TDE__state66__Save and Send__state1__state64`
- `TDE__state66__Save and Send__state1__state60`
- `TDE__state66__Save and Send__state1__state4`
- `TDE__state66__Save and Send__state1__state3`
- `TDE__state66__Save and Send__state1__state2`
- `TDE__state47__Rotate__state45__state66`
- `TDE__state47__Rotate__state45__state65`
- `TDE__state47__Rotate__state45__state63`
- `TDE__state47__Rotate__state45__state62`
- `TDE__state18__Remove search 
condition field__state19__state17`
- `TDE__state47__Ranges__state45__state66`
- `TDE__state47__Ranges__state45__state65`
- `TDE__state47__Ranges__state45__state63`
- `TDE__state47__Ranges__state45__state62`
- `TDE__state9__Remove search 
condition field__state19__state17`
- `TDE__state9__Add search 
condition field__state18__state17`
- `TDE__state30__Synch__state45__state44`
- `TDE__state30__Synch__state45__state46`

### Product 2

**Selected features:** selected = {MMR, ab, as, bl, br, clop, ct, fip, ftp, hi, iar, inl, layg, mfa, mrca, pat, pb, rel, sa, ser, sp, srl, sso, swl, tl, tob, tog, tr, vi, vip, wfl, wlb}

**Repaired FTS:** 70 states, 274 transitions (273 real / 1 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 112 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 273 | 0/273 = 0.0% | 0/273 = 0.0% | 95/273 = 34.8% | 273/273 = 100.0% | 273/273 = 100.0% | 116/273 = 42.5% |
| ActionExchange | 1945 | 0/1945 = 0.0% | 0/1945 = 0.0% | 863/1945 = 44.4% | 1945/1945 = 100.0% | 1945/1945 = 100.0% | 689/1945 = 35.4% |
| StateMissing | 69 | 0/69 = 0.0% | 0/69 = 0.0% | 69/69 = 100.0% | 69/69 = 100.0% | 69/69 = 100.0% | 55/69 = 79.7% |
| TransitionDestinationExchange | 1228 | 508/1228 = 41.4% | 0/720 = 0.0% | 293/720 = 40.7% | 667/720 = 92.6% | 720/720 = 100.0% | 338/720 = 46.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (508):

- `TDE__state30__Hide Graphics__state45__state44`
- `TDE__state30__Hide Graphics__state45__state46`
- `TDE__state61__Remove from 
Favorites__state1__state3`
- `TDE__state61__Remove from 
Favorites__state1__state2`
- `TDE__state47__Bone Removal Head__state45__state66`
- `TDE__state47__Bone Removal Head__state45__state65`
- `TDE__state47__Bone Removal Head__state45__state63`
- `TDE__state47__Bone Removal Head__state45__state62`
- `TDE__state39__select instance
from Instances list__state58__state39`
- `TDE__state59__2D Distortion 
Correction__state45__state73`
- `TDE__state47__Bone Removal Leg__state45__state66`
- `TDE__state47__Bone Removal Leg__state45__state65`
- `TDE__state47__Bone Removal Leg__state45__state63`
- `TDE__state47__Bone Removal Leg__state45__state62`
- `TDE__state60__Discard Changes__state1__state3`
- `TDE__state60__Discard Changes__state1__state2`
- `TDE__state47__Lock Reference
Lines__state45__state66`
- `TDE__state47__Lock Reference
Lines__state45__state65`
- `TDE__state47__Lock Reference
Lines__state45__state63`
- `TDE__state47__Lock Reference
Lines__state45__state62`
- `TDE__state30__Align Timepoints__state45__state44`
- `TDE__state30__Align Timepoints__state45__state46`
- `TDE__state58__Drag data
into Patient
tab__state1__state58`
- `TDE__state58__Drag data
into Patient
tab__state1__state14`
- `TDE__state58__Drag data
into Patient
tab__state1__state13`
- `TDE__state58__Drag data
into Patient
tab__state1__state41`
- `TDE__state58__Drag data
into Patient
tab__state1__state3`
- `TDE__state58__Drag data
into Patient
tab__state1__state2`
- `TDE__state44__Save and Send__state1__state59`
- `TDE__state44__Save and Send__state1__state60`
- `TDE__state44__Save and Send__state1__state3`
- `TDE__state44__Save and Send__state1__state2`
- `TDE__state39__Drag data
into Patient
tab__state1__state58`
- `TDE__state39__Drag data
into Patient
tab__state1__state14`
- `TDE__state39__Drag data
into Patient
tab__state1__state13`
- `TDE__state39__Drag data
into Patient
tab__state1__state39`
- `TDE__state39__Drag data
into Patient
tab__state1__state41`
- `TDE__state39__Drag data
into Patient
tab__state1__state3`
- `TDE__state39__Drag data
into Patient
tab__state1__state2`
- `TDE__state24__LayoutGallery icon__state35__state33`
- `TDE__state26__Delete__state1__state26`
- `TDE__state26__Delete__state1__state14`
- `TDE__state26__Delete__state1__state13`
- `TDE__state26__Delete__state1__state39`
- `TDE__state26__Delete__state1__state41`
- `TDE__state26__Delete__state1__state40`
- `TDE__state26__Delete__state1__state3`
- `TDE__state26__Delete__state1__state2`
- `TDE__state62__Save and Send__state1__state45`
- `TDE__state62__Save and Send__state1__state60`
- `TDE__state62__Save and Send__state1__state3`
- `TDE__state62__Save and Send__state1__state2`
- `TDE__state47__Bone Removal Auto__state45__state66`
- `TDE__state47__Bone Removal Auto__state45__state65`
- `TDE__state47__Bone Removal Auto__state45__state63`
- `TDE__state47__Bone Removal Auto__state45__state62`
- `TDE__state26__Add into 
Demo List__state1__state26`
- `TDE__state26__Add into 
Demo List__state1__state14`
- `TDE__state26__Add into 
Demo List__state1__state13`
- `TDE__state26__Add into 
Demo List__state1__state39`
- `TDE__state26__Add into 
Demo List__state1__state41`
- `TDE__state26__Add into 
Demo List__state1__state40`
- `TDE__state26__Add into 
Demo List__state1__state3`
- `TDE__state26__Add into 
Demo List__state1__state2`
- `TDE__state45__Save and Send__state1__state60`
- `TDE__state45__Save and Send__state1__state3`
- `TDE__state45__Save and Send__state1__state2`
- `TDE__state63__Save and Send__state1__state45`
- `TDE__state63__Save and Send__state1__state60`
- `TDE__state63__Save and Send__state1__state3`
- `TDE__state63__Save and Send__state1__state2`
- `TDE__state47__Structure Isolation__state45__state66`
- `TDE__state47__Structure Isolation__state45__state65`
- `TDE__state47__Structure Isolation__state45__state63`
- `TDE__state47__Structure Isolation__state45__state62`
- `TDE__state65__Save and Pause__state1__state45`
- `TDE__state65__Save and Pause__state1__state60`
- `TDE__state65__Save and Pause__state1__state3`
- `TDE__state65__Save and Pause__state1__state2`
- `TDE__state59__Time curve__state45__state73`
- `TDE__state30__Hide Lines__state45__state44`
- `TDE__state30__Hide Lines__state45__state46`
- `TDE__state59__Time ROI__state45__state73`
- `TDE__state46__Save and Pause__state1__state61`
- `TDE__state46__Save and Pause__state1__state60`
- `TDE__state46__Save and Pause__state1__state3`
- `TDE__state46__Save and Pause__state1__state2`
- `TDE__state47__Home Position__state45__state66`
- `TDE__state47__Home Position__state45__state65`
- `TDE__state47__Home Position__state45__state63`
- `TDE__state47__Home Position__state45__state62`
- `TDE__state68__Rename Layout__state1__state3`
- `TDE__state68__Rename Layout__state1__state2`
- `TDE__state26__Drag data
into Patient
tab__state1__state26`
- `TDE__state26__Drag data
into Patient
tab__state1__state14`
- `TDE__state26__Drag data
into Patient
tab__state1__state13`
- `TDE__state26__Drag data
into Patient
tab__state1__state39`
- `TDE__state26__Drag data
into Patient
tab__state1__state41`
- `TDE__state26__Drag data
into Patient
tab__state1__state40`
- `TDE__state26__Drag data
into Patient
tab__state1__state3`
- `TDE__state26__Drag data
into Patient
tab__state1__state2`
- `TDE__state59__Multiplication__state45__state73`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state14`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state13`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state3`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state2`
- `TDE__state66__Save and Pause__state1__state64`
- `TDE__state66__Save and Pause__state1__state60`
- `TDE__state66__Save and Pause__state1__state3`
- `TDE__state66__Save and Pause__state1__state2`
- `TDE__state59__Show Vessel__state45__state73`
- `TDE__state73__drag icon to the
Favorite Tools 
Panel__state1__state77`
- `TDE__state73__drag icon to the
Favorite Tools 
Panel__state1__state60`
- `TDE__state73__drag icon to the
Favorite Tools 
Panel__state1__state3`
- `TDE__state73__drag icon to the
Favorite Tools 
Panel__state1__state2`
- `TDE__state47__Save as Series__state45__state66`
- `TDE__state47__Save as Series__state45__state65`
- `TDE__state47__Save as Series__state45__state63`
- `TDE__state47__Save as Series__state45__state62`
- `TDE__state30__Align__state45__state44`
- `TDE__state30__Align__state45__state46`
- `TDE__state78__export
findings__state1__state3`
- `TDE__state78__export
findings__state1__state2`
- `TDE__state26__double-click
to assign default 
workflow__state41__state26`
- `TDE__state26__double-click
to assign default 
workflow__state41__state39`
- `TDE__state77__Add to Favorites__state1__state3`
- `TDE__state77__Add to Favorites__state1__state2`
- `TDE__state47__Shutters On&Off__state45__state66`
- `TDE__state47__Shutters On&Off__state45__state65`
- `TDE__state47__Shutters On&Off__state45__state63`
- `TDE__state47__Shutters On&Off__state45__state62`
- `TDE__state30__Print Layout__state45__state44`
- `TDE__state30__Print Layout__state45__state46`
- `TDE__state47__Punch__state45__state66`
- `TDE__state47__Punch__state45__state65`
- `TDE__state47__Punch__state45__state63`
- `TDE__state47__Punch__state45__state62`
- `TDE__state58__Correct__state1__state58`
- `TDE__state58__Correct__state1__state14`
- `TDE__state58__Correct__state1__state13`
- `TDE__state58__Correct__state1__state41`
- `TDE__state58__Correct__state1__state3`
- `TDE__state58__Correct__state1__state2`
- `TDE__state47__Radial Sliced
Ranges__state45__state66`
- `TDE__state47__Radial Sliced
Ranges__state45__state65`
- `TDE__state47__Radial Sliced
Ranges__state45__state63`
- `TDE__state47__Radial Sliced
Ranges__state45__state62`
- `TDE__state59__Image Filter&
Smooth__state45__state73`
- `TDE__state30__Print Image__state45__state44`
- `TDE__state30__Print Image__state45__state46`
- `TDE__state47__Fit to Acquisition
Size__state64__state45`
- `TDE__state47__Fit to Acquisition
Size__state64__state66`
- `TDE__state47__Fit to Acquisition
Size__state64__state65`
- `TDE__state47__Fit to Acquisition
Size__state64__state63`
- `TDE__state47__Fit to Acquisition
Size__state64__state62`
- `TDE__state30__Print Stack__state45__state44`
- `TDE__state30__Print Stack__state45__state46`
- `TDE__state24__Minimize__state1__state34`
- `TDE__state24__Minimize__state1__state33`
- `TDE__state24__Minimize__state1__state10`
- `TDE__state24__Minimize__state1__state31`
- `TDE__state24__Minimize__state1__state36`
- `TDE__state24__Minimize__state1__state35`
- `TDE__state24__Minimize__state1__state30`
- `TDE__state24__Minimize__state1__state3`
- `TDE__state24__Minimize__state1__state2`
- `TDE__state47__Region Growing__state67__state45`
- `TDE__state47__Region Growing__state67__state66`
- `TDE__state47__Region Growing__state67__state65`
- `TDE__state47__Region Growing__state67__state64`
- `TDE__state47__Region Growing__state67__state63`
- `TDE__state47__Region Growing__state67__state62`
- `TDE__state24__Layout Gallery__state33__state35`
- `TDE__state39__Correct__state1__state58`
- `TDE__state39__Correct__state1__state14`
- `TDE__state39__Correct__state1__state13`
- `TDE__state39__Correct__state1__state39`
- `TDE__state39__Correct__state1__state41`
- `TDE__state39__Correct__state1__state3`
- `TDE__state39__Correct__state1__state2`
- `TDE__state47__Zoom&Pan__state45__state66`
- `TDE__state47__Zoom&Pan__state45__state65`
- `TDE__state47__Zoom&Pan__state45__state63`
- `TDE__state47__Zoom&Pan__state45__state62`
- `TDE__state39__double-click
to assign default 
workflow__state41__state58`
- `TDE__state39__double-click
to assign default 
workflow__state41__state39`
- `TDE__state10__Minimize__state1__state5`
- `TDE__state10__Minimize__state1__state7`
- `TDE__state10__Minimize__state1__state6`
- `TDE__state10__Minimize__state1__state9`
- `TDE__state10__Minimize__state1__state8`
- `TDE__state10__Minimize__state1__state3`
- `TDE__state10__Minimize__state1__state2`
- `TDE__state58__double-click
to assign default 
workflow__state41__state58`
- `TDE__state47__Edit Result__state45__state66`
- `TDE__state47__Edit Result__state45__state65`
- `TDE__state47__Edit Result__state45__state63`
- `TDE__state47__Edit Result__state45__state62`
- `TDE__state75__Replace__state1__state3`
- `TDE__state75__Replace__state1__state2`
- `TDE__state34__Change View__state1__state53`
- `TDE__state34__Change View__state1__state52`
- `TDE__state34__Change View__state1__state3`
- `TDE__state34__Change View__state1__state2`
- `TDE__state24__Configuration 
Panel__state1__state34`
- `TDE__state24__Configuration 
Panel__state1__state33`
- `TDE__state24__Configuration 
Panel__state1__state10`
- `TDE__state24__Configuration 
Panel__state1__state31`
- `TDE__state24__Configuration 
Panel__state1__state36`
- `TDE__state24__Configuration 
Panel__state1__state35`
- `TDE__state24__Configuration 
Panel__state1__state30`
- `TDE__state24__Configuration 
Panel__state1__state3`
- `TDE__state24__Configuration 
Panel__state1__state2`
- `TDE__state59__Arithmetic Mean__state45__state73`
- `TDE__state47__Fit to Segment__state64__state45`
- `TDE__state47__Fit to Segment__state64__state66`
- `TDE__state47__Fit to Segment__state64__state65`
- `TDE__state47__Fit to Segment__state64__state63`
- `TDE__state47__Fit to Segment__state64__state62`
- `TDE__state47__Bone Opacity__state45__state66`
- `TDE__state47__Bone Opacity__state45__state65`
- `TDE__state47__Bone Opacity__state45__state63`
- `TDE__state47__Bone Opacity__state45__state62`
- `TDE__state26__Correct__state1__state26`
- `TDE__state26__Correct__state1__state14`
- `TDE__state26__Correct__state1__state13`
- `TDE__state26__Correct__state1__state39`
- `TDE__state26__Correct__state1__state41`
- `TDE__state26__Correct__state1__state40`
- `TDE__state26__Correct__state1__state3`
- `TDE__state26__Correct__state1__state2`
- `TDE__state47__Curved 
Ranges__state45__state66`
- `TDE__state47__Curved 
Ranges__state45__state65`
- `TDE__state47__Curved 
Ranges__state45__state63`
- `TDE__state47__Curved 
Ranges__state45__state62`
- `TDE__state59__Addition__state45__state73`
- `TDE__state59__NextStudy&
PreviousStudy__state45__state73`
- `TDE__state47__Table Removal__state67__state45`
- `TDE__state47__Table Removal__state67__state66`
- `TDE__state47__Table Removal__state67__state65`
- `TDE__state47__Table Removal__state67__state64`
- `TDE__state47__Table Removal__state67__state63`
- `TDE__state47__Table Removal__state67__state62`
- `TDE__state19__Add search 
condition field__state18__state17`
- `TDE__state59__Subtraction__state45__state73`
- `TDE__state39__Print__state1__state58`
- `TDE__state39__Print__state1__state14`
- `TDE__state39__Print__state1__state13`
- `TDE__state39__Print__state1__state39`
- `TDE__state39__Print__state1__state41`
- `TDE__state39__Print__state1__state3`
- `TDE__state39__Print__state1__state2`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state14`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state13`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state3`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state2`
- `TDE__state75__Insert before__state1__state3`
- `TDE__state75__Insert before__state1__state2`
- `TDE__state26__Import__state1__state26`
- `TDE__state26__Import__state1__state14`
- `TDE__state26__Import__state1__state13`
- `TDE__state26__Import__state1__state39`
- `TDE__state26__Import__state1__state41`
- `TDE__state26__Import__state1__state40`
- `TDE__state26__Import__state1__state3`
- `TDE__state26__Import__state1__state2`
- `TDE__state17__Remove search 
condition field__state19__state17`
- `TDE__state58__Print__state1__state58`
- `TDE__state58__Print__state1__state14`
- `TDE__state58__Print__state1__state13`
- `TDE__state58__Print__state1__state41`
- `TDE__state58__Print__state1__state3`
- `TDE__state58__Print__state1__state2`
- `TDE__state59__Coronary Tree__state45__state73`
- `TDE__state17__Add search 
condition field__state18__state17`
- `TDE__state47__Scroll__state45__state66`
- `TDE__state47__Scroll__state45__state65`
- `TDE__state47__Scroll__state45__state63`
- `TDE__state47__Scroll__state45__state62`
- `TDE__state47__Spine 
Ranges__state45__state66`
- `TDE__state47__Spine 
Ranges__state45__state65`
- `TDE__state47__Spine 
Ranges__state45__state63`
- `TDE__state47__Spine 
Ranges__state45__state62`
- `TDE__state25__Asterisk to add
to favorite 
workflows__state1__state38`
- `TDE__state25__Asterisk to add
to favorite 
workflows__state1__state3`
- `TDE__state25__Asterisk to add
to favorite 
workflows__state1__state2`
- `TDE__state16__Abort search__state1__state26`
- `TDE__state16__Abort search__state1__state3`
- `TDE__state16__Abort search__state1__state2`
- `TDE__state47__Vascular 
Ranges__state45__state66`
- `TDE__state47__Vascular 
Ranges__state45__state65`
- `TDE__state47__Vascular 
Ranges__state45__state63`
- `TDE__state47__Vascular 
Ranges__state45__state62`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state53`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state52`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state3`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state2`
- `TDE__state10__Configuration 
Panel__state1__state5`
- `TDE__state10__Configuration 
Panel__state1__state7`
- `TDE__state10__Configuration 
Panel__state1__state6`
- `TDE__state10__Configuration 
Panel__state1__state9`
- `TDE__state10__Configuration 
Panel__state1__state8`
- `TDE__state10__Configuration 
Panel__state1__state3`
- `TDE__state10__Configuration 
Panel__state1__state2`
- `TDE__state59__ADC&b-value__state45__state73`
- `TDE__state26__Print__state1__state26`
- `TDE__state26__Print__state1__state14`
- `TDE__state26__Print__state1__state13`
- `TDE__state26__Print__state1__state39`
- `TDE__state26__Print__state1__state41`
- `TDE__state26__Print__state1__state40`
- `TDE__state26__Print__state1__state3`
- `TDE__state26__Print__state1__state2`
- `TDE__state59__Division__state45__state73`
- `TDE__state39__Import__state1__state58`
- `TDE__state39__Import__state1__state14`
- `TDE__state39__Import__state1__state13`
- `TDE__state39__Import__state1__state39`
- `TDE__state39__Import__state1__state41`
- `TDE__state39__Import__state1__state3`
- `TDE__state39__Import__state1__state2`
- `TDE__state59__Assisted 
Perpendicular__state45__state73`
- `TDE__state47__Clip Box__state45__state66`
- `TDE__state47__Clip Box__state45__state65`
- `TDE__state47__Clip Box__state45__state63`
- `TDE__state47__Clip Box__state45__state62`
- `TDE__state47__Clip Plane__state45__state66`
- `TDE__state47__Clip Plane__state45__state65`
- `TDE__state47__Clip Plane__state45__state63`
- `TDE__state47__Clip Plane__state45__state62`
- `TDE__state47__Visual Alignment__state45__state66`
- `TDE__state47__Visual Alignment__state45__state65`
- `TDE__state47__Visual Alignment__state45__state63`
- `TDE__state47__Visual Alignment__state45__state62`
- `TDE__state47__Correlated Cursors__state45__state66`
- `TDE__state47__Correlated Cursors__state45__state65`
- `TDE__state47__Correlated Cursors__state45__state63`
- `TDE__state47__Correlated Cursors__state45__state62`
- `TDE__state24__Online 
Help__state1__state34`
- `TDE__state24__Online 
Help__state1__state33`
- `TDE__state24__Online 
Help__state1__state10`
- `TDE__state24__Online 
Help__state1__state31`
- `TDE__state24__Online 
Help__state1__state36`
- `TDE__state24__Online 
Help__state1__state35`
- `TDE__state24__Online 
Help__state1__state30`
- `TDE__state24__Online 
Help__state1__state3`
- `TDE__state24__Online 
Help__state1__state2`
- `TDE__state24__Close__state1__state34`
- `TDE__state24__Close__state1__state33`
- `TDE__state24__Close__state1__state10`
- `TDE__state24__Close__state1__state31`
- `TDE__state24__Close__state1__state36`
- `TDE__state24__Close__state1__state35`
- `TDE__state24__Close__state1__state30`
- `TDE__state24__Close__state1__state3`
- `TDE__state24__Close__state1__state2`
- `TDE__state47__Anatomy Visualizer__state45__state66`
- `TDE__state47__Anatomy Visualizer__state45__state65`
- `TDE__state47__Anatomy Visualizer__state45__state63`
- `TDE__state47__Anatomy Visualizer__state45__state62`
- `TDE__state62__Save and Pause__state1__state45`
- `TDE__state62__Save and Pause__state1__state60`
- `TDE__state62__Save and Pause__state1__state3`
- `TDE__state62__Save and Pause__state1__state2`
- `TDE__state59__Heart Isolation__state45__state73`
- `TDE__state47__Windowing__state45__state66`
- `TDE__state47__Windowing__state45__state65`
- `TDE__state47__Windowing__state45__state63`
- `TDE__state47__Windowing__state45__state62`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state53`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state52`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state3`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state2`
- `TDE__state39__Delete__state1__state58`
- `TDE__state39__Delete__state1__state14`
- `TDE__state39__Delete__state1__state13`
- `TDE__state39__Delete__state1__state39`
- `TDE__state39__Delete__state1__state41`
- `TDE__state39__Delete__state1__state3`
- `TDE__state39__Delete__state1__state2`
- `TDE__state43__confirm__state1__state3`
- `TDE__state43__confirm__state1__state2`
- `TDE__state58__Delete__state1__state58`
- `TDE__state58__Delete__state1__state14`
- `TDE__state58__Delete__state1__state13`
- `TDE__state58__Delete__state1__state41`
- `TDE__state58__Delete__state1__state3`
- `TDE__state58__Delete__state1__state2`
- `TDE__state10__Online 
Help__state1__state5`
- `TDE__state10__Online 
Help__state1__state7`
- `TDE__state10__Online 
Help__state1__state6`
- `TDE__state10__Online 
Help__state1__state9`
- `TDE__state10__Online 
Help__state1__state8`
- `TDE__state10__Online 
Help__state1__state3`
- `TDE__state10__Online 
Help__state1__state2`
- `TDE__state26__Export__state1__state26`
- `TDE__state26__Export__state1__state14`
- `TDE__state26__Export__state1__state13`
- `TDE__state26__Export__state1__state39`
- `TDE__state26__Export__state1__state41`
- `TDE__state26__Export__state1__state40`
- `TDE__state26__Export__state1__state3`
- `TDE__state26__Export__state1__state2`
- `TDE__state58__Import__state1__state58`
- `TDE__state58__Import__state1__state14`
- `TDE__state58__Import__state1__state13`
- `TDE__state58__Import__state1__state41`
- `TDE__state58__Import__state1__state3`
- `TDE__state58__Import__state1__state2`
- `TDE__state47__Clip Plane Slab__state45__state66`
- `TDE__state47__Clip Plane Slab__state45__state65`
- `TDE__state47__Clip Plane Slab__state45__state63`
- `TDE__state47__Clip Plane Slab__state45__state62`
- `TDE__state73__Save and Pause__state1__state77`
- `TDE__state73__Save and Pause__state1__state60`
- `TDE__state73__Save and Pause__state1__state3`
- `TDE__state73__Save and Pause__state1__state2`
- `TDE__state47__Edit Bone Removal__state45__state66`
- `TDE__state47__Edit Bone Removal__state45__state65`
- `TDE__state47__Edit Bone Removal__state45__state63`
- `TDE__state47__Edit Bone Removal__state45__state62`
- `TDE__state47__Bone Removal Body__state45__state66`
- `TDE__state47__Bone Removal Body__state45__state65`
- `TDE__state47__Bone Removal Body__state45__state63`
- `TDE__state47__Bone Removal Body__state45__state62`
- `TDE__state47__Parallel Ranges__state45__state66`
- `TDE__state47__Parallel Ranges__state45__state65`
- `TDE__state47__Parallel Ranges__state45__state63`
- `TDE__state47__Parallel Ranges__state45__state62`
- `TDE__state45__Save and Pause__state1__state60`
- `TDE__state45__Save and Pause__state1__state3`
- `TDE__state45__Save and Pause__state1__state2`
- `TDE__state63__Save and Pause__state1__state45`
- `TDE__state63__Save and Pause__state1__state60`
- `TDE__state63__Save and Pause__state1__state3`
- `TDE__state63__Save and Pause__state1__state2`
- `TDE__state74__Load Data__state1__state3`
- `TDE__state74__Load Data__state1__state2`
- `TDE__state47__Automatic 
Registration__state45__state66`
- `TDE__state47__Automatic 
Registration__state45__state65`
- `TDE__state47__Automatic 
Registration__state45__state63`
- `TDE__state47__Automatic 
Registration__state45__state62`
- `TDE__state2__Delete the search 
criteria in the Quick
Search field__state8__state10`
- `TDE__state44__Save and Pause__state1__state59`
- `TDE__state44__Save and Pause__state1__state60`
- `TDE__state44__Save and Pause__state1__state3`
- `TDE__state44__Save and Pause__state1__state2`
- `TDE__state47__Radial 
Ranges__state45__state66`
- `TDE__state47__Radial 
Ranges__state45__state65`
- `TDE__state47__Radial 
Ranges__state45__state63`
- `TDE__state47__Radial 
Ranges__state45__state62`
- `TDE__state10__Close__state1__state5`
- `TDE__state10__Close__state1__state7`
- `TDE__state10__Close__state1__state6`
- `TDE__state10__Close__state1__state9`
- `TDE__state10__Close__state1__state8`
- `TDE__state10__Close__state1__state3`
- `TDE__state10__Close__state1__state2`
- `TDE__state46__Save and Send__state1__state61`
- `TDE__state46__Save and Send__state1__state60`
- `TDE__state46__Save and Send__state1__state3`
- `TDE__state46__Save and Send__state1__state2`
- `TDE__state73__Save and Send__state1__state77`
- `TDE__state73__Save and Send__state1__state60`
- `TDE__state73__Save and Send__state1__state3`
- `TDE__state73__Save and Send__state1__state2`
- `TDE__state64__Save and Send__state1__state60`
- `TDE__state64__Save and Send__state1__state3`
- `TDE__state64__Save and Send__state1__state2`
- `TDE__state58__Export__state1__state58`
- `TDE__state58__Export__state1__state14`
- `TDE__state58__Export__state1__state13`
- `TDE__state58__Export__state1__state41`
- `TDE__state58__Export__state1__state3`
- `TDE__state58__Export__state1__state2`
- `TDE__state30__Custom text__state45__state44`
- `TDE__state30__Custom text__state45__state46`
- `TDE__state59__Motion 
Correction__state45__state73`
- `TDE__state65__Save and Send__state1__state45`
- `TDE__state65__Save and Send__state1__state60`
- `TDE__state65__Save and Send__state1__state3`
- `TDE__state65__Save and Send__state1__state2`
- `TDE__state39__Export__state1__state58`
- `TDE__state39__Export__state1__state14`
- `TDE__state39__Export__state1__state13`
- `TDE__state39__Export__state1__state39`
- `TDE__state39__Export__state1__state41`
- `TDE__state39__Export__state1__state3`
- `TDE__state39__Export__state1__state2`
- `TDE__state30__Full text__state45__state44`
- `TDE__state30__Full text__state45__state46`
- `TDE__state66__Save and Send__state1__state64`
- `TDE__state66__Save and Send__state1__state60`
- `TDE__state66__Save and Send__state1__state3`
- `TDE__state66__Save and Send__state1__state2`
- `TDE__state47__Rotate__state45__state66`
- `TDE__state47__Rotate__state45__state65`
- `TDE__state47__Rotate__state45__state63`
- `TDE__state47__Rotate__state45__state62`
- `TDE__state18__Remove search 
condition field__state19__state17`
- `TDE__state47__Ranges__state45__state66`
- `TDE__state47__Ranges__state45__state65`
- `TDE__state47__Ranges__state45__state63`
- `TDE__state47__Ranges__state45__state62`
- `TDE__state9__Remove search 
condition field__state19__state17`
- `TDE__state9__Add search 
condition field__state18__state17`
- `TDE__state30__Synch__state45__state44`
- `TDE__state30__Synch__state45__state46`

### Product 3

**Selected features:** selected = {USR, ab, as, bl, br, cli, clop, fnav, iar, inl, layg, layt, mfa, pat, pb, rel, sa, ser, sp, srl, swl, tl, tob, tr, vi, wfl, wlb, wst}

**Repaired FTS:** 63 states, 231 transitions (230 real / 1 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 107 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 230 | 0/230 = 0.0% | 0/230 = 0.0% | 86/230 = 37.4% | 230/230 = 100.0% | 230/230 = 100.0% | 107/230 = 46.5% |
| ActionExchange | 1659 | 0/1659 = 0.0% | 0/1659 = 0.0% | 730/1659 = 44.0% | 1659/1659 = 100.0% | 1659/1659 = 100.0% | 588/1659 = 35.4% |
| StateMissing | 62 | 0/62 = 0.0% | 0/62 = 0.0% | 62/62 = 100.0% | 62/62 = 100.0% | 62/62 = 100.0% | 46/62 = 74.2% |
| TransitionDestinationExchange | 1049 | 440/1049 = 41.9% | 0/609 = 0.0% | 246/609 = 40.4% | 555/609 = 91.1% | 609/609 = 100.0% | 295/609 = 48.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (440):

- `TDE__state54__Save As__state1__state4`
- `TDE__state54__Save As__state1__state3`
- `TDE__state47__Bone Removal Head__state45__state66`
- `TDE__state47__Bone Removal Head__state45__state65`
- `TDE__state47__Bone Removal Head__state45__state63`
- `TDE__state47__Bone Removal Head__state45__state62`
- `TDE__state39__select instance
from Instances list__state58__state39`
- `TDE__state47__Bone Removal Leg__state45__state66`
- `TDE__state47__Bone Removal Leg__state45__state65`
- `TDE__state47__Bone Removal Leg__state45__state63`
- `TDE__state47__Bone Removal Leg__state45__state62`
- `TDE__state60__Discard Changes__state1__state4`
- `TDE__state60__Discard Changes__state1__state3`
- `TDE__state47__Lock Reference
Lines__state45__state66`
- `TDE__state47__Lock Reference
Lines__state45__state65`
- `TDE__state47__Lock Reference
Lines__state45__state63`
- `TDE__state47__Lock Reference
Lines__state45__state62`
- `TDE__state58__Drag data
into Patient
tab__state1__state58`
- `TDE__state58__Drag data
into Patient
tab__state1__state14`
- `TDE__state58__Drag data
into Patient
tab__state1__state13`
- `TDE__state58__Drag data
into Patient
tab__state1__state41`
- `TDE__state58__Drag data
into Patient
tab__state1__state4`
- `TDE__state58__Drag data
into Patient
tab__state1__state3`
- `TDE__state39__Drag data
into Patient
tab__state1__state58`
- `TDE__state39__Drag data
into Patient
tab__state1__state14`
- `TDE__state39__Drag data
into Patient
tab__state1__state13`
- `TDE__state39__Drag data
into Patient
tab__state1__state39`
- `TDE__state39__Drag data
into Patient
tab__state1__state41`
- `TDE__state39__Drag data
into Patient
tab__state1__state4`
- `TDE__state39__Drag data
into Patient
tab__state1__state3`
- `TDE__state26__Delete__state1__state26`
- `TDE__state26__Delete__state1__state14`
- `TDE__state26__Delete__state1__state13`
- `TDE__state26__Delete__state1__state39`
- `TDE__state26__Delete__state1__state41`
- `TDE__state26__Delete__state1__state40`
- `TDE__state26__Delete__state1__state4`
- `TDE__state26__Delete__state1__state3`
- `TDE__state62__Save and Send__state1__state45`
- `TDE__state62__Save and Send__state1__state60`
- `TDE__state62__Save and Send__state1__state4`
- `TDE__state62__Save and Send__state1__state3`
- `TDE__state47__Bone Removal Auto__state45__state66`
- `TDE__state47__Bone Removal Auto__state45__state65`
- `TDE__state47__Bone Removal Auto__state45__state63`
- `TDE__state47__Bone Removal Auto__state45__state62`
- `TDE__state26__Add into 
Demo List__state1__state26`
- `TDE__state26__Add into 
Demo List__state1__state14`
- `TDE__state26__Add into 
Demo List__state1__state13`
- `TDE__state26__Add into 
Demo List__state1__state39`
- `TDE__state26__Add into 
Demo List__state1__state41`
- `TDE__state26__Add into 
Demo List__state1__state40`
- `TDE__state26__Add into 
Demo List__state1__state4`
- `TDE__state26__Add into 
Demo List__state1__state3`
- `TDE__state45__Save and Send__state1__state60`
- `TDE__state45__Save and Send__state1__state4`
- `TDE__state45__Save and Send__state1__state3`
- `TDE__state57__include findings 
in the report__state1__state4`
- `TDE__state57__include findings 
in the report__state1__state3`
- `TDE__state63__Save and Send__state1__state45`
- `TDE__state63__Save and Send__state1__state60`
- `TDE__state63__Save and Send__state1__state4`
- `TDE__state63__Save and Send__state1__state3`
- `TDE__state47__Structure Isolation__state45__state66`
- `TDE__state47__Structure Isolation__state45__state65`
- `TDE__state47__Structure Isolation__state45__state63`
- `TDE__state47__Structure Isolation__state45__state62`
- `TDE__state65__Save and Pause__state1__state45`
- `TDE__state65__Save and Pause__state1__state60`
- `TDE__state65__Save and Pause__state1__state4`
- `TDE__state65__Save and Pause__state1__state3`
- `TDE__state47__Home Position__state45__state66`
- `TDE__state47__Home Position__state45__state65`
- `TDE__state47__Home Position__state45__state63`
- `TDE__state47__Home Position__state45__state62`
- `TDE__state68__Rename Layout__state1__state4`
- `TDE__state68__Rename Layout__state1__state3`
- `TDE__state26__Drag data
into Patient
tab__state1__state26`
- `TDE__state26__Drag data
into Patient
tab__state1__state14`
- `TDE__state26__Drag data
into Patient
tab__state1__state13`
- `TDE__state26__Drag data
into Patient
tab__state1__state39`
- `TDE__state26__Drag data
into Patient
tab__state1__state41`
- `TDE__state26__Drag data
into Patient
tab__state1__state40`
- `TDE__state26__Drag data
into Patient
tab__state1__state4`
- `TDE__state26__Drag data
into Patient
tab__state1__state3`
- `TDE__state54__Save__state1__state4`
- `TDE__state54__Save__state1__state3`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state14`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state13`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state4`
- `TDE__state15__drag&drop from
More work 
list menu__state1__state3`
- `TDE__state66__Save and Pause__state1__state64`
- `TDE__state66__Save and Pause__state1__state60`
- `TDE__state66__Save and Pause__state1__state4`
- `TDE__state66__Save and Pause__state1__state3`
- `TDE__state47__Save as Series__state45__state66`
- `TDE__state47__Save as Series__state45__state65`
- `TDE__state47__Save as Series__state45__state63`
- `TDE__state47__Save as Series__state45__state62`
- `TDE__state26__double-click
to assign default 
workflow__state41__state26`
- `TDE__state26__double-click
to assign default 
workflow__state41__state39`
- `TDE__state47__Shutters On&Off__state45__state66`
- `TDE__state47__Shutters On&Off__state45__state65`
- `TDE__state47__Shutters On&Off__state45__state63`
- `TDE__state47__Shutters On&Off__state45__state62`
- `TDE__state47__Punch__state45__state66`
- `TDE__state47__Punch__state45__state65`
- `TDE__state47__Punch__state45__state63`
- `TDE__state47__Punch__state45__state62`
- `TDE__state58__Correct__state1__state58`
- `TDE__state58__Correct__state1__state14`
- `TDE__state58__Correct__state1__state13`
- `TDE__state58__Correct__state1__state41`
- `TDE__state58__Correct__state1__state4`
- `TDE__state58__Correct__state1__state3`
- `TDE__state47__Radial Sliced
Ranges__state45__state66`
- `TDE__state47__Radial Sliced
Ranges__state45__state65`
- `TDE__state47__Radial Sliced
Ranges__state45__state63`
- `TDE__state47__Radial Sliced
Ranges__state45__state62`
- `TDE__state47__Fit to Acquisition
Size__state64__state45`
- `TDE__state47__Fit to Acquisition
Size__state64__state66`
- `TDE__state47__Fit to Acquisition
Size__state64__state65`
- `TDE__state47__Fit to Acquisition
Size__state64__state63`
- `TDE__state47__Fit to Acquisition
Size__state64__state62`
- `TDE__state24__Minimize__state1__state34`
- `TDE__state24__Minimize__state1__state32`
- `TDE__state24__Minimize__state1__state10`
- `TDE__state24__Minimize__state1__state31`
- `TDE__state24__Minimize__state1__state37`
- `TDE__state24__Minimize__state1__state35`
- `TDE__state24__Minimize__state1__state4`
- `TDE__state24__Minimize__state1__state3`
- `TDE__state47__Region Growing__state67__state45`
- `TDE__state47__Region Growing__state67__state66`
- `TDE__state47__Region Growing__state67__state65`
- `TDE__state47__Region Growing__state67__state64`
- `TDE__state47__Region Growing__state67__state63`
- `TDE__state47__Region Growing__state67__state62`
- `TDE__state39__Correct__state1__state58`
- `TDE__state39__Correct__state1__state14`
- `TDE__state39__Correct__state1__state13`
- `TDE__state39__Correct__state1__state39`
- `TDE__state39__Correct__state1__state41`
- `TDE__state39__Correct__state1__state4`
- `TDE__state39__Correct__state1__state3`
- `TDE__state47__Zoom&Pan__state45__state66`
- `TDE__state47__Zoom&Pan__state45__state65`
- `TDE__state47__Zoom&Pan__state45__state63`
- `TDE__state47__Zoom&Pan__state45__state62`
- `TDE__state39__double-click
to assign default 
workflow__state41__state58`
- `TDE__state39__double-click
to assign default 
workflow__state41__state39`
- `TDE__state10__Minimize__state1__state5`
- `TDE__state10__Minimize__state1__state7`
- `TDE__state10__Minimize__state1__state6`
- `TDE__state10__Minimize__state1__state9`
- `TDE__state10__Minimize__state1__state8`
- `TDE__state10__Minimize__state1__state4`
- `TDE__state10__Minimize__state1__state3`
- `TDE__state58__double-click
to assign default 
workflow__state41__state58`
- `TDE__state47__Edit Result__state45__state66`
- `TDE__state47__Edit Result__state45__state65`
- `TDE__state47__Edit Result__state45__state63`
- `TDE__state47__Edit Result__state45__state62`
- `TDE__state75__Replace__state1__state4`
- `TDE__state75__Replace__state1__state3`
- `TDE__state32__confirm info__state1__state4`
- `TDE__state32__confirm info__state1__state3`
- `TDE__state34__Change View__state1__state53`
- `TDE__state34__Change View__state1__state52`
- `TDE__state34__Change View__state1__state4`
- `TDE__state34__Change View__state1__state3`
- `TDE__state24__Configuration 
Panel__state1__state34`
- `TDE__state24__Configuration 
Panel__state1__state32`
- `TDE__state24__Configuration 
Panel__state1__state10`
- `TDE__state24__Configuration 
Panel__state1__state31`
- `TDE__state24__Configuration 
Panel__state1__state37`
- `TDE__state24__Configuration 
Panel__state1__state35`
- `TDE__state24__Configuration 
Panel__state1__state4`
- `TDE__state24__Configuration 
Panel__state1__state3`
- `TDE__state47__Fit to Segment__state64__state45`
- `TDE__state47__Fit to Segment__state64__state66`
- `TDE__state47__Fit to Segment__state64__state65`
- `TDE__state47__Fit to Segment__state64__state63`
- `TDE__state47__Fit to Segment__state64__state62`
- `TDE__state47__Bone Opacity__state45__state66`
- `TDE__state47__Bone Opacity__state45__state65`
- `TDE__state47__Bone Opacity__state45__state63`
- `TDE__state47__Bone Opacity__state45__state62`
- `TDE__state26__Correct__state1__state26`
- `TDE__state26__Correct__state1__state14`
- `TDE__state26__Correct__state1__state13`
- `TDE__state26__Correct__state1__state39`
- `TDE__state26__Correct__state1__state41`
- `TDE__state26__Correct__state1__state40`
- `TDE__state26__Correct__state1__state4`
- `TDE__state26__Correct__state1__state3`
- `TDE__state47__Curved 
Ranges__state45__state66`
- `TDE__state47__Curved 
Ranges__state45__state65`
- `TDE__state47__Curved 
Ranges__state45__state63`
- `TDE__state47__Curved 
Ranges__state45__state62`
- `TDE__state47__Table Removal__state67__state45`
- `TDE__state47__Table Removal__state67__state66`
- `TDE__state47__Table Removal__state67__state65`
- `TDE__state47__Table Removal__state67__state64`
- `TDE__state47__Table Removal__state67__state63`
- `TDE__state47__Table Removal__state67__state62`
- `TDE__state19__Add search 
condition field__state18__state17`
- `TDE__state39__Print__state1__state58`
- `TDE__state39__Print__state1__state14`
- `TDE__state39__Print__state1__state13`
- `TDE__state39__Print__state1__state39`
- `TDE__state39__Print__state1__state41`
- `TDE__state39__Print__state1__state4`
- `TDE__state39__Print__state1__state3`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state14`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state13`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state4`
- `TDE__state5__drag&drop to
More work 
list menu__state1__state3`
- `TDE__state75__Insert before__state1__state4`
- `TDE__state75__Insert before__state1__state3`
- `TDE__state26__Import__state1__state26`
- `TDE__state26__Import__state1__state14`
- `TDE__state26__Import__state1__state13`
- `TDE__state26__Import__state1__state39`
- `TDE__state26__Import__state1__state41`
- `TDE__state26__Import__state1__state40`
- `TDE__state26__Import__state1__state4`
- `TDE__state26__Import__state1__state3`
- `TDE__state17__Remove search 
condition field__state19__state17`
- `TDE__state58__Print__state1__state58`
- `TDE__state58__Print__state1__state14`
- `TDE__state58__Print__state1__state13`
- `TDE__state58__Print__state1__state41`
- `TDE__state58__Print__state1__state4`
- `TDE__state58__Print__state1__state3`
- `TDE__state17__Add search 
condition field__state18__state17`
- `TDE__state47__Scroll__state45__state66`
- `TDE__state47__Scroll__state45__state65`
- `TDE__state47__Scroll__state45__state63`
- `TDE__state47__Scroll__state45__state62`
- `TDE__state47__Spine 
Ranges__state45__state66`
- `TDE__state47__Spine 
Ranges__state45__state65`
- `TDE__state47__Spine 
Ranges__state45__state63`
- `TDE__state47__Spine 
Ranges__state45__state62`
- `TDE__state25__Asterisk to add
to favorite 
workflows__state1__state38`
- `TDE__state25__Asterisk to add
to favorite 
workflows__state1__state4`
- `TDE__state25__Asterisk to add
to favorite 
workflows__state1__state3`
- `TDE__state16__Abort search__state1__state26`
- `TDE__state16__Abort search__state1__state4`
- `TDE__state16__Abort search__state1__state3`
- `TDE__state47__Vascular 
Ranges__state45__state66`
- `TDE__state47__Vascular 
Ranges__state45__state65`
- `TDE__state47__Vascular 
Ranges__state45__state63`
- `TDE__state47__Vascular 
Ranges__state45__state62`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state53`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state52`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state4`
- `TDE__state34__Expand&Collapse
Series Panel__state1__state3`
- `TDE__state10__Configuration 
Panel__state1__state5`
- `TDE__state10__Configuration 
Panel__state1__state7`
- `TDE__state10__Configuration 
Panel__state1__state6`
- `TDE__state10__Configuration 
Panel__state1__state9`
- `TDE__state10__Configuration 
Panel__state1__state8`
- `TDE__state10__Configuration 
Panel__state1__state4`
- `TDE__state10__Configuration 
Panel__state1__state3`
- `TDE__state26__Print__state1__state26`
- `TDE__state26__Print__state1__state14`
- `TDE__state26__Print__state1__state13`
- `TDE__state26__Print__state1__state39`
- `TDE__state26__Print__state1__state41`
- `TDE__state26__Print__state1__state40`
- `TDE__state26__Print__state1__state4`
- `TDE__state26__Print__state1__state3`
- `TDE__state39__Import__state1__state58`
- `TDE__state39__Import__state1__state14`
- `TDE__state39__Import__state1__state13`
- `TDE__state39__Import__state1__state39`
- `TDE__state39__Import__state1__state41`
- `TDE__state39__Import__state1__state4`
- `TDE__state39__Import__state1__state3`
- `TDE__state47__Clip Box__state45__state66`
- `TDE__state47__Clip Box__state45__state65`
- `TDE__state47__Clip Box__state45__state63`
- `TDE__state47__Clip Box__state45__state62`
- `TDE__state47__Clip Plane__state45__state66`
- `TDE__state47__Clip Plane__state45__state65`
- `TDE__state47__Clip Plane__state45__state63`
- `TDE__state47__Clip Plane__state45__state62`
- `TDE__state47__Visual Alignment__state45__state66`
- `TDE__state47__Visual Alignment__state45__state65`
- `TDE__state47__Visual Alignment__state45__state63`
- `TDE__state47__Visual Alignment__state45__state62`
- `TDE__state47__Correlated Cursors__state45__state66`
- `TDE__state47__Correlated Cursors__state45__state65`
- `TDE__state47__Correlated Cursors__state45__state63`
- `TDE__state47__Correlated Cursors__state45__state62`
- `TDE__state24__Online 
Help__state1__state34`
- `TDE__state24__Online 
Help__state1__state32`
- `TDE__state24__Online 
Help__state1__state10`
- `TDE__state24__Online 
Help__state1__state31`
- `TDE__state24__Online 
Help__state1__state37`
- `TDE__state24__Online 
Help__state1__state35`
- `TDE__state24__Online 
Help__state1__state4`
- `TDE__state24__Online 
Help__state1__state3`
- `TDE__state24__Close__state1__state34`
- `TDE__state24__Close__state1__state32`
- `TDE__state24__Close__state1__state10`
- `TDE__state24__Close__state1__state31`
- `TDE__state24__Close__state1__state37`
- `TDE__state24__Close__state1__state35`
- `TDE__state24__Close__state1__state4`
- `TDE__state24__Close__state1__state3`
- `TDE__state47__Anatomy Visualizer__state45__state66`
- `TDE__state47__Anatomy Visualizer__state45__state65`
- `TDE__state47__Anatomy Visualizer__state45__state63`
- `TDE__state47__Anatomy Visualizer__state45__state62`
- `TDE__state62__Save and Pause__state1__state45`
- `TDE__state62__Save and Pause__state1__state60`
- `TDE__state62__Save and Pause__state1__state4`
- `TDE__state62__Save and Pause__state1__state3`
- `TDE__state47__Windowing__state45__state66`
- `TDE__state47__Windowing__state45__state65`
- `TDE__state47__Windowing__state45__state63`
- `TDE__state47__Windowing__state45__state62`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state53`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state52`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state4`
- `TDE__state34__Expand&Collapse
Hierarchy__state1__state3`
- `TDE__state39__Delete__state1__state58`
- `TDE__state39__Delete__state1__state14`
- `TDE__state39__Delete__state1__state13`
- `TDE__state39__Delete__state1__state39`
- `TDE__state39__Delete__state1__state41`
- `TDE__state39__Delete__state1__state4`
- `TDE__state39__Delete__state1__state3`
- `TDE__state43__confirm__state1__state4`
- `TDE__state43__confirm__state1__state3`
- `TDE__state58__Delete__state1__state58`
- `TDE__state58__Delete__state1__state14`
- `TDE__state58__Delete__state1__state13`
- `TDE__state58__Delete__state1__state41`
- `TDE__state58__Delete__state1__state4`
- `TDE__state58__Delete__state1__state3`
- `TDE__state10__Online 
Help__state1__state5`
- `TDE__state10__Online 
Help__state1__state7`
- `TDE__state10__Online 
Help__state1__state6`
- `TDE__state10__Online 
Help__state1__state9`
- `TDE__state10__Online 
Help__state1__state8`
- `TDE__state10__Online 
Help__state1__state4`
- `TDE__state10__Online 
Help__state1__state3`
- `TDE__state26__Export__state1__state26`
- `TDE__state26__Export__state1__state14`
- `TDE__state26__Export__state1__state13`
- `TDE__state26__Export__state1__state39`
- `TDE__state26__Export__state1__state41`
- `TDE__state26__Export__state1__state40`
- `TDE__state26__Export__state1__state4`
- `TDE__state26__Export__state1__state3`
- `TDE__state58__Import__state1__state58`
- `TDE__state58__Import__state1__state14`
- `TDE__state58__Import__state1__state13`
- `TDE__state58__Import__state1__state41`
- `TDE__state58__Import__state1__state4`
- `TDE__state58__Import__state1__state3`
- `TDE__state47__Clip Plane Slab__state45__state66`
- `TDE__state47__Clip Plane Slab__state45__state65`
- `TDE__state47__Clip Plane Slab__state45__state63`
- `TDE__state47__Clip Plane Slab__state45__state62`
- `TDE__state47__Edit Bone Removal__state45__state66`
- `TDE__state47__Edit Bone Removal__state45__state65`
- `TDE__state47__Edit Bone Removal__state45__state63`
- `TDE__state47__Edit Bone Removal__state45__state62`
- `TDE__state47__Bone Removal Body__state45__state66`
- `TDE__state47__Bone Removal Body__state45__state65`
- `TDE__state47__Bone Removal Body__state45__state63`
- `TDE__state47__Bone Removal Body__state