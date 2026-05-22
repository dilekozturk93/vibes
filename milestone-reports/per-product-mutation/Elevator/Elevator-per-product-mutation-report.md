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

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 18 | 7/18 = 38.9% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 56/144 = 38.9% | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (11):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state2__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (11):

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

**ActionExchange — survived family-level state coverage (Devroey)** (88):

- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__enter PIN__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__enter PIN__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press alarm
button__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press hall up__state6`
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

**ActionExchange — survived product state coverage** (88):

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

### Product 2

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 30 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 28 | 12/28 = 42.9% | 9/28 = 32.1% | 28/28 = 100.0% | 28/28 = 100.0% |
| ActionExchange | 336 | 144/336 = 42.9% | 108/336 = 32.1% | 336/336 = 100.0% | 336/336 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (16):

- `TM__state12__press alarm
button__state9`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (19):

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

**ActionExchange — survived family-level state coverage (Devroey)** (192):

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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press door close__state9`
- `AEX__state5__press intercom__tap mobile
key__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
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
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin 
executive floor__state8`
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

**ActionExchange — survived product state coverage** (228):

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

### Product 3

**Selected features:** selected = {Alarm, CardReader, ControlButtons}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 18 | 8/18 = 44.4% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 64/144 = 44.4% | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (10):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin roof__state5`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (11):

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

**ActionExchange — survived family-level state coverage (Devroey)** (80):

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
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press alarm
button__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press hall up__state6`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (88):

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

### Product 4

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 28 | 8/28 = 28.6% | 11/28 = 39.3% | 28/28 = 100.0% | 28/28 = 100.0% |
| ActionExchange | 336 | 96/336 = 28.6% | 132/336 = 39.3% | 336/336 = 100.0% | 336/336 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (20):

- `TM__state12__press alarm
button__state9`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state12__press door close__state7`
- `TM__state2__enter PIN__state6`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (17):

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

**ActionExchange — survived family-level state coverage (Devroey)** (240):

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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__enter PIN__state9`
- `AEX__state5__press intercom__press door close__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press door open__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press cabin 
executive floor__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press door close__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press alarm
button__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press intercom__state6`
- `AEX__state2__enter PIN__press hall up__state6`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin 
executive floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__enter PIN__state8`
- `AEX__state5__press door open__press door close__state8`
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
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__enter PIN__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin 
executive floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__enter PIN__state7`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__enter PIN__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
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

**ActionExchange — survived product state coverage** (204):

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

### Product 5

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 13 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 23 | 7/23 = 30.4% | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% |
| ActionExchange | 253 | 77/253 = 30.4% | 99/253 = 39.1% | 253/253 = 100.0% | 253/253 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (16):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__enter PIN__state6`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (14):

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

**ActionExchange — survived family-level state coverage (Devroey)** (176):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__enter PIN__state9`
- `AEX__state5__press intercom__press door close__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press door open__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press door close__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press alarm
button__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press intercom__state6`
- `AEX__state2__enter PIN__press hall up__state6`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__enter PIN__state8`
- `AEX__state5__press door open__press door close__state8`
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
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__enter PIN__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__enter PIN__state7`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__enter PIN__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
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

**ActionExchange — survived product state coverage** (154):

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

### Product 6

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Repaired FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 24 | 7/24 = 29.2% | 13/24 = 54.2% | 24/24 = 100.0% | 24/24 = 100.0% |
| ActionExchange | 312 | 91/312 = 29.2% | 169/312 = 54.2% | 312/312 = 100.0% | 312/312 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (17):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state13__press&hold 
door open__state10`
- `TM__state14__press&hold 
door close__state11`
- `TM__state9__press&hold 
door open__state10`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state5__press&hold 
door open__state10`
- `TM__state2__press cabin lobby__state5`
- `TM__state9__press&hold 
door close__state11`
- `TM__state10__release door open__state13`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (11):

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

