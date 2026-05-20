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

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 20 | 8/20 = 40.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 72/180 = 40.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived state coverage** (12):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state4__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (108):

- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__enter PIN__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__enter PIN__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state4__enter PIN__press cabin roof__state6`
- `AEX__state4__enter PIN__press hall down__state6`
- `AEX__state4__enter PIN__press hall 
RoofDown__state6`
- `AEX__state4__enter PIN__press cabin 
executive floor__state6`
- `AEX__state4__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state4__enter PIN__press cabin lobby__state6`
- `AEX__state4__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state4__enter PIN__press intercom__state6`
- `AEX__state4__enter PIN__press hall up__state6`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 2

**Selected features:** selected = {Alarm, ControlButtons, MobileKey}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 18 | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived state coverage** (11):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__tap mobile
key__state6`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (88):

- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press alarm
button__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press alarm
button__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 3

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 20 | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 63/180 = 35.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived state coverage** (13):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state4__read card__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (117):

- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__read card__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__read card__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press cabin 
executive floor__state6`
- `AEX__state4__read card__press alarm
button__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press cabin 
executive floor__state6`
- `AEX__state2__read card__press alarm
button__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 4

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 22 | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived state coverage** (13):

- `TM__state4__press cabin roof__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__tap mobile
key__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (130):

- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__tap mobile
key__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press door open__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press intercom__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state2__tap mobile
key__press door close__state6`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press door open__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press intercom__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state4__tap mobile
key__press door close__state6`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__tap mobile
key__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press intercom__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state1__press hall down__press door close__state3`

### Product 5

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService}

**Repaired FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 20 | 10/20 = 50.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 220 | 110/220 = 50.0% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived state coverage** (10):

- `TM__state9__press&hold 
door close__state11`
- `TM__state4__press cabin roof__state5`
- `TM__state13__press&hold 
door open__state10`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state14__press&hold 
door close__state11`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state5__press&hold 
door open__state10`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (110):

- `AEX__state9__press&hold 
door close__press cabin roof__state11`
- `AEX__state9__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state9__press&hold 
door close__press hall down__state11`
- `AEX__state9__press&hold 
door close__press cabin lobby__state11`
- `AEX__state9__press&hold 
door close__release door open__state11`
- `AEX__state9__press&hold 
door close__release door close__state11`
- `AEX__state9__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state9__press&hold 
door close__press alarm
button__state11`
- `AEX__state9__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state9__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state9__press&hold 
door close__press hall up__state11`
- `AEX__state4__press cabin roof__press&hold 
door open__state5`
- `AEX__state4__press cabin roof__press&hold 
door close__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__release door open__state5`
- `AEX__state4__press cabin roof__release door close__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state13__press&hold 
door open__press cabin roof__state10`
- `AEX__state13__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state13__press&hold 
door open__press hall down__state10`
- `AEX__state13__press&hold 
door open__press cabin lobby__state10`
- `AEX__state13__press&hold 
door open__release door open__state10`
- `AEX__state13__press&hold 
door open__release door close__state10`
- `AEX__state13__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state13__press&hold 
door open__press alarm
button__state10`
- `AEX__state13__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state13__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state13__press&hold 
door open__press hall up__state10`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state14__press&hold 
door close__press cabin roof__state11`
- `AEX__state14__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state14__press&hold 
door close__press hall down__state11`
- `AEX__state14__press&hold 
door close__press cabin lobby__state11`
- `AEX__state14__press&hold 
door close__release door open__state11`
- `AEX__state14__press&hold 
door close__release door close__state11`
- `AEX__state14__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state14__press&hold 
door close__press alarm
button__state11`
- `AEX__state14__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state14__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state14__press&hold 
door close__press hall up__state11`
- `AEX__state3__press cabin roof__press&hold 
door open__state5`
- `AEX__state3__press cabin roof__press&hold 
door close__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__release door open__state5`
- `AEX__state3__press cabin roof__release door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press&hold 
door open__state3`
- `AEX__state1__press hall down__press&hold 
door close__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__release door open__state3`
- `AEX__state1__press hall down__release door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state5__press&hold 
door open__press cabin roof__state10`
- `AEX__state5__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state5__press&hold 
door open__press hall down__state10`
- `AEX__state5__press&hold 
door open__press cabin lobby__state10`
- `AEX__state5__press&hold 
door open__release door open__state10`
- `AEX__state5__press&hold 
door open__release door close__state10`
- `AEX__state5__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state5__press&hold 
door open__press alarm
button__state10`
- `AEX__state5__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state5__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state5__press&hold 
door open__press hall up__state10`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 6

**Selected features:** selected = {CardReader, ControlButtons, Intercom, ManualDoorControl}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 22 | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived state coverage** (13):

- `TM__state4__press cabin roof__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state4__read card__state6`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (130):

- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press door open__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press intercom__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state4__read card__press door close__state6`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__read card__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__read card__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press intercom__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press door open__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press intercom__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state2__read card__press door close__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state1__press hall down__press door close__state3`

### Product 7

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 22 | 7/22 = 31.8% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 70/220 = 31.8% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived state coverage** (15):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__tap mobile
key__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (150):

- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__tap mobile
key__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__press intercom__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__tap mobile
key__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__tap mobile
key__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state2__tap mobile
key__press alarm
button__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press intercom__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__tap mobile
key__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press alarm
button__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state4__tap mobile
key__press alarm
button__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press intercom__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 8

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 26 | 11/26 = 42.3% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 121/286 = 42.3% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived state coverage** (15):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state12__press door close__state7`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (165):

- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__press door open__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__enter PIN__state9`
- `AEX__state12__press alarm
button__press door close__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state12__press door close__press cabin roof__state7`
- `AEX__state12__press door close__press door open__state7`
- `AEX__state12__press door close__press hall down__state7`
- `AEX__state12__press door close__press cabin 
executive floor__state7`
- `AEX__state12__press door close__press cabin lobby__state7`
- `AEX__state12__press door close__enter PIN__state7`
- `AEX__state12__press door close__press hall 
RoofDown__state7`
- `AEX__state12__press door close__press alarm
button__state7`
- `AEX__state12__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state12__press door close__press hall 
LobbyUp__state7`
- `AEX__state12__press door close__press hall up__state7`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin 
executive floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__enter PIN__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press door open__press cabin roof__state8`
- `AEX__state12__press door open__press hall down__state8`
- `AEX__state12__press door open__press cabin 
executive floor__state8`
- `AEX__state12__press door open__press cabin lobby__state8`
- `AEX__state12__press door open__enter PIN__state8`
- `AEX__state12__press door open__press door close__state8`
- `AEX__state12__press door open__press hall 
RoofDown__state8`
- `AEX__state12__press door open__press alarm
button__state8`
- `AEX__state12__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state12__press door open__press hall 
LobbyUp__state8`
- `AEX__state12__press door open__press hall up__state8`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__enter PIN__press cabin roof__state6`
- `AEX__state3__enter PIN__press door open__state6`
- `AEX__state3__enter PIN__press hall down__state6`
- `AEX__state3__enter PIN__press cabin 
executive floor__state6`
- `AEX__state3__enter PIN__press cabin lobby__state6`
- `AEX__state3__enter PIN__press door close__state6`
- `AEX__state3__enter PIN__press hall 
RoofDown__state6`
- `AEX__state3__enter PIN__press alarm
button__state6`
- `AEX__state3__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state3__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state3__enter PIN__press hall up__state6`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin 
executive floor__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__enter PIN__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press alarm
button__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 9

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 26 | 11/26 = 42.3% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 121/286 = 42.3% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived state coverage** (15):

- `TM__state4__press cabin roof__state5`
- `TM__state12__press door close__state7`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (165):

- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state12__press door close__press cabin roof__state7`
- `AEX__state12__press door close__press door open__state7`
- `AEX__state12__press door close__press hall down__state7`
- `AEX__state12__press door close__press cabin 
executive floor__state7`
- `AEX__state12__press door close__press cabin lobby__state7`
- `AEX__state12__press door close__enter PIN__state7`
- `AEX__state12__press door close__press hall 
RoofDown__state7`
- `AEX__state12__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state12__press door close__press hall 
LobbyUp__state7`
- `AEX__state12__press door close__press intercom__state7`
- `AEX__state12__press door close__press hall up__state7`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin 
executive floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__enter PIN__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press door open__press cabin roof__state8`
- `AEX__state12__press door open__press hall down__state8`
- `AEX__state12__press door open__press cabin 
executive floor__state8`
- `AEX__state12__press door open__press cabin lobby__state8`
- `AEX__state12__press door open__enter PIN__state8`
- `AEX__state12__press door open__press door close__state8`
- `AEX__state12__press door open__press hall 
RoofDown__state8`
- `AEX__state12__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state12__press door open__press hall 
LobbyUp__state8`
- `AEX__state12__press door open__press intercom__state8`
- `AEX__state12__press door open__press hall up__state8`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__enter PIN__press cabin roof__state6`
- `AEX__state3__enter PIN__press door open__state6`
- `AEX__state3__enter PIN__press hall down__state6`
- `AEX__state3__enter PIN__press cabin 
executive floor__state6`
- `AEX__state3__enter PIN__press cabin lobby__state6`
- `AEX__state3__enter PIN__press door close__state6`
- `AEX__state3__enter PIN__press hall 
RoofDown__state6`
- `AEX__state3__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state3__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state3__enter PIN__press intercom__state6`
- `AEX__state3__enter PIN__press hall up__state6`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__press door open__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__enter PIN__state9`
- `AEX__state12__press intercom__press door close__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin 
executive floor__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__enter PIN__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press intercom__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 10

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom}

**Repaired FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 20 | 10/20 = 50.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 220 | 110/220 = 50.0% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived state coverage** (10):

- `TM__state9__press&hold 
door close__state11`
- `TM__state4__press cabin roof__state5`
- `TM__state13__press&hold 
door open__state10`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state14__press&hold 
door close__state11`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state5__press&hold 
door open__state10`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (110):

- `AEX__state9__press&hold 
door close__press cabin roof__state11`
- `AEX__state9__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state9__press&hold 
door close__press hall down__state11`
- `AEX__state9__press&hold 
door close__press cabin lobby__state11`
- `AEX__state9__press&hold 
door close__release door open__state11`
- `AEX__state9__press&hold 
door close__release door close__state11`
- `AEX__state9__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state9__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state9__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state9__press&hold 
door close__press intercom__state11`
- `AEX__state9__press&hold 
door close__press hall up__state11`
- `AEX__state4__press cabin roof__press&hold 
door open__state5`
- `AEX__state4__press cabin roof__press&hold 
door close__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__release door open__state5`
- `AEX__state4__press cabin roof__release door close__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state13__press&hold 
door open__press cabin roof__state10`
- `AEX__state13__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state13__press&hold 
door open__press hall down__state10`
- `AEX__state13__press&hold 
door open__press cabin lobby__state10`
- `AEX__state13__press&hold 
door open__release door open__state10`
- `AEX__state13__press&hold 
door open__release door close__state10`
- `AEX__state13__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state13__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state13__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state13__press&hold 
door open__press intercom__state10`
- `AEX__state13__press&hold 
door open__press hall up__state10`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state14__press&hold 
door close__press cabin roof__state11`
- `AEX__state14__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state14__press&hold 
door close__press hall down__state11`
- `AEX__state14__press&hold 
door close__press cabin lobby__state11`
- `AEX__state14__press&hold 
door close__release door open__state11`
- `AEX__state14__press&hold 
door close__release door close__state11`
- `AEX__state14__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state14__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state14__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state14__press&hold 
door close__press intercom__state11`
- `AEX__state14__press&hold 
door close__press hall up__state11`
- `AEX__state3__press cabin roof__press&hold 
door open__state5`
- `AEX__state3__press cabin roof__press&hold 
door close__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__release door open__state5`
- `AEX__state3__press cabin roof__release door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press&hold 
door open__state3`
- `AEX__state1__press hall down__press&hold 
door close__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__release door open__state3`
- `AEX__state1__press hall down__release door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state5__press&hold 
door open__press cabin roof__state10`
- `AEX__state5__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state5__press&hold 
door open__press hall down__state10`
- `AEX__state5__press&hold 
door open__press cabin lobby__state10`
- `AEX__state5__press&hold 
door open__release door open__state10`
- `AEX__state5__press&hold 
door open__release door close__state10`
- `AEX__state5__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state5__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state5__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state5__press&hold 
door open__press intercom__state10`
- `AEX__state5__press&hold 
door open__press hall up__state10`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 11

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Repaired FTS:** 12 states, 31 transitions (25 real / 6 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 25 | 13/25 = 52.0% | 25/25 = 100.0% | 25/25 = 100.0% |
| ActionExchange | 350 | 182/350 = 52.0% | 350/350 = 100.0% | 350/350 = 100.0% |

**TransitionMissing — survived state coverage** (12):

- `TM__state13__press&hold 
door open__state10`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state14__press&hold 
door close__state11`
- `TM__state5__press door close__state7`
- `TM__state5__press&hold 
door open__state10`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state9__press&hold 
door close__state11`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (168):

- `AEX__state13__press&hold 
door open__press cabin roof__state10`
- `AEX__state13__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state13__press&hold 
door open__press door open__state10`
- `AEX__state13__press&hold 
door open__press hall down__state10`
- `AEX__state13__press&hold 
door open__press cabin lobby__state10`
- `AEX__state13__press&hold 
door open__release door open__state10`
- `AEX__state13__press&hold 
door open__press door close__state10`
- `AEX__state13__press&hold 
door open__release door close__state10`
- `AEX__state13__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state13__press&hold 
door open__press alarm
button__state10`
- `AEX__state13__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state13__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state13__press&hold 
door open__press intercom__state10`
- `AEX__state13__press&hold 
door open__press hall up__state10`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press&hold 
door open__state9`
- `AEX__state5__press alarm
button__press&hold 
door close__state9`
- `AEX__state5__press alarm
button__press door open__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__release door open__state9`
- `AEX__state5__press alarm
button__press door close__state9`
- `AEX__state5__press alarm
button__release door close__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state14__press&hold 
door close__press cabin roof__state11`
- `AEX__state14__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state14__press&hold 
door close__press door open__state11`
- `AEX__state14__press&hold 
door close__press hall down__state11`
- `AEX__state14__press&hold 
door close__press cabin lobby__state11`
- `AEX__state14__press&hold 
door close__release door open__state11`
- `AEX__state14__press&hold 
door close__press door close__state11`
- `AEX__state14__press&hold 
door close__release door close__state11`
- `AEX__state14__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state14__press&hold 
door close__press alarm
button__state11`
- `AEX__state14__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state14__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state14__press&hold 
door close__press intercom__state11`
- `AEX__state14__press&hold 
door close__press hall up__state11`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press&hold 
door open__state7`
- `AEX__state5__press door close__press&hold 
door close__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__release door open__state7`
- `AEX__state5__press door close__release door close__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state5__press&hold 
door open__press cabin roof__state10`
- `AEX__state5__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state5__press&hold 
door open__press door open__state10`
- `AEX__state5__press&hold 
door open__press hall down__state10`
- `AEX__state5__press&hold 
door open__press cabin lobby__state10`
- `AEX__state5__press&hold 
door open__release door open__state10`
- `AEX__state5__press&hold 
door open__press door close__state10`
- `AEX__state5__press&hold 
door open__release door close__state10`
- `AEX__state5__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state5__press&hold 
door open__press alarm
button__state10`
- `AEX__state5__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state5__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state5__press&hold 
door open__press intercom__state10`
- `AEX__state5__press&hold 
door open__press hall up__state10`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state9__press&hold 
door close__press cabin roof__state11`
- `AEX__state9__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state9__press&hold 
door close__press door open__state11`
- `AEX__state9__press&hold 
door close__press hall down__state11`
- `AEX__state9__press&hold 
door close__press cabin lobby__state11`
- `AEX__state9__press&hold 
door close__release door open__state11`
- `AEX__state9__press&hold 
door close__press door close__state11`
- `AEX__state9__press&hold 
door close__release door close__state11`
- `AEX__state9__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state9__press&hold 
door close__press alarm
button__state11`
- `AEX__state9__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state9__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state9__press&hold 
door close__press intercom__state11`
- `AEX__state9__press&hold 
door close__press hall up__state11`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__press cabin roof__press&hold 
door open__state5`
- `AEX__state3__press cabin roof__press&hold 
door close__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__release door open__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__release door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press&hold 
door open__state8`
- `AEX__state7__press door open__press&hold 
door close__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__release door open__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state7__press door open__release door close__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press alarm
button__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press intercom__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press&hold 
door open__state3`
- `AEX__state1__press hall down__press&hold 
door close__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__release door open__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__release door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 12

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, PinPad}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 20 | 8/20 = 40.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 72/180 = 40.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived state coverage** (12):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state4__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (108):

- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__enter PIN__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__enter PIN__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state4__enter PIN__press cabin roof__state6`
- `AEX__state4__enter PIN__press hall down__state6`
- `AEX__state4__enter PIN__press hall 
RoofDown__state6`
- `AEX__state4__enter PIN__press cabin 
executive floor__state6`
- `AEX__state4__enter PIN__press alarm
button__state6`
- `AEX__state4__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state4__enter PIN__press cabin lobby__state6`
- `AEX__state4__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state4__enter PIN__press hall up__state6`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 13

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, ManualDoorControl}

**Repaired FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 24 | 13/24 = 54.2% | 24/24 = 100.0% | 24/24 = 100.0% |
| ActionExchange | 312 | 169/312 = 54.2% | 312/312 = 100.0% | 312/312 = 100.0% |

**TransitionMissing — survived state coverage** (11):

- `TM__state13__press&hold 
door open__state10`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state14__press&hold 
door close__state11`
- `TM__state5__press door close__state7`
- `TM__state5__press&hold 
door open__state10`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state9__press&hold 
door close__state11`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (143):

- `AEX__state13__press&hold 
door open__press cabin roof__state10`
- `AEX__state13__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state13__press&hold 
door open__press door open__state10`
- `AEX__state13__press&hold 
door open__press hall down__state10`
- `AEX__state13__press&hold 
door open__press cabin lobby__state10`
- `AEX__state13__press&hold 
door open__release door open__state10`
- `AEX__state13__press&hold 
door open__press door close__state10`
- `AEX__state13__press&hold 
door open__release door close__state10`
- `AEX__state13__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state13__press&hold 
door open__press alarm
button__state10`
- `AEX__state13__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state13__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state13__press&hold 
door open__press hall up__state10`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state14__press&hold 
door close__press cabin roof__state11`
- `AEX__state14__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state14__press&hold 
door close__press door open__state11`
- `AEX__state14__press&hold 
door close__press hall down__state11`
- `AEX__state14__press&hold 
door close__press cabin lobby__state11`
- `AEX__state14__press&hold 
door close__release door open__state11`
- `AEX__state14__press&hold 
door close__press door close__state11`
- `AEX__state14__press&hold 
door close__release door close__state11`
- `AEX__state14__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state14__press&hold 
door close__press alarm
button__state11`
- `AEX__state14__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state14__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state14__press&hold 
door close__press hall up__state11`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press&hold 
door open__state7`
- `AEX__state5__press door close__press&hold 
door close__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__release door open__state7`
- `AEX__state5__press door close__release door close__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state5__press&hold 
door open__press cabin roof__state10`
- `AEX__state5__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state5__press&hold 
door open__press door open__state10`
- `AEX__state5__press&hold 
door open__press hall down__state10`
- `AEX__state5__press&hold 
door open__press cabin lobby__state10`
- `AEX__state5__press&hold 
door open__release door open__state10`
- `AEX__state5__press&hold 
door open__press door close__state10`
- `AEX__state5__press&hold 
door open__release door close__state10`
- `AEX__state5__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state5__press&hold 
door open__press alarm
button__state10`
- `AEX__state5__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state5__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state5__press&hold 
door open__press hall up__state10`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state9__press&hold 
door close__press cabin roof__state11`
- `AEX__state9__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state9__press&hold 
door close__press door open__state11`
- `AEX__state9__press&hold 
door close__press hall down__state11`
- `AEX__state9__press&hold 
door close__press cabin lobby__state11`
- `AEX__state9__press&hold 
door close__release door open__state11`
- `AEX__state9__press&hold 
door close__press door close__state11`
- `AEX__state9__press&hold 
door close__release door close__state11`
- `AEX__state9__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state9__press&hold 
door close__press alarm
button__state11`
- `AEX__state9__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state9__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state9__press&hold 
door close__press hall up__state11`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__press cabin roof__press&hold 
door open__state5`
- `AEX__state3__press cabin roof__press&hold 
door close__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__release door open__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__release door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press&hold 
door open__state8`
- `AEX__state7__press door open__press&hold 
door close__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__release door open__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state7__press door open__release door close__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press alarm
button__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press&hold 
door open__state3`
- `AEX__state1__press hall down__press&hold 
door close__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__release door open__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__release door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 14

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 22 | 7/22 = 31.8% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 70/220 = 31.8% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived state coverage** (15):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state4__read card__state6`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (150):

- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__read card__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__press intercom__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__read card__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press cabin 
executive floor__state6`
- `AEX__state4__read card__press alarm
button__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press intercom__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__read card__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press alarm
button__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__read card__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press cabin 
executive floor__state6`
- `AEX__state2__read card__press alarm
button__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press intercom__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 15

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Repaired FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 24 | 13/24 = 54.2% | 24/24 = 100.0% | 24/24 = 100.0% |
| ActionExchange | 312 | 169/312 = 54.2% | 312/312 = 100.0% | 312/312 = 100.0% |

**TransitionMissing — survived state coverage** (11):

- `TM__state13__press&hold 
door open__state10`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state14__press&hold 
door close__state11`
- `TM__state5__press door close__state7`
- `TM__state5__press&hold 
door open__state10`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state9__press&hold 
door close__state11`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (143):

- `AEX__state13__press&hold 
door open__press cabin roof__state10`
- `AEX__state13__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state13__press&hold 
door open__press door open__state10`
- `AEX__state13__press&hold 
door open__press hall down__state10`
- `AEX__state13__press&hold 
door open__press cabin lobby__state10`
- `AEX__state13__press&hold 
door open__release door open__state10`
- `AEX__state13__press&hold 
door open__press door close__state10`
- `AEX__state13__press&hold 
door open__release door close__state10`
- `AEX__state13__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state13__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state13__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state13__press&hold 
door open__press intercom__state10`
- `AEX__state13__press&hold 
door open__press hall up__state10`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state14__press&hold 
door close__press cabin roof__state11`
- `AEX__state14__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state14__press&hold 
door close__press door open__state11`
- `AEX__state14__press&hold 
door close__press hall down__state11`
- `AEX__state14__press&hold 
door close__press cabin lobby__state11`
- `AEX__state14__press&hold 
door close__release door open__state11`
- `AEX__state14__press&hold 
door close__press door close__state11`
- `AEX__state14__press&hold 
door close__release door close__state11`
- `AEX__state14__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state14__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state14__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state14__press&hold 
door close__press intercom__state11`
- `AEX__state14__press&hold 
door close__press hall up__state11`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press&hold 
door open__state7`
- `AEX__state5__press door close__press&hold 
door close__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__release door open__state7`
- `AEX__state5__press door close__release door close__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state5__press&hold 
door open__press cabin roof__state10`
- `AEX__state5__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state5__press&hold 
door open__press door open__state10`
- `AEX__state5__press&hold 
door open__press hall down__state10`
- `AEX__state5__press&hold 
door open__press cabin lobby__state10`
- `AEX__state5__press&hold 
door open__release door open__state10`
- `AEX__state5__press&hold 
door open__press door close__state10`
- `AEX__state5__press&hold 
door open__release door close__state10`
- `AEX__state5__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state5__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state5__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state5__press&hold 
door open__press intercom__state10`
- `AEX__state5__press&hold 
door open__press hall up__state10`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state9__press&hold 
door close__press cabin roof__state11`
- `AEX__state9__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state9__press&hold 
door close__press door open__state11`
- `AEX__state9__press&hold 
door close__press hall down__state11`
- `AEX__state9__press&hold 
door close__press cabin lobby__state11`
- `AEX__state9__press&hold 
door close__release door open__state11`
- `AEX__state9__press&hold 
door close__press door close__state11`
- `AEX__state9__press&hold 
door close__release door close__state11`
- `AEX__state9__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state9__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state9__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state9__press&hold 
door close__press intercom__state11`
- `AEX__state9__press&hold 
door close__press hall up__state11`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__press cabin roof__press&hold 
door open__state5`
- `AEX__state3__press cabin roof__press&hold 
door close__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__release door open__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__release door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press&hold 
door open__state8`
- `AEX__state7__press door open__press&hold 
door close__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__release door open__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state7__press door open__release door close__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press intercom__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press&hold 
door open__state3`
- `AEX__state1__press hall down__press&hold 
door close__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__release door open__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__release door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 16

