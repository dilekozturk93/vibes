# Per-Product Mutation Report — SVM

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

**Selected features:** selected = {c, f, s, t}

**Repaired FTS:** 6 states, 8 transitions (8 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 8 | 0/8 = 0.0% | 7/8 = 87.5% | 8/8 = 100.0% | 8/8 = 100.0% |
| ActionExchange | 56 | 0/56 = 0.0% | 49/56 = 87.5% | 56/56 = 100.0% | 56/56 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (8):

- `TM__state4__serveSoda__state7`
- `TM__state5__serveTea__state7`
- `TM__state7__take__state1`
- `TM__state3__cancel__state6`
- `TM__state1__free__state3`
- `TM__state6__return__state1`
- `TM__state3__tea__state5`
- `TM__state3__soda__state4`

**TransitionMissing — survived product state coverage** (1):

- `TM__state6__return__state1`

**ActionExchange — survived family-level state coverage (Devroey)** (56):

- `AEX__state4__serveSoda__cancel__state7`
- `AEX__state4__serveSoda__take__state7`
- `AEX__state4__serveSoda__tea__state7`
- `AEX__state4__serveSoda__serveTea__state7`
- `AEX__state4__serveSoda__free__state7`
- `AEX__state4__serveSoda__soda__state7`
- `AEX__state4__serveSoda__return__state7`
- `AEX__state5__serveTea__cancel__state7`
- `AEX__state5__serveTea__take__state7`
- `AEX__state5__serveTea__tea__state7`
- `AEX__state5__serveTea__serveSoda__state7`
- `AEX__state5__serveTea__free__state7`
- `AEX__state5__serveTea__soda__state7`
- `AEX__state5__serveTea__return__state7`
- `AEX__state7__take__cancel__state1`
- `AEX__state7__take__tea__state1`
- `AEX__state7__take__serveTea__state1`
- `AEX__state7__take__serveSoda__state1`
- `AEX__state7__take__free__state1`
- `AEX__state7__take__soda__state1`
- `AEX__state7__take__return__state1`
- `AEX__state3__cancel__take__state6`
- `AEX__state3__cancel__tea__state6`
- `AEX__state3__cancel__serveTea__state6`
- `AEX__state3__cancel__serveSoda__state6`
- `AEX__state3__cancel__free__state6`
- `AEX__state3__cancel__soda__state6`
- `AEX__state3__cancel__return__state6`
- `AEX__state1__free__cancel__state3`
- `AEX__state1__free__take__state3`
- `AEX__state1__free__tea__state3`
- `AEX__state1__free__serveTea__state3`
- `AEX__state1__free__serveSoda__state3`
- `AEX__state1__free__soda__state3`
- `AEX__state1__free__return__state3`
- `AEX__state6__return__cancel__state1`
- `AEX__state6__return__take__state1`
- `AEX__state6__return__tea__state1`
- `AEX__state6__return__serveTea__state1`
- `AEX__state6__return__serveSoda__state1`
- `AEX__state6__return__free__state1`
- `AEX__state6__return__soda__state1`
- `AEX__state3__tea__cancel__state5`
- `AEX__state3__tea__take__state5`
- `AEX__state3__tea__serveTea__state5`
- `AEX__state3__tea__serveSoda__state5`
- `AEX__state3__tea__free__state5`
- `AEX__state3__tea__soda__state5`
- `AEX__state3__tea__return__state5`
- `AEX__state3__soda__cancel__state4`
- `AEX__state3__soda__take__state4`
- `AEX__state3__soda__tea__state4`
- `AEX__state3__soda__serveTea__state4`
- `AEX__state3__soda__serveSoda__state4`
- `AEX__state3__soda__free__state4`
- `AEX__state3__soda__return__state4`

**ActionExchange — survived product state coverage** (7):

- `AEX__state6__return__cancel__state1`
- `AEX__state6__return__take__state1`
- `AEX__state6__return__tea__state1`
- `AEX__state6__return__serveTea__state1`
- `AEX__state6__return__serveSoda__state1`
- `AEX__state6__return__free__state1`
- `AEX__state6__return__soda__state1`

### Product 2

**Selected features:** selected = {t}

**Repaired FTS:** 7 states, 7 transitions (7 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 11 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 7 | 7/7 = 100.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% |
| ActionExchange | 42 | 42/42 = 100.0% | 36/42 = 85.7% | 42/42 = 100.0% | 42/42 = 100.0% |

**TransitionMissing — survived product state coverage** (1):

- `TM__state9__close__state1`

**ActionExchange — survived product state coverage** (6):

- `AEX__state9__close__take__state1`
- `AEX__state9__close__tea__state1`
- `AEX__state9__close__serveTea__state1`
- `AEX__state9__close__change__state1`
- `AEX__state9__close__pay__state1`
- `AEX__state9__close__open__state1`

### Product 3

**Selected features:** selected = {f, t}

**Repaired FTS:** 4 states, 4 transitions (4 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 4 | 0/4 = 0.0% | 3/4 = 75.0% | 4/4 = 100.0% | 4/4 = 100.0% |
| ActionExchange | 12 | 0/12 = 0.0% | 9/12 = 75.0% | 12/12 = 100.0% | 12/12 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (4):

- `TM__state5__serveTea__state7`
- `TM__state7__take__state1`
- `TM__state1__free__state3`
- `TM__state3__tea__state5`

**TransitionMissing — survived product state coverage** (1):

- `TM__state7__take__state1`

**ActionExchange — survived family-level state coverage (Devroey)** (12):

- `AEX__state5__serveTea__take__state7`
- `AEX__state5__serveTea__tea__state7`
- `AEX__state5__serveTea__free__state7`
- `AEX__state7__take__tea__state1`
- `AEX__state7__take__serveTea__state1`
- `AEX__state7__take__free__state1`
- `AEX__state1__free__take__state3`
- `AEX__state1__free__tea__state3`
- `AEX__state1__free__serveTea__state3`
- `AEX__state3__tea__take__state5`
- `AEX__state3__tea__serveTea__state5`
- `AEX__state3__tea__free__state5`

**ActionExchange — survived product state coverage** (3):

- `AEX__state7__take__tea__state1`
- `AEX__state7__take__serveTea__state1`
- `AEX__state7__take__free__state1`

### Product 4

**Selected features:** selected = {c, f, t}

**Repaired FTS:** 5 states, 6 transitions (6 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 6 | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% |
| ActionExchange | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 30/30 = 100.0% | 30/30 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (6):

- `TM__state5__serveTea__state7`
- `TM__state7__take__state1`
- `TM__state3__cancel__state6`
- `TM__state1__free__state3`
- `TM__state6__return__state1`
- `TM__state3__tea__state5`

**TransitionMissing — survived product state coverage** (1):

- `TM__state6__return__state1`

**ActionExchange — survived family-level state coverage (Devroey)** (30):

- `AEX__state5__serveTea__cancel__state7`
- `AEX__state5__serveTea__take__state7`
- `AEX__state5__serveTea__tea__state7`
- `AEX__state5__serveTea__free__state7`
- `AEX__state5__serveTea__return__state7`
- `AEX__state7__take__cancel__state1`
- `AEX__state7__take__tea__state1`
- `AEX__state7__take__serveTea__state1`
- `AEX__state7__take__free__state1`
- `AEX__state7__take__return__state1`
- `AEX__state3__cancel__take__state6`
- `AEX__state3__cancel__tea__state6`
- `AEX__state3__cancel__serveTea__state6`
- `AEX__state3__cancel__free__state6`
- `AEX__state3__cancel__return__state6`
- `AEX__state1__free__cancel__state3`
- `AEX__state1__free__take__state3`
- `AEX__state1__free__tea__state3`
- `AEX__state1__free__serveTea__state3`
- `AEX__state1__free__return__state3`
- `AEX__state6__return__cancel__state1`
- `AEX__state6__return__take__state1`
- `AEX__state6__return__tea__state1`
- `AEX__state6__return__serveTea__state1`
- `AEX__state6__return__free__state1`
- `AEX__state3__tea__cancel__state5`
- `AEX__state3__tea__take__state5`
- `AEX__state3__tea__serveTea__state5`
- `AEX__state3__tea__free__state5`
- `AEX__state3__tea__return__state5`

**ActionExchange — survived product state coverage** (5):

- `AEX__state6__return__cancel__state1`
- `AEX__state6__return__take__state1`
- `AEX__state6__return__tea__state1`
- `AEX__state6__return__serveTea__state1`
- `AEX__state6__return__free__state1`

### Product 5

**Selected features:** selected = {s}

**Repaired FTS:** 7 states, 7 transitions (7 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 11 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 7 | 7/7 = 100.0% | 6/7 = 85.7% | 7/7 = 100.0% | 7/7 = 100.0% |
| ActionExchange | 42 | 42/42 = 100.0% | 36/42 = 85.7% | 42/42 = 100.0% | 42/42 = 100.0% |

**TransitionMissing — survived product state coverage** (1):

- `TM__state9__close__state1`

**ActionExchange — survived product state coverage** (6):

- `AEX__state9__close__take__state1`
- `AEX__state9__close__serveSoda__state1`
- `AEX__state9__close__change__state1`
- `AEX__state9__close__pay__state1`
- `AEX__state9__close__soda__state1`
- `AEX__state9__close__open__state1`

### Product 6

**Selected features:** selected = {s, t}

**Repaired FTS:** 8 states, 9 transitions (9 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 16 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 9 | 9/9 = 100.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% |
| ActionExchange | 72 | 72/72 = 100.0% | 64/72 = 88.9% | 72/72 = 100.0% | 72/72 = 100.0% |

**TransitionMissing — survived product state coverage** (1):

- `TM__state5__serveTea__state7`

**ActionExchange — survived product state coverage** (8):

- `AEX__state5__serveTea__take__state7`
- `AEX__state5__serveTea__tea__state7`
- `AEX__state5__serveTea__serveSoda__state7`
- `AEX__state5__serveTea__change__state7`
- `AEX__state5__serveTea__pay__state7`
- `AEX__state5__serveTea__soda__state7`
- `AEX__state5__serveTea__close__state7`
- `AEX__state5__serveTea__open__state7`

### Product 7

**Selected features:** selected = {c, f, s}

**Repaired FTS:** 5 states, 6 transitions (6 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 6 | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% |
| ActionExchange | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 30/30 = 100.0% | 30/30 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (6):

- `TM__state4__serveSoda__state7`
- `TM__state7__take__state1`
- `TM__state3__cancel__state6`
- `TM__state1__free__state3`
- `TM__state6__return__state1`
- `TM__state3__soda__state4`

**TransitionMissing — survived product state coverage** (1):

- `TM__state6__return__state1`

**ActionExchange — survived family-level state coverage (Devroey)** (30):

- `AEX__state4__serveSoda__cancel__state7`
- `AEX__state4__serveSoda__take__state7`
- `AEX__state4__serveSoda__free__state7`
- `AEX__state4__serveSoda__soda__state7`
- `AEX__state4__serveSoda__return__state7`
- `AEX__state7__take__cancel__state1`
- `AEX__state7__take__serveSoda__state1`
- `AEX__state7__take__free__state1`
- `AEX__state7__take__soda__state1`
- `AEX__state7__take__return__state1`
- `AEX__state3__cancel__take__state6`
- `AEX__state3__cancel__serveSoda__state6`
- `AEX__state3__cancel__free__state6`
- `AEX__state3__cancel__soda__state6`
- `AEX__state3__cancel__return__state6`
- `AEX__state1__free__cancel__state3`
- `AEX__state1__free__take__state3`
- `AEX__state1__free__serveSoda__state3`
- `AEX__state1__free__soda__state3`
- `AEX__state1__free__return__state3`
- `AEX__state6__return__cancel__state1`
- `AEX__state6__return__take__state1`
- `AEX__state6__return__serveSoda__state1`
- `AEX__state6__return__free__state1`
- `AEX__state6__return__soda__state1`
- `AEX__state3__soda__cancel__state4`
- `AEX__state3__soda__take__state4`
- `AEX__state3__soda__serveSoda__state4`
- `AEX__state3__soda__free__state4`
- `AEX__state3__soda__return__state4`

**ActionExchange — survived product state coverage** (5):

- `AEX__state6__return__cancel__state1`
- `AEX__state6__return__take__state1`
- `AEX__state6__return__serveSoda__state1`
- `AEX__state6__return__free__state1`
- `AEX__state6__return__soda__state1`

### Product 8

**Selected features:** selected = {c, s}

**Repaired FTS:** 8 states, 9 transitions (9 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 13 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 9 | 9/9 = 100.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% |
| ActionExchange | 72 | 72/72 = 100.0% | 64/72 = 88.9% | 72/72 = 100.0% | 72/72 = 100.0% |

**TransitionMissing — survived product state coverage** (1):

- `TM__state6__return__state1`

**ActionExchange — survived product state coverage** (8):

- `AEX__state6__return__cancel__state1`
- `AEX__state6__return__take__state1`
- `AEX__state6__return__serveSoda__state1`
- `AEX__state6__return__change__state1`
- `AEX__state6__return__pay__state1`
- `AEX__state6__return__soda__state1`
- `AEX__state6__return__close__state1`
- `AEX__state6__return__open__state1`

### Product 9

**Selected features:** selected = {f, s}

**Repaired FTS:** 4 states, 4 transitions (4 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 4 | 0/4 = 0.0% | 3/4 = 75.0% | 4/4 = 100.0% | 4/4 = 100.0% |
| ActionExchange | 12 | 0/12 = 0.0% | 9/12 = 75.0% | 12/12 = 100.0% | 12/12 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (4):

- `TM__state4__serveSoda__state7`
- `TM__state7__take__state1`
- `TM__state1__free__state3`
- `TM__state3__soda__state4`

**TransitionMissing — survived product state coverage** (1):

- `TM__state7__take__state1`

**ActionExchange — survived family-level state coverage (Devroey)** (12):

- `AEX__state4__serveSoda__take__state7`
- `AEX__state4__serveSoda__free__state7`
- `AEX__state4__serveSoda__soda__state7`
- `AEX__state7__take__serveSoda__state1`
- `AEX__state7__take__free__state1`
- `AEX__state7__take__soda__state1`
- `AEX__state1__free__take__state3`
- `AEX__state1__free__serveSoda__state3`
- `AEX__state1__free__soda__state3`
- `AEX__state3__soda__take__state4`
- `AEX__state3__soda__serveSoda__state4`
- `AEX__state3__soda__free__state4`

**ActionExchange — survived product state coverage** (3):

- `AEX__state7__take__serveSoda__state1`
- `AEX__state7__take__free__state1`
- `AEX__state7__take__soda__state1`

### Product 10

**Selected features:** selected = {c, t}

**Repaired FTS:** 8 states, 9 transitions (9 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 13 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 9 | 9/9 = 100.0% | 8/9 = 88.9% | 9/9 = 100.0% | 9/9 = 100.0% |
| ActionExchange | 72 | 72/72 = 100.0% | 64/72 = 88.9% | 72/72 = 100.0% | 72/72 = 100.0% |

**TransitionMissing — survived product state coverage** (1):

- `TM__state6__return__state1`

**ActionExchange — survived product state coverage** (8):

- `AEX__state6__return__cancel__state1`
- `AEX__state6__return__take__state1`
- `AEX__state6__return__tea__state1`
- `AEX__state6__return__serveTea__state1`
- `AEX__state6__return__change__state1`
- `AEX__state6__return__pay__state1`
- `AEX__state6__return__close__state1`
- `AEX__state6__return__open__state1`

### Product 11

**Selected features:** selected = {c, s, t}

**Repaired FTS:** 9 states, 11 transitions (11 real / 0 `__end__`).

**Family baseline projected to this product:** 3 test case(s) (of 3 family-level), 18 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 11 | 11/11 = 100.0% | 10/11 = 90.9% | 11/11 = 100.0% | 11/11 = 100.0% |
| ActionExchange | 110 | 110/110 = 100.0% | 100/110 = 90.9% | 110/110 = 100.0% | 110/110 = 100.0% |

**TransitionMissing — survived product state coverage** (1):

- `TM__state6__return__state1`

**ActionExchange — survived product state coverage** (10):

- `AEX__state6__return__cancel__state1`
- `AEX__state6__return__take__state1`
- `AEX__state6__return__tea__state1`
- `AEX__state6__return__serveTea__state1`
- `AEX__state6__return__serveSoda__state1`
- `AEX__state6__return__change__state1`
- `AEX__state6__return__pay__state1`
- `AEX__state6__return__soda__state1`
- `AEX__state6__return__close__state1`
- `AEX__state6__return__open__state1`

### Product 12

**Selected features:** selected = {f, s, t}

**Repaired FTS:** 5 states, 6 transitions (6 real / 0 `__end__`).

**Family baseline projected to this product:** 0 test case(s) (of 3 family-level), 0 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 6 | 0/6 = 0.0% | 5/6 = 83.3% | 6/6 = 100.0% | 6/6 = 100.0% |
| ActionExchange | 30 | 0/30 = 0.0% | 25/30 = 83.3% | 30/30 = 100.0% | 30/30 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (6):

- `TM__state4__serveSoda__state7`
- `TM__state5__serveTea__state7`
- `TM__state7__take__state1`
- `TM__state1__free__state3`
- `TM__state3__tea__state5`
- `TM__state3__soda__state4`

**TransitionMissing — survived product state coverage** (1):

- `TM__state5__serveTea__state7`

**ActionExchange — survived family-level state coverage (Devroey)** (30):

- `AEX__state4__serveSoda__take__state7`
- `AEX__state4__serveSoda__tea__state7`
- `AEX__state4__serveSoda__serveTea__state7`
- `AEX__state4__serveSoda__free__state7`
- `AEX__state4__serveSoda__soda__state7`
- `AEX__state5__serveTea__take__state7`
- `AEX__state5__serveTea__tea__state7`
- `AEX__state5__serveTea__serveSoda__state7`
- `AEX__state5__serveTea__free__state7`
- `AEX__state5__serveTea__soda__state7`
- `AEX__state7__take__tea__state1`
- `AEX__state7__take__serveTea__state1`
- `AEX__state7__take__serveSoda__state1`
- `AEX__state7__take__free__state1`
- `AEX__state7__take__soda__state1`
- `AEX__state1__free__take__state3`
- `AEX__state1__free__tea__state3`
- `AEX__state1__free__serveTea__state3`
- `AEX__state1__free__serveSoda__state3`
- `AEX__state1__free__soda__state3`
- `AEX__state3__tea__take__state5`
- `AEX__state3__tea__serveTea__state5`
- `AEX__state3__tea__serveSoda__state5`
- `AEX__state3__tea__free__state5`
- `AEX__state3__tea__soda__state5`
- `AEX__state3__soda__take__state4`
- `AEX__state3__soda__tea__state4`
- `AEX__state3__soda__serveTea__state4`
- `AEX__state3__soda__serveSoda__state4`
- `AEX__state3__soda__free__state4`

**ActionExchange — survived product state coverage** (5):

- `AEX__state5__serveTea__take__state7`
- `AEX__state5__serveTea__tea__state7`
- `AEX__state5__serveTea__serveSoda__state7`
- `AEX__state5__serveTea__free__state7`
- `AEX__state5__serveTea__soda__state7`
---

## SVM summary (aggregate over 12 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 3 test case(s) generated once for the SPL, projected per-product via fexpr-filtering before kill-checking.

| Operator | Mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 86 | 52/86 = 60.5% | 74/86 = 86.0% | 86/86 = 100.0% | 86/86 = 100.0% |
| ActionExchange | 580 | 410/580 = 70.7% | 506/580 = 87.2% | 580/580 = 100.0% | 580/580 = 100.0% |

Total products: 12.
