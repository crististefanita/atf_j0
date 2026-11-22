<a id="drafting-a-game-assertion-name"></a>
<a id="redactarea-unui-nume-de-aserțiune-de-joc"></a>
<a id="redaction-a-game-assertion-name"></a>
# Redactarea unui nume de aserțiune de joc

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## Pe scurt

Reporting deține verificarea redactării etichetelor publicate. Criteriul acoperă păstrarea textului de joc și mascarea credențialelor reale.

<a id="witness-and-status"></a>
<a id="martor-și-stare-curentă"></a>
## Martor și stare curentă

Martorul istoric provine dintr-o componentă JVM publicată prin Reporting J18.

| Suprafață | Valoare observată |
|---|---|
| Etichetă capturată | `Safe exit always scores five basic Romanian points` |
| Etichetă în HTML | `Safe exit always scores five basic ***REDACTED*** points` |
| Comparație HTML–receipt | `Comparison name/order changed` |

- **P2 · owner Reporting.** Extent și fallback aplică redactorul etichetei aserțiunii.
- [Regula textuală](../../atf-reporting/src/main/java/atf/reporting/security/SensitiveDataRedactor.java) asociază `basic` cu următorul cuvânt și în text liber. Aceasta este calea de fals pozitiv observată în martorul J18.
- Calificarea pe JAR-ul curent rămâne deschisă.

<a id="criteria-for-the-library"></a>
<a id="criterii-pentru-bibliotecă"></a>
## Criterii pentru bibliotecă

- Reproduceți în sursele deținute de bibliotecă, printr-o aserțiune cu acest nume și valori întregi.
- Păstrați proba pozitivă de mascarea unei credențiale Basic într-un context de autentificare. Folosiți exclusiv un token sintetic.
- Verificați separat o propoziție de business cu aceleași două cuvinte, un antet de autentificare și un secret configurat explicit.
- Verificați eticheta randată în DOM și lipsa tokenului sintetic din HTML, atât pentru rendererul principal, cât și pentru fallback.

<a id="local-adaptation-of-the-witness"></a>
<a id="adaptarea-locală-a-martorului"></a>
## Adaptarea locală a martorului

Componenta a fost reexecutată într-o copie a sursei înghețate cu eticheta `An exact zero bid earns five points under Romanian scoring`. Cele șase capturi ale metodei au proveniență de execuție nouă. Încercarea originală și HTML-ul respins au fost păstrate.
