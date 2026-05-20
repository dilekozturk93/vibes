# Per-Product All-Transition-Pairs Coverage — Elevator

## How the all-transition-pairs test suite is built

**Why a fresh transformation, not `vibes-transformation`.** The legacy `vibes-transformation` module (not in the root pom) is an I/O and format-conversion layer (AUT &harr; LTS, XML readers, Dot printers, structural pruning) operating on the obsolete `be.unamur.transitionsystem.*` namespace; it does not contain anything for pair-graph / k-tuple coverage transformation and is dormant. The L=2 pair graph here is the FTS analog of the ESG-Fx-side `TransformedESGFxGenerator` (from the user's prior published study); we re-implement against the current `be.vibes.ts.*` types directly.

**Reduction insight.** The pair-coverage problem on FTS *F* reduces to the edge-coverage problem on a transformed FTS *P(F)* — the *pair graph*. A Hierholzer Euler cycle on *P(F)* visits every edge of *P(F)* exactly once, which by construction means every contiguous transition pair of *F* is exercised in the resulting test sequence.

**Step 1 — Project onto the product.** Same as the other criteria: `FExpressionPreservingProjection.project(fts, config)` keeps transitions whose feature expression evaluates true; reachability filter drops unreachable states.

**Step 2 — Repair strong connectivity on the original FTS.** Same as the other criteria: `InitialSccFilter.keepInitialScc(projected)`.

**Step 3 — Build the pair graph.** [`PairGraphTransformer.transform(repaired)`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/PairGraphTransformer.java) produces a pair graph *P(F)* whose:

- **node** = original transition (named `p_<source>_<action>_<target>`), plus a synthetic `INIT` node representing "no transition executed yet";
- **edge** `INIT -> p(t)` (labelled `action(t)`) for every original transition `t` starting at the original initial state;
- **edge** `p(t1) -> p(t2)` (labelled `action(t2)`) for every ordered pair `(t1, t2)` with `target(t1) == source(t2)`.

Complexity: enumerate via an outgoing-by-source index on the original FTS, so the construction is *O(|T| + sum_s out_deg(s))* rather than *O(|T|^2)*. The transformer also returns a side-map `pairStateToOriginalTransition` used in step 6 to translate the pair-graph cycle back to original transitions.

**Step 4 — Balance (no SCC precheck).** The pair graph is intentionally not strongly connected at this point: INIT has out-degree N (one per original initial-state transition) and in-degree 0. Running the usual `InitialSccFilter` here would discard everything except INIT. [`EulerianBalancer.balanceWithoutPrecheck(pairGraph)`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/EulerianBalancer.java) skips the SCC check and supplies the missing INIT-incoming edges as synthetic `__balance__N` edges — exactly enough to make every pair-graph state in-balanced AND restore strong connectivity. The balanced pair graph is verified to be strongly connected after this step; if it isn't, the projection has produced an unrepaired pair-graph fragment (does not happen on the three MVP SPLs).

**Step 5 — Hierholzer Euler cycle on the balanced pair graph.** [`HierholzerEulerCycle.compute(balanced)`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/HierholzerEulerCycle.java) returns one contiguous cycle visiting every pair-graph edge exactly once. By the reduction insight above, every contiguous transition pair of the original FTS is covered.

**Step 6 — Split + translate back.** Synthetic `__balance__N` edges in the pair-graph cycle cannot be replayed in the original FTS (they encode "teleport back to INIT"), so the cycle is split at every synthetic edge. Each non-synthetic segment is translated: each pair-graph edge `p(t1) -> p(t2)` corresponds to executing `t2` in the original FTS (the side-map gives us `t2` from the edge's target pair-state). One subtlety: the FIRST pair-graph edge of a non-INIT-starting segment carries a real pair `(t_Y, t_X)` that would otherwise be split across two test cases by the synthetic teleport. To preserve coverage we PREPEND `t_Y` to the segment (the test case then starts mid-FTS at `source(t_Y)` — legal as long as the executor is reset between test cases).

**Step 7 — Wrap into a List<TestCase>.** Unlike all-transitions and all-states (which return one TestCase), this generator returns a *suite* because the pair graph naturally yields multiple test cases (one per real segment between synthetic teleports). For display each TestCase is additionally split at every visit to the original repaired FTS's initial state via `TestCaseSplitter.splitAtInitialReturns(...)` — same operational test-case semantics as the other two reports.

**Coverage claim.** Hierholzer visits every pair-graph edge exactly once → every contiguous original transition pair is covered, by construction. The pair-graph reachable-pair count is the denominator; pairs that exist only via dropped transitions (post-projection) are NOT in the denominator, which is the correct semantics for product-level coverage.

---

## Products

Three FTS images per product: (a) the repaired original FTS — source of the pair info; (b) the pair graph BEFORE balancing — INIT is source-only and the graph is not Eulerian; (c) the pair graph AFTER balancing — synthetic edges (`__balance__N`) added back to INIT shown as dashed-red. Generated test cases are listed underneath, split at every visit to the original repaired FTS's initial state. Synthetic actions are hidden / `__dup__N` is stripped, same convention as the other reports.


### Product 1

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Pair graph (raw):** 24 nodes (incl. INIT), 66 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 24 nodes, 127 edges (61 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 1](Elevator-product1-repaired.png)

![Pair graph (raw) — product 1](Elevator-product1-pairgraph-raw.png)

![Pair graph (balanced) — product 1](Elevator-product1-pairgraph-balanced.png)

**Generated test suite** — 32 test case(s) total (81 real step(s); pair-graph cycle has 127 edge(s) total, 61 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press hall down -> press cabin lobby`
- **test case 3**: `press hall RoofDown -> press cabin lobby -> press intercom`
- **test case 4**: `press hall RoofDown`
- **test case 5**: `enter PIN -> press cabin lobby -> press intercom`
- **test case 6**: `enter PIN -> press cabin [1-N] floor -> press intercom`
- **test case 7**: `press hall RoofDown`
- **test case 8**: `enter PIN -> press cabin executive floor`
- **test case 9**: `press hall LobbyUp -> press cabin roof`
- **test case 10**: `press hall LobbyUp`
- **test case 11**: `press hall up -> press cabin lobby -> press intercom`
- **test case 12**: `press hall up -> press cabin roof -> press intercom`
- **test case 13**: `press hall down -> press cabin roof`
- **test case 14**: `press hall RoofDown -> enter PIN -> press cabin roof`
- **test case 15**: `press cabin roof -> press intercom`
- **test case 16**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 17**: `press hall RoofDown -> press cabin [1-N] floor -> press intercom`
- **test case 18**: `press hall LobbyUp`
- **test case 19**: `press cabin [1-N] floor -> press intercom`
- **test case 20**: `press hall down -> enter PIN -> press cabin [1-N] floor`
- **test case 21**: `press hall up -> enter PIN -> press cabin roof`
- **test case 22**: `enter PIN -> press cabin [1-N] floor`
- **test case 23**: `press hall LobbyUp`
- **test case 24**: `enter PIN -> press cabin executive floor`
- **test case 25**: `press hall down -> press cabin [1-N] floor`
- **test case 26**: `press hall up`
- **test case 27**: `enter PIN -> press cabin lobby`
- **test case 28**: `enter PIN -> press cabin executive floor -> press intercom`
- **test case 29**: `press hall LobbyUp -> enter PIN -> press cabin roof -> press intercom`
- **test case 30**: `press hall LobbyUp`
- **test case 31**: `enter PIN -> press cabin lobby`
- **test case 32**: `press hall up -> press cabin [1-N] floor -> press intercom`


### Product 2

**Selected features:** selected = {Alarm, ControlButtons, MobileKey}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Pair graph (raw):** 21 nodes (incl. INIT), 56 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 21 nodes, 100 edges (44 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 2](Elevator-product2-repaired.png)

![Pair graph (raw) — product 2](Elevator-product2-pairgraph-raw.png)

![Pair graph (balanced) — product 2](Elevator-product2-pairgraph-balanced.png)

**Generated test suite** — 27 test case(s) total (68 real step(s); pair-graph cycle has 100 edge(s) total, 44 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press cabin lobby -> press alarm button`
- **test case 3**: `press hall RoofDown -> press cabin lobby`
- **test case 4**: `press hall down -> press cabin lobby -> press alarm button`
- **test case 5**: `press hall up -> press cabin lobby`
- **test case 6**: `press hall down -> press cabin roof`
- **test case 7**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **test case 8**: `press hall down -> press cabin [1-N] floor -> press alarm button`
- **test case 9**: `press hall LobbyUp -> press cabin roof`
- **test case 10**: `tap mobile key -> press cabin roof`
- **test case 11**: `press hall RoofDown -> tap mobile key -> press cabin lobby`
- **test case 12**: `press hall RoofDown`
- **test case 13**: `tap mobile key -> press cabin [1-N] floor -> press alarm button`
- **test case 14**: `press cabin roof -> press alarm button`
- **test case 15**: `press hall LobbyUp`
- **test case 16**: `tap mobile key -> press cabin [1-N] floor`
- **test case 17**: `press hall LobbyUp -> tap mobile key -> press cabin roof`
- **test case 18**: `press hall LobbyUp`
- **test case 19**: `tap mobile key -> press cabin lobby`
- **test case 20**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 21**: `press hall up -> press cabin roof -> press alarm button`
- **test case 22**: `press hall down -> tap mobile key -> press cabin [1-N] floor`
- **test case 23**: `press hall up -> press cabin [1-N] floor`
- **test case 24**: `tap mobile key -> press cabin roof -> press alarm button`
- **test case 25**: `press hall up -> tap mobile key -> press cabin lobby -> press alarm button`
- **test case 26**: `press hall LobbyUp`
- **test case 27**: `press cabin [1-N] floor -> press alarm button`


### Product 3

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Pair graph (raw):** 24 nodes (incl. INIT), 66 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 24 nodes, 127 edges (61 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 3](Elevator-product3-repaired.png)

![Pair graph (raw) — product 3](Elevator-product3-pairgraph-raw.png)

![Pair graph (balanced) — product 3](Elevator-product3-pairgraph-balanced.png)

**Generated test suite** — 35 test case(s) total (84 real step(s); pair-graph cycle has 127 edge(s) total, 61 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `read card -> press cabin [1-N] floor -> press alarm button`
- **test case 3**: `press hall RoofDown -> read card`
- **test case 4**: `press hall RoofDown`
- **test case 5**: `read card -> press cabin executive floor`
- **test case 6**: `press hall LobbyUp`
- **test case 7**: `read card -> press cabin [1-N] floor`
- **test case 8**: `press hall down -> press cabin lobby -> press alarm button`
- **test case 9**: `press hall RoofDown`
- **test case 10**: `press hall up -> press cabin lobby`
- **test case 11**: `read card -> press cabin executive floor`
- **test case 12**: `press hall down -> press cabin roof`
- **test case 13**: `read card -> press cabin lobby`
- **test case 14**: `press hall RoofDown`
- **test case 15**: `read card -> press cabin roof`
- **test case 16**: `press cabin lobby -> press alarm button`
- **test case 17**: `press hall RoofDown -> press cabin lobby`
- **test case 18**: `press hall up -> press cabin roof -> press alarm button`
- **test case 19**: `press hall down -> read card -> press cabin executive floor -> press alarm button`
- **test case 20**: `read card -> press cabin lobby`
- **test case 21**: `press cabin [1-N] floor -> press alarm button`
- **test case 22**: `press hall LobbyUp -> read card -> press cabin roof`
- **test case 23**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 24**: `press hall LobbyUp -> press cabin roof`
- **test case 25**: `press hall LobbyUp`
- **test case 26**: `press cabin roof -> press alarm button`
- **test case 27**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 28**: `press hall up -> press cabin [1-N] floor -> press alarm button`
- **test case 29**: `press hall down -> press cabin [1-N] floor`
- **test case 30**: `read card -> press cabin [1-N] floor`
- **test case 31**: `press hall up -> read card -> press cabin roof -> press alarm button`
- **test case 32**: `press hall up`
- **test case 33**: `read card -> press cabin lobby -> press alarm button`
- **test case 34**: `press hall LobbyUp`
- **test case 35**: `press cabin [1-N] floor -> press alarm button`


### Product 4

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Pair graph (raw):** 27 nodes (incl. INIT), 92 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 27 nodes, 207 edges (115 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 4](Elevator-product4-repaired.png)

![Pair graph (raw) — product 4](Elevator-product4-pairgraph-raw.png)

![Pair graph (balanced) — product 4](Elevator-product4-pairgraph-balanced.png)

**Generated test suite** — 55 test case(s) total (128 real step(s); pair-graph cycle has 207 edge(s) total, 115 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press door open -> press door close -> press door open`
- **test case 3**: `press hall RoofDown`
- **test case 4**: `press cabin lobby -> press door open`
- **test case 5**: `press hall down -> press cabin lobby -> press door open`
- **test case 6**: `press hall RoofDown -> press cabin lobby`
- **test case 7**: `press hall RoofDown`
- **test case 8**: `press cabin lobby -> press intercom`
- **test case 9**: `press hall up -> press cabin lobby`
- **test case 10**: `press door close -> press door open -> press door close`
- **test case 11**: `press cabin lobby -> press door close`
- **test case 12**: `press cabin [1-N] floor -> press door close`
- **test case 13**: `press cabin [1-N] floor -> press door open`
- **test case 14**: `press hall down -> press cabin roof -> press intercom`
- **test case 15**: `press hall up`
- **test case 16**: `press cabin lobby -> press intercom`
- **test case 17**: `press cabin lobby -> press door close`
- **test case 18**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 19**: `press cabin [1-N] floor -> press intercom`
- **test case 20**: `press hall down -> press cabin [1-N] floor -> press door open`
- **test case 21**: `press hall RoofDown`
- **test case 22**: `tap mobile key -> press cabin roof`
- **test case 23**: `press hall RoofDown -> tap mobile key`
- **test case 24**: `press cabin roof -> press door close`
- **test case 25**: `tap mobile key -> press cabin lobby -> press door open`
- **test case 26**: `press hall up`
- **test case 27**: `press cabin roof -> press door open`
- **test case 28**: `press hall up -> press cabin roof -> press door close`
- **test case 29**: `press cabin lobby -> press intercom`
- **test case 30**: `press hall LobbyUp -> press cabin roof`
- **test case 31**: `press hall LobbyUp`
- **test case 32**: `press hall up`
- **test case 33**: `press hall up -> press cabin [1-N] floor -> press door close`
- **test case 34**: `press hall RoofDown`
- **test case 35**: `tap mobile key -> press cabin [1-N] floor -> press intercom`
- **test case 36**: `press hall down`
- **test case 37**: `tap mobile key -> press cabin [1-N] floor -> press door close`
- **test case 38**: `press hall LobbyUp`
- **test case 39**: `press cabin roof -> press door open`
- **test case 40**: `press hall LobbyUp`
- **test case 41**: `press cabin roof -> press intercom`
- **test case 42**: `press cabin roof -> press door close`
- **test case 43**: `press hall down -> tap mobile key`
- **test case 44**: `press cabin [1-N] floor -> press door open`
- **test case 45**: `tap mobile key -> press cabin [1-N] floor`
- **test case 46**: `press hall up`
- **test case 47**: `press cabin [1-N] floor -> press intercom`
- **test case 48**: `press hall LobbyUp -> tap mobile key -> press cabin roof -> press intercom`
- **test case 49**: `tap mobile key -> press cabin lobby`
- **test case 50**: `press hall LobbyUp`
- **test case 51**: `press cabin [1-N] floor -> press door close`
- **test case 52**: `tap mobile key -> press cabin lobby -> press door close`
- **test case 53**: `press hall up -> tap mobile key -> press cabin roof -> press door open`
- **test case 54**: `press hall LobbyUp -> press cabin [1-N] floor -> press intercom`
- **test case 55**: `press cabin [1-N] floor -> press door open`


### Product 5

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService}

**Repaired FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

**Pair graph (raw):** 25 nodes (incl. INIT), 71 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 25 nodes, 162 edges (91 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 5](Elevator-product5-repaired.png)

![Pair graph (raw) — product 5](Elevator-product5-pairgraph-raw.png)

![Pair graph (balanced) — product 5](Elevator-product5-pairgraph-balanced.png)

**Generated test suite** — 37 test case(s) total (94 real step(s); pair-graph cycle has 162 edge(s) total, 91 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press cabin lobby -> press alarm button -> press&hold door close -> release door close`
- **test case 3**: `press hall LobbyUp -> press cabin roof`
- **test case 4**: `press hall down -> press cabin lobby -> press&hold door close`
- **test case 5**: `press cabin roof -> press alarm button -> press&hold door open -> release door open -> press&hold door open -> release door open`
- **test case 6**: `press hall up -> press cabin lobby`
- **test case 7**: `press cabin roof -> press&hold door close`
- **test case 8**: `press cabin roof -> press&hold door open`
- **test case 9**: `press hall down`
- **test case 10**: `press cabin roof -> press&hold door open`
- **test case 11**: `press cabin [1-N] floor -> press&hold door open`
- **test case 12**: `press hall LobbyUp`
- **test case 13**: `press cabin lobby -> press&hold door close`
- **test case 14**: `press hall RoofDown -> press cabin lobby`
- **test case 15**: `press cabin lobby -> press&hold door open`
- **test case 16**: `press cabin [1-N] floor -> press&hold door open -> release door open`
- **test case 17**: `press hall RoofDown`
- **test case 18**: `press hall down`
- **test case 19**: `press cabin [1-N] floor -> press alarm button`
- **test case 20**: `press hall RoofDown`
- **test case 21**: `press cabin [1-N] floor -> press&hold door close -> release door close`
- **test case 22**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 23**: `press hall LobbyUp -> press cabin [1-N] floor -> press&hold door close`
- **test case 24**: `press hall down -> press cabin roof -> press&hold door close`
- **test case 25**: `press hall up`
- **test case 26**: `press cabin lobby -> press alarm button`
- **test case 27**: `press hall up`
- **test case 28**: `press cabin lobby -> press&hold door open`
- **test case 29**: `press hall up -> press cabin roof -> press alarm button`
- **test case 30**: `press hall up -> press cabin [1-N] floor -> press&hold door close`
- **test case 31**: `release door close -> press&hold door close -> release door close`
- **test case 32**: `press cabin [1-N] floor -> press alarm button`
- **test case 33**: `press hall down -> press cabin [1-N] floor`
- **test case 34**: `press cabin [1-N] floor -> press&hold door open`
- **test case 35**: `press hall LobbyUp`
- **test case 36**: `press cabin [1-N] floor -> press alarm button`
- **test case 37**: `press hall LobbyUp`


### Product 6

**Selected features:** selected = {CardReader, ControlButtons, Intercom, ManualDoorControl}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Pair graph (raw):** 27 nodes (incl. INIT), 92 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 27 nodes, 207 edges (115 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 6](Elevator-product6-repaired.png)

![Pair graph (raw) — product 6](Elevator-product6-pairgraph-raw.png)

![Pair graph (balanced) — product 6](Elevator-product6-pairgraph-balanced.png)

**Generated test suite** — 51 test case(s) total (124 real step(s); pair-graph cycle has 207 edge(s) total, 115 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press cabin [1-N] floor -> press intercom`
- **test case 3**: `read card -> press cabin [1-N] floor -> press door close -> press door open`
- **test case 4**: `press hall RoofDown`
- **test case 5**: `read card -> press cabin lobby -> press door open -> press door close -> press door open -> press door close`
- **test case 6**: `press hall RoofDown -> read card`
- **test case 7**: `press cabin lobby -> press intercom`
- **test case 8**: `press hall RoofDown`
- **test case 9**: `read card -> press cabin roof`
- **test case 10**: `press hall down -> press cabin lobby -> press door open`
- **test case 11**: `press cabin roof -> press door close`
- **test case 12**: `press cabin lobby -> press door open`
- **test case 13**: `press hall down -> press cabin roof -> press intercom`
- **test case 14**: `press hall up -> press cabin lobby`
- **test case 15**: `press hall RoofDown -> press cabin lobby -> press intercom`
- **test case 16**: `press hall up`
- **test case 17**: `press cabin lobby -> press intercom`
- **test case 18**: `press cabin lobby -> press door close`
- **test case 19**: `press hall up -> press cabin roof -> press door open`
- **test case 20**: `press cabin roof -> press door close`
- **test case 21**: `press cabin lobby -> press door close`
- **test case 22**: `press cabin [1-N] floor -> press door close`
- **test case 23**: `press cabin [1-N] floor -> press door open`
- **test case 24**: `press hall up`
- **test case 25**: `press cabin [1-N] floor -> press door open`
- **test case 26**: `press hall up -> press cabin [1-N] floor`
- **test case 27**: `read card -> press cabin [1-N] floor -> press door open`
- **test case 28**: `press hall LobbyUp`
- **test case 29**: `read card -> press cabin lobby`
- **test case 30**: `press hall LobbyUp -> read card -> press cabin roof -> press intercom`
- **test case 31**: `press hall down -> read card -> press cabin [1-N] floor`
- **test case 32**: `press hall up`
- **test case 33**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 34**: `press hall down -> press cabin [1-N] floor -> press door close`
- **test case 35**: `press hall RoofDown`
- **test case 36**: `press cabin [1-N] floor -> press intercom`
- **test case 37**: `press hall LobbyUp -> press cabin roof`
- **test case 38**: `press hall up`
- **test case 39**: `read card -> press cabin roof -> press door open`
- **test case 40**: `press hall LobbyUp`
- **test case 41**: `press cabin roof -> press door open`
- **test case 42**: `press cabin roof -> press intercom`
- **test case 43**: `press cabin roof -> press door close`
- **test case 44**: `press hall LobbyUp`
- **test case 45**: `press cabin [1-N] floor -> press door close`
- **test case 46**: `press hall up -> read card -> press cabin lobby -> press door close`
- **test case 47**: `press hall down`
- **test case 48**: `press cabin [1-N] floor -> press intercom`
- **test case 49**: `press hall LobbyUp`
- **test case 50**: `press cabin [1-N] floor -> press intercom`
- **test case 51**: `press hall LobbyUp -> press cabin [1-N] floor -> press door open`


### Product 7

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Pair graph (raw):** 26 nodes (incl. INIT), 79 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 26 nodes, 151 edges (72 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 7](Elevator-product7-repaired.png)

![Pair graph (raw) — product 7](Elevator-product7-pairgraph-raw.png)

![Pair graph (balanced) — product 7](Elevator-product7-pairgraph-balanced.png)

**Generated test suite** — 38 test case(s) total (98 real step(s); pair-graph cycle has 151 edge(s) total, 72 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press cabin lobby -> press alarm button`
- **test case 3**: `press hall down -> press cabin lobby -> press alarm button`
- **test case 4**: `press hall RoofDown -> press cabin lobby`
- **test case 5**: `press cabin lobby -> press intercom`
- **test case 6**: `press cabin [1-N] floor -> press alarm button`
- **test case 7**: `press hall RoofDown -> press cabin [1-N] floor -> press intercom`
- **test case 8**: `tap mobile key -> press cabin roof`
- **test case 9**: `tap mobile key -> press cabin lobby -> press intercom`
- **test case 10**: `tap mobile key -> press cabin [1-N] floor -> press intercom`
- **test case 11**: `press hall up -> press cabin lobby`
- **test case 12**: `press hall RoofDown`
- **test case 13**: `press hall RoofDown -> tap mobile key -> press cabin executive floor`
- **test case 14**: `press hall LobbyUp -> press cabin roof`
- **test case 15**: `press hall up`
- **test case 16**: `press cabin lobby -> press intercom`
- **test case 17**: `press cabin roof -> press intercom`
- **test case 18**: `press hall up -> press cabin roof`
- **test case 19**: `press hall down -> press cabin roof -> press alarm button`
- **test case 20**: `press cabin [1-N] floor -> press alarm button`
- **test case 21**: `press hall down -> press cabin [1-N] floor`
- **test case 22**: `press hall LobbyUp`
- **test case 23**: `press cabin roof -> press alarm button`
- **test case 24**: `press cabin roof -> press intercom`
- **test case 25**: `tap mobile key -> press cabin [1-N] floor -> press alarm button`
- **test case 26**: `press hall LobbyUp -> tap mobile key -> press cabin roof -> press alarm button`
- **test case 27**: `press hall down -> tap mobile key -> press cabin [1-N] floor`
- **test case 28**: `press hall up -> press cabin [1-N] floor -> press intercom`
- **test case 29**: `tap mobile key -> press cabin lobby`
- **test case 30**: `tap mobile key -> press cabin roof -> press intercom`
- **test case 31**: `press hall LobbyUp`
- **test case 32**: `tap mobile key -> press cabin executive floor -> press alarm button`
- **test case 33**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 34**: `tap mobile key -> press cabin executive floor -> press intercom`
- **test case 35**: `press cabin [1-N] floor -> press intercom`
- **test case 36**: `press hall up -> tap mobile key -> press cabin lobby -> press alarm button`
- **test case 37**: `press hall LobbyUp`
- **test case 38**: `press cabin [1-N] floor -> press alarm button`


### Product 8

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Pair graph (raw):** 32 nodes (incl. INIT), 108 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 32 nodes, 253 edges (145 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 8](Elevator-product8-repaired.png)

![Pair graph (raw) — product 8](Elevator-product8-pairgraph-raw.png)

![Pair graph (balanced) — product 8](Elevator-product8-pairgraph-balanced.png)

**Generated test suite** — 59 test case(s) total (144 real step(s); pair-graph cycle has 253 edge(s) total, 145 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press door open -> press door close -> press door open`
- **test case 3**: `press hall down -> press cabin lobby -> press door open`
- **test case 4**: `press hall up -> press cabin roof`
- **test case 5**: `press hall down -> press cabin roof -> press door close -> press door open`
- **test case 6**: `press hall up`
- **test case 7**: `press cabin roof -> press alarm button`
- **test case 8**: `press hall down -> enter PIN -> press cabin lobby -> press door close`
- **test case 9**: `press cabin lobby -> press door open`
- **test case 10**: `press hall up -> enter PIN -> press cabin executive floor -> press alarm button`
- **test case 11**: `press hall RoofDown -> press cabin lobby -> press alarm button`
- **test case 12**: `press cabin lobby -> press door close`
- **test case 13**: `press cabin lobby -> press alarm button`
- **test case 14**: `enter PIN -> press cabin lobby -> press door open`
- **test case 15**: `press hall RoofDown`
- **test case 16**: `enter PIN -> press cabin [1-N] floor -> press door close`
- **test case 17**: `press hall RoofDown`
- **test case 18**: `enter PIN -> press cabin roof -> press door close`
- **test case 19**: `press hall RoofDown`
- **test case 20**: `press hall LobbyUp`
- **test case 21**: `press hall LobbyUp`
- **test case 22**: `press cabin [1-N] floor -> press door open`
- **test case 23**: `press hall up`
- **test case 24**: `enter PIN -> press cabin [1-N] floor -> press alarm button`
- **test case 25**: `enter PIN -> press cabin executive floor`
- **test case 26**: `press hall RoofDown`
- **test case 27**: `press hall down`
- **test case 28**: `press cabin [1-N] floor -> press door open`
- **test case 29**: `press cabin executive floor -> press door open`
- **test case 30**: `press hall RoofDown -> enter PIN`
- **test case 31**: `press door open -> press door close`
- **test case 32**: `press cabin executive floor -> press door close -> press door open -> press door close`
- **test case 33**: `press hall LobbyUp -> press cabin [1-N] floor -> press door close`
- **test case 34**: `press cabin [1-N] floor -> press alarm button`
- **test case 35**: `press cabin [1-N] floor -> press door close`
- **test case 36**: `press hall LobbyUp -> press cabin roof`
- **test case 37**: `enter PIN -> press cabin roof -> press alarm button`
- **test case 38**: `press hall RoofDown -> press cabin [1-N] floor -> press door open`
- **test case 39**: `press hall LobbyUp`
- **test case 40**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 41**: `press cabin [1-N] floor -> press alarm button`
- **test case 42**: `press hall up`
- **test case 43**: `press cabin lobby -> press door close`
- **test case 44**: `press hall LobbyUp`
- **test case 45**: `press cabin roof -> press alarm button`
- **test case 46**: `press cabin [1-N] floor -> press alarm button`
- **test case 47**: `press hall LobbyUp`
- **test case 48**: `press cabin roof -> press door open`
- **test case 49**: `press cabin roof -> press door close`
- **test case 50**: `press hall LobbyUp -> enter PIN -> press cabin [1-N] floor -> press door open`
- **test case 51**: `enter PIN -> press cabin roof -> press door open`
- **test case 52**: `enter PIN -> press cabin executive floor`
- **test case 53**: `press hall up -> press cabin [1-N] floor`
- **test case 54**: `press hall up`
- **test case 55**: `press hall LobbyUp`
- **test case 56**: `enter PIN -> press cabin lobby`
- **test case 57**: `press hall RoofDown`
- **test case 58**: `press hall down -> press cabin [1-N] floor -> press door close`
- **test case 59**: `press hall down`


### Product 9

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Pair graph (raw):** 32 nodes (incl. INIT), 108 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 32 nodes, 235 edges (127 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 9](Elevator-product9-repaired.png)

![Pair graph (raw) — product 9](Elevator-product9-pairgraph-raw.png)

![Pair graph (balanced) — product 9](Elevator-product9-pairgraph-balanced.png)

**Generated test suite** — 59 test case(s) total (144 real step(s); pair-graph cycle has 235 edge(s) total, 127 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press intercom`
- **test case 3**: `press hall down -> press cabin lobby -> press door open -> press door close -> press door open`
- **test case 4**: `press hall down -> press cabin roof -> press door open`
- **test case 5**: `press hall down`
- **test case 6**: `enter PIN -> press cabin lobby -> press door close -> press door open`
- **test case 7**: `press hall up -> press cabin roof -> press door close`
- **test case 8**: `press cabin lobby -> press intercom`
- **test case 9**: `press hall RoofDown`
- **test case 10**: `press hall RoofDown -> press cabin lobby`
- **test case 11**: `enter PIN -> press cabin executive floor -> press intercom`
- **test case 12**: `press cabin lobby -> press door open`
- **test case 13**: `press hall RoofDown`
- **test case 14**: `press cabin lobby -> press door close`
- **test case 15**: `enter PIN -> press cabin lobby -> press door open`
- **test case 16**: `press cabin lobby -> press intercom`
- **test case 17**: `press hall up -> enter PIN`
- **test case 18**: `press hall LobbyUp`
- **test case 19**: `enter PIN -> press cabin [1-N] floor -> press intercom`
- **test case 20**: `press hall up -> press cabin lobby`
- **test case 21**: `press hall RoofDown`
- **test case 22**: `enter PIN -> press cabin roof -> press door close`
- **test case 23**: `press cabin roof -> press intercom`
- **test case 24**: `press cabin lobby -> press intercom`
- **test case 25**: `press hall up`
- **test case 26**: `press cabin lobby -> press door close`
- **test case 27**: `press hall up`
- **test case 28**: `press cabin [1-N] floor -> press door open`
- **test case 29**: `press hall RoofDown -> enter PIN -> press cabin executive floor`
- **test case 30**: `press hall down -> enter PIN -> press cabin [1-N] floor -> press door close`
- **test case 31**: `press cabin executive floor -> press door open`
- **test case 32**: `press door open -> press door close`
- **test case 33**: `press cabin [1-N] floor -> press door close`
- **test case 34**: `press hall RoofDown`
- **test case 35**: `press cabin [1-N] floor -> press door open`
- **test case 36**: `press hall LobbyUp`
- **test case 37**: `press cabin [1-N] floor -> press door open`
- **test case 38**: `press hall LobbyUp -> press cabin [1-N] floor -> press door close`
- **test case 39**: `press cabin [1-N] floor -> press intercom`
- **test case 40**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 41**: `press hall LobbyUp -> press cabin roof`
- **test case 42**: `press hall up -> press cabin [1-N] floor`
- **test case 43**: `press hall LobbyUp`
- **test case 44**: `press cabin roof -> press door open`
- **test case 45**: `press cabin roof -> press intercom`
- **test case 46**: `press cabin roof -> press door close`
- **test case 47**: `press hall LobbyUp`
- **test case 48**: `press hall RoofDown`
- **test case 49**: `press cabin [1-N] floor -> press intercom`
- **test case 50**: `press hall LobbyUp -> enter PIN -> press cabin [1-N] floor -> press door open`
- **test case 51**: `enter PIN -> press cabin roof`
- **test case 52**: `press hall down`
- **test case 53**: `enter PIN -> press cabin roof -> press door open`
- **test case 54**: `press door close -> press door open -> press door close`
- **test case 55**: `enter PIN -> press cabin executive floor -> press door close`
- **test case 56**: `press hall up`
- **test case 57**: `press cabin [1-N] floor -> press door close`
- **test case 58**: `press hall down -> press cabin [1-N] floor -> press intercom`
- **test case 59**: `enter PIN -> press cabin lobby`


### Product 10

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom}

**Repaired FTS:** 10 states, 24 transitions (20 real / 4 `__end__`).

**Pair graph (raw):** 25 nodes (incl. INIT), 71 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 25 nodes, 162 edges (91 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 10](Elevator-product10-repaired.png)

![Pair graph (raw) — product 10](Elevator-product10-pairgraph-raw.png)

![Pair graph (balanced) — product 10](Elevator-product10-pairgraph-balanced.png)

**Generated test suite** — 40 test case(s) total (97 real step(s); pair-graph cycle has 162 edge(s) total, 91 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press cabin lobby -> press&hold door close`
- **test case 3**: `press hall LobbyUp -> press cabin roof`
- **test case 4**: `press hall down -> press cabin lobby -> press&hold door close`
- **test case 5**: `press cabin roof -> press&hold door close`
- **test case 6**: `press cabin roof -> press&hold door open`
- **test case 7**: `release door open -> press&hold door open -> release door open`
- **test case 8**: `press hall up -> press cabin lobby`
- **test case 9**: `press hall down`
- **test case 10**: `press cabin roof -> press&hold door open`
- **test case 11**: `press cabin roof -> press intercom -> press&hold door open -> release door open`
- **test case 12**: `press cabin [1-N] floor -> press&hold door open`
- **test case 13**: `press hall LobbyUp`
- **test case 14**: `press hall RoofDown -> press cabin lobby -> press&hold door open`
- **test case 15**: `press cabin lobby -> press intercom -> press&hold door close -> release door close`
- **test case 16**: `press hall RoofDown`
- **test case 17**: `press cabin [1-N] floor -> press&hold door open -> release door open`
- **test case 18**: `press hall RoofDown`
- **test case 19**: `press hall down`
- **test case 20**: `press cabin roof -> press intercom`
- **test case 21**: `press hall RoofDown`
- **test case 22**: `press cabin [1-N] floor -> press&hold door close -> release door close`
- **test case 23**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 24**: `press hall LobbyUp -> press cabin [1-N] floor -> press intercom`
- **test case 25**: `press cabin [1-N] floor -> press intercom`
- **test case 26**: `press hall up`
- **test case 27**: `press hall down -> press cabin roof`
- **test case 28**: `press hall up`
- **test case 29**: `press cabin lobby -> press intercom`
- **test case 30**: `press hall down -> press cabin [1-N] floor -> press&hold door close`
- **test case 31**: `press hall up`
- **test case 32**: `press cabin lobby -> press&hold door open`
- **test case 33**: `press hall up -> press cabin roof -> press&hold door close`
- **test case 34**: `press hall up -> press cabin [1-N] floor`
- **test case 35**: `press cabin [1-N] floor -> press intercom`
- **test case 36**: `press hall LobbyUp`
- **test case 37**: `press cabin [1-N] floor -> press&hold door close`
- **test case 38**: `release door close -> press&hold door close -> release door close`
- **test case 39**: `press cabin [1-N] floor -> press&hold door open`
- **test case 40**: `press hall LobbyUp`


### Product 11

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Repaired FTS:** 12 states, 31 transitions (25 real / 6 `__end__`).

**Pair graph (raw):** 32 nodes (incl. INIT), 111 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 32 nodes, 302 edges (191 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 11](Elevator-product11-repaired.png)

![Pair graph (raw) — product 11](Elevator-product11-pairgraph-raw.png)

![Pair graph (balanced) — product 11](Elevator-product11-pairgraph-balanced.png)

**Generated test suite** — 71 test case(s) total (163 real step(s); pair-graph cycle has 302 edge(s) total, 191 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press&hold door open`
- **test case 3**: `release door open -> press&hold door open -> release door open`
- **test case 4**: `press hall up`
- **test case 5**: `press cabin roof -> press intercom -> press&hold door open -> release door open`
- **test case 6**: `press hall down -> press cabin lobby -> press&hold door close`
- **test case 7**: `press hall LobbyUp`
- **test case 8**: `press hall down`
- **test case 9**: `press cabin roof -> press door open -> press door close -> press door open`
- **test case 10**: `press hall down -> press cabin roof`
- **test case 11**: `press cabin lobby -> press&hold door close`
- **test case 12**: `press cabin [1-N] floor -> press&hold door close`
- **test case 13**: `press cabin [1-N] floor -> press door open`
- **test case 14**: `press hall up`
- **test case 15**: `press cabin roof -> press&hold door close -> release door close`
- **test case 16**: `press hall RoofDown`
- **test case 17**: `press hall LobbyUp -> press cabin [1-N] floor -> press door close -> press door open -> press door close`
- **test case 18**: `press cabin lobby -> press&hold door open`
- **test case 19**: `press hall LobbyUp`
- **test case 20**: `press cabin [1-N] floor -> press&hold door open`
- **test case 21**: `press cabin [1-N] floor -> press intercom -> press&hold door close -> release door close`
- **test case 22**: `press cabin [1-N] floor -> press alarm button -> press&hold door open`
- **test case 23**: `press hall LobbyUp -> press cabin roof`
- **test case 24**: `press hall RoofDown -> press cabin lobby -> press intercom`
- **test case 25**: `press hall down -> press cabin [1-N] floor -> press door open`
- **test case 26**: `press hall RoofDown`
- **test case 27**: `press hall RoofDown`
- **test case 28**: `press hall up -> press cabin roof -> press door close`
- **test case 29**: `press cabin lobby -> press door open`
- **test case 30**: `press cabin lobby -> press alarm button`
- **test case 31**: `press hall RoofDown`
- **test case 32**: `press hall LobbyUp`
- **test case 33**: `press cabin roof -> press alarm button -> press&hold door close`
- **test case 34**: `press hall down`
- **test case 35**: `press cabin [1-N] floor -> press alarm button`
- **test case 36**: `press hall up -> press cabin lobby -> press door open`
- **test case 37**: `press hall up`
- **test case 38**: `press cabin lobby -> press alarm button`
- **test case 39**: `press hall LobbyUp`
- **test case 40**: `press cabin lobby -> press intercom`
- **test case 41**: `press cabin roof -> press alarm button`
- **test case 42**: `press hall RoofDown`
- **test case 43**: `press cabin lobby -> press door close`
- **test case 44**: `press cabin lobby -> press&hold door open`
- **test case 45**: `press cabin lobby -> press door close`
- **test case 46**: `press cabin [1-N] floor -> press&hold door open`
- **test case 47**: `press cabin [1-N] floor -> press door close`
- **test case 48**: `press cabin [1-N] floor -> press door open`
- **test case 49**: `press cabin [1-N] floor -> press&hold door close`
- **test case 50**: `press cabin [1-N] floor -> press&hold door close`
- **test case 51**: `press hall up -> press cabin [1-N] floor`
- **test case 52**: `press hall up`
- **test case 53**: `press hall RoofDown`
- **test case 54**: `press cabin [1-N] floor -> press alarm button`
- **test case 55**: `press hall down`
- **test case 56**: `press hall LobbyUp`
- **test case 57**: `press cabin roof -> press door open`
- **test case 58**: `press hall LobbyUp`
- **test case 59**: `press cabin roof -> press intercom`
- **test case 60**: `press cabin roof -> press&hold door close`
- **test case 61**: `release door close -> press&hold door close -> release door close`
- **test case 62**: `press cabin [1-N] floor -> press door close`
- **test case 63**: `press hall up`
- **test case 64**: `press cabin [1-N] floor -> press intercom`
- **test case 65**: `press hall LobbyUp`
- **test case 66**: `press cabin roof -> press&hold door open -> release door open`
- **test case 67**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 68**: `press cabin [1-N] floor -> press&hold door open`
- **test case 69**: `press cabin [1-N] floor -> press intercom`
- **test case 70**: `press cabin roof -> press door close`
- **test case 71**: `press hall down`


### Product 12

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, PinPad}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Pair graph (raw):** 24 nodes (incl. INIT), 66 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 24 nodes, 127 edges (61 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 12](Elevator-product12-repaired.png)

![Pair graph (raw) — product 12](Elevator-product12-pairgraph-raw.png)

![Pair graph (balanced) — product 12](Elevator-product12-pairgraph-balanced.png)

**Generated test suite** — 33 test case(s) total (82 real step(s); pair-graph cycle has 127 edge(s) total, 61 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press cabin lobby -> press alarm button`
- **test case 3**: `press hall RoofDown -> press cabin lobby`
- **test case 4**: `press hall down -> press cabin lobby -> press alarm button`
- **test case 5**: `enter PIN -> press cabin lobby`
- **test case 6**: `enter PIN -> press cabin [1-N] floor -> press alarm button`
- **test case 7**: `press hall RoofDown`
- **test case 8**: `enter PIN -> press cabin executive floor`
- **test case 9**: `press hall LobbyUp -> press cabin roof`
- **test case 10**: `press hall LobbyUp`
- **test case 11**: `press hall up -> press cabin lobby`
- **test case 12**: `press hall RoofDown -> enter PIN -> press cabin roof`
- **test case 13**: `press hall up -> press cabin roof`
- **test case 14**: `press hall down -> press cabin roof -> press alarm button`
- **test case 15**: `press cabin roof -> press alarm button`
- **test case 16**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 17**: `press cabin [1-N] floor -> press alarm button`
- **test case 18**: `press hall LobbyUp`
- **test case 19**: `press cabin [1-N] floor -> press alarm button`
- **test case 20**: `press hall down -> enter PIN -> press cabin [1-N] floor`
- **test case 21**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 22**: `enter PIN -> press cabin [1-N] floor`
- **test case 23**: `press hall LobbyUp`
- **test case 24**: `enter PIN -> press cabin executive floor`
- **test case 25**: `press hall down -> press cabin [1-N] floor -> press alarm button`
- **test case 26**: `press hall up -> enter PIN -> press cabin roof`
- **test case 27**: `press hall up`
- **test case 28**: `enter PIN -> press cabin lobby`
- **test case 29**: `press hall up -> press cabin [1-N] floor`
- **test case 30**: `enter PIN -> press cabin executive floor -> press alarm button`
- **test case 31**: `press hall LobbyUp -> enter PIN -> press cabin roof -> press alarm button`
- **test case 32**: `press hall LobbyUp`
- **test case 33**: `enter PIN -> press cabin lobby -> press alarm button`


### Product 13

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, ManualDoorControl}

**Repaired FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

**Pair graph (raw):** 31 nodes (incl. INIT), 101 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 31 nodes, 270 edges (169 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 13](Elevator-product13-repaired.png)

![Pair graph (raw) — product 13](Elevator-product13-pairgraph-raw.png)

![Pair graph (balanced) — product 13](Elevator-product13-pairgraph-balanced.png)

**Generated test suite** — 66 test case(s) total (149 real step(s); pair-graph cycle has 270 edge(s) total, 169 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press&hold door open`
- **test case 3**: `release door open -> press&hold door open -> release door open`
- **test case 4**: `press hall up`
- **test case 5**: `press cabin roof -> press door open -> press door close -> press door open`
- **test case 6**: `press hall down -> press cabin lobby -> press&hold door close`
- **test case 7**: `press hall down -> press cabin roof`
- **test case 8**: `press cabin lobby -> press&hold door close`
- **test case 9**: `press hall LobbyUp`
- **test case 10**: `press cabin [1-N] floor -> press&hold door close`
- **test case 11**: `press hall LobbyUp -> press cabin [1-N] floor -> press door open`
- **test case 12**: `press hall up`
- **test case 13**: `press cabin roof -> press&hold door close -> release door close`
- **test case 14**: `press cabin [1-N] floor -> press door close -> press door open -> press door close`
- **test case 15**: `press cabin lobby -> press&hold door open`
- **test case 16**: `press hall down -> press cabin [1-N] floor -> press door open`
- **test case 17**: `press hall RoofDown -> press cabin lobby`
- **test case 18**: `press cabin [1-N] floor -> press&hold door open`
- **test case 19**: `press hall LobbyUp`
- **test case 20**: `press cabin [1-N] floor -> press alarm button`
- **test case 21**: `press&hold door open -> release door open`
- **test case 22**: `press hall LobbyUp -> press cabin roof`
- **test case 23**: `press cabin lobby -> press door open`
- **test case 24**: `press cabin lobby -> press alarm button -> press&hold door open`
- **test case 25**: `press alarm button -> press&hold door close -> release door close`
- **test case 26**: `press hall RoofDown`
- **test case 27**: `press hall down`
- **test case 28**: `press cabin [1-N] floor -> press alarm button`
- **test case 29**: `press hall RoofDown`
- **test case 30**: `press hall RoofDown`
- **test case 31**: `press hall LobbyUp`
- **test case 32**: `press hall up -> press cabin roof -> press door close`
- **test case 33**: `press hall up`
- **test case 34**: `press cabin roof -> press alarm button`
- **test case 35**: `press hall up -> press cabin lobby -> press door open`
- **test case 36**: `press cabin lobby -> press alarm button`
- **test case 37**: `press hall LobbyUp`
- **test case 38**: `press cabin roof -> press alarm button`
- **test case 39**: `press hall LobbyUp`
- **test case 40**: `press cabin roof -> press door open`
- **test case 41**: `press hall LobbyUp`
- **test case 42**: `press cabin roof -> press&hold door close`
- **test case 43**: `press hall down`
- **test case 44**: `press cabin lobby -> press door close`
- **test case 45**: `press cabin lobby -> press&hold door open`
- **test case 46**: `press cabin [1-N] floor -> press&hold door close`
- **test case 47**: `press hall up -> press cabin [1-N] floor`
- **test case 48**: `press cabin lobby -> press door close`
- **test case 49**: `press cabin [1-N] floor -> press&hold door open`
- **test case 50**: `press cabin [1-N] floor -> press door close`
- **test case 51**: `press hall RoofDown`
- **test case 52**: `press cabin [1-N] floor -> press door open`
- **test case 53**: `press cabin roof -> press&hold door open -> release door open`
- **test case 54**: `press hall RoofDown`
- **test case 55**: `press cabin [1-N] floor -> press&hold door close`
- **test case 56**: `release door close -> press&hold door close -> release door close`
- **test case 57**: `press hall up`
- **test case 58**: `press cabin [1-N] floor -> press door close`
- **test case 59**: `press hall LobbyUp`
- **test case 60**: `press cabin roof -> press door close`
- **test case 61**: `press hall up`
- **test case 62**: `press hall down`
- **test case 63**: `press hall RoofDown`
- **test case 64**: `press cabin [1-N] floor -> press&hold door open`
- **test case 65**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **test case 66**: `press hall down`


### Product 14

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Pair graph (raw):** 26 nodes (incl. INIT), 79 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 26 nodes, 151 edges (72 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 14](Elevator-product14-repaired.png)

![Pair graph (raw) — product 14](Elevator-product14-pairgraph-raw.png)

![Pair graph (balanced) — product 14](Elevator-product14-pairgraph-balanced.png)

**Generated test suite** — 41 test case(s) total (101 real step(s); pair-graph cycle has 151 edge(s) total, 72 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `read card -> press cabin [1-N] floor -> press intercom`
- **test case 3**: `press hall RoofDown -> read card`
- **test case 4**: `press hall LobbyUp`
- **test case 5**: `read card -> press cabin executive floor`
- **test case 6**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 7**: `press hall RoofDown`
- **test case 8**: `press hall down -> press cabin lobby`
- **test case 9**: `press hall down -> press cabin roof -> press intercom`
- **test case 10**: `read card -> press cabin executive floor -> press alarm button`
- **test case 11**: `read card -> press cabin lobby -> press intercom`
- **test case 12**: `read card -> press cabin roof`
- **test case 13**: `press cabin lobby -> press alarm button`
- **test case 14**: `press hall RoofDown -> press cabin lobby`
- **test case 15**: `press cabin lobby -> press intercom`
- **test case 16**: `press cabin lobby -> press intercom`
- **test case 17**: `press hall up -> press cabin roof`
- **test case 18**: `press hall RoofDown`
- **test case 19**: `press cabin [1-N] floor -> press alarm button`
- **test case 20**: `press hall up`
- **test case 21**: `press cabin roof -> press alarm button`
- **test case 22**: `press hall up -> press cabin [1-N] floor -> press alarm button`
- **test case 23**: `press hall RoofDown -> press cabin [1-N] floor -> press intercom`
- **test case 24**: `press hall down -> read card -> press cabin executive floor -> press intercom`
- **test case 25**: `read card -> press cabin [1-N] floor -> press alarm button`
- **test case 26**: `read card -> press cabin lobby`
- **test case 27**: `press hall LobbyUp -> read card -> press cabin roof -> press alarm button`
- **test case 28**: `press hall LobbyUp -> press cabin roof`
- **test case 29**: `press hall LobbyUp`
- **test case 30**: `press cabin roof -> press alarm button`
- **test case 31**: `press cabin roof -> press intercom`
- **test case 32**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 33**: `press hall up`
- **test case 34**: `read card -> press cabin [1-N] floor`
- **test case 35**: `press hall up -> read card -> press cabin roof -> press intercom`
- **test case 36**: `press cabin [1-N] floor -> press intercom`
- **test case 37**: `press hall down -> press cabin [1-N] floor -> press intercom`
- **test case 38**: `press hall up`
- **test case 39**: `read card -> press cabin lobby -> press alarm button`
- **test case 40**: `press hall LobbyUp`
- **test case 41**: `press cabin [1-N] floor -> press alarm button`


### Product 15

**Selected features:** selected = {ControlButtons, FirefighterService, Intercom, ManualDoorControl}

**Repaired FTS:** 12 states, 30 transitions (24 real / 6 `__end__`).

**Pair graph (raw):** 31 nodes (incl. INIT), 101 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 31 nodes, 274 edges (173 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 15](Elevator-product15-repaired.png)

![Pair graph (raw) — product 15](Elevator-product15-pairgraph-raw.png)

![Pair graph (balanced) — product 15](Elevator-product15-pairgraph-balanced.png)

**Generated test suite** — 63 test case(s) total (146 real step(s); pair-graph cycle has 274 edge(s) total, 173 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press&hold door open`
- **test case 3**: `release door open -> press&hold door open -> release door open`
- **test case 4**: `press hall up`
- **test case 5**: `press cabin roof -> press intercom -> press&hold door open -> release door open`
- **test case 6**: `press hall down -> press cabin lobby -> press&hold door close`
- **test case 7**: `press hall down -> press cabin roof -> press door open -> press door close -> press door open`
- **test case 8**: `press hall down -> press cabin [1-N] floor -> press door open`
- **test case 9**: `press hall up`
- **test case 10**: `press hall RoofDown -> press cabin lobby -> press&hold door close`
- **test case 11**: `press hall LobbyUp`
- **test case 12**: `press cabin [1-N] floor -> press&hold door close`
- **test case 13**: `press hall LobbyUp -> press cabin [1-N] floor -> press door open`
- **test case 14**: `press hall RoofDown`
- **test case 15**: `press cabin [1-N] floor -> press door close -> press door open -> press door close`
- **test case 16**: `press cabin lobby -> press&hold door open`
- **test case 17**: `press hall LobbyUp`
- **test case 18**: `press cabin [1-N] floor -> press&hold door open`
- **test case 19**: `press cabin [1-N] floor -> press intercom -> press&hold door close -> release door close`
- **test case 20**: `press hall RoofDown`
- **test case 21**: `press cabin lobby -> press intercom`
- **test case 22**: `press hall down`
- **test case 23**: `press hall RoofDown`
- **test case 24**: `press cabin lobby -> press door open`
- **test case 25**: `press hall LobbyUp -> press cabin roof`
- **test case 26**: `press hall RoofDown`
- **test case 27**: `press hall LobbyUp`
- **test case 28**: `press hall LobbyUp`
- **test case 29**: `press hall up -> press cabin roof -> press&hold door close -> release door close`
- **test case 30**: `press hall down`
- **test case 31**: `press cabin roof -> press door close`
- **test case 32**: `press cabin lobby -> press door close`
- **test case 33**: `press hall RoofDown`
- **test case 34**: `press cabin [1-N] floor -> press&hold door open`
- **test case 35**: `press hall up -> press cabin lobby -> press door open`
- **test case 36**: `press cabin lobby -> press intercom`
- **test case 37**: `press cabin lobby -> press door close`
- **test case 38**: `press cabin [1-N] floor -> press door close`
- **test case 39**: `press hall LobbyUp`
- **test case 40**: `press cabin lobby -> press&hold door open`
- **test case 41**: `press cabin [1-N] floor -> press door open`
- **test case 42**: `press cabin roof -> press door open`
- **test case 43**: `press cabin roof -> press intercom`
- **test case 44**: `press hall up`
- **test case 45**: `press cabin [1-N] floor -> press&hold door close`
- **test case 46**: `press hall up -> press cabin [1-N] floor`
- **test case 47**: `press cabin [1-N] floor -> press door close`
- **test case 48**: `press hall up`
- **test case 49**: `press cabin [1-N] floor -> press intercom`
- **test case 50**: `press cabin roof -> press&hold door close`
- **test case 51**: `press hall up`
- **test case 52**: `press cabin [1-N] floor -> press&hold door open`
- **test case 53**: `press cabin [1-N] floor -> press&hold door close`
- **test case 54**: `release door close -> press&hold door close -> release door close`
- **test case 55**: `press&hold door open -> release door open`
- **test case 56**: `press hall RoofDown -> press cabin [1-N] floor -> press intercom`
- **test case 57**: `press hall LobbyUp`
- **test case 58**: `press cabin roof -> press&hold door open`
- **test case 59**: `press hall LobbyUp`
- **test case 60**: `press cabin roof -> press door close`
- **test case 61**: `press hall down`
- **test case 62**: `press hall RoofDown`
- **test case 63**: `press hall down`


### Product 16

**Selected features:** selected = {Alarm, CardReader, ControlButtons}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Pair graph (raw):** 21 nodes (incl. INIT), 56 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 21 nodes, 100 edges (44 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 16](Elevator-product16-repaired.png)

![Pair graph (raw) — product 16](Elevator-product16-pairgraph-raw.png)

![Pair graph (balanced) — product 16](Elevator-product16-pairgraph-balanced.png)

**Generated test suite** — 24 test case(s) total (65 real step(s); pair-graph cycle has 100 edge(s) total, 44 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown -> read card -> press cabin [1-N] floor -> press alarm button`
- **test case 2**: `read card -> press cabin lobby`
- **test case 3**: `press hall down -> press cabin lobby -> press alarm button`
- **test case 4**: `press hall RoofDown`
- **test case 5**: `read card -> press cabin roof`
- **test case 6**: `press hall RoofDown -> press cabin lobby -> press alarm button`
- **test case 7**: `press hall up -> press cabin lobby`
- **test case 8**: `press hall up -> press cabin roof`
- **test case 9**: `press hall down -> press cabin roof -> press alarm button`
- **test case 10**: `press hall down -> read card -> press cabin [1-N] floor`
- **test case 11**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **test case 12**: `press hall LobbyUp`
- **test case 13**: `read card -> press cabin [1-N] floor`
- **test case 14**: `press hall RoofDown`
- **test case 15**: `read card -> press cabin lobby`
- **test case 16**: `press hall LobbyUp -> read card -> press cabin roof`
- **test case 17**: `press hall LobbyUp -> press cabin roof`
- **test case 18**: `press cabin roof -> press alarm button`
- **test case 19**: `press hall up -> press cabin [1-N] floor -> press alarm button`
- **test case 20**: `press hall down -> press cabin [1-N] floor`
- **test case 21**: `press hall up -> read card -> press cabin roof -> press alarm button`
- **test case 22**: `press hall up`
- **test case 23**: `read card -> press cabin lobby -> press alarm button`
- **test case 24**: `press hall LobbyUp -> press cabin [1-N] floor -> press alarm button`


### Product 17

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Pair graph (raw):** 22 nodes (incl. INIT), 67 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 22 nodes, 127 edges (60 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 17](Elevator-product17-repaired.png)

![Pair graph (raw) — product 17](Elevator-product17-pairgraph-raw.png)

![Pair graph (balanced) — product 17](Elevator-product17-pairgraph-balanced.png)

**Generated test suite** — 36 test case(s) total (87 real step(s); pair-graph cycle has 127 edge(s) total, 60 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `read card -> press cabin [1-N] floor -> press intercom`
- **test case 3**: `read card -> press cabin lobby -> press intercom`
- **test case 4**: `press hall RoofDown -> read card -> press cabin roof`
- **test case 5**: `press hall down -> press cabin lobby -> press alarm button`
- **test case 6**: `press cabin lobby -> press alarm button`
- **test case 7**: `press hall RoofDown -> press cabin lobby -> press intercom`
- **test case 8**: `press cabin [1-N] floor -> press alarm button`
- **test case 9**: `press hall up -> press cabin lobby`
- **test case 10**: `press hall RoofDown`
- **test case 11**: `press cabin [1-N] floor -> press intercom`
- **test case 12**: `press cabin lobby -> press intercom`
- **test case 13**: `press hall up`
- **test case 14**: `press cabin roof -> press intercom`
- **test case 15**: `press hall up -> press cabin roof`
- **test case 16**: `press hall up -> press cabin [1-N] floor -> press alarm button`
- **test case 17**: `press hall down -> press cabin roof -> press alarm button`
- **test case 18**: `press hall down -> read card -> press cabin [1-N] floor -> press alarm button`
- **test case 19**: `read card -> press cabin [1-N] floor`
- **test case 20**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 21**: `read card -> press cabin lobby`
- **test case 22**: `press hall LobbyUp -> read card -> press cabin roof -> press alarm button`
- **test case 23**: `press hall LobbyUp -> press cabin roof`
- **test case 24**: `press hall LobbyUp`
- **test case 25**: `press cabin roof -> press alarm button`
- **test case 26**: `press hall down -> press cabin [1-N] floor`
- **test case 27**: `press hall up`
- **test case 28**: `press cabin [1-N] floor -> press intercom`
- **test case 29**: `press hall LobbyUp`
- **test case 30**: `press cabin roof -> press intercom`
- **test case 31**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 32**: `read card -> press cabin roof -> press intercom`
- **test case 33**: `press cabin [1-N] floor -> press intercom`
- **test case 34**: `press hall up -> read card -> press cabin lobby -> press alarm button`
- **test case 35**: `press hall LobbyUp`
- **test case 36**: `press cabin [1-N] floor -> press alarm button`


### Product 18

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, MobileKey}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Pair graph (raw):** 24 nodes (incl. INIT), 66 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 24 nodes, 124 edges (58 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 18](Elevator-product18-repaired.png)

![Pair graph (raw) — product 18](Elevator-product18-pairgraph-raw.png)

![Pair graph (balanced) — product 18](Elevator-product18-pairgraph-balanced.png)

**Generated test suite** — 34 test case(s) total (83 real step(s); pair-graph cycle has 124 edge(s) total, 58 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown -> press cabin lobby -> press alarm button`
- **test case 2**: `press hall down -> press cabin lobby -> press alarm button`
- **test case 3**: `press hall RoofDown`
- **test case 4**: `press cabin [1-N] floor -> press alarm button`
- **test case 5**: `press hall up -> press cabin lobby`
- **test case 6**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 7**: `tap mobile key -> press cabin roof`
- **test case 8**: `tap mobile key -> press cabin lobby`
- **test case 9**: `press hall RoofDown`
- **test case 10**: `tap mobile key -> press cabin [1-N] floor -> press alarm button`
- **test case 11**: `press hall up -> press cabin roof`
- **test case 12**: `press hall RoofDown`
- **test case 13**: `press hall LobbyUp -> press cabin roof`
- **test case 14**: `press hall RoofDown -> tap mobile key -> press cabin executive floor`
- **test case 15**: `press hall LobbyUp`
- **test case 16**: `press cabin roof -> press alarm button`
- **test case 17**: `press hall down -> press cabin roof -> press alarm button`
- **test case 18**: `press hall LobbyUp`
- **test case 19**: `tap mobile key -> press cabin [1-N] floor`
- **test case 20**: `press hall up -> press cabin [1-N] floor -> press alarm button`
- **test case 21**: `press hall down -> press cabin [1-N] floor`
- **test case 22**: `press hall LobbyUp -> tap mobile key -> press cabin roof`
- **test case 23**: `press hall LobbyUp`
- **test case 24**: `tap mobile key -> press cabin lobby`
- **test case 25**: `tap mobile key -> press cabin executive floor`
- **test case 26**: `press hall down -> tap mobile key -> press cabin [1-N] floor`
- **test case 27**: `press hall up`
- **test case 28**: `tap mobile key -> press cabin roof -> press alarm button`
- **test case 29**: `press hall up`
- **test case 30**: `tap mobile key -> press cabin executive floor -> press alarm button`
- **test case 31**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 32**: `press hall up -> tap mobile key -> press cabin lobby -> press alarm button`
- **test case 33**: `press hall LobbyUp`
- **test case 34**: `press cabin [1-N] floor -> press alarm button`


### Product 19

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Pair graph (raw):** 27 nodes (incl. INIT), 92 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 27 nodes, 210 edges (118 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 19](Elevator-product19-repaired.png)

![Pair graph (raw) — product 19](Elevator-product19-pairgraph-raw.png)

![Pair graph (balanced) — product 19](Elevator-product19-pairgraph-balanced.png)

**Generated test suite** — 53 test case(s) total (126 real step(s); pair-graph cycle has 210 edge(s) total, 118 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press door open -> press door close -> press door open`
- **test case 3**: `press hall RoofDown`
- **test case 4**: `press cabin lobby -> press door open`
- **test case 5**: `press hall RoofDown -> press cabin lobby`
- **test case 6**: `press cabin lobby -> press alarm button`
- **test case 7**: `press hall RoofDown`
- **test case 8**: `press hall down -> press cabin lobby -> press door open`
- **test case 9**: `press hall RoofDown`
- **test case 10**: `press door close -> press door open -> press door close`
- **test case 11**: `press cabin lobby -> press door close`
- **test case 12**: `press cabin [1-N] floor -> press door close`
- **test case 13**: `press cabin [1-N] floor -> press alarm button`
- **test case 14**: `press cabin [1-N] floor -> press door open`
- **test case 15**: `press hall down -> press cabin roof`
- **test case 16**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 17**: `tap mobile key -> press cabin roof`
- **test case 18**: `press hall LobbyUp -> press cabin roof`
- **test case 19**: `press cabin roof -> press alarm button`
- **test case 20**: `press cabin roof -> press alarm button`
- **test case 21**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 22**: `press hall up`
- **test case 23**: `press hall LobbyUp`
- **test case 24**: `press cabin roof -> press door open`
- **test case 25**: `press cabin lobby -> press door close`
- **test case 26**: `press hall RoofDown -> tap mobile key -> press cabin lobby -> press door open`
- **test case 27**: `press hall up`
- **test case 28**: `press cabin roof -> press door open`
- **test case 29**: `press hall up -> press cabin roof -> press door close`
- **test case 30**: `press cabin roof -> press door close`
- **test case 31**: `press cabin [1-N] floor -> press door close`
- **test case 32**: `press hall RoofDown`
- **test case 33**: `tap mobile key -> press cabin [1-N] floor -> press alarm button`
- **test case 34**: `press cabin roof -> press alarm button`
- **test case 35**: `press hall up -> press cabin [1-N] floor -> press door open`
- **test case 36**: `press hall LobbyUp -> tap mobile key -> press cabin [1-N] floor -> press door open`
- **test case 37**: `tap mobile key -> press cabin roof -> press door close`
- **test case 38**: `press hall LobbyUp`
- **test case 39**: `tap mobile key -> press cabin lobby -> press door close`
- **test case 40**: `press hall down -> press cabin [1-N] floor -> press alarm button`
- **test case 41**: `press hall up`
- **test case 42**: `press hall down`
- **test case 43**: `press hall up`
- **test case 44**: `press cabin [1-N] floor -> press door close`
- **test case 45**: `press hall up -> tap mobile key -> press cabin [1-N] floor`
- **test case 46**: `press hall LobbyUp`
- **test case 47**: `press cabin [1-N] floor -> press door close`
- **test case 48**: `tap mobile key -> press cabin roof -> press door open`
- **test case 49**: `press cabin [1-N] floor -> press alarm button`
- **test case 50**: `press hall down -> tap mobile key -> press cabin lobby -> press alarm button`
- **test case 51**: `press hall LobbyUp`
- **test case 52**: `press cabin [1-N] floor -> press door open`
- **test case 53**: `press hall LobbyUp -> press cabin [1-N] floor`


### Product 20

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Pair graph (raw):** 32 nodes (incl. INIT), 108 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 32 nodes, 240 edges (132 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 20](Elevator-product20-repaired.png)

![Pair graph (raw) — product 20](Elevator-product20-pairgraph-raw.png)

![Pair graph (balanced) — product 20](Elevator-product20-pairgraph-balanced.png)

**Generated test suite** — 61 test case(s) total (146 real step(s); pair-graph cycle has 240 edge(s) total, 132 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press door open -> press door close -> press door open`
- **test case 3**: `press hall down -> press cabin lobby -> press door open`
- **test case 4**: `press hall up -> press cabin roof`
- **test case 5**: `press hall down -> tap mobile key -> press cabin [1-N] floor -> press door close -> press door open`
- **test case 6**: `press hall up`
- **test case 7**: `tap mobile key -> press cabin lobby -> press door close`
- **test case 8**: `press hall up -> tap mobile key -> press cabin roof -> press door close`
- **test case 9**: `press cabin lobby -> press door open`
- **test case 10**: `press hall RoofDown`
- **test case 11**: `press hall down -> press cabin roof -> press door close`
- **test case 12**: `press cabin lobby -> press alarm button`
- **test case 13**: `press hall RoofDown -> press cabin lobby`
- **test case 14**: `press cabin lobby -> press door close`
- **test case 15**: `press cabin [1-N] floor -> press door close`
- **test case 16**: `press cabin [1-N] floor -> press door open`
- **test case 17**: `press cabin [1-N] floor -> press alarm button`
- **test case 18**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 19**: `tap mobile key -> press cabin roof`
- **test case 20**: `press hall RoofDown`
- **test case 21**: `press cabin roof -> press alarm button`
- **test case 22**: `tap mobile key -> press cabin lobby -> press alarm button`
- **test case 23**: `press cabin lobby -> press door open`
- **test case 24**: `tap mobile key -> press cabin [1-N] floor`
- **test case 25**: `press cabin [1-N] floor -> press alarm button`
- **test case 26**: `press cabin executive floor -> press alarm button`
- **test case 27**: `press hall LobbyUp`
- **test case 28**: `tap mobile key -> press cabin [1-N] floor -> press door open`
- **test case 29**: `press hall up`
- **test case 30**: `tap mobile key -> press cabin executive floor`
- **test case 31**: `press hall RoofDown`
- **test case 32**: `press hall down`
- **test case 33**: `press cabin roof -> press alarm button`
- **test case 34**: `press cabin executive floor -> press door open`
- **test case 35**: `press hall LobbyUp -> tap mobile key -> press cabin lobby`
- **test case 36**: `tap mobile key -> press cabin executive floor`
- **test case 37**: `press door open -> press door close`
- **test case 38**: `press hall RoofDown -> tap mobile key -> press cabin executive floor -> press door close -> press door open -> press door close`
- **test case 39**: `tap mobile key -> press cabin roof -> press door open`
- **test case 40**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 41**: `press hall LobbyUp`
- **test case 42**: `press cabin [1-N] floor -> press door open`
- **test case 43**: `press cabin [1-N] floor -> press door close`
- **test case 44**: `press hall LobbyUp`
- **test case 45**: `press cabin [1-N] floor -> press alarm button`
- **test case 46**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 47**: `press cabin lobby -> press door close`
- **test case 48**: `press cabin [1-N] floor -> press door open`
- **test case 49**: `press hall LobbyUp -> press cabin roof`
- **test case 50**: `press hall up`
- **test case 51**: `press cabin [1-N] floor -> press alarm button`
- **test case 52**: `press hall LobbyUp`
- **test case 53**: `press cabin roof -> press alarm button`
- **test case 54**: `press hall LobbyUp`
- **test case 55**: `press cabin roof -> press door open`
- **test case 56**: `press cabin roof -> press door close`
- **test case 57**: `press hall up -> press cabin [1-N] floor`
- **test case 58**: `press hall RoofDown`
- **test case 59**: `press hall up`
- **test case 60**: `press hall down -> press cabin [1-N] floor -> press door close`
- **test case 61**: `press hall down`


### Product 21

**Selected features:** selected = {CardReader, ControlButtons, Intercom}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Pair graph (raw):** 21 nodes (incl. INIT), 56 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 21 nodes, 100 edges (44 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 21](Elevator-product21-repaired.png)

![Pair graph (raw) — product 21](Elevator-product21-pairgraph-raw.png)

![Pair graph (balanced) — product 21](Elevator-product21-pairgraph-balanced.png)

**Generated test suite** — 25 test case(s) total (66 real step(s); pair-graph cycle has 100 edge(s) total, 44 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown -> read card -> press cabin [1-N] floor -> press intercom`
- **test case 2**: `read card -> press cabin lobby -> press intercom`
- **test case 3**: `press hall RoofDown`
- **test case 4**: `read card -> press cabin roof`
- **test case 5**: `press hall down -> press cabin lobby`
- **test case 6**: `press hall RoofDown -> press cabin lobby`
- **test case 7**: `press cabin lobby -> press intercom`
- **test case 8**: `press cabin [1-N] floor -> press intercom`
- **test case 9**: `press hall up -> press cabin lobby -> press intercom`
- **test case 10**: `press hall up -> press cabin roof -> press intercom`
- **test case 11**: `press hall down -> press cabin roof`
- **test case 12**: `press hall down -> read card -> press cabin [1-N] floor`
- **test case 13**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 14**: `press hall LobbyUp`
- **test case 15**: `read card -> press cabin [1-N] floor`
- **test case 16**: `press hall RoofDown`
- **test case 17**: `read card -> press cabin lobby`
- **test case 18**: `press hall LobbyUp -> read card -> press cabin roof`
- **test case 19**: `press hall LobbyUp -> press cabin roof`
- **test case 20**: `press cabin roof -> press intercom`
- **test case 21**: `press hall down -> press cabin [1-N] floor`
- **test case 22**: `press hall up -> press cabin [1-N] floor -> press intercom`
- **test case 23**: `press hall up -> read card -> press cabin lobby`
- **test case 24**: `read card -> press cabin roof -> press intercom`
- **test case 25**: `press hall LobbyUp -> press cabin [1-N] floor -> press intercom`


### Product 22

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, PinPad}

**Repaired FTS:** 8 states, 25 transitions (22 real / 3 `__end__`).

**Pair graph (raw):** 26 nodes (incl. INIT), 79 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 26 nodes, 151 edges (72 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 22](Elevator-product22-repaired.png)

![Pair graph (raw) — product 22](Elevator-product22-pairgraph-raw.png)

![Pair graph (balanced) — product 22](Elevator-product22-pairgraph-balanced.png)

**Generated test suite** — 42 test case(s) total (102 real step(s); pair-graph cycle has 151 edge(s) total, 72 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press cabin lobby -> press alarm button`
- **test case 3**: `press hall RoofDown`
- **test case 4**: `press hall down -> press cabin lobby -> press alarm button`
- **test case 5**: `press hall RoofDown -> press cabin lobby`
- **test case 6**: `press cabin lobby -> press intercom`
- **test case 7**: `enter PIN -> press cabin lobby -> press intercom`
- **test case 8**: `enter PIN -> press cabin [1-N] floor -> press intercom`
- **test case 9**: `press hall RoofDown`
- **test case 10**: `enter PIN -> press cabin executive floor`
- **test case 11**: `press hall LobbyUp -> press cabin roof`
- **test case 12**: `press hall RoofDown -> enter PIN`
- **test case 13**: `press hall up -> press cabin lobby`
- **test case 14**: `press hall down -> press cabin roof -> press intercom`
- **test case 15**: `press cabin lobby -> press intercom`
- **test case 16**: `press hall up -> press cabin roof`
- **test case 17**: `enter PIN -> press cabin roof`
- **test case 18**: `press hall RoofDown`
- **test case 19**: `press cabin [1-N] floor -> press alarm button`
- **test case 20**: `press hall up`
- **test case 21**: `press cabin roof -> press alarm button`
- **test case 22**: `enter PIN -> press cabin [1-N] floor -> press alarm button`
- **test case 23**: `press hall up -> enter PIN -> press cabin roof -> press alarm button`
- **test case 24**: `press hall down -> enter PIN -> press cabin lobby`
- **test case 25**: `press hall RoofDown -> press cabin [1-N] floor -> press intercom`
- **test case 26**: `press hall LobbyUp`
- **test case 27**: `press cabin roof -> press alarm button`
- **test case 28**: `press cabin roof -> press intercom`
- **test case 29**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 30**: `press cabin [1-N] floor -> press intercom`
- **test case 31**: `press cabin [1-N] floor -> press alarm button`
- **test case 32**: `press hall LobbyUp -> enter PIN -> press cabin [1-N] floor`
- **test case 33**: `press hall up`
- **test case 34**: `enter PIN -> press cabin executive floor -> press alarm button`
- **test case 35**: `press hall LobbyUp`
- **test case 36**: `enter PIN -> press cabin executive floor -> press intercom`
- **test case 37**: `enter PIN -> press cabin roof -> press intercom`
- **test case 38**: `press hall down -> press cabin [1-N] floor -> press alarm button`
- **test case 39**: `press hall up -> press cabin [1-N] floor`
- **test case 40**: `press cabin [1-N] floor -> press intercom`
- **test case 41**: `press hall LobbyUp`
- **test case 42**: `enter PIN -> press cabin lobby -> press alarm button`


### Product 23

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Pair graph (raw):** 34 nodes (incl. INIT), 121 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 34 nodes, 299 edges (178 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 23](Elevator-product23-repaired.png)

![Pair graph (raw) — product 23](Elevator-product23-pairgraph-raw.png)

![Pair graph (balanced) — product 23](Elevator-product23-pairgraph-balanced.png)

**Generated test suite** — 74 test case(s) total (170 real step(s); pair-graph cycle has 299 edge(s) total, 178 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press intercom`
- **test case 3**: `press hall down -> press cabin lobby -> press door open -> press door close -> press door open`
- **test case 4**: `press hall down -> read card -> press cabin [1-N] floor -> press intercom`
- **test case 5**: `press cabin executive floor -> press alarm button`
- **test case 6**: `press hall RoofDown -> read card`
- **test case 7**: `press cabin executive floor -> press intercom`
- **test case 8**: `press hall LobbyUp`
- **test case 9**: `read card -> press cabin [1-N] floor -> press door close -> press door open`
- **test case 10**: `press cabin roof -> press door open`
- **test case 11**: `press hall up -> press cabin roof`
- **test case 12**: `press hall down -> press cabin roof -> press door close`
- **test case 13**: `press hall up`
- **test case 14**: `press cabin roof -> press alarm button`
- **test case 15**: `press hall RoofDown`
- **test case 16**: `read card -> press cabin executive floor`
- **test case 17**: `press hall down`
- **test case 18**: `press cabin [1-N] floor -> press door open`
- **test case 19**: `press hall up -> read card -> press cabin lobby -> press door close`
- **test case 20**: `press cabin executive floor -> press door open`
- **test case 21**: `read card -> press cabin executive floor`
- **test case 22**: `press door open -> press door close`
- **test case 23**: `press cabin executive floor -> press door close -> press door open -> press door close`
- **test case 24**: `read card -> press cabin roof -> press door close`
- **test case 25**: `press cabin roof -> press intercom`
- **test case 26**: `read card -> press cabin [1-N] floor`
- **test case 27**: `press cabin [1-N] floor -> press alarm button`
- **test case 28**: `press cabin [1-N] floor -> press door open`
- **test case 29**: `press hall RoofDown`
- **test case 30**: `read card -> press cabin lobby -> press alarm button`
- **test case 31**: `press cabin lobby -> press door open`
- **test case 32**: `press cabin lobby -> press intercom`
- **test case 33**: `press cabin lobby -> press intercom`
- **test case 34**: `press hall RoofDown`
- **test case 35**: `press cabin lobby -> press door open`
- **test case 36**: `press hall LobbyUp`
- **test case 37**: `read card -> press cabin roof -> press alarm button`
- **test case 38**: `press cabin lobby -> press alarm button`
- **test case 39**: `press hall RoofDown -> press cabin lobby`
- **test case 40**: `press hall RoofDown`
- **test case 41**: `press hall LobbyUp -> read card -> press cabin executive floor`
- **test case 42**: `press cabin lobby -> press door close`
- **test case 43**: `press hall RoofDown`
- **test case 44**: `press cabin [1-N] floor -> press door close`
- **test case 45**: `press hall RoofDown -> press cabin [1-N] floor -> press door open`
- **test case 46**: `read card -> press cabin lobby`
- **test case 47**: `press hall LobbyUp`
- **test case 48**: `press cabin [1-N] floor -> press door open`
- **test case 49**: `press hall LobbyUp -> press cabin [1-N] floor -> press door close`
- **test case 50**: `press hall LobbyUp`
- **test case 51**: `press cabin [1-N] floor -> press intercom`
- **test case 52**: `press cabin [1-N] floor -> press alarm button`
- **test case 53**: `press cabin [1-N] floor -> press alarm button`
- **test case 54**: `read card -> press cabin roof -> press door open`
- **test case 55**: `press hall LobbyUp -> press cabin roof`
- **test case 56**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 57**: `press cabin lobby -> press intercom`
- **test case 58**: `press hall up`
- **test case 59**: `press cabin lobby -> press door close`
- **test case 60**: `press cabin [1-N] floor -> press alarm button`
- **test case 61**: `press hall up`
- **test case 62**: `press hall up -> press cabin [1-N] floor`
- **test case 63**: `press hall LobbyUp`
- **test case 64**: `press hall RoofDown`
- **test case 65**: `press cabin [1-N] floor -> press intercom`
- **test case 66**: `press hall down -> press cabin [1-N] floor -> press door close`
- **test case 67**: `press hall up`
- **test case 68**: `press cabin [1-N] floor -> press intercom`
- **test case 69**: `press cabin roof -> press alarm button`
- **test case 70**: `press hall LobbyUp`
- **test case 71**: `press cabin roof -> press door open`
- **test case 72**: `press cabin roof -> press intercom`
- **test case 73**: `press cabin roof -> press door close`
- **test case 74**: `press hall down`


### Product 24

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Pair graph (raw):** 34 nodes (incl. INIT), 121 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 34 nodes, 288 edges (167 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 24](Elevator-product24-repaired.png)

![Pair graph (raw) — product 24](Elevator-product24-pairgraph-raw.png)

![Pair graph (balanced) — product 24](Elevator-product24-pairgraph-balanced.png)

**Generated test suite** — 73 test case(s) total (169 real step(s); pair-graph cycle has 288 edge(s) total, 167 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press intercom`
- **test case 3**: `press hall down -> press cabin lobby -> press door open -> press door close -> press door open`
- **test case 4**: `press hall down -> tap mobile key`
- **test case 5**: `press cabin [1-N] floor -> press intercom`
- **test case 6**: `press cabin lobby -> press intercom`
- **test case 7**: `press hall RoofDown -> press cabin lobby -> press door open`
- **test case 8**: `press cabin roof -> press door open`
- **test case 9**: `press hall up`
- **test case 10**: `press hall down`
- **test case 11**: `tap mobile key -> press cabin [1-N] floor -> press door close -> press door open`
- **test case 12**: `press hall up -> press cabin roof -> press door close`
- **test case 13**: `press cabin lobby -> press alarm button`
- **test case 14**: `press hall RoofDown`
- **test case 15**: `press cabin lobby -> press door close`
- **test case 16**: `press cabin [1-N] floor -> press door close`
- **test case 17**: `press hall RoofDown -> press cabin [1-N] floor -> press door open`
- **test case 18**: `tap mobile key -> press cabin lobby -> press door close`
- **test case 19**: `press cabin [1-N] floor -> press alarm button`
- **test case 20**: `press cabin [1-N] floor -> press intercom`
- **test case 21**: `press hall RoofDown`
- **test case 22**: `tap mobile key -> press cabin roof -> press door close`
- **test case 23**: `press hall RoofDown`
- **test case 24**: `press cabin roof -> press intercom`
- **test case 25**: `press cabin lobby -> press alarm button`
- **test case 26**: `press cabin lobby -> press door open`
- **test case 27**: `press hall RoofDown`
- **test case 28**: `tap mobile key -> press cabin lobby -> press intercom`
- **test case 29**: `tap mobile key -> press cabin [1-N] floor`
- **test case 30**: `press cabin [1-N] floor -> press alarm button`
- **test case 31**: `press cabin executive floor -> press alarm button`
- **test case 32**: `press cabin executive floor -> press intercom`
- **test case 33**: `press hall up`
- **test case 34**: `tap mobile key -> press cabin roof -> press alarm button`
- **test case 35**: `press hall LobbyUp -> tap mobile key -> press cabin [1-N] floor -> press door open`
- **test case 36**: `press hall LobbyUp`
- **test case 37**: `tap mobile key -> press cabin lobby`
- **test case 38**: `press hall LobbyUp`
- **test case 39**: `tap mobile key -> press cabin executive floor`
- **test case 40**: `press hall up -> tap mobile key -> press cabin executive floor`
- **test case 41**: `press hall RoofDown`
- **test case 42**: `press hall down -> press cabin roof -> press alarm button`
- **test case 43**: `tap mobile key -> press cabin roof -> press door open`
- **test case 44**: `press hall RoofDown -> tap mobile key`
- **test case 45**: `press cabin executive floor -> press door open`
- **test case 46**: `press hall LobbyUp`
- **test case 47**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 48**: `press cabin [1-N] floor -> press door open`
- **test case 49**: `press hall LobbyUp -> press cabin [1-N] floor -> press door close`
- **test case 50**: `tap mobile key -> press cabin executive floor`
- **test case 51**: `press door open -> press door close`
- **test case 52**: `press cabin [1-N] floor -> press intercom`
- **test case 53**: `press cabin [1-N] floor -> press alarm button`
- **test case 54**: `press hall LobbyUp -> press cabin roof`
- **test case 55**: `press hall up`
- **test case 56**: `press cabin lobby -> press intercom`
- **test case 57**: `press cabin lobby -> press door close`
- **test case 58**: `press cabin executive floor -> press door close -> press door open -> press door close`
- **test case 59**: `press hall LobbyUp`
- **test case 60**: `press cabin [1-N] floor -> press door open`
- **test case 61**: `press cabin [1-N] floor -> press alarm button`
- **test case 62**: `press hall LobbyUp`
- **test case 63**: `press cabin roof -> press alarm button`
- **test case 64**: `press hall RoofDown`
- **test case 65**: `press hall up -> press cabin [1-N] floor`
- **test case 66**: `press hall down -> press cabin [1-N] floor`
- **test case 67**: `press cabin [1-N] floor -> press door close`
- **test case 68**: `press hall up`
- **test case 69**: `press cabin [1-N] floor -> press intercom`
- **test case 70**: `press cabin roof -> press door open`
- **test case 71**: `press cabin roof -> press intercom`
- **test case 72**: `press cabin roof -> press door close`
- **test case 73**: `press hall down`


### Product 25

**Selected features:** selected = {Alarm, CardReader, ControlButtons, Intercom, ManualDoorControl}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Pair graph (raw):** 28 nodes (incl. INIT), 103 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 28 nodes, 248 edges (145 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 25](Elevator-product25-repaired.png)

![Pair graph (raw) — product 25](Elevator-product25-pairgraph-raw.png)

![Pair graph (balanced) — product 25](Elevator-product25-pairgraph-balanced.png)

**Generated test suite** — 58 test case(s) total (141 real step(s); pair-graph cycle has 248 edge(s) total, 145 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press intercom`
- **test case 3**: `press hall down -> press cabin lobby -> press door open -> press door close -> press door open`
- **test case 4**: `press hall down -> read card -> press cabin [1-N] floor -> press intercom`
- **test case 5**: `read card -> press cabin roof -> press door close -> press door open -> press door close`
- **test case 6**: `press hall RoofDown -> read card`
- **test case 7**: `press cabin roof -> press intercom`
- **test case 8**: `press hall down`
- **test case 9**: `press cabin roof -> press door open`
- **test case 10**: `press cabin [1-N] floor -> press door close`
- **test case 11**: `read card -> press cabin [1-N] floor`
- **test case 12**: `press cabin [1-N] floor -> press alarm button`
- **test case 13**: `press hall RoofDown`
- **test case 14**: `press cabin lobby -> press door close`
- **test case 15**: `read card -> press cabin lobby -> press alarm button`
- **test case 16**: `press cabin lobby -> press door open`
- **test case 17**: `press hall up -> press cabin roof -> press door close`
- **test case 18**: `press cabin lobby -> press intercom`
- **test case 19**: `press hall up -> read card -> press cabin lobby -> press intercom`
- **test case 20**: `press hall RoofDown -> press cabin lobby -> press door open`
- **test case 21**: `read card -> press cabin roof -> press alarm button`
- **test case 22**: `press cabin lobby -> press alarm button`
- **test case 23**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 24**: `press hall RoofDown`
- **test case 25**: `press hall LobbyUp`
- **test case 26**: `read card -> press cabin [1-N] floor -> press door open`
- **test case 27**: `press hall RoofDown`
- **test case 28**: `press cabin lobby -> press door close`
- **test case 29**: `press cabin [1-N] floor -> press door close`
- **test case 30**: `press cabin [1-N] floor -> press door open`
- **test case 31**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **test case 32**: `press hall up`
- **test case 33**: `press cabin lobby -> press intercom`
- **test case 34**: `press cabin lobby -> press door close`
- **test case 35**: `press hall RoofDown`
- **test case 36**: `press cabin [1-N] floor -> press door open`
- **test case 37**: `press hall LobbyUp`
- **test case 38**: `read card -> press cabin roof -> press door open`
- **test case 39**: `press hall LobbyUp -> read card -> press cabin lobby`
- **test case 40**: `press hall up -> press cabin [1-N] floor -> press alarm button`
- **test case 41**: `press hall up`
- **test case 42**: `press hall LobbyUp -> press cabin [1-N] floor -> press door open`
- **test case 43**: `press cabin [1-N] floor -> press door close`
- **test case 44**: `press hall LobbyUp`
- **test case 45**: `press cabin [1-N] floor -> press intercom`
- **test case 46**: `press cabin [1-N] floor -> press alarm button`
- **test case 47**: `press hall LobbyUp -> press cabin roof`
- **test case 48**: `press hall RoofDown`
- **test case 49**: `press cabin [1-N] floor -> press intercom`
- **test case 50**: `press hall LobbyUp`
- **test case 51**: `press hall down -> press cabin roof -> press alarm button`
- **test case 52**: `press cabin roof -> press door open`
- **test case 53**: `press cabin roof -> press intercom`
- **test case 54**: `press cabin roof -> press door close`
- **test case 55**: `press hall up`
- **test case 56**: `press cabin [1-N] floor -> press door close`
- **test case 57**: `press hall down -> press cabin [1-N] floor -> press intercom`
- **test case 58**: `press cabin roof -> press alarm button`


### Product 26

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Pair graph (raw):** 32 nodes (incl. INIT), 108 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 32 nodes, 235 edges (127 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 26](Elevator-product26-repaired.png)

![Pair graph (raw) — product 26](Elevator-product26-pairgraph-raw.png)

![Pair graph (balanced) — product 26](Elevator-product26-pairgraph-balanced.png)

**Generated test suite** — 61 test case(s) total (146 real step(s); pair-graph cycle has 235 edge(s) total, 127 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press intercom`
- **test case 3**: `press hall down -> press cabin lobby -> press door open -> press door close -> press door open`
- **test case 4**: `press hall down`
- **test case 5**: `read card -> press cabin [1-N] floor -> press intercom`
- **test case 6**: `press hall RoofDown -> read card`
- **test case 7**: `press cabin executive floor -> press intercom`
- **test case 8**: `press hall LobbyUp`
- **test case 9**: `read card -> press cabin [1-N] floor -> press door close -> press door open`
- **test case 10**: `press cabin roof -> press door open`
- **test case 11**: `press hall up -> press cabin roof`
- **test case 12**: `press hall down -> read card -> press cabin lobby -> press door close`
- **test case 13**: `press hall up`
- **test case 14**: `read card -> press cabin executive floor`
- **test case 15**: `press hall RoofDown`
- **test case 16**: `press hall down`
- **test case 17**: `press cabin executive floor -> press door open`
- **test case 18**: `press hall up -> read card`
- **test case 19**: `press door open -> press door close`
- **test case 20**: `read card -> press cabin executive floor -> press door close -> press door open -> press door close`
- **test case 21**: `read card -> press cabin roof -> press door close`
- **test case 22**: `press cabin roof -> press intercom`
- **test case 23**: `read card -> press cabin roof`
- **test case 24**: `read card -> press cabin [1-N] floor`
- **test case 25**: `press cabin [1-N] floor -> press door open`
- **test case 26**: `press hall up -> press cabin lobby`
- **test case 27**: `press hall RoofDown`
- **test case 28**: `read card -> press cabin lobby -> press door open`
- **test case 29**: `press hall RoofDown`
- **test case 30**: `press cabin lobby -> press intercom`
- **test case 31**: `press cabin lobby -> press intercom`
- **test case 32**: `press cabin lobby -> press door close`
- **test case 33**: `press cabin lobby -> press intercom`
- **test case 34**: `press hall up -> press cabin [1-N] floor -> press door open`
- **test case 35**: `press hall up`
- **test case 36**: `press hall RoofDown -> press cabin lobby`
- **test case 37**: `press cabin lobby -> press door open`
- **test case 38**: `press hall LobbyUp`
- **test case 39**: `read card -> press cabin roof -> press door open`
- **test case 40**: `press hall LobbyUp -> read card -> press cabin executive floor`
- **test case 41**: `press cabin lobby -> press door close`
- **test case 42**: `press cabin [1-N] floor -> press door close`
- **test case 43**: `press cabin [1-N] floor -> press door open`
- **test case 44**: `read card -> press cabin lobby`
- **test case 45**: `press hall up`
- **test case 46**: `press hall LobbyUp`
- **test case 47**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 48**: `press hall down -> press cabin roof -> press door close`
- **test case 49**: `press hall RoofDown`
- **test case 50**: `press cabin [1-N] floor -> press intercom`
- **test case 51**: `press hall LobbyUp -> press cabin [1-N] floor -> press door open`
- **test case 52**: `press cabin [1-N] floor -> press door close`
- **test case 53**: `press hall LobbyUp`
- **test case 54**: `press cabin [1-N] floor -> press intercom`
- **test case 55**: `press hall LobbyUp -> press cabin roof -> press door open`
- **test case 56**: `press cabin roof -> press intercom`
- **test case 57**: `press hall LobbyUp`
- **test case 58**: `press cabin roof -> press door close`
- **test case 59**: `press hall up`
- **test case 60**: `press cabin [1-N] floor -> press door close`
- **test case 61**: `press hall down -> press cabin [1-N] floor -> press intercom`


### Product 27

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Pair graph (raw):** 28 nodes (incl. INIT), 103 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 28 nodes, 244 edges (141 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 27](Elevator-product27-repaired.png)

![Pair graph (raw) — product 27](Elevator-product27-pairgraph-raw.png)

![Pair graph (balanced) — product 27](Elevator-product27-pairgraph-balanced.png)

**Generated test suite** — 58 test case(s) total (141 real step(s); pair-graph cycle has 244 edge(s) total, 141 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press intercom`
- **test case 3**: `press hall down -> press cabin lobby -> press door open -> press door close -> press door open`
- **test case 4**: `press hall down -> tap mobile key -> press cabin [1-N] floor -> press intercom`
- **test case 5**: `press cabin lobby -> press intercom`
- **test case 6**: `press cabin lobby -> press door open`
- **test case 7**: `press cabin roof -> press door open`
- **test case 8**: `press hall up`
- **test case 9**: `press hall down -> press cabin roof -> press door close -> press door open -> press door close`
- **test case 10**: `press hall RoofDown -> press cabin lobby -> press alarm button`
- **test case 11**: `press hall RoofDown`
- **test case 12**: `press cabin lobby -> press door close`
- **test case 13**: `press cabin [1-N] floor -> press door close`
- **test case 14**: `press cabin [1-N] floor -> press door open`
- **test case 15**: `press hall up -> press cabin roof -> press alarm button`
- **test case 16**: `press cabin [1-N] floor -> press alarm button`
- **test case 17**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 18**: `press cabin [1-N] floor -> press intercom`
- **test case 19**: `tap mobile key -> press cabin roof -> press door close`
- **test case 20**: `press hall RoofDown`
- **test case 21**: `press cabin roof -> press intercom`
- **test case 22**: `tap mobile key -> press cabin lobby -> press door close`
- **test case 23**: `press hall RoofDown`
- **test case 24**: `tap mobile key -> press cabin lobby -> press alarm button`
- **test case 25**: `press hall up -> tap mobile key -> press cabin roof -> press alarm button`
- **test case 26**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 27**: `press cabin lobby -> press door open`
- **test case 28**: `press cabin lobby -> press intercom`
- **test case 29**: `press hall LobbyUp -> tap mobile key -> press cabin [1-N] floor -> press door close`
- **test case 30**: `press cabin lobby -> press intercom`
- **test case 31**: `tap mobile key -> press cabin lobby`
- **test case 32**: `press hall LobbyUp`
- **test case 33**: `tap mobile key -> press cabin roof -> press door open`
- **test case 34**: `press hall RoofDown -> tap mobile key -> press cabin [1-N] floor`
- **test case 35**: `press hall up`
- **test case 36**: `press cabin lobby -> press door close`
- **test case 37**: `press hall LobbyUp`
- **test case 38**: `press cabin [1-N] floor -> press door open`
- **test case 39**: `press cabin [1-N] floor -> press alarm button`
- **test case 40**: `press cabin [1-N] floor -> press door open`
- **test case 41**: `press hall LobbyUp -> press cabin [1-N] floor -> press door close`
- **test case 42**: `press cabin [1-N] floor -> press alarm button`
- **test case 43**: `press cabin [1-N] floor -> press intercom`
- **test case 44**: `press hall LobbyUp`
- **test case 45**: `press cabin [1-N] floor -> press alarm button`
- **test case 46**: `press hall LobbyUp -> press cabin roof`
- **test case 47**: `press hall up -> press cabin [1-N] floor`
- **test case 48**: `press hall RoofDown`
- **test case 49**: `press cabin [1-N] floor -> press door open`
- **test case 50**: `press cabin roof -> press alarm button`
- **test case 51**: `press hall down`
- **test case 52**: `press cabin [1-N] floor -> press door close`
- **test case 53**: `press hall up`
- **test case 54**: `press cabin [1-N] floor -> press intercom`
- **test case 55**: `press cabin roof -> press door open`
- **test case 56**: `press cabin roof -> press intercom`
- **test case 57**: `press cabin roof -> press door close`
- **test case 58**: `press hall down -> press cabin [1-N] floor`


### Product 28

**Selected features:** selected = {Alarm, ControlButtons, FirefighterService, Intercom}

**Repaired FTS:** 10 states, 25 transitions (21 real / 4 `__end__`).

**Pair graph (raw):** 26 nodes (incl. INIT), 81 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 26 nodes, 197 edges (116 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 28](Elevator-product28-repaired.png)

![Pair graph (raw) — product 28](Elevator-product28-pairgraph-raw.png)

![Pair graph (balanced) — product 28](Elevator-product28-pairgraph-balanced.png)

**Generated test suite** — 46 test case(s) total (112 real step(s); pair-graph cycle has 197 edge(s) total, 116 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press cabin lobby -> press alarm button -> press&hold door close -> release door close`
- **test case 3**: `press hall LobbyUp -> press cabin roof`
- **test case 4**: `press hall down -> press cabin lobby -> press&hold door close`
- **test case 5**: `press cabin roof -> press alarm button -> press&hold door open -> release door open -> press&hold door open -> release door open`
- **test case 6**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 7**: `press hall RoofDown`
- **test case 8**: `press cabin roof -> press&hold door close`
- **test case 9**: `press cabin roof -> press&hold door open`
- **test case 10**: `press hall down -> press cabin roof -> press&hold door open`
- **test case 11**: `press hall LobbyUp`
- **test case 12**: `press cabin roof -> press intercom -> press&hold door open`
- **test case 13**: `press hall LobbyUp -> press cabin [1-N] floor -> press&hold door open`
- **test case 14**: `press cabin lobby -> press&hold door close -> release door close`
- **test case 15**: `press cabin [1-N] floor -> press intercom -> press&hold door close`
- **test case 16**: `press hall RoofDown -> press cabin lobby`
- **test case 17**: `press hall RoofDown`
- **test case 18**: `press cabin [1-N] floor -> press&hold door close`
- **test case 19**: `press hall down`
- **test case 20**: `press cabin [1-N] floor -> press&hold door close`
- **test case 21**: `press cabin lobby -> press&hold door open`
- **test case 22**: `press cabin lobby -> press intercom`
- **test case 23**: `press cabin [1-N] floor -> press&hold door open`
- **test case 24**: `press cabin [1-N] floor -> press alarm button`
- **test case 25**: `press cabin lobby -> press intercom`
- **test case 26**: `press cabin lobby -> press&hold door open`
- **test case 27**: `press cabin [1-N] floor -> press&hold door close`
- **test case 28**: `press hall up`
- **test case 29**: `press cabin roof -> press intercom`
- **test case 30**: `press hall LobbyUp`
- **test case 31**: `press cabin [1-N] floor -> press alarm button`
- **test case 32**: `press hall up -> press cabin roof -> press&hold door close`
- **test case 33**: `press cabin roof -> press alarm button`
- **test case 34**: `press hall up -> press cabin [1-N] floor`
- **test case 35**: `release door close -> press&hold door close -> release door close`
- **test case 36**: `press cabin [1-N] floor -> press alarm button`
- **test case 37**: `press hall up`
- **test case 38**: `press cabin [1-N] floor -> press intercom`
- **test case 39**: `press hall down -> press cabin [1-N] floor`
- **test case 40**: `press hall RoofDown -> press cabin [1-N] floor -> press intercom`
- **test case 41**: `press hall LobbyUp`
- **test case 42**: `press hall down`
- **test case 43**: `press cabin [1-N] floor -> press&hold door open -> release door open`
- **test case 44**: `press hall RoofDown`
- **test case 45**: `press hall up`
- **test case 46**: `press hall LobbyUp`


### Product 29

**Selected features:** selected = {CardReader, ControlButtons, ExecutiveFloor, Intercom}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Pair graph (raw):** 24 nodes (incl. INIT), 66 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 24 nodes, 127 edges (61 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 29](Elevator-product29-repaired.png)

![Pair graph (raw) — product 29](Elevator-product29-pairgraph-raw.png)

![Pair graph (balanced) — product 29](Elevator-product29-pairgraph-balanced.png)

**Generated test suite** — 31 test case(s) total (80 real step(s); pair-graph cycle has 127 edge(s) total, 61 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `read card -> press cabin [1-N] floor -> press intercom`
- **test case 3**: `press hall RoofDown -> read card`
- **test case 4**: `press hall RoofDown`
- **test case 5**: `read card -> press cabin executive floor`
- **test case 6**: `press hall LobbyUp`
- **test case 7**: `read card -> press cabin [1-N] floor`
- **test case 8**: `press hall down -> press cabin lobby`
- **test case 9**: `press hall up -> press cabin lobby -> press intercom`
- **test case 10**: `press hall RoofDown`
- **test case 11**: `read card -> press cabin executive floor`
- **test case 12**: `press hall down -> press cabin roof -> press intercom`
- **test case 13**: `read card -> press cabin lobby -> press intercom`
- **test case 14**: `read card -> press cabin roof`
- **test case 15**: `press hall RoofDown -> press cabin lobby -> press intercom`
- **test case 16**: `press hall up -> press cabin roof`
- **test case 17**: `press hall down -> read card -> press cabin executive floor -> press intercom`
- **test case 18**: `read card -> press cabin lobby`
- **test case 19**: `press hall RoofDown -> press cabin [1-N] floor -> press intercom`
- **test case 20**: `press hall LobbyUp -> read card -> press cabin roof`
- **test case 21**: `press hall LobbyUp -> press cabin roof`
- **test case 22**: `press cabin roof -> press intercom`
- **test case 23**: `press hall down -> press cabin [1-N] floor`
- **test case 24**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 25**: `press hall up -> press cabin [1-N] floor -> press intercom`
- **test case 26**: `press hall up`
- **test case 27**: `read card -> press cabin [1-N] floor`
- **test case 28**: `press hall up -> read card -> press cabin lobby`
- **test case 29**: `read card -> press cabin roof -> press intercom`
- **test case 30**: `press hall LobbyUp`
- **test case 31**: `press cabin [1-N] floor -> press intercom`


### Product 30

**Selected features:** selected = {ControlButtons, Intercom, MobileKey}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Pair graph (raw):** 21 nodes (incl. INIT), 56 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 21 nodes, 100 edges (44 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 30](Elevator-product30-repaired.png)

![Pair graph (raw) — product 30](Elevator-product30-pairgraph-raw.png)

![Pair graph (balanced) — product 30](Elevator-product30-pairgraph-balanced.png)

**Generated test suite** — 28 test case(s) total (69 real step(s); pair-graph cycle has 100 edge(s) total, 44 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press hall down -> press cabin lobby`
- **test case 3**: `press hall down -> press cabin roof -> press intercom`
- **test case 4**: `press hall RoofDown -> press cabin lobby -> press intercom`
- **test case 5**: `press cabin [1-N] floor -> press intercom`
- **test case 6**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 7**: `press hall up -> press cabin lobby -> press intercom`
- **test case 8**: `press hall down -> press cabin [1-N] floor`
- **test case 9**: `press hall LobbyUp -> press cabin roof`
- **test case 10**: `tap mobile key -> press cabin roof`
- **test case 11**: `press hall RoofDown -> tap mobile key -> press cabin lobby -> press intercom`
- **test case 12**: `press cabin roof -> press intercom`
- **test case 13**: `press hall LobbyUp`
- **test case 14**: `tap mobile key -> press cabin [1-N] floor -> press intercom`
- **test case 15**: `press hall down -> tap mobile key -> press cabin [1-N] floor`
- **test case 16**: `press hall RoofDown`
- **test case 17**: `tap mobile key -> press cabin [1-N] floor`
- **test case 18**: `press hall LobbyUp -> tap mobile key -> press cabin roof`
- **test case 19**: `press hall LobbyUp`
- **test case 20**: `tap mobile key -> press cabin lobby`
- **test case 21**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 22**: `press hall up -> press cabin roof`
- **test case 23**: `press hall up -> press cabin [1-N] floor -> press intercom`
- **test case 24**: `press hall up`
- **test case 25**: `tap mobile key -> press cabin lobby`
- **test case 26**: `press hall up -> tap mobile key -> press cabin roof -> press intercom`
- **test case 27**: `press hall LobbyUp`
- **test case 28**: `press cabin [1-N] floor -> press intercom`


### Product 31

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ManualDoorControl}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Pair graph (raw):** 27 nodes (incl. INIT), 92 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 27 nodes, 210 edges (118 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 31](Elevator-product31-repaired.png)

![Pair graph (raw) — product 31](Elevator-product31-pairgraph-raw.png)

![Pair graph (balanced) — product 31](Elevator-product31-pairgraph-balanced.png)

**Generated test suite** — 51 test case(s) total (124 real step(s); pair-graph cycle has 210 edge(s) total, 118 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `read card -> press cabin [1-N] floor -> press door close -> press door open`
- **test case 3**: `press hall RoofDown`
- **test case 4**: `press cabin [1-N] floor -> press alarm button`
- **test case 5**: `press hall RoofDown -> read card`
- **test case 6**: `press cabin lobby -> press door open -> press door close -> press door open -> press door close`
- **test case 7**: `read card -> press cabin lobby`
- **test case 8**: `press hall down -> press cabin lobby -> press door open`
- **test case 9**: `read card -> press cabin roof`
- **test case 10**: `press hall RoofDown`
- **test case 11**: `press cabin roof -> press alarm button`
- **test case 12**: `press hall RoofDown`
- **test case 13**: `press cabin lobby -> press door open`
- **test case 14**: `press cabin lobby -> press alarm button`
- **test case 15**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 16**: `press hall up`
- **test case 17**: `press cabin lobby -> press door close`
- **test case 18**: `press hall RoofDown -> press cabin lobby`
- **test case 19**: `read card -> press cabin [1-N] floor -> press door open`
- **test case 20**: `press hall down -> press cabin roof`
- **test case 21**: `press hall LobbyUp`
- **test case 22**: `read card -> press cabin lobby -> press door close`
- **test case 23**: `press cabin lobby -> press door close`
- **test case 24**: `press cabin [1-N] floor -> press door close`
- **test case 25**: `press cabin [1-N] floor -> press alarm button`
- **test case 26**: `press cabin roof -> press door open`
- **test case 27**: `press hall up -> press cabin roof -> press door close`
- **test case 28**: `press cabin [1-N] floor -> press door open`
- **test case 29**: `press hall up`
- **test case 30**: `press cabin roof -> press alarm button`
- **test case 31**: `press hall up -> press cabin [1-N] floor -> press door open`
- **test case 32**: `press hall LobbyUp -> read card -> press cabin roof -> press door close`
- **test case 33**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 34**: `press hall LobbyUp -> press cabin roof`
- **test case 35**: `press hall up`
- **test case 36**: `press cabin [1-N] floor -> press alarm button`
- **test case 37**: `press hall down -> read card -> press cabin [1-N] floor`
- **test case 38**: `press hall down -> press cabin [1-N] floor`
- **test case 39**: `press hall up -> read card -> press cabin roof -> press door open`
- **test case 40**: `press hall LobbyUp`
- **test case 41**: `press cabin roof -> press alarm button`
- **test case 42**: `press cabin roof -> press door open`
- **test case 43**: `press cabin roof -> press door close`
- **test case 44**: `press hall LobbyUp -> press cabin [1-N] floor -> press door close`
- **test case 45**: `press hall down`
- **test case 46**: `press cabin [1-N] floor -> press door close`
- **test case 47**: `press hall up`
- **test case 48**: `read card -> press cabin lobby -> press alarm button`
- **test case 49**: `press cabin [1-N] floor -> press alarm button`
- **test case 50**: `press hall LobbyUp`
- **test case 51**: `press cabin [1-N] floor -> press door open`


### Product 32

**Selected features:** selected = {ControlButtons, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Pair graph (raw):** 27 nodes (incl. INIT), 92 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 27 nodes, 207 edges (115 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 32](Elevator-product32-repaired.png)

![Pair graph (raw) — product 32](Elevator-product32-pairgraph-raw.png)

![Pair graph (balanced) — product 32](Elevator-product32-pairgraph-balanced.png)

**Generated test suite** — 52 test case(s) total (125 real step(s); pair-graph cycle has 207 edge(s) total, 115 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press door open -> press door close -> press door open`
- **test case 3**: `press hall RoofDown`
- **test case 4**: `press cabin lobby -> press door open`
- **test case 5**: `press hall down -> press cabin lobby -> press door open`
- **test case 6**: `press hall RoofDown -> press cabin lobby -> press intercom`
- **test case 7**: `press hall up -> press cabin lobby`
- **test case 8**: `press door close -> press door open -> press door close`
- **test case 9**: `press cabin lobby -> press door close`
- **test case 10**: `press cabin lobby -> press door open`
- **test case 11**: `press hall down -> press cabin roof -> press intercom`
- **test case 12**: `press hall up`
- **test case 13**: `press cabin lobby -> press intercom`
- **test case 14**: `press cabin lobby -> press door close`
- **test case 15**: `enter PIN -> press cabin lobby -> press intercom`
- **test case 16**: `press hall RoofDown -> enter PIN -> press cabin [1-N] floor -> press intercom`
- **test case 17**: `press hall up -> press cabin roof -> press door open`
- **test case 18**: `press cabin roof -> press door close`
- **test case 19**: `enter PIN -> press cabin roof`
- **test case 20**: `press cabin roof -> press door close`
- **test case 21**: `press cabin [1-N] floor -> press door close`
- **test case 22**: `press hall RoofDown`
- **test case 23**: `press cabin [1-N] floor -> press door open`
- **test case 24**: `press hall up -> enter PIN`
- **test case 25**: `press cabin [1-N] floor -> press door close`
- **test case 26**: `press hall LobbyUp -> press cabin roof`
- **test case 27**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 28**: `press cabin [1-N] floor -> press intercom`
- **test case 29**: `press hall down`
- **test case 30**: `enter PIN -> press cabin [1-N] floor -> press door open`
- **test case 31**: `enter PIN -> press cabin roof -> press intercom`
- **test case 32**: `press hall LobbyUp`
- **test case 33**: `press hall up`
- **test case 34**: `press cabin [1-N] floor -> press door open`
- **test case 35**: `press hall LobbyUp`
- **test case 36**: `press cabin roof -> press door open`
- **test case 37**: `press cabin roof -> press intercom`
- **test case 38**: `press cabin roof -> press door close`
- **test case 39**: `press hall LobbyUp`
- **test case 40**: `press cabin [1-N] floor -> press door close`
- **test case 41**: `press hall up -> press cabin [1-N] floor`
- **test case 42**: `press hall down -> enter PIN -> press cabin lobby`
- **test case 43**: `press hall up`
- **test case 44**: `press hall LobbyUp -> press cabin [1-N] floor -> press intercom`
- **test case 45**: `press hall LobbyUp`
- **test case 46**: `press cabin [1-N] floor -> press door open`
- **test case 47**: `enter PIN -> press cabin roof -> press door open`
- **test case 48**: `press hall LobbyUp -> enter PIN -> press cabin lobby -> press door close`
- **test case 49**: `press hall down -> press cabin [1-N] floor -> press door close`
- **test case 50**: `press hall up`
- **test case 51**: `press cabin [1-N] floor -> press intercom`
- **test case 52**: `enter PIN -> press cabin [1-N] floor`


### Product 33

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, MobileKey}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Pair graph (raw):** 32 nodes (incl. INIT), 108 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 32 nodes, 236 edges (128 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 33](Elevator-product33-repaired.png)

![Pair graph (raw) — product 33](Elevator-product33-pairgraph-raw.png)

![Pair graph (balanced) — product 33](Elevator-product33-pairgraph-balanced.png)

**Generated test suite** — 60 test case(s) total (145 real step(s); pair-graph cycle has 236 edge(s) total, 128 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press intercom`
- **test case 3**: `press hall down -> press cabin lobby -> press door open -> press door close -> press door open`
- **test case 4**: `press hall down -> tap mobile key -> press cabin [1-N] floor -> press intercom`
- **test case 5**: `press hall RoofDown`
- **test case 6**: `press cabin lobby -> press intercom`
- **test case 7**: `press cabin roof -> press door open`
- **test case 8**: `press hall up -> press cabin roof`
- **test case 9**: `press hall down`
- **test case 10**: `tap mobile key -> press cabin lobby -> press door close -> press door open`
- **test case 11**: `press hall up`
- **test case 12**: `tap mobile key -> press cabin roof -> press door close`
- **test case 13**: `press hall up -> tap mobile key -> press cabin executive floor -> press intercom`
- **test case 14**: `press hall up`
- **test case 15**: `press hall LobbyUp`
- **test case 16**: `tap mobile key -> press cabin [1-N] floor -> press door close`
- **test case 17**: `press hall RoofDown -> press cabin lobby`
- **test case 18**: `press hall up -> press cabin lobby`
- **test case 19**: `press cabin lobby -> press door open`
- **test case 20**: `press hall RoofDown`
- **test case 21**: `press cabin lobby -> press door close`
- **test case 22**: `press cabin [1-N] floor -> press door close`
- **test case 23**: `press cabin [1-N] floor -> press door open`
- **test case 24**: `press hall RoofDown -> press cabin [1-N] floor -> press intercom`
- **test case 25**: `press cabin lobby -> press intercom`
- **test case 26**: `press hall RoofDown`
- **test case 27**: `tap mobile key -> press cabin roof -> press intercom`
- **test case 28**: `press hall LobbyUp -> tap mobile key -> press cabin lobby -> press door open`
- **test case 29**: `tap mobile key -> press cabin lobby -> press intercom`
- **test case 30**: `press hall up`
- **test case 31**: `press cabin lobby -> press door close`
- **test case 32**: `press hall RoofDown -> tap mobile key`
- **test case 33**: `tap mobile key -> press cabin [1-N] floor -> press door open`
- **test case 34**: `press hall RoofDown`
- **test case 35**: `press hall down -> press cabin roof -> press door close`
- **test case 36**: `press cabin executive floor -> press door open`
- **test case 37**: `press hall LobbyUp`
- **test case 38**: `tap mobile key -> press cabin executive floor`
- **test case 39**: `press door open -> press door close`
- **test case 40**: `press hall RoofDown`
- **test case 41**: `tap mobile key -> press cabin executive floor`
- **test case 42**: `press door close -> press door open -> press door close`
- **test case 43**: `press hall LobbyUp`
- **test case 44**: `tap mobile key -> press cabin roof -> press door open`
- **test case 45**: `press hall LobbyUp -> press cabin [1-N] floor -> press door open`
- **test case 46**: `press cabin [1-N] floor -> press door close`
- **test case 47**: `press cabin [1-N] floor -> press door open`
- **test case 48**: `press cabin [1-N] floor -> press intercom`
- **test case 49**: `press hall LobbyUp -> press cabin roof`
- **test case 50**: `press hall up -> press cabin [1-N] floor`
- **test case 51**: `press hall LobbyUp`
- **test case 52**: `press cabin roof -> press door open`
- **test case 53**: `press cabin roof -> press intercom`
- **test case 54**: `press cabin roof -> press door close`
- **test case 55**: `press hall RoofDown`
- **test case 56**: `press cabin executive floor -> press door close`
- **test case 57**: `press hall down -> press cabin [1-N] floor -> press door close`
- **test case 58**: `press hall up`
- **test case 59**: `press cabin [1-N] floor -> press intercom`
- **test case 60**: `press hall down`


### Product 34

**Selected features:** selected = {Alarm, CardReader, ControlButtons, ExecutiveFloor, ManualDoorControl}

**Repaired FTS:** 10 states, 31 transitions (26 real / 5 `__end__`).

**Pair graph (raw):** 32 nodes (incl. INIT), 108 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 32 nodes, 241 edges (133 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 34](Elevator-product34-repaired.png)

![Pair graph (raw) — product 34](Elevator-product34-pairgraph-raw.png)

![Pair graph (balanced) — product 34](Elevator-product34-pairgraph-balanced.png)

**Generated test suite** — 60 test case(s) total (145 real step(s); pair-graph cycle has 241 edge(s) total, 133 synthetic dropped at translation).

- **test case 1**: `press hall up -> press cabin roof -> press door open -> press door close -> press door open`
- **test case 2**: `press hall down -> press cabin lobby -> press door open`
- **test case 3**: `read card -> press cabin [1-N] floor -> press door close -> press door open`
- **test case 4**: `press hall up -> read card -> press cabin lobby -> press door close`
- **test case 5**: `press hall RoofDown -> read card`
- **test case 6**: `press cabin executive floor -> press alarm button`
- **test case 7**: `press hall down -> read card -> press cabin executive floor`
- **test case 8**: `press hall LobbyUp`
- **test case 9**: `read card -> press cabin [1-N] floor`
- **test case 10**: `press hall down`
- **test case 11**: `press hall RoofDown`
- **test case 12**: `press hall up`
- **test case 13**: `read card -> press cabin roof -> press door close`
- **test case 14**: `press hall RoofDown`
- **test case 15**: `press hall down -> press cabin roof -> press door close`
- **test case 16**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 17**: `press cabin executive floor -> press door open -> press door close`
- **test case 18**: `read card -> press cabin executive floor -> press door close -> press door open -> press door close`
- **test case 19**: `read card -> press cabin roof`
- **test case 20**: `press cabin roof -> press alarm button`
- **test case 21**: `press cabin [1-N] floor -> press alarm button`
- **test case 22**: `press hall RoofDown`
- **test case 23**: `read card -> press cabin [1-N] floor -> press door open`
- **test case 24**: `press cabin lobby -> press alarm button`
- **test case 25**: `read card -> press cabin lobby -> press door open`
- **test case 26**: `press hall RoofDown`
- **test case 27**: `press cabin lobby -> press door open`
- **test case 28**: `press cabin lobby -> press alarm button`
- **test case 29**: `press hall RoofDown -> press cabin lobby`
- **test case 30**: `press hall LobbyUp`
- **test case 31**: `read card -> press cabin roof -> press door open`
- **test case 32**: `press cabin lobby -> press door close`
- **test case 33**: `press cabin [1-N] floor -> press door close`
- **test case 34**: `press cabin [1-N] floor -> press door open`
- **test case 35**: `press hall LobbyUp`
- **test case 36**: `read card -> press cabin executive floor`
- **test case 37**: `press hall RoofDown`
- **test case 38**: `press cabin [1-N] floor -> press alarm button`
- **test case 39**: `press hall up`
- **test case 40**: `press cabin lobby -> press door close`
- **test case 41**: `press hall LobbyUp -> read card -> press cabin lobby`
- **test case 42**: `press cabin [1-N] floor -> press door open`
- **test case 43**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 44**: `press hall up -> press cabin [1-N] floor -> press alarm button`
- **test case 45**: `press cabin [1-N] floor -> press door open`
- **test case 46**: `press cabin [1-N] floor -> press door close`
- **test case 47**: `press hall up`
- **test case 48**: `press hall LobbyUp`
- **test case 49**: `press cabin [1-N] floor -> press alarm button`
- **test case 50**: `press hall LobbyUp -> press cabin roof`
- **test case 51**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 52**: `press hall down`
- **test case 53**: `press cabin roof -> press alarm button`
- **test case 54**: `press cabin roof -> press alarm button`
- **test case 55**: `press hall LobbyUp`
- **test case 56**: `press cabin roof -> press door open`
- **test case 57**: `press cabin roof -> press door close`
- **test case 58**: `press hall up`
- **test case 59**: `press cabin [1-N] floor -> press door close`
- **test case 60**: `press hall down -> press cabin [1-N] floor`


### Product 35

**Selected features:** selected = {Alarm, ControlButtons, Intercom, MobileKey}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Pair graph (raw):** 22 nodes (incl. INIT), 67 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 22 nodes, 127 edges (60 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 35](Elevator-product35-repaired.png)

![Pair graph (raw) — product 35](Elevator-product35-pairgraph-raw.png)

![Pair graph (balanced) — product 35](Elevator-product35-pairgraph-balanced.png)

**Generated test suite** — 34 test case(s) total (85 real step(s); pair-graph cycle has 127 edge(s) total, 60 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press cabin lobby -> press alarm button`
- **test case 3**: `press hall RoofDown -> press cabin lobby`
- **test case 4**: `press hall down -> press cabin lobby -> press alarm button`
- **test case 5**: `press hall RoofDown`
- **test case 6**: `press cabin lobby -> press intercom`
- **test case 7**: `press cabin [1-N] floor -> press alarm button`
- **test case 8**: `press cabin [1-N] floor -> press intercom`
- **test case 9**: `press hall up -> press cabin lobby`
- **test case 10**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 11**: `press cabin lobby -> press intercom`
- **test case 12**: `press cabin roof -> press intercom`
- **test case 13**: `press hall up -> press cabin roof`
- **test case 14**: `press hall RoofDown`
- **test case 15**: `tap mobile key -> press cabin roof`
- **test case 16**: `press hall RoofDown -> tap mobile key -> press cabin lobby -> press intercom`
- **test case 17**: `press hall down -> press cabin roof -> press alarm button`
- **test case 18**: `press hall down -> press cabin [1-N] floor -> press alarm button`
- **test case 19**: `press hall LobbyUp -> press cabin roof`
- **test case 20**: `tap mobile key -> press cabin [1-N] floor -> press intercom`
- **test case 21**: `press cabin roof -> press alarm button`
- **test case 22**: `press cabin roof -> press intercom`
- **test case 23**: `tap mobile key -> press cabin [1-N] floor -> press alarm button`
- **test case 24**: `press hall down -> tap mobile key -> press cabin [1-N] floor`
- **test case 25**: `press hall LobbyUp -> tap mobile key -> press cabin roof -> press alarm button`
- **test case 26**: `press hall up -> press cabin [1-N] floor`
- **test case 27**: `press hall up`
- **test case 28**: `press cabin [1-N] floor -> press intercom`
- **test case 29**: `press hall LobbyUp`
- **test case 30**: `tap mobile key -> press cabin lobby`
- **test case 31**: `tap mobile key -> press cabin roof -> press intercom`
- **test case 32**: `press hall up -> tap mobile key -> press cabin lobby -> press alarm button`
- **test case 33**: `press hall LobbyUp -> press cabin [1-N] floor -> press intercom`
- **test case 34**: `press cabin [1-N] floor -> press alarm button`


### Product 36

**Selected features:** selected = {Alarm, ControlButtons, Intercom, PinPad}

**Repaired FTS:** 7 states, 21 transitions (19 real / 2 `__end__`).

**Pair graph (raw):** 22 nodes (incl. INIT), 67 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 22 nodes, 127 edges (60 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 36](Elevator-product36-repaired.png)

![Pair graph (raw) — product 36](Elevator-product36-pairgraph-raw.png)

![Pair graph (balanced) — product 36](Elevator-product36-pairgraph-balanced.png)

**Generated test suite** — 36 test case(s) total (87 real step(s); pair-graph cycle has 127 edge(s) total, 60 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press cabin lobby -> press alarm button`
- **test case 3**: `press hall RoofDown`
- **test case 4**: `press hall down -> press cabin lobby -> press alarm button`
- **test case 5**: `press hall RoofDown -> press cabin lobby -> press intercom`
- **test case 6**: `enter PIN -> press cabin lobby -> press intercom`
- **test case 7**: `press hall RoofDown -> enter PIN -> press cabin [1-N] floor -> press intercom`
- **test case 8**: `press hall up -> press cabin lobby`
- **test case 9**: `enter PIN -> press cabin roof`
- **test case 10**: `press cabin [1-N] floor -> press alarm button`
- **test case 11**: `press cabin lobby -> press intercom`
- **test case 12**: `press hall up`
- **test case 13**: `press cabin roof -> press intercom`
- **test case 14**: `press hall up -> press cabin roof`
- **test case 15**: `press hall RoofDown`
- **test case 16**: `press cabin [1-N] floor -> press intercom`
- **test case 17**: `press hall up -> enter PIN -> press cabin [1-N] floor -> press alarm button`
- **test case 18**: `press hall down -> press cabin roof -> press alarm button`
- **test case 19**: `press hall down -> enter PIN -> press cabin roof -> press alarm button`
- **test case 20**: `press hall LobbyUp -> press cabin roof`
- **test case 21**: `press cabin roof -> press alarm button`
- **test case 22**: `press cabin roof -> press intercom`
- **test case 23**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 24**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 25**: `press cabin [1-N] floor -> press intercom`
- **test case 26**: `press hall LobbyUp`
- **test case 27**: `press cabin [1-N] floor -> press alarm button`
- **test case 28**: `press hall down -> press cabin [1-N] floor -> press alarm button`
- **test case 29**: `press hall up`
- **test case 30**: `enter PIN -> press cabin lobby`
- **test case 31**: `press hall LobbyUp -> enter PIN -> press cabin [1-N] floor`
- **test case 32**: `press hall up -> press cabin [1-N] floor`
- **test case 33**: `press cabin [1-N] floor -> press intercom`
- **test case 34**: `enter PIN -> press cabin roof -> press intercom`
- **test case 35**: `press hall LobbyUp`
- **test case 36**: `enter PIN -> press cabin lobby -> press alarm button`


### Product 37

**Selected features:** selected = {ControlButtons, ExecutiveFloor, Intercom, MobileKey}

**Repaired FTS:** 8 states, 23 transitions (20 real / 3 `__end__`).

**Pair graph (raw):** 24 nodes (incl. INIT), 66 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 24 nodes, 124 edges (58 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 37](Elevator-product37-repaired.png)

![Pair graph (raw) — product 37](Elevator-product37-pairgraph-raw.png)

![Pair graph (balanced) — product 37](Elevator-product37-pairgraph-balanced.png)

**Generated test suite** — 33 test case(s) total (82 real step(s); pair-graph cycle has 124 edge(s) total, 58 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown -> press cabin lobby`
- **test case 2**: `press hall down -> press cabin lobby`
- **test case 3**: `press cabin lobby -> press intercom`
- **test case 4**: `press cabin [1-N] floor -> press intercom`
- **test case 5**: `press hall RoofDown`
- **test case 6**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 7**: `tap mobile key -> press cabin roof`
- **test case 8**: `tap mobile key -> press cabin lobby -> press intercom`
- **test case 9**: `press hall up -> press cabin lobby -> press intercom`
- **test case 10**: `press hall up -> press cabin roof -> press intercom`
- **test case 11**: `press hall down -> press cabin roof`
- **test case 12**: `press hall RoofDown`
- **test case 13**: `tap mobile key -> press cabin [1-N] floor -> press intercom`
- **test case 14**: `press hall LobbyUp -> press cabin roof`
- **test case 15**: `press hall RoofDown`
- **test case 16**: `press cabin roof -> press intercom`
- **test case 17**: `press hall down -> press cabin [1-N] floor`
- **test case 18**: `press hall RoofDown -> tap mobile key -> press cabin executive floor`
- **test case 19**: `press hall LobbyUp`
- **test case 20**: `tap mobile key -> press cabin [1-N] floor`
- **test case 21**: `press hall up -> press cabin [1-N] floor -> press intercom`
- **test case 22**: `press hall up`
- **test case 23**: `tap mobile key -> press cabin [1-N] floor`
- **test case 24**: `press hall LobbyUp -> tap mobile key -> press cabin roof`
- **test case 25**: `press hall LobbyUp`
- **test case 26**: `tap mobile key -> press cabin lobby`
- **test case 27**: `tap mobile key -> press cabin executive floor`
- **test case 28**: `press hall down -> tap mobile key -> press cabin roof -> press intercom`
- **test case 29**: `press hall LobbyUp -> press cabin [1-N] floor`
- **test case 30**: `press hall up`
- **test case 31**: `tap mobile key -> press cabin lobby`
- **test case 32**: `press hall up -> tap mobile key -> press cabin executive floor -> press intercom`
- **test case 33**: `press cabin [1-N] floor -> press intercom`


### Product 38

**Selected features:** selected = {Alarm, ControlButtons, PinPad}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Pair graph (raw):** 21 nodes (incl. INIT), 56 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 21 nodes, 100 edges (44 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 38](Elevator-product38-repaired.png)

![Pair graph (raw) — product 38](Elevator-product38-pairgraph-raw.png)

![Pair graph (balanced) — product 38](Elevator-product38-pairgraph-balanced.png)

**Generated test suite** — 25 test case(s) total (66 real step(s); pair-graph cycle has 100 edge(s) total, 44 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown -> press cabin lobby -> press alarm button`
- **test case 2**: `press hall RoofDown`
- **test case 3**: `press hall down -> press cabin lobby -> press alarm button`
- **test case 4**: `press hall up -> press cabin lobby`
- **test case 5**: `enter PIN -> press cabin lobby`
- **test case 6**: `enter PIN -> press cabin [1-N] floor -> press alarm button`
- **test case 7**: `press hall up -> press cabin roof`
- **test case 8**: `press hall RoofDown -> enter PIN -> press cabin roof`
- **test case 9**: `press hall down -> press cabin roof -> press alarm button`
- **test case 10**: `press hall down -> enter PIN -> press cabin [1-N] floor`
- **test case 11**: `press hall LobbyUp -> press cabin roof`
- **test case 12**: `press hall RoofDown`
- **test case 13**: `press hall LobbyUp`
- **test case 14**: `press cabin roof -> press alarm button`
- **test case 15**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **test case 16**: `press hall LobbyUp -> press cabin [1-N] floor -> press alarm button`
- **test case 17**: `press hall down -> press cabin [1-N] floor -> press alarm button`
- **test case 18**: `press hall up -> enter PIN -> press cabin roof`
- **test case 19**: `press hall LobbyUp`
- **test case 20**: `enter PIN -> press cabin [1-N] floor`
- **test case 21**: `press hall LobbyUp -> enter PIN -> press cabin roof -> press alarm button`
- **test case 22**: `press hall LobbyUp`
- **test case 23**: `enter PIN -> press cabin lobby`
- **test case 24**: `press hall up -> press cabin [1-N] floor`
- **test case 25**: `enter PIN -> press cabin lobby -> press alarm button`


### Product 39

**Selected features:** selected = {ControlButtons, Intercom, PinPad}

**Repaired FTS:** 7 states, 20 transitions (18 real / 2 `__end__`).

**Pair graph (raw):** 21 nodes (incl. INIT), 56 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 21 nodes, 100 edges (44 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 39](Elevator-product39-repaired.png)

![Pair graph (raw) — product 39](Elevator-product39-pairgraph-raw.png)

![Pair graph (balanced) — product 39](Elevator-product39-pairgraph-balanced.png)

**Generated test suite** — 28 test case(s) total (69 real step(s); pair-graph cycle has 100 edge(s) total, 44 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown -> press cabin lobby`
- **test case 2**: `press hall down -> press cabin lobby`
- **test case 3**: `press cabin lobby -> press intercom`
- **test case 4**: `press hall RoofDown`
- **test case 5**: `enter PIN -> press cabin lobby -> press intercom`
- **test case 6**: `press hall up -> press cabin lobby -> press intercom`
- **test case 7**: `press hall up -> press cabin roof -> press intercom`
- **test case 8**: `press hall down -> press cabin roof`
- **test case 9**: `enter PIN -> press cabin [1-N] floor -> press intercom`
- **test case 10**: `press hall down -> enter PIN -> press cabin [1-N] floor`
- **test case 11**: `press hall RoofDown -> enter PIN -> press cabin roof`
- **test case 12**: `press cabin [1-N] floor -> press intercom`
- **test case 13**: `press hall LobbyUp -> press cabin roof`
- **test case 14**: `press hall RoofDown`
- **test case 15**: `press hall LobbyUp`
- **test case 16**: `press cabin roof -> press intercom`
- **test case 17**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 18**: `press hall LobbyUp -> press cabin [1-N] floor -> press intercom`
- **test case 19**: `press hall down -> press cabin [1-N] floor`
- **test case 20**: `press hall up -> enter PIN -> press cabin roof`
- **test case 21**: `press hall LobbyUp`
- **test case 22**: `enter PIN -> press cabin [1-N] floor`
- **test case 23**: `press hall LobbyUp -> enter PIN -> press cabin roof -> press intercom`
- **test case 24**: `press hall LobbyUp`
- **test case 25**: `enter PIN -> press cabin lobby`
- **test case 26**: `press hall up`
- **test case 27**: `enter PIN -> press cabin lobby`
- **test case 28**: `press hall up -> press cabin [1-N] floor -> press intercom`


### Product 40

**Selected features:** selected = {Alarm, ControlButtons, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 26 transitions (22 real / 4 `__end__`).

**Pair graph (raw):** 27 nodes (incl. INIT), 92 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 27 nodes, 204 edges (112 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 40](Elevator-product40-repaired.png)

![Pair graph (raw) — product 40](Elevator-product40-pairgraph-raw.png)

![Pair graph (balanced) — product 40](Elevator-product40-pairgraph-balanced.png)

**Generated test suite** — 53 test case(s) total (126 real step(s); pair-graph cycle has 204 edge(s) total, 112 synthetic dropped at translation).

- **test case 1**: `press hall RoofDown`
- **test case 2**: `press door open -> press door close -> press door open`
- **test case 3**: `press hall RoofDown`
- **test case 4**: `press cabin lobby -> press door open`
- **test case 5**: `press hall RoofDown -> press cabin lobby`
- **test case 6**: `press hall RoofDown`
- **test case 7**: `press cabin lobby -> press alarm button`
- **test case 8**: `press hall down -> press cabin lobby -> press door open`
- **test case 9**: `press door close -> press door open -> press door close`
- **test case 10**: `press cabin lobby -> press door close`
- **test case 11**: `enter PIN -> press cabin lobby -> press door open`
- **test case 12**: `enter PIN -> press cabin [1-N] floor -> press door close`
- **test case 13**: `press cabin [1-N] floor -> press alarm button`
- **test case 14**: `enter PIN -> press cabin roof`
- **test case 15**: `press hall RoofDown -> enter PIN`
- **test case 16**: `press cabin roof -> press alarm button`
- **test case 17**: `press cabin [1-N] floor -> press door close`
- **test case 18**: `press cabin [1-N] floor -> press alarm button`
- **test case 19**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 20**: `press hall LobbyUp -> press cabin roof`
- **test case 21**: `press hall LobbyUp`
- **test case 22**: `press cabin roof -> press alarm button`
- **test case 23**: `press cabin lobby -> press door close`
- **test case 24**: `press hall RoofDown -> press cabin [1-N] floor -> press door open`
- **test case 25**: `press hall down -> press cabin roof`
- **test case 26**: `press hall up`
- **test case 27**: `press cabin roof -> press door open`
- **test case 28**: `press cabin roof -> press door close`
- **test case 29**: `press hall RoofDown`
- **test case 30**: `press hall up -> press cabin roof -> press alarm button`
- **test case 31**: `press hall up`
- **test case 32**: `enter PIN -> press cabin [1-N] floor -> press door open`
- **test case 33**: `press hall up -> enter PIN -> press cabin roof -> press door close`
- **test case 34**: `press cabin roof -> press door open`
- **test case 35**: `press hall up -> press cabin [1-N] floor -> press door open`
- **test case 36**: `press hall LobbyUp`
- **test case 37**: `press cabin roof -> press door close`
- **test case 38**: `press hall LobbyUp`
- **test case 39**: `press cabin [1-N] floor -> press door close`
- **test case 40**: `press hall down -> enter PIN -> press cabin lobby -> press door close`
- **test case 41**: `press hall down -> press cabin [1-N] floor`
- **test case 42**: `press hall LobbyUp -> press cabin [1-N] floor -> press alarm button`
- **test case 43**: `press cabin [1-N] floor -> press alarm button`
- **test case 44**: `press hall down`
- **test case 45**: `press hall up`
- **test case 46**: `press cabin [1-N] floor -> press door close`
- **test case 47**: `press hall up`
- **test case 48**: `press hall LobbyUp`
- **test case 49**: `press cabin [1-N] floor -> press door open`
- **test case 50**: `enter PIN -> press cabin roof -> press door open`
- **test case 51**: `press hall LobbyUp -> enter PIN -> press cabin lobby -> press alarm button`
- **test case 52**: `press hall LobbyUp`
- **test case 53**: `enter PIN -> press cabin [1-N] floor`


### Product 41

**Selected features:** selected = {Alarm, ControlButtons, ExecutiveFloor, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 10 states, 33 transitions (28 real / 5 `__end__`).

**Pair graph (raw):** 34 nodes (incl. INIT), 121 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 34 nodes, 284 edges (163 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 41](Elevator-product41-repaired.png)

![Pair graph (raw) — product 41](Elevator-product41-pairgraph-raw.png)

![Pair graph (balanced) — product 41](Elevator-product41-pairgraph-balanced.png)

**Generated test suite** — 71 test case(s) total (167 real step(s); pair-graph cycle has 284 edge(s) total, 163 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press intercom`
- **test case 3**: `press hall down -> press cabin lobby -> press door open -> press door close -> press door open`
- **test case 4**: `press hall down -> press cabin roof -> press door open`
- **test case 5**: `press hall down -> enter PIN -> press cabin lobby -> press door close -> press door open`
- **test case 6**: `press hall up`
- **test case 7**: `press cabin roof -> press door close`
- **test case 8**: `press cabin lobby -> press intercom`
- **test case 9**: `press cabin lobby -> press door open`
- **test case 10**: `press hall up -> press cabin roof -> press alarm button`
- **test case 11**: `press hall RoofDown -> press cabin lobby`
- **test case 12**: `press hall up -> enter PIN -> press cabin executive floor -> press alarm button`
- **test case 13**: `press hall RoofDown`
- **test case 14**: `press cabin lobby -> press alarm button`
- **test case 15**: `press cabin lobby -> press door close`
- **test case 16**: `enter PIN -> press cabin lobby -> press alarm button`
- **test case 17**: `press cabin lobby -> press door open`
- **test case 18**: `press hall RoofDown`
- **test case 19**: `press cabin lobby -> press intercom`
- **test case 20**: `enter PIN -> press cabin [1-N] floor -> press intercom`
- **test case 21**: `press cabin [1-N] floor -> press door close`
- **test case 22**: `enter PIN -> press cabin roof -> press door close`
- **test case 23**: `press cabin roof -> press intercom`
- **test case 24**: `press cabin executive floor -> press intercom`
- **test case 25**: `press hall LobbyUp`
- **test case 26**: `press hall RoofDown`
- **test case 27**: `press hall up`
- **test case 28**: `enter PIN -> press cabin [1-N] floor`
- **test case 29**: `press hall RoofDown`
- **test case 30**: `press hall down`
- **test case 31**: `press cabin [1-N] floor -> press alarm button`
- **test case 32**: `enter PIN -> press cabin executive floor -> press door open`
- **test case 33**: `press door open -> press door close`
- **test case 34**: `press hall RoofDown -> enter PIN`
- **test case 35**: `press door close -> press door open -> press door close`
- **test case 36**: `press hall RoofDown`
- **test case 37**: `press cabin [1-N] floor -> press door close`
- **test case 38**: `press cabin [1-N] floor -> press door open`
- **test case 39**: `press cabin [1-N] floor -> press door open`
- **test case 40**: `press hall LobbyUp -> press cabin [1-N] floor -> press door close`
- **test case 41**: `press hall LobbyUp`
- **test case 42**: `press cabin [1-N] floor -> press intercom`
- **test case 43**: `press hall RoofDown -> press cabin [1-N] floor -> press alarm button`
- **test case 44**: `enter PIN -> press cabin roof -> press alarm button`
- **test case 45**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 46**: `press hall up`
- **test case 47**: `press hall LobbyUp`
- **test case 48**: `press cabin [1-N] floor -> press alarm button`
- **test case 49**: `press cabin lobby -> press intercom`
- **test case 50**: `press cabin lobby -> press door close`
- **test case 51**: `press hall LobbyUp -> press cabin roof`
- **test case 52**: `press cabin [1-N] floor -> press door open`
- **test case 53**: `press hall up`
- **test case 54**: `press cabin [1-N] floor -> press alarm button`
- **test case 55**: `press hall up -> press cabin [1-N] floor`
- **test case 56**: `press hall LobbyUp`
- **test case 57**: `press hall RoofDown`
- **test case 58**: `press cabin [1-N] floor -> press intercom`
- **test case 59**: `press cabin roof -> press alarm button`
- **test case 60**: `press hall LobbyUp`
- **test case 61**: `press cabin roof -> press door open`
- **test case 62**: `press cabin roof -> press intercom`
- **test case 63**: `press cabin roof -> press door close`
- **test case 64**: `enter PIN -> press cabin [1-N] floor -> press door open`
- **test case 65**: `press hall LobbyUp -> enter PIN -> press cabin roof -> press door open`
- **test case 66**: `enter PIN -> press cabin executive floor -> press door close`
- **test case 67**: `press hall down`
- **test case 68**: `press hall up`
- **test case 69**: `press cabin [1-N] floor -> press door close`
- **test case 70**: `press hall down -> press cabin [1-N] floor -> press intercom`
- **test case 71**: `enter PIN -> press cabin lobby`


### Product 42

**Selected features:** selected = {Alarm, ControlButtons, Intercom, ManualDoorControl, PinPad}

**Repaired FTS:** 9 states, 27 transitions (23 real / 4 `__end__`).

**Pair graph (raw):** 28 nodes (incl. INIT), 103 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 28 nodes, 251 edges (148 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 42](Elevator-product42-repaired.png)

![Pair graph (raw) — product 42](Elevator-product42-pairgraph-raw.png)

![Pair graph (balanced) — product 42](Elevator-product42-pairgraph-balanced.png)

**Generated test suite** — 58 test case(s) total (141 real step(s); pair-graph cycle has 251 edge(s) total, 148 synthetic dropped at translation).

- **test case 1**: `press hall up`
- **test case 2**: `press cabin roof -> press intercom`
- **test case 3**: `press hall down -> press cabin lobby -> press door open -> press door close -> press door open`
- **test case 4**: `press hall down -> press cabin roof -> press door open`
- **test case 5**: `press hall down`
- **test case 6**: `press cabin roof -> press door close -> press door open -> press door close`
- **test case 7**: `press cabin lobby -> press intercom`
- **test case 8**: `press hall RoofDown`
- **test case 9**: `press hall up -> press cabin roof -> press alarm button`
- **test case 10**: `press hall RoofDown -> press cabin lobby -> press door open`
- **test case 11**: `press hall up -> enter PIN -> press cabin lobby -> press door close`
- **test case 12**: `enter PIN -> press cabin [1-N] floor -> press intercom`
- **test case 13**: `press cabin lobby -> press alarm button`
- **test case 14**: `press hall RoofDown`
- **test case 15**: `press cabin lobby -> press door close`
- **test case 16**: `press cabin lobby -> press alarm button`
- **test case 17**: `enter PIN -> press cabin lobby -> press door open`
- **test case 18**: `press hall RoofDown`
- **test case 19**: `press cabin lobby -> press intercom`
- **test case 20**: `press cabin [1-N] floor -> press door close`
- **test case 21**: `enter PIN -> press cabin [1-N] floor`
- **test case 22**: `press cabin [1-N] floor -> press alarm button`
- **test case 23**: `press cabin roof -> press door close`
- **test case 24**: `press hall RoofDown -> enter PIN -> press cabin roof -> press intercom`
- **test case 25**: `press cabin [1-N] floor -> press door close`
- **test case 26**: `press hall LobbyUp`
- **test case 27**: `press cabin [1-N] floor -> press door open`
- **test case 28**: `press cabin [1-N] floor -> press door open`
- **test case 29**: `press hall LobbyUp -> press cabin [1-N] floor -> press door close`
- **test case 30**: `press hall LobbyUp`
- **test case 31**: `press cabin [1-N] floor -> press intercom`
- **test case 32**: `press cabin [1-N] floor -> press alarm button`
- **test case 33**: `press hall RoofDown -> press cabin [1-N] floor`
- **test case 34**: `press hall up -> press cabin lobby -> press alarm button`
- **test case 35**: `press hall up`
- **test case 36**: `press cabin lobby -> press intercom`
- **test case 37**: `press hall up`
- **test case 38**: `press cabin lobby -> press door close`
- **test case 39**: `press cabin [1-N] floor -> press alarm button`
- **test case 40**: `press cabin [1-N] floor -> press door open`
- **test case 41**: `press hall LobbyUp -> press cabin roof`
- **test case 42**: `press cabin [1-N] floor -> press alarm button`
- **test case 43**: `press hall up -> press cabin [1-N] floor`
- **test case 44**: `press hall LobbyUp`
- **test case 45**: `press hall RoofDown`
- **test case 46**: `press cabin [1-N] floor -> press intercom`
- **test case 47**: `press cabin roof -> press alarm button`
- **test case 48**: `press hall LobbyUp`
- **test case 49**: `press cabin roof -> press door open`
- **test case 50**: `press cabin roof -> press intercom`
- **test case 51**: `press cabin roof -> press door close`
- **test case 52**: `press hall up`
- **test case 53**: `press cabin [1-N] floor -> press door close`
- **test case 54**: `press hall down -> enter PIN -> press cabin roof -> press alarm button`
- **test case 55**: `press hall down -> press cabin [1-N] floor -> press intercom`
- **test case 56**: `enter PIN -> press cabin [1-N] floor -> press door open`
- **test case 57**: `press hall LobbyUp -> enter PIN -> press cabin roof -> press door open`
- **test case 58**: `enter PIN -> press cabin lobby`

---

Total products: 42.