**ActionExchange — survived family-level state coverage (Devroey)** (221):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press&hold 
door open__state9`
- `AEX__state5__press intercom__press&hold 
door close__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__release door open__state9`
- `AEX__state5__press intercom__press door close__state9`
- `AEX__state5__press intercom__release door close__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
- `AEX__state4__press cabin roof__press&hold 
door open__state5`
- `AEX__state4__press cabin roof__press&hold 
door close__state5`
- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__release door open__state5`
- `AEX__state4__press cabin roof__press door close__state5`
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
- `AEX__state9__press&hold 
door open__press cabin roof__state10`
- `AEX__state9__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state9__press&hold 
door open__press door open__state10`
- `AEX__state9__press&hold 
door open__press hall down__state10`
- `AEX__state9__press&hold 
door open__press cabin lobby__state10`
- `AEX__state9__press&hold 
door open__release door open__state10`
- `AEX__state9__press&hold 
door open__press door close__state10`
- `AEX__state9__press&hold 
door open__release door close__state10`
- `AEX__state9__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state9__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state9__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state9__press&hold 
door open__press intercom__state10`
- `AEX__state9__press&hold 
door open__press hall up__state10`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press&hold 
door open__state8`
- `AEX__state5__press door open__press&hold 
door close__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__release door open__state8`
- `AEX__state5__press door open__press door close__state8`
- `AEX__state5__press door open__release door close__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press intercom__state8`
- `AEX__state5__press door open__press hall up__state8`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press&hold 
door open__state5`
- `AEX__state2__press cabin lobby__press&hold 
door close__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__release door open__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__release door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
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
- `AEX__state10__release door open__press cabin roof__state13`
- `AEX__state10__release door open__press&hold 
door open__state13`
- `AEX__state10__release door open__press&hold 
door close__state13`
- `AEX__state10__release door open__press door open__state13`
- `AEX__state10__release door open__press hall down__state13`
- `AEX__state10__release door open__press cabin lobby__state13`
- `AEX__state10__release door open__press door close__state13`
- `AEX__state10__release door open__release door close__state13`
- `AEX__state10__release door open__press hall 
RoofDown__state13`
- `AEX__state10__release door open__press cabin 
[1-N] floor__state13`
- `AEX__state10__release door open__press hall 
LobbyUp__state13`
- `AEX__state10__release door open__press intercom__state13`
- `AEX__state10__release door open__press hall up__state13`
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press&hold 
door open__state7`
- `AEX__state8__press door close__press&hold 
door close__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__release door open__state7`
- `AEX__state8__press door close__release door close__state7`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press&hold 
door open__state5`
- `AEX__state3__press cabin lobby__press&hold 
door close__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__release door open__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin lobby__release door close__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
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

**ActionExchange — survived product state coverage** (143):

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

### Product 7

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService}

**Repaired FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 20 | 10/20 = 50.0% | 10/20 = 50.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 220 | 110/220 = 50.0% | 110/220 = 50.0% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (10):

- `TM__state9__press&hold 
door close__state11`
- `TM__state4__press cabin roof__state5`
- `TM__state13__press&hold 
door open__state10`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state14__press&hold 
door close__state11`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state5__press&hold 
door open__state10`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (10):

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

**ActionExchange — survived family-level state coverage (Devroey)** (110):

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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press&hold 
door open__state5`
- `AEX__state3__press cabin lobby__press&hold 
door close__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__release door open__state5`
- `AEX__state3__press cabin lobby__release door close__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press&hold 
door open__state5`
- `AEX__state2__press cabin lobby__press&hold 
door close__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__release door open__state5`
- `AEX__state2__press cabin lobby__release door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (110):

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

### Product 8

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 26 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 22 | 10/22 = 45.5% | 7/22 = 31.8% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 100/220 = 45.5% | 70/220 = 31.8% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (12):

- `TM__state12__press alarm
button__state9`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (15):

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

**ActionExchange — survived family-level state coverage (Devroey)** (120):

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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__tap mobile
key__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
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
- `AEX__state2__press cabin lobby__press hall up__state5`
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

**ActionExchange — survived product state coverage** (150):

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

### Product 9

**Selected features:** selected = {CardReader, ControlButtons, Intercom, ManualDoorControl}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 22 | 7/22 = 31.8% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 70/220 = 31.8% | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (15):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (13):

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

**ActionExchange — survived family-level state coverage (Devroey)** (150):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__read card__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
- `AEX__state5__press intercom__press door close__state9`
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
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__read card__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press intercom__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state5__press door open__press door close__state8`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__read card__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__press intercom__state7`
- `AEX__state8__press door close__press hall up__state7`
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press door open__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press intercom__state6`
- `AEX__state3__read card__press hall up__state6`
- `AEX__state3__read card__press door close__state6`
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

**ActionExchange — survived product state coverage** (130):

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

### Product 10

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 20 | 7/20 = 35.0% | 8/20 = 40.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 63/180 = 35.0% | 72/180 = 40.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (13):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state2__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (12):

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

**ActionExchange — survived family-level state coverage (Devroey)** (117):

- `AEX__state6__press cabin lobby__press cabin roof__state5`
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
- `AEX__state6__press cabin lobby__enter PIN__state5`
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
executive floor__state6`
- `AEX__state3__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state3__enter PIN__press cabin lobby__state6`
- `AEX__state3__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state3__enter PIN__press intercom__state6`
- `AEX__state3__enter PIN__press hall up__state6`
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__enter PIN__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press cabin 
executive floor__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press intercom__state6`
- `AEX__state2__enter PIN__press hall up__state6`
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

**ActionExchange — survived product state coverage** (108):

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

