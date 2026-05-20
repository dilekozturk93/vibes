# Per-Product All-States Coverage — SVM

## How the all-states test case is built

Given an SPL-level FTS plus one product configuration, [`StateCoverageGenerator.generate(fts, config, id)`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/StateCoverageGenerator.java) runs five steps. The algorithm is the analog of the ESG-Fx-side `EulerCycleGeneratorForEventCoverage` (event coverage on an ESG = state coverage on its FTS conversion); the structural objective is different from all-transitions, so the pipeline diverges after the SCC repair.

**Step 1 — Project onto the product.** Same as all-transitions: `FExpressionPreservingProjection.project(fts, config)` keeps every transition whose feature expression evaluates true under the product, then a forward BFS drops states unreachable from the initial state.

**Step 2 — Repair strong connectivity.** Same as all-transitions: `InitialSccFilter.keepInitialScc(projected)` keeps the SCC containing the initial state. State coverage requires only that every state in the repaired FTS be reachable AND able to return to wherever the walk decides to keep going — strong connectivity guarantees both at once.

**Step 3 — Greedy walk.** Starting at the initial state, at every step pick any outgoing transition whose target has not yet been visited. Implementation in [`pickUnvisitedNeighbour(...)`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/coverage/StateCoverageGenerator.java) — first match wins; iteration order is the FTS's natural outgoing-transition order, which is stable across runs given the deterministic VIBeS state-name ordering. Walk extends, every newly-visited target enters the `visited` set.

