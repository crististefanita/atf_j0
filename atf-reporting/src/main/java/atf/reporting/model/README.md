<a id="atf-reporting-model---immeasurable-facts-and-common-projections"></a>
<a id="atf-reporting-model--fapte-imutabile-și-proiecții-comune"></a>
# ATF Reporting Model - Immeasurable facts and common projections

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

The package describes the execution facts and evidence without starting the transport or browser. The immutable models are distinguished from the mutable aggregates that build them in `semantic` and the fallback document.

[Atlas](../README.md) · [API and adapters](../api/README.md) · [Typed contracts](../contract/AtfReportPorts.java)

| Model or mechanism | Source | Essential context |
|---|---|---|
| observed transport | [ReportExecutionEvidence](ReportExecutionEvidence.java) | Answers may be missing; attentions, traces and termination remain distinct |
| tree fallback immutable | [ReportExecutionModel](ReportExecutionModel.java) | population from which all views are obtained |
| secondary projections | [ReportExecutionProjections](projection/ReportExecutionProjections.java) | Overview, Categories and Fails of the same population |
| joint snapshot | [AtfReportSemanticSnapshot](../semantic/AtfReportSemanticSnapshot.java) | renderer-neutral equality without HTML / CSS |
| Semantic construction | [recorder](../semantic/AtfReportSemanticRecorder.java), [state](../semantic/AtfReportSemanticState.java) | Envelope mutation and immutable snapshot |
| durations | [timing](timing) | measured durations and availability, separately from publication |

<a id="identity-status-and-availability"></a>
<a id="identitate-status-și-disponibilitate"></a>
## Identity, status and availability

- `ClassSnapshot.name` identifies class owner; `ScenarioSnapshot` has `id`,
  `name`, phases and status. The title is consolidated and the preview is do not replace identity.
- The status of the caller shall be reconciled with the evidence according to contract
  reporting; a renderer does not reassess the comparison using the text displayed.
- Absence of response does not turn into status HTTP 200 or a body
  Invented. API channel empty is different from a terminal snapshot. Without responses, the caller provides a different status from INFO or assessed assertions; the transport metadata does not decide on its own success.
- Overview counts PASS + WARNING in total pass-like according to contract;
  WARNING remains distinct in the scenario and does not enter the Fails. The common collector keeps the same population for totals, categories and failures.
- [ReportExecutionFailure](diagnostic/ReportExecutionFailure.java) designs
  The cause of the transport, and the collector indexes it only from a quest FAIL. The lack of response does not transform an expected failure PASS into an index failure.

<details>
<summary><strong>Types easily confused</strong></summary>

- `ReportJsonBody` maintains direction and format; historical name JSON no
  It restricts all courts to the `application/json` replies.
- `ReportScreenshot` carries a payload Base64. PNG signature check
  is not a browser catch and does not prove that image corresponds to the business moment.
- Semantic model and execution model are read models; internal status
  The mutable is not exposed to be altered by a view.
- A time field absent is not measured zero. Lifecycline timings and
  the amount of the evidence has different sources and meanings.

</details>

<a id="verification"></a>
<a id="verificare"></a>
## Verification

[AtfReportSemanticSnapshotTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/semantic/AtfReportSemanticSnapshotTest.java) compares renderers' populations; [ReportingStandaloneVerdictComponentTest](../../../../../test/java/atf/reporting/standalone/ReportingStandaloneVerdictComponentTest.java) maintains local verdict, and [ReportTransportTimingTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/api/timing/ReportTransportTimingTest.java) checks the availability of durations.

The samples are related from [plugin](../../../../../../README.md#verificare-rapidă) and [Reporting Tests](../../../../../../../atf-reporting-tests/README.md).

The terminal input contract and Extent / fallback sample are at [unanswered execution owner](../../../../../../README.md#execuție-api-terminată-fără-răspuns).
