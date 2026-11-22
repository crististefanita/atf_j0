<a id="mock--deterministic-http-transport"></a>
<a id="mock--transport-http-determinist"></a>
# Mock — deterministic HTTP transport

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

`atf-mock` replaces the Core `ApiTransport` port with a local response catalog. Use it for offline components and contracts, with controlled inputs and an explicit fallback.

<a id="minimal-example"></a>
<a id="exemplu-minim"></a>
## Minimal example

`mock.yaml` in the working directory:

```yaml
mock:
  routes:
    - {method: GET, path: /health, status: 200, body: '{"status":"ready"}'}
  fallback: {status: 500, body: '{}'}
```

Snippet inside a method; the transport remains offline:

```java
import atf.core.http.request.ApiRequest;
import atf.mock.AtfMockTransport;
import java.nio.file.Path;

var mock = AtfMockTransport.fromConfiguration(Path.of("mock.yaml"));
var response = mock.send(ApiRequest.of("GET", "/health"),
        "https://offline.test/health", "none").join();
```

`response.status()` is `200`; a request with no matching route receives the `500` fallback. The test independently compares the response with the expected result.

<a id="catalog-and-matching"></a>
<a id="catalog-și-potrivire"></a>
## Catalog and matching

| Need | API or contract |
|---|---|
| Explicit YAML catalog | `AtfMockTransport.fromConfiguration(Path)` |
| Project configuration | `fromProjectConfiguration()`: `atf.mock.config` > `ATF_MOCK_CONFIG` > `atf-mock.yaml/yml` |
| Request conditions | Method, path, auth profile, query, required headers, and expected body in `MockRoute` |
| Request body | Default `json` mode, negative MIME probe `wrong-content-type`; numeric matching without conversion through `double` |
| Repeated query values | `ApiQueryParameters`; includes the required values with multiplicity |

- Without configuration, `fromProjectConfiguration()` uses the `defaults()` fixture; the application catalog remains local.
- The duration in a route is response metadata, not a simulated real delay.
- A mock probe verifies integration; acceptance of the real service, broker, or device remains separate.

---

Mock source and native tests are maintained in this project's library module; [maintenance and feedback](README.md#mentenanță-și-feedback) routes shared findings.

[Consumer integration guides](README.md#consumer-guides).
