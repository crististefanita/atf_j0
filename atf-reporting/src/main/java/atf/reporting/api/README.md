# ATF Reporting API — modele de transport și audit fără executor HTTP

## Pe scurt

Pachetul normalizează fapte de request/response și verifică un contract API din
evidence deja furnizat. Nu trimite cereri, nu autentifică un client și nu reia
operații; acele responsabilități rămân în consumer.

[Atlas](../README.md) · [Fidelitatea query-ului](../../../../../../README.md#fidelitatea-contractului-query)

| Întrebare | Sursa principală | Limită |
|---|---|---|
| ce request/response este publicat? | [ReportRequest](ReportRequest.java), [ReportResponse](ReportResponse.java) | DTO-uri de raportare |
| cum intră un model legacy? | [ReportModelAdapters](ReportModelAdapters.java) | reflection delimitată, fără import Core |
| ce înseamnă query complet? | [ReportQueryParameters](ReportQueryParameters.java) | perechi și multiplicitate, separat de view-ul legacy |
| ce măsoară attempt-urile? | [ReportTransportTiming](evidence/ReportTransportTiming.java), [sumarul](evidence/ReportExecutionSummary.java) | date observate, inclusiv response indisponibil |
| ce verifică artifactul API? | [ApiReportContractAuditor](contract/ApiReportContractAuditor.java) | audit pur al contractului, fără endpoint live |

## Fluxul datelor

- Intrarea typed este preferată; adaptorul legacy citește accessorii ceruți și
  refuză valorile malformate cu tipul/câmpul sursei.
- [ReportExecutionEvidence](../model/ReportExecutionEvidence.java) păstrează
  request, response opțional prin absență, provenance, attempts și termination.
- Contractul și proiecția JSON consumă aceste fapte; HTTP-ul și verdictul
  business nu sunt recuperate printr-un client ascuns în Reporting.

<details>
<summary><strong>Distincțiile esențiale pentru compatibilitate</strong></summary>

- `ReportQueryParameters.values()` păstrează aparițiile; `asMap()` este view
  legacy cu ultima valoare pe nume. Eticheta lizibilă nu este identitatea completă.
- `ReportRequest.queryParameters()` păstrează aceste perechi prin builder și
  adaptarea externă. Semnătura sigură păstrează multiplicitatea și ordinea
  aparițiilor aceluiași nume; politica de mascare a credentialelor rămâne explicită.
- Când politica de captură a eliminat valorile, `queryFingerprint()` păstrează
  numai identitatea opacă. Nu reconstruiește perechile; digestul nu apare în HTML.
  Schimbarea query-ului prin builder invalidează amprenta capturii precedente.
- `partialExecution(...)` cere explicit disponibilitatea response-ului din
  snapshot-ul extern; istoricul și termination nu fabrică un răspuns nou.
- Semnătura pentru prezentare și identitatea request-ului nu autorizează replay.
- `ReportModelAdapters` este o punte publică existentă; eliminarea reflection-ului
  cere inventarul consumatorilor și o migrare demonstrată.

</details>

## Probe și context adiacent

- [TerminalExecutionProjectionTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/api/execution/terminal/TerminalExecutionProjectionTest.java): adaptarea unui snapshot parțial și diagnosticul terminal.
- [ReportTransportTimingTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/api/timing/ReportTransportTimingTest.java): durate și disponibilitate, fără timp inventat.
- Contractele de query și asociere sunt legate din [Reporting Tests](../../../../../../../atf-reporting-tests/README.md#fidelitatea-query-ului-și-asocierea-mock).

Pentru statusul unui exchange atașat UI se citește și
[UiApiValidationEvidence](../ui/validation/UiApiValidationEvidence.java).

- Fără response, verdictul vine explicit de la caller sau din aserțiunile evaluate;
  inputul nejudecat este refuzat înaintea publicării.
- `ApiExchangeEvidence.publish` păstrează request-ul și execution evidence,
  cu URL necunoscut gol și fără replay presupus.
- Contractul și probele sunt la
  [execuția terminală](../../../../../../README.md#execuție-api-terminată-fără-răspuns).

