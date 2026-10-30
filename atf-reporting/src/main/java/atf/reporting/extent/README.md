# ATF Reporting Extent — scope-uri și publicarea artifactului final

## Pe scurt

Pachetul adaptează porturile de raportare la arborele Extent și deține
publicarea finală. Lifecycle-ul este separat de postprocesarea HTML-ului;
niciuna nu execută scenariul consumerului.

[Atlas](../README.md) · [Publicare](../../../../../../README.md#publicare-și-ieșire) ·
[Renderer fallback](../html/README.md)

| Schimbare | Sursa de pornire | Owner |
|---|---|---|
| intrare și thread-ul sesiunii | [ExtentReportSession](session/ExtentReportSession.java) | path, configurație și finalizare |
| scenariu, fază și ierarhie | [ExtentReportTree](session/ExtentReportTree.java), [lifecycle nod](session/ExtentNodeLifecycle.java) | arbore și tranziții |
| request, validation, assertion-step | scope-urile din acest pachet | porturi peste nodurile Extent |
| workspace și commit | [SessionPublication](session/ExtentReportSessionPublication.java) | staging privat și destinație finală |
| transformarea Spark în HTML autonom | [PublicationPipeline](session/ExtentReportPublicationPipeline.java) | assets, navigare, Overview și deduplicare |
| ore Categories în format 24h | [TimestampReporter](session/ExtentReportTimestampReporter.java) | adaptor punctual al template-ului vendor |

## Ordinea care contează

- `flush()` scrie outputul vendor în staging. `close()` finalizează nodurile,
  face flush și publică rezultatul final prin ownerul de publication.
- Pipeline-ul inlinează assets Spark, injectează tema/runtime-ul ATF și
  finalizează documentul înaintea mutării către calea publică.
- Evenimentul diagnostic de publicare urmează confirmării fișierului final;
  scope-ul deține cleanup-ul workspace-ului.

<details>
<summary><strong>Invariante ale lifecycle-ului</strong></summary>

- Sesiunea Extent este limitată la thread-ul care a creat-o. Un test care
  demonstrează alt renderer nu autorizează acces concurent la această sesiune.
- Pornirea unui nod și rollback-ul au probe proprii; validarea inputului și
  dobândirea nodului nu sunt aceeași etapă.
- Reînchiderea nu publică un eveniment duplicat. Cauza publicării și rezultatul
  notificării diagnostice sunt responsabilități distincte.
- `ReportPublicationCoordinator` serializează mutarea la destinație. Nu este
  un scheduler de teste și nu reexecută operația business.
- Template-ul Categories vine din Extent; adaptorul timestamp refuză expresia
  vendor schimbată, în loc să pretindă o conversie reușită.

</details>

## Probe și consolidare

[ReportingStandaloneVerdictComponentTest](../../../../../test/java/atf/reporting/standalone/ReportingStandaloneVerdictComponentTest.java) verifică contracte locale;
[UiPublicationFailureComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/ui/publication/UiPublicationFailureComponentTest.java) și probele de publicare din
[Reporting Tests](../../../../../../../atf-reporting-tests/README.md) completează fault-path-ul și artifactul.

Diferențele față de fallback și delimitarea API-ului public au
[criterii active](../../../../../../../docs/feedback/ATF_Reporting_Feedback.md#report-session-contract-44--lifecycle-explicit-pentru-fiecare-renderer).
La o modificare de finalizare se citesc și [assets](../asset/ReportAssetLoader.java)
și [runtime](../runtime/README.md), deoarece HTML-ul final include aceste două boundary-uri.

