# Per-Product Mutation Report — Elevator

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

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 8/22 = 36.4% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 11/22 = 50.0% |
| ActionExchange | 78 | 0/78 = 0.0% | 34/78 = 43.6% | 38/78 = 48.7% | 78/78 = 100.0% | 78/78 = 100.0% | 45/78 = 57.7% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 7/8 = 87.5% |

### Product 2

**Selected features:** selected = {Alarm, CardReader, ControlButtons}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 8/18 = 44.4% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |
| ActionExchange | 50 | 0/50 = 0.0% | 27/50 = 54.0% | 28/50 = 56.0% | 50/50 = 100.0% | 50/50 = 100.0% | 38/50 = 76.0% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 3

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 8/22 = 36.4% | 7/22 = 31.8% | 22/22 = 100.0% | 22/22 = 100.0% | 13/22 = 59.1% |
| ActionExchange | 71 | 0/71 = 0.0% | 32/71 = 45.1% | 32/71 = 45.1% | 71/71 = 100.0% | 71/71 = 100.0% | 51/71 = 71.8% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 4

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 30 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 12/28 = 42.9% | 9/28 = 32.1% | 28/28 = 100.0% | 28/28 = 100.0% | 15/28 = 53.6% |
| ActionExchange | 115 | 0/115 = 0.0% | 56/115 = 48.7% | 46/115 = 40.0% | 115/115 = 100.0% | 115/115 = 100.0% | 68/115 = 59.1% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 8/9 = 88.9% |

### Product 5

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 7/20 = 35.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |
| ActionExchange | 60 | 0/60 = 0.0% | 28/60 = 46.7% | 32/60 = 53.3% | 60/60 = 100.0% | 60/60 = 100.0% | 48/60 = 80.0% |
| StateMissing | 7 | 0/7 = 0.0% | 5/7 = 71.4% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 6

**Selected features:** selected = {ControlButtons, Intercom, PinPad}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 12 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 6/18 = 33.3% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |
| ActionExchange | 50 | 0/50 = 0.0% | 25/50 = 50.0% | 26/50 = 52.0% | 50/50 = 100.0% | 50/50 = 100.0% | 41/50 = 82.0% |
| StateMissing | 6 | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 7

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 8/28 = 28.6% | 11/28 = 39.3% | 28/28 = 100.0% | 28/28 = 100.0% | 18/28 = 64.3% |
| ActionExchange | 115 | 0/115 = 0.0% | 42/115 = 36.5% | 53/115 = 46.1% | 115/115 = 100.0% | 115/115 = 100.0% | 78/115 = 67.8% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 8/9 = 88.9% |

### Product 8

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom}

**Repaired FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 7/20 = 35.0% | 10/20 = 50.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |
| ActionExchange | 57 | 0/57 = 0.0% | 27/57 = 47.4% | 33/57 = 57.9% | 57/57 = 100.0% | 57/57 = 100.0% | 37/57 = 64.9% |
| StateMissing | 9 | 0/9 = 0.0% | 6/9 = 66.7% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 8/9 = 88.9% |

### Product 9

