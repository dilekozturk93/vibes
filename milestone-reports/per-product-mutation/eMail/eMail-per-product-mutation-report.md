# Per-Product Mutation Report — eMail

## How mutation scores are computed

**Why a fresh mutation module, not `vibes-mutation`.** The legacy `vibes-mutation` module (commented out in the root pom) is on the old `be.unamur.transitionsystem.*` + `be.unamur.fts.fexpression.*` namespaces and transitively depends on `vibes-transformation` (also dormant) plus a non-existent `vibes-execution` module. Re-vivifying all three for just the two operators referenced in the ICTSS abstract (TransitionMissing, ActionExchange) would have been disproportionate; we re-implement against the current `be.vibes.ts.*` types in `vibes-testgeneration/.../mutation/`.

**Pipeline per product:**

1. **Project + repair.** Same as the coverage reports — `FExpressionPreservingProjection.project` followed by `InitialSccFilter.keepInitialScc` gives the product-level repaired FTS (the system under test for this product).
2. **Generate test suites.** Three independent generators produce one suite per criterion: `StateCoverageGenerator.generate` (one TestCase, greedy + BFS reroute), `TransitionCoverageGenerator.generate` (one TestCase, Chinese-Postman + Hierholzer Euler cycle), and `TransitionPairCoverageGenerator.generate` (suite of TestCases via pair-graph Hierholzer, deduped by action sequence).
3. **Generate mutants.** [`TransitionMissing`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/TransitionMissing.java) emits one mutant per transition (the transition is removed). [`ActionExchange`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/ActionExchange.java) emits one mutant per (transition, alternative-action) pair (the transition's action label is swapped to the alternative).
4. **Filter synthetic mutants.** A mutant whose mutation site is on a synthetic transition (`__end__`) is dropped from the denominator. The SUT doesn't have such a transition; whether a test suite happens to 'kill' such a mutant is not a meaningful signal about real fault detection.
5. **Replay test suite on each mutant.** A TestCase **kills** a mutant iff at least one of its non-synthetic transitions `(source, action, target)` is NOT present in the mutant. For TransitionMissing this happens whenever the suite traverses the removed transition; for ActionExchange whenever the suite traverses the mutated transition (the original `(s, a_orig, t)` triple is gone — replaced by `(s, a_new, t)`).
6. **Mutation score per criterion** = killed mutants / total real mutants. Higher is better. The central RQ2 claim is that the score monotonically increases with the coverage criterion's strictness (state &le; transition &le; transition-pair).

**Note on kill semantics.** Strict definition: the test suite kills the mutant iff running the suite on the mutant produces a different observable behaviour from running it on the original (e.g. a transition refused mid-execution, or an extra transition fired). For both operators, this is equivalent to the cheaper static check used here — "some real transition in the test case is not in the mutant" — because both operators only modify the FTS's transition set, not its execution semantics.

---

## Products


### Product 1

**Selected features:** selected = {e, en, s}

**Repaired FTS:** 7 states, 14 transitions (13 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 9/13 = 69.2% | 6/13 = 46.2% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |
| ActionExchange | 117 | 0/117 = 0.0% | 87/117 = 74.4% | 60/117 = 51.3% | 117/117 = 100.0% | 117/117 = 100.0% | 88/117 = 75.2% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 2

**Selected features:** selected = {au, e, en, s}

**Repaired FTS:** 9 states, 19 transitions (17 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 11/17 = 64.7% | 10/17 = 58.8% | 17/17 = 100.0% | 17/17 = 100.0% | 6/17 = 35.3% |
| ActionExchange | 187 | 0/187 = 0.0% | 130/187 = 69.5% | 117/187 = 62.6% | 187/187 = 100.0% | 187/187 = 100.0% | 69/187 = 36.9% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 4/8 = 50.0% |

### Product 3

**Selected features:** selected = {ad, au, e, f}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 19 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 13/19 = 68.4% | 11/19 = 57.9% | 19/19 = 100.0% | 19/19 = 100.0% | 15/19 = 78.9% |
| ActionExchange | 247 | 0/247 = 0.0% | 175/247 = 70.9% | 151/247 = 61.1% | 247/247 = 100.0% | 247/247 = 100.0% | 201/247 = 81.4% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |

### Product 4

**Selected features:** selected = {ad, e, en, s}

**Repaired FTS:** 9 states, 18 transitions (17 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 12/17 = 70.6% | 9/17 = 52.9% | 17/17 = 100.0% | 17/17 = 100.0% | 14/17 = 82.4% |
| ActionExchange | 221 | 0/221 = 0.0% | 163/221 = 73.8% | 124/221 = 56.1% | 221/221 = 100.0% | 221/221 = 100.0% | 186/221 = 84.2% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 5

**Selected features:** selected = {ad, au, e, en, s}

**Repaired FTS:** 11 states, 23 transitions (21 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 20 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 14/21 = 66.7% | 13/21 = 61.9% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |
| ActionExchange | 315 | 0/315 = 0.0% | 221/315 = 70.2% | 203/315 = 64.4% | 315/315 = 100.0% | 315/315 = 100.0% | 190/315 = 60.3% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 6

**Selected features:** selected = {ad, au, e}

**Repaired FTS:** 10 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 11/18 = 61.1% | 11/18 = 61.1% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |
| ActionExchange | 216 | 0/216 = 0.0% | 138/216 = 63.9% | 140/216 = 64.8% | 216/216 = 100.0% | 216/216 = 100.0% | 102/216 = 47.2% |
| StateMissing | 9 | 0/9 = 0.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 7

**Selected features:** selected = {e, f}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 11 | 0/11 = 0.0% | 8/11 = 72.7% | 6/11 = 54.5% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |
| ActionExchange | 77 | 0/77 = 0.0% | 56/77 = 72.7% | 42/77 = 54.5% | 77/77 = 100.0% | 77/77 = 100.0% | 55/77 = 71.4% |
| StateMissing | 5 | 0/5 = 0.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% |

### Product 8

**Selected features:** selected = {ad, e, en}

**Repaired FTS:** 9 states, 17 transitions (16 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 12/16 = 75.0% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |
| ActionExchange | 192 | 0/192 = 0.0% | 150/192 = 78.1% | 114/192 = 59.4% | 192/192 = 100.0% | 192/192 = 100.0% | 138/192 = 71.9% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 7/8 = 87.5% |

### Product 9

**Selected features:** selected = {e, f, s}

**Repaired FTS:** 6 states, 13 transitions (12 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 8/12 = 66.7% | 6/12 = 50.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |
| ActionExchange | 96 | 0/96 = 0.0% | 66/96 = 68.8% | 48/96 = 50.0% | 96/96 = 100.0% | 96/96 = 100.0% | 64/96 = 66.7% |
| StateMissing | 5 | 0/5 = 0.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% |

### Product 10

**Selected features:** selected = {ad, au, e, en}

**Repaired FTS:** 11 states, 22 transitions (20 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 20 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 14/20 = 70.0% | 13/20 = 65.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |
| ActionExchange | 280 | 0/280 = 0.0% | 206/280 = 73.6% | 190/280 = 67.9% | 280/280 = 100.0% | 280/280 = 100.0% | 177/280 = 63.2% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 6/10 = 60.0% |

### Product 11

**Selected features:** selected = {au, e, en}

**Repaired FTS:** 9 states, 18 transitions (16 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 11/16 = 68.8% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% | 12/16 = 75.0% |
| ActionExchange | 160 | 0/160 = 0.0% | 117/160 = 73.1% | 98/160 = 61.3% | 160/160 = 100.0% | 160/160 = 100.0% | 122/160 = 76.3% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 6/8 = 75.0% |

### Product 12

**Selected features:** selected = {e, en}

**Repaired FTS:** 7 states, 13 transitions (12 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 9/12 = 75.0% | 6/12 = 50.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% |
| ActionExchange | 96 | 0/96 = 0.0% | 76/96 = 79.2% | 53/96 = 55.2% | 96/96 = 100.0% | 96/96 = 100.0% | 96/96 = 100.0% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 13

**Selected features:** selected = {ad, e, f}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 11/15 = 73.3% | 9/15 = 60.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |
| ActionExchange | 165 | 0/165 = 0.0% | 123/165 = 74.5% | 99/165 = 60.0% | 165/165 = 100.0% | 165/165 = 100.0% | 154/165 = 93.3% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 14

**Selected features:** selected = {au, e, f}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 10/15 = 66.7% | 9/15 = 60.0% | 15/15 = 100.0% | 15/15 = 100.0% | 8/15 = 53.3% |
| ActionExchange | 135 | 0/135 = 0.0% | 95/135 = 70.4% | 88/135 = 65.2% | 135/135 = 100.0% | 135/135 = 100.0% | 76/135 = 56.3% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 4/7 = 57.1% |

### Product 15

**Selected features:** selected = {ad, e}

**Repaired FTS:** 8 states, 15 transitions (14 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 9/14 = 64.3% | 8/14 = 57.1% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |
| ActionExchange | 140 | 0/140 = 0.0% | 92/140 = 65.7% | 82/140 = 58.6% | 140/140 = 100.0% | 140/140 = 100.0% | 82/140 = 58.6% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 4/7 = 57.1% |

### Product 16

**Selected features:** selected = {au, e, s}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 8/15 = 53.3% | 8/15 = 53.3% | 15/15 = 100.0% | 15/15 = 100.0% | 4/15 = 26.7% |
| ActionExchange | 135 | 0/135 = 0.0% | 77/135 = 57.0% | 79/135 = 58.5% | 135/135 = 100.0% | 135/135 = 100.0% | 40/135 = 29.6% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 2/7 = 28.6% |

### Product 17

**Selected features:** selected = {ad, e, f, s}

**Repaired FTS:** 8 states, 17 transitions (16 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 11/16 = 68.8% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% | 9/16 = 56.3% |
| ActionExchange | 192 | 0/192 = 0.0% | 134/192 = 69.8% | 108/192 = 56.3% | 192/192 = 100.0% | 192/192 = 100.0% | 111/192 = 57.8% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 6/7 = 85.7% |

### Product 18

**Selected features:** selected = {ad, e, s}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 9/15 = 60.0% | 8/15 = 53.3% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |
| ActionExchange | 165 | 0/165 = 0.0% | 101/165 = 61.2% | 90/165 = 54.5% | 165/165 = 100.0% | 165/165 = 100.0% | 158/165 = 95.8% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 19

**Selected features:** selected = {au, e}

**Repaired FTS:** 7 states, 15 transitions (13 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 8/13 = 61.5% | 7/13 = 53.8% | 13/13 = 100.0% | 13/13 = 100.0% | 6/13 = 46.2% |
| ActionExchange | 104 | 0/104 = 0.0% | 69/104 = 66.3% | 59/104 = 56.7% | 104/104 = 100.0% | 104/104 = 100.0% | 51/104 = 49.0% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 4/6 = 66.7% |

### Product 20

**Selected features:** selected = {e, s}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

**Family baseline projected to this product:** 4 test case(s) (of 6 family-level), 11 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 11 | 0/11 = 0.0% | 6/11 = 54.5% | 5/11 = 45.5% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |
| ActionExchange | 77 | 0/77 = 0.0% | 42/77 = 54.5% | 35/77 = 45.5% | 77/77 = 100.0% | 77/77 = 100.0% | 66/77 = 85.7% |
| StateMissing | 5 | 0/5 = 0.0% | 4/5 = 80.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% |

### Product 21

**Selected features:** selected = {ad, au, e, s}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 11/19 = 57.9% | 11/19 = 57.9% | 19/19 = 100.0% | 19/19 = 100.0% | 7/19 = 36.8% |
| ActionExchange | 247 | 0/247 = 0.0% | 149/247 = 60.3% | 151/247 = 61.1% | 247/247 = 100.0% | 247/247 = 100.0% | 97/247 = 39.3% |
| StateMissing | 9 | 0/9 = 0.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 4/9 = 44.4% |

### Product 22

**Selected features:** selected = {ad, au, e, f, s}

**Repaired FTS:** 10 states, 22 transitions (20 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 19 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 13/20 = 65.0% | 11/20 = 55.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |
| ActionExchange | 280 | 0/280 = 0.0% | 188/280 = 67.1% | 162/280 = 57.9% | 280/280 = 100.0% | 280/280 = 100.0% | 192/280 = 68.6% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 23

**Selected features:** selected = {au, e, f, s}

**Repaired FTS:** 8 states, 18 transitions (16 real / 2 `__end__`).

**Family baseline projected to this product:** 5 test case(s) (of 6 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 10/16 = 62.5% | 8/16 = 50.0% | 16/16 = 100.0% | 16/16 = 100.0% | 7/16 = 43.8% |
| ActionExchange | 160 | 0/160 = 0.0% | 105/160 = 65.6% | 87/160 = 54.4% | 160/160 = 100.0% | 160/160 = 100.0% | 74/160 = 46.3% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 5/7 = 71.4% |
---

## eMail summary (aggregate over 23 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 6 test case(s) generated once for the SPL.

**Equivalent-mutant treatment:** Inozemtseva &amp; Holmes (2014) — mutant not killed by ANY of the five suites (family + product state + product transition + product pair + random) is conservatively classified equivalent and EXCLUDED from the score denominator. Scores below are **killed / (total &minus; equivalent) = adjusted%**.

| Operator | Total mutants | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 361 | 0/361 = 0.0% | 238/361 = 65.9% | 202/361 = 56.0% | 361/361 = 100.0% | 361/361 = 100.0% | 226/361 = 62.6% |
| ActionExchange | 4004 | 0/4004 = 0.0% | 2760/4004 = 68.9% | 2380/4004 = 59.4% | 4004/4004 = 100.0% | 4004/4004 = 100.0% | 2589/4004 = 64.7% |
| StateMissing | 170 | 0/170 = 0.0% | 164/170 = 96.5% | 170/170 = 100.0% | 170/170 = 100.0% | 170/170 = 100.0% | 130/170 = 76.5% |

Total products: 23.