**Selected features:** selected = {Alarm, CardReader, ControlButtons}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 18 | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived state coverage** (11):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state4__read card__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (88):

- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press alarm
button__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press alarm
button__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 17

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 19 | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% |
| ActionExchange | 171 | 63/171 = 36.8% | 171/171 = 100.0% | 171/171 = 100.0% |

**TransitionMissing — survived state coverage** (12):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state4__read card__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (108):

- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__read card__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press alarm
button__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press intercom__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press alarm
button__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press intercom__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 18

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, MobileKey}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 20 | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 63/180 = 35.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived state coverage** (13):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state12__press alarm
button__state9`
- `TM__state2__tap mobile
key__state6`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (117):

- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__tap mobile
key__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__tap mobile
key__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state2__tap mobile
key__press alarm
button__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state4__tap mobile
key__press alarm
button__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 19

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 22 | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived state coverage** (13):

- `TM__state4__press cabin roof__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__tap mobile
key__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (130):

- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__tap mobile
key__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press door open__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press alarm
button__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state2__tap mobile
key__press door close__state6`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press door open__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press alarm
button__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state4__tap mobile
key__press door close__state6`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__tap mobile
key__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press alarm
button__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state1__press hall down__press door close__state3`

### Product 20

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 26 | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 99/286 = 34.6% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived state coverage** (17):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__tap mobile
key__state6`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (187):

- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__press door open__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__press door close__state9`
- `AEX__state12__press alarm
button__tap mobile
key__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin 
executive floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press door close__state8`
- `AEX__state5__press door open__tap mobile
key__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press alarm
button__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin 
executive floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__tap mobile
key__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press door open__press cabin roof__state8`
- `AEX__state12__press door open__press hall down__state8`
- `AEX__state12__press door open__press cabin 
executive floor__state8`
- `AEX__state12__press door open__press cabin lobby__state8`
- `AEX__state12__press door open__press door close__state8`
- `AEX__state12__press door open__tap mobile
key__state8`
- `AEX__state12__press door open__press hall 
RoofDown__state8`
- `AEX__state12__press door open__press alarm
button__state8`
- `AEX__state12__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state12__press door open__press hall 
LobbyUp__state8`
- `AEX__state12__press door open__press hall up__state8`
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
- `AEX__state6__press cabin lobby__tap mobile
key__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press door open__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press door close__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press alarm
button__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin 
executive floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__tap mobile
key__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press alarm
button__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__press hall up__state7`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press door open__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press door close__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press alarm
button__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 21

**Selected features:** selected = {CardReader, ControlButtons, Intercom}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 18 | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived state coverage** (11):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state4__read card__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (88):

- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press intercom__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press intercom__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 22

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 22 | 8/22 = 36.4% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 80/220 = 36.4% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived state coverage** (14):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state4__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (140):

- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__press intercom__state9`
- `AEX__state12__press alarm
button__enter PIN__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__enter PIN__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__enter PIN__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press alarm
button__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__enter PIN__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state4__enter PIN__press cabin roof__state6`
- `AEX__state4__enter PIN__press hall down__state6`
- `AEX__state4__enter PIN__press hall 
RoofDown__state6`
- `AEX__state4__enter PIN__press cabin 
executive floor__state6`
- `AEX__state4__enter PIN__press alarm
button__state6`
- `AEX__state4__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state4__enter PIN__press cabin lobby__state6`
- `AEX__state4__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state4__enter PIN__press intercom__state6`
- `AEX__state4__enter PIN__press hall up__state6`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 23

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 28 | 9/28 = 32.1% | 28/28 = 100.0% | 28/28 = 100.0% |
| ActionExchange | 336 | 108/336 = 32.1% | 336/336 = 100.0% | 336/336 = 100.0% |

**TransitionMissing — survived state coverage** (19):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door open__state8`
- `TM__state4__read card__state6`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (228):

- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__press door open__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__press door close__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__read card__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__press intercom__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press door open__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__press door close__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__read card__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin 
executive floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press door close__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press alarm
button__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__read card__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press intercom__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press door open__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press cabin 
executive floor__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press door close__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press alarm
button__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press intercom__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin 
executive floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__read card__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press door open__press cabin roof__state8`
- `AEX__state12__press door open__press hall down__state8`
- `AEX__state12__press door open__press cabin 
executive floor__state8`
- `AEX__state12__press door open__press cabin lobby__state8`
- `AEX__state12__press door open__press door close__state8`
- `AEX__state12__press door open__press hall 
RoofDown__state8`
- `AEX__state12__press door open__press alarm
button__state8`
- `AEX__state12__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state12__press door open__read card__state8`
- `AEX__state12__press door open__press hall 
LobbyUp__state8`
- `AEX__state12__press door open__press intercom__state8`
- `AEX__state12__press door open__press hall up__state8`
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__read card__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin 
executive floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press alarm
button__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__read card__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__press intercom__state7`
- `AEX__state8__press door close__press hall up__state7`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__press door open__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__press door close__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press alarm
button__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__read card__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press door open__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press cabin 
executive floor__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press door close__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press alarm
button__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press intercom__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 24

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 28 | 9/28 = 32.1% | 28/28 = 100.0% | 28/28 = 100.0% |
| ActionExchange | 336 | 108/336 = 32.1% | 336/336 = 100.0% | 336/336 = 100.0% |

**TransitionMissing — survived state coverage** (19):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__tap mobile
key__state6`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (228):

- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__press door open__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__press door close__state9`
- `AEX__state12__press alarm
button__tap mobile
key__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__press intercom__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press door open__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__press door close__state9`
- `AEX__state5__press alarm
button__tap mobile
key__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin 
executive floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press door close__state8`
- `AEX__state5__press door open__tap mobile
key__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press alarm
button__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press intercom__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin 
executive floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__tap mobile
key__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press door open__press cabin roof__state8`
- `AEX__state12__press door open__press hall down__state8`
- `AEX__state12__press door open__press cabin 
executive floor__state8`
- `AEX__state12__press door open__press cabin lobby__state8`
- `AEX__state12__press door open__press door close__state8`
- `AEX__state12__press door open__tap mobile
key__state8`
- `AEX__state12__press door open__press hall 
RoofDown__state8`
- `AEX__state12__press door open__press alarm
button__state8`
- `AEX__state12__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state12__press door open__press hall 
LobbyUp__state8`
- `AEX__state12__press door open__press intercom__state8`
- `AEX__state12__press door open__press hall up__state8`
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
- `AEX__state6__press cabin lobby__tap mobile
key__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press door open__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press door close__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press alarm
button__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press intercom__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin 
executive floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__tap mobile
key__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press alarm
button__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__press intercom__state7`
- `AEX__state8__press door close__press hall up__state7`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__press door open__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__press door close__state9`
- `AEX__state12__press intercom__tap mobile
key__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press alarm
button__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press door open__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press door close__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press alarm
button__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press intercom__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 25

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom, ManualDoorControl}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 23 | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% |
| ActionExchange | 253 | 99/253 = 39.1% | 253/253 = 100.0% | 253/253 = 100.0% |

**TransitionMissing — survived state coverage** (14):

- `TM__state4__press cabin roof__state5`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state4__read card__state6`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (154):

- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press door open__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__press door close__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__read card__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press door open__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press door close__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press alarm
button__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press intercom__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__read card__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press alarm
button__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__read card__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press intercom__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press door open__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press door close__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press alarm
button__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press intercom__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 26

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 26 | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 99/286 = 34.6% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived state coverage** (17):

- `TM__state4__press cabin roof__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door open__state8`
- `TM__state4__read card__state6`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (187):

- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin 
executive floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press door close__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__read card__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press intercom__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press door open__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press cabin 
executive floor__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press door close__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press intercom__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin 
executive floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__read card__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press door open__press cabin roof__state8`
- `AEX__state12__press door open__press hall down__state8`
- `AEX__state12__press door open__press cabin 
executive floor__state8`
- `AEX__state12__press door open__press cabin lobby__state8`
- `AEX__state12__press door open__press door close__state8`
- `AEX__state12__press door open__press hall 
RoofDown__state8`
- `AEX__state12__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state12__press door open__read card__state8`
- `AEX__state12__press door open__press hall 
LobbyUp__state8`
- `AEX__state12__press door open__press intercom__state8`
- `AEX__state12__press door open__press hall up__state8`
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__read card__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin 
executive floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__read card__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__press intercom__state7`
- `AEX__state8__press door close__press hall up__state7`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__press door open__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__press door close__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__read card__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press door open__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press cabin 
executive floor__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press door close__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press intercom__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 27

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 23 | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% |
| ActionExchange | 253 | 99/253 = 39.1% | 253/253 = 100.0% | 253/253 = 100.0% |

**TransitionMissing — survived state coverage** (14):

- `TM__state4__press cabin roof__state5`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__tap mobile
key__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (154):

- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press door open__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__press door close__state9`
- `AEX__state5__press alarm
button__tap mobile
key__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__tap mobile
key__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press door open__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press door close__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press alarm
button__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press intercom__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press door open__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press door close__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press alarm
button__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press intercom__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state7__press door open__tap mobile
key__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press alarm
button__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press intercom__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 28

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom}

