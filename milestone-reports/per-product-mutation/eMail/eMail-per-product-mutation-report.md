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

**Selected features:** selected = {ad, e, en, s}

**Repaired FTS:** 9 states, 18 transitions (17 real / 1 `__end__`).

**Family baseline projected to this product:** 7 test case(s) (of 6 family-level), 19 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 17 | 12/17 = 70.6% | 9/17 = 52.9% | 17/17 = 100.0% | 17/17 = 100.0% |
| ActionExchange | 221 | 156/221 = 70.6% | 117/221 = 52.9% | 221/221 = 100.0% | 221/221 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (5):

- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (8):

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

**ActionExchange — survived family-level state coverage (Devroey)** (65):

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

**ActionExchange — survived product state coverage** (104):

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

### Product 2

**Selected features:** selected = {au, e, en, s}

**Repaired FTS:** 9 states, 19 transitions (17 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 18 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 17 | 11/17 = 64.7% | 10/17 = 58.8% | 17/17 = 100.0% | 17/17 = 100.0% |
| ActionExchange | 187 | 121/187 = 64.7% | 110/187 = 58.8% | 187/187 = 100.0% | 187/187 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (6):

- `TM__state4__enter 
autoresponse 
email body__state5`
- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter email 
autoresponse 
date interval__state4`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**TransitionMissing — survived product state coverage** (7):

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

**ActionExchange — survived family-level state coverage (Devroey)** (66):

- `AEX__state4__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state4__enter 
autoresponse 
email body__get receiver's  
public key__state5`
- `AEX__state4__enter 
autoresponse 
email body__send email__state5`
- `AEX__state4__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state4__enter 
autoresponse 
email body__encrypt mail 
with receiver's 
public key__state5`
- `AEX__state4__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state4__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state4__enter 
autoresponse 
email body__select email__state5`
- `AEX__state7__sign mail__enter 
autoresponse 
email body__state10`
- `AEX__state7__sign mail__enter
 email 
subject__state10`
- `AEX__state7__sign mail__enter email 
body__state10`
- `AEX__state7__sign mail__get receiver's  
public key__state10`
- `AEX__state7__sign mail__send email__state10`
- `AEX__state7__sign mail__compose new 
email__state10`
- `AEX__state7__sign mail__enter 
receiver's 
email address__state10`
- `AEX__state7__sign mail__enter email 
autoresponse 
date interval__state10`
- `AEX__state7__sign mail__encrypt mail 
with receiver's 
public key__state10`
- `AEX__state7__sign mail__open mailbox__state10`
- `AEX__state7__sign mail__select email__state10`
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
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__get receiver's  
public key__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__encrypt mail 
with receiver's 
public key__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__sign mail__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__select email__state4`
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

**ActionExchange — survived product state coverage** (77):

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

### Product 3

**Selected features:** selected = {e, f, s}

**Repaired FTS:** 6 states, 13 transitions (12 real / 1 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 12 | 8/12 = 66.7% | 6/12 = 50.0% | 12/12 = 100.0% | 12/12 = 100.0% |
| ActionExchange | 96 | 64/96 = 66.7% | 48/96 = 50.0% | 96/96 = 100.0% | 96/96 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (4):

- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**TransitionMissing — survived product state coverage** (6):

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

**ActionExchange — survived family-level state coverage (Devroey)** (32):

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

**ActionExchange — survived product state coverage** (48):

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

### Product 4

**Selected features:** selected = {au, e, f}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 17 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 15 | 10/15 = 66.7% | 9/15 = 60.0% | 15/15 = 100.0% | 15/15 = 100.0% |
| ActionExchange | 135 | 90/135 = 66.7% | 81/135 = 60.0% | 135/135 = 100.0% | 135/135 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (5):

- `TM__state4__enter 
autoresponse 
email body__state5`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter email 
autoresponse 
date interval__state4`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**TransitionMissing — survived product state coverage** (6):

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

**ActionExchange — survived family-level state coverage (Devroey)** (45):

- `AEX__state4__enter 
autoresponse 
email body__enter forward 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state4__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state4__enter 
autoresponse 
email body__send email__state5`
- `AEX__state4__enter 
autoresponse 
email body__select email__state5`
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
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter forward 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__select email__state4`
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

**ActionExchange — survived product state coverage** (54):

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

### Product 5

**Selected features:** selected = {ad, e, en}

**Repaired FTS:** 9 states, 17 transitions (16 real / 1 `__end__`).

**Family baseline projected to this product:** 7 test case(s) (of 6 family-level), 19 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 16 | 12/16 = 75.0% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% |
| ActionExchange | 192 | 144/192 = 75.0% | 108/192 = 56.3% | 192/192 = 100.0% | 192/192 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (4):

- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (7):

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

**ActionExchange — survived family-level state coverage (Devroey)** (48):

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

**ActionExchange — survived product state coverage** (84):

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

### Product 6

**Selected features:** selected = {e, en, s}

**Repaired FTS:** 7 states, 14 transitions (13 real / 1 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 16 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 13 | 9/13 = 69.2% | 6/13 = 46.2% | 13/13 = 100.0% | 13/13 = 100.0% |
| ActionExchange | 117 | 81/117 = 69.2% | 54/117 = 46.2% | 117/117 = 100.0% | 117/117 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (4):

- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**TransitionMissing — survived product state coverage** (7):

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

**ActionExchange — survived family-level state coverage (Devroey)** (36):

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

**ActionExchange — survived product state coverage** (63):

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

### Product 7

**Selected features:** selected = {ad, e, f, s}

**Repaired FTS:** 8 states, 17 transitions (16 real / 1 `__end__`).

**Family baseline projected to this product:** 7 test case(s) (of 6 family-level), 18 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 16 | 11/16 = 68.8% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% |
| ActionExchange | 192 | 132/192 = 68.8% | 108/192 = 56.3% | 192/192 = 100.0% | 192/192 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (5):

- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (7):

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

**ActionExchange — survived family-level state coverage (Devroey)** (60):

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

**ActionExchange — survived product state coverage** (84):

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

### Product 8

**Selected features:** selected = {au, e, f, s}

**Repaired FTS:** 8 states, 18 transitions (16 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 17 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 16 | 10/16 = 62.5% | 8/16 = 50.0% | 16/16 = 100.0% | 16/16 = 100.0% |
| ActionExchange | 160 | 100/160 = 62.5% | 80/160 = 50.0% | 160/160 = 100.0% | 160/160 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (6):

- `TM__state4__enter 
autoresponse 
email body__state5`
- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter email 
autoresponse 
date interval__state4`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**TransitionMissing — survived product state coverage** (8):

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

**ActionExchange — survived family-level state coverage (Devroey)** (60):

- `AEX__state4__enter 
autoresponse 
email body__enter forward 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state4__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state4__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state4__enter 
autoresponse 
email body__send email__state5`
- `AEX__state4__enter 
autoresponse 
email body__select email__state5`
- `AEX__state7__sign mail__enter 
autoresponse 
email body__state10`
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
autoresponse 
date interval__state10`
- `AEX__state7__sign mail__enter email 
body__state10`
- `AEX__state7__sign mail__open mailbox__state10`
- `AEX__state7__sign mail__send email__state10`
- `AEX__state7__sign mail__select email__state10`
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
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter forward 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__sign mail__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__select email__state4`
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

**ActionExchange — survived product state coverage** (80):

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

### Product 9

**Selected features:** selected = {e, en}

**Repaired FTS:** 7 states, 13 transitions (12 real / 1 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 16 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 12 | 9/12 = 75.0% | 6/12 = 50.0% | 12/12 = 100.0% | 12/12 = 100.0% |
| ActionExchange | 96 | 72/96 = 75.0% | 48/96 = 50.0% | 96/96 = 100.0% | 96/96 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (3):

- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**TransitionMissing — survived product state coverage** (6):

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

**ActionExchange — survived family-level state coverage (Devroey)** (24):

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

**ActionExchange — survived product state coverage** (48):

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

### Product 10

**Selected features:** selected = {au, e, en}

**Repaired FTS:** 9 states, 18 transitions (16 real / 2 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 18 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 16 | 11/16 = 68.8% | 9/16 = 56.3% | 16/16 = 100.0% | 16/16 = 100.0% |
| ActionExchange | 160 | 110/160 = 68.8% | 90/160 = 56.3% | 160/160 = 100.0% | 160/160 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (5):

- `TM__state4__enter 
autoresponse 
email body__state5`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter email 
autoresponse 
date interval__state4`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**TransitionMissing — survived product state coverage** (7):

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

**ActionExchange — survived family-level state coverage (Devroey)** (50):

- `AEX__state4__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state4__enter 
autoresponse 
email body__encrypt mail 
with receiver's 
public key__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state4__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state4__enter 
autoresponse 
email body__get receiver's  
public key__state5`
- `AEX__state4__enter 
autoresponse 
email body__send email__state5`
- `AEX__state4__enter 
autoresponse 
email body__select email__state5`
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
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__encrypt mail 
with receiver's 
public key__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__get receiver's  
public key__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__select email__state4`
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

**ActionExchange — survived product state coverage** (70):

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

### Product 11

**Selected features:** selected = {ad, au, e, en}

**Repaired FTS:** 11 states, 22 transitions (20 real / 2 `__end__`).

**Family baseline projected to this product:** 7 test case(s) (of 6 family-level), 21 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 20 | 14/20 = 70.0% | 13/20 = 65.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 280 | 196/280 = 70.0% | 182/280 = 65.0% | 280/280 = 100.0% | 280/280 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (6):

- `TM__state4__enter 
autoresponse 
email body__state5`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter email 
autoresponse 
date interval__state4`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (7):

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

**ActionExchange — survived family-level state coverage (Devroey)** (84):

- `AEX__state4__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state4__enter 
autoresponse 
email body__get receiver's  
public key__state5`
- `AEX__state4__enter 
autoresponse 
email body__send email__state5`
- `AEX__state4__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state4__enter 
autoresponse 
email body__encrypt mail 
with receiver's 
public key__state5`
- `AEX__state4__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state4__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__select email__state5`
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
- `AEX__state1__enter email 
autoresponse 
date interval__enter the  
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__get receiver's  
public key__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__get alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__encrypt mail 
with receiver's 
public key__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__create an 
addressbook 
for a receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__select email__state4`
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
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`

**ActionExchange — survived product state coverage** (98):

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

### Product 12

**Selected features:** selected = {e, f}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

**Family baseline projected to this product:** 6 test case(s) (of 6 family-level), 15 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 11 | 8/11 = 72.7% | 6/11 = 54.5% | 11/11 = 100.0% | 11/11 = 100.0% |
| ActionExchange | 77 | 56/77 = 72.7% | 42/77 = 54.5% | 77/77 = 100.0% | 77/77 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (3):

- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**TransitionMissing — survived product state coverage** (5):

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

**ActionExchange — survived family-level state coverage (Devroey)** (21):

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

**ActionExchange — survived product state coverage** (35):

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

### Product 13

**Selected features:** selected = {ad, e, s}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 6 family-level), 17 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 15 | 10/15 = 66.7% | 8/15 = 53.3% | 15/15 = 100.0% | 15/15 = 100.0% |
| ActionExchange | 165 | 110/165 = 66.7% | 88/165 = 53.3% | 165/165 = 100.0% | 165/165 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (5):

- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (7):

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

**ActionExchange — survived family-level state coverage (Devroey)** (55):

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

**ActionExchange — survived product state coverage** (77):

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

**Selected features:** selected = {ad, au, e, en, s}

**Repaired FTS:** 11 states, 23 transitions (21 real / 2 `__end__`).

**Family baseline projected to this product:** 7 test case(s) (of 6 family-level), 21 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 21 | 14/21 = 66.7% | 13/21 = 61.9% | 21/21 = 100.0% | 21/21 = 100.0% |
| ActionExchange | 315 | 210/315 = 66.7% | 195/315 = 61.9% | 315/315 = 100.0% | 315/315 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (7):

- `TM__state4__enter 
autoresponse 
email body__state5`
- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter email 
autoresponse 
date interval__state4`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (8):

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

**ActionExchange — survived family-level state coverage (Devroey)** (105):

- `AEX__state4__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state4__enter 
autoresponse 
email body__get receiver's  
public key__state5`
- `AEX__state4__enter 
autoresponse 
email body__send email__state5`
- `AEX__state4__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state4__enter 
autoresponse 
email body__encrypt mail 
with receiver's 
public key__state5`
- `AEX__state4__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state4__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state4__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__select email__state5`
- `AEX__state7__sign mail__enter the  
receiver's 
email address__state10`
- `AEX__state7__sign mail__enter 
autoresponse 
email body__state10`
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
- `AEX__state7__sign mail__enter email 
autoresponse 
date interval__state10`
- `AEX__state7__sign mail__encrypt mail 
with receiver's 
public key__state10`
- `AEX__state7__sign mail__open mailbox__state10`
- `AEX__state7__sign mail__create an 
addressbook 
for a receiver__state10`
- `AEX__state7__sign mail__select email__state10`
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
- `AEX__state1__enter email 
autoresponse 
date interval__enter the  
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__get receiver's  
public key__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__get alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__encrypt mail 
with receiver's 
public key__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__sign mail__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__create an 
addressbook 
for a receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__select email__state4`
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

**ActionExchange — survived product state coverage** (120):

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

### Product 15

**Selected features:** selected = {e, s}

**Repaired FTS:** 6 states, 12 transitions (11 real / 1 `__end__`).

**Family baseline projected to this product:** 7 test case(s) (of 6 family-level), 14 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 11 | 7/11 = 63.6% | 5/11 = 45.5% | 11/11 = 100.0% | 11/11 = 100.0% |
| ActionExchange | 77 | 49/77 = 63.6% | 35/77 = 45.5% | 77/77 = 100.0% | 77/77 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (4):

- `TM__state7__sign mail__state10`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**TransitionMissing — survived product state coverage** (6):

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

**ActionExchange — survived family-level state coverage (Devroey)** (28):

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

**ActionExchange — survived product state coverage** (42):

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

### Product 16

**Selected features:** selected = {au, e}

**Repaired FTS:** 7 states, 15 transitions (13 real / 2 `__end__`).

**Family baseline projected to this product:** 7 test case(s) (of 6 family-level), 16 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 13 | 13/13 = 100.0% | 7/13 = 53.8% | 13/13 = 100.0% | 13/13 = 100.0% |
| ActionExchange | 104 | 104/104 = 100.0% | 56/104 = 53.8% | 104/104 = 100.0% | 104/104 = 100.0% |

**TransitionMissing — survived product state coverage** (6):

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

**ActionExchange — survived product state coverage** (48):

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

### Product 17

**Selected features:** selected = {ad, au, e, s}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 6 family-level), 19 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 19 | 12/19 = 63.2% | 11/19 = 57.9% | 19/19 = 100.0% | 19/19 = 100.0% |
| ActionExchange | 247 | 156/247 = 63.2% | 143/247 = 57.9% | 247/247 = 100.0% | 247/247 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (7):

- `TM__state4__enter 
autoresponse 
email body__state5`
- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter email 
autoresponse 
date interval__state4`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (8):

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

**ActionExchange — survived family-level state coverage (Devroey)** (91):

- `AEX__state4__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state4__enter 
autoresponse 
email body__send email__state5`
- `AEX__state4__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state4__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state4__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state4__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__select email__state5`
- `AEX__state7__sign mail__enter the  
receiver's 
email address__state10`
- `AEX__state7__sign mail__enter 
autoresponse 
email body__state10`
- `AEX__state7__sign mail__enter
 email 
subject__state10`
- `AEX__state7__sign mail__enter email 
body__state10`
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
- `AEX__state7__sign mail__enter email 
autoresponse 
date interval__state10`
- `AEX__state7__sign mail__open mailbox__state10`
- `AEX__state7__sign mail__create an 
addressbook 
for a receiver__state10`
- `AEX__state7__sign mail__select email__state10`
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
- `AEX__state1__enter email 
autoresponse 
date interval__enter the  
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__get alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__sign mail__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__create an 
addressbook 
for a receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__select email__state4`
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

**ActionExchange — survived product state coverage** (104):

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

### Product 18

**Selected features:** selected = {au, e, s}

**Repaired FTS:** 8 states, 17 transitions (15 real / 2 `__end__`).

**Family baseline projected to this product:** 7 test case(s) (of 6 family-level), 16 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 15 | 9/15 = 60.0% | 8/15 = 53.3% | 15/15 = 100.0% | 15/15 = 100.0% |
| ActionExchange | 135 | 81/135 = 60.0% | 72/135 = 53.3% | 135/135 = 100.0% | 135/135 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (6):

- `TM__state4__enter 
autoresponse 
email body__state5`
- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter email 
autoresponse 
date interval__state4`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`

**TransitionMissing — survived product state coverage** (7):

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

**ActionExchange — survived family-level state coverage (Devroey)** (54):

- `AEX__state4__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state4__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state4__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state4__enter 
autoresponse 
email body__send email__state5`
- `AEX__state4__enter 
autoresponse 
email body__select email__state5`
- `AEX__state7__sign mail__enter 
autoresponse 
email body__state10`
- `AEX__state7__sign mail__compose new 
email__state10`
- `AEX__state7__sign mail__enter
 email 
subject__state10`
- `AEX__state7__sign mail__enter 
receiver's 
email address__state10`
- `AEX__state7__sign mail__enter email 
autoresponse 
date interval__state10`
- `AEX__state7__sign mail__enter email 
body__state10`
- `AEX__state7__sign mail__open mailbox__state10`
- `AEX__state7__sign mail__send email__state10`
- `AEX__state7__sign mail__select email__state10`
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
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__sign mail__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__select email__state4`
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

**ActionExchange — survived product state coverage** (63):

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

### Product 19

**Selected features:** selected = {ad, e, f}

**Repaired FTS:** 8 states, 16 transitions (15 real / 1 `__end__`).

**Family baseline projected to this product:** 7 test case(s) (of 6 family-level), 18 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 15 | 11/15 = 73.3% | 9/15 = 60.0% | 15/15 = 100.0% | 15/15 = 100.0% |
| ActionExchange | 165 | 121/165 = 73.3% | 99/165 = 60.0% | 165/165 = 100.0% | 165/165 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (4):

- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (6):

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

**ActionExchange — survived family-level state coverage (Devroey)** (44):

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

**ActionExchange — survived product state coverage** (66):

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

### Product 20

**Selected features:** selected = {ad, e}

**Repaired FTS:** 8 states, 15 transitions (14 real / 1 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 6 family-level), 17 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 14 | 10/14 = 71.4% | 8/14 = 57.1% | 14/14 = 100.0% | 14/14 = 100.0% |
| ActionExchange | 140 | 100/140 = 71.4% | 80/140 = 57.1% | 140/140 = 100.0% | 140/140 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (4):

- `TM__state7__enter
 email 
subject__state2`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (6):

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

**ActionExchange — survived family-level state coverage (Devroey)** (40):

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
- `AEX__state7__get alias 
email addresses 
of receiver__enter the  
receiver's 
email address__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__compose new 
email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__enter
 email 
subject__state10`
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
body__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__send email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`

**ActionExchange — survived product state coverage** (60):

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

### Product 21

**Selected features:** selected = {ad, au, e}

**Repaired FTS:** 10 states, 20 transitions (18 real / 2 `__end__`).

**Family baseline projected to this product:** 8 test case(s) (of 6 family-level), 19 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 18 | 12/18 = 66.7% | 11/18 = 61.1% | 18/18 = 100.0% | 18/18 = 100.0% |
| ActionExchange | 216 | 144/216 = 66.7% | 132/216 = 61.1% | 216/216 = 100.0% | 216/216 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (6):

- `TM__state4__enter 
autoresponse 
email body__state5`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter email 
autoresponse 
date interval__state4`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (7):

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

**ActionExchange — survived family-level state coverage (Devroey)** (72):

- `AEX__state4__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state4__enter 
autoresponse 
email body__send email__state5`
- `AEX__state4__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state4__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state4__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__select email__state5`
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
- `AEX__state1__enter email 
autoresponse 
date interval__enter the  
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__get alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__create an 
addressbook 
for a receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__select email__state4`
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
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`

**ActionExchange — survived product state coverage** (84):

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

### Product 22

**Selected features:** selected = {ad, au, e, f}

**Repaired FTS:** 10 states, 21 transitions (19 real / 2 `__end__`).

**Family baseline projected to this product:** 7 test case(s) (of 6 family-level), 20 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 19 | 13/19 = 68.4% | 11/19 = 57.9% | 19/19 = 100.0% | 19/19 = 100.0% |
| ActionExchange | 247 | 169/247 = 68.4% | 143/247 = 57.9% | 247/247 = 100.0% | 247/247 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (6):

- `TM__state4__enter 
autoresponse 
email body__state5`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter email 
autoresponse 
date interval__state4`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (8):

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

**ActionExchange — survived family-level state coverage (Devroey)** (78):

- `AEX__state4__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state4__enter 
autoresponse 
email body__send email__state5`
- `AEX__state4__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter forward 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state4__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state4__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__select email__state5`
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
- `AEX__state1__enter email 
autoresponse 
date interval__enter the  
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__get alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter forward 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__create an 
addressbook 
for a receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__select email__state4`
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
of receiver__open mailbox__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__create an 
addressbook 
for a receiver__state10`
- `AEX__state7__get alias 
email addresses 
of receiver__select email__state10`

**ActionExchange — survived product state coverage** (104):

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

### Product 23

**Selected features:** selected = {ad, au, e, f, s}

**Repaired FTS:** 10 states, 22 transitions (20 real / 2 `__end__`).

**Family baseline projected to this product:** 7 test case(s) (of 6 family-level), 20 real step(s) applicable.

| Operator | Real mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 20 | 13/20 = 65.0% | 11/20 = 55.0% | 20/20 = 100.0% | 20/20 = 100.0% |
| ActionExchange | 280 | 182/280 = 65.0% | 154/280 = 55.0% | 280/280 = 100.0% | 280/280 = 100.0% |

**TransitionMissing — survived family-level state coverage (Devroey)** (7):

- `TM__state4__enter 
autoresponse 
email body__state5`
- `TM__state7__sign mail__state10`
- `TM__state7__enter
 email 
subject__state2`
- `TM__state1__enter email 
autoresponse 
date interval__state4`
- `TM__state2__enter
 email 
subject__state2`
- `TM__state7__enter 
receiver's 
email address__state7`
- `TM__state7__get alias 
email addresses 
of receiver__state10`

**TransitionMissing — survived product state coverage** (9):

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

**ActionExchange — survived family-level state coverage (Devroey)** (98):

- `AEX__state4__enter 
autoresponse 
email body__enter the  
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter
 email 
subject__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
body__state5`
- `AEX__state4__enter 
autoresponse 
email body__send email__state5`
- `AEX__state4__enter 
autoresponse 
email body__get alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter forward 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__compose new 
email__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter alias 
email addresses 
of receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter 
receiver's 
email address__state5`
- `AEX__state4__enter 
autoresponse 
email body__enter email 
autoresponse 
date interval__state5`
- `AEX__state4__enter 
autoresponse 
email body__sign mail__state5`
- `AEX__state4__enter 
autoresponse 
email body__open mailbox__state5`
- `AEX__state4__enter 
autoresponse 
email body__create an 
addressbook 
for a receiver__state5`
- `AEX__state4__enter 
autoresponse 
email body__select email__state5`
- `AEX__state7__sign mail__enter the  
receiver's 
email address__state10`
- `AEX__state7__sign mail__enter 
autoresponse 
email body__state10`
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
- `AEX__state7__sign mail__enter email 
autoresponse 
date interval__state10`
- `AEX__state7__sign mail__open mailbox__state10`
- `AEX__state7__sign mail__create an 
addressbook 
for a receiver__state10`
- `AEX__state7__sign mail__select email__state10`
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
- `AEX__state1__enter email 
autoresponse 
date interval__enter the  
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
autoresponse 
email body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter
 email 
subject__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter email 
body__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__send email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__get alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter forward 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__compose new 
email__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter alias 
email addresses 
of receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__enter 
receiver's 
email address__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__sign mail__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__open mailbox__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__create an 
addressbook 
for a receiver__state4`
- `AEX__state1__enter email 
autoresponse 
date interval__select email__state4`
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

**ActionExchange — survived product state coverage** (126):

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
---

## eMail summary (aggregate over 23 products)

**Family-level baseline** (Devroey 2014, ported from VIBeS commit f856c90): 6 test case(s) generated once for the SPL, projected per-product via fexpr-filtering before kill-checking.

| Operator | Mutants | Family state-cov (Devroey) | Product state-cov | Product transition-cov | Product pair-cov |
|---|---|---|---|---|---|
| TransitionMissing | 361 | 249/361 = 69.0% | 202/361 = 56.0% | 361/361 = 100.0% | 361/361 = 100.0% |
| ActionExchange | 4004 | 2748/4004 = 68.6% | 2265/4004 = 56.6% | 4004/4004 = 100.0% | 4004/4004 = 100.0% |

Total products: 23.
