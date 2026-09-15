# Per-Product Mutation Report — Tesla

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

**Selected features:** selected = {5Y, AD, AW, AWD, C, CI, Ca, FSD, MS, S, S5, T, UR, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 33/33 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 2

**Selected features:** selected = {5Y, ACT, AD, AWL, C, CI, Ca, FSD, MC, MX, PD, QS, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 59/81 = 72.8% | 81/81 = 100.0% | 81/81 = 100.0% | 70/81 = 86.4% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 56/70 = 80.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 3

**Selected features:** selected = {AD, AWD, AWL, CI, CW, Ca, F, MX, S, S5, SB, T, TP, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 27/29 = 93.1% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 31/33 = 93.9% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 4

**Selected features:** selected = {ABI, ACT, AD, AWD, AWL, CC, CW, Ca, F, FSD, MX, S, S7, SG, T, TP, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 52/77 = 67.5% | 77/77 = 100.0% | 77/77 = 100.0% | 70/77 = 90.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 53/66 = 80.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 5

**Selected features:** selected = {AD, AWD, AWL, BWI, C, CW, Ca, FSD, MC, MX, S, S5, T, TP, UR, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 6

**Selected features:** selected = {AD, CI, Ca, DAP, F, FSD, MS, PD, S, S5, SS, T, TeW, UR, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 30/34 = 88.2% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 44/51 = 86.3% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 35/46 = 76.1% | 45/46 = 97.8% | 46/46 = 100.0% | 40/46 = 87.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state18__Sunshade__state22__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`

### Product 7

**Selected features:** selected = {ABI, AD, AW, AWD, Ca, DAP, L, MS, Mo, QS, S, S5, T, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 8

**Selected features:** selected = {AD, AWL, CI, Ca, DBM, F, FSD, MS, Mo, PD, S, S5, SS, T, TeW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 51/55 = 92.7% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 42/50 = 84.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Sunshade__state22__state18`

### Product 9

**Selected features:** selected = {AD, C, CI, Ca, DAP, FSD, MC, MS, PD, PWh, RR, S, S5, T, TeW, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 34/39 = 87.2% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 50/73 = 68.5% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 35/62 = 56.5% | 58/62 = 93.5% | 62/62 = 100.0% | 47/62 = 75.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state16__Roof 
Rack__state23__state28`
- `TDE__state28__Roof 
Rack__state23__state26`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state18__Roof 
Rack__state23__state28`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 10

**Selected features:** selected = {AD, CC, CI, CW, Ca, F, FSD, MX, Mo, PD, S, S6, T, TP, UR, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 32/34 = 94.1% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 42/45 = 93.3% | 45/45 = 100.0% | 45/45 = 100.0% | 44/45 = 97.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 40/43 = 93.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state18`

### Product 11

**Selected features:** selected = {AD, AWD, AWL, BWI, Ca, F, FSD, HC, MX, S, S5, SB, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 49/55 = 89.1% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 38/50 = 76.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`

### Product 12

**Selected features:** selected = {AD, AWL, BWI, Ca, FSD, GW, L, LRAWD, MY, Mo, S, S7, SG, SS, T, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 51/55 = 92.7% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 42/50 = 84.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Sunshade__state22__state18`

### Product 13

**Selected features:** selected = {AD, AWL, CC, CI, CW, Ca, DAP, F, MC, MX, Mo, PD, S, S6, SG, T, TP, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 59/81 = 72.8% | 81/81 = 100.0% | 81/81 = 100.0% | 73/81 = 90.1% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 66/70 = 94.3% | 70/70 = 100.0% | 61/70 = 87.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 14

**Selected features:** selected = {AD, AWD, BWI, CC, Ca, DAP, F, FSD, HC, MX, S, S6, T, TP, TuW, UR, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 38/41 = 92.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 71/77 = 92.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 63/66 = 95.5% | 66/66 = 100.0% | 51/66 = 77.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`

### Product 15

**Selected features:** selected = {AD, BWI, CCT, Ca, DBM, F, FSD, M3, NW, RWD, S, S5, T, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 34/37 = 91.9% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 33/35 = 94.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 16

**Selected features:** selected = {AD, AW, AWL, CI, Ca, DAP, HC, L, MC, MS, Mo, PD, PWh, S, S5, T, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 38/41 = 92.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 54/77 = 70.1% | 77/77 = 100.0% | 77/77 = 100.0% | 71/77 = 92.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 56/66 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 17

**Selected features:** selected = {5Y, ABI, ACT, AD, C, Ca, DBM, FSD, MX, PD, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 42/45 = 93.3% | 45/45 = 100.0% | 45/45 = 100.0% | 43/45 = 95.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 36/43 = 83.7% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 18

**Selected features:** selected = {5Y, ACT, AD, AWL, BWI, C, CC, CW, Ca, DAP, FSD, HC, MX, PD, PWh, S, S6, T, TP, WS}

**Repaired FTS:** 33 states, 56 transitions (56 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 37/56 = 66.1% | 56/56 = 100.0% | 56/56 = 100.0% | 50/56 = 89.3% |
| ActionExchange | 157 | 0/157 = 0.0% | 0/157 = 0.0% | 86/157 = 54.8% | 157/157 = 100.0% | 157/157 = 100.0% | 138/157 = 87.9% |
| StateMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| TransitionDestinationExchange | 157 | 36/157 = 22.9% | 0/121 = 0.0% | 52/121 = 43.0% | 105/121 = 86.8% | 121/121 = 100.0% | 88/121 = 72.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (36):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 19

**Selected features:** selected = {AD, AWD, CI, Ca, DAP, FSD, L, MC, MS, Mo, RR, S, S5, SB, SS, T, TeW, WS}

**Repaired FTS:** 31 states, 48 transitions (48 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 48 | 0/48 = 0.0% | 0/48 = 0.0% | 34/48 = 70.8% | 48/48 = 100.0% | 48/48 = 100.0% | 44/48 = 91.7% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 72/113 = 63.7% | 113/113 = 100.0% | 113/113 = 100.0% | 106/113 = 93.8% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 113 | 17/113 = 15.0% | 0/96 = 0.0% | 46/96 = 47.9% | 82/96 = 85.4% | 96/96 = 100.0% | 78/96 = 81.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (17):

- `TDE__state18__Sunshade__state22__state26`
- `TDE__state18__Sunshade__state22__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state16__Roof 
Rack__state23__state28`
- `TDE__state28__Roof 
Rack__state23__state26`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state18__Roof 
Rack__state23__state28`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state28__Sunshade__state22__state26`

### Product 20

**Selected features:** selected = {AD, AWD, AWL, C, CI, CW, Ca, FSD, MX, S, S7, SB, T, TP, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 21

**Selected features:** selected = {AD, BWI, Ca, F, GW, HC, LRRWD, MC, MY, S, S5, T, TP, UR, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 22

**Selected features:** selected = {AD, AWD, AWL, BWI, CW, Ca, HC, L, MC, MX, PWh, S, S5, T, TP, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 46/50 = 92.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 23

**Selected features:** selected = {AD, AWD, CI, CW, Ca, DBM, F, FSD, MC, MX, S, S7, T, TP, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state18`

### Product 24

**Selected features:** selected = {ACT, AD, AWD, AWL, C, CC, CI, CW, Ca, DAP, FSD, HC, MC, MX, S, S6, T, TP, UR, YS}

**Repaired FTS:** 33 states, 62 transitions (62 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 20 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 62 | 0/62 = 0.0% | 0/62 = 0.0% | 35/62 = 56.5% | 62/62 = 100.0% | 62/62 = 100.0% | 54/62 = 87.1% |
| ActionExchange | 211 | 0/211 = 0.0% | 0/211 = 0.0% | 93/211 = 44.1% | 211/211 = 100.0% | 211/211 = 100.0% | 194/211 = 91.9% |
| StateMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| TransitionDestinationExchange | 211 | 57/211 = 27.0% | 0/154 = 0.0% | 53/154 = 34.4% | 130/154 = 84.4% | 154/154 = 100.0% | 112/154 = 72.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (57):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 25

**Selected features:** selected = {AD, AWL, CI, Ca, DAP, F, HC, MX, Mo, PD, QS, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 38/38 = 100.0% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 46/59 = 78.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 38/54 = 70.4% | 53/54 = 98.1% | 54/54 = 100.0% | 53/54 = 98.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 26

**Selected features:** selected = {ACT, AD, AWD, BWI, Ca, HC, L, MX, S, S7, SB, T, TP, TuW, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 27

**Selected features:** selected = {5Y, ABI, AD, C, Ca, FSD, GW, HC, LRAWD, MY, S, S7, SG, SS, T, TP, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 36/38 = 94.7% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 46/59 = 78.0% | 59/59 = 100.0% | 59/59 = 100.0% | 55/59 = 93.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 38/54 = 70.4% | 53/54 = 98.1% | 54/54 = 100.0% | 48/54 = 88.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state18__Sunshade__state22__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state18`

### Product 28

**Selected features:** selected = {5Y, ABI, ACT, AD, AWL, C, CC, CW, Ca, DAP, MC, MX, PD, S, S6, SG, T, TP, YS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 35/49 = 71.4% | 49/49 = 100.0% | 49/49 = 100.0% | 40/49 = 81.6% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 69/113 = 61.1% | 113/113 = 100.0% | 113/113 = 100.0% | 83/113 = 73.5% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 30/31 = 96.8% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 47/92 = 51.1% | 85/92 = 92.4% | 92/92 = 100.0% | 61/92 = 66.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`

### Product 29

**Selected features:** selected = {AD, AWL, BWI, C, Ca, FSD, GW, HC, LRAWD, MC, MY, S, S7, SB, T, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 33/39 = 84.6% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 50/73 = 68.5% | 73/73 = 100.0% | 73/73 = 100.0% | 63/73 = 86.3% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 35/62 = 56.5% | 59/62 = 95.2% | 62/62 = 100.0% | 46/62 = 74.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 30

**Selected features:** selected = {ABI, AD, AWL, Ca, GW, HC, L, LRAWD, MC, MY, Mo, PWh, S, S5, T, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 45/50 = 90.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 31

**Selected features:** selected = {5Y, ABI, AD, C, CCT, Ca, GW, LRAWD, MC, MY, S, S7, SB, T, TP, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 32/34 = 94.1% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 42/45 = 93.3% | 45/45 = 100.0% | 45/45 = 100.0% | 45/45 = 100.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 41/43 = 95.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Center 
Console 
Trays__state25__state26`

### Product 32

**Selected features:** selected = {ABI, ACT, AD, AWD, AWL, CC, Ca, DAP, F, FSD, HC, MC, MX, Mo, S, S6, T, TP, TuW, UR, WS}

**Repaired FTS:** 34 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 21 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 38/64 = 59.4% | 64/64 = 100.0% | 64/64 = 100.0% | 52/64 = 81.3% |
| ActionExchange | 215 | 0/215 = 0.0% | 0/215 = 0.0% | 100/215 = 46.5% | 215/215 = 100.0% | 215/215 = 100.0% | 172/215 = 80.0% |
| StateMissing | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 33/33 = 100.0% | 33/33 = 100.0% | 33/33 = 100.0% | 33/33 = 100.0% |
| TransitionDestinationExchange | 215 | 57/215 = 26.5% | 0/158 = 0.0% | 58/158 = 36.7% | 131/158 = 82.9% | 158/158 = 100.0% | 108/158 = 68.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (57):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 33

**Selected features:** selected = {5Y, AD, AWD, AWL, C, CI, Ca, DAP, MS, PWh, S, S5, SS, T, TeW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 53/55 = 96.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Sunshade__state22__state28`

### Product 34

**Selected features:** selected = {ABI, AD, AWL, Ca, F, GW, LRRWD, MY, S, S5, T, TP, UR, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 27/29 = 93.1% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 31/33 = 93.9% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 35

**Selected features:** selected = {ABI, AD, Ca, F, FSD, HC, M3, Mo, PAWD, QS, RR, S, S5, T, WS, WW}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 46/55 = 83.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 38/50 = 76.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state18__Roof 
Rack__state23__state27`

### Product 36

**Selected features:** selected = {ABI, AD, CCT, Ca, DBM, FSD, IW, L, LRAWD, MY, Mo, S, S5, T, TP, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 32/34 = 94.1% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 42/45 = 93.3% | 45/45 = 100.0% | 45/45 = 100.0% | 44/45 = 97.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 37

**Selected features:** selected = {ACT, AD, AWD, C, CC, CI, CW, Ca, HC, MC, MX, QS, S, S7, T, TP, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 36/41 = 87.8% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 57/77 = 74.0% | 77/77 = 100.0% | 77/77 = 100.0% | 67/77 = 87.0% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 51/66 = 77.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state27__Car Cover__state21__state26`

### Product 38

**Selected features:** selected = {ABI, ACT, AD, AWL, Ca, DAP, F, HC, MC, MX, PD, S, S6, SG, T, TP, TuW, WS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 42/47 = 89.4% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 64/109 = 58.7% | 109/109 = 100.0% | 109/109 = 100.0% | 98/109 = 89.9% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 43/88 = 48.9% | 81/88 = 92.0% | 88/88 = 100.0% | 70/88 = 79.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 39

**Selected features:** selected = {ABI, ACT, AD, AWD, CC, CW, Ca, DAP, FSD, HC, L, MC, MX, S, S6, SG, T, TP, YS}

**Repaired FTS:** 32 states, 54 transitions (54 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 54 | 0/54 = 0.0% | 0/54 = 0.0% | 34/54 = 63.0% | 54/54 = 100.0% | 54/54 = 100.0% | 44/54 = 81.5% |
| ActionExchange | 153 | 0/153 = 0.0% | 0/153 = 0.0% | 81/153 = 52.9% | 153/153 = 100.0% | 153/153 = 100.0% | 123/153 = 80.4% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 153 | 36/153 = 23.5% | 0/117 = 0.0% | 49/117 = 41.9% | 103/117 = 88.0% | 117/117 = 100.0% | 79/117 = 67.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (36):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 40

**Selected features:** selected = {ABI, AD, AWD, AWL, CC, Ca, F, FSD, MC, MX, Mo, S, S7, SG, T, TP, TuW, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 55/81 = 67.9% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 66/70 = 94.3% | 70/70 = 100.0% | 57/70 = 81.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Car Cover__state21__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 41

**Selected features:** selected = {AD, AW, AWD, C, CI, Ca, DAP, HC, MS, S, S5, SG, T, YS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 27/35 = 77.1% | 35/35 = 100.0% | 35/35 = 100.0% | 35/35 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 42

**Selected features:** selected = {ABI, AD, Ca, FSD, GW, HC, L, LRAWD, MY, QS, S, S5, T, TP, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`

### Product 43

**Selected features:** selected = {5Y, AD, AWD, AWL, BWI, C, CW, Ca, HC, MX, QS, S, S5, T, TP, YS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 30/34 = 88.2% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 40/45 = 88.9% | 45/45 = 100.0% | 45/45 = 100.0% | 44/45 = 97.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 41/43 = 95.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`

### Product 44

**Selected features:** selected = {5Y, AD, AW, BWI, C, Ca, DAP, HC, MC, MS, PD, RR, S, S5, T, UR, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 38/41 = 92.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 57/77 = 74.0% | 77/77 = 100.0% | 77/77 = 100.0% | 71/77 = 92.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 55/66 = 83.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state28`
- `TDE__state28__Roof 
Rack__state23__state27`
- `TDE__state28__Roof 
Rack__state23__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state27__Roof 
Rack__state23__state26`

### Product 45

**Selected features:** selected = {ABI, ACT, AD, AWD, AWL, C, CC, CW, Ca, DBM, MC, MX, S, S6, T, TP, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 35/41 = 85.4% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 63/77 = 81.8% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 51/66 = 77.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 46

**Selected features:** selected = {AD, AWD, AWL, BWI, C, CW, Ca, MC, MX, S, S5, SB, T, TP, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`

### Product 47

**Selected features:** selected = {ABI, AD, AWD, C, Ca, MC, MS, PWh, S, S5, SS, T, TeW, YS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 27/35 = 77.1% | 35/35 = 100.0% | 35/35 = 100.0% | 35/35 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Sunshade__state22__state26`

### Product 48

**Selected features:** selected = {AD, AWL, BWI, CCT, Ca, DBM, FSD, IW, L, LRAWD, MC, MY, S, S5, T, TP, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 33/41 = 80.5% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 60/77 = 77.9% | 77/77 = 100.0% | 77/77 = 100.0% | 69/77 = 89.6% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 56/66 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Center 
Console 
Trays__state25__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 49

**Selected features:** selected = {AD, AWD, AWL, CI, Ca, DAP, F, FSD, MC, MS, Mo, S, S5, SB, SS, T, TeW, WS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 34/47 = 72.3% | 47/47 = 100.0% | 47/47 = 100.0% | 40/47 = 85.1% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 70/109 = 64.2% | 109/109 = 100.0% | 109/109 = 100.0% | 93/109 = 85.3% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 45/88 = 51.1% | 81/88 = 92.0% | 88/88 = 100.0% | 66/88 = 75.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state18__Sunshade__state22__state26`
- `TDE__state18__Sunshade__state22__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state28__Sunshade__state22__state26`

### Product 50

**Selected features:** selected = {AD, AW, AWD, C, CI, Ca, HC, MC, MS, RR, S, S5, SG, T, YS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 44/51 = 86.3% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 32/46 = 69.6% | 45/46 = 97.8% | 46/46 = 100.0% | 38/46 = 82.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Roof 
Rack__state23__state27`
- `TDE__state17__Roof 
Rack__state23__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state27__Roof 
Rack__state23__state26`

### Product 51

**Selected features:** selected = {ABI, AD, AWD, Ca, HC, L, MC, MX, S, S7, SB, T, TP, TuW, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 52

**Selected features:** selected = {ABI, AD, Ca, F, LRRWD, M3, Mo, PW, PWh, RR, S, S5, T, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 26/29 = 89.7% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 30/33 = 90.9% | 33/33 = 100.0% | 33/33 = 100.0% | 33/33 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 27/32 = 84.4% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 53

**Selected features:** selected = {AD, AW, AWD, BWI, Ca, DAP, F, FSD, HC, MS, Mo, QS, S, S5, T, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`

### Product 54

**Selected features:** selected = {AD, AWL, CC, CI, CW, Ca, FSD, HC, L, MX, PD, S, S6, SB, T, TP, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 34/41 = 82.9% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 54/77 = 70.1% | 77/77 = 100.0% | 77/77 = 100.0% | 59/77 = 76.6% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 46/66 = 69.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`

### Product 55

**Selected features:** selected = {5Y, ACT, AD, AWD, BWI, C, CW, Ca, DAP, DBM, HC, MX, S, S6, T, TP, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 35/38 = 92.1% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 49/59 = 83.1% | 59/59 = 100.0% | 59/59 = 100.0% | 55/59 = 93.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 46/54 = 85.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 56

**Selected features:** selected = {AD, AWL, BWI, Ca, HC, L, M3, MC, NW, PWh, RR, RWD, S, S5, T, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 29/39 = 74.4% | 39/39 = 100.0% | 39/39 = 100.0% | 33/39 = 84.6% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 47/73 = 64.4% | 73/73 = 100.0% | 73/73 = 100.0% | 58/73 = 79.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 35/62 = 56.5% | 59/62 = 95.2% | 62/62 = 100.0% | 44/62 = 71.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__Roof 
Rack__state23__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state27__Roof 
Rack__state23__state26`
- `TDE__state27__Roof 
Rack__state23__state24`

### Product 57

**Selected features:** selected = {ABI, AD, CCT, Ca, FSD, HC, IW, L, LRAWD, MY, S, S5, SG, T, TP, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 50/55 = 90.9% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 41/50 = 82.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 58

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, C, CW, Ca, MX, S, S6, SB, T, TP, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 59

**Selected features:** selected = {AD, AWD, AWL, BWI, Ca, F, HC, MX, PWh, S, S5, T, TP, TuW, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`

### Product 60

**Selected features:** selected = {5Y, ABI, AD, AWL, C, Ca, DAP, HC, MC, MS, PD, RR, S, S5, SG, SS, T, TeW, WS}

**Repaired FTS:** 32 states, 55 transitions (55 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 34/55 = 61.8% | 55/55 = 100.0% | 55/55 = 100.0% | 47/55 = 85.5% |
| ActionExchange | 159 | 0/159 = 0.0% | 0/159 = 0.0% | 81/159 = 50.9% | 159/159 = 100.0% | 159/159 = 100.0% | 135/159 = 84.9% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 159 | 27/159 = 17.0% | 0/132 = 0.0% | 50/132 = 37.9% | 105/132 = 79.5% | 132/132 = 100.0% | 88/132 = 66.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (27):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state27__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state28__Roof 
Rack__state23__state27`
- `TDE__state28__Roof 
Rack__state23__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state28__Sunshade__state22__state27`
- `TDE__state28__Sunshade__state22__state26`
- `TDE__state27__Roof 
Rack__state23__state26`

### Product 61

**Selected features:** selected = {ABI, AD, CCT, Ca, FSD, L, M3, Mo, NW, RWD, S, S5, SB, T, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 34/39 = 87.2% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 62

**Selected features:** selected = {5Y, ABI, AD, AWL, C, CC, CW, Ca, DAP, HC, MC, MX, PD, PWh, S, S6, T, TP, YS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 35/49 = 71.4% | 49/49 = 100.0% | 49/49 = 100.0% | 37/49 = 75.5% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 70/113 = 61.9% | 113/113 = 100.0% | 113/113 = 100.0% | 74/113 = 65.5% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 30/31 = 96.8% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 46/92 = 50.0% | 85/92 = 92.4% | 92/92 = 100.0% | 52/92 = 56.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 63

**Selected features:** selected = {AD, AW, AWD, CI, Ca, DAP, F, MS, Mo, S, S5, SS, T, UR, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 26/27 = 96.3% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 34/39 = 87.2% | 39/39 = 100.0% | 39/39 = 100.0% | 34/39 = 87.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state28`

### Product 64

**Selected features:** selected = {AD, AWD, BWI, C, CC, CW, Ca, DAP, MC, MX, QS, S, S7, T, TP, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 50/55 = 90.9% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 42/50 = 84.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`

### Product 65

**Selected features:** selected = {AD, BWI, C, Ca, DBM, IW, LRRWD, MC, MY, S, S5, SS, T, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 34/37 = 91.9% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 35/35 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state26`

### Product 66

**Selected features:** selected = {ABI, AD, AWD, CW, Ca, F, FSD, HC, MC, MX, Mo, S, S6, SB, T, TP, YS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 35/38 = 92.1% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 49/59 = 83.1% | 59/59 = 100.0% | 59/59 = 100.0% | 55/59 = 93.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 47/54 = 87.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`

### Product 67

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, CW, Ca, F, HC, MX, S, S7, SB, T, TP, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 45/55 = 81.8% | 55/55 = 100.0% | 55/55 = 100.0% | 54/55 = 98.2% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 36/50 = 72.0% | 49/50 = 98.0% | 50/50 = 100.0% | 45/50 = 90.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`

### Product 68

**Selected features:** selected = {AD, AWD, AWL, CC, CI, Ca, DAP, DBM, F, MC, MX, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 38/41 = 92.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 51/77 = 66.2% | 77/77 = 100.0% | 77/77 = 100.0% | 72/77 = 93.5% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 62/66 = 93.9% | 66/66 = 100.0% | 57/66 = 86.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 69

**Selected features:** selected = {AD, AWD, C, CC, CI, CW, Ca, HC, MC, MX, S, S6, SG, T, TP, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 48/55 = 87.3% | 55/55 = 100.0% | 55/55 = 100.0% | 50/55 = 90.9% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 36/50 = 72.0% | 49/50 = 98.0% | 50/50 = 100.0% | 41/50 = 82.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state27__Car Cover__state21__state26`

### Product 70

**Selected features:** selected = {ABI, ACT, AD, AWD, CC, CW, Ca, DAP, F, FSD, HC, MC, MX, S, S5, T, TP, UR, WS}

**Repaired FTS:** 32 states, 54 transitions (54 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 54 | 0/54 = 0.0% | 0/54 = 0.0% | 35/54 = 64.8% | 54/54 = 100.0% | 54/54 = 100.0% | 43/54 = 79.6% |
| ActionExchange | 153 | 0/153 = 0.0% | 0/153 = 0.0% | 84/153 = 54.9% | 153/153 = 100.0% | 153/153 = 100.0% | 112/153 = 73.2% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 153 | 36/153 = 23.5% | 0/117 = 0.0% | 49/117 = 41.9% | 101/117 = 86.3% | 117/117 = 100.0% | 71/117 = 60.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (36):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 71

**Selected features:** selected = {ABI, AD, AWL, Ca, F, FSD, MC, MS, Mo, PD, S, S5, SG, T, TeW, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 53/55 = 96.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 45/50 = 90.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 72

**Selected features:** selected = {AD, AW, AWD, AWL, CI, Ca, DAP, F, MC, MS, Mo, S, S5, SG, T, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 28/36 = 77.8% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 42/55 = 76.4% | 55/55 = 100.0% | 55/55 = 100.0% | 49/55 = 89.1% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 42/50 = 84.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 73

**Selected features:** selected = {AD, AWD, AWL, BWI, CC, Ca, DAP, F, MC, MX, Mo, S, S7, T, TP, TuW, UR, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 55/81 = 67.9% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 66/70 = 94.3% | 70/70 = 100.0% | 57/70 = 81.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 74

**Selected features:** selected = {AD, CI, CW, Ca, HC, L, MC, MX, Mo, PD, S, S6, SG, T, TP, YS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 42/45 = 93.3% | 45/45 = 100.0% | 45/45 = 100.0% | 45/45 = 100.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 36/43 = 83.7% | 43/43 = 100.0% | 43/43 = 100.0% | 41/43 = 95.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state27`

### Product 75

**Selected features:** selected = {AD, AW, AWL, CI, Ca, L, MS, PD, S, S5, SG, SS, T, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 27/35 = 77.1% | 35/35 = 100.0% | 35/35 = 100.0% | 33/35 = 94.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 76

**Selected features:** selected = {ABI, AD, AWD, Ca, DAP, FSD, HC, L, MC, MS, PWh, S, S5, SS, T, TeW, WS}

**Repaired FTS:** 30 states, 45 transitions (45 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 31/45 = 68.9% | 45/45 = 100.0% | 45/45 = 100.0% | 36/45 = 80.0% |
| ActionExchange | 105 | 0/105 = 0.0% | 0/105 = 0.0% | 60/105 = 57.1% | 105/105 = 100.0% | 105/105 = 100.0% | 83/105 = 79.0% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 105 | 21/105 = 20.0% | 0/84 = 0.0% | 38/84 = 45.2% | 76/84 = 90.5% | 84/84 = 100.0% | 52/84 = 61.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state27__Sunshade__state22__state26`
- `TDE__state18__Sunshade__state22__state27`
- `TDE__state18__Sunshade__state22__state26`
- `TDE__state18__Sunshade__state22__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state28__Sunshade__state22__state27`
- `TDE__state28__Sunshade__state22__state26`

### Product 77

**Selected features:** selected = {AD, BWI, Ca, F, FSD, GW, LRAWD, MY, S, S7, T, TP, UR, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 78

**Selected features:** selected = {5Y, AD, AWL, BWI, C, CCT, Ca, FSD, HC, IW, LRAWD, MY, S, S5, SB, SS, T, WS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 34/47 = 72.3% | 47/47 = 100.0% | 47/47 = 100.0% | 37/47 = 78.7% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 70/109 = 64.2% | 109/109 = 100.0% | 109/109 = 100.0% | 75/109 = 68.8% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 44/88 = 50.0% | 80/88 = 90.9% | 88/88 = 100.0% | 56/88 = 63.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state25__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state27__Sunshade__state22__state25`
- `TDE__state18__Sunshade__state22__state27`
- `TDE__state18__Sunshade__state22__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 79

**Selected features:** selected = {AD, AWL, BWI, C, Ca, FSD, LRAWD, M3, MC, NW, RR, S, S5, SG, T, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 31/39 = 79.5% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 51/73 = 69.9% | 73/73 = 100.0% | 73/73 = 100.0% | 54/73 = 74.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 35/62 = 56.5% | 59/62 = 95.2% | 62/62 = 100.0% | 40/62 = 64.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state26__Roof 
Rack__state23__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state18__Roof 
Rack__state23__state24`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 80

**Selected features:** selected = {AD, AWD, AWL, BWI, C, CC, Ca, HC, MX, S, S6, SB, T, TP, TuW, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 54/55 = 98.2% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 47/50 = 94.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`

### Product 81

**Selected features:** selected = {ABI, AD, Ca, F, HC, MC, MX, PD, S, S6, T, TP, TuW, UR, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state27`

### Product 82

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, CW, Ca, DAP, F, MC, MX, QS, S, S7, T, TP, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 36/41 = 87.8% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 67/77 = 87.0% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 63/66 = 95.5% | 66/66 = 100.0% | 52/66 = 78.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 83

**Selected features:** selected = {5Y, ACT, AD, BWI, C, CC, CW, Ca, FSD, HC, MX, PD, PWh, S, S6, T, TP, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 59/81 = 72.8% | 81/81 = 100.0% | 81/81 = 100.0% | 73/81 = 90.1% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 44/70 = 62.9% | 67/70 = 95.7% | 70/70 = 100.0% | 59/70 = 84.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state18`

### Product 84

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, CW, Ca, F, FSD, MC, MX, S, S5, T, TP, UR, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 35/41 = 85.4% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 64/77 = 83.1% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 52/66 = 78.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 85

**Selected features:** selected = {5Y, ABI, AD, AWD, AWL, C, CC, Ca, HC, MX, QS, S, S5, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 37/38 = 97.4% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 47/59 = 79.7% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 38/54 = 70.4% | 53/54 = 98.1% | 54/54 = 100.0% | 51/54 = 94.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`

### Product 86

**Selected features:** selected = {AD, AWD, AWL, CI, CW, Ca, F, FSD, HC, MC, MX, Mo, S, S7, SB, T, TP, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 58/81 = 71.6% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 56/70 = 80.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 87

**Selected features:** selected = {5Y, AD, AW, AWL, BWI, C, Ca, MS, PD, QS, S, S5, T, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 26/29 = 89.7% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 30/33 = 90.9% | 33/33 = 100.0% | 33/33 = 100.0% | 33/33 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 27/32 = 84.4% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 88

**Selected features:** selected = {AD, AWL, BWI, Ca, FSD, IW, L, LRAWD, MY, S, S5, SB, SS, T, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 42/51 = 82.4% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 31/46 = 67.4% | 45/46 = 97.8% | 46/46 = 100.0% | 38/46 = 82.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Sunshade__state22__state18`

### Product 89

**Selected features:** selected = {5Y, AD, AWD, BWI, C, CC, CW, Ca, FSD, HC, MX, S, S7, SB, T, TP, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 33/38 = 86.8% | 38/38 = 100.0% | 38/38 = 100.0% | 36/38 = 94.7% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 50/59 = 84.7% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 49/54 = 90.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state18`

### Product 90

**Selected features:** selected = {ABI, AD, CCT, Ca, L, MC, MY, PAWD, QS, S, S5, SS, T, UW, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 30/34 = 88.2% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 44/51 = 86.3% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 35/46 = 76.1% | 45/46 = 97.8% | 46/46 = 100.0% | 40/46 = 87.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state26__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state26`

### Product 91

**Selected features:** selected = {ACT, AD, AWL, BWI, CC, CW, Ca, DBM, F, FSD, MX, PD, S, S6, T, TP, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 52/77 = 67.5% | 77/77 = 100.0% | 77/77 = 100.0% | 70/77 = 90.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 53/66 = 80.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 92

**Selected features:** selected = {AD, AW, AWL, C, CI, Ca, DAP, FSD, MS, PD, PWh, RR, S, S5, SS, T, YS}

**Repaired FTS:** 30 states, 46 transitions (46 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 46 | 0/46 = 0.0% | 0/46 = 0.0% | 31/46 = 67.4% | 46/46 = 100.0% | 46/46 = 100.0% | 43/46 = 93.5% |
| ActionExchange | 111 | 0/111 = 0.0% | 0/111 = 0.0% | 64/111 = 57.7% | 111/111 = 100.0% | 111/111 = 100.0% | 104/111 = 93.7% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 111 | 14/111 = 12.6% | 0/97 = 0.0% | 40/97 = 41.2% | 81/97 = 83.5% | 97/97 = 100.0% | 79/97 = 81.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (14):

- `TDE__state17__Roof 
Rack__state23__state18`
- `TDE__state17__Roof 
Rack__state23__state28`
- `TDE__state18__Sunshade__state22__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__Sunshade__state22__state18`
- `TDE__state17__Sunshade__state22__state28`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state18__Roof 
Rack__state23__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 93

**Selected features:** selected = {ACT, AD, CI, Ca, F, FSD, MX, PD, PWh, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 94

**Selected features:** selected = {ABI, AD, AWL, CCT, Ca, HC, L, LRRWD, M3, Mo, PW, QS, S, S5, T, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 45/50 = 90.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`

### Product 95

**Selected features:** selected = {ACT, AD, AWD, AWL, CI, CW, Ca, DAP, FSD, L, MX, Mo, S, S7, T, TP, UR, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 56/81 = 69.1% | 81/81 = 100.0% | 81/81 = 100.0% | 75/81 = 92.6% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 55/70 = 78.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 96

**Selected features:** selected = {AD, BWI, Ca, DBM, HC, IW, L, LRAWD, MY, Mo, S, S7, SS, T, TP, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 30/34 = 88.2% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 40/45 = 88.9% | 45/45 = 100.0% | 45/45 = 100.0% | 43/45 = 95.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state27`

### Product 97

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, C, CW, Ca, DAP, FSD, MX, PWh, S, S6, T, TP, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 38/41 = 92.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 54/77 = 70.1% | 77/77 = 100.0% | 77/77 = 100.0% | 70/77 = 90.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 62/66 = 93.9% | 66/66 = 100.0% | 59/66 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 98

**Selected features:** selected = {ACT, AD, CC, CI, Ca, DBM, FSD, HC, L, MC, MX, PD, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 38/47 = 80.9% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 68/109 = 62.4% | 109/109 = 100.0% | 109/109 = 100.0% | 83/109 = 76.1% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 44/88 = 50.0% | 81/88 = 92.0% | 88/88 = 100.0% | 56/88 = 63.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state27__Car Cover__state21__state26`

### Product 99

**Selected features:** selected = {ABI, AD, AWD, AWL, Ca, DBM, F, MS, Mo, S, S5, SS, T, TeW, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 100

**Selected features:** selected = {ABI, AD, Ca, HC, L, LRRWD, M3, Mo, NW, QS, RR, S, S5, T, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 38/39 = 97.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state27`

### Product 101

**Selected features:** selected = {5Y, AD, AWD, C, CI, Ca, DAP, MX, QS, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 28 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 30/31 = 96.8% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 36/37 = 97.3% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 37 | 1/37 = 2.7% | 0/36 = 0.0% | 34/36 = 94.4% | 36/36 = 100.0% | 36/36 = 100.0% | 36/36 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 102

**Selected features:** selected = {ABI, ACT, AD, AWD, Ca, DBM, FSD, L, MC, MX, Mo, S, S5, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 33/38 = 86.8% | 38/38 = 100.0% | 38/38 = 100.0% | 36/38 = 94.7% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 52/59 = 88.1% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 40/54 = 74.1% | 53/54 = 98.1% | 54/54 = 100.0% | 49/54 = 90.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Mobile 
Charger__state26__state18`

### Product 103

**Selected features:** selected = {ABI, AD, AWD, CW, Ca, DAP, F, FSD, MX, QS, S, S7, T, TP, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 34/39 = 87.2% | 39/39 = 100.0% | 39/39 = 100.0% | 39/39 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Drive Anywhere
Package__state28__state18`

### Product 104

**Selected features:** selected = {ABI, AD, AWD, Ca, DBM, F, FSD, MC, MS, S, S5, SS, T, TeW, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 30/34 = 88.2% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 44/51 = 86.3% | 51/51 = 100.0% | 51/51 = 100.0% | 49/51 = 96.1% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 35/46 = 76.1% | 45/46 = 97.8% | 46/46 = 100.0% | 41/46 = 89.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state18__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 105

**Selected features:** selected = {5Y, AD, AW, AWL, BWI, C, Ca, DAP, HC, MS, PD, S, S5, T, UR, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 46/55 = 83.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 40/50 = 80.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__Home 
Charger__state27__state28`

### Product 106

**Selected features:** selected = {ABI, AD, AWD, AWL, CW, Ca, DAP, FSD, HC, L, MX, S, S6, SB, T, TP, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 51/77 = 66.2% | 77/77 = 100.0% | 77/77 = 100.0% | 68/77 = 88.3% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 56/66 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 107

**Selected features:** selected = {ABI, AD, AWD, C, Ca, DAP, FSD, MX, QS, S, S7, T, TP, TuW, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 34/39 = 87.2% | 39/39 = 100.0% | 39/39 = 100.0% | 39/39 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Drive Anywhere
Package__state28__state18`

### Product 108

**Selected features:** selected = {ACT, AD, AWD, BWI, CC, Ca, DAP, F, MX, QS, S, S5, T, TP, TuW, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 29/36 = 80.6% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 50/55 = 90.9% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 42/50 = 84.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Car Cover__state21__state28`

### Product 109

**Selected features:** selected = {ABI, AD, AWD, AWL, C, CC, Ca, DAP, MX, S, S5, SG, T, TP, TuW, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 29/36 = 80.6% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 50/55 = 90.9% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 42/50 = 84.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 110

**Selected features:** selected = {AD, AWD, CC, CI, CW, Ca, F, MX, PWh, S, S5, T, TP, YS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 30/32 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 111

**Selected features:** selected = {ABI, AD, AWL, Ca, HC, IW, L, LRAWD, MC, MY, QS, S, S5, T, TP, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 46/50 = 92.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 112

**Selected features:** selected = {AD, BWI, Ca, FSD, L, MC, MY, PAWD, PWh, S, S5, T, TP, UW, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 34/39 = 87.2% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 113

**Selected features:** selected = {5Y, ABI, ACT, AD, AWD, AWL, C, CW, Ca, MC, MX, S, S5, T, TP, UR, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 35/38 = 92.1% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 49/59 = 83.1% | 59/59 = 100.0% | 59/59 = 100.0% | 55/59 = 93.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 47/54 = 87.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 114

**Selected features:** selected = {AD, AWL, BWI, C, CCT, Ca, FSD, HC, LRRWD, M3, PW, S, S5, SG, T, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 33/39 = 84.6% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 50/73 = 68.5% | 73/73 = 100.0% | 73/73 = 100.0% | 63/73 = 86.3% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 35/62 = 56.5% | 59/62 = 95.2% | 62/62 = 100.0% | 46/62 = 74.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 115

**Selected features:** selected = {5Y, AD, AWD, C, CI, Ca, MC, MS, QS, S, S5, T, TeW, YS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 33/33 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 116

**Selected features:** selected = {AD, AWL, BWI, CCT, Ca, HC, IW, L, LRAWD, MY, Mo, S, S7, SS, T, UR, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 68/77 = 88.3% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 52/66 = 78.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state25__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state27__Sunshade__state22__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`

### Product 117

**Selected features:** selected = {AD, AWL, BWI, Ca, F, FSD, IW, LRAWD, MC, MY, QS, S, S7, T, TP, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 118

**Selected features:** selected = {AD, BWI, Ca, L, M3, MC, Mo, PAWD, RR, S, S5, SG, T, WS, WW}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 39/39 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state26`

### Product 119

**Selected features:** selected = {AD, AWL, C, CI, Ca, FSD, MS, PD, RR, S, S5, T, TeW, UR, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 28/34 = 82.4% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 38/51 = 74.5% | 51/51 = 100.0% | 51/51 = 100.0% | 48/51 = 94.1% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 30/46 = 65.2% | 45/46 = 97.8% | 46/46 = 100.0% | 39/46 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state18__Roof 
Rack__state23__state24`

### Product 120

**Selected features:** selected = {5Y, ABI, AD, AWD, C, CC, Ca, DAP, DBM, HC, MC, MX, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 61/81 = 75.3% | 81/81 = 100.0% | 81/81 = 100.0% | 74/81 = 91.4% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 56/70 = 80.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 121

**Selected features:** selected = {AD, AWD, AWL, BWI, Ca, DAP, HC, L, MS, Mo, PWh, RR, S, S5, T, TeW, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 50/77 = 64.9% | 77/77 = 100.0% | 77/77 = 100.0% | 68/77 = 88.3% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 63/66 = 95.5% | 66/66 = 100.0% | 54/66 = 81.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state17__Roof 
Rack__state23__state27`
- `TDE__state17__Roof 
Rack__state23__state24`
- `TDE__state17__Roof 
Rack__state23__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__Roof 
Rack__state23__state27`
- `TDE__state28__Roof 
Rack__state23__state24`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state27__Roof 
Rack__state23__state24`

### Product 122

**Selected features:** selected = {ACT, AD, AWL, CC, CI, Ca, DAP, FSD, HC, L, MC, MX, PD, S, S6, T, TP, TuW, UR, WS}

**Repaired FTS:** 33 states, 62 transitions (62 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 21 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 62 | 0/62 = 0.0% | 0/62 = 0.0% | 36/62 = 58.1% | 62/62 = 100.0% | 62/62 = 100.0% | 45/62 = 72.6% |
| ActionExchange | 211 | 0/211 = 0.0% | 0/211 = 0.0% | 96/211 = 45.5% | 211/211 = 100.0% | 211/211 = 100.0% | 137/211 = 64.9% |
| StateMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| TransitionDestinationExchange | 211 | 57/211 = 27.0% | 0/154 = 0.0% | 54/154 = 35.1% | 127/154 = 82.5% | 154/154 = 100.0% | 89/154 = 57.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (57):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 123

**Selected features:** selected = {AD, AW, BWI, Ca, F, HC, MC, MS, PD, S, S5, SG, T, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 34/37 = 91.9% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 28/35 = 80.0% | 35/35 = 100.0% | 35/35 = 100.0% | 33/35 = 94.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 124

**Selected features:** selected = {ABI, AD, AWD, AWL, Ca, FSD, HC, L, MC, MX, PWh, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 36/41 = 87.8% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 52/77 = 67.5% | 77/77 = 100.0% | 77/77 = 100.0% | 70/77 = 90.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 63/66 = 95.5% | 66/66 = 100.0% | 53/66 = 80.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 125

**Selected features:** selected = {AD, AWL, BWI, CCT, Ca, FSD, L, M3, MC, Mo, NW, PWh, RWD, S, S5, T, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 33/41 = 80.5% | 41/41 = 100.0% | 41/41 = 100.0% | 38/41 = 92.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 60/77 = 77.9% | 77/77 = 100.0% | 77/77 = 100.0% | 74/77 = 96.1% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 59/66 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Center 
Console 
Trays__state25__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 126

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, CW, Ca, DAP, F, HC, MC, MX, S, S5, T, TP, UR, WS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 42/47 = 89.4% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 64/109 = 58.7% | 109/109 = 100.0% | 109/109 = 100.0% | 98/109 = 89.9% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 43/88 = 48.9% | 81/88 = 92.0% | 88/88 = 100.0% | 70/88 = 79.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 127

**Selected features:** selected = {5Y, ACT, AD, BWI, C, CC, CW, Ca, DAP, FSD, MC, MX, PD, S, S6, SB, T, TP, WS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 36/49 = 73.5% | 49/49 = 100.0% | 49/49 = 100.0% | 42/49 = 85.7% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 75/113 = 66.4% | 113/113 = 100.0% | 113/113 = 100.0% | 96/113 = 85.0% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 53/92 = 57.6% | 83/92 = 90.2% | 92/92 = 100.0% | 74/92 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 128

**Selected features:** selected = {5Y, AD, AW, AWD, AWL, C, CI, Ca, DAP, HC, MS, S, S5, SS, T, UR, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 34/41 = 82.9% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 54/77 = 70.1% | 77/77 = 100.0% | 77/77 = 100.0% | 57/77 = 74.0% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 46/66 = 69.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state28__Sunshade__state22__state27`

### Product 129

**Selected features:** selected = {AD, AWD, AWL, CC, CI, CW, Ca, DAP, DBM, HC, L, MX, S, S5, T, TP, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 52/77 = 67.5% | 77/77 = 100.0% | 77/77 = 100.0% | 71/77 = 92.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 63/66 = 95.5% | 66/66 = 100.0% | 56/66 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 130

**Selected features:** selected = {5Y, ACT, AD, AWD, C, CC, CI, CW, Ca, FSD, MC, MX, PWh, S, S6, T, TP, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 35/43 = 81.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 55/81 = 67.9% | 81/81 = 100.0% | 81/81 = 100.0% | 65/81 = 80.2% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 67/70 = 95.7% | 70/70 = 100.0% | 48/70 = 68.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Car Cover__state21__state26`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`

### Product 131

**Selected features:** selected = {AD, AWD, AWL, BWI, CC, Ca, DAP, FSD, HC, L, MC, MX, S, S6, T, TP, TuW, UR, WS}

**Repaired FTS:** 32 states, 54 transitions (54 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 54 | 0/54 = 0.0% | 0/54 = 0.0% | 35/54 = 64.8% | 54/54 = 100.0% | 54/54 = 100.0% | 47/54 = 87.0% |
| ActionExchange | 153 | 0/153 = 0.0% | 0/153 = 0.0% | 85/153 = 55.6% | 153/153 = 100.0% | 153/153 = 100.0% | 132/153 = 86.3% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 153 | 36/153 = 23.5% | 0/117 = 0.0% | 51/117 = 43.6% | 101/117 = 86.3% | 117/117 = 100.0% | 85/117 = 72.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (36):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 132

**Selected features:** selected = {AD, AWL, BWI, CCT, Ca, GW, L, LRRWD, MC, MY, Mo, QS, S, S5, SS, T, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 33/41 = 80.5% | 41/41 = 100.0% | 41/41 = 100.0% | 34/41 = 82.9% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 60/77 = 77.9% | 77/77 = 100.0% | 77/77 = 100.0% | 60/77 = 77.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 46/66 = 69.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state25__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state26__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state25`

### Product 133

**Selected features:** selected = {ABI, AD, Ca, F, FSD, IW, LRAWD, MC, MY, S, S7, T, TP, UR, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 34/39 = 87.2% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 134

**Selected features:** selected = {5Y, AD, AWL, BWI, C, CCT, Ca, DBM, FSD, GW, LRRWD, MY, S, S5, T, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 135

**Selected features:** selected = {ACT, AD, AWL, CC, CI, CW, Ca, F, MX, PD, PWh, S, S6, T, TP, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 45/55 = 81.8% | 55/55 = 100.0% | 55/55 = 100.0% | 46/55 = 83.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 38/50 = 76.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 136

**Selected features:** selected = {ABI, AD, AWL, CCT, Ca, FSD, L, LRAWD, M3, MC, Mo, NW, RR, S, S5, T, UR, WS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 34/47 = 72.3% | 47/47 = 100.0% | 47/47 = 100.0% | 43/47 = 91.5% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 72/109 = 66.1% | 109/109 = 100.0% | 109/109 = 100.0% | 102/109 = 93.6% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 45/88 = 51.1% | 81/88 = 92.0% | 88/88 = 100.0% | 75/88 = 85.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state26__Roof 
Rack__state23__state25`
- `TDE__state26__Roof 
Rack__state23__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state25`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state25__Roof 
Rack__state23__state24`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Center 
Console 
Trays__state25__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state18__Roof 
Rack__state23__state25`
- `TDE__state18__Roof 
Rack__state23__state24`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 137

**Selected features:** selected = {AD, AWL, BWI, Ca, FSD, L, MC, MS, PD, S, S5, SG, T, TeW, YS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 28/34 = 82.4% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 39/51 = 76.5% | 51/51 = 100.0% | 51/51 = 100.0% | 48/51 = 94.1% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 30/46 = 65.2% | 45/46 = 97.8% | 46/46 = 100.0% | 41/46 = 89.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 138

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, CW, Ca, FSD, L, MX, Mo, S, S5, SB, T, TP, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 33/38 = 86.8% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 49/59 = 83.1% | 59/59 = 100.0% | 59/59 = 100.0% | 50/59 = 84.7% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 42/54 = 77.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`

### Product 139

**Selected features:** selected = {ABI, AD, AWD, AWL, Ca, DAP, FSD, HC, L, MS, Mo, QS, S, S5, SS, T, TeW, WS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 34/47 = 72.3% | 47/47 = 100.0% | 47/47 = 100.0% | 38/47 = 80.9% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 71/109 = 65.1% | 109/109 = 100.0% | 109/109 = 100.0% | 90/109 = 82.6% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 45/88 = 51.1% | 81/88 = 92.0% | 88/88 = 100.0% | 60/88 = 68.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state18__Sunshade__state22__state27`
- `TDE__state18__Sunshade__state22__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state28__Sunshade__state22__state27`

### Product 140

**Selected features:** selected = {AD, AWD, AWL, C, CI, Ca, DBM, HC, MC, MS, RR, S, S5, SS, T, TeW, YS}

**Repaired FTS:** 30 states, 46 transitions (46 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 46 | 0/46 = 0.0% | 0/46 = 0.0% | 31/46 = 67.4% | 46/46 = 100.0% | 46/46 = 100.0% | 41/46 = 89.1% |
| ActionExchange | 111 | 0/111 = 0.0% | 0/111 = 0.0% | 63/111 = 56.8% | 111/111 = 100.0% | 111/111 = 100.0% | 103/111 = 92.8% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 111 | 14/111 = 12.6% | 0/97 = 0.0% | 39/97 = 40.2% | 80/97 = 82.5% | 97/97 = 100.0% | 75/97 = 77.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (14):

- `TDE__state17__Roof 
Rack__state23__state27`
- `TDE__state17__Roof 
Rack__state23__state26`
- `TDE__state27__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Sunshade__state22__state27`
- `TDE__state17__Sunshade__state22__state26`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__Roof 
Rack__state23__state26`

### Product 141

**Selected features:** selected = {AD, AWD, AWL, BWI, CC, CW, Ca, L, MX, S, S7, SG, T, TP, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`

### Product 142

**Selected features:** selected = {AD, AWL, BWI, CCT, Ca, F, FSD, HC, IW, LRAWD, MY, Mo, S, S5, SS, T, UR, WS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 34/47 = 72.3% | 47/47 = 100.0% | 47/47 = 100.0% | 39/47 = 83.0% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 70/109 = 64.2% | 109/109 = 100.0% | 109/109 = 100.0% | 92/109 = 84.4% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 44/88 = 50.0% | 80/88 = 90.9% | 88/88 = 100.0% | 59/88 = 67.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state25__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state27__Sunshade__state22__state25`
- `TDE__state18__Sunshade__state22__state27`
- `TDE__state18__Sunshade__state22__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 143

**Selected features:** selected = {ACT, AD, AWD, C, CI, CW, Ca, HC, MC, MX, S, S5, T, TP, UR, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 50/55 = 90.9% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 36/50 = 72.0% | 49/50 = 98.0% | 50/50 = 100.0% | 42/50 = 84.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`

### Product 144

**Selected features:** selected = {AD, AW, AWD, AWL, BWI, Ca, DAP, HC, L, MS, RR, S, S5, SB, T, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 35/39 = 89.7% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 51/73 = 69.9% | 73/73 = 100.0% | 73/73 = 100.0% | 64/73 = 87.7% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 35/62 = 56.5% | 59/62 = 95.2% | 62/62 = 100.0% | 50/62 = 80.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state16__Roof 
Rack__state23__state28`
- `TDE__state28__Roof 
Rack__state23__state27`
- `TDE__state28__Roof 
Rack__state23__state24`
- `TDE__state27__Roof 
Rack__state23__state24`

### Product 145

**Selected features:** selected = {AD, AWL, BWI, Ca, F, HC, IW, LRAWD, MC, MY, Mo, S, S7, SS, T, UR, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 68/77 = 88.3% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 62/66 = 93.9% | 66/66 = 100.0% | 53/66 = 80.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 146

**Selected features:** selected = {ABI, AD, AWL, Ca, F, GW, LRRWD, MY, Mo, QS, S, S5, SS, T, TP, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 40/45 = 88.9% | 45/45 = 100.0% | 45/45 = 100.0% | 45/45 = 100.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 41/43 = 95.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 147

**Selected features:** selected = {ABI, AD, AWD, AWL, C, Ca, DAP, MC, MS, S, S5, T, TeW, UR, YS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 27/34 = 79.4% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 39/51 = 76.5% | 51/51 = 100.0% | 51/51 = 100.0% | 49/51 = 96.1% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 32/46 = 69.6% | 45/46 = 97.8% | 46/46 = 100.0% | 39/46 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 148

**Selected features:** selected = {AD, AWL, BWI, CC, CW, Ca, F, MC, MX, Mo, PD, S, S6, SB, T, TP, YS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 33/38 = 86.8% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 47/59 = 79.7% | 59/59 = 100.0% | 59/59 = 100.0% | 49/59 = 83.1% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 42/54 = 77.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`

### Product 149

**Selected features:** selected = {ACT, AD, AWD, CI, Ca, F, HC, MX, QS, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`

### Product 150

**Selected features:** selected = {AD, AWD, CI, CW, Ca, DAP, FSD, HC, L, MC, MX, S, S5, SG, T, TP, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 39/41 = 95.1% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 54/77 = 70.1% | 77/77 = 100.0% | 77/77 = 100.0% | 73/77 = 94.8% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 58/66 = 87.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state18__Home 
Charger__state27__state28`

### Product 151

**Selected features:** selected = {ABI, ACT, AD, AWD, AWL, CC, Ca, FSD, HC, L, MX, Mo, S, S5, T, TP, TuW, UR, YS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 34/49 = 69.4% | 49/49 = 100.0% | 49/49 = 100.0% | 41/49 = 83.7% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 69/113 = 61.1% | 113/113 = 100.0% | 113/113 = 100.0% | 92/113 = 81.4% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 48/92 = 52.2% | 85/92 = 92.4% | 92/92 = 100.0% | 68/92 = 73.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state18`

### Product 152

**Selected features:** selected = {5Y, ABI, ACT, AD, AWD, C, CC, Ca, DAP, FSD, MC, MX, S, S6, SG, T, TP, TuW, WS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 36/49 = 73.5% | 49/49 = 100.0% | 49/49 = 100.0% | 41/49 = 83.7% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 75/113 = 66.4% | 113/113 = 100.0% | 113/113 = 100.0% | 94/113 = 83.2% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 53/92 = 57.6% | 83/92 = 90.2% | 92/92 = 100.0% | 58/92 = 63.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 153

**Selected features:** selected = {ABI, AD, AWD, CW, Ca, HC, L, MC, MX, Mo, S, S5, SG, T, TP, YS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 42/45 = 93.3% | 45/45 = 100.0% | 45/45 = 100.0% | 45/45 = 100.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 36/43 = 83.7% | 43/43 = 100.0% | 43/43 = 100.0% | 41/43 = 95.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state27`

### Product 154

**Selected features:** selected = {ACT, AD, AWD, CC, CI, CW, Ca, DAP, F, FSD, MX, Mo, S, S6, T, TP, UR, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 55/81 = 67.9% | 81/81 = 100.0% | 81/81 = 100.0% | 69/81 = 85.2% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 67/70 = 95.7% | 70/70 = 100.0% | 55/70 = 78.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Car Cover__state21__state28`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`

### Product 155

**Selected features:** selected = {AD, BWI, C, Ca, DBM, MC, MY, PAWD, S, S5, SS, T, UW, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 34/37 = 91.9% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 35/35 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state26`

### Product 156

**Selected features:** selected = {AD, AWD, AWL, BWI, Ca, FSD, L, MC, MX, QS, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 54/55 = 98.2% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 47/50 = 94.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 157

**Selected features:** selected = {ABI, AD, AW, Ca, DAP, DBM, HC, L, MC, MS, PD, RR, S, S5, T, YS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 35/39 = 89.7% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 53/73 = 72.6% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 36/62 = 58.1% | 59/62 = 95.2% | 62/62 = 100.0% | 51/62 = 82.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state17__Roof 
Rack__state23__state27`
- `TDE__state17__Roof 
Rack__state23__state26`
- `TDE__state17__Roof 
Rack__state23__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state28__Roof 
Rack__state23__state27`
- `TDE__state28__Roof 
Rack__state23__state26`
- `TDE__state27__Roof 
Rack__state23__state26`

### Product 158

**Selected features:** selected = {AD, BWI, Ca, F, FSD, IW, LRAWD, MY, S, S5, SB, T, WS}

**Repaired FTS:** 26 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 26/27 = 96.3% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| ActionExchange | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| StateMissing | 25 | 0/25 = 0.0% | 0/25 = 0.0% | 25/25 = 100.0% | 25/25 = 100.0% | 25/25 = 100.0% | 25/25 = 100.0% |
| TransitionDestinationExchange | 29 | 1/29 = 3.4% | 0/28 = 0.0% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 159

**Selected features:** selected = {AD, AWD, AWL, CI, CW, Ca, DAP, F, HC, MX, S, S7, T, TP, UR, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 49/55 = 89.1% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 38/50 = 76.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__Home 
Charger__state27__state28`

### Product 160

**Selected features:** selected = {ABI, AD, AWD, Ca, DAP, F, MS, PWh, RR, S, S5, T, TeW, YS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 27/30 = 90.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 33/37 = 89.2% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 27/35 = 77.1% | 35/35 = 100.0% | 35/35 = 100.0% | 29/35 = 82.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state17__Roof 
Rack__state23__state28`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 161

**Selected features:** selected = {AD, AWD, AWL, BWI, Ca, F, FSD, MX, QS, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 162

**Selected features:** selected = {AD, AW, AWD, AWL, BWI, C, Ca, MC, MS, RR, S, S5, SG, T, YS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 42/51 = 82.4% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 31/46 = 67.4% | 45/46 = 97.8% | 46/46 = 100.0% | 41/46 = 89.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Roof 
Rack__state23__state26`
- `TDE__state17__Roof 
Rack__state23__state24`
- `TDE__state26__Roof 
Rack__state23__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`

### Product 163

**Selected features:** selected = {ABI, AD, CCT, Ca, DBM, F, LRAWD, M3, PW, S, S5, T, WS}

**Repaired FTS:** 26 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 26/27 = 96.3% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| ActionExchange | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| StateMissing | 25 | 0/25 = 0.0% | 0/25 = 0.0% | 25/25 = 100.0% | 25/25 = 100.0% | 25/25 = 100.0% | 25/25 = 100.0% |
| TransitionDestinationExchange | 29 | 1/29 = 3.4% | 0/28 = 0.0% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 164

**Selected features:** selected = {AD, AW, CI, Ca, DAP, F, FSD, HC, MS, PD, RR, S, S5, T, UR, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 33/39 = 84.6% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 56/73 = 76.7% | 73/73 = 100.0% | 73/73 = 100.0% | 60/73 = 82.2% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 36/62 = 58.1% | 58/62 = 93.5% | 62/62 = 100.0% | 47/62 = 75.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state16__Roof 
Rack__state23__state28`
- `TDE__state28__Roof 
Rack__state23__state27`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state18__Roof 
Rack__state23__state27`
- `TDE__state18__Roof 
Rack__state23__state28`

### Product 165

**Selected features:** selected = {ABI, AD, AWD, CW, Ca, HC, L, MX, QS, S, S7, T, TP, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 26/29 = 89.7% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 30/33 = 90.9% | 33/33 = 100.0% | 33/33 = 100.0% | 33/33 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 27/32 = 84.4% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 166

**Selected features:** selected = {ABI, AD, AWL, C, CCT, Ca, GW, HC, LRAWD, MC, MY, S, S7, SG, T, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 33/39 = 84.6% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 51/73 = 69.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 35/62 = 56.5% | 59/62 = 95.2% | 62/62 = 100.0% | 46/62 = 74.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Center 
Console 
Trays__state25__state26`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 167

**Selected features:** selected = {5Y, AD, AWL, BWI, C, Ca, DAP, MS, PD, RR, S, S5, T, TeW, UR, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 42/55 = 76.4% | 55/55 = 100.0% | 55/55 = 100.0% | 45/55 = 81.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 38/50 = 76.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state16__Roof 
Rack__state23__state28`
- `TDE__state28__Roof 
Rack__state23__state24`

### Product 168

**Selected features:** selected = {AD, AWL, BWI, Ca, DAP, L, MS, Mo, PD, S, S5, SB, SS, T, TeW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 53/55 = 96.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Sunshade__state22__state28`

### Product 169

**Selected features:** selected = {ABI, AD, AWL, Ca, DBM, HC, IW, L, LRAWD, MC, MY, S, S5, SS, T, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 32/39 = 82.1% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 51/73 = 69.9% | 73/73 = 100.0% | 73/73 = 100.0% | 54/73 = 74.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 35/62 = 56.5% | 58/62 = 93.5% | 62/62 = 100.0% | 42/62 = 67.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 170

**Selected features:** selected = {5Y, AD, AWL, C, CI, Ca, FSD, MS, PD, QS, S, S5, T, TeW, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 39/39 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 171

**Selected features:** selected = {ABI, AD, AWL, CCT, Ca, FSD, HC, L, MY, Mo, PAWD, S, S5, SG, SS, T, UW, WS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 34/47 = 72.3% | 47/47 = 100.0% | 47/47 = 100.0% | 39/47 = 83.0% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 70/109 = 64.2% | 109/109 = 100.0% | 109/109 = 100.0% | 92/109 = 84.4% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 44/88 = 50.0% | 80/88 = 90.9% | 88/88 = 100.0% | 59/88 = 67.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state25__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state27__Sunshade__state22__state25`
- `TDE__state18__Sunshade__state22__state27`
- `TDE__state18__Sunshade__state22__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 172

**Selected features:** selected = {ABI, AD, AWD, CC, Ca, DAP, L, MX, S, S6, SB, T, TP, TuW, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Car Cover__state21__state28`

### Product 173

**Selected features:** selected = {AD, AWL, C, CC, CI, CW, Ca, MX, PD, QS, S, S6, T, TP, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`

### Product 174

**Selected features:** selected = {AD, BWI, CCT, Ca, FSD, L, LRRWD, M3, MC, Mo, NW, PWh, S, S5, T, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 32/36 = 88.9% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 48/55 = 87.3% | 55/55 = 100.0% | 55/55 = 100.0% | 46/55 = 83.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 39/50 = 78.0% | 49/50 = 98.0% | 50/50 = 100.0% | 40/50 = 80.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Center 
Console 
Trays__state25__state18`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 175

**Selected features:** selected = {ACT, AD, AWL, BWI, Ca, DBM, F, FSD, HC, MX, Mo, PD, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 58/81 = 71.6% | 81/81 = 100.0% | 81/81 = 100.0% | 68/81 = 84.0% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 54/70 = 77.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 176

**Selected features:** selected = {AD, BWI, Ca, DAP, F, HC, MX, Mo, PD, QS, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 40/45 = 88.9% | 45/45 = 100.0% | 45/45 = 100.0% | 43/45 = 95.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state28`

### Product 177

**Selected features:** selected = {AD, BWI, C, Ca, FSD, HC, MC, MS, PD, RR, S, S5, T, TeW, UR, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 34/39 = 87.2% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 56/73 = 76.7% | 73/73 = 100.0% | 73/73 = 100.0% | 64/73 = 87.7% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 36/62 = 58.1% | 59/62 = 95.2% | 62/62 = 100.0% | 50/62 = 80.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state18__Roof 
Rack__state23__state27`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state27__Roof 
Rack__state23__state26`

### Product 178

**Selected features:** selected = {ABI, AD, Ca, DAP, HC, L, MC, MS, Mo, PD, S, S5, SB, SS, T, TeW, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 36/41 = 87.8% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 52/77 = 67.5% | 77/77 = 100.0% | 77/77 = 100.0% | 69/77 = 89.6% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 63/66 = 95.5% | 66/66 = 100.0% | 54/66 = 81.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state27__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state28__Sunshade__state22__state27`
- `TDE__state28__Sunshade__state22__state26`

### Product 179

**Selected features:** selected = {ABI, AD, CCT, Ca, L, MC, MY, Mo, PAWD, S, S5, SB, T, UW, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 34/39 = 87.2% | 39/39 = 100.0% | 39/39 = 100.0% | 39/39 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Center 
Console 
Trays__state25__state26`

### Product 180

**Selected features:** selected = {5Y, AD, AWD, C, CI, Ca, DAP, DBM, FSD, MC, MS, S, S5, T, TeW, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 29/36 = 80.6% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 51/55 = 92.7% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 44/50 = 88.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`

### Product 181

**Selected features:** selected = {5Y, AD, AW, AWL, BWI, C, Ca, DAP, HC, MS, PD, RR, S, S5, SB, SS, T, WS}

**Repaired FTS:** 31 states, 48 transitions (48 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 48 | 0/48 = 0.0% | 0/48 = 0.0% | 33/48 = 68.8% | 48/48 = 100.0% | 48/48 = 100.0% | 42/48 = 87.5% |
| ActionExchange | 115 | 0/115 = 0.0% | 0/115 = 0.0% | 67/115 = 58.3% | 115/115 = 100.0% | 115/115 = 100.0% | 101/115 = 87.8% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 115 | 14/115 = 12.2% | 0/101 = 0.0% | 44/101 = 43.6% | 83/101 = 82.2% | 101/101 = 100.0% | 74/101 = 73.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (14):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state28`
- `TDE__state28__Roof 
Rack__state23__state27`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state28__Sunshade__state22__state27`

### Product 182

**Selected features:** selected = {AD, AWL, BWI, CCT, Ca, F, FSD, HC, IW, LRAWD, MY, S, S7, SB, T, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 33/39 = 84.6% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 50/73 = 68.5% | 73/73 = 100.0% | 73/73 = 100.0% | 63/73 = 86.3% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 35/62 = 56.5% | 59/62 = 95.2% | 62/62 = 100.0% | 46/62 = 74.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 183

**Selected features:** selected = {ABI, AD, AWL, Ca, DBM, FSD, L, LRRWD, M3, PW, RR, S, S5, T, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 28/34 = 82.4% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 38/51 = 74.5% | 51/51 = 100.0% | 51/51 = 100.0% | 48/51 = 94.1% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 30/46 = 65.2% | 45/46 = 97.8% | 46/46 = 100.0% | 39/46 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state18__Roof 
Rack__state23__state24`

### Product 184

**Selected features:** selected = {AD, AWD, BWI, Ca, DAP, F, MS, Mo, QS, S, S5, T, TeW, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 185

**Selected features:** selected = {5Y, ACT, AD, AWD, BWI, C, CC, Ca, DAP, HC, MC, MX, PWh, S, S5, T, TP, TuW, WS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 35/49 = 71.4% | 49/49 = 100.0% | 49/49 = 100.0% | 41/49 = 83.7% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 72/113 = 63.7% | 113/113 = 100.0% | 113/113 = 100.0% | 98/113 = 86.7% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 48/92 = 52.2% | 85/92 = 92.4% | 92/92 = 100.0% | 71/92 = 77.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 186

**Selected features:** selected = {ACT, AD, AWD, CC, CI, CW, Ca, F, FSD, HC, MC, MX, S, S5, T, TP, UR, YS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 38/47 = 80.9% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 68/109 = 62.4% | 109/109 = 100.0% | 109/109 = 100.0% | 83/109 = 76.1% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 44/88 = 50.0% | 81/88 = 92.0% | 88/88 = 100.0% | 56/88 = 63.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state27__Car Cover__state21__state26`

### Product 187

**Selected features:** selected = {AD, AWL, BWI, Ca, DBM, IW, L, LRAWD, MC, MY, S, S5, SS, T, TP, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 46/55 = 83.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 39/50 = 78.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`

### Product 188

**Selected features:** selected = {ACT, AD, AWD, BWI, CC, Ca, DAP, F, MX, Mo, S, S6, SB, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 31/38 = 81.6% | 38/38 = 100.0% | 38/38 = 100.0% | 35/38 = 92.1% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 47/59 = 79.7% | 59/59 = 100.0% | 59/59 = 100.0% | 55/59 = 93.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 48/54 = 88.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Car Cover__state21__state28`

### Product 189

**Selected features:** selected = {ABI, AD, AWL, Ca, F, IW, LRAWD, MY, S, S7, SB, T, TP, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 27/29 = 93.1% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 31/33 = 93.9% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 190

**Selected features:** selected = {5Y, ABI, ACT, AD, AWD, AWL, C, CC, Ca, DAP, MC, MX, S, S6, SB, T, TP, TuW, YS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 35/49 = 71.4% | 49/49 = 100.0% | 49/49 = 100.0% | 43/49 = 87.8% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 69/113 = 61.1% | 113/113 = 100.0% | 113/113 = 100.0% | 99/113 = 87.6% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 47/92 = 51.1% | 85/92 = 92.4% | 92/92 = 100.0% | 72/92 = 78.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`

### Product 191

**Selected features:** selected = {5Y, ABI, ACT, AD, AWD, C, CC, Ca, FSD, HC, MC, MX, S, S5, SG, T, TP, TuW, YS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 35/49 = 71.4% | 49/49 = 100.0% | 49/49 = 100.0% | 43/49 = 87.8% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 72/113 = 63.7% | 113/113 = 100.0% | 113/113 = 100.0% | 100/113 = 88.5% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 48/92 = 52.2% | 85/92 = 92.4% | 92/92 = 100.0% | 68/92 = 73.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state27__Car Cover__state21__state26`

### Product 192

**Selected features:** selected = {ACT, AD, AWD, BWI, Ca, DAP, F, HC, MC, MX, S, S5, SB, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 57/77 = 74.0% | 77/77 = 100.0% | 77/77 = 100.0% | 72/77 = 93.5% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 55/66 = 83.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`

### Product 193

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, C, CW, Ca, DAP, FSD, MC, MX, S, S6, SG, T, TP, YS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 37/47 = 78.7% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 66/109 = 60.6% | 109/109 = 100.0% | 109/109 = 100.0% | 77/109 = 70.6% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 44/88 = 50.0% | 81/88 = 92.0% | 88/88 = 100.0% | 54/88 = 61.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 194

**Selected features:** selected = {AD, AWD, BWI, CW, Ca, DBM, F, FSD, MX, S, S7, T, TP, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 30/32 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 195

**Selected features:** selected = {AD, AWD, AWL, CC, CI, Ca, DAP, L, MC, MX, S, S6, SB, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 38/41 = 92.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 51/77 = 66.2% | 77/77 = 100.0% | 77/77 = 100.0% | 72/77 = 93.5% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 62/66 = 93.9% | 66/66 = 100.0% | 57/66 = 86.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 196

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, CC, Ca, DAP, F, FSD, MX, Mo, S, S5, SB, T, TP, TuW, YS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 34/49 = 69.4% | 49/49 = 100.0% | 49/49 = 100.0% | 43/49 = 87.8% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 67/113 = 59.3% | 113/113 = 100.0% | 113/113 = 100.0% | 101/113 = 89.4% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 47/92 = 51.1% | 85/92 = 92.4% | 92/92 = 100.0% | 70/92 = 76.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`

### Product 197

**Selected features:** selected = {AD, BWI, Ca, FSD, HC, L, MS, PD, RR, S, S5, SS, T, TeW, UR, YS}

**Repaired FTS:** 29 states, 40 transitions (40 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 40 | 0/40 = 0.0% | 0/40 = 0.0% | 29/40 = 72.5% | 40/40 = 100.0% | 40/40 = 100.0% | 35/40 = 87.5% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 48/77 = 62.3% | 77/77 = 100.0% | 77/77 = 100.0% | 67/77 = 87.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 77 | 8/77 = 10.4% | 0/69 = 0.0% | 34/69 = 49.3% | 61/69 = 88.4% | 69/69 = 100.0% | 52/69 = 75.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Roof 
Rack__state23__state27`
- `TDE__state17__Roof 
Rack__state23__state18`
- `TDE__state18__Sunshade__state22__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Sunshade__state22__state27`
- `TDE__state17__Sunshade__state22__state18`
- `TDE__state18__Roof 
Rack__state23__state27`

### Product 198

**Selected features:** selected = {ABI, ACT, AD, AWD, AWL, CC, CW, Ca, HC, L, MX, S, S7, SB, T, TP, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 71/77 = 92.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 63/66 = 95.5% | 66/66 = 100.0% | 56/66 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 199

**Selected features:** selected = {AD, AW, AWD, CI, Ca, FSD, HC, L, MS, Mo, QS, RR, S, S5, T, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 46/55 = 83.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 38/50 = 76.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state18__Roof 
Rack__state23__state27`

### Product 200

**Selected features:** selected = {5Y, AD, AWL, C, CI, Ca, DAP, HC, MC, MS, PD, QS, S, S5, T, TeW, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 38/41 = 92.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 54/77 = 70.1% | 77/77 = 100.0% | 77/77 = 100.0% | 71/77 = 92.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 56/66 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 201

**Selected features:** selected = {ABI, ACT, AD, AWL, CC, Ca, DBM, F, FSD, MX, Mo, PD, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 58/81 = 71.6% | 81/81 = 100.0% | 81/81 = 100.0% | 73/81 = 90.1% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 66/70 = 94.3% | 70/70 = 100.0% | 59/70 = 84.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`

### Product 202

**Selected features:** selected = {5Y, AD, AWL, BWI, C, CCT, Ca, DBM, HC, IW, LRAWD, MC, MY, S, S7, T, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 34/41 = 82.9% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 58/77 = 75.3% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 46/66 = 69.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Center 
Console 
Trays__state25__state26`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 203

**Selected features:** selected = {ACT, AD, AWD, CC, CI, CW, Ca, DAP, L, MC, MX, S, S7, SG, T, TP, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 51/77 = 66.2% | 77/77 = 100.0% | 77/77 = 100.0% | 71/77 = 92.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 63/66 = 95.5% | 66/66 = 100.0% | 54/66 = 81.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`

### Product 204

**Selected features:** selected = {AD, BWI, Ca, F, FSD, MC, MS, PD, S, S5, SS, T, TeW, UR, YS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 28/34 = 82.4% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 39/51 = 76.5% | 51/51 = 100.0% | 51/51 = 100.0% | 48/51 = 94.1% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 30/46 = 65.2% | 45/46 = 97.8% | 46/46 = 100.0% | 41/46 = 89.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state18__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Sunshade__state22__state26`
- `TDE__state17__Sunshade__state22__state18`
- `TDE__state17__Mobile 
Charger__state26__state18`

### Product 205

**Selected features:** selected = {AD, AWD, CC, CI, CW, Ca, DAP, DBM, FSD, HC, L, MC, MX, S, S6, T, TP, YS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 37/47 = 78.7% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 68/109 = 62.4% | 109/109 = 100.0% | 109/109 = 100.0% | 82/109 = 75.2% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 43/88 = 48.9% | 81/88 = 92.0% | 88/88 = 100.0% | 58/88 = 65.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 206

**Selected features:** selected = {5Y, ABI, AD, AW, AWD, AWL, C, Ca, FSD, MC, MS, RR, S, S5, SB, T, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 35/41 = 85.4% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 63/77 = 81.8% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 51/66 = 77.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Roof 
Rack__state23__state26`
- `TDE__state17__Roof 
Rack__state23__state24`
- `TDE__state17__Roof 
Rack__state23__state18`
- `TDE__state26__Roof 
Rack__state23__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state18__Roof 
Rack__state23__state24`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 207

**Selected features:** selected = {ABI, AD, AW, Ca, DBM, F, FSD, HC, MS, PD, RR, S, S5, T, YS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 42/51 = 82.4% | 51/51 = 100.0% | 51/51 = 100.0% | 50/51 = 98.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 31/46 = 67.4% | 45/46 = 97.8% | 46/46 = 100.0% | 42/46 = 91.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Roof 
Rack__state23__state27`
- `TDE__state17__Roof 
Rack__state23__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Roof 
Rack__state23__state27`

### Product 208

**Selected features:** selected = {ABI, ACT, AD, AWD, AWL, C, CC, Ca, DAP, FSD, HC, MC, MX, S, S7, SG, T, TP, TuW, WS}

**Repaired FTS:** 33 states, 62 transitions (62 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 21 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 62 | 0/62 = 0.0% | 0/62 = 0.0% | 36/62 = 58.1% | 62/62 = 100.0% | 62/62 = 100.0% | 45/62 = 72.6% |
| ActionExchange | 211 | 0/211 = 0.0% | 0/211 = 0.0% | 96/211 = 45.5% | 211/211 = 100.0% | 211/211 = 100.0% | 137/211 = 64.9% |
| StateMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| TransitionDestinationExchange | 211 | 57/211 = 27.0% | 0/154 = 0.0% | 54/154 = 35.1% | 127/154 = 82.5% | 154/154 = 100.0% | 89/154 = 57.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (57):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 209

**Selected features:** selected = {ABI, AD, AWD, C, Ca, HC, MC, MS, RR, S, S5, SS, T, TeW, UR, WS}

**Repaired FTS:** 29 states, 40 transitions (40 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 40 | 0/40 = 0.0% | 0/40 = 0.0% | 29/40 = 72.5% | 40/40 = 100.0% | 40/40 = 100.0% | 34/40 = 85.0% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 49/77 = 63.6% | 77/77 = 100.0% | 77/77 = 100.0% | 63/77 = 81.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 77 | 8/77 = 10.4% | 0/69 = 0.0% | 34/69 = 49.3% | 61/69 = 88.4% | 69/69 = 100.0% | 49/69 = 71.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state27__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state27__Roof 
Rack__state23__state26`

### Product 210

**Selected features:** selected = {ABI, AD, AW, AWD, Ca, FSD, L, MS, Mo, PWh, S, S5, T, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 211

**Selected features:** selected = {AD, AWD, CI, Ca, DBM, FSD, HC, L, MX, Mo, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 40/45 = 88.9% | 45/45 = 100.0% | 45/45 = 100.0% | 41/45 = 91.1% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 37/43 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 212

**Selected features:** selected = {AD, AWD, BWI, Ca, DAP, DBM, FSD, L, MC, MX, S, S5, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 51/55 = 92.7% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 38/50 = 76.0% | 49/50 = 98.0% | 50/50 = 100.0% | 44/50 = 88.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 213

**Selected features:** selected = {AD, AW, AWD, AWL, BWI, Ca, F, FSD, MC, MS, PWh, S, S5, SS, T, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 35/39 = 89.7% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 56/73 = 76.7% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 36/62 = 58.1% | 59/62 = 95.2% | 62/62 = 100.0% | 51/62 = 82.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state18__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 214

**Selected features:** selected = {AD, AW, CI, Ca, DAP, F, FSD, HC, MS, PD, QS, S, S5, T, YS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 28/34 = 82.4% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 39/51 = 76.5% | 51/51 = 100.0% | 51/51 = 100.0% | 46/51 = 90.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 31/46 = 67.4% | 45/46 = 97.8% | 46/46 = 100.0% | 38/46 = 82.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`

### Product 215

**Selected features:** selected = {5Y, ABI, AD, AWD, AWL, C, Ca, DAP, DBM, FSD, MC, MX, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 32/43 = 74.4% | 43/43 = 100.0% | 43/43 = 100.0% | 37/43 = 86.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 56/81 = 69.1% | 81/81 = 100.0% | 81/81 = 100.0% | 68/81 = 84.0% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 44/70 = 62.9% | 67/70 = 95.7% | 70/70 = 100.0% | 54/70 = 77.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 216

**Selected features:** selected = {5Y, ABI, AD, AW, AWD, C, Ca, HC, MS, RR, S, S5, T, UR, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 26/27 = 96.3% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 33/39 = 84.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state27`

### Product 217

**Selected features:** selected = {ABI, AD, AWL, CCT, Ca, F, IW, LRAWD, MC, MY, Mo, S, S7, SG, SS, T, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 33/41 = 80.5% | 41/41 = 100.0% | 41/41 = 100.0% | 34/41 = 82.9% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 60/77 = 77.9% | 77/77 = 100.0% | 77/77 = 100.0% | 60/77 = 77.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 46/66 = 69.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state25__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state26__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state25`

### Product 218

**Selected features:** selected = {AD, BWI, Ca, FSD, IW, L, LRRWD, MY, QS, S, S5, SS, T, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 34/37 = 91.9% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 35/35 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state18`

### Product 219

**Selected features:** selected = {AD, AW, AWD, AWL, C, CI, Ca, DBM, MS, RR, S, S5, SS, T, WS}

**Repaired FTS:** 28 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 0/35 = 0.0% | 28/35 = 80.0% | 35/35 = 100.0% | 35/35 = 100.0% | 29/35 = 82.9% |
| ActionExchange | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 41/57 = 71.9% | 57/57 = 100.0% | 57/57 = 100.0% | 46/57 = 80.7% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 26/27 = 96.3% |
| TransitionDestinationExchange | 57 | 2/57 = 3.5% | 0/55 = 0.0% | 31/55 = 56.4% | 51/55 = 92.7% | 55/55 = 100.0% | 37/55 = 67.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 220

**Selected features:** selected = {ABI, ACT, AD, AWD, AWL, CC, Ca, DAP, FSD, HC, L, MC, MX, PWh, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 33 states, 62 transitions (62 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 21 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 62 | 0/62 = 0.0% | 0/62 = 0.0% | 36/62 = 58.1% | 62/62 = 100.0% | 62/62 = 100.0% | 45/62 = 72.6% |
| ActionExchange | 211 | 0/211 = 0.0% | 0/211 = 0.0% | 96/211 = 45.5% | 211/211 = 100.0% | 211/211 = 100.0% | 137/211 = 64.9% |
| StateMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| TransitionDestinationExchange | 211 | 57/211 = 27.0% | 0/154 = 0.0% | 54/154 = 35.1% | 127/154 = 82.5% | 154/154 = 100.0% | 89/154 = 57.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (57):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 221

**Selected features:** selected = {ABI, AD, AWL, C, CC, Ca, DAP, MX, PD, QS, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 53/55 = 96.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__Car Cover__state21__state28`

### Product 222

**Selected features:** selected = {ABI, AD, AWD, AWL, CC, Ca, F, FSD, HC, MC, MX, S, S5, SG, T, TP, TuW, YS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 39/47 = 83.0% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 66/109 = 60.6% | 109/109 = 100.0% | 109/109 = 100.0% | 87/109 = 79.8% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 42/88 = 47.7% | 81/88 = 92.0% | 88/88 = 100.0% | 61/88 = 69.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state27__Car Cover__state21__state26`

### Product 223

**Selected features:** selected = {ABI, ACT, AD, AWD, AWL, CC, CW, Ca, FSD, L, MC, MX, Mo, PWh, S, S6, T, TP, YS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 35/49 = 71.4% | 49/49 = 100.0% | 49/49 = 100.0% | 40/49 = 81.6% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 69/113 = 61.1% | 113/113 = 100.0% | 113/113 = 100.0% | 83/113 = 73.5% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 30/31 = 96.8% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 47/92 = 51.1% | 85/92 = 92.4% | 92/92 = 100.0% | 61/92 = 66.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state18__Car Cover__state21__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`

### Product 224

**Selected features:** selected = {5Y, ABI, AD, AWD, AWL, C, CC, Ca, DAP, FSD, MX, PWh, S, S7, T, TP, TuW, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 34/43 = 79.1% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 58/81 = 71.6% | 81/81 = 100.0% | 81/81 = 100.0% | 55/81 = 67.9% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 28/30 = 93.3% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 66/70 = 94.3% | 70/70 = 100.0% | 45/70 = 64.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Car Cover__state21__state28`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`

### Product 225

**Selected features:** selected = {AD, BWI, CCT, Ca, DBM, F, HC, IW, LRAWD, MC, MY, S, S7, T, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 44/51 = 86.3% | 51/51 = 100.0% | 51/51 = 100.0% | 48/51 = 94.1% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 32/46 = 69.6% | 45/46 = 97.8% | 46/46 = 100.0% | 41/46 = 89.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 226

**Selected features:** selected = {AD, AWD, CI, Ca, L, MC, MS, Mo, QS, S, S5, T, TeW, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 227

**Selected features:** selected = {AD, AWD, CI, CW, Ca, DAP, F, MX, Mo, S, S7, SG, T, TP, YS}

**Repaired FTS:** 28 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 30/31 = 96.8% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 36/37 = 97.3% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 37 | 1/37 = 2.7% | 0/36 = 0.0% | 34/36 = 94.4% | 36/36 = 100.0% | 36/36 = 100.0% | 36/36 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 228

**Selected features:** selected = {AD, BWI, CCT, Ca, F, FSD, HC, MY, PAWD, S, S5, SG, T, UW, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 42/51 = 82.4% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 31/46 = 67.4% | 45/46 = 97.8% | 46/46 = 100.0% | 38/46 = 82.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 229

**Selected features:** selected = {AD, BWI, C, Ca, HC, LRAWD, M3, PW, RR, S, S5, SG, T, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 27/30 = 90.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 34/37 = 91.9% | 37/37 = 100.0% | 37/37 = 100.0% | 35/37 = 94.6% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 28/35 = 80.0% | 35/35 = 100.0% | 35/35 = 100.0% | 31/35 = 88.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state27`

### Product 230

**Selected features:** selected = {5Y, ABI, AD, AWL, C, Ca, DAP, FSD, HC, MX, PD, PWh, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 37/43 = 86.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 64/81 = 79.0% | 81/81 = 100.0% | 81/81 = 100.0% | 70/81 = 86.4% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 44/70 = 62.9% | 66/70 = 94.3% | 70/70 = 100.0% | 53/70 = 75.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`

### Product 231

**Selected features:** selected = {ABI, ACT, AD, AWD, CC, Ca, DBM, FSD, HC, L, MX, Mo, S, S5, T, TP, TuW, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 59/81 = 72.8% | 81/81 = 100.0% | 81/81 = 100.0% | 75/81 = 92.6% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 61/70 = 87.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`

### Product 232

**Selected features:** selected = {ABI, AD, AW, AWD, AWL, C, Ca, DAP, FSD, MC, MS, RR, S, S5, T, UR, YS}

**Repaired FTS:** 30 states, 45 transitions (45 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 30/45 = 66.7% | 45/45 = 100.0% | 45/45 = 100.0% | 38/45 = 84.4% |
| ActionExchange | 105 | 0/105 = 0.0% | 0/105 = 0.0% | 61/105 = 58.1% | 105/105 = 100.0% | 105/105 = 100.0% | 86/105 = 81.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 105 | 21/105 = 20.0% | 0/84 = 0.0% | 42/84 = 50.0% | 77/84 = 91.7% | 84/84 = 100.0% | 58/84 = 69.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Roof 
Rack__state23__state26`
- `TDE__state17__Roof 
Rack__state23__state24`
- `TDE__state17__Roof 
Rack__state23__state18`
- `TDE__state17__Roof 
Rack__state23__state28`
- `TDE__state26__Roof 
Rack__state23__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state28__Roof 
Rack__state23__state26`
- `TDE__state28__Roof 
Rack__state23__state24`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state18__Roof 
Rack__state23__state24`
- `TDE__state18__Roof 
Rack__state23__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 233

**Selected features:** selected = {ABI, AD, AWD, CW, Ca, DAP, HC, L, MX, Mo, S, S5, SB, T, TP, YS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 40/45 = 88.9% | 45/45 = 100.0% | 45/45 = 100.0% | 44/45 = 97.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 41/43 = 95.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 234

**Selected features:** selected = {AD, AWD, AWL, BWI, C, Ca, MS, PWh, S, S5, SS, T, TeW, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 27/35 = 77.1% | 35/35 = 100.0% | 35/35 = 100.0% | 33/35 = 94.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 235

**Selected features:** selected = {ABI, AD, AWL, Ca, DBM, F, FSD, HC, MX, Mo, PD, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 35/38 = 92.1% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 46/59 = 78.0% | 59/59 = 100.0% | 59/59 = 100.0% | 55/59 = 93.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 38/54 = 70.4% | 53/54 = 98.1% | 54/54 = 100.0% | 48/54 = 88.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 236

**Selected features:** selected = {5Y, ABI, ACT, AD, AWD, C, CC, CW, Ca, HC, MC, MX, PWh, S, S6, T, TP, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 36/43 = 83.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 62/81 = 76.5% | 81/81 = 100.0% | 81/81 = 100.0% | 67/81 = 82.7% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 45/70 = 64.3% | 67/70 = 95.7% | 70/70 = 100.0% | 54/70 = 77.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state27__Car Cover__state21__state26`

### Product 237

**Selected features:** selected = {ABI, AD, CCT, Ca, DBM, HC, IW, L, LRAWD, MC, MY, Mo, S, S5, SS, T, TP, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 36/43 = 83.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 59/81 = 72.8% | 81/81 = 100.0% | 81/81 = 100.0% | 67/81 = 82.7% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 67/70 = 95.7% | 70/70 = 100.0% | 52/70 = 74.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state27__Sunshade__state22__state26`
- `TDE__state27__Sunshade__state22__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Center 
Console 
Trays__state25__state26`
- `TDE__state26__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 238

**Selected features:** selected = {AD, AWD, AWL, BWI, C, CW, Ca, DAP, HC, MX, S, S5, T, TP, UR, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 49/55 = 89.1% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 38/50 = 76.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__Home 
Charger__state27__state28`

### Product 239

**Selected features:** selected = {AD, BWI, CCT, Ca, FSD, GW, L, LRRWD, MY, QS, S, S5, T, TP, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 34/39 = 87.2% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 240

**Selected features:** selected = {AD, AWD, AWL, BWI, Ca, DAP, L, MS, Mo, S, S5, SB, T, TeW, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 39/39 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 241

**Selected features:** selected = {5Y, AD, AWL, C, CI, CW, Ca, FSD, MX, PD, PWh, S, S6, T, TP, YS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 30/34 = 88.2% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 40/45 = 88.9% | 45/45 = 100.0% | 45/45 = 100.0% | 44/45 = 97.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 40/43 = 93.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 242

**Selected features:** selected = {5Y, AD, AWD, BWI, C, Ca, FSD, MC, MX, PWh, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 32/34 = 94.1% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 42/45 = 93.3% | 45/45 = 100.0% | 45/45 = 100.0% | 44/45 = 97.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 41/43 = 95.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 243

**Selected features:** selected = {5Y, AD, AW, BWI, C, Ca, DAP, DBM, MS, PD, S, S5, T, YS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 244

**Selected features:** selected = {5Y, ABI, AD, AWL, C, CW, Ca, DAP, MX, PD, PWh, S, S6, T, TP, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 40/45 = 88.9% | 45/45 = 100.0% | 45/45 = 100.0% | 43/45 = 95.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 245

**Selected features:** selected = {AD, AWD, BWI, Ca, DAP, FSD, L, MS, S, S5, SG, T, TeW, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 34/37 = 91.9% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 33/35 = 94.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Drive Anywhere
Package__state28__state18`

### Product 246

**Selected features:** selected = {5Y, ABI, AD, C, Ca, FSD, HC, M3, PW, RR, RWD, S, S5, SB, T, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 54/55 = 98.2% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 46/50 = 92.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state18__Roof 
Rack__state23__state27`

### Product 247

**Selected features:** selected = {AD, AW, AWD, CI, Ca, L, MS, RR, S, S5, SS, T, UR, YS}

**Repaired FTS:** 27 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 27/31 = 87.1% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 34/41 = 82.9% | 41/41 = 100.0% | 41/41 = 100.0% | 35/41 = 85.4% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 41 | 1/41 = 2.4% | 0/40 = 0.0% | 28/40 = 70.0% | 38/40 = 95.0% | 40/40 = 100.0% | 30/40 = 75.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 248

**Selected features:** selected = {AD, CI, CW, Ca, FSD, L, MC, MX, PD, PWh, S, S6, T, TP, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 34/39 = 87.2% | 39/39 = 100.0% | 39/39 = 100.0% | 39/39 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 249

**Selected features:** selected = {5Y, ABI, AD, AWL, C, CCT, Ca, FSD, IW, LRRWD, MY, S, S5, SS, T, UR, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 33/41 = 80.5% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 60/77 = 77.9% | 77/77 = 100.0% | 77/77 = 100.0% | 70/77 = 90.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 54/66 = 81.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state25__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state18__Sunshade__state22__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 250

**Selected features:** selected = {5Y, AD, AWL, BWI, C, Ca, DBM, FSD, GW, LRRWD, MC, MY, S, S5, T, TP, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 33/38 = 86.8% | 38/38 = 100.0% | 38/38 = 100.0% | 36/38 = 94.7% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 50/59 = 84.7% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 50/54 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 251

**Selected features:** selected = {ACT, AD, AWD, BWI, C, CC, CW, Ca, MC, MX, PWh, S, S7, T, TP, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 46/55 = 83.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 38/50 = 76.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Car Cover__state21__state26`

### Product 252

**Selected features:** selected = {AD, AWD, BWI, C, CC, CW, Ca, MX, S, S7, SB, T, TP, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 253

**Selected features:** selected = {ABI, AD, AWD, AWL, CW, Ca, DAP, L, MX, Mo, S, S7, SG, T, TP, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 40/45 = 88.9% | 45/45 = 100.0% | 45/45 = 100.0% | 43/45 = 95.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 254

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, CC, CW, Ca, F, HC, MX, Mo, QS, S, S6, T, TP, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 59/81 = 72.8% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 67/70 = 95.7% | 70/70 = 100.0% | 60/70 = 85.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 255

**Selected features:** selected = {AD, AWD, BWI, Ca, DAP, F, FSD, HC, MX, Mo, S, S6, SG, T, TP, TuW, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 33/38 = 86.8% | 38/38 = 100.0% | 38/38 = 100.0% | 33/38 = 86.8% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 50/59 = 84.7% | 59/59 = 100.0% | 59/59 = 100.0% | 52/59 = 88.1% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 44/54 = 81.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`

### Product 256

**Selected features:** selected = {AD, AWD, AWL, BWI, Ca, F, FSD, HC, MX, S, S7, T, TP, TuW, UR, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 35/36 = 97.2% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 42/55 = 76.4% | 55/55 = 100.0% | 55/55 = 100.0% | 55/55 = 100.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 48/50 = 96.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 257

**Selected features:** selected = {AD, AWD, AWL, CC, CI, Ca, DAP, DBM, F, HC, MX, Mo, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 56/81 = 69.1% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 67/70 = 95.7% | 70/70 = 100.0% | 59/70 = 84.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 258

**Selected features:** selected = {ABI, AD, AW, Ca, DAP, FSD, L, MS, Mo, PD, QS, RR, S, S5, SS, T, WS}

**Repaired FTS:** 30 states, 42 transitions (42 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 42 | 0/42 = 0.0% | 0/42 = 0.0% | 33/42 = 78.6% | 42/42 = 100.0% | 42/42 = 100.0% | 34/42 = 81.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 62/81 = 76.5% | 81/81 = 100.0% | 81/81 = 100.0% | 62/81 = 76.5% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 81 | 8/81 = 9.9% | 0/73 = 0.0% | 41/73 = 56.2% | 65/73 = 89.0% | 73/73 = 100.0% | 47/73 = 64.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state18__Sunshade__state22__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state16__Roof 
Rack__state23__state28`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Roof 
Rack__state23__state28`

### Product 259

**Selected features:** selected = {ABI, AD, CW, Ca, DAP, L, MX, Mo, PD, S, S6, T, TP, UR, YS}

**Repaired FTS:** 28 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 30/31 = 96.8% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 36/37 = 97.3% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 37 | 1/37 = 2.7% | 0/36 = 0.0% | 34/36 = 94.4% | 36/36 = 100.0% | 36/36 = 100.0% | 36/36 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 260

**Selected features:** selected = {AD, AWD, CI, Ca, DAP, DBM, FSD, L, MX, S, S7, T, TP, TuW, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 39/39 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Drive Anywhere
Package__state28__state18`

### Product 261

**Selected features:** selected = {AD, AWD, CI, Ca, DAP, F, FSD, MS, RR, S, S5, SS, T, TeW, UR, YS}

**Repaired FTS:** 29 states, 40 transitions (40 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 40 | 0/40 = 0.0% | 0/40 = 0.0% | 30/40 = 75.0% | 40/40 = 100.0% | 40/40 = 100.0% | 35/40 = 87.5% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 53/77 = 68.8% | 77/77 = 100.0% | 77/77 = 100.0% | 67/77 = 87.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 77 | 8/77 = 10.4% | 0/69 = 0.0% | 35/69 = 50.7% | 61/69 = 88.4% | 69/69 = 100.0% | 52/69 = 75.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Roof 
Rack__state23__state18`
- `TDE__state17__Roof 
Rack__state23__state28`
- `TDE__state18__Sunshade__state22__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Sunshade__state22__state18`
- `TDE__state17__Sunshade__state22__state28`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Roof 
Rack__state23__state28`

### Product 262

**Selected features:** selected = {AD, BWI, Ca, DAP, DBM, FSD, HC, L, MS, PD, S, S5, T, TeW, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 42/51 = 82.4% | 51/51 = 100.0% | 51/51 = 100.0% | 49/51 = 96.1% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 31/46 = 67.4% | 45/46 = 97.8% | 46/46 = 100.0% | 41/46 = 89.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`

### Product 263

**Selected features:** selected = {AD, BWI, CC, CW, Ca, DAP, FSD, L, MC, MX, PD, QS, S, S6, T, TP, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 33/41 = 80.5% | 41/41 = 100.0% | 41/41 = 100.0% | 36/41 = 87.8% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 58/77 = 75.3% | 77/77 = 100.0% | 77/77 = 100.0% | 69/77 = 89.6% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 43/66 = 65.2% | 62/66 = 93.9% | 66/66 = 100.0% | 52/66 = 78.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 264

**Selected features:** selected = {ACT, AD, CI, CW, Ca, HC, L, MC, MX, PD, S, S6, T, TP, UR, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 48/55 = 87.3% | 55/55 = 100.0% | 55/55 = 100.0% | 51/55 = 92.7% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 36/50 = 72.0% | 49/50 = 98.0% | 50/50 = 100.0% | 45/50 = 90.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Mobile 
Charger__state26__state27`

### Product 265

**Selected features:** selected = {ABI, ACT, AD, AWD, Ca, L, MX, S, S7, SG, T, TP, TuW, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 266

**Selected features:** selected = {AD, AWD, AWL, BWI, CW, Ca, F, FSD, HC, MC, MX, Mo, S, S5, SG, T, TP, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 56/81 = 69.1% | 81/81 = 100.0% | 81/81 = 100.0% | 75/81 = 92.6% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 67/70 = 95.7% | 70/70 = 100.0% | 58/70 = 82.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 267

**Selected features:** selected = {5Y, ABI, AD, AWD, AWL, C, Ca, DAP, HC, MS, QS, S, S5, SS, T, TeW, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 34/41 = 82.9% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 54/77 = 70.1% | 77/77 = 100.0% | 77/77 = 100.0% | 57/77 = 74.0% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 46/66 = 69.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state28__Sunshade__state22__state27`

### Product 268

**Selected features:** selected = {AD, AWL, CI, CW, Ca, DAP, F, FSD, HC, MX, Mo, PD, S, S6, SG, T, TP, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 55/81 = 67.9% | 81/81 = 100.0% | 81/81 = 100.0% | 73/81 = 90.1% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 59/70 = 84.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 269

**Selected features:** selected = {ACT, AD, AWD, AWL, CC, CI, CW, Ca, F, MC, MX, Mo, S, S5, SG, T, TP, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 58/81 = 71.6% | 81/81 = 100.0% | 81/81 = 100.0% | 75/81 = 92.6% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 66/70 = 94.3% | 70/70 = 100.0% | 57/70 = 81.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 270

**Selected features:** selected = {ACT, AD, AWD, AWL, CC, CI, Ca, DAP, L, MC, MX, Mo, PWh, S, S7, T, TP, TuW, YS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 35/49 = 71.4% | 49/49 = 100.0% | 49/49 = 100.0% | 40/49 = 81.6% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 69/113 = 61.1% | 113/113 = 100.0% | 113/113 = 100.0% | 83/113 = 73.5% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 30/31 = 96.8% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 47/92 = 51.1% | 85/92 = 92.4% | 92/92 = 100.0% | 61/92 = 66.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`

### Product 271

**Selected features:** selected = {ABI, AD, AWD, CW, Ca, DAP, L, MC, MX, S, S7, T, TP, UR, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state28`

### Product 272

**Selected features:** selected = {5Y, ACT, AD, BWI, C, Ca, HC, MX, PD, QS, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 42/45 = 93.3% | 45/45 = 100.0% | 45/45 = 100.0% | 44/45 = 97.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 36/43 = 83.7% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`

### Product 273

**Selected features:** selected = {5Y, ABI, AD, AWL, C, Ca, GW, HC, LRAWD, MC, MY, S, S7, SG, SS, T, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 34/41 = 82.9% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 58/77 = 75.3% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 62/66 = 93.9% | 66/66 = 100.0% | 47/66 = 71.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 274

**Selected features:** selected = {AD, AW, AWD, CI, Ca, FSD, L, MC, MS, S, S5, SG, SS, T, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 30/34 = 88.2% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 44/51 = 86.3% | 51/51 = 100.0% | 51/51 = 100.0% | 49/51 = 96.1% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 35/46 = 76.1% | 45/46 = 97.8% | 46/46 = 100.0% | 41/46 = 89.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state18__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 275

**Selected features:** selected = {ABI, ACT, AD, AWL, CC, CW, Ca, FSD, L, MX, Mo, PD, PWh, S, S6, T, TP, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 40/43 = 93.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 56/81 = 69.1% | 81/81 = 100.0% | 81/81 = 100.0% | 74/81 = 91.4% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 61/70 = 87.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 276

**Selected features:** selected = {ABI, ACT, AD, Ca, F, HC, MC, MX, Mo, PD, S, S6, T, TP, TuW, UR, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 33/38 = 86.8% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 50/59 = 84.7% | 59/59 = 100.0% | 59/59 = 100.0% | 50/59 = 84.7% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 40/54 = 74.1% | 53/54 = 98.1% | 54/54 = 100.0% | 43/54 = 79.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`

### Product 277

**Selected features:** selected = {AD, AWL, BWI, Ca, DAP, F, HC, MC, MS, Mo, PD, PWh, S, S5, SS, T, TeW, YS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 32/47 = 68.1% | 47/47 = 100.0% | 47/47 = 100.0% | 42/47 = 89.4% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 65/109 = 59.6% | 109/109 = 100.0% | 109/109 = 100.0% | 92/109 = 84.4% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 44/88 = 50.0% | 80/88 = 90.9% | 88/88 = 100.0% | 69/88 = 78.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state27__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Sunshade__state22__state27`
- `TDE__state17__Sunshade__state22__state26`
- `TDE__state17__Sunshade__state22__state28`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Sunshade__state22__state27`
- `TDE__state28__Sunshade__state22__state26`

### Product 278

**Selected features:** selected = {AD, AWD, CI, CW, Ca, DAP, F, FSD, HC, MC, MX, S, S6, SB, T, TP, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 39/41 = 95.1% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 54/77 = 70.1% | 77/77 = 100.0% | 77/77 = 100.0% | 73/77 = 94.8% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 58/66 = 87.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state18__Home 
Charger__state27__state28`

### Product 279

**Selected features:** selected = {ABI, AD, AWL, CW, Ca, F, FSD, MX, PD, S, S6, SB, T, TP, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 280

**Selected features:** selected = {AD, AWD, BWI, Ca, DBM, HC, L, MX, Mo, S, S7, T, TP, TuW, YS}

**Repaired FTS:** 28 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 29/31 = 93.5% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 33/37 = 89.2% | 37/37 = 100.0% | 37/37 = 100.0% | 36/37 = 97.3% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 26/27 = 96.3% |
| TransitionDestinationExchange | 37 | 1/37 = 2.7% | 0/36 = 0.0% | 29/36 = 80.6% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 281

**Selected features:** selected = {AD, BWI, C, Ca, FSD, GW, HC, LRAWD, MY, S, S5, SG, T, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 27/35 = 77.1% | 35/35 = 100.0% | 35/35 = 100.0% | 33/35 = 94.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`

### Product 282

**Selected features:** selected = {ABI, AD, C, CCT, Ca, HC, MY, PAWD, QS, S, S5, SS, T, TP, UW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 51/55 = 92.7% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state27__Sunshade__state22__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`

### Product 283

**Selected features:** selected = {ACT, AD, AWD, CC, CI, CW, Ca, DAP, F, HC, MC, MX, Mo, PWh, S, S5, T, TP, WS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 35/49 = 71.4% | 49/49 = 100.0% | 49/49 = 100.0% | 42/49 = 85.7% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 72/113 = 63.7% | 113/113 = 100.0% | 113/113 = 100.0% | 96/113 = 85.0% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 48/92 = 52.2% | 85/92 = 92.4% | 92/92 = 100.0% | 73/92 = 79.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 284

**Selected features:** selected = {5Y, ACT, AD, AWD, AWL, C, CI, CW, Ca, FSD, MC, MX, PWh, S, S6, T, TP, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 59/81 = 72.8% | 81/81 = 100.0% | 81/81 = 100.0% | 72/81 = 88.9% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 56/70 = 80.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 285

**Selected features:** selected = {ACT, AD, AWL, CC, CI, CW, Ca, DAP, F, FSD, MC, MX, Mo, PD, S, S6, SB, T, TP, YS}

**Repaired FTS:** 33 states, 56 transitions (56 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 35/56 = 62.5% | 56/56 = 100.0% | 56/56 = 100.0% | 47/56 = 83.9% |
| ActionExchange | 157 | 0/157 = 0.0% | 0/157 = 0.0% | 80/157 = 51.0% | 157/157 = 100.0% | 157/157 = 100.0% | 132/157 = 84.1% |
| StateMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| TransitionDestinationExchange | 157 | 36/157 = 22.9% | 0/121 = 0.0% | 53/121 = 43.8% | 108/121 = 89.3% | 121/121 = 100.0% | 86/121 = 71.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (36):

- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`

### Product 286

**Selected features:** selected = {AD, AWD, BWI, CC, Ca, DAP, DBM, HC, L, MX, S, S7, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 45/55 = 81.8% | 55/55 = 100.0% | 55/55 = 100.0% | 46/55 = 83.6% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 38/50 = 76.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state28`

### Product 287

**Selected features:** selected = {ACT, AD, AWL, C, CI, Ca, DAP, FSD, MC, MX, PD, PWh, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 37/47 = 78.7% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 66/109 = 60.6% | 109/109 = 100.0% | 109/109 = 100.0% | 77/109 = 70.6% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 44/88 = 50.0% | 81/88 = 92.0% | 88/88 = 100.0% | 54/88 = 61.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 288

**Selected features:** selected = {5Y, ABI, AD, C, CCT, Ca, HC, IW, LRAWD, MY, S, S5, SB, T, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 30/32 = 93.8% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 35/39 = 89.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Center 
Console 
Trays__state25__state27`

### Product 289

**Selected features:** selected = {ACT, AD, AWD, CC, CI, Ca, DAP, HC, L, MX, QS, S, S7, T, TP, TuW, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 39/41 = 95.1% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 75/77 = 97.4% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 57/66 = 86.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 290

**Selected features:** selected = {AD, BWI, Ca, DAP, F, HC, MS, PD, QS, S, S5, SS, T, TeW, YS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 28/34 = 82.4% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 38/51 = 74.5% | 51/51 = 100.0% | 51/51 = 100.0% | 46/51 = 90.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 30/46 = 65.2% | 45/46 = 97.8% | 46/46 = 100.0% | 37/46 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Sunshade__state22__state27`
- `TDE__state17__Sunshade__state22__state28`
- `TDE__state28__Sunshade__state22__state27`

### Product 291

**Selected features:** selected = {ACT, AD, AWD, BWI, CC, Ca, DAP, L, MX, Mo, S, S7, SB, T, TP, TuW, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 35/38 = 92.1% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 51/59 = 86.4% | 59/59 = 100.0% | 59/59 = 100.0% | 56/59 = 94.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 43/54 = 79.6% | 53/54 = 98.1% | 54/54 = 100.0% | 47/54 = 87.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 292

**Selected features:** selected = {ABI, AD, AWD, Ca, F, HC, MX, Mo, S, S7, SG, T, TP, TuW, YS}

**Repaired FTS:** 28 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 29/31 = 93.5% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 33/37 = 89.2% | 37/37 = 100.0% | 37/37 = 100.0% | 36/37 = 97.3% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 26/27 = 96.3% |
| TransitionDestinationExchange | 37 | 1/37 = 2.7% | 0/36 = 0.0% | 29/36 = 80.6% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 293

**Selected features:** selected = {AD, AWL, BWI, Ca, F, FSD, HC, LRRWD, M3, MC, Mo, PW, RR, S, S5, SG, T, WS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 43/47 = 91.5% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 65/109 = 59.6% | 109/109 = 100.0% | 109/109 = 100.0% | 100/109 = 91.7% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 44/88 = 50.0% | 80/88 = 90.9% | 88/88 = 100.0% | 75/88 = 85.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state26__Roof 
Rack__state23__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__Roof 
Rack__state23__state27`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state18__Roof 
Rack__state23__state24`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state27__Roof 
Rack__state23__state26`
- `TDE__state27__Roof 
Rack__state23__state24`

### Product 294

**Selected features:** selected = {AD, CI, Ca, FSD, HC, L, MC, MX, PD, QS, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 32/36 = 88.9% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 50/55 = 90.9% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 41/50 = 82.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 295

**Selected features:** selected = {ABI, AD, AWL, Ca, F, FSD, GW, LRRWD, MY, Mo, S, S5, SB, SS, T, TP, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 33/38 = 86.8% | 38/38 = 100.0% | 38/38 = 100.0% | 33/38 = 86.8% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 50/59 = 84.7% | 59/59 = 100.0% | 59/59 = 100.0% | 50/59 = 84.7% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 43/54 = 79.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Sunshade__state22__state18`

### Product 296

**Selected features:** selected = {5Y, AD, AWD, AWL, BWI, C, CC, Ca, DAP, HC, MC, MX, QS, S, S7, T, TP, TuW, YS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 35/49 = 71.4% | 49/49 = 100.0% | 49/49 = 100.0% | 41/49 = 83.7% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 70/113 = 61.9% | 113/113 = 100.0% | 113/113 = 100.0% | 94/113 = 83.2% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 46/92 = 50.0% | 85/92 = 92.4% | 92/92 = 100.0% | 69/92 = 75.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 297

**Selected features:** selected = {5Y, ACT, AD, AWD, BWI, C, CC, CW, Ca, DAP, MX, S, S6, SG, T, TP, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 33/38 = 86.8% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 51/59 = 86.4% | 59/59 = 100.0% | 59/59 = 100.0% | 50/59 = 84.7% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 43/54 = 79.6% | 53/54 = 98.1% | 54/54 = 100.0% | 43/54 = 79.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 298

**Selected features:** selected = {5Y, ABI, AD, AWD, C, Ca, DAP, HC, MC, MX, S, S5, SG, T, TP, TuW, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 35/38 = 92.1% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 49/59 = 83.1% | 59/59 = 100.0% | 59/59 = 100.0% | 55/59 = 93.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 46/54 = 85.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 299

**Selected features:** selected = {AD, AWD, BWI, CC, Ca, DAP, FSD, HC, L, MX, Mo, S, S5, T, TP, TuW, UR, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 59/81 = 72.8% | 81/81 = 100.0% | 81/81 = 100.0% | 75/81 = 92.6% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 67/70 = 95.7% | 70/70 = 100.0% | 57/70 = 81.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`

### Product 300

**Selected features:** selected = {AD, AWL, BWI, C, CCT, Ca, FSD, LRAWD, M3, NW, RR, S, S5, SB, T, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 31/39 = 79.5% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 51/73 = 69.9% | 73/73 = 100.0% | 73/73 = 100.0% | 54/73 = 74.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 35/62 = 56.5% | 58/62 = 93.5% | 62/62 = 100.0% | 40/62 = 64.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state25`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state25__Roof 
Rack__state23__state24`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state18`
- `TDE__state18__Roof 
Rack__state23__state25`
- `TDE__state18__Roof 
Rack__state23__state24`

### Product 301

**Selected features:** selected = {AD, AWL, CI, Ca, DAP, FSD, HC, L, MS, PD, PWh, RR, S, S5, T, TeW, YS}

**Repaired FTS:** 30 states, 45 transitions (45 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 30/45 = 66.7% | 45/45 = 100.0% | 45/45 = 100.0% | 39/45 = 86.7% |
| ActionExchange | 105 | 0/105 = 0.0% | 0/105 = 0.0% | 57/105 = 54.3% | 105/105 = 100.0% | 105/105 = 100.0% | 91/105 = 86.7% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 105 | 21/105 = 20.0% | 0/84 = 0.0% | 40/84 = 47.6% | 77/84 = 91.7% | 84/84 = 100.0% | 61/84 = 72.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state17__Roof 
Rack__state23__state27`
- `TDE__state17__Roof 
Rack__state23__state24`
- `TDE__state17__Roof 
Rack__state23__state18`
- `TDE__state17__Roof 
Rack__state23__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state28__Roof 
Rack__state23__state27`
- `TDE__state28__Roof 
Rack__state23__state24`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state18__Roof 
Rack__state23__state27`
- `TDE__state18__Roof 
Rack__state23__state24`
- `TDE__state18__Roof 
Rack__state23__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state27__Roof 
Rack__state23__state24`

### Product 302

**Selected features:** selected = {ACT, AD, AWD, CC, CI, Ca, FSD, HC, L, MX, Mo, S, S7, SG, T, TP, TuW, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 59/81 = 72.8% | 81/81 = 100.0% | 81/81 = 100.0% | 73/81 = 90.1% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 44/70 = 62.9% | 67/70 = 95.7% | 70/70 = 100.0% | 59/70 = 84.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state18`

### Product 303

**Selected features:** selected = {ACT, AD, AWD, BWI, C, CW, Ca, DAP, FSD, MX, QS, S, S7, T, TP, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 51/55 = 92.7% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 38/50 = 76.0% | 49/50 = 98.0% | 50/50 = 100.0% | 44/50 = 88.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 304

**Selected features:** selected = {AD, AWD, C, CC, CI, CW, Ca, DAP, FSD, MC, MX, QS, S, S7, T, TP, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 33/41 = 80.5% | 41/41 = 100.0% | 41/41 = 100.0% | 36/41 = 87.8% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 58/77 = 75.3% | 77/77 = 100.0% | 77/77 = 100.0% | 69/77 = 89.6% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 43/66 = 65.2% | 62/66 = 93.9% | 66/66 = 100.0% | 52/66 = 78.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 305

**Selected features:** selected = {ABI, AD, Ca, FSD, L, LRAWD, M3, MC, NW, RR, S, S5, T, UR, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 42/51 = 82.4% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 31/46 = 67.4% | 45/46 = 97.8% | 46/46 = 100.0% | 41/46 = 89.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 306

**Selected features:** selected = {ACT, AD, AWD, AWL, C, CI, CW, Ca, DAP, DBM, HC, MX, S, S5, T, TP, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 54/77 = 70.1% | 77/77 = 100.0% | 77/77 = 100.0% | 67/77 = 87.0% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 52/66 = 78.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 307

**Selected features:** selected = {ABI, AD, CW, Ca, DAP, F, HC, MX, Mo, PD, PWh, S, S6, T, TP, YS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 40/45 = 88.9% | 45/45 = 100.0% | 45/45 = 100.0% | 44/45 = 97.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 41/43 = 95.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 308

**Selected features:** selected = {ABI, AD, AWD, Ca, DAP, F, FSD, HC, MS, RR, S, S5, SG, T, TeW, YS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 35/39 = 89.7% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 51/73 = 69.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 36/62 = 58.1% | 59/62 = 95.2% | 62/62 = 100.0% | 48/62 = 77.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state17__Roof 
Rack__state23__state27`
- `TDE__state17__Roof 
Rack__state23__state18`
- `TDE__state17__Roof 
Rack__state23__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state28__Roof 
Rack__state23__state27`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state18__Roof 
Rack__state23__state27`
- `TDE__state18__Roof 
Rack__state23__state28`

### Product 309

**Selected features:** selected = {ABI, AD, AWL, CCT, Ca, FSD, IW, L, LRRWD, MC, MY, PWh, S, S5, SS, T, WS}

**Repaired FTS:** 30 states, 45 transitions (45 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 33/45 = 73.3% | 45/45 = 100.0% | 45/45 = 100.0% | 39/45 = 86.7% |
| ActionExchange | 105 | 0/105 = 0.0% | 0/105 = 0.0% | 74/105 = 70.5% | 105/105 = 100.0% | 105/105 = 100.0% | 92/105 = 87.6% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 105 | 21/105 = 20.0% | 0/84 = 0.0% | 42/84 = 50.0% | 76/84 = 90.5% | 84/84 = 100.0% | 67/84 = 79.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state25__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state18__Sunshade__state22__state26`
- `TDE__state18__Sunshade__state22__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Center 
Console 
Trays__state25__state26`
- `TDE__state26__Sunshade__state22__state25`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Center 
Console 
Trays__state25__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 310

**Selected features:** selected = {5Y, ACT, AD, AWL, C, CI, CW, Ca, DAP, FSD, MX, PD, S, S6, SG, T, TP, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 58/81 = 71.6% | 81/81 = 100.0% | 81/81 = 100.0% | 73/81 = 90.1% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 66/70 = 94.3% | 70/70 = 100.0% | 59/70 = 84.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 311

**Selected features:** selected = {ABI, AD, AWL, Ca, L, MC, MS, PD, S, S5, SB, SS, T, TeW, YS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 42/51 = 82.4% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 31/46 = 67.4% | 45/46 = 97.8% | 46/46 = 100.0% | 38/46 = 82.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Sunshade__state22__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`

### Product 312

**Selected features:** selected = {AD, AWD, AWL, C, CC, CI, CW, Ca, MX, S, S5, SB, T, TP, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`

### Product 313

**Selected features:** selected = {AD, AWD, BWI, CW, Ca, DAP, F, FSD, MC, MX, Mo, PWh, S, S5, T, TP, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 33/38 = 86.8% | 38/38 = 100.0% | 38/38 = 100.0% | 36/38 = 94.7% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 50/59 = 84.7% | 59/59 = 100.0% | 59/59 = 100.0% | 55/59 = 93.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 42/54 = 77.8% | 53/54 = 98.1% | 54/54 = 100.0% | 49/54 = 90.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 314

**Selected features:** selected = {ACT, AD, AWD, BWI, CC, Ca, F, FSD, MX, QS, S, S7, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 51/55 = 92.7% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 38/50 = 76.0% | 49/50 = 98.0% | 50/50 = 100.0% | 44/50 = 88.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`

### Product 315

**Selected features:** selected = {AD, AW, AWD, BWI, Ca, DAP, L, MS, Mo, PWh, RR, S, S5, T, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state17__Roof 
Rack__state23__state28`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 316

**Selected features:** selected = {ACT, AD, AWD, AWL, CC, CI, Ca, DAP, FSD, L, MC, MX, Mo, S, S7, T, TP, TuW, UR, WS}

**Repaired FTS:** 33 states, 56 transitions (56 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 37/56 = 66.1% | 56/56 = 100.0% | 56/56 = 100.0% | 45/56 = 80.4% |
| ActionExchange | 157 | 0/157 = 0.0% | 0/157 = 0.0% | 87/157 = 55.4% | 157/157 = 100.0% | 157/157 = 100.0% | 130/157 = 82.8% |
| StateMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| TransitionDestinationExchange | 157 | 36/157 = 22.9% | 0/121 = 0.0% | 53/121 = 43.8% | 105/121 = 86.8% | 121/121 = 100.0% | 89/121 = 73.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (36):

- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 317

**Selected features:** selected = {5Y, AD, BWI, C, Ca, FSD, GW, LRAWD, MY, PWh, S, S5, T, TP, WS}

**Repaired FTS:** 28 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 30/31 = 96.8% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 36/37 = 97.3% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 37 | 1/37 = 2.7% | 0/36 = 0.0% | 34/36 = 94.4% | 36/36 = 100.0% | 36/36 = 100.0% | 36/36 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 318

**Selected features:** selected = {ABI, AD, AWL, Ca, DBM, GW, L, LRAWD, MY, Mo, S, S5, T, TP, WS}

**Repaired FTS:** 28 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 29/31 = 93.5% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 35/37 = 94.6% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 37 | 1/37 = 2.7% | 0/36 = 0.0% | 32/36 = 88.9% | 36/36 = 100.0% | 36/36 = 100.0% | 36/36 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 319

**Selected features:** selected = {AD, BWI, CCT, Ca, DBM, F, GW, HC, LRAWD, MY, S, S5, SS, T, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 28/34 = 82.4% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 39/51 = 76.5% | 51/51 = 100.0% | 51/51 = 100.0% | 50/51 = 98.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 30/46 = 65.2% | 45/46 = 97.8% | 46/46 = 100.0% | 44/46 = 95.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state27__Sunshade__state22__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`

### Product 320

**Selected features:** selected = {AD, AWD, AWL, CI, Ca, F, FSD, MC, MX, S, S7, SG, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 321

**Selected features:** selected = {ABI, AD, AWD, C, Ca, FSD, MC, MS, QS, S, S5, T, TeW, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 34/37 = 91.9% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 33/35 = 94.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 322

**Selected features:** selected = {AD, AWD, CC, CI, Ca, F, FSD, HC, MX, Mo, S, S5, T, TP, TuW, UR, YS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 35/38 = 92.1% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 49/59 = 83.1% | 59/59 = 100.0% | 59/59 = 100.0% | 55/59 = 93.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 47/54 = 87.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state18`

### Product 323

**Selected features:** selected = {ABI, AD, AWD, CC, Ca, HC, L, MX, PWh, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state27`

### Product 324

**Selected features:** selected = {AD, AWD, AWL, CI, Ca, DBM, FSD, HC, L, MC, MS, Mo, S, S5, SS, T, TeW, YS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 43/47 = 91.5% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 66/109 = 60.6% | 109/109 = 100.0% | 109/109 = 100.0% | 99/109 = 90.8% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 42/88 = 47.7% | 80/88 = 90.9% | 88/88 = 100.0% | 70/88 = 79.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state27__Sunshade__state22__state26`
- `TDE__state18__Sunshade__state22__state27`
- `TDE__state18__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state17__Sunshade__state22__state27`
- `TDE__state17__Sunshade__state22__state26`
- `TDE__state17__Sunshade__state22__state18`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 325

**Selected features:** selected = {AD, AWD, BWI, CC, Ca, DAP, F, FSD, HC, MX, S, S6, SG, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 38/41 = 92.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 71/77 = 92.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 63/66 = 95.5% | 66/66 = 100.0% | 51/66 = 77.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`

### Product 326

**Selected features:** selected = {ABI, AD, AWD, AWL, Ca, DAP, F, HC, MX, PWh, S, S5, T, TP, TuW, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 35/36 = 97.2% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 42/55 = 76.4% | 55/55 = 100.0% | 55/55 = 100.0% | 55/55 = 100.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 49/50 = 98.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 327

**Selected features:** selected = {ABI, AD, AWL, CW, Ca, DAP, L, MC, MX, PD, S, S6, SB, T, TP, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 29/36 = 80.6% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 50/55 = 90.9% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 42/50 = 84.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 328

**Selected features:** selected = {5Y, ABI, ACT, AD, AWD, AWL, C, CW, Ca, FSD, HC, MC, MX, S, S6, SG, T, TP, WS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 35/49 = 71.4% | 49/49 = 100.0% | 49/49 = 100.0% | 40/49 = 81.6% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 68/113 = 60.2% | 113/113 = 100.0% | 113/113 = 100.0% | 88/113 = 77.9% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 30/31 = 96.8% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 47/92 = 51.1% | 84/92 = 91.3% | 92/92 = 100.0% | 62/92 = 67.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`

### Product 329

**Selected features:** selected = {AD, AW, AWD, AWL, CI, Ca, DAP, F, FSD, MS, PWh, RR, S, S5, T, YS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 29/39 = 74.4% | 39/39 = 100.0% | 39/39 = 100.0% | 35/39 = 89.7% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 49/73 = 67.1% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 36/62 = 58.1% | 59/62 = 95.2% | 62/62 = 100.0% | 52/62 = 83.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Roof 
Rack__state23__state24`
- `TDE__state17__Roof 
Rack__state23__state18`
- `TDE__state17__Roof 
Rack__state23__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state28__Roof 
Rack__state23__state24`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state18__Roof 
Rack__state23__state24`
- `TDE__state18__Roof 
Rack__state23__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 330

**Selected features:** selected = {5Y, ABI, ACT, AD, AWD, C, CW, Ca, DAP, HC, MC, MX, S, S7, SB, T, TP, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 37/43 = 86.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 61/81 = 75.3% | 81/81 = 100.0% | 81/81 = 100.0% | 68/81 = 84.0% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 52/70 = 74.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`

### Product 331

**Selected features:** selected = {AD, AWD, AWL, CI, Ca, DAP, F, MC, MS, PWh, RR, S, S5, SS, T, TeW, WS}

**Repaired FTS:** 30 states, 46 transitions (46 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 46 | 0/46 = 0.0% | 0/46 = 0.0% | 31/46 = 67.4% | 46/46 = 100.0% | 46/46 = 100.0% | 42/46 = 91.3% |
| ActionExchange | 111 | 0/111 = 0.0% | 0/111 = 0.0% | 65/111 = 58.6% | 111/111 = 100.0% | 111/111 = 100.0% | 106/111 = 95.5% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 111 | 14/111 = 12.6% | 0/97 = 0.0% | 42/97 = 43.3% | 79/97 = 81.4% | 97/97 = 100.0% | 79/97 = 81.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (14):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state28`
- `TDE__state28__Roof 
Rack__state23__state26`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state28__Sunshade__state22__state26`

### Product 332

**Selected features:** selected = {ABI, AD, AWD, AWL, Ca, F, FSD, MX, QS, S, S5, T, TP, TuW, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 36/41 = 87.8% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 31/39 = 79.5% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 333

**Selected features:** selected = {AD, AWL, BWI, Ca, HC, L, M3, PAWD, RR, S, S5, T, UR, WS, WW}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 28/34 = 82.4% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 38/51 = 74.5% | 51/51 = 100.0% | 51/51 = 100.0% | 50/51 = 98.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 30/46 = 65.2% | 45/46 = 97.8% | 46/46 = 100.0% | 42/46 = 91.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state27__Roof 
Rack__state23__state24`

### Product 334

**Selected features:** selected = {AD, AWL, BWI, CCT, Ca, HC, L, LRAWD, M3, MC, NW, S, S5, T, UR, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 33/39 = 84.6% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 51/73 = 69.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 35/62 = 56.5% | 59/62 = 95.2% | 62/62 = 100.0% | 46/62 = 74.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Center 
Console 
Trays__state25__state26`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 335

**Selected features:** selected = {AD, AW, AWD, AWL, C, CI, Ca, MS, RR, S, S5, SG, SS, T, WS}

**Repaired FTS:** 28 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 0/35 = 0.0% | 28/35 = 80.0% | 35/35 = 100.0% | 35/35 = 100.0% | 29/35 = 82.9% |
| ActionExchange | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 41/57 = 71.9% | 57/57 = 100.0% | 57/57 = 100.0% | 46/57 = 80.7% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 26/27 = 96.3% |
| TransitionDestinationExchange | 57 | 2/57 = 3.5% | 0/55 = 0.0% | 31/55 = 56.4% | 51/55 = 92.7% | 55/55 = 100.0% | 37/55 = 67.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 336

**Selected features:** selected = {5Y, ABI, AD, AWL, C, Ca, FSD, HC, IW, LRAWD, MY, PWh, S, S7, SS, T, TP, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 36/43 = 83.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 58/81 = 71.6% | 81/81 = 100.0% | 81/81 = 100.0% | 61/81 = 75.3% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 47/70 = 67.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state18__Sunshade__state22__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state18`

### Product 337

**Selected features:** selected = {ACT, AD, AWD, AWL, CI, Ca, F, FSD, MX, Mo, QS, S, S7, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 35/38 = 92.1% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 49/59 = 83.1% | 59/59 = 100.0% | 59/59 = 100.0% | 55/59 = 93.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 40/54 = 74.1% | 53/54 = 98.1% | 54/54 = 100.0% | 46/54 = 85.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 338

**Selected features:** selected = {5Y, ABI, AD, AWL, C, Ca, FSD, HC, M3, PW, RWD, S, S5, SB, T, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 53/55 = 96.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 42/50 = 84.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`

### Product 339

**Selected features:** selected = {AD, AWD, AWL, BWI, Ca, DAP, DBM, F, HC, MS, S, S5, T, TeW, YS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 28/34 = 82.4% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 38/51 = 74.5% | 51/51 = 100.0% | 51/51 = 100.0% | 49/51 = 96.1% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 30/46 = 65.2% | 45/46 = 97.8% | 46/46 = 100.0% | 39/46 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 340

**Selected features:** selected = {ABI, AD, Ca, F, HC, MC, MY, PAWD, S, S5, SG, SS, T, UW, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 28/34 = 82.4% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 39/51 = 76.5% | 51/51 = 100.0% | 51/51 = 100.0% | 48/51 = 94.1% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 30/46 = 65.2% | 45/46 = 97.8% | 46/46 = 100.0% | 41/46 = 89.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state27__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 341

**Selected features:** selected = {ABI, AD, CCT, Ca, IW, L, LRAWD, MC, MY, Mo, PWh, S, S7, T, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 34/39 = 87.2% | 39/39 = 100.0% | 39/39 = 100.0% | 39/39 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Center 
Console 
Trays__state25__state26`

### Product 342

**Selected features:** selected = {AD, AWD, AWL, BWI, CC, Ca, DBM, FSD, HC, L, MC, MX, Mo, S, S5, T, TP, TuW, WS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 35/49 = 71.4% | 49/49 = 100.0% | 49/49 = 100.0% | 40/49 = 81.6% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 69/113 = 61.1% | 113/113 = 100.0% | 113/113 = 100.0% | 90/113 = 79.6% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 48/92 = 52.2% | 85/92 = 92.4% | 92/92 = 100.0% | 62/92 = 67.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state27__Car Cover__state21__state26`

### Product 343

**Selected features:** selected = {ABI, AD, AWD, CC, Ca, DBM, FSD, HC, L, MX, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 50/55 = 90.9% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 41/50 = 82.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state18`

### Product 344

**Selected features:** selected = {AD, AW, AWD, CI, Ca, F, FSD, MS, Mo, PWh, RR, S, S5, SS, T, YS}

**Repaired FTS:** 29 states, 37 transitions (37 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 30/37 = 81.1% | 37/37 = 100.0% | 37/37 = 100.0% | 32/37 = 86.5% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 45/59 = 76.3% | 59/59 = 100.0% | 59/59 = 100.0% | 50/59 = 84.7% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 59 | 3/59 = 5.1% | 0/56 = 0.0% | 35/56 = 62.5% | 52/56 = 92.9% | 56/56 = 100.0% | 40/56 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state17__Roof 
Rack__state23__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Sunshade__state22__state18`

### Product 345

**Selected features:** selected = {AD, AWL, BWI, CCT, Ca, F, LRRWD, M3, Mo, NW, RR, S, S5, SG, T, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 42/55 = 76.4% | 55/55 = 100.0% | 55/55 = 100.0% | 45/55 = 81.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 38/50 = 76.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state25`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state25__Roof 
Rack__state23__state24`

### Product 346

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, C, CC, Ca, MX, S, S6, SB, T, TP, TuW, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 46/55 = 83.6% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 45/50 = 90.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 347

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, CC, Ca, DAP, HC, L, MX, Mo, S, S6, SB, T, TP, TuW, YS}

**Repaired FTS:** 32 states, 49 transitions (49 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 49 | 0/49 = 0.0% | 0/49 = 0.0% | 34/49 = 69.4% | 49/49 = 100.0% | 49/49 = 100.0% | 42/49 = 85.7% |
| ActionExchange | 113 | 0/113 = 0.0% | 0/113 = 0.0% | 69/113 = 61.1% | 113/113 = 100.0% | 113/113 = 100.0% | 99/113 = 87.6% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 113 | 21/113 = 18.6% | 0/92 = 0.0% | 48/92 = 52.2% | 85/92 = 92.4% | 92/92 = 100.0% | 69/92 = 75.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state28`

### Product 348

**Selected features:** selected = {AD, AWD, AWL, CI, Ca, DAP, HC, L, MC, MX, Mo, S, S7, SB, T, TP, TuW, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 40/43 = 93.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 58/81 = 71.6% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 60/70 = 85.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 349

**Selected features:** selected = {ABI, ACT, AD, AWD, CW, Ca, DAP, FSD, L, MC, MX, S, S7, T, TP, UR, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 36/41 = 87.8% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 52/77 = 67.5% | 77/77 = 100.0% | 77/77 = 100.0% | 72/77 = 93.5% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 55/66 = 83.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`

### Product 350

**Selected features:** selected = {5Y, ACT, AD, AWD, BWI, C, Ca, FSD, HC, MC, MX, S, S6, T, TP, TuW, UR, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 59/81 = 72.8% | 81/81 = 100.0% | 81/81 = 100.0% | 73/81 = 90.1% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 57/70 = 81.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`

### Product 351

**Selected features:** selected = {AD, CC, CI, Ca, FSD, HC, L, MC, MX, Mo, PD, S, S6, T, TP, TuW, UR, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 35/43 = 81.4% | 43/43 = 100.0% | 43/43 = 100.0% | 37/43 = 86.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 64/81 = 79.0% | 81/81 = 100.0% | 81/81 = 100.0% | 64/81 = 79.0% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 44/70 = 62.9% | 67/70 = 95.7% | 70/70 = 100.0% | 51/70 = 72.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state27__Car Cover__state21__state26`

### Product 352

**Selected features:** selected = {ACT, AD, AWD, CI, CW, Ca, DAP, F, FSD, MC, MX, Mo, S, S5, T, TP, UR, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 40/43 = 93.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 56/81 = 69.1% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 62/70 = 88.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`

### Product 353

**Selected features:** selected = {5Y, AD, AW, AWL, BWI, C, Ca, DBM, MC, MS, PD, RR, S, S5, SS, T, YS}

**Repaired FTS:** 30 states, 42 transitions (42 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 42 | 0/42 = 0.0% | 0/42 = 0.0% | 32/42 = 76.2% | 42/42 = 100.0% | 42/42 = 100.0% | 36/42 = 85.7% |
| ActionExchange | 83 | 0/83 = 0.0% | 0/83 = 0.0% | 56/83 = 67.5% | 83/83 = 100.0% | 83/83 = 100.0% | 66/83 = 79.5% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 83 | 6/83 = 7.2% | 0/77 = 0.0% | 40/77 = 51.9% | 67/77 = 87.0% | 77/77 = 100.0% | 57/77 = 74.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (6):

- `TDE__state17__Roof 
Rack__state23__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Sunshade__state22__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`

### Product 354

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, CC, Ca, DBM, F, FSD, HC, MC, MX, S, S5, T, TP, TuW, YS}

**Repaired FTS:** 32 states, 54 transitions (54 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 54 | 0/54 = 0.0% | 0/54 = 0.0% | 34/54 = 63.0% | 54/54 = 100.0% | 54/54 = 100.0% | 46/54 = 85.2% |
| ActionExchange | 153 | 0/153 = 0.0% | 0/153 = 0.0% | 78/153 = 51.0% | 153/153 = 100.0% | 153/153 = 100.0% | 129/153 = 84.3% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 153 | 36/153 = 23.5% | 0/117 = 0.0% | 47/117 = 40.2% | 104/117 = 88.9% | 117/117 = 100.0% | 87/117 = 74.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (36):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state27__Car Cover__state21__state26`

### Product 355

**Selected features:** selected = {AD, CI, Ca, DAP, FSD, L, MS, PD, S, S5, SG, T, TeW, WS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 34/37 = 91.9% | 37/37 = 100.0% | 37/37 = 100.0% | 37/37 = 100.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 33/35 = 94.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Drive Anywhere
Package__state28__state18`

### Product 356

**Selected features:** selected = {5Y, AD, BWI, C, CC, Ca, DAP, MC, MX, PD, S, S6, T, TP, TuW, UR, YS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 33/38 = 86.8% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 47/59 = 79.7% | 59/59 = 100.0% | 59/59 = 100.0% | 52/59 = 88.1% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 38/54 = 70.4% | 53/54 = 98.1% | 54/54 = 100.0% | 44/54 = 81.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state28`

### Product 357

**Selected features:** selected = {ABI, ACT, AD, CC, CW, Ca, DAP, DBM, L, MX, PD, S, S6, T, TP, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 47/55 = 85.5% | 55/55 = 100.0% | 55/55 = 100.0% | 53/55 = 96.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 39/50 = 78.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 358

**Selected features:** selected = {ABI, ACT, AD, AWL, CC, CW, Ca, F, MC, MX, Mo, PD, S, S6, SG, T, TP, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 39/43 = 90.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 58/81 = 71.6% | 81/81 = 100.0% | 81/81 = 100.0% | 75/81 = 92.6% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 66/70 = 94.3% | 70/70 = 100.0% | 57/70 = 81.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 359

**Selected features:** selected = {ABI, ACT, AD, AWD, AWL, CW, Ca, DAP, FSD, L, MX, Mo, S, S5, SG, T, TP, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 58/81 = 71.6% | 81/81 = 100.0% | 81/81 = 100.0% | 73/81 = 90.1% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 66/70 = 94.3% | 70/70 = 100.0% | 59/70 = 84.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 360

**Selected features:** selected = {AD, BWI, CCT, Ca, DBM, GW, HC, L, LRAWD, MC, MY, S, S5, SS, T, WS}

**Repaired FTS:** 29 states, 39 transitions (39 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 39 | 0/39 = 0.0% | 0/39 = 0.0% | 30/39 = 76.9% | 39/39 = 100.0% | 39/39 = 100.0% | 32/39 = 82.1% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 51/73 = 69.9% | 73/73 = 100.0% | 73/73 = 100.0% | 54/73 = 74.0% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 73 | 11/73 = 15.1% | 0/62 = 0.0% | 34/62 = 54.8% | 59/62 = 95.2% | 62/62 = 100.0% | 42/62 = 67.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state27__Sunshade__state22__state26`
- `TDE__state27__Sunshade__state22__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Center 
Console 
Trays__state25__state26`
- `TDE__state26__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 361

**Selected features:** selected = {AD, AW, BWI, Ca, F, FSD, HC, MC, MS, Mo, PD, RR, S, S5, SG, T, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 33/41 = 80.5% | 41/41 = 100.0% | 41/41 = 100.0% | 35/41 = 85.4% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 60/77 = 77.9% | 77/77 = 100.0% | 77/77 = 100.0% | 60/77 = 77.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 40/66 = 60.6% | 63/66 = 95.5% | 66/66 = 100.0% | 48/66 = 72.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state18__Roof 
Rack__state23__state27`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state27__Roof 
Rack__state23__state26`

### Product 362

**Selected features:** selected = {AD, BWI, CCT, Ca, F, FSD, MY, Mo, PAWD, PWh, S, S5, T, TP, UW, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 32/34 = 94.1% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 42/45 = 93.3% | 45/45 = 100.0% | 45/45 = 100.0% | 44/45 = 97.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Center 
Console 
Trays__state25__state18`

### Product 363

**Selected features:** selected = {ACT, AD, AWD, BWI, CC, Ca, HC, L, MX, Mo, QS, S, S6, T, TP, TuW, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 35/38 = 92.1% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 50/59 = 84.7% | 59/59 = 100.0% | 59/59 = 100.0% | 55/59 = 93.2% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 40/54 = 74.1% | 53/54 = 98.1% | 54/54 = 100.0% | 48/54 = 88.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`

### Product 364

**Selected features:** selected = {AD, AW, AWD, AWL, BWI, Ca, DAP, F, FSD, HC, MC, MS, Mo, PWh, S, S5, SS, T, WS}

**Repaired FTS:** 32 states, 54 transitions (54 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 54 | 0/54 = 0.0% | 0/54 = 0.0% | 35/54 = 64.8% | 54/54 = 100.0% | 54/54 = 100.0% | 43/54 = 79.6% |
| ActionExchange | 153 | 0/153 = 0.0% | 0/153 = 0.0% | 82/153 = 53.6% | 153/153 = 100.0% | 153/153 = 100.0% | 124/153 = 81.0% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 30/31 = 96.8% |
| TransitionDestinationExchange | 153 | 36/153 = 23.5% | 0/117 = 0.0% | 49/117 = 41.9% | 102/117 = 87.2% | 117/117 = 100.0% | 86/117 = 73.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (36):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state27__Sunshade__state22__state26`
- `TDE__state18__Sunshade__state22__state27`
- `TDE__state18__Sunshade__state22__state26`
- `TDE__state18__Sunshade__state22__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state16__Sunshade__state22__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state28__Sunshade__state22__state27`
- `TDE__state28__Sunshade__state22__state26`

### Product 365

**Selected features:** selected = {AD, BWI, CW, Ca, DAP, HC, L, MC, MX, PD, S, S6, SG, T, TP, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 45/55 = 81.8% | 55/55 = 100.0% | 55/55 = 100.0% | 53/55 = 96.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state28`

### Product 366

**Selected features:** selected = {ABI, AD, AW, AWL, Ca, FSD, L, MC, MS, Mo, PD, S, S5, SS, T, UR, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 36/41 = 87.8% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 67/77 = 87.0% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 62/66 = 93.9% | 66/66 = 100.0% | 51/66 = 77.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Sunshade__state22__state26`
- `TDE__state17__Sunshade__state22__state18`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 367

**Selected features:** selected = {5Y, AD, AWL, C, CI, Ca, MC, MS, PD, RR, S, S5, SB, T, TeW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 42/55 = 76.4% | 55/55 = 100.0% | 55/55 = 100.0% | 45/55 = 81.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 38/50 = 76.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state26__Roof 
Rack__state23__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state24`

### Product 368

**Selected features:** selected = {AD, AW, AWL, BWI, Ca, F, FSD, HC, MC, MS, PD, QS, RR, S, S5, SS, T, WS}

**Repaired FTS:** 31 states, 53 transitions (53 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 53 | 0/53 = 0.0% | 0/53 = 0.0% | 33/53 = 62.3% | 53/53 = 100.0% | 53/53 = 100.0% | 45/53 = 84.9% |
| ActionExchange | 155 | 0/155 = 0.0% | 0/155 = 0.0% | 82/155 = 52.9% | 155/155 = 100.0% | 155/155 = 100.0% | 131/155 = 84.5% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 155 | 27/155 = 17.4% | 0/128 = 0.0% | 46/128 = 35.9% | 100/128 = 78.1% | 128/128 = 100.0% | 84/128 = 65.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (27):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state27__Sunshade__state22__state26`
- `TDE__state18__Sunshade__state22__state27`
- `TDE__state18__Sunshade__state22__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state22`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Roof 
Rack__state23__state27`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state27__Roof 
Rack__state23__state26`

### Product 369

**Selected features:** selected = {5Y, ABI, ACT, AD, AWD, C, Ca, DAP, FSD, HC, MX, QS, S, S5, T, TP, TuW, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 37/43 = 86.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 58/81 = 71.6% | 81/81 = 100.0% | 81/81 = 100.0% | 68/81 = 84.0% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 66/70 = 94.3% | 70/70 = 100.0% | 56/70 = 80.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 370

**Selected features:** selected = {AD, AWD, BWI, CW, Ca, F, FSD, MX, Mo, S, S7, SG, T, TP, YS}

**Repaired FTS:** 28 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 29/31 = 93.5% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 33/37 = 89.2% | 37/37 = 100.0% | 37/37 = 100.0% | 36/37 = 97.3% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 26/27 = 96.3% |
| TransitionDestinationExchange | 37 | 1/37 = 2.7% | 0/36 = 0.0% | 29/36 = 80.6% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 371

**Selected features:** selected = {5Y, ACT, AD, C, CC, CI, Ca, DAP, FSD, MX, PD, S, S6, SB, T, TP, TuW, WS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 60/81 = 74.1% | 81/81 = 100.0% | 81/81 = 100.0% | 72/81 = 88.9% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 47/70 = 67.1% | 66/70 = 94.3% | 70/70 = 100.0% | 57/70 = 81.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 372

**Selected features:** selected = {ACT, AD, AWD, AWL, CC, CI, Ca, DAP, DBM, FSD, L, MC, MX, Mo, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 33 states, 56 transitions (56 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 35/56 = 62.5% | 56/56 = 100.0% | 56/56 = 100.0% | 47/56 = 83.9% |
| ActionExchange | 157 | 0/157 = 0.0% | 0/157 = 0.0% | 80/157 = 51.0% | 157/157 = 100.0% | 157/157 = 100.0% | 132/157 = 84.1% |
| StateMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% | 32/32 = 100.0% |
| TransitionDestinationExchange | 157 | 36/157 = 22.9% | 0/121 = 0.0% | 53/121 = 43.8% | 108/121 = 89.3% | 121/121 = 100.0% | 86/121 = 71.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (36):

- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`

### Product 373

**Selected features:** selected = {5Y, ABI, ACT, AD, C, CC, Ca, DAP, MX, PD, S, S6, SG, T, TP, TuW, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 35/38 = 92.1% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 51/59 = 86.4% | 59/59 = 100.0% | 59/59 = 100.0% | 56/59 = 94.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 43/54 = 79.6% | 53/54 = 98.1% | 54/54 = 100.0% | 47/54 = 87.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 374

**Selected features:** selected = {AD, BWI, CCT, Ca, F, MC, MY, PAWD, S, S5, SG, SS, T, TP, UW, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 32/36 = 88.9% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 48/55 = 87.3% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 39/50 = 78.0% | 49/50 = 98.0% | 50/50 = 100.0% | 46/50 = 92.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state26__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state26`

### Product 375

**Selected features:** selected = {5Y, AD, BWI, C, Ca, DAP, DBM, HC, MC, MX, PD, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 36/38 = 94.7% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 49/59 = 83.1% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 50/54 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state28`

### Product 376

**Selected features:** selected = {ABI, ACT, AD, AWD, CC, Ca, F, MX, Mo, S, S6, SG, T, TP, TuW, WS}

**Repaired FTS:** 29 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 31/34 = 91.2% | 34/34 = 100.0% | 34/34 = 100.0% | 32/34 = 94.1% |
| ActionExchange | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 42/45 = 93.3% | 45/45 = 100.0% | 45/45 = 100.0% | 44/45 = 97.8% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 27/28 = 96.4% |
| TransitionDestinationExchange | 45 | 2/45 = 4.4% | 0/43 = 0.0% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 40/43 = 93.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`

### Product 377

**Selected features:** selected = {AD, AWD, AWL, CI, CW, Ca, DAP, F, MC, MX, S, S6, SG, T, TP, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 53/55 = 96.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 378

**Selected features:** selected = {ABI, AD, AWD, CC, CW, Ca, F, MX, S, S7, SG, T, TP, WS}

**Repaired FTS:** 27 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 28/29 = 96.6% | 29/29 = 100.0% | 29/29 = 100.0% | 27/29 = 93.1% |
| ActionExchange | 33 | 0/33 = 0.0% | 0/33 = 0.0% | 32/33 = 97.0% | 33/33 = 100.0% | 33/33 = 100.0% | 32/33 = 97.0% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 33 | 1/33 = 3.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 29/32 = 90.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state41__ZIP 
Code__state1__state2`

### Product 379

**Selected features:** selected = {AD, AWL, BWI, Ca, HC, L, MC, MY, Mo, PAWD, S, S5, SB, T, TP, UW, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 36/38 = 94.7% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 47/59 = 79.7% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 52/54 = 96.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 380

**Selected features:** selected = {ABI, AD, Ca, F, FSD, MS, PD, RR, S, S5, SG, T, TeW, YS}

**Repaired FTS:** 27 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 27/30 = 90.0% |
| ActionExchange | 37 | 0/37 = 0.0% | 0/37 = 0.0% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 33/37 = 89.2% |
| StateMissing | 26 | 0/26 = 0.0% | 0/26 = 0.0% | 26/26 = 100.0% | 26/26 = 100.0% | 26/26 = 100.0% | 25/26 = 96.2% |
| TransitionDestinationExchange | 37 | 2/37 = 5.4% | 0/35 = 0.0% | 27/35 = 77.1% | 35/35 = 100.0% | 35/35 = 100.0% | 29/35 = 82.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state17__Roof 
Rack__state23__state18`
- `TDE__state41__ZIP 
Code__state1__state2`

### Product 381

**Selected features:** selected = {AD, AWD, CC, CI, Ca, DAP, F, FSD, HC, MC, MX, S, S7, SG, T, TP, TuW, WS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 34/47 = 72.3% | 47/47 = 100.0% | 47/47 = 100.0% | 41/47 = 87.2% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 70/109 = 64.2% | 109/109 = 100.0% | 109/109 = 100.0% | 95/109 = 87.2% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 43/88 = 48.9% | 79/88 = 89.8% | 88/88 = 100.0% | 66/88 = 75.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state18`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state18__Home 
Charger__state27__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 382

**Selected features:** selected = {AD, AWD, AWL, C, CI, CW, Ca, DAP, MC, MX, PWh, S, S6, T, TP, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 53/55 = 96.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state28`

### Product 383

**Selected features:** selected = {AD, AWD, AWL, CC, CI, Ca, DAP, F, FSD, MX, Mo, QS, S, S7, T, TP, TuW, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 33/43 = 76.7% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 55/81 = 67.9% | 81/81 = 100.0% | 81/81 = 100.0% | 69/81 = 85.2% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 42/70 = 60.0% | 67/70 = 95.7% | 70/70 = 100.0% | 55/70 = 78.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 384

**Selected features:** selected = {ABI, AD, CCT, Ca, HC, L, LRAWD, M3, PW, PWh, RR, S, S5, T, WS}

**Repaired FTS:** 28 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 0/34 = 0.0% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 31/34 = 91.2% |
| ActionExchange | 51 | 0/51 = 0.0% | 0/51 = 0.0% | 44/51 = 86.3% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 51 | 5/51 = 9.8% | 0/46 = 0.0% | 32/46 = 69.6% | 45/46 = 97.8% | 46/46 = 100.0% | 38/46 = 82.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state27__Roof 
Rack__state23__state25`

### Product 385

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, CC, CW, Ca, F, MX, Mo, QS, S, S6, T, TP, WS}

**Repaired FTS:** 30 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 0/38 = 0.0% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 33/38 = 86.8% |
| ActionExchange | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 49/59 = 83.1% | 59/59 = 100.0% | 59/59 = 100.0% | 50/59 = 84.7% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 59 | 5/59 = 8.5% | 0/54 = 0.0% | 39/54 = 72.2% | 53/54 = 98.1% | 54/54 = 100.0% | 42/54 = 77.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 386

**Selected features:** selected = {AD, AWL, CI, Ca, DAP, F, HC, MS, Mo, PD, PWh, S, S5, T, TeW, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 35/36 = 97.2% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 42/55 = 76.4% | 55/55 = 100.0% | 55/55 = 100.0% | 54/55 = 98.2% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 45/50 = 90.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state28`

### Product 387

**Selected features:** selected = {AD, AWL, BWI, CCT, Ca, F, FSD, HC, M3, MC, Mo, PW, PWh, RWD, S, S5, T, WS}

**Repaired FTS:** 31 states, 47 transitions (47 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 47 | 0/47 = 0.0% | 0/47 = 0.0% | 34/47 = 72.3% | 47/47 = 100.0% | 47/47 = 100.0% | 39/47 = 83.0% |
| ActionExchange | 109 | 0/109 = 0.0% | 0/109 = 0.0% | 70/109 = 64.2% | 109/109 = 100.0% | 109/109 = 100.0% | 81/109 = 74.3% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 29/30 = 96.7% |
| TransitionDestinationExchange | 109 | 21/109 = 19.3% | 0/88 = 0.0% | 44/88 = 50.0% | 80/88 = 90.9% | 88/88 = 100.0% | 59/88 = 67.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state27__Center 
Console 
Trays__state25__state26`
- `TDE__state18__Center 
Console 
Trays__state25__state27`
- `TDE__state18__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Center 
Console 
Trays__state25__state18`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state25`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`

### Product 388

**Selected features:** selected = {AD, AWD, CC, CI, CW, Ca, FSD, L, MX, S, S5, SG, T, TP, WS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 30/32 = 93.8% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 40/41 = 97.6% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 34/39 = 87.2% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state16__Car Cover__state21__state18`

### Product 389

**Selected features:** selected = {AD, AWL, BWI, CC, CW, Ca, FSD, L, MX, PD, S, S6, SG, T, TP, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 29/36 = 80.6% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 50/55 = 90.9% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 42/50 = 84.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`

### Product 390

**Selected features:** selected = {ABI, AD, AWL, Ca, DBM, HC, L, M3, MC, Mo, NW, RWD, S, S5, T, WS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 45/50 = 90.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 391

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, CC, Ca, DBM, F, FSD, MX, S, S7, T, TP, TuW, YS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 31/41 = 75.6% | 41/41 = 100.0% | 41/41 = 100.0% | 37/41 = 90.2% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 52/77 = 67.5% | 77/77 = 100.0% | 77/77 = 100.0% | 70/77 = 90.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 39/66 = 59.1% | 63/66 = 95.5% | 66/66 = 100.0% | 53/66 = 80.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state17__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`

### Product 392

**Selected features:** selected = {ACT, AD, AWD, AWL, CI, CW, Ca, DAP, FSD, L, MX, S, S6, SB, T, TP, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 38/41 = 92.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 54/77 = 70.1% | 77/77 = 100.0% | 77/77 = 100.0% | 70/77 = 90.9% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 62/66 = 93.9% | 66/66 = 100.0% | 59/66 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state18__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Drive Anywhere
Package__state28__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state18`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`

### Product 393

**Selected features:** selected = {ACT, AD, AWD, AWL, BWI, C, CC, CW, Ca, DAP, HC, MC, MX, S, S6, T, TP, UR, WS}

**Repaired FTS:** 32 states, 54 transitions (54 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 54 | 0/54 = 0.0% | 0/54 = 0.0% | 34/54 = 63.0% | 54/54 = 100.0% | 54/54 = 100.0% | 44/54 = 81.5% |
| ActionExchange | 153 | 0/153 = 0.0% | 0/153 = 0.0% | 79/153 = 51.6% | 153/153 = 100.0% | 153/153 = 100.0% | 114/153 = 74.5% |
| StateMissing | 31 | 0/31 = 0.0% | 0/31 = 0.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| TransitionDestinationExchange | 153 | 36/153 = 23.5% | 0/117 = 0.0% | 49/117 = 41.9% | 104/117 = 88.9% | 117/117 = 100.0% | 81/117 = 69.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (36):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Car Cover__state21__state27`
- `TDE__state28__Car Cover__state21__state26`
- `TDE__state16__Home 
Charger__state27__state28`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state26__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state21__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state28__Mobile 
Charger__state26__state27`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state26__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state21`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state28__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state27__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Car Cover__state21__state27`
- `TDE__state16__Car Cover__state21__state26`
- `TDE__state16__Car Cover__state21__state28`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state28`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state21`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state26`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state24`
- `TDE__state16__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state27__Car Cover__state21__state26`

### Product 394

**Selected features:** selected = {ABI, AD, AWL, C, Ca, FSD, HC, LRAWD, M3, MC, NW, PWh, RR, S, S5, T, WS}

**Repaired FTS:** 30 states, 45 transitions (45 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 45 | 0/45 = 0.0% | 0/45 = 0.0% | 31/45 = 68.9% | 45/45 = 100.0% | 45/45 = 100.0% | 39/45 = 86.7% |
| ActionExchange | 105 | 0/105 = 0.0% | 0/105 = 0.0% | 61/105 = 58.1% | 105/105 = 100.0% | 105/105 = 100.0% | 90/105 = 85.7% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| TransitionDestinationExchange | 105 | 21/105 = 20.0% | 0/84 = 0.0% | 40/84 = 47.6% | 76/84 = 90.5% | 84/84 = 100.0% | 62/84 = 73.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state16__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state16__All-Weather 
Interior 
Liners__state24__state18`
- `TDE__state26__Roof 
Rack__state23__state24`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state16__Home 
Charger__state27__state18`
- `TDE__state16__Roof 
Rack__state23__state27`
- `TDE__state16__Roof 
Rack__state23__state26`
- `TDE__state16__Roof 
Rack__state23__state24`
- `TDE__state16__Roof 
Rack__state23__state18`
- `TDE__state27__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state27`
- `TDE__state18__All-Weather 
Interior 
Liners__state24__state26`
- `TDE__state18__Roof 
Rack__state23__state27`
- `TDE__state18__Roof 
Rack__state23__state26`
- `TDE__state18__Roof 
Rack__state23__state24`
- `TDE__state16__Mobile 
Charger__state26__state27`
- `TDE__state16__Mobile 
Charger__state26__state18`
- `TDE__state27__Roof 
Rack__state23__state26`
- `TDE__state27__Roof 
Rack__state23__state24`

### Product 395

**Selected features:** selected = {AD, AWD, BWI, C, CW, Ca, DAP, FSD, MC, MX, PWh, S, S6, T, TP, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 29/36 = 80.6% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 52/55 = 94.5% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 46/50 = 92.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state28`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Mobile 
Charger__state26__state28`

### Product 396

**Selected features:** selected = {AD, AWD, BWI, C, CC, CW, Ca, DAP, FSD, MX, S, S7, T, TP, UR, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 34/36 = 94.4% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 43/55 = 78.2% | 55/55 = 100.0% | 55/55 = 100.0% | 53/55 = 96.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 34/50 = 68.0% | 49/50 = 98.0% | 50/50 = 100.0% | 45/50 = 90.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state18__Car Cover__state21__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Drive Anywhere
Package__state28__state18`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state17__Car Cover__state21__state28`

### Product 397

**Selected features:** selected = {ACT, AD, C, CI, CW, Ca, DAP, HC, MX, PD, QS, S, S6, T, TP, YS}

**Repaired FTS:** 29 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 0/36 = 0.0% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 33/36 = 91.7% |
| ActionExchange | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 45/55 = 81.8% | 55/55 = 100.0% | 55/55 = 100.0% | 53/55 = 96.4% |
| StateMissing | 28 | 0/28 = 0.0% | 0/28 = 0.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% | 28/28 = 100.0% |
| TransitionDestinationExchange | 55 | 5/55 = 9.1% | 0/50 = 0.0% | 35/50 = 70.0% | 49/50 = 98.0% | 50/50 = 100.0% | 43/50 = 86.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state17__Home 
Charger__state27__state28`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state27`
- `TDE__state17__Air Compressor+
Tire Repair 
Kit__state20__state28`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state28__Air Compressor+
Tire Repair 
Kit__state20__state27`

### Product 398

**Selected features:** selected = {ABI, AD, CCT, Ca, HC, L, MC, MY, PAWD, S, S5, SB, SS, T, TP, UW, WS}

**Repaired FTS:** 30 states, 41 transitions (41 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 32/41 = 78.0% | 41/41 = 100.0% | 41/41 = 100.0% | 34/41 = 82.9% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 55/77 = 71.4% | 77/77 = 100.0% | 77/77 = 100.0% | 63/77 = 81.8% |
| StateMissing | 29 | 0/29 = 0.0% | 0/29 = 0.0% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% | 28/29 = 96.6% |
| TransitionDestinationExchange | 77 | 11/77 = 14.3% | 0/66 = 0.0% | 38/66 = 57.6% | 63/66 = 95.5% | 66/66 = 100.0% | 48/66 = 72.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state27__Sunshade__state22__state26`
- `TDE__state27__Sunshade__state22__state25`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state27__Center 
Console 
Trays__state25__state26`
- `TDE__state26__Sunshade__state22__state25`
- `TDE__state16__Sunshade__state22__state27`
- `TDE__state16__Sunshade__state22__state26`
- `TDE__state16__Sunshade__state22__state25`
- `TDE__state16__Center 
Console 
Trays__state25__state27`
- `TDE__state16__Center 
Console 
Trays__state25__state26`
- `TDE__state16__Mobile 
Charger__state26__state27`

### Product 399

**Selected features:** selected = {5Y, AD, AWD, BWI, C, CC, CW, Ca, FSD, HC, MC, MX, S, S7, T, TP, UR, YS}

**Repaired FTS:** 31 states, 43 transitions (43 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 43 | 0/43 = 0.0% | 0/43 = 0.0% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 61/81 = 75.3% | 81/81 = 100.0% | 81/81 = 100.0% | 72/81 = 88.9% |
| StateMissing | 30 | 0/30 = 0.0% | 0/30 = 0.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% | 30/30 = 100.0% |
| TransitionDestinationExchange | 81 | 11/81 = 13.6% | 0/70 = 0.0% | 43/70 = 61.4% | 67/70 = 95.7% | 70/70 = 100.0% | 52/70 = 74.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state17__Home 
Charger__state27__state18`
- `TDE__state18__Car Cover__state21__state27`
- `TDE__state18__Car Cover__state21__state26`
- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state18__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state27`
- `TDE__state17__Mobile 
Charger__state26__state18`
- `TDE__state17__Car Cover__state21__state27`
- `TDE__state17__Car Cover__state21__state26`
- `TDE__state17__Car Cover__state21__state18`
- `TDE__state27__Car Cover__state21__state26`

### Product 400

**Selected features:** selected = {AD, BWI, Ca, DBM, F, HC, MC, MX, PD, S, S6, T, TP, TuW, YS}

**Repaired FTS:** 28 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 0/32 = 0.0% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 31/32 = 96.9% |
| ActionExchange | 41 | 0/41 = 0.0% | 0/41 = 0.0% | 38/41 = 92.7% | 41/41 = 100.0% | 41/41 = 100.0% | 41/41 = 100.0% |
| StateMissing | 27 | 0/27 = 0.0% | 0/27 = 0.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% | 27/27 = 100.0% |
| TransitionDestinationExchange | 41 | 2/41 = 4.9% | 0/39 = 0.0% | 32/39 = 82.1% | 39/39 = 100.0% | 39/39 = 100.0% | 37/39 = 94.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state41__ZIP 
Code__state1__state2`
- `TDE__state17__Mobile 
Charger__state26__state27`
---

## Tesla summary (aggregate over 400 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 0 test case(s) generated once for the SPL.

**Equivalent-mutant treatment:** Inozemtseva &amp; Holmes (2014) — mutant not killed by ANY of the five suites (family + product state + product transition + product pair + random) is conservatively classified equivalent and EXCLUDED from the score denominator. Scores below are **killed / (total &minus; equivalent) = adjusted%**.

| Operator | Total mutants | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15370 | 0/15370 = 0.0% | 0/15370 = 0.0% | 12407/15370 = 80.7% | 15370/15370 = 100.0% | 15370/15370 = 100.0% | 13827/15370 = 90.0% |
| ActionExchange | 27308 | 0/27308 = 0.0% | 0/27308 = 0.0% | 19876/27308 = 72.8% | 27308/27308 = 100.0% | 27308/27308 = 100.0% | 24168/27308 = 88.5% |
| StateMissing | 11360 | 0/11360 = 0.0% | 0/11360 = 0.0% | 11360/11360 = 100.0% | 11360/11360 = 100.0% | 11360/11360 = 100.0% | 11270/11360 = 99.2% |
| TransitionDestinationExchange | 27308 | 3583/27308 = 13.1% | 0/23725 = 0.0% | 14916/23725 = 62.9% | 22500/23725 = 94.8% | 23725/23725 = 100.0% | 19205/23725 = 80.9% |

Total products: 400.
