# Mock — transport HTTP determinist

## Pe scurt

`atf-mock` înlocuiește portul Core `ApiTransport` cu un catalog local de răspunsuri. Folosiți-l pentru componente și contracte offline, cu intrări controlate și un fallback explicit.

## Exemplu minim

Fragment într-o metodă; execuția nu deschide o conexiune HTTP:

```java
import atf.core.http.request.ApiRequest;
import atf.mock.AtfMockConfiguration;
import atf.mock.AtfMockTransport;
import atf.mock.MockRoute;
import java.time.Duration;
import java.util.List;
import java.util.Map;

var ready = new MockRoute("GET", "/health", 200,
        "{\"status\":\"ready\"}", Map.of(), Duration.ZERO);
var unexpected = new MockRoute("*", "/*", 500,
        "{}", Map.of(), Duration.ZERO);
var mock = new AtfMockTransport(
        new AtfMockConfiguration(List.of(ready), unexpected));
var response = mock.send(ApiRequest.of("GET", "/health"),
        "https://offline.test/health", "none").join();
```

`response.status()` este `200`; o cerere fără rută potrivită primește fallback-ul `500`. Testul compară independent răspunsul cu rezultatul așteptat.

## Catalog și potrivire

| Necesitate | API sau contract |
|---|---|
| Catalog YAML explicit | `AtfMockTransport.fromConfiguration(Path)` |
| Configurația proiectului | `fromProjectConfiguration()`; `atf.mock.config` sau `ATF_MOCK_CONFIG` |
| Condiții de request | Metodă, cale, profil auth, query, headere cerute și corp așteptat în `MockRoute` |
| Corp de request | Mod implicit `json`, probă MIME negativă `wrong-content-type`; potrivire numerică fără conversie prin `double` |
| Query repetat | `ApiQueryParameters`; include valorile cerute, cu multiplicitate |

- Fără configurație, `fromProjectConfiguration()` folosește fixture-ul `defaults()`; catalogul aplicației rămâne local.
- Durata din rută este metadată a răspunsului, nu o întârziere reală simulată.
- O probă mock verifică integrarea; acceptanța serviciului, brokerului sau dispozitivului real rămâne separată.

---

Mentenanța codului și testelor Mock aparține proiectului separat `atf_j0/atf-mock`; [feedback-ul și sincronizarea](README.md#mentenanță-și-feedback) pornesc de la J0.
