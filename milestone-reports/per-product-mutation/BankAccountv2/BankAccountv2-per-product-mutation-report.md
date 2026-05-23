# Per-Product Mutation Report — BankAccountv2

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

**Note on coverage saturation.** TransitionMissing and ActionExchange mutants on a deterministic FTS are killed precisely when the mutated transition is traversed; transition coverage therefore detects them by construction. Stronger criteria such as transition-pair coverage cannot improve detection for this operator set, though they do increase execution cost. This is an inherent property of these mutation operators on deterministic models. RQ3 is reframed around the cost dimension under this saturation property.

---

## Products


### Product 1

**Selected features:** selected = {b, c, cd, d, i, t, us, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 20/25 = 80.0% |
| ActionExchange | 89 | 0/89 = 0.0% | 82/89 = 92.1% | 77/89 = 86.5% | 89/89 = 100.0% | 89/89 = 100.0% | 70/89 = 78.7% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 2

**Selected features:** selected = {b, d, eu, i, t, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |
| ActionExchange | 54 | 0/54 = 0.0% | 44/54 = 81.5% | 46/54 = 85.2% | 54/54 = 100.0% | 54/54 = 100.0% | 44/54 = 81.5% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 3

**Selected features:** selected = {b, c, cd, cw, d, tl, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |
| ActionExchange | 49 | 0/49 = 0.0% | 47/49 = 95.9% | 37/49 = 75.5% | 49/49 = 100.0% | 49/49 = 100.0% | 44/49 = 89.8% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 9/10 = 90.0% |

### Product 4

**Selected features:** selected = {b, c, cd, d, t, tl, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 68 | 0/68 = 0.0% | 62/68 = 91.2% | 58/68 = 85.3% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |

### Product 5

**Selected features:** selected = {b, c, cd, cw, d, t, tl, up, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 18/29 = 62.1% |
| ActionExchange | 107 | 0/107 = 0.0% | 100/107 = 93.5% | 90/107 = 84.1% | 107/107 = 100.0% | 107/107 = 100.0% | 76/107 = 71.0% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |

### Product 6

**Selected features:** selected = {b, cw, d, dl, eu, o, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |
| ActionExchange | 64 | 0/64 = 0.0% | 56/64 = 87.5% | 51/64 = 79.7% | 64/64 = 100.0% | 64/64 = 100.0% | 56/64 = 87.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 7

**Selected features:** selected = {b, cw, d, dl, eu, o, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 18/24 = 75.0% |
| ActionExchange | 95 | 0/95 = 0.0% | 86/95 = 90.5% | 79/95 = 83.2% | 95/95 = 100.0% | 95/95 = 100.0% | 73/95 = 76.8% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 8

**Selected features:** selected = {b, cw, d, eu, i, ie, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 69 | 0/69 = 0.0% | 63/69 = 91.3% | 57/69 = 82.6% | 69/69 = 100.0% | 69/69 = 100.0% | 56/69 = 81.2% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 9

**Selected features:** selected = {b, d, i, ie, t, tl, up, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 24/27 = 88.9% | 27/27 = 100.0% | 27/27 = 100.0% | 22/27 = 81.5% |
| ActionExchange | 93 | 0/93 = 0.0% | 81/93 = 87.1% | 78/93 = 83.9% | 93/93 = 100.0% | 93/93 = 100.0% | 75/93 = 80.6% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |

### Product 10

**Selected features:** selected = {b, cd, d, i, tl, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |
| ActionExchange | 43 | 0/43 = 0.0% | 41/43 = 95.3% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% |

### Product 11

**Selected features:** selected = {b, c, d, tl, up, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 54 | 0/54 = 0.0% | 48/54 = 88.9% | 50/54 = 92.6% | 54/54 = 100.0% | 54/54 = 100.0% | 40/54 = 74.1% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 12

**Selected features:** selected = {b, cw, d, dl, i, ie, o, t, tl, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 22/30 = 73.3% |
| ActionExchange | 131 | 0/131 = 0.0% | 115/131 = 87.8% | 100/131 = 76.3% | 131/131 = 100.0% | 131/131 = 100.0% | 101/131 = 77.1% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 14/18 = 77.8% |

### Product 13

**Selected features:** selected = {b, cw, d, dl, i, t, us, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 91 | 0/91 = 0.0% | 79/91 = 86.8% | 71/91 = 78.0% | 91/91 = 100.0% | 91/91 = 100.0% | 68/91 = 74.7% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 13/16 = 81.3% |

### Product 14

**Selected features:** selected = {b, c, d, t, up, us, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 84 | 0/84 = 0.0% | 72/84 = 85.7% | 74/84 = 88.1% | 84/84 = 100.0% | 84/84 = 100.0% | 68/84 = 81.0% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |

### Product 15

**Selected features:** selected = {b, c, d, t, us, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 19/20 = 95.0% |
| ActionExchange | 54 | 0/54 = 0.0% | 44/54 = 81.5% | 46/54 = 85.2% | 54/54 = 100.0% | 54/54 = 100.0% | 50/54 = 92.6% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% |

### Product 16

**Selected features:** selected = {b, c, d, i, t, us, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 21/22 = 95.5% |
| ActionExchange | 74 | 0/74 = 0.0% | 62/74 = 83.8% | 64/74 = 86.5% | 74/74 = 100.0% | 74/74 = 100.0% | 69/74 = 93.2% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% |

### Product 17

**Selected features:** selected = {b, c, cd, cw, d, eu, i, ie, t, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 25/33 = 75.8% |
| ActionExchange | 144 | 0/144 = 0.0% | 136/144 = 94.4% | 118/144 = 81.9% | 144/144 = 100.0% | 144/144 = 100.0% | 108/144 = 75.0% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 17/20 = 85.0% |

### Product 18

**Selected features:** selected = {b, d, t, us, w}

**Repaired FTS:** 14 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 14/18 = 77.8% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 38 | 0/38 = 0.0% | 30/38 = 78.9% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 29/38 = 76.3% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 19

**Selected features:** selected = {b, c, cw, d, dl, eu, i, ie, t, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 24/29 = 82.8% | 29/29 = 100.0% | 29/29 = 100.0% | 17/29 = 58.6% |
| ActionExchange | 127 | 0/127 = 0.0% | 113/127 = 89.0% | 97/127 = 76.4% | 127/127 = 100.0% | 127/127 = 100.0% | 79/127 = 62.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 20

**Selected features:** selected = {b, cd, d, eu, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 18/19 = 94.7% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |
| ActionExchange | 51 | 0/51 = 0.0% | 49/51 = 96.1% | 48/51 = 94.1% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 11/12 = 91.7% |

### Product 21

**Selected features:** selected = {b, c, cd, d, t, tl, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 22/28 = 78.6% |
| ActionExchange | 99 | 0/99 = 0.0% | 92/99 = 92.9% | 89/99 = 89.9% | 99/99 = 100.0% | 99/99 = 100.0% | 80/99 = 80.8% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |

### Product 22

**Selected features:** selected = {b, cw, d, eu, i, ie, t, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 24/28 = 85.7% |
| ActionExchange | 101 | 0/101 = 0.0% | 89/101 = 88.1% | 81/101 = 80.2% | 101/101 = 100.0% | 101/101 = 100.0% | 84/101 = 83.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 23

**Selected features:** selected = {b, c, d, up, us, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 54 | 0/54 = 0.0% | 48/54 = 88.9% | 50/54 = 92.6% | 54/54 = 100.0% | 54/54 = 100.0% | 40/54 = 74.1% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 24

**Selected features:** selected = {b, c, cd, cw, d, dl, tl, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 74 | 0/74 = 0.0% | 72/74 = 97.3% | 60/74 = 81.1% | 74/74 = 100.0% | 74/74 = 100.0% | 60/74 = 81.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 25

**Selected features:** selected = {b, cw, d, dl, eu, o, t, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 26/31 = 83.9% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 20/31 = 64.5% |
| ActionExchange | 132 | 0/132 = 0.0% | 116/132 = 87.9% | 107/132 = 81.1% | 132/132 = 100.0% | 132/132 = 100.0% | 89/132 = 67.4% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 13/19 = 68.4% |

### Product 26

**Selected features:** selected = {b, d, eu, i, ie, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |
| ActionExchange | 37 | 0/37 = 0.0% | 32/37 = 86.5% | 31/37 = 83.8% | 37/37 = 100.0% | 37/37 = 100.0% | 28/37 = 75.7% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 27

**Selected features:** selected = {b, cd, cw, d, dl, eu, t, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 83 | 0/83 = 0.0% | 77/83 = 92.8% | 65/83 = 78.3% | 83/83 = 100.0% | 83/83 = 100.0% | 63/83 = 75.9% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 28

**Selected features:** selected = {b, cw, d, t, tl, up, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 21/24 = 87.5% |
| ActionExchange | 70 | 0/70 = 0.0% | 60/70 = 85.7% | 58/70 = 82.9% | 70/70 = 100.0% | 70/70 = 100.0% | 61/70 = 87.1% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 15/16 = 93.8% |

### Product 29

**Selected features:** selected = {b, cw, d, eu, up, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |
| ActionExchange | 44 | 0/44 = 0.0% | 39/44 = 88.6% | 38/44 = 86.4% | 44/44 = 100.0% | 44/44 = 100.0% | 35/44 = 79.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 30

**Selected features:** selected = {b, cd, cw, d, dl, o, t, tl, up, w}

**Repaired FTS:** 21 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 31/34 = 91.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 25/34 = 73.5% |
| ActionExchange | 148 | 0/148 = 0.0% | 138/148 = 93.2% | 123/148 = 83.1% | 148/148 = 100.0% | 148/148 = 100.0% | 112/148 = 75.7% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 18/20 = 90.0% |

### Product 31

**Selected features:** selected = {b, c, cw, d, dl, eu, t, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 22/25 = 88.0% |
| ActionExchange | 91 | 0/91 = 0.0% | 79/91 = 86.8% | 71/91 = 78.0% | 91/91 = 100.0% | 91/91 = 100.0% | 76/91 = 83.5% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% |

### Product 32

**Selected features:** selected = {b, c, d, eu, i, ie, t, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 16/24 = 66.7% |
| ActionExchange | 83 | 0/83 = 0.0% | 71/83 = 85.5% | 68/83 = 81.9% | 83/83 = 100.0% | 83/83 = 100.0% | 60/83 = 72.3% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |

### Product 33

**Selected features:** selected = {b, c, d, t, tl, up, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 84 | 0/84 = 0.0% | 72/84 = 85.7% | 74/84 = 88.1% | 84/84 = 100.0% | 84/84 = 100.0% | 68/84 = 81.0% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |

### Product 34

**Selected features:** selected = {b, d, t, up, us, w}

**Repaired FTS:** 17 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 63 | 0/63 = 0.0% | 53/63 = 84.1% | 55/63 = 87.3% | 63/63 = 100.0% | 63/63 = 100.0% | 53/63 = 84.1% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 35

**Selected features:** selected = {b, cw, d, dl, i, ie, t, up, us, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 27/32 = 84.4% | 32/32 = 100.0% | 32/32 = 100.0% | 22/32 = 68.8% |
| ActionExchange | 138 | 0/138 = 0.0% | 124/138 = 89.9% | 108/138 = 78.3% | 138/138 = 100.0% | 138/138 = 100.0% | 99/138 = 71.7% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 15/20 = 75.0% |

### Product 36

**Selected features:** selected = {b, c, d, i, up, us, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 19/20 = 95.0% | 20/20 = 100.0% | 20/20 = 100.0% | 18/20 = 90.0% |
| ActionExchange | 74 | 0/74 = 0.0% | 67/74 = 90.5% | 69/74 = 93.2% | 74/74 = 100.0% | 74/74 = 100.0% | 68/74 = 91.9% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 37

**Selected features:** selected = {b, cw, d, dl, i, o, t, tl, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 28/33 = 84.8% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 25/33 = 75.8% |
| ActionExchange | 163 | 0/163 = 0.0% | 145/163 = 89.0% | 134/163 = 82.2% | 163/163 = 100.0% | 163/163 = 100.0% | 142/163 = 87.1% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 15/20 = 75.0% |

### Product 38

**Selected features:** selected = {b, cw, d, dl, eu, t, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 23/28 = 82.1% |
| ActionExchange | 101 | 0/101 = 0.0% | 89/101 = 88.1% | 81/101 = 80.2% | 101/101 = 100.0% | 101/101 = 100.0% | 80/101 = 79.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 39

**Selected features:** selected = {b, cd, cw, d, eu, i, up, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 15/22 = 68.2% |
| ActionExchange | 75 | 0/75 = 0.0% | 73/75 = 97.3% | 60/75 = 80.0% | 75/75 = 100.0% | 75/75 = 100.0% | 58/75 = 77.3% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 40

**Selected features:** selected = {b, d, i, ie, up, us, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 15/20 = 75.0% |
| ActionExchange | 62 | 0/62 = 0.0% | 56/62 = 90.3% | 54/62 = 87.1% | 62/62 = 100.0% | 62/62 = 100.0% | 50/62 = 80.6% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 41

**Selected features:** selected = {b, cw, d, dl, eu, i, o, t, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 23/28 = 82.1% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 19/28 = 67.9% |
| ActionExchange | 121 | 0/121 = 0.0% | 105/121 = 86.8% | 96/121 = 79.3% | 121/121 = 100.0% | 121/121 = 100.0% | 85/121 = 70.2% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 42

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, o, w}

**Repaired FTS:** 15 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 20/26 = 76.9% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 109 | 0/109 = 0.0% | 105/109 = 96.3% | 86/109 = 78.9% | 109/109 = 100.0% | 109/109 = 100.0% | 84/109 = 77.1% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 43

**Selected features:** selected = {b, d, i, t, up, us, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 22/25 = 88.0% |
| ActionExchange | 84 | 0/84 = 0.0% | 72/84 = 85.7% | 74/84 = 88.1% | 84/84 = 100.0% | 84/84 = 100.0% | 73/84 = 86.9% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 44

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, us, w}

**Repaired FTS:** 15 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 20/26 = 76.9% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 109 | 0/109 = 0.0% | 105/109 = 96.3% | 86/109 = 78.9% | 109/109 = 100.0% | 109/109 = 100.0% | 84/109 = 77.1% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 45

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, o, up, w}

**Repaired FTS:** 17 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 23/29 = 79.3% |
| ActionExchange | 137 | 0/137 = 0.0% | 133/137 = 97.1% | 118/137 = 86.1% | 137/137 = 100.0% | 137/137 = 100.0% | 114/137 = 83.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 15/16 = 93.8% |

### Product 46

**Selected features:** selected = {b, cd, d, eu, i, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |
| ActionExchange | 43 | 0/43 = 0.0% | 41/43 = 95.3% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% |

### Product 47

**Selected features:** selected = {b, c, cw, d, eu, i, t, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 70/82 = 85.4% | 67/82 = 81.7% | 82/82 = 100.0% | 82/82 = 100.0% | 65/82 = 79.3% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 48

**Selected features:** selected = {b, d, i, ie, t, up, us, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 24/27 = 88.9% | 27/27 = 100.0% | 27/27 = 100.0% | 22/27 = 81.5% |
| ActionExchange | 93 | 0/93 = 0.0% | 81/93 = 87.1% | 78/93 = 83.9% | 93/93 = 100.0% | 93/93 = 100.0% | 75/93 = 80.6% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |

### Product 49

**Selected features:** selected = {b, cd, cw, d, dl, eu, t, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 24/31 = 77.4% |
| ActionExchange | 116 | 0/116 = 0.0% | 109/116 = 94.0% | 96/116 = 82.8% | 116/116 = 100.0% | 116/116 = 100.0% | 92/116 = 79.3% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |

### Product 50

**Selected features:** selected = {b, cw, d, dl, o, t, tl, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 26/31 = 83.9% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 20/31 = 64.5% |
| ActionExchange | 132 | 0/132 = 0.0% | 116/132 = 87.9% | 107/132 = 81.1% | 132/132 = 100.0% | 132/132 = 100.0% | 89/132 = 67.4% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 13/19 = 68.4% |

### Product 51

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 23/28 = 82.1% |
| ActionExchange | 133 | 0/133 = 0.0% | 131/133 = 98.5% | 115/133 = 86.5% | 133/133 = 100.0% | 133/133 = 100.0% | 112/133 = 84.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 15/16 = 93.8% |

### Product 52

**Selected features:** selected = {b, c, cw, d, dl, eu, i, ie, up, w}

**Repaired FTS:** 17 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 24/27 = 88.9% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 16/27 = 59.3% |
| ActionExchange | 127 | 0/127 = 0.0% | 119/127 = 93.7% | 103/127 = 81.1% | 127/127 = 100.0% | 127/127 = 100.0% | 87/127 = 68.5% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |

### Product 53

**Selected features:** selected = {b, c, d, i, ie, t, up, us, w}

**Repaired FTS:** 20 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 26/29 = 89.7% | 29/29 = 100.0% | 29/29 = 100.0% | 18/29 = 62.1% |
| ActionExchange | 119 | 0/119 = 0.0% | 105/119 = 88.2% | 101/119 = 84.9% | 119/119 = 100.0% | 119/119 = 100.0% | 82/119 = 68.9% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 54

**Selected features:** selected = {b, c, d, t, tl, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 19/20 = 95.0% |
| ActionExchange | 54 | 0/54 = 0.0% | 44/54 = 81.5% | 46/54 = 85.2% | 54/54 = 100.0% | 54/54 = 100.0% | 50/54 = 92.6% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% |

### Product 55

**Selected features:** selected = {b, cd, d, i, ie, t, us, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 19/25 = 76.0% |
| ActionExchange | 76 | 0/76 = 0.0% | 70/76 = 92.1% | 62/76 = 81.6% | 76/76 = 100.0% | 76/76 = 100.0% | 58/76 = 76.3% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 56

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, o, t, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 22/30 = 73.3% |
| ActionExchange | 131 | 0/131 = 0.0% | 115/131 = 87.8% | 100/131 = 76.3% | 131/131 = 100.0% | 131/131 = 100.0% | 101/131 = 77.1% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 14/18 = 77.8% |

### Product 57

**Selected features:** selected = {b, cd, cw, d, dl, o, up, us, w}

**Repaired FTS:** 16 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 18/27 = 66.7% |
| ActionExchange | 110 | 0/110 = 0.0% | 106/110 = 96.4% | 94/110 = 85.5% | 110/110 = 100.0% | 110/110 = 100.0% | 84/110 = 76.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 58

**Selected features:** selected = {b, cd, cw, d, eu, t, up, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 22/27 = 81.5% |
| ActionExchange | 84 | 0/84 = 0.0% | 78/84 = 92.9% | 70/84 = 83.3% | 84/84 = 100.0% | 84/84 = 100.0% | 67/84 = 79.8% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |

### Product 59

**Selected features:** selected = {b, cd, cw, d, dl, o, t, us, w}

**Repaired FTS:** 18 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 26/29 = 89.7% | 23/29 = 79.3% | 29/29 = 100.0% | 29/29 = 100.0% | 24/29 = 82.8% |
| ActionExchange | 110 | 0/110 = 0.0% | 101/110 = 91.8% | 87/110 = 79.1% | 110/110 = 100.0% | 110/110 = 100.0% | 92/110 = 83.6% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 60

**Selected features:** selected = {b, cw, d, eu, i, ie, t, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 69 | 0/69 = 0.0% | 59/69 = 85.5% | 53/69 = 76.8% | 69/69 = 100.0% | 69/69 = 100.0% | 55/69 = 79.7% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 61

**Selected features:** selected = {b, c, d, i, ie, up, us, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 17/22 = 77.3% |
| ActionExchange | 83 | 0/83 = 0.0% | 76/83 = 91.6% | 73/83 = 88.0% | 83/83 = 100.0% | 83/83 = 100.0% | 67/83 = 80.7% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 62

**Selected features:** selected = {b, c, cd, d, eu, t, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 22/28 = 78.6% |
| ActionExchange | 99 | 0/99 = 0.0% | 92/99 = 92.9% | 89/99 = 89.9% | 99/99 = 100.0% | 99/99 = 100.0% | 80/99 = 80.8% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |

### Product 63

**Selected features:** selected = {b, cd, d, t, tl, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 24/26 = 92.3% | 26/26 = 100.0% | 26/26 = 100.0% | 16/26 = 61.5% |
| ActionExchange | 77 | 0/77 = 0.0% | 71/77 = 92.2% | 69/77 = 89.6% | 77/77 = 100.0% | 77/77 = 100.0% | 47/77 = 61.0% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 64

**Selected features:** selected = {b, cd, cw, d, t, tl, up, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 22/27 = 81.5% |
| ActionExchange | 84 | 0/84 = 0.0% | 78/84 = 92.9% | 70/84 = 83.3% | 84/84 = 100.0% | 84/84 = 100.0% | 67/84 = 79.8% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |

### Product 65

**Selected features:** selected = {b, cw, d, dl, eu, i, t, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 91 | 0/91 = 0.0% | 79/91 = 86.8% | 71/91 = 78.0% | 91/91 = 100.0% | 91/91 = 100.0% | 68/91 = 74.7% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 13/16 = 81.3% |

### Product 66

**Selected features:** selected = {b, c, cd, d, eu, i, ie, t, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 23/27 = 85.2% |
| ActionExchange | 98 | 0/98 = 0.0% | 91/98 = 92.9% | 81/98 = 82.7% | 98/98 = 100.0% | 98/98 = 100.0% | 82/98 = 83.7% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 67

**Selected features:** selected = {b, c, d, i, us, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 14/15 = 93.3% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |
| ActionExchange | 45 | 0/45 = 0.0% | 39/45 = 86.7% | 41/45 = 91.1% | 45/45 = 100.0% | 45/45 = 100.0% | 34/45 = 75.6% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 68

**Selected features:** selected = {b, d, t, tl, up, w}

**Repaired FTS:** 17 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 63 | 0/63 = 0.0% | 53/63 = 84.1% | 55/63 = 87.3% | 63/63 = 100.0% | 63/63 = 100.0% | 53/63 = 84.1% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 69

**Selected features:** selected = {b, d, eu, i, w}

**Repaired FTS:** 10 states, 13 transitions (13 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 10/13 = 76.9% | 12/13 = 92.3% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |
| ActionExchange | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 25/30 = 83.3% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 70

**Selected features:** selected = {b, cd, cw, d, dl, eu, o, t, w}

**Repaired FTS:** 18 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 26/29 = 89.7% | 23/29 = 79.3% | 29/29 = 100.0% | 29/29 = 100.0% | 24/29 = 82.8% |
| ActionExchange | 110 | 0/110 = 0.0% | 101/110 = 91.8% | 87/110 = 79.1% | 110/110 = 100.0% | 110/110 = 100.0% | 92/110 = 83.6% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 71

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, t, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 26/35 = 74.3% |
| ActionExchange | 154 | 0/154 = 0.0% | 146/154 = 94.8% | 124/154 = 80.5% | 154/154 = 100.0% | 154/154 = 100.0% | 114/154 = 74.0% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |

### Product 72

**Selected features:** selected = {b, c, d, i, t, up, us, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 25/27 = 92.6% | 27/27 = 100.0% | 27/27 = 100.0% | 18/27 = 66.7% |
| ActionExchange | 109 | 0/109 = 0.0% | 95/109 = 87.2% | 97/109 = 89.0% | 109/109 = 100.0% | 109/109 = 100.0% | 82/109 = 75.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 73

**Selected features:** selected = {b, cd, cw, d, dl, o, t, tl, w}

**Repaired FTS:** 18 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 26/29 = 89.7% | 23/29 = 79.3% | 29/29 = 100.0% | 29/29 = 100.0% | 24/29 = 82.8% |
| ActionExchange | 110 | 0/110 = 0.0% | 101/110 = 91.8% | 87/110 = 79.1% | 110/110 = 100.0% | 110/110 = 100.0% | 92/110 = 83.6% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 74

**Selected features:** selected = {b, d, eu, t, w}

**Repaired FTS:** 14 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 14/18 = 77.8% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 38 | 0/38 = 0.0% | 30/38 = 78.9% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 29/38 = 76.3% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 75

**Selected features:** selected = {b, d, t, tl, w}

**Repaired FTS:** 14 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 14/18 = 77.8% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 38 | 0/38 = 0.0% | 30/38 = 78.9% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 29/38 = 76.3% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 76

**Selected features:** selected = {b, d, i, ie, tl, up, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 15/20 = 75.0% |
| ActionExchange | 62 | 0/62 = 0.0% | 56/62 = 90.3% | 54/62 = 87.1% | 62/62 = 100.0% | 62/62 = 100.0% | 50/62 = 80.6% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 77

**Selected features:** selected = {b, cd, cw, d, dl, eu, o, w}

**Repaired FTS:** 13 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 17/22 = 77.3% |
| ActionExchange | 78 | 0/78 = 0.0% | 74/78 = 94.9% | 63/78 = 80.8% | 78/78 = 100.0% | 78/78 = 100.0% | 60/78 = 76.9% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 78

**Selected features:** selected = {b, cw, d, dl, i, ie, o, us, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 15/23 = 65.2% |
| ActionExchange | 94 | 0/94 = 0.0% | 85/94 = 90.4% | 73/94 = 77.7% | 94/94 = 100.0% | 94/94 = 100.0% | 67/94 = 71.3% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 79

**Selected features:** selected = {b, cw, d, dl, up, us, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 69 | 0/69 = 0.0% | 63/69 = 91.3% | 57/69 = 82.6% | 69/69 = 100.0% | 69/69 = 100.0% | 56/69 = 81.2% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 80

**Selected features:** selected = {b, d, i, ie, t, us, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 19/22 = 86.4% | 22/22 = 100.0% | 22/22 = 100.0% | 18/22 = 81.8% |
| ActionExchange | 62 | 0/62 = 0.0% | 52/62 = 83.9% | 50/62 = 80.6% | 62/62 = 100.0% | 62/62 = 100.0% | 52/62 = 83.9% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 81

**Selected features:** selected = {b, c, cd, d, tl, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 68 | 0/68 = 0.0% | 66/68 = 97.1% | 64/68 = 94.1% | 68/68 = 100.0% | 68/68 = 100.0% | 53/68 = 77.9% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 82

**Selected features:** selected = {b, c, d, eu, i, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 14/15 = 93.3% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |
| ActionExchange | 45 | 0/45 = 0.0% | 39/45 = 86.7% | 41/45 = 91.1% | 45/45 = 100.0% | 45/45 = 100.0% | 34/45 = 75.6% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 83

**Selected features:** selected = {b, c, d, eu, i, ie, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |
| ActionExchange | 53 | 0/53 = 0.0% | 47/53 = 88.7% | 45/53 = 84.9% | 53/53 = 100.0% | 53/53 = 100.0% | 40/53 = 75.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 84

**Selected features:** selected = {b, c, cd, cw, d, dl, i, t, tl, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 31/35 = 88.6% | 35/35 = 100.0% | 35/35 = 100.0% | 27/35 = 77.1% |
| ActionExchange | 176 | 0/176 = 0.0% | 167/176 = 94.9% | 148/176 = 84.1% | 176/176 = 100.0% | 176/176 = 100.0% | 136/176 = 77.3% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |

### Product 85

**Selected features:** selected = {b, d, eu, i, up, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 54 | 0/54 = 0.0% | 48/54 = 88.9% | 50/54 = 92.6% | 54/54 = 100.0% | 54/54 = 100.0% | 43/54 = 79.6% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 86

**Selected features:** selected = {b, cd, cw, d, dl, o, us, w}

**Repaired FTS:** 13 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 17/22 = 77.3% |
| ActionExchange | 78 | 0/78 = 0.0% | 74/78 = 94.9% | 63/78 = 80.8% | 78/78 = 100.0% | 78/78 = 100.0% | 60/78 = 76.9% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 87

**Selected features:** selected = {b, cd, d, eu, i, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 68 | 0/68 = 0.0% | 66/68 = 97.1% | 64/68 = 94.1% | 68/68 = 100.0% | 68/68 = 100.0% | 53/68 = 77.9% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 88

**Selected features:** selected = {b, d, eu, i, ie, up, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 15/20 = 75.0% |
| ActionExchange | 62 | 0/62 = 0.0% | 56/62 = 90.3% | 54/62 = 87.1% | 62/62 = 100.0% | 62/62 = 100.0% | 50/62 = 80.6% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 89

**Selected features:** selected = {b, c, cw, d, i, t, tl, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 70/82 = 85.4% | 67/82 = 81.7% | 82/82 = 100.0% | 82/82 = 100.0% | 65/82 = 79.3% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 90

**Selected features:** selected = {b, cd, cw, d, i, t, tl, up, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 21/29 = 72.4% |
| ActionExchange | 107 | 0/107 = 0.0% | 100/107 = 93.5% | 90/107 = 84.1% | 107/107 = 100.0% | 107/107 = 100.0% | 77/107 = 72.0% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 15/18 = 83.3% |

### Product 91

**Selected features:** selected = {b, c, d, i, ie, t, us, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 16/24 = 66.7% |
| ActionExchange | 83 | 0/83 = 0.0% | 71/83 = 85.5% | 68/83 = 81.9% | 83/83 = 100.0% | 83/83 = 100.0% | 60/83 = 72.3% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |

### Product 92

**Selected features:** selected = {b, c, d, eu, i, ie, up, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 17/22 = 77.3% |
| ActionExchange | 83 | 0/83 = 0.0% | 76/83 = 91.6% | 73/83 = 88.0% | 83/83 = 100.0% | 83/83 = 100.0% | 67/83 = 80.7% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 93

**Selected features:** selected = {b, cd, d, eu, t, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 51 | 0/51 = 0.0% | 46/51 = 90.2% | 43/51 = 84.3% | 51/51 = 100.0% | 51/51 = 100.0% | 36/51 = 70.6% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 94

**Selected features:** selected = {b, d, up, us, w}

**Repaired FTS:** 12 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 15/16 = 93.8% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |
| ActionExchange | 38 | 0/38 = 0.0% | 33/38 = 86.8% | 35/38 = 92.1% | 38/38 = 100.0% | 38/38 = 100.0% | 28/38 = 73.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |

### Product 95

**Selected features:** selected = {b, c, cw, d, dl, eu, i, ie, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 90 | 0/90 = 0.0% | 83/90 = 92.2% | 70/90 = 77.8% | 90/90 = 100.0% | 90/90 = 100.0% | 62/90 = 68.9% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 96

**Selected features:** selected = {b, d, i, t, us, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |
| ActionExchange | 54 | 0/54 = 0.0% | 44/54 = 81.5% | 46/54 = 85.2% | 54/54 = 100.0% | 54/54 = 100.0% | 44/54 = 81.5% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 97

**Selected features:** selected = {b, cd, cw, d, dl, i, o, t, us, w}

**Repaired FTS:** 19 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 28/31 = 90.3% | 25/31 = 80.6% | 31/31 = 100.0% | 31/31 = 100.0% | 25/31 = 80.6% |
| ActionExchange | 137 | 0/137 = 0.0% | 127/137 = 92.7% | 110/137 = 80.3% | 137/137 = 100.0% | 137/137 = 100.0% | 114/137 = 83.2% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 98

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, o, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 16/28 = 57.1% |
| ActionExchange | 131 | 0/131 = 0.0% | 121/131 = 92.4% | 106/131 = 80.9% | 131/131 = 100.0% | 131/131 = 100.0% | 87/131 = 66.4% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |

### Product 99

**Selected features:** selected = {b, c, d, i, ie, t, tl, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 16/24 = 66.7% |
| ActionExchange | 83 | 0/83 = 0.0% | 71/83 = 85.5% | 68/83 = 81.9% | 83/83 = 100.0% | 83/83 = 100.0% | 60/83 = 72.3% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |

### Product 100

**Selected features:** selected = {b, d, i, up, us, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 54 | 0/54 = 0.0% | 48/54 = 88.9% | 50/54 = 92.6% | 54/54 = 100.0% | 54/54 = 100.0% | 43/54 = 79.6% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 101

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, t, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 21/28 = 75.0% |
| ActionExchange | 106 | 0/106 = 0.0% | 99/106 = 93.4% | 84/106 = 79.2% | 106/106 = 100.0% | 106/106 = 100.0% | 83/106 = 78.3% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |

### Product 102

**Selected features:** selected = {b, d, eu, i, ie, t, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 19/22 = 86.4% | 22/22 = 100.0% | 22/22 = 100.0% | 18/22 = 81.8% |
| ActionExchange | 62 | 0/62 = 0.0% | 52/62 = 83.9% | 50/62 = 80.6% | 62/62 = 100.0% | 62/62 = 100.0% | 52/62 = 83.9% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 103

**Selected features:** selected = {b, c, cw, d, eu, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |
| ActionExchange | 36 | 0/36 = 0.0% | 31/36 = 86.1% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 104

**Selected features:** selected = {b, cw, d, dl, t, tl, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 23/28 = 82.1% |
| ActionExchange | 101 | 0/101 = 0.0% | 89/101 = 88.1% | 81/101 = 80.2% | 101/101 = 100.0% | 101/101 = 100.0% | 80/101 = 79.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 105

**Selected features:** selected = {b, cd, d, i, ie, t, up, us, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 23/30 = 76.7% |
| ActionExchange | 108 | 0/108 = 0.0% | 101/108 = 93.5% | 93/108 = 86.1% | 108/108 = 100.0% | 108/108 = 100.0% | 85/108 = 78.7% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 16/19 = 84.2% |

### Product 106

**Selected features:** selected = {b, c, cw, d, dl, eu, i, t, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 15/27 = 55.6% |
| ActionExchange | 117 | 0/117 = 0.0% | 103/117 = 88.0% | 93/117 = 79.5% | 117/117 = 100.0% | 117/117 = 100.0% | 69/117 = 59.0% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 107

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, us, w}

**Repaired FTS:** 15 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 20/25 = 80.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 105 | 0/105 = 0.0% | 103/105 = 98.1% | 83/105 = 79.0% | 105/105 = 100.0% | 105/105 = 100.0% | 82/105 = 78.1% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 108

**Selected features:** selected = {b, c, cd, d, tl, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |
| ActionExchange | 43 | 0/43 = 0.0% | 41/43 = 95.3% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% |

### Product 109

**Selected features:** selected = {b, cw, d, i, ie, us, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 13/16 = 81.3% |
| ActionExchange | 43 | 0/43 = 0.0% | 38/43 = 88.4% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 35/43 = 81.4% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 9/10 = 90.0% |

### Product 110

**Selected features:** selected = {b, cd, d, t, up, us, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 24/26 = 92.3% | 26/26 = 100.0% | 26/26 = 100.0% | 16/26 = 61.5% |
| ActionExchange | 77 | 0/77 = 0.0% | 71/77 = 92.2% | 69/77 = 89.6% | 77/77 = 100.0% | 77/77 = 100.0% | 47/77 = 61.0% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 111

**Selected features:** selected = {b, cd, cw, d, dl, o, t, up, us, w}

**Repaired FTS:** 21 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 31/34 = 91.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 25/34 = 73.5% |
| ActionExchange | 148 | 0/148 = 0.0% | 138/148 = 93.2% | 123/148 = 83.1% | 148/148 = 100.0% | 148/148 = 100.0% | 112/148 = 75.7% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 18/20 = 90.0% |

### Product 112

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, t, us, w}

**Repaired FTS:** 20 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 26/32 = 81.3% | 32/32 = 100.0% | 32/32 = 100.0% | 27/32 = 84.4% |
| ActionExchange | 143 | 0/143 = 0.0% | 135/143 = 94.4% | 111/143 = 77.6% | 143/143 = 100.0% | 143/143 = 100.0% | 121/143 = 84.6% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 18/19 = 94.7% |

### Product 113

**Selected features:** selected = {b, c, d, eu, i, t, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 21/22 = 95.5% |
| ActionExchange | 74 | 0/74 = 0.0% | 62/74 = 83.8% | 64/74 = 86.5% | 74/74 = 100.0% | 74/74 = 100.0% | 69/74 = 93.2% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% |

### Product 114

**Selected features:** selected = {b, d, i, t, tl, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |
| ActionExchange | 54 | 0/54 = 0.0% | 44/54 = 81.5% | 46/54 = 85.2% | 54/54 = 100.0% | 54/54 = 100.0% | 44/54 = 81.5% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 115

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, t, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 22/33 = 66.7% |
| ActionExchange | 144 | 0/144 = 0.0% | 136/144 = 94.4% | 120/144 = 83.3% | 144/144 = 100.0% | 144/144 = 100.0% | 97/144 = 67.4% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |

### Product 116

**Selected features:** selected = {b, c, d, eu, up, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 54 | 0/54 = 0.0% | 48/54 = 88.9% | 50/54 = 92.6% | 54/54 = 100.0% | 54/54 = 100.0% | 40/54 = 74.1% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 117

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, o, t, up, w}

**Repaired FTS:** 23 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 35/38 = 92.1% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 32/38 = 84.2% |
| ActionExchange | 191 | 0/191 = 0.0% | 180/191 = 94.2% | 155/191 = 81.2% | 191/191 = 100.0% | 191/191 = 100.0% | 160/191 = 83.8% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 20/22 = 90.9% |

### Product 118

**Selected features:** selected = {b, c, cd, d, up, us, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 68 | 0/68 = 0.0% | 66/68 = 97.1% | 64/68 = 94.1% | 68/68 = 100.0% | 68/68 = 100.0% | 53/68 = 77.9% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 119

**Selected features:** selected = {b, d, i, us, w}

**Repaired FTS:** 10 states, 13 transitions (13 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 10/13 = 76.9% | 12/13 = 92.3% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |
| ActionExchange | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 25/30 = 83.3% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 120

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, o, t, w}

**Repaired FTS:** 19 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 28/31 = 90.3% | 25/31 = 80.6% | 31/31 = 100.0% | 31/31 = 100.0% | 25/31 = 80.6% |
| ActionExchange | 137 | 0/137 = 0.0% | 127/137 = 92.7% | 110/137 = 80.3% | 137/137 = 100.0% | 137/137 = 100.0% | 114/137 = 83.2% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 121

**Selected features:** selected = {b, cd, d, t, tl, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 51 | 0/51 = 0.0% | 46/51 = 90.2% | 43/51 = 84.3% | 51/51 = 100.0% | 51/51 = 100.0% | 36/51 = 70.6% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 122

**Selected features:** selected = {b, cw, d, dl, o, up, us, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 18/24 = 75.0% |
| ActionExchange | 95 | 0/95 = 0.0% | 86/95 = 90.5% | 79/95 = 83.2% | 95/95 = 100.0% | 95/95 = 100.0% | 73/95 = 76.8% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 123

**Selected features:** selected = {b, c, cw, d, dl, i, ie, up, us, w}

**Repaired FTS:** 17 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 24/27 = 88.9% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 16/27 = 59.3% |
| ActionExchange | 127 | 0/127 = 0.0% | 119/127 = 93.7% | 103/127 = 81.1% | 127/127 = 100.0% | 127/127 = 100.0% | 87/127 = 68.5% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |

### Product 124

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 17/20 = 85.0% |
| ActionExchange | 68 | 0/68 = 0.0% | 62/68 = 91.2% | 52/68 = 76.5% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% |

### Product 125

**Selected features:** selected = {b, c, cd, d, eu, t, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 68 | 0/68 = 0.0% | 62/68 = 91.2% | 58/68 = 85.3% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |

### Product 126

**Selected features:** selected = {b, c, cd, d, eu, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |
| ActionExchange | 43 | 0/43 = 0.0% | 41/43 = 95.3% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% |

### Product 127

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, t, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 27/32 = 84.4% | 32/32 = 100.0% | 32/32 = 100.0% | 22/32 = 68.8% |
| ActionExchange | 138 | 0/138 = 0.0% | 124/138 = 89.9% | 108/138 = 78.3% | 138/138 = 100.0% | 138/138 = 100.0% | 99/138 = 71.7% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 15/20 = 75.0% |

### Product 128

**Selected features:** selected = {b, c, d, i, ie, us, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |
| ActionExchange | 53 | 0/53 = 0.0% | 47/53 = 88.7% | 45/53 = 84.9% | 53/53 = 100.0% | 53/53 = 100.0% | 40/53 = 75.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 129

**Selected features:** selected = {b, cd, cw, d, i, ie, us, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 14/19 = 73.7% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 56 | 0/56 = 0.0% | 54/56 = 96.4% | 41/56 = 73.2% | 56/56 = 100.0% | 56/56 = 100.0% | 38/56 = 67.9% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 130

**Selected features:** selected = {b, c, cw, d, tl, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |
| ActionExchange | 36 | 0/36 = 0.0% | 31/36 = 86.1% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 131

**Selected features:** selected = {b, c, d, i, tl, up, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 19/20 = 95.0% | 20/20 = 100.0% | 20/20 = 100.0% | 18/20 = 90.0% |
| ActionExchange | 74 | 0/74 = 0.0% | 67/74 = 90.5% | 69/74 = 93.2% | 74/74 = 100.0% | 74/74 = 100.0% | 68/74 = 91.9% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 132

**Selected features:** selected = {b, d, i, ie, t, tl, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 19/22 = 86.4% | 22/22 = 100.0% | 22/22 = 100.0% | 18/22 = 81.8% |
| ActionExchange | 62 | 0/62 = 0.0% | 52/62 = 83.9% | 50/62 = 80.6% | 62/62 = 100.0% | 62/62 = 100.0% | 52/62 = 83.9% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 133

**Selected features:** selected = {b, c, cw, d, i, tl, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |
| ActionExchange | 82 | 0/82 = 0.0% | 75/82 = 91.5% | 72/82 = 87.8% | 82/82 = 100.0% | 82/82 = 100.0% | 76/82 = 92.7% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 134

**Selected features:** selected = {b, c, cd, d, eu, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 68 | 0/68 = 0.0% | 66/68 = 97.1% | 64/68 = 94.1% | 68/68 = 100.0% | 68/68 = 100.0% | 53/68 = 77.9% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 135

**Selected features:** selected = {b, cw, d, dl, i, o, tl, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 85 | 0/85 = 0.0% | 76/85 = 89.4% | 69/85 = 81.2% | 85/85 = 100.0% | 85/85 = 100.0% | 58/85 = 68.2% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 136

**Selected features:** selected = {b, c, cw, d, dl, t, tl, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 26/30 = 86.7% |
| ActionExchange | 128 | 0/128 = 0.0% | 114/128 = 89.1% | 104/128 = 81.3% | 128/128 = 100.0% | 128/128 = 100.0% | 109/128 = 85.2% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 18/19 = 94.7% |

### Product 137

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, t, tl, up, w}

**Repaired FTS:** 23 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 35/38 = 92.1% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 32/38 = 84.2% |
| ActionExchange | 191 | 0/191 = 0.0% | 180/191 = 94.2% | 155/191 = 81.2% | 191/191 = 100.0% | 191/191 = 100.0% | 160/191 = 83.8% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 20/22 = 90.9% |

### Product 138

**Selected features:** selected = {b, c, d, eu, t, up, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 84 | 0/84 = 0.0% | 72/84 = 85.7% | 74/84 = 88.1% | 84/84 = 100.0% | 84/84 = 100.0% | 68/84 = 81.0% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |

### Product 139

**Selected features:** selected = {b, c, cd, d, t, up, us, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 22/28 = 78.6% |
| ActionExchange | 99 | 0/99 = 0.0% | 92/99 = 92.9% | 89/99 = 89.9% | 99/99 = 100.0% | 99/99 = 100.0% | 80/99 = 80.8% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |

### Product 140

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, t, up, us, w}

**Repaired FTS:** 23 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 35/38 = 92.1% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 32/38 = 84.2% |
| ActionExchange | 191 | 0/191 = 0.0% | 180/191 = 94.2% | 155/191 = 81.2% | 191/191 = 100.0% | 191/191 = 100.0% | 160/191 = 83.8% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 20/22 = 90.9% |

### Product 141

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, t, tl, w}

**Repaired FTS:** 20 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 30/33 = 90.9% | 26/33 = 78.8% | 33/33 = 100.0% | 33/33 = 100.0% | 27/33 = 81.8% |
| ActionExchange | 147 | 0/147 = 0.0% | 137/147 = 93.2% | 114/147 = 77.6% | 147/147 = 100.0% | 147/147 = 100.0% | 123/147 = 83.7% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 18/19 = 94.7% |

### Product 142

**Selected features:** selected = {b, c, cw, d, dl, i, t, tl, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 15/27 = 55.6% |
| ActionExchange | 117 | 0/117 = 0.0% | 103/117 = 88.0% | 93/117 = 79.5% | 117/117 = 100.0% | 117/117 = 100.0% | 69/117 = 59.0% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 143

**Selected features:** selected = {b, cd, cw, d, dl, i, o, t, tl, up, w}

**Repaired FTS:** 22 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 33/36 = 91.7% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 30/36 = 83.3% |
| ActionExchange | 180 | 0/180 = 0.0% | 169/180 = 93.9% | 151/180 = 83.9% | 180/180 = 100.0% | 180/180 = 100.0% | 154/180 = 85.6% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |

### Product 144

**Selected features:** selected = {b, c, cw, d, dl, eu, i, t, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 19/32 = 59.4% |
| ActionExchange | 159 | 0/159 = 0.0% | 143/159 = 89.9% | 131/159 = 82.4% | 159/159 = 100.0% | 159/159 = 100.0% | 115/159 = 72.3% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |

### Product 145

**Selected features:** selected = {b, c, cw, d, dl, tl, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |
| ActionExchange | 60 | 0/60 = 0.0% | 54/60 = 90.0% | 48/60 = 80.0% | 60/60 = 100.0% | 60/60 = 100.0% | 52/60 = 86.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 146

**Selected features:** selected = {b, cd, d, eu, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 13/14 = 92.9% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |
| ActionExchange | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 18/31 = 58.1% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 147

**Selected features:** selected = {b, cd, cw, d, dl, tl, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 11/19 = 57.9% |
| ActionExchange | 56 | 0/56 = 0.0% | 54/56 = 96.4% | 45/56 = 80.4% | 56/56 = 100.0% | 56/56 = 100.0% | 35/56 = 62.5% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 148

**Selected features:** selected = {b, c, cd, cw, d, i, t, us, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 97 | 0/97 = 0.0% | 90/97 = 92.8% | 80/97 = 82.5% | 97/97 = 100.0% | 97/97 = 100.0% | 73/97 = 75.3% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 149

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, tl, w}

**Repaired FTS:** 15 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 20/26 = 76.9% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 109 | 0/109 = 0.0% | 105/109 = 96.3% | 86/109 = 78.9% | 109/109 = 100.0% | 109/109 = 100.0% | 84/109 = 77.1% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 150

**Selected features:** selected = {b, cd, d, i, tl, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 68 | 0/68 = 0.0% | 66/68 = 97.1% | 64/68 = 94.1% | 68/68 = 100.0% | 68/68 = 100.0% | 53/68 = 77.9% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 151

**Selected features:** selected = {b, c, cd, d, i, ie, t, tl, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 23/27 = 85.2% |
| ActionExchange | 98 | 0/98 = 0.0% | 91/98 = 92.9% | 81/98 = 82.7% | 98/98 = 100.0% | 98/98 = 100.0% | 82/98 = 83.7% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 152

**Selected features:** selected = {b, cw, d, dl, eu, o, t, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 21/26 = 80.8% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 95 | 0/95 = 0.0% | 81/95 = 85.3% | 74/95 = 77.9% | 95/95 = 100.0% | 95/95 = 100.0% | 73/95 = 76.8% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 153

**Selected features:** selected = {b, cd, cw, d, eu, i, ie, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 14/19 = 73.7% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 56 | 0/56 = 0.0% | 54/56 = 96.4% | 41/56 = 73.2% | 56/56 = 100.0% | 56/56 = 100.0% | 38/56 = 67.9% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 154

**Selected features:** selected = {b, cw, d, dl, i, ie, o, tl, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 15/23 = 65.2% |
| ActionExchange | 94 | 0/94 = 0.0% | 85/94 = 90.4% | 73/94 = 77.7% | 94/94 = 100.0% | 94/94 = 100.0% | 67/94 = 71.3% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 155

**Selected features:** selected = {b, c, d, eu, t, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 19/20 = 95.0% |
| ActionExchange | 54 | 0/54 = 0.0% | 44/54 = 81.5% | 46/54 = 85.2% | 54/54 = 100.0% | 54/54 = 100.0% | 50/54 = 92.6% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% |

### Product 156

**Selected features:** selected = {b, cw, d, eu, i, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 23 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |
| ActionExchange | 36 | 0/36 = 0.0% | 31/36 = 86.1% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 157

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 106 | 0/106 = 0.0% | 104/106 = 98.1% | 91/106 = 85.8% | 106/106 = 100.0% | 106/106 = 100.0% | 82/106 = 77.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 158

**Selected features:** selected = {b, c, cw, d, eu, t, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |
| ActionExchange | 61 | 0/61 = 0.0% | 51/61 = 83.6% | 49/61 = 80.3% | 61/61 = 100.0% | 61/61 = 100.0% | 41/61 = 67.2% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |

### Product 159

**Selected features:** selected = {b, d, eu, i, t, up, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 22/25 = 88.0% |
| ActionExchange | 84 | 0/84 = 0.0% | 72/84 = 85.7% | 74/84 = 88.1% | 84/84 = 100.0% | 84/84 = 100.0% | 73/84 = 86.9% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 160

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, ie, t, up, w}

**Repaired FTS:** 23 states, 37 transitions (37 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 37 | 0/37 = 0.0% | 35/37 = 94.6% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 33/37 = 89.2% |
| ActionExchange | 187 | 0/187 = 0.0% | 178/187 = 95.2% | 152/187 = 81.3% | 187/187 = 100.0% | 187/187 = 100.0% | 164/187 = 87.7% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 21/22 = 95.5% |

### Product 161

**Selected features:** selected = {b, cw, d, dl, eu, i, t, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 20/30 = 66.7% |
| ActionExchange | 128 | 0/128 = 0.0% | 114/128 = 89.1% | 104/128 = 81.3% | 128/128 = 100.0% | 128/128 = 100.0% | 89/128 = 69.5% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 14/19 = 73.7% |

### Product 162

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 15/25 = 60.0% |
| ActionExchange | 100 | 0/100 = 0.0% | 93/100 = 93.0% | 80/100 = 80.0% | 100/100 = 100.0% | 100/100 = 100.0% | 65/100 = 65.0% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 163

**Selected features:** selected = {b, cw, d, eu, t, w}

**Repaired FTS:** 14 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 16/19 = 84.2% | 19/19 = 100.0% | 19/19 = 100.0% | 15/19 = 78.9% |
| ActionExchange | 44 | 0/44 = 0.0% | 36/44 = 81.8% | 35/44 = 79.5% | 44/44 = 100.0% | 44/44 = 100.0% | 36/44 = 81.8% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 164

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, o, up, w}

**Repaired FTS:** 18 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 15 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 21/31 = 67.7% |
| ActionExchange | 147 | 0/147 = 0.0% | 143/147 = 97.3% | 122/147 = 83.0% | 147/147 = 100.0% | 147/147 = 100.0% | 114/147 = 77.6% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 165

**Selected features:** selected = {b, cd, d, up, us, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 18/19 = 94.7% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |
| ActionExchange | 51 | 0/51 = 0.0% | 49/51 = 96.1% | 48/51 = 94.1% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 11/12 = 91.7% |

### Product 166

**Selected features:** selected = {b, cd, d, i, ie, us, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |
| ActionExchange | 50 | 0/50 = 0.0% | 48/50 = 96.0% | 42/50 = 84.0% | 50/50 = 100.0% | 50/50 = 100.0% | 45/50 = 90.0% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 167

**Selected features:** selected = {b, cd, cw, d, dl, i, t, tl, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 22/33 = 66.7% |
| ActionExchange | 144 | 0/144 = 0.0% | 136/144 = 94.4% | 120/144 = 83.3% | 144/144 = 100.0% | 144/144 = 100.0% | 97/144 = 67.4% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |

### Product 168

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, o, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 15/23 = 65.2% |
| ActionExchange | 94 | 0/94 = 0.0% | 85/94 = 90.4% | 73/94 = 77.7% | 94/94 = 100.0% | 94/94 = 100.0% | 67/94 = 71.3% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 169

**Selected features:** selected = {b, c, cd, d, i, us, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 15/18 = 83.3% |
| ActionExchange | 59 | 0/59 = 0.0% | 57/59 = 96.6% | 53/59 = 89.8% | 59/59 = 100.0% | 59/59 = 100.0% | 53/59 = 89.8% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 170

**Selected features:** selected = {b, c, d, eu, i, t, up, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 25/27 = 92.6% | 27/27 = 100.0% | 27/27 = 100.0% | 18/27 = 66.7% |
| ActionExchange | 109 | 0/109 = 0.0% | 95/109 = 87.2% | 97/109 = 89.0% | 109/109 = 100.0% | 109/109 = 100.0% | 82/109 = 75.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 171

**Selected features:** selected = {b, cd, d, eu, i, ie, t, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 23/30 = 76.7% |
| ActionExchange | 108 | 0/108 = 0.0% | 101/108 = 93.5% | 93/108 = 86.1% | 108/108 = 100.0% | 108/108 = 100.0% | 85/108 = 78.7% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 16/19 = 84.2% |

### Product 172

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, t, up, us, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 26/35 = 74.3% |
| ActionExchange | 154 | 0/154 = 0.0% | 146/154 = 94.8% | 124/154 = 80.5% | 154/154 = 100.0% | 154/154 = 100.0% | 114/154 = 74.0% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |

### Product 173

**Selected features:** selected = {b, c, cw, d, dl, i, ie, us, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 90 | 0/90 = 0.0% | 83/90 = 92.2% | 70/90 = 77.8% | 90/90 = 100.0% | 90/90 = 100.0% | 62/90 = 68.9% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 174

**Selected features:** selected = {b, cw, d, dl, i, ie, t, us, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 22/27 = 81.5% | 27/27 = 100.0% | 27/27 = 100.0% | 21/27 = 77.8% |
| ActionExchange | 100 | 0/100 = 0.0% | 88/100 = 88.0% | 75/100 = 75.0% | 100/100 = 100.0% | 100/100 = 100.0% | 78/100 = 78.0% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |

### Product 175

**Selected features:** selected = {b, c, d, eu, i, up, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 19/20 = 95.0% | 20/20 = 100.0% | 20/20 = 100.0% | 18/20 = 90.0% |
| ActionExchange | 74 | 0/74 = 0.0% | 67/74 = 90.5% | 69/74 = 93.2% | 74/74 = 100.0% | 74/74 = 100.0% | 68/74 = 91.9% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 176

**Selected features:** selected = {b, c, cw, d, dl, t, us, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 22/25 = 88.0% |
| ActionExchange | 91 | 0/91 = 0.0% | 79/91 = 86.8% | 71/91 = 78.0% | 91/91 = 100.0% | 91/91 = 100.0% | 76/91 = 83.5% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% |

### Product 177

**Selected features:** selected = {b, c, cd, d, i, ie, t, tl, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 26/32 = 81.3% |
| ActionExchange | 135 | 0/135 = 0.0% | 127/135 = 94.1% | 117/135 = 86.7% | 135/135 = 100.0% | 135/135 = 100.0% | 113/135 = 83.7% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 18/20 = 90.0% |

### Product 178

**Selected features:** selected = {b, d, tl, up, w}

**Repaired FTS:** 12 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 15/16 = 93.8% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |
| ActionExchange | 38 | 0/38 = 0.0% | 33/38 = 86.8% | 35/38 = 92.1% | 38/38 = 100.0% | 38/38 = 100.0% | 28/38 = 73.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |

### Product 179

**Selected features:** selected = {b, d, eu, t, up, w}

**Repaired FTS:** 17 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 63 | 0/63 = 0.0% | 53/63 = 84.1% | 55/63 = 87.3% | 63/63 = 100.0% | 63/63 = 100.0% | 53/63 = 84.1% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 180

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, o, w}

**Repaired FTS:** 14 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 19/24 = 79.2% | 24/24 = 100.0% | 24/24 = 100.0% | 22/24 = 91.7% |
| ActionExchange | 100 | 0/100 = 0.0% | 96/100 = 96.0% | 82/100 = 82.0% | 100/100 = 100.0% | 100/100 = 100.0% | 93/100 = 93.0% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% |

### Product 181

**Selected features:** selected = {b, c, cw, d, eu, i, ie, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |
| ActionExchange | 60 | 0/60 = 0.0% | 54/60 = 90.0% | 48/60 = 80.0% | 60/60 = 100.0% | 60/60 = 100.0% | 41/60 = 68.3% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 182

**Selected features:** selected = {b, cd, cw, d, dl, eu, o, up, w}

**Repaired FTS:** 16 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 18/27 = 66.7% |
| ActionExchange | 110 | 0/110 = 0.0% | 106/110 = 96.4% | 94/110 = 85.5% | 110/110 = 100.0% | 110/110 = 100.0% | 84/110 = 76.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 183

**Selected features:** selected = {b, c, d, i, t, tl, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 21/22 = 95.5% |
| ActionExchange | 74 | 0/74 = 0.0% | 62/74 = 83.8% | 64/74 = 86.5% | 74/74 = 100.0% | 74/74 = 100.0% | 69/74 = 93.2% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% |

### Product 184

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, o, t, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 30/35 = 85.7% | 29/35 = 82.9% | 35/35 = 100.0% | 35/35 = 100.0% | 18/35 = 51.4% |
| ActionExchange | 174 | 0/174 = 0.0% | 156/174 = 89.7% | 138/174 = 79.3% | 174/174 = 100.0% | 174/174 = 100.0% | 99/174 = 56.9% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |

### Product 185

**Selected features:** selected = {b, c, cd, cw, d, t, up, us, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 18/29 = 62.1% |
| ActionExchange | 107 | 0/107 = 0.0% | 100/107 = 93.5% | 90/107 = 84.1% | 107/107 = 100.0% | 107/107 = 100.0% | 76/107 = 71.0% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |

### Product 186

**Selected features:** selected = {b, c, d, i, tl, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 14/15 = 93.3% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |
| ActionExchange | 45 | 0/45 = 0.0% | 39/45 = 86.7% | 41/45 = 91.1% | 45/45 = 100.0% | 45/45 = 100.0% | 34/45 = 75.6% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 187

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, o, t, w}

**Repaired FTS:** 20 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 30/33 = 90.9% | 26/33 = 78.8% | 33/33 = 100.0% | 33/33 = 100.0% | 27/33 = 81.8% |
| ActionExchange | 147 | 0/147 = 0.0% | 137/147 = 93.2% | 114/147 = 77.6% | 147/147 = 100.0% | 147/147 = 100.0% | 123/147 = 83.7% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 18/19 = 94.7% |

### Product 188

**Selected features:** selected = {b, c, d, i, ie, t, tl, up, w}

**Repaired FTS:** 20 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 26/29 = 89.7% | 29/29 = 100.0% | 29/29 = 100.0% | 18/29 = 62.1% |
| ActionExchange | 119 | 0/119 = 0.0% | 105/119 = 88.2% | 101/119 = 84.9% | 119/119 = 100.0% | 119/119 = 100.0% | 82/119 = 68.9% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 189

**Selected features:** selected = {b, d, eu, up, w}

**Repaired FTS:** 12 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 15/16 = 93.8% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |
| ActionExchange | 38 | 0/38 = 0.0% | 33/38 = 86.8% | 35/38 = 92.1% | 38/38 = 100.0% | 38/38 = 100.0% | 28/38 = 73.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |

### Product 190

**Selected features:** selected = {b, c, cw, d, t, tl, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |
| ActionExchange | 61 | 0/61 = 0.0% | 51/61 = 83.6% | 49/61 = 80.3% | 61/61 = 100.0% | 61/61 = 100.0% | 41/61 = 67.2% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |

### Product 191

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 80/82 = 97.6% | 64/82 = 78.0% | 82/82 = 100.0% | 82/82 = 100.0% | 66/82 = 80.5% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 192

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, up, us, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 23/28 = 82.1% |
| ActionExchange | 115 | 0/115 = 0.0% | 113/115 = 98.3% | 95/115 = 82.6% | 115/115 = 100.0% | 115/115 = 100.0% | 98/115 = 85.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 193

**Selected features:** selected = {b, d, i, tl, w}

**Repaired FTS:** 10 states, 13 transitions (13 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 10/13 = 76.9% | 12/13 = 92.3% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |
| ActionExchange | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 25/30 = 83.3% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 194

**Selected features:** selected = {b, cd, cw, d, i, ie, up, us, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 17/24 = 70.8% |
| ActionExchange | 83 | 0/83 = 0.0% | 81/83 = 97.6% | 69/83 = 83.1% | 83/83 = 100.0% | 83/83 = 100.0% | 60/83 = 72.3% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 195

**Selected features:** selected = {b, c, cw, d, t, up, us, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 92 | 0/92 = 0.0% | 80/92 = 87.0% | 77/92 = 83.7% | 92/92 = 100.0% | 92/92 = 100.0% | 71/92 = 77.2% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |

### Product 196

**Selected features:** selected = {b, cd, d, us, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 13/14 = 92.9% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |
| ActionExchange | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 18/31 = 58.1% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 197

**Selected features:** selected = {b, d, i, t, tl, up, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 22/25 = 88.0% |
| ActionExchange | 84 | 0/84 = 0.0% | 72/84 = 85.7% | 74/84 = 88.1% | 84/84 = 100.0% | 84/84 = 100.0% | 73/84 = 86.9% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 198

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, t, us, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 25/30 = 83.3% |
| ActionExchange | 115 | 0/115 = 0.0% | 108/115 = 93.9% | 88/115 = 76.5% | 115/115 = 100.0% | 115/115 = 100.0% | 94/115 = 81.7% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 199

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, t, tl, up, w}

**Repaired FTS:** 23 states, 37 transitions (37 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 37 | 0/37 = 0.0% | 35/37 = 94.6% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 33/37 = 89.2% |
| ActionExchange | 187 | 0/187 = 0.0% | 178/187 = 95.2% | 152/187 = 81.3% | 187/187 = 100.0% | 187/187 = 100.0% | 164/187 = 87.7% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 21/22 = 95.5% |

### Product 200

**Selected features:** selected = {b, c, cd, cw, d, eu, i, ie, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 15/26 = 57.7% |
| ActionExchange | 106 | 0/106 = 0.0% | 104/106 = 98.1% | 89/106 = 84.0% | 106/106 = 100.0% | 106/106 = 100.0% | 67/106 = 63.2% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |

### Product 201

**Selected features:** selected = {b, cw, d, dl, i, ie, up, us, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 15/25 = 60.0% |
| ActionExchange | 100 | 0/100 = 0.0% | 93/100 = 93.0% | 80/100 = 80.0% | 100/100 = 100.0% | 100/100 = 100.0% | 65/100 = 65.0% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 202

**Selected features:** selected = {b, cw, d, dl, i, ie, o, t, us, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 22/30 = 73.3% |
| ActionExchange | 131 | 0/131 = 0.0% | 115/131 = 87.8% | 100/131 = 76.3% | 131/131 = 100.0% | 131/131 = 100.0% | 101/131 = 77.1% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 14/18 = 77.8% |

### Product 203

**Selected features:** selected = {b, cd, d, i, up, us, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 68 | 0/68 = 0.0% | 66/68 = 97.1% | 64/68 = 94.1% | 68/68 = 100.0% | 68/68 = 100.0% | 53/68 = 77.9% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 204

**Selected features:** selected = {b, d, i, tl, up, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 54 | 0/54 = 0.0% | 48/54 = 88.9% | 50/54 = 92.6% | 54/54 = 100.0% | 54/54 = 100.0% | 43/54 = 79.6% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 205

**Selected features:** selected = {b, cd, cw, d, dl, o, tl, up, w}

**Repaired FTS:** 16 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 18/27 = 66.7% |
| ActionExchange | 110 | 0/110 = 0.0% | 106/110 = 96.4% | 94/110 = 85.5% | 110/110 = 100.0% | 110/110 = 100.0% | 84/110 = 76.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 206

**Selected features:** selected = {b, cd, cw, d, dl, up, us, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 17/24 = 70.8% |
| ActionExchange | 83 | 0/83 = 0.0% | 81/83 = 97.6% | 71/83 = 85.5% | 83/83 = 100.0% | 83/83 = 100.0% | 62/83 = 74.7% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 207

**Selected features:** selected = {b, cd, d, eu, i, t, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 22/28 = 78.6% |
| ActionExchange | 99 | 0/99 = 0.0% | 92/99 = 92.9% | 89/99 = 89.9% | 99/99 = 100.0% | 99/99 = 100.0% | 80/99 = 80.8% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |

### Product 208

**Selected features:** selected = {b, cd, cw, d, dl, us, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 11/19 = 57.9% |
| ActionExchange | 56 | 0/56 = 0.0% | 54/56 = 96.4% | 45/56 = 80.4% | 56/56 = 100.0% | 56/56 = 100.0% | 35/56 = 62.5% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 209

**Selected features:** selected = {b, c, cd, d, i, ie, tl, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 14/20 = 70.0% |
| ActionExchange | 67 | 0/67 = 0.0% | 65/67 = 97.0% | 57/67 = 85.1% | 67/67 = 100.0% | 67/67 = 100.0% | 51/67 = 76.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 210

**Selected features:** selected = {b, cd, cw, d, dl, i, tl, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 74 | 0/74 = 0.0% | 72/74 = 97.3% | 60/74 = 81.1% | 74/74 = 100.0% | 74/74 = 100.0% | 60/74 = 81.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 211

**Selected features:** selected = {b, d, eu, i, ie, t, up, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 24/27 = 88.9% | 27/27 = 100.0% | 27/27 = 100.0% | 22/27 = 81.5% |
| ActionExchange | 93 | 0/93 = 0.0% | 81/93 = 87.1% | 78/93 = 83.9% | 93/93 = 100.0% | 93/93 = 100.0% | 75/93 = 80.6% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |

### Product 212

**Selected features:** selected = {b, c, cw, d, eu, i, ie, t, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 16/25 = 64.0% |
| ActionExchange | 91 | 0/91 = 0.0% | 79/91 = 86.8% | 71/91 = 78.0% | 91/91 = 100.0% | 91/91 = 100.0% | 60/91 = 65.9% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |

### Product 213

**Selected features:** selected = {b, c, cd, cw, d, i, t, up, us, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 19/31 = 61.3% |
| ActionExchange | 134 | 0/134 = 0.0% | 126/134 = 94.0% | 114/134 = 85.1% | 134/134 = 100.0% | 134/134 = 100.0% | 94/134 = 70.1% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 214

**Selected features:** selected = {b, cd, d, tl, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 13/14 = 92.9% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |
| ActionExchange | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 18/31 = 58.1% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 215

**Selected features:** selected = {b, cw, d, dl, i, ie, us, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 17/20 = 85.0% |
| ActionExchange | 68 | 0/68 = 0.0% | 62/68 = 91.2% | 52/68 = 76.5% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% |

### Product 216

**Selected features:** selected = {b, c, cw, d, dl, eu, i, ie, t, up, w}

**Repaired FTS:** 22 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 30/34 = 88.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 19/34 = 55.9% |
| ActionExchange | 170 | 0/170 = 0.0% | 154/170 = 90.6% | 135/170 = 79.4% | 170/170 = 100.0% | 170/170 = 100.0% | 116/170 = 68.2% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |

### Product 217

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, t, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 25/30 = 83.3% | 30/30 = 100.0% | 30/30 = 100.0% | 25/30 = 83.3% |
| ActionExchange | 133 | 0/133 = 0.0% | 125/133 = 94.0% | 107/133 = 80.5% | 133/133 = 100.0% | 133/133 = 100.0% | 112/133 = 84.2% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 218

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, up, us, w}

**Repaired FTS:** 18 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 29/30 = 96.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 21/30 = 70.0% |
| ActionExchange | 143 | 0/143 = 0.0% | 141/143 = 98.6% | 119/143 = 83.2% | 143/143 = 100.0% | 143/143 = 100.0% | 112/143 = 78.3% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 219

**Selected features:** selected = {b, cd, cw, d, dl, i, o, us, w}

**Repaired FTS:** 14 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 19/24 = 79.2% | 24/24 = 100.0% | 24/24 = 100.0% | 22/24 = 91.7% |
| ActionExchange | 100 | 0/100 = 0.0% | 96/100 = 96.0% | 82/100 = 82.0% | 100/100 = 100.0% | 100/100 = 100.0% | 93/100 = 93.0% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% |

### Product 220

**Selected features:** selected = {b, c, d, i, ie, tl, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |
| ActionExchange | 53 | 0/53 = 0.0% | 47/53 = 88.7% | 45/53 = 84.9% | 53/53 = 100.0% | 53/53 = 100.0% | 40/53 = 75.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 221

**Selected features:** selected = {b, cd, cw, d, i, ie, t, us, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 83 | 0/83 = 0.0% | 77/83 = 92.8% | 65/83 = 78.3% | 83/83 = 100.0% | 83/83 = 100.0% | 62/83 = 74.7% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 222

**Selected features:** selected = {b, d, i, ie, tl, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |
| ActionExchange | 37 | 0/37 = 0.0% | 32/37 = 86.5% | 31/37 = 83.8% | 37/37 = 100.0% | 37/37 = 100.0% | 28/37 = 75.7% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 223

**Selected features:** selected = {b, cd, cw, d, dl, eu, o, t, up, w}

**Repaired FTS:** 21 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 31/34 = 91.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 25/34 = 73.5% |
| ActionExchange | 148 | 0/148 = 0.0% | 138/148 = 93.2% | 123/148 = 83.1% | 148/148 = 100.0% | 148/148 = 100.0% | 112/148 = 75.7% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 18/20 = 90.0% |

### Product 224

**Selected features:** selected = {b, cd, cw, d, i, tl, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |
| ActionExchange | 49 | 0/49 = 0.0% | 47/49 = 95.9% | 37/49 = 75.5% | 49/49 = 100.0% | 49/49 = 100.0% | 44/49 = 89.8% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 9/10 = 90.0% |

### Product 225

**Selected features:** selected = {b, c, cd, cw, d, eu, i, ie, t, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 16/28 = 57.1% |
| ActionExchange | 106 | 0/106 = 0.0% | 99/106 = 93.4% | 84/106 = 79.2% | 106/106 = 100.0% | 106/106 = 100.0% | 60/106 = 56.6% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 226

**Selected features:** selected = {b, cd, cw, d, tl, up, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 14/20 = 70.0% |
| ActionExchange | 57 | 0/57 = 0.0% | 55/57 = 96.5% | 45/57 = 78.9% | 57/57 = 100.0% | 57/57 = 100.0% | 46/57 = 80.7% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 227

**Selected features:** selected = {b, cw, d, dl, o, t, up, us, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 26/31 = 83.9% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 20/31 = 64.5% |
| ActionExchange | 132 | 0/132 = 0.0% | 116/132 = 87.9% | 107/132 = 81.1% | 132/132 = 100.0% | 132/132 = 100.0% | 89/132 = 67.4% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 13/19 = 68.4% |

### Product 228

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, ie, up, w}

**Repaired FTS:** 18 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 29/30 = 96.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 21/30 = 70.0% |
| ActionExchange | 143 | 0/143 = 0.0% | 141/143 = 98.6% | 119/143 = 83.2% | 143/143 = 100.0% | 143/143 = 100.0% | 112/143 = 78.3% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 229

**Selected features:** selected = {b, c, cd, cw, d, eu, up, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 75 | 0/75 = 0.0% | 73/75 = 97.3% | 60/75 = 80.0% | 75/75 = 100.0% | 75/75 = 100.0% | 52/75 = 69.3% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 230

**Selected features:** selected = {b, c, cd, cw, d, eu, t, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 21/24 = 87.5% |
| ActionExchange | 75 | 0/75 = 0.0% | 69/75 = 92.0% | 61/75 = 81.3% | 75/75 = 100.0% | 75/75 = 100.0% | 65/75 = 86.7% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |

### Product 231

**Selected features:** selected = {b, cw, d, t, tl, w}

**Repaired FTS:** 14 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 16/19 = 84.2% | 19/19 = 100.0% | 19/19 = 100.0% | 15/19 = 78.9% |
| ActionExchange | 44 | 0/44 = 0.0% | 36/44 = 81.8% | 35/44 = 79.5% | 44/44 = 100.0% | 44/44 = 100.0% | 36/44 = 81.8% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 232

**Selected features:** selected = {b, cd, cw, d, dl, i, o, up, us, w}

**Repaired FTS:** 17 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 23/29 = 79.3% |
| ActionExchange | 137 | 0/137 = 0.0% | 133/137 = 97.1% | 118/137 = 86.1% | 137/137 = 100.0% | 137/137 = 100.0% | 114/137 = 83.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 15/16 = 93.8% |

### Product 233

**Selected features:** selected = {b, d, i, ie, us, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |
| ActionExchange | 37 | 0/37 = 0.0% | 32/37 = 86.5% | 31/37 = 83.8% | 37/37 = 100.0% | 37/37 = 100.0% | 28/37 = 75.7% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 234

**Selected features:** selected = {b, c, cd, d, i, ie, t, up, us, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 26/32 = 81.3% |
| ActionExchange | 135 | 0/135 = 0.0% | 127/135 = 94.1% | 117/135 = 86.7% | 135/135 = 100.0% | 135/135 = 100.0% | 113/135 = 83.7% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 18/20 = 90.0% |

### Product 235

**Selected features:** selected = {b, c, d, i, ie, tl, up, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 17/22 = 77.3% |
| ActionExchange | 83 | 0/83 = 0.0% | 76/83 = 91.6% | 73/83 = 88.0% | 83/83 = 100.0% | 83/83 = 100.0% | 67/83 = 80.7% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 236

**Selected features:** selected = {b, c, cw, d, i, ie, t, tl, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 16/25 = 64.0% |
| ActionExchange | 91 | 0/91 = 0.0% | 79/91 = 86.8% | 71/91 = 78.0% | 91/91 = 100.0% | 91/91 = 100.0% | 60/91 = 65.9% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |

### Product 237

**Selected features:** selected = {b, cd, cw, d, eu, i, t, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 16/24 = 66.7% |
| ActionExchange | 75 | 0/75 = 0.0% | 69/75 = 92.0% | 61/75 = 81.3% | 75/75 = 100.0% | 75/75 = 100.0% | 48/75 = 64.0% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 238

**Selected features:** selected = {b, cw, d, dl, i, t, tl, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 91 | 0/91 = 0.0% | 79/91 = 86.8% | 71/91 = 78.0% | 91/91 = 100.0% | 91/91 = 100.0% | 68/91 = 74.7% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 13/16 = 81.3% |

### Product 239

**Selected features:** selected = {b, cw, d, dl, eu, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |
| ActionExchange | 43 | 0/43 = 0.0% | 38/43 = 88.4% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 32/43 = 74.4% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 240

**Selected features:** selected = {b, c, cd, cw, d, dl, t, up, us, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 22/33 = 66.7% |
| ActionExchange | 144 | 0/144 = 0.0% | 136/144 = 94.4% | 120/144 = 83.3% | 144/144 = 100.0% | 144/144 = 100.0% | 97/144 = 67.4% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |

### Product 241

**Selected features:** selected = {b, cd, cw, d, eu, t, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 57 | 0/57 = 0.0% | 52/57 = 91.2% | 42/57 = 73.7% | 57/57 = 100.0% | 57/57 = 100.0% | 40/57 = 70.2% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 242

**Selected features:** selected = {b, c, cd, d, us, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |
| ActionExchange | 43 | 0/43 = 0.0% | 41/43 = 95.3% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% |

### Product 243

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, ie, t, w}

**Repaired FTS:** 20 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 26/32 = 81.3% | 32/32 = 100.0% | 32/32 = 100.0% | 27/32 = 84.4% |
| ActionExchange | 143 | 0/143 = 0.0% | 135/143 = 94.4% | 111/143 = 77.6% | 143/143 = 100.0% | 143/143 = 100.0% | 121/143 = 84.6% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 18/19 = 94.7% |

### Product 244

**Selected features:** selected = {b, c, cd, d, eu, i, ie, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 14/20 = 70.0% |
| ActionExchange | 67 | 0/67 = 0.0% | 65/67 = 97.0% | 57/67 = 85.1% | 67/67 = 100.0% | 67/67 = 100.0% | 51/67 = 76.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 245

**Selected features:** selected = {b, c, d, i, t, tl, up, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 25/27 = 92.6% | 27/27 = 100.0% | 27/27 = 100.0% | 18/27 = 66.7% |
| ActionExchange | 109 | 0/109 = 0.0% | 95/109 = 87.2% | 97/109 = 89.0% | 109/109 = 100.0% | 109/109 = 100.0% | 82/109 = 75.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 246

**Selected features:** selected = {b, c, cd, cw, d, i, ie, up, us, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 15/26 = 57.7% |
| ActionExchange | 106 | 0/106 = 0.0% | 104/106 = 98.1% | 89/106 = 84.0% | 106/106 = 100.0% | 106/106 = 100.0% | 67/106 = 63.2% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |

### Product 247

**Selected features:** selected = {b, cw, d, i, ie, up, us, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 69 | 0/69 = 0.0% | 63/69 = 91.3% | 57/69 = 82.6% | 69/69 = 100.0% | 69/69 = 100.0% | 56/69 = 81.2% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 248

**Selected features:** selected = {b, c, cw, d, us, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |
| ActionExchange | 36 | 0/36 = 0.0% | 31/36 = 86.1% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 249

**Selected features:** selected = {b, cd, d, eu, i, ie, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |
| ActionExchange | 50 | 0/50 = 0.0% | 48/50 = 96.0% | 42/50 = 84.0% | 50/50 = 100.0% | 50/50 = 100.0% | 45/50 = 90.0% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 250

**Selected features:** selected = {b, cd, cw, d, dl, o, tl, w}

**Repaired FTS:** 13 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 17/22 = 77.3% |
| ActionExchange | 78 | 0/78 = 0.0% | 74/78 = 94.9% | 63/78 = 80.8% | 78/78 = 100.0% | 78/78 = 100.0% | 60/78 = 76.9% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 251

**Selected features:** selected = {b, c, cw, d, eu, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 61 | 0/61 = 0.0% | 55/61 = 90.2% | 53/61 = 86.9% | 61/61 = 100.0% | 61/61 = 100.0% | 41/61 = 67.2% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 252

**Selected features:** selected = {b, c, cd, cw, d, dl, up, us, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 106 | 0/106 = 0.0% | 104/106 = 98.1% | 91/106 = 85.8% | 106/106 = 100.0% | 106/106 = 100.0% | 82/106 = 77.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 253

**Selected features:** selected = {b, cw, d, dl, i, o, us, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 85 | 0/85 = 0.0% | 76/85 = 89.4% | 69/85 = 81.2% | 85/85 = 100.0% | 85/85 = 100.0% | 58/85 = 68.2% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 254

**Selected features:** selected = {b, c, cw, d, dl, i, us, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 74/81 = 91.4% | 66/81 = 81.5% | 81/81 = 100.0% | 81/81 = 100.0% | 58/81 = 71.6% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 255

**Selected features:** selected = {b, cw, d, dl, o, tl, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 18/24 = 75.0% |
| ActionExchange | 95 | 0/95 = 0.0% | 86/95 = 90.5% | 79/95 = 83.2% | 95/95 = 100.0% | 95/95 = 100.0% | 73/95 = 76.8% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 256

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, tl, w}

**Repaired FTS:** 15 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 20/25 = 80.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 105 | 0/105 = 0.0% | 103/105 = 98.1% | 83/105 = 79.0% | 105/105 = 100.0% | 105/105 = 100.0% | 82/105 = 78.1% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 257

**Selected features:** selected = {b, cd, cw, d, dl, i, us, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 74 | 0/74 = 0.0% | 72/74 = 97.3% | 60/74 = 81.1% | 74/74 = 100.0% | 74/74 = 100.0% | 60/74 = 81.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 258

**Selected features:** selected = {b, cw, d, dl, i, t, up, us, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 20/30 = 66.7% |
| ActionExchange | 128 | 0/128 = 0.0% | 114/128 = 89.1% | 104/128 = 81.3% | 128/128 = 100.0% | 128/128 = 100.0% | 89/128 = 69.5% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 14/19 = 73.7% |

### Product 259

**Selected features:** selected = {b, cw, d, dl, o, t, tl, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 21/26 = 80.8% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 95 | 0/95 = 0.0% | 81/95 = 85.3% | 74/95 = 77.9% | 95/95 = 100.0% | 95/95 = 100.0% | 73/95 = 76.8% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 260

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, up, us, w}

**Repaired FTS:** 18 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 15 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 21/31 = 67.7% |
| ActionExchange | 147 | 0/147 = 0.0% | 143/147 = 97.3% | 122/147 = 83.0% | 147/147 = 100.0% | 147/147 = 100.0% | 114/147 = 77.6% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 261

**Selected features:** selected = {b, cd, cw, d, dl, i, o, tl, w}

**Repaired FTS:** 14 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 19/24 = 79.2% | 24/24 = 100.0% | 24/24 = 100.0% | 22/24 = 91.7% |
| ActionExchange | 100 | 0/100 = 0.0% | 96/100 = 96.0% | 82/100 = 82.0% | 100/100 = 100.0% | 100/100 = 100.0% | 93/100 = 93.0% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% |

### Product 262

**Selected features:** selected = {b, cd, d, eu, i, t, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 68 | 0/68 = 0.0% | 62/68 = 91.2% | 58/68 = 85.3% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |

### Product 263

**Selected features:** selected = {b, cw, d, i, ie, t, up, us, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 24/28 = 85.7% |
| ActionExchange | 101 | 0/101 = 0.0% | 89/101 = 88.1% | 81/101 = 80.2% | 101/101 = 100.0% | 101/101 = 100.0% | 84/101 = 83.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 264

**Selected features:** selected = {b, c, cw, d, dl, t, tl, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 22/25 = 88.0% |
| ActionExchange | 91 | 0/91 = 0.0% | 79/91 = 86.8% | 71/91 = 78.0% | 91/91 = 100.0% | 91/91 = 100.0% | 76/91 = 83.5% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% |

### Product 265

**Selected features:** selected = {b, c, cd, d, t, us, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 68 | 0/68 = 0.0% | 62/68 = 91.2% | 58/68 = 85.3% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |

### Product 266

**Selected features:** selected = {b, cw, d, dl, i, o, t, up, us, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 28/33 = 84.8% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 25/33 = 75.8% |
| ActionExchange | 163 | 0/163 = 0.0% | 145/163 = 89.0% | 134/163 = 82.2% | 163/163 = 100.0% | 163/163 = 100.0% | 142/163 = 87.1% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 15/20 = 75.0% |

### Product 267

**Selected features:** selected = {b, cw, d, dl, t, us, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 20/23 = 87.0% |
| ActionExchange | 69 | 0/69 = 0.0% | 59/69 = 85.5% | 53/69 = 76.8% | 69/69 = 100.0% | 69/69 = 100.0% | 57/69 = 82.6% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% |

### Product 268

**Selected features:** selected = {b, cd, cw, d, t, us, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 57 | 0/57 = 0.0% | 52/57 = 91.2% | 42/57 = 73.7% | 57/57 = 100.0% | 57/57 = 100.0% | 40/57 = 70.2% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 269

**Selected features:** selected = {b, c, cw, d, dl, i, ie, t, us, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 24/29 = 82.8% | 29/29 = 100.0% | 29/29 = 100.0% | 17/29 = 58.6% |
| ActionExchange | 127 | 0/127 = 0.0% | 113/127 = 89.0% | 97/127 = 76.4% | 127/127 = 100.0% | 127/127 = 100.0% | 79/127 = 62.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 270

**Selected features:** selected = {b, cw, d, dl, i, o, tl, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 14/26 = 53.8% |
| ActionExchange | 121 | 0/121 = 0.0% | 111/121 = 91.7% | 102/121 = 84.3% | 121/121 = 100.0% | 121/121 = 100.0% | 77/121 = 63.6% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 271

**Selected features:** selected = {b, cd, cw, d, i, ie, t, tl, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 83 | 0/83 = 0.0% | 77/83 = 92.8% | 65/83 = 78.3% | 83/83 = 100.0% | 83/83 = 100.0% | 62/83 = 74.7% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 272

**Selected features:** selected = {b, c, d, eu, i, ie, t, up, w}

**Repaired FTS:** 20 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 26/29 = 89.7% | 29/29 = 100.0% | 29/29 = 100.0% | 18/29 = 62.1% |
| ActionExchange | 119 | 0/119 = 0.0% | 105/119 = 88.2% | 101/119 = 84.9% | 119/119 = 100.0% | 119/119 = 100.0% | 82/119 = 68.9% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 273

**Selected features:** selected = {b, cw, d, dl, eu, i, o, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 85 | 0/85 = 0.0% | 76/85 = 89.4% | 69/85 = 81.2% | 85/85 = 100.0% | 85/85 = 100.0% | 58/85 = 68.2% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 274

**Selected features:** selected = {b, cw, d, dl, tl, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |
| ActionExchange | 43 | 0/43 = 0.0% | 38/43 = 88.4% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 32/43 = 74.4% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 275

**Selected features:** selected = {b, cd, cw, d, dl, i, o, tl, up, w}

**Repaired FTS:** 17 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 23/29 = 79.3% |
| ActionExchange | 137 | 0/137 = 0.0% | 133/137 = 97.1% | 118/137 = 86.1% | 137/137 = 100.0% | 137/137 = 100.0% | 114/137 = 83.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 15/16 = 93.8% |

### Product 276

**Selected features:** selected = {b, cd, cw, d, dl, eu, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 11/19 = 57.9% |
| ActionExchange | 56 | 0/56 = 0.0% | 54/56 = 96.4% | 45/56 = 80.4% | 56/56 = 100.0% | 56/56 = 100.0% | 35/56 = 62.5% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 277

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, tl, up, w}

**Repaired FTS:** 18 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 15 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 21/31 = 67.7% |
| ActionExchange | 147 | 0/147 = 0.0% | 143/147 = 97.3% | 122/147 = 83.0% | 147/147 = 100.0% | 147/147 = 100.0% | 114/147 = 77.6% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 278

**Selected features:** selected = {b, cw, d, tl, w}

**Repaired FTS:** 9 states, 12 transitions (12 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 20 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 9/12 = 75.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 11/12 = 91.7% |
| ActionExchange | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 22/24 = 91.7% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 279

**Selected features:** selected = {b, c, cw, d, eu, i, ie, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 15/23 = 65.2% |
| ActionExchange | 91 | 0/91 = 0.0% | 84/91 = 92.3% | 76/91 = 83.5% | 91/91 = 100.0% | 91/91 = 100.0% | 63/91 = 69.2% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 280

**Selected features:** selected = {b, c, cd, cw, d, dl, tl, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 106 | 0/106 = 0.0% | 104/106 = 98.1% | 91/106 = 85.8% | 106/106 = 100.0% | 106/106 = 100.0% | 82/106 = 77.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 281

**Selected features:** selected = {b, c, cd, cw, d, t, tl, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 21/24 = 87.5% |
| ActionExchange | 75 | 0/75 = 0.0% | 69/75 = 92.0% | 61/75 = 81.3% | 75/75 = 100.0% | 75/75 = 100.0% | 65/75 = 86.7% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |

### Product 282

**Selected features:** selected = {b, cd, d, t, us, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 51 | 0/51 = 0.0% | 46/51 = 90.2% | 43/51 = 84.3% | 51/51 = 100.0% | 51/51 = 100.0% | 36/51 = 70.6% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 283

**Selected features:** selected = {b, cd, d, i, ie, up, us, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 13/23 = 56.5% |
| ActionExchange | 76 | 0/76 = 0.0% | 74/76 = 97.4% | 68/76 = 89.5% | 76/76 = 100.0% | 76/76 = 100.0% | 46/76 = 60.5% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |

### Product 284

**Selected features:** selected = {b, c, cd, cw, d, i, ie, t, tl, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 16/28 = 57.1% |
| ActionExchange | 106 | 0/106 = 0.0% | 99/106 = 93.4% | 84/106 = 79.2% | 106/106 = 100.0% | 106/106 = 100.0% | 60/106 = 56.6% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 285

**Selected features:** selected = {b, c, cd, cw, d, i, tl, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 15/24 = 62.5% |
| ActionExchange | 97 | 0/97 = 0.0% | 95/97 = 97.9% | 85/97 = 87.6% | 97/97 = 100.0% | 97/97 = 100.0% | 65/97 = 67.0% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 286

**Selected features:** selected = {b, cw, d, i, tl, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 23 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |
| ActionExchange | 36 | 0/36 = 0.0% | 31/36 = 86.1% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 287

**Selected features:** selected = {b, cd, cw, d, t, up, us, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 22/27 = 81.5% |
| ActionExchange | 84 | 0/84 = 0.0% | 78/84 = 92.9% | 70/84 = 83.3% | 84/84 = 100.0% | 84/84 = 100.0% | 67/84 = 79.8% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |

### Product 288

**Selected features:** selected = {b, cw, d, dl, t, tl, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 20/23 = 87.0% |
| ActionExchange | 69 | 0/69 = 0.0% | 59/69 = 85.5% | 53/69 = 76.8% | 69/69 = 100.0% | 69/69 = 100.0% | 57/69 = 82.6% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% |

### Product 289

**Selected features:** selected = {b, c, cd, cw, d, i, ie, us, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 16/21 = 76.2% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 74 | 0/74 = 0.0% | 72/74 = 97.3% | 55/74 = 74.3% | 74/74 = 100.0% | 74/74 = 100.0% | 51/74 = 68.9% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 290

**Selected features:** selected = {b, c, cw, d, i, ie, t, us, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 16/25 = 64.0% |
| ActionExchange | 91 | 0/91 = 0.0% | 79/91 = 86.8% | 71/91 = 78.0% | 91/91 = 100.0% | 91/91 = 100.0% | 60/91 = 65.9% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |

### Product 291

**Selected features:** selected = {b, cw, d, dl, i, ie, o, t, tl, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 30/35 = 85.7% | 29/35 = 82.9% | 35/35 = 100.0% | 35/35 = 100.0% | 18/35 = 51.4% |
| ActionExchange | 174 | 0/174 = 0.0% | 156/174 = 89.7% | 138/174 = 79.3% | 174/174 = 100.0% | 174/174 = 100.0% | 99/174 = 56.9% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |

### Product 292

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, o, t, up, w}

**Repaired FTS:** 22 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 33/36 = 91.7% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 30/36 = 83.3% |
| ActionExchange | 180 | 0/180 = 0.0% | 169/180 = 93.9% | 151/180 = 83.9% | 180/180 = 100.0% | 180/180 = 100.0% | 154/180 = 85.6% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |

### Product 293

**Selected features:** selected = {b, c, cd, d, i, ie, t, us, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 23/27 = 85.2% |
| ActionExchange | 98 | 0/98 = 0.0% | 91/98 = 92.9% | 81/98 = 82.7% | 98/98 = 100.0% | 98/98 = 100.0% | 82/98 = 83.7% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 294

**Selected features:** selected = {b, c, cd, d, i, ie, us, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 14/20 = 70.0% |
| ActionExchange | 67 | 0/67 = 0.0% | 65/67 = 97.0% | 57/67 = 85.1% | 67/67 = 100.0% | 67/67 = 100.0% | 51/67 = 76.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 295

**Selected features:** selected = {b, c, cw, d, dl, i, ie, tl, up, w}

**Repaired FTS:** 17 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 24/27 = 88.9% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 16/27 = 59.3% |
| ActionExchange | 127 | 0/127 = 0.0% | 119/127 = 93.7% | 103/127 = 81.1% | 127/127 = 100.0% | 127/127 = 100.0% | 87/127 = 68.5% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |

### Product 296

**Selected features:** selected = {b, c, cd, d, i, t, up, us, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 24/30 = 80.0% |
| ActionExchange | 125 | 0/125 = 0.0% | 117/125 = 93.6% | 113/125 = 90.4% | 125/125 = 100.0% | 125/125 = 100.0% | 103/125 = 82.4% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |

### Product 297

**Selected features:** selected = {b, cw, d, i, ie, t, tl, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 24/28 = 85.7% |
| ActionExchange | 101 | 0/101 = 0.0% | 89/101 = 88.1% | 81/101 = 80.2% | 101/101 = 100.0% | 101/101 = 100.0% | 84/101 = 83.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 298

**Selected features:** selected = {b, cw, d, dl, eu, i, o, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 14/26 = 53.8% |
| ActionExchange | 121 | 0/121 = 0.0% | 111/121 = 91.7% | 102/121 = 84.3% | 121/121 = 100.0% | 121/121 = 100.0% | 77/121 = 63.6% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 299

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, t, tl, w}

**Repaired FTS:** 20 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 26/32 = 81.3% | 32/32 = 100.0% | 32/32 = 100.0% | 27/32 = 84.4% |
| ActionExchange | 143 | 0/143 = 0.0% | 135/143 = 94.4% | 111/143 = 77.6% | 143/143 = 100.0% | 143/143 = 100.0% | 121/143 = 84.6% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 18/19 = 94.7% |

### Product 300

**Selected features:** selected = {b, cd, cw, d, dl, i, o, t, up, us, w}

**Repaired FTS:** 22 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 33/36 = 91.7% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 30/36 = 83.3% |
| ActionExchange | 180 | 0/180 = 0.0% | 169/180 = 93.9% | 151/180 = 83.9% | 180/180 = 100.0% | 180/180 = 100.0% | 154/180 = 85.6% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |

### Product 301

**Selected features:** selected = {b, c, cd, cw, d, dl, i, tl, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 23/28 = 82.1% |
| ActionExchange | 133 | 0/133 = 0.0% | 131/133 = 98.5% | 115/133 = 86.5% | 133/133 = 100.0% | 133/133 = 100.0% | 112/133 = 84.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 15/16 = 93.8% |

### Product 302

**Selected features:** selected = {b, c, cd, cw, d, t, us, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 21/24 = 87.5% |
| ActionExchange | 75 | 0/75 = 0.0% | 69/75 = 92.0% | 61/75 = 81.3% | 75/75 = 100.0% | 75/75 = 100.0% | 65/75 = 86.7% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |

### Product 303

**Selected features:** selected = {b, cd, d, eu, t, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 24/26 = 92.3% | 26/26 = 100.0% | 26/26 = 100.0% | 16/26 = 61.5% |
| ActionExchange | 77 | 0/77 = 0.0% | 71/77 = 92.2% | 69/77 = 89.6% | 77/77 = 100.0% | 77/77 = 100.0% | 47/77 = 61.0% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 304

**Selected features:** selected = {b, cd, cw, d, dl, t, us, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 83 | 0/83 = 0.0% | 77/83 = 92.8% | 65/83 = 78.3% | 83/83 = 100.0% | 83/83 = 100.0% | 63/83 = 75.9% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 305

**Selected features:** selected = {b, c, cd, cw, d, tl, up, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 75 | 0/75 = 0.0% | 73/75 = 97.3% | 60/75 = 80.0% | 75/75 = 100.0% | 75/75 = 100.0% | 52/75 = 69.3% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 306

**Selected features:** selected = {b, cw, d, dl, i, ie, t, tl, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 27/32 = 84.4% | 32/32 = 100.0% | 32/32 = 100.0% | 22/32 = 68.8% |
| ActionExchange | 138 | 0/138 = 0.0% | 124/138 = 89.9% | 108/138 = 78.3% | 138/138 = 100.0% | 138/138 = 100.0% | 99/138 = 71.7% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 15/20 = 75.0% |

### Product 307

**Selected features:** selected = {b, cw, d, t, up, us, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 21/24 = 87.5% |
| ActionExchange | 70 | 0/70 = 0.0% | 60/70 = 85.7% | 58/70 = 82.9% | 70/70 = 100.0% | 70/70 = 100.0% | 61/70 = 87.1% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 15/16 = 93.8% |

### Product 308

**Selected features:** selected = {b, cw, d, dl, eu, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 69 | 0/69 = 0.0% | 63/69 = 91.3% | 57/69 = 82.6% | 69/69 = 100.0% | 69/69 = 100.0% | 56/69 = 81.2% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 309

**Selected features:** selected = {b, cd, cw, d, dl, t, tl, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 83 | 0/83 = 0.0% | 77/83 = 92.8% | 65/83 = 78.3% | 83/83 = 100.0% | 83/83 = 100.0% | 63/83 = 75.9% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 310

**Selected features:** selected = {b, cw, d, dl, i, o, t, us, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 23/28 = 82.1% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 19/28 = 67.9% |
| ActionExchange | 121 | 0/121 = 0.0% | 105/121 = 86.8% | 96/121 = 79.3% | 121/121 = 100.0% | 121/121 = 100.0% | 85/121 = 70.2% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 311

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, t, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 22/33 = 66.7% |
| ActionExchange | 144 | 0/144 = 0.0% | 136/144 = 94.4% | 120/144 = 83.3% | 144/144 = 100.0% | 144/144 = 100.0% | 97/144 = 67.4% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |

### Product 312

**Selected features:** selected = {b, c, cw, d, t, us, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |
| ActionExchange | 61 | 0/61 = 0.0% | 51/61 = 83.6% | 49/61 = 80.3% | 61/61 = 100.0% | 61/61 = 100.0% | 41/61 = 67.2% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |

### Product 313

**Selected features:** selected = {b, c, cw, d, eu, i, ie, t, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 18/30 = 60.0% |
| ActionExchange | 128 | 0/128 = 0.0% | 114/128 = 89.1% | 104/128 = 81.3% | 128/128 = 100.0% | 128/128 = 100.0% | 85/128 = 66.4% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 314

**Selected features:** selected = {b, c, cw, d, i, ie, t, up, us, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 18/30 = 60.0% |
| ActionExchange | 128 | 0/128 = 0.0% | 114/128 = 89.1% | 104/128 = 81.3% | 128/128 = 100.0% | 128/128 = 100.0% | 85/128 = 66.4% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 315

**Selected features:** selected = {b, c, cd, cw, d, dl, t, tl, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 21/28 = 75.0% |
| ActionExchange | 106 | 0/106 = 0.0% | 99/106 = 93.4% | 84/106 = 79.2% | 106/106 = 100.0% | 106/106 = 100.0% | 83/106 = 78.3% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |

### Product 316

**Selected features:** selected = {b, cd, cw, d, eu, i, ie, t, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 23/31 = 74.2% |
| ActionExchange | 116 | 0/116 = 0.0% | 109/116 = 94.0% | 94/116 = 81.0% | 116/116 = 100.0% | 116/116 = 100.0% | 86/116 = 74.1% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 16/19 = 84.2% |

### Product 317

**Selected features:** selected = {b, cw, d, up, us, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |
| ActionExchange | 44 | 0/44 = 0.0% | 39/44 = 88.6% | 38/44 = 86.4% | 44/44 = 100.0% | 44/44 = 100.0% | 35/44 = 79.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 318

**Selected features:** selected = {b, cw, d, dl, i, ie, o, tl, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 16/28 = 57.1% |
| ActionExchange | 131 | 0/131 = 0.0% | 121/131 = 92.4% | 106/131 = 80.9% | 131/131 = 100.0% | 131/131 = 100.0% | 87/131 = 66.4% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |

### Product 319

**Selected features:** selected = {b, c, cd, cw, d, i, t, tl, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 19/31 = 61.3% |
| ActionExchange | 134 | 0/134 = 0.0% | 126/134 = 94.0% | 114/134 = 85.1% | 134/134 = 100.0% | 134/134 = 100.0% | 94/134 = 70.1% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 320

**Selected features:** selected = {b, cd, cw, d, dl, i, o, t, tl, w}

**Repaired FTS:** 19 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 28/31 = 90.3% | 25/31 = 80.6% | 31/31 = 100.0% | 31/31 = 100.0% | 25/31 = 80.6% |
| ActionExchange | 137 | 0/137 = 0.0% | 127/137 = 92.7% | 110/137 = 80.3% | 137/137 = 100.0% | 137/137 = 100.0% | 114/137 = 83.2% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 321

**Selected features:** selected = {b, c, cw, d, i, ie, tl, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 15/23 = 65.2% |
| ActionExchange | 91 | 0/91 = 0.0% | 84/91 = 92.3% | 76/91 = 83.5% | 91/91 = 100.0% | 91/91 = 100.0% | 63/91 = 69.2% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 322

**Selected features:** selected = {b, c, cd, cw, d, up, us, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 75 | 0/75 = 0.0% | 73/75 = 97.3% | 60/75 = 80.0% | 75/75 = 100.0% | 75/75 = 100.0% | 52/75 = 69.3% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 323

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, ie, w}

**Repaired FTS:** 15 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 20/25 = 80.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 105 | 0/105 = 0.0% | 103/105 = 98.1% | 83/105 = 79.0% | 105/105 = 100.0% | 105/105 = 100.0% | 82/105 = 78.1% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 324

**Selected features:** selected = {b, c, cw, d, dl, up, us, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 14/23 = 60.9% |
| ActionExchange | 91 | 0/91 = 0.0% | 84/91 = 92.3% | 76/91 = 83.5% | 91/91 = 100.0% | 91/91 = 100.0% | 62/91 = 68.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 325

**Selected features:** selected = {b, cw, d, dl, i, ie, t, tl, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 22/27 = 81.5% | 27/27 = 100.0% | 27/27 = 100.0% | 21/27 = 77.8% |
| ActionExchange | 100 | 0/100 = 0.0% | 88/100 = 88.0% | 75/100 = 75.0% | 100/100 = 100.0% | 100/100 = 100.0% | 78/100 = 78.0% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |

### Product 326

**Selected features:** selected = {b, c, cd, d, eu, i, ie, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 98 | 0/98 = 0.0% | 96/98 = 98.0% | 88/98 = 89.8% | 98/98 = 100.0% | 98/98 = 100.0% | 74/98 = 75.5% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 327

**Selected features:** selected = {b, c, cw, d, dl, i, tl, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 14/25 = 56.0% |
| ActionExchange | 117 | 0/117 = 0.0% | 109/117 = 93.2% | 99/117 = 84.6% | 117/117 = 100.0% | 117/117 = 100.0% | 77/117 = 65.8% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 328

**Selected features:** selected = {b, cd, d, i, t, up, us, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 22/28 = 78.6% |
| ActionExchange | 99 | 0/99 = 0.0% | 92/99 = 92.9% | 89/99 = 89.9% | 99/99 = 100.0% | 99/99 = 100.0% | 80/99 = 80.8% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |

### Product 329

**Selected features:** selected = {b, c, cw, d, i, t, us, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 70/82 = 85.4% | 67/82 = 81.7% | 82/82 = 100.0% | 82/82 = 100.0% | 65/82 = 79.3% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 330

**Selected features:** selected = {b, c, cw, d, tl, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 61 | 0/61 = 0.0% | 55/61 = 90.2% | 53/61 = 86.9% | 61/61 = 100.0% | 61/61 = 100.0% | 41/61 = 67.2% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 331

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 23/28 = 82.1% |
| ActionExchange | 115 | 0/115 = 0.0% | 113/115 = 98.3% | 95/115 = 82.6% | 115/115 = 100.0% | 115/115 = 100.0% | 98/115 = 85.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 332

**Selected features:** selected = {b, cw, d, dl, o, us, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |
| ActionExchange | 64 | 0/64 = 0.0% | 56/64 = 87.5% | 51/64 = 79.7% | 64/64 = 100.0% | 64/64 = 100.0% | 56/64 = 87.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 333

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, t, us, w}

**Repaired FTS:** 20 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 30/33 = 90.9% | 26/33 = 78.8% | 33/33 = 100.0% | 33/33 = 100.0% | 27/33 = 81.8% |
| ActionExchange | 147 | 0/147 = 0.0% | 137/147 = 93.2% | 114/147 = 77.6% | 147/147 = 100.0% | 147/147 = 100.0% | 123/147 = 83.7% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 18/19 = 94.7% |

### Product 334

**Selected features:** selected = {b, c, cd, d, eu, i, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 22/23 = 95.7% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 89 | 0/89 = 0.0% | 87/89 = 97.8% | 84/89 = 94.4% | 89/89 = 100.0% | 89/89 = 100.0% | 72/89 = 80.9% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 335

**Selected features:** selected = {b, cd, cw, d, dl, i, t, up, us, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 22/33 = 66.7% |
| ActionExchange | 144 | 0/144 = 0.0% | 136/144 = 94.4% | 120/144 = 83.3% | 144/144 = 100.0% | 144/144 = 100.0% | 97/144 = 67.4% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |

### Product 336

**Selected features:** selected = {b, c, cd, cw, d, dl, t, us, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 21/28 = 75.0% |
| ActionExchange | 106 | 0/106 = 0.0% | 99/106 = 93.4% | 84/106 = 79.2% | 106/106 = 100.0% | 106/106 = 100.0% | 83/106 = 78.3% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |

### Product 337

**Selected features:** selected = {b, cw, d, dl, o, tl, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |
| ActionExchange | 64 | 0/64 = 0.0% | 56/64 = 87.5% | 51/64 = 79.7% | 64/64 = 100.0% | 64/64 = 100.0% | 56/64 = 87.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 338

**Selected features:** selected = {b, cw, d, dl, i, ie, o, up, us, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 16/28 = 57.1% |
| ActionExchange | 131 | 0/131 = 0.0% | 121/131 = 92.4% | 106/131 = 80.9% | 131/131 = 100.0% | 131/131 = 100.0% | 87/131 = 66.4% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |

### Product 339

**Selected features:** selected = {b, cw, d, dl, i, ie, o, t, up, us, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 30/35 = 85.7% | 29/35 = 82.9% | 35/35 = 100.0% | 35/35 = 100.0% | 18/35 = 51.4% |
| ActionExchange | 174 | 0/174 = 0.0% | 156/174 = 89.7% | 138/174 = 79.3% | 174/174 = 100.0% | 174/174 = 100.0% | 99/174 = 56.9% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |

### Product 340

**Selected features:** selected = {b, c, cd, d, i, t, tl, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 24/30 = 80.0% |
| ActionExchange | 125 | 0/125 = 0.0% | 117/125 = 93.6% | 113/125 = 90.4% | 125/125 = 100.0% | 125/125 = 100.0% | 103/125 = 82.4% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |

### Product 341

**Selected features:** selected = {b, c, cw, d, eu, i, t, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 18/28 = 64.3% |
| ActionExchange | 118 | 0/118 = 0.0% | 104/118 = 88.1% | 100/118 = 84.7% | 118/118 = 100.0% | 118/118 = 100.0% | 85/118 = 72.0% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 342

**Selected features:** selected = {b, cd, d, eu, i, ie, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 13/23 = 56.5% |
| ActionExchange | 76 | 0/76 = 0.0% | 74/76 = 97.4% | 68/76 = 89.5% | 76/76 = 100.0% | 76/76 = 100.0% | 46/76 = 60.5% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |

### Product 343

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, us, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 80/82 = 97.6% | 64/82 = 78.0% | 82/82 = 100.0% | 82/82 = 100.0% | 66/82 = 80.5% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 344

**Selected features:** selected = {b, c, cw, d, t, tl, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 92 | 0/92 = 0.0% | 80/92 = 87.0% | 77/92 = 83.7% | 92/92 = 100.0% | 92/92 = 100.0% | 71/92 = 77.2% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |

### Product 345

**Selected features:** selected = {b, cd, cw, d, dl, i, up, us, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 106 | 0/106 = 0.0% | 104/106 = 98.1% | 91/106 = 85.8% | 106/106 = 100.0% | 106/106 = 100.0% | 82/106 = 77.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 346

**Selected features:** selected = {b, cd, d, i, t, us, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 68 | 0/68 = 0.0% | 62/68 = 91.2% | 58/68 = 85.3% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |

### Product 347

**Selected features:** selected = {b, c, cd, d, i, up, us, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 22/23 = 95.7% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 89 | 0/89 = 0.0% | 87/89 = 97.8% | 84/89 = 94.4% | 89/89 = 100.0% | 89/89 = 100.0% | 72/89 = 80.9% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 348

**Selected features:** selected = {b, c, cw, d, dl, us, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |
| ActionExchange | 60 | 0/60 = 0.0% | 54/60 = 90.0% | 48/60 = 80.0% | 60/60 = 100.0% | 60/60 = 100.0% | 52/60 = 86.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 349

**Selected features:** selected = {b, c, cw, d, dl, i, ie, t, tl, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 24/29 = 82.8% | 29/29 = 100.0% | 29/29 = 100.0% | 17/29 = 58.6% |
| ActionExchange | 127 | 0/127 = 0.0% | 113/127 = 89.0% | 97/127 = 76.4% | 127/127 = 100.0% | 127/127 = 100.0% | 79/127 = 62.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 350

**Selected features:** selected = {b, c, cw, d, i, ie, us, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |
| ActionExchange | 60 | 0/60 = 0.0% | 54/60 = 90.0% | 48/60 = 80.0% | 60/60 = 100.0% | 60/60 = 100.0% | 41/60 = 68.3% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 351

**Selected features:** selected = {b, cw, d, dl, o, t, us, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 21/26 = 80.8% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 95 | 0/95 = 0.0% | 81/95 = 85.3% | 74/95 = 77.9% | 95/95 = 100.0% | 95/95 = 100.0% | 73/95 = 76.8% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 352

**Selected features:** selected = {b, c, cd, cw, d, dl, i, t, us, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 25/30 = 83.3% | 30/30 = 100.0% | 30/30 = 100.0% | 25/30 = 83.3% |
| ActionExchange | 133 | 0/133 = 0.0% | 125/133 = 94.0% | 107/133 = 80.5% | 133/133 = 100.0% | 133/133 = 100.0% | 112/133 = 84.2% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 353

**Selected features:** selected = {b, c, cd, d, eu, i, ie, t, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 26/32 = 81.3% |
| ActionExchange | 135 | 0/135 = 0.0% | 127/135 = 94.1% | 117/135 = 86.7% | 135/135 = 100.0% | 135/135 = 100.0% | 113/135 = 83.7% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 18/20 = 90.0% |

### Product 354

**Selected features:** selected = {b, cw, d, tl, up, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |
| ActionExchange | 44 | 0/44 = 0.0% | 39/44 = 88.6% | 38/44 = 86.4% | 44/44 = 100.0% | 44/44 = 100.0% | 35/44 = 79.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 355

**Selected features:** selected = {b, cw, d, i, t, tl, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 22/26 = 84.6% |
| ActionExchange | 92 | 0/92 = 0.0% | 80/92 = 87.0% | 77/92 = 83.7% | 92/92 = 100.0% | 92/92 = 100.0% | 76/92 = 82.6% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 356

**Selected features:** selected = {b, c, cd, cw, d, us, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |
| ActionExchange | 49 | 0/49 = 0.0% | 47/49 = 95.9% | 37/49 = 75.5% | 49/49 = 100.0% | 49/49 = 100.0% | 44/49 = 89.8% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 9/10 = 90.0% |

### Product 357

**Selected features:** selected = {b, cd, d, i, us, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |
| ActionExchange | 43 | 0/43 = 0.0% | 41/43 = 95.3% | 38/43 = 88.4% | 43/43 = 100.0% | 43/43 = 100.0% | 38/43 = 88.4% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% |

### Product 358

**Selected features:** selected = {b, c, cd, cw, d, dl, us, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 74 | 0/74 = 0.0% | 72/74 = 97.3% | 60/74 = 81.1% | 74/74 = 100.0% | 74/74 = 100.0% | 60/74 = 81.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 359

**Selected features:** selected = {b, c, cw, d, i, tl, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 12/16 = 75.0% |
| ActionExchange | 52 | 0/52 = 0.0% | 46/52 = 88.5% | 44/52 = 84.6% | 52/52 = 100.0% | 52/52 = 100.0% | 41/52 = 78.8% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 360

**Selected features:** selected = {b, c, cd, cw, d, i, us, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 11/19 = 57.9% |
| ActionExchange | 66 | 0/66 = 0.0% | 64/66 = 97.0% | 51/66 = 77.3% | 66/66 = 100.0% | 66/66 = 100.0% | 43/66 = 65.2% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |

### Product 361

**Selected features:** selected = {b, cw, d, dl, i, o, t, tl, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 23/28 = 82.1% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 19/28 = 67.9% |
| ActionExchange | 121 | 0/121 = 0.0% | 105/121 = 86.8% | 96/121 = 79.3% | 121/121 = 100.0% | 121/121 = 100.0% | 85/121 = 70.2% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 362

**Selected features:** selected = {b, cw, d, dl, i, tl, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |
| ActionExchange | 60 | 0/60 = 0.0% | 54/60 = 90.0% | 48/60 = 80.0% | 60/60 = 100.0% | 60/60 = 100.0% | 52/60 = 86.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 363

**Selected features:** selected = {b, cw, d, dl, us, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |
| ActionExchange | 43 | 0/43 = 0.0% | 38/43 = 88.4% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 32/43 = 74.4% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 364

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, tl, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 82 | 0/82 = 0.0% | 80/82 = 97.6% | 64/82 = 78.0% | 82/82 = 100.0% | 82/82 = 100.0% | 66/82 = 80.5% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 365

**Selected features:** selected = {b, cw, d, dl, i, o, up, us, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 14/26 = 53.8% |
| ActionExchange | 121 | 0/121 = 0.0% | 111/121 = 91.7% | 102/121 = 84.3% | 121/121 = 100.0% | 121/121 = 100.0% | 77/121 = 63.6% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 366

**Selected features:** selected = {b, c, cw, d, dl, tl, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 14/23 = 60.9% |
| ActionExchange | 91 | 0/91 = 0.0% | 84/91 = 92.3% | 76/91 = 83.5% | 91/91 = 100.0% | 91/91 = 100.0% | 62/91 = 68.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 367

**Selected features:** selected = {b, cw, d, dl, eu, i, o, t, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 28/33 = 84.8% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 25/33 = 75.8% |
| ActionExchange | 163 | 0/163 = 0.0% | 145/163 = 89.0% | 134/163 = 82.2% | 163/163 = 100.0% | 163/163 = 100.0% | 142/163 = 87.1% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 15/20 = 75.0% |

### Product 368

**Selected features:** selected = {b, cw, d, dl, i, ie, tl, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 17/20 = 85.0% |
| ActionExchange | 68 | 0/68 = 0.0% | 62/68 = 91.2% | 52/68 = 76.5% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% |

### Product 369

**Selected features:** selected = {b, c, cw, d, dl, eu, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |
| ActionExchange | 60 | 0/60 = 0.0% | 54/60 = 90.0% | 48/60 = 80.0% | 60/60 = 100.0% | 60/60 = 100.0% | 52/60 = 86.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 370

**Selected features:** selected = {b, cd, cw, d, up, us, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 14/20 = 70.0% |
| ActionExchange | 57 | 0/57 = 0.0% | 55/57 = 96.5% | 45/57 = 78.9% | 57/57 = 100.0% | 57/57 = 100.0% | 46/57 = 80.7% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 371

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, t, tl, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 25/30 = 83.3% |
| ActionExchange | 115 | 0/115 = 0.0% | 108/115 = 93.9% | 88/115 = 76.5% | 115/115 = 100.0% | 115/115 = 100.0% | 94/115 = 81.7% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 372

**Selected features:** selected = {b, cw, d, eu, w}

**Repaired FTS:** 9 states, 12 transitions (12 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 20 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 9/12 = 75.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 11/12 = 91.7% |
| ActionExchange | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 22/24 = 91.7% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 373

**Selected features:** selected = {b, c, cd, d, eu, i, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 15/18 = 83.3% |
| ActionExchange | 59 | 0/59 = 0.0% | 57/59 = 96.6% | 53/59 = 89.8% | 59/59 = 100.0% | 59/59 = 100.0% | 53/59 = 89.8% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 374

**Selected features:** selected = {b, c, cw, d, dl, t, up, us, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 26/30 = 86.7% |
| ActionExchange | 128 | 0/128 = 0.0% | 114/128 = 89.1% | 104/128 = 81.3% | 128/128 = 100.0% | 128/128 = 100.0% | 109/128 = 85.2% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 18/19 = 94.7% |

### Product 375

**Selected features:** selected = {b, cd, cw, d, dl, eu, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 17/24 = 70.8% |
| ActionExchange | 83 | 0/83 = 0.0% | 81/83 = 97.6% | 71/83 = 85.5% | 83/83 = 100.0% | 83/83 = 100.0% | 62/83 = 74.7% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 376

**Selected features:** selected = {b, c, cd, cw, d, eu, t, up, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 18/29 = 62.1% |
| ActionExchange | 107 | 0/107 = 0.0% | 100/107 = 93.5% | 90/107 = 84.1% | 107/107 = 100.0% | 107/107 = 100.0% | 76/107 = 71.0% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |

### Product 377

**Selected features:** selected = {b, c, cd, cw, d, eu, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |
| ActionExchange | 49 | 0/49 = 0.0% | 47/49 = 95.9% | 37/49 = 75.5% | 49/49 = 100.0% | 49/49 = 100.0% | 44/49 = 89.8% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 9/10 = 90.0% |

### Product 378

**Selected features:** selected = {b, c, cd, d, eu, i, t, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 20/25 = 80.0% |
| ActionExchange | 89 | 0/89 = 0.0% | 82/89 = 92.1% | 77/89 = 86.5% | 89/89 = 100.0% | 89/89 = 100.0% | 70/89 = 78.7% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 379

**Selected features:** selected = {b, cd, d, eu, i, ie, t, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 19/25 = 76.0% |
| ActionExchange | 76 | 0/76 = 0.0% | 70/76 = 92.1% | 62/76 = 81.6% | 76/76 = 100.0% | 76/76 = 100.0% | 58/76 = 76.3% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 380

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, t, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 22/27 = 81.5% | 27/27 = 100.0% | 27/27 = 100.0% | 21/27 = 77.8% |
| ActionExchange | 100 | 0/100 = 0.0% | 88/100 = 88.0% | 75/100 = 75.0% | 100/100 = 100.0% | 100/100 = 100.0% | 78/100 = 78.0% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |

### Product 381

**Selected features:** selected = {b, cw, d, dl, eu, t, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 20/23 = 87.0% |
| ActionExchange | 69 | 0/69 = 0.0% | 59/69 = 85.5% | 53/69 = 76.8% | 69/69 = 100.0% | 69/69 = 100.0% | 57/69 = 82.6% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% |

### Product 382

**Selected features:** selected = {b, c, cd, d, i, ie, tl, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 98 | 0/98 = 0.0% | 96/98 = 98.0% | 88/98 = 89.8% | 98/98 = 100.0% | 98/98 = 100.0% | 74/98 = 75.5% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 383

**Selected features:** selected = {b, c, cd, d, i, ie, up, us, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 98 | 0/98 = 0.0% | 96/98 = 98.0% | 88/98 = 89.8% | 98/98 = 100.0% | 98/98 = 100.0% | 74/98 = 75.5% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 384

**Selected features:** selected = {b, cw, d, eu, i, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 15/19 = 78.9% |
| ActionExchange | 61 | 0/61 = 0.0% | 55/61 = 90.2% | 53/61 = 86.9% | 61/61 = 100.0% | 61/61 = 100.0% | 48/61 = 78.7% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 385

**Selected features:** selected = {b, cd, cw, d, i, ie, tl, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 14/19 = 73.7% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 56 | 0/56 = 0.0% | 54/56 = 96.4% | 41/56 = 73.2% | 56/56 = 100.0% | 56/56 = 100.0% | 38/56 = 67.9% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 386

**Selected features:** selected = {b, c, cd, d, eu, i, t, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 24/30 = 80.0% |
| ActionExchange | 125 | 0/125 = 0.0% | 117/125 = 93.6% | 113/125 = 90.4% | 125/125 = 100.0% | 125/125 = 100.0% | 103/125 = 82.4% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |

### Product 387

**Selected features:** selected = {b, c, cd, d, i, tl, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 22/23 = 95.7% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 89 | 0/89 = 0.0% | 87/89 = 97.8% | 84/89 = 94.4% | 89/89 = 100.0% | 89/89 = 100.0% | 72/89 = 80.9% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 388

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, tl, up, w}

**Repaired FTS:** 18 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 29/30 = 96.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 21/30 = 70.0% |
| ActionExchange | 143 | 0/143 = 0.0% | 141/143 = 98.6% | 119/143 = 83.2% | 143/143 = 100.0% | 143/143 = 100.0% | 112/143 = 78.3% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 389

**Selected features:** selected = {b, cd, cw, d, eu, w}

**Repaired FTS:** 10 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 14/15 = 93.3% | 11/15 = 73.3% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |
| ActionExchange | 36 | 0/36 = 0.0% | 34/36 = 94.4% | 27/36 = 75.0% | 36/36 = 100.0% | 36/36 = 100.0% | 28/36 = 77.8% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 390

**Selected features:** selected = {b, cd, cw, d, dl, i, t, us, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 21/28 = 75.0% |
| ActionExchange | 106 | 0/106 = 0.0% | 99/106 = 93.4% | 84/106 = 79.2% | 106/106 = 100.0% | 106/106 = 100.0% | 83/106 = 78.3% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |

### Product 391

**Selected features:** selected = {b, c, cd, cw, d, i, tl, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 11/19 = 57.9% |
| ActionExchange | 66 | 0/66 = 0.0% | 64/66 = 97.0% | 51/66 = 77.3% | 66/66 = 100.0% | 66/66 = 100.0% | 43/66 = 65.2% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |

### Product 392

**Selected features:** selected = {b, c, cd, cw, d, eu, i, t, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 19/31 = 61.3% |
| ActionExchange | 134 | 0/134 = 0.0% | 126/134 = 94.0% | 114/134 = 85.1% | 134/134 = 100.0% | 134/134 = 100.0% | 94/134 = 70.1% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 393

**Selected features:** selected = {b, c, cd, cw, d, i, up, us, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 15/24 = 62.5% |
| ActionExchange | 97 | 0/97 = 0.0% | 95/97 = 97.9% | 85/97 = 87.6% | 97/97 = 100.0% | 97/97 = 100.0% | 65/97 = 67.0% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 394

**Selected features:** selected = {b, cw, d, i, tl, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 15/19 = 78.9% |
| ActionExchange | 61 | 0/61 = 0.0% | 55/61 = 90.2% | 53/61 = 86.9% | 61/61 = 100.0% | 61/61 = 100.0% | 48/61 = 78.7% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 395

**Selected features:** selected = {b, cd, d, i, t, tl, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 68 | 0/68 = 0.0% | 62/68 = 91.2% | 58/68 = 85.3% | 68/68 = 100.0% | 68/68 = 100.0% | 56/68 = 82.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |

### Product 396

**Selected features:** selected = {b, cd, cw, d, eu, i, ie, t, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 83 | 0/83 = 0.0% | 77/83 = 92.8% | 65/83 = 78.3% | 83/83 = 100.0% | 83/83 = 100.0% | 62/83 = 74.7% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 397

**Selected features:** selected = {b, c, cd, cw, d, i, ie, tl, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 15/26 = 57.7% |
| ActionExchange | 106 | 0/106 = 0.0% | 104/106 = 98.1% | 89/106 = 84.0% | 106/106 = 100.0% | 106/106 = 100.0% | 67/106 = 63.2% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |

### Product 398

**Selected features:** selected = {b, c, cw, d, dl, i, ie, t, tl, up, w}

**Repaired FTS:** 22 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 30/34 = 88.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 19/34 = 55.9% |
| ActionExchange | 170 | 0/170 = 0.0% | 154/170 = 90.6% | 135/170 = 79.4% | 170/170 = 100.0% | 170/170 = 100.0% | 116/170 = 68.2% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |

### Product 399

**Selected features:** selected = {b, c, cd, cw, d, dl, i, up, us, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 23/28 = 82.1% |
| ActionExchange | 133 | 0/133 = 0.0% | 131/133 = 98.5% | 115/133 = 86.5% | 133/133 = 100.0% | 133/133 = 100.0% | 112/133 = 84.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 15/16 = 93.8% |

### Product 400

**Selected features:** selected = {b, cd, d, i, ie, tl, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 13/23 = 56.5% |
| ActionExchange | 76 | 0/76 = 0.0% | 74/76 = 97.4% | 68/76 = 89.5% | 76/76 = 100.0% | 76/76 = 100.0% | 46/76 = 60.5% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |

### Product 401

**Selected features:** selected = {b, c, cw, d, up, us, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 61 | 0/61 = 0.0% | 55/61 = 90.2% | 53/61 = 86.9% | 61/61 = 100.0% | 61/61 = 100.0% | 41/61 = 67.2% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 402

**Selected features:** selected = {b, c, cd, d, i, tl, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 15/18 = 83.3% |
| ActionExchange | 59 | 0/59 = 0.0% | 57/59 = 96.6% | 53/59 = 89.8% | 59/59 = 100.0% | 59/59 = 100.0% | 53/59 = 89.8% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 403

**Selected features:** selected = {b, c, cw, d, i, ie, tl, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |
| ActionExchange | 60 | 0/60 = 0.0% | 54/60 = 90.0% | 48/60 = 80.0% | 60/60 = 100.0% | 60/60 = 100.0% | 41/60 = 68.3% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 404

**Selected features:** selected = {b, cd, d, tl, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 18/19 = 94.7% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |
| ActionExchange | 51 | 0/51 = 0.0% | 49/51 = 96.1% | 48/51 = 94.1% | 51/51 = 100.0% | 51/51 = 100.0% | 47/51 = 92.2% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 11/12 = 91.7% |

### Product 405

**Selected features:** selected = {b, c, cw, d, dl, i, up, us, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 14/25 = 56.0% |
| ActionExchange | 117 | 0/117 = 0.0% | 109/117 = 93.2% | 99/117 = 84.6% | 117/117 = 100.0% | 117/117 = 100.0% | 77/117 = 65.8% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 406

**Selected features:** selected = {b, cw, d, dl, i, us, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |
| ActionExchange | 60 | 0/60 = 0.0% | 54/60 = 90.0% | 48/60 = 80.0% | 60/60 = 100.0% | 60/60 = 100.0% | 52/60 = 86.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 407

**Selected features:** selected = {b, cw, d, i, ie, t, us, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 69 | 0/69 = 0.0% | 59/69 = 85.5% | 53/69 = 76.8% | 69/69 = 100.0% | 69/69 = 100.0% | 55/69 = 79.7% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 408

**Selected features:** selected = {b, c, cw, d, dl, i, ie, t, up, us, w}

**Repaired FTS:** 22 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 30/34 = 88.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 19/34 = 55.9% |
| ActionExchange | 170 | 0/170 = 0.0% | 154/170 = 90.6% | 135/170 = 79.4% | 170/170 = 100.0% | 170/170 = 100.0% | 116/170 = 68.2% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |

### Product 409

**Selected features:** selected = {b, cw, d, dl, i, up, us, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 14/23 = 60.9% |
| ActionExchange | 91 | 0/91 = 0.0% | 84/91 = 92.3% | 76/91 = 83.5% | 91/91 = 100.0% | 91/91 = 100.0% | 62/91 = 68.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 410

**Selected features:** selected = {b, cw, d, dl, i, t, tl, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 14 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 20/30 = 66.7% |
| ActionExchange | 128 | 0/128 = 0.0% | 114/128 = 89.1% | 104/128 = 81.3% | 128/128 = 100.0% | 128/128 = 100.0% | 89/128 = 69.5% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 14/19 = 73.7% |

### Product 411

**Selected features:** selected = {b, cd, cw, d, i, us, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |
| ActionExchange | 49 | 0/49 = 0.0% | 47/49 = 95.9% | 37/49 = 75.5% | 49/49 = 100.0% | 49/49 = 100.0% | 44/49 = 89.8% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 9/10 = 90.0% |

### Product 412

**Selected features:** selected = {b, cd, cw, d, eu, i, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |
| ActionExchange | 49 | 0/49 = 0.0% | 47/49 = 95.9% | 37/49 = 75.5% | 49/49 = 100.0% | 49/49 = 100.0% | 44/49 = 89.8% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 9/10 = 90.0% |

### Product 413

**Selected features:** selected = {b, cd, d, i, ie, t, tl, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 23/30 = 76.7% |
| ActionExchange | 108 | 0/108 = 0.0% | 101/108 = 93.5% | 93/108 = 86.1% | 108/108 = 100.0% | 108/108 = 100.0% | 85/108 = 78.7% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 16/19 = 84.2% |

### Product 414

**Selected features:** selected = {b, cd, d, i, ie, t, tl, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 19/25 = 76.0% |
| ActionExchange | 76 | 0/76 = 0.0% | 70/76 = 92.1% | 62/76 = 81.6% | 76/76 = 100.0% | 76/76 = 100.0% | 58/76 = 76.3% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 415

**Selected features:** selected = {b, cw, d, t, us, w}

**Repaired FTS:** 14 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 16/19 = 84.2% | 19/19 = 100.0% | 19/19 = 100.0% | 15/19 = 78.9% |
| ActionExchange | 44 | 0/44 = 0.0% | 36/44 = 81.8% | 35/44 = 79.5% | 44/44 = 100.0% | 44/44 = 100.0% | 36/44 = 81.8% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 416

**Selected features:** selected = {b, c, cd, cw, d, eu, i, t, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 97 | 0/97 = 0.0% | 90/97 = 92.8% | 80/97 = 82.5% | 97/97 = 100.0% | 97/97 = 100.0% | 73/97 = 75.3% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 417

**Selected features:** selected = {b, c, cd, cw, d, i, ie, tl, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 16/21 = 76.2% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 74 | 0/74 = 0.0% | 72/74 = 97.3% | 55/74 = 74.3% | 74/74 = 100.0% | 74/74 = 100.0% | 51/74 = 68.9% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 418

**Selected features:** selected = {b, cd, cw, d, i, t, us, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 16/24 = 66.7% |
| ActionExchange | 75 | 0/75 = 0.0% | 69/75 = 92.0% | 61/75 = 81.3% | 75/75 = 100.0% | 75/75 = 100.0% | 48/75 = 64.0% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 419

**Selected features:** selected = {b, cw, d, dl, i, ie, tl, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 15/25 = 60.0% |
| ActionExchange | 100 | 0/100 = 0.0% | 93/100 = 93.0% | 80/100 = 80.0% | 100/100 = 100.0% | 100/100 = 100.0% | 65/100 = 65.0% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 420

**Selected features:** selected = {b, c, cd, cw, d, i, ie, t, up, us, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 25/33 = 75.8% |
| ActionExchange | 144 | 0/144 = 0.0% | 136/144 = 94.4% | 118/144 = 81.9% | 144/144 = 100.0% | 144/144 = 100.0% | 108/144 = 75.0% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 17/20 = 85.0% |

### Product 421

**Selected features:** selected = {b, cw, d, us, w}

**Repaired FTS:** 9 states, 12 transitions (12 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 20 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 9/12 = 75.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 11/12 = 91.7% |
| ActionExchange | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 22/24 = 91.7% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 422

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, t, up, us, w}

**Repaired FTS:** 23 states, 37 transitions (37 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 37 | 0/37 = 0.0% | 35/37 = 94.6% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 33/37 = 89.2% |
| ActionExchange | 187 | 0/187 = 0.0% | 178/187 = 95.2% | 152/187 = 81.3% | 187/187 = 100.0% | 187/187 = 100.0% | 164/187 = 87.7% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 21/22 = 95.5% |

### Product 423

**Selected features:** selected = {b, c, cd, cw, d, dl, t, tl, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 22/33 = 66.7% |
| ActionExchange | 144 | 0/144 = 0.0% | 136/144 = 94.4% | 120/144 = 83.3% | 144/144 = 100.0% | 144/144 = 100.0% | 97/144 = 67.4% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |

### Product 424

**Selected features:** selected = {b, cd, d, i, ie, tl, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |
| ActionExchange | 50 | 0/50 = 0.0% | 48/50 = 96.0% | 42/50 = 84.0% | 50/50 = 100.0% | 50/50 = 100.0% | 45/50 = 90.0% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 425

**Selected features:** selected = {b, c, cw, d, dl, eu, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 14/23 = 60.9% |
| ActionExchange | 91 | 0/91 = 0.0% | 84/91 = 92.3% | 76/91 = 83.5% | 91/91 = 100.0% | 91/91 = 100.0% | 62/91 = 68.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 426

**Selected features:** selected = {b, c, cd, cw, d, i, ie, t, tl, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 25/33 = 75.8% |
| ActionExchange | 144 | 0/144 = 0.0% | 136/144 = 94.4% | 118/144 = 81.9% | 144/144 = 100.0% | 144/144 = 100.0% | 108/144 = 75.0% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 17/20 = 85.0% |

### Product 427

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 74 | 0/74 = 0.0% | 72/74 = 97.3% | 60/74 = 81.1% | 74/74 = 100.0% | 74/74 = 100.0% | 60/74 = 81.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 428

**Selected features:** selected = {b, c, cd, cw, d, eu, i, ie, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 16/21 = 76.2% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 74 | 0/74 = 0.0% | 72/74 = 97.3% | 55/74 = 74.3% | 74/74 = 100.0% | 74/74 = 100.0% | 51/74 = 68.9% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 429

**Selected features:** selected = {b, cw, d, eu, i, ie, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 13/16 = 81.3% |
| ActionExchange | 43 | 0/43 = 0.0% | 38/43 = 88.4% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 35/43 = 81.4% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 9/10 = 90.0% |

### Product 430

**Selected features:** selected = {b, c, cd, d, i, t, tl, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 20/25 = 80.0% |
| ActionExchange | 89 | 0/89 = 0.0% | 82/89 = 92.1% | 77/89 = 86.5% | 89/89 = 100.0% | 89/89 = 100.0% | 70/89 = 78.7% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 431

**Selected features:** selected = {b, c, cw, d, dl, i, t, tl, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 19/32 = 59.4% |
| ActionExchange | 159 | 0/159 = 0.0% | 143/159 = 89.9% | 131/159 = 82.4% | 159/159 = 100.0% | 159/159 = 100.0% | 115/159 = 72.3% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |

### Product 432

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, t, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 25/30 = 83.3% |
| ActionExchange | 115 | 0/115 = 0.0% | 108/115 = 93.9% | 88/115 = 76.5% | 115/115 = 100.0% | 115/115 = 100.0% | 94/115 = 81.7% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 433

**Selected features:** selected = {b, cd, d, i, t, tl, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 22/28 = 78.6% |
| ActionExchange | 99 | 0/99 = 0.0% | 92/99 = 92.9% | 89/99 = 89.9% | 99/99 = 100.0% | 99/99 = 100.0% | 80/99 = 80.8% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |

### Product 434

**Selected features:** selected = {b, c, cd, cw, d, dl, i, t, up, us, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 31/35 = 88.6% | 35/35 = 100.0% | 35/35 = 100.0% | 27/35 = 77.1% |
| ActionExchange | 176 | 0/176 = 0.0% | 167/176 = 94.9% | 148/176 = 84.1% | 176/176 = 100.0% | 176/176 = 100.0% | 136/176 = 77.3% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |

### Product 435

**Selected features:** selected = {b, cd, cw, d, eu, up, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 14/20 = 70.0% |
| ActionExchange | 57 | 0/57 = 0.0% | 55/57 = 96.5% | 45/57 = 78.9% | 57/57 = 100.0% | 57/57 = 100.0% | 46/57 = 80.7% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 436

**Selected features:** selected = {b, cw, d, dl, tl, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 69 | 0/69 = 0.0% | 63/69 = 91.3% | 57/69 = 82.6% | 69/69 = 100.0% | 69/69 = 100.0% | 56/69 = 81.2% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 437

**Selected features:** selected = {b, cd, cw, d, dl, tl, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 17/24 = 70.8% |
| ActionExchange | 83 | 0/83 = 0.0% | 81/83 = 97.6% | 71/83 = 85.5% | 83/83 = 100.0% | 83/83 = 100.0% | 62/83 = 74.7% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 438

**Selected features:** selected = {b, c, cw, d, i, up, us, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |
| ActionExchange | 82 | 0/82 = 0.0% | 75/82 = 91.5% | 72/82 = 87.8% | 82/82 = 100.0% | 82/82 = 100.0% | 76/82 = 92.7% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 439

**Selected features:** selected = {b, c, cd, cw, d, eu, i, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 11/19 = 57.9% |
| ActionExchange | 66 | 0/66 = 0.0% | 64/66 = 97.0% | 51/66 = 77.3% | 66/66 = 100.0% | 66/66 = 100.0% | 43/66 = 65.2% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |

### Product 440

**Selected features:** selected = {b, cw, d, dl, eu, i, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 14/23 = 60.9% |
| ActionExchange | 91 | 0/91 = 0.0% | 84/91 = 92.3% | 76/91 = 83.5% | 91/91 = 100.0% | 91/91 = 100.0% | 62/91 = 68.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 441

**Selected features:** selected = {b, c, cw, d, dl, eu, i, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 14/25 = 56.0% |
| ActionExchange | 117 | 0/117 = 0.0% | 109/117 = 93.2% | 99/117 = 84.6% | 117/117 = 100.0% | 117/117 = 100.0% | 77/117 = 65.8% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 442

**Selected features:** selected = {b, c, cw, d, dl, i, ie, tl, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 90 | 0/90 = 0.0% | 83/90 = 92.2% | 70/90 = 77.8% | 90/90 = 100.0% | 90/90 = 100.0% | 62/90 = 68.9% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 443

**Selected features:** selected = {b, c, cw, d, eu, i, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 12/16 = 75.0% |
| ActionExchange | 52 | 0/52 = 0.0% | 46/52 = 88.5% | 44/52 = 84.6% | 52/52 = 100.0% | 52/52 = 100.0% | 41/52 = 78.8% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 444

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, tl, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 23/28 = 82.1% |
| ActionExchange | 115 | 0/115 = 0.0% | 113/115 = 98.3% | 95/115 = 82.6% | 115/115 = 100.0% | 115/115 = 100.0% | 98/115 = 85.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 445

**Selected features:** selected = {b, cw, d, dl, t, up, us, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 23/28 = 82.1% |
| ActionExchange | 101 | 0/101 = 0.0% | 89/101 = 88.1% | 81/101 = 80.2% | 101/101 = 100.0% | 101/101 = 100.0% | 80/101 = 79.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 446

**Selected features:** selected = {b, c, cd, cw, d, dl, i, tl, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 96 | 0/96 = 0.0% | 94/96 = 97.9% | 79/96 = 82.3% | 96/96 = 100.0% | 96/96 = 100.0% | 84/96 = 87.5% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 447

**Selected features:** selected = {b, cw, d, i, ie, tl, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 13/16 = 81.3% |
| ActionExchange | 43 | 0/43 = 0.0% | 38/43 = 88.4% | 34/43 = 79.1% | 43/43 = 100.0% | 43/43 = 100.0% | 35/43 = 81.4% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 9/10 = 90.0% |

### Product 448

**Selected features:** selected = {b, c, cd, cw, d, i, t, tl, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 20/26 = 76.9% |
| ActionExchange | 97 | 0/97 = 0.0% | 90/97 = 92.8% | 80/97 = 82.5% | 97/97 = 100.0% | 97/97 = 100.0% | 73/97 = 75.3% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 449

**Selected features:** selected = {b, c, cd, cw, d, dl, i, us, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 96 | 0/96 = 0.0% | 94/96 = 97.9% | 79/96 = 82.3% | 96/96 = 100.0% | 96/96 = 100.0% | 84/96 = 87.5% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 450

**Selected features:** selected = {b, c, cw, d, eu, i, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |
| ActionExchange | 82 | 0/82 = 0.0% | 75/82 = 91.5% | 72/82 = 87.8% | 82/82 = 100.0% | 82/82 = 100.0% | 76/82 = 92.7% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 451

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 96 | 0/96 = 0.0% | 94/96 = 97.9% | 79/96 = 82.3% | 96/96 = 100.0% | 96/96 = 100.0% | 84/96 = 87.5% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 12/13 = 92.3% |

### Product 452

**Selected features:** selected = {b, cw, d, dl, eu, i, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |
| ActionExchange | 60 | 0/60 = 0.0% | 54/60 = 90.0% | 48/60 = 80.0% | 60/60 = 100.0% | 60/60 = 100.0% | 52/60 = 86.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 453

**Selected features:** selected = {b, cw, d, i, us, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 23 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |
| ActionExchange | 36 | 0/36 = 0.0% | 31/36 = 86.1% | 30/36 = 83.3% | 36/36 = 100.0% | 36/36 = 100.0% | 31/36 = 86.1% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 454

**Selected features:** selected = {b, cd, cw, d, i, ie, t, tl, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 23/31 = 74.2% |
| ActionExchange | 116 | 0/116 = 0.0% | 109/116 = 94.0% | 94/116 = 81.0% | 116/116 = 100.0% | 116/116 = 100.0% | 86/116 = 74.1% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 16/19 = 84.2% |

### Product 455

**Selected features:** selected = {b, c, cw, d, i, t, up, us, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 18/28 = 64.3% |
| ActionExchange | 118 | 0/118 = 0.0% | 104/118 = 88.1% | 100/118 = 84.7% | 118/118 = 100.0% | 118/118 = 100.0% | 85/118 = 72.0% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 456

**Selected features:** selected = {b, c, cw, d, eu, t, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 92 | 0/92 = 0.0% | 80/92 = 87.0% | 77/92 = 83.7% | 92/92 = 100.0% | 92/92 = 100.0% | 71/92 = 77.2% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |

### Product 457

**Selected features:** selected = {b, c, cw, d, dl, eu, t, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 13 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 26/30 = 86.7% |
| ActionExchange | 128 | 0/128 = 0.0% | 114/128 = 89.1% | 104/128 = 81.3% | 128/128 = 100.0% | 128/128 = 100.0% | 109/128 = 85.2% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 18/19 = 94.7% |

### Product 458

**Selected features:** selected = {b, cw, d, i, ie, tl, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 8 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 69 | 0/69 = 0.0% | 63/69 = 91.3% | 57/69 = 82.6% | 69/69 = 100.0% | 69/69 = 100.0% | 56/69 = 81.2% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 459

**Selected features:** selected = {b, c, cw, d, dl, i, t, us, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 15/27 = 55.6% |
| ActionExchange | 117 | 0/117 = 0.0% | 103/117 = 88.0% | 93/117 = 79.5% | 117/117 = 100.0% | 117/117 = 100.0% | 69/117 = 59.0% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 460

**Selected features:** selected = {b, c, cw, d, i, ie, up, us, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 15/23 = 65.2% |
| ActionExchange | 91 | 0/91 = 0.0% | 84/91 = 92.3% | 76/91 = 83.5% | 91/91 = 100.0% | 91/91 = 100.0% | 63/91 = 69.2% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 461

**Selected features:** selected = {b, c, cw, d, i, ie, t, tl, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 18/30 = 60.0% |
| ActionExchange | 128 | 0/128 = 0.0% | 114/128 = 89.1% | 104/128 = 81.3% | 128/128 = 100.0% | 128/128 = 100.0% | 85/128 = 66.4% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 462

**Selected features:** selected = {b, cw, d, eu, t, up, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 21/24 = 87.5% |
| ActionExchange | 70 | 0/70 = 0.0% | 60/70 = 85.7% | 58/70 = 82.9% | 70/70 = 100.0% | 70/70 = 100.0% | 61/70 = 87.1% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 15/16 = 93.8% |

### Product 463

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, t, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 17 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 31/35 = 88.6% | 35/35 = 100.0% | 35/35 = 100.0% | 27/35 = 77.1% |
| ActionExchange | 176 | 0/176 = 0.0% | 167/176 = 94.9% | 148/176 = 84.1% | 176/176 = 100.0% | 176/176 = 100.0% | 136/176 = 77.3% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |

### Product 464

**Selected features:** selected = {b, cd, cw, d, dl, t, up, us, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 24/31 = 77.4% |
| ActionExchange | 116 | 0/116 = 0.0% | 109/116 = 94.0% | 96/116 = 82.8% | 116/116 = 100.0% | 116/116 = 100.0% | 92/116 = 79.3% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |

### Product 465

**Selected features:** selected = {b, c, cw, d, dl, i, t, up, us, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 16 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 19/32 = 59.4% |
| ActionExchange | 159 | 0/159 = 0.0% | 143/159 = 89.9% | 131/159 = 82.4% | 159/159 = 100.0% | 159/159 = 100.0% | 115/159 = 72.3% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |

### Product 466

**Selected features:** selected = {b, c, cd, cw, d, dl, i, t, tl, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 25/30 = 83.3% | 30/30 = 100.0% | 30/30 = 100.0% | 25/30 = 83.3% |
| ActionExchange | 133 | 0/133 = 0.0% | 125/133 = 94.0% | 107/133 = 80.5% | 133/133 = 100.0% | 133/133 = 100.0% | 112/133 = 84.2% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 17/18 = 94.4% |

### Product 467

**Selected features:** selected = {b, cw, d, eu, i, t, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 22/26 = 84.6% |
| ActionExchange | 92 | 0/92 = 0.0% | 80/92 = 87.0% | 77/92 = 83.7% | 92/92 = 100.0% | 92/92 = 100.0% | 76/92 = 82.6% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 468

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, t, tl, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 26/35 = 74.3% |
| ActionExchange | 154 | 0/154 = 0.0% | 146/154 = 94.8% | 124/154 = 80.5% | 154/154 = 100.0% | 154/154 = 100.0% | 114/154 = 74.0% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 19/21 = 90.5% |

### Product 469

**Selected features:** selected = {b, c, cw, d, dl, i, tl, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 74/81 = 91.4% | 66/81 = 81.5% | 81/81 = 100.0% | 81/81 = 100.0% | 58/81 = 71.6% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 470

**Selected features:** selected = {b, c, cw, d, dl, eu, i, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |
| ActionExchange | 81 | 0/81 = 0.0% | 74/81 = 91.4% | 66/81 = 81.5% | 81/81 = 100.0% | 81/81 = 100.0% | 58/81 = 71.6% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 471

**Selected features:** selected = {b, cw, d, i, up, us, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 15/19 = 78.9% |
| ActionExchange | 61 | 0/61 = 0.0% | 55/61 = 90.2% | 53/61 = 86.9% | 61/61 = 100.0% | 61/61 = 100.0% | 48/61 = 78.7% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 472

**Selected features:** selected = {b, cd, cw, d, us, w}

**Repaired FTS:** 10 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 14/15 = 93.3% | 11/15 = 73.3% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |
| ActionExchange | 36 | 0/36 = 0.0% | 34/36 = 94.4% | 27/36 = 75.0% | 36/36 = 100.0% | 36/36 = 100.0% | 28/36 = 77.8% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 473

**Selected features:** selected = {b, c, cd, cw, d, eu, i, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 15/24 = 62.5% |
| ActionExchange | 97 | 0/97 = 0.0% | 95/97 = 97.9% | 85/97 = 87.6% | 97/97 = 100.0% | 97/97 = 100.0% | 65/97 = 67.0% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 474

**Selected features:** selected = {b, cd, cw, d, dl, i, tl, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 106 | 0/106 = 0.0% | 104/106 = 98.1% | 91/106 = 85.8% | 106/106 = 100.0% | 106/106 = 100.0% | 82/106 = 77.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 475

**Selected features:** selected = {b, c, cw, d, i, t, tl, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 18/28 = 64.3% |
| ActionExchange | 118 | 0/118 = 0.0% | 104/118 = 88.1% | 100/118 = 84.7% | 118/118 = 100.0% | 118/118 = 100.0% | 85/118 = 72.0% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 476

**Selected features:** selected = {b, cd, cw, d, i, ie, t, up, us, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 23/31 = 74.2% |
| ActionExchange | 116 | 0/116 = 0.0% | 109/116 = 94.0% | 94/116 = 81.0% | 116/116 = 100.0% | 116/116 = 100.0% | 86/116 = 74.1% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 16/19 = 84.2% |

### Product 477

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 74 | 0/74 = 0.0% | 72/74 = 97.3% | 60/74 = 81.1% | 74/74 = 100.0% | 74/74 = 100.0% | 60/74 = 81.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 478

**Selected features:** selected = {b, cd, cw, d, i, tl, up, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 15/22 = 68.2% |
| ActionExchange | 75 | 0/75 = 0.0% | 73/75 = 97.3% | 60/75 = 80.0% | 75/75 = 100.0% | 75/75 = 100.0% | 58/75 = 77.3% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 479

**Selected features:** selected = {b, cd, cw, d, eu, i, t, up, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 21/29 = 72.4% |
| ActionExchange | 107 | 0/107 = 0.0% | 100/107 = 93.5% | 90/107 = 84.1% | 107/107 = 100.0% | 107/107 = 100.0% | 77/107 = 72.0% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 15/18 = 83.3% |

### Product 480

**Selected features:** selected = {b, c, cw, d, i, us, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 12/16 = 75.0% |
| ActionExchange | 52 | 0/52 = 0.0% | 46/52 = 88.5% | 44/52 = 84.6% | 52/52 = 100.0% | 52/52 = 100.0% | 41/52 = 78.8% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 481

**Selected features:** selected = {b, cw, d, eu, i, t, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 61 | 0/61 = 0.0% | 51/61 = 83.6% | 49/61 = 80.3% | 61/61 = 100.0% | 61/61 = 100.0% | 45/61 = 73.8% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 482

**Selected features:** selected = {b, cd, cw, d, dl, t, tl, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 24/31 = 77.4% |
| ActionExchange | 116 | 0/116 = 0.0% | 109/116 = 94.0% | 96/116 = 82.8% | 116/116 = 100.0% | 116/116 = 100.0% | 92/116 = 79.3% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 17/19 = 89.5% |

### Product 483

**Selected features:** selected = {b, cd, cw, d, eu, i, ie, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 17/24 = 70.8% |
| ActionExchange | 83 | 0/83 = 0.0% | 81/83 = 97.6% | 69/83 = 83.1% | 83/83 = 100.0% | 83/83 = 100.0% | 60/83 = 72.3% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 484

**Selected features:** selected = {b, cd, cw, d, t, tl, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 57 | 0/57 = 0.0% | 52/57 = 91.2% | 42/57 = 73.7% | 57/57 = 100.0% | 57/57 = 100.0% | 40/57 = 70.2% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 485

**Selected features:** selected = {b, cw, d, dl, i, tl, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 12 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 14/23 = 60.9% |
| ActionExchange | 91 | 0/91 = 0.0% | 84/91 = 92.3% | 76/91 = 83.5% | 91/91 = 100.0% | 91/91 = 100.0% | 62/91 = 68.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 486

**Selected features:** selected = {b, cd, cw, d, i, t, tl, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 16/24 = 66.7% |
| ActionExchange | 75 | 0/75 = 0.0% | 69/75 = 92.0% | 61/75 = 81.3% | 75/75 = 100.0% | 75/75 = 100.0% | 48/75 = 64.0% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 487

**Selected features:** selected = {b, cd, cw, d, i, up, us, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 15/22 = 68.2% |
| ActionExchange | 75 | 0/75 = 0.0% | 73/75 = 97.3% | 60/75 = 80.0% | 75/75 = 100.0% | 75/75 = 100.0% | 58/75 = 77.3% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 488

**Selected features:** selected = {b, cd, cw, d, i, ie, tl, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 17/24 = 70.8% |
| ActionExchange | 83 | 0/83 = 0.0% | 81/83 = 97.6% | 69/83 = 83.1% | 83/83 = 100.0% | 83/83 = 100.0% | 60/83 = 72.3% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 489

**Selected features:** selected = {b, c, cd, cw, d, i, ie, t, us, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 16/28 = 57.1% |
| ActionExchange | 106 | 0/106 = 0.0% | 99/106 = 93.4% | 84/106 = 79.2% | 106/106 = 100.0% | 106/106 = 100.0% | 60/106 = 56.6% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 490

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 106 | 0/106 = 0.0% | 104/106 = 98.1% | 91/106 = 85.8% | 106/106 = 100.0% | 106/106 = 100.0% | 82/106 = 77.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 491

**Selected features:** selected = {b, cd, cw, d, i, t, up, us, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 11 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 21/29 = 72.4% |
| ActionExchange | 107 | 0/107 = 0.0% | 100/107 = 93.5% | 90/107 = 84.1% | 107/107 = 100.0% | 107/107 = 100.0% | 77/107 = 72.0% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 15/18 = 83.3% |

### Product 492

**Selected features:** selected = {b, cd, cw, d, tl, w}

**Repaired FTS:** 10 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 14/15 = 93.3% | 11/15 = 73.3% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |
| ActionExchange | 36 | 0/36 = 0.0% | 34/36 = 94.4% | 27/36 = 75.0% | 36/36 = 100.0% | 36/36 = 100.0% | 28/36 = 77.8% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 493

**Selected features:** selected = {b, cw, d, i, t, us, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 61 | 0/61 = 0.0% | 51/61 = 83.6% | 49/61 = 80.3% | 61/61 = 100.0% | 61/61 = 100.0% | 45/61 = 73.8% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 494

**Selected features:** selected = {b, cw, d, i, t, up, us, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 22/26 = 84.6% |
| ActionExchange | 92 | 0/92 = 0.0% | 80/92 = 87.0% | 77/92 = 83.7% | 92/92 = 100.0% | 92/92 = 100.0% | 76/92 = 82.6% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 16/17 = 94.1% |

### Product 495

**Selected features:** selected = {b, cd, cw, d, dl, i, t, tl, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 21/28 = 75.0% |
| ActionExchange | 106 | 0/106 = 0.0% | 99/106 = 93.4% | 84/106 = 79.2% | 106/106 = 100.0% | 106/106 = 100.0% | 83/106 = 78.3% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |

### Product 496

**Selected features:** selected = {b, cw, d, i, t, tl, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 15/21 = 71.4% |
| ActionExchange | 61 | 0/61 = 0.0% | 51/61 = 83.6% | 49/61 = 80.3% | 61/61 = 100.0% | 61/61 = 100.0% | 45/61 = 73.8% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 497

**Selected features:** selected = {b, cw, d, i, ie, t, tl, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 9 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 18/23 = 78.3% |
| ActionExchange | 69 | 0/69 = 0.0% | 59/69 = 85.5% | 53/69 = 76.8% | 69/69 = 100.0% | 69/69 = 100.0% | 55/69 = 79.7% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 498

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, t, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 10 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 21/28 = 75.0% |
| ActionExchange | 106 | 0/106 = 0.0% | 99/106 = 93.4% | 84/106 = 79.2% | 106/106 = 100.0% | 106/106 = 100.0% | 83/106 = 78.3% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 15/17 = 88.2% |
---

## BankAccountv2 summary (aggregate over 498 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 16 test case(s) generated once for the SPL.

**Equivalent-mutant treatment:** Inozemtseva &amp; Holmes (2014) — mutant not killed by ANY of the five suites (family + product state + product transition + product pair + random) is conservatively classified equivalent and EXCLUDED from the score denominator. Scores below are **killed / (total &minus; equivalent) = adjusted%**.

| Operator | Total mutants | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12132 | 0/12132 = 0.0% | 10818/12132 = 89.2% | 10374/12132 = 85.5% | 12132/12132 = 100.0% | 12132/12132 = 100.0% | 8946/12132 = 73.7% |
| ActionExchange | 45465 | 0/45465 = 0.0% | 41862/45465 = 92.1% | 37557/45465 = 82.6% | 45465/45465 = 100.0% | 45465/45465 = 100.0% | 34824/45465 = 76.6% |
| StateMissing | 7473 | 0/7473 = 0.0% | 6981/7473 = 93.4% | 7473/7473 = 100.0% | 7473/7473 = 100.0% | 7473/7473 = 100.0% | 6057/7473 = 81.1% |

Total products: 498.