**Step 4 — BFS reroute when stuck.** When `pickUnvisitedNeighbour` returns `null` (every outgoing of the current state goes to a state we've already visited), the walk is stuck. [`ShortestPaths.shortestPathToAny(fts, current, remaining)`](../../../vibes-testgeneration/src/main/java/be/vibes/testgeneration/graph/ShortestPaths.java) runs a BFS from the current state and returns the path to the **nearest** state in the still-uncovered set. The path is appended to the walk; every state along the path is marked visited (the reroute incidentally covers intermediates too). Strong connectivity from step 2 guarantees that a path always exists.

**Step 5 — Repeat until covered, then wrap.** Steps 3+4 alternate until every state in the repaired FTS is in `visited`; the final transition list is enqueued into `be.vibes.ts.TestCase`. For display the cycle is split at initial-state returns via `TestCaseSplitter.splitAtInitialReturns(...)`. Synthetics (`__end__`) are hidden in the rendered sequence; `__dup__` and `__balance__` cannot occur (the state-coverage pipeline performs no balancing).

**Why BFS over Dijkstra.** All transitions are unit-weight (no edge cost distinguishes them at this layer), so BFS yields the optimal shortest path without paying for a priority queue. If we ever want to bias the walk (e.g. prefer paths that exercise more `__dup__` candidates, or paths that discharge dangerous feature expressions first), we'd switch to weighted Dijkstra — the API in `ShortestPaths` is structured to accept a weight function in a follow-up.

**Coverage claim.** Step 5 terminates iff every state is visited, and step 5 always terminates because (a) strong connectivity from step 2 guarantees BFS finds a path to any uncovered state, and (b) every iteration removes at least one state from `remaining`. State coverage is therefore **100% on the repaired FTS** by construction.

**Why this is generally shorter than all-transitions.** A walk that visits every state is bounded below by `|V| - 1` transitions (visit-once tree); an Euler cycle visiting every transition is bounded below by `|E|`. In real FTSs `|E|` is typically 1.5–4× `|V|`, so state-coverage walks are shorter — but they leave many edges un-exercised, weakening mutation detection. That trade-off is what RQ3 quantifies.

---

## Products

Each product below shows the repaired FTS with the state-coverage walk overlaid. **Legend:**

- **darkblue solid bold** — real transition picked by the **greedy** phase (target was unvisited at selection time);
- **darkorange solid bold** — real transition picked as part of a **BFS-reroute** shortest path (the greedy phase was stuck at a state with no unvisited neighbour);
- **red dashed bold** — synthetic transition (`__end__`) that the walk happens to traverse; same convention as in the all-transitions report;
- **light grey** — real transition not in the walk; **faint dashed red** — synthetic transition not in the walk. All transitions shown are present in the projected FTS exactly as drawn — none are synthesized for this report; the colour only encodes which phase of the algorithm picked them.

Test cases below are obtained by splitting the walk at every visit to the initial state — `__end__` and `__balance__N` are hidden from the displayed action sequence, `__dup__N` is stripped (the latter two never appear in a state-coverage walk since no balancing is performed).


### Product 1

**Selected features:** selected = {c, f, t}

**Repaired FTS:** 5 states, 6 transitions (6 real / 0 `__end__`).

**State-coverage walk:** 6 transition step(s) — 3 unique greedy edge(s), 1 BFS-reroute segment(s). State coverage on the repaired FTS: **5/5 = 100.0%**.

![Walk overlay — product 1](SVM-product1-walk.png)

**Generated test cases** (2 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `free -> tea -> serveTea -> take`
- **test case 2**: `free -> cancel`


### Product 2

**Selected features:** selected = {s, t}

**Repaired FTS:** 8 states, 9 transitions (9 real / 0 `__end__`).

**State-coverage walk:** 10 transition step(s) — 6 unique greedy edge(s), 1 BFS-reroute segment(s). State coverage on the repaired FTS: **8/8 = 100.0%**.

![Walk overlay — product 2](SVM-product2-walk.png)

**Generated test cases** (2 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `pay -> change -> soda -> serveSoda -> open -> take -> close`
- **test case 2**: `pay -> change -> tea`


### Product 3

**Selected features:** selected = {s}

**Repaired FTS:** 7 states, 7 transitions (7 real / 0 `__end__`).

**State-coverage walk:** 6 transition step(s) — 6 unique greedy edge(s), 0 BFS-reroute segment(s). State coverage on the repaired FTS: **7/7 = 100.0%**.

![Walk overlay — product 3](SVM-product3-walk.png)

**Generated test cases** (1 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `pay -> change -> soda -> serveSoda -> open -> take`


### Product 4

**Selected features:** selected = {t}

**Repaired FTS:** 7 states, 7 transitions (7 real / 0 `__end__`).

**State-coverage walk:** 6 transition step(s) — 6 unique greedy edge(s), 0 BFS-reroute segment(s). State coverage on the repaired FTS: **7/7 = 100.0%**.

![Walk overlay — product 4](SVM-product4-walk.png)

**Generated test cases** (1 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `pay -> change -> tea -> serveTea -> open -> take`


### Product 5

**Selected features:** selected = {f, s, t}

**Repaired FTS:** 5 states, 6 transitions (6 real / 0 `__end__`).

**State-coverage walk:** 6 transition step(s) — 3 unique greedy edge(s), 1 BFS-reroute segment(s). State coverage on the repaired FTS: **5/5 = 100.0%**.

![Walk overlay — product 5](SVM-product5-walk.png)

**Generated test cases** (2 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `free -> soda -> serveSoda -> take`
- **test case 2**: `free -> tea`


### Product 6

**Selected features:** selected = {c, f, s}

**Repaired FTS:** 5 states, 6 transitions (6 real / 0 `__end__`).

**State-coverage walk:** 6 transition step(s) — 3 unique greedy edge(s), 1 BFS-reroute segment(s). State coverage on the repaired FTS: **5/5 = 100.0%**.

![Walk overlay — product 6](SVM-product6-walk.png)

**Generated test cases** (2 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `free -> soda -> serveSoda -> take`
- **test case 2**: `free -> cancel`


### Product 7

**Selected features:** selected = {c, s}

**Repaired FTS:** 8 states, 9 transitions (9 real / 0 `__end__`).

**State-coverage walk:** 10 transition step(s) — 6 unique greedy edge(s), 1 BFS-reroute segment(s). State coverage on the repaired FTS: **8/8 = 100.0%**.

![Walk overlay — product 7](SVM-product7-walk.png)

**Generated test cases** (2 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `pay -> change -> soda -> serveSoda -> open -> take -> close`
- **test case 2**: `pay -> change -> cancel`


### Product 8

**Selected features:** selected = {f, t}

**Repaired FTS:** 4 states, 4 transitions (4 real / 0 `__end__`).

**State-coverage walk:** 3 transition step(s) — 3 unique greedy edge(s), 0 BFS-reroute segment(s). State coverage on the repaired FTS: **4/4 = 100.0%**.

![Walk overlay — product 8](SVM-product8-walk.png)

**Generated test cases** (1 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `free -> tea -> serveTea`


### Product 9

**Selected features:** selected = {c, f, s, t}

**Repaired FTS:** 6 states, 8 transitions (8 real / 0 `__end__`).

**State-coverage walk:** 10 transition step(s) — 3 unique greedy edge(s), 2 BFS-reroute segment(s). State coverage on the repaired FTS: **6/6 = 100.0%**.

![Walk overlay — product 9](SVM-product9-walk.png)

**Generated test cases** (3 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `free -> soda -> serveSoda -> take`
- **test case 2**: `free -> tea -> serveTea -> take`
- **test case 3**: `free -> cancel`


### Product 10

**Selected features:** selected = {c, s, t}

**Repaired FTS:** 9 states, 11 transitions (11 real / 0 `__end__`).

**State-coverage walk:** 17 transition step(s) — 6 unique greedy edge(s), 2 BFS-reroute segment(s). State coverage on the repaired FTS: **9/9 = 100.0%**.

![Walk overlay — product 10](SVM-product10-walk.png)

**Generated test cases** (3 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `pay -> change -> soda -> serveSoda -> open -> take -> close`
- **test case 2**: `pay -> change -> tea -> serveTea -> open -> take -> close`
- **test case 3**: `pay -> change -> cancel`


### Product 11

**Selected features:** selected = {c, t}

**Repaired FTS:** 8 states, 9 transitions (9 real / 0 `__end__`).

**State-coverage walk:** 10 transition step(s) — 6 unique greedy edge(s), 1 BFS-reroute segment(s). State coverage on the repaired FTS: **8/8 = 100.0%**.

![Walk overlay — product 11](SVM-product11-walk.png)

**Generated test cases** (2 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `pay -> change -> tea -> serveTea -> open -> take -> close`
- **test case 2**: `pay -> change -> cancel`


### Product 12

**Selected features:** selected = {f, s}

**Repaired FTS:** 4 states, 4 transitions (4 real / 0 `__end__`).

**State-coverage walk:** 3 transition step(s) — 3 unique greedy edge(s), 0 BFS-reroute segment(s). State coverage on the repaired FTS: **4/4 = 100.0%**.

![Walk overlay — product 12](SVM-product12-walk.png)

**Generated test cases** (1 trip(s) from initial back to initial, hidden synthetics removed):

- **test case 1**: `free -> soda -> serveSoda`

---

Total products: 12.