**Selected features:** selected = {Alarm, ControlButtons, MobileKey}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 8/18 = 44.4% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |
| ActionExchange | 50 | 0/50 = 0.0% | 28/50 = 56.0% | 26/50 = 52.0% | 50/50 = 100.0% | 50/50 = 100.0% | 37/50 = 74.0% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 10

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 28 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 11/26 = 42.3% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% | 11/26 = 42.3% |
| ActionExchange | 96 | 0/96 = 0.0% | 49/96 = 51.0% | 43/96 = 44.8% | 96/96 = 100.0% | 96/96 = 100.0% | 45/96 = 46.9% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 11

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 8/20 = 40.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |
| ActionExchange | 60 | 0/60 = 0.0% | 29/60 = 48.3% | 32/60 = 53.3% | 60/60 = 100.0% | 60/60 = 100.0% | 48/60 = 80.0% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 12

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Repaired FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 7/24 = 29.2% | 13/24 = 54.2% | 24/24 = 100.0% | 24/24 = 100.0% | 14/24 = 58.3% |
| ActionExchange | 87 | 0/87 = 0.0% | 35/87 = 40.2% | 55/87 = 63.2% | 87/87 = 100.0% | 87/87 = 100.0% | 61/87 = 70.1% |
| StateMissing | 11 | 0/11 = 0.0% | 6/11 = 54.5% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 13

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ManualDoorControl}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 8/22 = 36.4% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 78 | 0/78 = 0.0% | 35/78 = 44.9% | 36/78 = 46.2% | 78/78 = 100.0% | 78/78 = 100.0% | 51/78 = 65.4% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 14

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 7/20 = 35.0% | 8/20 = 40.0% | 20/20 = 100.0% | 20/20 = 100.0% | 14/20 = 70.0% |
| ActionExchange | 60 | 0/60 = 0.0% | 30/60 = 50.0% | 36/60 = 60.0% | 60/60 = 100.0% | 60/60 = 100.0% | 49/60 = 81.7% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 15

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 8/19 = 42.1% | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 60 | 0/60 = 0.0% | 30/60 = 50.0% | 30/60 = 50.0% | 60/60 = 100.0% | 60/60 = 100.0% | 43/60 = 71.7% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 16

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom}

**Repaired FTS:** 10 states, 25 transitions (21 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 10/21 = 47.6% | 10/21 = 47.6% | 21/21 = 100.0% | 21/21 = 100.0% | 12/21 = 57.1% |
| ActionExchange | 68 | 0/68 = 0.0% | 36/68 = 52.9% | 36/68 = 52.9% | 68/68 = 100.0% | 68/68 = 100.0% | 45/68 = 66.2% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 8/9 = 88.9% |

### Product 17

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService}

**Repaired FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 10/20 = 50.0% | 10/20 = 50.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |
| ActionExchange | 57 | 0/57 = 0.0% | 33/57 = 57.9% | 33/57 = 57.9% | 57/57 = 100.0% | 57/57 = 100.0% | 37/57 = 64.9% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 8/9 = 88.9% |

### Product 18

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 13 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 7/22 = 31.8% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 78 | 0/78 = 0.0% | 32/78 = 41.0% | 37/78 = 47.4% | 78/78 = 100.0% | 78/78 = 100.0% | 51/78 = 65.4% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 19

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, MobileKey}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 24 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 9/20 = 45.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |
| ActionExchange | 60 | 0/60 = 0.0% | 37/60 = 61.7% | 30/60 = 50.0% | 60/60 = 100.0% | 60/60 = 100.0% | 44/60 = 73.3% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 20

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 8/26 = 30.8% | 11/26 = 42.3% | 26/26 = 100.0% | 26/26 = 100.0% | 16/26 = 61.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 39/96 = 40.6% | 49/96 = 51.0% | 96/96 = 100.0% | 96/96 = 100.0% | 62/96 = 64.6% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |

### Product 21

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, ManualDoorControl}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 8/26 = 30.8% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% | 16/26 = 61.5% |
| ActionExchange | 96 | 0/96 = 0.0% | 37/96 = 38.5% | 41/96 = 42.7% | 96/96 = 100.0% | 96/96 = 100.0% | 62/96 = 64.6% |
| StateMissing | 9 | 0/9 = 0.0% | 6/9 = 66.7% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |

### Product 22

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 25 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 9/20 = 45.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |
| ActionExchange | 60 | 0/60 = 0.0% | 37/60 = 61.7% | 30/60 = 50.0% | 60/60 = 100.0% | 60/60 = 100.0% | 44/60 = 73.3% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 23

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, ManualDoorControl}

**Repaired FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 10/24 = 41.7% | 13/24 = 54.2% | 24/24 = 100.0% | 24/24 = 100.0% | 14/24 = 58.3% |
| ActionExchange | 87 | 0/87 = 0.0% | 45/87 = 51.7% | 55/87 = 63.2% | 87/87 = 100.0% | 87/87 = 100.0% | 61/87 = 70.1% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 8/11 = 72.7% |

