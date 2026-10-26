# ATF — teste business simple și suport la owner

## Pe scurt

Ținta este un test scurt care exprimă intenția și un runtime cu responsabilități
clare. Aici rămân numai diferențele față de contractele livrate.

| Contract implementat | Documentație owner |
|---|---|
| Plan evidence configurat pe trei axe, resolver direct/compus și conflicte înaintea resurselor | [Kit](../../atf-test-kit/README.md), [adnotări](../ATF_Test_Infrastructure_Annotations.md) |
| Politică de billing independentă de evidence și metadata rezolvată explicit | [Shop](../../atf-test-practice-shop/README.md#raport-partajat--starea-consumerului) |
| Overview cu valori comune sau mixte, fără a păstra profilul primului frate; detalii de acțiuni per scenariu | [Reporting](../../atf-reporting/README.md) |
| Pilot de coș cu gate pur, port UI restrâns, lifecycle de cont per execuție și factură observată separat de validare | [AutoEx](../../atf-test-auto-ex/README.md) |

Probele componente acoperă cazuri pozitive și negative controlate. Ele nu
certifică disponibilitatea magazinelor publice sau migrarea tuturor flow-urilor.

## Ordinea refactorizării

Scorul este prioritate: impact /40 + lacună /30 + lucrabil /20 + datorie
documentară /10. Nu reprezintă o notă a codului sau un rezultat de test.

| ID | Scor /100 | Diferența rămasă |
|---|---:|---|
| `CONSUMER-FLOW-LAYERS-09` | 88 = 36 + 24 + 20 + 8 | extinderea separării business/UI/validare dincolo de pilotul AutoEx |
| `AUTOEX-LIVE-AVAILABILITY-07` | 78 = 30 + 20 + 20 + 8 | corelarea eșecului unei mutații cu postcondiția UI, fără replay |
| `CONSUMER-RUNTIME-BOUNDARY-03` | 77 = 31 + 18 + 20 + 8 | responsabilitățile de achiziție, publicare și lifecycle rămase în Shop |
| `CONSUMER-TEST-INTENT-05` | 67 = 27 + 18 + 16 + 6 | compoziția declarațiilor repetitive din clasele E2E, peste resolverul livrat |
| `CONSUMER-E2E-PROFILES-01` | 57 = 24 + 10 + 17 + 6 | reducerea repetării live numai după echivalență demonstrată |

## CONSUMER-FLOW-LAYERS-09 — business, traseu UI și validare separate

**Owner:** consumerii pentru business și adaptoare; UI, Reporting și Kit pentru
capabilitățile neutre existente. Pilotul nu reprezintă migrarea întregului journey.

Arhitectura țintă și categoriile de validare sunt în
[ghidul oficial](../ATF_Testing_Guide.md#separarea-flow-urilor-business-ui-și-a-validărilor).

| Zonă rămasă | Limita actuală | Criteriul următor |
|---|---|---|
| `ShopPurchaseFlow` / `ShopPurchaseJourneyContext` | Page, recorder, config completă, observer și API journal în același context | porturi locale pe capabilități, snapshot observat și gate separat |
| Account/Home/Checkout AutoEx | păstrează încă `AutoExPurchaseSession` larg | păstrarea numai a capabilităților necesare paginii, după modelul Cart/Product |
| Checkout Shop | observația, validarea și publicarea sunt încă amestecate în etape | extragere incrementală, cu Confirm și numărul comenzii păstrate |
| Evidence și cleanup la limita flow-urilor | există publisher și lifecycle comune, dar nu toate traseele au porturi restrânse | păstrarea identității checkpoint-ului, ordinii și cauzei primare |

- [ ] **09-A — baseline pentru următoarea migrare.** Inventariați operațiile,
      observațiile, gate-urile și mutațiile traseelor Shop guest/autentificat.
      Păstrați selecțiile, toate cazurile și aserțiunile existente; maparea
      namespace-urilor nu substituie proba comportamentului.
- [ ] **09-D — politica sursei contului.** Declarați separat sursa și ownership-ul
      contului la fiecare consumer. Billing-ul Shop este deja configurat separat;
      capturile, gruparea ori canalul de evidence nu aleg mutațiile business.
- [ ] **09-E — checkout Shop.** Separați observația și gate-urile de navigare și
      publicare. Plata verificată nu este comandă finală: păstrați Confirm,
      confirmarea și numărul comenzii; factura AutoEx are deja validator pur.
- [ ] **09-G — porturi pentru flow-urile rămase.** Restrângeți dependențele
      Account/Home/Checkout AutoEx și ale etapelor Shop, fără wrapper per click.
      Păstrați UI/API frați, fereastra cauzală, captura stării observate și cauza
      de infrastructură; un failure business anterior rămâne primar.
- [ ] **09-H — probe înaintea migrării generale.** Extindeți matricea pilotului
      la fiecare port nou: observație lipsă, timeout, gate FAIL, mutație incertă
      și eșec simultan business/publicare/cleanup. Guard-ul business respinge
      browser, UI, JUnit și Reporting fără excepții sau praguri mărite.

Business-ul și modelele aplicației rămân în main la consumer; fixture-urile,
suitele și adaptoarele JUnit rămân în test. Limita de opt clase per pachet
rămâne obligatorie; o extracție comună cere același contract demonstrat.

## AUTOEX-LIVE-AVAILABILITY-07 — indisponibilitatea nu este drift de selector

**Owner:** AutoEx pentru corelarea răspunsului observat cu starea UI.
Recuperarea GET document configurată și selecția exactă GET/XHR/FETCH pentru
readback-ul coșului sunt livrate la [owner](../../atf-test-auto-ex/README.md#limita-disponibilității-serviciului-public).

- [ ] Corelați răspunsul mutației cu postcondiția UI, inclusiv add-to-cart
      refuzat și modal absent. UI-only păstrează diagnosticul fără API evidence;
      UI+API păstrează exchange-ul sub acțiunea cauzală.
- [ ] Probați succes, 5xx observat, request eșuat și lipsă de răspuns, cu vecini
      controlați. Selecția exactă a unui readback nu închide această matrice;
      timeout-ul singur nu autorizează inferarea unui 503.
- [ ] Păstrați FAIL și cauza observată, fără retry de click, creare/ștergere cont
      ori plată. O execuție nouă nu reia mutația incertă a celei anterioare.

## CONSUMER-RUNTIME-BOUNDARY-03 — mai puțin suport local, nu un harness ascuns

**Owner:** Shop pentru runtime-ul aplicației; Kit/UI/Reporting pentru
capabilitățile neutre. Metadata Shop primește acum inputuri explicite, iar
diferențele de profil sunt agregate corect în raportul partajat.

- [ ] Extrageți din `ShopE2eContext` câte o responsabilitate de achiziție,
      publicare sau lifecycle, păstrând fațada business. Store-ul și
      ParameterResolver-ul rămân la granița JUnit.
- [ ] Reutilizați primitivele Kit existente cu callbacks tipizate, fără
      dependențe Kit către browser, Reporting sau aplicații.
- [ ] Verificați achiziția parțială și eșecul la captură, summary, publish și
      close; cauza inițială și erorile secundare rămân disponibile.
- [ ] Păstrați probele adaptorului și boundary-ul guvernat de
      [ConsumerModuleArchitectureGovernanceTest](../../atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerModuleArchitectureGovernanceTest.java),
      fără allowlist sau relaxarea scope-ului Maven.

## CONSUMER-TEST-INTENT-05 — declarații compuse în consumeri

Resolverul neutru și wiring-ul ambilor consumeri sunt livrate și probate pentru
declarație absentă/directă/compusă, moștenire, axe configurate și conflicte.
Clasele E2E păstrează încă declarații repetitive care pot fi compuse incremental.

- [ ] Compuneți numai combinațiile efectiv repetitive din consumeri; păstrați
      selecțiile, canalul și activarea externe explicite, fără achiziții ascunse.
- [ ] Probați mapping-ul declarației reale a consumerului, override-ul local și
      conflictul înaintea resurselor; păstrați lansarea manuală din IntelliJ.

## CONSUMER-E2E-PROFILES-01 — variantele de evidence și acoperirea business

**Stare: parțial implementat.** Suitele și poveștile E2E sunt păstrate.
Resolverul și probele Extent/fallback permit izolarea politicilor per scenariu;
ele nu demonstrează singure echivalența tuturor adaptoarelor consumerilor.

- [ ] Inventariați separat poveștile, canalele și prezentările; UI+API poate
      avea precondiții/corelări absente în UI-only.
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
  - id: AUTOEX-CONSUMER-COMPONENT-09
    status: ACTIVE
    base: 490a0f274d71ec4bb288b01655e60f768daa74ba
    expires: 2026-11-26
    reason: Păstrează probele noi pentru flow-ul coșului și lifecycle-ul contului din snapshotul live; inventarul guvernat include explicit clasa nouă fără a mări limita de opt; repară substituția YAML dependentă de CRLF fără a slăbi aserțiunea pentru recuperarea implicit dezactivată.
    proof: mvn -pl atf-test-auto-ex,atf-tests -am "-Dtest=AutoExComponentSuite,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=490a0f274d71ec4bb288b01655e60f768daa74ba" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/configuration/AutoExConfigurationLoaderComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExAccountLifecycleComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExCartFlowComponentTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/AtfPackageCohesionGovernanceTest.java
```

</details>
