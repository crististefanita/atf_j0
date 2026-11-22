<a id="core---contracts-and-maintenance-routes"></a>
<a id="core--contracte-și-trasee-de-mentenanță"></a>
# Core - contracts and maintenance routes

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

J0 holds the execution of shipments, cancellation, redaction and promotion of files. Contracts and permanent evidence are in [Core README](../../atf-core/README.md); consumers retain the acceptance of their own deployment.

<a id="core-urlconnection-cancel-126--body-subscription-and-dispatch-ownership"></a>
<a id="core-console-redaction-124--sanitize-every-emitted-console-channel"></a>
<a id="core-transport-cancel-125--retain-ownership-of-the-upstream-http-future"></a>

<a id="contracts-held-at-the-owner"></a>
<a id="contracte-păstrate-la-owner"></a>
## Contracts held at the owner

| Boundary | Contract and permanent proof |
|---|---|
| HttpClient | [Cancellation of upstream](../../atf-core/README.md#proiecția-console-și-anularea-transportului), one terminal result and cleanup suppressed |
| URLConnection | Subscription body, virtual worker and connection held by the same operation; late body cannot start a disconnect after cancellation or deadline |
| Deadline | Collecting the body, connecting and reading use the original budget; [regression](../../atf-core/src/test/java/atf/core/http/transport/UrlConnectionDeadlineTest.java) keeps 250 ms; [JVM isolation](../../atf-core/src/test/java/atf/core/http/transport/UrlConnectionDeadlineIsolationTest.java) proposes progress when a virtual carrier is blocked |
| Miscellaneous errors and races | [Controlled and loopback Publisher](../../atf-core/src/test/java/atf/core/http/transport/UrlConnectionCancellationTest.java): original case, suppressed, competitive completion and lack of a later disconnect |
| Distribution | [Consumery contract](../ATF_Core_Consumer.md): native source / JAR, POM embedded identical and consistent dependent pairs |

Cancellation of an already deployed operation does not confirm rollback and does not authorise replica. Core samples do not know the application, broker, device or ASR-ul consumerul.

<a id="where-the-following-gap-is-recorded"></a>
<a id="unde-se-înregistrează-următoarea-lacună"></a>
## Where the following gap is recorded

- Trigger, coordinated / hash, minimum reproduction, expected / observed and negative criterion; one transport problem does not generalise in the other.
- [Reporting](ATF_Reporting_Feedback.md) holds the renderer; [Reporting Tests](ATF_Reporting_Tests_Feedback.md) holds neutral rendering oracles.
- [J1 Registry](../../../atf_j1/docs/feedback/README.md) coordinates common non-library criteria; consumers retain the acceptance of the domain.
- [Baseline-ul tehnic](../ATF_Test_Changes_Baseline.md) shall keep the Git controls separate from the backlog.
