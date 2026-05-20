# Per-Product All-Transitions Coverage — Elevator

## How the all-transitions test case is built (M4 pipeline)

Given an SPL-level FTS plus one product configuration, the generator runs five steps. All five live in the `vibes-testgeneration` module; the orchestrator is [`TransitionCoverageGenerator.generate(...)`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/TransitionCoverageGenerator.java).

**Step 1 — Project onto the product.** [`FExpressionPreservingProjection.project(fts, config)`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/product/FExpressionPreservingProjection.java) keeps every transition `t` whose feature expression evaluates true under the product (`fts.getFExpression(t).assign(config).applySimplification().isTrue()`); the original (un-assigned) `FExpression` is preserved on the kept transition for traceability. A forward BFS from the initial state then drops states unreachable from it. Output: a product-level `FeaturedTransitionSystem`.

**Step 2 — Repair strong connectivity.** [`InitialSccFilter.keepInitialScc(projected)`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/InitialSccFilter.java) runs [Tarjan's SCC](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/StronglyConnectedComponents.java), keeps the SCC containing the initial state, and drops every other state (and its transitions). The result is strongly connected by construction — the precondition for any Eulerian-cycle algorithm. In the three MVP SPLs this step is currently a no-op (the projection already produced a single SCC reachable from initial); we still run it as an invariant check and to keep the pipeline robust for larger SPLs.

**Step 3 — Balance for an Euler cycle.** [`EulerianBalancer.balance(repaired)`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/EulerianBalancer.java) makes the graph Eulerian by enforcing `in-degree == out-degree` at every state. The approach is a directed **Chinese Postman**: for each pair of imbalanced states `(u, v)` (`u` has excess outgoing, `v` has excess incoming) it finds a shortest path of real transitions from `v` to `u` via BFS, then **doubles** every transition along that path. Doubled transitions get a unique action name `<original>__dup__N` so they survive VIBeS' dedup but their semantic action is the original — coverage measurement strips the suffix. Where no real path exists (e.g. the pair-graph from M6) the balancer falls back to a direct synthetic `__balance__N` edge.

**Step 4 — Trace the Euler cycle.** [`HierholzerEulerCycle.compute(balanced)`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/HierholzerEulerCycle.java) walks the balanced graph using Hierholzer's algorithm: DFS until a sub-cycle closes, splice in additional sub-cycles from unvisited transitions, repeat. The output is one contiguous sequence of transitions that visits every edge of the balanced graph exactly once and returns to the initial state.

**Step 5 — Wrap into a TestCase.** The cycle is enqueued into `be.vibes.ts.TestCase`. Synthetic actions (`__end__`, `__balance__N`, `<action>__dup__N`) remain in the test case so the executor can use them as test-case boundary markers (everything between two synthetics is one real-SUT sub-walk); they are filtered before coverage measurement via `EulerianBalancer.isSyntheticAction(...)`.

**Coverage claim (by construction).** Every real transition in the projected FTS appears in the balanced FTS (balancing only adds, never removes). The Hierholzer cycle visits every transition of the balanced graph exactly once. Therefore the cycle's real (non-synthetic) transitions cover **100% of the projected FTS' real transitions**. This is a structural invariant, not an empirical observation.

---

## Products

Each product below shows the projected FTS (left, synthetic `__end__` transitions dashed-red) and the balanced FTS (right, Chinese-Postman doubled transitions `<action>__dup__N` dashed-red). The generated all-transitions test case is listed underneath, segmented at every synthetic action (the action sequence between two synthetic boundaries is one self-contained sub-walk on real SUT events).


### Product 1

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Projected FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 8 states, 23 transitions (20 real / 3 `__end__`).

**Balanced FTS:** 8 states, 43 transitions (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`).

![Projected FTS — product 1](Elevator-product1-projected.png)

![Balanced FTS — product 1](Elevator-product1-balanced.png)

**All-transitions test case (`Elevator_p1_trans`)** — 43 step(s) total (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **20/20 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> press cabin [1-N] floor -> press intercom`
- **sub-walk 2**: `press cabin lobby`
- **sub-walk 3**: `enter PIN -> press cabin lobby`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press hall up -> enter PIN -> press cabin executive floor -> press intercom`
- **sub-walk 7**: `press hall down -> press cabin lobby`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press cabin [1-N] floor`
- **sub-walk 10**: `press cabin roof`
- **sub-walk 11**: `press cabin [1-N] floor`
- **sub-walk 12**: `press hall LobbyUp -> enter PIN`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> press cabin [1-N] floor -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin roof -> __end__(dup) -> press hall up -> enter PIN -> press cabin executive floor -> press intercom -> __end__(dup) -> press hall down -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> enter PIN -> press cabin executive floor(dup) -> __end__
```


### Product 2

**Selected features:** selected = {Alarm, ControlButtons, MobileKey}

**Projected FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 7 states, 20 transitions (18 real / 2 `__end__`).

**Balanced FTS:** 7 states, 34 transitions (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`).

![Projected FTS — product 2](Elevator-product2-projected.png)

![Balanced FTS — product 2](Elevator-product2-balanced.png)

**All-transitions test case (`Elevator_p2_trans`)** — 34 step(s) total (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **18/18 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `tap mobile key -> press cabin lobby -> press alarm button`
- **sub-walk 2**: `press cabin [1-N] floor`
- **sub-walk 3**: `press hall RoofDown -> press cabin lobby`
- **sub-walk 4**: `press cabin lobby`
- **sub-walk 5**: `press hall up -> press cabin roof`
- **sub-walk 6**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 7**: `tap mobile key -> press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `tap mobile key -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> tap mobile key -> press cabin lobby -> press alarm button -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> tap mobile key -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> tap mobile key -> press cabin roof -> __end__(dup)
```


### Product 3

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor}

**Projected FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 8 states, 23 transitions (20 real / 3 `__end__`).

**Balanced FTS:** 8 states, 43 transitions (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`).

![Projected FTS — product 3](Elevator-product3-projected.png)

![Balanced FTS — product 3](Elevator-product3-balanced.png)

**All-transitions test case (`Elevator_p3_trans`)** — 43 step(s) total (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **20/20 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **sub-walk 2**: `press cabin lobby`
- **sub-walk 3**: `press cabin lobby`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `read card -> press cabin roof`
- **sub-walk 6**: `press hall up -> read card -> press cabin executive floor -> press alarm button`
- **sub-walk 7**: `press hall down -> press cabin lobby`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press cabin [1-N] floor`
- **sub-walk 10**: `press cabin roof`
- **sub-walk 11**: `press cabin [1-N] floor`
- **sub-walk 12**: `press hall LobbyUp -> read card`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> press cabin [1-N] floor -> press alarm button -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> read card -> press cabin roof -> __end__(dup) -> press hall up -> read card -> press cabin executive floor -> press alarm button -> __end__(dup) -> press hall down -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> read card -> press cabin executive floor(dup) -> __end__
```


### Product 4

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Projected FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 9 states, 26 transitions (22 real / 4 `__end__`).

**Balanced FTS:** 9 states, 38 transitions (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`).

![Projected FTS — product 4](Elevator-product4-projected.png)

![Balanced FTS — product 4](Elevator-product4-balanced.png)

**All-transitions test case (`Elevator_p4_trans`)** — 38 step(s) total (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **22/22 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin lobby -> press intercom`
- **sub-walk 2**: `press hall RoofDown -> press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 3**: `tap mobile key -> press cabin lobby -> press door close -> press door open`
- **sub-walk 4**: `press hall up -> tap mobile key -> press cabin [1-N] floor`
- **sub-walk 5**: `press cabin lobby`
- **sub-walk 6**: `press hall down -> press cabin roof`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `tap mobile key -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown -> press cabin [1-N] floor -> press door open -> press door close -> __end__ -> press hall RoofDown(dup) -> tap mobile key -> press cabin lobby -> press door close -> press door open -> __end__ -> press hall up -> tap mobile key -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> tap mobile key -> press cabin roof -> __end__(dup)
```


### Product 5

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService}

**Projected FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 24 transitions (20 real / 4 `__end__`).

**Balanced FTS:** 10 states, 49 transitions (20 real / 10 `__end__` / 25 `__dup__` / 0 `__balance__`).

![Projected FTS — product 5](Elevator-product5-projected.png)

![Balanced FTS — product 5](Elevator-product5-balanced.png)

**All-transitions test case (`Elevator_p5_trans`)** — 49 step(s) total (20 real / 10 `__end__` / 25 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **20/20 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press&hold door close -> release door close`
- **sub-walk 2**: `press hall RoofDown`
- **sub-walk 3**: `press alarm button`
- **sub-walk 4**: `press&hold door open`
- **sub-walk 5**: `press cabin [1-N] floor`
- **sub-walk 6**: `press cabin lobby -> press&hold door open -> release door open -> press&hold door open`
- **sub-walk 7**: `press hall up -> press cabin lobby`
- **sub-walk 8**: `press hall down -> press cabin roof -> press&hold door close`
- **sub-walk 9**: `press&hold door close`
- **sub-walk 10**: `press cabin [1-N] floor`
- **sub-walk 11**: `press cabin roof`
- **sub-walk 12**: `press hall LobbyUp -> press cabin [1-N] floor`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby(dup) -> press alarm button(dup) -> press&hold door close -> release door close -> __end__ -> press hall RoofDown -> press cabin lobby(dup) -> press alarm button -> __end__ -> press hall RoofDown(dup) -> press cabin lobby(dup) -> press alarm button(dup) -> press&hold door open -> release door open(dup) -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press&hold door open(dup) -> release door open(dup) -> __end__(dup) -> press hall RoofDown(dup) -> press cabin lobby -> press&hold door open -> release door open -> press&hold door open -> release door open(dup) -> __end__(dup) -> press hall up -> press cabin lobby -> press&hold door close(dup) -> release door close(dup) -> __end__(dup) -> press hall down -> press cabin roof -> press&hold door close -> release door close(dup) -> press&hold door close -> release door close(dup) -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__
```


### Product 6

**Selected features:** selected = {CardReader, ControlButtons, Intercom, ManualDoorControl}

**Projected FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 9 states, 26 transitions (22 real / 4 `__end__`).

**Balanced FTS:** 9 states, 38 transitions (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`).

![Projected FTS — product 6](Elevator-product6-projected.png)

![Balanced FTS — product 6](Elevator-product6-balanced.png)

**All-transitions test case (`Elevator_p6_trans`)** — 38 step(s) total (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **22/22 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin lobby -> press intercom`
- **sub-walk 2**: `press hall RoofDown -> press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 3**: `read card -> press cabin lobby -> press door close -> press door open`
- **sub-walk 4**: `press hall up -> press cabin lobby`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 7**: `read card -> press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `read card -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown -> press cabin [1-N] floor -> press door open -> press door close -> __end__ -> press hall RoofDown(dup) -> read card -> press cabin lobby -> press door close -> press door open -> __end__ -> press hall up -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> read card -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> read card -> press cabin roof -> __end__(dup)
```


### Product 7

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Projected FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 8 states, 25 transitions (22 real / 3 `__end__`).

**Balanced FTS:** 8 states, 49 transitions (22 real / 13 `__end__` / 24 `__dup__` / 0 `__balance__`).

![Projected FTS — product 7](Elevator-product7-projected.png)

![Balanced FTS — product 7](Elevator-product7-balanced.png)

**All-transitions test case (`Elevator_p7_trans`)** — 49 step(s) total (22 real / 13 `__end__` / 24 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **22/22 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin lobby -> press intercom`
- **sub-walk 2**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **sub-walk 3**: `press cabin lobby`
- **sub-walk 4**: `tap mobile key -> press cabin [1-N] floor`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press cabin executive floor -> press intercom`
- **sub-walk 7**: `tap mobile key`
- **sub-walk 8**: `press alarm button`
- **sub-walk 9**: `press cabin lobby`
- **sub-walk 10**: `press hall up -> press cabin roof`
- **sub-walk 11**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 12**: `press cabin roof`
- **sub-walk 13**: `press cabin [1-N] floor`
- **sub-walk 14**: `press hall LobbyUp -> tap mobile key`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown -> press cabin [1-N] floor -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin roof -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin executive floor -> press intercom -> __end__(dup) -> press hall up(dup) -> tap mobile key -> press cabin executive floor(dup) -> press alarm button -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__ -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp -> tap mobile key -> press cabin executive floor(dup) -> __end__
```


### Product 8

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, PinPad}

**Projected FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 31 transitions (26 real / 5 `__end__`).

**Balanced FTS:** 10 states, 57 transitions (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`).

![Projected FTS — product 8](Elevator-product8-projected.png)

![Balanced FTS — product 8](Elevator-product8-balanced.png)

**All-transitions test case (`Elevator_p8_trans`)** — 57 step(s) total (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **26/26 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> enter PIN -> press cabin lobby -> press alarm button`
- **sub-walk 2**: `press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 3**: `press cabin roof -> press door close`
- **sub-walk 4**: `press cabin executive floor -> press alarm button`
- **sub-walk 5**: `press door close -> press door open`
- **sub-walk 6**: `press cabin lobby`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin lobby`
- **sub-walk 9**: `press hall up -> press cabin roof`
- **sub-walk 10**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 11**: `enter PIN`
- **sub-walk 12**: `press door open`
- **sub-walk 13**: `press cabin roof`
- **sub-walk 14**: `press cabin [1-N] floor`
- **sub-walk 15**: `press hall LobbyUp -> enter PIN`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> enter PIN -> press cabin lobby -> press alarm button -> __end__ -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin [1-N] floor -> press door open -> press door close -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin roof -> press door close -> __end__ -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin executive floor -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin executive floor(dup) -> press door close -> press door open -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> enter PIN -> press cabin executive floor(dup) -> press door open -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> enter PIN -> press cabin executive floor(dup) -> __end__
```


### Product 9

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Projected FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 31 transitions (26 real / 5 `__end__`).

**Balanced FTS:** 10 states, 57 transitions (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`).

![Projected FTS — product 9](Elevator-product9-projected.png)

![Balanced FTS — product 9](Elevator-product9-balanced.png)

**All-transitions test case (`Elevator_p9_trans`)** — 57 step(s) total (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **26/26 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> enter PIN -> press cabin lobby -> press intercom`
- **sub-walk 2**: `press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 3**: `press cabin roof -> press door close`
- **sub-walk 4**: `press cabin executive floor -> press door close -> press door open`
- **sub-walk 5**: `press door open`
- **sub-walk 6**: `press cabin lobby`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin lobby`
- **sub-walk 9**: `press hall up -> press cabin roof`
- **sub-walk 10**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 11**: `enter PIN`
- **sub-walk 12**: `press intercom`
- **sub-walk 13**: `press cabin roof`
- **sub-walk 14**: `press cabin [1-N] floor`
- **sub-walk 15**: `press hall LobbyUp -> enter PIN`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> enter PIN -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin [1-N] floor -> press door open -> press door close -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin roof -> press door close -> __end__ -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin executive floor -> press door close -> press door open -> __end__ -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin executive floor(dup) -> press door open -> __end__(dup) -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> enter PIN -> press cabin executive floor(dup) -> press intercom -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> enter PIN -> press cabin executive floor(dup) -> __end__
```


### Product 10

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom}

**Projected FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 24 transitions (20 real / 4 `__end__`).

**Balanced FTS:** 10 states, 49 transitions (20 real / 10 `__end__` / 25 `__dup__` / 0 `__balance__`).

![Projected FTS — product 10](Elevator-product10-projected.png)

![Balanced FTS — product 10](Elevator-product10-balanced.png)

**All-transitions test case (`Elevator_p10_trans`)** — 49 step(s) total (20 real / 10 `__end__` / 25 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **20/20 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press&hold door close -> release door close`
- **sub-walk 2**: `press hall RoofDown`
- **sub-walk 3**: `press intercom`
- **sub-walk 4**: `press&hold door open`
- **sub-walk 5**: `press cabin [1-N] floor`
- **sub-walk 6**: `press cabin lobby -> press&hold door open -> release door open -> press&hold door open`
- **sub-walk 7**: `press hall up -> press cabin lobby`
- **sub-walk 8**: `press hall down -> press cabin roof -> press&hold door close`
- **sub-walk 9**: `press&hold door close`
- **sub-walk 10**: `press cabin [1-N] floor`
- **sub-walk 11**: `press cabin roof`
- **sub-walk 12**: `press hall LobbyUp -> press cabin [1-N] floor`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby(dup) -> press intercom(dup) -> press&hold door close -> release door close -> __end__ -> press hall RoofDown -> press cabin lobby(dup) -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin lobby(dup) -> press intercom(dup) -> press&hold door open -> release door open(dup) -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press&hold door open(dup) -> release door open(dup) -> __end__(dup) -> press hall RoofDown(dup) -> press cabin lobby -> press&hold door open -> release door open -> press&hold door open -> release door open(dup) -> __end__(dup) -> press hall up -> press cabin lobby -> press&hold door close(dup) -> release door close(dup) -> __end__(dup) -> press hall down -> press cabin roof -> press&hold door close -> release door close(dup) -> press&hold door close -> release door close(dup) -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__
```


### Product 11

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Projected FTS:** 12 states, 31 transitions (25 real / 6 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 12 states, 31 transitions (25 real / 6 `__end__`).

**Balanced FTS:** 12 states, 48 transitions (25 real / 9 `__end__` / 17 `__dup__` / 0 `__balance__`).

![Projected FTS — product 11](Elevator-product11-projected.png)

![Balanced FTS — product 11](Elevator-product11-balanced.png)

**All-transitions test case (`Elevator_p11_trans`)** — 48 step(s) total (25 real / 9 `__end__` / 17 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **25/25 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press intercom -> press&hold door open -> release door open`
- **sub-walk 2**: `press hall RoofDown`
- **sub-walk 3**: `press alarm button -> press&hold door close -> release door close`
- **sub-walk 4**: `press cabin lobby`
- **sub-walk 5**: `press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 6**: `press hall up -> press cabin lobby -> press door close -> press door open`
- **sub-walk 7**: `press cabin roof -> press&hold door open`
- **sub-walk 8**: `press&hold door open`
- **sub-walk 9**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 10**: `press cabin roof -> press&hold door close`
- **sub-walk 11**: `press&hold door close`
- **sub-walk 12**: `press hall LobbyUp -> press cabin [1-N] floor`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby(dup) -> press intercom -> press&hold door open -> release door open -> __end__ -> press hall RoofDown -> press cabin lobby(dup) -> press alarm button -> press&hold door close -> release door close -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> press intercom(dup) -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press door open -> press door close -> __end__ -> press hall up -> press cabin lobby -> press door close -> press door open -> __end__ -> press hall up(dup) -> press cabin roof -> press&hold door open -> release door open(dup) -> press&hold door open -> release door open(dup) -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> press&hold door close(dup) -> release door close(dup) -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> press&hold door close -> release door close(dup) -> press&hold door close -> release door close(dup) -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__
```


### Product 12

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, PinPad}

**Projected FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 8 states, 23 transitions (20 real / 3 `__end__`).

**Balanced FTS:** 8 states, 43 transitions (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`).

![Projected FTS — product 12](Elevator-product12-projected.png)

![Balanced FTS — product 12](Elevator-product12-balanced.png)

**All-transitions test case (`Elevator_p12_trans`)** — 43 step(s) total (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **20/20 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **sub-walk 2**: `press cabin lobby`
- **sub-walk 3**: `enter PIN -> press cabin lobby`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press hall up -> enter PIN -> press cabin executive floor -> press alarm button`
- **sub-walk 7**: `press hall down -> press cabin lobby`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press cabin [1-N] floor`
- **sub-walk 10**: `press cabin roof`
- **sub-walk 11**: `press cabin [1-N] floor`
- **sub-walk 12**: `press hall LobbyUp -> enter PIN`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> press cabin [1-N] floor -> press alarm button -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin roof -> __end__(dup) -> press hall up -> enter PIN -> press cabin executive floor -> press alarm button -> __end__(dup) -> press hall down -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> enter PIN -> press cabin executive floor(dup) -> __end__
```


### Product 13

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, ManualDoorControl}

**Projected FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 12 states, 30 transitions (24 real / 6 `__end__`).

**Balanced FTS:** 12 states, 46 transitions (24 real / 9 `__end__` / 16 `__dup__` / 0 `__balance__`).

![Projected FTS — product 13](Elevator-product13-projected.png)

![Balanced FTS — product 13](Elevator-product13-balanced.png)

**All-transitions test case (`Elevator_p13_trans`)** — 46 step(s) total (24 real / 9 `__end__` / 16 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **24/24 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press&hold door open`
- **sub-walk 2**: `press hall RoofDown`
- **sub-walk 3**: `press alarm button -> press&hold door close -> release door close`
- **sub-walk 4**: `press cabin lobby`
- **sub-walk 5**: `press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 6**: `press cabin lobby -> press door close -> press door open`
- **sub-walk 7**: `press hall up -> press cabin roof -> press&hold door open -> release door open -> press&hold door open`
- **sub-walk 8**: `press hall down -> press cabin [1-N] floor -> press&hold door close`
- **sub-walk 9**: `press&hold door close`
- **sub-walk 10**: `press cabin roof`
- **sub-walk 11**: `press hall LobbyUp -> press cabin [1-N] floor`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby(dup) -> press alarm button(dup) -> press&hold door open -> release door open(dup) -> __end__ -> press hall RoofDown -> press cabin lobby(dup) -> press alarm button -> press&hold door close -> release door close -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> press alarm button(dup) -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press door open -> press door close -> __end__ -> press hall up(dup) -> press cabin lobby -> press door close -> press door open -> __end__ -> press hall up -> press cabin roof -> press&hold door open -> release door open -> press&hold door open -> release door open(dup) -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> press&hold door close -> release door close(dup) -> press&hold door close -> release door close(dup) -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__
```


### Product 14

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Projected FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 8 states, 25 transitions (22 real / 3 `__end__`).

**Balanced FTS:** 8 states, 49 transitions (22 real / 13 `__end__` / 24 `__dup__` / 0 `__balance__`).

![Projected FTS — product 14](Elevator-product14-projected.png)

![Balanced FTS — product 14](Elevator-product14-balanced.png)

**All-transitions test case (`Elevator_p14_trans`)** — 49 step(s) total (22 real / 13 `__end__` / 24 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **22/22 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin lobby -> press intercom`
- **sub-walk 2**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **sub-walk 3**: `press cabin lobby`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `read card -> press cabin executive floor -> press intercom`
- **sub-walk 7**: `press cabin lobby`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall up -> press cabin [1-N] floor`
- **sub-walk 10**: `press hall down -> read card`
- **sub-walk 11**: `press alarm button`
- **sub-walk 12**: `press cabin roof`
- **sub-walk 13**: `press cabin [1-N] floor`
- **sub-walk 14**: `press hall LobbyUp -> read card`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown -> press cabin [1-N] floor -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin roof -> __end__(dup) -> press hall RoofDown(dup) -> read card -> press cabin executive floor -> press intercom -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall up -> press cabin [1-N] floor -> __end__(dup) -> press hall down -> read card -> press cabin executive floor(dup) -> press alarm button -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__ -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp -> read card -> press cabin executive floor(dup) -> __end__
```


### Product 15

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Projected FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 12 states, 30 transitions (24 real / 6 `__end__`).

**Balanced FTS:** 12 states, 46 transitions (24 real / 9 `__end__` / 16 `__dup__` / 0 `__balance__`).

![Projected FTS — product 15](Elevator-product15-projected.png)

![Balanced FTS — product 15](Elevator-product15-balanced.png)

**All-transitions test case (`Elevator_p15_trans`)** — 46 step(s) total (24 real / 9 `__end__` / 16 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **24/24 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press&hold door open`
- **sub-walk 2**: `press hall RoofDown`
- **sub-walk 3**: `press&hold door close -> release door close`
- **sub-walk 4**: `press cabin lobby -> press intercom`
- **sub-walk 5**: `press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 6**: `press cabin lobby -> press door close -> press door open`
- **sub-walk 7**: `press hall up -> press cabin roof -> press&hold door open -> release door open -> press&hold door open`
- **sub-walk 8**: `press hall down -> press cabin [1-N] floor -> press&hold door close`
- **sub-walk 9**: `press&hold door close`
- **sub-walk 10**: `press cabin roof`
- **sub-walk 11**: `press hall LobbyUp -> press cabin [1-N] floor`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby(dup) -> press intercom(dup) -> press&hold door open -> release door open(dup) -> __end__ -> press hall RoofDown -> press cabin lobby(dup) -> press intercom(dup) -> press&hold door close -> release door close -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press door open -> press door close -> __end__ -> press hall up(dup) -> press cabin lobby -> press door close -> press door open -> __end__ -> press hall up -> press cabin roof -> press&hold door open -> release door open -> press&hold door open -> release door open(dup) -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> press&hold door close -> release door close(dup) -> press&hold door close -> release door close(dup) -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__
```


### Product 16

**Selected features:** selected = {Alarm, CardReader, ControlButtons}

**Projected FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 7 states, 20 transitions (18 real / 2 `__end__`).

**Balanced FTS:** 7 states, 34 transitions (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`).

![Projected FTS — product 16](Elevator-product16-projected.png)

![Balanced FTS — product 16](Elevator-product16-balanced.png)

**All-transitions test case (`Elevator_p16_trans`)** — 34 step(s) total (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **18/18 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin [1-N] floor -> press alarm button`
- **sub-walk 2**: `press cabin lobby`
- **sub-walk 3**: `press hall RoofDown -> read card -> press cabin lobby`
- **sub-walk 4**: `read card -> press cabin [1-N] floor`
- **sub-walk 5**: `press hall up -> press cabin lobby`
- **sub-walk 6**: `press hall down -> press cabin roof`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `read card -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin [1-N] floor -> press alarm button -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown -> read card -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> read card -> press cabin [1-N] floor -> __end__(dup) -> press hall up -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> read card -> press cabin roof -> __end__(dup)
```


### Product 17

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom}

**Projected FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 7 states, 21 transitions (19 real / 2 `__end__`).

**Balanced FTS:** 7 states, 35 transitions (19 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`).

![Projected FTS — product 17](Elevator-product17-projected.png)

![Balanced FTS — product 17](Elevator-product17-balanced.png)

**All-transitions test case (`Elevator_p17_trans`)** — 35 step(s) total (19 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **19/19 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin [1-N] floor -> press intercom`
- **sub-walk 2**: `press cabin lobby -> press alarm button`
- **sub-walk 3**: `press hall RoofDown -> read card -> press cabin lobby`
- **sub-walk 4**: `read card -> press cabin [1-N] floor`
- **sub-walk 5**: `press hall up -> press cabin lobby`
- **sub-walk 6**: `press hall down -> press cabin roof`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `read card -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin [1-N] floor -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> press alarm button -> __end__(dup) -> press hall RoofDown -> read card -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> read card -> press cabin [1-N] floor -> __end__(dup) -> press hall up -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> read card -> press cabin roof -> __end__(dup)
```


### Product 18

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, MobileKey}

**Projected FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 8 states, 23 transitions (20 real / 3 `__end__`).

**Balanced FTS:** 8 states, 43 transitions (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`).

![Projected FTS — product 18](Elevator-product18-projected.png)

![Balanced FTS — product 18](Elevator-product18-balanced.png)

**All-transitions test case (`Elevator_p18_trans`)** — 43 step(s) total (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **20/20 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> tap mobile key -> press cabin lobby -> press alarm button`
- **sub-walk 2**: `press cabin [1-N] floor`
- **sub-walk 3**: `press cabin roof`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `press cabin lobby`
- **sub-walk 6**: `press hall up -> press cabin lobby`
- **sub-walk 7**: `press hall down -> press cabin roof`
- **sub-walk 8**: `press cabin [1-N] floor`
- **sub-walk 9**: `tap mobile key -> press cabin executive floor -> press alarm button`
- **sub-walk 10**: `press cabin roof`
- **sub-walk 11**: `press cabin [1-N] floor`
- **sub-walk 12**: `press hall LobbyUp -> tap mobile key`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> tap mobile key -> press cabin lobby -> press alarm button -> __end__ -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin roof -> __end__(dup) -> press hall RoofDown(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall up -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> tap mobile key -> press cabin executive floor -> press alarm button -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> tap mobile key -> press cabin executive floor(dup) -> __end__
```


### Product 19

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, MobileKey}

**Projected FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 9 states, 26 transitions (22 real / 4 `__end__`).

**Balanced FTS:** 9 states, 38 transitions (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`).

![Projected FTS — product 19](Elevator-product19-projected.png)

![Balanced FTS — product 19](Elevator-product19-balanced.png)

**All-transitions test case (`Elevator_p19_trans`)** — 38 step(s) total (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **22/22 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin lobby -> press alarm button`
- **sub-walk 2**: `press hall RoofDown -> press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 3**: `tap mobile key -> press cabin lobby -> press door close -> press door open`
- **sub-walk 4**: `press hall up -> tap mobile key -> press cabin [1-N] floor`
- **sub-walk 5**: `press cabin lobby`
- **sub-walk 6**: `press hall down -> press cabin roof`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `tap mobile key -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby -> press alarm button -> __end__ -> press hall RoofDown -> press cabin [1-N] floor -> press door open -> press door close -> __end__ -> press hall RoofDown(dup) -> tap mobile key -> press cabin lobby -> press door close -> press door open -> __end__ -> press hall up -> tap mobile key -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> tap mobile key -> press cabin roof -> __end__(dup)
```


### Product 20

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, MobileKey}

**Projected FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 31 transitions (26 real / 5 `__end__`).

**Balanced FTS:** 10 states, 57 transitions (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`).

![Projected FTS — product 20](Elevator-product20-projected.png)

![Balanced FTS — product 20](Elevator-product20-balanced.png)

**All-transitions test case (`Elevator_p20_trans`)** — 57 step(s) total (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **26/26 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> press cabin lobby -> press alarm button`
- **sub-walk 2**: `press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 3**: `tap mobile key -> press cabin lobby -> press door close`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press cabin executive floor -> press alarm button`
- **sub-walk 7**: `press door close -> press door open`
- **sub-walk 8**: `tap mobile key`
- **sub-walk 9**: `press door open`
- **sub-walk 10**: `press hall up -> press cabin lobby`
- **sub-walk 11**: `press hall down -> press cabin roof`
- **sub-walk 12**: `press cabin [1-N] floor`
- **sub-walk 13**: `press cabin roof`
- **sub-walk 14**: `press cabin [1-N] floor`
- **sub-walk 15**: `press hall LobbyUp -> tap mobile key`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> press cabin lobby -> press alarm button -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press door open -> press door close -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key -> press cabin lobby -> press door close -> __end__ -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin roof -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin executive floor -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin executive floor(dup) -> press door close -> press door open -> __end__ -> press hall up(dup) -> tap mobile key -> press cabin executive floor(dup) -> press door open -> __end__(dup) -> press hall up -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> tap mobile key -> press cabin executive floor(dup) -> __end__
```


### Product 21

**Selected features:** selected = {CardReader, ControlButtons, Intercom}

**Projected FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 7 states, 20 transitions (18 real / 2 `__end__`).

**Balanced FTS:** 7 states, 34 transitions (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`).

![Projected FTS — product 21](Elevator-product21-projected.png)

![Balanced FTS — product 21](Elevator-product21-balanced.png)

**All-transitions test case (`Elevator_p21_trans`)** — 34 step(s) total (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **18/18 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin [1-N] floor -> press intercom`
- **sub-walk 2**: `press cabin lobby`
- **sub-walk 3**: `press hall RoofDown -> read card -> press cabin lobby`
- **sub-walk 4**: `read card -> press cabin [1-N] floor`
- **sub-walk 5**: `press hall up -> press cabin lobby`
- **sub-walk 6**: `press hall down -> press cabin roof`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `read card -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin [1-N] floor -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown -> read card -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> read card -> press cabin [1-N] floor -> __end__(dup) -> press hall up -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> read card -> press cabin roof -> __end__(dup)
```


### Product 22

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Projected FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 8 states, 25 transitions (22 real / 3 `__end__`).

**Balanced FTS:** 8 states, 49 transitions (22 real / 13 `__end__` / 24 `__dup__` / 0 `__balance__`).

![Projected FTS — product 22](Elevator-product22-projected.png)

![Balanced FTS — product 22](Elevator-product22-balanced.png)

**All-transitions test case (`Elevator_p22_trans`)** — 49 step(s) total (22 real / 13 `__end__` / 24 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **22/22 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `enter PIN -> press cabin lobby -> press intercom`
- **sub-walk 2**: `press hall RoofDown`
- **sub-walk 3**: `press cabin [1-N] floor -> press alarm button`
- **sub-walk 4**: `press cabin roof`
- **sub-walk 5**: `press cabin executive floor -> press intercom`
- **sub-walk 6**: `press cabin lobby`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin lobby`
- **sub-walk 9**: `press cabin roof`
- **sub-walk 10**: `press hall up -> press cabin [1-N] floor`
- **sub-walk 11**: `press hall down -> enter PIN`
- **sub-walk 12**: `press alarm button`
- **sub-walk 13**: `press cabin roof`
- **sub-walk 14**: `press cabin [1-N] floor`
- **sub-walk 15**: `press hall LobbyUp -> enter PIN`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> enter PIN -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown -> enter PIN(dup) -> press cabin [1-N] floor -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin roof -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin executive floor -> press intercom -> __end__(dup) -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall up -> press cabin [1-N] floor -> __end__(dup) -> press hall down -> enter PIN -> press cabin executive floor(dup) -> press alarm button -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__ -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp -> enter PIN -> press cabin executive floor(dup) -> __end__
```


### Product 23

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Projected FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 33 transitions (28 real / 5 `__end__`).

**Balanced FTS:** 10 states, 63 transitions (28 real / 15 `__end__` / 30 `__dup__` / 0 `__balance__`).

![Projected FTS — product 23](Elevator-product23-projected.png)

![Balanced FTS — product 23](Elevator-product23-balanced.png)

**All-transitions test case (`Elevator_p23_trans`)** — 63 step(s) total (28 real / 15 `__end__` / 30 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **28/28 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin lobby -> press intercom`
- **sub-walk 2**: `press cabin [1-N] floor -> press alarm button`
- **sub-walk 3**: `press hall RoofDown`
- **sub-walk 4**: `press cabin lobby -> press door open -> press door close`
- **sub-walk 5**: `press cabin [1-N] floor -> press door close`
- **sub-walk 6**: `press cabin roof`
- **sub-walk 7**: `press cabin executive floor -> press intercom`
- **sub-walk 8**: `press alarm button`
- **sub-walk 9**: `read card`
- **sub-walk 10**: `press door close -> press door open`
- **sub-walk 11**: `press cabin lobby`
- **sub-walk 12**: `press cabin roof`
- **sub-walk 13**: `press hall up -> press cabin [1-N] floor`
- **sub-walk 14**: `press hall down -> read card`
- **sub-walk 15**: `press door open`
- **sub-walk 16**: `press cabin roof`
- **sub-walk 17**: `press cabin [1-N] floor`
- **sub-walk 18**: `press hall LobbyUp -> read card`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press alarm button -> __end__(dup) -> press hall RoofDown -> read card(dup) -> press cabin lobby -> press door open -> press door close -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin [1-N] floor -> press door close -> __end__ -> press hall RoofDown(dup) -> read card(dup) -> press cabin roof -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin executive floor -> press intercom -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin executive floor(dup) -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> read card -> press cabin executive floor(dup) -> press door close -> press door open -> __end__ -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall up -> press cabin [1-N] floor -> __end__(dup) -> press hall down -> read card -> press cabin executive floor(dup) -> press door open -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> read card -> press cabin executive floor(dup) -> __end__
```


### Product 24

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Projected FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 33 transitions (28 real / 5 `__end__`).

**Balanced FTS:** 10 states, 63 transitions (28 real / 15 `__end__` / 30 `__dup__` / 0 `__balance__`).

![Projected FTS — product 24](Elevator-product24-projected.png)

![Balanced FTS — product 24](Elevator-product24-balanced.png)

**All-transitions test case (`Elevator_p24_trans`)** — 63 step(s) total (28 real / 15 `__end__` / 30 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **28/28 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin lobby -> press intercom`
- **sub-walk 2**: `press cabin [1-N] floor -> press alarm button`
- **sub-walk 3**: `press hall RoofDown`
- **sub-walk 4**: `press cabin lobby -> press door open -> press door close`
- **sub-walk 5**: `tap mobile key -> press cabin [1-N] floor -> press door close`
- **sub-walk 6**: `press cabin roof`
- **sub-walk 7**: `press cabin executive floor -> press intercom`
- **sub-walk 8**: `press alarm button`
- **sub-walk 9**: `press door close -> press door open`
- **sub-walk 10**: `tap mobile key`
- **sub-walk 11**: `press door open`
- **sub-walk 12**: `press cabin lobby`
- **sub-walk 13**: `press hall up -> press cabin roof`
- **sub-walk 14**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 15**: `press cabin roof`
- **sub-walk 16**: `press cabin [1-N] floor`
- **sub-walk 17**: `press hall LobbyUp -> tap mobile key`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press alarm button -> __end__(dup) -> press hall RoofDown -> tap mobile key(dup) -> press cabin lobby -> press door open -> press door close -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key -> press cabin [1-N] floor -> press door close -> __end__ -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin roof -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin executive floor -> press intercom -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin executive floor(dup) -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin executive floor(dup) -> press door close -> press door open -> __end__ -> press hall up(dup) -> tap mobile key -> press cabin executive floor(dup) -> press door open -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> tap mobile key -> press cabin executive floor(dup) -> __end__
```


### Product 25

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom, ManualDoorControl}

**Projected FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 9 states, 27 transitions (23 real / 4 `__end__`).

**Balanced FTS:** 9 states, 39 transitions (23 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`).

![Projected FTS — product 25](Elevator-product25-projected.png)

![Balanced FTS — product 25](Elevator-product25-balanced.png)

**All-transitions test case (`Elevator_p25_trans`)** — 39 step(s) total (23 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **23/23 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin lobby -> press intercom`
- **sub-walk 2**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **sub-walk 3**: `read card -> press cabin lobby -> press door open -> press door close`
- **sub-walk 4**: `press hall up -> press cabin lobby -> press door close -> press door open`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 7**: `read card -> press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `read card -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown -> press cabin [1-N] floor -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> read card -> press cabin lobby -> press door open -> press door close -> __end__ -> press hall up -> press cabin lobby -> press door close -> press door open -> __end__ -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> read card -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> read card -> press cabin roof -> __end__
```


### Product 26

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Projected FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 31 transitions (26 real / 5 `__end__`).

**Balanced FTS:** 10 states, 57 transitions (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`).

![Projected FTS — product 26](Elevator-product26-projected.png)

![Balanced FTS — product 26](Elevator-product26-balanced.png)

**All-transitions test case (`Elevator_p26_trans`)** — 57 step(s) total (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **26/26 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> press cabin lobby -> press intercom`
- **sub-walk 2**: `press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 3**: `press cabin lobby -> press door close`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press cabin executive floor -> press door close -> press door open`
- **sub-walk 7**: `read card`
- **sub-walk 8**: `press door open`
- **sub-walk 9**: `press cabin lobby`
- **sub-walk 10**: `press hall up -> press cabin roof`
- **sub-walk 11**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 12**: `read card`
- **sub-walk 13**: `press intercom`
- **sub-walk 14**: `press cabin roof`
- **sub-walk 15**: `press cabin [1-N] floor`
- **sub-walk 16**: `press hall LobbyUp -> read card`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press door open -> press door close -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin lobby -> press door close -> __end__ -> press hall RoofDown(dup) -> read card(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin roof -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin executive floor -> press door close -> press door open -> __end__ -> press hall RoofDown(dup) -> read card -> press cabin executive floor(dup) -> press door open -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> read card -> press cabin executive floor(dup) -> press intercom -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> read card -> press cabin executive floor(dup) -> __end__
```


### Product 27

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Projected FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 9 states, 27 transitions (23 real / 4 `__end__`).

**Balanced FTS:** 9 states, 39 transitions (23 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`).

![Projected FTS — product 27](Elevator-product27-projected.png)

![Balanced FTS — product 27](Elevator-product27-balanced.png)

**All-transitions test case (`Elevator_p27_trans`)** — 39 step(s) total (23 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **23/23 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin lobby -> press intercom`
- **sub-walk 2**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **sub-walk 3**: `tap mobile key -> press cabin lobby -> press door open -> press door close`
- **sub-walk 4**: `press hall up -> tap mobile key -> press cabin [1-N] floor -> press door close -> press door open`
- **sub-walk 5**: `press cabin lobby`
- **sub-walk 6**: `press hall down -> press cabin roof`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `tap mobile key -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown -> press cabin [1-N] floor -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key -> press cabin lobby -> press door open -> press door close -> __end__ -> press hall up -> tap mobile key -> press cabin [1-N] floor -> press door close -> press door open -> __end__ -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> tap mobile key -> press cabin roof -> __end__
```


### Product 28

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom}

**Projected FTS:** 10 states, 25 transitions (21 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 25 transitions (21 real / 4 `__end__`).

**Balanced FTS:** 10 states, 44 transitions (21 real / 9 `__end__` / 19 `__dup__` / 0 `__balance__`).

![Projected FTS — product 28](Elevator-product28-projected.png)

![Balanced FTS — product 28](Elevator-product28-balanced.png)

**All-transitions test case (`Elevator_p28_trans`)** — 44 step(s) total (21 real / 9 `__end__` / 19 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **21/21 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press intercom -> press&hold door open`
- **sub-walk 2**: `press&hold door close -> release door close`
- **sub-walk 3**: `press hall RoofDown -> press cabin lobby -> press alarm button`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `release door open`
- **sub-walk 6**: `press hall up -> press cabin lobby -> press&hold door open`
- **sub-walk 7**: `press&hold door open`
- **sub-walk 8**: `press hall down -> press cabin roof -> press&hold door close`
- **sub-walk 9**: `press&hold door close`
- **sub-walk 10**: `press cabin [1-N] floor`
- **sub-walk 11**: `press cabin roof`
- **sub-walk 12**: `press hall LobbyUp -> press cabin [1-N] floor`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby(dup) -> press intercom -> press&hold door open -> release door open(dup) -> __end__ -> press hall RoofDown(dup) -> press cabin lobby(dup) -> press intercom(dup) -> press&hold door close -> release door close -> __end__(dup) -> press hall RoofDown -> press cabin lobby -> press alarm button -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press&hold door open(dup) -> release door open -> __end__(dup) -> press hall up -> press cabin lobby -> press&hold door open -> release door open(dup) -> press&hold door open -> release door open(dup) -> __end__(dup) -> press hall down -> press cabin roof -> press&hold door close -> release door close(dup) -> press&hold door close -> release door close(dup) -> __end__ -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__
```


### Product 29

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Projected FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 8 states, 23 transitions (20 real / 3 `__end__`).

**Balanced FTS:** 8 states, 43 transitions (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`).

![Projected FTS — product 29](Elevator-product29-projected.png)

![Balanced FTS — product 29](Elevator-product29-balanced.png)

**All-transitions test case (`Elevator_p29_trans`)** — 43 step(s) total (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **20/20 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> press cabin [1-N] floor -> press intercom`
- **sub-walk 2**: `press cabin lobby`
- **sub-walk 3**: `press cabin lobby`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `read card -> press cabin roof`
- **sub-walk 6**: `press hall up -> read card -> press cabin executive floor -> press intercom`
- **sub-walk 7**: `press hall down -> press cabin lobby`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press cabin [1-N] floor`
- **sub-walk 10**: `press cabin roof`
- **sub-walk 11**: `press cabin [1-N] floor`
- **sub-walk 12**: `press hall LobbyUp -> read card`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> press cabin [1-N] floor -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> read card -> press cabin roof -> __end__(dup) -> press hall up -> read card -> press cabin executive floor -> press intercom -> __end__(dup) -> press hall down -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> read card -> press cabin executive floor(dup) -> __end__
```


### Product 30

**Selected features:** selected = {ControlButtons, Intercom, MobileKey}

**Projected FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 7 states, 20 transitions (18 real / 2 `__end__`).

**Balanced FTS:** 7 states, 34 transitions (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`).

![Projected FTS — product 30](Elevator-product30-projected.png)

![Balanced FTS — product 30](Elevator-product30-balanced.png)

**All-transitions test case (`Elevator_p30_trans`)** — 34 step(s) total (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **18/18 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `tap mobile key -> press cabin lobby -> press intercom`
- **sub-walk 2**: `press cabin [1-N] floor`
- **sub-walk 3**: `press hall RoofDown -> press cabin lobby`
- **sub-walk 4**: `press cabin lobby`
- **sub-walk 5**: `press hall up -> press cabin roof`
- **sub-walk 6**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 7**: `tap mobile key -> press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `tap mobile key -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> tap mobile key -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> tap mobile key -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> tap mobile key -> press cabin roof -> __end__(dup)
```


### Product 31

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ManualDoorControl}

**Projected FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 9 states, 26 transitions (22 real / 4 `__end__`).

**Balanced FTS:** 9 states, 38 transitions (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`).

![Projected FTS — product 31](Elevator-product31-projected.png)

![Balanced FTS — product 31](Elevator-product31-balanced.png)

**All-transitions test case (`Elevator_p31_trans`)** — 38 step(s) total (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **22/22 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin lobby -> press alarm button`
- **sub-walk 2**: `press hall RoofDown -> press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 3**: `read card -> press cabin lobby -> press door close -> press door open`
- **sub-walk 4**: `press hall up -> press cabin lobby`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 7**: `read card -> press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `read card -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin lobby -> press alarm button -> __end__ -> press hall RoofDown -> press cabin [1-N] floor -> press door open -> press door close -> __end__ -> press hall RoofDown(dup) -> read card -> press cabin lobby -> press door close -> press door open -> __end__ -> press hall up -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> read card -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> read card -> press cabin roof -> __end__(dup)
```


### Product 32

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, PinPad}

**Projected FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 9 states, 26 transitions (22 real / 4 `__end__`).

**Balanced FTS:** 9 states, 38 transitions (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`).

![Projected FTS — product 32](Elevator-product32-projected.png)

![Balanced FTS — product 32](Elevator-product32-balanced.png)

**All-transitions test case (`Elevator_p32_trans`)** — 38 step(s) total (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **22/22 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `enter PIN -> press cabin lobby -> press intercom`
- **sub-walk 2**: `press hall RoofDown -> press cabin lobby -> press door open -> press door close`
- **sub-walk 3**: `press cabin [1-N] floor -> press door close -> press door open`
- **sub-walk 4**: `press hall up -> press cabin lobby`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 7**: `enter PIN -> press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `enter PIN -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> enter PIN -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown -> press cabin lobby -> press door open -> press door close -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press door close -> press door open -> __end__ -> press hall up -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> enter PIN -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> enter PIN -> press cabin roof -> __end__(dup)
```


### Product 33

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Projected FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 31 transitions (26 real / 5 `__end__`).

**Balanced FTS:** 10 states, 57 transitions (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`).

![Projected FTS — product 33](Elevator-product33-projected.png)

![Balanced FTS — product 33](Elevator-product33-balanced.png)

**All-transitions test case (`Elevator_p33_trans`)** — 57 step(s) total (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **26/26 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> press cabin lobby -> press intercom`
- **sub-walk 2**: `press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 3**: `tap mobile key -> press cabin lobby -> press door close`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press cabin executive floor -> press door close -> press door open`
- **sub-walk 7**: `press door open`
- **sub-walk 8**: `tap mobile key`
- **sub-walk 9**: `press intercom`
- **sub-walk 10**: `press hall up -> press cabin lobby`
- **sub-walk 11**: `press hall down -> press cabin roof`
- **sub-walk 12**: `press cabin [1-N] floor`
- **sub-walk 13**: `press cabin roof`
- **sub-walk 14**: `press cabin [1-N] floor`
- **sub-walk 15**: `press hall LobbyUp -> tap mobile key`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press door open -> press door close -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key -> press cabin lobby -> press door close -> __end__ -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin roof -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin executive floor -> press door close -> press door open -> __end__ -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin executive floor(dup) -> press door open -> __end__(dup) -> press hall up(dup) -> tap mobile key -> press cabin executive floor(dup) -> press intercom -> __end__(dup) -> press hall up -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> tap mobile key -> press cabin executive floor(dup) -> __end__
```


### Product 34

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, ManualDoorControl}

**Projected FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 31 transitions (26 real / 5 `__end__`).

**Balanced FTS:** 10 states, 57 transitions (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`).

![Projected FTS — product 34](Elevator-product34-projected.png)

![Balanced FTS — product 34](Elevator-product34-balanced.png)

**All-transitions test case (`Elevator_p34_trans`)** — 57 step(s) total (26 real / 14 `__end__` / 26 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **26/26 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> press cabin lobby -> press alarm button`
- **sub-walk 2**: `press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 3**: `press cabin lobby -> press door close`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press cabin executive floor -> press alarm button`
- **sub-walk 7**: `read card`
- **sub-walk 8**: `press door close -> press door open`
- **sub-walk 9**: `press cabin lobby`
- **sub-walk 10**: `press hall up -> press cabin roof`
- **sub-walk 11**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 12**: `read card`
- **sub-walk 13**: `press door open`
- **sub-walk 14**: `press cabin roof`
- **sub-walk 15**: `press cabin [1-N] floor`
- **sub-walk 16**: `press hall LobbyUp -> read card`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> press cabin lobby -> press alarm button -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press door open -> press door close -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin lobby -> press door close -> __end__ -> press hall RoofDown(dup) -> read card(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin roof -> __end__(dup) -> press hall RoofDown(dup) -> read card(dup) -> press cabin executive floor -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> read card -> press cabin executive floor(dup) -> press door close -> press door open -> __end__ -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> read card -> press cabin executive floor(dup) -> press door open -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> read card -> press cabin executive floor(dup) -> __end__
```


### Product 35

**Selected features:** selected = {Alarm, ControlButtons, Intercom, MobileKey}

**Projected FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 7 states, 21 transitions (19 real / 2 `__end__`).

**Balanced FTS:** 7 states, 35 transitions (19 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`).

![Projected FTS — product 35](Elevator-product35-projected.png)

![Balanced FTS — product 35](Elevator-product35-balanced.png)

**All-transitions test case (`Elevator_p35_trans`)** — 35 step(s) total (19 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **19/19 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `tap mobile key -> press cabin lobby -> press intercom`
- **sub-walk 2**: `press cabin [1-N] floor -> press alarm button`
- **sub-walk 3**: `press hall RoofDown -> press cabin lobby`
- **sub-walk 4**: `press cabin lobby`
- **sub-walk 5**: `press hall up -> press cabin roof`
- **sub-walk 6**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 7**: `tap mobile key -> press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `tap mobile key -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> tap mobile key -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press alarm button -> __end__(dup) -> press hall RoofDown -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> tap mobile key -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> tap mobile key -> press cabin roof -> __end__(dup)
```


### Product 36

**Selected features:** selected = {Alarm, ControlButtons, Intercom, PinPad}

**Projected FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 7 states, 21 transitions (19 real / 2 `__end__`).

**Balanced FTS:** 7 states, 35 transitions (19 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`).

![Projected FTS — product 36](Elevator-product36-projected.png)

![Balanced FTS — product 36](Elevator-product36-balanced.png)

**All-transitions test case (`Elevator_p36_trans`)** — 35 step(s) total (19 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **19/19 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin [1-N] floor -> press intercom`
- **sub-walk 2**: `press cabin lobby -> press alarm button`
- **sub-walk 3**: `press hall RoofDown -> enter PIN -> press cabin lobby`
- **sub-walk 4**: `enter PIN -> press cabin [1-N] floor`
- **sub-walk 5**: `press hall up -> press cabin lobby`
- **sub-walk 6**: `press hall down -> press cabin roof`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `enter PIN -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin [1-N] floor -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> press alarm button -> __end__(dup) -> press hall RoofDown -> enter PIN -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> enter PIN -> press cabin [1-N] floor -> __end__(dup) -> press hall up -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> enter PIN -> press cabin roof -> __end__(dup)
```


### Product 37

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Projected FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 8 states, 23 transitions (20 real / 3 `__end__`).

**Balanced FTS:** 8 states, 43 transitions (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`).

![Projected FTS — product 37](Elevator-product37-projected.png)

![Balanced FTS — product 37](Elevator-product37-balanced.png)

**All-transitions test case (`Elevator_p37_trans`)** — 43 step(s) total (20 real / 12 `__end__` / 20 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **20/20 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press hall RoofDown -> tap mobile key -> press cabin lobby -> press intercom`
- **sub-walk 2**: `press cabin [1-N] floor`
- **sub-walk 3**: `press cabin roof`
- **sub-walk 4**: `press cabin [1-N] floor`
- **sub-walk 5**: `press cabin lobby`
- **sub-walk 6**: `press hall up -> press cabin lobby`
- **sub-walk 7**: `press hall down -> press cabin roof`
- **sub-walk 8**: `press cabin [1-N] floor`
- **sub-walk 9**: `tap mobile key -> press cabin executive floor -> press intercom`
- **sub-walk 10**: `press cabin roof`
- **sub-walk 11**: `press cabin [1-N] floor`
- **sub-walk 12**: `press hall LobbyUp -> tap mobile key`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown -> tap mobile key -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> tap mobile key(dup) -> press cabin roof -> __end__(dup) -> press hall RoofDown(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall up -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> tap mobile key -> press cabin executive floor -> press intercom -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> tap mobile key -> press cabin executive floor(dup) -> __end__
```


### Product 38

**Selected features:** selected = {Alarm, ControlButtons, PinPad}

**Projected FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 7 states, 20 transitions (18 real / 2 `__end__`).

**Balanced FTS:** 7 states, 34 transitions (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`).

![Projected FTS — product 38](Elevator-product38-projected.png)

![Balanced FTS — product 38](Elevator-product38-balanced.png)

**All-transitions test case (`Elevator_p38_trans`)** — 34 step(s) total (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **18/18 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin [1-N] floor -> press alarm button`
- **sub-walk 2**: `press cabin lobby`
- **sub-walk 3**: `press hall RoofDown -> enter PIN -> press cabin lobby`
- **sub-walk 4**: `enter PIN -> press cabin [1-N] floor`
- **sub-walk 5**: `press hall up -> press cabin lobby`
- **sub-walk 6**: `press hall down -> press cabin roof`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `enter PIN -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin [1-N] floor -> press alarm button -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown -> enter PIN -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> enter PIN -> press cabin [1-N] floor -> __end__(dup) -> press hall up -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> enter PIN -> press cabin roof -> __end__(dup)
```


### Product 39

**Selected features:** selected = {ControlButtons, Intercom, PinPad}

**Projected FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 7 states, 20 transitions (18 real / 2 `__end__`).

**Balanced FTS:** 7 states, 34 transitions (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`).

![Projected FTS — product 39](Elevator-product39-projected.png)

![Balanced FTS — product 39](Elevator-product39-balanced.png)

**All-transitions test case (`Elevator_p39_trans`)** — 34 step(s) total (18 real / 10 `__end__` / 14 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **18/18 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `press cabin [1-N] floor -> press intercom`
- **sub-walk 2**: `press cabin lobby`
- **sub-walk 3**: `press hall RoofDown -> enter PIN -> press cabin lobby`
- **sub-walk 4**: `enter PIN -> press cabin [1-N] floor`
- **sub-walk 5**: `press hall up -> press cabin lobby`
- **sub-walk 6**: `press hall down -> press cabin roof`
- **sub-walk 7**: `press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `enter PIN -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> press cabin [1-N] floor -> press intercom -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> __end__(dup) -> press hall RoofDown -> enter PIN -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> enter PIN -> press cabin [1-N] floor -> __end__(dup) -> press hall up -> press cabin lobby -> __end__(dup) -> press hall down -> press cabin roof -> __end__(dup) -> press hall up(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> enter PIN -> press cabin roof -> __end__(dup)
```


### Product 40

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, PinPad}

**Projected FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 9 states, 26 transitions (22 real / 4 `__end__`).

**Balanced FTS:** 9 states, 38 transitions (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`).

![Projected FTS — product 40](Elevator-product40-projected.png)

![Balanced FTS — product 40](Elevator-product40-balanced.png)

**All-transitions test case (`Elevator_p40_trans`)** — 38 step(s) total (22 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **22/22 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `enter PIN -> press cabin lobby -> press alarm button`
- **sub-walk 2**: `press hall RoofDown -> press cabin lobby -> press door open -> press door close`
- **sub-walk 3**: `press cabin [1-N] floor -> press door close -> press door open`
- **sub-walk 4**: `press hall up -> press cabin lobby`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 7**: `enter PIN -> press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `enter PIN -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> enter PIN -> press cabin lobby -> press alarm button -> __end__ -> press hall RoofDown -> press cabin lobby -> press door open -> press door close -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press door close -> press door open -> __end__ -> press hall up -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> enter PIN -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp(dup) -> enter PIN -> press cabin roof -> __end__(dup)
```


### Product 41

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Projected FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 10 states, 33 transitions (28 real / 5 `__end__`).

**Balanced FTS:** 10 states, 63 transitions (28 real / 15 `__end__` / 30 `__dup__` / 0 `__balance__`).

![Projected FTS — product 41](Elevator-product41-projected.png)

![Balanced FTS — product 41](Elevator-product41-balanced.png)

**All-transitions test case (`Elevator_p41_trans`)** — 63 step(s) total (28 real / 15 `__end__` / 30 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **28/28 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `enter PIN -> press cabin lobby -> press intercom`
- **sub-walk 2**: `press cabin [1-N] floor -> press alarm button`
- **sub-walk 3**: `press hall RoofDown`
- **sub-walk 4**: `press cabin roof -> press door open -> press door close`
- **sub-walk 5**: `press cabin executive floor -> press intercom`
- **sub-walk 6**: `press alarm button`
- **sub-walk 7**: `press door close`
- **sub-walk 8**: `press cabin lobby -> press door close -> press door open`
- **sub-walk 9**: `press cabin [1-N] floor`
- **sub-walk 10**: `press cabin lobby`
- **sub-walk 11**: `press cabin roof`
- **sub-walk 12**: `press hall up -> press cabin [1-N] floor`
- **sub-walk 13**: `press hall down -> enter PIN`
- **sub-walk 14**: `press door open`
- **sub-walk 15**: `press cabin roof`
- **sub-walk 16**: `press cabin [1-N] floor`
- **sub-walk 17**: `press hall LobbyUp -> enter PIN`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> enter PIN -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin [1-N] floor -> press alarm button -> __end__(dup) -> press hall RoofDown -> enter PIN(dup) -> press cabin roof -> press door open -> press door close -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin executive floor -> press intercom -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin executive floor(dup) -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> enter PIN(dup) -> press cabin executive floor(dup) -> press door close -> __end__ -> press hall RoofDown(dup) -> press cabin lobby -> press door close -> press door open -> __end__ -> press hall RoofDown(dup) -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> press cabin lobby -> __end__(dup) -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall up -> press cabin [1-N] floor -> __end__(dup) -> press hall down -> enter PIN -> press cabin executive floor(dup) -> press door open -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin [1-N] floor -> __end__ -> press hall LobbyUp -> enter PIN -> press cabin executive floor(dup) -> __end__
```


### Product 42

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, PinPad}

**Projected FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Repaired FTS (after `InitialSccFilter`):** 9 states, 27 transitions (23 real / 4 `__end__`).

**Balanced FTS:** 9 states, 39 transitions (23 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`).

![Projected FTS — product 42](Elevator-product42-projected.png)

![Balanced FTS — product 42](Elevator-product42-balanced.png)

**All-transitions test case (`Elevator_p42_trans`)** — 39 step(s) total (23 real / 10 `__end__` / 12 `__dup__` / 0 `__balance__`). Real-transition coverage on the repaired FTS: **23/23 = 100.0%**.

Sub-walks between synthetic boundaries:

- **sub-walk 1**: `enter PIN -> press cabin lobby -> press intercom`
- **sub-walk 2**: `press hall RoofDown -> press cabin lobby -> press alarm button`
- **sub-walk 3**: `press cabin [1-N] floor -> press door open -> press door close`
- **sub-walk 4**: `press hall up -> press cabin lobby -> press door close -> press door open`
- **sub-walk 5**: `press cabin roof`
- **sub-walk 6**: `press hall down -> press cabin [1-N] floor`
- **sub-walk 7**: `enter PIN -> press cabin [1-N] floor`
- **sub-walk 8**: `press cabin roof`
- **sub-walk 9**: `press hall LobbyUp -> press cabin [1-N] floor`
- **sub-walk 10**: `enter PIN -> press cabin roof`

Full cycle (synthetic actions shown verbatim):

```
press hall RoofDown(dup) -> enter PIN -> press cabin lobby -> press intercom -> __end__ -> press hall RoofDown -> press cabin lobby -> press alarm button -> __end__(dup) -> press hall RoofDown(dup) -> press cabin [1-N] floor -> press door open -> press door close -> __end__ -> press hall up -> press cabin lobby -> press door close -> press door open -> __end__ -> press hall up(dup) -> press cabin roof -> __end__(dup) -> press hall down -> press cabin [1-N] floor -> __end__(dup) -> press hall up(dup) -> enter PIN -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> press cabin roof -> __end__(dup) -> press hall LobbyUp -> press cabin [1-N] floor -> __end__(dup) -> press hall LobbyUp(dup) -> enter PIN -> press cabin roof -> __end__
```

---

Total products: 42.
