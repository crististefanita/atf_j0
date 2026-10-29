# Bune practici ATF — cod și testare

Acest document este owner-ul regulilor de implementare: module, OOP, modele,
constante, asset-uri de raportare și stratificarea testelor. Pentru redactarea
README-urilor și a feedback-ului se folosește
[README_Atf_DocumentationBestPractices.md](README_Atf_DocumentationBestPractices.md).

## Pe scurt

**Trasee rapide:** [ownership](#limite-de-responsabilitate-și-proprietari) ·
[OOP](#oop-și-modele) · [layere](#structura-testelor) ·
[probe](#verificarea-regulilor-de-cod)

Codul ATF trebuie să fie ușor de citit din boundary spre detaliu:

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":40,"padding":10,"diagramPadding":16}}}%%
flowchart LR
    Intent["Intenție"] -->|delimitează| Boundary["Boundary"]
    Boundary -->|formează| Model["Model typed"]
    Model -->|alimentează| Policy["Policy"]
    Policy -->|adaptează| Adapter["Adaptor"]
    Adapter -->|produce| Evidence["Dovadă"]
```

<details>
<summary><strong>Legendă — pipeline de implementare</strong></summary>

- intenția pornește din test, dar boundary-ul stabilește ownerul;
- modelul typed intră într-o policy pură înainte de adapterul extern;
- evidence-ul este output verificabil, nu logică ascunsă în renderer.
Etichetele scurte păstrează următorul sens:

| În schemă | Sens complet |
|---|---|
| Intenție | intenție de test |
| Boundary | boundary explicit |
| Model typed | model typed |
| Policy | policy / validator pur |
| Adaptor | adapter extern |
| Dovadă | evidence / artifact |

</details>

| Principiu | Dovadă preferată |
|---|---|
| boundary și ownership explicite | architecture guard + compile |
| modele imutabile și fațade mici | unit/component |
| module decuplate prin porturi | component/integration |
| output și asset-uri stabile | contract test + artifact audit |
| E2E minim și configurabil | selecție semantică + coverage matrix |

<details>
<summary><strong>Harta de lectură</strong></summary>

- [ownership](#limite-de-responsabilitate-și-proprietari),
  [suport E2E](#suport-e2e-reutilizabil-fără-abstractizare-forțată) și
  [schimbări protejate](#contracte-protejate-și-schimbări-controlate);
- [OOP și coeziune](#oop-și-modele),
  [constante](#constante-enum-uri-și-string-uri) și
  [importuri statice](#importuri-statice);
- [raportare](#raportare-html-css-și-javascript),
  [structura testelor](#structura-testelor),
  [configurare și căi](#configurare-și-căi) și
  [consolă](#consolă-și-raportare).

</details>

## Limite de responsabilitate și proprietari

| Zonă | Responsabilitate | Nu deține |
|---|---|---|
| `atf-core` | contracte runtime neutre și utilități reutilizabile | Extent, browser, fixture business, lifecycle de test |
| `atf-test-kit` | clasificare/adnotări de test și cleanup test-only failure-aware; alte primitive numai după probe | Playwright, browser lifecycle, Extent, profile sau flow-uri de consumer |
| `atf-reporting` | modele, renderer, asset-uri și contracte reporting standalone | mock, browser și aplicație consumatoare |
| `atf-mock` | transport/fixture determinist | reguli de randare |
| `atf-ui` | Playwright și politici browser generice | configurarea concretă a raportului |
| `atf-reporting-tests` | consumer, composition root și fixture-uri de reporting | producție reutilizabilă |
| `atf-tests` | governance whole-reactor | teste de produs sau fixture-uri concrete |

```text
consumer test
  → atf-core ports/policies
  → API/mock/UI adapters
  → atf-reporting
  → HTML/Extent artifact
```

`atf-reporting` trebuie să poată fi compilat și consumat independent. Orice
dependență către mock, browser sau fixture de aplicație rămâne în test scope-ul
consumerului. Compatibilitatea legacy se face într-un adapter cu owner și test,
nu prin reflecție răspândită în renderer.

## Suport E2E reutilizabil fără abstractizare forțată

Shop și AutoEx repetă unele responsabilități de test, dar diferența nu justifică
un modul UI nou. Prima țintă este mult mai mică: mutarea în `atf-test-kit` doar a
primitivelor neutre care se repetă și pot fi testate fără browser sau site.

- `atf-test-kit` deține deja `TestRunInitialization`, `CloseOnceResource`,
  `FailureAwareCleanup`, `FailureEvidenceCleanup` și `ArmedCleanup`;
  reutilizează aceste contracte, nu recrea inițializarea sau cleanup-ul local.
- Kit-ul nu primește Playwright, `Page`, sesiuni Extent, profile de captură,
  network inspector, YAML consumer sau vocabulary de journey. `atf-ui` îl
  consumă deja în test scope; dependența inversă ar crea un ciclu.
- Extensiile JUnit care deschid browserul, raportează evidence și închid sesiunea
  sunt astăzi adaptoare în `src/test` pentru fiecare consumer. Ținta refactorizării
  este un adaptor minim peste capabilități la owner, nu mutarea extensiei întregi
  în main; un helper JUnit comun intră în Kit numai cu un contract neutru demonstrat.
- Codul folosit exclusiv de teste nu stă în `src/main` doar pentru că un gate
  clasifică orice pachet `support` drept reutilizabil; ownership-ul se determină
  din apelanți, dependențe și source-set.
- `SETUP` și `TEST BODY` pot fi opționale în adaptoare, fără a impune aceeași
  secvență de business tuturor consumatorilor.
- AutoEx .NET nu consumă librăria Java; reutilizarea între limbaje înseamnă
  contracte comportamentale paralele, nu dependență comună de cod.

**Gate de extracție în Test Kit:**

- teste comportamentale Shop și AutoEx pentru achiziție parțială, close-once și
  eșecuri multiple de teardown;
- fără importuri `Shop*`, `AutoEx*`, `atf-ui` sau `atf-reporting`;
- dacă API-ul cere `Page`, sesiuni de raport, config, checkpoint-uri ori
  semantica rețelei, păstrează-l local;
- nu crea un modul nou pentru a ascunde un contract încă neclar.

Boundary-ul neutru al infrastructurii comune este documentat în
[README-ul Test Kit](../atf-test-kit/README.md); source-set-ul și compoziția
concretă rămân în README-ul ownerului fiecărui consumer.

### Testul exprimă intenția, main deține capabilitatea

- Mută în `src/main` logica reutilizabilă numai la ownerul real: runtime de
  aplicație la consumer, browser la UI, model/publicare la Reporting și
  infrastructură de test neutră în Kit-ul existent.
- Nu muta în main oracolul, fixture-ul artificial sau un runner ascuns ca să
  golești un director. Numele `support` nu decide source-set-ul.
- Păstrează testul declarativ: precondiție, acțiune business și rezultat observat;
  o metodă scurtă nu justifică un `execute()` fără contract ori assertions ascunse.
- Separă povestea business de canal, grupare și capturi; un profil de evidence
  diferit reutilizează journey-ul, nu îl copiază și nu îi elimină pași.
- Redu un harness câte o responsabilitate, cu probe nominale și failure-path;
  o fațadă stabilă păstrează testele lizibile în timpul extracției.
- Adaptoarele dependente de JUnit rămân minime la granița de execuție;
  extinderea unui API comun nu introduce dependențe UI/Reporting în Kit.

Flow-ul business, flow-ul UI, acțiunile compuse și operația Playwright atomică
au contracte diferite; validarea produce rezultate tipizate înaintea proiecției
în raport. Boundary-urile și probele țintă sunt în
[ghidul de testare](ATF_Testing_Guide.md#separarea-flow-urilor-business-ui-și-a-validărilor).

Cerințele încă neimplementate și ordinea sunt în
[feedback-ul refactorizării](feedback/ATF_Consumer_Business_Test_Focus_Feedback.md).

## Contracte protejate și schimbări controlate

README-urile ownerilor și testele-gate existente sunt parte din contractul de
mentenanță, nu material de curățare incidentală.

- [ ] Un gate existent nu este șters, relaxat, redenumit sau dezactivat doar
      pentru a face verde un run.
- [ ] Schimbarea unui gate sau a unui README normativ are cerință explicită,
      aprobare, motiv și dovadă nouă pentru contractul modificat.
- [ ] Un failure este urmărit către ownerul real: producție, test, fixture,
      renderer sau configurație; assertion-ul nu este mascat.
- [ ] Feedback-ul este actualizat dinamic în timpul investigației și păstrează
      doar gap-ul rămas, fără să devină o a doua sursă de contract.
- [ ] O regulă închisă este promovată în README-ul ownerului, iar feedback-ul
      este eliminat sau arhivat numai după ce testul confirmă închiderea.

## OOP și modele

- [ ] O clasă are un motiv dominant de schimbare.
- [ ] Fațada coordonează; nu citește YAML, nu face I/O, nu construiește HTML și
      nu decide verdicturi în aceeași metodă.
- [ ] Un `record`/POJO reprezintă o zonă semantică stabilă: setup, request,
      response, assertion, evidence, section sau verdict.
- [ ] Metadata de run pentru `Overview/System` folosește un snapshot typed și
      allowlist-uit (`ReportEnvironmentField`/`ReportEnvironmentSnapshot`);
      `Map<String,String>` rămâne numai boundary de compatibilitate.
- [ ] Builder-ul gestionează opționale; validatorul verifică invariants; niciunul
      nu ascunde efecte externe.
- [ ] Interfața reprezintă un port real care poate fi observat, înlocuit sau
      izolat (`ReportSource`, `ReportValidator`, `BrowserActions`).
- [ ] I/O, transport, browser și randare sunt adapters separate.

Un split de clasă este justificat de boundary semantic — facade, provider,
extractor, validator, publisher sau lifecycle — nu doar de numărul de linii.
Nu se creează pachete `misc` și nu se mută mecanic fișiere doar pentru a atinge
un număr.

### Coeziunea pachetelor — pragul de opt clase

```text
un pachet
   ├─ maximum 8 clase
   ├─ un boundary semantic dominant
   └─ peste prag → split coeziv sau excepție revizuită explicit
```

- `AtfPackageCohesionGovernanceTest` verifică producția modulelor enumerate în
  `SOURCE_GOVERNED_MODULES` și pachetele de test din toate modulele reactorului;
- orice modul cu surse `src/main/java` trebuie inclus în inventarul guvernat;
  `TEST_ONLY_COMPOSITION_MODULES` este rezervat modulelor fără cod de producție;
- un pachet peste opt clase nu devine automat acceptat printr-o allow-list;
- o excepție trebuie să aibă motiv semantic, owner și dovadă de boundary;
- diagnosticul guard-ului enumeră pachetul, numărul și clasele concrete;
- split-ul urmărește responsabilități — `api`, `runtime`, `evidence`,
  `lifecycle`, `support` — nu o împărțire arbitrară a fișierelor;
- feedback-ul păstrează hotspot-ul până când split-ul sau excepția revizuită
  este demonstrată de test.

<details>
<summary><strong>De ce pragul nu se rezolvă prin mutări mecanice</strong></summary>

Pragul de opt clase este un semnal de navigare și coeziune, nu o regulă de
formatare. Un pachet mare poate ascunde mai multe motive de schimbare; un
split bun separă ownership-ul și păstrează dependențele lizibile. Nu se creează
pachete artificiale și nu se șterg clase fără dovadă de nefolosire.
</details>

## Constante, enum-uri și string-uri

Valorile stabile ale contractului nu se repetă ca string-uri brute.

| Tip de valoare | Owner recomandat |
|---|---|
| HTTP method/header/media type | enum sau catalog HTTP |
| rută API | catalog de rute al ownerului API |
| adresă de aplicație / origin fictiv | identitate canonică la consumer / fixture numai în `src/test` |
| status reporting | `AtfReportStatus` |
| fază, tab, evidence type | vocabular reporting |
| CSS class, `id`, `data-*`, rol DOM | vocabular HTML/UI |
| culoare și spacing | token semantic de temă |
| payload strict local | builderul scenariului |

- [ ] Enum-ul reprezintă un vocabular închis și are conversie explicită spre
      textul rendererului.
- [ ] Constanta este pusă în ownerul semantic, nu într-un bucket global.
- [ ] Adresa canonică este o constantă `URI`; execuția folosește endpoint-ul
      configurat. O probă cu alt origin verifică propagarea override-ului.
- [ ] Rutele și header-ele provin din catalog, nu din testul concret.
- [ ] Textele variabile se construiesc cu `String.format` sau builder typed;
      nu se concatenează HTML în assertions.
- [ ] În afara oracolelor wire, un literal rămâne local numai când este un
      edge-case nereutilizabil și are nume semantic.
- [ ] Un oracol wire independent poate păstra explicit payload-ul și numele
      câmpurilor așteptate; nu se generează expected-ul prin serializerul testat.

### Importuri statice

Importurile statice sunt o scurtătură de citire, nu o scurtătură de ownership:

- assertions JUnit și DSL-uri de assertion stabile (`check`) sunt permise;
- constantele nominale dintr-un singur fixture semantic sunt permise când nu
  există ambiguitate (`PRODUCT_NAME`, `QUANTITY`, `COUNTRY`);
- wildcard imports, metode de business și valori dinamice nu se importă static;
- enum-urile și owner-ul unui contract rămân explicite când numele ar putea
  ascunde domeniul sau ar amesteca două vocabularuri.

```java
import static atf.shop.tests.api.support.PracticeShopApiScenarioFixture.PRODUCT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
```

## Raportare: HTML, CSS și JavaScript

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":40,"padding":10,"diagramPadding":16}}}%%
flowchart LR
    JavaFacade["Fațadă typed"] -->|expune| Semantic["Model"]
    Semantic -->|generează| Css[CSS]
    Semantic -->|generează| Js[JavaScript]
    Css -->|stilizează| Html["HTML"]
    Js -->|comportă| Html
    Html -->|dovedește| Contract["Verificare"]
```

<details>
<summary><strong>Legendă — asset-uri și contract vizual</strong></summary>

- Java compune și expune token-uri; CSS și JavaScript rămân asset-uri native pe responsabilități;
- modelul semantic este sursa comună pentru HTML, CSS și JavaScript;
- contractul component verifică asset-urile, iar E2E verifică efectul în browser.
Etichetele scurte păstrează următorul sens:

| În schemă | Sens complet |
|---|---|
| Fațadă typed | typed facade |
| Model | semantic model |
| HTML | HTML artifact |
| Verificare | component + browser proof |

</details>

- [ ] Java orchestrează; nu devine depozit de JavaScript inline.
- [ ] CSS este împărțit după responsabilitate: layout, theme, evidence,
      disclosure și responsive.
- [ ] JavaScript este împărțit după responsabilitate: DOM contract, disclosure,
      navigation, filters și controller.
- [ ] Clasele CSS, id-urile și `data-*` au vocabular centralizat.
- [ ] Locatorii de test preferă `element[attribute=value]`, `class~=` și
      `[id=value]`, nu shorthand-uri împrăștiate.
- [ ] Orice schimbare de markup/CSS/JS are test component și artifact fresh
      verificat în ATF Debug/browser MCP.

## Structura testelor

`support` este zonă auxiliară, nu al cincilea nivel de test.
Strategia de selecție, matricea module × layer și proof-ul potrivit fiecărui
boundary sunt deținute de [ATF_Testing_Guide.md](ATF_Testing_Guide.md); această
secțiune păstrează convențiile de structură și lizibilitate a codului de test.

```text
src/test/java/<owner>/
├─ <domain-or-feature>/       # organizare după comportament
│  └─ *Test.java              # layer declarat prin adnotare
└─ support/                   # fixture, builder, loader, adapter, locator
```

| Nivel | Verifică | Nu verifică |
|---|---|---|
| unit | model, policy, parser, validator, formatter | browser, server sau flux complet |
| component | fragment HTML/CSS/JS, asset, boundary de modul | aplicația completă |
| integration | colaborarea dintre module, mock și reporting | fiecare assertion deja testată jos |
| e2e | browser, tab-uri, filtre, click-uri și randare | toate combinațiile posibile |
| support | fixture, flow, locator, builder, adapter sau harness test-scoped | verdict executabil propriu |

- [ ] Fiecare clasă executabilă declară exact un layer prin adnotare.
- [ ] Adnotarea este sursa de adevăr; path-ul este doar convenție și diagnostic.
- [ ] Nu se creează directoare goale pentru layere inexistente.
- [ ] Support-ul nu devine layer executabil. Store-ul și extensia JUnit sunt
      test-only; runtime-ul main poate deține resurse prin porturi explicite,
      fără dependențe JUnit/Kit și cu probe pentru achiziție și cleanup.
- [ ] E2E rămâne mic, configurabil și selectează scenarii punctuale prin
      adnotări/configurație typed.
- [ ] Combinațiile exhaustive sunt coborâte în unit/component/integration.

Pentru API, mock-ul poate furniza `pass`, `fail`, `warning` și `skip`. Pentru
UI, inputul poate fi un raport static sau unul generat; validarea verifică
structura și randarea observabilă, fără să inventeze rezultate de business.

<details>
<summary><strong>Modelul unui test ATF elegant și plug-and-play</strong></summary>

### Ordinea de proiectare

1. **Alege layer-ul** — unit, component, integration sau E2E.
2. **Declară intenția** — adnotarea și metadata typed spun ce se testează, cu ce
   transport și cu ce precondiție.
3. **Încarcă inputul** — configurația/fixture-ul este rezolvat o singură dată,
   printr-un loader/harness reutilizabil.
4. **Compune flow-ul** — testul apelează un flow semantic, nu locatori, request
   factory, report session sau cleanup intern.
5. **Verifică efectul** — `expected` vine din config/fixture, `actual` vine din
   sistemul observat; assertions sunt scurte și reale.
6. **Publică evidence** — support-ul atașează acțiunile, observations și
   artifactul; testul nu reconstruiește HTML-ul.
7. **Închide prin owner** — extension/harness-ul închide browserul, transportul
   și sesiunea; testul nu adaugă `try/catch/finally`.

### Forma preferată

```text
@layer + metadata
class BusinessJourneyTest {
    @Test
    void configuredJourneyProducesExpectedOutcome() {
        flow.execute(configuredScenario());
        assertObservedOutcome();
    }
}
```

### Reguli de lizibilitate

- un test are un singur motiv de failure și un singur rezultat dominant;
- `given → when → then` este vizibil din numele metodelor sau al flow-ului;
- nu se repetă loader/context/locators în fiecare composition root;
- nu se folosesc assertions tautologice: `check(expected, expected, true)`;
- nu se acceptă `true` hardcodat ca dovadă a unei proprietăți observabile;
- rutele și starea se verifică semantic, nu prin `URL.contains(...)` sau text
  copiat din aceeași configurație;
- `contains` rămâne permis doar pentru smoke text și forbidden-secret checks;
- matricea de variații se mută în component/parameterized tests;
- E2E rămâne o dovadă de wiring și business journey, nu un loc pentru toate
  cazurile de status, body și selector;
- importurile statice sunt nominale și fac testul mai ușor de citit numai când
  owner-ul rămâne evident.

### Criteriu vizual pentru un test bun

```text
metadata → fixture → flow → observed assertions → evidence
```

Dacă cititorul vede înainte de flow inițializări de browser, directoare de
raport, request factory, selectori sau cleanup, composition root-ul este prea
încărcat și responsabilitatea trebuie mutată într-un harness/support typed.

</details>

### Date de test și configurare inițială cu efect extern

Un test autentificat separă sursa contului de journey-ul pe care îl verifică:

- sursa este aleasă explicit prin configurație/adnotare typed; preflight-ul
  validează profilul înainte de browser și înaintea oricărei mutații;
- reuse-ul unui fixture și crearea per-rulare sunt moduri distincte; numele și
  evidence-ul nu sugerează creare când s-a selectat doar un cont existent;
- achiziția contului și confirmarea sesiunii apar în `SETUP`; testul de business
  începe numai după un setup PASS;
- o creare externă se trimite o singură dată; dacă rezultatul este incert,
  execuția se oprește pentru reconciliere, nu repetă automat mutația;
- cheia de reuse corespunde scope-ului declarat (metodă, class-run sau suită),
  iar acel scope se dovedește comportamental, nu doar printr-un test al map-ului;
- raportul rezumă check-urile în `Setup evidence`, păstrează API exchange-ul
  separat când este colectat și redactează credențiale/date personale din
  acțiuni, assertions și capturi.

Detaliul contractului E2E aparține consumerului; forma generică a evidence-ului
este deținută de [ATF Reporting Contract](ATF_Reporting_Contract.md#context-intern-și-diagnostic-al-scenariului).

## Configurare și căi

- [ ] Nicio cale absolută nu apare în cod, comentarii, mesaj sau README.
- [ ] Root-ul, staging-ul și output-ul sunt rezolvate la boundary-ul de
      configurație/I/O.
- [ ] Discovery-ul de module/source-root și traversal-ul generic de fișiere stau
      în `atf-core.files`; testele nu reimplementează `locateModuleRoot()`,
      `sourceFiles()`, `user.dir` traversal sau `Files.walk` pentru aceeași
      responsabilitate.
- [ ] Helper-ul generic primește markere relative și excluderi explicite;
      nu cunoaște module concrete, package-uri de produs sau reguli ascunse ale
      unui singur test.
- [ ] Un helper nou se adaugă numai dacă API-ul existent nu exprimă contractul;
      în acest caz se extinde ownerul `files`, nu se creează o copie în consumer.
- [ ] YAML-ul configurează fixture și runtime; adnotarea exprimă selecția și
      intenția semantică a testului.
- [ ] Un output este referit prin nume logic sau placeholder configurabil,
      niciodată prin `Path.toString()` într-un raport permanent.

## Consolă și raportare

Consola și detaliile UI/API sunt active implicit în Reporting și Shop,
inclusiv fără YAML; opt-out-ul este explicit prin `reporting.console`.
Producătorul redactează datele, `atf-console` le formatează, iar proiecția nu
schimbă verdictul. `ConsoleDefaultsGovernanceTest` protejează configurația și
composition root-urile.

Testele modulelor, suportul și resursele lor se schimbă numai cu un control
tehnic structurat, exceptând `atf-test-practice-shop`. Guvernanța verifică
SHA-ul baseline-ului, controlul exact, starea, expirarea și comanda de probă;
regula include testele de guvernanță înseși. Procedura și configurarea CI sunt
în [README-ul atf-tests](../atf-tests/README.md#guvernanța-schimbării-testelor).

Regula de separare pentru consolă și reporting este:

- consola este responsabilitatea modulului `atf-console`;
- API/UI/reporting emit evenimente structurate și evidence, nu diagnostic direct în renderer;
- un pas observabil conține acțiune, țintă, rezultat și cauza clară la fail;
- verdictul aparține zonei care a eșuat efectiv: action, audit, assertion sau setup.

<details>
<summary><strong>Regula `try/catch`: support/runtime versus teste</strong></summary>

### Comportamentul dorit

Regula ATF este separată pe boundary:

- **support/runtime** — folosește `try-with-resources` pentru orice sesiune,
  raport sau resource `AutoCloseable`;
- **support/parser/config** — acceptă numai `catch` cu tip îngust, context și
  cauză; eroarea se propagă sau devine `Result`/evidence explicit;
- **teste** — nu conțin niciodată `try/catch` sau `try/finally` ad-hoc;
  testul exprimă intenția, acțiunea și expected-ul;
- **teardown/retry** — aparțin unui lifecycle helper, runner sau extension, nu
  corpului testului;
- **failure** — nu este convertit în `false`, `0`, listă goală sau fallback.

Anti-pattern-uri interzise:

```java
try {
    action();
} catch (Exception ignored) {
    // false-green: failure-ul a dispărut
}
```

```java
try {
    return parse(body);
} catch (RuntimeException error) {
    return false; // malformed input devine „nu există”
}
```

Forma preferată în clasele de support este:

```java
try (ExtentReportSession session = ExtentReportSession.openConfigured()) {
    publish(session);
}
```

Un parser poate converti o eroare într-un `Optional`/`Result` numai dacă absența
este parte din contract. Dacă inputul este malformed, caller-ul trebuie să poată
produce un failure distinct. Un validator UI poate păstra o linie `AtfAssertion`
cu status FAIL pentru o indisponibilitate, dar nu are voie să ocolească statusul,
să piardă cauza sau să lase scenariul PASS.

Checklist obligatoriu pentru un modul de test:

- [ ] Testele noi nu conțin `try/catch` sau `try/finally` în corpul testului.
- [ ] Nu există `catch` gol, `ignored`, `return false/0/empty` sau fallback
      implicit pentru o excepție de execuție/configurație.
- [ ] Orice catch din support păstrează contextul semantic, cauza și verdictul
      observabil; tipul excepției este cât mai îngust.
- [ ] Resource/session/report lifecycle folosește `try-with-resources` sau un
      helper comun; close failure nu maschează primary failure.
- [ ] Testele componente acoperă input malformed, selector indisponibil și
      eroare de teardown unde boundary-ul o poate întâlni.
- [ ] `assertThrows` este folosit pentru excepții așteptate; nu se scrie
      `try/catch` manual doar pentru a valida că o mutație este respinsă.

</details>

## Protocol de schimbare

- [ ] Recitește codul live și identifică owner-ul.
- [ ] Verifică dacă README-ul sau gate-ul atins este contract protejat; cere
      scope și aprobare explicită înainte de a-l schimba.
- [ ] Scrie întâi testul de contract pentru schimbarea propusă.
- [ ] Rulează în ordinea unit → component → integration → e2e relevant.
- [ ] Dacă se schimbă randarea, generează artifact fresh și verifică DOM,
      computed style, disclosure, overflow și console errors.
- [ ] Auditează fiecare `try/catch/finally` din diff; justifică resource
      management-ul și demonstrează că failure-ul nu este înghițit.
- [ ] Sincronizează selectiv în live; conflictul oprește fișierul afectat.
- [ ] Mută regula stabilă în README-ul ownerului și păstrează în feedback doar
      gap-ul rămas.

## Criterii de finalizare

- [ ] Responsabilitatea este mai clară decât înainte.
- [ ] Boundary-ul poate fi substituit printr-o interfață sau un adapter real.
- [ ] Constantele și enum-urile au ownership semantic.
- [ ] CSS/JavaScript au asset-uri și teste separate după responsabilitate.
- [ ] Testul de nivel inferior protejează schimbarea; E2E rămâne minim.
- [ ] Nu există `try` ad-hoc în corpul testului și nicio excepție nu este
      convertită în false-green; lifecycle-ul este centralizat și fail-safe.
- [ ] Raportul spune precis ce a eșuat.
- [ ] Documentația nu conține date de rulare, timestamp-uri, numărări de run,
      căi locale, linkuri `file:///` sau artifacte generate.

## Verificarea regulilor de cod

| Regulă | Owner executabil | Limita probei |
|---|---|---|
| Dependențe și namespace-uri | [ArchitectureModuleBoundaryTest](../atf-tests/src/test/java/atf/api/tests/governance/ArchitectureModuleBoundaryTest.java) | validează boundary-ul declarat, nu utilitatea fiecărei abstracții |
| Amplasarea responsabilităților | [ArchitectureOwnershipBoundaryTest](../atf-tests/src/test/java/atf/api/tests/governance/ArchitectureOwnershipBoundaryTest.java) | excepțiile sunt explicite; verdictul business rămâne la consumer |
| Coeziunea pachetelor | [AtfPackageCohesionGovernanceTest](../atf-tests/src/test/java/atf/api/tests/governance/AtfPackageCohesionGovernanceTest.java) | limita de inventar nu justifică împărțirea mecanică a unei clase |
| Layer și surse Java | [AtfTestLayerArchitectureTest](../atf-tests/src/test/java/atf/api/tests/governance/layers/AtfTestLayerArchitectureTest.java) | include rădăcinile din POM; nu demonstrează acoperirea funcțională |

[Comanda whole-ATF](../atf-tests/README.md#verificare-rapidă) rulează în staging.
Testele unit/component ale ownerului verifică comportamentul schimbat;
verdele guvernanței nu substituie aceste probe sau auditul de raport.

## Rubrica reevaluării modulelor

Nota modulului evaluează codul livrat și probele lui, separat de nota README-ului,
readiness-ul unei propuneri și prioritatea backlog-ului. Aceeași rubrică se
aplică tuturor modulelor; punctajele motivate se raportează în canalul reviziei.

| Atribut | Maximum | Ce susține punctajul |
|---|---:|---|
| Corectitudine | 30 | oracol independent, nominal/negativ și refuz fără efecte nepermise |
| Ownership și OOP | 25 | motive de schimbare delimitate, porturi și modele coerente, fără mutări mecanice |
| Testabilitate | 20 | probe la boundary, deterministe, capabile să refuze comportamentul greșit |
| Reziliență | 15 | cauză primară, restaurarea scope-ului, eliberare bounded și istoric fidel la eroare |
| Operare | 10 | configurare explicită, execuție și output auditabile, fără resurse ascunse |

- Un false PASS, pierderea cauzei funcționale, contaminarea unui scope sau un
  shutdown fără limită, reproduse în boundary-ul declarat, împiedică declararea
  unei note peste 91 până la rezolvarea și verificarea criteriului.
- Închiderea include proba negativă care eșuează pe comportamentul anterior,
  regresiile adiacente și guvernanța existentă; simpla adăugare a unui test nu
  acordă puncte dacă oracolul repetă implementarea.
- Pentru consumeri și schimbări vizibile în raportare, verificați câte un caz
  relevant individual și prin suită, în Extent/fallback unde se aplică; păstrați
  raportul unic, timing-ul, aserțiunile și expandările. Unit și fixture-urile
  Reporting își păstrează categoriile, fără artifacte business fabricate.
- Lipsa unei lacune demonstrate cere revalidare țintită, nu o refactorizare
  inventată pentru punctaj. Numărul de module, clase, teste ori linii nu
  substituie calitatea și nu extinde funcționalitatea incrementului.

Reevaluarea folosește codul live după sync și probele executate în staging;
nu promite automat puncte pentru închiderea unor checkbox-uri. Regula de
[păstrare a scorurilor în canal](README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale)
se aplică și acestei evaluări, fără scorecard datat în contractele proiectului.
