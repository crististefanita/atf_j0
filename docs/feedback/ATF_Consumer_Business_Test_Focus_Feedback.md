# ATF — responsabilitățile rămase ale consumerilor

## Pe scurt

Delegarea JUnit unică, redactarea înaintea plafonului, așteptările negative,
fazele lifecycle și rezultatul PASS/FAIL/SKIP sunt contracte ale
[Test Kit](../../atf-test-kit/README.md#comparații-junit-cu-dovadă-neutră) și
[Reporting](../../atf-reporting/README.md#proiecția-sigură-a-aserțiunilor).

Restanțele includ probele achiziției parțiale Shop, billing-ul observat în suită,
tranziția produsului AutoEx și echivalența profilelor.
Contractele deja livrate aparțin [AutoEx](../../atf-test-auto-ex/README.md),
[Shop](../../atf-test-practice-shop/README.md) și
[ghidului auditului componentelor](../ATF_Testing_Guide.md#auditul-componentelor-consumerilor).
Politica BE read-only este documentată la [UI](../../atf-ui/README.md#recuperare-be-prin-observație-pasivă);
auditul agregat păstrează [feedback separat](ATF_UI_Backend_Guided_Recovery_Feedback.md).

Încărcarea inițială bounded a catalogului are [contract la Shop](../../atf-test-practice-shop/README.md#încărcarea-inițială-a-catalogului).
Navigarea de la Fails la dovadă păstrează [criteriu separat Reporting](ATF_Reporting_Feedback.md#report-failure-deep-link-29--revenire-la-dovada-eșecului).

## Ordinea refactorizării

Scorul este prioritate: impact /40 + lacună /30 + lucrabil /20 + datorie
documentară /10. Nu reprezintă o notă a codului sau un rezultat de test.

| ID | Scor /100 | Diferența rămasă |
|---|---:|---|
| `CONSUMER-SHOP-BILLING-STATE-01` | 71 = 29 + 22 + 14 + 6 | diferență de valoare billing între suită și lansare individuală |
| `CONSUMER-AUTOEX-PRODUCT-TRANSITION-24` | 70 = 29 + 19 + 17 + 5 | timeout la detaliul produsului după click reușit, observat în suita filtrată |
| `CONSUMER-RUNTIME-BOUNDARY-03` | 61 = 22 + 14 + 18 + 7 | probă deterministă a achiziției parțiale în factory-ul main extras |
| `CONSUMER-E2E-PROFILES-01` | 57 = 24 + 10 + 17 + 6 | reducerea repetării live numai după echivalență demonstrată |

## CONSUMER-SHOP-BILLING-STATE-01 — valoarea observată în suita E2E

**Owner:** flow-ul și observația billing din Practice Shop.

- `ShopUiOnlySingleFlowPurchaseE2eTest` a trecut individual înainte și după
  suită, dar a eșuat în `ShopE2eSuite`: checkpoint-ul billing a așteptat
  `Alaska` și a observat `AlaskaAlaska` în câmpul state.
- Raportul comun a păstrat comparația FAIL. O singură suită eșuată nu stabilește
  încă o cauză deterministă a diferenței dintre traseele de execuție.

Codul curent alege adresa prin `checkout.billingStrategy`, independent de canal.
`configured-fields` compară cu fixture-ul; `postal-lookup` cere valorile generate
non-empty și le raportează. Un PASS al ultimei strategii nu probează egalitatea
cu fixture-ul și nu închide diferența istorică; limitele sunt la
[owner](../../atf-test-practice-shop/README.md#dovezi-și-comportament-vizibil).

- [ ] Reproduceți cu suita și clasa individuală, păstrând configurația și strategia billing;
      comparați ordinea acțiunilor, valorile DOM înainte și după lookup-ul postal,
      evenimentele de input și state-ul per scenariu. La `postal-lookup`, nu
      reintroduceți tastarea peste câmpurile pe care aplicația le completează.
- [ ] Stabiliți dacă dublarea vine din aplicația publică, din sincronizarea
      lookup-ului sau din adaptor; corectați numai la ownerul dovedit. Păstrați
      expected-ul din fixture și actual-ul citit din UI, fără normalizare care
      ar transforma `AlaskaAlaska` în PASS.
- [ ] Probați aceeași verificare în lansare individuală și prin suită; raportul
      FAIL trebuie să arate comparația și să permită expandarea până la rândul
      exact, iar raportul PASS trebuie să păstreze rândurile de validare.

Un succes al altei clase purchase sau al profilului UI+API nu demonstrează
criteriul clasei single-flow și nu închide această diferență.

## CONSUMER-AUTOEX-PRODUCT-TRANSITION-24 — detaliul produsului după navigare

**Prioritate: 70/100** = impact 29 + lacună 19 + lucrabil 17 + documentare 5.
**Owner:** AutoEx pentru tranziția business; UI pentru observația paginii și readiness.

| Lansare observată | Rezultat și limită |
|---|---|
| `AutoExPurchaseUiE2eTest` individual | PASS în verificarea randării; nu demonstrează izolarea întregii suite |
| Același caz prin `AutoExE2eSuite` filtrată | clickul `a[href^='/product_details/']` a trecut în 0,809 s; citirea `.product-information` a epuizat deadline-ul existent de 20 s înainte de checkout; JUnit și HTML au păstrat FAIL |
| O reluare cu aceeași configurație | PASS cu deadline nemodificat; cauza rămâne de stabilit, fără reproducere deterministă |

- [ ] Reproduceți cu aceeași clasă/configurație individual și prin suita filtrată;
      păstrați și artifactul eșuat, nu deduceți stabilitate dintr-o reluare verde.
- [ ] Observați URL-ul/documentul curent, titlul, locatorul și traficul browser
      disponibil înaintea citirii; delimitați pagina produsului de redirecționare,
      overlay și răspuns incomplet, fără a presupune una dintre aceste cauze.
- [ ] Corectați la ownerul dovedit și compuneți mecanismele existente de
      navigare/readiness; nu creșteți timeout-ul, nu repetați mutații și nu
      adăugați un al doilea recovery generic ori un transport care reia clickul.
- [ ] Probați nominalul și refuzul local înaintea journey-ului real; raportul
      păstrează acțiunea, durata, cauza și comparațiile, fără verdict PASS fabricat.

**Închidere:** tranziția este demonstrată cu expected independent în ambele
lansări, iar eșecul rămâne auditabil; randarea corectă nu închide robustețea journey-ului.


## CONSUMER-RUNTIME-BOUNDARY-03 — probele achiziției parțiale Shop

**Prioritate: 61/100** = impact 22 + lacună 14 + lucrabil 18 + documentare 7.
**Owner:** Shop; primitivele neutre rămân în Kit/UI/Reporting.

Achiziția browser/journal/captură, factory-ul raportului și proiecția metadata
sunt acum în main, cu sink și environment explicite. Harness-ul JUnit păstrează
Store-ul și composition root-ul; guvernanța verifică această delegare fără
relaxarea boundary-urilor existente. Contractul este la
[Shop](../../atf-test-practice-shop/README.md#ce-deține-modulul).

Probele existente acoperă cauza business plus publicare/close și profilele
scenariilor-surori. Rămâne o lacună mai îngustă în probele factory-ului extras:

- [ ] Introduceți o probă deterministă pentru achiziția parțială a capturilor
      ori journal-ului după deschiderea browserului, fără mutarea fixture-ului în main.
- [ ] Verificați că browserul se închide exact o dată, iar eșecul close-ului este
      suppressed pe cauza inițială; o achiziție reușită transferă ownership-ul harness-ului.
- [ ] Păstrați interfețele explicite și scope-urile Maven; nu construiți un container
      de servicii sau un wrapper generic numai pentru a satisface proba.

**Închidere:** proba negativă detectează o scurgere sau pierderea cauzei din
factory-ul real; un E2E reușit nu substituie acest caz de eroare.

### Reutilizare Shop/AutoEx — compuneți Kit-ul existent

Analiza adiacentă catalogului este [delimitată la UI](ATF_UI_Backend_Guided_Recovery_Feedback.md#ui-readiness-reuse-30--consolidare-fără-schimbarea-protocoalelor).
Browserul și politica de retry nu se mută în Kit; candidații neutri se evaluează
la granița JUnit, unde ambele module au deja dependența `test`.

**Shop este modelul de referință pentru alinierea AutoEx.** Extracția neutră
păstrează contractele Shop și îmbunătățește organizarea internă; prezența unei
primitive în AutoEx este o dovadă de utilizare, nu motiv să înlocuim modelul Shop.

Proiecția comună `ReportUiEnvironment` este deja consumată în main de ambele
module, la ownerul Reporting. `ReportGherkinSequence` este folosit în AutoEx;
aceste primitive și mapper-ele System nu sunt candidați de mutat în Kit.

| Loc inspectat | Refactorizare de evaluat, neimplementată |
|---|---|
| `ShopE2eContext.open/openBrowser` și `AutoExRunFactory.open` | Shop folosește `FailureAwareCleanup`; AutoEx compune `TestRunInitialization`. Păstrați ownership-ul și ordinea Shop, apoi evaluați compoziția Kit; nu copiați factory-ul AutoEx peste Shop și nu creați o bază ce cunoaște ambele aplicații |
| `ShopE2eTestSupport.SuiteReportResource` și `AutoExRunFactory.ReportResource` | ownership-ul raportului la Store este comun; Shop deține și conturi, iar AutoEx un `AutoExReportSession`. Evaluați `ScopedTestResource`/`CloseOnceResource` fără a muta Extent, credentiale sau profile în Kit |
| `AutoExRunLifecycleAdapter` și teardown-ul Shop | `FailureEvidenceCleanup`/`FailureAwareCleanup` sunt deja neutre. Compuneți ordinea existentă prin porturi locale; runtime-ul din main nu importă JUnit/Kit |
| Rezolvarea recovery din cele două factory-uri JUnit | `AtfUiRecoveryResolver` și planul sunt deja comune; trei linii de adaptare a configurației nu justifică un nou resolver ori un factory generic |

- [ ] Înaintea extracției, fixați prin probe diferențele de ownership: raport
      propriu vs raport comun, browser achiziționat parțial, conturile deținute și
      includerea politicii de publicare a căii pentru fiecare clasă din suită.
      Păstrați și metadatele System ale configurației efective per scenariu,
      inclusiv agregarea `Mixed`; nu reveniți la configurația primei clase.
- [ ] Verificați erori simultane la report close și account close. `finally`
      din `SuiteReportResource.close()` permite închiderea conturilor, dar poate
      masca eroarea raportului dacă ambele aruncă; constatarea este statică,
      iar proba de injecție și reconcilierea cauzelor rămân de făcut.
- [ ] Demonstrați două utilizări reale pentru orice API Kit nou, cu nominal,
      achiziție parțială, rollback, close repetat și erori multiple. Dacă primitivele
      existente acoperă contractul, închiderea se face prin compoziție, fără API nou.
- [ ] Porniți matricea de acceptanță de la Shop: componentă/E2E, individual/suită,
      UI/API/UI+API, faze și dovezi colapsabile. Aliniați AutoEx la contractele
      aplicabile prin adaptoare locale, fără a adăuga operații business sau faze
      artificiale numai pentru identitate structurală.
- [ ] Păstrați aserțiunile componentelor auditabile în pasul standard colapsat,
      un HTML individual/prin suită și contractul consolei după flush. Aceleași
      probe verifică și absența dublei publicări/închideri, cu limita 240 și fără
      creșterea numărului de module ori a timeout-urilor.

Aceste criterii completează `CONSUMER-RUNTIME-BOUNDARY-03`; nu declară defect
al Kit-ului și nu închid celelalte restanțe business sau de randare.

## CONSUMER-E2E-PROFILES-01 — variantele de evidence și acoperirea business

**Owner:** consumerii pentru selecție; Reporting Tests pentru contractele neutre.
Resolverul și probele Extent/fallback permit izolarea politicilor per scenariu;
ele nu demonstrează singure echivalența tuturor adaptoarelor consumerilor.

Inventarul din [README-ul modulului](../../atf-test-auto-ex/README.md#profile-uiapi-și-acoperirea-network-inspector)
mapează Account, Cart și Purchase UI/UI+API la aceleași intrări de journey,
separă politica de screenshot și păstrează Existing customer distinct pentru setup-ul API.
Aceasta dovedește reutilizarea codului, nu echivalența rezultatului live sau a evidence-ului randat.

- [ ] Acoperiți failure-ul remote și celelalte profile; o suită limitată la
      cazul pozitiv nu închide billing-ul observat în suita Shop completă.

Timing-ul explicit, capturile și publicarea au contract la
[Reporting](../../atf-reporting/README.md#timing-e2e-la-ownerul-runtime).
O lansare filtrată la un caz nu certifică izolarea tuturor claselor suitei.

- [ ] Înaintea reducerii selecției, demonstrați smoke-ul adaptorului pe succes
      și failure, plus contractele generice `ReportEvidenceProfileIntegrationTest`
      și `UiScreenshotPolicyIntegrationTest`.
- [ ] Păstrați lansarea manuală ori un profil echivalent și opt-in-urile
      independente pentru API read-only/mutații; fără suită pentru fiecare captură.
- [ ] Actualizați inventarul protejat numai după dovada echivalenței;
      `ConsumerSuiteCoverageGovernanceTest` și boundary-urile nu se relaxează
      pentru a ascunde cazuri eliminate.

## Extensii amânate

Evaluatorul general de modele (`CONSUMER-COMPONENT-ASSERTIONS-01`) rămâne
[design amânat la Validation](../../atf-validation/README.md#evaluator-general-pentru-modele).
Nu este un defect al auditului JUnit implementat și nu condiționează refactorizările
curente ori pasul standard de aserțiuni.

## Acceptanță pentru reevaluarea Shop și AutoEx

- Închideți întâi criteriile care pot confirma operația greșită sau pot executa
  o mutație fără precondiția business; extracția runtime-ului nu compensează un
  oracol insuficient. Billing-ul se compară cu aceeași strategie și configurație.
- Un test de componentă care evaluează comparații publică pasul standard de
  aserțiuni colapsat, cu expected independent, actual observat și rezultat
  original; fără întoarcerea verdictului din textul trunchiat.
- Probați refactorizarea cu un caz relevant individual și aceeași clasă prin
  suită în fiecare consumer afectat; raportul business rămâne unic pe suită,
  iar timing-ul, Fails → Tests și expandarea până la rând se păstrează.
- Primitivele reutilizabile sunt în main la Kit/UI/Reporting ori consumer;
  wiring-ul JUnit și fixture-ul de test rămân în test. Testele unitare și
  fixture-urile Reporting nu primesc artificial contractul E2E business.

Reevaluarea urmează [rubrica de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor),
după implementare și dovadă negativă; închiderea editorială nu ridică nota modulului.

## Controlul modificărilor

Schimbările noi urmează [baseline-ul tehnic](../ATF_Test_Changes_Baseline.md)
și [guvernanța testelor](../../atf-tests/README.md#guvernanța-schimbării-testelor).
Un control tehnic verificat nu închide criteriile business de mai sus.
Verificarea și promovarea folosesc [staging-ul izolat](../ATF_Build_And_Execution_Safety.md#regula-de-lucru-pentru-orice-schimbare).
