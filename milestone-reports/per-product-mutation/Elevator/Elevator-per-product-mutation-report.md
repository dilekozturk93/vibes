# Per-Product Mutation Report — Elevator

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

**Selected features:** selected = {Alarm, ControlButtons, PinPad}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 7/18 = 38.9% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 8/18 = 44.4% |
| ActionExchange | 144 | 0/144 = 0.0% | 57/144 = 39.6% | 58/144 = 40.3% | 144/144 = 100.0% | 144/144 = 100.0% | 68/144 = 47.2% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 5/6 = 83.3% |

### Product 2

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 30 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 12/28 = 42.9% | 9/28 = 32.1% | 28/28 = 100.0% | 28/28 = 100.0% | 10/28 = 35.7% |
| ActionExchange | 336 | 0/336 = 0.0% | 147/336 = 43.8% | 114/336 = 33.9% | 336/336 = 100.0% | 336/336 = 100.0% | 122/336 = 36.3% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 3

**Selected features:** selected = {Alarm, CardReader, ControlButtons}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 8/18 = 44.4% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |
| ActionExchange | 144 | 0/144 = 0.0% | 65/144 = 45.1% | 60/144 = 41.7% | 144/144 = 100.0% | 144/144 = 100.0% | 76/144 = 52.8% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 5/6 = 83.3% |

### Product 4

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 8/28 = 28.6% | 11/28 = 39.3% | 28/28 = 100.0% | 28/28 = 100.0% | 12/28 = 42.9% |
| ActionExchange | 336 | 0/336 = 0.0% | 97/336 = 28.9% | 136/336 = 40.5% | 336/336 = 100.0% | 336/336 = 100.0% | 150/336 = 44.6% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 5

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 7/23 = 30.4% | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% | 12/23 = 52.2% |
| ActionExchange | 253 | 0/253 = 0.0% | 78/253 = 30.8% | 103/253 = 40.7% | 253/253 = 100.0% | 253/253 = 100.0% | 135/253 = 53.4% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 7/8 = 87.5% |

### Product 6

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Repaired FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 7/24 = 29.2% | 13/24 = 54.2% | 24/24 = 100.0% | 24/24 = 100.0% | 15/24 = 62.5% |
| ActionExchange | 312 | 0/312 = 0.0% | 95/312 = 30.4% | 176/312 = 56.4% | 312/312 = 100.0% | 312/312 = 100.0% | 198/312 = 63.5% |
| StateMissing | 11 | 0/11 = 0.0% | 6/11 = 54.5% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% |

### Product 7

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService}

**Repaired FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 10/20 = 50.0% | 10/20 = 50.0% | 20/20 = 100.0% | 20/20 = 100.0% | 12/20 = 60.0% |
| ActionExchange | 220 | 0/220 = 0.0% | 113/220 = 51.4% | 113/220 = 51.4% | 220/220 = 100.0% | 220/220 = 100.0% | 135/220 = 61.4% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 8/9 = 88.9% |

### Product 8

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 26 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 10/22 = 45.5% | 7/22 = 31.8% | 22/22 = 100.0% | 22/22 = 100.0% | 12/22 = 54.5% |
| ActionExchange | 220 | 0/220 = 0.0% | 103/220 = 46.8% | 74/220 = 33.6% | 220/220 = 100.0% | 220/220 = 100.0% | 123/220 = 55.9% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 9

**Selected features:** selected = {CardReader, ControlButtons, Intercom, ManualDoorControl}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 7/22 = 31.8% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 11/22 = 50.0% |
| ActionExchange | 220 | 0/220 = 0.0% | 70/220 = 31.8% | 93/220 = 42.3% | 220/220 = 100.0% | 220/220 = 100.0% | 114/220 = 51.8% |
| StateMissing | 8 | 0/8 = 0.0% | 5/8 = 62.5% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 7/8 = 87.5% |

