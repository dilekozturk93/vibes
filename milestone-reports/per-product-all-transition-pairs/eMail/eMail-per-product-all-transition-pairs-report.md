# Per-Product All-Transition-Pairs Coverage — eMail

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

**Selected features:** selected = {e, s}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

**Pair graph (raw):** 13 nodes (incl. INIT), 31 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 13 nodes, 46 edges (15 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 1](eMail-product1-repaired.png)

![Pair graph (raw) — product 1](eMail-product1-pairgraph-raw.png)

![Pair graph (balanced) — product 1](eMail-product1-pairgraph-balanced.png)

**Generated test suite** — 10 test case(s) total (38 real step(s); pair-graph cycle has 46 edge(s) total, 15 synthetic dropped at translation).

- **test case 1**: `compose new email -> enter email subject -> enter email subject -> enter email body`
- **test case 2**: `send email`
- **test case 3**: `open mailbox -> select email`
- **test case 4**: `open mailbox`
- **test case 5**: `compose new email -> enter email body -> enter receiver's email address -> sign mail -> send email`
- **test case 6**: `compose new email -> enter receiver's email address`
- **test case 7**: `enter receiver's email address -> send email`
- **test case 8**: `compose new email`
- **test case 9**: `enter receiver's email address -> sign mail`
- **test case 10**: `enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 11**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 12**: `enter email subject -> enter email body -> enter email body -> enter email subject -> enter receiver's email address -> send email`
- **test case 13**: `open mailbox`
- **test case 14**: `open mailbox`


### Product 2

**Selected features:** selected = {ad, e}

**Repaired FTS:** 8 states, 15 transitions (14 real / 1 `__end__`).

**Pair graph (raw):** 16 nodes (incl. INIT), 40 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 16 nodes, 72 edges (32 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 2](eMail-product2-repaired.png)

![Pair graph (raw) — product 2](eMail-product2-pairgraph-raw.png)

![Pair graph (balanced) — product 2](eMail-product2-pairgraph-balanced.png)

**Generated test suite** — 21 test case(s) total (57 real step(s); pair-graph cycle has 72 edge(s) total, 32 synthetic dropped at translation).

- **test case 1**: `compose new email -> enter email subject -> enter email subject -> enter email body`
- **test case 2**: `enter receiver's email address -> get alias email addresses of receiver`
- **test case 3**: `send email`
- **test case 4**: `create an addressbook for a receiver`
- **test case 5**: `enter alias email addresses of receiver`
- **test case 6**: `create an addressbook for a receiver`
- **test case 7**: `enter alias email addresses of receiver`
- **test case 8**: `compose new email -> enter email body -> enter receiver's email address`
- **test case 9**: `send email`
- **test case 10**: `open mailbox -> select email`
- **test case 11**: `open mailbox`
- **test case 12**: `compose new email`
- **test case 13**: `enter receiver's email address -> send email`
- **test case 14**: `compose new email`
- **test case 15**: `enter receiver's email address -> get alias email addresses of receiver -> send email`
- **test case 16**: `compose new email`
- **test case 17**: `enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 18**: `enter receiver's email address -> send email`
- **test case 19**: `create an addressbook for a receiver`
- **test case 20**: `enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 21**: `compose new email -> enter receiver's email address`
- **test case 22**: `send email`
- **test case 23**: `open mailbox`
- **test case 24**: `create an addressbook for a receiver`
- **test case 25**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 26**: `enter email subject -> enter email body -> enter email body -> enter email subject -> enter receiver's email address`
- **test case 27**: `open mailbox`
- **test case 28**: `create an addressbook for a receiver -> enter the receiver's email address`
- **test case 29**: `enter alias email addresses of receiver`
- **test case 30**: `open mailbox`


### Product 3

**Selected features:** selected = {ad, au, e}

**Repaired FTS:** 10 states, 20 transitions (18 real / 2 `__end__`).

**Pair graph (raw):** 21 nodes (incl. INIT), 61 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 21 nodes, 148 edges (87 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 3](eMail-product3-repaired.png)

![Pair graph (raw) — product 3](eMail-product3-pairgraph-raw.png)

![Pair graph (balanced) — product 3](eMail-product3-pairgraph-balanced.png)

