# ATF Reporting Tests — oracole independente și acceptanță sursă/JAR

## Pe scurt

Acest feedback deține corectitudinea probelor din `atf-reporting-tests`:
oracol, fixture, classpath, navigare și acceptanța browserului.
[Implementarea bibliotecii](ATF_Reporting_Feedback.md) păstrează lifecycle-ul,
API-ul public, publicarea și release-ul; un test reparat nu închide implicit acel criteriu.

**Prioritate: 89/100** = impact 35 + lacună 27 + lucrabil 19 + documentare 8.
Un candidat de bibliotecă cere și probele canonice afectate, cu ieșirea integrală
păstrată în staging. O selecție focalizată verde nu certifică inventarul rezidual.

| Decizie obligatorie | Refuzul unei închideri premature |
|---|---|
| clasificați defectul înaintea modificării expected-ului | nu schimbați `component` în `execution` numai pentru verde; justificați ownerul cauzei și proba lui |
| oracolul citește dovada publicată a scenariului | textul găsit în script/CSS sau în alt scenariu nu dovedește verdict, captură ori comparație |
| verificați dovada exactă după navigare | tabul Tests vizibil și titlul comun `Failure evidence` nu confirmă faza și ordinalul |
| păstrați două limite de distribuție | runtime-ul JAR nu primește sursa producătorului pentru a satisface un fixture |
| controalele negative rămân executabile | eliminarea unei capturi, schimbarea unui verdict ori ascunderea dovezii originale trebuie detectate separat |

## REPORT-TEST-AUDIT-60 — o dovadă identificabilă pentru fiecare scenariu

**Prioritate: 80/100** = impact 31 + lacună 24 + lucrabil 18 + documentare 7.
**Owner:** Reporting Tests pentru fixture-ul neutru și oracol; consumerii
pentru asocierea fault-ului și expected-ul aplicației.

Căutarea unui marker în întregul HTML poate găsi template-ul JavaScript ori
dovada altui scenariu. Un raport comun cu mai multe negative cere identificarea
scenariului, fazei, ownerului, ordinalului și capturii originale.

- Auditul Shop confirmă și copii DOM ascunse după navigarea runtime: numărarea
  globală poate găsi 270 de rânduri, deși flow-ul activ afișează corect 135.
  Oracolul trebuie să identifice ownerul activ și comparația originală;
  vizibilitatea singură nu înlocuiește identitatea sau inventarul semantic.

Regresiile permanente acoperă ordinalele originale ACTION/OBSERVE/comparison,
etichetele de eșec repetate, navigarea inversă și reload. Păstrați [contractul
catalogului](../../atf-reporting-tests/src/test/java/atf/reporting/ui/contract/UiFailureCatalogueContractTest.java)
și [testele de navigare publicată](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/failurecontext/PublishedFailureNavigationEdgeComponentTest.java);
nu relistați acoperirea nominală ca lacună.

- [ ] Reconcile the remaining source/JAR mutation matrix per evidence kind and scenario; identify exactly which capture/owner/value degradation lacks an independent rejection before adding another fixture.
- [ ] Controlul negativ elimină numai captura unui caz, mută dovada la un
      sibling sau inversează numai verdictul lui; oracolul trebuie să refuze
      fiecare variantă fără să fie satisfăcut de script ori de alt scenariu.
- [ ] Pentru linkurile Fails native verificați activarea cu Enter și starea
      focusată; Space se cere disclosure-urilor, nu linkurilor. Enter/Space,
      grupul deschis și reload rămân verificate pentru disclosure-urile existente.
      Nu acceptați asocierea după titlu.
- [ ] Păstrați preview-ul implicit 240 și bugetele configurate; comparația
      integrală precede redacția. Un mismatch după limita preview-ului rămâne
      FAIL fără publicarea integrală a valorilor ori expunerea unui secret.

