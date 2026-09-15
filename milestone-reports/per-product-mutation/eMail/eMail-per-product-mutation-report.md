# Per-Product Mutation Report — eMail

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

**Selected features:** selected = {ad, au, e}

**Repaired FTS:** 10 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 11/18 = 61.1% | 11/18 = 61.1% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |
| ActionExchange | 62 | 0/62 = 0.0% | 49/62 = 79.0% | 44/62 = 71.0% | 62/62 = 100.0% | 62/62 = 100.0% | 54/62 = 87.1% |
| StateMissing | 9 | 0/9 = 0.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |
| TransitionDestinationExchange | 61 | 19/61 = 31.1% | 25/42 = 59.5% | 27/42 = 64.3% | 38/42 = 90.5% | 42/42 = 100.0% | 37/42 = 88.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (19):

- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state5`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state4`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state10__send email__state1__state5`
- `TDE__state10__send email__state1__state4`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`

### Product 2

**Selected features:** selected = {ad, au, e, en}

**Repaired FTS:** 11 states, 22 transitions (20 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 20 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 14/20 = 70.0% | 13/20 = 65.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |
| ActionExchange | 73 | 0/73 = 0.0% | 67/73 = 91.8% | 56/73 = 76.7% | 73/73 = 100.0% | 73/73 = 100.0% | 55/73 = 75.3% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% |
| TransitionDestinationExchange | 71 | 20/71 = 28.2% | 35/51 = 68.6% | 30/51 = 58.8% | 47/51 = 92.2% | 51/51 = 100.0% | 38/51 = 74.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (20):

- `TDE__state7__send email__state1__state11`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state5`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state4`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state10__send email__state1__state5`
- `TDE__state10__send email__state1__state4`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`

### Product 3

**Selected features:** selected = {au, e}

**Repaired FTS:** 7 states, 15 transitions (13 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 8/13 = 61.5% | 7/13 = 53.8% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |
| ActionExchange | 34 | 0/34 = 0.0% | 30/34 = 88.2% | 21/34 = 61.8% | 34/34 = 100.0% | 34/34 = 100.0% | 30/34 = 88.2% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 4/6 = 66.7% |
| TransitionDestinationExchange | 33 | 6/33 = 18.2% | 15/27 = 55.6% | 14/27 = 51.9% | 23/27 = 85.2% | 26/27 = 96.3% | 14/27 = 51.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (6):

- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`

### Product 4

**Selected features:** selected = {ad, e, en}

**Repaired FTS:** 9 states, 17 transitions (16 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 12/16 = 75.0% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |
| ActionExchange | 51 | 0/51 = 0.0% | 46/51 = 90.2% | 32/51 = 62.7% | 51/51 = 100.0% | 51/51 = 100.0% | 35/51 = 68.6% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 6/8 = 75.0% |
| TransitionDestinationExchange | 46 | 13/46 = 28.3% | 23/33 = 69.7% | 18/33 = 54.5% | 30/33 = 90.9% | 32/33 = 97.0% | 18/33 = 54.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (13):

- `TDE__state7__send email__state1__state11`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`

### Product 5

**Selected features:** selected = {au, e, en, s}

**Repaired FTS:** 9 states, 19 transitions (17 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 11/17 = 64.7% | 10/17 = 58.8% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |
| ActionExchange | 56 | 0/56 = 0.0% | 50/56 = 89.3% | 40/56 = 71.4% | 56/56 = 100.0% | 56/56 = 100.0% | 38/56 = 67.9% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |
| TransitionDestinationExchange | 54 | 13/54 = 24.1% | 26/41 = 63.4% | 21/41 = 51.2% | 37/41 = 90.2% | 41/41 = 100.0% | 26/41 = 63.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (13):

- `TDE__state7__send email__state1__state11`
- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state5`
- `TDE__state10__send email__state1__state4`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`

### Product 6

**Selected features:** selected = {ad, au, e, en, s}

**Repaired FTS:** 11 states, 23 transitions (21 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 20 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 14/21 = 66.7% | 13/21 = 61.9% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |
| ActionExchange | 82 | 0/82 = 0.0% | 72/82 = 87.8% | 58/82 = 70.7% | 82/82 = 100.0% | 82/82 = 100.0% | 53/82 = 64.6% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 6/10 = 60.0% |
| TransitionDestinationExchange | 75 | 21/75 = 28.0% | 35/54 = 64.8% | 30/54 = 55.6% | 50/54 = 92.6% | 54/54 = 100.0% | 30/54 = 55.6% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (21):

- `TDE__state7__send email__state1__state11`
- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state5`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state4`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state5`
- `TDE__state10__send email__state1__state4`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`

### Product 7

**Selected features:** selected = {e, en}

**Repaired FTS:** 7 states, 13 transitions (12 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 1 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 9/12 = 75.0% | 6/12 = 50.0% | 12/12 = 100.0% | 12/12 = 100.0% | 7/12 = 58.3% |
| ActionExchange | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 20/31 = 64.5% | 31/31 = 100.0% | 31/31 = 100.0% | 25/31 = 80.6% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 2/6 = 33.3% |
| TransitionDestinationExchange | 27 | 6/27 = 22.2% | 15/21 = 71.4% | 11/21 = 52.4% | 18/21 = 85.7% | 20/21 = 95.2% | 12/21 = 57.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (6):

- `TDE__state7__send email__state1__state11`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state3`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`

### Product 8

**Selected features:** selected = {e, en, s}

**Repaired FTS:** 7 states, 14 transitions (13 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 9/13 = 69.2% | 6/13 = 46.2% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |
| ActionExchange | 40 | 0/40 = 0.0% | 35/40 = 87.5% | 23/40 = 57.5% | 40/40 = 100.0% | 40/40 = 100.0% | 34/40 = 85.0% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |
| TransitionDestinationExchange | 35 | 8/35 = 22.9% | 17/27 = 63.0% | 12/27 = 44.4% | 24/27 = 88.9% | 26/27 = 96.3% | 21/27 = 77.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (8):

- `TDE__state7__send email__state1__state11`
- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state3`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`

### Product 9

**Selected features:** selected = {au, e, en}

**Repaired FTS:** 9 states, 18 transitions (16 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 11/16 = 68.8% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% | 13/16 = 81.3% |
| ActionExchange | 47 | 0/47 = 0.0% | 44/47 = 93.6% | 35/47 = 74.5% | 47/47 = 100.0% | 47/47 = 100.0% | 40/47 = 85.1% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |
| TransitionDestinationExchange | 46 | 11/46 = 23.9% | 24/35 = 68.6% | 19/35 = 54.3% | 31/35 = 88.6% | 34/35 = 97.1% | 29/35 = 82.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (11):

- `TDE__state7__send email__state1__state11`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`
- `TDE__state10__send email__state1__state5`
- `TDE__state10__send email__state1__state4`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`

### Product 10

**Selected features:** selected = {ad, e, en, s}

**Repaired FTS:** 9 states, 18 transitions (17 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 12/17 = 70.6% | 9/17 = 52.9% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |
| ActionExchange | 60 | 0/60 = 0.0% | 51/60 = 85.0% | 35/60 = 58.3% | 60/60 = 100.0% | 60/60 = 100.0% | 48/60 = 80.0% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 5/8 = 62.5% |
| TransitionDestinationExchange | 50 | 14/50 = 28.0% | 23/36 = 63.9% | 18/36 = 50.0% | 33/36 = 91.7% | 35/36 = 97.2% | 24/36 = 66.7% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (14):

- `TDE__state7__send email__state1__state11`
- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`

### Product 11

**Selected features:** selected = {au, e, f}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 10/15 = 66.7% | 9/15 = 60.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |
| ActionExchange | 40 | 0/40 = 0.0% | 36/40 = 90.0% | 34/40 = 85.0% | 40/40 = 100.0% | 40/40 = 100.0% | 38/40 = 95.0% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 6/7 = 85.7% |
| TransitionDestinationExchange | 39 | 10/39 = 25.6% | 17/29 = 58.6% | 15/29 = 51.7% | 25/29 = 86.2% | 28/29 = 96.6% | 19/29 = 65.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (10):

- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`
- `TDE__state10__send email__state1__state5`
- `TDE__state10__send email__state1__state4`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`

### Product 12

**Selected features:** selected = {e, f, s}

**Repaired FTS:** 6 states, 13 transitions (12 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 13 real step(s) applicable.

**Random baseline:** 2 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 8/12 = 66.7% | 6/12 = 50.0% | 12/12 = 100.0% | 12/12 = 100.0% | 11/12 = 91.7% |
| ActionExchange | 31 | 0/31 = 0.0% | 24/31 = 77.4% | 14/31 = 45.2% | 31/31 = 100.0% | 31/31 = 100.0% | 29/31 = 93.5% |
| StateMissing | 5 | 0/5 = 0.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% |
| TransitionDestinationExchange | 27 | 7/27 = 25.9% | 9/20 = 45.0% | 6/20 = 30.0% | 17/20 = 85.0% | 19/20 = 95.0% | 16/20 = 80.0% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (7):

- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state3`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`

### Product 13

**Selected features:** selected = {au, e, s}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 8/15 = 53.3% | 8/15 = 53.3% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |
| ActionExchange | 45 | 0/45 = 0.0% | 33/45 = 73.3% | 32/45 = 71.1% | 45/45 = 100.0% | 45/45 = 100.0% | 40/45 = 88.9% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |
| TransitionDestinationExchange | 44 | 12/44 = 27.3% | 16/32 = 50.0% | 16/32 = 50.0% | 28/32 = 87.5% | 31/32 = 96.9% | 28/32 = 87.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (12):

- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state5`
- `TDE__state10__send email__state1__state4`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`

### Product 14

**Selected features:** selected = {ad, au, e, f, s}

**Repaired FTS:** 10 states, 22 transitions (20 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 19 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 13/20 = 65.0% | 11/20 = 55.0% | 20/20 = 100.0% | 20/20 = 100.0% | 11/20 = 55.0% |
| ActionExchange | 71 | 0/71 = 0.0% | 59/71 = 83.1% | 46/71 = 64.8% | 71/71 = 100.0% | 71/71 = 100.0% | 48/71 = 67.6% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 5/9 = 55.6% |
| TransitionDestinationExchange | 66 | 20/66 = 30.3% | 27/46 = 58.7% | 28/46 = 60.9% | 42/46 = 91.3% | 46/46 = 100.0% | 24/46 = 52.2% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (20):

- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state5`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state4`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state5`
- `TDE__state10__send email__state1__state4`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`

### Product 15

**Selected features:** selected = {ad, au, e, s}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 11/19 = 57.9% | 11/19 = 57.9% | 19/19 = 100.0% | 19/19 = 100.0% | 10/19 = 52.6% |
| ActionExchange | 69 | 0/69 = 0.0% | 52/69 = 75.4% | 45/69 = 65.2% | 69/69 = 100.0% | 69/69 = 100.0% | 44/69 = 63.8% |
| StateMissing | 9 | 0/9 = 0.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 5/9 = 55.6% |
| TransitionDestinationExchange | 64 | 20/64 = 31.3% | 25/44 = 56.8% | 27/44 = 61.4% | 40/44 = 90.9% | 44/44 = 100.0% | 20/44 = 45.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (20):

- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state5`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state4`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state5`
- `TDE__state10__send email__state1__state4`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`

### Product 16

**Selected features:** selected = {ad, e, s}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 9/15 = 60.0% | 8/15 = 53.3% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |
| ActionExchange | 47 | 0/47 = 0.0% | 33/47 = 70.2% | 25/47 = 53.2% | 47/47 = 100.0% | 47/47 = 100.0% | 40/47 = 85.1% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |
| TransitionDestinationExchange | 39 | 13/39 = 33.3% | 13/26 = 50.0% | 13/26 = 50.0% | 23/26 = 88.5% | 25/26 = 96.2% | 20/26 = 76.9% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (13):

- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`

### Product 17

**Selected features:** selected = {au, e, f, s}

**Repaired FTS:** 8 states, 18 transitions (16 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 10/16 = 62.5% | 8/16 = 50.0% | 16/16 = 100.0% | 16/16 = 100.0% | 13/16 = 81.3% |
| ActionExchange | 47 | 0/47 = 0.0% | 39/47 = 83.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 41/47 = 87.2% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |
| TransitionDestinationExchange | 46 | 12/46 = 26.1% | 18/34 = 52.9% | 17/34 = 50.0% | 30/34 = 88.2% | 33/34 = 97.1% | 29/34 = 85.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (12):

- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state5`
- `TDE__state10__send email__state1__state4`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`

### Product 18

**Selected features:** selected = {ad, e, f, s}

**Repaired FTS:** 8 states, 17 transitions (16 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 11/16 = 68.8% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |
| ActionExchange | 49 | 0/49 = 0.0% | 38/49 = 77.6% | 25/49 = 51.0% | 49/49 = 100.0% | 49/49 = 100.0% | 42/49 = 85.7% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |
| TransitionDestinationExchange | 41 | 13/41 = 31.7% | 15/28 = 53.6% | 12/28 = 42.9% | 25/28 = 89.3% | 27/28 = 96.4% | 23/28 = 82.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (13):

- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`

### Product 19

**Selected features:** selected = {ad, e, f}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 11/15 = 73.3% | 9/15 = 60.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |
| ActionExchange | 42 | 0/42 = 0.0% | 35/42 = 83.3% | 24/42 = 57.1% | 42/42 = 100.0% | 42/42 = 100.0% | 40/42 = 95.2% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |
| TransitionDestinationExchange | 38 | 12/38 = 31.6% | 15/26 = 57.7% | 12/26 = 46.2% | 23/26 = 88.5% | 25/26 = 96.2% | 21/26 = 80.8% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (12):

- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`

### Product 20

**Selected features:** selected = {e, f}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 13 real step(s) applicable.

**Random baseline:** 2 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 11 | 0/11 = 0.0% | 8/11 = 72.7% | 6/11 = 54.5% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |
| ActionExchange | 24 | 0/24 = 0.0% | 19/24 = 79.2% | 13/24 = 54.2% | 24/24 = 100.0% | 24/24 = 100.0% | 19/24 = 79.2% |
| StateMissing | 5 | 0/5 = 0.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 2/5 = 40.0% |
| TransitionDestinationExchange | 20 | 5/20 = 25.0% | 8/15 = 53.3% | 6/15 = 40.0% | 12/15 = 80.0% | 14/15 = 93.3% | 8/15 = 53.3% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (5):

- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state3`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`

### Product 21

**Selected features:** selected = {e, s}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 11 real step(s) applicable.

**Random baseline:** 1 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 11 | 0/11 = 0.0% | 6/11 = 54.5% | 5/11 = 45.5% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |
| ActionExchange | 29 | 0/29 = 0.0% | 18/29 = 62.1% | 13/29 = 44.8% | 29/29 = 100.0% | 29/29 = 100.0% | 24/29 = 82.8% |
| StateMissing | 5 | 0/5 = 0.0% | 4/5 = 80.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 2/5 = 40.0% |
| TransitionDestinationExchange | 25 | 7/25 = 28.0% | 7/18 = 38.9% | 7/18 = 38.9% | 15/18 = 83.3% | 17/18 = 94.4% | 11/18 = 61.1% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (7):

- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state3`
- `TDE__state7__sign mail__state10__state7`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`

### Product 22

**Selected features:** selected = {ad, e}

**Repaired FTS:** 8 states, 15 transitions (14 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 3 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 9/14 = 64.3% | 8/14 = 57.1% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |
| ActionExchange | 40 | 0/40 = 0.0% | 30/40 = 75.0% | 24/40 = 60.0% | 40/40 = 100.0% | 40/40 = 100.0% | 29/40 = 72.5% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 5/7 = 71.4% |
| TransitionDestinationExchange | 36 | 12/36 = 33.3% | 13/24 = 54.2% | 13/24 = 54.2% | 21/24 = 87.5% | 23/24 = 95.8% | 15/24 = 62.5% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (12):

- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`

### Product 23

**Selected features:** selected = {ad, au, e, f}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 19 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 13/19 = 68.4% | 11/19 = 57.9% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |
| ActionExchange | 64 | 0/64 = 0.0% | 56/64 = 87.5% | 45/64 = 70.3% | 64/64 = 100.0% | 64/64 = 100.0% | 59/64 = 92.2% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |
| TransitionDestinationExchange | 63 | 19/63 = 30.2% | 27/44 = 61.4% | 28/44 = 63.6% | 40/44 = 90.9% | 44/44 = 100.0% | 38/44 = 86.4% |

**Equivalent mutants (not killed by any of the five suites):**

_TransitionDestinationExchange_ (19):

- `TDE__state7__send email__state1__state7`
- `TDE__state7__send email__state1__state10`
- `TDE__state7__send email__state1__state2`
- `TDE__state7__send email__state1__state5`
- `TDE__state7__send email__state1__state4`
- `TDE__state7__send email__state1__state6`
- `TDE__state7__send email__state1__state3`
- `TDE__state4__enter 
autoresponse 
email body__state5__state1`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state5`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state4`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state6`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state3`
- `TDE__state9__enter alias 
email addresses 
of receiver__state1__state2`
- `TDE__state10__send email__state1__state5`
- `TDE__state10__send email__state1__state4`
- `TDE__state10__send email__state1__state6`
- `TDE__state10__send email__state1__state3`
- `TDE__state10__send email__state1__state2`
- `TDE__state7__get alias 
email addresses 
of receiver__state10__state7`
---

## eMail summary (aggregate over 23 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 6 test case(s) generated once for the SPL.

**Equivalent-mutant treatment:** Inozemtseva &amp; Holmes (2014) — mutant not killed by ANY of the five suites (family + product state + product transition + product pair + random) is conservatively classified equivalent and EXCLUDED from the score denominator. Scores below are **killed / (total &minus; equivalent) = adjusted%**.

| Operator | Total mutants | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 361 | 0/361 = 0.0% | 238/361 = 65.9% | 202/361 = 56.0% | 361/361 = 100.0% | 361/361 = 100.0% | 268/361 = 74.2% |
| ActionExchange | 1134 | 0/1134 = 0.0% | 945/1134 = 83.3% | 737/1134 = 65.0% | 1134/1134 = 100.0% | 1134/1134 = 100.0% | 905/1134 = 79.8% |
| StateMissing | 170 | 0/170 = 0.0% | 164/170 = 96.5% | 170/170 = 100.0% | 170/170 = 100.0% | 170/170 = 100.0% | 138/170 = 81.2% |
| TransitionDestinationExchange | 1046 | 293/1046 = 28.0% | 448/753 = 59.5% | 400/753 = 53.1% | 672/753 = 89.2% | 737/753 = 97.9% | 521/753 = 69.2% |

Total products: 23.