### Product 24

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 29 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 11/26 = 42.3% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% | 11/26 = 42.3% |
| ActionExchange | 96 | 0/96 = 0.0% | 49/96 = 51.0% | 43/96 = 44.8% | 96/96 = 100.0% | 96/96 = 100.0% | 45/96 = 46.9% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 25

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 8/28 = 28.6% | 9/28 = 32.1% | 28/28 = 100.0% | 28/28 = 100.0% | 18/28 = 64.3% |
| ActionExchange | 115 | 0/115 = 0.0% | 40/115 = 34.8% | 44/115 = 38.3% | 115/115 = 100.0% | 115/115 = 100.0% | 80/115 = 69.6% |
| StateMissing | 9 | 0/9 = 0.0% | 6/9 = 66.7% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 8/9 = 88.9% |

### Product 26

**Selected features:** selected = {CardReader, ControlButtons, Intercom}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 7/18 = 38.9% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |
| ActionExchange | 50 | 0/50 = 0.0% | 26/50 = 52.0% | 28/50 = 56.0% | 50/50 = 100.0% | 50/50 = 100.0% | 38/50 = 76.0% |
| StateMissing | 6 | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 27

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Repaired FTS:** 12 states, 31 transitions (25 real / 6 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 10/25 = 40.0% | 13/25 = 52.0% | 25/25 = 100.0% | 25/25 = 100.0% | 15/25 = 60.0% |
| ActionExchange | 102 | 0/102 = 0.0% | 48/102 = 47.1% | 60/102 = 58.8% | 102/102 = 100.0% | 102/102 = 100.0% | 71/102 = 69.6% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 28

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom, ManualDoorControl}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 8/23 = 34.8% | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% | 16/23 = 69.6% |
| ActionExchange | 92 | 0/92 = 0.0% | 38/92 = 41.3% | 39/92 = 42.4% | 92/92 = 100.0% | 92/92 = 100.0% | 65/92 = 70.7% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 29

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 7/26 = 26.9% | 11/26 = 42.3% | 26/26 = 100.0% | 26/26 = 100.0% | 14/26 = 53.8% |
| ActionExchange | 96 | 0/96 = 0.0% | 36/96 = 37.5% | 49/96 = 51.0% | 96/96 = 100.0% | 96/96 = 100.0% | 58/96 = 60.4% |
| StateMissing | 9 | 0/9 = 0.0% | 6/9 = 66.7% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 30

**Selected features:** selected = {CardReader, ControlButtons, Intercom, ManualDoorControl}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 7/22 = 31.8% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 78 | 0/78 = 0.0% | 32/78 = 41.0% | 36/78 = 46.2% | 78/78 = 100.0% | 78/78 = 100.0% | 51/78 = 65.4% |
| StateMissing | 8 | 0/8 = 0.0% | 5/8 = 62.5% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 31

**Selected features:** selected = {Alarm, ControlButtons, Intercom, PinPad}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 7/19 = 36.8% | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 60 | 0/60 = 0.0% | 28/60 = 46.7% | 28/60 = 46.7% | 60/60 = 100.0% | 60/60 = 100.0% | 44/60 = 73.3% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 32

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 16 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 7/22 = 31.8% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 11/22 = 50.0% |
| ActionExchange | 78 | 0/78 = 0.0% | 32/78 = 41.0% | 38/78 = 48.7% | 78/78 = 100.0% | 78/78 = 100.0% | 45/78 = 57.7% |
| StateMissing | 8 | 0/8 = 0.0% | 5/8 = 62.5% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 7/8 = 87.5% |

### Product 33

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 7/26 = 26.9% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% | 14/26 = 53.8% |
| ActionExchange | 96 | 0/96 = 0.0% | 34/96 = 35.4% | 41/96 = 42.7% | 96/96 = 100.0% | 96/96 = 100.0% | 58/96 = 60.4% |
| StateMissing | 9 | 0/9 = 0.0% | 5/9 = 55.6% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 34

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 26 real step(s) applicable.

**Random baseline:** 7 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 10/22 = 45.5% | 7/22 = 31.8% | 22/22 = 100.0% | 22/22 = 100.0% | 11/22 = 50.0% |
| ActionExchange | 71 | 0/71 = 0.0% | 40/71 = 56.3% | 34/71 = 47.9% | 71/71 = 100.0% | 71/71 = 100.0% | 44/71 = 62.0% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 5/7 = 71.4% |

