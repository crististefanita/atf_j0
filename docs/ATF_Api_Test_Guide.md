# Ghid pentru adăugarea unui test API în ATF Java

## Pe scurt

- scop: un test API determinist, semantic și reproductibil;
- boundary: testul compune core, transportul configurat și validatorul;
- raportul este verificat separat după contractul Extent;
- arhitectură: [ATF_Architecture.md](ATF_Architecture.md);
- raportare: [ATF_Reporting_Contract.md](ATF_Reporting_Contract.md).

## Pașii obligatorii

- [ ] definește comportamentul API și criteriul de acceptare;
- [ ] verifică ruta, modelul, serviciul și validatorul existente;
- [ ] păstrează vocabularul rutelor la owner: `MockRoutePaths` acoperă serviciul
      demo al modulului `atf-mock`, nu rutele fiecărui consumer; pentru live,
      `ApiLiveTransportCatalog` oferă transport generic cu endpoint declarat și
      opt-in explicit;
- [ ] adaugă serviciu semantic numai când operația are sens de domeniu;
- [ ] execută prin executor și transport injectat; testul nu construiește clientul HTTP;
- [ ] grupează verificările response-ului într-un singur tabel de assertions;
- [ ] înregistrează cleanup-ul imediat după dobândirea ownership-ului;
- [ ] adaugă întâi testul unit/component pentru regula nouă;
- [ ] numește scenariul și tag-urile după intenție;
- [ ] generează artifactul și verifică raportul după contract.

<details>
<summary><strong>Flow cu mai multe request-uri</strong></summary>

- contractul de mutație și contractul read-model-ului rămân distincte;
- relația dintre răspunsuri verifică `id/self-link`, versiune sau lineage;
- verificarea se atașează request-ului de read-back ca evidence;
- nu se inventează un request HTTP pentru a explica relația.

</details>

## Structura minimă a unui scenariu

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":16,"padding":6,"diagramPadding":20}}}%%
flowchart LR
    Test[Test API] -->|planifică| Plan["Plan"]
    Plan -->|pregătește| Setup["SETUP"]
    Setup -->|execută| Body[TEST BODY]
    Body -->|închide| Teardown[TEARDOWN]
    Teardown -->|curăță| Cleanup["Cleanup"]
    Setup -. atașează .-> SetupRequest["Dovezi setup"]
    Body -. atașează .-> BodyRequest["Schimb HTTP"]
    BodyRequest -. deține .-> BodyValidation["Contract validation"]
```

<details>
<summary><strong>Legendă — scenariul API minim</strong></summary>

- linia solidă arată `TEST API → plan → SETUP → TEST BODY → TEARDOWN`;
- ramificațiile punctate arată numai evidence-ul local al fazei;
- `SETUP` pregătește numai precondițiile care există în scenariu;
- `TEST BODY` păstrează contract validation nested sub request-ul al cărui
  response îl verifică;
- `TEARDOWN` apare doar când există cleanup sau read-back real;
- tabelul de assertions este verdictul semantic, nu un rezumat duplicat în fază.

Etichetele scurte păstrează următorul sens:

| În schemă | Sens complet |
|---|---|
| Plan | Scenario plan |
| SETUP | SETUP opțional |
| Cleanup | Cleanup/read-back |
| Dovezi setup | Setup request |
| Schimb HTTP | Request + trace · response evidence |
| Contract validation | Contract validation + aggregate · assertion table |
| atașează | local evidence |
| deține | owns |

</details>

Pentru un test simplu, TEST BODY trebuie să conțină request-ul și validarea
lui. Pentru lifecycle, fazele sunt create numai când au conținut real. Un nod
TEST BODY nu repetă failure-urile care apar deja în tabelul de assertions; faza
arată doar sumarul, iar diagnosticul detaliat rămâne sub validation.

## Ce verifică un test API

Un validator poate grupa în același tabel:

| Assertion | Expected | Actual | Result |
|---|---|---|---|
| HTTP status | codul și textul așteptat | răspunsul primit | PASS/FAIL/WARNING |
| Content-Type | tipul acceptat | header-ul primit | PASS/FAIL |
| JSON schema | schema contractului | schema evaluată | PASS/FAIL |
| câmp obligatoriu | valoare sau tip | valoarea primită | PASS/FAIL/WARNING |
| relație | resursa asociată | rezultatul verificării | PASS/FAIL |

Nu se creează câte un nod pentru fiecare assertion. Tabelul este agregat pentru
response-ul curent, astfel încât toate abaterile să fie vizibile împreună.

Corelarea alege observația curentă înaintea evaluării statusului, conform
[contractului dovezii](ATF_Evidence_Capture_Contract.md#alegerea-dovezii-înaintea-verdictului).
Un răspuns de eroare și lipsa capturii au diagnostice distincte; un 2xx mai vechi
nu poate înlocui dovada operației evaluate.

## Durată și status în raport

Regula vizibilă este:

- durata are unitate explicită (`ms` sau `s`);
- statusul și durata sunt tokens diferite;
- frunza folosește durata măsurată;
- părintele folosește durata proprie și totalurile copiilor direcți;
- o durată Extent inclusivă nu se adună din nou cu descendenții;
- timestamp-ul nu este durată și nu apare ca o coloană în `Tests`.

Unitatea, geometria și paleta badge-urilor sunt contracte vizuale comune, nu
reguli API; sursa normativă este
[contractul Reporting Display](ATF_Reporting_Display_Contract.md#tokens-vizuale).

## Separarea responsabilităților

```text
MockRoutePaths              -> rutele serviciului demo atf-mock
ApiLiveTransportCatalog    -> catalog generic de endpoint-uri live, opt-in
ApiOperationLabel           -> label semantic pentru raport
Service                     -> acțiune semantică
ApiExecutor                 -> transport, retry și durată
Validator                   -> assertions și tabel agregat
Report session              -> arbore și evidence
Scenario                    -> ordine și intenție
```

Dacă testul începe să conțină JSON de infrastructură, CSS, markup Extent,
politici de retry sau endpoint-uri literale, responsabilitatea este în locul
greșit.

`ApiLiveTransportCatalog` nu descoperă host-ul serviciului și nu selectează
automat o rută de business: consumerul îi furnizează endpoint-ul din configurația
sa, apoi catalogul validează modul `LIVE`, opt-in-ul și originea request-ului.
Contractul serviciului și rutele lui rămân în modulul consumerului.

## Replay manual și request-uri reale

Raportul API este sursa de pași reproductibili: `METHOD`, `URL`, headers
efective, `REQUEST JSON`, `RESPONSE JSON`, `TRANSPORT PROVENANCE` și
`MANUAL REPLAY`. În acceptance, un URL real configurat este numai metadata a
mock-ului; raportul îl marchează `ACCEPTANCE / MOCK` și `NOT EXECUTABLE`. Nu se
copiează un cURL dintr-un raport mock ca și cum ar fi fost executat.

Request-urile reale sunt separate și opt-in:

```yaml
api:
  transport: live
  baseUri: https://<real-api-host>/<base-path>
