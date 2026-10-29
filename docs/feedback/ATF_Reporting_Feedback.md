# ATF Reporting — prezentarea și publicarea dovezilor

## Pe scurt

Criteriile lifecycle și timing au contracte și regresii la owner. Rămâne
revenirea directă la dovadă; nu se extind funcționalitățile ori numărul modulelor.

## REPORT-FAILURE-DEEP-LINK-29 — revenire la dovada eșecului

**Prioritate: 75/100** = impact 30 + lacună 22 + lucrabil 17 + documentare 6.
**Owner:** navigarea runtime în Reporting; probe neutre în Reporting Tests.

În cele două HTML-uri Shop controlate prin clasa E2E existentă, 503 repetat și
200 fără produse rămân FAIL, cu acțiunea originală și observațiile catalogului.
`Fails → Open in Tests` deschide scenariul corect, dar faza `TEST BODY`, pasul
și validarea rămân colapsate. Comparația nu devine vizibilă fără alte clickuri.

- **Rutare:** [proiecția Fails](../../atf-reporting/src/main/resources/atf/reporting/javascript/extent-failure-projection.js)
  transmite numai clasa și indexul scenariului către `atfOpenTest`.
  `data-atf-failure-ordinal` există în markup, însă callback-ul nu îl folosește
  pentru deschiderea dovezii exacte.
- **Limita helperului:** expansiunea `ui-validation` existentă alege prima
  validare din `TEST BODY`; nu rezolvă general SETUP, TEARDOWN, un al doilea
  checkpoint sau ownerul API/component.
- **Limita probelor:** [UiTimeoutReportProbe](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/failurecontext/UiTimeoutReportProbe.java)
  folosește `Expand all` după backlink, iar
  [UiFailureContextProbe](../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/support/inspection/failurecontext/UiFailureContextProbe.java)
  redeschide explicit conținutul. Ele confirmă păstrarea dovezilor și pot trece
  când deschiderea automată a căii este defectă.

**Criterii rămase:**

- [ ] Leagați backlink-ul de ownerul și calea semantică a eșecului: scenariu,
      fază, request/pas, validare și comparația originală. Nu alegeți primul
      nod din body și nu reconstituiți identitatea prin textul trunchiat.
- [ ] Verificați vizibilitatea întregii căi imediat după click, înainte de
      `Expand all` sau de un helper care deschide ancestorii.
- [ ] Probați separat SETUP/TEST BODY/TEARDOWN, owner UI/API/component,
      checkpoint ulterior și scenariu vecin PASS; includeți 392/1280 px,
      navigare repetată, ordinalul exact și toate coloanele accesibile.
- [ ] Adăugați control negativ care păstrează scenariul vizibil, dar închide
      faza ori validarea. Oracolul trebuie să îl refuze fără a elimina
      verificările de conținut, plafon 240, redactare și verdict existente.

**Închidere:** clickul Fails deschide dovada originală până la comparație,
cu ancestorii corecți și fără asistență din harness; scenariile vecine rămân neschimbate.

## Contracte existente

| Informație | Owner |
|---|---|
| Cauza publicării și închiderea scope-urilor | [Reporting](../../atf-reporting/README.md#cauza-publicării-și-scope-urile-deținute) |
| Tranziții de scope și refuz fără mutație | [Reporting](../../atf-reporting/README.md#atomicitatea-tranzițiilor-de-scope) |
| Timing explicit, capturi și publicare | [Reporting](../../atf-reporting/README.md#timing-e2e-la-ownerul-runtime) |
| Setup și dovezi UI în fallback | [Reporting](../../atf-reporting/README.md#evidence-ui-și-setup-în-fallback) |
| Coloane dinamice și scroll local | [Reporting](../../atf-reporting/README.md#lizibilitatea-dovezilor-în-panouri-înguste) |
| Probe de compatibilitate și browser | [Reporting Tests](../../atf-reporting-tests/README.md) |
| Inventar UI/API/UI+API și controale negative de randare | [Reporting Tests](../../atf-reporting-tests/README.md#tabele-publicate-și-inventar-independent) |
| Controale Git verificate | [Baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate) |
| Criterii active ale consumerilor | [Consumer](ATF_Consumer_Business_Test_Focus_Feedback.md) |
| Identitatea query-ului între modele | [Reporting Tests](../../atf-reporting-tests/README.md#fidelitatea-query-ului-și-asocierea-mock) |

## Acceptanță pentru reevaluarea Reporting și Reporting Tests

- Fidelitatea query-ului și asocierea cu sursa au [contract la owner](../../atf-reporting/README.md#fidelitatea-contractului-query);
  refactorizarea publisherului păstrează aceste oracole și probele negative.
- Orice refactorizare ulterioară a publisherului păstrează oracolele, aceeași fațadă,
  snapshot-uri, ownership, timing și verdict. Regresia nu împrumută expected-ul
  din helperul pe care încearcă să îl verifice.
- Reporting Tests păstrează fixture-uri neutre și probe negative de control:
  inputul greșit este refuzat, iar schimbarea unei asocieri sau ascunderea unui
  rând este detectată. Nu mutați aceste probe în testele business Shop/AutoEx.

Nota se revede numai după [criteriul comun de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor),
cu randare inspectată și regresiile ownerului trecute în TMP.