### Product 11

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Repaired FTS:** 12 states, 31 transitions (25 real / 6 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 25 | 10/25 = 40.0% | 13/25 = 52.0% | 25/25 = 100.0% | 25/25 = 100.0% |
| ActionExchange | 350 | 140/350 = 40.0% | 182/350 = 52.0% | 350/350 = 100.0% | 350/350 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (15):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state13__press&hold 
door open__state10`
- `TM__state14__press&hold 
door close__state11`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state5__press&hold 
door open__state10`
- `TM__state2__press cabin lobby__state5`
- `TM__state9__press&hold 
door close__state11`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (12):

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

**ActionExchange — survived family-level state coverage (Devroey)** (210):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press&hold 
door open__state9`
- `AEX__state5__press intercom__press&hold 
door close__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__release door open__state9`
- `AEX__state5__press intercom__press door close__state9`
- `AEX__state5__press intercom__release door close__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
- `AEX__state4__press cabin roof__press&hold 
door open__state5`
- `AEX__state4__press cabin roof__press&hold 
door close__state5`
- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__release door open__state5`
- `AEX__state4__press cabin roof__press door close__state5`
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
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press&hold 
door open__state8`
- `AEX__state5__press door open__press&hold 
door close__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__release door open__state8`
- `AEX__state5__press door open__press door close__state8`
- `AEX__state5__press door open__release door close__state8`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press&hold 
door open__state5`
- `AEX__state2__press cabin lobby__press&hold 
door close__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__release door open__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__release door close__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press&hold 
door open__state7`
- `AEX__state8__press door close__press&hold 
door close__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__release door open__state7`
- `AEX__state8__press door close__release door close__state7`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press&hold 
door open__state5`
- `AEX__state3__press cabin lobby__press&hold 
door close__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__release door open__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin lobby__release door close__state5`
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

**ActionExchange — survived product state coverage** (168):

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

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 29 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 26 | 11/26 = 42.3% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 121/286 = 42.3% | 99/286 = 34.6% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (15):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (17):

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

**ActionExchange — survived family-level state coverage (Devroey)** (165):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press door close__state9`
- `AEX__state5__press intercom__tap mobile
key__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
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
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin 
executive floor__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state7__press door open__tap mobile
key__state8`
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

**ActionExchange — survived product state coverage** (187):

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

### Product 13

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 28 | 8/28 = 28.6% | 9/28 = 32.1% | 28/28 = 100.0% | 28/28 = 100.0% |
| ActionExchange | 336 | 96/336 = 28.6% | 108/336 = 32.1% | 336/336 = 100.0% | 336/336 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (20):

- `TM__state12__press alarm
button__state9`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state12__press door close__state7`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
executive floor__state12`
- `TM__state8__press door close__state7`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (19):

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

**ActionExchange — survived family-level state coverage (Devroey)** (240):

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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press door close__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__read card__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state12__press door close__press cabin roof__state7`
- `AEX__state12__press door close__press door open__state7`
- `AEX__state12__press door close__press hall down__state7`
- `AEX__state12__press door close__press cabin 
executive floor__state7`
- `AEX__state12__press door close__press cabin lobby__state7`
- `AEX__state12__press door close__press hall 
RoofDown__state7`
- `AEX__state12__press door close__press alarm
button__state7`
- `AEX__state12__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state12__press door close__read card__state7`
- `AEX__state12__press door close__press hall 
LobbyUp__state7`
- `AEX__state12__press door close__press intercom__state7`
- `AEX__state12__press door close__press hall up__state7`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
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
- `AEX__state6__press cabin 
executive floor__press cabin roof__state12`
- `AEX__state6__press cabin 
executive floor__press door open__state12`
- `AEX__state6__press cabin 
executive floor__press hall down__state12`
- `AEX__state6__press cabin 
executive floor__press cabin lobby__state12`
- `AEX__state6__press cabin 
executive floor__press door close__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
RoofDown__state12`
- `AEX__state6__press cabin 
executive floor__press alarm
button__state12`
- `AEX__state6__press cabin 
executive floor__press cabin 
[1-N] floor__state12`
- `AEX__state6__press cabin 
executive floor__read card__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
LobbyUp__state12`
- `AEX__state6__press cabin 
executive floor__press intercom__state12`
- `AEX__state6__press cabin 
executive floor__press hall up__state12`
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
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press door open__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press cabin 
executive floor__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press door close__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press alarm
button__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press intercom__state6`
- `AEX__state3__read card__press hall up__state6`
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
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin 
executive floor__state8`
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

**ActionExchange — survived product state coverage** (228):

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

### Product 14

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 20 | 8/20 = 40.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 72/180 = 40.0% | 63/180 = 35.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (12):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
executive floor__state12`
- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (13):

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

**ActionExchange — survived family-level state coverage (Devroey)** (108):

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
- `AEX__state6__press cabin 
executive floor__press cabin roof__state12`
- `AEX__state6__press cabin 
executive floor__press hall down__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
RoofDown__state12`
- `AEX__state6__press cabin 
executive floor__press alarm
button__state12`
- `AEX__state6__press cabin 
executive floor__press cabin 
[1-N] floor__state12`
- `AEX__state6__press cabin 
executive floor__read card__state12`
- `AEX__state6__press cabin 
executive floor__press cabin lobby__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
LobbyUp__state12`
- `AEX__state6__press cabin 
executive floor__press hall up__state12`
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
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press cabin 
executive floor__state6`
- `AEX__state3__read card__press alarm
button__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press hall up__state6`
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
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (117):

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

### Product 15

**Selected features:** selected = {Alarm, ControlButtons, Intercom, MobileKey}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 19 | 8/19 = 42.1% | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% |
| ActionExchange | 171 | 72/171 = 42.1% | 63/171 = 36.8% | 171/171 = 100.0% | 171/171 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (11):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (12):

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

**ActionExchange — survived family-level state coverage (Devroey)** (99):

- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__tap mobile
key__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__tap mobile
key__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
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
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (108):

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

### Product 16

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 16 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 22 | 7/22 = 31.8% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 70/220 = 31.8% | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (15):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (13):

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

**ActionExchange — survived family-level state coverage (Devroey)** (150):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__tap mobile
key__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
- `AEX__state5__press intercom__press door close__state9`
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
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__tap mobile
key__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press intercom__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state5__press door open__press door close__state8`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__tap mobile
key__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__tap mobile
key__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__press intercom__state7`
- `AEX__state8__press door close__press hall up__state7`
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

**ActionExchange — survived product state coverage** (130):

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

### Product 17

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 25 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 20 | 9/20 = 45.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 81/180 = 45.0% | 63/180 = 35.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (11):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (13):

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

**ActionExchange — survived family-level state coverage (Devroey)** (99):

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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__tap mobile
key__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
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
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (117):

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

### Product 18

**Selected features:** selected = {CardReader, ControlButtons, Intercom}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 18 | 7/18 = 38.9% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 56/144 = 38.9% | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (11):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (11):

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

**ActionExchange — survived family-level state coverage (Devroey)** (88):

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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__read card__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press intercom__state6`
- `AEX__state3__read card__press hall up__state6`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (88):

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

### Product 19

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 28 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 26 | 11/26 = 42.3% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 121/286 = 42.3% | 99/286 = 34.6% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (15):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (17):

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

**ActionExchange — survived family-level state coverage (Devroey)** (165):

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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
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
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin 
executive floor__state8`
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
- `AEX__state7__press door open__press hall up__state8`
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

**ActionExchange — survived product state coverage** (187):

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

### Product 20

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, PinPad}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 20 | 8/20 = 40.0% | 8/20 = 40.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 72/180 = 40.0% | 72/180 = 40.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (12):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state12__press alarm
button__state9`
- `TM__state3__enter PIN__state6`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state2__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (12):

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

**ActionExchange — survived family-level state coverage (Devroey)** (108):

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
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__enter PIN__state5`
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
- `AEX__state3__enter PIN__press cabin roof__state6`
- `AEX__state3__enter PIN__press hall down__state6`
- `AEX__state3__enter PIN__press hall 
RoofDown__state6`
- `AEX__state3__enter PIN__press cabin 
executive floor__state6`
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
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press cabin 
executive floor__state6`
- `AEX__state2__enter PIN__press alarm
button__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press hall up__state6`
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

**ActionExchange — survived product state coverage** (108):

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

### Product 21

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom}

**Repaired FTS:** 10 states, 25 transitions (21 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 21 | 10/21 = 47.6% | 10/21 = 47.6% | 21/21 = 100.0% | 21/21 = 100.0% |
| ActionExchange | 252 | 120/252 = 47.6% | 120/252 = 47.6% | 252/252 = 100.0% | 252/252 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (11):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state13__press&hold 
door open__state10`
- `TM__state14__press&hold 
door close__state11`
- `TM__state5__press&hold 
door open__state10`
- `TM__state2__press cabin lobby__state5`
- `TM__state9__press&hold 
door close__state11`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (11):

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