### Product 10

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 7/20 = 35.0% | 8/20 = 40.0% | 20/20 = 100.0% | 20/20 = 100.0% | 11/20 = 55.0% |
| ActionExchange | 180 | 0/180 = 0.0% | 63/180 = 35.0% | 76/180 = 42.2% | 180/180 = 100.0% | 180/180 = 100.0% | 102/180 = 56.7% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% |

### Product 11

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Repaired FTS:** 12 states, 31 transitions (25 real / 6 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 25 | 0/25 = 0.0% | 10/25 = 40.0% | 13/25 = 52.0% | 25/25 = 100.0% | 25/25 = 100.0% | 17/25 = 68.0% |
| ActionExchange | 350 | 0/350 = 0.0% | 147/350 = 42.0% | 189/350 = 54.0% | 350/350 = 100.0% | 350/350 = 100.0% | 246/350 = 70.3% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 12

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 29 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 11/26 = 42.3% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% | 9/26 = 34.6% |
| ActionExchange | 286 | 0/286 = 0.0% | 124/286 = 43.4% | 105/286 = 36.7% | 286/286 = 100.0% | 286/286 = 100.0% | 104/286 = 36.4% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 13

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 28 | 0/28 = 0.0% | 8/28 = 28.6% | 9/28 = 32.1% | 28/28 = 100.0% | 28/28 = 100.0% | 10/28 = 35.7% |
| ActionExchange | 336 | 0/336 = 0.0% | 97/336 = 28.9% | 112/336 = 33.3% | 336/336 = 100.0% | 336/336 = 100.0% | 127/336 = 37.8% |
| StateMissing | 9 | 0/9 = 0.0% | 6/9 = 66.7% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 14

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 8/20 = 40.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |
| ActionExchange | 180 | 0/180 = 0.0% | 73/180 = 40.6% | 67/180 = 37.2% | 180/180 = 100.0% | 180/180 = 100.0% | 83/180 = 46.1% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 5/7 = 71.4% |

### Product 15

**Selected features:** selected = {Alarm, ControlButtons, Intercom, MobileKey}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 8/19 = 42.1% | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% | 10/19 = 52.6% |
| ActionExchange | 171 | 0/171 = 0.0% | 72/171 = 42.1% | 65/171 = 38.0% | 171/171 = 100.0% | 171/171 = 100.0% | 92/171 = 53.8% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 5/6 = 83.3% |

### Product 16

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 7/22 = 31.8% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 9/22 = 40.9% |
| ActionExchange | 220 | 0/220 = 0.0% | 70/220 = 31.8% | 95/220 = 43.2% | 220/220 = 100.0% | 220/220 = 100.0% | 93/220 = 42.3% |
| StateMissing | 8 | 0/8 = 0.0% | 5/8 = 62.5% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 5/8 = 62.5% |

### Product 17

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 25 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 9/20 = 45.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% | 10/20 = 50.0% |
| ActionExchange | 180 | 0/180 = 0.0% | 84/180 = 46.7% | 65/180 = 36.1% | 180/180 = 100.0% | 180/180 = 100.0% | 92/180 = 51.1% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 6/7 = 85.7% |

### Product 18

**Selected features:** selected = {CardReader, ControlButtons, Intercom}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 7/18 = 38.9% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |
| ActionExchange | 144 | 0/144 = 0.0% | 56/144 = 38.9% | 60/144 = 41.7% | 144/144 = 100.0% | 144/144 = 100.0% | 76/144 = 52.8% |
| StateMissing | 6 | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 19

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 28 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 11/26 = 42.3% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% | 14/26 = 53.8% |
| ActionExchange | 286 | 0/286 = 0.0% | 124/286 = 43.4% | 105/286 = 36.7% | 286/286 = 100.0% | 286/286 = 100.0% | 158/286 = 55.2% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% |

### Product 20

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, PinPad}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 8/20 = 40.0% | 8/20 = 40.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |
| ActionExchange | 180 | 0/180 = 0.0% | 73/180 = 40.6% | 76/180 = 42.2% | 180/180 = 100.0% | 180/180 = 100.0% | 85/180 = 47.2% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 6/7 = 85.7% |