**Închidere:** fiecare degradare focalizată are refuz observabil și diagnostic
cu identitatea dovezii. [Proba business a consumerilor](../ATF_Testing_Guide.md#acceptanța-business-individual-și-prin-suită)
rămâne la aplicație; fixture-ul neutru nu aduce Shop/AutoEx în Reporting Tests.

## Dovada cerută pentru handoff

| Livrabil | Criteriu |
|---|---|
| clasificare | implementare, test/oracol, fixture sau document; cauza și ownerul sunt explicite |
| regresie | nominal și negativ independent, fără praguri relaxate ori teste ascunse |
| distribuție | sursa izolată și consumerul JAR au classpath verificat; assets din același candidat |
| browser | fază și comparație exacte, două diagnostice omonime, tastatură, reload și panou îngust |
| acceptare | gate-urile relevante trec, criteriul demonstrat se elimină, controlul Git verificat trece în baseline |

Contractele și selecțiile sunt la [README-ul modulului](../../atf-reporting-tests/README.md).
Build-ul și auditul respectă [procesul izolat](../ATF_Build_And_Execution_Safety.md);
documentele normative nu păstrează artifacte sau rezultate datate.

## REPORT-TEST-FALLBACK-RESULT-WIDTH-101 — geometria celulei RESULT

**Prioritate: 64/100.** [Criteriul bibliotecii](ATF_Reporting_Feedback.md#report-fallback-result-width-101--regresie-geometrică-pentru-badge)
păstrează martorul istoric și cere măsurarea geometrică a codului/JAR-ului actual.
Testele existente verifică alocarea dinamică și alte familii, dar nu publică
WARNING ca badge în celula RESULT.

- [ ] Extindeți fixture-ul geometric existent cu verdicturile PASS, FAIL și
      WARNING plus header lung; măsurați lățimea reală a badge-ului, celulei și
      tabelului la viewport normal și îngust în Extent/fallback.
- [ ] Refuzați mutantul care restrânge numai RESULT, apoi confirmați ultima
      coloană/rând, wrapping și preview 240. Reutilizați fixture-ul existent dacă
      permite un oracol independent; nu impuneți o clasă nouă fără motiv.

**Închidere:** regresia reproduce overflow-ul vechi și refuză mutantul, apoi
verifică sursa și JAR-ul real. Proba de valori din consumer rămâne acolo;
fixture-ul și oracolul generic de geometrie sunt deținute permanent în J0.

## REPORT-TEST-SQL-STEPS-116 — probe dedicate SQL

**Prioritate: 88/100** = impact 35 + lacună 26 + lucrabil 18 + documentare 9.
**Owner:** atf-reporting-tests pentru componentele neutre și oracolele de
randare; nativele modelelor/configurației aparțin atf-reporting. Aceste probe
sunt cerute pentru [contractul SQL nou](ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile), nu implementate în acest increment documentar.

Fixture-urile primesc fapte SQL capturate, fără Eventuate, H2 sau conexiune
reală în harness. Execuția JDBC independentă și selecțiile individual/suită
rămân acceptanța J2; un fixture neutru nu dovedește tranzacțiile aplicației.

| Familie componentă propusă | Oracol independent obligatoriu |
|---|---|
| disclosure și identitate | query plus bind-uri exacte în faza corectă, inclusiv două titluri identice, vecin PASS și SQL error controlat |
| bugete și redacție | query 4.096, bind-uri 2.048, rezultate 16.384 și payload SQL cumulat 1.048.576; fiecare configurabil separat, fără leak în canale ascunse |
| tabel rezultate | zero/unul/multe rânduri, duplicate de rând și de etichetă coloană, ordine fixă, NULL/gol, întregi mari și zecimale exacte |
| lifecycle și ownership | cauză inițială, cleanup secundar, cancel și captură parțială, publicare refuzată/repetată și niciun callback JDBC ori assertion reevaluat |

Separați familiile în pachete coezive în limita existentă de opt clase;
nu creați un modul nou. AtfComponentTest și selecțiile explicite rămân vizibile;
un test contractual închis nu dispare din suita de regresie.

<details>
<summary><strong>Matricea de regresie și probele negative</strong></summary>

- Pentru fiecare buget verificați limita minus unu, limita exactă și limita
  plus unu, valoare mică validă și configurații invalide/overflow. Schimbarea
  limitei rezultatelor nu trebuie să modifice limita query-ului sau preview-ul 240.
- Numărați separat code points vizibile și payload-ul efectiv serializat.
  Includeți diacritice, emoji compuse, markup care amplifică escaping-ul,
  parametri sensibili și multe execuții care epuizează bugetul cumulat.
- Oracolul scanează DOM, JSON, script, atribute și indexul de căutare pentru
  full payload/secret interzis; markerul de omisiune este inclus în buget și
  documentul final rămâne valid. Verificați și publicarea concurentă.
- Includeți eroare SQL așteptată într-un test PASS și eroare neinterceptată
  într-un FAIL, SQLState/vendor code și cauza originală; nu deduceți verdictul
  numai din existența unui error field sau din culoarea tabelului.
- Un mismatch după toate limitele de preview rămâne FAIL. SQL-only legitim,
  assertion-only și combinația UI/API/SQL se verifică fără request-uri fictive;
  includeți setup eșuat înaintea body-ului și cleanup eșuat după body.
- Mutați numai query-ul, bind-urile, rândul ori verdictul la un sibling omonim;
  eliminați o coloană duplicată, rotunjiți un număr, ascundeți full payload-ul
  peste plafon ori scurgeți un secret. Fiecare mutant trebuie refuzat independent.
- În Extent și fallback inspectați toate disclosure-urile, lățimea disponibilă,
  ultima coloană/rând, scroll interior, Enter/Space, panou îngust, reload și
  linkurile Tests/Fails/Overview către aceeași execuție și cauză.

</details>

**Închidere:** componente permanente și controale negative, matrice pe sursă
izolată și JAR fizic fără sursa producătorului, plus audit browser pentru
ambele renderere. Receipt-ul leagă scenariul, faza, ID-ul execuției și limitele
efective; un marker găsit ori un screenshot singur nu închide criteriul.

J2 adoptă apoi noul candidat și extinde matricea proprie de 18 rapoarte SQL cu
query/bind-uri/schema efectiv capturate. Acceptanța anterioară J9 rămâne istorică.

### Intrări concrete pentru componentele J0 — 2026-11-01

- [Matricea J2](../../../atf_j2/docs/ATF_SQL_Live_Verification.md) furnizează acum query/binds/schema/rows typed și erori controlate, în 12 Extent/fallback. Adăugați fixture-uri neutre permanente pentru fiecare familie de mai sus, fără copierea H2 sau a suitei JDBC în Reporting Tests.
- Păstrați negativele query absent, ID diferit de metadata, DECIMAL rotunjit și rând/coloană duplicată pierdută. Adăugați cross-owner cu titluri identice și SQL-only fără HTTP; refuzul legacy J10 este istoric; J12 oferă profilul explicit, iar acceptoarele J2 îl adoptă separat.
- Verificați separat captura parțială JDBC, tăierea HTML configurată și testul PASS care verifică o eroare SQL așteptată. Un error field nu impune FAIL asupra comparațiilor care au trecut; eroarea neconsumată păstrează FAIL și cauza originală.

## Current regression boundaries

- SQL-STEPS-116, exact-row/reload failure identity and RESULT geometry remain separate open criteria.
- [Unicode and Kit scope](../../../atf_j1/docs/feedback/ATF_Common_Test_Support_Feedback.md) belong to J1; Reporting keeps the neutral rendering boundary.
- [Long UI assertion capture](ATF_Android_UI_Reporting_Feedback.md#report-ui-assertion-full-evidence-133--valori-lungi-capturate-înainte-de-preview) remains separate from the delivered ACTION/OBSERVE and literal TEXT contracts.

<details>
<summary><strong>Delivered regression families and compatibility routes</strong></summary>

## REPORT-TEST-ARTIFACT-PROFILE-62 — matrice neutră fără HTTP fictiv

[Reporting owner contract](../../atf-reporting/README.md) and the [harness contract](../../atf-reporting-tests/README.md#probe-inspector-timing-și-indicator-assertions) retain explicit artifact profiles and degraded-inventory negatives.

## Timing capturat versus publicare — regresie de păstrat

[RecordedPublicationTimingComponentTest](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/timing/RecordedPublicationTimingComponentTest.java) and [Reporting owner contract](../../atf-reporting/README.md) own the permanent barrier-based timing regression.

## REPORT-TEST-SCREENSHOT-RETURN-137 — contextul viewer-ului pentru PNG reutilizat

[Screenshot reference contracts](../../atf-reporting-tests/src/test/java/atf/reporting/observation/ScreenshotReferenceComponentTest.java), the [browser family](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/screenshot/ScreenshotViewerBrowserComponentTest.java) and [inline audit contract](../ATF_Inline_Audit_Contract.md) retain owner, caption, focus and legal scroll restoration.

## REPORT-TEST-SCREENSHOT-VIEWPORT-138 — fit integral fără crop

The [neutral fixture](../../atf-reporting-tests/src/test/java/atf/reporting/support/screenshot/ScreenshotViewerFixture.java) and [inline audit contract](../ATF_Inline_Audit_Contract.md) own full-image geometry, input controls and immutable PNG acceptance on source/JAR.

## REPORT-LARGE-NAVIGATION-139 — permanent retained-evidence regression

The [navigation contract](../ATF_Large_Report_Navigation.md) defines the navigation, hidden-table, semantic-tree and canonical-PNG mutation families. Large external reports use targeted paths or at most two explicitly selected original owners.

## REPORT-COLD-READINESS-141 — permanent startup regression

[navigation contract](../ATF_Large_Report_Navigation.md) owns ColdStartReadinessComponentTest, unchanged-write measurement checks and the original cold-start deadlines. Control 141 remains verified in the [baseline](../ATF_Test_Changes_Baseline.md).

## REPORT-TIMELINE-TITLE-142 — permanent native-script contract

[native timeline contract](../ATF_Large_Report_Navigation.md#native-timeline-labels) retains three full-label script cases, the old-binary negative and unchanged timestamp coverage.

</details>

## Public adapter and API-client boundary

Ghidul din `lib` este deja compilat și executat pe JAR-ul fizic prin
`ReportingLibraryGuideIT`, fără sursa producerului, Core sau Kit în classpath.
Rămâne deschisă compatibilitatea API susținută și capabilitățile adaptoarelor,
nu portabilitatea ghidului livrat.

- [ ] Characterize lightweight legacy adapters: requested mandatory screenshots, actions, execution diagnostics and aggregate data either survive publication or receive an explicit capability refusal; a no-op call alone is not auditable evidence.
- [ ] Exercitați un client declarat susținut contra JAR-ului fizic, fără clase
      interne ale producerului; comparați separat semnăturile susținute și
      presupunerile runtime/vendor. Ghidul existent demonstrează numai exemplul
      documentat și publicarea de bază.

<a id="report-unit-baseline-149--restore-the-complete-existing-unit-gate"></a>

Contractul suitei canonice, al hostului Fails și al controalelor negative este
menținut în [Reporting Tests](../../atf-reporting-tests/README.md). Controalele
tehnice eligibile rămân în [baseline](../ATF_Test_Changes_Baseline.md).

<a id="report-test-controls-150--reconcile-existing-java-changes-at-the-real-review-base"></a>

Eligibilitatea controalelor Git este guvernată de [baseline-ul tehnic](../ATF_Test_Changes_Baseline.md), cu path-uri exacte și baza reală a revizuirii.

<a id="report-test-setup-153--inițializare-browser-cu-termen-propriu"></a>

Contractul de [setup și verdict terminal](../../atf-reporting-tests/README.md#setup-browser-și-verdict-terminal)
separă inspecția statică de proba browser. Controalele de factory necooperant,
cleanup, cauză inițială și ownership rămân permanente în J0; controlul tehnic
verificat este în [baseline](../ATF_Test_Changes_Baseline.md).

<details>
<summary><strong>Control tehnic — REPORT-BROWSER-LIFECYCLE-154-CONTROL</strong></summary>

```yaml
testChanges:
  - id: REPORT-BROWSER-LIFECYCLE-154-CONTROL
    status: ACTIVE
    base: 2f33813701b47e362b8091c6aca7c1c2b602727d
    expires: 2026-11-25
    reason: Izoleaza selectia originala si lifecycle-ul SDK. Executa un singur plan Jupiter si clasifica discovery in bugetul de verificare. Confirma iesirea arborelui Chromium detinut de worker pe Windows. Pastreaza asertiunile si pragurile originale, inclusiv cleanup-ul global legacy.
    proof: >-
      mvn -pl atf-reporting-tests -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=AtfReportingUnitSuite,AtfReportingBrowserSuite test;
      mvn -pl atf-reporting-tests -Preporting-release -Datf.reporting.test.version=1.0.0-j23 -Dtest=AtfReportingUnitSuite,AtfReportingBrowserSuite test
    paths:
      - atf-reporting-tests/src/test/java/atf/reporting/runtime/core/ExtentReportRuntimeScriptTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/locators/ReportEvidenceLocators.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/audit/api/ApiReportContractValidation.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/locators/table/PublishedEvidenceTableLocators.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/locators/ReportTreeLocators.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/api/ApiReportUiFlow.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/failure/UiFailureReportInspector.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/locators/failurecontext/ReportFailureContextLocators.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/locators/verdict/VerdictReportLocators.java
      - atf-reporting/src/test/java/atf/reporting/standalone/ReportingStandaloneVerdictComponentTest.java
      - atf-reporting-tests/pom.xml
      - atf-reporting-tests/src/test/java/atf/reporting/api/request/ObservedBodyAuditComponentTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/api/request/ObservedRequestEvidenceComponentTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/StaticHtmlScreenshotAdapter.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/StaticHtmlScreenshotAdapterTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/contract/BrowserBoundaryContractTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/contract/BrowserPhaseChannelContractTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/contract/BrowserPhaseDeadlineContractTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/contract/BrowserProcessInventoryContractTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/contract/BrowserResourcesContractTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/governance/BrowserLifecycleGovernanceTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/governance/BrowserReachabilityContractTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/governance/BrowserSourceInventory.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/junit/BrowserClassRun.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/junit/BrowserSelection.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/junit/BrowserTestBoundary.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/junit/JunitBrowserProbe.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/junit/ReportBrowserTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/lifecycle/BrowserLease.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/lifecycle/BrowserProbeBudget.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/lifecycle/BrowserProbeDiagnostics.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/lifecycle/BrowserProbeProcess.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/lifecycle/BrowserProbeScope.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/lifecycle/BrowserProbeWorker.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/process/BrowserPhaseChannel.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/process/BrowserPhaseDeadline.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/process/BrowserProcessInventory.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/process/OwnedChromiumProcess.java
      - atf-reporting-tests/src/test/java/atf/reporting/browser/process/OwnedChromiumProcessContractTest.java
      - atf-reporting-tests/src/test/java/atf/ui/playwright/ReportingBrowserSession.java
      - atf-reporting-tests/src/test/java/atf/reporting/governance/AtfReportingBrowserSuite.java
      - atf-reporting-tests/src/test/java/atf/reporting/observation/ReportObservationBrowserComponentTest.java
      - atf-reporting-tests/src/test/java/atf/reporting/support/observation/ObservationBrowserProbe.java
      - atf-reporting-tests/src/test/java/atf/reporting/support/request/ObservedRequestFixture.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/assertions/ComponentAssertionStepReportTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/audit/ApiReportRenderedEvidenceComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/audit/CategoryOutcomeFlowAuditorTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/audit/UiApiVerdictProjectionComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/audit/UiFailureCorrelationComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/composition/SelfContainedAuditBrowserComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/execution/TerminalApiExecutionReportTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/execution/UiTerminalExchangeReportComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/failurecontext/PublishedFailureNavigationComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/failurecontext/PublishedFailureNavigationEdgeComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/failurecontext/TerminalFailureNavigationComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/failurecontext/UiFailureContextComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/failurecontext/UiTimeoutReportComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/flow/BodyOnlyUnifiedFlowComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/flow/PhaseUnifiedFlowComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/ApiExchangeParityComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/EvidenceDisclosureAriaComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/ExtentReportDisclosureBrowserComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/ReportApiViewportParityComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/ReportBulkDisclosureComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/ReportPhaseAuditBrowserComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/disclosure/SiblingDisclosureStateComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/duration/OverviewDurationProjectionComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/json/JsonEvidenceFidelityComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/network/NetworkCaptureEvidenceTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/profile/ReportArtifactProfileComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/profile/SelfContainedArtifactOfflineComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/retry/ApiRetryProjectionContractTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/table/TableViewportBrowserComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/inspection/validation/ApiValidationDisclosureComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/layout/EvidenceColumnSizingComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/layout/EvidenceTableVisibilityComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/layout/FallbackLongTitleComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/layout/PublishedEvidenceTableComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/lifecycle/BrowserProbeProcessComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/lifecycle/UiSetupFallbackParityComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/navigation/ColdStartReadinessComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/navigation/LargeReportNavigationBrowserComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/navigation/TimelineTitleEncodingComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/presentation/AssertionIndicatorComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/presentation/FallbackIndexPresentationComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/presentation/ReportSystemMetadataComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/preview/LegacyPreviewBudgetComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/provenance/UiActionAttributionComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/readability/NarrowEvidenceReadabilityComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/recovery/BackendRecoveryObservationReportTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/regression/UnitBaselineBehaviorComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/screenshot/ScreenshotViewerBrowserComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/section/contract/SectionAuditRegistryTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/timing/ComponentExecutionDiagnosticReportTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/timing/RecordedPublicationTimingComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/timing/ReportPhaseTimingComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/values/UiFullValueBrowserComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/values/UiRowProjectionBrowserComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/verdict/QueryFailureReportComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/verdict/ReportingVerdictProjectionComponentTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/e2e/configurable/CategoryCardinalityBrowserFixtureTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/e2e/failure/FailureEvidenceFlowTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/e2e/failure/UiFailureReportContractTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/e2e/navigation/ReportNavigationAggregationFlowTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/e2e/sanity/UiThenDisclosureSanityTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/e2e/section/SelectedApiTestsReportUiTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/e2e/uireport/StaticUiExtentReportContractTest.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/audit/ReportAuditContext.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/audit/query/QueryFailureReportProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/diagnostic/ReportPhaseAuditProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/disclosure/EvidenceDisclosureAriaProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/disclosure/ExtentReportDisclosureProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/disclosure/ReportBulkDisclosureProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/duration/OverviewDurationProjectionProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/execution/UiTerminalExchangeProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/failurecontext/UiFailureContextProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/failurecontext/UiTimeoutReportProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/failurenavigation/FailureNavigationEdgeProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/failurenavigation/FailureNavigationProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/failurenavigation/TerminalFailureIndexProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/flow/BodyOnlyUnifiedFlowProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/flow/PhaseUnifiedFlowProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/layout/EvidenceColumnSizingProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/navigation/FallbackLongTitleProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/parity/ApiExchangeParityProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/preview/LegacyPreviewBudgetProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/readability/NarrowEvidenceReadabilityProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/reconciliation/ApiReportRenderedEvidenceProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/reconciliation/UiApiVerdictProjectionProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/reconciliation/UiFailureCorrelationProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/table/PublishedEvidenceTableMutation.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/table/PublishedEvidenceTableProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/table/TableViewportProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/viewport/ReportApiViewportProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/lifecycle/BrowserResources.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/lifecycle/SinglePageBrowserProbe.java
      - atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/locators/readability/ReportReadabilityLocators.java
      - atf-reporting-tests/src/test/resources/META-INF/services/org.junit.platform.launcher.LauncherDiscoveryListener
      - atf-reporting-tests/src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener
```

</details>