**ActionExchange — survived family-level state coverage (Devroey)** (132):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press&hold 
door open__state9`
- `AEX__state5__press intercom__press&hold 
door close__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__release door open__state9`
- `AEX__state5__press intercom__release door close__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press&hold 
door open__state5`
- `AEX__state2__press cabin lobby__press&hold 
door close__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__release door open__state5`
- `AEX__state2__press cabin lobby__release door close__state5`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press&hold 
door open__state5`
- `AEX__state3__press cabin lobby__press&hold 
door close__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__release door open__state5`
- `AEX__state3__press cabin lobby__release door close__state5`
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

**ActionExchange — survived product state coverage** (132):

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

### Product 22

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, ManualDoorControl}

**Repaired FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 18 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 24 | 10/24 = 41.7% | 13/24 = 54.2% | 24/24 = 100.0% | 24/24 = 100.0% |
| ActionExchange | 312 | 130/312 = 41.7% | 169/312 = 54.2% | 312/312 = 100.0% | 312/312 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (14):

- `TM__state4__press cabin roof__state5`
- `TM__state13__press&hold 
door open__state10`
- `TM__state14__press&hold 
door close__state11`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state5__press&hold 
door open__state10`
- `TM__state2__press cabin lobby__state5`
- `TM__state9__press&hold 
door close__state11`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (11):

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

**ActionExchange — survived family-level state coverage (Devroey)** (182):

- `AEX__state4__press cabin roof__press&hold 
door open__state5`
- `AEX__state4__press cabin roof__press&hold 
door close__state5`
- `AEX__state4__press cabin roof__press door open__state5`
- `AEX__state4__press cabin roof__press hall down__state5`
- `AEX__state4__press cabin roof__press cabin lobby__state5`
- `AEX__state4__press cabin roof__release door open__state5`
- `AEX__state4__press cabin roof__press door close__state5`
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
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press&hold 
door open__state8`
- `AEX__state5__press door open__press&hold 
door close__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__release door open__state8`
- `AEX__state5__press door open__press door close__state8`
- `AEX__state5__press door open__release door close__state8`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press&hold 
door open__state5`
- `AEX__state2__press cabin lobby__press&hold 
door close__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__release door open__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__release door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press&hold 
door open__state7`
- `AEX__state8__press door close__press&hold 
door close__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__release door open__state7`
- `AEX__state8__press door close__release door close__state7`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press&hold 
door open__state5`
- `AEX__state3__press cabin lobby__press&hold 
door close__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__release door open__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
- `AEX__state3__press cabin lobby__release door close__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press alarm
button__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
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

**ActionExchange — survived product state coverage** (143):

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

### Product 23

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 23 | 8/23 = 34.8% | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% |
| ActionExchange | 253 | 88/253 = 34.8% | 99/253 = 39.1% | 253/253 = 100.0% | 253/253 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (15):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (14):

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

**ActionExchange — survived family-level state coverage (Devroey)** (165):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press door close__state9`
- `AEX__state5__press intercom__tap mobile
key__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
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
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
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

**ActionExchange — survived product state coverage** (154):

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

### Product 24

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 22 | 8/22 = 36.4% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 80/220 = 36.4% | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (14):

- `TM__state4__press cabin roof__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (13):

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

**ActionExchange — survived family-level state coverage (Devroey)** (140):

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
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__tap mobile
key__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press alarm
button__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state5__press door open__press door close__state8`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
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
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__tap mobile
key__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__tap mobile
key__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press alarm
button__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__press hall up__state7`
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

**ActionExchange — survived product state coverage** (130):

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

### Product 25

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ManualDoorControl}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 22 | 8/22 = 36.4% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 80/220 = 36.4% | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (14):

- `TM__state4__press cabin roof__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (13):

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

