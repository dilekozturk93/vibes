# SCC Walkthrough — SVM (product 1)

Stepwise rendering of the M2 SCC repair pipeline on one hand-picked product of SVM. Each step shows the FTS as a PNG and a one-paragraph summary of what changed. Synthetic balancing edges (introduced later by EulerianBalancer in M3) are not present here.

**Chosen configuration:** selected = {c, f, t}

## Step 1 — SPL-level FTS (initial input to the pipeline)

**Size:** 9 states, 13 transitions

This is the bisimulation-minimized FTS produced by MxeToFtsConverter from the MXE ESG-Fx model. Every transition still carries its feature expression for traceability. The same FTS is the input for every product.

![Step 1](SVM-step1-spl.png)

## Step 2 — Projected FTS (after applying the configuration)

**Size:** 5 states, 6 transitions

FExpressionPreservingProjection keeps only transitions whose feature expression evaluates to true under the chosen configuration. 7 transition(s) were dropped relative to step 1. After projection the graph has 1 strongly-connected component(s); the SCC containing the initial state has 5 state(s). If that count is less than the total state count, the next step removes the unreachable / non-returnable portions.

![Step 2](SVM-step2-projected.png)

## Step 3 — Repaired FTS (initial-state SCC only)

**Size:** 5 states, 6 transitions

InitialSccFilter keeps only the SCC containing the initial state and drops every other state along with the transitions touching them. 0 state(s) and 0 transition(s) were removed. The result is strongly connected by construction and feeds the EulerianBalancer + Hierholzer pipeline in M3 and the coverage generators in M4 / M5 / M6.

![Step 3](SVM-step3-repaired.png)

