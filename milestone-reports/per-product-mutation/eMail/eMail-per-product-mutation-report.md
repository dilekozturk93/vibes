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

**Note on coverage saturation.** TransitionMissing and ActionExchange mutants on a deterministic FTS are killed precisely when the mutated transition is traversed; transition coverage therefore detects them by construction. Stronger criteria such as transition-pair coverage cannot improve detection for this operator set, though they do increase execution cost. This is an inherent property of these mutation operators on deterministic models. RQ3 is reframed around the cost dimension under this saturation property.

---

## Products


### Product 1

**Selected features:** selected = {e, en}

**Repaired FTS:** 7 states, 13 transitions (12 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 9/12 = 75.0% | 6/12 = 50.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |
| ActionExchange | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 20/31 = 64.5% | 31/31 = 100.0% | 31/31 = 100.0% | 26/31 = 83.9% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 4/6 = 66.7% |

### Product 2

**Selected features:** selected = {ad, au, e, f, s}

**Repaired FTS:** 10 states, 22 transitions (20 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 19 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 13/20 = 65.0% | 11/20 = 55.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |
| ActionExchange | 71 | 0/71 = 0.0% | 59/71 = 83.1% | 46/71 = 64.8% | 71/71 = 100.0% | 71/71 = 100.0% | 39/71 = 54.9% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 4/9 = 44.4% |

### Product 3

**Selected features:** selected = {e, f, s}

**Repaired FTS:** 6 states, 13 transitions (12 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 8/12 = 66.7% | 6/12 = 50.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% |
| ActionExchange | 31 | 0/31 = 0.0% | 24/31 = 77.4% | 14/31 = 45.2% | 31/31 = 100.0% | 31/31 = 100.0% | 31/31 = 100.0% |
| StateMissing | 5 | 0/5 = 0.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% |

### Product 4

**Selected features:** selected = {ad, au, e, en, s}

**Repaired FTS:** 11 states, 23 transitions (21 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 20 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 14/21 = 66.7% | 13/21 = 61.9% | 21/21 = 100.0% | 21/21 = 100.0% | 8/21 = 38.1% |
| ActionExchange | 82 | 0/82 = 0.0% | 72/82 = 87.8% | 58/82 = 70.7% | 82/82 = 100.0% | 82/82 = 100.0% | 49/82 = 59.8% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 4/10 = 40.0% |

### Product 5

**Selected features:** selected = {e, s}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 11 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 11 | 0/11 = 0.0% | 6/11 = 54.5% | 5/11 = 45.5% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |
| ActionExchange | 29 | 0/29 = 0.0% | 18/29 = 62.1% | 13/29 = 44.8% | 29/29 = 100.0% | 29/29 = 100.0% | 29/29 = 100.0% |
| StateMissing | 5 | 0/5 = 0.0% | 4/5 = 80.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% |

### Product 6

**Selected features:** selected = {ad, e, en}

**Repaired FTS:** 9 states, 17 transitions (16 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 12/16 = 75.0% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% | 15/16 = 93.8% |
| ActionExchange | 51 | 0/51 = 0.0% | 46/51 = 90.2% | 32/51 = 62.7% | 51/51 = 100.0% | 51/51 = 100.0% | 48/51 = 94.1% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 7

**Selected features:** selected = {au, e, en}

**Repaired FTS:** 9 states, 18 transitions (16 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 11/16 = 68.8% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% | 8/16 = 50.0% |
| ActionExchange | 47 | 0/47 = 0.0% | 44/47 = 93.6% | 35/47 = 74.5% | 47/47 = 100.0% | 47/47 = 100.0% | 30/47 = 63.8% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 6/8 = 75.0% |

### Product 8

**Selected features:** selected = {ad, au, e, s}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 11/19 = 57.9% | 11/19 = 57.9% | 19/19 = 100.0% | 19/19 = 100.0% | 10/19 = 52.6% |
| ActionExchange | 69 | 0/69 = 0.0% | 52/69 = 75.4% | 45/69 = 65.2% | 69/69 = 100.0% | 69/69 = 100.0% | 47/69 = 68.1% |
| StateMissing | 9 | 0/9 = 0.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 9

**Selected features:** selected = {e, f}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 11 | 0/11 = 0.0% | 8/11 = 72.7% | 6/11 = 54.5% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |
| ActionExchange | 24 | 0/24 = 0.0% | 19/24 = 79.2% | 13/24 = 54.2% | 24/24 = 100.0% | 24/24 = 100.0% | 21/24 = 87.5% |
| StateMissing | 5 | 0/5 = 0.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 4/5 = 80.0% |

### Product 10

**Selected features:** selected = {au, e, s}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 8/15 = 53.3% | 8/15 = 53.3% | 15/15 = 100.0% | 15/15 = 100.0% | 11/15 = 73.3% |
| ActionExchange | 45 | 0/45 = 0.0% | 33/45 = 73.3% | 32/45 = 71.1% | 45/45 = 100.0% | 45/45 = 100.0% | 37/45 = 82.2% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 11

**Selected features:** selected = {ad, au, e, en}

**Repaired FTS:** 11 states, 22 transitions (20 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 20 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 14/20 = 70.0% | 13/20 = 65.0% | 20/20 = 100.0% | 20/20 = 100.0% | 14/20 = 70.0% |
| ActionExchange | 73 | 0/73 = 0.0% | 67/73 = 91.8% | 56/73 = 76.7% | 73/73 = 100.0% | 73/73 = 100.0% | 58/73 = 79.5% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% |

### Product 12

**Selected features:** selected = {ad, e, f, s}

**Repaired FTS:** 8 states, 17 transitions (16 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 11/16 = 68.8% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |
| ActionExchange | 49 | 0/49 = 0.0% | 38/49 = 77.6% | 25/49 = 51.0% | 49/49 = 100.0% | 49/49 = 100.0% | 44/49 = 89.8% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 13

**Selected features:** selected = {au, e, f, s}

**Repaired FTS:** 8 states, 18 transitions (16 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 10/16 = 62.5% | 8/16 = 50.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |
| ActionExchange | 47 | 0/47 = 0.0% | 39/47 = 83.0% | 33/47 = 70.2% | 47/47 = 100.0% | 47/47 = 100.0% | 43/47 = 91.5% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 14

**Selected features:** selected = {au, e, en, s}

**Repaired FTS:** 9 states, 19 transitions (17 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 11/17 = 64.7% | 10/17 = 58.8% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |
| ActionExchange | 56 | 0/56 = 0.0% | 50/56 = 89.3% | 40/56 = 71.4% | 56/56 = 100.0% | 56/56 = 100.0% | 41/56 = 73.2% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 6/8 = 75.0% |

### Product 15

**Selected features:** selected = {ad, e, f}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 11/15 = 73.3% | 9/15 = 60.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |
| ActionExchange | 42 | 0/42 = 0.0% | 35/42 = 83.3% | 24/42 = 57.1% | 42/42 = 100.0% | 42/42 = 100.0% | 36/42 = 85.7% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 16

**Selected features:** selected = {ad, au, e}

**Repaired FTS:** 10 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 11/18 = 61.1% | 11/18 = 61.1% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |
| ActionExchange | 62 | 0/62 = 0.0% | 49/62 = 79.0% | 44/62 = 71.0% | 62/62 = 100.0% | 62/62 = 100.0% | 46/62 = 74.2% |
| StateMissing | 9 | 0/9 = 0.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 5/9 = 55.6% |

### Product 17

**Selected features:** selected = {au, e, f}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 10/15 = 66.7% | 9/15 = 60.0% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |
| ActionExchange | 40 | 0/40 = 0.0% | 36/40 = 90.0% | 36/40 = 90.0% | 40/40 = 100.0% | 40/40 = 100.0% | 33/40 = 82.5% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 4/7 = 57.1% |

### Product 18

**Selected features:** selected = {ad, e}

**Repaired FTS:** 8 states, 15 transitions (14 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 9/14 = 64.3% | 8/14 = 57.1% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |
| ActionExchange | 40 | 0/40 = 0.0% | 30/40 = 75.0% | 24/40 = 60.0% | 40/40 = 100.0% | 40/40 = 100.0% | 34/40 = 85.0% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 19

**Selected features:** selected = {ad, e, en, s}

**Repaired FTS:** 9 states, 18 transitions (17 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 12/17 = 70.6% | 9/17 = 52.9% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |
| ActionExchange | 60 | 0/60 = 0.0% | 51/60 = 85.0% | 35/60 = 58.3% | 60/60 = 100.0% | 60/60 = 100.0% | 46/60 = 76.7% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 20

**Selected features:** selected = {e, en, s}

**Repaired FTS:** 7 states, 14 transitions (13 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 9/13 = 69.2% | 6/13 = 46.2% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |
| ActionExchange | 40 | 0/40 = 0.0% | 35/40 = 87.5% | 23/40 = 57.5% | 40/40 = 100.0% | 40/40 = 100.0% | 36/40 = 90.0% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 21

**Selected features:** selected = {au, e}

**Repaired FTS:** 7 states, 15 transitions (13 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 8/13 = 61.5% | 7/13 = 53.8% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |
| ActionExchange | 34 | 0/34 = 0.0% | 30/34 = 88.2% | 21/34 = 61.8% | 34/34 = 100.0% | 34/34 = 100.0% | 33/34 = 97.1% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 22

**Selected features:** selected = {ad, e, s}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 9/15 = 60.0% | 8/15 = 53.3% | 15/15 = 100.0% | 15/15 = 100.0% | 7/15 = 46.7% |
| ActionExchange | 47 | 0/47 = 0.0% | 33/47 = 70.2% | 25/47 = 53.2% | 47/47 = 100.0% | 47/47 = 100.0% | 28/47 = 59.6% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 4/7 = 57.1% |

### Product 23

**Selected features:** selected = {ad, au, e, f}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 19 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 13/19 = 68.4% | 11/19 = 57.9% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |
| ActionExchange | 64 | 0/64 = 0.0% | 56/64 = 87.5% | 45/64 = 70.3% | 64/64 = 100.0% | 64/64 = 100.0% | 46/64 = 71.9% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |
---

## eMail summary (aggregate over 23 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 6 test case(s) generated once for the SPL.

**Equivalent-mutant treatment:** Inozemtseva &amp; Holmes (2014) — mutant not killed by ANY of the five suites (family + product state + product transition + product pair + random) is conservatively classified equivalent and EXCLUDED from the score denominator. Scores below are **killed / (total &minus; equivalent) = adjusted%**.

| Operator | Total mutants | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 361 | 0/361 = 0.0% | 238/361 = 65.9% | 202/361 = 56.0% | 361/361 = 100.0% | 361/361 = 100.0% | 252/361 = 69.8% |
| ActionExchange | 1134 | 0/1134 = 0.0% | 945/1134 = 83.3% | 739/1134 = 65.2% | 1134/1134 = 100.0% | 1134/1134 = 100.0% | 881/1134 = 77.7% |
| StateMissing | 170 | 0/170 = 0.0% | 164/170 = 96.5% | 170/170 = 100.0% | 170/170 = 100.0% | 170/170 = 100.0% | 137/170 = 80.6% |

Total products: 23.