**ActionExchange — survived family-level state coverage (Devroey)** (140):

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
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press alarm
button__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__read card__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state5__press door open__press door close__state8`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press alarm
button__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__read card__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__press hall up__state7`
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press door open__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press alarm
button__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press hall up__state6`
- `AEX__state3__read card__press door close__state6`
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

**ActionExchange — survived product state coverage** (130):

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

### Product 26

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom}

**Repaired FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 20 | 7/20 = 35.0% | 10/20 = 50.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 220 | 77/220 = 35.0% | 110/220 = 50.0% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (13):

- `TM__state9__press&hold 
door close__state11`
- `TM__state5__press intercom__state9`
- `TM__state10__release door open__state13`
- `TM__state4__press cabin roof__state5`
- `TM__state13__press&hold 
door open__state10`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state14__press&hold 
door close__state11`
- `TM__state9__press&hold 
door open__state10`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state5__press&hold 
door open__state10`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (10):

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

**ActionExchange — survived family-level state coverage (Devroey)** (143):

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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press&hold 
door open__state9`
- `AEX__state5__press intercom__press&hold 
door close__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__release door open__state9`
- `AEX__state5__press intercom__release door close__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
- `AEX__state10__release door open__press cabin roof__state13`
- `AEX__state10__release door open__press&hold 
door open__state13`
- `AEX__state10__release door open__press&hold 
door close__state13`
- `AEX__state10__release door open__press hall down__state13`
- `AEX__state10__release door open__press cabin lobby__state13`
- `AEX__state10__release door open__release door close__state13`
- `AEX__state10__release door open__press hall 
RoofDown__state13`
- `AEX__state10__release door open__press cabin 
[1-N] floor__state13`
- `AEX__state10__release door open__press hall 
LobbyUp__state13`
- `AEX__state10__release door open__press intercom__state13`
- `AEX__state10__release door open__press hall up__state13`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press&hold 
door open__state5`
- `AEX__state3__press cabin lobby__press&hold 
door close__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__release door open__state5`
- `AEX__state3__press cabin lobby__release door close__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
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
- `AEX__state9__press&hold 
door open__press cabin roof__state10`
- `AEX__state9__press&hold 
door open__press&hold 
door close__state10`
- `AEX__state9__press&hold 
door open__press hall down__state10`
- `AEX__state9__press&hold 
door open__press cabin lobby__state10`
- `AEX__state9__press&hold 
door open__release door open__state10`
- `AEX__state9__press&hold 
door open__release door close__state10`
- `AEX__state9__press&hold 
door open__press hall 
RoofDown__state10`
- `AEX__state9__press&hold 
door open__press cabin 
[1-N] floor__state10`
- `AEX__state9__press&hold 
door open__press hall 
LobbyUp__state10`
- `AEX__state9__press&hold 
door open__press intercom__state10`
- `AEX__state9__press&hold 
door open__press hall up__state10`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press&hold 
door open__state5`
- `AEX__state2__press cabin lobby__press&hold 
door close__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__release door open__state5`
- `AEX__state2__press cabin lobby__release door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (110):

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

### Product 27

**Selected features:** selected = {Alarm, ControlButtons, MobileKey}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 17 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 18 | 8/18 = 44.4% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 64/144 = 44.4% | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (10):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (11):

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

**ActionExchange — survived family-level state coverage (Devroey)** (80):

- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__tap mobile
key__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (88):

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

### Product 28

**Selected features:** selected = {ControlButtons, Intercom, MobileKey}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 16 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 18 | 7/18 = 38.9% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 56/144 = 38.9% | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (11):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (11):

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

**ActionExchange — survived family-level state coverage (Devroey)** (88):

- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__tap mobile
key__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__tap mobile
key__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (88):

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

### Product 29

**Selected features:** selected = {ControlButtons, Intercom, PinPad}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 12 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 18 | 6/18 = 33.3% | 7/18 = 38.9% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 144 | 48/144 = 33.3% | 56/144 = 38.9% | 144/144 = 100.0% | 144/144 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (12):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state2__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (11):

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

**ActionExchange — survived family-level state coverage (Devroey)** (96):

- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__enter PIN__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__enter PIN__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__enter PIN__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press intercom__state6`
- `AEX__state2__enter PIN__press hall up__state6`
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

**ActionExchange — survived product state coverage** (88):

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

### Product 30

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, MobileKey}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 24 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 20 | 9/20 = 45.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 81/180 = 45.0% | 63/180 = 35.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (11):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state4__tap mobile
key__state6`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (13):

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

**ActionExchange — survived family-level state coverage (Devroey)** (99):

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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__tap mobile
key__state5`
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
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (117):

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

### Product 31

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 22 | 8/22 = 36.4% | 7/22 = 31.8% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 80/220 = 36.4% | 70/220 = 31.8% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (14):

- `TM__state12__press alarm
button__state9`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
executive floor__state12`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (15):

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

**ActionExchange — survived family-level state coverage (Devroey)** (140):

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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__read card__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
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
- `AEX__state6__press cabin 
executive floor__press cabin roof__state12`
- `AEX__state6__press cabin 
executive floor__press hall down__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
RoofDown__state12`
- `AEX__state6__press cabin 
executive floor__press alarm
button__state12`
- `AEX__state6__press cabin 
executive floor__press cabin 
[1-N] floor__state12`
- `AEX__state6__press cabin 
executive floor__read card__state12`
- `AEX__state6__press cabin 
executive floor__press cabin lobby__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
LobbyUp__state12`
- `AEX__state6__press cabin 
executive floor__press intercom__state12`
- `AEX__state6__press cabin 
executive floor__press hall up__state12`
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press cabin 
executive floor__state6`
- `AEX__state3__read card__press alarm
button__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press intercom__state6`
- `AEX__state3__read card__press hall up__state6`
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

**ActionExchange — survived product state coverage** (150):

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

### Product 32

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 26 | 7/26 = 26.9% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 77/286 = 26.9% | 99/286 = 34.6% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (19):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state12__press door close__state7`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
executive floor__state12`
- `TM__state8__press door close__state7`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (17):

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

