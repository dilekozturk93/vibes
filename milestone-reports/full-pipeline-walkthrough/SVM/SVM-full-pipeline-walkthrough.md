# Full Pipeline Walkthrough — SVM (product 7)

Four-stage rendering of the full conversion-to-balancing pipeline on one hand-picked product of SVM. Each step shows the FTS as a PNG and a one-paragraph summary of what changed.

**Chosen configuration:** selected = {c, s}

## Step 1 — SPL-level FTS (canonical input)

**Size:** 9 states, 13 transitions

Bisimulation-minimized FTS produced by MxeToFtsConverter. Feature expressions are retained on every transition for traceability. Synthetic '__end__' transitions (back-to-INIT for mixed-terminal ESG vertices) appear here if the source ESG has any '['-only-edge vertices.

![Step 1](SVM-step1-spl.png)

## Step 2 — Projected FTS (after applying the configuration)

**Size:** 8 states, 9 transitions

FExpressionPreservingProjection keeps transitions whose feature expression evaluates to true under the configuration. 4 transition(s) dropped relative to step 1. Resulting graph has 1 strongly-connected component(s). If that count is 1 the next step is a no-op; otherwise it removes the parts of the graph the initial state cannot reach AND return from.

![Step 2](SVM-step2-projected.png)

## Step 3 — Strongly-connected projected FTS

**Size:** 8 states, 9 transitions

InitialSccFilter keeps only the SCC containing the initial state and drops every other state plus the transitions touching them. 0 state(s) and 0 transition(s) removed. Result is strongly connected by construction — every state can reach every other state.

![Step 3](SVM-step3-strongly-connected.png)

## Step 4 — Strongly-connected + balanced FTS

**Size:** 8 states, 10 transitions

EulerianBalancer adds 1 synthetic '__balance__N' transitions so every state has in-degree = out-degree, the second precondition for Hierholzer's Euler-cycle algorithm. 9 real transition(s) preserved, 1 synthetic balancing transition(s) inserted; 8 total state(s).

![Step 4](SVM-step4-balanced.png)