**Repaired FTS:** 10 states, 25 transitions (21 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 21 | 10/21 = 47.6% | 21/21 = 100.0% | 21/21 = 100.0% |
| ActionExchange | 252 | 120/252 = 47.6% | 252/252 = 100.0% | 252/252 = 100.0% |

**TransitionMissing — survived state coverage** (11):

- `TM__state4__press cabin roof__state5`
- `TM__state13__press&hold 
door open__state10`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state14__press&hold 
door close__state11`
- `TM__state5__press&hold 
door open__state10`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state9__press&hold 
door close__state11`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (132):

- `AEX__state4__press cabin roof__press&hold 
door open__state5`
- `AEX__state4__press cabin roof__press&hold 
door close__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__release door open__state5`
- `AEX__state4__press cabin roof__release door close__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state13__press&hold 
door open__press cabin roof__state10`
- `AEX__state13__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state13__press&hold 
door open__press hall down__state10`
- `AEX__state13__press&hold 
door open__press cabin lobby__state10`
- `AEX__state13__press&hold 
door open__release door open__state10`
- `AEX__state13__press&hold 
door open__release door close__state10`
- `AEX__state13__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state13__press&hold 
door open__press alarm
button__state10`
- `AEX__state13__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state13__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state13__press&hold 
door open__press intercom__state10`
- `AEX__state13__press&hold 
door open__press hall up__state10`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press&hold 
door open__state9`
- `AEX__state5__press alarm
button__press&hold 
door close__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__release door open__state9`
- `AEX__state5__press alarm
button__release door close__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state14__press&hold 
door close__press cabin roof__state11`
- `AEX__state14__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state14__press&hold 
door close__press hall down__state11`
- `AEX__state14__press&hold 
door close__press cabin lobby__state11`
- `AEX__state14__press&hold 
door close__release door open__state11`
- `AEX__state14__press&hold 
door close__release door close__state11`
- `AEX__state14__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state14__press&hold 
door close__press alarm
button__state11`
- `AEX__state14__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state14__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state14__press&hold 
door close__press intercom__state11`
- `AEX__state14__press&hold 
door close__press hall up__state11`
- `AEX__state5__press&hold 
door open__press cabin roof__state10`
- `AEX__state5__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state5__press&hold 
door open__press hall down__state10`
- `AEX__state5__press&hold 
door open__press cabin lobby__state10`
- `AEX__state5__press&hold 
door open__release door open__state10`
- `AEX__state5__press&hold 
door open__release door close__state10`
- `AEX__state5__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state5__press&hold 
door open__press alarm
button__state10`
- `AEX__state5__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state5__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state5__press&hold 
door open__press intercom__state10`
- `AEX__state5__press&hold 
door open__press hall up__state10`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state9__press&hold 
door close__press cabin roof__state11`
- `AEX__state9__press&hold 
door close__press&hold 
door open__state11`
- `AEX__state9__press&hold 
door close__press hall down__state11`
- `AEX__state9__press&hold 
door close__press cabin lobby__state11`
- `AEX__state9__press&hold 
door close__release door open__state11`
- `AEX__state9__press&hold 
door close__release door close__state11`
- `AEX__state9__press&hold 
door close__press hall 
RoofDown__state11`
- `AEX__state9__press&hold 
door close__press alarm
button__state11`
- `AEX__state9__press&hold 
door close__press cabin 
[1-N] floor__state11`
- `AEX__state9__press&hold 
door close__press hall 
LobbyUp__state11`
- `AEX__state9__press&hold 
door close__press intercom__state11`
- `AEX__state9__press&hold 
door close__press hall up__state11`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press&hold 
door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__release door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__press cabin roof__press&hold 
door open__state5`
- `AEX__state3__press cabin roof__press&hold 
door close__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__release door open__state5`
- `AEX__state3__press cabin roof__release door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press&hold 
door open__state3`
- `AEX__state1__press hall down__press&hold 
door close__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__release door open__state3`
- `AEX__state1__press hall down__release door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 29

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 20 | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 63/180 = 35.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived state coverage** (13):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state4__read card__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (117):

- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__read card__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__read card__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press cabin 
executive floor__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press intercom__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press cabin 
executive floor__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press intercom__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 30

**Selected features:** selected = {ControlButtons, Intercom, MobileKey}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 18 | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived state coverage** (11):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__tap mobile
key__state6`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (88):

- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press intercom__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press intercom__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 31

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ManualDoorControl}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 22 | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived state coverage** (13):

- `TM__state4__press cabin roof__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state4__read card__state6`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (130):

- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press door open__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press alarm
button__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state4__read card__press door close__state6`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__read card__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press alarm
button__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__read card__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press door open__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press alarm
button__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state2__read card__press door close__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state1__press hall down__press door close__state3`

### Product 32

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 22 | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived state coverage** (13):

- `TM__state4__press cabin roof__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state4__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (130):

- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__enter PIN__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__enter PIN__press cabin roof__state6`
- `AEX__state3__enter PIN__press door open__state6`
- `AEX__state3__enter PIN__press hall down__state6`
- `AEX__state3__enter PIN__press hall 
RoofDown__state6`
- `AEX__state3__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state3__enter PIN__press cabin lobby__state6`
- `AEX__state3__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state3__enter PIN__press intercom__state6`
- `AEX__state3__enter PIN__press hall up__state6`
- `AEX__state3__enter PIN__press door close__state6`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state4__enter PIN__press cabin roof__state6`
- `AEX__state4__enter PIN__press door open__state6`
- `AEX__state4__enter PIN__press hall down__state6`
- `AEX__state4__enter PIN__press hall 
RoofDown__state6`
- `AEX__state4__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state4__enter PIN__press cabin lobby__state6`
- `AEX__state4__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state4__enter PIN__press intercom__state6`
- `AEX__state4__enter PIN__press hall up__state6`
- `AEX__state4__enter PIN__press door close__state6`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press intercom__state8`
- `AEX__state7__press door open__enter PIN__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state1__press hall down__press door close__state3`

### Product 33

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 26 | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 99/286 = 34.6% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived state coverage** (17):

- `TM__state4__press cabin roof__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__tap mobile
key__state6`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (187):

- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin 
executive floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press door close__state8`
- `AEX__state5__press door open__tap mobile
key__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press intercom__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin 
executive floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__tap mobile
key__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press door open__press cabin roof__state8`
- `AEX__state12__press door open__press hall down__state8`
- `AEX__state12__press door open__press cabin 
executive floor__state8`
- `AEX__state12__press door open__press cabin lobby__state8`
- `AEX__state12__press door open__press door close__state8`
- `AEX__state12__press door open__tap mobile
key__state8`
- `AEX__state12__press door open__press hall 
RoofDown__state8`
- `AEX__state12__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state12__press door open__press hall 
LobbyUp__state8`
- `AEX__state12__press door open__press intercom__state8`
- `AEX__state12__press door open__press hall up__state8`
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
- `AEX__state6__press cabin lobby__tap mobile
key__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press door open__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press door close__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press intercom__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin 
executive floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__tap mobile
key__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__press intercom__state7`
- `AEX__state8__press door close__press hall up__state7`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__press door open__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__press door close__state9`
- `AEX__state12__press intercom__tap mobile
key__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press door open__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press door close__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press intercom__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 34

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, ManualDoorControl}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 26 | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 99/286 = 34.6% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived state coverage** (17):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door open__state8`
- `TM__state4__read card__state6`
- `TM__state5__press door close__state7`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (187):

- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__press door open__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__press door close__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__read card__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__read card__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__read card__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin 
executive floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press door close__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press alarm
button__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__read card__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state4__read card__press cabin roof__state6`
- `AEX__state4__read card__press door open__state6`
- `AEX__state4__read card__press hall down__state6`
- `AEX__state4__read card__press cabin 
executive floor__state6`
- `AEX__state4__read card__press cabin lobby__state6`
- `AEX__state4__read card__press door close__state6`
- `AEX__state4__read card__press hall 
RoofDown__state6`
- `AEX__state4__read card__press alarm
button__state6`
- `AEX__state4__read card__press cabin 
[1-N] floor__state6`
- `AEX__state4__read card__press hall 
LobbyUp__state6`
- `AEX__state4__read card__press hall up__state6`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin 
executive floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__read card__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__read card__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press door open__press cabin roof__state8`
- `AEX__state12__press door open__press hall down__state8`
- `AEX__state12__press door open__press cabin 
executive floor__state8`
- `AEX__state12__press door open__press cabin lobby__state8`
- `AEX__state12__press door open__press door close__state8`
- `AEX__state12__press door open__press hall 
RoofDown__state8`
- `AEX__state12__press door open__press alarm
button__state8`
- `AEX__state12__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state12__press door open__read card__state8`
- `AEX__state12__press door open__press hall 
LobbyUp__state8`
- `AEX__state12__press door open__press hall up__state8`
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__read card__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__read card__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin 
executive floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press alarm
button__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__read card__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__press hall up__state7`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__read card__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__read card__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__read card__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__read card__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state2__read card__press cabin roof__state6`
- `AEX__state2__read card__press door open__state6`
- `AEX__state2__read card__press hall down__state6`
- `AEX__state2__read card__press cabin 
executive floor__state6`
- `AEX__state2__read card__press cabin lobby__state6`
- `AEX__state2__read card__press door close__state6`
- `AEX__state2__read card__press hall 
RoofDown__state6`
- `AEX__state2__read card__press alarm
button__state6`
- `AEX__state2__read card__press cabin 
[1-N] floor__state6`
- `AEX__state2__read card__press hall 
LobbyUp__state6`
- `AEX__state2__read card__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__read card__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 35

**Selected features:** selected = {Alarm, ControlButtons, Intercom, MobileKey}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 19 | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% |
| ActionExchange | 171 | 63/171 = 36.8% | 171/171 = 100.0% | 171/171 = 100.0% |

**TransitionMissing — survived state coverage** (12):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__tap mobile
key__state6`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (108):

- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press alarm
button__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press intercom__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__tap mobile
key__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press alarm
button__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press intercom__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 36

**Selected features:** selected = {Alarm, ControlButtons, Intercom, PinPad}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 19 | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% |
| ActionExchange | 171 | 63/171 = 36.8% | 171/171 = 100.0% | 171/171 = 100.0% |

**TransitionMissing — survived state coverage** (12):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state4__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (108):

- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__enter PIN__press cabin roof__state6`
- `AEX__state3__enter PIN__press hall down__state6`
- `AEX__state3__enter PIN__press hall 
RoofDown__state6`
- `AEX__state3__enter PIN__press alarm
button__state6`
- `AEX__state3__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state3__enter PIN__press cabin lobby__state6`
- `AEX__state3__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state3__enter PIN__press intercom__state6`
- `AEX__state3__enter PIN__press hall up__state6`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__enter PIN__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state4__enter PIN__press cabin roof__state6`
- `AEX__state4__enter PIN__press hall down__state6`
- `AEX__state4__enter PIN__press hall 
RoofDown__state6`
- `AEX__state4__enter PIN__press alarm
button__state6`
- `AEX__state4__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state4__enter PIN__press cabin lobby__state6`
- `AEX__state4__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state4__enter PIN__press intercom__state6`
- `AEX__state4__enter PIN__press hall up__state6`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 37

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 20 | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 63/180 = 35.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived state coverage** (13):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__tap mobile
key__state6`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (117):

- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__tap mobile
key__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__tap mobile
key__press cabin roof__state6`
- `AEX__state2__tap mobile
key__press hall down__state6`
- `AEX__state2__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state2__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state2__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state2__tap mobile
key__press cabin lobby__state6`
- `AEX__state2__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state2__tap mobile
key__press intercom__state6`
- `AEX__state2__tap mobile
key__press hall up__state6`
- `AEX__state4__press cabin roof__tap mobile
key__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__tap mobile
key__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__tap mobile
key__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__tap mobile
key__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state3__press cabin roof__tap mobile
key__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state4__tap mobile
key__press cabin roof__state6`
- `AEX__state4__tap mobile
key__press hall down__state6`
- `AEX__state4__tap mobile
key__press hall 
RoofDown__state6`
- `AEX__state4__tap mobile
key__press cabin 
executive floor__state6`
- `AEX__state4__tap mobile
key__press cabin 
[1-N] floor__state6`
- `AEX__state4__tap mobile
key__press cabin lobby__state6`
- `AEX__state4__tap mobile
key__press hall 
LobbyUp__state6`
- `AEX__state4__tap mobile
key__press intercom__state6`
- `AEX__state4__tap mobile
key__press hall up__state6`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__tap mobile
key__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__tap mobile
key__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 38

**Selected features:** selected = {Alarm, ControlButtons, PinPad}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 18 | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived state coverage** (11):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state4__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (88):

- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__enter PIN__press cabin roof__state6`
- `AEX__state3__enter PIN__press hall down__state6`
- `AEX__state3__enter PIN__press hall 
RoofDown__state6`
- `AEX__state3__enter PIN__press alarm
button__state6`
- `AEX__state3__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state3__enter PIN__press cabin lobby__state6`
- `AEX__state3__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state3__enter PIN__press hall up__state6`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state4__enter PIN__press cabin roof__state6`
- `AEX__state4__enter PIN__press hall down__state6`
- `AEX__state4__enter PIN__press hall 
RoofDown__state6`
- `AEX__state4__enter PIN__press alarm
button__state6`
- `AEX__state4__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state4__enter PIN__press cabin lobby__state6`
- `AEX__state4__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state4__enter PIN__press hall up__state6`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 39

**Selected features:** selected = {ControlButtons, Intercom, PinPad}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 18 | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived state coverage** (11):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state4__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`

**ActionExchange — survived state coverage** (88):

- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__enter PIN__press cabin roof__state6`
- `AEX__state3__enter PIN__press hall down__state6`
- `AEX__state3__enter PIN__press hall 
RoofDown__state6`
- `AEX__state3__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state3__enter PIN__press cabin lobby__state6`
- `AEX__state3__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state3__enter PIN__press intercom__state6`
- `AEX__state3__enter PIN__press hall up__state6`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state4__enter PIN__press cabin roof__state6`
- `AEX__state4__enter PIN__press hall down__state6`
- `AEX__state4__enter PIN__press hall 
RoofDown__state6`
- `AEX__state4__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state4__enter PIN__press cabin lobby__state6`
- `AEX__state4__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state4__enter PIN__press intercom__state6`
- `AEX__state4__enter PIN__press hall up__state6`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`

### Product 40

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 22 | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived state coverage** (13):

- `TM__state4__press cabin roof__state5`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state4__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (130):

- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__enter PIN__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__enter PIN__press cabin roof__state6`
- `AEX__state3__enter PIN__press door open__state6`
- `AEX__state3__enter PIN__press hall down__state6`
- `AEX__state3__enter PIN__press hall 
RoofDown__state6`
- `AEX__state3__enter PIN__press alarm
button__state6`
- `AEX__state3__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state3__enter PIN__press cabin lobby__state6`
- `AEX__state3__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state3__enter PIN__press hall up__state6`
- `AEX__state3__enter PIN__press door close__state6`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state4__enter PIN__press cabin roof__state6`
- `AEX__state4__enter PIN__press door open__state6`
- `AEX__state4__enter PIN__press hall down__state6`
- `AEX__state4__enter PIN__press hall 
RoofDown__state6`
- `AEX__state4__enter PIN__press alarm
button__state6`
- `AEX__state4__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state4__enter PIN__press cabin lobby__state6`
- `AEX__state4__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state4__enter PIN__press hall up__state6`
- `AEX__state4__enter PIN__press door close__state6`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press alarm
button__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__enter PIN__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press hall up__state3`
- `AEX__state1__press hall down__press door close__state3`

### Product 41

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 28 | 11/28 = 39.3% | 28/28 = 100.0% | 28/28 = 100.0% |
| ActionExchange | 336 | 132/336 = 39.3% | 336/336 = 100.0% | 336/336 = 100.0% |