**ActionExchange — survived family-level state coverage (Devroey)** (209):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press door close__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__read card__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state12__press door close__press cabin roof__state7`
- `AEX__state12__press door close__press door open__state7`
- `AEX__state12__press door close__press hall down__state7`
- `AEX__state12__press door close__press cabin 
executive floor__state7`
- `AEX__state12__press door close__press cabin lobby__state7`
- `AEX__state12__press door close__press hall 
RoofDown__state7`
- `AEX__state12__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state12__press door close__read card__state7`
- `AEX__state12__press door close__press hall 
LobbyUp__state7`
- `AEX__state12__press door close__press intercom__state7`
- `AEX__state12__press door close__press hall up__state7`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
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
- `AEX__state6__press cabin 
executive floor__press cabin roof__state12`
- `AEX__state6__press cabin 
executive floor__press door open__state12`
- `AEX__state6__press cabin 
executive floor__press hall down__state12`
- `AEX__state6__press cabin 
executive floor__press cabin lobby__state12`
- `AEX__state6__press cabin 
executive floor__press door close__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
RoofDown__state12`
- `AEX__state6__press cabin 
executive floor__press cabin 
[1-N] floor__state12`
- `AEX__state6__press cabin 
executive floor__read card__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
LobbyUp__state12`
- `AEX__state6__press cabin 
executive floor__press intercom__state12`
- `AEX__state6__press cabin 
executive floor__press hall up__state12`
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
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press door open__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press cabin 
executive floor__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press door close__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press intercom__state6`
- `AEX__state3__read card__press hall up__state6`
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
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin 
executive floor__state8`
- `AEX__state7__press door open__press cabin lobby__state8`
- `AEX__state7__press door open__press door close__state8`
- `AEX__state7__press door open__press hall 
RoofDown__state8`
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

**ActionExchange — survived product state coverage** (187):

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

### Product 33

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom, ManualDoorControl}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 23 | 8/23 = 34.8% | 9/23 = 39.1% | 23/23 = 100.0% | 23/23 = 100.0% |
| ActionExchange | 253 | 88/253 = 34.8% | 99/253 = 39.1% | 253/253 = 100.0% | 253/253 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (15):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state8__press door close__state7`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (14):

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

**ActionExchange — survived family-level state coverage (Devroey)** (165):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press door close__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__read card__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
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
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press door open__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press door close__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press alarm
button__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press intercom__state6`
- `AEX__state3__read card__press hall up__state6`
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

**ActionExchange — survived product state coverage** (154):

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

### Product 34

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, ManualDoorControl}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 26 | 8/26 = 30.8% | 9/26 = 34.6% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 88/286 = 30.8% | 99/286 = 34.6% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (18):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state12__press door close__state7`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
executive floor__state12`
- `TM__state8__press door close__state7`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (17):

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

**ActionExchange — survived family-level state coverage (Devroey)** (198):

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
- `AEX__state12__press door close__press cabin roof__state7`
- `AEX__state12__press door close__press door open__state7`
- `AEX__state12__press door close__press hall down__state7`
- `AEX__state12__press door close__press cabin 
executive floor__state7`
- `AEX__state12__press door close__press cabin lobby__state7`
- `AEX__state12__press door close__press hall 
RoofDown__state7`
- `AEX__state12__press door close__press alarm
button__state7`
- `AEX__state12__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state12__press door close__read card__state7`
- `AEX__state12__press door close__press hall 
LobbyUp__state7`
- `AEX__state12__press door close__press hall up__state7`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press door open__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__press door close__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`
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
- `AEX__state6__press cabin 
executive floor__press cabin roof__state12`
- `AEX__state6__press cabin 
executive floor__press door open__state12`
- `AEX__state6__press cabin 
executive floor__press hall down__state12`
- `AEX__state6__press cabin 
executive floor__press cabin lobby__state12`
- `AEX__state6__press cabin 
executive floor__press door close__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
RoofDown__state12`
- `AEX__state6__press cabin 
executive floor__press alarm
button__state12`
- `AEX__state6__press cabin 
executive floor__press cabin 
[1-N] floor__state12`
- `AEX__state6__press cabin 
executive floor__read card__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
LobbyUp__state12`
- `AEX__state6__press cabin 
executive floor__press hall up__state12`
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
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press door open__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press cabin 
executive floor__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press door close__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press alarm
button__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press hall up__state6`
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
- `AEX__state7__press door open__press cabin roof__state8`
- `AEX__state7__press door open__press hall down__state8`
- `AEX__state7__press door open__press cabin 
executive floor__state8`
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
- `AEX__state7__press door open__press hall up__state8`
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

**ActionExchange — survived product state coverage** (187):

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

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 20 | 7/20 = 35.0% | 7/20 = 35.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 180 | 63/180 = 35.0% | 63/180 = 35.0% | 180/180 = 100.0% | 180/180 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (13):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin 
executive floor__state12`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (13):

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

**ActionExchange — survived family-level state coverage (Devroey)** (117):

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
- `AEX__state6__press cabin 
executive floor__press cabin roof__state12`
- `AEX__state6__press cabin 
executive floor__press hall down__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
RoofDown__state12`
- `AEX__state6__press cabin 
executive floor__press cabin 
[1-N] floor__state12`
- `AEX__state6__press cabin 
executive floor__read card__state12`
- `AEX__state6__press cabin 
executive floor__press cabin lobby__state12`
- `AEX__state6__press cabin 
executive floor__press hall 
LobbyUp__state12`
- `AEX__state6__press cabin 
executive floor__press intercom__state12`
- `AEX__state6__press cabin 
executive floor__press hall up__state12`
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__read card__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press cabin 
executive floor__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press intercom__state6`
- `AEX__state3__read card__press hall up__state6`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (117):

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

### Product 36

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 19 | 8/19 = 42.1% | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% |
| ActionExchange | 171 | 72/171 = 42.1% | 63/171 = 36.8% | 171/171 = 100.0% | 171/171 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (11):

- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state3__read card__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state2__read card__state6`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (12):

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

