# Core — active ownership and cancellation gaps

## Pe scurt

Core owns transport execution, cancellation, redaction and filesystem promotion. Current contracts are in the [Core README](../../atf-core/README.md); the active gap is specific to the URLConnection fallback and must not be generalized to HttpClient.

## CORE-URLCONNECTION-CANCEL-126 — body subscription and dispatch ownership

**Priority: 96/100** = impact 38 + gap 29 + actionable 20 + documentation 9.

| Evidence | Observed behavior |
|---|---|
| source boundary | [UrlConnectionTransport.send/bodyBytes](../../atf-core/src/main/java/atf/core/http/transport/UrlConnectionTransport.java) uses an independent async supplier and an unbounded body future wait |
| controlled cancellation | cancel returns true; the body subscription is not cancelled; controlled release causes a loopback request after termination |
| controlled deadline | the original 250 ms caller deadline expires; the body survives; later release still dispatches |
| distribution | both witnesses reproduce on freshly compiled source and the physical consumer JAR, without an external service |
| existing protection | [transport catalog](../../atf-core/src/test/java/atf/core/http/transport/ApiLiveTransportCatalogTest.java) and HttpClient cancellation contracts do not close the fallback body lifecycle gap |

- [ ] Give the operation explicit ownership of its body subscription, blocking worker and connection; cancellation must reach that operation instead of only completing its wrapper future.
- [ ] Apply the remaining deadline to body collection and connection/read work; preserve a terminal guard immediately before dispatch so a late body cannot restart a cancelled operation.
- [ ] Add permanent native tests with a controlled publisher and loopback receiver: cancellation, deadline, body failure, cancellation/completion race and cleanup failure; release every fixture resource in bounded finally blocks.
- [ ] Re-run independent source/JAR witnesses; prove no post-terminal request, retain primary/suppressed causes and avoid retry or larger timeouts.
- [ ] Revalidate both transport implementations and relevant consumer contracts before distributing a new Core package.

**Closure:** the current implementation fails the new negatives; the corrected operation passes with no dispatch after pre-dispatch cancellation/deadline. Cancellation of an already dispatched mutation still does not establish rollback.

<a id="core-console-redaction-124--sanitize-every-emitted-console-channel"></a>
<a id="core-transport-cancel-125--retain-ownership-of-the-upstream-http-future"></a>

## Stable contracts and owner routes

- [Console projection and HttpClient cancellation](../../atf-core/README.md#proiecția-console-și-anularea-transportului) retain permanent native regressions.
- [Reporting](ATF_Reporting_Feedback.md) owns renderer behavior; [Reporting Tests](ATF_Reporting_Tests_Feedback.md) owns neutral source/JAR rendering oracles.
- The [J1 registry](../../../atf_j1/docs/feedback/README.md) coordinates common non-library criteria; consumers retain domain acceptance.
- Approved test controls remain in the [technical baseline](../ATF_Test_Changes_Baseline.md), separate from active backlog.
