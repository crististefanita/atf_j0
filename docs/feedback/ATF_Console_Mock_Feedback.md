# Console and Mock — library ownership

## Pe scurt

J0 owns the common Console and Mock implementation and native tests. The five live Java trees are textually identical; consumer POM and documentation differences remain deployment specific.

## Delivered ownership contract

[Platform library consumption](../ATF_Platform_Libraries.md) defines standalone publication, physical-JAR integrity, native-test retention and independent consumer acceptance. Closed extraction requirements are removed from the active backlog; the current package retains detailed execution evidence in owned TMP. Existing Core/Reporting and J1 UI findings remain separate.

## MOCK-FAULT-SCRIPT-111 — compare the two existing failure fixtures before extracting

Priority: 78/100. J2's TerminalFailureFixtures and ConvergenceFailureFixtures repeat finite transport sequencing, dispatch counting and exceptional completion. J0 now owns the Mock library; J2 retains the domain oracles and independent acceptance.

- [ ] Compare the concrete fixture requirements before adding an API; extract only a demonstrated common primitive, with no new Maven module.
- [ ] If justified, keep the plan immutable and invocation state local; create a fresh exception for each execution and distinguish dispatched attempts, matched routes and completed responses.
- [ ] Preserve method, MIME, auth, query, JSON/numeric matching, strict ordering and exhaustion failure. Keep OrderContract, saga and SQL witness policy in J2.
- [ ] Add native public-contract negatives in J0 and run both original J2 matrices on the physical JAR, including original/suppressed causes and failing sinks.

Closure requires reduced duplication and the same independent domain oracles, without importing consumer code into Mock. The publication extraction does not implement this optional primitive or certify live messaging.