**ActionExchange — survived family-level state coverage (Devroey)** (99):

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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__read card__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state3__read card__press cabin roof__state6`
- `AEX__state3__read card__press hall down__state6`
- `AEX__state3__read card__press hall 
RoofDown__state6`
- `AEX__state3__read card__press alarm
button__state6`
- `AEX__state3__read card__press cabin 
[1-N] floor__state6`
- `AEX__state3__read card__press cabin lobby__state6`
- `AEX__state3__read card__press hall 
LobbyUp__state6`
- `AEX__state3__read card__press intercom__state6`
- `AEX__state3__read card__press hall up__state6`
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
- `AEX__state2__press cabin lobby__press cabin roof__state5`
- `AEX__state2__press cabin lobby__press hall down__state5`
- `AEX__state2__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state2__press cabin lobby__press alarm
button__state5`
- `AEX__state2__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state2__press cabin lobby__read card__state5`
- `AEX__state2__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state2__press cabin lobby__press intercom__state5`
- `AEX__state2__press cabin lobby__press hall up__state5`

**ActionExchange — survived product state coverage** (108):

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

### Product 37

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 26 | 8/26 = 30.8% | 11/26 = 42.3% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 88/286 = 30.8% | 121/286 = 42.3% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (18):

- `TM__state12__press alarm
button__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state12__press door close__state7`
- `TM__state2__enter PIN__state6`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (15):

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

**ActionExchange — survived family-level state coverage (Devroey)** (198):

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
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press door open__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press cabin 
executive floor__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press door close__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press alarm
button__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press hall up__state6`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin 
executive floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__enter PIN__state8`
- `AEX__state5__press door open__press door close__state8`
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
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__enter PIN__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin 
executive floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__enter PIN__state7`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__enter PIN__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
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

**ActionExchange — survived product state coverage** (165):

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

### Product 38

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 13 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 22 | 7/22 = 31.8% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 70/220 = 31.8% | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (15):

- `TM__state4__press cabin roof__state5`
- `TM__state2__enter PIN__state6`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (13):

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

**ActionExchange — survived family-level state coverage (Devroey)** (150):

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
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press door open__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press alarm
button__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press hall up__state6`
- `AEX__state2__enter PIN__press door close__state6`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press alarm
button__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__enter PIN__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state5__press door open__press door close__state8`
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
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__enter PIN__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press alarm
button__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__enter PIN__state7`
- `AEX__state8__press door close__press hall up__state7`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
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
- `AEX__state3__press cabin lobby__enter PIN__state5`
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

**ActionExchange — survived product state coverage** (130):

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

### Product 39

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 12 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 22 | 6/22 = 27.3% | 9/22 = 40.9% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 60/220 = 27.3% | 90/220 = 40.9% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (16):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__enter PIN__state6`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (13):

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

**ActionExchange — survived family-level state coverage (Devroey)** (160):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__enter PIN__state9`
- `AEX__state5__press intercom__press hall up__state9`
- `AEX__state5__press intercom__press door close__state9`
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
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press door open__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press intercom__state6`
- `AEX__state2__enter PIN__press hall up__state6`
- `AEX__state2__enter PIN__press door close__state6`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press hall 
RoofDown__state8`
- `AEX__state5__press door open__press cabin 
[1-N] floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__press hall 
LobbyUp__state8`
- `AEX__state5__press door open__press intercom__state8`
- `AEX__state5__press door open__enter PIN__state8`
- `AEX__state5__press door open__press hall up__state8`
- `AEX__state5__press door open__press door close__state8`
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
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__enter PIN__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press hall 
RoofDown__state7`
- `AEX__state8__press door close__press cabin 
[1-N] floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__press hall 
LobbyUp__state7`
- `AEX__state8__press door close__press intercom__state7`
- `AEX__state8__press door close__enter PIN__state7`
- `AEX__state8__press door close__press hall up__state7`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state3__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state3__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state3__press cabin lobby__press intercom__state5`
- `AEX__state3__press cabin lobby__enter PIN__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
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

**ActionExchange — survived product state coverage** (130):

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

### Product 40

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 14 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 26 | 7/26 = 26.9% | 11/26 = 42.3% | 26/26 = 100.0% | 26/26 = 100.0% |
| ActionExchange | 286 | 77/286 = 26.9% | 121/286 = 42.3% | 286/286 = 100.0% | 286/286 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (19):

- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state12__press door close__state7`
- `TM__state2__enter PIN__state6`
- `TM__state5__press door open__state8`
- `TM__state5__press door close__state7`
- `TM__state2__press cabin lobby__state5`
- `TM__state12__press door open__state8`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state8__press door close__state7`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state7__press door open__state8`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (15):

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

**ActionExchange — survived family-level state coverage (Devroey)** (209):

- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press door open__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__enter PIN__state9`
- `AEX__state5__press intercom__press door close__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press door open__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press cabin 
executive floor__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press door close__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press intercom__state6`
- `AEX__state2__enter PIN__press hall up__state6`
- `AEX__state5__press door open__press cabin roof__state8`
- `AEX__state5__press door open__press hall down__state8`
- `AEX__state5__press door open__press cabin 
executive floor__state8`
- `AEX__state5__press door open__press cabin lobby__state8`
- `AEX__state5__press door open__enter PIN__state8`
- `AEX__state5__press door open__press door close__state8`
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
- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press door open__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state6__press cabin lobby__enter PIN__state5`
- `AEX__state6__press cabin lobby__press door close__state5`
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
- `AEX__state8__press door close__press cabin roof__state7`
- `AEX__state8__press door close__press door open__state7`
- `AEX__state8__press door close__press hall down__state7`
- `AEX__state8__press door close__press cabin 
executive floor__state7`
- `AEX__state8__press door close__press cabin lobby__state7`
- `AEX__state8__press door close__enter PIN__state7`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
- `AEX__state3__press cabin lobby__press door open__state5`
- `AEX__state3__press cabin lobby__press hall down__state5`
- `AEX__state3__press cabin lobby__press cabin 
executive floor__state5`
- `AEX__state3__press cabin lobby__enter PIN__state5`
- `AEX__state3__press cabin lobby__press door close__state5`
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

**ActionExchange — survived product state coverage** (165):

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

### Product 41

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 22 | 8/22 = 36.4% | 8/22 = 36.4% | 22/22 = 100.0% | 22/22 = 100.0% |
| ActionExchange | 220 | 80/220 = 36.4% | 80/220 = 36.4% | 220/220 = 100.0% | 220/220 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (14):

- `TM__state12__press alarm
button__state9`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__enter PIN__state6`
- `TM__state2__press cabin lobby__state5`
- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state12__press intercom__state9`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`

