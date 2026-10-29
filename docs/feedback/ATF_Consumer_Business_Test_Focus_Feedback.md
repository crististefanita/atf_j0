# ATF — responsabilitățile rămase ale consumerilor

## Pe scurt

Delegarea JUnit unică, redactarea înaintea plafonului, așteptările negative,
fazele lifecycle și rezultatul PASS/FAIL/SKIP sunt contracte ale
[Test Kit](../../atf-test-kit/README.md#comparații-junit-cu-dovadă-neutră) și
[Reporting](../../atf-reporting/README.md#proiecția-sigură-a-aserțiunilor).

Restanțele includ cauzalitatea capturii order, identitatea/coșul înainte de checkout, runtime-ul E2E,
billing-ul observat în suită, catalogul inițial Shop, tranziția produsului AutoEx și echivalența profilelor.
Contractele deja livrate aparțin [AutoEx](../../atf-test-auto-ex/README.md),
[Shop](../../atf-test-practice-shop/README.md) și
[ghidului auditului componentelor](../ATF_Testing_Guide.md#auditul-componentelor-consumerilor).
Politica BE read-only este documentată la [UI](../../atf-ui/README.md#recuperare-be-prin-observație-pasivă);
auditul agregat păstrează [feedback separat](ATF_UI_Backend_Guided_Recovery_Feedback.md).

## Ordinea refactorizării

Scorul este prioritate: impact /40 + lacună /30 + lucrabil /20 + datorie
documentară /10. Nu reprezintă o notă a codului sau un rezultat de test.

| ID | Scor /100 | Diferența rămasă |
|---|---:|---|
| `CONSUMER-SHOP-REQUEST-START-06` | 82 = 34 + 24 + 17 + 7 | o cerere anterioară, terminată după marcaj, poate valida noul Confirm |
| `UI-LOCATOR-READINESS-01` | 79 = 32 + 23 + 17 + 7 | identitate și coș verificate după înregistrare, înaintea mutației checkout |
| `CONSUMER-RUNTIME-BOUNDARY-03` | 77 = 31 + 18 + 20 + 8 | achiziție, publicare și lifecycle E2E rămase în contextul Shop |
| `CONSUMER-SHOP-BILLING-STATE-01` | 71 = 29 + 22 + 14 + 6 | diferență de valoare billing între suită și lansare individuală |
| `CONSUMER-AUTOEX-PRODUCT-TRANSITION-24` | 70 = 29 + 19 + 17 + 5 | timeout la detaliul produsului după click reușit, observat în suita filtrată |
| `CONSUMER-SHOP-CATALOG-AVAILABILITY-27` | 65 = 25 + 18 + 17 + 5 | search disponibil, dar primul produs nu devine vizibil în suita filtrată; cauza rămâne de stabilit |
| `CONSUMER-E2E-PROFILES-01` | 57 = 24 + 10 + 17 + 6 | reducerea repetării live numai după echivalență demonstrată |

## CONSUMER-SHOP-REQUEST-START-06 — cerere anterioară terminată după Confirm

**Prioritate: 82/100** = impact 34 + lacună 24 + lucrabil 17 + documentare 7.
**Owner:** UI pentru identitatea generică a capturii; Shop pentru asocierea
operației business. Rendererul proiectează datele primite, fără decizie de corelare.

`ShopApiNetworkJournal.recordCapturedExchange()` alocă secvența la recepția
răspunsului, iar `orderAfter()` filtrează după acea secvență. Proba controlată
cu browser și HTTP loopback reproduce același rezultat pentru `/invoices/guest`
și `/invoices`, atât prin adaptorul Page, cât și prin `UiNetworkInspector`:

1. Porniți un POST order și mențineți răspunsul în așteptare pe server.
2. Luați marcajul și apăsați o dată Confirm; handlerul nou nu trimite alt POST,
   iar pagina păstrează confirmarea facturii vechi.
3. Eliberați răspunsul anterior, cu ID/factură și totalul compatibil cu fixture-ul.
4. `awaitOrderCreated()` îl selectează, iar cele cinci comparații dau PASS;
   serverul a primit exact o cerere înainte și după noul Confirm.

Proba folosește hostul Shop mapat local în Chromium; nu afirmă că aplicația
publică reproduce acest caz. Ea demonstrează că ordinea recepției nu dovedește
începutul request-ului. Selecția ultimului status și eliminarea cache-ului vechi
la captură absentă rămân contracte distincte, deja implementate la
[owner](../../atf-test-practice-shop/README.md#dovada-comenzii-curente).

- [ ] Corelați începutul cererii cu acțiunea curentă printr-o identitate typed
      observată la boundary-ul UI. Nu înlocuiți corelarea cu sleep, un timeout
      mai mare sau presupunerea că toate cererile vechi s-au terminat.
- [ ] O observație anterioară încă în zbor nu satisface noul Confirm; identitatea
      indisponibilă rămâne explicită, fără succes dedus din factura deja vizibilă.
- [ ] Probați răspuns vechi întârziat + cerere nouă absentă/201/500, pentru ambele
      rute și adaptoare, cu câte un click și fără dispatch suplimentar.
- [ ] Păstrați răspunsul vechi în istoricul raw, ordinea capturilor, selecția
      checkpoint-urilor surori, totalul independent, raportul unic și plafonul 240.

**Închidere:** comanda selectată aparține invocării curente, nu doar unei
ferestre de recepție compatibile; cazul negativ rămâne FAIL auditabil.

## UI-LOCATOR-READINESS-01 — identitate și coș înaintea checkout-ului

**Prioritate: 79/100** = impact 32 + lacună 23 + lucrabil 17 + documentare 7.
**Owner:** flow-ul business AutoEx; sincronizarea controlului rămâne la UI.

[Readiness-ul local](../../atf-test-auto-ex/README.md#readiness-înainte-de-checkout)
are probe pentru țintă întârziată, absentă și ambiguă. Restanța privește starea
business după înregistrare: `AutoExPurchaseUiFlow.reviewCheckout()` redeschide
coșul și cheamă `proceedToCheckout()` înaintea citirii datelor de review.
Validarea coșului din etapa anterioară înregistrării nu probează această tranziție.

- [ ] Verificați identitatea deținută și produsul/cantitatea din coș după Continue,
      înainte de checkout, cu expected independent de observația curentă.
- [ ] O sesiune pierdută, un cont străin sau un coș greșit opresc mutația și publică
      comparația FAIL; nu re-adăugați produsul și nu creați alt cont.
- [ ] Probați refuzul și cazul nominal individual și prin suita filtrată la același
      caz; păstrați clickul unic, raportul comun și scope-ul scenariului.

**Închidere:** aceeași precondiție business este verificată înaintea tranziției
în ambele lansări. Proba locală a locatorului sau un purchase reușit nu înlocuiesc
cazurile negative pentru identitate și coș.

## CONSUMER-RUNTIME-BOUNDARY-03 — responsabilitățile E2E rămase în Shop

**Owner:** Shop pentru runtime-ul aplicației; Kit/UI/Reporting pentru primitivele neutre.

Protocolul componentă și runtime-ul API sunt extrase; `ShopE2eContext` păstrează
achiziția browser/API, configurarea raportului, capturile și scenariile E2E.
`FailureAwareCleanup` este deja compus; restanța privește separarea ownerilor,
nu introducerea încă unei primitive de cleanup. `openReport(...)` are deja sink
explicit și `openBrowser(...)` este separat ca metodă; ultimul încă citește
`AtfConsoleTestContext` și construiește resursele în source-set-ul de test.

- [ ] Desprindeți compoziția runtime din cele două factory-uri deja delimitate
      de wiring-ul JUnit; browserul primește explicit sink-ul și environment-ul.
      `AtfConsoleTestContext`, Store-ul, adnotările și rezolvarea fixture-ului rămân
      în test. Nu mutați clasele prin schimbarea package-ului și a scope-ului Kit.
- [ ] Extrageți câte o responsabilitate runtime către main la owner, păstrând fațada
      business; Store-ul JUnit, fixture-ul și oracolul rămân în test.
- [ ] Compuneți primitivele Kit existente pentru resurse neutre; nu mutați browser,
      Extent sau configurație de aplicație în Kit și nu creați o bază JUnit cu resurse ascunse.
- [ ] Probați achiziția parțială și eșecuri simultane la captură, diagnostic, complete
      și close; fiecare etapă se încearcă o dată, cu cauza primară și erorile secundare păstrate.
- [ ] Păstrați scope-urile Maven și
      [boundary-ul guvernat](../../atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerModuleArchitectureGovernanceTest.java).

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


## CONSUMER-SHOP-CATALOG-AVAILABILITY-27 — catalog inițial fără produse vizibile

**Prioritate: 65/100** = impact 25 + lacună 18 + lucrabil 17 + documentare 5.
**Owner:** Shop pentru precondiția catalogului; UI pentru mecanica așteptării native.

| Lansare observată | Rezultat și limită |
|---|---|
| `ShopPurchaseE2eTest` prin `ShopE2eSuite` filtrată | navigarea și search-ul devin disponibile; primul `a[data-test^='product-']` nu devine vizibil în deadline-ul existent de 20 s; un HTML păstrează FAIL înaintea achiziției |
| Același caz individual | PASS cu aceeași configurație și un HTML; nu separă încă indisponibilitatea aplicației, presiunea locală și alte cauze posibile |

- [ ] Reproduceți aceeași clasă individual/prin suită, cu resurse locale izolate;
      păstrați artifactul FAIL și nu deduceți stabilitate dintr-o reluare verde.
- [ ] Înregistrați URL-ul, starea documentului, numărul cardurilor și traficul
      disponibil în fereastra așteptării; fără a atribui timeout-ul serviciului
      sau codului înainte de dovada cauzei.
- [ ] Probați local tranziția search vizibil → produse disponibile și refuzul
      când produsele rămân absente; reutilizați readiness-ul existent la owner,
      fără creșterea deadline-ului ori reluarea întregului journey.
- [ ] Păstrați în Extent operația eșuată, durata, cauza și politica de captură;
      un checkpoint neexecutat nu primește aserțiuni ori verdict PASS fabricate.

**Închidere:** precondiția catalogului este demonstrată în ambele lansări,
inclusiv prin proba negativă; randarea corectă nu închide disponibilitatea journey-ului.


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
