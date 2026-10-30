# ATF Reporting Model — fapte imutabile și proiecții comune

## Pe scurt

Pachetul descrie faptele de execuție și dovezile fără a porni transportul sau
browserul. Modelele imutabile se disting de agregatele mutabile care le
construiesc în `semantic` și în documentul fallback.

[Atlas](../README.md) · [API și adaptoare](../api/README.md) ·
[Contracte typed](../contract/AtfReportPorts.java)

| Model sau mecanism | Sursa | Context esențial |
|---|---|---|
| transport observat | [ReportExecutionEvidence](ReportExecutionEvidence.java) | response poate lipsi; attempts, trace și termination rămân distincte |
| arbore fallback imutabil | [ReportExecutionModel](ReportExecutionModel.java) | populația din care se obțin toate views |
| proiecții secundare | [ReportExecutionProjections](projection/ReportExecutionProjections.java) | Overview, Categories și Fails din aceeași populație |
| snapshot comun | [AtfReportSemanticSnapshot](../semantic/AtfReportSemanticSnapshot.java) | egalitate renderer-neutrală, fără HTML/CSS |
| construcția semantică | [recorder](../semantic/AtfReportSemanticRecorder.java), [state](../semantic/AtfReportSemanticState.java) | mutație încapsulată și snapshot imutabil |
| durate | [timing](timing) | durate măsurate și disponibilitate, separat de publicare |

## Identitate, status și disponibilitate

- `ClassSnapshot.name` identifică ownerul clasei; `ScenarioSnapshot` are `id`,
  `name`, faze și status. Titlul randat și preview-ul nu înlocuiesc identitatea.
- Statusul transmis de caller se reconciliază cu dovezile conform contractului
  de raportare; un renderer nu reevaluează comparația folosind textul afișat.
- Absența response-ului nu se transformă în status HTTP 200 sau într-un body
  inventat. Canalul API gol este diferit de un snapshot terminal.
- Overview numără PASS + WARNING în totalul pass-like, conform contractului;
  WARNING rămâne distinct în scenariu și nu intră în Fails. Collectorul comun
  păstrează aceeași populație pentru totaluri, categorii și failure-uri.
- [ReportExecutionFailure](diagnostic/ReportExecutionFailure.java) proiectează
  cauza transportului, iar collectorul o indexează numai dintr-un request FAIL.
  Lipsa response-ului nu transformă un eșec așteptat PASS într-un failure de index.

<details>
<summary><strong>Tipuri ușor de confundat</strong></summary>

- `ReportJsonBody` păstrează direcția și formatul; numele istoric JSON nu
  restrânge toate instanțele la răspunsuri `application/json`.
- `ReportScreenshot` transportă un payload Base64. Verificarea semnăturii PNG
  nu este o captură browser și nu dovedește că imaginea corespunde momentului business.
- Modelul semantic și modelul de execuție sunt read models; starea internă
  mutabilă nu este expusă pentru a fi modificată de un view.
- Un câmp temporal absent nu este zero măsurat. Timing-ul lifecycle-ului și
  suma evidence-ului au surse și semnificații diferite.

</details>

## Verificare

[AtfReportSemanticSnapshotTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/semantic/AtfReportSemanticSnapshotTest.java) compară populațiile rendererelor;
[ReportingStandaloneVerdictComponentTest](../../../../../test/java/atf/reporting/standalone/ReportingStandaloneVerdictComponentTest.java) păstrează verdictul local, iar
[ReportTransportTimingTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/api/timing/ReportTransportTimingTest.java) verifică disponibilitatea duratelor.

Probele sunt legate din [modul](../../../../../../README.md#verificare-rapidă)
și [Reporting Tests](../../../../../../../atf-reporting-tests/README.md).

- [ReportingStandaloneVerdictComponentTest](../../../../../test/java/atf/reporting/standalone/ReportingStandaloneVerdictComponentTest.java) păstrează distincția dintre canal
  API gol, response absent și verdict furnizat de caller.
- Fără response, un status diferit de INFO sau aserțiuni evaluate este obligatoriu;
  metadata de transport nu decide singură succesul.
- [Contractul terminal](../../../../../../README.md#execuție-api-terminată-fără-răspuns)
  leagă această regulă de publicarea Extent/fallback și diagnosticul inspectabil.