### Product 21

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom}

**Repaired FTS:** 10 states, 25 transitions (21 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 21 | 0/21 = 0.0% | 10/21 = 47.6% | 10/21 = 47.6% | 21/21 = 100.0% | 21/21 = 100.0% | 14/21 = 66.7% |
| ActionExchange | 252 | 0/252 = 0.0% | 123/252 = 48.8% | 123/252 = 48.8% | 252/252 = 100.0% | 252/252 = 100.0% | 172/252 = 68.3% |
| StateMissing | 9 | 0/9 = 0.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 22

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, ManualDoorControl}

**Repaired FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 24 | 0/24 = 0.0% | 10/24 = 41.7% | 13/24 = 54.2% | 24/24 = 100.0% | 24/24 = 100.0% | 16/24 = 66.7% |
| ActionExchange | 312 | 0/312 = 0.0% | 137/312 = 43.9% | 176/312 = 56.4% | 312/312 = 100.0% | 312/312 = 100.0% | 216/312 = 69.2% |
| StateMissing | 11 | 0/11 = 0.0% | 9/11 = 81.8% | 11/11 = 100.0% | 11/11 = 100.0% | 11/11 = 100.0% | 9/11 = 81.8% |

### Product 23

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 8/23 = 34.8% | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% | 14/23 = 60.9% |
| ActionExchange | 253 | 0/253 = 0.0% | 88/253 = 34.8% | 104/253 = 41.1% | 253/253 = 100.0% | 253/253 = 100.0% | 158/253 = 62.5% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 7/8 = 87.5% |

### Product 24

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 8/22 = 36.4% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 8/22 = 36.4% |
| ActionExchange | 220 | 0/220 = 0.0% | 80/220 = 36.4% | 95/220 = 43.2% | 220/220 = 100.0% | 220/220 = 100.0% | 82/220 = 37.3% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 5/8 = 62.5% |

### Product 25

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ManualDoorControl}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 8/22 = 36.4% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 13/22 = 59.1% |
| ActionExchange | 220 | 0/220 = 0.0% | 81/220 = 36.8% | 93/220 = 42.3% | 220/220 = 100.0% | 220/220 = 100.0% | 135/220 = 61.4% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 7/8 = 87.5% |

### Product 26

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom}

**Repaired FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 7/20 = 35.0% | 10/20 = 50.0% | 20/20 = 100.0% | 20/20 = 100.0% | 13/20 = 65.0% |
| ActionExchange | 220 | 0/220 = 0.0% | 79/220 = 35.9% | 113/220 = 51.4% | 220/220 = 100.0% | 220/220 = 100.0% | 144/220 = 65.5% |
| StateMissing | 9 | 0/9 = 0.0% | 6/9 = 66.7% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 8/9 = 88.9% |

### Product 27

**Selected features:** selected = {Alarm, ControlButtons, MobileKey}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 8/18 = 44.4% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 10/18 = 55.6% |
| ActionExchange | 144 | 0/144 = 0.0% | 64/144 = 44.4% | 58/144 = 40.3% | 144/144 = 100.0% | 144/144 = 100.0% | 82/144 = 56.9% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 5/6 = 83.3% |

### Product 28

**Selected features:** selected = {ControlButtons, Intercom, MobileKey}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 16 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 7/18 = 38.9% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 9/18 = 50.0% |
| ActionExchange | 144 | 0/144 = 0.0% | 56/144 = 38.9% | 58/144 = 40.3% | 144/144 = 100.0% | 144/144 = 100.0% | 76/144 = 52.8% |
| StateMissing | 6 | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 29