**TransitionMissing — survived product state coverage** (14):

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

**ActionExchange — survived family-level state coverage (Devroey)** (140):

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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press cabin 
executive floor__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__enter PIN__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press cabin 
executive floor__state6`
- `AEX__state2__enter PIN__press alarm
button__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press intercom__state6`
- `AEX__state2__enter PIN__press hall up__state6`
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
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__enter PIN__state5`
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
executive floor__state6`
- `AEX__state3__enter PIN__press alarm
button__state6`
- `AEX__state3__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state3__enter PIN__press cabin lobby__state6`
- `AEX__state3__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state3__enter PIN__press intercom__state6`
- `AEX__state3__enter PIN__press hall up__state6`
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

**ActionExchange — survived product state coverage** (140):

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

### Product 42

**Selected features:** selected = {Alarm, ControlButtons, Intercom, PinPad}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 9 test case(s) (of 9 family-level), 13 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 19 | 7/19 = 36.8% | 7/19 = 36.8% | 19/19 = 100.0% | 19/19 = 100.0% |
| ActionExchange | 171 | 63/171 = 36.8% | 63/171 = 36.8% | 171/171 = 100.0% | 171/171 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (12):

- `TM__state6__press cabin lobby__state5`
- `TM__state6__press cabin 
[1-N] floor__state5`
- `TM__state3__enter PIN__state6`
- `TM__state5__press intercom__state9`
- `TM__state4__press cabin roof__state5`
- `TM__state2__press cabin 
[1-N] floor__state5`
- `TM__state6__press cabin roof__state5`
- `TM__state3__press cabin lobby__state5`
- `TM__state2__enter PIN__state6`
- `TM__state3__press cabin roof__state5`
- `TM__state1__press hall down__state3`
- `TM__state2__press cabin lobby__state5`

**TransitionMissing — survived product state coverage** (12):

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

**ActionExchange — survived family-level state coverage (Devroey)** (108):

- `AEX__state6__press cabin lobby__press cabin roof__state5`
- `AEX__state6__press cabin lobby__press hall down__state5`
- `AEX__state6__press cabin lobby__press hall 
RoofDown__state5`
- `AEX__state6__press cabin lobby__press alarm
button__state5`
- `AEX__state6__press cabin lobby__press cabin 
[1-N] floor__state5`
- `AEX__state6__press cabin lobby__press hall 
LobbyUp__state5`
- `AEX__state6__press cabin lobby__press intercom__state5`
- `AEX__state6__press cabin lobby__enter PIN__state5`
- `AEX__state6__press cabin lobby__press hall up__state5`
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
- `AEX__state5__press intercom__press cabin roof__state9`
- `AEX__state5__press intercom__press hall down__state9`
- `AEX__state5__press intercom__press hall 
RoofDown__state9`
- `AEX__state5__press intercom__press alarm
button__state9`
- `AEX__state5__press intercom__press cabin 
[1-N] floor__state9`
- `AEX__state5__press intercom__press cabin lobby__state9`
- `AEX__state5__press intercom__press hall 
LobbyUp__state9`
- `AEX__state5__press intercom__enter PIN__state9`
- `AEX__state5__press intercom__press hall up__state9`
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
- `AEX__state3__press cabin lobby__press cabin roof__state5`
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
- `AEX__state3__press cabin lobby__enter PIN__state5`
- `AEX__state3__press cabin lobby__press hall up__state5`
- `AEX__state2__enter PIN__press cabin roof__state6`
- `AEX__state2__enter PIN__press hall down__state6`
- `AEX__state2__enter PIN__press hall 
RoofDown__state6`
- `AEX__state2__enter PIN__press alarm
button__state6`
- `AEX__state2__enter PIN__press cabin 
[1-N] floor__state6`
- `AEX__state2__enter PIN__press cabin lobby__state6`
- `AEX__state2__enter PIN__press hall 
LobbyUp__state6`
- `AEX__state2__enter PIN__press intercom__state6`
- `AEX__state2__enter PIN__press hall up__state6`
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

**ActionExchange — survived product state coverage** (108):

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
---

## Elevator summary (aggregate over 42 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 9 test case(s) generated once for the SPL, projected per-product via fexpr-filtering before kill-checking.

| Operator | Mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 926 | 340/926 = 36.7% | 366/926 = 39.5% | 926/926 = 100.0% | 926/926 = 100.0% |
| ActionExchange | 9586 | 3502/9586 = 36.5% | 3824/9586 = 39.9% | 9586/9586 = 100.0% | 9586/9586 = 100.0% |

Total products: 42.