```

Transportul live cere mod și opt-in explicite la composition root, plus un
endpoint validat de `ApiLiveTransportCatalog`. Configurația generică poate citi
`atf.api.live.optIn` sau `ATF_API_LIVE_OPT_IN`; gate-urile consumerilor au politici proprii.

| Lansare | Gate curent | Owner |
|---|---|---|
| Catalog generic configurat | profil `live` și opt-in de transport | `atf-core` |
| API AutoEx | opt-in-uri independente `atf.autoex.api.readonly.live.enabled` și `atf.autoex.api.live.enabled` | [AutoEx](../atf-test-auto-ex/README.md#api-direct-și-limita-evidence-ului-uiapi) |
| API autentificat Shop | profil autentificat permis, fixture valid și mod API `LIVE` | [Shop](../atf-test-practice-shop/README.md#de-ce-raportul-api-nu-arată-ca-raportul-ui) |

În toate cazurile se păstrează:

- replay cURL generat după execuție, cu token-uri și headere sensibile redactate;
- fără network pentru testele care nu declară explicit profilul `live`;
- fără placeholder acceptance și fără request în afara originii endpoint-ului selectat.

## Ownership-ul raportului

Suita nu garantează singură raport comun: adaptorul trebuie să dețină explicit
sesiunea și finalizarea. AutoEx declară scope `suite` pentru API și păstrează
scope `class` la lansare individuală; Shop are o singură clasă în suita API live.

- Auditul Shop se deschide înaintea clientului și credentialelor și se publică
  după cleanup, inclusiv pe ieșiri de eșec. Lipsa răspunsului nu inventează status sau body HTTP.
- Un HTTP respins păstrează request-ul, răspunsul disponibil și comparațiile FAIL;
  o clasă reușită nu acoperă eșecul clasei-surori din artifactul comun.
- Componentele fără trafic publică verificări prin adaptorul de componentă,
  fără request-uri sintetice; contractul este în
  [ghidul de testare](ATF_Testing_Guide.md#auditul-componentelor-consumerilor).

## Cleanup și failure

- resursa creată se înregistrează imediat în registry-ul scenariului;
- teardown-ul se execută și când body-ul eșuează, dacă există ownership;
- failure-ul din cleanup nu ascunde failure-ul primar;
- statusul final respectă aceeași precedence peste tot;
- un test skip nu simulează un pass și nu publică assertions inventate.

## Verificarea raportului

După testele offline și rularea scenariului:

- [ ] deschide artifactul HTML în TAB MCP;
- [ ] verifică `Tests → Category → Fails → Overview`, cu `Overview` implicit;
- [ ] selectează un test pozitiv și unul cu warning/fail/skip;
- [ ] expandează request, trace, JSON și validation;
- [ ] confirmă `Contract validation` nested sub request-ul evaluat și un singur tabel;
- [ ] confirmă JSON scrollabil intern, fără overflow al paginii;
- [ ] verifică tag-urile în detaliile scenariului;
- [ ] verifică Fails: o singură zonă, sumar schematic și link stabil;
- [ ] confirmă că nu apar resurse externe sau folder auxiliar Extent.

Validarea vizuală este parte din testul de raportare. Build-ul verde singur nu
este suficient când schimbarea atinge DOM-ul sau CSS-ul.

