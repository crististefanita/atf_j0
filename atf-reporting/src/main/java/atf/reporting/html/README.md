# ATF Reporting HTML — rendererul fallback și populația semantică

## Pe scurt

Fallback-ul construiește views din aceeași populație imutabilă de scenarii și
dovezi. Nu este un browser și nu repară observații lipsă; semantica se verifică
împotriva snapshot-ului comun, separat de diferențele vizuale față de Extent.

[Atlas](../README.md) · [Modele](../model/README.md) · [Extent](../extent/README.md)

| Boundary | Sursă | Responsabilitate |
|---|---|---|
| sesiune și output | [HtmlReportSession](HtmlReportSession.java) | render, flush și close |
| stare în construcție | [HtmlReportSemanticDocument](HtmlReportSemanticDocument.java) | identitate, ordine și snapshot-ul execuției |
| lifecycle | [scope](scope) | porturile scenariului/fazei/request-ului/validării |
| views | [Tests](HtmlReportTestsView.java), [Overview](HtmlReportOverviewView.java), [Fails](HtmlReportFailsView.java) | proiecții peste același model |
| dovezi | [HtmlReportEvidenceView](evidence/HtmlReportEvidenceView.java) | markup și politica de display comună |
| shell și navigare | [HtmlReportShell](HtmlReportShell.java), [script](script/HtmlReportNavigationScript.java) | document și interacțiuni fallback |

## De la stare la document

- Documentul mutabil este transformat în `ReportExecutionModel` imutabil;
  recorderul semantic păstrează în paralel snapshot-ul renderer-neutral.
- `render()` verifică egalitatea populațiilor înainte de construirea views.
  O diferență semantică nu este compensată prin ascunderea unui rând HTML.
- Views Tests, Categories, Fails și Overview provin din aceeași execuție;
  diagnosticul unei faze nu devine automat o aserțiune business nouă.

<details>
<summary><strong>Diferențe care trebuie citite explicit</strong></summary>

- În implementarea curentă, `flush()` scrie direct fișierul fallback;
  `close()` apelează flush și marchează sesiunea închisă.
- Constructorul păstrează path-ul primit; guard-ul de thread al Extent nu este
  prezent aici. Aceste fapte nu reprezintă o promisiune de thread-safety.
- Navigation script este un port injectabil; înlocuirea lui nu schimbă
  lifecycle-ul semantic sau responsabilitatea consumerului pentru verdict.
- Paritatea semantică nu cere copierea DOM-ului Spark. Controalele și
  disclosure-urile se verifică și prin probele rendererului respectiv.

</details>

## Probe și criteriu rămas

[HtmlReportSessionTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/html/HtmlReportSessionTest.java) și [AtfReportSemanticSnapshotTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/semantic/AtfReportSemanticSnapshotTest.java) verifică modelul și
paritatea; [UiFallbackFailureEvidenceTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/ui/UiFallbackFailureEvidenceTest.java) păstrează cauza și izolarea scenariilor.
Probe de browser și responsive sunt la [Reporting Tests](../../../../../../../atf-reporting-tests/README.md).

Contractul comun flush/publicare/path/threading rămâne de închis în
[REPORT-SESSION-CONTRACT-44](../../../../../../../docs/feedback/ATF_Reporting_Feedback.md#report-session-contract-44--lifecycle-explicit-pentru-fiecare-renderer).

