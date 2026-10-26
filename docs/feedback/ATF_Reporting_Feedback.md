# ATF Reporting — lacune active

## Pe scurt

Aici rămân lacunele deschise și controalele tehnice încă necesare.
Contractele stabile aparțin [Reporting](../../atf-reporting/README.md),
[Reporting Tests](../../atf-reporting-tests/README.md),
[contractului semantic](../ATF_Reporting_Contract.md) și
[contractului de display](../ATF_Reporting_Display_Contract.md).

Fidelitatea request-ului observat, disponibilitatea body-ului și ownerul UI/API
sunt contracte livrate, documentate la
[ownerul Reporting](../../atf-reporting/README.md#fidelitatea-request-ului-și-ownerul-aserțiunilor).
Matricea Extent/fallback/UI_API și probele Fails/deep-link aparțin
[testelor Reporting](../../atf-reporting-tests/README.md), nu checklist-ului de restanțe.

- Lizibilitatea la lățime mică: **77/100**.
- Launcher browser canonic: **58/100**.
- Scorurile folosesc [formula comună de prioritate](README.md#top-feedback-uri-active).

## REPORT-NARROW-READABILITY-01 — tabele imbricate la lățime mică

**Prioritate:** 77/100 = impact 33 + lacună 24 + lucrabil 16 + documentare 4.
Problema afectează și un caz API nominal: după expandarea
`Request → Response → Execution`, tabelul `Technical trace` poate comprima
celulele până la câte o literă pe rând.

În panouri înguste, inclusiv la aproximativ 392 px, același efect apare în
`Failure evidence` cu observații și aserțiuni deschise. Scroll-ul vertical și
absența overflow-ului orizontal nu dovedesc lizibilitatea.

**Owner:** layout-ul comun `atf-reporting`, cu probe neutre în Reporting Tests;
wiring-ul consumerului nu justifică un renderer sau un backlog paralel.

- [ ] Adăugați fixture neutră nominală și negativă cu nesting complet,
      texte scurte/lungi, request/response/execution și failure evidence.
      Verificați lățime standard, 591 px și un panou îngust comparabil.
- [ ] Verificați lățimea utilă a coloanelor, textul vizibil și accesul la ultimul
      rând prin constante explicite, nu numai `scrollWidth <= clientWidth`;
      eșuați și pentru heading-uri ori valori rupte câte o literă pe rând.
- [ ] Reduceți spațiul consumat de nesting sau aplicați o prezentare mobilă;
      păstrați ierarhia, fontul lizibil și expand/collapse, fără a relaxa plafoanele.
      Tabelele scurte nu primesc înălțime goală, iar plafonul de scroll rămâne comun.
- [ ] Verificați redeschiderea părinților și navigarea Fails → Tests cu detalii
      expandate; comparați fixture-ul neutru, standalone API și checkpoint-ul UI+API.

## REPORTING-LAUNCHER-06 — selecție egală, acoperire numărată o singură dată

**Owner:** Reporting Tests. **Prioritate:** 58/100 = 23 + 14 + 17 + 4.
Cele două launchere selectează același pachet `e2e`, același tag și aceeași
excludere de suite.

- [AtfReportingUiAuditSuite](../../atf-reporting-tests/src/test/java/atf/reporting/governance/AtfReportingUiAuditSuite.java)
  și [ExtentReportingE2eSuite](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/e2e/ExtentReportingE2eSuite.java)
  nu sunt gate-uri de acoperire disjuncte.
- Al doilea declară și `@AtfTestChannel(UI)`; diferența de metadata trebuie
  păstrată, nu confundată cu alt inventar de teste.

- [ ] Alegeți un launcher canonic; celălalt devine alias documentat de
      compatibilitate ori primește o selecție distinctă, justificată.
- [ ] Comparați identitățile JUnit, inclusiv invocările parametrizate; nu
      lansați ambele selecții ca probe disjuncte și nu eliminați cazuri.
- [ ] Probați selecția și metadata fără browser; păstrați separat verificarea
      browser a arborelui, capturilor și disclosure-urilor.

## Controale tehnice încă necesare

Aceste aprobări protejează diferențe Git, nu sunt ToDo-uri funcționale.
Transferul și eliminarea lor urmează
[baseline-ul tehnic](../ATF_Test_Changes_Baseline.md#reguli-de-folosire),
fără rebazare, extindere de scope sau relaxarea guard-urilor.

<details>
<summary><strong>Controale tehnice active</strong></summary>

```yaml
testChanges:
  - id: REPORT-UNIFIED-TABLES-01
    status: ACTIVE
    base: 9d444354620b904f736302a2217f35e38ee5a875
    expires: 2026-11-26
    reason: Adaugă numai probe componente noi pentru agregarea UI-only cross-phase, paritatea Extent/fallback, captură terminală, policy fără captură și regresiile failure/UI+API; testele existente rămân intacte.
    proof: mvn -pl atf-reporting-tests,atf-tests -am "-Dtest=UiCrossPhaseCompactFlowComponentTest,ReportEvidenceProfileIntegrationTest,ExtentUnifiedFlowMultipleScreenshotTest,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=9d444354620b904f736302a2217f35e38ee5a875" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-reporting-tests/src/test/java/atf/reporting/ui/profile/UiCrossPhaseCompactFlowComponentTest.java
  - id: REPORT-UI-FAILURE-CORRELATION-10
    status: ACTIVE
    base: 9b0860bfc734df570c7512b76aa5dbb559527b7d
    expires: 2026-11-25
    reason: Corelare explicită prin identitatea aserțiunii UI publicate în scenariul curent; probe neutre pentru propagare directă și wrapped, excepții independente, identități egale ca text, canale distincte și cleanup; păstrează toate cazurile anterioare. Corelarea AutoEx este păstrată de runtime-ul main; mutarea și probele consumerului sunt urmărite separat de CONSUMER-AUTOEX-RUNTIME-10.
    proof: mvn -pl atf-reporting-tests,atf-test-auto-ex,atf-tests -am "-Dtest=AtfReportingUnitSuite,AtfReportingComponentSuite,AtfReportingIntegrationSuite,AutoExComponentSuite,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=9b0860bfc734df570c7512b76aa5dbb559527b7d" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-reporting-tests/src/test/java/atf/reporting/ui/failure/UiCheckpointFailureTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/support/failurecorrelation/UiFailureCorrelationFixture.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/reconciliation/UiFailureCorrelationProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/api/ApiReportUiFlow.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/audit/UiFailureCorrelationComponentTest.java
  - id: REPORT-ASSET-CLEANUP-COMPONENT-06
    status: ACTIVE
    base: 5eb3fd3cb099fc67b49be3c17de564db63755db7
    expires: 2026-11-24
    reason: Adaugă numai fixture-ul Spark offline pentru două cazuri componente ale pipeline-ului de publicare; probele existente ale cleanup-ului și sesiunii păstrează controalele lor exacte, fără dublare de owner sau rebazare automată.
    proof: mvn -pl atf-reporting-tests -am "-Dtest=ExtentReportAssetCleanupTest,ExtentReportSessionContractTest,AtfReportingComponentSuite,UiFailureReportContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-reporting-tests/src/test/java/atf/reporting/support/publication/CleanupPublicationFixture.java
  - id: REPORT-LIVE-CONTROL-04
    status: ACTIVE
    base: b9b1325c28d665be6289bb2cf9286666513e7a05
    expires: 2026-11-24
    allowDelete: true
    reason: Revizuiește explicit diff-ul curent al relocării Network cu toate cele cinci cazuri și fațada de locatori, al probei Client și al inventarului finit standalone. Nu modifică guard-urile; reexecută probele pentru baza curentă, fără rebazarea automată a controalelor anterioare.
    proof: mvn -pl atf-reporting-tests,atf-test-kit -am "-Dtest=AtfReportingComponentSuite,AtfTestLayerArchitectureTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-reporting-tests/src/test/java/atf/reporting/extent/session/ExtentReportSessionContractTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/ui/network/NetworkCaptureEvidenceTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/network/NetworkCaptureEvidenceTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/locators/network/ReportNetworkCaptureLocators.java
      - atf-test-kit/src/test/java/atf/testkit/AtfTestLayerArchitectureTest.java
  - id: SHOP-CAPTURE-01
    status: ACTIVE
    base: 0f6a95265024a78c20754d5c18937d24434a2d2d
    expires: 2026-10-30
    reason: Probează agregarea fazelor Shop, ownership-ul API și cazul setup eșuat fără body ori screenshot terminal.
    proof: mvn -pl atf-reporting-tests,atf-test-practice-shop -am "-Dtest=ShopComponentSuite,ReportEvidenceProfileIntegrationTest,ExtentUnifiedFlowMultipleScreenshotTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-reporting-tests/src/test/java/atf/reporting/ui/profile/ReportEvidenceProfileIntegrationTest.java
  - id: REPORT-UI-ACTION-REDACTION-01
    status: ACTIVE
    base: a23c76e662516504f55a4369c58ca98bbf2f26ec
    expires: 2026-10-30
    reason: Prove card/CVC redaction before UI evidence recording, including exception and diagnostic trace paths, plus the rendered HTML artifact.
    proof: mvn -pl atf-ui -am "-Dtest=PlaywrightActionSequenceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test; mvn -pl atf-reporting-tests -am "-Dtest=ExtentReportUiEvidenceRenderingTest,ReportEnvironmentSnapshotTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-ui/src/test/java/atf/ui/playwright/PlaywrightActionSequenceTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/ui/ExtentReportUiEvidenceRenderingTest.java
```

</details>
