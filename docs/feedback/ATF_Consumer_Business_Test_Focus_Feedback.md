# ATF — responsabilitățile rămase ale consumerilor

## Pe scurt

Delegarea JUnit unică, redactarea înaintea plafonului, așteptările negative,
fazele lifecycle și rezultatul PASS/FAIL/SKIP sunt contracte ale
[Test Kit](../../atf-test-kit/README.md#comparații-junit-cu-dovadă-neutră) și
[Reporting](../../atf-reporting/README.md#proiecția-sigură-a-aserțiunilor).

- Adaptoarele AutoEx/Shop compun protocolul comun, fără dependențe Kit către renderer.
- Runtime-ul API Shop, auditul deschis înaintea execuției și scope-ul coșului sunt în
  [main la Shop](../../atf-test-practice-shop/README.md#de-ce-raportul-api-nu-arată-ca-raportul-ui).
- Scope-ul raportului API AutoEx și recuperarea cleanup-ului sunt la
  [ownerul AutoEx](../../atf-test-auto-ex/README.md#de-ce-raportul-api-nu-arată-ca-raportul-ui).
- Aici rămân extinderile arhitecturale și cazurile încă nedemonstrate; controalele
  Git se păstrează independent de închiderea unui gap funcțional.

## Ordinea refactorizării

Scorul este prioritate: impact /40 + lacună /30 + lucrabil /20 + datorie
documentară /10. Nu reprezintă o notă a codului sau un rezultat de test.

| ID | Scor /100 | Diferența rămasă |
|---|---:|---|
| `CONSUMER-FLOW-LAYERS-09` | 88 = 36 + 24 + 20 + 8 | separarea business/UI/validare în journey-urile și etapele rămase |
| `CONSUMER-RUNTIME-BOUNDARY-03` | 77 = 31 + 18 + 20 + 8 | achiziție, publicare și lifecycle E2E rămase în contextul Shop |
| `CONSUMER-SHOP-BILLING-STATE-01` | 71 = 29 + 22 + 14 + 6 | diferență de valoare billing între suită și lansare individuală |
| `CONSUMER-COMPONENT-ASSERTIONS-01` | 63 = 25 + 18 + 16 + 4 | evaluator neutru reutilizabil în Validation, după corectarea boundary-urilor existente |
| `CONSUMER-E2E-PROFILES-01` | 57 = 24 + 10 + 17 + 6 | reducerea repetării live numai după echivalență demonstrată |

## CONSUMER-FLOW-LAYERS-09 — business, traseu UI și validare separate

**Owner:** main la consumer pentru business și modele; UI, Validation și
Reporting pentru capabilitățile lor. Testele păstrează oracolul, fixture-ul și adaptorul JUnit.

**Dovadă statică:** [`AutoExPurchaseJourney`](../../atf-test-auto-ex/src/main/java/atf/autoex/support/journey/AutoExPurchaseJourney.java)
primește încă `AutoExPurchaseSession`, citește configurația globală, creează page objects
și construiește unele aserțiuni inline. Extracția cart flow nu închide restul journey-ului.

| Zonă rămasă | Increment urmărit | Dovada necesară |
|---|---|---|
| Purchase AutoEx | intrări tipizate pe etapă și porturi restrânse pentru operațiile necesare | aceeași ordine business și aceleași valori independente |
| Etape Shop din afara checkout-ului extras | separați observația UI de evaluare și publicare | FAIL publicat înaintea opririi tranziției |
| Evidence și cleanup la granița flow-ului | păstrați identitatea checkpoint-ului și cauza primară | failure-path fără acțiunea următoare sau retry ascuns |

Contractele deja livrate și probele lor aparțin
[AutoEx](../../atf-test-auto-ex/README.md#boundary-ul-cauzal-pentru-ui_api) și
[Shop](../../atf-test-practice-shop/README.md#gate-uri-de-checkout), nu unui istoric de increment în feedback.

- [ ] **09-G — porturi pentru flow-urile rămase.** Inventariați operațiile fiecărei
      etape înaintea extracției; folosiți compoziție și snapshot-uri imutabile,
      fără o interfață per click sau un `execute()` care ascunde întreaga poveste.
- [ ] **09-H — probe înaintea migrării generale.** Extindeți cazurile cu heading/valoare
      absentă, timeout și eșec de publicare; păstrați UI/API frați, fereastra cauzală,
      verdictul business anterior și ordinea erorilor secundare.
- [ ] Păstrați așteptările independente de observații și suitele existente;
      guard-ul business nu primește excepții pentru browser, JUnit ori Reporting.

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

## CONSUMER-COMPONENT-ASSERTIONS-01 — evaluatorul neutru al componentelor

**Owner al restanței:** Validation pentru evaluarea pură reutilizabilă.
Fațada JUnit, politica rezultatului și publicarea componentelor sunt implementate;
acest item privește un evaluator business independent de JUnit.

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
Controlul Git de mai jos protejează testele modificate; nu certifică migrarea
flow-urilor rămase.

<details>
<summary><strong>Controale tehnice ale incrementelor</strong></summary>

```yaml
testChanges:
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