**Generated test suite** — 35 test case(s) total (88 real step(s); pair-graph cycle has 148 edge(s) total, 87 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `open mailbox -> select email`
- **test case 3**: `create an addressbook for a receiver`
- **test case 4**: `compose new email -> enter email body -> enter email subject -> enter email subject -> enter receiver's email address`
- **test case 5**: `send email`
- **test case 6**: `enter autoresponse email body`
- **test case 7**: `enter email autoresponse date interval`
- **test case 8**: `open mailbox`
- **test case 9**: `enter email autoresponse date interval`
- **test case 10**: `enter alias email addresses of receiver`
- **test case 11**: `compose new email -> enter email subject -> enter email body -> enter receiver's email address -> get alias email addresses of receiver`
- **test case 12**: `send email`
- **test case 13**: `create an addressbook for a receiver`
- **test case 14**: `send email`
- **test case 15**: `open mailbox`
- **test case 16**: `enter autoresponse email body`
- **test case 17**: `create an addressbook for a receiver`
- **test case 18**: `get alias email addresses of receiver -> send email`
- **test case 19**: `compose new email`
- **test case 20**: `enter receiver's email address -> send email`
- **test case 21**: `enter autoresponse email body`
- **test case 22**: `enter alias email addresses of receiver`
- **test case 23**: `enter email autoresponse date interval`
- **test case 24**: `send email`
- **test case 25**: `compose new email`
- **test case 26**: `send email`
- **test case 27**: `create an addressbook for a receiver -> enter the receiver's email address`
- **test case 28**: `enter alias email addresses of receiver`
- **test case 29**: `open mailbox`
- **test case 30**: `compose new email`
- **test case 31**: `send email`
- **test case 32**: `enter email autoresponse date interval`
- **test case 33**: `compose new email -> enter receiver's email address -> enter email subject -> enter email body -> enter email body`
- **test case 34**: `enter autoresponse email body -> enter email autoresponse date interval`
- **test case 35**: `enter email subject -> enter email subject`
- **test case 36**: `compose new email`
- **test case 37**: `enter receiver's email address -> enter receiver's email address -> send email`
- **test case 38**: `open mailbox`
- **test case 39**: `open mailbox`
- **test case 40**: `create an addressbook for a receiver`
- **test case 41**: `enter alias email addresses of receiver`
- **test case 42**: `create an addressbook for a receiver`
- **test case 43**: `enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 44**: `enter autoresponse email body`
- **test case 45**: `compose new email`
- **test case 46**: `enter receiver's email address -> enter email subject -> enter receiver's email address -> enter receiver's email address -> get alias email addresses of receiver`
- **test case 47**: `send email`
- **test case 48**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 49**: `enter autoresponse email body`
- **test case 50**: `enter email autoresponse date interval -> enter autoresponse email body`


### Product 4

**Selected features:** selected = {ad, au, e, en, s}

**Repaired FTS:** 11 states, 23 transitions (21 real / 2 `__end__`).

**Pair graph (raw):** 24 nodes (incl. INIT), 68 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 24 nodes, 162 edges (94 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 4](eMail-product4-repaired.png)

![Pair graph (raw) — product 4](eMail-product4-pairgraph-raw.png)

![Pair graph (balanced) — product 4](eMail-product4-pairgraph-balanced.png)

**Generated test suite** — 39 test case(s) total (99 real step(s); pair-graph cycle has 162 edge(s) total, 94 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `open mailbox -> select email`
- **test case 3**: `open mailbox`
- **test case 4**: `create an addressbook for a receiver`
- **test case 5**: `compose new email -> enter email body -> enter email subject -> enter email subject`
- **test case 6**: `enter receiver's email address -> sign mail -> send email`
- **test case 7**: `enter autoresponse email body`
- **test case 8**: `open mailbox`
- **test case 9**: `enter email autoresponse date interval`
- **test case 10**: `enter autoresponse email body -> enter email autoresponse date interval`
- **test case 11**: `enter email autoresponse date interval`
- **test case 12**: `compose new email -> enter email subject -> enter receiver's email address -> get alias email addresses of receiver -> send email`
- **test case 13**: `create an addressbook for a receiver`
- **test case 14**: `enter alias email addresses of receiver`
- **test case 15**: `compose new email`
- **test case 16**: `enter receiver's email address -> send email`
- **test case 17**: `enter autoresponse email body`
- **test case 18**: `create an addressbook for a receiver`
- **test case 19**: `enter alias email addresses of receiver`
- **test case 20**: `enter email autoresponse date interval`
- **test case 21**: `send email`
- **test case 22**: `compose new email`
- **test case 23**: `send email`
- **test case 24**: `create an addressbook for a receiver`
- **test case 25**: `enter alias email addresses of receiver`
- **test case 26**: `open mailbox`
- **test case 27**: `send email`
- **test case 28**: `enter email autoresponse date interval`
- **test case 29**: `send email`
- **test case 30**: `open mailbox`
- **test case 31**: `compose new email`
- **test case 32**: `enter receiver's email address -> enter email subject -> enter email body -> enter receiver's email address`
- **test case 33**: `enter email subject -> enter email subject -> enter email body -> enter email body`
- **test case 34**: `enter autoresponse email body`
- **test case 35**: `compose new email -> enter receiver's email address`
- **test case 36**: `enter receiver's email address -> sign mail`
- **test case 37**: `send email`
- **test case 38**: `open mailbox`
- **test case 39**: `enter receiver's email address -> get receiver's public key -> encrypt mail with receiver's public key -> send email`
- **test case 40**: `compose new email`
- **test case 41**: `enter receiver's email address -> enter receiver's email address -> send email`
- **test case 42**: `open mailbox`
- **test case 43**: `create an addressbook for a receiver -> enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 44**: `create an addressbook for a receiver`
- **test case 45**: `enter alias email addresses of receiver`
- **test case 46**: `enter autoresponse email body`
- **test case 47**: `enter receiver's email address -> enter receiver's email address -> get alias email addresses of receiver`
- **test case 48**: `send email`
- **test case 49**: `compose new email`
- **test case 50**: `enter receiver's email address -> enter email subject -> enter receiver's email address -> get receiver's public key`
- **test case 51**: `send email`
- **test case 52**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 53**: `enter autoresponse email body`
- **test case 54**: `enter email autoresponse date interval -> enter autoresponse email body`


### Product 5

**Selected features:** selected = {e, en, s}

**Repaired FTS:** 7 states, 14 transitions (13 real / 1 `__end__`).

**Pair graph (raw):** 15 nodes (incl. INIT), 35 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 15 nodes, 59 edges (24 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 5](eMail-product5-repaired.png)

![Pair graph (raw) — product 5](eMail-product5-pairgraph-raw.png)

![Pair graph (balanced) — product 5](eMail-product5-pairgraph-balanced.png)

**Generated test suite** — 15 test case(s) total (47 real step(s); pair-graph cycle has 59 edge(s) total, 24 synthetic dropped at translation).

- **test case 1**: `compose new email -> enter email subject -> enter email subject -> enter email body`
- **test case 2**: `enter receiver's email address -> sign mail -> send email`
- **test case 3**: `open mailbox -> select email`
- **test case 4**: `open mailbox`
- **test case 5**: `compose new email -> enter email body -> enter receiver's email address`
- **test case 6**: `enter receiver's email address -> send email`
- **test case 7**: `compose new email`
- **test case 8**: `enter receiver's email address -> sign mail`
- **test case 9**: `send email`
- **test case 10**: `compose new email -> enter receiver's email address`
- **test case 11**: `enter receiver's email address -> get receiver's public key -> encrypt mail with receiver's public key -> send email`
- **test case 12**: `enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 13**: `enter receiver's email address -> get receiver's public key`
- **test case 14**: `enter receiver's email address -> send email`
- **test case 15**: `open mailbox`
- **test case 16**: `compose new email`
- **test case 17**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 18**: `enter email subject -> enter email body -> enter email body -> enter email subject -> enter receiver's email address`
- **test case 19**: `open mailbox`


### Product 6

**Selected features:** selected = {au, e, en, s}

**Repaired FTS:** 9 states, 19 transitions (17 real / 2 `__end__`).

**Pair graph (raw):** 20 nodes (incl. INIT), 53 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 20 nodes, 110 edges (57 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 6](eMail-product6-repaired.png)

![Pair graph (raw) — product 6](eMail-product6-pairgraph-raw.png)

![Pair graph (balanced) — product 6](eMail-product6-pairgraph-balanced.png)

**Generated test suite** — 28 test case(s) total (74 real step(s); pair-graph cycle has 110 edge(s) total, 57 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `open mailbox -> select email`
- **test case 3**: `enter email autoresponse date interval`
- **test case 4**: `open mailbox`
- **test case 5**: `enter email autoresponse date interval`
- **test case 6**: `compose new email -> enter email body -> enter email subject -> enter email subject`
- **test case 7**: `send email`
- **test case 8**: `enter autoresponse email body`
- **test case 9**: `compose new email -> enter email subject -> enter receiver's email address -> sign mail -> send email`
- **test case 10**: `open mailbox`
- **test case 11**: `enter autoresponse email body`
- **test case 12**: `enter receiver's email address -> send email`
- **test case 13**: `enter autoresponse email body -> enter email autoresponse date interval`
- **test case 14**: `send email`
- **test case 15**: `compose new email -> enter receiver's email address`
- **test case 16**: `send email`
- **test case 17**: `enter email autoresponse date interval`
- **test case 18**: `compose new email`
- **test case 19**: `send email`
- **test case 20**: `open mailbox`
- **test case 21**: `enter receiver's email address -> enter email subject -> enter email body -> enter receiver's email address`
- **test case 22**: `enter email subject -> enter email subject -> enter email body -> enter email body`
- **test case 23**: `enter autoresponse email body`
- **test case 24**: `enter receiver's email address -> sign mail`
- **test case 25**: `compose new email`
- **test case 26**: `enter receiver's email address -> get receiver's public key -> encrypt mail with receiver's public key`
- **test case 27**: `send email`
- **test case 28**: `compose new email`
- **test case 29**: `enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> send email`
- **test case 30**: `compose new email`
- **test case 31**: `enter receiver's email address -> enter email subject -> enter receiver's email address -> get receiver's public key`
- **test case 32**: `encrypt mail with receiver's public key -> send email`
- **test case 33**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 34**: `enter autoresponse email body`
- **test case 35**: `enter email autoresponse date interval -> enter autoresponse email body`
- **test case 36**: `open mailbox`


### Product 7

**Selected features:** selected = {ad, au, e, f, s}

**Repaired FTS:** 10 states, 22 transitions (20 real / 2 `__end__`).

**Pair graph (raw):** 23 nodes (incl. INIT), 66 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 23 nodes, 161 edges (95 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 7](eMail-product7-repaired.png)

![Pair graph (raw) — product 7](eMail-product7-pairgraph-raw.png)

![Pair graph (balanced) — product 7](eMail-product7-pairgraph-balanced.png)

**Generated test suite** — 38 test case(s) total (96 real step(s); pair-graph cycle has 161 edge(s) total, 95 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `open mailbox -> select email`
- **test case 3**: `enter forward receiver's email address -> send email`
- **test case 4**: `enter autoresponse email body`
- **test case 5**: `send email`
- **test case 6**: `create an addressbook for a receiver`
- **test case 7**: `compose new email -> enter email body -> enter email subject -> enter email subject`
- **test case 8**: `enter receiver's email address -> sign mail -> send email`
- **test case 9**: `open mailbox`
- **test case 10**: `select email -> enter forward receiver's email address`
- **test case 11**: `open mailbox`
- **test case 12**: `create an addressbook for a receiver`
- **test case 13**: `enter alias email addresses of receiver`
- **test case 14**: `compose new email -> enter email subject -> enter receiver's email address -> get alias email addresses of receiver`
- **test case 15**: `send email`
- **test case 16**: `compose new email`
- **test case 17**: `enter receiver's email address -> send email`
- **test case 18**: `enter autoresponse email body -> enter email autoresponse date interval`
- **test case 19**: `enter email autoresponse date interval`
- **test case 20**: `open mailbox`
- **test case 21**: `enter autoresponse email body`
- **test case 22**: `enter email autoresponse date interval`
- **test case 23**: `send email`
- **test case 24**: `compose new email`
- **test case 25**: `send email`
- **test case 26**: `create an addressbook for a receiver`
- **test case 27**: `send email`
- **test case 28**: `enter email autoresponse date interval`
- **test case 29**: `create an addressbook for a receiver`
- **test case 30**: `enter alias email addresses of receiver`
- **test case 31**: `enter email autoresponse date interval`
- **test case 32**: `enter alias email addresses of receiver`
- **test case 33**: `open mailbox`
- **test case 34**: `compose new email`
- **test case 35**: `send email`
- **test case 36**: `open mailbox`
- **test case 37**: `compose new email -> enter receiver's email address -> enter email subject -> enter email body -> enter receiver's email address`
- **test case 38**: `enter email subject -> enter email subject -> enter email body -> enter email body`
- **test case 39**: `enter autoresponse email body`
- **test case 40**: `enter receiver's email address -> sign mail`
- **test case 41**: `compose new email`
- **test case 42**: `enter receiver's email address -> enter receiver's email address -> send email`
- **test case 43**: `open mailbox`
- **test case 44**: `create an addressbook for a receiver`
- **test case 45**: `enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 46**: `create an addressbook for a receiver -> enter the receiver's email address`
- **test case 47**: `enter alias email addresses of receiver`
- **test case 48**: `enter autoresponse email body`
- **test case 49**: `compose new email`
- **test case 50**: `enter receiver's email address -> enter email subject -> enter receiver's email address -> enter receiver's email address -> get alias email addresses of receiver -> send email`
- **test case 51**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 52**: `enter autoresponse email body`
- **test case 53**: `enter email autoresponse date interval -> enter autoresponse email body`


### Product 8

**Selected features:** selected = {e, f, s}

**Repaired FTS:** 6 states, 13 transitions (12 real / 1 `__end__`).

**Pair graph (raw):** 14 nodes (incl. INIT), 33 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 14 nodes, 49 edges (16 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 8](eMail-product8-repaired.png)

![Pair graph (raw) — product 8](eMail-product8-pairgraph-raw.png)

![Pair graph (balanced) — product 8](eMail-product8-pairgraph-balanced.png)

**Generated test suite** — 13 test case(s) total (43 real step(s); pair-graph cycle has 49 edge(s) total, 16 synthetic dropped at translation).

- **test case 1**: `compose new email -> enter email subject -> enter email subject -> enter email body`
- **test case 2**: `enter receiver's email address -> sign mail -> send email`
- **test case 3**: `open mailbox -> select email`
- **test case 4**: `open mailbox`
- **test case 5**: `compose new email -> enter email body -> enter receiver's email address`
- **test case 6**: `enter receiver's email address -> send email`
- **test case 7**: `compose new email -> enter receiver's email address`
- **test case 8**: `enter receiver's email address -> sign mail`
- **test case 9**: `send email`
- **test case 10**: `compose new email`
- **test case 11**: `enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 12**: `enter receiver's email address -> send email`
- **test case 13**: `open mailbox`
- **test case 14**: `select email -> enter forward receiver's email address -> send email`
- **test case 15**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 16**: `enter email subject -> enter email body -> enter email body -> enter email subject -> enter receiver's email address`
- **test case 17**: `open mailbox`


### Product 9

**Selected features:** selected = {au, e}

**Repaired FTS:** 7 states, 15 transitions (13 real / 2 `__end__`).

**Pair graph (raw):** 16 nodes (incl. INIT), 42 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 16 nodes, 76 edges (34 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 9](eMail-product9-repaired.png)

![Pair graph (raw) — product 9](eMail-product9-pairgraph-raw.png)

![Pair graph (balanced) — product 9](eMail-product9-pairgraph-balanced.png)

**Generated test suite** — 19 test case(s) total (54 real step(s); pair-graph cycle has 76 edge(s) total, 34 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `enter autoresponse email body -> enter email autoresponse date interval`
- **test case 3**: `open mailbox -> select email`
- **test case 4**: `open mailbox`
- **test case 5**: `compose new email -> enter email subject -> enter email subject -> enter email body -> enter receiver's email address`
- **test case 6**: `enter receiver's email address -> send email`
- **test case 7**: `enter email autoresponse date interval`
- **test case 8**: `enter email autoresponse date interval`
- **test case 9**: `compose new email -> enter email body -> enter email body -> enter email subject`
- **test case 10**: `enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject -> enter receiver's email address -> send email`
- **test case 11**: `enter autoresponse email body`
- **test case 12**: `send email`
- **test case 13**: `compose new email -> enter receiver's email address`
- **test case 14**: `compose new email`
- **test case 15**: `send email`
- **test case 16**: `open mailbox`
- **test case 17**: `open mailbox`
- **test case 18**: `enter autoresponse email body`
- **test case 19**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 20**: `enter autoresponse email body`
- **test case 21**: `enter email autoresponse date interval -> enter autoresponse email body`
- **test case 22**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 23**: `enter email subject -> enter email body`


### Product 10

**Selected features:** selected = {ad, e, f}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

**Pair graph (raw):** 17 nodes (incl. INIT), 42 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 17 nodes, 76 edges (34 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 10](eMail-product10-repaired.png)

![Pair graph (raw) — product 10](eMail-product10-pairgraph-raw.png)

![Pair graph (balanced) — product 10](eMail-product10-pairgraph-balanced.png)

**Generated test suite** — 23 test case(s) total (61 real step(s); pair-graph cycle has 76 edge(s) total, 34 synthetic dropped at translation).

- **test case 1**: `compose new email -> enter email subject -> enter email subject -> enter email body`
- **test case 2**: `enter receiver's email address -> get alias email addresses of receiver`
- **test case 3**: `send email`
- **test case 4**: `create an addressbook for a receiver`
- **test case 5**: `enter alias email addresses of receiver`
- **test case 6**: `create an addressbook for a receiver`
- **test case 7**: `enter alias email addresses of receiver`
- **test case 8**: `compose new email -> enter email body -> enter receiver's email address`
- **test case 9**: `enter receiver's email address -> send email`
- **test case 10**: `compose new email`
- **test case 11**: `enter receiver's email address -> get alias email addresses of receiver -> send email`
- **test case 12**: `open mailbox -> select email`
- **test case 13**: `open mailbox`
- **test case 14**: `compose new email`
- **test case 15**: `enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 16**: `enter receiver's email address -> send email`
- **test case 17**: `create an addressbook for a receiver`
- **test case 18**: `enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 19**: `compose new email -> enter receiver's email address`
- **test case 20**: `send email`
- **test case 21**: `open mailbox`
- **test case 22**: `create an addressbook for a receiver`
- **test case 23**: `enter forward receiver's email address -> send email`
- **test case 24**: `select email -> enter forward receiver's email address`
- **test case 25**: `send email`
- **test case 26**: `compose new email`
- **test case 27**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 28**: `enter email subject -> enter email body -> enter email body -> enter email subject -> enter receiver's email address`
- **test case 29**: `open mailbox`
- **test case 30**: `create an addressbook for a receiver -> enter the receiver's email address`
- **test case 31**: `enter alias email addresses of receiver`
- **test case 32**: `open mailbox`


### Product 11

**Selected features:** selected = {ad, au, e, f}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

**Pair graph (raw):** 22 nodes (incl. INIT), 63 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 22 nodes, 152 edges (89 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 11](eMail-product11-repaired.png)

![Pair graph (raw) — product 11](eMail-product11-pairgraph-raw.png)

![Pair graph (balanced) — product 11](eMail-product11-pairgraph-balanced.png)

**Generated test suite** — 35 test case(s) total (90 real step(s); pair-graph cycle has 152 edge(s) total, 89 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `enter forward receiver's email address -> send email`
- **test case 3**: `enter autoresponse email body`
- **test case 4**: `open mailbox -> select email -> enter forward receiver's email address`
- **test case 5**: `send email`
- **test case 6**: `create an addressbook for a receiver`
- **test case 7**: `compose new email -> enter email body -> enter email subject -> enter email subject -> enter receiver's email address -> get alias email addresses of receiver -> send email`
- **test case 8**: `open mailbox`
- **test case 9**: `send email`
- **test case 10**: `compose new email -> enter email subject -> enter email body -> enter receiver's email address -> send email`
- **test case 11**: `enter autoresponse email body`
- **test case 12**: `open mailbox`
- **test case 13**: `create an addressbook for a receiver`
- **test case 14**: `enter alias email addresses of receiver`
- **test case 15**: `compose new email`
- **test case 16**: `send email`
- **test case 17**: `compose new email`
- **test case 18**: `send email`
- **test case 19**: `create an addressbook for a receiver`
- **test case 20**: `send email`
- **test case 21**: `enter email autoresponse date interval`
- **test case 22**: `enter email autoresponse date interval`
- **test case 23**: `open mailbox`
- **test case 24**: `enter autoresponse email body`
- **test case 25**: `enter email autoresponse date interval`
- **test case 26**: `compose new email -> enter receiver's email address -> enter email subject -> enter email body -> enter email body`
- **test case 27**: `enter autoresponse email body -> enter email autoresponse date interval`
- **test case 28**: `create an addressbook for a receiver`
- **test case 29**: `enter alias email addresses of receiver`
- **test case 30**: `enter email autoresponse date interval`
- **test case 31**: `create an addressbook for a receiver -> enter the receiver's email address`
- **test case 32**: `enter alias email addresses of receiver`
- **test case 33**: `open mailbox`
- **test case 34**: `compose new email`
- **test case 35**: `enter email subject -> enter email subject`
- **test case 36**: `compose new email`
- **test case 37**: `enter receiver's email address -> enter receiver's email address -> send email`
- **test case 38**: `open mailbox`
- **test case 39**: `open mailbox`
- **test case 40**: `create an addressbook for a receiver`
- **test case 41**: `enter alias email addresses of receiver`
- **test case 42**: `create an addressbook for a receiver`
- **test case 43**: `enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 44**: `enter autoresponse email body`
- **test case 45**: `compose new email`
- **test case 46**: `enter receiver's email address -> enter email subject -> enter receiver's email address -> enter receiver's email address -> get alias email addresses of receiver`
- **test case 47**: `send email`
- **test case 48**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 49**: `enter autoresponse email body`
- **test case 50**: `enter email autoresponse date interval -> enter autoresponse email body`


### Product 12

**Selected features:** selected = {ad, e, en}

**Repaired FTS:** 9 states, 17 transitions (16 real / 1 `__end__`).

**Pair graph (raw):** 18 nodes (incl. INIT), 44 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 18 nodes, 76 edges (32 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 12](eMail-product12-repaired.png)

![Pair graph (raw) — product 12](eMail-product12-pairgraph-raw.png)

![Pair graph (balanced) — product 12](eMail-product12-pairgraph-balanced.png)

**Generated test suite** — 22 test case(s) total (62 real step(s); pair-graph cycle has 76 edge(s) total, 32 synthetic dropped at translation).

- **test case 1**: `compose new email -> enter email subject -> enter email subject -> enter email body`
- **test case 2**: `enter receiver's email address -> get alias email addresses of receiver`
- **test case 3**: `send email`
- **test case 4**: `create an addressbook for a receiver`
- **test case 5**: `enter alias email addresses of receiver`
- **test case 6**: `create an addressbook for a receiver`
- **test case 7**: `compose new email -> enter email body -> enter receiver's email address`
- **test case 8**: `enter receiver's email address -> send email`
- **test case 9**: `compose new email`
- **test case 10**: `enter receiver's email address -> get alias email addresses of receiver -> send email`
- **test case 11**: `enter receiver's email address -> get receiver's public key -> encrypt mail with receiver's public key`
- **test case 12**: `send email`
- **test case 13**: `open mailbox`
- **test case 14**: `open mailbox`
- **test case 15**: `compose new email -> enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 16**: `enter receiver's email address -> get receiver's public key`
- **test case 17**: `encrypt mail with receiver's public key -> send email`
- **test case 18**: `compose new email`
- **test case 19**: `enter receiver's email address -> send email`
- **test case 20**: `create an addressbook for a receiver -> enter the receiver's email address`
- **test case 21**: `enter alias email addresses of receiver`
- **test case 22**: `compose new email`
- **test case 23**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 24**: `enter email subject -> enter email body -> enter email body -> enter email subject -> enter receiver's email address`
- **test case 25**: `send email`
- **test case 26**: `open mailbox -> select email`
- **test case 27**: `create an addressbook for a receiver`
- **test case 28**: `enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 29**: `open mailbox`
- **test case 30**: `open mailbox`
- **test case 31**: `create an addressbook for a receiver`


### Product 13

**Selected features:** selected = {ad, e, s}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

**Pair graph (raw):** 17 nodes (incl. INIT), 43 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 17 nodes, 77 edges (34 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 13](eMail-product13-repaired.png)

![Pair graph (raw) — product 13](eMail-product13-pairgraph-raw.png)

![Pair graph (balanced) — product 13](eMail-product13-pairgraph-balanced.png)

**Generated test suite** — 20 test case(s) total (59 real step(s); pair-graph cycle has 77 edge(s) total, 34 synthetic dropped at translation).

- **test case 1**: `compose new email -> enter email subject -> enter email subject -> enter email body`
- **test case 2**: `enter receiver's email address -> sign mail -> send email`
- **test case 3**: `create an addressbook for a receiver`
- **test case 4**: `enter alias email addresses of receiver`
- **test case 5**: `create an addressbook for a receiver`
- **test case 6**: `enter alias email addresses of receiver`
- **test case 7**: `compose new email -> enter email body -> enter receiver's email address -> get alias email addresses of receiver`
- **test case 8**: `send email`
- **test case 9**: `open mailbox -> select email`
- **test case 10**: `open mailbox`
- **test case 11**: `compose new email`
- **test case 12**: `enter receiver's email address -> send email`
- **test case 13**: `compose new email`
- **test case 14**: `enter receiver's email address -> sign mail`
- **test case 15**: `send email`
- **test case 16**: `compose new email`
- **test case 17**: `enter receiver's email address -> get alias email addresses of receiver -> send email`
- **test case 18**: `enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 19**: `enter receiver's email address -> send email`
- **test case 20**: `create an addressbook for a receiver`
- **test case 21**: `compose new email -> enter receiver's email address`
- **test case 22**: `create an addressbook for a receiver`
- **test case 23**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 24**: `enter email subject -> enter email body -> enter email body -> enter email subject -> enter receiver's email address`
- **test case 25**: `send email`
- **test case 26**: `open mailbox`
- **test case 27**: `open mailbox`
- **test case 28**: `create an addressbook for a receiver -> enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 29**: `open mailbox`


### Product 14

**Selected features:** selected = {au, e, f, s}

**Repaired FTS:** 8 states, 18 transitions (16 real / 2 `__end__`).

**Pair graph (raw):** 19 nodes (incl. INIT), 51 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 19 nodes, 110 edges (59 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 14](eMail-product14-repaired.png)

![Pair graph (raw) — product 14](eMail-product14-pairgraph-raw.png)

![Pair graph (balanced) — product 14](eMail-product14-pairgraph-balanced.png)

**Generated test suite** — 25 test case(s) total (69 real step(s); pair-graph cycle has 110 edge(s) total, 59 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `enter forward receiver's email address -> send email`
- **test case 3**: `enter autoresponse email body`
- **test case 4**: `select email -> enter forward receiver's email address`
- **test case 5**: `send email`
- **test case 6**: `open mailbox`
- **test case 7**: `send email`
- **test case 8**: `compose new email -> enter email body -> enter email subject -> enter email subject`
- **test case 9**: `enter receiver's email address -> sign mail -> send email`
- **test case 10**: `enter email autoresponse date interval`
- **test case 11**: `open mailbox -> select email`
- **test case 12**: `enter email autoresponse date interval`
- **test case 13**: `open mailbox`
- **test case 14**: `enter autoresponse email body`
- **test case 15**: `enter email autoresponse date interval`
- **test case 16**: `compose new email -> enter email subject -> enter receiver's email address -> send email`
- **test case 17**: `enter autoresponse email body`
- **test case 18**: `send email`
- **test case 19**: `compose new email -> enter receiver's email address`
- **test case 20**: `send email`
- **test case 21**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 22**: `compose new email`
- **test case 23**: `send email`
- **test case 24**: `open mailbox`
- **test case 25**: `enter receiver's email address -> enter email subject -> enter email body -> enter receiver's email address`
- **test case 26**: `enter email subject -> enter email subject -> enter email body -> enter email body`
- **test case 27**: `enter autoresponse email body`
- **test case 28**: `enter autoresponse email body -> enter email autoresponse date interval -> enter autoresponse email body`
- **test case 29**: `compose new email`
- **test case 30**: `enter receiver's email address -> sign mail`
- **test case 31**: `open mailbox`
- **test case 32**: `compose new email`
- **test case 33**: `enter receiver's email address -> enter receiver's email address -> enter email subject -> enter receiver's email address -> enter receiver's email address -> send email`


### Product 15

**Selected features:** selected = {e, en}

**Repaired FTS:** 7 states, 13 transitions (12 real / 1 `__end__`).

**Pair graph (raw):** 14 nodes (incl. INIT), 32 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 14 nodes, 49 edges (17 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 15](eMail-product15-repaired.png)

![Pair graph (raw) — product 15](eMail-product15-pairgraph-raw.png)

![Pair graph (balanced) — product 15](eMail-product15-pairgraph-balanced.png)

**Generated test suite** — 11 test case(s) total (40 real step(s); pair-graph cycle has 49 edge(s) total, 17 synthetic dropped at translation).

- **test case 1**: `compose new email -> enter email subject -> enter email subject -> enter email body`
- **test case 2**: `enter receiver's email address -> send email`
- **test case 3**: `compose new email -> enter email body -> enter receiver's email address`
- **test case 4**: `enter receiver's email address -> get receiver's public key -> encrypt mail with receiver's public key -> send email`
- **test case 5**: `open mailbox -> select email`
- **test case 6**: `open mailbox`
- **test case 7**: `compose new email`
- **test case 8**: `enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 9**: `compose new email -> enter receiver's email address -> get receiver's public key`
- **test case 10**: `send email`
- **test case 11**: `compose new email`
- **test case 12**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 13**: `enter email subject -> enter email body -> enter email body -> enter email subject -> enter receiver's email address -> send email`
- **test case 14**: `open mailbox`
- **test case 15**: `open mailbox`


### Product 16

**Selected features:** selected = {au, e, f}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

**Pair graph (raw):** 18 nodes (incl. INIT), 48 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 18 nodes, 107 edges (59 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 16](eMail-product16-repaired.png)

![Pair graph (raw) — product 16](eMail-product16-pairgraph-raw.png)

![Pair graph (balanced) — product 16](eMail-product16-pairgraph-balanced.png)

**Generated test suite** — 26 test case(s) total (67 real step(s); pair-graph cycle has 107 edge(s) total, 59 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `enter autoresponse email body`
- **test case 3**: `open mailbox`
- **test case 4**: `compose new email -> enter email subject -> enter email subject -> enter email body -> enter receiver's email address`
- **test case 5**: `enter receiver's email address -> send email`
- **test case 6**: `enter email autoresponse date interval`
- **test case 7**: `open mailbox -> select email`
- **test case 8**: `compose new email -> enter email body -> enter email body -> enter email subject -> enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 9**: `compose new email`
- **test case 10**: `enter receiver's email address -> send email`
- **test case 11**: `enter autoresponse email body`
- **test case 12**: `enter email autoresponse date interval`
- **test case 13**: `open mailbox`
- **test case 14**: `enter forward receiver's email address -> send email`
- **test case 15**: `open mailbox`
- **test case 16**: `send email`
- **test case 17**: `compose new email`
- **test case 18**: `send email`
- **test case 19**: `compose new email -> enter receiver's email address`
- **test case 20**: `send email`
- **test case 21**: `open mailbox`
- **test case 22**: `select email -> enter forward receiver's email address`
- **test case 23**: `send email`
- **test case 24**: `enter autoresponse email body -> enter email autoresponse date interval`
- **test case 25**: `send email`
- **test case 26**: `enter email autoresponse date interval`
- **test case 27**: `compose new email`
- **test case 28**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 29**: `enter email subject -> enter email body`
- **test case 30**: `open mailbox`
- **test case 31**: `enter autoresponse email body`
- **test case 32**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 33**: `enter autoresponse email body`
- **test case 34**: `enter email autoresponse date interval -> enter autoresponse email body`


### Product 17

**Selected features:** selected = {ad, e, f, s}

**Repaired FTS:** 8 states, 17 transitions (16 real / 1 `__end__`).

**Pair graph (raw):** 18 nodes (incl. INIT), 45 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 18 nodes, 81 edges (36 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 17](eMail-product17-repaired.png)

![Pair graph (raw) — product 17](eMail-product17-pairgraph-raw.png)

![Pair graph (balanced) — product 17](eMail-product17-pairgraph-balanced.png)

**Generated test suite** — 20 test case(s) total (61 real step(s); pair-graph cycle has 81 edge(s) total, 36 synthetic dropped at translation).

- **test case 1**: `compose new email -> enter email subject -> enter email subject -> enter email body`
- **test case 2**: `enter receiver's email address -> sign mail -> send email`
- **test case 3**: `create an addressbook for a receiver`
- **test case 4**: `enter alias email addresses of receiver`
- **test case 5**: `create an addressbook for a receiver`
- **test case 6**: `enter alias email addresses of receiver`
- **test case 7**: `compose new email -> enter email body -> enter receiver's email address -> get alias email addresses of receiver`
- **test case 8**: `send email`
- **test case 9**: `open mailbox`
- **test case 10**: `open mailbox -> select email`
- **test case 11**: `compose new email`
- **test case 12**: `enter receiver's email address -> send email`
- **test case 13**: `compose new email`
- **test case 14**: `enter receiver's email address -> sign mail`
- **test case 15**: `create an addressbook for a receiver`
- **test case 16**: `enter receiver's email address -> get alias email addresses of receiver -> send email`
- **test case 17**: `compose new email`
- **test case 18**: `enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 19**: `enter receiver's email address -> send email`
- **test case 20**: `create an addressbook for a receiver`
- **test case 21**: `compose new email -> enter receiver's email address`
- **test case 22**: `select email -> enter forward receiver's email address -> send email`
- **test case 23**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 24**: `enter email subject -> enter email body -> enter email body -> enter email subject -> enter receiver's email address`
- **test case 25**: `send email`
- **test case 26**: `open mailbox`
- **test case 27**: `open mailbox`
- **test case 28**: `create an addressbook for a receiver -> enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 29**: `open mailbox`


### Product 18

**Selected features:** selected = {ad, au, e, en}

**Repaired FTS:** 11 states, 22 transitions (20 real / 2 `__end__`).

**Pair graph (raw):** 23 nodes (incl. INIT), 65 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 23 nodes, 157 edges (92 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 18](eMail-product18-repaired.png)

![Pair graph (raw) — product 18](eMail-product18-pairgraph-raw.png)

![Pair graph (balanced) — product 18](eMail-product18-pairgraph-balanced.png)

**Generated test suite** — 39 test case(s) total (96 real step(s); pair-graph cycle has 157 edge(s) total, 92 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `open mailbox -> select email`
- **test case 3**: `open mailbox`
- **test case 4**: `create an addressbook for a receiver`
- **test case 5**: `compose new email -> enter email body -> enter email subject -> enter email subject`
- **test case 6**: `enter receiver's email address -> get alias email addresses of receiver`
- **test case 7**: `send email`
- **test case 8**: `enter autoresponse email body`
- **test case 9**: `enter email autoresponse date interval`
- **test case 10**: `open mailbox`
- **test case 11**: `enter autoresponse email body`
- **test case 12**: `enter email autoresponse date interval`
- **test case 13**: `enter alias email addresses of receiver`
- **test case 14**: `compose new email -> enter email subject -> enter receiver's email address`
- **test case 15**: `send email`
- **test case 16**: `create an addressbook for a receiver`
- **test case 17**: `enter receiver's email address -> send email`
- **test case 18**: `enter autoresponse email body`
- **test case 19**: `create an addressbook for a receiver`
- **test case 20**: `send email`
- **test case 21**: `compose new email`
- **test case 22**: `send email`
- **test case 23**: `create an addressbook for a receiver`
- **test case 24**: `enter alias email addresses of receiver`
- **test case 25**: `enter email autoresponse date interval`
- **test case 26**: `enter alias email addresses of receiver`
- **test case 27**: `open mailbox`
- **test case 28**: `compose new email`
- **test case 29**: `send email`
- **test case 30**: `enter email autoresponse date interval`
- **test case 31**: `send email`
- **test case 32**: `open mailbox`
- **test case 33**: `enter receiver's email address -> enter email subject -> enter email body -> enter receiver's email address`
- **test case 34**: `enter email subject -> enter email subject -> enter email body -> enter email body`
- **test case 35**: `enter autoresponse email body`
- **test case 36**: `compose new email -> enter receiver's email address`
- **test case 37**: `enter receiver's email address -> get receiver's public key -> encrypt mail with receiver's public key`
- **test case 38**: `send email`
- **test case 39**: `open mailbox`
- **test case 40**: `compose new email`
- **test case 41**: `enter receiver's email address -> enter receiver's email address -> send email`
- **test case 42**: `open mailbox`
- **test case 43**: `create an addressbook for a receiver -> enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 44**: `create an addressbook for a receiver`
- **test case 45**: `enter alias email addresses of receiver`
- **test case 46**: `enter autoresponse email body -> enter email autoresponse date interval`
- **test case 47**: `compose new email`
- **test case 48**: `enter receiver's email address -> get alias email addresses of receiver -> send email`
- **test case 49**: `compose new email`
- **test case 50**: `enter receiver's email address -> enter receiver's email address -> enter email subject -> enter receiver's email address -> get receiver's public key`
- **test case 51**: `encrypt mail with receiver's public key -> send email`
- **test case 52**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 53**: `enter autoresponse email body`
- **test case 54**: `enter email autoresponse date interval -> enter autoresponse email body`


### Product 19

**Selected features:** selected = {au, e, en}

**Repaired FTS:** 9 states, 18 transitions (16 real / 2 `__end__`).

**Pair graph (raw):** 19 nodes (incl. INIT), 50 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 19 nodes, 109 edges (59 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 19](eMail-product19-repaired.png)

![Pair graph (raw) — product 19](eMail-product19-pairgraph-raw.png)

![Pair graph (balanced) — product 19](eMail-product19-pairgraph-balanced.png)

**Generated test suite** — 30 test case(s) total (73 real step(s); pair-graph cycle has 109 edge(s) total, 59 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `open mailbox`
- **test case 3**: `enter email autoresponse date interval`
- **test case 4**: `open mailbox -> select email`
- **test case 5**: `enter email autoresponse date interval`
- **test case 6**: `compose new email -> enter email body -> enter email subject -> enter email subject`
- **test case 7**: `enter receiver's email address -> send email`
- **test case 8**: `enter autoresponse email body`
- **test case 9**: `compose new email -> enter email subject -> enter receiver's email address`
- **test case 10**: `send email`
- **test case 11**: `compose new email`
- **test case 12**: `send email`
- **test case 13**: `enter email autoresponse date interval`
- **test case 14**: `send email`
- **test case 15**: `open mailbox`
- **test case 16**: `enter autoresponse email body`
- **test case 17**: `compose new email`
- **test case 18**: `enter receiver's email address -> enter email subject -> enter email body -> enter receiver's email address`
- **test case 19**: `enter email subject -> enter email subject -> enter email body -> enter email body`
- **test case 20**: `enter autoresponse email body -> enter email autoresponse date interval`
- **test case 21**: `compose new email -> enter receiver's email address`
- **test case 22**: `enter receiver's email address -> get receiver's public key -> encrypt mail with receiver's public key`
- **test case 23**: `send email`
- **test case 24**: `enter autoresponse email body`
- **test case 25**: `enter receiver's email address -> enter receiver's email address -> send email`
- **test case 26**: `compose new email`
- **test case 27**: `enter receiver's email address -> enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 28**: `encrypt mail with receiver's public key -> send email`
- **test case 29**: `open mailbox`
- **test case 30**: `compose new email`
- **test case 31**: `send email`
- **test case 32**: `compose new email`
- **test case 33**: `enter receiver's email address -> get receiver's public key`
- **test case 34**: `send email`
- **test case 35**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 36**: `enter autoresponse email body`
- **test case 37**: `enter email autoresponse date interval -> enter autoresponse email body`
- **test case 38**: `open mailbox`


### Product 20

**Selected features:** selected = {e, f}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

**Pair graph (raw):** 13 nodes (incl. INIT), 30 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 13 nodes, 47 edges (17 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 20](eMail-product20-repaired.png)

![Pair graph (raw) — product 20](eMail-product20-pairgraph-raw.png)

![Pair graph (balanced) — product 20](eMail-product20-pairgraph-balanced.png)

**Generated test suite** — 10 test case(s) total (37 real step(s); pair-graph cycle has 47 edge(s) total, 17 synthetic dropped at translation).

- **test case 1**: `compose new email -> enter email subject -> enter email subject -> enter email body`
- **test case 2**: `enter receiver's email address -> send email`
- **test case 3**: `compose new email -> enter email body -> enter receiver's email address -> enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 4**: `enter receiver's email address -> send email`
- **test case 5**: `open mailbox`
- **test case 6**: `open mailbox -> select email`
- **test case 7**: `compose new email -> enter receiver's email address`
- **test case 8**: `select email -> enter forward receiver's email address -> send email`
- **test case 9**: `open mailbox`
- **test case 10**: `send email`
- **test case 11**: `compose new email`
- **test case 12**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 13**: `enter email subject -> enter email body -> enter email body -> enter email subject -> enter receiver's email address`
- **test case 14**: `open mailbox`


### Product 21

**Selected features:** selected = {ad, e, en, s}

**Repaired FTS:** 9 states, 18 transitions (17 real / 1 `__end__`).

**Pair graph (raw):** 19 nodes (incl. INIT), 47 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 19 nodes, 88 edges (41 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 21](eMail-product21-repaired.png)

![Pair graph (raw) — product 21](eMail-product21-pairgraph-raw.png)

![Pair graph (balanced) — product 21](eMail-product21-pairgraph-balanced.png)

**Generated test suite** — 25 test case(s) total (68 real step(s); pair-graph cycle has 88 edge(s) total, 41 synthetic dropped at translation).

- **test case 1**: `compose new email -> enter email subject -> enter email subject -> enter email body`
- **test case 2**: `enter receiver's email address -> sign mail -> send email`
- **test case 3**: `enter alias email addresses of receiver`
- **test case 4**: `create an addressbook for a receiver`
- **test case 5**: `compose new email -> enter email body -> enter receiver's email address -> get alias email addresses of receiver`
- **test case 6**: `send email`
- **test case 7**: `create an addressbook for a receiver`
- **test case 8**: `enter receiver's email address -> send email`
- **test case 9**: `compose new email`
- **test case 10**: `enter receiver's email address -> sign mail`
- **test case 11**: `enter alias email addresses of receiver`
- **test case 12**: `compose new email`
- **test case 13**: `enter receiver's email address -> get alias email addresses of receiver -> send email`
- **test case 14**: `open mailbox -> select email`
- **test case 15**: `open mailbox`
- **test case 16**: `compose new email`
- **test case 17**: `enter receiver's email address -> enter receiver's email address -> get receiver's public key -> encrypt mail with receiver's public key`
- **test case 18**: `send email`
- **test case 19**: `compose new email`
- **test case 20**: `enter receiver's email address -> enter receiver's email address -> enter email subject -> enter email subject`
- **test case 21**: `enter receiver's email address -> get receiver's public key`
- **test case 22**: `encrypt mail with receiver's public key -> send email`
- **test case 23**: `compose new email -> enter receiver's email address -> send email`
- **test case 24**: `create an addressbook for a receiver`
- **test case 25**: `enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 26**: `enter receiver's email address -> enter email subject -> enter receiver's email address`
- **test case 27**: `enter email subject -> enter email body -> enter email body -> enter email subject -> enter receiver's email address`
- **test case 28**: `send email`
- **test case 29**: `open mailbox`
- **test case 30**: `create an addressbook for a receiver -> enter the receiver's email address`
- **test case 31**: `enter alias email addresses of receiver`
- **test case 32**: `open mailbox`
- **test case 33**: `open mailbox`
- **test case 34**: `create an addressbook for a receiver`


### Product 22

**Selected features:** selected = {ad, au, e, s}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

**Pair graph (raw):** 22 nodes (incl. INIT), 64 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 22 nodes, 157 edges (93 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 22](eMail-product22-repaired.png)

![Pair graph (raw) — product 22](eMail-product22-pairgraph-raw.png)

![Pair graph (balanced) — product 22](eMail-product22-pairgraph-balanced.png)

**Generated test suite** — 38 test case(s) total (94 real step(s); pair-graph cycle has 157 edge(s) total, 93 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `open mailbox -> select email`
- **test case 3**: `open mailbox`
- **test case 4**: `create an addressbook for a receiver`
- **test case 5**: `compose new email -> enter email body -> enter email subject -> enter email subject`
- **test case 6**: `enter receiver's email address -> sign mail -> send email`
- **test case 7**: `enter autoresponse email body`
- **test case 8**: `enter email autoresponse date interval`
- **test case 9**: `open mailbox`
- **test case 10**: `enter email autoresponse date interval`
- **test case 11**: `enter alias email addresses of receiver`
- **test case 12**: `compose new email -> enter email subject -> enter receiver's email address`
- **test case 13**: `send email`
- **test case 14**: `create an addressbook for a receiver`
- **test case 15**: `send email`
- **test case 16**: `open mailbox`
- **test case 17**: `enter autoresponse email body -> enter email autoresponse date interval`
- **test case 18**: `create an addressbook for a receiver`
- **test case 19**: `enter receiver's email address -> get alias email addresses of receiver`
- **test case 20**: `send email`
- **test case 21**: `compose new email`
- **test case 22**: `enter receiver's email address -> send email`
- **test case 23**: `enter autoresponse email body`
- **test case 24**: `enter alias email addresses of receiver`
- **test case 25**: `enter email autoresponse date interval`
- **test case 26**: `send email`
- **test case 27**: `compose new email`
- **test case 28**: `send email`
- **test case 29**: `create an addressbook for a receiver`
- **test case 30**: `enter alias email addresses of receiver`
- **test case 31**: `open mailbox`
- **test case 32**: `compose new email`
- **test case 33**: `send email`
- **test case 34**: `enter email autoresponse date interval`
- **test case 35**: `send email`
- **test case 36**: `open mailbox`
- **test case 37**: `compose new email -> enter receiver's email address -> enter email subject -> enter email body -> enter receiver's email address`
- **test case 38**: `enter email subject -> enter email subject -> enter email body -> enter email body`
- **test case 39**: `enter autoresponse email body`
- **test case 40**: `compose new email`
- **test case 41**: `enter receiver's email address -> sign mail`
- **test case 42**: `compose new email`
- **test case 43**: `enter receiver's email address -> enter receiver's email address -> send email`
- **test case 44**: `open mailbox`
- **test case 45**: `create an addressbook for a receiver`
- **test case 46**: `enter the receiver's email address -> enter alias email addresses of receiver`
- **test case 47**: `create an addressbook for a receiver -> enter the receiver's email address`
- **test case 48**: `enter alias email addresses of receiver`
- **test case 49**: `enter autoresponse email body`
- **test case 50**: `enter receiver's email address -> enter email subject -> enter receiver's email address -> enter receiver's email address -> get alias email addresses of receiver -> send email`
- **test case 51**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 52**: `enter autoresponse email body`
- **test case 53**: `enter email autoresponse date interval -> enter autoresponse email body`


### Product 23

**Selected features:** selected = {au, e, s}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

**Pair graph (raw):** 18 nodes (incl. INIT), 49 edges (every edge = one contiguous transition pair in the original FTS; the subset starting at INIT correspond to pairs `(start, t)` for any original initial-state outgoing `t`).

**Pair graph (balanced):** 18 nodes, 110 edges (61 synthetic `__balance__N` added to restore in-balance at INIT).

![Repaired FTS — product 23](eMail-product23-repaired.png)

![Pair graph (raw) — product 23](eMail-product23-pairgraph-raw.png)

![Pair graph (balanced) — product 23](eMail-product23-pairgraph-balanced.png)

**Generated test suite** — 27 test case(s) total (69 real step(s); pair-graph cycle has 110 edge(s) total, 61 synthetic dropped at translation).

- **test case 1**: `enter email autoresponse date interval`
- **test case 2**: `open mailbox`
- **test case 3**: `enter email autoresponse date interval`
- **test case 4**: `open mailbox -> select email`
- **test case 5**: `enter email autoresponse date interval`
- **test case 6**: `compose new email -> enter email body -> enter email subject -> enter email subject`
- **test case 7**: `sign mail -> send email`
- **test case 8**: `enter autoresponse email body`
- **test case 9**: `compose new email -> enter email subject -> enter receiver's email address`
- **test case 10**: `send email`
- **test case 11**: `open mailbox`
- **test case 12**: `enter autoresponse email body`
- **test case 13**: `enter receiver's email address -> sign mail`
- **test case 14**: `send email`
- **test case 15**: `compose new email`
- **test case 16**: `send email`
- **test case 17**: `enter email autoresponse date interval`
- **test case 18**: `compose new email -> enter receiver's email address -> send email`
- **test case 19**: `enter autoresponse email body`
- **test case 20**: `compose new email`
- **test case 21**: `send email`
- **test case 22**: `compose new email`
- **test case 23**: `send email`
- **test case 24**: `enter email autoresponse date interval -> enter autoresponse email body -> enter email autoresponse date interval`
- **test case 25**: `send email`
- **test case 26**: `open mailbox`
- **test case 27**: `enter receiver's email address -> enter email subject -> enter email body -> enter receiver's email address`
- **test case 28**: `enter email subject -> enter email subject -> enter email body -> enter email body`
- **test case 29**: `enter autoresponse email body`
- **test case 30**: `enter autoresponse email body -> enter email autoresponse date interval -> enter autoresponse email body`
- **test case 31**: `compose new email`
- **test case 32**: `enter receiver's email address -> sign mail`
- **test case 33**: `open mailbox`
- **test case 34**: `compose new email`
- **test case 35**: `enter receiver's email address -> enter receiver's email address -> enter email subject -> enter receiver's email address -> enter receiver's email address -> send email`

---

Total products: 23.