### Product 35

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, PinPad}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 8/20 = 40.0% | 8/20 = 40.0% | 20/20 = 100.0% | 20/20 = 100.0% | 14/20 = 70.0% |
| ActionExchange | 60 | 0/60 = 0.0% | 31/60 = 51.7% | 36/60 = 60.0% | 60/60 = 100.0% | 60/60 = 100.0% | 49/60 = 81.7% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 36

**Selected features:** selected = {Alarm, ControlButtons, PinPad}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 7/18 = 38.9% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |
| ActionExchange | 50 | 0/50 = 0.0% | 26/50 = 52.0% | 26/50 = 52.0% | 50/50 = 100.0% | 50/50 = 100.0% | 41/50 = 82.0% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 37

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 8/22 = 36.4% | 8/22 = 36.4% | 22/22 = 100.0% | 22/22 = 100.0% | 13/22 = 59.1% |
| ActionExchange | 71 | 0/71 = 0.0% | 34/71 = 47.9% | 37/71 = 52.1% | 71/71 = 100.0% | 71/71 = 100.0% | 52/71 = 73.2% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 38

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 12 real step(s) applicable.

**Random baseline:** 4 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 6/22 = 27.3% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 78 | 0/78 = 0.0% | 29/78 = 37.2% | 37/78 = 47.4% | 78/78 = 100.0% | 78/78 = 100.0% | 51/78 = 65.4% |
| StateMissing | 8 | 0/8 = 0.0% | 5/8 = 62.5% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 39

**Selected features:** selected = {Alarm, ControlButtons, Intercom, MobileKey}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

**Random baseline:** 6 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 8/19 = 42.1% | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% | 9/19 = 47.4% |
| ActionExchange | 60 | 0/60 = 0.0% | 30/60 = 50.0% | 28/60 = 46.7% | 60/60 = 100.0% | 60/60 = 100.0% | 40/60 = 66.7% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 5/6 = 83.3% |

### Product 40

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 7/23 = 30.4% | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% | 16/23 = 69.6% |
| ActionExchange | 92 | 0/92 = 0.0% | 34/92 = 37.0% | 40/92 = 43.5% | 92/92 = 100.0% | 92/92 = 100.0% | 65/92 = 70.7% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 41

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 8/23 = 34.8% | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% | 13/23 = 56.5% |
| ActionExchange | 92 | 0/92 = 0.0% | 36/92 = 39.1% | 41/92 = 44.6% | 92/92 = 100.0% | 92/92 = 100.0% | 62/92 = 67.4% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 7/8 = 87.5% |

### Product 42

**Selected features:** selected = {ControlButtons, Intercom, MobileKey}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 7/18 = 38.9% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |
| ActionExchange | 50 | 0/50 = 0.0% | 28/50 = 56.0% | 26/50 = 52.0% | 50/50 = 100.0% | 50/50 = 100.0% | 37/50 = 74.0% |
| StateMissing | 6 | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |
---

## Elevator summary (aggregate over 42 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 9 test case(s) generated once for the SPL.

**Equivalent-mutant treatment:** Inozemtseva &amp; Holmes (2014) — mutant not killed by ANY of the five suites (family + product state + product transition + product pair + random) is conservatively classified equivalent and EXCLUDED from the score denominator. Scores below are **killed / (total &minus; equivalent) = adjusted%**.

| Operator | Total mutants | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 926 | 0/926 = 0.0% | 340/926 = 36.7% | 366/926 = 39.5% | 926/926 = 100.0% | 926/926 = 100.0% | 547/926 = 59.1% |
| ActionExchange | 3176 | 0/3176 = 0.0% | 1454/3176 = 45.8% | 1568/3176 = 49.4% | 3176/3176 = 100.0% | 3176/3176 = 100.0% | 2142/3176 = 67.4% |
| StateMissing | 330 | 0/330 = 0.0% | 272/330 = 82.4% | 330/330 = 100.0% | 330/330 = 100.0% | 330/330 = 100.0% | 302/330 = 91.5% |

Total products: 42.
