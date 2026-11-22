<a id="atf-reporting-html---renderer-fallback-and-semantic-population"></a>
<a id="atf-reporting-html--rendererul-fallback-și-populația-semantică"></a>
# ATF Reporting HTML - renderer fallback and semantic population

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

Fallback builds views from the same population that are immutable by scenarios and evidence. It is not a browser and does not repair missing observations; semantics are checked against common snapshots, separated from visual differences from Extent.

[Atlas](../README.md) · [Models](../model/README.md) · [Extent](../extent/README.md)

| Boundary | Source | Responsibility |
|---|---|---|
| session and output | [HtmlReportSession](HtmlReportSession.java) | render, flush and close |
| conditions under construction | [HtmlReportSemanticDocument](HtmlReportSemanticDocument.java) | ID, orders and execution snack |
| lifecycle | [scope](scope) | Ports of scenario / phase / request / validation |
| views | [Tests](HtmlReportTestsView.java), [Overview](HtmlReportOverviewView.java), [Fails](HtmlReportFailsView.java) | projections above the same model |
| evidence | [HtmlReportEvidenceView](evidence/HtmlReportEvidenceView.java) | Marup and common display policy |
| shell and navigation | [HtmlReportShell](HtmlReportShell.java), [script](script/HtmlReportNavigationScript.java) | Fallback document and interactions |

<a id="from-status-to-document"></a>
<a id="de-la-stare-la-document"></a>
## From status to document

- The mutable document is converted into `ReportExecutionModel` immutable;
  Semantic recorder keeps in parallel renderer-neutral snapshots.
- `render()` checks the equality of populations before building views.
  A semantic difference is not compensated by hiding a HTML line.
- Views Tests, Categories, Fails and Overview come from the same execution;
  Diagnosis of a phase does not automatically become a new business assertion.
- The context of a unified UI flow uses the common neutral discoloration,
  separate from the obvious responses; its validations have their own `Assertions · n`, initially collapsed. Aggregation per phase is not reclassified as compaction.
- The titles of scenarios and classes remain integral and are packed in
  sub or 760 px, navigation precedes the article on a single column; the anchor, status and identity of the script remaining the original.

<details>
<summary><strong>Differences to be explicitly read</strong></summary>

- In the current implementation, `flush()` writes directly the fallback file;
  `close()` calls flush and marks closed session.
- The manufacturer keeps the received pathic; the Extent Thread Guard is not
  These facts are not a promise of thread-safety.
- Script navigation is an injectable port; its replacement does not change
  Semantic lifecycle or the consumer's responsibility for the verdict.
- Semantic parity does not require copying DOM Spark. Controls and
  The disclosures shall be checked by the evidence of the renderer concerned.

</details>

<a id="evidence-and-contract-of-the-session"></a>
<a id="probe-și-contractul-sesiunii"></a>
## Evidence and contract of the session

[HtmlReportSessionTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/html/HtmlReportSessionTest.java) and [AtfReportSemanticSnapshotTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/semantic/AtfReportSemanticSnapshotTest.java) check model and parity; [UiFallbackFailureEvidenceTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/ui/UiFallbackFailureEvidenceTest.java) keeps the cause and isolation of scenarios. Browser and responsive samples are at [Reporting Tests](../../../../../../../atf-reporting-tests/README.md).

Semantics flush / publication / path / thread is declared in [contract of the session](../../../../../../README.md#publicare-și-ieșire). The consumer waits for the publication moment of the chosen renderer and serializations fallback access; the common port does not promise competitive access.

