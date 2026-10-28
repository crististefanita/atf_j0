# ATF — responsabilitățile rămase ale consumerilor

## Pe scurt

Delegarea JUnit unică, redactarea înaintea plafonului, așteptările negative,
fazele lifecycle și rezultatul PASS/FAIL/SKIP sunt contracte ale
[Test Kit](../../atf-test-kit/README.md#comparații-junit-cu-dovadă-neutră) și
[Reporting](../../atf-reporting/README.md#proiecția-sigură-a-aserțiunilor).

Restanțele includ corelarea payment și totalul comenzii Shop, runtime-ul E2E,
billing-ul observat în suită și echivalența profilelor înaintea reducerii selecției.
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
| `CONSUMER-SHOP-PAYMENT-CAUSALITY-02` | 83 = 34 + 24 + 19 + 6 | răspuns payment reușit vechi selectat înaintea unui eșec mai nou |
| `CONSUMER-RUNTIME-BOUNDARY-03` | 77 = 31 + 18 + 20 + 8 | achiziție, publicare și lifecycle E2E rămase în contextul Shop |
| `CONSUMER-SHOP-ORDER-TOTAL-02` | 72 = 30 + 20 + 16 + 6 | totalul pozitiv nu este corelat cu prețul și cantitatea cumpărate |
| `CONSUMER-SHOP-BILLING-STATE-01` | 71 = 29 + 22 + 14 + 6 | diferență de valoare billing între suită și lansare individuală |
| `CONSUMER-COMPONENT-ASSERTIONS-01` | 63 = 25 + 18 + 16 + 4 | evaluator neutru reutilizabil în Validation, după corectarea boundary-urilor existente |
| `CONSUMER-E2E-PROFILES-01` | 57 = 24 + 10 + 17 + 6 | reducerea repetării live numai după echivalență demonstrată |

## CONSUMER-SHOP-PAYMENT-CAUSALITY-02 — răspunsul operației payment curente

**Owner:** selecția dovezilor din `ShopApiNetworkJournal` și etapa checkout Shop.
Corecția privește validarea operației existente, fără retry ori replay de payment.

- `paymentConfirmed()` caută ultimul `POST /payment/check` cu status 200;
  filtrarea succesului precedă alegerea răspunsului curent.
- Proba controlată prin subscripția publică a inspectorului capturează un 200
  la secvența 1 și un 500 la secvența 2. Validarea payment publică secvența 1 și
  toate comparațiile trec, deși ultimul răspuns capturat este eșuat.
- `orderCreated(afterSequence, ...)` și registration au deja prag de secvență;
  aceeași delimitare nu este transmisă de etapa `checkPayment()`.

- [ ] Capturați pragul înaintea acțiunii UI existente și selectați mai întâi
      răspunsul corelat cu acea acțiune; evaluați apoi statusul și body-ul.
- [ ] Păstrați unicul click, metoda, ruta și verdictul curent; un 500 sau lipsa
      unui răspuns nou nu poate împrumuta un PASS din istoricul anterior.
- [ ] Probați succesul vechi urmat de eșec nou, lipsa unui răspuns nou, două
      răspunsuri valide și trafic pentru altă rută; publicați secvența selectată.
- [ ] Verificați și perechea cart add/read, unde selecția curentă filtrează tot
      statusul 200; aplicați numai corelarea demonstrată, fără framework nou de retry.
- [ ] Păstrați un singur raport, assertion-urile expandabile și limita 240 în
      lansare individuală și prin suită; cazul negativ trebuie să rămână FAIL.

Proba delimitează un risc reproductibil al selectorului; nu demonstrează că
purchase-urile remote trecute anterior au folosit un răspuns vechi.

## CONSUMER-RUNTIME-BOUNDARY-03 — responsabilitățile E2E rămase în Shop

**Owner:** Shop pentru runtime-ul aplicației; Kit/UI/Reporting pentru primitivele neutre.
Protocolul componentă și runtime-ul API sunt extrase; `ShopE2eContext` păstrează
achiziția browser/API, configurarea raportului, capturile și scenariile E2E.

- [ ] Extrageți câte o responsabilitate runtime către main la owner, păstrând fațada
      business; Store-ul JUnit, fixture-ul și oracolul rămân în test.
- [ ] Compuneți primitivele Kit existente pentru resurse neutre; nu mutați browser,
      Extent sau configurație de aplicație în Kit și nu creați o bază JUnit cu resurse ascunse.
- [ ] Probați achiziția parțială și eșecuri simultane la captură, diagnostic, complete
      și close; fiecare etapă se încearcă o dată, cu cauza primară și erorile secundare păstrate.
- [ ] Păstrați scope-urile Maven și
      [boundary-ul guvernat](../../atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerModuleArchitectureGovernanceTest.java).

## CONSUMER-SHOP-ORDER-TOTAL-02 — totalul comenzii față de cumpărăturile curente

**Owner:** modelele și validarea business Shop, în modulele existente.
Este completarea purchase-ului declarat, păstrând profilele UI și UI+API.

- `ShopApiAssertions.order()` verifică numai `total > 0`, alături de identitatea
  comenzii și corelarea facturii; nu primește un total așteptat independent.
- O probă cu JSON valid, factura corelată și `total=0.01` trece toate comparațiile.
  `ShopCartPage.CartLine` păstrează numele și cantitatea, fără preț ori total.

- [ ] Propagați în flow-ul existent prețul, cantitatea și totalul așteptat din
      observațiile anterioare plății; nu construiți expected din răspunsul comenzii.
- [ ] Reprezentați valorile monetare exact și documentați moneda și regula
      aplicației pentru calcul; nu introduceți rotunjiri sau taxe presupuse.
- [ ] Comparați independent totalul și identitatea comenzii cu datele cumpărăturii;
      în UI+API păstrați și corelarea facturii deja implementată.
- [ ] Probați un total pozitiv greșit, cantitate/preț greșite, câmp lipsă, tip greșit
      și factura străină; un simplu HTTP 2xx nu poate transforma diferența în PASS.
- [ ] Verificați rândul exact expected/actual în raport, cu plafonul 240 păstrat,
      pentru flow-ul guest și cel autentificat, individual și prin suită.

Aceasta este o limită a validării actuale, fără dovadă că aplicația demo a
calculat greșit totalurile rulărilor remote observate.

## CONSUMER-SHOP-BILLING-STATE-01 — valoarea observată în suita E2E

**Owner:** flow-ul și observația billing din Practice Shop.

- `ShopUiOnlySingleFlowPurchaseE2eTest` a trecut individual înainte și după
  suită, dar a eșuat în `ShopE2eSuite`: checkpoint-ul billing a așteptat
  `Alaska` și a observat `AlaskaAlaska` în câmpul state.
- Raportul comun a păstrat comparația FAIL. O singură suită eșuată nu stabilește
  încă o cauză deterministă a diferenței dintre traseele de execuție.

- [ ] Reproduceți cu suita și clasa individuală, păstrând exact aceeași configurație;
      comparați ordinea acțiunilor, valorile DOM înainte și după lookup-ul postal,
      evenimentele de input și state-ul per scenariu. Nu reintroduceți tastarea
      peste câmpurile pe care lookup-ul Shop le completează.
- [ ] Stabiliți dacă dublarea vine din aplicația publică, din sincronizarea
      lookup-ului sau din adaptor; corectați numai la ownerul dovedit. Păstrați
      expected-ul din fixture și actual-ul citit din UI, fără normalizare care
      ar transforma `AlaskaAlaska` în PASS.
- [ ] Probați aceeași verificare în lansare individuală și prin suită; raportul
      FAIL trebuie să arate comparația și să permită expandarea până la rândul
      exact, iar raportul PASS trebuie să păstreze rândurile de validare.

Un succes al altei clase purchase sau al profilului UI+API nu demonstrează
criteriul clasei single-flow și nu închide această diferență.

## CONSUMER-COMPONENT-ASSERTIONS-01 — evaluatorul neutru al componentelor

**Owner al restanței:** Validation pentru evaluarea pură reutilizabilă.
Fațada JUnit, politica rezultatului și publicarea componentelor sunt implementate.

`ValidationEvaluator` evaluează deja contracte API; [limita sa actuală](../../atf-validation/README.md#limita-față-de-auditul-testelor-componente)
exclude evaluatorul general pentru modele și componente. Acest item este o
extindere de contract, nu un defect al auditului componentelor existent.

Un API nou de evaluare rămâne în afara incrementului curent de mentenanță;
auditul implementat nu depinde de această propunere.

| Owner | Contract rămas |
|---|---|
| Testul consumerului | expected independent, observație și regula business |
| Validation | evaluare pură și rezultat imutabil, reutilizând modelele Core potrivite |
| Adaptorul consumerului | conversie către Kit/Reporting fără repetarea comparației |

- [ ] Probați un model, un adaptor API și un adaptor UI din ambele module înaintea
      extinderii API-ului; introduceți numai operatorii demonstrați de aceste cazuri.
- [ ] Păstrați tipurile, `null` și mesajul concis când diferența depășește prefixul afișat;
      limita implicită rămâne 240 inclusiv `…`, cu redactare înaintea plafonului.
- [ ] Păstrați politicile JUnit și excepțiile așteptate în Kit, iar evaluarea pură în
      Validation; fără dependență Kit către Core, Validation sau Reporting.

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

Timing-ul E2E explicit rămâne în [feedback-ul Reporting](ATF_Reporting_Feedback.md#report-e2e-phase-timing-01--execuția-e2e-separată-de-agregarea-dovezilor).
Agregarea coerentă între taburi nu certifică durata integrală a fazei.

- [ ] Înaintea reducerii selecției, demonstrați smoke-ul adaptorului pe succes
      și failure, plus contractele generice `ReportEvidenceProfileIntegrationTest`
      și `UiScreenshotPolicyIntegrationTest`.
- [ ] Păstrați lansarea manuală ori un profil echivalent și opt-in-urile
      independente pentru API read-only/mutații; fără suită pentru fiecare captură.
- [ ] Actualizați inventarul protejat numai după dovada echivalenței;
      `ConsumerSuiteCoverageGovernanceTest` și boundary-urile nu se relaxează
      pentru a ascunde cazuri eliminate.

## Controlul modificărilor

Incrementul se închide conform [ghidului de testare](../ATF_Testing_Guide.md#inventarul-executabil-după-mutări-și-reclasificare)
și [protocolului de staging](../ATF_Build_And_Execution_Safety.md#regula-de-lucru-pentru-orice-schimbare).
Controlul Git de mai jos protejează testele modificate; nu certifică echivalența
tuturor profilelor remote sau extracția runtime-ului E2E Shop.

<details>
<summary><strong>Controale tehnice ale incrementelor</strong></summary>

```yaml
testChanges:
  - id: CONSUMER-CHECKOUT-READINESS-06
    status: ACTIVE
    base: 3219ef1ff546369d00a006cfc7434644251c6f9a
    expires: 2026-11-26
    reason: Adaugă probe browser locale pentru checkout întârziat, absent și ambiguu, după failure live matched 0 cu butonul prezent în captura ulterioară; păstrează clickul unic, comparațiile business și lipsa replay-ului mutațiilor.
    proof: mvn -pl atf-test-auto-ex,atf-tests -am "-Dtest=AutoExCheckoutReadinessComponentTest,AutoExCartDomComponentTest,AutoExPurchaseUiAdapterComponentTest,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=3219ef1ff546369d00a006cfc7434644251c6f9a" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/purchase/AutoExCheckoutReadinessComponentTest.java
  - id: CONSUMER-PURCHASE-STAGES-05
    status: ACTIVE
    base: 3219ef1ff546369d00a006cfc7434644251c6f9a
    expires: 2026-11-26
    reason: Adaugă probe auditate ale gate-urilor purchase AutoEx și catalog Shop; păstrează oracolele și selecțiile existente, identitatea erorilor și oprirea înaintea următoarei mutații.
    proof: mvn -pl atf-test-auto-ex,atf-test-practice-shop,atf-tests -am "-Dtest=AutoExPurchaseFlowComponentTest,AutoExPurchaseUiAdapterComponentTest,AutoExCartFlowComponentTest,AutoExPurchaseBusinessComponentTest,ShopCatalogPurchaseFlowComponentTest,ShopCheckoutFlowComponentTest,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=3219ef1ff546369d00a006cfc7434644251c6f9a" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/purchase/AutoExPurchaseUiAdapterComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/purchase/AutoExPurchaseFlowComponentTest.java
  - id: CONSUMER-AUDIT-SURGICAL-04
    status: ACTIVE
    base: 0a20e7c38f19ef02334e44419a2974d5fd01e398
    expires: 2026-11-26
    reason: Păstrează selecțiile și oracolele existente; probează cleanup GET fără replay, ownership class/suite și redacția diagnosticului. Corectează falsul pozitiv al inventarului care trata o suită-fixture nested drept gate nou; identifică declarația ownerului și păstrează allowlist-ul, acoperirea disjunctă și respingerea unui gate suplimentar real.
    proof: mvn -pl atf-test-auto-ex,atf-tests -am "-Dtest=AutoExAccountCleanupRecoveryComponentTest,AutoExStoreAvailabilityComponentTest,AutoExAccountLifecycleComponentTest,AutoExApiReportScopeComponentTest,AutoExApiContractReportPublisherComponentTest,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=0a20e7c38f19ef02334e44419a2974d5fd01e398" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/navigation/AutoExAccountCleanupRecoveryComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/navigation/AutoExCleanupBrowserFixture.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/api/AutoExApiReportScopeComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/api/AutoExApiContractReportPublisherComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/api/AutoExApiContractReportExtension.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/suites/AutoExApiLiveSuite.java
      - atf-tests/src/test/java/atf/api/tests/governance/layers/ConsumerSuiteCoverageGovernanceTest.java
  - id: CONSUMER-AUDIT-BOUNDARIES-03
    status: ACTIVE
    base: 0a20e7c38f19ef02334e44419a2974d5fd01e398
    expires: 2026-11-26
    reason: Păstrează aserțiunile existente și adaugă probe pentru evaluare unică, redacție înaintea plafonului, recorder/fațadă, lifecycle, abort, template și erori de publicare; compune adaptorul AutoEx și întărește inventarul semantic al auditului fără relaxarea regulilor ori a selecției existente.
    proof: mvn -pl atf-test-kit,atf-validation,atf-test-auto-ex,atf-test-practice-shop,atf-reporting-tests,atf-tests -am "-Dtest=AssertionProjectionTest,AtfJavaSourceReferencesTest,RedactionStrategiesTest,ConsumerAuditSourceGraphTest,ComponentAuditExtensionComponentTest,ValidationPreviewRedactionTest,AutoExComponentSuite,ShopComponentSuite,AssertionAuditPublicationComponentTest,ComponentAssertionStepReportTest,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=0a20e7c38f19ef02334e44419a2974d5fd01e398" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-test-kit/src/test/java/atf/testkit/assertions/AssertionProjectionTest.java
      - atf-core/src/test/java/atf/core/security/RedactionStrategiesTest.java
      - atf-core/src/test/java/atf/core/files/AtfJavaSourceReferencesTest.java
      - atf-test-kit/src/test/java/atf/testkit/assertions/ComponentAuditExtensionComponentTest.java
      - atf-validation/src/test/java/atf/validation/ValidationPreviewRedactionTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/extension/AutoExExtentTestExtension.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/assertions/AssertionAuditPublicationComponentTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerModuleArchitectureGovernanceTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerAuditSourceGraph.java
      - atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerAuditSourceGraphTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/layers/AtfTestLayerArchitectureTest.java
  - id: CONSUMER-COMPONENT-AUDIT-ALL-02
    status: ACTIVE
    base: db5f06c83ede6422b9509350136da9eca6111aa2
    expires: 2026-11-26
    reason: Mută numai apelurile JUnit ale componentelor pe fațada auditabilă comună; păstrează overload-urile, expected/actual și verdictul, excepțiile așteptate și izolarea lansărilor imbricate; adaugă probele neutre și guard-ul fără a relaxa regulile existente.
    proof: mvn -pl atf-test-kit,atf-test-auto-ex,atf-test-practice-shop,atf-tests -am "-Dtest=AuditedAssertionsTest,AuditedAssertionScopeComponentTest,AutoExComponentSuite,ShopComponentSuite,WholeAtfGovernanceSuiteTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/api/AutoExApiContractComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/api/AutoExApiContractReportPublisherComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/api/AutoExApiResponseAssertionsComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/api/AutoExCatalogApiPreconditionComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/AutoExAccountCleanupDecisionComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/AutoExCatalogAssertionsComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/AutoExEvidenceProfileComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/AutoExPrivacyConsentHandlerComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/AutoExPurchaseTraceStepTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/AutoExStoreAssertionsComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/configuration/AutoExConfigurationLoaderComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExAccountDeletionSafetyComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExAccountLifecycleComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExCartDomComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExCartFlowComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExCheckoutAddressComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExInvoiceComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExPurchaseBusinessComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/lifecycle/AutoExRunLifecycleAdapterComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/lifecycle/AutoExRuntimeBoundaryComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/navigation/AutoExStoreAvailabilityComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/pages/AutoExPagePortsComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/extension/AutoExExtentTestExtension.java
      - atf-test-kit/src/test/java/atf/testkit/assertions/AuditedAssertionScopeComponentTest.java
      - atf-test-kit/src/test/java/atf/testkit/assertions/AuditedAssertionsTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerModuleArchitectureGovernanceTest.java
  - id: CONSUMER-COHESION-INVENTORY-01
    status: ACTIVE
    base: 466a045e95a7336b183a7d75e0b369f8deb3082c
    expires: 2026-11-26
    reason: Actualizează inventarul exact al pachetului journey AutoEx cu AutoExAccountJourney deja prezent în sursele main; păstrează limita de coeziune și verificările tuturor pachetelor.
    proof: mvn -pl atf-tests -am "-Dtest=AtfPackageCohesionGovernanceTest,WholeAtfGovernanceSuiteTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-tests/src/test/java/atf/api/tests/governance/AtfPackageCohesionGovernanceTest.java
  - id: CONSUMER-COMPONENT-ASSERTION-STEP-01
    status: ACTIVE
    base: 466a045e95a7336b183a7d75e0b369f8deb3082c
    expires: 2026-11-26
    reason: Conectează comparațiile JUnit evaluate din configurarea AutoEx și Shop la pasul tipizat; recorderul neutru comun este în main Test Kit, iar adaptoarele JUnit păstrează mapping-ul și raportul per metodă.
    proof: mvn -pl atf-test-kit,atf-test-auto-ex,atf-test-practice-shop,atf-tests -am "-Dtest=RecordedAssertionsTest,AutoExConfigurationLoaderComponentTest,ShopConfigurationTest,WholeAtfGovernanceSuiteTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/configuration/AutoExConfigurationLoaderComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/extension/AutoExExtentTestExtension.java
      - atf-test-kit/src/test/java/atf/testkit/assertions/RecordedAssertionsTest.java
  - id: AUTOEX-CONSUMER-COMPONENT-09
    status: ACTIVE
    base: fdbafdbaa4bdf617c9fd103ba0b917cc2efdc766
    expires: 2026-11-26
    reason: Păstrează probele protejate pentru lifecycle/configurația, porturile de pagină și portul UI al cart stories AutoEx; guard-urile verifică ownership-ul capabilităților și limita de opt clase. ShopComponentSuite rulează în aceeași verificare, iar politica de schimbări îl exclude explicit.
    proof: mvn -pl atf-test-auto-ex,atf-test-practice-shop,atf-tests -am "-Dtest=AutoExComponentSuite,ShopComponentSuite,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=fdbafdbaa4bdf617c9fd103ba0b917cc2efdc766" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/configuration/AutoExConfigurationLoaderComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExAccountLifecycleComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExCartFlowComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/pages/AutoExPagePortsComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/lifecycle/AutoExRuntimeBoundaryComponentTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerModuleArchitectureGovernanceTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/AtfPackageCohesionGovernanceTest.java
```

</details>