**Selected features:** selected = {ControlButtons, Intercom, PinPad}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 12 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 18 | 0/18 = 0.0% | 6/18 = 33.3% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% | 12/18 = 66.7% |
| ActionExchange | 144 | 0/144 = 0.0% | 48/144 = 33.3% | 58/144 = 40.3% | 144/144 = 100.0% | 144/144 = 100.0% | 100/144 = 69.4% |
| StateMissing | 6 | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |

### Product 30

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, MobileKey}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 24 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 9/20 = 45.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% | 9/20 = 45.0% |
| ActionExchange | 180 | 0/180 = 0.0% | 84/180 = 46.7% | 65/180 = 36.1% | 180/180 = 100.0% | 180/180 = 100.0% | 85/180 = 47.2% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 6/7 = 85.7% |

### Product 31

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 8/22 = 36.4% | 7/22 = 31.8% | 22/22 = 100.0% | 22/22 = 100.0% | 11/22 = 50.0% |
| ActionExchange | 220 | 0/220 = 0.0% | 81/220 = 36.8% | 72/220 = 32.7% | 220/220 = 100.0% | 220/220 = 100.0% | 113/220 = 51.4% |
| StateMissing | 7 | 0/7 = 0.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 6/7 = 85.7% |

### Product 32

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 7/26 = 26.9% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% | 11/26 = 42.3% |
| ActionExchange | 286 | 0/286 = 0.0% | 77/286 = 26.9% | 103/286 = 36.0% | 286/286 = 100.0% | 286/286 = 100.0% | 122/286 = 42.7% |
| StateMissing | 9 | 0/9 = 0.0% | 5/9 = 55.6% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 33

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom, ManualDoorControl}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 23 | 0/23 = 0.0% | 8/23 = 34.8% | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% | 11/23 = 47.8% |
| ActionExchange | 253 | 0/253 = 0.0% | 89/253 = 35.2% | 102/253 = 40.3% | 253/253 = 100.0% | 253/253 = 100.0% | 126/253 = 49.8% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 6/8 = 75.0% |

### Product 34

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, ManualDoorControl}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 8/26 = 30.8% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% | 12/26 = 46.2% |
| ActionExchange | 286 | 0/286 = 0.0% | 89/286 = 31.1% | 103/286 = 36.0% | 286/286 = 100.0% | 286/286 = 100.0% | 135/286 = 47.2% |
| StateMissing | 9 | 0/9 = 0.0% | 6/9 = 66.7% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 7/9 = 77.8% |

### Product 35

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 20 | 0/20 = 0.0% | 7/20 = 35.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% | 11/20 = 55.0% |
| ActionExchange | 180 | 0/180 = 0.0% | 63/180 = 35.0% | 67/180 = 37.2% | 180/180 = 100.0% | 180/180 = 100.0% | 103/180 = 57.2% |
| StateMissing | 7 | 0/7 = 0.0% | 5/7 = 71.4% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 6/7 = 85.7% |

### Product 36

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 8/19 = 42.1% | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% | 10/19 = 52.6% |
| ActionExchange | 171 | 0/171 = 0.0% | 73/171 = 42.7% | 67/171 = 39.2% | 171/171 = 100.0% | 171/171 = 100.0% | 93/171 = 54.4% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 5/6 = 83.3% |

