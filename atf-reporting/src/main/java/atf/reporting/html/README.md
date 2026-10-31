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
- Contextul unui flow UI unificat folosește disclosure-ul neutru comun,
  separat de response evidence; validările sale au propriul `Assertions · n`,
  colapsat inițial. Agregarea per fază nu este reclasificată drept compactare.
- Titlurile scenariilor și claselor rămân integrale și se împachetează în
  sidebar. Sub sau la 760 px, navigarea precede articolul pe o singură coloană;
  anchor-ul, statusul și identitatea scenariului rămân cele originale.

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

## Probe și contractul sesiunii

[HtmlReportSessionTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/html/HtmlReportSessionTest.java) și [AtfReportSemanticSnapshotTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/semantic/AtfReportSemanticSnapshotTest.java) verifică modelul și
paritatea; [UiFallbackFailureEvidenceTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/ui/UiFallbackFailureEvidenceTest.java) păstrează cauza și izolarea scenariilor.
Probe de browser și responsive sunt la [Reporting Tests](../../../../../../../atf-reporting-tests/README.md).

Semantica flush/publicare/path/threading este declarată în
[contractul sesiunii](../../../../../../README.md#publicare-și-ieșire).
Consumerul așteaptă momentul de publicare al rendererului ales și serializează
accesul fallback; portul comun nu promite acces concurent.

