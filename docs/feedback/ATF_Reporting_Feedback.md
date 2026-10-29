# ATF Reporting — prezentarea și publicarea dovezilor

## Pe scurt

Criteriile lifecycle și timing au contracte și regresii la owner. Rămân
fidelitatea query-ului în audit și delimitarea publisherului; nu se extind funcționalitățile ori numărul modulelor.

## REPORT-QUERY-FIDELITY-23 — query incomplet validat ca fiind conform

**Prioritate: 83/100** = impact 35 + lacună 24 + lucrabil 17 + documentare 7.
**Owner:** Reporting pentru model și auditor; Reporting Tests pentru conversia
Mock și probele de integrare. Core păstrează propriul model, cu multiplicitate.

Probele pure folosesc `ApiReportContractInput.of(...)`, cu response `{}` și
HTTP 200 compatibile cu expected-ul fix. Auditorul complet întoarce
`compliant=true`, fără violations, în ambele cazuri negative de mai jos.

| Expected independent | Input observat | Rezultat curent |
|---|---|---|
| două perechi `tag=red`, `tag=blue` | numai `tag=blue` | Core refuză subsetul incomplet; Reporting îl acceptă |
| `required=wanted` | numai `different=observed` | resolverul revine la candidații fără match query și auditorul declară conformitate |
| Core cu două perechi `tag` | `ReportQueryParameters.from(coreQuery)` | conversia prin `asMap()` păstrează numai ultima pereche |

Sursele sunt [ReportQueryParameters](../../atf-reporting/src/main/java/atf/reporting/api/ReportQueryParameters.java),
[ApiReportRouteResolver](../../atf-reporting/src/main/java/atf/reporting/api/contract/internal/ApiReportRouteResolver.java)
și [ApiReportContractAuditor](../../atf-reporting/src/main/java/atf/reporting/api/contract/ApiReportContractAuditor.java).
Proba declară un defect al auditorului pur; nu pretinde că a reprodus un incident pe un site public.

- [ ] Păstrați lista de perechi și multiplicitatea la conversie; view-ul legacy
      Map rămâne explicit lossy și nu devine reprezentarea internă a contractului.
- [ ] `containsAll` verifică fiecare pereche și numărul aparițiilor, inclusiv
      duplicate identice. Nu deduceți conformitatea din ultima valoare pe nume.
- [ ] Când URL-ul disponibil contrazice query-ul cerut, nici fallback-ul la
      candidați și nici alegerea după status nu produc PASS. Păstrați diagnosticul
      de nepotrivire; URL lipsă/invalid nu devine dovadă de query potrivit.
- [ ] Păstrați comportamentul legacy pentru contracte fără query și subsetul
      permis cu parametri suplimentari; probați ordini diferite, valori goale,
      duplicate identice/diferite, nume prefix și delimitatori codificați.
- [ ] Expected-ul probei negative este literal sau construit independent de
      funcția reparată. Probați auditorul complet și adaptorul Mock, nu numai
      colecția de query; [identitatea rutelor](ATF_Code_Maintenance_Feedback.md#api-query-identity-01--identitate-structurată-separată-de-eticheta-afișată)
      rămâne un criteriu distinct.
- [ ] Reporting rămâne standalone; adaptoarele către Core/Mock sunt la ownerul
      de compoziție. Verificați și proiecția Extent/fallback a rezultatului FAIL,
      fără schimbarea titlurilor, verdictului ori limitei redactate 240.

**Închidere:** query-ul incomplet sau contradictoriu produce încălcare explicită;
același input nu mai poate trece prin pierderea perechilor ori selecția unui
candidat neeligibil. [ApiReportContractAuditorTest](../../atf-reporting-tests/src/test/java/atf/reporting/api/contract/ApiReportContractAuditorTest.java)
este regresie adiacentă, nu dovada cazurilor lipsă.

## REPORT-EVIDENCE-RESPONSIBILITIES-20 — delimitarea protocoalelor de publicare

**Prioritate: 64/100** = impact 26 + lacună 14 + lucrabil 17 + documentare 7.
**Owner:** Reporting în main; probele neutre rămân în Reporting Tests.

`UiEvidencePublisher` coordonează setup-ul, flow-ul unificat în fază și peste
faze, request-urile API imbricate, capturile și failure evidence. Motivele de
schimbare sunt distincte: `publishApiEvidence()` depinde de schema exchange-ului,
`publishCrossPhaseUnifiedFlow()` de gruparea checkpoint-urilor, iar
`publishScreenshot()` de politica de captură. Fațada și lifecycle-ul există deja;
restanța nu se rezolvă prin mutarea lor într-un nou modul sau într-un helper generic.

- [ ] Extrageți mai întâi proiecția API într-un colaborator coeziv, păstrând
      aceeași fațadă și aceleași date evaluate; separați apoi gruparea flow-ului
      numai dacă rămâne un motiv independent de schimbare.
- [ ] Politica pură primește modele typed; publicarea primește porturi Reporting.
      Nu introduceți browser, JUnit, Shop ori AutoEx în producția librăriei.
- [ ] Păstrați snapshot-ul semantic, ownership-ul UI/API, FAIL/SKIP, markerii de
      timing, fazele, capturile și limita 240; fără reevaluarea comparației din text.
- [ ] Comparați Extent/fallback pe succes, failure, flow peste faze și publicare
      eșuată; verificați expandările și raportul business unic individual/prin suită.

**Închidere:** protocoalele pot fi schimbate și probate separat, fără o fațadă
mai mare sau un API public suplimentar; contractele randate rămân identice.

## Contracte existente

| Informație | Owner |
|---|---|
| Tranziții de scope și refuz fără mutație | [Reporting](../../atf-reporting/README.md#atomicitatea-tranzițiilor-de-scope) |
| Timing explicit, capturi și publicare | [Reporting](../../atf-reporting/README.md#timing-e2e-la-ownerul-runtime) |
| Setup și dovezi UI în fallback | [Reporting](../../atf-reporting/README.md#evidence-ui-și-setup-în-fallback) |
| Coloane dinamice și scroll local | [Reporting](../../atf-reporting/README.md#lizibilitatea-dovezilor-în-panouri-înguste) |
| Probe de compatibilitate și browser | [Reporting Tests](../../atf-reporting-tests/README.md) |
| Controale Git verificate | [Baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate) |
| Criterii active ale consumerilor | [Consumer](ATF_Consumer_Business_Test_Focus_Feedback.md) |
| Identitatea query-ului între modele | [Boundary-uri reutilizabile](ATF_Code_Maintenance_Feedback.md#api-query-identity-01--identitate-structurată-separată-de-eticheta-afișată) |

## Acceptanță pentru reevaluarea Reporting și Reporting Tests

- Fidelitatea query-ului rămâne deschisă; randarea dinamică demonstrată nu
  compensează un auditor care acceptă un input greșit.
- Publisherul se refactorizează după fixarea oracolelor, păstrând aceeași fațadă,
  snapshot-uri, ownership, timing și verdict. Regresia nu împrumută expected-ul
  din helperul pe care încearcă să îl verifice.
- Reporting Tests păstrează fixture-uri neutre și probe negative de control:
  inputul greșit este refuzat, iar schimbarea unei asocieri sau ascunderea unui
  rând este detectată. Nu mutați aceste probe în testele business Shop/AutoEx.

Nota se revede numai după [criteriul comun de cod](../README_Atf_CodeBestPractices.md#rubrica-reevaluării-modulelor),
cu randare inspectată și regresiile ownerului trecute în TMP.
