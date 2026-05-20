# Per-Product Mutation Report — eMail

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

**Selected features:** selected = {e, s}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 11 | 5/11 = 45.5% | 11/11 = 100.0% | 11/11 = 100.0% |
| ActionExchange | 77 | 35/77 = 45.5% | 77/77 = 100.0% | 77/77 = 100.0% |

**TransitionMissing — survived state coverage** (6):

- `TM__state7__send email__state1`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state10__send email__state1`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (42):

- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state10__send email__compose new 
email__state1`
- `AEX__state10__send email__enter
 email 
subject__state1`
- `AEX__state10__send email__enter 
receiver's 
email address__state1`
- `AEX__state10__send email__sign mail__state1`
- `AEX__state10__send email__enter email 
body__state1`
- `AEX__state10__send email__open mailbox__state1`
- `AEX__state10__send email__select email__state1`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 2

**Selected features:** selected = {ad, e}

**Repaired FTS:** 8 states, 15 transitions (14 real / 1 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 14 | 8/14 = 57.1% | 14/14 = 100.0% | 14/14 = 100.0% |
| ActionExchange | 140 | 80/140 = 57.1% | 140/140 = 100.0% | 140/140 = 100.0% |

**TransitionMissing — survived state coverage** (6):

- `TM__state7__send email__state1`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state10__send email__state1`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (60):

- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state10__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state10__send email__enter the  
receiver's 
email address__state1`
- `AEX__state10__send email__compose new 
email__state1`
- `AEX__state10__send email__enter
 email 
subject__state1`
- `AEX__state10__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state10__send email__enter 
receiver's 
email address__state1`
- `AEX__state10__send email__enter email 
body__state1`
- `AEX__state10__send email__open mailbox__state1`
- `AEX__state10__send email__select email__state1`
- `AEX__state10__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`

### Product 3

**Selected features:** selected = {ad, au, e}

**Repaired FTS:** 10 states, 20 transitions (18 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 18 | 11/18 = 61.1% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 216 | 132/216 = 61.1% | 216/216 = 100.0% | 216/216 = 100.0% |

**TransitionMissing — survived state coverage** (7):

- `TM__state7__send email__state1`
- `TM__state9__enter alias 
email addresses 
of receiver__state1`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (84):

- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__enter 
autoresponse 
email body__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
autoresponse 
date interval__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter 
autoresponse 
email body__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter
 email 
subject__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter email 
body__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__send email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__get alias 
email addresses 
of receiver__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__compose new 
email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter 
receiver's 
email address__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter email 
autoresponse 
date interval__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__open mailbox__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__select email__state1`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 4

**Selected features:** selected = {ad, au, e, en, s}

**Repaired FTS:** 11 states, 23 transitions (21 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 21 | 13/21 = 61.9% | 21/21 = 100.0% | 21/21 = 100.0% |
| ActionExchange | 315 | 195/315 = 61.9% | 315/315 = 100.0% | 315/315 = 100.0% |

**TransitionMissing — survived state coverage** (8):

- `TM__state7__send email__state1`
- `TM__state11__encrypt mail 
with receiver's 
public key__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**ActionExchange — survived state coverage** (120):

- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__enter 
autoresponse 
email body__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get receiver's  
public key__state1`
- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
autoresponse 
date interval__state1`
- `AEX__state7__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter the  
receiver's 
email address__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter 
autoresponse 
email body__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter
 email 
subject__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter email 
body__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__get receiver's  
public key__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__send email__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__get alias 
email addresses 
of receiver__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__compose new 
email__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter alias 
email addresses 
of receiver__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter 
receiver's 
email address__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter email 
autoresponse 
date interval__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__sign mail__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__open mailbox__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__create an 
addressbook 
for a receiver__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__select email__state10`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__get receiver's  
public key__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__encrypt mail 
with receiver's 
public key__state5`
- `AEX__state1__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__get receiver's  
public key__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__get receiver's  
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__encrypt mail 
with receiver's 
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`
- `AEX__state7__get alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter 
autoresponse 
email body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter
 email 
subject__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter email 
body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__get receiver's  
public key__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__send email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__compose new 
email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter alias 
email addresses 
of receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter 
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter email 
autoresponse 
date interval__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__encrypt mail 
with receiver's 
public key__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__sign mail__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`

### Product 5

**Selected features:** selected = {e, en, s}

**Repaired FTS:** 7 states, 14 transitions (13 real / 1 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 13 | 6/13 = 46.2% | 13/13 = 100.0% | 13/13 = 100.0% |
| ActionExchange | 117 | 54/117 = 46.2% | 117/117 = 100.0% | 117/117 = 100.0% |

**TransitionMissing — survived state coverage** (7):

- `TM__state7__send email__state1`
- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state10__send email__state1`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (63):

- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__get receiver's  
public key__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state7__sign mail__compose new 
email__state10`
- `AEX__state7__sign mail__enter
 email 
subject__state10`
- `AEX__state7__sign mail__enter 
receiver's 
email address__state10`
- `AEX__state7__sign mail__encrypt mail 
with receiver's 
public key__state10`
- `AEX__state7__sign mail__enter email 
body__state10`
- `AEX__state7__sign mail__open mailbox__state10`
- `AEX__state7__sign mail__get receiver's  
public key__state10`
- `AEX__state7__sign mail__send email__state10`
- `AEX__state7__sign mail__select email__state10`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__get receiver's  
public key__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state10__send email__compose new 
email__state1`
- `AEX__state10__send email__enter
 email 
subject__state1`
- `AEX__state10__send email__enter 
receiver's 
email address__state1`
- `AEX__state10__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state10__send email__sign mail__state1`
- `AEX__state10__send email__enter email 
body__state1`
- `AEX__state10__send email__open mailbox__state1`
- `AEX__state10__send email__get receiver's  
public key__state1`
- `AEX__state10__send email__select email__state1`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__encrypt mail 
with receiver's 
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__get receiver's  
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 6

**Selected features:** selected = {au, e, en, s}

**Repaired FTS:** 9 states, 19 transitions (17 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 17 | 10/17 = 58.8% | 17/17 = 100.0% | 17/17 = 100.0% |
| ActionExchange | 187 | 110/187 = 58.8% | 187/187 = 100.0% | 187/187 = 100.0% |

**TransitionMissing — survived state coverage** (7):

- `TM__state7__send email__state1`
- `TM__state11__encrypt mail 
with receiver's 
public key__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (77):

- `AEX__state7__send email__enter 
autoresponse 
email body__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get receiver's  
public key__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
autoresponse 
date interval__state1`
- `AEX__state7__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter 
autoresponse 
email body__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter
 email 
subject__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter email 
body__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__get receiver's  
public key__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__send email__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__compose new 
email__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter 
receiver's 
email address__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter email 
autoresponse 
date interval__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__sign mail__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__open mailbox__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__select email__state10`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__get receiver's  
public key__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__encrypt mail 
with receiver's 
public key__state5`
- `AEX__state1__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__get receiver's  
public key__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__get receiver's  
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__encrypt mail 
with receiver's 
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 7

**Selected features:** selected = {ad, au, e, f, s}

**Repaired FTS:** 10 states, 22 transitions (20 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 20 | 11/20 = 55.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 280 | 154/280 = 55.0% | 280/280 = 100.0% | 280/280 = 100.0% |

**TransitionMissing — survived state coverage** (9):

- `TM__state7__send email__state1`
- `TM__state9__enter alias 
email addresses 
of receiver__state1`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state8__enter forward 
receiver's 
email address__state10`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**ActionExchange — survived state coverage** (126):

- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__enter 
autoresponse 
email body__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter forward 
receiver's 
email address__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
autoresponse 
date interval__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter 
autoresponse 
email body__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter
 email 
subject__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter email 
body__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__send email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__get alias 
email addresses 
of receiver__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter forward 
receiver's 
email address__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__compose new 
email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter 
receiver's 
email address__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter email 
autoresponse 
date interval__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__sign mail__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__open mailbox__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__select email__state1`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter forward 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state8__enter forward 
receiver's 
email address__enter the  
receiver's 
email address__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter 
autoresponse 
email body__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter
 email 
subject__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter email 
body__state10`
- `AEX__state8__enter forward 
receiver's 
email address__send email__state10`
- `AEX__state8__enter forward 
receiver's 
email address__get alias 
email addresses 
of receiver__state10`
- `AEX__state8__enter forward 
receiver's 
email address__compose new 
email__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter alias 
email addresses 
of receiver__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter 
receiver's 
email address__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter email 
autoresponse 
date interval__state10`
- `AEX__state8__enter forward 
receiver's 
email address__sign mail__state10`
- `AEX__state8__enter forward 
receiver's 
email address__open mailbox__state10`
- `AEX__state8__enter forward 
receiver's 
email address__create an 
addressbook 
for a receiver__state10`
- `AEX__state8__enter forward 
receiver's 
email address__select email__state10`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter forward 
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`
- `AEX__state7__get alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter 
autoresponse 
email body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter
 email 
subject__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter email 
body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__send email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter forward 
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__compose new 
email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter alias 
email addresses 
of receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter 
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter email 
autoresponse 
date interval__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__sign mail__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`

### Product 8

**Selected features:** selected = {e, f, s}

**Repaired FTS:** 6 states, 13 transitions (12 real / 1 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 12 | 6/12 = 50.0% | 12/12 = 100.0% | 12/12 = 100.0% |
| ActionExchange | 96 | 48/96 = 50.0% | 96/96 = 100.0% | 96/96 = 100.0% |

**TransitionMissing — survived state coverage** (6):

- `TM__state7__send email__state1`
- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (48):

- `AEX__state7__send email__enter forward 
receiver's 
email address__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state7__sign mail__enter forward 
receiver's 
email address__state10`
- `AEX__state7__sign mail__compose new 
email__state10`
- `AEX__state7__sign mail__enter
 email 
subject__state10`
- `AEX__state7__sign mail__enter 
receiver's 
email address__state10`
- `AEX__state7__sign mail__enter email 
body__state10`
- `AEX__state7__sign mail__open mailbox__state10`
- `AEX__state7__sign mail__send email__state10`
- `AEX__state7__sign mail__select email__state10`
- `AEX__state7__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state2__enter email 
body__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter forward 
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 9

**Selected features:** selected = {au, e}

**Repaired FTS:** 7 states, 15 transitions (13 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 13 | 7/13 = 53.8% | 13/13 = 100.0% | 13/13 = 100.0% |
| ActionExchange | 104 | 56/104 = 53.8% | 104/104 = 100.0% | 104/104 = 100.0% |

**TransitionMissing — survived state coverage** (6):

- `TM__state7__send email__state1`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (48):

- `AEX__state7__send email__enter 
autoresponse 
email body__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
autoresponse 
date interval__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 10

**Selected features:** selected = {ad, e, f}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 15 | 9/15 = 60.0% | 15/15 = 100.0% | 15/15 = 100.0% |
| ActionExchange | 165 | 99/165 = 60.0% | 165/165 = 100.0% | 165/165 = 100.0% |

**TransitionMissing — survived state coverage** (6):

- `TM__state7__send email__state1`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**ActionExchange — survived state coverage** (66):

- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter forward 
receiver's 
email address__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter forward 
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`
- `AEX__state7__get alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter
 email 
subject__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter email 
body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__send email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter forward 
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__compose new 
email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter alias 
email addresses 
of receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter 
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`

### Product 11

**Selected features:** selected = {ad, au, e, f}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 19 | 11/19 = 57.9% | 19/19 = 100.0% | 19/19 = 100.0% |
| ActionExchange | 247 | 143/247 = 57.9% | 247/247 = 100.0% | 247/247 = 100.0% |

**TransitionMissing — survived state coverage** (8):

- `TM__state7__send email__state1`
- `TM__state9__enter alias 
email addresses 
of receiver__state1`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state8__enter forward 
receiver's 
email address__state10`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (104):

- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__enter 
autoresponse 
email body__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter forward 
receiver's 
email address__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
autoresponse 
date interval__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter 
autoresponse 
email body__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter
 email 
subject__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter email 
body__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__send email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__get alias 
email addresses 
of receiver__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter forward 
receiver's 
email address__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__compose new 
email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter 
receiver's 
email address__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter email 
autoresponse 
date interval__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__open mailbox__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__select email__state1`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter forward 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state8__enter forward 
receiver's 
email address__enter the  
receiver's 
email address__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter 
autoresponse 
email body__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter
 email 
subject__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter email 
body__state10`
- `AEX__state8__enter forward 
receiver's 
email address__send email__state10`
- `AEX__state8__enter forward 
receiver's 
email address__get alias 
email addresses 
of receiver__state10`
- `AEX__state8__enter forward 
receiver's 
email address__compose new 
email__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter alias 
email addresses 
of receiver__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter 
receiver's 
email address__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter email 
autoresponse 
date interval__state10`
- `AEX__state8__enter forward 
receiver's 
email address__open mailbox__state10`
- `AEX__state8__enter forward 
receiver's 
email address__create an 
addressbook 
for a receiver__state10`
- `AEX__state8__enter forward 
receiver's 
email address__select email__state10`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter forward 
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 12

**Selected features:** selected = {ad, e, en}

**Repaired FTS:** 9 states, 17 transitions (16 real / 1 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 16 | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% |
| ActionExchange | 192 | 108/192 = 56.3% | 192/192 = 100.0% | 192/192 = 100.0% |

**TransitionMissing — survived state coverage** (7):

- `TM__state7__send email__state1`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state10__send email__state1`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**ActionExchange — survived state coverage** (84):

- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get receiver's  
public key__state1`
- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__get receiver's  
public key__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state10__send email__enter the  
receiver's 
email address__state1`
- `AEX__state10__send email__enter
 email 
subject__state1`
- `AEX__state10__send email__enter email 
body__state1`
- `AEX__state10__send email__get receiver's  
public key__state1`
- `AEX__state10__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state10__send email__compose new 
email__state1`
- `AEX__state10__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state10__send email__enter 
receiver's 
email address__state1`
- `AEX__state10__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state10__send email__open mailbox__state1`
- `AEX__state10__send email__select email__state1`
- `AEX__state10__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__get receiver's  
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__encrypt mail 
with receiver's 
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`
- `AEX__state7__get alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter
 email 
subject__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter email 
body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__get receiver's  
public key__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__send email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__compose new 
email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter alias 
email addresses 
of receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter 
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__encrypt mail 
with receiver's 
public key__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`

### Product 13

**Selected features:** selected = {ad, e, s}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 15 | 8/15 = 53.3% | 15/15 = 100.0% | 15/15 = 100.0% |
| ActionExchange | 165 | 88/165 = 53.3% | 165/165 = 100.0% | 165/165 = 100.0% |

**TransitionMissing — survived state coverage** (7):

- `TM__state7__send email__state1`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state10__send email__state1`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**ActionExchange — survived state coverage** (77):

- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state10__send email__enter the  
receiver's 
email address__state1`
- `AEX__state10__send email__enter
 email 
subject__state1`
- `AEX__state10__send email__enter email 
body__state1`
- `AEX__state10__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state10__send email__compose new 
email__state1`
- `AEX__state10__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state10__send email__enter 
receiver's 
email address__state1`
- `AEX__state10__send email__sign mail__state1`
- `AEX__state10__send email__open mailbox__state1`
- `AEX__state10__send email__select email__state1`
- `AEX__state10__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`
- `AEX__state7__get alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter
 email 
subject__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter email 
body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__send email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__compose new 
email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter alias 
email addresses 
of receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter 
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__sign mail__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`

### Product 14

**Selected features:** selected = {au, e, f, s}

**Repaired FTS:** 8 states, 18 transitions (16 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 16 | 8/16 = 50.0% | 16/16 = 100.0% | 16/16 = 100.0% |
| ActionExchange | 160 | 80/160 = 50.0% | 160/160 = 100.0% | 160/160 = 100.0% |

**TransitionMissing — survived state coverage** (8):

- `TM__state7__send email__state1`
- `TM__state5__enter email 
autoresponse 
date interval__state4`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state8__enter forward 
receiver's 
email address__state10`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (80):

- `AEX__state7__send email__enter 
autoresponse 
email body__state1`
- `AEX__state7__send email__enter forward 
receiver's 
email address__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
autoresponse 
date interval__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state5__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter forward 
receiver's 
email address__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__sign mail__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__select email__state4`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__enter forward 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state8__enter forward 
receiver's 
email address__enter 
autoresponse 
email body__state10`
- `AEX__state8__enter forward 
receiver's 
email address__compose new 
email__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter
 email 
subject__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter 
receiver's 
email address__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter email 
autoresponse 
date interval__state10`
- `AEX__state8__enter forward 
receiver's 
email address__sign mail__state10`
- `AEX__state8__enter forward 
receiver's 
email address__enter email 
body__state10`
- `AEX__state8__enter forward 
receiver's 
email address__open mailbox__state10`
- `AEX__state8__enter forward 
receiver's 
email address__send email__state10`
- `AEX__state8__enter forward 
receiver's 
email address__select email__state10`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__enter forward 
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 15

**Selected features:** selected = {e, en}

**Repaired FTS:** 7 states, 13 transitions (12 real / 1 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 12 | 6/12 = 50.0% | 12/12 = 100.0% | 12/12 = 100.0% |
| ActionExchange | 96 | 48/96 = 50.0% | 96/96 = 100.0% | 96/96 = 100.0% |

**TransitionMissing — survived state coverage** (6):

- `TM__state7__send email__state1`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state10__send email__state1`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (48):

- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__get receiver's  
public key__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__get receiver's  
public key__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state10__send email__compose new 
email__state1`
- `AEX__state10__send email__enter
 email 
subject__state1`
- `AEX__state10__send email__enter 
receiver's 
email address__state1`
- `AEX__state10__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state10__send email__enter email 
body__state1`
- `AEX__state10__send email__open mailbox__state1`
- `AEX__state10__send email__get receiver's  
public key__state1`
- `AEX__state10__send email__select email__state1`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__encrypt mail 
with receiver's 
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__get receiver's  
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 16

**Selected features:** selected = {au, e, f}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 15 | 9/15 = 60.0% | 15/15 = 100.0% | 15/15 = 100.0% |
| ActionExchange | 135 | 81/135 = 60.0% | 135/135 = 100.0% | 135/135 = 100.0% |

**TransitionMissing — survived state coverage** (6):

- `TM__state5__enter email 
autoresponse 
date interval__state4`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (54):

- `AEX__state5__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter forward 
receiver's 
email address__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__select email__state4`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__enter forward 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__enter forward 
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 17

**Selected features:** selected = {ad, e, f, s}

**Repaired FTS:** 8 states, 17 transitions (16 real / 1 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 16 | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% |
| ActionExchange | 192 | 108/192 = 56.3% | 192/192 = 100.0% | 192/192 = 100.0% |

**TransitionMissing — survived state coverage** (7):

- `TM__state7__send email__state1`
- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**ActionExchange — survived state coverage** (84):

- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter forward 
receiver's 
email address__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__sign mail__enter the  
receiver's 
email address__state10`
- `AEX__state7__sign mail__enter
 email 
subject__state10`
- `AEX__state7__sign mail__enter email 
body__state10`
- `AEX__state7__sign mail__send email__state10`
- `AEX__state7__sign mail__get alias 
email addresses 
of receiver__state10`
- `AEX__state7__sign mail__enter forward 
receiver's 
email address__state10`
- `AEX__state7__sign mail__compose new 
email__state10`
- `AEX__state7__sign mail__enter alias 
email addresses 
of receiver__state10`
- `AEX__state7__sign mail__enter 
receiver's 
email address__state10`
- `AEX__state7__sign mail__open mailbox__state10`
- `AEX__state7__sign mail__select email__state10`
- `AEX__state7__sign mail__create an 
addressbook 
for a receiver__state10`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter forward 
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`
- `AEX__state7__get alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter
 email 
subject__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter email 
body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__send email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter forward 
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__compose new 
email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter alias 
email addresses 
of receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter 
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__sign mail__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`

### Product 18

**Selected features:** selected = {ad, au, e, en}

**Repaired FTS:** 11 states, 22 transitions (20 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 20 | 13/20 = 65.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 280 | 182/280 = 65.0% | 280/280 = 100.0% | 280/280 = 100.0% |

**TransitionMissing — survived state coverage** (7):

- `TM__state7__send email__state1`
- `TM__state11__encrypt mail 
with receiver's 
public key__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (98):

- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__enter 
autoresponse 
email body__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get receiver's  
public key__state1`
- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
autoresponse 
date interval__state1`
- `AEX__state7__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter the  
receiver's 
email address__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter 
autoresponse 
email body__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter
 email 
subject__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter email 
body__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__get receiver's  
public key__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__send email__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__get alias 
email addresses 
of receiver__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__compose new 
email__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter alias 
email addresses 
of receiver__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter 
receiver's 
email address__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__enter email 
autoresponse 
date interval__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__open mailbox__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__create an 
addressbook 
for a receiver__state10`
- `AEX__state11__encrypt mail 
with receiver's 
public key__select email__state10`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__get receiver's  
public key__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__encrypt mail 
with receiver's 
public key__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__get receiver's  
public key__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__get receiver's  
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__encrypt mail 
with receiver's 
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 19

**Selected features:** selected = {au, e, en}

**Repaired FTS:** 9 states, 18 transitions (16 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 16 | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% |
| ActionExchange | 160 | 90/160 = 56.3% | 160/160 = 100.0% | 160/160 = 100.0% |

**TransitionMissing — survived state coverage** (7):

- `TM__state7__send email__state1`
- `TM__state5__enter email 
autoresponse 
date interval__state4`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (70):

- `AEX__state7__send email__enter 
autoresponse 
email body__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
autoresponse 
date interval__state1`
- `AEX__state7__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__get receiver's  
public key__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state5__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__encrypt mail 
with receiver's 
public key__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__get receiver's  
public key__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__select email__state4`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__encrypt mail 
with receiver's 
public key__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__get receiver's  
public key__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__get receiver's  
public key__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__encrypt mail 
with receiver's 
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__get receiver's  
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 20

**Selected features:** selected = {e, f}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 11 | 6/11 = 54.5% | 11/11 = 100.0% | 11/11 = 100.0% |
| ActionExchange | 77 | 42/77 = 54.5% | 77/77 = 100.0% | 77/77 = 100.0% |

**TransitionMissing — survived state coverage** (5):

- `TM__state7__send email__state1`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (35):

- `AEX__state7__send email__enter forward 
receiver's 
email address__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state2__enter email 
body__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter
 email 
subject__enter forward 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter forward 
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`

### Product 21

**Selected features:** selected = {ad, e, en, s}

**Repaired FTS:** 9 states, 18 transitions (17 real / 1 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 17 | 9/17 = 52.9% | 17/17 = 100.0% | 17/17 = 100.0% |
| ActionExchange | 221 | 117/221 = 52.9% | 221/221 = 100.0% | 221/221 = 100.0% |

**TransitionMissing — survived state coverage** (8):

- `TM__state7__send email__state1`
- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state10__send email__state1`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**ActionExchange — survived state coverage** (104):

- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get receiver's  
public key__state1`
- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__sign mail__enter the  
receiver's 
email address__state10`
- `AEX__state7__sign mail__enter
 email 
subject__state10`
- `AEX__state7__sign mail__enter email 
body__state10`
- `AEX__state7__sign mail__get receiver's  
public key__state10`
- `AEX__state7__sign mail__send email__state10`
- `AEX__state7__sign mail__get alias 
email addresses 
of receiver__state10`
- `AEX__state7__sign mail__compose new 
email__state10`
- `AEX__state7__sign mail__enter alias 
email addresses 
of receiver__state10`
- `AEX__state7__sign mail__enter 
receiver's 
email address__state10`
- `AEX__state7__sign mail__encrypt mail 
with receiver's 
public key__state10`
- `AEX__state7__sign mail__open mailbox__state10`
- `AEX__state7__sign mail__select email__state10`
- `AEX__state7__sign mail__create an 
addressbook 
for a receiver__state10`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__get receiver's  
public key__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__get receiver's  
public key__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__encrypt mail 
with receiver's 
public key__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state10__send email__enter the  
receiver's 
email address__state1`
- `AEX__state10__send email__enter
 email 
subject__state1`
- `AEX__state10__send email__enter email 
body__state1`
- `AEX__state10__send email__get receiver's  
public key__state1`
- `AEX__state10__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state10__send email__compose new 
email__state1`
- `AEX__state10__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state10__send email__enter 
receiver's 
email address__state1`
- `AEX__state10__send email__encrypt mail 
with receiver's 
public key__state1`
- `AEX__state10__send email__sign mail__state1`
- `AEX__state10__send email__open mailbox__state1`
- `AEX__state10__send email__select email__state1`
- `AEX__state10__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__get receiver's  
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__encrypt mail 
with receiver's 
public key__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`
- `AEX__state7__get alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter
 email 
subject__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter email 
body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__get receiver's  
public key__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__send email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__compose new 
email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter alias 
email addresses 
of receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter 
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__encrypt mail 
with receiver's 
public key__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__sign mail__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`

### Product 22

**Selected features:** selected = {ad, au, e, s}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 19 | 11/19 = 57.9% | 19/19 = 100.0% | 19/19 = 100.0% |
| ActionExchange | 247 | 143/247 = 57.9% | 247/247 = 100.0% | 247/247 = 100.0% |

**TransitionMissing — survived state coverage** (8):

- `TM__state7__send email__state1`
- `TM__state9__enter alias 
email addresses 
of receiver__state1`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**ActionExchange — survived state coverage** (104):

- `AEX__state7__send email__enter the  
receiver's 
email address__state1`
- `AEX__state7__send email__enter 
autoresponse 
email body__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__get alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter alias 
email addresses 
of receiver__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
autoresponse 
date interval__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__create an 
addressbook 
for a receiver__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter 
autoresponse 
email body__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter
 email 
subject__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter email 
body__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__send email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__get alias 
email addresses 
of receiver__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__compose new 
email__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter 
receiver's 
email address__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__enter email 
autoresponse 
date interval__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__sign mail__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__open mailbox__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state1`
- `AEX__state9__enter alias 
email addresses 
of receiver__select email__state1`
- `AEX__state7__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state2__enter email 
body__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter the  
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__get alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter alias 
email addresses 
of receiver__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__create an 
addressbook 
for a receiver__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter the  
receiver's 
email address__state7`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__get alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter alias 
email addresses 
of receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__create an 
addressbook 
for a receiver__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`
- `AEX__state7__get alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter 
autoresponse 
email body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter
 email 
subject__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter email 
body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__send email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__compose new 
email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter alias 
email addresses 
of receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter 
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter email 
autoresponse 
date interval__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__sign mail__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`

### Product 23

**Selected features:** selected = {au, e, s}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

| Operator | Real mutants | State-cov | Transition-cov | Pair-cov |
|---|---|---|---|---|
| TransitionMissing | 15 | 8/15 = 53.3% | 15/15 = 100.0% | 15/15 = 100.0% |
| ActionExchange | 135 | 72/135 = 53.3% | 135/135 = 100.0% | 135/135 = 100.0% |

**TransitionMissing — survived state coverage** (7):

- `TM__state7__send email__state1`
- `TM__state5__enter email 
autoresponse 
date interval__state4`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter 
autoresponse 
email body__state5`
- `TM__state2__enter email 
body__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**ActionExchange — survived state coverage** (63):

- `AEX__state7__send email__enter 
autoresponse 
email body__state1`
- `AEX__state7__send email__compose new 
email__state1`
- `AEX__state7__send email__enter
 email 
subject__state1`
- `AEX__state7__send email__enter 
receiver's 
email address__state1`
- `AEX__state7__send email__enter email 
autoresponse 
date interval__state1`
- `AEX__state7__send email__sign mail__state1`
- `AEX__state7__send email__enter email 
body__state1`
- `AEX__state7__send email__open mailbox__state1`
- `AEX__state7__send email__select email__state1`
- `AEX__state5__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__sign mail__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state5__enter email 
autoresponse 
date interval__select email__state4`
- `AEX__state7__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state7__enter
 email 
subject__compose new 
email__state2`
- `AEX__state7__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state7__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state7__enter
 email 
subject__sign mail__state2`
- `AEX__state7__enter
 email 
subject__enter email 
body__state2`
- `AEX__state7__enter
 email 
subject__open mailbox__state2`
- `AEX__state7__enter
 email 
subject__send email__state2`
- `AEX__state7__enter
 email 
subject__select email__state2`
- `AEX__state1__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state1__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state1__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state1__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state1__enter 
autoresponse 
email body__send email__state5`
- `AEX__state1__enter 
autoresponse 
email body__select email__state5`
- `AEX__state2__enter email 
body__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter email 
body__compose new 
email__state2`
- `AEX__state2__enter email 
body__enter
 email 
subject__state2`
- `AEX__state2__enter email 
body__enter 
receiver's 
email address__state2`
- `AEX__state2__enter email 
body__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter email 
body__sign mail__state2`
- `AEX__state2__enter email 
body__open mailbox__state2`
- `AEX__state2__enter email 
body__send email__state2`
- `AEX__state2__enter email 
body__select email__state2`
- `AEX__state2__enter
 email 
subject__enter 
autoresponse 
email body__state2`
- `AEX__state2__enter
 email 
subject__compose new 
email__state2`
- `AEX__state2__enter
 email 
subject__enter 
receiver's 
email address__state2`
- `AEX__state2__enter
 email 
subject__enter email 
autoresponse 
date interval__state2`
- `AEX__state2__enter
 email 
subject__sign mail__state2`
- `AEX__state2__enter
 email 
subject__enter email 
body__state2`
- `AEX__state2__enter
 email 
subject__open mailbox__state2`
- `AEX__state2__enter
 email 
subject__send email__state2`
- `AEX__state2__enter
 email 
subject__select email__state2`
- `AEX__state7__enter 
receiver's 
email address__enter 
autoresponse 
email body__state7`
- `AEX__state7__enter 
receiver's 
email address__compose new 
email__state7`
- `AEX__state7__enter 
receiver's 
email address__enter
 email 
subject__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
autoresponse 
date interval__state7`
- `AEX__state7__enter 
receiver's 
email address__sign mail__state7`
- `AEX__state7__enter 
receiver's 
email address__enter email 
body__state7`
- `AEX__state7__enter 
receiver's 
email address__open mailbox__state7`
- `AEX__state7__enter 
receiver's 
email address__send email__state7`
- `AEX__state7__enter 
receiver's 
email address__select email__state7`
---

## eMail summary (aggregate over 23 products)

| Operator | Mutants | State-cov kills | Transition-cov kills | Pair-cov kills |
|---|---|---|---|---|
| TransitionMissing | 361 | 202/361 = 56.0% | 361/361 = 100.0% | 361/361 = 100.0% |
| ActionExchange | 4004 | 2265/4004 = 56.6% | 4004/4004 = 100.0% | 4004/4004 = 100.0% |

Total products: 23.
