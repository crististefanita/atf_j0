# ATF — controlul raportului comun al componentelor

## Pe scurt

Contractul implementat este documentat la [Kit](../../atf-test-kit/README.md#valori-cu-ownership-per-clasă-sau-execuție),
[Reporting](../../atf-reporting/README.md#sesiunea-comună-a-componentelor),
[Shop](../../atf-test-practice-shop/README.md#de-ce-raportul-api-nu-arată-ca-raportul-ui)
și [AutoEx](../../atf-test-auto-ex/README.md#de-ce-raportul-api-nu-arată-ca-raportul-ui).

- Sesiunea aparține clasei sau suitei; scenariul și auditul rămân per invocare.
- Selecțiile canonice sunt complete, iar comentariile nu satisfac inventarul.
- Nu rămân ToDo-uri funcționale în acest increment; controlul tehnic rămâne
  activ pentru verificarea diferențelor față de baza Git.

## Regresii și responsabilități

| Owner | Contract protejat |
|---|---|
| Kit | achiziție/close unice, audit izolat, lifecycle și cauze păstrate |
| Reporting Tests | scenarii împrumutate, constructor standalone, concurență și randare |
| Cele două module consumer | metodă/clasă/suită, parametrizări, lansări repetate și publicare finală |
| ATF Tests + Core | selectori executabili, ignorarea comentariilor și omisiuni respinse |

- Probele notificării păstrează rezultatele și existența artifactului înaintea
  mesajului; cardinalitatea urmează ownerul class/suite.
- Căile de la aceeași bază ale `REPORT-PATH-WIRING-05` rămân în acel control
  verificat, fără dublarea lor în acest increment.
- Proba de lizibilitate păstrează ownerul `REPORT-NARROW-READABILITY-02`;
  selectorii ei sunt mutați în fațada dedicată.
- Shop folosește source-set-ul exceptat explicit de politica `testChanges`;
  probele sale rămân obligatorii la owner, fără extinderea listei protejate.

## Controlul incrementului

<details>
<summary><strong>Control tehnic — scope-ul componentelor și selecțiile executabile</strong></summary>

```yaml
testChanges:
  - id: COMPONENT-SCOPE-AND-SELECTORS-01
    status: ACTIVE
    base: 3219ef1ff546369d00a006cfc7434644251c6f9a
    expires: 2026-11-26
    reason: Agregă numai sesiunea componentelor la ownerul class/suite, păstrează auditul per invocare și constructorul Reporting owned, restaurează inventarul complet al suitei Shop și întărește parserul împotriva comentariilor fără relaxarea acoperirii; adaugă probe de lifecycle, parametrizare, paralelism, notificare și regresii adiacente; aliniază proba scriptului la ownerul component și centralizează selectorii probelor de randare, fără relaxarea guard-urilor.
    proof: mvn -pl atf-test-auto-ex,atf-test-practice-shop,atf-reporting-tests,atf-tests -am "-Dtest=AutoExComponentSuite,ShopComponentSuite,ScopedTestResourceTest,ScopedComponentAuditExtensionComponentTest,AtfJavaSourceReferencesTest,AssertionAuditRunComponentTest,ExtentReportSessionContractTest,ReportUiLocatorsTest,UiSelectorBoundaryArchitectureTest,ComponentAssertionStepReportTest,NarrowEvidenceReadabilityComponentTest,ReportTableViewportComponentTest,ReportApiViewportParityComponentTest,TableViewportBrowserComponentTest,ReportAssetSizeInventoryTest,WholeAtfGovernanceSuiteTest" "-Datf.governance.testChanges.baseRef=3219ef1ff546369d00a006cfc7434644251c6f9a" "-Dsurefire.failIfNoSpecifiedTests=false" test
    paths:
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/locators/readability/ReportReadabilityLocators.java
      - atf-reporting-tests/src/test/java/atf/reporting/extent/session/ExtentReportSessionContractTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/locators/diagnostic/ReportPhaseAuditLocators.java
      - atf-reporting-tests/src/test/java/atf/reporting/audit/AssertionAuditRunComponentTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/component/reporting/AutoExComponentReportScopeComponentTest.java
      - atf-test-kit/src/test/java/atf/testkit/assertions/ScopedComponentAuditExtensionComponentTest.java
      - atf-test-kit/src/test/java/atf/testkit/scope/ScopedTestResourceTest.java
      - atf-core/src/test/java/atf/core/files/AtfJavaSourceReferencesTest.java
      - atf-test-auto-ex/src/test/java/atf/autoex/tests/extension/AutoExComponentTest.java

      - atf-test-auto-ex/src/test/java/atf/autoex/tests/suites/AutoExComponentSuite.java
      - atf-tests/src/test/java/atf/api/tests/governance/consumers/ConsumerModuleArchitectureGovernanceTest.java
      - atf-tests/src/test/java/atf/api/tests/governance/layers/ConsumerSuiteCoverageGovernanceTest.java
```

</details>

