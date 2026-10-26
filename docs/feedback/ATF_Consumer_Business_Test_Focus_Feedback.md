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
| Harness E2E compus fără a ascunde canalul, selecția ori evidence-ul specific | [AutoEx](../../atf-test-auto-ex/README.md#suite-și-variante-de-evidence) |

Probele componente acoperă cazuri pozitive și negative controlate. Ele nu
certifică disponibilitatea magazinelor publice sau migrarea tuturor flow-urilor.

## Ordinea refactorizării

Scorul este prioritate: impact /40 + lacună /30 + lucrabil /20 + datorie
documentară /10. Nu reprezintă o notă a codului sau un rezultat de test.

| ID | Scor /100 | Diferența rămasă |
|---|---:|---|
| `CONSUMER-FLOW-LAYERS-09` | 88 = 36 + 24 + 20 + 8 | extinderea separării business/UI/validare în etapele AutoEx și Shop rămase |
| `AUTOEX-LIVE-AVAILABILITY-07` | 78 = 30 + 20 + 20 + 8 | corelarea eșecului unei mutații cu postcondiția UI, fără replay |
| `CONSUMER-RUNTIME-BOUNDARY-03` | 77 = 31 + 18 + 20 + 8 | responsabilitățile de achiziție, publicare și lifecycle rămase în Shop |
| `CONSUMER-E2E-PROFILES-01` | 57 = 24 + 10 + 17 + 6 | reducerea repetării live numai după echivalență demonstrată |

## CONSUMER-FLOW-LAYERS-09 — business, traseu UI și validare separate

**Owner:** consumerii pentru business și adaptoare; UI, Reporting și Kit pentru
capabilitățile neutre existente. Pilotul nu reprezintă migrarea întregului journey.

Arhitectura țintă și categoriile de validare sunt în
[ghidul oficial](../ATF_Testing_Guide.md#separarea-flow-urilor-business-ui-și-a-validărilor).

| Zonă rămasă | Limita actuală | Criteriul următor |
|---|---|---|
| `ShopPurchaseFlow` / `ShopPurchaseJourneyContext` | selecția Catalog și checkout-ul primesc proiecții de config; checkpoint-urile sunt snapshot-uri imutabile, publicate înaintea gate-ului | păstrați aceste contracte; evaluați separat celelalte flow-uri Shop/AutoEx, fără wrapper per acțiune |
| Etapele AutoEx rămase și etapele Shop | paginile Login/Registration primesc acum URI rezolvat; rămân flow-uri care primesc fațade complete | restrângerea următoarelor dependențe numai după inventarierea operațiilor, păstrând raportarea și ordinea |
| Evidence și cleanup la limita flow-urilor | există publisher și lifecycle comune, dar nu toate traseele au porturi restrânse | păstrarea identității checkpoint-ului, ordinii și cauzei primare |

**Livrat — baseline de migrare (09-A).** Inventarul celor cinci selecții Shop,
cu faze, gate-uri, mutații și ownership, este păstrat în
[README-ul Practice Shop](../../atf-test-practice-shop/README.md#baseline-pentru-separarea-flow-ului).
Selecțiile și aserțiunile rămân neschimbate.

**Livrat — ownership-ul conturilor (09-D).** Shop distinge fixture împrumutat,
cont sintetic nou și scope de clasă/suită; AutoEx păstrează identitatea creată
și lifecycle-ul delete per execuție. Politicile sunt în
[Shop](../../atf-test-practice-shop/README.md#conturile-autentificate) și
[AutoEx](../../atf-test-auto-ex/README.md#coș-și-continuitatea-sesiunii).
- **Livrat — checkout Shop (09-E).** [Coordinatorul pur](../../atf-test-practice-shop/README.md#gate-uri-de-checkout)
  publică și verifică fiecare checkpoint înaintea tranziției următoare; adaptorul
  Playwright deține observațiile UI și API evidence. `ShopCheckoutFlowComponentTest`
  probează ordinea, ambele ramuri de identitate, fail-fast și eșecul publisherului.
  Payment check rămâne distinct de Confirm și de confirmarea finală cu număr `INV-`.
- **Livrat — porturi de pagină AutoEx (09-G, tranșă).** Account, Home și Checkout
  nu mai depind de `AutoExPurchaseSession`; fiecare primește numai portul de care
  are nevoie. `ProductsPage` primește acțiunile UI și produsul așteptat explicit.
  `AutoExPagePortsComponentTest` probează ordinea navigare/prompt/căutare și
  checkout/plată, produsul așteptat și fail-fast offline; testul lifecycle
  folosește portul de cont direct.
- **Livrat — publisher Shop (09-G, tranșă).** `ShopPurchaseCheckpointPublisher`
  izolează formarea și publicarea checkpoint-urilor de contextul UI/config/API;
  componenta verifică FAIL publicat înaintea gate-ului și propagarea intactă a
  erorii sink-ului. Contextul încă agregă dependențe de browser și configurare.
- **Livrat — intrări tipizate pe stage Shop (09-G, tranșă).** Catalogul și
  checkout-ul primesc proiecții imutabile distincte; niciunul nu mai primește
  `ShopConfiguration` globală, iar checkout-ul nu expune validatorul Home.
  `ShopPurchaseStageInputsComponentTest` fixează shape-ul; `ShopCheckoutFlowComponentTest`
  și checkpoint-urile imutabile probează ordinea publish/gate. Resursele UI/API
  comune rămân la composition root.
- **Livrat — navigare îngustă pentru contul Shop (09-G, tranșă).** Login și
  Registration primesc URI rezolvat la composition root; testul de componentă
  fixează absența `ShopConfiguration` din API-ul page objects.
- [ ] **09-G — porturi pentru flow-urile rămase.** Restrângeți dependențele
      din journey-urile AutoEx și etapele Shop rămase, fără wrapper per click.
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
    base: fdbafdbaa4bdf617c9fd103ba0b917cc2efdc766
    expires: 2026-11-26
    reason: Păstrează probele protejate pentru lifecycle/configurația și porturile Home/Account/Checkout AutoEx; guard-urile verifică ownership-ul capabilităților și limita de opt clase. ShopComponentSuite rulează în aceeași verificare, iar politica de schimbări îl exclude explicit.
    proof: mvn -pl atf-test-auto-ex,atf-test-practice-shop,atf-tests -am "-Dtest=AutoExComponentSuite,ShopComponentSuite,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=fdbafdbaa4bdf617c9fd103ba0b917cc2efdc766" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/configuration/AutoExConfigurationLoaderComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExAccountLifecycleComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/journey/AutoExCartFlowComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/pages/AutoExPagePortsComponentTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerModuleArchitectureGovernanceTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/AtfPackageCohesionGovernanceTest.java
```

</details>
