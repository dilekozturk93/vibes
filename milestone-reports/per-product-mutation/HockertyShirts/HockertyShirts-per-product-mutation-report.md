# Per-Product Mutation Report — HockertyShirts

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

**Selected features:** selected = {AC, ASta, Bd, CCh, Ches, DBu, DSt, Gar, HS, In, LA, Me, NAG, Ord, PO, R1b, Rr, SS, Shi, Ss, Sta, UnT}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 59/67 = 88.1% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 82/96 = 85.4% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 77/93 = 82.8% | 92/93 = 98.9% | 93/93 = 100.0% | 76/93 = 81.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 2

**Selected features:** selected = {AA, ACh, AS, CSta, Cuf, DBu, Go, HS, In, Me, NAG, NK, Ord, PO, Shi, Ss, Sta, Tbc, Th, UnT}

**Repaired FTS:** 56 states, 60 transitions (60 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 60 | 0/60 = 0.0% | 0/60 = 0.0% | 58/60 = 96.7% | 60/60 = 100.0% | 60/60 = 100.0% | 58/60 = 96.7% |
| ActionExchange | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 70/72 = 97.2% | 72/72 = 100.0% | 72/72 = 100.0% | 70/72 = 97.2% |
| StateMissing | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 55/55 = 100.0% | 55/55 = 100.0% | 55/55 = 100.0% | 54/55 = 98.2% |
| TransitionDestinationExchange | 72 | 1/72 = 1.4% | 0/71 = 0.0% | 66/71 = 93.0% | 71/71 = 100.0% | 71/71 = 100.0% | 68/71 = 95.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 3

**Selected features:** selected = {AS, Bd, CBu, Ches, DCu, DSt, HS, In, MG, Mcm, Me, Np, Ord, PO, RB, SCh, SSta, Shi, Ss, TA, Tbc, Tu, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 4

**Selected features:** selected = {AS, ASta, Bd, CBu, Cop, Cuf, DC, HS, In, LA, Ls, Me, NAG, Np, OCu, Ord, PO, SCh, Sc2b, Shi, To, Tu, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 5

**Selected features:** selected = {AA, ASta, BAB, Ben, BluB, CBu, Ches, HS, IC, ICu, In, Ls, MB, Me, Ord, PO, Rc, SCh, Sc1b, Shi, StS, Sta, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 6

**Selected features:** selected = {AC, BAB, CBu, Ches, Cop, HS, Har, In, LA, Ls, Me, Np, Ord, PO, R1b, SCh, SSta, Shi, StS, Suc, Tu, WB}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 7

**Selected features:** selected = {ACu, ASt, CBu, CCh, CSta, Cha, Cuf, HS, In, Me, Ord, PO, RB, Rr, Sc2b, Shi, Ss, StS, Sta, TA, Tu, Wc, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 88/94 = 93.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 8

**Selected features:** selected = {AA, ACh, ACu, ASt, BAB, Bd, BluB, CBu, CSta, Cuf, DC, HS, In, LB, Marv, Me, Np, Ord, PO, SS, Sc1b, Shi, Ss, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 9

**Selected features:** selected = {AA, AC, ACu, AS, BAB, CSta, Cuf, DBu, Ds, Go, HS, In, Kayc, Me, NK, OCu, Ord, PO, SCh, Shi, Ss, Sta, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 67/72 = 93.1% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 109/119 = 91.6% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 95/111 = 85.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 10

**Selected features:** selected = {ACh, ACu, AS, BAB, Bla, Cuf, DBu, DSt, HS, IC, In, Kc, LA, Me, Ord, PO, Reg, SSta, Sc1b, Shi, Ss, Sta, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 99/119 = 83.2% | 119/119 = 100.0% | 119/119 = 100.0% | 107/119 = 89.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 93/111 = 83.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 11

**Selected features:** selected = {AA, AC, ACu, ASta, BAB, Bla, BluB, CBu, CCh, Cuf, DSt, HS, In, Kay, Ls, Me, Ord, PO, Sc1b, Shi, StS, Sta, Suc, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 12

**Selected features:** selected = {ASt, CBu, Cuf, DB, DCu, HS, IC, In, Kc, Me, Np, Ord, PO, R1b, SCh, SS, SSta, Shi, Ss, TA, Tr, Tu, WB, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 13

**Selected features:** selected = {ACh, ASta, Ba, Ches, DBu, DCu, DG, DSt, Ds, HS, IC, In, LA, Me, Np, ODI, Ord, PO, SS, Shi, Ss, UnT, Wc}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 68/72 = 94.4% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 109/119 = 91.6% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 98/111 = 88.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 14

**Selected features:** selected = {ACh, ACu, BAB, Ches, DBu, DSt, Go, HS, IC, In, Ja, Kc, LA, Me, Ord, PO, SS, SSta, Sc2b, Shi, Ss, Sta, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 99/119 = 83.2% | 119/119 = 100.0% | 119/119 = 100.0% | 107/119 = 89.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 93/111 = 83.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 15

**Selected features:** selected = {AA, ACu, AS, ASt, ASta, Bd, CBu, CCh, Ches, Ev, HS, In, LC, Me, NAG, Np, Ord, PO, Shi, Ss, Tbc, Tu, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 88/94 = 93.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 16

**Selected features:** selected = {ACh, CBu, Ches, DC, DCu, HS, In, Ls, Me, NAG, Np, Ord, PO, Rr, SSta, Sc1b, Shi, StS, Suc, TA, Tu, WB, Ya}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 89/97 = 91.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 82/94 = 87.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 17

**Selected features:** selected = {AS, ASt, Alt, CCh, CSta, Ches, DBu, Ds, HS, IC, In, Ls, MG, Me, ODI, Ord, PO, Shi, Sta, Suc, TA, Tu}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 65/67 = 97.0% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 92/96 = 95.8% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 88/93 = 94.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 18

**Selected features:** selected = {AC, ASt, Bd, BluB, CBu, CCh, Cha, Cuf, Go, HS, In, LA, Me, NAG, Np, Ord, PO, SSta, Shi, Ss, StS, Tbc, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 97/97 = 100.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 91/94 = 96.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 19

**Selected features:** selected = {ACh, Bd, CBu, CSta, Ches, Cop, DSt, HS, IC, In, LA, Ls, Me, NAG, Np, Ord, PO, RB, SS, Set, Shi, Tbc, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 20

**Selected features:** selected = {AC, ACh, AS, CBu, Cal, Ches, DSt, HS, In, MB, Me, Ord, PO, RB, SSta, Shi, Ss, Sta, TA, Tbc, Tu, Wc, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 21

**Selected features:** selected = {AA, ASt, BAB, CBu, CSta, Ches, DC, HS, In, LG, Ls, Mcc, Me, NK, Ord, PO, RB, SCh, SS, Sc1b, Shi, Sta, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 86/94 = 91.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 22

**Selected features:** selected = {AA, ASt, ASta, BB, CBu, CCh, Ches, Cop, Ds, HS, IC, In, Le, Ls, Me, Np, Ord, PO, Shi, StS, Suc, UnT, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 23

**Selected features:** selected = {ASt, ASta, BAB, Bla, CBu, Cuf, DC, DCu, HS, In, Ls, Me, Np, Ord, PO, Pr, RB, Rc, SCh, SS, Sc1b, Shi, TA, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 24

**Selected features:** selected = {Arm, CBu, CCh, Cuf, DB, HS, IC, In, Ls, Me, NAG, Ord, PO, R1b, RB, SSta, Shi, StS, Sta, TA, UnT, Wc}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 80/82 = 97.6% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 25

**Selected features:** selected = {AC, Ben, BluB, CBu, CCh, Cuf, DSt, Ds, HS, In, LA, Me, NAG, Np, Ord, PO, Rc, Rr, SS, SSta, Shi, Ss, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 26

**Selected features:** selected = {ASt, ASta, CBu, Cuf, HS, In, Kc, MB, Me, NAG, Ord, PO, RB, Ry, SCh, Sc2b, Shi, Ss, StS, Sta, TA, Tu}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 62/64 = 96.9% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 80/82 = 97.6% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 78/81 = 96.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 27

**Selected features:** selected = {ACh, AS, ASt, CBu, CSta, Cuf, HS, IC, In, LA, LG, Ls, Me, NAG, Np, Ord, PO, R1b, RB, Sh, Shi, Suc, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 28

**Selected features:** selected = {ASt, ASta, BAB, Ches, DBu, HS, IC, In, LA, MB, Mc, Me, Np, Ord, PO, SCh, Shi, Ss, StS, Tbc, Tu, Wc}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 65/67 = 97.0% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 92/96 = 95.8% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 88/93 = 94.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 29

**Selected features:** selected = {ACh, AS, ASt, ASta, BAB, BB, CBu, Ches, Di, Go, HS, IC, In, Kc, Ls, Me, Np, Ord, PO, Sc2b, Shi, TA, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 30

**Selected features:** selected = {AA, ACh, ACu, Ches, Cu, DBu, DC, DG, HS, In, Kc, Me, Ord, PO, SSta, Sc2b, Shi, Ss, StS, Sta, Tu, YD}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 61/67 = 91.0% | 67/67 = 100.0% | 67/67 = 100.0% | 65/67 = 97.0% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 95/96 = 99.0% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 76/93 = 81.7% | 92/93 = 98.9% | 93/93 = 100.0% | 88/93 = 94.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 31

**Selected features:** selected = {AC, ASt, BAB, BB, By, CBu, Ches, Ds, HS, ICu, In, Ls, Me, Ord, PO, Pu, Rc, SCh, SS, SSta, Shi, Sta, TA, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 32

**Selected features:** selected = {AC, ACh, AS, Bd, CSta, Ches, DBu, HS, ICu, In, LA, Ls, Me, NAG, Ord, PO, R1b, Rr, Shi, Sta, Tu, Val}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 62/67 = 92.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 89/96 = 92.7% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 77/93 = 82.8% | 92/93 = 98.9% | 93/93 = 100.0% | 83/93 = 89.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 33

**Selected features:** selected = {ACh, ASta, Cuf, DBu, Et, HS, IC, ICu, In, LG, Ls, Me, NK, Np, ODI, Ord, PO, SS, Sc1b, Shi, TA, Tu}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 59/67 = 88.1% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 82/96 = 85.4% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 74/93 = 79.6% | 92/93 = 98.9% | 93/93 = 100.0% | 76/93 = 81.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 34

**Selected features:** selected = {AC, ACu, ASta, BB, Bla, Bri, CBu, Cuf, Ds, HS, In, Ls, Me, NAG, Np, OCu, Ord, PO, SCh, Shi, StS, Suc, TA, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 103/112 = 92.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 35

**Selected features:** selected = {ACu, AS, ASt, BB, CBu, Cuf, DC, HS, Hu, In, LP, Me, Ord, PO, SCh, SSta, Sc2b, Shi, Ss, Sta, TA, UnT, Wc, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 36

**Selected features:** selected = {AA, ACh, ASta, Bla, CBu, Ches, Gar, HS, In, Me, ODI, Ord, PO, Rc, SS, Sc2b, Shi, Ss, Sta, UnT, WB}

**Repaired FTS:** 57 states, 61 transitions (61 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 61 | 0/61 = 0.0% | 0/61 = 0.0% | 59/61 = 96.7% | 61/61 = 100.0% | 61/61 = 100.0% | 58/61 = 95.1% |
| ActionExchange | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 71/73 = 97.3% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 55/56 = 98.2% |
| TransitionDestinationExchange | 73 | 1/73 = 1.4% | 0/72 = 0.0% | 67/72 = 93.1% | 72/72 = 100.0% | 72/72 = 100.0% | 67/72 = 93.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 37

**Selected features:** selected = {AA, ACh, BAB, Cuf, DBu, DCu, HS, In, Lev, Ls, MG, Me, Ord, PO, Rc, SS, SSta, Shi, Sta, Tbc, UnT}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 56/63 = 88.9% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 72/81 = 88.9% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 52/56 = 92.9% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 71/80 = 88.8% | 80/80 = 100.0% | 80/80 = 100.0% | 68/80 = 85.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 38

**Selected features:** selected = {AA, ACu, AS, ASta, Bla, BluB, CBu, CCh, Ches, HS, IC, In, Me, NAG, Ord, PO, Ra, Rc, Shi, Ss, Sta, Tbc, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 94/97 = 96.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 39

**Selected features:** selected = {AA, ASta, CBu, CCh, Cuf, DC, DSt, Go, HS, ICu, In, Ls, Maj, Me, NK, Ord, PO, RB, Sc2b, Shi, StS, Sta, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 40

**Selected features:** selected = {AS, ASt, Cuf, DBu, DC, DCu, HS, In, Kc, LA, LG, Ls, Marv, Me, NAG, Np, Ord, PO, SCh, SSta, Sc2b, Shi, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 67/72 = 93.1% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 111/119 = 93.3% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 97/111 = 87.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 41

**Selected features:** selected = {AS, ASt, BB, Bla, CBu, CCh, Ches, HS, IC, In, Ke, LA, Me, ODI, Ord, PO, Rc, SSta, Sc1b, Shi, Ss, Sta, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 42

**Selected features:** selected = {AC, AS, CBu, CSta, Cuf, Ds, Gr, HS, In, LA, Ls, Me, Np, OCu, ODI, Ord, PO, RB, Rc, SCh, Shi, Ti, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 43

**Selected features:** selected = {AC, ACu, AS, ASt, BAB, CSta, Cuf, DBu, Go, HS, In, Kc, M, Me, Ord, PO, SCh, Sc2b, Shi, Ss, Sta, TA, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 110/119 = 92.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 95/111 = 85.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 44

**Selected features:** selected = {AA, AC, AS, ASt, Bd, Bri, CCh, Cuf, DB, DBu, DCu, HS, In, Ls, Me, NAG, Np, Ord, PO, SSta, Sc1b, Shi, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 69/72 = 95.8% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 113/119 = 95.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 101/111 = 91.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 45

**Selected features:** selected = {AS, BAB, BB, Bro, CBu, CCh, CSta, Cuf, DB, DCu, HS, IC, In, Kc, LA, Ls, Me, Np, Ord, PO, Shi, Tbc, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 46

**Selected features:** selected = {AA, AS, BluB, CBu, Ch, HS, IC, ICu, Me, Ord, PO, Rc, SCh, SSta, Shi, Ss, Sta, Tbc, Tu}

**Repaired FTS:** 55 states, 61 transitions (61 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 61 | 0/61 = 0.0% | 0/61 = 0.0% | 58/61 = 95.1% | 61/61 = 100.0% | 61/61 = 100.0% | 59/61 = 96.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 73/77 = 94.8% | 77/77 = 100.0% | 77/77 = 100.0% | 75/77 = 97.4% |
| StateMissing | 54 | 0/54 = 0.0% | 0/54 = 0.0% | 54/54 = 100.0% | 54/54 = 100.0% | 54/54 = 100.0% | 53/54 = 98.1% |
| TransitionDestinationExchange | 77 | 2/77 = 2.6% | 0/75 = 0.0% | 68/75 = 90.7% | 75/75 = 100.0% | 75/75 = 100.0% | 72/75 = 96.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 47

**Selected features:** selected = {CBu, CSta, Ches, DC, DCu, HS, In, Mar, Me, OCu, ODI, Ord, PO, Pu, RB, SCh, SS, Sc1b, Shi, Ss, Sta, TA, UnT, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 98/112 = 87.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 48

**Selected features:** selected = {BAB, Be, CBu, Ches, DC, HS, In, LA, LB, Ls, Me, Ord, PO, R1b, RB, SCh, SS, SSta, Shi, Sta, Suc, UnT}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 49

**Selected features:** selected = {ASt, BAB, BluB, CBu, CSta, Cuf, Ds, HS, In, Ja, Kc, LG, Me, Ord, PO, SCh, Shi, Ss, StS, Sta, TA, Tu}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 62/64 = 96.9% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 80/82 = 97.6% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 78/81 = 96.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 50

**Selected features:** selected = {AC, ACu, ASta, Ches, DB, DBu, HS, In, Kc, LA, Ls, Mcc, Me, NAG, Np, Ord, PO, R1b, SCh, SS, Shi, UnT}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 62/67 = 92.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 89/96 = 92.7% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 77/93 = 82.8% | 92/93 = 98.9% | 93/93 = 100.0% | 82/93 = 88.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 51

**Selected features:** selected = {AA, AC, ASt, CBu, CCh, CSta, Cuf, DB, DCu, HS, In, Me, NK, ODI, Ord, PO, Ry, SS, Shi, Ss, Sta, Tbc, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 96/112 = 85.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 52

**Selected features:** selected = {AA, ACh, Ar, CSta, Cuf, DBu, HS, IC, ICu, In, Kc, LC, Me, OCu, Ord, PO, Sc2b, Shi, Ss, StS, Sta, UnT, YD}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 69/72 = 95.8% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 116/119 = 97.5% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 103/111 = 92.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 53

**Selected features:** selected = {AA, ACu, Ak, Bla, CCh, CSta, Cuf, DBu, HS, IC, In, Ls, Me, OCu, ODI, Ord, PO, R1b, Rc, SS, Shi, Sta, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 67/72 = 93.1% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 109/119 = 91.6% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 95/111 = 85.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 54

**Selected features:** selected = {AA, ACu, ASt, BB, CBu, CSta, Ches, HS, IC, In, Ls, MB, Me, Ord, PO, Pe, SCh, SS, Sc1b, Shi, Sta, UnT, Wc, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 109/120 = 90.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 55

**Selected features:** selected = {AS, BB, CBu, CCh, CSta, DSt, Ds, HS, LA, Me, N, Ord, PO, Shi, Ss, Sta, Suc, Tu}

**Repaired FTS:** 54 states, 58 transitions (58 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 56/58 = 96.6% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| ActionExchange | 69 | 0/69 = 0.0% | 0/69 = 0.0% | 67/69 = 97.1% | 69/69 = 100.0% | 69/69 = 100.0% | 68/69 = 98.6% |
| StateMissing | 53 | 0/53 = 0.0% | 0/53 = 0.0% | 53/53 = 100.0% | 53/53 = 100.0% | 53/53 = 100.0% | 53/53 = 100.0% |
| TransitionDestinationExchange | 69 | 1/69 = 1.4% | 0/68 = 0.0% | 63/68 = 92.6% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 56

**Selected features:** selected = {AS, ASt, BB, CBu, CSta, Cuf, DC, HS, ICu, In, LA, LP, Ls, Mcg, Me, NAG, Ord, PO, Rc, SCh, Sc1b, Shi, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 111/120 = 92.5% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 57

**Selected features:** selected = {A, AC, ASt, ASta, Ches, DBu, HS, ICu, In, Me, NAG, Np, Ord, PO, Pu, Rc, SCh, SS, Shi, Ss, TA, Tbc, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 65/72 = 90.3% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 108/119 = 90.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 91/111 = 82.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 58

**Selected features:** selected = {AA, ACh, AS, ASta, Bd, Bla, By, Cuf, DBu, HS, IC, In, Me, ODI, Ord, PO, Sc1b, Shi, Ss, Sta, UnT}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 56/63 = 88.9% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 72/81 = 88.9% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 52/56 = 92.9% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 70/80 = 87.5% | 80/80 = 100.0% | 80/80 = 100.0% | 68/80 = 85.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 59

**Selected features:** selected = {AA, AC, BluB, CBu, CCh, Ches, DCu, HS, Ha, In, Ls, MG, Me, OCu, Ord, PO, SSta, Sc1b, Shi, StS, Sta, Suc, Tu, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 60

**Selected features:** selected = {ACh, ASta, BluB, CBu, Ches, HS, IC, ICu, In, LC, Ls, Me, NK, ODI, Ord, PO, SS, Sc2b, Shi, So, Sta, TA, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 61

**Selected features:** selected = {AC, AS, Bd, BluB, CBu, CCh, Cuf, Ga, HS, ICu, In, LA, LC, Me, Np, OCu, ODI, Ord, PO, SSta, Sc1b, Shi, Ss, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 62

**Selected features:** selected = {ACh, ACu, AS, An, BB, CBu, Ches, DC, HS, In, LA, LC, Me, NAG, OCu, Ord, PO, SSta, Shi, Ss, Sta, Tbc, Tu, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 63

**Selected features:** selected = {AA, ACu, ASta, Cuf, DBu, DC, HS, In, J, Me, OCu, ODI, Ord, PO, Rr, SCh, SS, Sc1b, Shi, Ss, Sta, Suc, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 67/72 = 93.1% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 109/119 = 91.6% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 96/111 = 86.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 64

**Selected features:** selected = {AA, ACh, ACu, BB, CBu, CSta, Cuf, DSt, HS, In, Li, Ls, Me, Ord, PO, Sc2b, Shi, Sil, StS, Sta, Suc, UnT, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 84/94 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 65

**Selected features:** selected = {AC, ACh, Bd, BluB, CBu, CSta, Cuf, DSt, HS, ICu, In, LC, Me, Np, ODI, Ord, PO, SS, Shi, Ss, TA, Tbc, UnT, W}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 104/120 = 86.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 90/112 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 66

**Selected features:** selected = {BAB, BluB, CBu, CCh, CSta, Car, Cuf, DC, DCu, HS, In, Ls, MG, Me, Ord, PO, Rc, SS, Sc1b, Shi, Sta, TA, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 89/97 = 91.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 82/94 = 87.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 67

**Selected features:** selected = {ACu, BAB, CBu, Cuf, DB, DC, HS, In, Ls, Mcd, Me, Ord, PO, R1b, RB, SCh, SSta, Shi, StS, Sta, TA, UnT, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 86/94 = 91.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 68

**Selected features:** selected = {ACu, ASta, CBu, CCh, Ches, DB, HS, IC, In, Ls, Me, Mu, Np, ODI, Ord, PO, RB, Shi, StS, TA, Tbc, Tu, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 94/97 = 96.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 69

**Selected features:** selected = {AA, AC, ACh, ASta, BB, Bro, CBu, Cuf, Go, HS, In, Ls, Me, Np, OCu, Ord, PO, Sc2b, Shi, StS, UnT, Wc, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 70

**Selected features:** selected = {AS, BAB, BB, CBu, CCh, CSta, Ches, DCu, DSt, Ds, HS, In, Me, Ord, PO, Rc, Shi, Sil, Ss, Sta, TA, UnT, Ya}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 71

**Selected features:** selected = {CCh, CSta, Cuf, DBu, DC, Eli, HS, In, Kc, LA, Me, NAG, OCu, Ord, PO, R1b, Rr, Shi, Ss, StS, Sta, Tu}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 58/67 = 86.6% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 81/96 = 84.4% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 52/57 = 91.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 75/93 = 80.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 72

**Selected features:** selected = {CBu, CSta, Ches, DCu, F, Gr, HS, IC, In, LA, Ls, Me, NK, Np, ODI, Ord, PO, RB, SCh, Sc1b, Shi, StS, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 73

**Selected features:** selected = {ACu, CBu, CCh, CSta, Ches, Cop, D, DSt, HS, In, Me, Np, Ord, PO, R1b, SS, Shi, Ss, Suc, TA, UnT, WB, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 84/94 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 74

**Selected features:** selected = {AA, Arm, CBu, CSta, Cuf, DC, Ds, HS, In, Kc, LG, Ls, Me, NAG, Np, Ord, PO, SCh, Shi, StS, UnT, WB}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 75

**Selected features:** selected = {ASta, Bd, Cuf, DBu, DC, DSt, HS, ICu, In, LG, Me, Np, Ord, PO, SCh, SS, Shi, Ss, TA, Tbc, UnT, Vi, YD}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 63/72 = 87.5% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 98/119 = 82.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 54/58 = 93.1% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 79/111 = 71.2% | 108/111 = 97.3% | 111/111 = 100.0% | 87/111 = 78.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 76

**Selected features:** selected = {ACu, AS, ASta, BAB, CBu, CCh, Ches, DC, HS, In, Ja, MG, Me, OCu, Ord, PO, Rc, Sc2b, Shi, Ss, Sta, TA, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 77

**Selected features:** selected = {AC, ACu, At, Bla, CBu, CCh, Cuf, DSt, Ds, HS, In, LA, Me, ODI, Ord, PO, SS, SSta, Shi, Ss, Sta, Suc, Tu, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 78

**Selected features:** selected = {AS, BAB, Bd, CCh, Ches, DBu, DCu, DG, HS, IC, In, Ken, LA, Me, OCu, Ord, PO, R1b, SSta, Shi, Ss, Sta, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 69/72 = 95.8% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 116/119 = 97.5% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 103/111 = 92.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 79

**Selected features:** selected = {ACh, ASt, CSta, Cuf, DBu, DC, Go, HS, ICu, In, LA, Ls, Me, NAG, Np, Ord, PO, Rc, SS, Sc2b, Shi, Tu, W}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 68/72 = 94.4% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 112/119 = 94.1% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 98/111 = 88.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 80

**Selected features:** selected = {AA, CBu, CSta, Ches, DCu, DSt, HS, IC, In, LP, Me, ODI, Ord, PO, Rc, SCh, SS, Sc2b, Shi, Ss, Sta, UnT, Va, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 81

**Selected features:** selected = {AA, ACu, Bla, CBu, CSta, Cuf, Fe, HS, IC, In, Ls, Me, OCu, Ord, PO, RB, SCh, Sc1b, Shi, StS, Sta, UnT, Wc, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 82

**Selected features:** selected = {CBu, CCh, CSta, Co, Cuf, DSt, Ds, HS, In, Kc, Ls, MG, Me, NAG, Ord, PO, SS, Shi, Sta, TA, UnT, WB}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 83

**Selected features:** selected = {AA, ASt, CBu, CCh, Cuf, Gr, HS, Ho, IC, ICu, In, Kc, Ls, Me, Np, ODI, Ord, PO, SSta, Sc2b, Shi, StS, Tu, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 111/120 = 92.5% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 96/112 = 85.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 84

**Selected features:** selected = {AS, ASt, BB, CBu, CCh, Ches, Cop, Eli, HS, IC, In, Me, Np, ODI, Ord, PO, SSta, Shi, Ss, Suc, TA, Tbc, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 85

**Selected features:** selected = {ACh, ASta, BAB, BB, CBu, Ches, DCu, Gl, HS, In, LA, Ls, MG, Me, OCu, Ord, PO, Rc, SS, Sc2b, Shi, Sta, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 86

**Selected features:** selected = {AA, ACh, ASt, BAB, By, CBu, CSta, Cuf, DC, DG, HS, ICu, In, Me, NK, Ord, PO, R1b, RB, Shi, Ss, StS, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 111/120 = 92.5% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 87

**Selected features:** selected = {AC, AS, ASt, BAB, CBu, CSta, Ches, HS, ICu, In, LB, Me, Np, Ord, PO, SCh, Sc2b, Shi, Ss, Suc, TA, Ti, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 88

**Selected features:** selected = {AS, CBu, CCh, Ches, Cop, DC, DCu, El, HS, In, LA, Ls, Me, OCu, Ord, PO, R1b, SSta, Shi, Sta, Suc, Tu, WB, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 98/112 = 87.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 89

**Selected features:** selected = {AC, Am, CBu, Cuf, DCu, HS, In, LA, Me, OCu, Ord, PO, Rc, SCh, SS, SSta, Shi, Sil, Ss, Sta, Tbc, Tu, WB, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 90

**Selected features:** selected = {AA, ACu, AS, ASt, Ak, BAB, CBu, CSta, Cuf, DC, HS, In, Ls, MB, Me, NK, Ord, PO, SCh, Sc2b, Shi, Sta, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 91

**Selected features:** selected = {AA, AC, CCh, Cuf, DBu, HS, In, LG, Me, Np, OCu, ODI, Ord, PO, Rc, SS, SSta, Sc2b, Shi, Ss, UnT, We}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 65/67 = 97.0% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 92/96 = 95.8% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 87/93 = 93.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 92

**Selected features:** selected = {ACh, AS, ASta, C, CBu, Ches, DB, DC, DCu, DSt, Ds, HS, In, Kc, Ls, Me, Ord, PO, RB, Shi, Sta, TA, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 93

**Selected features:** selected = {BluB, CBu, CCh, CSta, Ches, DCu, HS, IC, In, Kay, LA, Ls, Me, NAG, Np, Ord, PO, Rc, Rr, Sc2b, Shi, StS, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 94

**Selected features:** selected = {AA, Am, Ches, DBu, DC, HS, ICu, In, Kc, Me, Np, Ord, PO, SCh, SSta, Sc2b, Shi, Sil, Ss, StS, UnT, YD}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 64/67 = 95.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 92/96 = 95.8% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 74/93 = 79.6% | 92/93 = 98.9% | 93/93 = 100.0% | 85/93 = 91.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 95

**Selected features:** selected = {ASta, B, BB, CBu, CCh, Cuf, DC, DSt, Go, HS, ICu, In, LA, Me, NAG, Ord, PO, Sc2b, Shi, Ss, StS, Sta, Suc, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 96

**Selected features:** selected = {AC, ASt, BAB, BluB, CBu, Cuf, Ds, Ga, HS, ICu, In, LA, LC, Me, Np, Ord, PO, SCh, SS, SSta, Shi, Ss, Tu, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 97

**Selected features:** selected = {ACh, BB, CBu, Cuf, DSt, HS, IC, ICu, In, LA, LB, Ls, Me, Mid, NAG, NK, Ord, PO, SS, SSta, Shi, Sta, Tbc, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 98

**Selected features:** selected = {AA, ACu, CBu, CSta, Cop, Cuf, Ds, Gar, HS, IC, In, Ls, Me, NK, Ord, PO, SCh, SS, Shi, Sta, UnT, WB, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 94/97 = 96.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 99

**Selected features:** selected = {ACh, ASta, Be, CBu, Ches, DC, DSt, Ds, Gr, HS, ICu, In, Ls, Me, Np, ODI, Ord, PO, Shi, StS, Suc, TA, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 100

**Selected features:** selected = {AC, ACh, ASt, BAB, CSta, Cuf, DBu, DCu, DG, Fl, HS, In, LA, Ls, Me, NK, Np, Ord, PO, R1b, Shi, StS, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 69/72 = 95.8% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 113/119 = 95.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 101/111 = 91.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 101

**Selected features:** selected = {AA, ASta, Arm, BAB, BB, CBu, Cuf, HS, IC, In, MB, Me, Ord, PO, SCh, Sc1b, Shi, Ss, StS, Sta, Suc, Tu}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 80/82 = 97.6% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 102

**Selected features:** selected = {AA, ASta, CCh, Ches, DBu, DSt, Ew, HS, IC, ICu, In, Ls, Me, NAG, Np, Ord, PO, R1b, Rc, SS, Shi, Sil, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 68/72 = 94.4% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 109/119 = 91.6% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 98/111 = 88.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 103

**Selected features:** selected = {AA, AC, ACh, ACu, ASt, BluB, CBu, Ches, DB, HS, In, Me, Ord, PO, Rc, SS, SSta, Sc2b, Shi, So, Ss, Sta, Tu, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 104

**Selected features:** selected = {AA, ACh, ACu, AS, ASt, ASta, CBu, Cuf, HS, In, Kc, LB, Me, Ord, PO, Shi, Ss, Sta, Tam, Tbc, UnT, WB, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 88/94 = 93.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 105

**Selected features:** selected = {AA, ACu, ASt, ASta, BAB, BluB, CBu, Cuf, HS, In, LB, Me, NK, Ord, PO, Rk, SCh, SS, Sc2b, Shi, Ss, Sta, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 88/94 = 93.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 106

**Selected features:** selected = {AS, ASta, CBu, Ches, DCu, DSt, HS, In, LA, MG, Me, Np, Ord, PO, R1b, RB, SCh, Shi, Si, Ss, Tu, Wc, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 107

**Selected features:** selected = {AA, AC, AS, Bd, CCh, Car, Cuf, DBu, Go, HS, ICu, In, Ls, Me, NAG, Ord, PO, SSta, Sc2b, Shi, Sta, Tu}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 62/67 = 92.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 89/96 = 92.7% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 77/93 = 82.8% | 92/93 = 98.9% | 93/93 = 100.0% | 83/93 = 89.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 108

**Selected features:** selected = {AA, AC, ACh, ASt, BAB, By, CBu, CSta, Cuf, DG, HS, In, Me, Np, Ord, PO, SS, Sc1b, Shi, Ss, Tu, WB, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 97/97 = 100.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 91/94 = 96.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 109

**Selected features:** selected = {AC, ACh, ACu, ASt, Ak, BB, Bd, CBu, CSta, Cuf, DG, Ds, HS, In, LA, Me, Np, ODI, Ord, PO, SS, Shi, Ss, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 110

**Selected features:** selected = {AS, Bla, CCh, CSta, Ches, DBu, DCu, Gar, HS, In, LA, Me, Np, Ord, PO, R1b, Rc, Shi, Ss, UnT, YD}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 56/63 = 88.9% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 72/81 = 88.9% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 52/56 = 92.9% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 71/80 = 88.8% | 80/80 = 100.0% | 80/80 = 100.0% | 68/80 = 85.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 111

**Selected features:** selected = {AA, AS, Bd, BluB, CBu, Ches, HS, IC, ICu, In, Me, Nu, Ord, PO, R1b, Rr, SCh, SSta, Shi, Ss, Sta, Tu, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 112

**Selected features:** selected = {AA, AS, BB, CBu, CSta, Cu, Cuf, DB, DCu, HS, In, Me, NAG, NK, Ord, PO, SCh, Shi, Ss, Sta, Tbc, UnT}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 113

**Selected features:** selected = {AA, AC, ACh, ASta, BluB, CBu, Ches, DSt, HS, ICu, In, LP, Ls, Me, Ord, PO, Sc1b, Shi, So, StS, Sta, Tu, Wc, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 104/120 = 86.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 90/112 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 114

**Selected features:** selected = {ASta, Bla, CBu, CCh, Ches, HS, IC, In, LA, Ls, Me, Np, OCu, ODI, Ord, PO, RB, Rc, Sa, Sc1b, Shi, StS, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 89/94 = 94.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 115

**Selected features:** selected = {ACh, Bd, BluB, CBu, Ches, DB, DC, DCu, HS, Har, In, LA, Me, NAG, Ord, PO, SSta, Sc2b, Shi, Ss, StS, Sta, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 89/97 = 91.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 82/94 = 87.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 116

**Selected features:** selected = {AA, ACh, BAB, BB, CBu, Cu, Cuf, DC, DSt, HS, ICu, In, LC, Ls, Me, Np, Ord, PO, SSta, Shi, StS, Tbc, UnT, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 117

**Selected features:** selected = {AA, AS, ASta, BB, CBu, Coa, Cuf, DCu, DG, Ds, HS, In, Ls, Me, Np, OCu, Ord, PO, Rc, SCh, Shi, Tu, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 118

**Selected features:** selected = {AC, ACu, AS, ASta, BB, CBu, Cl, Cuf, DSt, HS, In, LA, Ls, Me, Ord, PO, R1b, Rr, SCh, Shi, Sta, UnT, Wc, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 119

**Selected features:** selected = {ACu, AS, ASta, BB, CBu, Cuf, DSt, HS, IC, In, Ls, MG, Me, Np, ODI, Ord, PO, SCh, Sc1b, Shi, Suc, TA, Th, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 100/120 = 83.3% | 120/120 = 100.0% | 120/120 = 100.0% | 111/120 = 92.5% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 98/112 = 87.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 120

**Selected features:** selected = {ASt, ASta, Bd, CCh, Ches, DBu, Gr, HS, In, Me, NAG, Ord, PO, Sc2b, Shi, Ss, StS, Sta, TA, UnT, We}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 63/63 = 100.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 81/81 = 100.0% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 70/80 = 87.5% | 80/80 = 100.0% | 80/80 = 100.0% | 80/80 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 121

**Selected features:** selected = {ASt, CCh, CSta, Calh, Cuf, DBu, DCu, HS, IC, In, LA, LG, Me, Np, ODI, Ord, PO, SS, Sc2b, Shi, Ss, UnT, Wc}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 65/72 = 90.3% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 106/119 = 89.1% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 92/111 = 82.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 122

**Selected features:** selected = {ACh, ACu, AS, BluB, CBu, Ches, Gr, HS, Ha, In, LA, Me, Np, Ord, PO, R1b, Rc, SSta, Shi, Ss, Tu, YD}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 123

**Selected features:** selected = {AA, ACh, BAB, Bd, Cuf, DBu, Ds, Go, HS, IC, ICu, In, M, Me, Np, OCu, Ord, PO, SSta, Shi, Ss, StS, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 69/72 = 95.8% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 116/119 = 97.5% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 103/111 = 92.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 124

**Selected features:** selected = {AA, ACu, ASt, ASta, Arm, Bla, BluB, CBu, CCh, Cuf, HS, IC, In, Ls, Me, Ord, PO, R1b, Rc, Shi, StS, Sta, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 109/120 = 90.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 125

**Selected features:** selected = {ACh, ACu, AS, BluB, CBu, Ches, DSt, Ds, HS, IC, In, LA, Li, Me, Np, Ord, PO, SSta, Shi, Sil, Ss, Suc, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 100/120 = 83.3% | 120/120 = 100.0% | 120/120 = 100.0% | 111/120 = 92.5% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 98/112 = 87.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 126

**Selected features:** selected = {BAB, BluB, CBu, Cop, Cuf, DSt, HS, IC, ICu, In, LA, Ls, Me, Np, Ord, PO, Po, SCh, SSta, Sc1b, Shi, StS, UnT, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 127

**Selected features:** selected = {AA, CBu, CCh, Co, Cuf, DCu, DSt, HS, In, MB, Me, Ord, PO, R1b, RB, SSta, Shi, Ss, StS, Sta, Tu, Wc, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 128

**Selected features:** selected = {ASt, ASta, CCh, Cuf, DBu, DC, DCu, HS, In, LG, Mcg, Me, Ord, PO, R1b, Shi, Ss, StS, Sta, TA, UnT, Wc, YD}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 67/72 = 93.1% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 111/119 = 93.3% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 97/111 = 87.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 129

**Selected features:** selected = {ACh, AS, ASta, BluB, CBu, Ches, Cop, DC, DCu, HS, In, LA, Me, OCu, ODI, Ord, PO, Rc, Ry, Sc1b, Shi, Ss, Sta, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 98/112 = 87.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 130

**Selected features:** selected = {AS, BAB, BluB, CBu, CSta, Cuf, HS, IC, ICu, In, LA, LG, Me, Np, OCu, Ord, PO, SCh, Shi, Ss, Suc, Tbc, Tr, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 97/112 = 86.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 131

**Selected features:** selected = {AC, ACu, AS, BB, CBu, CSta, Ches, Ell, HS, In, Ls, Me, Ord, PO, R1b, Rr, SCh, Shi, Sta, TA, Tu, Wc, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 132

**Selected features:** selected = {Arm, BAB, Bd, CBu, CCh, CSta, Cuf, DCu, HS, In, LA, LB, Ls, Me, Np, Ord, PO, SS, Sc1b, Shi, Tu, WB}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 133

**Selected features:** selected = {AS, ASt, Bla, CBu, CCh, Ches, DC, HS, In, LA, Me, Np, Nu, ODI, Ord, PO, SSta, Sc2b, Shi, Ss, UnT, WB, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 86/94 = 91.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 134

**Selected features:** selected = {AA, CBu, CCh, Ches, HS, Holl, IC, ICu, In, LP, Me, Np, OCu, ODI, Ord, PO, RB, Rc, SS, SSta, Sc1b, Shi, Ss, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 97/112 = 86.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 135

**Selected features:** selected = {AS, BAB, CBu, CCh, Cuf, DSt, Et, HS, IC, ICu, In, LA, MG, Me, Np, Ord, PO, R1b, RB, Rc, SSta, Shi, Ss, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 136

**Selected features:** selected = {AS, BAB, BB, Bd, CBu, CCh, CSta, Ches, DB, HS, IC, ICu, In, LA, Me, OCu, Ol, Ord, PO, Shi, Ss, Sta, Tbc, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 97/112 = 86.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 137

**Selected features:** selected = {AA, ACu, AS, ASt, BAB, CBu, Cop, Cuf, DC, HS, In, Kl, Ls, Me, Ord, PO, R1b, RB, SCh, SSta, Shi, Sta, Suc, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 138

**Selected features:** selected = {AA, ASta, Ar, BAB, BluB, CBu, Cuf, DCu, HS, IC, In, Ls, MB, Me, OCu, Ord, PO, SCh, Sc2b, Shi, StS, Sta, Tu, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 97/112 = 86.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 139

**Selected features:** selected = {AA, AS, BAB, BluB, CBu, CCh, Cuf, DC, HS, In, Kayc, LP, Ls, Me, Ord, PO, SSta, Sc1b, Shi, Sta, Suc, UnT}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 140

**Selected features:** selected = {AA, BB, CBu, CCh, Cuf, DB, DC, DSt, HS, Ho, In, Me, NK, Np, Ord, PO, R1b, SSta, Shi, Ss, StS, UnT, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 84/94 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 141

**Selected features:** selected = {AC, ACu, AS, ASt, ASta, CCh, Ches, DBu, HS, In, LA, Ls, MB, Me, NAG, NK, Ord, PO, Ri, Sc1b, Shi, Sta, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 110/119 = 92.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 95/111 = 85.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 142

**Selected features:** selected = {AS, BAB, CSta, Ches, DBu, DCu, HS, Hu, IC, In, LA, LG, Ls, Me, Ord, PO, Rc, SCh, Sc2b, Shi, Sta, UnT}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 59/67 = 88.1% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 82/96 = 85.4% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 74/93 = 79.6% | 92/93 = 98.9% | 93/93 = 100.0% | 76/93 = 81.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 143

**Selected features:** selected = {AA, AC, ACh, ASt, CBu, CSta, Cuf, DB, DCu, HS, In, Ls, Me, NAG, NK, Np, Ord, PO, Pr, Sc1b, Shi, StS, Tu, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 96/112 = 85.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 144

**Selected features:** selected = {AA, AC, CBu, CCh, CSta, Ches, DCu, HS, Hu, In, MB, Me, NAG, Np, OCu, Ord, PO, SS, Shi, Ss, Suc, Tbc, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 145

**Selected features:** selected = {ACh, AS, Arm, Bla, CBu, CSta, Ches, DCu, HS, In, LA, Ls, Me, NAG, Np, Ord, PO, RB, Sc2b, Shi, Suc, Tu}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 146

**Selected features:** selected = {ASt, BAB, CBu, CCh, Ches, DCu, Go, HS, IC, In, Ke, Ls, Me, NK, Ord, PO, RB, SS, SSta, Sc1b, Shi, Sta, TA, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 147

**Selected features:** selected = {AA, ACu, ASta, Bd, CBu, CCh, Ches, DC, DG, HS, In, Me, Np, OCu, Ord, PO, Sc2b, Shi, Ss, StS, UnT, WB, Y, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 148

**Selected features:** selected = {ACh, AS, ASt, Ches, DBu, Ds, HS, ICu, In, LA, Ls, Mc, Me, Np, Ord, PO, Pu, Rc, SSta, Shi, UnT, YD}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 65/67 = 97.0% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 95/96 = 99.0% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 89/93 = 95.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 149

**Selected features:** selected = {ACh, AS, ASta, Be, CBu, Cuf, DCu, Ds, HS, IC, In, Kc, Ls, MG, Me, Ord, PO, RB, Shi, Sta, TA, Tu, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 150

**Selected features:** selected = {AA, AC, ASt, BluB, By, CBu, CCh, Ches, DCu, HS, In, LC, Me, NAG, NK, Ord, PO, SS, SSta, Sc2b, Shi, Ss, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 96/112 = 85.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 151

**Selected features:** selected = {ACu, ASt, CBu, CCh, CSta, Cuf, HS, In, LA, LC, Me, Np, Ord, PO, Sc2b, Shi, Ss, StS, Suc, UnT, WB, Wa, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 88/94 = 93.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 152

**Selected features:** selected = {AC, ACh, ASta, Ches, DBu, DSt, Eli, Go, HS, ICu, In, Me, NK, Np, ODI, Ord, PO, SS, Shi, Ss, TA, Tbc, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 68/72 = 94.4% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 113/119 = 95.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 100/111 = 90.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 153

**Selected features:** selected = {AC, AS, ASt, ASta, Ches, DBu, DG, HS, ICu, In, LA, Me, NAG, Np, Ord, PO, Reg, SCh, Sc2b, Shi, Ss, Suc, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 65/72 = 90.3% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 108/119 = 90.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 91/111 = 82.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 154

**Selected features:** selected = {AS, ASt, ASta, Cuf, DBu, DC, HS, In, Kat, LA, Ls, MB, Me, NK, ODI, Ord, PO, SCh, Sc1b, Shi, Sta, UnT}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 58/67 = 86.6% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 81/96 = 84.4% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 52/57 = 91.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 75/93 = 80.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 155

**Selected features:** selected = {AS, ASt, Bd, Cop, Cuf, DBu, HS, IC, ICu, In, Ls, Mc, Me, Np, ODI, Ord, PO, SCh, SSta, Sc2b, Shi, TA, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 108/119 = 90.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 95/111 = 85.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 156

**Selected features:** selected = {ACh, ASt, Bd, BluB, CBu, CSta, DCu, HS, Me, Np, Nu, Ord, PO, SS, Shi, Ss, TA, Tbc, Tu}

**Repaired FTS:** 55 states, 61 transitions (61 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 61 | 0/61 = 0.0% | 0/61 = 0.0% | 57/61 = 93.4% | 61/61 = 100.0% | 61/61 = 100.0% | 58/61 = 95.1% |
| ActionExchange | 77 | 0/77 = 0.0% | 0/77 = 0.0% | 74/77 = 96.1% | 77/77 = 100.0% | 77/77 = 100.0% | 74/77 = 96.1% |
| StateMissing | 54 | 0/54 = 0.0% | 0/54 = 0.0% | 54/54 = 100.0% | 54/54 = 100.0% | 54/54 = 100.0% | 53/54 = 98.1% |
| TransitionDestinationExchange | 77 | 2/77 = 2.6% | 0/75 = 0.0% | 65/75 = 86.7% | 75/75 = 100.0% | 75/75 = 100.0% | 70/75 = 93.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (2):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 157

**Selected features:** selected = {AA, ASt, ASta, CBu, Ches, DC, DCu, HS, In, Ls, Me, NAG, Np, Ord, PO, Pu, RB, Ra, SCh, SS, Sc2b, Shi, Suc, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 158

**Selected features:** selected = {AA, AS, ASta, Ak, CBu, Ches, DC, DCu, DSt, Ds, HS, In, Kc, LC, Me, ODI, Ord, PO, RB, SCh, Shi, Ss, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 159

**Selected features:** selected = {AA, AS, ASta, BAB, CBu, CCh, Cuf, DCu, DSt, Ds, HS, IC, In, Kayc, LP, Ls, Me, Np, Ord, PO, Shi, UnT, WB, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 160

**Selected features:** selected = {AC, AS, ASta, BB, Bla, CBu, Ches, DSt, Ds, HS, In, Ls, Me, NAG, Ord, PO, SCh, Shi, Si, Sta, Suc, TA, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 161

**Selected features:** selected = {ACh, BluB, CBu, Ches, HS, ICu, In, Kc, LA, Ls, Mcc, Me, Np, OCu, Ord, PO, Pu, SSta, Sc1b, Shi, StS, Tu, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 162

**Selected features:** selected = {AC, AS, CBu, Ches, DCu, DSt, Ds, HS, In, Ls, Mart, Me, NAG, Np, Ord, PO, Rc, Rr, SCh, SSta, Shi, TA, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 104/120 = 86.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 90/112 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 163

**Selected features:** selected = {Bens, BluB, CBu, CCh, Ches, DSt, HS, ICu, In, LA, LG, Ls, Me, Np, ODI, Ord, PO, Rc, SS, SSta, Sc2b, Shi, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 164

**Selected features:** selected = {AA, ACh, AS, ASta, BluB, CBu, Ches, Coa, DCu, HS, IC, In, Ls, Me, OCu, Ord, PO, Pu, Sc2b, Shi, Sta, Suc, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 97/112 = 86.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 165

**Selected features:** selected = {AA, AC, CSta, Ches, Cop, DBu, HS, ICu, In, Me, NAG, Ord, PO, R1b, Rc, SCh, SS, Shi, Ss, Sta, Tu, Van}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 62/67 = 92.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 89/96 = 92.7% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 77/93 = 82.8% | 92/93 = 98.9% | 93/93 = 100.0% | 83/93 = 89.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 166

**Selected features:** selected = {AA, AC, CBu, CCh, CSta, Cuf, Ds, Gr, HS, In, Me, NAG, Np, Ord, PO, RB, Ree, SS, Shi, Ss, Suc, UnT}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 167

**Selected features:** selected = {ASta, Bd, CBu, CCh, Cuf, Ds, HS, IC, In, LG, Me, Np, OCu, ODI, Ord, PO, RB, Re, Shi, Ss, StS, TA, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 89/94 = 94.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 168

**Selected features:** selected = {AA, ACh, Bla, Cuf, DBu, DCu, HS, In, Me, OCu, ODI, Ord, PO, SSta, Sc1b, Shi, Ss, StS, Sta, Suc, Tap, UnT}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 65/67 = 97.0% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 95/96 = 99.0% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 89/93 = 95.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 169

**Selected features:** selected = {ASt, BAB, CBu, Ches, HS, ICu, In, LA, LB, Me, Ord, PO, S, SCh, SSta, Sc2b, Shi, Ss, StS, Sta, Suc, Tu, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 91/97 = 93.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 170

**Selected features:** selected = {ASta, Bd, Ches, DBu, DSt, Ew, HS, IC, ICu, In, Me, NAG, Np, Ord, PO, SCh, Shi, Sil, Ss, StS, TA, Tbc, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 68/72 = 94.4% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 109/119 = 91.6% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 98/111 = 88.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 171

**Selected features:** selected = {AA, AC, ASta, BluB, CBu, Cuf, Ga, HS, In, Me, NAG, Np, Ord, PO, Pu, SCh, Sc1b, Shi, Ss, StS, UnT, Wc}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 172

**Selected features:** selected = {AC, AS, ASt, BluB, CBu, CSta, Ches, Cop, HS, ICu, In, Kc, Me, NAG, Ord, PO, Pr, SCh, Sc2b, Shi, Ss, Sta, TA, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 173

**Selected features:** selected = {ACh, ACu, AS, Ches, DBu, HS, IC, In, LA, Ls, Me, NK, Np, ODI, Ol, Ord, PO, Pu, SSta, Sc1b, Shi, Tu}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 61/67 = 91.0% | 67/67 = 100.0% | 67/67 = 100.0% | 62/67 = 92.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 89/96 = 92.7% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 76/93 = 81.7% | 92/93 = 98.9% | 93/93 = 100.0% | 82/93 = 88.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 174

**Selected features:** selected = {AC, AS, CBu, Cuf, HS, He, ICu, In, Ls, MB, Me, NK, Ord, PO, RB, SCh, SSta, Sc1b, Shi, Sta, TA, Tu, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 175

**Selected features:** selected = {A, ACh, ASt, BAB, BB, CBu, Ches, DB, DC, DCu, HS, In, LA, Ls, Me, NK, Np, Ord, PO, SS, SSta, Shi, Tbc, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 176

**Selected features:** selected = {ACh, AS, ASt, CBu, Ches, Go, HS, IC, In, Kc, LA, Ls, Me, ODI, Ord, P, PO, SSta, Shi, Sta, Tbc, UnT, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 177

**Selected features:** selected = {ACh, ASt, BB, CBu, CSta, Ches, Cop, DC, Ds, HS, ICu, In, LA, Me, NAG, Np, Ord, PO, Rc, Se, Shi, Ss, StS, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 111/120 = 92.5% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 178

**Selected features:** selected = {AC, ACu, ASt, BAB, BB, CBu, CSta, Ches, HS, In, Ls, Me, Np, Ord, PO, Rr, SCh, Sc2b, Set, Shi, StS, TA, Tu, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 179

**Selected features:** selected = {ACh, Cop, Cuf, DBu, DC, DCu, F, HS, In, LA, Me, NAG, Np, Ord, PO, SS, SSta, Shi, Ss, Tbc, UnT, Wc}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 64/67 = 95.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 92/96 = 95.8% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 74/93 = 79.6% | 92/93 = 98.9% | 93/93 = 100.0% | 85/93 = 91.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 180

**Selected features:** selected = {AC, ACh, AS, ASta, CBu, Ches, DCu, Ds, Go, HS, In, LA, Me, NAG, Np, Ord, PO, RB, Rc, Reu, Shi, Ss, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 181

**Selected features:** selected = {AS, CBu, CCh, CSta, Cuf, Eli, HS, IC, ICu, In, LA, LP, Me, NAG, Np, Ord, PO, RB, Shi, Ss, Tbc, UnT, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 182

**Selected features:** selected = {ACh, CBu, Cuf, DC, DCu, DSt, HS, In, Me, Mu, ODI, Ord, PO, Pu, RB, SSta, Shi, Ss, StS, Sta, TA, Tbc, UnT, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 183

**Selected features:** selected = {ACu, CBu, Ches, DC, Gr, HS, In, LA, Lev, Me, OCu, ODI, Ord, PO, SCh, SS, SSta, Sc1b, Shi, Ss, Sta, UnT, WB, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 184

**Selected features:** selected = {CBu, Cr, Cuf, DCu, HS, IC, In, LA, Ls, MG, Me, Ord, PO, R1b, RB, SCh, SS, SSta, Shi, Sta, Suc, Tu, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 185

**Selected features:** selected = {ACu, ASt, ASta, CBu, Che, Ches, DC, HS, In, LA, MG, Me, NAG, Ord, PO, RB, Rc, SCh, SS, Sc2b, Shi, Ss, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 186

**Selected features:** selected = {AA, BAB, Ba, BluB, CBu, CCh, CSta, Cuf, DC, DSt, HS, In, Kc, Me, Ord, PO, Rr, SS, Shi, Ss, Sta, Tbc, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 84/94 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 187

**Selected features:** selected = {ACh, AS, BB, CBu, Cuf, HS, IC, In, LA, LC, Ls, Me, OCu, Ord, PO, Reg, SSta, Sc1b, Shi, Sta, UnT, Wc, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 89/94 = 94.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 188

**Selected features:** selected = {AA, ACh, ASta, Bd, Bla, Ches, DBu, DCu, DSt, El, HS, IC, In, Me, ODI, Ord, PO, SS, Sc1b, Shi, Ss, Sta, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 68/72 = 94.4% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 109/119 = 91.6% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 98/111 = 88.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 189

**Selected features:** selected = {ACh, AS, C, Ches, DBu, DCu, Ds, HS, In, Me, NAG, OCu, Ord, PO, Rr, SSta, Shi, Ss, Sta, TA, Tu, Wc}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 65/67 = 97.0% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 95/96 = 99.0% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 89/93 = 95.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 190

**Selected features:** selected = {AA, ASta, BAB, BB, Bd, CBu, Cuf, DB, DCu, HS, IC, In, Ls, Me, Ord, PO, R1b, SCh, SS, Shi, Sta, Tu, Wn}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 191

**Selected features:** selected = {AC, ACu, AS, CCh, Cuf, DBu, DSt, Ds, HS, In, LB, Ls, Me, Ord, PO, SSta, Shi, Sta, TA, Tu, Vi, Wc, YD}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 107/119 = 89.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 93/111 = 83.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 192

**Selected features:** selected = {ACh, ACu, ASta, Bla, CBu, Cuf, DC, HS, In, LA, Me, My, NAG, NK, Np, OCu, Ord, PO, R1b, RB, SS, Shi, Ss, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 193

**Selected features:** selected = {AS, CBu, Cuf, DG, Ds, HS, IC, ICu, In, LA, Ls, Me, NAG, Np, Ord, PO, SCh, SSta, Shi, UnT, Va, WB, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 194

**Selected features:** selected = {AC, CBu, Cuf, Gr, HS, In, Me, Ord, PO, RB, SCh, SS, SSta, Shi, Ss, Sta, T, TA, Tbc, Tu, Wc, YD}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 195

**Selected features:** selected = {AA, ASt, ASta, CBu, Ches, DCu, Go, HS, In, Me, ODI, Ord, PO, RB, Rc, SCh, Sc1b, Set, Shi, Ss, StS, Sta, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 196

**Selected features:** selected = {AC, ACu, Bri, CBu, CSta, Ches, HS, In, LA, LC, Ls, Me, Np, OCu, ODI, Ord, PO, RB, Rc, SCh, SS, Sc2b, Shi, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 103/112 = 92.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 197

**Selected features:** selected = {AA, ACh, ACu, ASt, Bens, Bla, BluB, CBu, Cuf, HS, IC, In, Kc, Me, ODI, Ord, PO, SS, SSta, Sc1b, Shi, Ss, Sta, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 109/120 = 90.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 198

**Selected features:** selected = {AA, ACu, AS, BAB, CBu, CCh, Cuf, HS, In, LC, Ls, Me, Np, OCu, Ord, PO, Ro, SSta, Sc2b, Shi, Suc, UnT, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 199

**Selected features:** selected = {ASta, BB, CBu, Ches, DSt, HS, IC, In, Kat, Kc, Ls, Me, Np, ODI, Ord, PO, R1b, Rr, SCh, SS, Shi, TA, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 200

**Selected features:** selected = {AS, CBu, CCh, CSta, Ches, DCu, DSt, Ds, HS, Ha, IC, In, LA, Ls, Me, NAG, Ord, PO, Pu, Rc, Shi, Sta, Tu, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 201

**Selected features:** selected = {AA, AC, ASta, Bd, Cas, Ches, DBu, DSt, HS, ICu, In, Ls, Me, NAG, Np, Ord, PO, SCh, Shi, Sil, StS, Tbc, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 68/72 = 94.4% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 113/119 = 95.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 100/111 = 90.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 202

**Selected features:** selected = {AA, ACh, B, BluB, CBu, Ches, DSt, Go, HS, IC, ICu, In, Ls, Me, Ord, PO, Rc, SSta, Shi, StS, Sta, Tbc, Tu, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 203

**Selected features:** selected = {ACh, ASt, BluB, CBu, CSta, Cuf, DC, DCu, HS, In, Me, NAG, Ord, PO, Rr, Sc2b, Shi, So, Ss, StS, Sta, Suc, TA, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 204

**Selected features:** selected = {AC, BAB, BB, CBu, CCh, Ches, DSt, Gr, HS, ICu, In, Ls, Me, Np, Ord, PO, R1b, Rc, SSta, Shi, StS, T, TA, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 104/120 = 86.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 90/112 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 205

**Selected features:** selected = {AA, ACh, ASt, ASta, Bd, Cuf, DBu, DC, Go, HS, In, Ls, Me, Np, Ord, PO, SS, Saf, Sc1b, Shi, UnT, YD}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 58/67 = 86.6% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 81/96 = 84.4% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 52/57 = 91.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 75/93 = 80.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 206

**Selected features:** selected = {AA, AS, CCh, CSta, Ches, DBu, Gr, HS, Ha, IC, ICu, In, Kc, Me, ODI, Ord, PO, Shi, Ss, Sta, Tbc, UnT}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 59/67 = 88.1% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 82/96 = 85.4% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 74/93 = 79.6% | 92/93 = 98.9% | 93/93 = 100.0% | 76/93 = 81.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 207

**Selected features:** selected = {AA, AC, AS, BB, Bro, CBu, Ches, DSt, Ds, HS, ICu, In, Kc, Me, NAG, Ord, PO, Rr, SCh, SSta, Shi, Ss, Sta, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 104/120 = 86.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 90/112 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 208

**Selected features:** selected = {ACh, ASta, Bd, Ches, DBu, Gr, HS, In, Jo, LA, Me, Np, OCu, ODI, Ord, PO, R1b, SS, Shi, Ss, Tu}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 61/63 = 96.8% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 79/81 = 97.5% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 55/56 = 98.2% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 70/80 = 87.5% | 80/80 = 100.0% | 80/80 = 100.0% | 77/80 = 96.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 209

**Selected features:** selected = {AC, ASta, BAB, CBu, Ches, DSt, HS, In, Me, Ord, PO, R1b, Rc, Rei, Rr, SCh, Shi, Ss, StS, Sta, TA, UnT, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 210

**Selected features:** selected = {AC, ACh, AS, ASt, BAB, BB, CBu, CSta, Cuf, Ds, HS, In, Ls, Me, NK, Ord, PO, Reg, Rr, Shi, Sta, TA, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 97/97 = 100.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 91/94 = 96.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 211

**Selected features:** selected = {AC, ACh, ASta, CBu, Cuf, DSt, HS, In, Kc, L, Ls, Me, NAG, Ord, PO, Pu, SS, Shi, Sta, TA, Tbc, UnT, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 212

**Selected features:** selected = {ASt, Bd, BluB, CBu, Ches, HS, IC, In, Me, Np, Ord, PO, Pu, R1b, Rei, SCh, SSta, Shi, Ss, StS, TA, Tu, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 213

**Selected features:** selected = {AA, AS, BB, CBu, CCh, CSta, Ches, Cop, DCu, DSt, Ds, HS, In, Ls, Mart, Me, Np, ODI, Ord, PO, Shi, Tu, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 214

**Selected features:** selected = {BAB, BB, CBu, CSta, Cuf, DSt, Ds, HS, In, Ka, LA, LC, Me, NK, Np, Ord, PO, SCh, Shi, Ss, StS, UnT}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 215

**Selected features:** selected = {AA, BB, CBu, Ches, Cop, DCu, Ds, HS, IC, In, Kay, Me, ODI, Ord, PO, SCh, SSta, Shi, Ss, StS, Sta, Suc, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 216

**Selected features:** selected = {ACh, BAB, BB, CBu, Ches, D, DSt, Ds, Go, HS, IC, In, Me, NK, Np, Ord, PO, SSta, Shi, Ss, StS, TA, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 217

**Selected features:** selected = {ACh, BB, Bd, CBu, CSta, Cuf, DC, Ds, Go, HS, ICu, In, LA, Marv, Me, Ord, PO, SS, Shi, Ss, Sta, UnT, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 89/97 = 91.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 82/94 = 87.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 218

**Selected features:** selected = {AA, AC, ACh, ACu, CBu, CSta, Ches, DSt, Gr, HS, In, Ja, Me, Np, Ord, PO, R1b, SS, Shi, Ss, Tu, WB, Wc, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 219

**Selected features:** selected = {ACh, BluB, CBu, CSta, Cuf, DCu, DSt, HS, In, MG, Mau, Me, NAG, Np, Ord, PO, Rc, SS, Sc2b, Shi, Ss, TA, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 220

**Selected features:** selected = {AC, ACh, CBu, CSta, Ches, DCu, Ds, HS, In, Kc, LG, Ls, Me, Np, OCu, ODI, Ord, PO, RB, SS, Shi, TA, UnT, Val}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 221

**Selected features:** selected = {AC, ACu, ASt, BAB, Bla, CSta, Ches, DBu, Ds, HS, In, LA, Me, Ord, PO, SCh, SS, Shi, Ss, Sta, Tu, Vic, Wc}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 110/119 = 92.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 95/111 = 85.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 222

**Selected features:** selected = {AS, BluB, CBu, CCh, Cas, Cuf, DC, DSt, HS, ICu, In, Kc, LA, LC, Ls, Me, Np, ODI, Ord, PO, SSta, Shi, Tbc, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 223

**Selected features:** selected = {ACu, ASta, BAB, Bd, CBu, CCh, Ches, Cra, DC, Ds, HS, In, LA, Ls, Me, Ord, PO, RB, Rr, Shi, StS, Sta, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 86/94 = 91.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 224

**Selected features:** selected = {BluB, CBu, CCh, CSta, Cuf, DB, DC, HS, In, Kc, Li, Ls, Me, OCu, ODI, Ord, PO, SS, Sc1b, Shi, Sta, TA, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 225

**Selected features:** selected = {AA, ACh, ACu, ASt, Bd, Cuf, DBu, DC, HS, In, LG, Marv, Me, Np, ODI, Ord, PO, R1b, SSta, Shi, Ss, StS, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 111/119 = 93.3% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 95/111 = 85.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 226

**Selected features:** selected = {ACh, BAB, BB, CBu, Ches, DC, HS, ICu, In, LA, Me, OCu, Ord, PO, Pu, Rc, SS, SSta, Sc1b, Shi, Ss, Sta, Sw, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 98/112 = 87.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 227

**Selected features:** selected = {AC, ACh, AS, Bla, CBu, Cuf, HS, ICu, In, Kc, LA, Ls, Me, Nu, OCu, ODI, Ord, PO, R1b, RB, SSta, Shi, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 228

**Selected features:** selected = {AA, Ches, DBu, HS, In, Ls, MG, Me, NAG, NK, Np, Nu, Ord, PO, SCh, SSta, Shi, StS, Tbc, Tu}

**Repaired FTS:** 56 states, 60 transitions (60 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 60 | 0/60 = 0.0% | 0/60 = 0.0% | 58/60 = 96.7% | 60/60 = 100.0% | 60/60 = 100.0% | 58/60 = 96.7% |
| ActionExchange | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 70/72 = 97.2% | 72/72 = 100.0% | 72/72 = 100.0% | 70/72 = 97.2% |
| StateMissing | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 55/55 = 100.0% | 55/55 = 100.0% | 55/55 = 100.0% | 54/55 = 98.2% |
| TransitionDestinationExchange | 72 | 1/72 = 1.4% | 0/71 = 0.0% | 66/71 = 93.0% | 71/71 = 100.0% | 71/71 = 100.0% | 68/71 = 95.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 229

**Selected features:** selected = {AC, ACh, AS, CBu, Cop, Cuf, DCu, Ds, HS, In, LA, Ls, Me, Ord, PO, Ri, SSta, Shi, Sta, UnT, WB, Wc, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 230

**Selected features:** selected = {AA, AC, BAB, CBu, CCh, CSta, Ches, DCu, HS, In, Ls, MG, Me, Ord, PO, R1b, Rc, Rk, Shi, StS, Sta, Tu, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 231

**Selected features:** selected = {AS, CBu, CCh, CSta, Cuf, Ds, HS, IC, In, Kc, LA, LB, Ls, Me, NAG, Np, OCu, Ord, PO, Set, Shi, UnT, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 89/94 = 94.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 232

**Selected features:** selected = {ACu, BB, CBu, CCh, Ches, DSt, HS, In, Ls, Me, My, NAG, Ord, PO, SSta, Sc2b, Shi, Sil, StS, Sta, Suc, TA, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 84/94 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 233

**Selected features:** selected = {ACu, ASt, ASta, Bd, CBu, CCh, Ch, Ches, DC, DG, Ds, HS, In, LA, Ls, Me, Np, ODI, Ord, PO, Shi, StS, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 234

**Selected features:** selected = {ACh, ACu, BAB, BluB, CBu, Co, Cuf, DB, HS, In, LA, Me, Ord, PO, SS, SSta, Sc2b, Shi, Ss, Sta, Suc, UnT}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 235

**Selected features:** selected = {AA, AC, ASt, Al, Cuf, DBu, Ds, HS, In, LG, Me, Np, Ord, PO, SCh, SS, SSta, Shi, Ss, Suc, UnT, YD}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 66/67 = 98.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 95/96 = 99.0% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 91/93 = 97.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 236

**Selected features:** selected = {AA, AC, ASta, CBu, CCh, Ches, Ds, HS, In, Me, Mid, NAG, Np, OCu, Ord, PO, Rr, SS, Shi, Ss, UnT, WB, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 237

**Selected features:** selected = {ACu, ASt, CBu, Cuf, HS, IC, In, LB, Lev, Ls, Me, Np, ODI, Ord, PO, RB, Rc, SCh, SS, SSta, Sc1b, Shi, TA, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 109/120 = 90.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 238

**Selected features:** selected = {Bla, Ches, DBu, DCu, Ds, HS, In, Ls, Me, ODI, Ord, PO, Rc, SCh, SS, SSta, Shi, Sta, TA, Tu, Vic}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 56/63 = 88.9% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 72/81 = 88.9% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 52/56 = 92.9% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 71/80 = 88.8% | 80/80 = 100.0% | 80/80 = 100.0% | 68/80 = 85.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 239

**Selected features:** selected = {ASt, ASta, CBu, Cuf, HS, Holl, In, Ls, MB, Me, NAG, Ord, PO, RB, SCh, Sc2b, Shi, StS, Sta, Suc, TA, UnT}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 62/64 = 96.9% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 80/82 = 97.6% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 78/81 = 96.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 240

**Selected features:** selected = {AC, CBu, CCh, Ches, Cop, Ds, HS, ICu, In, Me, NAG, Ord, PO, RB, SSta, Shi, Ss, StS, Sta, Suc, TA, UnT, Z}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 241

**Selected features:** selected = {BluB, Bri, CBu, CCh, CSta, Cuf, DC, DCu, Ds, HS, In, Ls, MB, Me, Np, ODI, Ord, PO, SS, Shi, TA, Tu, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 89/97 = 91.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 82/94 = 87.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 242

**Selected features:** selected = {AC, ACh, ASta, Bd, CBu, Ches, Ds, Go, HS, ICu, In, Me, NAG, Na, OCu, Ord, PO, SS, Shi, Ss, Sta, TA, Tu, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 243

**Selected features:** selected = {ASt, BluB, CBu, CCh, CSta, Cuf, DC, Ds, HS, ICu, In, LA, Ls, MG, Me, N, Np, Ord, PO, Rc, SS, Shi, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 111/120 = 92.5% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 244

**Selected features:** selected = {AC, ACh, ASt, BAB, CBu, CSta, Cuf, HS, I, In, LA, MB, Me, Np, Ord, PO, R1b, Shi, Ss, StS, Tu, WB, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 97/97 = 100.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 91/94 = 96.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 245

**Selected features:** selected = {ACu, AS, BB, CBu, CCh, CSta, Cr, Cuf, HS, IC, In, LA, Ls, MB, Me, Np, Ord, PO, Sc1b, Shi, UnT, Wc, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 94/97 = 96.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 246

**Selected features:** selected = {AS, ASta, B, BAB, BB, CBu, Cuf, DCu, DSt, HS, In, LA, LG, Ls, Me, Ord, PO, SCh, Sc2b, Shi, Sta, UnT, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 247

**Selected features:** selected = {ACh, ASta, Bla, CBu, Cuf, DC, DCu, HS, In, Ls, Me, Np, OCu, ODI, Ord, PO, SS, Shi, TA, Tbc, UnT, Ve, WB, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 98/112 = 87.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 248

**Selected features:** selected = {ACu, AS, ASta, CCh, Ches, DB, DBu, HS, In, Kc, LA, Me, NAG, Ord, PO, Sc2b, Shi, Ss, Sta, Tu, Vic}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 59/63 = 93.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 77/81 = 95.1% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 55/56 = 98.2% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 70/80 = 87.5% | 80/80 = 100.0% | 80/80 = 100.0% | 73/80 = 91.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 249

**Selected features:** selected = {ASta, CCh, Ches, DBu, HS, ICu, In, LA, MB, Me, NAG, NK, Ord, PO, Shi, Ss, StS, Sta, Tbc, Tu, Ya}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 56/63 = 88.9% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 72/81 = 88.9% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 52/56 = 92.9% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 71/80 = 88.8% | 80/80 = 100.0% | 80/80 = 100.0% | 68/80 = 85.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 250

**Selected features:** selected = {AA, ACh, ACu, BAB, Bla, CBu, Cuf, HS, In, Me, NK, Np, Or, Ord, PO, RB, SS, SSta, Sc2b, Shi, Ss, Tu}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 251

**Selected features:** selected = {AA, AC, ACh, AS, BB, CBu, CSta, Cuf, DCu, HS, In, Ls, MG, Me, NAG, OCu, Ord, PO, Ru, Shi, Sta, Tbc, Tu, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 252

**Selected features:** selected = {AS, Bd, Bev, Cuf, DBu, DCu, Ds, HS, IC, In, LB, Ls, Me, NAG, Ord, PO, SCh, SSta, Shi, Sta, TA, Tu}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 59/67 = 88.1% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 82/96 = 85.4% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 74/93 = 79.6% | 92/93 = 98.9% | 93/93 = 100.0% | 76/93 = 81.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 253

**Selected features:** selected = {AC, AS, ASt, ASta, BB, CBu, Ches, HS, In, Ls, Me, Np, Ord, PO, R1b, Rr, SCh, Shi, TA, UnT, Va, Wc, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 97/97 = 100.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 91/94 = 96.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 254

**Selected features:** selected = {AA, AC, ACu, BAB, BB, CBu, CSta, Cuf, DSt, F, HS, In, Ls, MG, Me, NK, Ord, PO, SCh, Sc1b, Shi, StS, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 255

**Selected features:** selected = {AA, ASta, BB, Bd, CBu, Ches, DC, DCu, HS, I, In, LB, Ls, Me, NAG, Np, Ord, PO, SCh, Sc1b, Shi, StS, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 89/97 = 91.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 82/94 = 87.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 256

**Selected features:** selected = {ACh, ASt, CBu, CSta, Cop, Cuf, Ds, HS, IC, In, Kc, LA, Me, N, NAG, Np, Ord, PO, RB, SS, Shi, Ss, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 257

**Selected features:** selected = {AA, AC, ACh, ASt, BB, CBu, Cuf, HS, In, Kc, LB, Mcd, Me, NAG, Ord, PO, SSta, Sc1b, Shi, Ss, StS, Sta, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 97/97 = 100.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 91/94 = 96.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 258

**Selected features:** selected = {AA, ACh, ACu, CBu, Cuf, DC, HS, In, LG, Me, OCu, Ord, PO, RB, Ri, SS, SSta, Sc2b, Shi, Ss, Sta, Suc, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 259

**Selected features:** selected = {AA, ACu, ASta, BAB, CBu, CCh, Cuf, E, HS, IC, In, Ls, MB, Me, Np, Ord, PO, Sc1b, Shi, StS, UnT, WB, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 94/97 = 96.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 260

**Selected features:** selected = {AA, BluB, CBu, CSta, Ches, HS, ICu, In, L, Ls, Me, OCu, ODI, Ord, PO, SCh, SS, Shi, Sil, Sta, Suc, Tbc, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 261

**Selected features:** selected = {AC, AS, BB, CBu, CCh, Ches, DG, Eli, HS, In, LA, Me, NAG, Ord, PO, SSta, Sc1b, Shi, Ss, Sta, Tu, Wc}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 262

**Selected features:** selected = {ACh, AS, BluB, CBu, Ches, Cr, DCu, Go, HS, IC, In, Me, Np, OCu, Ord, PO, SSta, Sc1b, Shi, Ss, TA, Tu, Wc, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 97/112 = 86.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 263

**Selected features:** selected = {AA, AC, ACh, ACu, AS, ASt, Bd, BluB, CBu, CSta, Ches, HS, Holl, In, Ls, Me, ODI, Ord, PO, Rr, Sc2b, Shi, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 264

**Selected features:** selected = {AA, AS, BB, CBu, CSta, Cuf, DB, DC, DCu, DSt, HS, He, In, Me, NAG, NK, Np, Ord, PO, SCh, Sc2b, Shi, Ss, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 265

**Selected features:** selected = {ASt, BB, CBu, CSta, Cuf, El, HS, In, Me, NAG, Ord, PO, Rc, SCh, Sc1b, Shi, Sil, Ss, StS, Sta, TA, Tu}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 62/64 = 96.9% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 80/82 = 97.6% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 78/81 = 96.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 266

**Selected features:** selected = {ACu, ASt, ASta, CBu, Ches, DC, Gr, HS, In, LA, Ls, Me, NK, Np, Ord, PO, RB, Ree, SCh, Sc1b, Shi, StS, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 267

**Selected features:** selected = {ACu, ASt, ASta, Bd, CBu, CCh, Ches, DC, HS, In, LA, Ls, Man, Me, Np, ODI, Ord, PO, R1b, RB, Shi, Sil, StS, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 268

**Selected features:** selected = {AS, ASta, B, CCh, Cuf, DBu, Go, HS, IC, ICu, In, Me, NAG, Np, OCu, Ord, PO, Rc, Sc2b, Shi, Ss, TA, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 69/72 = 95.8% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 116/119 = 97.5% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 103/111 = 92.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 269

**Selected features:** selected = {AC, CCh, Cuf, DBu, DCu, Gl, HS, In, LA, MB, Me, NK, Ord, PO, SSta, Shi, Ss, StS, Sta, Tbc, UnT, YD}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 62/67 = 92.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 89/96 = 92.7% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 77/93 = 82.8% | 92/93 = 98.9% | 93/93 = 100.0% | 83/93 = 89.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 270

**Selected features:** selected = {ASta, Ca, Cuf, DBu, DSt, HS, In, LP, Me, NAG, NK, Ord, PO, SCh, Sc2b, Shi, Ss, StS, Sta, TA, Tu}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 56/63 = 88.9% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 72/81 = 88.9% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 52/56 = 92.9% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 70/80 = 87.5% | 80/80 = 100.0% | 80/80 = 100.0% | 68/80 = 85.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 271

**Selected features:** selected = {ACu, AS, BB, CBu, Cuf, HS, In, Ls, Me, NAG, NK, Np, OCu, Ord, PO, Rei, SCh, SSta, Sc2b, Shi, Sil, TA, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 272

**Selected features:** selected = {ASta, Cuf, DBu, Gr, HS, ICu, In, LA, Ls, Me, ODI, Ord, PO, SCh, Sc1b, Shi, StS, Sta, Tap, UnT, Wc}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 56/63 = 88.9% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 72/81 = 88.9% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 52/56 = 92.9% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 71/80 = 88.8% | 80/80 = 100.0% | 80/80 = 100.0% | 68/80 = 85.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 273

**Selected features:** selected = {AA, AC, ASta, BB, CBu, Ches, DCu, DSt, HS, In, Ja, Ls, Me, Ord, PO, Pu, Rc, SCh, Sc1b, Shi, StS, Sta, Tu, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 104/120 = 86.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 90/112 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 274

**Selected features:** selected = {AA, ACh, ASta, BluB, Bri, CBu, Cuf, DCu, Gr, HS, In, Me, NAG, Np, OCu, Ord, PO, SS, Sc1b, Shi, Ss, Suc, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 275

**Selected features:** selected = {AC, Bev, CCh, Cuf, DBu, HS, ICu, In, LA, Ls, Me, Ord, PO, Pu, R1b, SSta, Shi, StS, Sta, Tu, Wc, YD}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 62/67 = 92.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 89/96 = 92.7% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 77/93 = 82.8% | 92/93 = 98.9% | 93/93 = 100.0% | 83/93 = 89.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 276

**Selected features:** selected = {AA, ASt, BAB, CBu, CSta, Ches, DC, DCu, HS, In, Me, Np, Ord, PO, RB, Rr, SCh, Sc2b, Shi, Ss, StS, Suc, UnT, Y}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 277

**Selected features:** selected = {ACu, By, CBu, Ches, DC, DSt, HS, In, LA, Ls, MG, Me, NK, Ord, PO, SCh, SSta, Shi, StS, Sta, Tbc, UnT, WB, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 100/120 = 83.3% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 278

**Selected features:** selected = {ACu, ASta, BAB, CBu, CCh, Cuf, DC, HS, In, Kayc, LC, Ls, Me, Np, OCu, Ord, PO, R1b, SS, Shi, TA, Tu, WB, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 279

**Selected features:** selected = {ACh, Bd, CBu, Cop, Cuf, DC, HS, In, LA, Man, Me, Np, OCu, ODI, Ord, PO, RB, SSta, Shi, Ss, StS, Tbc, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 280

**Selected features:** selected = {AC, Bd, CBu, CSta, Ches, DSt, HS, ICu, In, LA, Ls, Me, ODI, Ord, PO, RB, Reg, SCh, SS, Sc2b, Shi, Sil, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 104/120 = 86.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 90/112 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 281

**Selected features:** selected = {ACh, CBu, Ches, HS, IC, ICu, In, LA, Ls, Me, OCu, Ord, PO, Rei, SSta, Sc2b, Shi, Sil, StS, Sta, UnT, WB, Wc, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 97/112 = 86.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 282

**Selected features:** selected = {Ches, DBu, Ds, Gr, HS, He, ICu, In, Me, NAG, Np, OCu, Ord, PO, SCh, SS, SSta, Shi, Ss, Suc, TA, UnT}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 65/67 = 97.0% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 95/96 = 99.0% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 89/93 = 95.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 283

**Selected features:** selected = {BAB, BB, Be, CBu, CCh, CSta, Ches, HS, ICu, In, Me, Ord, PO, Rr, Shi, Ss, StS, Sta, TA, Tbc, UnT, Wc}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 284

**Selected features:** selected = {AC, AS, ASt, Bd, CBu, Cuf, HS, I, In, LA, Ls, Me, Np, Ord, PO, R1b, Rr, SCh, SSta, Shi, Tu, WB, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 97/97 = 100.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 91/94 = 96.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 285

**Selected features:** selected = {ACh, ACu, AS, CBu, Cuf, DC, HS, In, LA, LC, Ls, Ma, Me, Np, OCu, Ord, PO, SSta, Shi, Tbc, Tu, WB, Wc, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 286

**Selected features:** selected = {ACh, BluB, CBu, Ches, HS, Holl, IC, In, LA, LP, Me, NAG, Np, OCu, Ord, PO, SSta, Shi, Ss, StS, Suc, Tbc, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 89/94 = 94.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 287

**Selected features:** selected = {AS, ASt, CBu, Ches, DG, HS, IC, In, Me, ODI, Ord, P, PO, SCh, SSta, Shi, Ss, Sta, Suc, TA, Tbc, UnT, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 288

**Selected features:** selected = {ASta, CBu, CCh, Cuf, DC, Ell, HS, In, LA, Me, NAG, Np, OCu, Ord, PO, Pu, RB, Sc1b, Shi, Ss, StS, Suc, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 289

**Selected features:** selected = {AA, AC, AS, ASta, Ches, DBu, HS, ICu, In, Ke, LB, Me, NAG, Np, Ord, PO, R1b, Rc, SCh, Shi, Ss, Tu}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 62/67 = 92.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 89/96 = 92.7% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 77/93 = 82.8% | 92/93 = 98.9% | 93/93 = 100.0% | 83/93 = 89.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 290

**Selected features:** selected = {ACh, ACu, ASt, ASta, Bea, BluB, CBu, Cop, Cuf, HS, IC, In, Ls, Me, NK, ODI, Ord, PO, R1b, Shi, StS, Sta, TA, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 109/120 = 90.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 291

**Selected features:** selected = {AS, CSta, Cuf, DBu, DSt, Go, HS, IC, ICu, In, Kc, Mcg, Me, Ord, PO, SCh, Shi, Ss, Sta, TA, Tbc, Tu, YD}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 68/72 = 94.4% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 109/119 = 91.6% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 98/111 = 88.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 292

**Selected features:** selected = {AA, ASta, BAB, BB, CBu, CCh, Ches, DC, DCu, DSt, HS, In, MG, Me, Ord, PO, Pe, Sc1b, Shi, Ss, StS, Sta, UnT, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 293

**Selected features:** selected = {AA, ASta, Bd, Ches, DBu, DSt, HS, In, Ls, MG, Me, Np, ODI, Ord, PO, SCh, Sc2b, Shi, StS, Tap, Tu}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 56/63 = 88.9% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 72/81 = 88.9% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 52/56 = 92.9% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 70/80 = 87.5% | 80/80 = 100.0% | 80/80 = 100.0% | 68/80 = 85.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 294

**Selected features:** selected = {AA, ACu, ASt, CBu, Cuf, DC, Gr, HS, In, Me, NAG, Np, Ord, PO, Rc, SCh, SS, SSta, Sc, Shi, Ss, Tbc, Tu, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 295

**Selected features:** selected = {AA, AC, AS, ASta, Bd, BluB, CBu, Ches, DCu, HS, In, Me, ODI, Ord, PO, SCh, Sc1b, Set, Shi, Sil, Ss, Sta, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 296

**Selected features:** selected = {AS, ASta, Bl, Cop, Cuf, DBu, DC, DCu, DSt, HS, In, LA, Me, NAG, Ord, PO, SCh, Sc1b, Shi, Ss, Sta, UnT, Wc}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 63/72 = 87.5% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 98/119 = 82.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 54/58 = 93.1% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 79/111 = 71.2% | 108/111 = 97.3% | 111/111 = 100.0% | 87/111 = 78.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 297

**Selected features:** selected = {AA, ACu, AS, ASt, CCh, Cop, Cuf, DBu, Ds, HS, In, Me, ODI, Ord, PO, SSta, Shi, Ss, Sta, Tu, Val, Wc}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 60/67 = 89.6% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 83/96 = 86.5% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 78/93 = 83.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 298

**Selected features:** selected = {BluB, CBu, Cuf, DSt, HS, IC, ICu, In, Jay, Kc, LA, LB, Me, ODI, Ord, PO, SCh, SS, SSta, Sc2b, Shi, Ss, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 299

**Selected features:** selected = {AA, CBu, CCh, Car, Cop, Cuf, DCu, HS, In, Kc, Ls, Me, NAG, Ord, PO, SS, SSta, Sc1b, Shi, Sta, Tu, WB}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 300

**Selected features:** selected = {AA, AS, CCh, CSta, Ches, DBu, DCu, Ds, HS, Hut, IC, In, LP, Me, NK, Np, ODI, Ord, PO, Shi, Ss, Tu}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 59/67 = 88.1% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 82/96 = 85.4% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 74/93 = 79.6% | 92/93 = 98.9% | 93/93 = 100.0% | 76/93 = 81.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 301

**Selected features:** selected = {AS, ASt, ASta, CBu, Cuf, Ds, HS, IC, In, LA, LP, Me, NAG, Ord, PO, RB, Rc, Ree, SCh, Shi, Ss, Sta, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 302

**Selected features:** selected = {AA, AC, ACh, BluB, CBu, Cuf, Ds, HS, ICu, In, Kc, MG, Maj, Me, Np, Ord, PO, SSta, Shi, Ss, StS, UnT, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 303

**Selected features:** selected = {AA, BB, CBu, CCh, CSta, Cop, Cuf, DC, DSt, Ds, HS, Hu, ICu, In, Ls, Me, Np, Ord, PO, Rc, Shi, StS, Tu, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 304

**Selected features:** selected = {AC, ACu, ASt, ASta, Bla, BluB, Br, CBu, CCh, Cuf, Ds, HS, In, LA, Ls, Me, Ord, PO, SS, Shi, Sta, Suc, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 305

**Selected features:** selected = {AA, ACh, Bd, CBu, CSta, Ches, DC, HS, In, Me, OCu, Ord, PO, Rr, SS, Sc1b, Se, Shi, Ss, Sta, UnT, WB, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 306

**Selected features:** selected = {ASta, BAB, BluB, CBu, Calh, Ches, Cop, DCu, Ds, HS, In, LA, Ls, Me, Np, OCu, Ord, PO, Rc, SCh, Shi, StS, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 307

**Selected features:** selected = {AC, ACh, ASt, BAB, Bd, CBu, Ches, DCu, Ds, HS, In, Me, Np, Ord, PO, Pu, RB, SSta, Shi, Ss, StS, TA, Tap, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 96/112 = 85.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 308

**Selected features:** selected = {AA, AC, BAB, BluB, CBu, CCh, Cuf, DCu, Ds, HS, In, MB, Me, NK, Np, Ord, PO, SSta, Shi, Ss, StS, Tu, Wi}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 309

**Selected features:** selected = {AA, AC, ACh, AS, Bla, CSta, Cuf, DBu, DCu, HS, In, Ls, Me, Np, OCu, Ord, PO, Reg, Shi, Tbc, UnT, Wc, YD}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 68/72 = 94.4% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 109/119 = 91.6% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 96/111 = 86.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 310

**Selected features:** selected = {AA, BB, CBu, CCh, CSta, Ches, DC, DSt, HS, ICu, In, Ls, M, MB, Me, NK, Np, Ord, PO, Shi, StS, Tbc, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 311

**Selected features:** selected = {AC, ACh, CBu, CSta, Ches, DCu, HS, In, MB, Mar, Me, Np, OCu, Ord, PO, RB, Rc, Shi, Ss, StS, TA, Tbc, Tu, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 312

**Selected features:** selected = {AA, AC, ACu, ASt, ASta, BAB, BB, Br, CBu, CCh, Ches, HS, In, Me, Ord, PO, Pu, Shi, Ss, StS, Sta, Suc, Tbc, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 313

**Selected features:** selected = {ASta, BB, Bd, CBu, Ca, Ches, Cop, DCu, DSt, HS, IC, In, LA, Ls, Me, NAG, Np, Ord, PO, SCh, Sc1b, Shi, StS, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 314

**Selected features:** selected = {ASt, ASta, CBu, CCh, Ches, DC, DCu, Ds, HS, In, LC, Ls, Me, Np, Ord, PO, RB, SS, Shi, Suc, TA, UnT, Vic, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 315

**Selected features:** selected = {AA, AC, ACh, ASt, BB, CBu, Ches, DCu, Ev, HS, In, LB, Me, NK, ODI, Ord, PO, SS, SSta, Sc2b, Shi, Ss, Sta, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 96/112 = 85.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 316

**Selected features:** selected = {AA, ACh, Bd, Bla, CBu, CSta, Cuf, DSt, HS, Har, IC, In, Me, NAG, Np, Ord, PO, RB, Sc2b, Shi, Ss, StS, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 317

**Selected features:** selected = {AC, ACu, ASta, Bla, Ches, DBu, DSt, HS, In, LA, Me, Np, ODI, Ord, PO, Rc, SCh, Sc1b, Se, Shi, Ss, StS, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 107/119 = 89.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 93/111 = 83.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 318

**Selected features:** selected = {ASta, BAB, BB, CBu, CCh, Cuf, HS, IC, ICu, In, LA, Ls, Me, NK, Np, OCu, Ord, PO, Pu, Re, Sc2b, Shi, StS, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 97/112 = 86.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 319

**Selected features:** selected = {AA, AC, CBu, Calh, Ches, HS, ICu, In, Ls, Me, Np, ODI, Ord, PO, Pu, RB, Rc, SCh, SS, SSta, Sc2b, Shi, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 320

**Selected features:** selected = {AC, CBu, CSta, Ches, DCu, Ds, HS, In, Ls, Me, N, NK, Np, OCu, ODI, Ord, PO, RB, Rr, SCh, Shi, StS, TA, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 321

**Selected features:** selected = {AC, BluB, CBu, CCh, Ches, DCu, Ds, HS, In, Kc, LA, LG, Me, NAG, Np, OCu, Ord, PO, SS, SSta, Sa, Shi, Ss, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 322

**Selected features:** selected = {Bd, BluB, CBu, CSta, Ches, HS, ICu, In, LA, Ls, Me, NAG, Np, Ord, PO, SCh, Shi, Sil, StS, Tbc, Tu, We}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 323

**Selected features:** selected = {CBu, Ches, DB, DCu, DSt, HS, In, Ls, Mcc, Me, Np, Ord, PO, SCh, SS, SSta, Sc1b, Shi, Suc, TA, UnT, WB, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 324

**Selected features:** selected = {AA, ACu, ASt, ASta, BluB, CBu, CCh, Cuf, DG, Ds, HS, In, Ls, Me, NAG, NK, Np, Ord, PO, Shi, StS, Ti, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 88/94 = 93.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 325

**Selected features:** selected = {AA, AS, ASt, Ak, BAB, BluB, CBu, CSta, Ches, DCu, HS, IC, In, Kc, MG, Me, Np, Ord, PO, R1b, SCh, Shi, Ss, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 113/120 = 94.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 326

**Selected features:** selected = {ACh, AS, CSta, Ches, DBu, HS, ICu, In, Kc, LA, LB, Ls, Mcg, Me, Np, OCu, ODI, Ord, PO, R1b, Shi, Tu}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 65/67 = 97.0% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 95/96 = 99.0% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 89/93 = 95.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 327

**Selected features:** selected = {BB, By, CBu, Ches, DSt, Ds, HS, ICu, In, LA, LB, Ls, Me, ODI, Ord, PO, SCh, SS, SSta, Shi, Sta, Tu, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 328

**Selected features:** selected = {BAB, CBu, CCh, Ches, DC, DSt, HS, In, Me, Np, Ord, PO, R1b, RB, SSta, Shi, Sil, Ss, StS, TA, Tu, Van, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 84/94 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 329

**Selected features:** selected = {AC, AS, ASt, BAB, CBu, CCh, Cuf, HS, ICu, In, Kc, LA, LP, Ls, Mcm, Me, Ord, PO, SSta, Sc2b, Shi, Sta, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 330

**Selected features:** selected = {AC, ACh, ASt, BAB, BB, CBu, CSta, Ches, Ds, Ew, HS, ICu, In, Ls, Me, Np, Ord, PO, Rr, Shi, StS, Suc, TA, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 331

**Selected features:** selected = {AA, ACu, AS, CCh, Cuf, DBu, DSt, Ds, HS, IC, In, Kc, MB, Me, NAG, Np, Ord, PO, SSta, Shi, Ss, Tu, Wi}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 99/119 = 83.2% | 119/119 = 100.0% | 119/119 = 100.0% | 107/119 = 89.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 93/111 = 83.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 332

**Selected features:** selected = {AC, ACh, CBu, CSta, Cuf, Gl, HS, ICu, In, LC, Ls, Me, NAG, NK, Ord, PO, R1b, SS, Shi, Sta, TA, Tu, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 333

**Selected features:** selected = {AA, AC, ACh, BluB, CBu, Ches, Ds, HS, ICu, In, Ls, MG, Me, NAG, Np, O, OCu, Ord, PO, Rc, SSta, Shi, StS, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 334

**Selected features:** selected = {At, BAB, Bla, CBu, CCh, CSta, Ches, DC, DSt, HS, ICu, In, Me, NK, Np, Ord, PO, SS, Shi, Ss, TA, Tbc, Tu, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 335

**Selected features:** selected = {AA, ASt, ASta, Bla, CBu, Ches, HS, ICu, In, Mcc, Me, Np, Ord, PO, Rc, SCh, SS, Sc2b, Shi, Ss, UnT, WB, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 91/97 = 93.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 336

**Selected features:** selected = {AA, ACh, AS, CBu, Che, Cuf, DCu, HS, IC, In, Me, OCu, ODI, Ord, PO, Pu, Rc, SSta, Sc2b, Shi, Ss, Sta, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 97/112 = 86.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 337

**Selected features:** selected = {AA, ACu, AS, BAB, CBu, CSta, Ches, DSt, HS, In, LG, Ls, Me, Ord, PO, SCh, Sc2b, Sh, Shi, Sta, UnT, WB, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 84/94 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 338

**Selected features:** selected = {ACu, ASt, BAB, CBu, CCh, CSta, Cuf, Go, HS, In, LA, Ls, Me, Np, Ord, PO, Pe, R1b, SS, Shi, UnT, WB, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 88/94 = 93.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 339

**Selected features:** selected = {AS, Alt, BluB, CBu, Ches, DSt, HS, In, Me, NAG, Ord, PO, Pu, Rc, SCh, SSta, Sc2b, Shi, Ss, Sta, TA, Tu}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 340

**Selected features:** selected = {AS, ASt, ASta, BB, CBu, CCh, Ches, DC, HS, Hal, In, LB, Ls, Me, Np, ODI, Ord, PO, Rc, Sc1b, Shi, TA, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 86/94 = 91.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 341

**Selected features:** selected = {CBu, Ches, Ds, HS, IC, ICu, In, Ls, Me, NK, ODI, Ord, PO, RB, Ri, Rr, SCh, SSta, Shi, StS, Sta, TA, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 342

**Selected features:** selected = {AC, ACh, BAB, CSta, Ches, Cop, DBu, DCu, HS, In, Jo, LA, Ls, Me, NK, Np, OCu, Ord, PO, SS, Sc1b, Shi, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 68/72 = 94.4% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 109/119 = 91.6% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 96/111 = 86.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 343

**Selected features:** selected = {ACh, AS, ASta, BB, CBu, Cuf, DC, HS, ICu, In, LA, LG, Mcd, Me, NAG, Ord, PO, Shi, Ss, Sta, Suc, Tbc, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 89/97 = 91.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 82/94 = 87.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 344

**Selected features:** selected = {ACh, AS, ASta, Ber, Ches, DBu, DC, Ds, Gr, HS, In, LA, Me, Np, OCu, Ord, PO, Shi, Ss, Suc, UnT, YD}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 58/67 = 86.6% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 81/96 = 84.4% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 52/57 = 91.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 75/93 = 80.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 345

**Selected features:** selected = {AA, ACu, AS, CCh, Cuf, D, DBu, Ds, HS, IC, In, Kc, LC, Ls, Me, NAG, Np, OCu, Ord, PO, SSta, Shi, UnT}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 67/72 = 93.1% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 109/119 = 91.6% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 95/111 = 85.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 346

**Selected features:** selected = {AA, BAB, BB, CBu, CCh, CSta, Ches, DC, DSt, HS, ICu, In, Ls, Me, Ord, PO, Rr, Sc2b, Shi, So, StS, Sta, Suc, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 347

**Selected features:** selected = {AS, BAB, CBu, CSta, Ches, DSt, Ds, HS, IC, In, LA, LP, Me, Ord, PO, RB, SCh, Shi, Ss, Sta, Suc, Sw, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 348

**Selected features:** selected = {AA, AC, ACh, ACu, Arb, Bd, Ches, DBu, HS, In, Ls, Me, Np, ODI, Ord, PO, Rr, SSta, Sc2b, Shi, StS, UnT}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 62/67 = 92.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 89/96 = 92.7% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 77/93 = 82.8% | 92/93 = 98.9% | 93/93 = 100.0% | 82/93 = 88.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 349

**Selected features:** selected = {AA, ACh, ACu, ASt, Bla, CBu, CSta, Ches, Ds, F, HS, In, Me, Ord, PO, Shi, Ss, StS, Sta, Tu, WB, Wc, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 88/94 = 93.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 350

**Selected features:** selected = {ASt, ASta, CCh, Cuf, DBu, HS, IC, ICu, In, Mart, Me, Np, Ord, PO, R1b, Shi, Sil, Ss, StS, Suc, TA, Tu, YD}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 62/72 = 86.1% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 108/119 = 90.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 95/111 = 85.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 351

**Selected features:** selected = {AA, AC, ACu, CCh, CSta, Ches, DBu, DSt, HS, In, Kc, LG, Ls, Me, Np, ODI, Ord, PO, Reu, Sc1b, Shi, StS, Tu}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 66/72 = 91.7% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 96/119 = 80.7% | 119/119 = 100.0% | 119/119 = 100.0% | 107/119 = 89.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 80/111 = 72.1% | 108/111 = 97.3% | 111/111 = 100.0% | 93/111 = 83.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 352

**Selected features:** selected = {AA, AS, ASt, CBu, Cuf, DC, Ds, HS, ICu, In, LC, Ls, Me, NAG, Ord, PO, Ry, SCh, SSta, Shi, Sta, Tu, WB, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 111/120 = 92.5% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 353

**Selected features:** selected = {AS, ASta, CBu, CCh, Cuf, DC, HS, ICu, In, Ja, LG, Ls, Me, NAG, Np, OCu, Ord, PO, R1b, RB, Shi, TA, Tu, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 98/112 = 87.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 354

**Selected features:** selected = {AC, ACu, ASt, Be, CBu, CCh, CSta, Ches, Gr, HS, In, LA, Me, Np, Ord, PO, SS, Sc2b, Shi, Ss, Suc, UnT, WB, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 355

**Selected features:** selected = {AA, BB, CBu, CSta, Ches, Ds, HS, IC, ICu, In, Kc, Me, N, NAG, OCu, Ord, PO, Rr, SCh, Shi, Ss, StS, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 97/112 = 86.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 356

**Selected features:** selected = {AA, ACh, ACu, ASta, BAB, CBu, Calh, Ches, Cop, DSt, HS, In, Ls, Me, Ord, PO, R1b, RB, Rc, SS, Shi, Sta, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 84/94 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 357

**Selected features:** selected = {AA, AS, ASta, BAB, BB, CBu, Cuf, DC, DCu, HS, In, LP, Ls, Me, Np, OCu, Ord, PO, Rei, SCh, Sc2b, Shi, Suc, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 98/112 = 87.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 358

**Selected features:** selected = {ACu, BAB, BluB, CBu, Ches, DSt, Ds, Go, HS, In, LA, Ls, Me, Np, Ord, PO, S, SCh, SSta, Shi, StS, UnT, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 84/94 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 359

**Selected features:** selected = {B, CBu, CCh, CSta, Ches, DSt, HS, In, Ls, MG, Me, ODI, Ord, PO, R1b, RB, Rc, SS, Shi, Sta, TA, Tu}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 360

**Selected features:** selected = {ASt, Bla, BluB, CBu, Ches, D, HS, IC, In, Ls, Me, NAG, Ord, PO, R1b, SCh, SS, SSta, Shi, Sta, TA, UnT, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 85/94 = 90.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 361

**Selected features:** selected = {AC, ACh, AS, ASt, BAB, BluB, CBu, Cuf, DCu, Ga, HS, In, MG, Me, NK, Ord, PO, SSta, Sc2b, Shi, Ss, Sta, TA, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 112/120 = 93.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 96/112 = 85.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 362

**Selected features:** selected = {AS, ASt, Bla, Ches, DBu, Ds, HS, In, Kat, Me, Ord, PO, SCh, SSta, Shi, Ss, Sta, Suc, TA, UnT, YD}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 63/63 = 100.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 81/81 = 100.0% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 70/80 = 87.5% | 80/80 = 100.0% | 80/80 = 100.0% | 80/80 = 100.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 363

**Selected features:** selected = {AC, ACh, ASta, BB, CBu, Ches, Cl, Go, HS, In, LA, Me, Np, ODI, Ord, PO, Shi, Ss, StS, Suc, Tbc, Tu}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 364

**Selected features:** selected = {ACu, ASt, Al, BAB, CCh, Cuf, DBu, HS, IC, In, LC, Me, Np, Ord, PO, SSta, Sc1b, Shi, Ss, StS, TA, Tu, Wc}

**Repaired FTS:** 59 states, 72 transitions (72 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 61/72 = 84.7% | 72/72 = 100.0% | 72/72 = 100.0% | 68/72 = 94.4% |
| ActionExchange | 119 | 0/119 = 0.0% | 0/119 = 0.0% | 97/119 = 81.5% | 119/119 = 100.0% | 119/119 = 100.0% | 114/119 = 95.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 119 | 8/119 = 6.7% | 0/111 = 0.0% | 81/111 = 73.0% | 108/111 = 97.3% | 111/111 = 100.0% | 101/111 = 91.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 365

**Selected features:** selected = {AS, CBu, CSta, Cuf, DCu, HS, IC, In, Ls, Me, OCu, Ord, PO, Pu, R, R1b, Rc, SCh, Shi, Sta, TA, Tu, WB, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 97/112 = 86.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 366

**Selected features:** selected = {AC, ACh, Bla, CBu, CSta, Ches, DSt, HS, ICu, In, Ls, Me, ODI, Ord, P, PO, SS, Sc1b, Shi, Sta, TA, Tu, WB, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 104/120 = 86.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 90/112 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 367

**Selected features:** selected = {AC, CBu, CCh, Ches, DCu, DSt, H, HS, In, LA, LP, Me, Np, Ord, PO, RB, Rc, SS, SSta, Sc1b, Shi, Ss, Tu, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 104/120 = 86.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 90/112 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 368

**Selected features:** selected = {AC, ACh, AS, CBu, CSta, Ches, Cop, DCu, Gy, HS, In, Ls, Me, OCu, Ord, PO, RB, Shi, Sta, TA, Tbc, UnT, Wc, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 369

**Selected features:** selected = {ACh, ASta, BluB, CBu, Ches, HS, How, ICu, In, Ls, Me, Np, ODI, Ord, PO, Rc, SS, Sc1b, Shi, Sil, TA, UnT}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 370

**Selected features:** selected = {BB, CBu, CSta, Ches, Cop, DC, DCu, DSt, HS, In, Kc, LA, Maj, Me, Np, Ord, PO, SCh, SS, Sc1b, Shi, Ss, Tu, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 371

**Selected features:** selected = {BB, Bd, CBu, Cop, Cuf, DC, Eli, HS, ICu, In, Ls, Me, Ord, PO, SCh, SS, SSta, Shi, Sta, TA, Tbc, UnT, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 89/97 = 91.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 82/94 = 87.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 372

**Selected features:** selected = {AA, ASt, Bd, BluB, CBu, Ches, DC, DCu, HS, In, LP, Ls, Me, NAG, Or, Ord, PO, SCh, SS, SSta, Sc1b, Shi, Sta, Tu}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 373

**Selected features:** selected = {AA, AC, ACh, AS, ASt, ASta, BluB, CBu, Cuf, Fl, HS, In, MB, Me, ODI, Ord, PO, Rc, Sc2b, Shi, Ss, Sta, Tu}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 97/97 = 100.0% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 91/94 = 96.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 374

**Selected features:** selected = {AC, Bd, CCh, CSta, Ches, DBu, DSt, Ds, HS, In, LP, Ls, Mcd, Me, Np, ODI, Ord, PO, SS, Shi, TA, Tu}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 59/67 = 88.1% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 82/96 = 85.4% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 77/93 = 82.8% | 92/93 = 98.9% | 93/93 = 100.0% | 76/93 = 81.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 375

**Selected features:** selected = {BAB, BB, CBu, CCh, Ches, DC, HS, Ho, In, LA, LB, Ls, Me, OCu, Ord, PO, R1b, SS, SSta, Shi, Sta, UnT, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 376

**Selected features:** selected = {ACh, ACu, ASta, Bd, CBu, Ches, DSt, Ds, HS, IC, In, LA, LP, Li, Ls, Me, Ord, PO, Shi, StS, Sta, UnT, WB, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 100/120 = 83.3% | 120/120 = 100.0% | 120/120 = 100.0% | 111/120 = 92.5% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 98/112 = 87.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state22__All Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 377

**Selected features:** selected = {ACu, ASt, ASta, BAB, CCh, Ches, DBu, H, HS, In, Ls, Me, Ord, PO, R1b, Rc, SS, Shi, Sil, Sta, TA, UnT}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 60/67 = 89.6% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 83/96 = 86.5% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 78/93 = 83.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 378

**Selected features:** selected = {AC, BAB, BluB, CBu, CCh, Cuf, DG, HS, In, LA, Ls, Me, Mo, Np, Ord, PO, R1b, SSta, Shi, StS, Tu, Wc}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 379

**Selected features:** selected = {ACh, ASt, ASta, BB, Bd, CBu, Cuf, DC, Ds, HS, In, Ls, Me, Np, Ord, PO, Rr, SS, Shi, TA, Tu, Wn, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 86/94 = 91.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 380

**Selected features:** selected = {BB, CBu, CCh, Ches, DC, DSt, HS, Ho, ICu, In, MG, Me, Ord, PO, Rc, SS, SSta, Sc1b, Shi, Ss, Sta, TA, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 381

**Selected features:** selected = {AA, ACu, BAB, CBu, CCh, Cuf, DSt, HS, In, Ke, Me, NK, Np, Ord, PO, Rr, SS, SSta, Shi, Ss, Tbc, UnT, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 84/94 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 382

**Selected features:** selected = {AA, AS, ASta, Bla, CCh, Ches, DBu, HS, In, Me, NK, Ord, PO, Sc2b, Shi, Ss, Sta, UnT, Vic, YD}

**Repaired FTS:** 56 states, 60 transitions (60 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 60 | 0/60 = 0.0% | 0/60 = 0.0% | 58/60 = 96.7% | 60/60 = 100.0% | 60/60 = 100.0% | 58/60 = 96.7% |
| ActionExchange | 72 | 0/72 = 0.0% | 0/72 = 0.0% | 70/72 = 97.2% | 72/72 = 100.0% | 72/72 = 100.0% | 70/72 = 97.2% |
| StateMissing | 55 | 0/55 = 0.0% | 0/55 = 0.0% | 55/55 = 100.0% | 55/55 = 100.0% | 55/55 = 100.0% | 54/55 = 98.2% |
| TransitionDestinationExchange | 72 | 1/72 = 1.4% | 0/71 = 0.0% | 66/71 = 93.0% | 71/71 = 100.0% | 71/71 = 100.0% | 68/71 = 95.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 383

**Selected features:** selected = {AA, ACu, BluB, CBu, Ches, Cra, HS, IC, In, Ls, Me, Np, ODI, Ord, PO, Rr, SCh, SS, SSta, Shi, Tbc, UnT, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 62/68 = 91.2% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 94/97 = 96.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 77/94 = 81.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 384

**Selected features:** selected = {ASt, ASta, BB, CBu, CCh, Cuf, DC, Gy, HS, In, LA, Ls, MB, Me, ODI, Ord, PO, Shi, StS, Sta, Tbc, UnT, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 86/94 = 91.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`

### Product 385

**Selected features:** selected = {ASt, BAB, BB, CBu, Ches, DC, DCu, HS, In, LC, Ma, Me, Ord, PO, Rc, SCh, SS, SSta, Sc2b, Shi, Ss, Sta, TA, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`

### Product 386

**Selected features:** selected = {AA, ACh, AS, BAB, Bla, BluB, CBu, CSta, Cuf, DCu, HS, IC, In, Le, Ls, Me, Np, Ord, PO, Sc2b, Shi, Tu, Wc}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 93/97 = 95.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default Cuff__state16__state17`

### Product 387

**Selected features:** selected = {ACh, ACu, An, BB, CBu, Ches, HS, IC, In, Kc, LC, Me, OCu, Ord, PO, R1b, SSta, Shi, Ss, StS, Sta, TA, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 388

**Selected features:** selected = {AA, ACh, ASta, Bd, CBu, Cuf, DB, DC, HS, ICu, In, Kayc, Me, NAG, Ord, PO, Sc2b, Shi, Ss, StS, Sta, Tu, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 89/97 = 91.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 75/94 = 79.8% | 93/94 = 98.9% | 94/94 = 100.0% | 82/94 = 87.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 389

**Selected features:** selected = {A, ACh, ACu, CBu, Cuf, DSt, HS, In, MB, Me, Np, Ord, PO, SS, SSta, Shi, Ss, Suc, TA, Tbc, UnT, WB, YD}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 63/68 = 92.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 84/94 = 89.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 390

**Selected features:** selected = {AC, ACh, ACu, AS, ASt, BB, Bd, CBu, CSta, Cuf, DG, HS, In, LA, Ls, Me, Mid, Np, Ord, PO, Shi, Tbc, Tu, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 110/120 = 91.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state22__All Cuff__state16__state17`

### Product 391

**Selected features:** selected = {ACu, CBu, CCh, CSta, Cuf, DC, HS, In, LC, Ls, Me, OCu, ODI, Ord, PO, Shi, StS, Sta, Suc, TA, Tbc, UnT, Vi, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 392

**Selected features:** selected = {ACu, ASta, CBu, Cuf, HS, Har, In, LC, Me, NAG, NK, Np, Ord, PO, SCh, Sc2b, Shi, Ss, StS, TA, UnT, WB}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 393

**Selected features:** selected = {AA, BAB, BB, CBu, CSta, Cuf, DCu, DSt, Gr, HS, In, Ls, Marv, Me, Ord, PO, Rc, SCh, Sc2b, Shi, StS, Sta, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 89/97 = 91.8% | 97/97 = 100.0% | 97/97 = 100.0% | 76/97 = 78.4% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 51/58 = 87.9% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 78/94 = 83.0% | 93/94 = 98.9% | 94/94 = 100.0% | 70/94 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state16`

### Product 394

**Selected features:** selected = {AC, AS, BAB, CBu, CSta, Cuf, Ga, HS, In, Ls, MG, Me, Ord, PO, R1b, Rc, SCh, Shi, Sta, TA, UnT, WB}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 61/64 = 95.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 79/82 = 96.3% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 72/81 = 88.9% | 81/81 = 100.0% | 81/81 = 100.0% | 76/81 = 93.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 395

**Selected features:** selected = {AA, ACu, ASt, BB, CBu, CCh, Cuf, Fa, HS, In, Kc, Me, Np, ODI, Ord, PO, SSta, Sc1b, Shi, Sil, Ss, StS, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 66/68 = 97.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 88/94 = 93.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 396

**Selected features:** selected = {AA, ACu, CCh, Ches, DBu, HS, In, LB, Me, Ord, PO, SSta, Shi, Ss, StS, Sta, Tbc, Th, Tu, Wc, YD}

**Repaired FTS:** 57 states, 63 transitions (63 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 63 | 0/63 = 0.0% | 0/63 = 0.0% | 59/63 = 93.7% | 63/63 = 100.0% | 63/63 = 100.0% | 59/63 = 93.7% |
| ActionExchange | 81 | 0/81 = 0.0% | 0/81 = 0.0% | 78/81 = 96.3% | 81/81 = 100.0% | 81/81 = 100.0% | 77/81 = 95.1% |
| StateMissing | 56 | 0/56 = 0.0% | 0/56 = 0.0% | 56/56 = 100.0% | 56/56 = 100.0% | 56/56 = 100.0% | 55/56 = 98.2% |
| TransitionDestinationExchange | 81 | 1/81 = 1.2% | 0/80 = 0.0% | 70/80 = 87.5% | 80/80 = 100.0% | 80/80 = 100.0% | 73/80 = 91.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 397

**Selected features:** selected = {AA, AC, CCh, CSta, Ches, DBu, Ds, HS, In, MG, Me, OCu, Ord, PO, S, SS, Shi, Ss, Sta, Tu, Wc, YD}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 60/67 = 89.6% | 67/67 = 100.0% | 67/67 = 100.0% | 65/67 = 97.0% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 86/96 = 89.6% | 96/96 = 100.0% | 96/96 = 100.0% | 92/96 = 95.8% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 75/93 = 80.6% | 92/93 = 98.9% | 93/93 = 100.0% | 87/93 = 93.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 398

**Selected features:** selected = {AA, AC, AS, CBu, CSta, Ches, DCu, Ds, HS, In, LB, Ls, Me, Np, OCu, ODI, Ord, PO, Po, Rc, SCh, Shi, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`

### Product 399

**Selected features:** selected = {AA, AS, ASta, BluB, CBu, Cop, Cuf, DSt, Ds, HS, IC, ICu, In, Me, ODI, Ord, PO, SCh, Shi, Ss, Sta, Suc, Tu, Val}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 400

**Selected features:** selected = {ACh, ACu, Arm, BluB, CBu, Cuf, DB, HS, In, Kc, Ls, Me, Np, ODI, Ord, PO, SS, SSta, Sc1b, Shi, TA, UnT}

**Repaired FTS:** 58 states, 64 transitions (64 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 64 | 0/64 = 0.0% | 0/64 = 0.0% | 60/64 = 93.8% | 64/64 = 100.0% | 64/64 = 100.0% | 58/64 = 90.6% |
| ActionExchange | 82 | 0/82 = 0.0% | 0/82 = 0.0% | 79/82 = 96.3% | 82/82 = 100.0% | 82/82 = 100.0% | 74/82 = 90.2% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 53/57 = 93.0% |
| TransitionDestinationExchange | 82 | 1/82 = 1.2% | 0/81 = 0.0% | 71/81 = 87.7% | 81/81 = 100.0% | 81/81 = 100.0% | 71/81 = 87.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 401

**Selected features:** selected = {ASt, ASta, CBu, CCh, Cuf, DC, HS, ICu, In, LG, Ls, Me, Ol, Ord, PO, Rc, Sc1b, Shi, StS, Sta, TA, Tu, WB, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 67/73 = 91.8% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 111/120 = 92.5% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 402

**Selected features:** selected = {AA, CBu, CCh, Cuf, DC, DSt, HS, ICu, In, Ls, MG, Ma, Me, NAG, Ord, PO, R1b, SS, SSta, Shi, Sta, Suc, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 61/73 = 83.6% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 94/120 = 78.3% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 54/59 = 91.5% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 80/112 = 71.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 403

**Selected features:** selected = {ACu, BAB, BB, Bd, CBu, CCh, CSta, Cop, Cuf, Ds, E, HS, In, LA, Me, Np, OCu, Ord, PO, SS, Shi, Ss, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`

### Product 404

**Selected features:** selected = {AC, ACh, ASta, CBu, Cuf, Ds, Gr, HS, ICu, In, LA, Ls, Me, ODI, Ord, PO, RB, Shi, StS, Sta, Tu, Wc, Wi}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 405

**Selected features:** selected = {AC, Bd, CBu, CCh, Fl, HS, LA, Ls, Me, Np, Ord, PO, SSta, Sc1b, Shi, StS, Tu, WB}

**Repaired FTS:** 54 states, 58 transitions (58 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 56/58 = 96.6% | 58/58 = 100.0% | 58/58 = 100.0% | 56/58 = 96.6% |
| ActionExchange | 69 | 0/69 = 0.0% | 0/69 = 0.0% | 67/69 = 97.1% | 69/69 = 100.0% | 69/69 = 100.0% | 67/69 = 97.1% |
| StateMissing | 53 | 0/53 = 0.0% | 0/53 = 0.0% | 53/53 = 100.0% | 53/53 = 100.0% | 53/53 = 100.0% | 52/53 = 98.1% |
| TransitionDestinationExchange | 69 | 1/69 = 1.4% | 0/68 = 0.0% | 63/68 = 92.6% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (1):

- `TDE__state61__Pay__state1__state2`

### Product 406

**Selected features:** selected = {A, AA, ACu, ASta, BAB, CBu, CCh, Ches, DC, Go, HS, In, Ls, Me, Np, OCu, Ord, PO, R1b, Shi, StS, UnT, WB, Wc}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 101/112 = 90.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 407

**Selected features:** selected = {ASt, Bd, BluB, CBu, Ches, DCu, DG, HS, In, LA, Ls, Me, Mu, NAG, Ord, PO, SCh, SSta, Sc2b, Shi, StS, Sta, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`

### Product 408

**Selected features:** selected = {AC, ACh, ASta, BB, Bla, CBu, Cuf, HS, ICu, In, LA, Ls, Me, NK, Np, OCu, ODI, Ord, PO, SS, Sh, Shi, Tbc, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 66/73 = 90.4% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 107/120 = 89.2% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 57/59 = 96.6% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 95/112 = 84.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 409

**Selected features:** selected = {AA, AC, ACh, AS, BB, CBu, Cuf, DCu, DSt, HS, In, Kc, LP, Me, N, Np, Ord, PO, SSta, Sc1b, Shi, Ss, UnT, YD}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 104/120 = 86.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 90/112 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 410

**Selected features:** selected = {ACu, AS, ASta, Bla, CCh, Ches, DBu, HS, IC, In, Ls, Me, Np, ODI, Ord, PO, R1b, S, Shi, Suc, TA, Tu}

**Repaired FTS:** 58 states, 67 transitions (67 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 67 | 0/67 = 0.0% | 0/67 = 0.0% | 61/67 = 91.0% | 67/67 = 100.0% | 67/67 = 100.0% | 62/67 = 92.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 0/96 = 0.0% | 88/96 = 91.7% | 96/96 = 100.0% | 96/96 = 100.0% | 89/96 = 92.7% |
| StateMissing | 57 | 0/57 = 0.0% | 0/57 = 0.0% | 57/57 = 100.0% | 57/57 = 100.0% | 57/57 = 100.0% | 56/57 = 98.2% |
| TransitionDestinationExchange | 96 | 3/96 = 3.1% | 0/93 = 0.0% | 76/93 = 81.7% | 92/93 = 98.9% | 93/93 = 100.0% | 82/93 = 88.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__All Cuff__state16__state17`

### Product 411

**Selected features:** selected = {AA, BB, CBu, CCh, Cas, Cop, Cuf, DCu, DSt, HS, IC, In, Kc, Me, ODI, Ord, PO, SS, SSta, Sc2b, Shi, Ss, Sta, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 68/73 = 93.2% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 102/112 = 91.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Default Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 412

**Selected features:** selected = {AC, ACh, AS, ASta, BAB, BluB, CBu, Ches, Gr, HS, In, LA, Me, Np, OCu, Ord, PO, Ri, Shi, Ss, Suc, Tbc, UnT}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 64/68 = 94.1% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 92/97 = 94.8% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 57/58 = 98.3% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state17`

### Product 413

**Selected features:** selected = {AA, AC, ACh, AS, BAB, CBu, Cuf, DSt, HS, ICu, In, Ls, Me, Ord, P, PO, Pu, SSta, Sc1b, Shi, Sta, Suc, Tu, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 65/73 = 89.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 104/120 = 86.7% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 109/112 = 97.3% | 112/112 = 100.0% | 90/112 = 80.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state17__Default 
Stitch__state15__state16`
- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Default 
Stitch__state15__state16`
- `TDE__state11__Default 
Stitch__state15__state17`
- `TDE__state11__Inner Cuff__state16__state17`
- `TDE__state22__Default 
Stitch__state15__state16`
- `TDE__state22__Default 
Stitch__state15__state17`

### Product 414

**Selected features:** selected = {AA, AC, ACh, AS, CBu, CSta, Cha, Ches, HS, ICu, In, Kc, MB, Me, ODI, Ord, PO, Shi, Ss, Sta, Tbc, UnT, WB}

**Repaired FTS:** 59 states, 68 transitions (68 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 68 | 0/68 = 0.0% | 0/68 = 0.0% | 61/68 = 89.7% | 68/68 = 100.0% | 68/68 = 100.0% | 65/68 = 95.6% |
| ActionExchange | 97 | 0/97 = 0.0% | 0/97 = 0.0% | 87/97 = 89.7% | 97/97 = 100.0% | 97/97 = 100.0% | 95/97 = 97.9% |
| StateMissing | 58 | 0/58 = 0.0% | 0/58 = 0.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% | 58/58 = 100.0% |
| TransitionDestinationExchange | 97 | 3/97 = 3.1% | 0/94 = 0.0% | 76/94 = 80.9% | 93/94 = 98.9% | 94/94 = 100.0% | 87/94 = 92.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (3):

- `TDE__state22__Inner Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state11__Inner Cuff__state16__state17`

### Product 415

**Selected features:** selected = {AC, ACu, CBu, CSta, Ches, HS, Hu, In, LA, LB, Ls, Me, NAG, Np, OCu, Ord, PO, Rc, SCh, SS, Sc2b, Shi, UnT, WB}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 62/73 = 84.9% | 73/73 = 100.0% | 73/73 = 100.0% | 69/73 = 94.5% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 98/120 = 81.7% | 120/120 = 100.0% | 120/120 = 100.0% | 115/120 = 95.8% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 58/59 = 98.3% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 82/112 = 73.2% | 109/112 = 97.3% | 112/112 = 100.0% | 103/112 = 92.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__Only Cuffs__state15__state16`
- `TDE__state11__Only Cuffs__state15__state17`
- `TDE__state17__Only Cuffs__state15__state16`
- `TDE__state11__All Cuff__state16__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state22__Only Cuffs__state15__state16`
- `TDE__state22__Only Cuffs__state15__state17`
- `TDE__state22__All Cuff__state16__state17`

### Product 416

**Selected features:** selected = {AA, ACh, ASt, ASta, Bens, BluB, CBu, Ches, DC, DCu, HS, In, Ls, MB, Me, NAG, Np, Ord, PO, Shi, StS, Suc, Tbc, UnT}

**Repaired FTS:** 60 states, 73 transitions (73 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 0 family-level), 0 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 73 | 0/73 = 0.0% | 0/73 = 0.0% | 63/73 = 86.3% | 73/73 = 100.0% | 73/73 = 100.0% | 70/73 = 95.9% |
| ActionExchange | 120 | 0/120 = 0.0% | 0/120 = 0.0% | 97/120 = 80.8% | 120/120 = 100.0% | 120/120 = 100.0% | 114/120 = 95.0% |
| StateMissing | 59 | 0/59 = 0.0% | 0/59 = 0.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% | 59/59 = 100.0% |
| TransitionDestinationExchange | 120 | 8/120 = 6.7% | 0/112 = 0.0% | 81/112 = 72.3% | 108/112 = 96.4% | 112/112 = 100.0% | 99/112 = 88.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state11__All Stitch__state15__state16`
- `TDE__state11__All Stitch__state15__state17`
- `TDE__state22__Default Cuff__state16__state17`
- `TDE__state22__All Stitch__state15__state16`
- `TDE__state22__All Stitch__state15__state17`
- `TDE__state61__Pay__state1__state2`
- `TDE__state17__All Stitch__state15__state16`
- `TDE__state11__Default Cuff__state16__state17`
---

## HockertyShirts summary (aggregate over 416 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 0 test case(s) generated once for the SPL.

**Equivalent-mutant treatment:** Inozemtseva &amp; Holmes (2014) — mutant not killed by ANY of the five suites (family + product state + product transition + product pair + random) is conservatively classified equivalent and EXCLUDED from the score denominator. Scores below are **killed / (total &minus; equivalent) = adjusted%**.

| Operator | Total mutants | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28895 | 0/28895 = 0.0% | 0/28895 = 0.0% | 25498/28895 = 88.2% | 28895/28895 = 100.0% | 28895/28895 = 100.0% | 26726/28895 = 92.5% |
| ActionExchange | 43725 | 0/43725 = 0.0% | 0/43725 = 0.0% | 37704/43725 = 86.2% | 43725/43725 = 100.0% | 43725/43725 = 100.0% | 40340/43725 = 92.3% |
| StateMissing | 24153 | 0/24153 = 0.0% | 0/24153 = 0.0% | 24153/24153 = 100.0% | 24153/24153 = 100.0% | 24153/24153 = 100.0% | 23568/24153 = 97.6% |
| TransitionDestinationExchange | 43725 | 2087/43725 = 4.8% | 0/41638 = 0.0% | 32279/41638 = 77.5% | 40845/41638 = 98.1% | 41638/41638 = 100.0% | 36536/41638 = 87.7% |

Total products: 416.
