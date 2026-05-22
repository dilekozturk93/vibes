# Per-Product Mutation Report — BankAccountv2

## How mutation scores are computed

**Why a fresh mutation module, not `vibes-mutation`.** The legacy `vibes-mutation` module (commented out in the root pom) is on the old `be.unamur.transitionsystem.*` + `be.unamur.fts.fexpression.*` namespaces and transitively depends on `vibes-transformation` (also dormant) plus a non-existent `vibes-execution` module. Re-vivifying all three for just the two operators referenced in the ICTSS abstract (TransitionMissing, ActionExchange) would have been disproportionate; we re-implement against the current `be.vibes.ts.*` types in `vibes-testgeneration/.../mutation/`.

**Pipeline per product:**

1. **Project + repair.** Same as the coverage reports — `FExpressionPreservingProjection.project` followed by `InitialSccFilter.keepInitialScc` gives the product-level repaired FTS (the system under test for this product).
2. **Generate test suites.** Three independent generators produce one suite per criterion: `StateCoverageGenerator.generate` (one TestCase, greedy + BFS reroute), `TransitionCoverageGenerator.generate` (one TestCase, Chinese-Postman + Hierholzer Euler cycle), and `TransitionPairCoverageGenerator.generate` (suite of TestCases via pair-graph Hierholzer, deduped by action sequence).
3. **Generate mutants.** [`TransitionMissing`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/TransitionMissing.java) emits one mutant per transition (the transition is removed). [`ActionExchange`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/mutation/ActionExchange.java) emits one mutant per (transition, alternative-action) pair (the transition's action label is swapped to the alternative).
4. **Filter synthetic mutants.** A mutant whose mutation site is on a synthetic transition (`__end__`) is dropped from the denominator. The SUT doesn't have such a transition; whether a test suite happens to 'kill' such a mutant is not a meaningful signal about real fault detection.
5. **Replay test suite on each mutant.** A TestCase **kills** a mutant iff at least one of its non-synthetic transitions `(source, action, target)` is NOT present in the mutant. For TransitionMissing this happens whenever the suite traverses the removed transition; for ActionExchange whenever the suite traverses the mutated transition (the original `(s, a_orig, t)` triple is gone — replaced by `(s, a_new, t)`).
6. **Mutation score per criterion** = killed mutants / (total &minus; equivalent), per Inozemtseva &amp; Holmes (2014). The equivalent set is conservatively defined as mutants surviving all five suites in this study (family + product state + product transition + product pair + random).

**Note on coverage saturation.** TransitionMissing and ActionExchange mutants on a deterministic FTS are killed precisely when the mutated transition is traversed; transition coverage therefore detects them by construction. Stronger criteria such as transition-pair coverage cannot improve detection for this operator set, though they do increase execution cost. This is an inherent property of these mutation operators on deterministic models. RQ3 is reframed around the cost dimension under this saturation property.

---

## Products


### Product 1

**Selected features:** selected = {b, cw, d, dl, o, t, us, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 21/26 = 80.8% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 8/26 = 30.8% |
| ActionExchange | 624 | 0/624 = 0.0% | 505/624 = 80.9% | 507/624 = 81.3% | 624/624 = 100.0% | 624/624 = 100.0% | 198/624 = 31.7% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 5/16 = 31.3% |

### Product 2

**Selected features:** selected = {b, c, cw, d, dl, eu, i, ie, up, w}

**Repaired FTS:** 17 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 24/27 = 88.9% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 8/27 = 29.6% |
| ActionExchange | 675 | 0/675 = 0.0% | 601/675 = 89.0% | 578/675 = 85.6% | 675/675 = 100.0% | 675/675 = 100.0% | 208/675 = 30.8% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 5/16 = 31.3% |

### Product 3

**Selected features:** selected = {b, cd, cw, d, dl, i, o, t, us, w}

**Repaired FTS:** 19 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 28/31 = 90.3% | 25/31 = 80.6% | 31/31 = 100.0% | 31/31 = 100.0% | 6/31 = 19.4% |
| ActionExchange | 868 | 0/868 = 0.0% | 786/868 = 90.6% | 704/868 = 81.1% | 868/868 = 100.0% | 868/868 = 100.0% | 177/868 = 20.4% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 3/18 = 16.7% |

### Product 4

**Selected features:** selected = {b, cd, d, up, us, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 18/19 = 94.7% | 19/19 = 100.0% | 19/19 = 100.0% | 13/19 = 68.4% |
| ActionExchange | 323 | 0/323 = 0.0% | 308/323 = 95.4% | 306/323 = 94.7% | 323/323 = 100.0% | 323/323 = 100.0% | 223/323 = 69.0% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 5

**Selected features:** selected = {b, cw, d, dl, i, ie, o, tl, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 11/28 = 39.3% |
| ActionExchange | 728 | 0/728 = 0.0% | 625/728 = 85.9% | 602/728 = 82.7% | 728/728 = 100.0% | 728/728 = 100.0% | 296/728 = 40.7% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 6/16 = 37.5% |

### Product 6

**Selected features:** selected = {b, d, eu, i, t, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 305/380 = 80.3% | 342/380 = 90.0% | 380/380 = 100.0% | 380/380 = 100.0% | 231/380 = 60.8% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 7

**Selected features:** selected = {b, cw, d, dl, o, up, us, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 8/24 = 33.3% |
| ActionExchange | 528 | 0/528 = 0.0% | 441/528 = 83.5% | 443/528 = 83.9% | 528/528 = 100.0% | 528/528 = 100.0% | 182/528 = 34.5% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 5/14 = 35.7% |

### Product 8

**Selected features:** selected = {b, c, cw, d, dl, i, t, us, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 12/27 = 44.4% |
| ActionExchange | 675 | 0/675 = 0.0% | 576/675 = 85.3% | 577/675 = 85.5% | 675/675 = 100.0% | 675/675 = 100.0% | 309/675 = 45.8% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 8/17 = 47.1% |

### Product 9

**Selected features:** selected = {b, cw, d, dl, i, ie, t, tl, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 27/32 = 84.4% | 32/32 = 100.0% | 32/32 = 100.0% | 17/32 = 53.1% |
| ActionExchange | 960 | 0/960 = 0.0% | 841/960 = 87.6% | 813/960 = 84.7% | 960/960 = 100.0% | 960/960 = 100.0% | 519/960 = 54.1% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |

### Product 10

**Selected features:** selected = {b, c, cd, cw, d, tl, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 10/17 = 58.8% |
| ActionExchange | 255 | 0/255 = 0.0% | 242/255 = 94.9% | 198/255 = 77.6% | 255/255 = 100.0% | 255/255 = 100.0% | 152/255 = 59.6% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 11

**Selected features:** selected = {b, c, cd, cw, d, t, up, us, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 14/29 = 48.3% |
| ActionExchange | 783 | 0/783 = 0.0% | 731/783 = 93.4% | 677/783 = 86.5% | 783/783 = 100.0% | 783/783 = 100.0% | 388/783 = 49.6% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |

### Product 12

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, ie, t, w}

**Repaired FTS:** 20 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 26/32 = 81.3% | 32/32 = 100.0% | 32/32 = 100.0% | 14/32 = 43.8% |
| ActionExchange | 928 | 0/928 = 0.0% | 872/928 = 94.0% | 758/928 = 81.7% | 928/928 = 100.0% | 928/928 = 100.0% | 417/928 = 44.9% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 10/19 = 52.6% |

### Product 13

**Selected features:** selected = {b, c, d, eu, i, t, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 8/22 = 36.4% |
| ActionExchange | 462 | 0/462 = 0.0% | 379/462 = 82.0% | 420/462 = 90.9% | 462/462 = 100.0% | 462/462 = 100.0% | 174/462 = 37.7% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 5/15 = 33.3% |

### Product 14

**Selected features:** selected = {b, cd, cw, d, i, t, tl, up, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 13/29 = 44.8% |
| ActionExchange | 783 | 0/783 = 0.0% | 731/783 = 93.4% | 677/783 = 86.5% | 783/783 = 100.0% | 783/783 = 100.0% | 359/783 = 45.8% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |

### Product 15

**Selected features:** selected = {b, cd, d, tl, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 13/14 = 92.9% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |
| ActionExchange | 168 | 0/168 = 0.0% | 158/168 = 94.0% | 145/168 = 86.3% | 168/168 = 100.0% | 168/168 = 100.0% | 134/168 = 79.8% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 16

**Selected features:** selected = {b, d, eu, up, w}

**Repaired FTS:** 12 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 15/16 = 93.8% | 16/16 = 100.0% | 16/16 = 100.0% | 13/16 = 81.3% |
| ActionExchange | 240 | 0/240 = 0.0% | 196/240 = 81.7% | 225/240 = 93.8% | 240/240 = 100.0% | 240/240 = 100.0% | 196/240 = 81.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 17

**Selected features:** selected = {b, cw, d, dl, i, o, t, up, us, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 28/33 = 84.8% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 12/33 = 36.4% |
| ActionExchange | 1023 | 0/1023 = 0.0% | 869/1023 = 84.9% | 871/1023 = 85.1% | 1023/1023 = 100.0% | 1023/1023 = 100.0% | 385/1023 = 37.6% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 8/20 = 40.0% |

### Product 18

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, t, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 17/33 = 51.5% |
| ActionExchange | 990 | 0/990 = 0.0% | 932/990 = 94.1% | 872/990 = 88.1% | 990/990 = 100.0% | 990/990 = 100.0% | 518/990 = 52.3% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |

### Product 19

**Selected features:** selected = {b, cw, d, up, us, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |
| ActionExchange | 272 | 0/272 = 0.0% | 225/272 = 82.7% | 241/272 = 88.6% | 272/272 = 100.0% | 272/272 = 100.0% | 178/272 = 65.4% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 20

**Selected features:** selected = {b, c, d, t, tl, up, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 11/25 = 44.0% |
| ActionExchange | 600 | 0/600 = 0.0% | 505/600 = 84.2% | 552/600 = 92.0% | 600/600 = 100.0% | 600/600 = 100.0% | 271/600 = 45.2% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 8/17 = 47.1% |

### Product 21

**Selected features:** selected = {b, c, cd, cw, d, dl, t, tl, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 11/28 = 39.3% |
| ActionExchange | 700 | 0/700 = 0.0% | 652/700 = 93.1% | 578/700 = 82.6% | 700/700 = 100.0% | 700/700 = 100.0% | 281/700 = 40.1% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 7/17 = 41.2% |

### Product 22

**Selected features:** selected = {b, cd, d, i, t, up, us, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 11/28 = 39.3% |
| ActionExchange | 728 | 0/728 = 0.0% | 678/728 = 93.1% | 676/728 = 92.9% | 728/728 = 100.0% | 728/728 = 100.0% | 293/728 = 40.2% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |

### Product 23

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, t, up, us, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 17/35 = 48.6% |
| ActionExchange | 1120 | 0/1120 = 0.0% | 1058/1120 = 94.5% | 963/1120 = 86.0% | 1120/1120 = 100.0% | 1120/1120 = 100.0% | 552/1120 = 49.3% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |

### Product 24

**Selected features:** selected = {b, c, cd, d, eu, i, t, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 12/25 = 48.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 531/575 = 92.3% | 507/575 = 88.2% | 575/575 = 100.0% | 575/575 = 100.0% | 284/575 = 49.4% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 8/16 = 50.0% |

### Product 25

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, t, tl, w}

**Repaired FTS:** 20 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 30/33 = 90.9% | 26/33 = 78.8% | 33/33 = 100.0% | 33/33 = 100.0% | 9/33 = 27.3% |
| ActionExchange | 990 | 0/990 = 0.0% | 902/990 = 91.1% | 785/990 = 79.3% | 990/990 = 100.0% | 990/990 = 100.0% | 278/990 = 28.1% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 6/19 = 31.6% |

### Product 26

**Selected features:** selected = {b, d, eu, i, t, up, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 14/25 = 56.0% |
| ActionExchange | 600 | 0/600 = 0.0% | 505/600 = 84.2% | 552/600 = 92.0% | 600/600 = 100.0% | 600/600 = 100.0% | 343/600 = 57.2% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 27

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, t, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 25/30 = 83.3% | 30/30 = 100.0% | 30/30 = 100.0% | 11/30 = 36.7% |
| ActionExchange | 810 | 0/810 = 0.0% | 758/810 = 93.6% | 678/810 = 83.7% | 810/810 = 100.0% | 810/810 = 100.0% | 309/810 = 38.1% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 7/18 = 38.9% |

### Product 28

**Selected features:** selected = {b, c, cw, d, t, tl, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 650 | 0/650 = 0.0% | 551/650 = 84.8% | 576/650 = 88.6% | 650/650 = 100.0% | 650/650 = 100.0% | 452/650 = 69.5% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 29

**Selected features:** selected = {b, d, tl, up, w}

**Repaired FTS:** 12 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 15/16 = 93.8% | 16/16 = 100.0% | 16/16 = 100.0% | 9/16 = 56.3% |
| ActionExchange | 240 | 0/240 = 0.0% | 196/240 = 81.7% | 225/240 = 93.8% | 240/240 = 100.0% | 240/240 = 100.0% | 138/240 = 57.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 6/11 = 54.5% |

### Product 30

**Selected features:** selected = {b, c, d, t, us, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 10/20 = 50.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 305/380 = 80.3% | 342/380 = 90.0% | 380/380 = 100.0% | 380/380 = 100.0% | 194/380 = 51.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 31

**Selected features:** selected = {b, cw, d, eu, w}

**Repaired FTS:** 9 states, 12 transitions (12 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 20 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 9/12 = 75.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 7/12 = 58.3% |
| ActionExchange | 132 | 0/132 = 0.0% | 100/132 = 75.8% | 111/132 = 84.1% | 132/132 = 100.0% | 132/132 = 100.0% | 78/132 = 59.1% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 5/8 = 62.5% |

### Product 32

**Selected features:** selected = {b, cw, d, eu, i, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 23 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |
| ActionExchange | 182 | 0/182 = 0.0% | 144/182 = 79.1% | 157/182 = 86.3% | 182/182 = 100.0% | 182/182 = 100.0% | 108/182 = 59.3% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 33

**Selected features:** selected = {b, cw, d, dl, o, us, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |
| ActionExchange | 323 | 0/323 = 0.0% | 256/323 = 79.3% | 258/323 = 79.9% | 323/323 = 100.0% | 323/323 = 100.0% | 159/323 = 49.2% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 5/11 = 45.5% |

### Product 34

**Selected features:** selected = {b, c, cd, d, eu, i, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 22/23 = 95.7% | 23/23 = 100.0% | 23/23 = 100.0% | 10/23 = 43.5% |
| ActionExchange | 483 | 0/483 = 0.0% | 464/483 = 96.1% | 462/483 = 95.7% | 483/483 = 100.0% | 483/483 = 100.0% | 219/483 = 45.3% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 35

**Selected features:** selected = {b, cd, cw, d, i, ie, up, us, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 11/24 = 45.8% |
| ActionExchange | 528 | 0/528 = 0.0% | 508/528 = 96.2% | 443/528 = 83.9% | 528/528 = 100.0% | 528/528 = 100.0% | 247/528 = 46.8% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 36

**Selected features:** selected = {b, c, cd, cw, d, dl, i, t, us, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 25/30 = 83.3% | 30/30 = 100.0% | 30/30 = 100.0% | 12/30 = 40.0% |
| ActionExchange | 810 | 0/810 = 0.0% | 758/810 = 93.6% | 678/810 = 83.7% | 810/810 = 100.0% | 810/810 = 100.0% | 334/810 = 41.2% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |

### Product 37

**Selected features:** selected = {b, cd, d, eu, t, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 399 | 0/399 = 0.0% | 363/399 = 91.0% | 343/399 = 86.0% | 399/399 = 100.0% | 399/399 = 100.0% | 325/399 = 81.5% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 13/14 = 92.9% |

### Product 38

**Selected features:** selected = {b, c, cd, d, us, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |
| ActionExchange | 224 | 0/224 = 0.0% | 212/224 = 94.6% | 197/224 = 87.9% | 224/224 = 100.0% | 224/224 = 100.0% | 142/224 = 63.4% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 39

**Selected features:** selected = {b, c, cw, d, eu, i, ie, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |
| ActionExchange | 306 | 0/306 = 0.0% | 256/306 = 83.7% | 257/306 = 84.0% | 306/306 = 100.0% | 306/306 = 100.0% | 157/306 = 51.3% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 6/11 = 54.5% |

### Product 40

**Selected features:** selected = {b, cd, d, i, ie, us, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |
| ActionExchange | 288 | 0/288 = 0.0% | 274/288 = 95.1% | 242/288 = 84.0% | 288/288 = 100.0% | 288/288 = 100.0% | 179/288 = 62.2% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 41

**Selected features:** selected = {b, cd, d, eu, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 18/19 = 94.7% | 19/19 = 100.0% | 19/19 = 100.0% | 7/19 = 36.8% |
| ActionExchange | 323 | 0/323 = 0.0% | 308/323 = 95.4% | 306/323 = 94.7% | 323/323 = 100.0% | 323/323 = 100.0% | 122/323 = 37.8% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 5/12 = 41.7% |

### Product 42

**Selected features:** selected = {b, d, i, t, us, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 305/380 = 80.3% | 342/380 = 90.0% | 380/380 = 100.0% | 380/380 = 100.0% | 251/380 = 66.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 43

**Selected features:** selected = {b, cd, cw, d, eu, i, t, up, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 12/29 = 41.4% |
| ActionExchange | 783 | 0/783 = 0.0% | 731/783 = 93.4% | 677/783 = 86.5% | 783/783 = 100.0% | 783/783 = 100.0% | 333/783 = 42.5% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |

### Product 44

**Selected features:** selected = {b, c, d, i, ie, tl, up, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 13/22 = 59.1% |
| ActionExchange | 462 | 0/462 = 0.0% | 400/462 = 86.6% | 421/462 = 91.1% | 462/462 = 100.0% | 462/462 = 100.0% | 279/462 = 60.4% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 45

**Selected features:** selected = {b, cw, d, dl, o, t, up, us, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 26/31 = 83.9% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 11/31 = 35.5% |
| ActionExchange | 899 | 0/899 = 0.0% | 755/899 = 84.0% | 757/899 = 84.2% | 899/899 = 100.0% | 899/899 = 100.0% | 329/899 = 36.6% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 8/19 = 42.1% |

### Product 46

**Selected features:** selected = {b, cd, d, eu, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 13/14 = 92.9% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |
| ActionExchange | 168 | 0/168 = 0.0% | 158/168 = 94.0% | 145/168 = 86.3% | 168/168 = 100.0% | 168/168 = 100.0% | 147/168 = 87.5% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |

### Product 47

**Selected features:** selected = {b, cd, cw, d, dl, i, o, t, up, us, w}

**Repaired FTS:** 22 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 33/36 = 91.7% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 14/36 = 38.9% |
| ActionExchange | 1188 | 0/1188 = 0.0% | 1091/1188 = 91.8% | 1026/1188 = 86.4% | 1188/1188 = 100.0% | 1188/1188 = 100.0% | 477/1188 = 40.2% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 10/21 = 47.6% |

### Product 48

**Selected features:** selected = {b, c, d, t, tl, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 305/380 = 80.3% | 342/380 = 90.0% | 380/380 = 100.0% | 380/380 = 100.0% | 305/380 = 80.3% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 49

**Selected features:** selected = {b, c, cw, d, tl, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 342 | 0/342 = 0.0% | 289/342 = 84.5% | 307/342 = 89.8% | 342/342 = 100.0% | 342/342 = 100.0% | 218/342 = 63.7% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 50

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, o, w}

**Repaired FTS:** 15 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 20/26 = 76.9% | 26/26 = 100.0% | 26/26 = 100.0% | 9/26 = 34.6% |
| ActionExchange | 598 | 0/598 = 0.0% | 554/598 = 92.6% | 465/598 = 77.8% | 598/598 = 100.0% | 598/598 = 100.0% | 214/598 = 35.8% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 6/14 = 42.9% |

### Product 51

**Selected features:** selected = {b, c, cw, d, eu, i, t, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 11/28 = 39.3% |
| ActionExchange | 756 | 0/756 = 0.0% | 649/756 = 85.8% | 676/756 = 89.4% | 756/756 = 100.0% | 756/756 = 100.0% | 306/756 = 40.5% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 7/18 = 38.9% |

### Product 52

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, t, tl, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 13/30 = 43.3% |
| ActionExchange | 810 | 0/810 = 0.0% | 758/810 = 93.6% | 652/810 = 80.5% | 810/810 = 100.0% | 810/810 = 100.0% | 357/810 = 44.1% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |

### Product 53

**Selected features:** selected = {b, cw, d, dl, eu, i, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |
| ActionExchange | 288 | 0/288 = 0.0% | 241/288 = 83.7% | 242/288 = 84.0% | 288/288 = 100.0% | 288/288 = 100.0% | 197/288 = 68.4% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 54

**Selected features:** selected = {b, cd, cw, d, dl, o, us, w}

**Repaired FTS:** 13 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 12/22 = 54.5% |
| ActionExchange | 418 | 0/418 = 0.0% | 382/418 = 91.4% | 327/418 = 78.2% | 418/418 = 100.0% | 418/418 = 100.0% | 231/418 = 55.3% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 55

**Selected features:** selected = {b, c, cd, d, i, us, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |
| ActionExchange | 288 | 0/288 = 0.0% | 274/288 = 95.1% | 257/288 = 89.2% | 288/288 = 100.0% | 288/288 = 100.0% | 133/288 = 46.2% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 5/11 = 45.5% |

### Product 56

**Selected features:** selected = {b, cd, cw, d, dl, eu, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 13/19 = 68.4% |
| ActionExchange | 304 | 0/304 = 0.0% | 290/304 = 95.4% | 243/304 = 79.9% | 304/304 = 100.0% | 304/304 = 100.0% | 212/304 = 69.7% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 57

**Selected features:** selected = {b, cw, d, i, ie, t, us, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 17/23 = 73.9% |
| ActionExchange | 506 | 0/506 = 0.0% | 419/506 = 82.8% | 420/506 = 83.0% | 506/506 = 100.0% | 506/506 = 100.0% | 377/506 = 74.5% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 58

**Selected features:** selected = {b, cw, d, dl, tl, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 12/16 = 75.0% |
| ActionExchange | 224 | 0/224 = 0.0% | 183/224 = 81.7% | 184/224 = 82.1% | 224/224 = 100.0% | 224/224 = 100.0% | 170/224 = 75.9% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 59

**Selected features:** selected = {b, d, i, tl, w}

**Repaired FTS:** 10 states, 13 transitions (13 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 10/13 = 76.9% | 12/13 = 92.3% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |
| ActionExchange | 156 | 0/156 = 0.0% | 121/156 = 77.6% | 144/156 = 92.3% | 156/156 = 100.0% | 156/156 = 100.0% | 121/156 = 77.6% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 60

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, tl, w}

**Repaired FTS:** 15 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 20/26 = 76.9% | 26/26 = 100.0% | 26/26 = 100.0% | 11/26 = 42.3% |
| ActionExchange | 598 | 0/598 = 0.0% | 554/598 = 92.6% | 465/598 = 77.8% | 598/598 = 100.0% | 598/598 = 100.0% | 261/598 = 43.6% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 61

**Selected features:** selected = {b, c, d, eu, i, ie, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |
| ActionExchange | 272 | 0/272 = 0.0% | 225/272 = 82.7% | 241/272 = 88.6% | 272/272 = 100.0% | 272/272 = 100.0% | 210/272 = 77.2% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 62

**Selected features:** selected = {b, cw, d, dl, o, tl, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 323 | 0/323 = 0.0% | 256/323 = 79.3% | 258/323 = 79.9% | 323/323 = 100.0% | 323/323 = 100.0% | 206/323 = 63.8% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 63

**Selected features:** selected = {b, cw, d, i, tl, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 23 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |
| ActionExchange | 182 | 0/182 = 0.0% | 144/182 = 79.1% | 157/182 = 86.3% | 182/182 = 100.0% | 182/182 = 100.0% | 146/182 = 80.2% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 8/9 = 88.9% |

### Product 64

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 8/21 = 38.1% |
| ActionExchange | 378 | 0/378 = 0.0% | 362/378 = 95.8% | 309/378 = 81.7% | 378/378 = 100.0% | 378/378 = 100.0% | 147/378 = 38.9% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 5/12 = 41.7% |

### Product 65

**Selected features:** selected = {b, c, d, i, ie, us, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 14/17 = 82.4% |
| ActionExchange | 272 | 0/272 = 0.0% | 225/272 = 82.7% | 241/272 = 88.6% | 272/272 = 100.0% | 272/272 = 100.0% | 225/272 = 82.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 66

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, o, t, up, w}

**Repaired FTS:** 23 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 35/38 = 92.1% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 12/38 = 31.6% |
| ActionExchange | 1330 | 0/1330 = 0.0% | 1227/1330 = 92.3% | 1124/1330 = 84.5% | 1330/1330 = 100.0% | 1330/1330 = 100.0% | 433/1330 = 32.6% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 8/22 = 36.4% |

### Product 67

**Selected features:** selected = {b, c, cd, cw, d, dl, up, us, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 17/26 = 65.4% |
| ActionExchange | 598 | 0/598 = 0.0% | 577/598 = 96.5% | 531/598 = 88.8% | 598/598 = 100.0% | 598/598 = 100.0% | 400/598 = 66.9% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 68

**Selected features:** selected = {b, c, d, i, ie, tl, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |
| ActionExchange | 272 | 0/272 = 0.0% | 225/272 = 82.7% | 241/272 = 88.6% | 272/272 = 100.0% | 272/272 = 100.0% | 196/272 = 72.1% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 69

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, o, w}

**Repaired FTS:** 14 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 19/24 = 79.2% | 24/24 = 100.0% | 24/24 = 100.0% | 8/24 = 33.3% |
| ActionExchange | 504 | 0/504 = 0.0% | 464/504 = 92.1% | 403/504 = 80.0% | 504/504 = 100.0% | 504/504 = 100.0% | 174/504 = 34.5% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 5/13 = 38.5% |

### Product 70

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, t, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 17/28 = 60.7% |
| ActionExchange | 700 | 0/700 = 0.0% | 652/700 = 93.1% | 578/700 = 82.6% | 700/700 = 100.0% | 700/700 = 100.0% | 427/700 = 61.0% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |

### Product 71

**Selected features:** selected = {b, c, cd, cw, d, dl, i, tl, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 12/23 = 52.2% |
| ActionExchange | 460 | 0/460 = 0.0% | 442/460 = 96.1% | 383/460 = 83.3% | 460/460 = 100.0% | 460/460 = 100.0% | 247/460 = 53.7% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 8/13 = 61.5% |

### Product 72

**Selected features:** selected = {b, c, d, eu, i, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 14/15 = 93.3% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |
| ActionExchange | 210 | 0/210 = 0.0% | 169/210 = 80.5% | 196/210 = 93.3% | 210/210 = 100.0% | 210/210 = 100.0% | 169/210 = 80.5% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 73

**Selected features:** selected = {b, cd, cw, d, tl, up, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 344/360 = 95.6% | 291/360 = 80.8% | 360/360 = 100.0% | 360/360 = 100.0% | 219/360 = 60.8% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 74

**Selected features:** selected = {b, cd, d, t, tl, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 399 | 0/399 = 0.0% | 363/399 = 91.0% | 343/399 = 86.0% | 399/399 = 100.0% | 399/399 = 100.0% | 325/399 = 81.5% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 12/14 = 85.7% |

### Product 75

**Selected features:** selected = {b, cw, d, dl, tl, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 10/21 = 47.6% |
| ActionExchange | 399 | 0/399 = 0.0% | 343/399 = 86.0% | 344/399 = 86.2% | 399/399 = 100.0% | 399/399 = 100.0% | 195/399 = 48.9% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 7/13 = 53.8% |

### Product 76

**Selected features:** selected = {b, cd, d, eu, i, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 14/21 = 66.7% |
| ActionExchange | 399 | 0/399 = 0.0% | 382/399 = 95.7% | 380/399 = 95.2% | 399/399 = 100.0% | 399/399 = 100.0% | 272/399 = 68.2% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 77

**Selected features:** selected = {b, cw, d, dl, eu, i, o, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 399 | 0/399 = 0.0% | 324/399 = 81.2% | 326/399 = 81.7% | 399/399 = 100.0% | 399/399 = 100.0% | 248/399 = 62.2% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 78

**Selected features:** selected = {b, cd, d, i, ie, up, us, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 11/23 = 47.8% |
| ActionExchange | 483 | 0/483 = 0.0% | 464/483 = 96.1% | 442/483 = 91.5% | 483/483 = 100.0% | 483/483 = 100.0% | 234/483 = 48.4% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 79

**Selected features:** selected = {b, c, cw, d, dl, i, ie, up, us, w}

**Repaired FTS:** 17 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 24/27 = 88.9% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 15/27 = 55.6% |
| ActionExchange | 675 | 0/675 = 0.0% | 601/675 = 89.0% | 578/675 = 85.6% | 675/675 = 100.0% | 675/675 = 100.0% | 386/675 = 57.2% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |

### Product 80

**Selected features:** selected = {b, c, cd, cw, d, i, t, tl, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 12/26 = 46.2% |
| ActionExchange | 624 | 0/624 = 0.0% | 578/624 = 92.6% | 530/624 = 84.9% | 624/624 = 100.0% | 624/624 = 100.0% | 298/624 = 47.8% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 8/16 = 50.0% |

### Product 81

**Selected features:** selected = {b, c, cd, cw, d, i, tl, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 7/24 = 29.2% |
| ActionExchange | 528 | 0/528 = 0.0% | 508/528 = 96.2% | 464/528 = 87.9% | 528/528 = 100.0% | 528/528 = 100.0% | 161/528 = 30.5% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 4/14 = 28.6% |

### Product 82

**Selected features:** selected = {b, c, d, t, up, us, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 9/25 = 36.0% |
| ActionExchange | 600 | 0/600 = 0.0% | 505/600 = 84.2% | 552/600 = 92.0% | 600/600 = 100.0% | 600/600 = 100.0% | 224/600 = 37.3% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 6/17 = 35.3% |

### Product 83

**Selected features:** selected = {b, cd, cw, d, dl, o, t, us, w}

**Repaired FTS:** 18 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 26/29 = 89.7% | 23/29 = 79.3% | 29/29 = 100.0% | 29/29 = 100.0% | 12/29 = 41.4% |
| ActionExchange | 754 | 0/754 = 0.0% | 678/754 = 89.9% | 602/754 = 79.8% | 754/754 = 100.0% | 754/754 = 100.0% | 318/754 = 42.2% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |

### Product 84

**Selected features:** selected = {b, d, i, ie, t, tl, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 19/22 = 86.4% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 462 | 0/462 = 0.0% | 379/462 = 82.0% | 400/462 = 86.6% | 462/462 = 100.0% | 462/462 = 100.0% | 298/462 = 64.5% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 11/15 = 73.3% |

### Product 85

**Selected features:** selected = {b, c, d, i, ie, t, us, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 12/24 = 50.0% |
| ActionExchange | 552 | 0/552 = 0.0% | 461/552 = 83.5% | 484/552 = 87.7% | 552/552 = 100.0% | 552/552 = 100.0% | 282/552 = 51.1% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 9/16 = 56.3% |

### Product 86

**Selected features:** selected = {b, c, cd, cw, d, i, us, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |
| ActionExchange | 323 | 0/323 = 0.0% | 308/323 = 95.4% | 258/323 = 79.9% | 323/323 = 100.0% | 323/323 = 100.0% | 158/323 = 48.9% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 6/11 = 54.5% |

### Product 87

**Selected features:** selected = {b, c, cw, d, dl, i, ie, t, tl, up, w}

**Repaired FTS:** 22 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 30/34 = 88.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 17/34 = 50.0% |
| ActionExchange | 1088 | 0/1088 = 0.0% | 961/1088 = 88.3% | 931/1088 = 85.6% | 1088/1088 = 100.0% | 1088/1088 = 100.0% | 555/1088 = 51.0% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |

### Product 88

**Selected features:** selected = {b, cw, d, dl, i, ie, o, us, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 12/23 = 52.2% |
| ActionExchange | 483 | 0/483 = 0.0% | 400/483 = 82.8% | 382/483 = 79.1% | 483/483 = 100.0% | 483/483 = 100.0% | 257/483 = 53.2% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 8/13 = 61.5% |

### Product 89

**Selected features:** selected = {b, d, t, up, us, w}

**Repaired FTS:** 17 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 13/23 = 56.5% |
| ActionExchange | 506 | 0/506 = 0.0% | 419/506 = 82.8% | 462/506 = 91.3% | 506/506 = 100.0% | 506/506 = 100.0% | 290/506 = 57.3% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |

### Product 90

**Selected features:** selected = {b, c, d, eu, up, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 16/18 = 88.9% |
| ActionExchange | 306 | 0/306 = 0.0% | 256/306 = 83.7% | 289/306 = 94.4% | 306/306 = 100.0% | 306/306 = 100.0% | 273/306 = 89.2% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 11/12 = 91.7% |

### Product 91

**Selected features:** selected = {b, c, cw, d, dl, i, ie, t, up, us, w}

**Repaired FTS:** 22 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 30/34 = 88.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 15/34 = 44.1% |
| ActionExchange | 1088 | 0/1088 = 0.0% | 961/1088 = 88.3% | 931/1088 = 85.6% | 1088/1088 = 100.0% | 1088/1088 = 100.0% | 494/1088 = 45.4% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 11/21 = 52.4% |

### Product 92

**Selected features:** selected = {b, c, cd, d, eu, t, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 15/28 = 53.6% |
| ActionExchange | 728 | 0/728 = 0.0% | 678/728 = 93.1% | 676/728 = 92.9% | 728/728 = 100.0% | 728/728 = 100.0% | 395/728 = 54.3% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |

### Product 93

**Selected features:** selected = {b, cw, d, dl, eu, o, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 4/19 = 21.1% |
| ActionExchange | 323 | 0/323 = 0.0% | 256/323 = 79.3% | 258/323 = 79.9% | 323/323 = 100.0% | 323/323 = 100.0% | 72/323 = 22.3% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 2/11 = 18.2% |

### Product 94

**Selected features:** selected = {b, cd, d, i, ie, t, us, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 17/25 = 68.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 531/575 = 92.3% | 485/575 = 84.3% | 575/575 = 100.0% | 575/575 = 100.0% | 397/575 = 69.0% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 13/16 = 81.3% |

### Product 95

**Selected features:** selected = {b, cw, d, i, ie, us, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |
| ActionExchange | 240 | 0/240 = 0.0% | 196/240 = 81.7% | 197/240 = 82.1% | 240/240 = 100.0% | 240/240 = 100.0% | 167/240 = 69.6% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 96

**Selected features:** selected = {b, c, cw, d, dl, eu, i, ie, t, up, w}

**Repaired FTS:** 22 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 30/34 = 88.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 10/34 = 29.4% |
| ActionExchange | 1088 | 0/1088 = 0.0% | 961/1088 = 88.3% | 931/1088 = 85.6% | 1088/1088 = 100.0% | 1088/1088 = 100.0% | 332/1088 = 30.5% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 6/21 = 28.6% |

### Product 97

**Selected features:** selected = {b, c, cd, cw, d, tl, up, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 8/22 = 36.4% |
| ActionExchange | 440 | 0/440 = 0.0% | 422/440 = 95.9% | 363/440 = 82.5% | 440/440 = 100.0% | 440/440 = 100.0% | 166/440 = 37.7% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 5/13 = 38.5% |

### Product 98

**Selected features:** selected = {b, c, cd, d, eu, i, ie, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 344/360 = 95.6% | 308/360 = 85.6% | 360/360 = 100.0% | 360/360 = 100.0% | 166/360 = 46.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 6/12 = 50.0% |

### Product 99

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, tl, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 12/23 = 52.2% |
| ActionExchange | 460 | 0/460 = 0.0% | 442/460 = 96.1% | 364/460 = 79.1% | 460/460 = 100.0% | 460/460 = 100.0% | 245/460 = 53.3% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 8/13 = 61.5% |

### Product 100

**Selected features:** selected = {b, c, cw, d, i, ie, tl, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |
| ActionExchange | 306 | 0/306 = 0.0% | 256/306 = 83.7% | 257/306 = 84.0% | 306/306 = 100.0% | 306/306 = 100.0% | 173/306 = 56.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 6/11 = 54.5% |

### Product 101

**Selected features:** selected = {b, c, cd, cw, d, t, tl, up, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 14/29 = 48.3% |
| ActionExchange | 783 | 0/783 = 0.0% | 731/783 = 93.4% | 677/783 = 86.5% | 783/783 = 100.0% | 783/783 = 100.0% | 385/783 = 49.2% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |

### Product 102

**Selected features:** selected = {b, c, d, i, up, us, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 19/20 = 95.0% | 20/20 = 100.0% | 20/20 = 100.0% | 10/20 = 50.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 324/380 = 85.3% | 361/380 = 95.0% | 380/380 = 100.0% | 380/380 = 100.0% | 197/380 = 51.8% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 7/13 = 53.8% |

### Product 103

**Selected features:** selected = {b, d, t, tl, w}

**Repaired FTS:** 14 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 14/18 = 77.8% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 15/18 = 83.3% |
| ActionExchange | 306 | 0/306 = 0.0% | 239/306 = 78.1% | 272/306 = 88.9% | 306/306 = 100.0% | 306/306 = 100.0% | 256/306 = 83.7% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 104

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, o, t, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 30/35 = 85.7% | 29/35 = 82.9% | 35/35 = 100.0% | 35/35 = 100.0% | 17/35 = 48.6% |
| ActionExchange | 1155 | 0/1155 = 0.0% | 991/1155 = 85.8% | 961/1155 = 83.2% | 1155/1155 = 100.0% | 1155/1155 = 100.0% | 574/1155 = 49.7% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |

### Product 105

**Selected features:** selected = {b, d, i, t, tl, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 14/20 = 70.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 305/380 = 80.3% | 342/380 = 90.0% | 380/380 = 100.0% | 380/380 = 100.0% | 270/380 = 71.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 106

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 10/23 = 43.5% |
| ActionExchange | 460 | 0/460 = 0.0% | 442/460 = 96.1% | 364/460 = 79.1% | 460/460 = 100.0% | 460/460 = 100.0% | 205/460 = 44.6% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 6/13 = 46.2% |

### Product 107

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |
| ActionExchange | 378 | 0/378 = 0.0% | 362/378 = 95.8% | 309/378 = 81.7% | 378/378 = 100.0% | 378/378 = 100.0% | 219/378 = 57.9% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 108

**Selected features:** selected = {b, c, d, i, t, up, us, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 25/27 = 92.6% | 27/27 = 100.0% | 27/27 = 100.0% | 16/27 = 59.3% |
| ActionExchange | 702 | 0/702 = 0.0% | 599/702 = 85.3% | 650/702 = 92.6% | 702/702 = 100.0% | 702/702 = 100.0% | 425/702 = 60.5% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 109

**Selected features:** selected = {b, cd, cw, d, i, up, us, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 15/22 = 68.2% |
| ActionExchange | 440 | 0/440 = 0.0% | 422/440 = 95.9% | 363/440 = 82.5% | 440/440 = 100.0% | 440/440 = 100.0% | 304/440 = 69.1% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 110

**Selected features:** selected = {b, cw, d, i, tl, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 13/19 = 68.4% |
| ActionExchange | 342 | 0/342 = 0.0% | 289/342 = 84.5% | 307/342 = 89.8% | 342/342 = 100.0% | 342/342 = 100.0% | 238/342 = 69.6% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 111

**Selected features:** selected = {b, cw, d, dl, i, ie, t, us, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 22/27 = 81.5% | 27/27 = 100.0% | 27/27 = 100.0% | 12/27 = 44.4% |
| ActionExchange | 675 | 0/675 = 0.0% | 576/675 = 85.3% | 553/675 = 81.9% | 675/675 = 100.0% | 675/675 = 100.0% | 306/675 = 45.3% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |

### Product 112

**Selected features:** selected = {b, c, d, eu, i, up, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 19/20 = 95.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 324/380 = 85.3% | 361/380 = 95.0% | 380/380 = 100.0% | 380/380 = 100.0% | 179/380 = 47.1% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 6/13 = 46.2% |

### Product 113

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, t, us, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 13/30 = 43.3% |
| ActionExchange | 810 | 0/810 = 0.0% | 758/810 = 93.6% | 652/810 = 80.5% | 810/810 = 100.0% | 810/810 = 100.0% | 357/810 = 44.1% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |

### Product 114

**Selected features:** selected = {b, cw, d, dl, eu, o, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 9/24 = 37.5% |
| ActionExchange | 528 | 0/528 = 0.0% | 441/528 = 83.5% | 443/528 = 83.9% | 528/528 = 100.0% | 528/528 = 100.0% | 205/528 = 38.8% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 6/14 = 42.9% |

### Product 115

**Selected features:** selected = {b, c, cd, cw, d, eu, t, up, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 15/29 = 51.7% |
| ActionExchange | 783 | 0/783 = 0.0% | 731/783 = 93.4% | 677/783 = 86.5% | 783/783 = 100.0% | 783/783 = 100.0% | 413/783 = 52.7% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |

### Product 116

**Selected features:** selected = {b, cw, d, tl, w}

**Repaired FTS:** 9 states, 12 transitions (12 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 20 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 9/12 = 75.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |
| ActionExchange | 132 | 0/132 = 0.0% | 100/132 = 75.8% | 111/132 = 84.1% | 132/132 = 100.0% | 132/132 = 100.0% | 110/132 = 83.3% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 7/8 = 87.5% |

### Product 117

**Selected features:** selected = {b, c, cw, d, i, ie, us, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |
| ActionExchange | 306 | 0/306 = 0.0% | 256/306 = 83.7% | 257/306 = 84.0% | 306/306 = 100.0% | 306/306 = 100.0% | 157/306 = 51.3% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 6/11 = 54.5% |

### Product 118

**Selected features:** selected = {b, cd, cw, d, dl, i, t, us, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 12/28 = 42.9% |
| ActionExchange | 700 | 0/700 = 0.0% | 652/700 = 93.1% | 578/700 = 82.6% | 700/700 = 100.0% | 700/700 = 100.0% | 306/700 = 43.7% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |

### Product 119

**Selected features:** selected = {b, d, eu, i, ie, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 6/15 = 40.0% |
| ActionExchange | 210 | 0/210 = 0.0% | 169/210 = 80.5% | 183/210 = 87.1% | 210/210 = 100.0% | 210/210 = 100.0% | 86/210 = 41.0% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 4/10 = 40.0% |

### Product 120

**Selected features:** selected = {b, cw, d, dl, i, o, tl, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 9/26 = 34.6% |
| ActionExchange | 624 | 0/624 = 0.0% | 529/624 = 84.8% | 531/624 = 85.1% | 624/624 = 100.0% | 624/624 = 100.0% | 225/624 = 36.1% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 5/15 = 33.3% |

### Product 121

**Selected features:** selected = {b, cd, d, t, us, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 10/21 = 47.6% |
| ActionExchange | 399 | 0/399 = 0.0% | 363/399 = 91.0% | 343/399 = 86.0% | 399/399 = 100.0% | 399/399 = 100.0% | 192/399 = 48.1% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |

### Product 122

**Selected features:** selected = {b, c, d, eu, i, ie, t, up, w}

**Repaired FTS:** 20 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 26/29 = 89.7% | 29/29 = 100.0% | 29/29 = 100.0% | 14/29 = 48.3% |
| ActionExchange | 812 | 0/812 = 0.0% | 701/812 = 86.3% | 729/812 = 89.8% | 812/812 = 100.0% | 812/812 = 100.0% | 401/812 = 49.4% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |

### Product 123

**Selected features:** selected = {b, cd, d, i, tl, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 15/16 = 93.8% |
| ActionExchange | 224 | 0/224 = 0.0% | 212/224 = 94.6% | 197/224 = 87.9% | 224/224 = 100.0% | 224/224 = 100.0% | 212/224 = 94.6% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% |

### Product 124

**Selected features:** selected = {b, d, i, us, w}

**Repaired FTS:** 10 states, 13 transitions (13 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 10/13 = 76.9% | 12/13 = 92.3% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% |
| ActionExchange | 156 | 0/156 = 0.0% | 121/156 = 77.6% | 144/156 = 92.3% | 156/156 = 100.0% | 156/156 = 100.0% | 156/156 = 100.0% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |

### Product 125

**Selected features:** selected = {b, c, d, tl, up, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 306 | 0/306 = 0.0% | 256/306 = 83.7% | 289/306 = 94.4% | 306/306 = 100.0% | 306/306 = 100.0% | 224/306 = 73.2% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 126

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, us, w}

**Repaired FTS:** 15 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 20/26 = 76.9% | 26/26 = 100.0% | 26/26 = 100.0% | 13/26 = 50.0% |
| ActionExchange | 598 | 0/598 = 0.0% | 554/598 = 92.6% | 465/598 = 77.8% | 598/598 = 100.0% | 598/598 = 100.0% | 309/598 = 51.7% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |

### Product 127

**Selected features:** selected = {b, d, eu, i, ie, t, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 19/22 = 86.4% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 462 | 0/462 = 0.0% | 379/462 = 82.0% | 400/462 = 86.6% | 462/462 = 100.0% | 462/462 = 100.0% | 297/462 = 64.3% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |

### Product 128

**Selected features:** selected = {b, cd, d, eu, t, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 24/26 = 92.3% | 26/26 = 100.0% | 26/26 = 100.0% | 14/26 = 53.8% |
| ActionExchange | 624 | 0/624 = 0.0% | 578/624 = 92.6% | 576/624 = 92.3% | 624/624 = 100.0% | 624/624 = 100.0% | 340/624 = 54.5% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 10/17 = 58.8% |

### Product 129

**Selected features:** selected = {b, c, cd, cw, d, dl, tl, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 16/26 = 61.5% |
| ActionExchange | 598 | 0/598 = 0.0% | 577/598 = 96.5% | 531/598 = 88.8% | 598/598 = 100.0% | 598/598 = 100.0% | 376/598 = 62.9% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 11/15 = 73.3% |

### Product 130

**Selected features:** selected = {b, d, i, ie, up, us, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 324/380 = 85.3% | 343/380 = 90.3% | 380/380 = 100.0% | 380/380 = 100.0% | 251/380 = 66.1% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 131

**Selected features:** selected = {b, cd, cw, d, eu, i, ie, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 14/19 = 73.7% | 19/19 = 100.0% | 19/19 = 100.0% | 7/19 = 36.8% |
| ActionExchange | 323 | 0/323 = 0.0% | 308/323 = 95.4% | 242/323 = 74.9% | 323/323 = 100.0% | 323/323 = 100.0% | 122/323 = 37.8% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 5/11 = 45.5% |

### Product 132

**Selected features:** selected = {b, d, i, ie, t, us, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 19/22 = 86.4% | 22/22 = 100.0% | 22/22 = 100.0% | 17/22 = 77.3% |
| ActionExchange | 462 | 0/462 = 0.0% | 379/462 = 82.0% | 400/462 = 86.6% | 462/462 = 100.0% | 462/462 = 100.0% | 360/462 = 77.9% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 133

**Selected features:** selected = {b, c, cw, d, dl, i, t, tl, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 14/27 = 51.9% |
| ActionExchange | 675 | 0/675 = 0.0% | 576/675 = 85.3% | 577/675 = 85.5% | 675/675 = 100.0% | 675/675 = 100.0% | 359/675 = 53.2% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 10/17 = 58.8% |

### Product 134

**Selected features:** selected = {b, cw, d, i, ie, t, tl, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 15/28 = 53.6% |
| ActionExchange | 756 | 0/756 = 0.0% | 649/756 = 85.8% | 650/756 = 86.0% | 756/756 = 100.0% | 756/756 = 100.0% | 411/756 = 54.4% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |

### Product 135

**Selected features:** selected = {b, c, d, eu, i, t, up, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 25/27 = 92.6% | 27/27 = 100.0% | 27/27 = 100.0% | 15/27 = 55.6% |
| ActionExchange | 702 | 0/702 = 0.0% | 599/702 = 85.3% | 650/702 = 92.6% | 702/702 = 100.0% | 702/702 = 100.0% | 397/702 = 56.6% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |

### Product 136

**Selected features:** selected = {b, c, d, i, t, tl, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 462 | 0/462 = 0.0% | 379/462 = 82.0% | 420/462 = 90.9% | 462/462 = 100.0% | 462/462 = 100.0% | 299/462 = 64.7% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |

### Product 137

**Selected features:** selected = {b, c, cw, d, dl, tl, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 14/23 = 60.9% |
| ActionExchange | 483 | 0/483 = 0.0% | 421/483 = 87.2% | 422/483 = 87.4% | 483/483 = 100.0% | 483/483 = 100.0% | 302/483 = 62.5% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 138

**Selected features:** selected = {b, cw, d, eu, i, ie, t, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 7/28 = 25.0% |
| ActionExchange | 756 | 0/756 = 0.0% | 649/756 = 85.8% | 650/756 = 86.0% | 756/756 = 100.0% | 756/756 = 100.0% | 196/756 = 25.9% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 5/18 = 27.8% |

### Product 139

**Selected features:** selected = {b, cw, d, dl, eu, i, o, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 10/26 = 38.5% |
| ActionExchange | 624 | 0/624 = 0.0% | 529/624 = 84.8% | 531/624 = 85.1% | 624/624 = 100.0% | 624/624 = 100.0% | 248/624 = 39.7% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 6/15 = 40.0% |

### Product 140

**Selected features:** selected = {b, c, cw, d, dl, i, ie, t, tl, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 24/29 = 82.8% | 29/29 = 100.0% | 29/29 = 100.0% | 14/29 = 48.3% |
| ActionExchange | 783 | 0/783 = 0.0% | 676/783 = 86.3% | 651/783 = 83.1% | 783/783 = 100.0% | 783/783 = 100.0% | 387/783 = 49.4% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |

### Product 141

**Selected features:** selected = {b, cd, cw, d, dl, i, o, tl, up, w}

**Repaired FTS:** 17 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 9/29 = 31.0% |
| ActionExchange | 754 | 0/754 = 0.0% | 704/754 = 93.4% | 653/754 = 86.6% | 754/754 = 100.0% | 754/754 = 100.0% | 244/754 = 32.4% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 6/16 = 37.5% |

### Product 142

**Selected features:** selected = {b, cd, d, eu, i, t, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 12/23 = 52.2% |
| ActionExchange | 483 | 0/483 = 0.0% | 443/483 = 91.7% | 421/483 = 87.2% | 483/483 = 100.0% | 483/483 = 100.0% | 258/483 = 53.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 8/15 = 53.3% |

### Product 143

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, o, t, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 8/30 = 26.7% |
| ActionExchange | 840 | 0/840 = 0.0% | 701/840 = 83.5% | 676/840 = 80.5% | 840/840 = 100.0% | 840/840 = 100.0% | 233/840 = 27.7% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 5/18 = 27.8% |

### Product 144

**Selected features:** selected = {b, c, cd, d, i, ie, t, up, us, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 15/32 = 46.9% |
| ActionExchange | 960 | 0/960 = 0.0% | 902/960 = 94.0% | 871/960 = 90.7% | 960/960 = 100.0% | 960/960 = 100.0% | 459/960 = 47.8% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 11/20 = 55.0% |

### Product 145

**Selected features:** selected = {b, cw, d, eu, i, t, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 420 | 0/420 = 0.0% | 341/420 = 81.2% | 361/420 = 86.0% | 420/420 = 100.0% | 420/420 = 100.0% | 264/420 = 62.9% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 146

**Selected features:** selected = {b, cw, d, dl, i, tl, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 11/23 = 47.8% |
| ActionExchange | 483 | 0/483 = 0.0% | 421/483 = 87.2% | 422/483 = 87.4% | 483/483 = 100.0% | 483/483 = 100.0% | 238/483 = 49.3% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 147

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, tl, w}

**Repaired FTS:** 15 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 20/25 = 80.0% | 25/25 = 100.0% | 25/25 = 100.0% | 15/25 = 60.0% |
| ActionExchange | 550 | 0/550 = 0.0% | 530/550 = 96.4% | 444/550 = 80.7% | 550/550 = 100.0% | 550/550 = 100.0% | 338/550 = 61.5% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 148

**Selected features:** selected = {b, c, d, eu, t, w}

**Repaired FTS:** 15 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 16/20 = 80.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 6/20 = 30.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 305/380 = 80.3% | 342/380 = 90.0% | 380/380 = 100.0% | 380/380 = 100.0% | 118/380 = 31.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 4/14 = 28.6% |

### Product 149

**Selected features:** selected = {b, cw, d, dl, t, tl, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 10/23 = 43.5% |
| ActionExchange | 483 | 0/483 = 0.0% | 400/483 = 82.8% | 401/483 = 83.0% | 483/483 = 100.0% | 483/483 = 100.0% | 214/483 = 44.3% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 7/15 = 46.7% |

### Product 150

**Selected features:** selected = {b, cw, d, dl, i, tl, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |
| ActionExchange | 288 | 0/288 = 0.0% | 241/288 = 83.7% | 242/288 = 84.0% | 288/288 = 100.0% | 288/288 = 100.0% | 131/288 = 45.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 5/11 = 45.5% |

### Product 151

**Selected features:** selected = {b, d, i, ie, t, up, us, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 24/27 = 88.9% | 27/27 = 100.0% | 27/27 = 100.0% | 18/27 = 66.7% |
| ActionExchange | 702 | 0/702 = 0.0% | 599/702 = 85.3% | 625/702 = 89.0% | 702/702 = 100.0% | 702/702 = 100.0% | 474/702 = 67.5% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |

### Product 152

**Selected features:** selected = {b, d, up, us, w}

**Repaired FTS:** 12 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 15/16 = 93.8% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |
| ActionExchange | 240 | 0/240 = 0.0% | 196/240 = 81.7% | 225/240 = 93.8% | 240/240 = 100.0% | 240/240 = 100.0% | 167/240 = 69.6% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 153

**Selected features:** selected = {b, cw, d, dl, i, ie, t, tl, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 22/27 = 81.5% | 27/27 = 100.0% | 27/27 = 100.0% | 13/27 = 48.1% |
| ActionExchange | 675 | 0/675 = 0.0% | 576/675 = 85.3% | 553/675 = 81.9% | 675/675 = 100.0% | 675/675 = 100.0% | 332/675 = 49.2% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |

### Product 154

**Selected features:** selected = {b, c, cd, cw, d, i, up, us, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 14/24 = 58.3% |
| ActionExchange | 528 | 0/528 = 0.0% | 508/528 = 96.2% | 464/528 = 87.9% | 528/528 = 100.0% | 528/528 = 100.0% | 317/528 = 60.0% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 155

**Selected features:** selected = {b, d, i, ie, tl, up, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 10/20 = 50.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 324/380 = 85.3% | 343/380 = 90.3% | 380/380 = 100.0% | 380/380 = 100.0% | 194/380 = 51.1% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 7/13 = 53.8% |

### Product 156

**Selected features:** selected = {b, c, cd, d, eu, i, ie, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 10/25 = 40.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 554/575 = 96.3% | 530/575 = 92.2% | 575/575 = 100.0% | 575/575 = 100.0% | 237/575 = 41.2% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 6/15 = 40.0% |

### Product 157

**Selected features:** selected = {b, cd, cw, d, dl, t, up, us, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 16/31 = 51.6% |
| ActionExchange | 868 | 0/868 = 0.0% | 814/868 = 93.8% | 758/868 = 87.3% | 868/868 = 100.0% | 868/868 = 100.0% | 456/868 = 52.5% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 158

**Selected features:** selected = {b, c, cw, d, i, tl, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |
| ActionExchange | 420 | 0/420 = 0.0% | 361/420 = 86.0% | 381/420 = 90.7% | 420/420 = 100.0% | 420/420 = 100.0% | 246/420 = 58.6% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 8/13 = 61.5% |

### Product 159

**Selected features:** selected = {b, c, d, i, t, tl, up, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 25/27 = 92.6% | 27/27 = 100.0% | 27/27 = 100.0% | 13/27 = 48.1% |
| ActionExchange | 702 | 0/702 = 0.0% | 599/702 = 85.3% | 650/702 = 92.6% | 702/702 = 100.0% | 702/702 = 100.0% | 348/702 = 49.6% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |

### Product 160

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, t, us, w}

**Repaired FTS:** 20 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 30/33 = 90.9% | 26/33 = 78.8% | 33/33 = 100.0% | 33/33 = 100.0% | 12/33 = 36.4% |
| ActionExchange | 990 | 0/990 = 0.0% | 902/990 = 91.1% | 785/990 = 79.3% | 990/990 = 100.0% | 990/990 = 100.0% | 373/990 = 37.7% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 8/19 = 42.1% |

### Product 161

**Selected features:** selected = {b, cd, d, i, t, us, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 483 | 0/483 = 0.0% | 443/483 = 91.7% | 421/483 = 87.2% | 483/483 = 100.0% | 483/483 = 100.0% | 402/483 = 83.2% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |

### Product 162

**Selected features:** selected = {b, cd, cw, d, dl, eu, t, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 16/26 = 61.5% |
| ActionExchange | 598 | 0/598 = 0.0% | 554/598 = 92.6% | 486/598 = 81.3% | 598/598 = 100.0% | 598/598 = 100.0% | 370/598 = 61.9% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 12/16 = 75.0% |

### Product 163

**Selected features:** selected = {b, cd, cw, d, dl, o, tl, up, w}

**Repaired FTS:** 16 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 8/27 = 29.6% |
| ActionExchange | 648 | 0/648 = 0.0% | 602/648 = 92.9% | 555/648 = 85.6% | 648/648 = 100.0% | 648/648 = 100.0% | 198/648 = 30.6% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 5/15 = 33.3% |

### Product 164

**Selected features:** selected = {b, cw, d, dl, i, ie, up, us, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 13/25 = 52.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 507/575 = 88.2% | 486/575 = 84.5% | 575/575 = 100.0% | 575/575 = 100.0% | 305/575 = 53.0% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 165

**Selected features:** selected = {b, c, cw, d, i, tl, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |
| ActionExchange | 240 | 0/240 = 0.0% | 196/240 = 81.7% | 211/240 = 87.9% | 240/240 = 100.0% | 240/240 = 100.0% | 155/240 = 64.6% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 166

**Selected features:** selected = {b, cw, d, dl, o, t, tl, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 21/26 = 80.8% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 9/26 = 34.6% |
| ActionExchange | 624 | 0/624 = 0.0% | 505/624 = 80.9% | 507/624 = 81.3% | 624/624 = 100.0% | 624/624 = 100.0% | 222/624 = 35.6% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 6/16 = 37.5% |

### Product 167

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, ie, w}

**Repaired FTS:** 15 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 20/25 = 80.0% | 25/25 = 100.0% | 25/25 = 100.0% | 8/25 = 32.0% |
| ActionExchange | 550 | 0/550 = 0.0% | 530/550 = 96.4% | 444/550 = 80.7% | 550/550 = 100.0% | 550/550 = 100.0% | 185/550 = 33.6% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 5/14 = 35.7% |

### Product 168

**Selected features:** selected = {b, c, cw, d, dl, t, tl, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 17/30 = 56.7% |
| ActionExchange | 840 | 0/840 = 0.0% | 729/840 = 86.8% | 730/840 = 86.9% | 840/840 = 100.0% | 840/840 = 100.0% | 486/840 = 57.9% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 169

**Selected features:** selected = {b, c, cd, cw, d, i, ie, tl, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 16/21 = 76.2% | 21/21 = 100.0% | 21/21 = 100.0% | 14/21 = 66.7% |
| ActionExchange | 399 | 0/399 = 0.0% | 382/399 = 95.7% | 308/399 = 77.2% | 399/399 = 100.0% | 399/399 = 100.0% | 270/399 = 67.7% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 170

**Selected features:** selected = {b, c, cw, d, i, t, tl, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 14/28 = 50.0% |
| ActionExchange | 756 | 0/756 = 0.0% | 649/756 = 85.8% | 676/756 = 89.4% | 756/756 = 100.0% | 756/756 = 100.0% | 385/756 = 50.9% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |

### Product 171

**Selected features:** selected = {b, c, cd, d, eu, t, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 16/23 = 69.6% |
| ActionExchange | 483 | 0/483 = 0.0% | 443/483 = 91.7% | 421/483 = 87.2% | 483/483 = 100.0% | 483/483 = 100.0% | 338/483 = 70.0% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 11/15 = 73.3% |

### Product 172

**Selected features:** selected = {b, cd, cw, d, dl, i, o, t, tl, up, w}

**Repaired FTS:** 22 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 33/36 = 91.7% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 7/36 = 19.4% |
| ActionExchange | 1188 | 0/1188 = 0.0% | 1091/1188 = 91.8% | 1026/1188 = 86.4% | 1188/1188 = 100.0% | 1188/1188 = 100.0% | 241/1188 = 20.3% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 4/21 = 19.0% |

### Product 173

**Selected features:** selected = {b, c, cw, d, i, up, us, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 420 | 0/420 = 0.0% | 361/420 = 86.0% | 381/420 = 90.7% | 420/420 = 100.0% | 420/420 = 100.0% | 266/420 = 63.3% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 174

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, t, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 22/27 = 81.5% | 27/27 = 100.0% | 27/27 = 100.0% | 15/27 = 55.6% |
| ActionExchange | 675 | 0/675 = 0.0% | 576/675 = 85.3% | 553/675 = 81.9% | 675/675 = 100.0% | 675/675 = 100.0% | 381/675 = 56.4% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 175

**Selected features:** selected = {b, c, cd, d, i, up, us, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 22/23 = 95.7% | 23/23 = 100.0% | 23/23 = 100.0% | 15/23 = 65.2% |
| ActionExchange | 483 | 0/483 = 0.0% | 464/483 = 96.1% | 462/483 = 95.7% | 483/483 = 100.0% | 483/483 = 100.0% | 318/483 = 65.8% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 176

**Selected features:** selected = {b, c, cw, d, dl, tl, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |
| ActionExchange | 288 | 0/288 = 0.0% | 241/288 = 83.7% | 242/288 = 84.0% | 288/288 = 100.0% | 288/288 = 100.0% | 164/288 = 56.9% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |

### Product 177

**Selected features:** selected = {b, c, cw, d, dl, i, ie, us, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 9/22 = 40.9% |
| ActionExchange | 440 | 0/440 = 0.0% | 381/440 = 86.6% | 363/440 = 82.5% | 440/440 = 100.0% | 440/440 = 100.0% | 187/440 = 42.5% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 6/13 = 46.2% |

### Product 178

**Selected features:** selected = {b, c, cd, cw, d, dl, t, tl, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 14/33 = 42.4% |
| ActionExchange | 990 | 0/990 = 0.0% | 932/990 = 94.1% | 872/990 = 88.1% | 990/990 = 100.0% | 990/990 = 100.0% | 428/990 = 43.2% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 10/20 = 50.0% |

### Product 179

**Selected features:** selected = {b, cw, d, dl, eu, o, t, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 26/31 = 83.9% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 10/31 = 32.3% |
| ActionExchange | 899 | 0/899 = 0.0% | 755/899 = 84.0% | 757/899 = 84.2% | 899/899 = 100.0% | 899/899 = 100.0% | 298/899 = 33.1% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 7/19 = 36.8% |

### Product 180

**Selected features:** selected = {b, c, d, i, ie, up, us, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 11/22 = 50.0% |
| ActionExchange | 462 | 0/462 = 0.0% | 400/462 = 86.6% | 421/462 = 91.1% | 462/462 = 100.0% | 462/462 = 100.0% | 236/462 = 51.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 181

**Selected features:** selected = {b, cw, d, dl, us, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 9/16 = 56.3% |
| ActionExchange | 224 | 0/224 = 0.0% | 183/224 = 81.7% | 184/224 = 82.1% | 224/224 = 100.0% | 224/224 = 100.0% | 127/224 = 56.7% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 6/10 = 60.0% |

### Product 182

**Selected features:** selected = {b, cd, d, t, up, us, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 24/26 = 92.3% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 624 | 0/624 = 0.0% | 578/624 = 92.6% | 576/624 = 92.3% | 624/624 = 100.0% | 624/624 = 100.0% | 437/624 = 70.0% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |

### Product 183

**Selected features:** selected = {b, cd, cw, d, dl, o, tl, w}

**Repaired FTS:** 13 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 7/22 = 31.8% |
| ActionExchange | 418 | 0/418 = 0.0% | 382/418 = 91.4% | 327/418 = 78.2% | 418/418 = 100.0% | 418/418 = 100.0% | 138/418 = 33.0% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 4/12 = 33.3% |

### Product 184

**Selected features:** selected = {b, d, t, tl, up, w}

**Repaired FTS:** 17 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 16/23 = 69.6% |
| ActionExchange | 506 | 0/506 = 0.0% | 419/506 = 82.8% | 462/506 = 91.3% | 506/506 = 100.0% | 506/506 = 100.0% | 355/506 = 70.2% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |

### Product 185

**Selected features:** selected = {b, c, cd, d, i, ie, up, us, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 8/25 = 32.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 554/575 = 96.3% | 530/575 = 92.2% | 575/575 = 100.0% | 575/575 = 100.0% | 190/575 = 33.0% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 5/15 = 33.3% |

### Product 186

**Selected features:** selected = {b, cd, cw, d, up, us, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 344/360 = 95.6% | 291/360 = 80.8% | 360/360 = 100.0% | 360/360 = 100.0% | 166/360 = 46.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 6/12 = 50.0% |

### Product 187

**Selected features:** selected = {b, cd, cw, d, dl, eu, o, t, up, w}

**Repaired FTS:** 21 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 31/34 = 91.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 16/34 = 47.1% |
| ActionExchange | 1054 | 0/1054 = 0.0% | 963/1054 = 91.4% | 902/1054 = 85.6% | 1054/1054 = 100.0% | 1054/1054 = 100.0% | 505/1054 = 47.9% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 11/20 = 55.0% |

### Product 188

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, o, up, w}

**Repaired FTS:** 18 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 14/31 = 45.2% |
| ActionExchange | 868 | 0/868 = 0.0% | 814/868 = 93.8% | 732/868 = 84.3% | 868/868 = 100.0% | 868/868 = 100.0% | 404/868 = 46.5% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |

### Product 189

**Selected features:** selected = {b, cw, d, t, tl, w}

**Repaired FTS:** 14 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 16/19 = 84.2% | 19/19 = 100.0% | 19/19 = 100.0% | 8/19 = 42.1% |
| ActionExchange | 342 | 0/342 = 0.0% | 271/342 = 79.2% | 289/342 = 84.5% | 342/342 = 100.0% | 342/342 = 100.0% | 147/342 = 43.0% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 6/13 = 46.2% |

### Product 190

**Selected features:** selected = {b, c, cw, d, i, t, us, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 12/23 = 52.2% |
| ActionExchange | 506 | 0/506 = 0.0% | 419/506 = 82.8% | 441/506 = 87.2% | 506/506 = 100.0% | 506/506 = 100.0% | 270/506 = 53.4% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 191

**Selected features:** selected = {b, cd, cw, d, dl, eu, o, up, w}

**Repaired FTS:** 16 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 10/27 = 37.0% |
| ActionExchange | 648 | 0/648 = 0.0% | 602/648 = 92.9% | 555/648 = 85.6% | 648/648 = 100.0% | 648/648 = 100.0% | 248/648 = 38.3% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 7/15 = 46.7% |

### Product 192

**Selected features:** selected = {b, c, cd, cw, d, dl, us, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |
| ActionExchange | 378 | 0/378 = 0.0% | 362/378 = 95.8% | 309/378 = 81.7% | 378/378 = 100.0% | 378/378 = 100.0% | 219/378 = 57.9% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 193

**Selected features:** selected = {b, d, eu, t, up, w}

**Repaired FTS:** 17 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 9/23 = 39.1% |
| ActionExchange | 506 | 0/506 = 0.0% | 419/506 = 82.8% | 462/506 = 91.3% | 506/506 = 100.0% | 506/506 = 100.0% | 202/506 = 39.9% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 6/16 = 37.5% |

### Product 194

**Selected features:** selected = {b, cd, cw, d, dl, o, up, us, w}

**Repaired FTS:** 16 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 9/27 = 33.3% |
| ActionExchange | 648 | 0/648 = 0.0% | 602/648 = 92.9% | 555/648 = 85.6% | 648/648 = 100.0% | 648/648 = 100.0% | 223/648 = 34.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 6/15 = 40.0% |

### Product 195

**Selected features:** selected = {b, cw, d, dl, eu, o, t, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 21/26 = 80.8% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 15/26 = 57.7% |
| ActionExchange | 624 | 0/624 = 0.0% | 505/624 = 80.9% | 507/624 = 81.3% | 624/624 = 100.0% | 624/624 = 100.0% | 366/624 = 58.7% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |

### Product 196

**Selected features:** selected = {b, cd, cw, d, dl, o, t, up, us, w}

**Repaired FTS:** 21 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 31/34 = 91.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 11/34 = 32.4% |
| ActionExchange | 1054 | 0/1054 = 0.0% | 963/1054 = 91.4% | 902/1054 = 85.6% | 1054/1054 = 100.0% | 1054/1054 = 100.0% | 349/1054 = 33.1% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 8/20 = 40.0% |

### Product 197

**Selected features:** selected = {b, cd, cw, d, dl, t, tl, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 19/31 = 61.3% |
| ActionExchange | 868 | 0/868 = 0.0% | 814/868 = 93.8% | 758/868 = 87.3% | 868/868 = 100.0% | 868/868 = 100.0% | 539/868 = 62.1% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 14/19 = 73.7% |

### Product 198

**Selected features:** selected = {b, c, cd, d, t, tl, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 17/23 = 73.9% |
| ActionExchange | 483 | 0/483 = 0.0% | 443/483 = 91.7% | 421/483 = 87.2% | 483/483 = 100.0% | 483/483 = 100.0% | 360/483 = 74.5% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 199

**Selected features:** selected = {b, c, d, i, tl, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 14/15 = 93.3% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |
| ActionExchange | 210 | 0/210 = 0.0% | 169/210 = 80.5% | 196/210 = 93.3% | 210/210 = 100.0% | 210/210 = 100.0% | 144/210 = 68.6% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 200

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, us, w}

**Repaired FTS:** 15 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 20/25 = 80.0% | 25/25 = 100.0% | 25/25 = 100.0% | 10/25 = 40.0% |
| ActionExchange | 550 | 0/550 = 0.0% | 530/550 = 96.4% | 444/550 = 80.7% | 550/550 = 100.0% | 550/550 = 100.0% | 224/550 = 40.7% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 6/14 = 42.9% |

### Product 201

**Selected features:** selected = {b, d, eu, t, w}

**Repaired FTS:** 14 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 14/18 = 77.8% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 14/18 = 77.8% |
| ActionExchange | 306 | 0/306 = 0.0% | 239/306 = 78.1% | 272/306 = 88.9% | 306/306 = 100.0% | 306/306 = 100.0% | 239/306 = 78.1% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 202

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, t, tl, w}

**Repaired FTS:** 20 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 26/32 = 81.3% | 32/32 = 100.0% | 32/32 = 100.0% | 11/32 = 34.4% |
| ActionExchange | 928 | 0/928 = 0.0% | 872/928 = 94.0% | 758/928 = 81.7% | 928/928 = 100.0% | 928/928 = 100.0% | 331/928 = 35.7% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 7/19 = 36.8% |

### Product 203

**Selected features:** selected = {b, cd, cw, d, dl, tl, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |
| ActionExchange | 304 | 0/304 = 0.0% | 290/304 = 95.4% | 243/304 = 79.9% | 304/304 = 100.0% | 304/304 = 100.0% | 146/304 = 48.0% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 6/11 = 54.5% |

### Product 204

**Selected features:** selected = {b, d, eu, i, up, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |
| ActionExchange | 306 | 0/306 = 0.0% | 256/306 = 83.7% | 289/306 = 94.4% | 306/306 = 100.0% | 306/306 = 100.0% | 191/306 = 62.4% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 7/12 = 58.3% |

### Product 205

**Selected features:** selected = {b, cw, d, dl, i, ie, o, t, tl, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 30/35 = 85.7% | 29/35 = 82.9% | 35/35 = 100.0% | 35/35 = 100.0% | 14/35 = 40.0% |
| ActionExchange | 1155 | 0/1155 = 0.0% | 991/1155 = 85.8% | 961/1155 = 83.2% | 1155/1155 = 100.0% | 1155/1155 = 100.0% | 474/1155 = 41.0% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 10/21 = 47.6% |

### Product 206

**Selected features:** selected = {b, cd, cw, d, dl, i, t, tl, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 8/28 = 28.6% |
| ActionExchange | 700 | 0/700 = 0.0% | 652/700 = 93.1% | 578/700 = 82.6% | 700/700 = 100.0% | 700/700 = 100.0% | 206/700 = 29.4% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 5/17 = 29.4% |

### Product 207

**Selected features:** selected = {b, c, cd, d, t, tl, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 16/28 = 57.1% |
| ActionExchange | 728 | 0/728 = 0.0% | 678/728 = 93.1% | 676/728 = 92.9% | 728/728 = 100.0% | 728/728 = 100.0% | 424/728 = 58.2% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 208

**Selected features:** selected = {b, c, cw, d, i, ie, t, tl, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 18/30 = 60.0% |
| ActionExchange | 870 | 0/870 = 0.0% | 755/870 = 86.8% | 756/870 = 86.9% | 870/870 = 100.0% | 870/870 = 100.0% | 530/870 = 60.9% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 13/19 = 68.4% |

### Product 209

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, o, t, w}

**Repaired FTS:** 20 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 30/33 = 90.9% | 26/33 = 78.8% | 33/33 = 100.0% | 33/33 = 100.0% | 16/33 = 48.5% |
| ActionExchange | 990 | 0/990 = 0.0% | 902/990 = 91.1% | 785/990 = 79.3% | 990/990 = 100.0% | 990/990 = 100.0% | 491/990 = 49.6% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 11/19 = 57.9% |

### Product 210

**Selected features:** selected = {b, c, cw, d, dl, i, tl, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 8/20 = 40.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 307/360 = 85.3% | 308/360 = 85.6% | 360/360 = 100.0% | 360/360 = 100.0% | 151/360 = 41.9% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 6/12 = 50.0% |

### Product 211

**Selected features:** selected = {b, d, eu, i, ie, up, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 11/20 = 55.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 324/380 = 85.3% | 343/380 = 90.3% | 380/380 = 100.0% | 380/380 = 100.0% | 213/380 = 56.1% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 7/13 = 53.8% |

### Product 212

**Selected features:** selected = {b, cw, d, dl, i, o, t, tl, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 23/28 = 82.1% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 12/28 = 42.9% |
| ActionExchange | 728 | 0/728 = 0.0% | 599/728 = 82.3% | 601/728 = 82.6% | 728/728 = 100.0% | 728/728 = 100.0% | 321/728 = 44.1% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 8/17 = 47.1% |

### Product 213

**Selected features:** selected = {b, cd, cw, d, dl, eu, t, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 15/31 = 48.4% |
| ActionExchange | 868 | 0/868 = 0.0% | 814/868 = 93.8% | 758/868 = 87.3% | 868/868 = 100.0% | 868/868 = 100.0% | 428/868 = 49.3% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 11/19 = 57.9% |

### Product 214

**Selected features:** selected = {b, cd, d, i, ie, t, tl, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 11/25 = 44.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 531/575 = 92.3% | 485/575 = 84.3% | 575/575 = 100.0% | 575/575 = 100.0% | 259/575 = 45.0% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 7/16 = 43.8% |

### Product 215

**Selected features:** selected = {b, cw, d, dl, i, ie, o, t, us, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 12/30 = 40.0% |
| ActionExchange | 840 | 0/840 = 0.0% | 701/840 = 83.5% | 676/840 = 80.5% | 840/840 = 100.0% | 840/840 = 100.0% | 345/840 = 41.1% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |

### Product 216

**Selected features:** selected = {b, d, eu, i, w}

**Repaired FTS:** 10 states, 13 transitions (13 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 13 | 0/13 = 0.0% | 10/13 = 76.9% | 12/13 = 92.3% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |
| ActionExchange | 156 | 0/156 = 0.0% | 121/156 = 77.6% | 144/156 = 92.3% | 156/156 = 100.0% | 156/156 = 100.0% | 134/156 = 85.9% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 8/9 = 88.9% |

### Product 217

**Selected features:** selected = {b, c, cd, cw, d, eu, i, ie, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 16/21 = 76.2% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 399 | 0/399 = 0.0% | 382/399 = 95.7% | 308/399 = 77.2% | 399/399 = 100.0% | 399/399 = 100.0% | 249/399 = 62.4% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 218

**Selected features:** selected = {b, cw, d, dl, i, o, t, us, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 23/28 = 82.1% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 6/28 = 21.4% |
| ActionExchange | 728 | 0/728 = 0.0% | 599/728 = 82.3% | 601/728 = 82.6% | 728/728 = 100.0% | 728/728 = 100.0% | 165/728 = 22.7% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 3/17 = 17.6% |

### Product 219

**Selected features:** selected = {b, c, cd, d, up, us, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 11/21 = 52.4% |
| ActionExchange | 399 | 0/399 = 0.0% | 382/399 = 95.7% | 380/399 = 95.2% | 399/399 = 100.0% | 399/399 = 100.0% | 215/399 = 53.9% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 8/13 = 61.5% |

### Product 220

**Selected features:** selected = {b, c, cd, cw, d, t, us, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 12/24 = 50.0% |
| ActionExchange | 528 | 0/528 = 0.0% | 486/528 = 92.0% | 442/528 = 83.7% | 528/528 = 100.0% | 528/528 = 100.0% | 267/528 = 50.6% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 221

**Selected features:** selected = {b, cw, d, dl, i, t, tl, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 17/30 = 56.7% |
| ActionExchange | 840 | 0/840 = 0.0% | 729/840 = 86.8% | 730/840 = 86.9% | 840/840 = 100.0% | 840/840 = 100.0% | 483/840 = 57.5% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |

### Product 222

**Selected features:** selected = {b, cd, d, eu, i, ie, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |
| ActionExchange | 288 | 0/288 = 0.0% | 274/288 = 95.1% | 242/288 = 84.0% | 288/288 = 100.0% | 288/288 = 100.0% | 146/288 = 50.7% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 6/11 = 54.5% |

### Product 223

**Selected features:** selected = {b, cw, d, dl, i, us, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |
| ActionExchange | 288 | 0/288 = 0.0% | 241/288 = 83.7% | 242/288 = 84.0% | 288/288 = 100.0% | 288/288 = 100.0% | 131/288 = 45.5% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 5/11 = 45.5% |

### Product 224

**Selected features:** selected = {b, d, i, ie, us, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 11/15 = 73.3% |
| ActionExchange | 210 | 0/210 = 0.0% | 169/210 = 80.5% | 183/210 = 87.1% | 210/210 = 100.0% | 210/210 = 100.0% | 156/210 = 74.3% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 225

**Selected features:** selected = {b, cd, cw, d, us, w}

**Repaired FTS:** 10 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 14/15 = 93.3% | 11/15 = 73.3% | 15/15 = 100.0% | 15/15 = 100.0% | 7/15 = 46.7% |
| ActionExchange | 195 | 0/195 = 0.0% | 184/195 = 94.4% | 146/195 = 74.9% | 195/195 = 100.0% | 195/195 = 100.0% | 93/195 = 47.7% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 5/9 = 55.6% |

### Product 226

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, t, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 13/33 = 39.4% |
| ActionExchange | 990 | 0/990 = 0.0% | 932/990 = 94.1% | 872/990 = 88.1% | 990/990 = 100.0% | 990/990 = 100.0% | 402/990 = 40.6% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |

### Product 227

**Selected features:** selected = {b, c, d, i, ie, t, tl, up, w}

**Repaired FTS:** 20 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 26/29 = 89.7% | 29/29 = 100.0% | 29/29 = 100.0% | 14/29 = 48.3% |
| ActionExchange | 812 | 0/812 = 0.0% | 701/812 = 86.3% | 729/812 = 89.8% | 812/812 = 100.0% | 812/812 = 100.0% | 402/812 = 49.5% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 10/19 = 52.6% |

### Product 228

**Selected features:** selected = {b, c, cd, d, eu, i, ie, t, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 13/32 = 40.6% |
| ActionExchange | 960 | 0/960 = 0.0% | 902/960 = 94.0% | 871/960 = 90.7% | 960/960 = 100.0% | 960/960 = 100.0% | 400/960 = 41.7% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |

### Product 229

**Selected features:** selected = {b, c, d, i, ie, t, tl, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 10/24 = 41.7% |
| ActionExchange | 552 | 0/552 = 0.0% | 461/552 = 83.5% | 484/552 = 87.7% | 552/552 = 100.0% | 552/552 = 100.0% | 236/552 = 42.8% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 6/16 = 37.5% |

### Product 230

**Selected features:** selected = {b, cw, d, dl, t, tl, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 11/28 = 39.3% |
| ActionExchange | 728 | 0/728 = 0.0% | 625/728 = 85.9% | 626/728 = 86.0% | 728/728 = 100.0% | 728/728 = 100.0% | 294/728 = 40.4% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |

### Product 231

**Selected features:** selected = {b, c, cw, d, i, us, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |
| ActionExchange | 240 | 0/240 = 0.0% | 196/240 = 81.7% | 211/240 = 87.9% | 240/240 = 100.0% | 240/240 = 100.0% | 169/240 = 70.4% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 232

**Selected features:** selected = {b, c, cd, d, i, t, us, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 18/25 = 72.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 531/575 = 92.3% | 507/575 = 88.2% | 575/575 = 100.0% | 575/575 = 100.0% | 421/575 = 73.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 13/16 = 81.3% |

### Product 233

**Selected features:** selected = {b, c, cw, d, dl, i, us, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 7/20 = 35.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 307/360 = 85.3% | 308/360 = 85.6% | 360/360 = 100.0% | 360/360 = 100.0% | 133/360 = 36.9% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 4/12 = 33.3% |

### Product 234

**Selected features:** selected = {b, c, cw, d, i, ie, up, us, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 15/23 = 65.2% |
| ActionExchange | 506 | 0/506 = 0.0% | 441/506 = 87.2% | 442/506 = 87.4% | 506/506 = 100.0% | 506/506 = 100.0% | 335/506 = 66.2% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 235

**Selected features:** selected = {b, c, cw, d, t, us, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 8/21 = 38.1% |
| ActionExchange | 420 | 0/420 = 0.0% | 341/420 = 81.2% | 361/420 = 86.0% | 420/420 = 100.0% | 420/420 = 100.0% | 165/420 = 39.3% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 6/14 = 42.9% |

### Product 236

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, tl, up, w}

**Repaired FTS:** 18 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 29/30 = 96.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 12/30 = 40.0% |
| ActionExchange | 810 | 0/810 = 0.0% | 785/810 = 96.9% | 705/810 = 87.0% | 810/810 = 100.0% | 810/810 = 100.0% | 334/810 = 41.2% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 8/17 = 47.1% |

### Product 237

**Selected features:** selected = {b, c, d, i, t, us, w}

**Repaired FTS:** 16 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 18/22 = 81.8% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 12/22 = 54.5% |
| ActionExchange | 462 | 0/462 = 0.0% | 379/462 = 82.0% | 420/462 = 90.9% | 462/462 = 100.0% | 462/462 = 100.0% | 258/462 = 55.8% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 238

**Selected features:** selected = {b, d, i, up, us, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |
| ActionExchange | 306 | 0/306 = 0.0% | 256/306 = 83.7% | 289/306 = 94.4% | 306/306 = 100.0% | 306/306 = 100.0% | 174/306 = 56.9% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 7/12 = 58.3% |

### Product 239

**Selected features:** selected = {b, cd, d, i, ie, t, up, us, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 13/30 = 43.3% |
| ActionExchange | 840 | 0/840 = 0.0% | 786/840 = 93.6% | 757/840 = 90.1% | 840/840 = 100.0% | 840/840 = 100.0% | 372/840 = 44.3% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |

### Product 240

**Selected features:** selected = {b, cd, cw, d, i, ie, t, up, us, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 18/31 = 58.1% |
| ActionExchange | 899 | 0/899 = 0.0% | 843/899 = 93.8% | 757/899 = 84.2% | 899/899 = 100.0% | 899/899 = 100.0% | 530/899 = 59.0% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 14/19 = 73.7% |

### Product 241

**Selected features:** selected = {b, cd, cw, d, tl, w}

**Repaired FTS:** 10 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 14/15 = 93.3% | 11/15 = 73.3% | 15/15 = 100.0% | 15/15 = 100.0% | 8/15 = 53.3% |
| ActionExchange | 195 | 0/195 = 0.0% | 184/195 = 94.4% | 146/195 = 74.9% | 195/195 = 100.0% | 195/195 = 100.0% | 107/195 = 54.9% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 5/9 = 55.6% |

### Product 242

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, t, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 31/35 = 88.6% | 35/35 = 100.0% | 35/35 = 100.0% | 12/35 = 34.3% |
| ActionExchange | 1120 | 0/1120 = 0.0% | 1058/1120 = 94.5% | 994/1120 = 88.8% | 1120/1120 = 100.0% | 1120/1120 = 100.0% | 399/1120 = 35.6% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 8/21 = 38.1% |

### Product 243

**Selected features:** selected = {b, cd, d, tl, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 18/19 = 94.7% | 19/19 = 100.0% | 19/19 = 100.0% | 7/19 = 36.8% |
| ActionExchange | 323 | 0/323 = 0.0% | 308/323 = 95.4% | 306/323 = 94.7% | 323/323 = 100.0% | 323/323 = 100.0% | 122/323 = 37.8% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 5/12 = 41.7% |

### Product 244

**Selected features:** selected = {b, cd, cw, d, dl, o, t, tl, up, w}

**Repaired FTS:** 21 states, 34 transitions (34 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 34 | 0/34 = 0.0% | 31/34 = 91.2% | 29/34 = 85.3% | 34/34 = 100.0% | 34/34 = 100.0% | 18/34 = 52.9% |
| ActionExchange | 1054 | 0/1054 = 0.0% | 963/1054 = 91.4% | 902/1054 = 85.6% | 1054/1054 = 100.0% | 1054/1054 = 100.0% | 570/1054 = 54.1% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |

### Product 245

**Selected features:** selected = {b, c, d, up, us, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 6/18 = 33.3% |
| ActionExchange | 306 | 0/306 = 0.0% | 256/306 = 83.7% | 289/306 = 94.4% | 306/306 = 100.0% | 306/306 = 100.0% | 106/306 = 34.6% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 4/12 = 33.3% |

### Product 246

**Selected features:** selected = {b, c, d, i, ie, t, up, us, w}

**Repaired FTS:** 20 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 26/29 = 89.7% | 29/29 = 100.0% | 29/29 = 100.0% | 7/29 = 24.1% |
| ActionExchange | 812 | 0/812 = 0.0% | 701/812 = 86.3% | 729/812 = 89.8% | 812/812 = 100.0% | 812/812 = 100.0% | 205/812 = 25.2% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 5/19 = 26.3% |

### Product 247

**Selected features:** selected = {b, cw, d, dl, i, o, tl, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |
| ActionExchange | 399 | 0/399 = 0.0% | 324/399 = 81.2% | 326/399 = 81.7% | 399/399 = 100.0% | 399/399 = 100.0% | 233/399 = 58.4% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 248

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, ie, t, up, w}

**Repaired FTS:** 23 states, 37 transitions (37 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 37 | 0/37 = 0.0% | 35/37 = 94.6% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 11/37 = 29.7% |
| ActionExchange | 1258 | 0/1258 = 0.0% | 1192/1258 = 94.8% | 1091/1258 = 86.7% | 1258/1258 = 100.0% | 1258/1258 = 100.0% | 387/1258 = 30.8% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 7/22 = 31.8% |

### Product 249

**Selected features:** selected = {b, c, cw, d, i, ie, tl, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 12/23 = 52.2% |
| ActionExchange | 506 | 0/506 = 0.0% | 441/506 = 87.2% | 442/506 = 87.4% | 506/506 = 100.0% | 506/506 = 100.0% | 271/506 = 53.6% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |

### Product 250

**Selected features:** selected = {b, d, eu, i, ie, t, up, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 24/27 = 88.9% | 27/27 = 100.0% | 27/27 = 100.0% | 17/27 = 63.0% |
| ActionExchange | 702 | 0/702 = 0.0% | 599/702 = 85.3% | 625/702 = 89.0% | 702/702 = 100.0% | 702/702 = 100.0% | 448/702 = 63.8% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |

### Product 251

**Selected features:** selected = {b, c, cd, d, eu, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |
| ActionExchange | 224 | 0/224 = 0.0% | 212/224 = 94.6% | 197/224 = 87.9% | 224/224 = 100.0% | 224/224 = 100.0% | 142/224 = 63.4% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 252

**Selected features:** selected = {b, cd, d, eu, i, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |
| ActionExchange | 224 | 0/224 = 0.0% | 212/224 = 94.6% | 197/224 = 87.9% | 224/224 = 100.0% | 224/224 = 100.0% | 143/224 = 63.8% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 253

**Selected features:** selected = {b, c, cd, d, t, up, us, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 14/28 = 50.0% |
| ActionExchange | 728 | 0/728 = 0.0% | 678/728 = 93.1% | 676/728 = 92.9% | 728/728 = 100.0% | 728/728 = 100.0% | 371/728 = 51.0% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |

### Product 254

**Selected features:** selected = {b, cd, cw, d, dl, i, o, up, us, w}

**Repaired FTS:** 17 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 15/29 = 51.7% |
| ActionExchange | 754 | 0/754 = 0.0% | 704/754 = 93.4% | 653/754 = 86.6% | 754/754 = 100.0% | 754/754 = 100.0% | 398/754 = 52.8% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |

### Product 255

**Selected features:** selected = {b, c, cd, cw, d, dl, i, t, up, us, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 31/35 = 88.6% | 35/35 = 100.0% | 35/35 = 100.0% | 8/35 = 22.9% |
| ActionExchange | 1120 | 0/1120 = 0.0% | 1058/1120 = 94.5% | 994/1120 = 88.8% | 1120/1120 = 100.0% | 1120/1120 = 100.0% | 268/1120 = 23.9% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 5/21 = 23.8% |

### Product 256

**Selected features:** selected = {b, c, d, eu, t, up, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 16/25 = 64.0% |
| ActionExchange | 600 | 0/600 = 0.0% | 505/600 = 84.2% | 552/600 = 92.0% | 600/600 = 100.0% | 600/600 = 100.0% | 389/600 = 64.8% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 12/17 = 70.6% |

### Product 257

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, t, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 12/30 = 40.0% |
| ActionExchange | 810 | 0/810 = 0.0% | 758/810 = 93.6% | 652/810 = 80.5% | 810/810 = 100.0% | 810/810 = 100.0% | 334/810 = 41.2% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |

### Product 258

**Selected features:** selected = {b, c, cw, d, eu, i, t, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 16/23 = 69.6% |
| ActionExchange | 506 | 0/506 = 0.0% | 419/506 = 82.8% | 441/506 = 87.2% | 506/506 = 100.0% | 506/506 = 100.0% | 358/506 = 70.8% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 259

**Selected features:** selected = {b, cw, d, i, ie, t, up, us, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 14/28 = 50.0% |
| ActionExchange | 756 | 0/756 = 0.0% | 649/756 = 85.8% | 650/756 = 86.0% | 756/756 = 100.0% | 756/756 = 100.0% | 385/756 = 50.9% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |

### Product 260

**Selected features:** selected = {b, c, cd, cw, d, dl, t, up, us, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 15/33 = 45.5% |
| ActionExchange | 990 | 0/990 = 0.0% | 932/990 = 94.1% | 872/990 = 88.1% | 990/990 = 100.0% | 990/990 = 100.0% | 459/990 = 46.4% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 11/20 = 55.0% |

### Product 261

**Selected features:** selected = {b, c, cw, d, tl, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |
| ActionExchange | 182 | 0/182 = 0.0% | 144/182 = 79.1% | 157/182 = 86.3% | 182/182 = 100.0% | 182/182 = 100.0% | 132/182 = 72.5% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 262

**Selected features:** selected = {b, c, cd, cw, d, eu, i, ie, t, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 18/28 = 64.3% |
| ActionExchange | 728 | 0/728 = 0.0% | 678/728 = 93.1% | 601/728 = 82.6% | 728/728 = 100.0% | 728/728 = 100.0% | 474/728 = 65.1% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 263

**Selected features:** selected = {b, cd, d, i, up, us, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 9/21 = 42.9% |
| ActionExchange | 399 | 0/399 = 0.0% | 382/399 = 95.7% | 380/399 = 95.2% | 399/399 = 100.0% | 399/399 = 100.0% | 175/399 = 43.9% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 6/13 = 46.2% |

### Product 264

**Selected features:** selected = {b, d, t, us, w}

**Repaired FTS:** 14 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 14/18 = 77.8% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 15/18 = 83.3% |
| ActionExchange | 306 | 0/306 = 0.0% | 239/306 = 78.1% | 272/306 = 88.9% | 306/306 = 100.0% | 306/306 = 100.0% | 256/306 = 83.7% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 265

**Selected features:** selected = {b, c, cw, d, i, ie, t, up, us, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 19/30 = 63.3% |
| ActionExchange | 870 | 0/870 = 0.0% | 755/870 = 86.8% | 756/870 = 86.9% | 870/870 = 100.0% | 870/870 = 100.0% | 558/870 = 64.1% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 14/19 = 73.7% |

### Product 266

**Selected features:** selected = {b, cd, d, us, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 13/14 = 92.9% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% |
| ActionExchange | 168 | 0/168 = 0.0% | 158/168 = 94.0% | 145/168 = 86.3% | 168/168 = 100.0% | 168/168 = 100.0% | 168/168 = 100.0% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |

### Product 267

**Selected features:** selected = {b, c, cd, cw, d, i, tl, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 5/19 = 26.3% |
| ActionExchange | 323 | 0/323 = 0.0% | 308/323 = 95.4% | 258/323 = 79.9% | 323/323 = 100.0% | 323/323 = 100.0% | 91/323 = 28.2% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 3/11 = 27.3% |

### Product 268

**Selected features:** selected = {b, d, i, ie, tl, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |
| ActionExchange | 210 | 0/210 = 0.0% | 169/210 = 80.5% | 183/210 = 87.1% | 210/210 = 100.0% | 210/210 = 100.0% | 169/210 = 80.5% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 269

**Selected features:** selected = {b, c, cw, d, i, ie, t, us, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 7/25 = 28.0% |
| ActionExchange | 600 | 0/600 = 0.0% | 505/600 = 84.2% | 506/600 = 84.3% | 600/600 = 100.0% | 600/600 = 100.0% | 175/600 = 29.2% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 4/16 = 25.0% |

### Product 270

**Selected features:** selected = {b, c, cd, cw, d, dl, tl, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 8/21 = 38.1% |
| ActionExchange | 378 | 0/378 = 0.0% | 362/378 = 95.8% | 309/378 = 81.7% | 378/378 = 100.0% | 378/378 = 100.0% | 151/378 = 39.9% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 5/12 = 41.7% |

### Product 271

**Selected features:** selected = {b, cw, d, dl, i, ie, o, t, up, us, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 30/35 = 85.7% | 29/35 = 82.9% | 35/35 = 100.0% | 35/35 = 100.0% | 12/35 = 34.3% |
| ActionExchange | 1155 | 0/1155 = 0.0% | 991/1155 = 85.8% | 961/1155 = 83.2% | 1155/1155 = 100.0% | 1155/1155 = 100.0% | 409/1155 = 35.4% |
| StateMissing | 21 | 0/21 = 0.0% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 8/21 = 38.1% |

### Product 272

**Selected features:** selected = {b, cw, d, dl, o, t, tl, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 26/31 = 83.9% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 9/31 = 29.0% |
| ActionExchange | 899 | 0/899 = 0.0% | 755/899 = 84.0% | 757/899 = 84.2% | 899/899 = 100.0% | 899/899 = 100.0% | 271/899 = 30.1% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 6/19 = 31.6% |

### Product 273

**Selected features:** selected = {b, cw, d, dl, i, ie, o, t, tl, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 24/30 = 80.0% | 30/30 = 100.0% | 30/30 = 100.0% | 9/30 = 30.0% |
| ActionExchange | 840 | 0/840 = 0.0% | 701/840 = 83.5% | 676/840 = 80.5% | 840/840 = 100.0% | 840/840 = 100.0% | 262/840 = 31.2% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 6/18 = 33.3% |

### Product 274

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, up, us, w}

**Repaired FTS:** 18 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 29/30 = 96.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 13/30 = 43.3% |
| ActionExchange | 810 | 0/810 = 0.0% | 785/810 = 96.9% | 705/810 = 87.0% | 810/810 = 100.0% | 810/810 = 100.0% | 364/810 = 44.9% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 8/17 = 47.1% |

### Product 275

**Selected features:** selected = {b, c, d, eu, i, ie, t, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 16/24 = 66.7% |
| ActionExchange | 552 | 0/552 = 0.0% | 461/552 = 83.5% | 484/552 = 87.7% | 552/552 = 100.0% | 552/552 = 100.0% | 373/552 = 67.6% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 12/16 = 75.0% |

### Product 276

**Selected features:** selected = {b, cw, d, dl, i, ie, o, tl, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 9/23 = 39.1% |
| ActionExchange | 483 | 0/483 = 0.0% | 400/483 = 82.8% | 382/483 = 79.1% | 483/483 = 100.0% | 483/483 = 100.0% | 196/483 = 40.6% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 5/13 = 38.5% |

### Product 277

**Selected features:** selected = {b, c, cw, d, dl, eu, t, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 9/30 = 30.0% |
| ActionExchange | 840 | 0/840 = 0.0% | 729/840 = 86.8% | 730/840 = 86.9% | 840/840 = 100.0% | 840/840 = 100.0% | 262/840 = 31.2% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 6/19 = 31.6% |

### Product 278

**Selected features:** selected = {b, c, cw, d, i, t, up, us, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 13/28 = 46.4% |
| ActionExchange | 756 | 0/756 = 0.0% | 649/756 = 85.8% | 676/756 = 89.4% | 756/756 = 100.0% | 756/756 = 100.0% | 360/756 = 47.6% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |

### Product 279

**Selected features:** selected = {b, cd, cw, d, t, tl, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 440 | 0/440 = 0.0% | 402/440 = 91.4% | 343/440 = 78.0% | 440/440 = 100.0% | 440/440 = 100.0% | 283/440 = 64.3% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 11/14 = 78.6% |

### Product 280

**Selected features:** selected = {b, c, cd, cw, d, i, ie, us, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 16/21 = 76.2% | 21/21 = 100.0% | 21/21 = 100.0% | 6/21 = 28.6% |
| ActionExchange | 399 | 0/399 = 0.0% | 382/399 = 95.7% | 308/399 = 77.2% | 399/399 = 100.0% | 399/399 = 100.0% | 118/399 = 29.6% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 4/12 = 33.3% |

### Product 281

**Selected features:** selected = {b, c, cw, d, eu, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 11/19 = 57.9% |
| ActionExchange | 342 | 0/342 = 0.0% | 289/342 = 84.5% | 307/342 = 89.8% | 342/342 = 100.0% | 342/342 = 100.0% | 202/342 = 59.1% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 7/12 = 58.3% |

### Product 282

**Selected features:** selected = {b, c, cw, d, dl, t, tl, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 11/25 = 44.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 484/575 = 84.2% | 485/575 = 84.3% | 575/575 = 100.0% | 575/575 = 100.0% | 259/575 = 45.0% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 7/16 = 43.8% |

### Product 283

**Selected features:** selected = {b, cd, cw, d, eu, i, ie, t, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 18/26 = 69.2% |
| ActionExchange | 624 | 0/624 = 0.0% | 578/624 = 92.6% | 507/624 = 81.3% | 624/624 = 100.0% | 624/624 = 100.0% | 439/624 = 70.4% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 14/16 = 87.5% |

### Product 284

**Selected features:** selected = {b, c, cd, cw, d, i, ie, t, up, us, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 14/33 = 42.4% |
| ActionExchange | 1023 | 0/1023 = 0.0% | 963/1023 = 94.1% | 871/1023 = 85.1% | 1023/1023 = 100.0% | 1023/1023 = 100.0% | 442/1023 = 43.2% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 10/20 = 50.0% |

### Product 285

**Selected features:** selected = {b, c, cw, d, dl, i, t, tl, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 13/32 = 40.6% |
| ActionExchange | 960 | 0/960 = 0.0% | 841/960 = 87.6% | 842/960 = 87.7% | 960/960 = 100.0% | 960/960 = 100.0% | 405/960 = 42.2% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |

### Product 286

**Selected features:** selected = {b, c, cw, d, us, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |
| ActionExchange | 182 | 0/182 = 0.0% | 144/182 = 79.1% | 157/182 = 86.3% | 182/182 = 100.0% | 182/182 = 100.0% | 118/182 = 64.8% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 287

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, up, us, w}

**Repaired FTS:** 18 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 13/31 = 41.9% |
| ActionExchange | 868 | 0/868 = 0.0% | 814/868 = 93.8% | 732/868 = 84.3% | 868/868 = 100.0% | 868/868 = 100.0% | 376/868 = 43.3% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 8/17 = 47.1% |

### Product 288

**Selected features:** selected = {b, cd, d, t, tl, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 24/26 = 92.3% | 26/26 = 100.0% | 26/26 = 100.0% | 15/26 = 57.7% |
| ActionExchange | 624 | 0/624 = 0.0% | 578/624 = 92.6% | 576/624 = 92.3% | 624/624 = 100.0% | 624/624 = 100.0% | 367/624 = 58.8% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 289

**Selected features:** selected = {b, cd, cw, d, i, ie, us, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 14/19 = 73.7% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |
| ActionExchange | 323 | 0/323 = 0.0% | 308/323 = 95.4% | 242/323 = 74.9% | 323/323 = 100.0% | 323/323 = 100.0% | 157/323 = 48.6% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 5/11 = 45.5% |

### Product 290

**Selected features:** selected = {b, c, cd, cw, d, i, t, up, us, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 8/31 = 25.8% |
| ActionExchange | 899 | 0/899 = 0.0% | 843/899 = 93.8% | 785/899 = 87.3% | 899/899 = 100.0% | 899/899 = 100.0% | 241/899 = 26.8% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 5/19 = 26.3% |

### Product 291

**Selected features:** selected = {b, d, i, t, up, us, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 19/25 = 76.0% |
| ActionExchange | 600 | 0/600 = 0.0% | 505/600 = 84.2% | 552/600 = 92.0% | 600/600 = 100.0% | 600/600 = 100.0% | 461/600 = 76.8% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 14/17 = 82.4% |

### Product 292

**Selected features:** selected = {b, cw, d, dl, i, ie, us, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 14/20 = 70.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 307/360 = 85.3% | 291/360 = 80.8% | 360/360 = 100.0% | 360/360 = 100.0% | 253/360 = 70.3% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 293

**Selected features:** selected = {b, cd, d, eu, i, ie, t, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 19/30 = 63.3% |
| ActionExchange | 840 | 0/840 = 0.0% | 786/840 = 93.6% | 757/840 = 90.1% | 840/840 = 100.0% | 840/840 = 100.0% | 540/840 = 64.3% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 14/19 = 73.7% |

### Product 294

**Selected features:** selected = {b, c, cd, cw, d, i, ie, t, us, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 13/28 = 46.4% |
| ActionExchange | 728 | 0/728 = 0.0% | 678/728 = 93.1% | 601/728 = 82.6% | 728/728 = 100.0% | 728/728 = 100.0% | 345/728 = 47.4% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |

### Product 295

**Selected features:** selected = {b, c, cd, d, eu, i, t, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 13/30 = 43.3% |
| ActionExchange | 840 | 0/840 = 0.0% | 786/840 = 93.6% | 784/840 = 93.3% | 840/840 = 100.0% | 840/840 = 100.0% | 373/840 = 44.4% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |

### Product 296

**Selected features:** selected = {b, cd, cw, d, dl, o, t, tl, w}

**Repaired FTS:** 18 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 26/29 = 89.7% | 23/29 = 79.3% | 29/29 = 100.0% | 29/29 = 100.0% | 11/29 = 37.9% |
| ActionExchange | 754 | 0/754 = 0.0% | 678/754 = 89.9% | 602/754 = 79.8% | 754/754 = 100.0% | 754/754 = 100.0% | 295/754 = 39.1% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 8/17 = 47.1% |

### Product 297

**Selected features:** selected = {b, cw, d, i, ie, tl, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 7/16 = 43.8% |
| ActionExchange | 240 | 0/240 = 0.0% | 196/240 = 81.7% | 197/240 = 82.1% | 240/240 = 100.0% | 240/240 = 100.0% | 108/240 = 45.0% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 5/10 = 50.0% |

### Product 298

**Selected features:** selected = {b, cw, d, dl, i, t, up, us, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 16/30 = 53.3% |
| ActionExchange | 840 | 0/840 = 0.0% | 729/840 = 86.8% | 730/840 = 86.9% | 840/840 = 100.0% | 840/840 = 100.0% | 458/840 = 54.5% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 11/19 = 57.9% |

### Product 299

**Selected features:** selected = {b, c, cd, d, eu, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 399 | 0/399 = 0.0% | 382/399 = 95.7% | 380/399 = 95.2% | 399/399 = 100.0% | 399/399 = 100.0% | 250/399 = 62.7% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 300

**Selected features:** selected = {b, cw, d, dl, i, t, tl, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 12/25 = 48.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 484/575 = 84.2% | 485/575 = 84.3% | 575/575 = 100.0% | 575/575 = 100.0% | 281/575 = 48.9% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 8/16 = 50.0% |

### Product 301

**Selected features:** selected = {b, cd, d, i, us, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 9/16 = 56.3% |
| ActionExchange | 224 | 0/224 = 0.0% | 212/224 = 94.6% | 197/224 = 87.9% | 224/224 = 100.0% | 224/224 = 100.0% | 130/224 = 58.0% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 6/10 = 60.0% |

### Product 302

**Selected features:** selected = {b, c, d, eu, i, ie, up, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 20/22 = 90.9% | 22/22 = 100.0% | 22/22 = 100.0% | 9/22 = 40.9% |
| ActionExchange | 462 | 0/462 = 0.0% | 400/462 = 86.6% | 421/462 = 91.1% | 462/462 = 100.0% | 462/462 = 100.0% | 197/462 = 42.6% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 6/14 = 42.9% |

### Product 303

**Selected features:** selected = {b, cw, d, eu, t, w}

**Repaired FTS:** 14 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 16/19 = 84.2% | 19/19 = 100.0% | 19/19 = 100.0% | 15/19 = 78.9% |
| ActionExchange | 342 | 0/342 = 0.0% | 271/342 = 79.2% | 289/342 = 84.5% | 342/342 = 100.0% | 342/342 = 100.0% | 271/342 = 79.2% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 304

**Selected features:** selected = {b, c, cd, d, t, us, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 19/23 = 82.6% |
| ActionExchange | 483 | 0/483 = 0.0% | 443/483 = 91.7% | 421/483 = 87.2% | 483/483 = 100.0% | 483/483 = 100.0% | 401/483 = 83.0% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 14/15 = 93.3% |

### Product 305

**Selected features:** selected = {b, cd, cw, d, dl, i, o, t, tl, w}

**Repaired FTS:** 19 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 28/31 = 90.3% | 25/31 = 80.6% | 31/31 = 100.0% | 31/31 = 100.0% | 8/31 = 25.8% |
| ActionExchange | 868 | 0/868 = 0.0% | 786/868 = 90.6% | 704/868 = 81.1% | 868/868 = 100.0% | 868/868 = 100.0% | 233/868 = 26.8% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 5/18 = 27.8% |

### Product 306

**Selected features:** selected = {b, cw, d, dl, eu, t, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 14/28 = 50.0% |
| ActionExchange | 728 | 0/728 = 0.0% | 625/728 = 85.9% | 626/728 = 86.0% | 728/728 = 100.0% | 728/728 = 100.0% | 372/728 = 51.1% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |

### Product 307

**Selected features:** selected = {b, cd, cw, d, i, us, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |
| ActionExchange | 255 | 0/255 = 0.0% | 242/255 = 94.9% | 198/255 = 77.6% | 255/255 = 100.0% | 255/255 = 100.0% | 137/255 = 53.7% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 6/10 = 60.0% |

### Product 308

**Selected features:** selected = {b, c, cd, cw, d, eu, i, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |
| ActionExchange | 323 | 0/323 = 0.0% | 308/323 = 95.4% | 258/323 = 79.9% | 323/323 = 100.0% | 323/323 = 100.0% | 158/323 = 48.9% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 6/11 = 54.5% |

### Product 309

**Selected features:** selected = {b, c, cw, d, eu, i, ie, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 5/23 = 21.7% |
| ActionExchange | 506 | 0/506 = 0.0% | 441/506 = 87.2% | 442/506 = 87.4% | 506/506 = 100.0% | 506/506 = 100.0% | 117/506 = 23.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 3/14 = 21.4% |

### Product 310

**Selected features:** selected = {b, c, d, i, us, w}

**Repaired FTS:** 11 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 12/15 = 80.0% | 14/15 = 93.3% | 15/15 = 100.0% | 15/15 = 100.0% | 8/15 = 53.3% |
| ActionExchange | 210 | 0/210 = 0.0% | 169/210 = 80.5% | 196/210 = 93.3% | 210/210 = 100.0% | 210/210 = 100.0% | 115/210 = 54.8% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 5/10 = 50.0% |

### Product 311

**Selected features:** selected = {b, cd, cw, d, t, tl, up, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 11/27 = 40.7% |
| ActionExchange | 675 | 0/675 = 0.0% | 627/675 = 92.9% | 577/675 = 85.5% | 675/675 = 100.0% | 675/675 = 100.0% | 279/675 = 41.3% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 8/17 = 47.1% |

### Product 312

**Selected features:** selected = {b, c, cd, cw, d, i, t, us, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 8/26 = 30.8% |
| ActionExchange | 624 | 0/624 = 0.0% | 578/624 = 92.6% | 530/624 = 84.9% | 624/624 = 100.0% | 624/624 = 100.0% | 198/624 = 31.7% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 5/16 = 31.3% |

### Product 313

**Selected features:** selected = {b, cw, d, dl, o, tl, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 13/24 = 54.2% |
| ActionExchange | 528 | 0/528 = 0.0% | 441/528 = 83.5% | 443/528 = 83.9% | 528/528 = 100.0% | 528/528 = 100.0% | 292/528 = 55.3% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 314

**Selected features:** selected = {b, c, cd, cw, d, i, ie, tl, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 11/26 = 42.3% |
| ActionExchange | 624 | 0/624 = 0.0% | 602/624 = 96.5% | 531/624 = 85.1% | 624/624 = 100.0% | 624/624 = 100.0% | 273/624 = 43.8% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 8/15 = 53.3% |

### Product 315

**Selected features:** selected = {b, cw, d, dl, eu, i, t, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 12/30 = 40.0% |
| ActionExchange | 840 | 0/840 = 0.0% | 729/840 = 86.8% | 730/840 = 86.9% | 840/840 = 100.0% | 840/840 = 100.0% | 346/840 = 41.2% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 8/19 = 42.1% |

### Product 316

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, us, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 11/23 = 47.8% |
| ActionExchange | 460 | 0/460 = 0.0% | 442/460 = 96.1% | 364/460 = 79.1% | 460/460 = 100.0% | 460/460 = 100.0% | 222/460 = 48.3% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 7/13 = 53.8% |

### Product 317

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, t, up, us, w}

**Repaired FTS:** 23 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 35/38 = 92.1% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 11/38 = 28.9% |
| ActionExchange | 1330 | 0/1330 = 0.0% | 1227/1330 = 92.3% | 1124/1330 = 84.5% | 1330/1330 = 100.0% | 1330/1330 = 100.0% | 398/1330 = 29.9% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 7/22 = 31.8% |

### Product 318

**Selected features:** selected = {b, c, cw, d, dl, eu, i, ie, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 9/22 = 40.9% |
| ActionExchange | 440 | 0/440 = 0.0% | 381/440 = 86.6% | 363/440 = 82.5% | 440/440 = 100.0% | 440/440 = 100.0% | 187/440 = 42.5% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 6/13 = 46.2% |

### Product 319

**Selected features:** selected = {b, cd, d, eu, i, ie, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 13/23 = 56.5% |
| ActionExchange | 483 | 0/483 = 0.0% | 464/483 = 96.1% | 442/483 = 91.5% | 483/483 = 100.0% | 483/483 = 100.0% | 278/483 = 57.6% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 320

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, tl, up, w}

**Repaired FTS:** 18 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 11/31 = 35.5% |
| ActionExchange | 868 | 0/868 = 0.0% | 814/868 = 93.8% | 732/868 = 84.3% | 868/868 = 100.0% | 868/868 = 100.0% | 317/868 = 36.5% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 7/17 = 41.2% |

### Product 321

**Selected features:** selected = {b, cd, cw, d, dl, i, o, us, w}

**Repaired FTS:** 14 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 19/24 = 79.2% | 24/24 = 100.0% | 24/24 = 100.0% | 8/24 = 33.3% |
| ActionExchange | 504 | 0/504 = 0.0% | 464/504 = 92.1% | 403/504 = 80.0% | 504/504 = 100.0% | 504/504 = 100.0% | 174/504 = 34.5% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 5/13 = 38.5% |

### Product 322

**Selected features:** selected = {b, d, i, tl, up, w}

**Repaired FTS:** 13 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 17/18 = 94.4% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |
| ActionExchange | 306 | 0/306 = 0.0% | 256/306 = 83.7% | 289/306 = 94.4% | 306/306 = 100.0% | 306/306 = 100.0% | 190/306 = 62.1% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 7/12 = 58.3% |

### Product 323

**Selected features:** selected = {b, c, cw, d, dl, eu, i, t, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 4/27 = 14.8% |
| ActionExchange | 675 | 0/675 = 0.0% | 576/675 = 85.3% | 577/675 = 85.5% | 675/675 = 100.0% | 675/675 = 100.0% | 108/675 = 16.0% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 2/17 = 11.8% |

### Product 324

**Selected features:** selected = {b, cd, d, i, t, tl, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 21/23 = 91.3% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 13/23 = 56.5% |
| ActionExchange | 483 | 0/483 = 0.0% | 443/483 = 91.7% | 421/483 = 87.2% | 483/483 = 100.0% | 483/483 = 100.0% | 279/483 = 57.8% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 325

**Selected features:** selected = {b, cw, d, dl, t, up, us, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 12/28 = 42.9% |
| ActionExchange | 728 | 0/728 = 0.0% | 625/728 = 85.9% | 626/728 = 86.0% | 728/728 = 100.0% | 728/728 = 100.0% | 318/728 = 43.7% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |

### Product 326

**Selected features:** selected = {b, d, i, ie, t, tl, up, w}

**Repaired FTS:** 19 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 23/27 = 85.2% | 24/27 = 88.9% | 27/27 = 100.0% | 27/27 = 100.0% | 11/27 = 40.7% |
| ActionExchange | 702 | 0/702 = 0.0% | 599/702 = 85.3% | 625/702 = 89.0% | 702/702 = 100.0% | 702/702 = 100.0% | 293/702 = 41.7% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |

### Product 327

**Selected features:** selected = {b, c, cw, d, dl, i, up, us, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 11/25 = 44.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 507/575 = 88.2% | 508/575 = 88.3% | 575/575 = 100.0% | 575/575 = 100.0% | 262/575 = 45.6% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 7/15 = 46.7% |

### Product 328

**Selected features:** selected = {b, cd, cw, d, dl, i, o, tl, w}

**Repaired FTS:** 14 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 19/24 = 79.2% | 24/24 = 100.0% | 24/24 = 100.0% | 6/24 = 25.0% |
| ActionExchange | 504 | 0/504 = 0.0% | 464/504 = 92.1% | 403/504 = 80.0% | 504/504 = 100.0% | 504/504 = 100.0% | 132/504 = 26.2% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 4/13 = 30.8% |

### Product 329

**Selected features:** selected = {b, cd, cw, d, i, ie, tl, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 14/19 = 73.7% | 19/19 = 100.0% | 19/19 = 100.0% | 15/19 = 78.9% |
| ActionExchange | 323 | 0/323 = 0.0% | 308/323 = 95.4% | 242/323 = 74.9% | 323/323 = 100.0% | 323/323 = 100.0% | 259/323 = 80.2% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 10/11 = 90.9% |

### Product 330

**Selected features:** selected = {b, cw, d, t, us, w}

**Repaired FTS:** 14 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 15/19 = 78.9% | 16/19 = 84.2% | 19/19 = 100.0% | 19/19 = 100.0% | 14/19 = 73.7% |
| ActionExchange | 342 | 0/342 = 0.0% | 271/342 = 79.2% | 289/342 = 84.5% | 342/342 = 100.0% | 342/342 = 100.0% | 254/342 = 74.3% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 331

**Selected features:** selected = {b, cw, d, t, tl, up, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 15/24 = 62.5% |
| ActionExchange | 552 | 0/552 = 0.0% | 461/552 = 83.5% | 484/552 = 87.7% | 552/552 = 100.0% | 552/552 = 100.0% | 349/552 = 63.2% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |

### Product 332

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, t, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 12/28 = 42.9% |
| ActionExchange | 700 | 0/700 = 0.0% | 652/700 = 93.1% | 578/700 = 82.6% | 700/700 = 100.0% | 700/700 = 100.0% | 306/700 = 43.7% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |

### Product 333

**Selected features:** selected = {b, c, d, i, tl, up, w}

**Repaired FTS:** 14 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 19/20 = 95.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |
| ActionExchange | 380 | 0/380 = 0.0% | 324/380 = 85.3% | 361/380 = 95.0% | 380/380 = 100.0% | 380/380 = 100.0% | 251/380 = 66.1% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 8/13 = 61.5% |

### Product 334

**Selected features:** selected = {b, cd, d, i, ie, tl, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 21/23 = 91.3% | 23/23 = 100.0% | 23/23 = 100.0% | 11/23 = 47.8% |
| ActionExchange | 483 | 0/483 = 0.0% | 464/483 = 96.1% | 442/483 = 91.5% | 483/483 = 100.0% | 483/483 = 100.0% | 235/483 = 48.7% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 335

**Selected features:** selected = {b, c, cd, cw, d, eu, up, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 9/22 = 40.9% |
| ActionExchange | 440 | 0/440 = 0.0% | 422/440 = 95.9% | 363/440 = 82.5% | 440/440 = 100.0% | 440/440 = 100.0% | 185/440 = 42.0% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 6/13 = 46.2% |

### Product 336

**Selected features:** selected = {b, d, i, t, tl, up, w}

**Repaired FTS:** 18 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 15/25 = 60.0% |
| ActionExchange | 600 | 0/600 = 0.0% | 505/600 = 84.2% | 552/600 = 92.0% | 600/600 = 100.0% | 600/600 = 100.0% | 366/600 = 61.0% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 337

**Selected features:** selected = {b, c, cd, cw, d, i, ie, up, us, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 14/26 = 53.8% |
| ActionExchange | 624 | 0/624 = 0.0% | 602/624 = 96.5% | 531/624 = 85.1% | 624/624 = 100.0% | 624/624 = 100.0% | 344/624 = 55.1% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 338

**Selected features:** selected = {b, c, cd, cw, d, dl, i, tl, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 11/28 = 39.3% |
| ActionExchange | 700 | 0/700 = 0.0% | 677/700 = 96.7% | 627/700 = 89.6% | 700/700 = 100.0% | 700/700 = 100.0% | 288/700 = 41.1% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 7/16 = 43.8% |

### Product 339

**Selected features:** selected = {b, cw, d, dl, eu, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 10/21 = 47.6% |
| ActionExchange | 399 | 0/399 = 0.0% | 343/399 = 86.0% | 344/399 = 86.2% | 399/399 = 100.0% | 399/399 = 100.0% | 194/399 = 48.6% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 7/13 = 53.8% |

### Product 340

**Selected features:** selected = {b, cd, cw, d, dl, eu, o, w}

**Repaired FTS:** 13 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 12/22 = 54.5% |
| ActionExchange | 418 | 0/418 = 0.0% | 382/418 = 91.4% | 327/418 = 78.2% | 418/418 = 100.0% | 418/418 = 100.0% | 231/418 = 55.3% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 341

**Selected features:** selected = {b, c, cw, d, eu, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 22 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |
| ActionExchange | 182 | 0/182 = 0.0% | 144/182 = 79.1% | 157/182 = 86.3% | 182/182 = 100.0% | 182/182 = 100.0% | 120/182 = 65.9% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 342

**Selected features:** selected = {b, cd, d, i, ie, t, tl, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 27/30 = 90.0% | 30/30 = 100.0% | 30/30 = 100.0% | 13/30 = 43.3% |
| ActionExchange | 840 | 0/840 = 0.0% | 786/840 = 93.6% | 757/840 = 90.1% | 840/840 = 100.0% | 840/840 = 100.0% | 373/840 = 44.4% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |

### Product 343

**Selected features:** selected = {b, c, cd, d, tl, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 15/16 = 93.8% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |
| ActionExchange | 224 | 0/224 = 0.0% | 212/224 = 94.6% | 197/224 = 87.9% | 224/224 = 100.0% | 224/224 = 100.0% | 142/224 = 63.4% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 344

**Selected features:** selected = {b, cw, d, dl, i, ie, o, up, us, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 12/28 = 42.9% |
| ActionExchange | 728 | 0/728 = 0.0% | 625/728 = 85.9% | 602/728 = 82.7% | 728/728 = 100.0% | 728/728 = 100.0% | 323/728 = 44.4% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 8/16 = 50.0% |

### Product 345

**Selected features:** selected = {b, cw, d, dl, i, o, us, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 10/21 = 47.6% |
| ActionExchange | 399 | 0/399 = 0.0% | 324/399 = 81.2% | 326/399 = 81.7% | 399/399 = 100.0% | 399/399 = 100.0% | 194/399 = 48.6% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 6/12 = 50.0% |

### Product 346

**Selected features:** selected = {b, c, cw, d, dl, eu, i, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 4/20 = 20.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 307/360 = 85.3% | 308/360 = 85.6% | 360/360 = 100.0% | 360/360 = 100.0% | 78/360 = 21.7% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 2/12 = 16.7% |

### Product 347

**Selected features:** selected = {b, cw, d, i, t, tl, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 15/26 = 57.7% |
| ActionExchange | 650 | 0/650 = 0.0% | 551/650 = 84.8% | 576/650 = 88.6% | 650/650 = 100.0% | 650/650 = 100.0% | 382/650 = 58.8% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 348

**Selected features:** selected = {b, cd, cw, d, dl, eu, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 11/24 = 45.8% |
| ActionExchange | 504 | 0/504 = 0.0% | 485/504 = 96.2% | 443/504 = 87.9% | 504/504 = 100.0% | 504/504 = 100.0% | 234/504 = 46.4% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 349

**Selected features:** selected = {b, c, cw, d, dl, i, ie, t, us, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 24/29 = 82.8% | 29/29 = 100.0% | 29/29 = 100.0% | 9/29 = 31.0% |
| ActionExchange | 783 | 0/783 = 0.0% | 676/783 = 86.3% | 651/783 = 83.1% | 783/783 = 100.0% | 783/783 = 100.0% | 251/783 = 32.1% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 6/18 = 33.3% |

### Product 350

**Selected features:** selected = {b, cw, d, dl, i, o, up, us, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 13/26 = 50.0% |
| ActionExchange | 624 | 0/624 = 0.0% | 529/624 = 84.8% | 531/624 = 85.1% | 624/624 = 100.0% | 624/624 = 100.0% | 318/624 = 51.0% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 8/15 = 53.3% |

### Product 351

**Selected features:** selected = {b, cw, d, dl, i, o, t, tl, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 28/33 = 84.8% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 15/33 = 45.5% |
| ActionExchange | 1023 | 0/1023 = 0.0% | 869/1023 = 84.9% | 871/1023 = 85.1% | 1023/1023 = 100.0% | 1023/1023 = 100.0% | 478/1023 = 46.7% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 10/20 = 50.0% |

### Product 352

**Selected features:** selected = {b, c, cd, d, tl, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 11/21 = 52.4% |
| ActionExchange | 399 | 0/399 = 0.0% | 382/399 = 95.7% | 380/399 = 95.2% | 399/399 = 100.0% | 399/399 = 100.0% | 214/399 = 53.6% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 7/13 = 53.8% |

### Product 353

**Selected features:** selected = {b, cw, d, dl, eu, i, o, t, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 23/28 = 82.1% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 8/28 = 28.6% |
| ActionExchange | 728 | 0/728 = 0.0% | 599/728 = 82.3% | 601/728 = 82.6% | 728/728 = 100.0% | 728/728 = 100.0% | 217/728 = 29.8% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 5/17 = 29.4% |

### Product 354

**Selected features:** selected = {b, c, cd, d, i, ie, tl, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 6/20 = 30.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 344/360 = 95.6% | 308/360 = 85.6% | 360/360 = 100.0% | 360/360 = 100.0% | 114/360 = 31.7% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 4/12 = 33.3% |

### Product 355

**Selected features:** selected = {b, cd, d, i, ie, tl, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |
| ActionExchange | 288 | 0/288 = 0.0% | 274/288 = 95.1% | 242/288 = 84.0% | 288/288 = 100.0% | 288/288 = 100.0% | 178/288 = 61.8% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |

### Product 356

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 307/360 = 85.3% | 291/360 = 80.8% | 360/360 = 100.0% | 360/360 = 100.0% | 235/360 = 65.3% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 357

**Selected features:** selected = {b, c, cd, d, eu, i, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 288 | 0/288 = 0.0% | 274/288 = 95.1% | 257/288 = 89.2% | 288/288 = 100.0% | 288/288 = 100.0% | 213/288 = 74.0% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 358

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, o, t, tl, up, w}

**Repaired FTS:** 23 states, 38 transitions (38 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 38 | 0/38 = 0.0% | 35/38 = 92.1% | 32/38 = 84.2% | 38/38 = 100.0% | 38/38 = 100.0% | 12/38 = 31.6% |
| ActionExchange | 1330 | 0/1330 = 0.0% | 1227/1330 = 92.3% | 1124/1330 = 84.5% | 1330/1330 = 100.0% | 1330/1330 = 100.0% | 435/1330 = 32.7% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 8/22 = 36.4% |

### Product 359

**Selected features:** selected = {b, c, cd, d, i, ie, t, tl, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 15/27 = 55.6% |
| ActionExchange | 675 | 0/675 = 0.0% | 627/675 = 92.9% | 577/675 = 85.5% | 675/675 = 100.0% | 675/675 = 100.0% | 382/675 = 56.6% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 360

**Selected features:** selected = {b, c, cd, d, eu, i, ie, t, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 13/27 = 48.1% |
| ActionExchange | 675 | 0/675 = 0.0% | 627/675 = 92.9% | 577/675 = 85.5% | 675/675 = 100.0% | 675/675 = 100.0% | 332/675 = 49.2% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |

### Product 361

**Selected features:** selected = {b, cd, d, eu, i, ie, t, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 12/25 = 48.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 531/575 = 92.3% | 485/575 = 84.3% | 575/575 = 100.0% | 575/575 = 100.0% | 281/575 = 48.9% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 8/16 = 50.0% |

### Product 362

**Selected features:** selected = {b, c, cw, d, dl, eu, i, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 12/25 = 48.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 507/575 = 88.2% | 508/575 = 88.3% | 575/575 = 100.0% | 575/575 = 100.0% | 287/575 = 49.9% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 8/15 = 53.3% |

### Product 363

**Selected features:** selected = {b, cw, d, t, up, us, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 15/24 = 62.5% |
| ActionExchange | 552 | 0/552 = 0.0% | 461/552 = 83.5% | 484/552 = 87.7% | 552/552 = 100.0% | 552/552 = 100.0% | 350/552 = 63.4% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |

### Product 364

**Selected features:** selected = {b, c, cd, d, i, ie, us, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 17/20 = 85.0% | 20/20 = 100.0% | 20/20 = 100.0% | 11/20 = 55.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 344/360 = 95.6% | 308/360 = 85.6% | 360/360 = 100.0% | 360/360 = 100.0% | 200/360 = 55.6% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 7/12 = 58.3% |

### Product 365

**Selected features:** selected = {b, c, cw, d, dl, i, t, up, us, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 15/32 = 46.9% |
| ActionExchange | 960 | 0/960 = 0.0% | 841/960 = 87.6% | 842/960 = 87.7% | 960/960 = 100.0% | 960/960 = 100.0% | 463/960 = 48.2% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 10/20 = 50.0% |

### Product 366

**Selected features:** selected = {b, c, cd, d, i, tl, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 22/23 = 95.7% | 23/23 = 100.0% | 23/23 = 100.0% | 15/23 = 65.2% |
| ActionExchange | 483 | 0/483 = 0.0% | 464/483 = 96.1% | 462/483 = 95.7% | 483/483 = 100.0% | 483/483 = 100.0% | 324/483 = 67.1% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 367

**Selected features:** selected = {b, cd, cw, d, i, ie, t, us, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 14/26 = 53.8% |
| ActionExchange | 624 | 0/624 = 0.0% | 578/624 = 92.6% | 507/624 = 81.3% | 624/624 = 100.0% | 624/624 = 100.0% | 342/624 = 54.8% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |

### Product 368

**Selected features:** selected = {b, cw, d, dl, i, ie, t, up, us, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 27/32 = 84.4% | 32/32 = 100.0% | 32/32 = 100.0% | 13/32 = 40.6% |
| ActionExchange | 960 | 0/960 = 0.0% | 841/960 = 87.6% | 813/960 = 84.7% | 960/960 = 100.0% | 960/960 = 100.0% | 401/960 = 41.8% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |

### Product 369

**Selected features:** selected = {b, cw, d, dl, i, ie, tl, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 5/25 = 20.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 507/575 = 88.2% | 486/575 = 84.5% | 575/575 = 100.0% | 575/575 = 100.0% | 122/575 = 21.2% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 3/15 = 20.0% |

### Product 370

**Selected features:** selected = {b, cd, cw, d, dl, t, tl, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 16/26 = 61.5% |
| ActionExchange | 598 | 0/598 = 0.0% | 554/598 = 92.6% | 486/598 = 81.3% | 598/598 = 100.0% | 598/598 = 100.0% | 371/598 = 62.0% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 12/16 = 75.0% |

### Product 371

**Selected features:** selected = {b, cw, d, i, t, us, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 17/21 = 81.0% |
| ActionExchange | 420 | 0/420 = 0.0% | 341/420 = 81.2% | 361/420 = 86.0% | 420/420 = 100.0% | 420/420 = 100.0% | 344/420 = 81.9% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 13/14 = 92.9% |

### Product 372

**Selected features:** selected = {b, cw, d, eu, i, ie, t, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 15/23 = 65.2% |
| ActionExchange | 506 | 0/506 = 0.0% | 419/506 = 82.8% | 420/506 = 83.0% | 506/506 = 100.0% | 506/506 = 100.0% | 335/506 = 66.2% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 11/15 = 73.3% |

### Product 373

**Selected features:** selected = {b, c, cw, d, i, ie, t, tl, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 10/25 = 40.0% |
| ActionExchange | 600 | 0/600 = 0.0% | 505/600 = 84.2% | 506/600 = 84.3% | 600/600 = 100.0% | 600/600 = 100.0% | 248/600 = 41.3% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 7/16 = 43.8% |

### Product 374

**Selected features:** selected = {b, cd, cw, d, dl, i, t, up, us, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 14/33 = 42.4% |
| ActionExchange | 990 | 0/990 = 0.0% | 932/990 = 94.1% | 872/990 = 88.1% | 990/990 = 100.0% | 990/990 = 100.0% | 431/990 = 43.5% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |

### Product 375

**Selected features:** selected = {b, c, cd, d, i, ie, t, tl, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 45 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 29/32 = 90.6% | 32/32 = 100.0% | 32/32 = 100.0% | 13/32 = 40.6% |
| ActionExchange | 960 | 0/960 = 0.0% | 902/960 = 94.0% | 871/960 = 90.7% | 960/960 = 100.0% | 960/960 = 100.0% | 399/960 = 41.6% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |

### Product 376

**Selected features:** selected = {b, c, cd, d, i, t, tl, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 23/25 = 92.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 6/25 = 24.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 531/575 = 92.3% | 507/575 = 88.2% | 575/575 = 100.0% | 575/575 = 100.0% | 144/575 = 25.0% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 4/16 = 25.0% |

### Product 377

**Selected features:** selected = {b, c, cd, cw, d, up, us, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 13/22 = 59.1% |
| ActionExchange | 440 | 0/440 = 0.0% | 422/440 = 95.9% | 363/440 = 82.5% | 440/440 = 100.0% | 440/440 = 100.0% | 264/440 = 60.0% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 378

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 12/25 = 48.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 507/575 = 88.2% | 486/575 = 84.5% | 575/575 = 100.0% | 575/575 = 100.0% | 283/575 = 49.2% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 8/15 = 53.3% |

### Product 379

**Selected features:** selected = {b, cw, d, i, t, up, us, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 15/26 = 57.7% |
| ActionExchange | 650 | 0/650 = 0.0% | 551/650 = 84.8% | 576/650 = 88.6% | 650/650 = 100.0% | 650/650 = 100.0% | 380/650 = 58.5% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |

### Product 380

**Selected features:** selected = {b, c, cd, d, i, ie, tl, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 24/25 = 96.0% | 23/25 = 92.0% | 25/25 = 100.0% | 25/25 = 100.0% | 13/25 = 52.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 554/575 = 96.3% | 530/575 = 92.2% | 575/575 = 100.0% | 575/575 = 100.0% | 305/575 = 53.0% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 8/15 = 53.3% |

### Product 381

**Selected features:** selected = {b, c, cd, cw, d, eu, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |
| ActionExchange | 255 | 0/255 = 0.0% | 242/255 = 94.9% | 198/255 = 77.6% | 255/255 = 100.0% | 255/255 = 100.0% | 170/255 = 66.7% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 8/10 = 80.0% |

### Product 382

**Selected features:** selected = {b, cd, d, eu, i, t, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 13/28 = 46.4% |
| ActionExchange | 728 | 0/728 = 0.0% | 678/728 = 93.1% | 676/728 = 92.9% | 728/728 = 100.0% | 728/728 = 100.0% | 346/728 = 47.5% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |

### Product 383

**Selected features:** selected = {b, cw, d, dl, eu, i, o, t, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 28/33 = 84.8% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 10/33 = 30.3% |
| ActionExchange | 1023 | 0/1023 = 0.0% | 869/1023 = 84.9% | 871/1023 = 85.1% | 1023/1023 = 100.0% | 1023/1023 = 100.0% | 324/1023 = 31.7% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 6/20 = 30.0% |

### Product 384

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, t, tl, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 17/35 = 48.6% |
| ActionExchange | 1120 | 0/1120 = 0.0% | 1058/1120 = 94.5% | 963/1120 = 86.0% | 1120/1120 = 100.0% | 1120/1120 = 100.0% | 555/1120 = 49.6% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |

### Product 385

**Selected features:** selected = {b, c, cd, d, i, ie, t, us, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 15/27 = 55.6% |
| ActionExchange | 675 | 0/675 = 0.0% | 627/675 = 92.9% | 577/675 = 85.5% | 675/675 = 100.0% | 675/675 = 100.0% | 382/675 = 56.6% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 10/17 = 58.8% |

### Product 386

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, t, up, us, w}

**Repaired FTS:** 23 states, 37 transitions (37 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 37 | 0/37 = 0.0% | 35/37 = 94.6% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 9/37 = 24.3% |
| ActionExchange | 1258 | 0/1258 = 0.0% | 1192/1258 = 94.8% | 1091/1258 = 86.7% | 1258/1258 = 100.0% | 1258/1258 = 100.0% | 320/1258 = 25.4% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 5/22 = 22.7% |

### Product 387

**Selected features:** selected = {b, c, cd, d, i, t, tl, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 14/30 = 46.7% |
| ActionExchange | 840 | 0/840 = 0.0% | 786/840 = 93.6% | 784/840 = 93.3% | 840/840 = 100.0% | 840/840 = 100.0% | 404/840 = 48.1% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |

### Product 388

**Selected features:** selected = {b, c, cw, d, t, up, us, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 13/26 = 50.0% |
| ActionExchange | 650 | 0/650 = 0.0% | 551/650 = 84.8% | 576/650 = 88.6% | 650/650 = 100.0% | 650/650 = 100.0% | 331/650 = 50.9% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |

### Product 389

**Selected features:** selected = {b, c, cd, cw, d, dl, i, up, us, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 13/28 = 46.4% |
| ActionExchange | 700 | 0/700 = 0.0% | 677/700 = 96.7% | 627/700 = 89.6% | 700/700 = 100.0% | 700/700 = 100.0% | 332/700 = 47.4% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 8/16 = 50.0% |

### Product 390

**Selected features:** selected = {b, c, cw, d, up, us, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |
| ActionExchange | 342 | 0/342 = 0.0% | 289/342 = 84.5% | 307/342 = 89.8% | 342/342 = 100.0% | 342/342 = 100.0% | 166/342 = 48.5% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 6/12 = 50.0% |

### Product 391

**Selected features:** selected = {b, c, cw, d, dl, eu, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 6/18 = 33.3% |
| ActionExchange | 288 | 0/288 = 0.0% | 241/288 = 83.7% | 242/288 = 84.0% | 288/288 = 100.0% | 288/288 = 100.0% | 100/288 = 34.7% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 4/11 = 36.4% |

### Product 392

**Selected features:** selected = {b, cd, cw, d, i, t, us, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 10/24 = 41.7% |
| ActionExchange | 528 | 0/528 = 0.0% | 486/528 = 92.0% | 442/528 = 83.7% | 528/528 = 100.0% | 528/528 = 100.0% | 226/528 = 42.8% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 7/15 = 46.7% |

### Product 393

**Selected features:** selected = {b, c, cd, d, i, t, up, us, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 43 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 28/30 = 93.3% | 30/30 = 100.0% | 30/30 = 100.0% | 6/30 = 20.0% |
| ActionExchange | 840 | 0/840 = 0.0% | 786/840 = 93.6% | 784/840 = 93.3% | 840/840 = 100.0% | 840/840 = 100.0% | 176/840 = 21.0% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 4/19 = 21.1% |

### Product 394

**Selected features:** selected = {b, c, cd, cw, d, us, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 14/17 = 82.4% |
| ActionExchange | 255 | 0/255 = 0.0% | 242/255 = 94.9% | 198/255 = 77.6% | 255/255 = 100.0% | 255/255 = 100.0% | 214/255 = 83.9% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% |

### Product 395

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, t, us, w}

**Repaired FTS:** 20 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 30/32 = 93.8% | 26/32 = 81.3% | 32/32 = 100.0% | 32/32 = 100.0% | 14/32 = 43.8% |
| ActionExchange | 928 | 0/928 = 0.0% | 872/928 = 94.0% | 758/928 = 81.7% | 928/928 = 100.0% | 928/928 = 100.0% | 414/928 = 44.6% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 10/19 = 52.6% |

### Product 396

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, o, up, w}

**Repaired FTS:** 17 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 13/29 = 44.8% |
| ActionExchange | 754 | 0/754 = 0.0% | 704/754 = 93.4% | 653/754 = 86.6% | 754/754 = 100.0% | 754/754 = 100.0% | 346/754 = 45.9% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 8/16 = 50.0% |

### Product 397

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, up, us, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 9/28 = 32.1% |
| ActionExchange | 700 | 0/700 = 0.0% | 677/700 = 96.7% | 603/700 = 86.1% | 700/700 = 100.0% | 700/700 = 100.0% | 231/700 = 33.0% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 6/16 = 37.5% |

### Product 398

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, o, t, w}

**Repaired FTS:** 19 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 28/31 = 90.3% | 25/31 = 80.6% | 31/31 = 100.0% | 31/31 = 100.0% | 12/31 = 38.7% |
| ActionExchange | 868 | 0/868 = 0.0% | 786/868 = 90.6% | 704/868 = 81.1% | 868/868 = 100.0% | 868/868 = 100.0% | 345/868 = 39.7% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |

### Product 399

**Selected features:** selected = {b, cd, d, i, tl, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 20/21 = 95.2% | 21/21 = 100.0% | 21/21 = 100.0% | 9/21 = 42.9% |
| ActionExchange | 399 | 0/399 = 0.0% | 382/399 = 95.7% | 380/399 = 95.2% | 399/399 = 100.0% | 399/399 = 100.0% | 177/399 = 44.4% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 6/13 = 46.2% |

### Product 400

**Selected features:** selected = {b, c, cd, d, i, tl, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 17/18 = 94.4% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |
| ActionExchange | 288 | 0/288 = 0.0% | 274/288 = 95.1% | 257/288 = 89.2% | 288/288 = 100.0% | 288/288 = 100.0% | 166/288 = 57.6% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |

### Product 401

**Selected features:** selected = {b, cw, d, eu, i, ie, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 16/21 = 76.2% |
| ActionExchange | 420 | 0/420 = 0.0% | 361/420 = 86.0% | 362/420 = 86.2% | 420/420 = 100.0% | 420/420 = 100.0% | 322/420 = 76.7% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 11/13 = 84.6% |

### Product 402

**Selected features:** selected = {b, cd, cw, d, dl, eu, o, t, w}

**Repaired FTS:** 18 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 26/29 = 89.7% | 23/29 = 79.3% | 29/29 = 100.0% | 29/29 = 100.0% | 14/29 = 48.3% |
| ActionExchange | 754 | 0/754 = 0.0% | 678/754 = 89.9% | 602/754 = 79.8% | 754/754 = 100.0% | 754/754 = 100.0% | 368/754 = 48.8% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 10/17 = 58.8% |

### Product 403

**Selected features:** selected = {b, cw, d, eu, up, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |
| ActionExchange | 272 | 0/272 = 0.0% | 225/272 = 82.7% | 241/272 = 88.6% | 272/272 = 100.0% | 272/272 = 100.0% | 209/272 = 76.8% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 404

**Selected features:** selected = {b, cd, cw, d, eu, up, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 19/20 = 95.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 16/20 = 80.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 344/360 = 95.6% | 291/360 = 80.8% | 360/360 = 100.0% | 360/360 = 100.0% | 289/360 = 80.3% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 10/12 = 83.3% |

### Product 405

**Selected features:** selected = {b, cw, d, i, t, tl, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |
| ActionExchange | 420 | 0/420 = 0.0% | 341/420 = 81.2% | 361/420 = 86.0% | 420/420 = 100.0% | 420/420 = 100.0% | 243/420 = 57.9% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 406

**Selected features:** selected = {b, c, cw, d, dl, us, w}

**Repaired FTS:** 12 states, 18 transitions (18 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 15/18 = 83.3% | 15/18 = 83.3% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |
| ActionExchange | 288 | 0/288 = 0.0% | 241/288 = 83.7% | 242/288 = 84.0% | 288/288 = 100.0% | 288/288 = 100.0% | 212/288 = 73.6% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 407

**Selected features:** selected = {b, c, cw, d, dl, eu, i, t, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 28/32 = 87.5% | 32/32 = 100.0% | 32/32 = 100.0% | 8/32 = 25.0% |
| ActionExchange | 960 | 0/960 = 0.0% | 841/960 = 87.6% | 842/960 = 87.7% | 960/960 = 100.0% | 960/960 = 100.0% | 253/960 = 26.4% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 5/20 = 25.0% |

### Product 408

**Selected features:** selected = {b, cd, cw, d, i, tl, up, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 10/22 = 45.5% |
| ActionExchange | 440 | 0/440 = 0.0% | 422/440 = 95.9% | 363/440 = 82.5% | 440/440 = 100.0% | 440/440 = 100.0% | 208/440 = 47.3% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 7/13 = 53.8% |

### Product 409

**Selected features:** selected = {b, c, cd, cw, d, eu, i, ie, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 15/26 = 57.7% |
| ActionExchange | 624 | 0/624 = 0.0% | 602/624 = 96.5% | 531/624 = 85.1% | 624/624 = 100.0% | 624/624 = 100.0% | 368/624 = 59.0% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 10/15 = 66.7% |

### Product 410

**Selected features:** selected = {b, c, cw, d, dl, i, ie, tl, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 19/22 = 86.4% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 11/22 = 50.0% |
| ActionExchange | 440 | 0/440 = 0.0% | 381/440 = 86.6% | 363/440 = 82.5% | 440/440 = 100.0% | 440/440 = 100.0% | 225/440 = 51.1% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 7/13 = 53.8% |

### Product 411

**Selected features:** selected = {b, cd, cw, d, dl, up, us, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 15/24 = 62.5% |
| ActionExchange | 504 | 0/504 = 0.0% | 485/504 = 96.2% | 443/504 = 87.9% | 504/504 = 100.0% | 504/504 = 100.0% | 317/504 = 62.9% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 10/14 = 71.4% |

### Product 412

**Selected features:** selected = {b, cd, cw, d, dl, tl, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 13/24 = 54.2% |
| ActionExchange | 504 | 0/504 = 0.0% | 485/504 = 96.2% | 443/504 = 87.9% | 504/504 = 100.0% | 504/504 = 100.0% | 279/504 = 55.4% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |

### Product 413

**Selected features:** selected = {b, cw, d, us, w}

**Repaired FTS:** 9 states, 12 transitions (12 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 20 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12 | 0/12 = 0.0% | 9/12 = 75.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 11/12 = 91.7% |
| ActionExchange | 132 | 0/132 = 0.0% | 100/132 = 75.8% | 111/132 = 84.1% | 132/132 = 100.0% | 132/132 = 100.0% | 122/132 = 92.4% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 414

**Selected features:** selected = {b, cw, d, i, ie, tl, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |
| ActionExchange | 420 | 0/420 = 0.0% | 361/420 = 86.0% | 362/420 = 86.2% | 420/420 = 100.0% | 420/420 = 100.0% | 264/420 = 62.9% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 9/13 = 69.2% |

### Product 415

**Selected features:** selected = {b, c, cd, cw, d, eu, t, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 17/24 = 70.8% |
| ActionExchange | 528 | 0/528 = 0.0% | 486/528 = 92.0% | 442/528 = 83.7% | 528/528 = 100.0% | 528/528 = 100.0% | 376/528 = 71.2% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 416

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, o, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 24/28 = 85.7% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 12/28 = 42.9% |
| ActionExchange | 728 | 0/728 = 0.0% | 625/728 = 85.9% | 602/728 = 82.7% | 728/728 = 100.0% | 728/728 = 100.0% | 322/728 = 44.2% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 8/16 = 50.0% |

### Product 417

**Selected features:** selected = {b, cd, d, i, t, tl, up, w}

**Repaired FTS:** 19 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 26/28 = 92.9% | 28/28 = 100.0% | 28/28 = 100.0% | 9/28 = 32.1% |
| ActionExchange | 728 | 0/728 = 0.0% | 678/728 = 93.1% | 676/728 = 92.9% | 728/728 = 100.0% | 728/728 = 100.0% | 241/728 = 33.1% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 6/18 = 33.3% |

### Product 418

**Selected features:** selected = {b, c, cw, d, eu, i, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |
| ActionExchange | 240 | 0/240 = 0.0% | 196/240 = 81.7% | 211/240 = 87.9% | 240/240 = 100.0% | 240/240 = 100.0% | 169/240 = 70.4% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 419

**Selected features:** selected = {b, cw, d, dl, up, us, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 8/21 = 38.1% |
| ActionExchange | 399 | 0/399 = 0.0% | 343/399 = 86.0% | 344/399 = 86.2% | 399/399 = 100.0% | 399/399 = 100.0% | 157/399 = 39.3% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 5/13 = 38.5% |

### Product 420

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, o, t, up, w}

**Repaired FTS:** 22 states, 36 transitions (36 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 36 | 0/36 = 0.0% | 33/36 = 91.7% | 31/36 = 86.1% | 36/36 = 100.0% | 36/36 = 100.0% | 12/36 = 33.3% |
| ActionExchange | 1188 | 0/1188 = 0.0% | 1091/1188 = 91.8% | 1026/1188 = 86.4% | 1188/1188 = 100.0% | 1188/1188 = 100.0% | 411/1188 = 34.6% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 8/21 = 38.1% |

### Product 421

**Selected features:** selected = {b, cd, cw, d, t, up, us, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 13/27 = 48.1% |
| ActionExchange | 675 | 0/675 = 0.0% | 627/675 = 92.9% | 577/675 = 85.5% | 675/675 = 100.0% | 675/675 = 100.0% | 329/675 = 48.7% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 10/17 = 58.8% |

### Product 422

**Selected features:** selected = {b, c, cw, d, eu, t, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 13/26 = 50.0% |
| ActionExchange | 650 | 0/650 = 0.0% | 551/650 = 84.8% | 576/650 = 88.6% | 650/650 = 100.0% | 650/650 = 100.0% | 331/650 = 50.9% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |

### Product 423

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, ie, up, w}

**Repaired FTS:** 18 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 29/30 = 96.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 11/30 = 36.7% |
| ActionExchange | 810 | 0/810 = 0.0% | 785/810 = 96.9% | 705/810 = 87.0% | 810/810 = 100.0% | 810/810 = 100.0% | 307/810 = 37.9% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 7/17 = 41.2% |

### Product 424

**Selected features:** selected = {b, c, cw, d, dl, i, ie, tl, up, w}

**Repaired FTS:** 17 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 24/27 = 88.9% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 11/27 = 40.7% |
| ActionExchange | 675 | 0/675 = 0.0% | 601/675 = 89.0% | 578/675 = 85.6% | 675/675 = 100.0% | 675/675 = 100.0% | 285/675 = 42.2% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 7/16 = 43.8% |

### Product 425

**Selected features:** selected = {b, cd, cw, d, i, ie, tl, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 8/24 = 33.3% |
| ActionExchange | 528 | 0/528 = 0.0% | 508/528 = 96.2% | 443/528 = 83.9% | 528/528 = 100.0% | 528/528 = 100.0% | 183/528 = 34.7% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 5/14 = 35.7% |

### Product 426

**Selected features:** selected = {b, cw, d, tl, up, w}

**Repaired FTS:** 12 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 14/17 = 82.4% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |
| ActionExchange | 272 | 0/272 = 0.0% | 225/272 = 82.7% | 241/272 = 88.6% | 272/272 = 100.0% | 272/272 = 100.0% | 178/272 = 65.4% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 427

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 9/26 = 34.6% |
| ActionExchange | 598 | 0/598 = 0.0% | 577/598 = 96.5% | 531/598 = 88.8% | 598/598 = 100.0% | 598/598 = 100.0% | 213/598 = 35.6% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 5/15 = 33.3% |

### Product 428

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, t, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 30/35 = 85.7% | 35/35 = 100.0% | 35/35 = 100.0% | 18/35 = 51.4% |
| ActionExchange | 1120 | 0/1120 = 0.0% | 1058/1120 = 94.5% | 963/1120 = 86.0% | 1120/1120 = 100.0% | 1120/1120 = 100.0% | 589/1120 = 52.6% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 13/21 = 61.9% |

### Product 429

**Selected features:** selected = {b, cw, d, i, ie, up, us, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 11/21 = 52.4% |
| ActionExchange | 420 | 0/420 = 0.0% | 361/420 = 86.0% | 362/420 = 86.2% | 420/420 = 100.0% | 420/420 = 100.0% | 226/420 = 53.8% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 7/13 = 53.8% |

### Product 430

**Selected features:** selected = {b, c, cw, d, dl, t, us, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 12/25 = 48.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 484/575 = 84.2% | 485/575 = 84.3% | 575/575 = 100.0% | 575/575 = 100.0% | 284/575 = 49.4% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 8/16 = 50.0% |

### Product 431

**Selected features:** selected = {b, cw, d, dl, eu, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |
| ActionExchange | 224 | 0/224 = 0.0% | 183/224 = 81.7% | 184/224 = 82.1% | 224/224 = 100.0% | 224/224 = 100.0% | 141/224 = 62.9% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 432

**Selected features:** selected = {b, cd, cw, d, eu, w}

**Repaired FTS:** 10 states, 15 transitions (15 real / 0 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 16 family-level), 27 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 15 | 0/15 = 0.0% | 14/15 = 93.3% | 11/15 = 73.3% | 15/15 = 100.0% | 15/15 = 100.0% | 8/15 = 53.3% |
| ActionExchange | 195 | 0/195 = 0.0% | 184/195 = 94.4% | 146/195 = 74.9% | 195/195 = 100.0% | 195/195 = 100.0% | 107/195 = 54.9% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 433

**Selected features:** selected = {b, cd, cw, d, i, t, up, us, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 27/29 = 93.1% | 25/29 = 86.2% | 29/29 = 100.0% | 29/29 = 100.0% | 15/29 = 51.7% |
| ActionExchange | 783 | 0/783 = 0.0% | 731/783 = 93.4% | 677/783 = 86.5% | 783/783 = 100.0% | 783/783 = 100.0% | 411/783 = 52.5% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |

### Product 434

**Selected features:** selected = {b, c, cw, d, i, t, tl, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 16/23 = 69.6% |
| ActionExchange | 506 | 0/506 = 0.0% | 419/506 = 82.8% | 441/506 = 87.2% | 506/506 = 100.0% | 506/506 = 100.0% | 357/506 = 70.6% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 435

**Selected features:** selected = {b, cd, cw, d, dl, us, w}

**Repaired FTS:** 12 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 18/19 = 94.7% | 15/19 = 78.9% | 19/19 = 100.0% | 19/19 = 100.0% | 10/19 = 52.6% |
| ActionExchange | 304 | 0/304 = 0.0% | 290/304 = 95.4% | 243/304 = 79.9% | 304/304 = 100.0% | 304/304 = 100.0% | 162/304 = 53.3% |
| StateMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 7/11 = 63.6% |

### Product 436

**Selected features:** selected = {b, c, cw, d, eu, i, ie, t, up, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 16/30 = 53.3% |
| ActionExchange | 870 | 0/870 = 0.0% | 755/870 = 86.8% | 756/870 = 86.9% | 870/870 = 100.0% | 870/870 = 100.0% | 472/870 = 54.3% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 11/19 = 57.9% |

### Product 437

**Selected features:** selected = {b, c, cd, cw, d, eu, i, t, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 13/31 = 41.9% |
| ActionExchange | 899 | 0/899 = 0.0% | 843/899 = 93.8% | 785/899 = 87.3% | 899/899 = 100.0% | 899/899 = 100.0% | 386/899 = 42.9% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |

### Product 438

**Selected features:** selected = {b, c, cw, d, dl, eu, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 11/23 = 47.8% |
| ActionExchange | 483 | 0/483 = 0.0% | 421/483 = 87.2% | 422/483 = 87.4% | 483/483 = 100.0% | 483/483 = 100.0% | 237/483 = 49.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 439

**Selected features:** selected = {b, cd, cw, d, dl, t, us, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 10/26 = 38.5% |
| ActionExchange | 598 | 0/598 = 0.0% | 554/598 = 92.6% | 486/598 = 81.3% | 598/598 = 100.0% | 598/598 = 100.0% | 235/598 = 39.3% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 7/16 = 43.8% |

### Product 440

**Selected features:** selected = {b, c, cw, d, dl, eu, t, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 11/25 = 44.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 484/575 = 84.2% | 485/575 = 84.3% | 575/575 = 100.0% | 575/575 = 100.0% | 258/575 = 44.9% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 7/16 = 43.8% |

### Product 441

**Selected features:** selected = {b, c, cw, d, eu, t, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 6/21 = 28.6% |
| ActionExchange | 420 | 0/420 = 0.0% | 341/420 = 81.2% | 361/420 = 86.0% | 420/420 = 100.0% | 420/420 = 100.0% | 124/420 = 29.5% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 4/14 = 28.6% |

### Product 442

**Selected features:** selected = {b, c, cw, d, t, tl, w}

**Repaired FTS:** 15 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 17/21 = 81.0% | 18/21 = 85.7% | 21/21 = 100.0% | 21/21 = 100.0% | 10/21 = 47.6% |
| ActionExchange | 420 | 0/420 = 0.0% | 341/420 = 81.2% | 361/420 = 86.0% | 420/420 = 100.0% | 420/420 = 100.0% | 204/420 = 48.6% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 443

**Selected features:** selected = {b, c, cd, cw, d, eu, i, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 11/24 = 45.8% |
| ActionExchange | 528 | 0/528 = 0.0% | 508/528 = 96.2% | 464/528 = 87.9% | 528/528 = 100.0% | 528/528 = 100.0% | 248/528 = 47.0% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 444

**Selected features:** selected = {b, c, cw, d, dl, up, us, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 11/23 = 47.8% |
| ActionExchange | 483 | 0/483 = 0.0% | 421/483 = 87.2% | 422/483 = 87.4% | 483/483 = 100.0% | 483/483 = 100.0% | 237/483 = 49.1% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 445

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, t, up, w}

**Repaired FTS:** 21 states, 32 transitions (32 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 41 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 32 | 0/32 = 0.0% | 28/32 = 87.5% | 27/32 = 84.4% | 32/32 = 100.0% | 32/32 = 100.0% | 12/32 = 37.5% |
| ActionExchange | 960 | 0/960 = 0.0% | 841/960 = 87.6% | 813/960 = 84.7% | 960/960 = 100.0% | 960/960 = 100.0% | 370/960 = 38.5% |
| StateMissing | 20 | 0/20 = 0.0% | 18/20 = 90.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 8/20 = 40.0% |

### Product 446

**Selected features:** selected = {b, cw, d, eu, i, ie, w}

**Repaired FTS:** 11 states, 16 transitions (16 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 16 | 0/16 = 0.0% | 13/16 = 81.3% | 13/16 = 81.3% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |
| ActionExchange | 240 | 0/240 = 0.0% | 196/240 = 81.7% | 197/240 = 82.1% | 240/240 = 100.0% | 240/240 = 100.0% | 152/240 = 63.3% |
| StateMissing | 10 | 0/10 = 0.0% | 8/10 = 80.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 447

**Selected features:** selected = {b, cd, cw, d, i, tl, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 11/17 = 64.7% |
| ActionExchange | 255 | 0/255 = 0.0% | 242/255 = 94.9% | 198/255 = 77.6% | 255/255 = 100.0% | 255/255 = 100.0% | 169/255 = 66.3% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 448

**Selected features:** selected = {b, cd, cw, d, dl, i, tl, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 8/21 = 38.1% |
| ActionExchange | 378 | 0/378 = 0.0% | 362/378 = 95.8% | 309/378 = 81.7% | 378/378 = 100.0% | 378/378 = 100.0% | 147/378 = 38.9% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 5/12 = 41.7% |

### Product 449

**Selected features:** selected = {b, cw, d, eu, i, up, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 342 | 0/342 = 0.0% | 289/342 = 84.5% | 307/342 = 89.8% | 342/342 = 100.0% | 342/342 = 100.0% | 220/342 = 64.3% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 450

**Selected features:** selected = {b, cd, cw, d, i, ie, t, tl, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 21/26 = 80.8% | 26/26 = 100.0% | 26/26 = 100.0% | 15/26 = 57.7% |
| ActionExchange | 624 | 0/624 = 0.0% | 578/624 = 92.6% | 507/624 = 81.3% | 624/624 = 100.0% | 624/624 = 100.0% | 363/624 = 58.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 10/16 = 62.5% |

### Product 451

**Selected features:** selected = {b, cw, d, dl, eu, i, ie, o, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 18/23 = 78.3% | 23/23 = 100.0% | 23/23 = 100.0% | 12/23 = 52.2% |
| ActionExchange | 483 | 0/483 = 0.0% | 400/483 = 82.8% | 382/483 = 79.1% | 483/483 = 100.0% | 483/483 = 100.0% | 258/483 = 53.4% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 8/13 = 61.5% |

### Product 452

**Selected features:** selected = {b, c, cw, d, dl, t, up, us, w}

**Repaired FTS:** 20 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 26/30 = 86.7% | 26/30 = 86.7% | 30/30 = 100.0% | 30/30 = 100.0% | 12/30 = 40.0% |
| ActionExchange | 840 | 0/840 = 0.0% | 729/840 = 86.8% | 730/840 = 86.9% | 840/840 = 100.0% | 840/840 = 100.0% | 345/840 = 41.1% |
| StateMissing | 19 | 0/19 = 0.0% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 8/19 = 42.1% |

### Product 453

**Selected features:** selected = {b, cd, cw, d, eu, i, up, w}

**Repaired FTS:** 14 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 21/22 = 95.5% | 18/22 = 81.8% | 22/22 = 100.0% | 22/22 = 100.0% | 16/22 = 72.7% |
| ActionExchange | 440 | 0/440 = 0.0% | 422/440 = 95.9% | 363/440 = 82.5% | 440/440 = 100.0% | 440/440 = 100.0% | 324/440 = 73.6% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 10/13 = 76.9% |

### Product 454

**Selected features:** selected = {b, cd, cw, d, dl, i, ie, tl, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 13/28 = 46.4% |
| ActionExchange | 700 | 0/700 = 0.0% | 677/700 = 96.7% | 603/700 = 86.1% | 700/700 = 100.0% | 700/700 = 100.0% | 330/700 = 47.1% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 8/16 = 50.0% |

### Product 455

**Selected features:** selected = {b, c, cd, cw, d, dl, t, us, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 14/28 = 50.0% |
| ActionExchange | 700 | 0/700 = 0.0% | 652/700 = 93.1% | 578/700 = 82.6% | 700/700 = 100.0% | 700/700 = 100.0% | 357/700 = 51.0% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 9/17 = 52.9% |

### Product 456

**Selected features:** selected = {b, cd, cw, d, eu, i, ie, up, w}

**Repaired FTS:** 15 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 23/24 = 95.8% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 10/24 = 41.7% |
| ActionExchange | 528 | 0/528 = 0.0% | 508/528 = 96.2% | 443/528 = 83.9% | 528/528 = 100.0% | 528/528 = 100.0% | 225/528 = 42.6% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 7/14 = 50.0% |

### Product 457

**Selected features:** selected = {b, c, cd, cw, d, t, tl, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 15/24 = 62.5% |
| ActionExchange | 528 | 0/528 = 0.0% | 486/528 = 92.0% | 442/528 = 83.7% | 528/528 = 100.0% | 528/528 = 100.0% | 332/528 = 62.9% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 11/15 = 73.3% |

### Product 458

**Selected features:** selected = {b, cd, cw, d, t, us, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 13/22 = 59.1% |
| ActionExchange | 440 | 0/440 = 0.0% | 402/440 = 91.4% | 343/440 = 78.0% | 440/440 = 100.0% | 440/440 = 100.0% | 264/440 = 60.0% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 459

**Selected features:** selected = {b, cw, d, i, up, us, w}

**Repaired FTS:** 13 states, 19 transitions (19 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 16/19 = 84.2% | 17/19 = 89.5% | 19/19 = 100.0% | 19/19 = 100.0% | 13/19 = 68.4% |
| ActionExchange | 342 | 0/342 = 0.0% | 289/342 = 84.5% | 307/342 = 89.8% | 342/342 = 100.0% | 342/342 = 100.0% | 236/342 = 69.0% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 9/12 = 75.0% |

### Product 460

**Selected features:** selected = {b, cd, cw, d, eu, i, t, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 12/24 = 50.0% |
| ActionExchange | 528 | 0/528 = 0.0% | 486/528 = 92.0% | 442/528 = 83.7% | 528/528 = 100.0% | 528/528 = 100.0% | 267/528 = 50.6% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 9/15 = 60.0% |

### Product 461

**Selected features:** selected = {b, c, cw, d, dl, eu, i, ie, t, w}

**Repaired FTS:** 19 states, 29 transitions (29 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 37 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 29 | 0/29 = 0.0% | 25/29 = 86.2% | 24/29 = 82.8% | 29/29 = 100.0% | 29/29 = 100.0% | 17/29 = 58.6% |
| ActionExchange | 783 | 0/783 = 0.0% | 676/783 = 86.3% | 651/783 = 83.1% | 783/783 = 100.0% | 783/783 = 100.0% | 469/783 = 59.9% |
| StateMissing | 18 | 0/18 = 0.0% | 16/18 = 88.9% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 13/18 = 72.2% |

### Product 462

**Selected features:** selected = {b, cd, cw, d, dl, i, tl, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 9/26 = 34.6% |
| ActionExchange | 598 | 0/598 = 0.0% | 577/598 = 96.5% | 531/598 = 88.8% | 598/598 = 100.0% | 598/598 = 100.0% | 214/598 = 35.8% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 6/15 = 40.0% |

### Product 463

**Selected features:** selected = {b, cd, cw, d, eu, i, ie, t, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 14/31 = 45.2% |
| ActionExchange | 899 | 0/899 = 0.0% | 843/899 = 93.8% | 757/899 = 84.2% | 899/899 = 100.0% | 899/899 = 100.0% | 413/899 = 45.9% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |

### Product 464

**Selected features:** selected = {b, c, cw, d, dl, i, tl, up, w}

**Repaired FTS:** 16 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 22/25 = 88.0% | 22/25 = 88.0% | 25/25 = 100.0% | 25/25 = 100.0% | 7/25 = 28.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 507/575 = 88.2% | 508/575 = 88.3% | 575/575 = 100.0% | 575/575 = 100.0% | 171/575 = 29.7% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 4/15 = 26.7% |

### Product 465

**Selected features:** selected = {b, cd, cw, d, eu, t, w}

**Repaired FTS:** 15 states, 22 transitions (22 real / 0 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 20/22 = 90.9% | 17/22 = 77.3% | 22/22 = 100.0% | 22/22 = 100.0% | 13/22 = 59.1% |
| ActionExchange | 440 | 0/440 = 0.0% | 402/440 = 91.4% | 343/440 = 78.0% | 440/440 = 100.0% | 440/440 = 100.0% | 264/440 = 60.0% |
| StateMissing | 14 | 0/14 = 0.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 466

**Selected features:** selected = {b, cw, d, dl, t, us, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 16/23 = 69.6% |
| ActionExchange | 483 | 0/483 = 0.0% | 400/483 = 82.8% | 401/483 = 83.0% | 483/483 = 100.0% | 483/483 = 100.0% | 341/483 = 70.6% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 12/15 = 80.0% |

### Product 467

**Selected features:** selected = {b, cd, cw, d, eu, i, w}

**Repaired FTS:** 11 states, 17 transitions (17 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 17 | 0/17 = 0.0% | 16/17 = 94.1% | 13/17 = 76.5% | 17/17 = 100.0% | 17/17 = 100.0% | 10/17 = 58.8% |
| ActionExchange | 255 | 0/255 = 0.0% | 242/255 = 94.9% | 198/255 = 77.6% | 255/255 = 100.0% | 255/255 = 100.0% | 152/255 = 59.6% |
| StateMissing | 10 | 0/10 = 0.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 10/10 = 100.0% | 7/10 = 70.0% |

### Product 468

**Selected features:** selected = {b, c, cd, cw, d, dl, i, ie, t, tl, up, w}

**Repaired FTS:** 23 states, 37 transitions (37 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 50 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 37 | 0/37 = 0.0% | 35/37 = 94.6% | 32/37 = 86.5% | 37/37 = 100.0% | 37/37 = 100.0% | 8/37 = 21.6% |
| ActionExchange | 1258 | 0/1258 = 0.0% | 1192/1258 = 94.8% | 1091/1258 = 86.7% | 1258/1258 = 100.0% | 1258/1258 = 100.0% | 284/1258 = 22.6% |
| StateMissing | 22 | 0/22 = 0.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 22/22 = 100.0% | 5/22 = 22.7% |

### Product 469

**Selected features:** selected = {b, cw, d, eu, t, up, w}

**Repaired FTS:** 17 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 32 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 20/24 = 83.3% | 21/24 = 87.5% | 24/24 = 100.0% | 24/24 = 100.0% | 14/24 = 58.3% |
| ActionExchange | 552 | 0/552 = 0.0% | 461/552 = 83.5% | 484/552 = 87.7% | 552/552 = 100.0% | 552/552 = 100.0% | 326/552 = 59.1% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 11/16 = 68.8% |

### Product 470

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 25/28 = 89.3% | 28/28 = 100.0% | 28/28 = 100.0% | 11/28 = 39.3% |
| ActionExchange | 700 | 0/700 = 0.0% | 677/700 = 96.7% | 627/700 = 89.6% | 700/700 = 100.0% | 700/700 = 100.0% | 284/700 = 40.6% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 7/16 = 43.8% |

### Product 471

**Selected features:** selected = {b, c, cd, cw, d, dl, i, us, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 9/23 = 39.1% |
| ActionExchange | 460 | 0/460 = 0.0% | 442/460 = 96.1% | 383/460 = 83.3% | 460/460 = 100.0% | 460/460 = 100.0% | 190/460 = 41.3% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 6/13 = 46.2% |

### Product 472

**Selected features:** selected = {b, cw, d, dl, i, t, us, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 7/25 = 28.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 484/575 = 84.2% | 485/575 = 84.3% | 575/575 = 100.0% | 575/575 = 100.0% | 168/575 = 29.2% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 4/16 = 25.0% |

### Product 473

**Selected features:** selected = {b, cw, d, dl, i, ie, tl, w}

**Repaired FTS:** 13 states, 20 transitions (20 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 17/20 = 85.0% | 16/20 = 80.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |
| ActionExchange | 360 | 0/360 = 0.0% | 307/360 = 85.3% | 291/360 = 80.8% | 360/360 = 100.0% | 360/360 = 100.0% | 221/360 = 61.4% |
| StateMissing | 12 | 0/12 = 0.0% | 10/12 = 83.3% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 8/12 = 66.7% |

### Product 474

**Selected features:** selected = {b, cw, d, i, ie, t, tl, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 14/23 = 60.9% |
| ActionExchange | 506 | 0/506 = 0.0% | 419/506 = 82.8% | 420/506 = 83.0% | 506/506 = 100.0% | 506/506 = 100.0% | 312/506 = 61.7% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 11/15 = 73.3% |

### Product 475

**Selected features:** selected = {b, c, cd, cw, d, dl, eu, i, w}

**Repaired FTS:** 14 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 22/23 = 95.7% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 8/23 = 34.8% |
| ActionExchange | 460 | 0/460 = 0.0% | 442/460 = 96.1% | 383/460 = 83.3% | 460/460 = 100.0% | 460/460 = 100.0% | 166/460 = 36.1% |
| StateMissing | 13 | 0/13 = 0.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 5/13 = 38.5% |

### Product 476

**Selected features:** selected = {b, cw, d, dl, eu, i, up, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 14/23 = 60.9% |
| ActionExchange | 483 | 0/483 = 0.0% | 421/483 = 87.2% | 422/483 = 87.4% | 483/483 = 100.0% | 483/483 = 100.0% | 302/483 = 62.5% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |

### Product 477

**Selected features:** selected = {b, c, cw, d, eu, i, up, w}

**Repaired FTS:** 14 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 31 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 18/21 = 85.7% | 19/21 = 90.5% | 21/21 = 100.0% | 21/21 = 100.0% | 11/21 = 52.4% |
| ActionExchange | 420 | 0/420 = 0.0% | 361/420 = 86.0% | 381/420 = 90.7% | 420/420 = 100.0% | 420/420 = 100.0% | 226/420 = 53.8% |
| StateMissing | 13 | 0/13 = 0.0% | 11/13 = 84.6% | 13/13 = 100.0% | 13/13 = 100.0% | 13/13 = 100.0% | 7/13 = 53.8% |

### Product 478

**Selected features:** selected = {b, cd, cw, d, i, t, tl, w}

**Repaired FTS:** 16 states, 24 transitions (24 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 36 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 22/24 = 91.7% | 20/24 = 83.3% | 24/24 = 100.0% | 24/24 = 100.0% | 17/24 = 70.8% |
| ActionExchange | 528 | 0/528 = 0.0% | 486/528 = 92.0% | 442/528 = 83.7% | 528/528 = 100.0% | 528/528 = 100.0% | 377/528 = 71.4% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 13/15 = 86.7% |

### Product 479

**Selected features:** selected = {b, c, cd, cw, d, eu, i, ie, t, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 17/33 = 51.5% |
| ActionExchange | 1023 | 0/1023 = 0.0% | 963/1023 = 94.1% | 871/1023 = 85.1% | 1023/1023 = 100.0% | 1023/1023 = 100.0% | 537/1023 = 52.5% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |

### Product 480

**Selected features:** selected = {b, cw, d, eu, i, t, up, w}

**Repaired FTS:** 18 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 35 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 22/26 = 84.6% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 19/26 = 73.1% |
| ActionExchange | 650 | 0/650 = 0.0% | 551/650 = 84.8% | 576/650 = 88.6% | 650/650 = 100.0% | 650/650 = 100.0% | 477/650 = 73.4% |
| StateMissing | 17 | 0/17 = 0.0% | 15/17 = 88.2% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 14/17 = 82.4% |

### Product 481

**Selected features:** selected = {b, c, cd, cw, d, dl, i, t, tl, up, w}

**Repaired FTS:** 22 states, 35 transitions (35 real / 0 `__end__`).

**Family baseline projected to this product:** 15 test case(s) (of 16 family-level), 48 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 35 | 0/35 = 0.0% | 33/35 = 94.3% | 31/35 = 88.6% | 35/35 = 100.0% | 35/35 = 100.0% | 16/35 = 45.7% |
| ActionExchange | 1120 | 0/1120 = 0.0% | 1058/1120 = 94.5% | 994/1120 = 88.8% | 1120/1120 = 100.0% | 1120/1120 = 100.0% | 530/1120 = 47.3% |
| StateMissing | 21 | 0/21 = 0.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 21/21 = 100.0% | 11/21 = 52.4% |

### Product 482

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, ie, up, w}

**Repaired FTS:** 17 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 27/28 = 96.4% | 24/28 = 85.7% | 28/28 = 100.0% | 28/28 = 100.0% | 11/28 = 39.3% |
| ActionExchange | 700 | 0/700 = 0.0% | 677/700 = 96.7% | 603/700 = 86.1% | 700/700 = 100.0% | 700/700 = 100.0% | 281/700 = 40.1% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 7/16 = 43.8% |

### Product 483

**Selected features:** selected = {b, cd, cw, d, dl, i, up, us, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 12/26 = 46.2% |
| ActionExchange | 598 | 0/598 = 0.0% | 577/598 = 96.5% | 531/598 = 88.8% | 598/598 = 100.0% | 598/598 = 100.0% | 285/598 = 47.7% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 8/15 = 53.3% |

### Product 484

**Selected features:** selected = {b, cw, d, dl, i, up, us, w}

**Repaired FTS:** 15 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 20/23 = 87.0% | 20/23 = 87.0% | 23/23 = 100.0% | 23/23 = 100.0% | 12/23 = 52.2% |
| ActionExchange | 483 | 0/483 = 0.0% | 421/483 = 87.2% | 422/483 = 87.4% | 483/483 = 100.0% | 483/483 = 100.0% | 257/483 = 53.2% |
| StateMissing | 14 | 0/14 = 0.0% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 14/14 = 100.0% | 8/14 = 57.1% |

### Product 485

**Selected features:** selected = {b, cw, d, i, us, w}

**Repaired FTS:** 10 states, 14 transitions (14 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 23 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 14 | 0/14 = 0.0% | 11/14 = 78.6% | 12/14 = 85.7% | 14/14 = 100.0% | 14/14 = 100.0% | 9/14 = 64.3% |
| ActionExchange | 182 | 0/182 = 0.0% | 144/182 = 79.1% | 157/182 = 86.3% | 182/182 = 100.0% | 182/182 = 100.0% | 118/182 = 64.8% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 486

**Selected features:** selected = {b, cw, d, dl, eu, i, t, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 6/25 = 24.0% |
| ActionExchange | 575 | 0/575 = 0.0% | 484/575 = 84.2% | 485/575 = 84.3% | 575/575 = 100.0% | 575/575 = 100.0% | 145/575 = 25.2% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 4/16 = 25.0% |

### Product 487

**Selected features:** selected = {b, cw, d, dl, eu, t, w}

**Repaired FTS:** 16 states, 23 transitions (23 real / 0 `__end__`).

**Family baseline projected to this product:** 10 test case(s) (of 16 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 19/23 = 82.6% | 19/23 = 82.6% | 23/23 = 100.0% | 23/23 = 100.0% | 15/23 = 65.2% |
| ActionExchange | 483 | 0/483 = 0.0% | 400/483 = 82.8% | 401/483 = 83.0% | 483/483 = 100.0% | 483/483 = 100.0% | 316/483 = 65.4% |
| StateMissing | 15 | 0/15 = 0.0% | 13/15 = 86.7% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 11/15 = 73.3% |

### Product 488

**Selected features:** selected = {b, cd, cw, d, dl, i, us, w}

**Repaired FTS:** 13 states, 21 transitions (21 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 34 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 20/21 = 95.2% | 17/21 = 81.0% | 21/21 = 100.0% | 21/21 = 100.0% | 4/21 = 19.0% |
| ActionExchange | 378 | 0/378 = 0.0% | 362/378 = 95.8% | 309/378 = 81.7% | 378/378 = 100.0% | 378/378 = 100.0% | 76/378 = 20.1% |
| StateMissing | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% | 2/12 = 16.7% |

### Product 489

**Selected features:** selected = {b, cd, cw, d, dl, eu, i, up, w}

**Repaired FTS:** 16 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 25/26 = 96.2% | 23/26 = 88.5% | 26/26 = 100.0% | 26/26 = 100.0% | 10/26 = 38.5% |
| ActionExchange | 598 | 0/598 = 0.0% | 577/598 = 96.5% | 531/598 = 88.8% | 598/598 = 100.0% | 598/598 = 100.0% | 241/598 = 40.3% |
| StateMissing | 15 | 0/15 = 0.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 15/15 = 100.0% | 7/15 = 46.7% |

### Product 490

**Selected features:** selected = {b, cd, cw, d, eu, t, up, w}

**Repaired FTS:** 18 states, 27 transitions (27 real / 0 `__end__`).

**Family baseline projected to this product:** 11 test case(s) (of 16 family-level), 39 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 27 | 0/27 = 0.0% | 25/27 = 92.6% | 23/27 = 85.2% | 27/27 = 100.0% | 27/27 = 100.0% | 13/27 = 48.1% |
| ActionExchange | 675 | 0/675 = 0.0% | 627/675 = 92.9% | 577/675 = 85.5% | 675/675 = 100.0% | 675/675 = 100.0% | 329/675 = 48.7% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 10/17 = 58.8% |

### Product 491

**Selected features:** selected = {b, cd, cw, d, dl, i, t, tl, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 29/33 = 87.9% | 33/33 = 100.0% | 33/33 = 100.0% | 12/33 = 36.4% |
| ActionExchange | 990 | 0/990 = 0.0% | 932/990 = 94.1% | 872/990 = 88.1% | 990/990 = 100.0% | 990/990 = 100.0% | 371/990 = 37.5% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 8/20 = 40.0% |

### Product 492

**Selected features:** selected = {b, c, cd, cw, d, i, ie, t, tl, up, w}

**Repaired FTS:** 21 states, 33 transitions (33 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 46 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 33 | 0/33 = 0.0% | 31/33 = 93.9% | 28/33 = 84.8% | 33/33 = 100.0% | 33/33 = 100.0% | 11/33 = 33.3% |
| ActionExchange | 1023 | 0/1023 = 0.0% | 963/1023 = 94.1% | 871/1023 = 85.1% | 1023/1023 = 100.0% | 1023/1023 = 100.0% | 355/1023 = 34.7% |
| StateMissing | 20 | 0/20 = 0.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 20/20 = 100.0% | 8/20 = 40.0% |

### Product 493

**Selected features:** selected = {b, cd, cw, d, i, ie, t, tl, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 26/31 = 83.9% | 31/31 = 100.0% | 31/31 = 100.0% | 18/31 = 58.1% |
| ActionExchange | 899 | 0/899 = 0.0% | 843/899 = 93.8% | 757/899 = 84.2% | 899/899 = 100.0% | 899/899 = 100.0% | 528/899 = 58.7% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 13/19 = 68.4% |

### Product 494

**Selected features:** selected = {b, c, cd, cw, d, i, t, tl, up, w}

**Repaired FTS:** 20 states, 31 transitions (31 real / 0 `__end__`).

**Family baseline projected to this product:** 14 test case(s) (of 16 family-level), 44 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 31 | 0/31 = 0.0% | 29/31 = 93.5% | 27/31 = 87.1% | 31/31 = 100.0% | 31/31 = 100.0% | 11/31 = 35.5% |
| ActionExchange | 899 | 0/899 = 0.0% | 843/899 = 93.8% | 785/899 = 87.3% | 899/899 = 100.0% | 899/899 = 100.0% | 331/899 = 36.8% |
| StateMissing | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% | 8/19 = 42.1% |

### Product 495

**Selected features:** selected = {b, c, cw, d, eu, i, ie, t, w}

**Repaired FTS:** 17 states, 25 transitions (25 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 33 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 21/25 = 84.0% | 21/25 = 84.0% | 25/25 = 100.0% | 25/25 = 100.0% | 10/25 = 40.0% |
| ActionExchange | 600 | 0/600 = 0.0% | 505/600 = 84.2% | 506/600 = 84.3% | 600/600 = 100.0% | 600/600 = 100.0% | 246/600 = 41.0% |
| StateMissing | 16 | 0/16 = 0.0% | 14/16 = 87.5% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 6/16 = 37.5% |

### Product 496

**Selected features:** selected = {b, c, cd, cw, d, eu, i, t, w}

**Repaired FTS:** 17 states, 26 transitions (26 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 38 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 24/26 = 92.3% | 22/26 = 84.6% | 26/26 = 100.0% | 26/26 = 100.0% | 7/26 = 26.9% |
| ActionExchange | 624 | 0/624 = 0.0% | 578/624 = 92.6% | 530/624 = 84.9% | 624/624 = 100.0% | 624/624 = 100.0% | 176/624 = 28.2% |
| StateMissing | 16 | 0/16 = 0.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% | 4/16 = 25.0% |

### Product 497

**Selected features:** selected = {b, c, cd, cw, d, i, ie, t, tl, w}

**Repaired FTS:** 18 states, 28 transitions (28 real / 0 `__end__`).

**Family baseline projected to this product:** 12 test case(s) (of 16 family-level), 40 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 26/28 = 92.9% | 23/28 = 82.1% | 28/28 = 100.0% | 28/28 = 100.0% | 18/28 = 64.3% |
| ActionExchange | 728 | 0/728 = 0.0% | 678/728 = 93.1% | 601/728 = 82.6% | 728/728 = 100.0% | 728/728 = 100.0% | 471/728 = 64.7% |
| StateMissing | 17 | 0/17 = 0.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 17/17 = 100.0% | 13/17 = 76.5% |

### Product 498

**Selected features:** selected = {b, c, cd, cw, d, dl, i, t, tl, w}

**Repaired FTS:** 19 states, 30 transitions (30 real / 0 `__end__`).

**Family baseline projected to this product:** 13 test case(s) (of 16 family-level), 42 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 30 | 0/30 = 0.0% | 28/30 = 93.3% | 25/30 = 83.3% | 30/30 = 100.0% | 30/30 = 100.0% | 15/30 = 50.0% |
| ActionExchange | 810 | 0/810 = 0.0% | 758/810 = 93.6% | 678/810 = 83.7% | 810/810 = 100.0% | 810/810 = 100.0% | 416/810 = 51.4% |
| StateMissing | 18 | 0/18 = 0.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 18/18 = 100.0% | 11/18 = 61.1% |
---

## BankAccountv2 summary (aggregate over 498 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 16 test case(s) generated once for the SPL.

**Equivalent-mutant treatment:** Inozemtseva &amp; Holmes (2014) — mutant not killed by ANY of the five suites (family + product state + product transition + product pair + random) is conservatively classified equivalent and EXCLUDED from the score denominator. Scores below are **killed / (total &minus; equivalent) = adjusted%**.

| Operator | Total mutants | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 12132 | 0/12132 = 0.0% | 10818/12132 = 89.2% | 10374/12132 = 85.5% | 12132/12132 = 100.0% | 12132/12132 = 100.0% | 5930/12132 = 48.9% |
| ActionExchange | 285954 | 0/285954 = 0.0% | 256572/285954 = 89.7% | 245643/285954 = 85.9% | 285954/285954 = 100.0% | 285954/285954 = 100.0% | 138412/285954 = 48.4% |
| StateMissing | 7473 | 0/7473 = 0.0% | 6981/7473 = 93.4% | 7473/7473 = 100.0% | 7473/7473 = 100.0% | 7473/7473 = 100.0% | 4070/7473 = 54.5% |

Total products: 498.