### Product 37

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 8/26 = 30.8% | 11/26 = 42.3% | 26/26 = 100.0% | 26/26 = 100.0% | 10/26 = 38.5% |
| ActionExchange | 286 | 0/286 = 0.0% | 89/286 = 31.1% | 125/286 = 43.7% | 286/286 = 100.0% | 286/286 = 100.0% | 112/286 = 39.2% |
| StateMissing | 9 | 0/9 = 0.0% | 7/9 = 77.8% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 38

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 7/22 = 31.8% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 13/22 = 59.1% |
| ActionExchange | 220 | 0/220 = 0.0% | 71/220 = 32.3% | 94/220 = 42.7% | 220/220 = 100.0% | 220/220 = 100.0% | 136/220 = 61.8% |
| StateMissing | 8 | 0/8 = 0.0% | 6/8 = 75.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 39

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 12 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 6/22 = 27.3% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% | 14/22 = 63.6% |
| ActionExchange | 220 | 0/220 = 0.0% | 60/220 = 27.3% | 94/220 = 42.7% | 220/220 = 100.0% | 220/220 = 100.0% | 144/220 = 65.5% |
| StateMissing | 8 | 0/8 = 0.0% | 5/8 = 62.5% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% | 8/8 = 100.0% |

### Product 40

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 26 | 0/26 = 0.0% | 7/26 = 26.9% | 11/26 = 42.3% | 26/26 = 100.0% | 26/26 = 100.0% | 8/26 = 30.8% |
| ActionExchange | 286 | 0/286 = 0.0% | 77/286 = 26.9% | 125/286 = 43.7% | 286/286 = 100.0% | 286/286 = 100.0% | 93/286 = 32.5% |
| StateMissing | 9 | 0/9 = 0.0% | 6/9 = 66.7% | 9/9 = 100.0% | 9/9 = 100.0% | 9/9 = 100.0% | 6/9 = 66.7% |

### Product 41

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 22 | 0/22 = 0.0% | 8/22 = 36.4% | 8/22 = 36.4% | 22/22 = 100.0% | 22/22 = 100.0% | 13/22 = 59.1% |
| ActionExchange | 220 | 0/220 = 0.0% | 81/220 = 36.8% | 83/220 = 37.7% | 220/220 = 100.0% | 220/220 = 100.0% | 134/220 = 60.9% |
| StateMissing | 7 | 0/7 = 0.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 7/7 = 100.0% | 6/7 = 85.7% |

### Product 42

**Selected features:** selected = {Alarm, ControlButtons, Intercom, PinPad}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 13 real step(s) applicable.

**Random baseline:** 5 test case(s).

Scores below: **killed / non-equivalent = adjusted%** (Inozemtseva & Holmes 2014 treatment — mutant not killed by ANY of five suites is equivalent and excluded from denominator).

| Operator | Total | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 19 | 0/19 = 0.0% | 7/19 = 36.8% | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% | 12/19 = 63.2% |
| ActionExchange | 171 | 0/171 = 0.0% | 64/171 = 37.4% | 65/171 = 38.0% | 171/171 = 100.0% | 171/171 = 100.0% | 111/171 = 64.9% |
| StateMissing | 6 | 0/6 = 0.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% | 6/6 = 100.0% |
---

## Elevator summary (aggregate over 42 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 9 test case(s) generated once for the SPL.

**Equivalent-mutant treatment:** Inozemtseva &amp; Holmes (2014) — mutant not killed by ANY of the five suites (family + product state + product transition + product pair + random) is conservatively classified equivalent and EXCLUDED from the score denominator. Scores below are **killed / (total &minus; equivalent) = adjusted%**.

| Operator | Total mutants | Equivalent | Family (Devroey) | Product state-cov | Product transition-cov | Product pair-cov | Random |
|---|---|---|---|---|---|---|---|
| TransitionMissing | 926 | 0/926 = 0.0% | 340/926 = 36.7% | 366/926 = 39.5% | 926/926 = 100.0% | 926/926 = 100.0% | 472/926 = 51.0% |
| ActionExchange | 9586 | 0/9586 = 0.0% | 3562/9586 = 37.2% | 3985/9586 = 41.6% | 9586/9586 = 100.0% | 9586/9586 = 100.0% | 5051/9586 = 52.7% |
| StateMissing | 330 | 0/330 = 0.0% | 272/330 = 82.4% | 330/330 = 100.0% | 330/330 = 100.0% | 330/330 = 100.0% | 275/330 = 83.3% |

Total products: 42.
