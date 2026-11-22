<a id="core--shared-contracts-and-mechanisms"></a>
<a id="core--contracte-și-mecanisme-comune"></a>
# Core — shared contracts and mechanisms

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

`atf-core` provides ATF's reusable vocabulary and mechanisms. Use it to compose adapters and executions while keeping application rules in the consumer.

| Need | Starting API |
|---|---|
| HTTP requests, responses, and execution | `ApiRequest`, `ApiResponse`, `ApiTransport`, `ApiExecutor` |
| Configuration captured for a run | `AtfConfiguration`, `ApiSettingsSnapshot`, `AtfRuntimeSnapshot` |
| Strict JSON and sensitive-data redaction | `JsonDocuments`, `SensitiveDataRedactor` |
| Releasing resources in reverse order | `CleanupRegistry` |
| Selective synchronization with baseline verification | `AtfPromotionManifest`, `AtfPromotionGuard` |

<a id="minimal-example"></a>
<a id="exemplu-minim"></a>
## Minimal example

Snippet inside a method that may propagate `IOException`; it constructs the data without sending a request:

```java
import atf.core.http.request.ApiRequest;
import atf.core.http.request.ApiUrlBuilder;
import atf.core.json.JsonDocuments;
import java.net.URI;

var request = ApiRequest.of("GET", "/orders/42");
var url = new ApiUrlBuilder(URI.create("https://example.test/api"))
        .build(request);
var body = JsonDocuments.read("{\"total\":19.90}");
var total = body.path("total").decimalValue();
```

`url` is `https://example.test/api/orders/42`. `JsonDocuments` rejects duplicate keys and trailing content after the document; decimal numbers are not converted through `double`.

<a id="use-in-atf"></a>
<a id="utilizare-în-atf"></a>
## Use in ATF

- Inject an `ApiTransport` into `ApiExecutor`; select the real or mock transport explicitly during composition.
- `HttpClientTransport` and `UrlConnectionTransport` propagate cancellation to the owned operation; the fallback includes the body in the deadline. Cancellation does not prove rollback or authorize replay.
- Reuse the run's configuration snapshot; the oracle, assertions, and business verdict belong to the consumer.
- Keep the original model for comparison; apply redaction and preview limits when publishing diagnostics.
- Core exposes neutral ports; [Console](README.Console.md), [Mock](README.Mock.md), and [Reporting](README.Reporting.md) have distinct roles.

---

Core source and native tests are maintained in this project's library module; [maintenance and feedback](README.md#mentenanță-și-feedback) routes shared findings.

[Consumer integration guides](README.md#consumer-guides).