**TransitionMissing — survived state coverage** (17):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state12__press door close__state7`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (204):

- `AEX__state12__press alarm
button__press cabin roof__state9`
- `AEX__state12__press alarm
button__press door open__state9`
- `AEX__state12__press alarm
button__press hall down__state9`
- `AEX__state12__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state12__press alarm
button__press cabin lobby__state9`
- `AEX__state12__press alarm
button__enter PIN__state9`
- `AEX__state12__press alarm
button__press door close__state9`
- `AEX__state12__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state12__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state12__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state12__press alarm
button__press intercom__state9`
- `AEX__state12__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state12__press door close__press cabin roof__state7`
- `AEX__state12__press door close__press door open__state7`
- `AEX__state12__press door close__press hall down__state7`
- `AEX__state12__press door close__press cabin 
executive floor__state7`
- `AEX__state12__press door close__press cabin lobby__state7`
- `AEX__state12__press door close__enter PIN__state7`
- `AEX__state12__press door close__press hall 
RoofDown__state7`
- `AEX__state12__press door close__press alarm
button__state7`
- `AEX__state12__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state12__press door close__press hall 
LobbyUp__state7`
- `AEX__state12__press door close__press intercom__state7`
- `AEX__state12__press door close__press hall up__state7`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press door open__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press cabin 
executive floor__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__enter PIN__state9`
- `AEX__state5__press alarm
button__press door close__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin 
executive floor__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__enter PIN__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state12__press door open__press cabin roof__state8`
- `AEX__state12__press door open__press hall down__state8`
- `AEX__state12__press door open__press cabin 
executive floor__state8`
- `AEX__state12__press door open__press cabin lobby__state8`
- `AEX__state12__press door open__enter PIN__state8`
- `AEX__state12__press door open__press door close__state8`
- `AEX__state12__press door open__press hall 
RoofDown__state8`
- `AEX__state12__press door open__press alarm
button__state8`
- `AEX__state12__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state12__press door open__press hall 
LobbyUp__state8`
- `AEX__state12__press door open__press intercom__state8`
- `AEX__state12__press door open__press hall up__state8`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__enter PIN__press cabin roof__state6`
- `AEX__state3__enter PIN__press door open__state6`
- `AEX__state3__enter PIN__press hall down__state6`
- `AEX__state3__enter PIN__press cabin 
executive floor__state6`
- `AEX__state3__enter PIN__press cabin lobby__state6`
- `AEX__state3__enter PIN__press door close__state6`
- `AEX__state3__enter PIN__press hall 
RoofDown__state6`
- `AEX__state3__enter PIN__press alarm
button__state6`
- `AEX__state3__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state3__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state3__enter PIN__press intercom__state6`
- `AEX__state3__enter PIN__press hall up__state6`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin 
executive floor__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state12__press intercom__press cabin roof__state9`
- `AEX__state12__press intercom__press door open__state9`
- `AEX__state12__press intercom__press hall down__state9`
- `AEX__state12__press intercom__press cabin 
executive floor__state9`
- `AEX__state12__press intercom__press cabin lobby__state9`
- `AEX__state12__press intercom__enter PIN__state9`
- `AEX__state12__press intercom__press door close__state9`
- `AEX__state12__press intercom__press hall 
RoofDown__state9`
- `AEX__state12__press intercom__press alarm
button__state9`
- `AEX__state12__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state12__press intercom__press hall 
LobbyUp__state9`
- `AEX__state12__press intercom__press hall up__state9`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin 
executive floor__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin 
executive floor__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__enter PIN__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press alarm
button__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press intercom__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin 
executive floor__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`

### Product 42

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 23 | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% |
| ActionExchange | 253 | 99/253 = 39.1% | 253/253 = 100.0% | 253/253 = 100.0% |

**TransitionMissing — survived state coverage** (14):

- `TM__state4__press cabin roof__state5`
- `TM__state5__press alarm
button__state9`
- `TM__state4__press cabin 
[1-N] floor__state5`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state3__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state4__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**ActionExchange — survived state coverage** (154):

- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__enter PIN__state5`
- `AEX__state4__press cabin roof__press door close__state5`
- `AEX__state4__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state4__press cabin roof__press alarm
button__state5`
- `AEX__state4__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state4__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin roof__press intercom__state5`
- `AEX__state4__press cabin roof__press hall up__state5`
- `AEX__state5__press alarm
button__press cabin roof__state9`
- `AEX__state5__press alarm
button__press door open__state9`
- `AEX__state5__press alarm
button__press hall down__state9`
- `AEX__state5__press alarm
button__press cabin lobby__state9`
- `AEX__state5__press alarm
button__enter PIN__state9`
- `AEX__state5__press alarm
button__press door close__state9`
- `AEX__state5__press alarm
button__press hall 
RoofDown__state9`
- `AEX__state5__press alarm
button__press cabin 
[1-N] floor__state9`
- `AEX__state5__press alarm
button__press hall 
LobbyUp__state9`
- `AEX__state5__press alarm
button__press intercom__state9`
- `AEX__state5__press alarm
button__press hall up__state9`
- `AEX__state4__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state4__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state4__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state4__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state4__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state4__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state4__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state5__press door close__press cabin roof__state7`
- `AEX__state5__press door close__press door open__state7`
- `AEX__state5__press door close__press hall down__state7`
- `AEX__state5__press door close__press cabin lobby__state7`
- `AEX__state5__press door close__enter PIN__state7`
- `AEX__state5__press door close__press hall 
RoofDown__state7`
- `AEX__state5__press door close__press alarm
button__state7`
- `AEX__state5__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state5__press door close__press hall 
LobbyUp__state7`
- `AEX__state5__press door close__press intercom__state7`
- `AEX__state5__press door close__press hall up__state7`
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__enter PIN__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state3__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state3__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state3__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state3__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state3__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state6__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state6__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state6__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state6__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state6__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state3__enter PIN__press cabin roof__state6`
- `AEX__state3__enter PIN__press door open__state6`
- `AEX__state3__enter PIN__press hall down__state6`
- `AEX__state3__enter PIN__press cabin lobby__state6`
- `AEX__state3__enter PIN__press door close__state6`
- `AEX__state3__enter PIN__press hall 
RoofDown__state6`
- `AEX__state3__enter PIN__press alarm
button__state6`
- `AEX__state3__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state3__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state3__enter PIN__press intercom__state6`
- `AEX__state3__enter PIN__press hall up__state6`
- `AEX__state2__press cabin 
[1-N] floor__press cabin roof__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door open__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall down__state5`
- `AEX__state2__press cabin 
[1-N] floor__press cabin lobby__state5`
- `AEX__state2__press cabin 
[1-N] floor__enter PIN__state5`
- `AEX__state2__press cabin 
[1-N] floor__press door close__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
RoofDown__state5`
- `AEX__state2__press cabin 
[1-N] floor__press alarm
button__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin 
[1-N] floor__press intercom__state5`
- `AEX__state2__press cabin 
[1-N] floor__press hall up__state5`
- `AEX__state6__press cabin roof__press door open__state5`
- `AEX__state6__press cabin roof__press hall down__state5`
- `AEX__state6__press cabin roof__press cabin lobby__state5`
- `AEX__state6__press cabin roof__enter PIN__state5`
- `AEX__state6__press cabin roof__press door close__state5`
- `AEX__state6__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state6__press cabin roof__press alarm
button__state5`
- `AEX__state6__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin roof__press intercom__state5`
- `AEX__state6__press cabin roof__press hall up__state5`
- `AEX__state4__enter PIN__press cabin roof__state6`
- `AEX__state4__enter PIN__press door open__state6`
- `AEX__state4__enter PIN__press hall down__state6`
- `AEX__state4__enter PIN__press cabin lobby__state6`
- `AEX__state4__enter PIN__press door close__state6`
- `AEX__state4__enter PIN__press hall 
RoofDown__state6`
- `AEX__state4__enter PIN__press alarm
button__state6`
- `AEX__state4__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state4__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state4__enter PIN__press intercom__state6`
- `AEX__state4__enter PIN__press hall up__state6`
- `AEX__state3__press cabin roof__press door open__state5`
- `AEX__state3__press cabin roof__press hall down__state5`
- `AEX__state3__press cabin roof__press cabin lobby__state5`
- `AEX__state3__press cabin roof__enter PIN__state5`
- `AEX__state3__press cabin roof__press door close__state5`
- `AEX__state3__press cabin roof__press hall 
RoofDown__state5`
- `AEX__state3__press cabin roof__press alarm
button__state5`
- `AEX__state3__press cabin roof__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin roof__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin roof__press intercom__state5`
- `AEX__state3__press cabin roof__press hall up__state5`
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__enter PIN__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
- `AEX__state7__press door open__press alarm
button__state8`
- `AEX__state7__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state7__press door open__press hall 
LobbyUp__state8`
- `AEX__state7__press door open__press intercom__state8`
- `AEX__state7__press door open__press hall up__state8`
- `AEX__state1__press hall down__press cabin roof__state3`
- `AEX__state1__press hall down__press door open__state3`
- `AEX__state1__press hall down__press cabin lobby__state3`
- `AEX__state1__press hall down__enter PIN__state3`
- `AEX__state1__press hall down__press door close__state3`
- `AEX__state1__press hall down__press hall 
RoofDown__state3`
- `AEX__state1__press hall down__press alarm
button__state3`
- `AEX__state1__press hall down__press cabin 
[1-N] floor__state3`
- `AEX__state1__press hall down__press hall 
LobbyUp__state3`
- `AEX__state1__press hall down__press intercom__state3`
- `AEX__state1__press hall down__press hall up__state3`
---

## Elevator summary (aggregate over 42 products)

| Operator | Mutants | State-cov kills | Transition-cov kills | Pair-cov kills |
|---|---|---|---|---|
| TransitionMissing | 926 | 366/926 = 39.5% | 926/926 = 100.0% | 926/926 = 100.0% |
| ActionExchange | 9586 | 3824/9586 = 39.9% | 9586/9586 = 100.0% | 9586/9586 = 100.0% |

Total products: 42.
