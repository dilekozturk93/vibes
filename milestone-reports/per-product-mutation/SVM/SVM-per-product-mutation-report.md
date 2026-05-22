# Per-Product Mutation Report — SVM

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

**Selected features:** selected = {c, f, s, t}

**Repaired FTS:** 6 states, 8 transitions (8 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 8 | 0/8 = 0.0% | 0/8 = 0.0% | 7/8 = 87.5% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |
| ActionExchange | 16 | 0/16 = 0.0% | 0/16 = 0.0% | 15/16 = 93.8% | 16/16 = 100.0% | 16/16 = 100.0% | 16/16 = 100.0% |
| StateMissing | 5 | 0/5 = 0.0% | 0/5 = 0.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% | 5/5 = 100.0% |

### Product 2

**Selected features:** selected = {c, f, s}

**Repaired FTS:** 5 states, 6 transitions (6 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 6 | 0/6 = 0.0% | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |
| ActionExchange | 9 | 0/9 = 0.0% | 0/9 = 0.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |
| StateMissing | 4 | 0/4 = 0.0% | 0/4 = 0.0% | 4/4 = 100.0% | 4/4 = 100.0% | 4/4 = 100.0% | 4/4 = 100.0% |

### Product 3

**Selected features:** selected = {s}

**Repaired FTS:** 7 states, 7 transitions (7 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 11 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |
| ActionExchange | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 4

**Selected features:** selected = {t}

**Repaired FTS:** 7 states, 7 transitions (7 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 11 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |
| ActionExchange | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 5

**Selected features:** selected = {f, t}

**Repaired FTS:** 4 states, 4 transitions (4 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 4 | 0/4 = 0.0% | 0/4 = 0.0% | 3/4 = 75.0% | 4/4 = 100.0% | 4/4 = 100.0% | 4/4 = 100.0% |
| ActionExchange | 4 | 0/4 = 0.0% | 0/4 = 0.0% | 3/4 = 75.0% | 4/4 = 100.0% | 4/4 = 100.0% | 4/4 = 100.0% |
| StateMissing | 3 | 0/3 = 0.0% | 0/3 = 0.0% | 3/3 = 100.0% | 3/3 = 100.0% | 3/3 = 100.0% | 3/3 = 100.0% |

### Product 6

**Selected features:** selected = {f, s}

**Repaired FTS:** 4 states, 4 transitions (4 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 4 | 0/4 = 0.0% | 0/4 = 0.0% | 3/4 = 75.0% | 4/4 = 100.0% | 4/4 = 100.0% | 4/4 = 100.0% |
| ActionExchange | 4 | 0/4 = 0.0% | 0/4 = 0.0% | 3/4 = 75.0% | 4/4 = 100.0% | 4/4 = 100.0% | 4/4 = 100.0% |
| StateMissing | 3 | 0/3 = 0.0% | 0/3 = 0.0% | 3/3 = 100.0% | 3/3 = 100.0% | 3/3 = 100.0% | 3/3 = 100.0% |

### Product 7

**Selected features:** selected = {c, s, t}

**Repaired FTS:** 9 states, 11 transitions (11 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 18 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 11 | 0/11 = 0.0% | 11/11 = 100.0% | 10/11 = 90.9% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |
| ActionExchange | 19 | 0/19 = 0.0% | 19/19 = 100.0% | 18/19 = 94.7% | 19/19 = 100.0% | 19/19 = 100.0% | 19/19 = 100.0% |
| StateMissing | 8 | 0/8 = 0.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 8

**Selected features:** selected = {s, t}

**Repaired FTS:** 8 states, 9 transitions (9 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |
| ActionExchange | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 11/12 = 91.7% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 9

**Selected features:** selected = {c, s}

**Repaired FTS:** 8 states, 9 transitions (9 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |
| ActionExchange | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 11/12 = 91.7% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 10

**Selected features:** selected = {c, t}

**Repaired FTS:** 8 states, 9 transitions (9 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |
| ActionExchange | 12 | 0/12 = 0.0% | 12/12 = 100.0% | 11/12 = 91.7% | 12/12 = 100.0% | 12/12 = 100.0% | 12/12 = 100.0% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 11

**Selected features:** selected = {f, s, t}

**Repaired FTS:** 5 states, 6 transitions (6 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 6 | 0/6 = 0.0% | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |
| ActionExchange | 9 | 0/9 = 0.0% | 0/9 = 0.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |
| StateMissing | 4 | 0/4 = 0.0% | 0/4 = 0.0% | 4/4 = 100.0% | 4/4 = 100.0% | 4/4 = 100.0% | 4/4 = 100.0% |

### Product 12

**Selected features:** selected = {c, f, t}

**Repaired FTS:** 5 states, 6 transitions (6 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 6 | 0/6 = 0.0% | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |
| ActionExchange | 9 | 0/9 = 0.0% | 0/9 = 0.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |
| StateMissing | 4 | 0/4 = 0.0% | 0/4 = 0.0% | 4/4 = 100.0% | 4/4 = 100.0% | 4/4 = 100.0% | 4/4 = 100.0% |
---

## SVM summary (aggregate over 12 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 3 test case(s) generated once for the SPL.

**Equivalent-mutant treatment:** Inozemtseva &amp; Holmes (2014) — mutant not killed by ANY of the five suites (family + product state + product transition + product pair + random) is conservatively classified equivalent and EXCLUDED from the score denominator. Scores below are **killed / (total &minus; equivalent) = adjusted%**.

| Operator | Total mutants | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 86 | 0/86 = 0.0% | 52/86 = 60.5% | 74/86 = 86.0% | 86/86 = 100.0% | 86/86 = 100.0% | 86/86 = 100.0% |
| ActionExchange | 120 | 0/120 = 0.0% | 69/120 = 57.5% | 108/120 = 90.0% | 120/120 = 100.0% | 120/120 = 100.0% | 120/120 = 100.0% |
| StateMissing | 64 | 0/64 = 0.0% | 41/64 = 64.1% | 64/64 = 100.0% | 64/64 = 100.0% | 64/64 = 100.0% | 64/64 = 100.0% |

Total products: 12.
