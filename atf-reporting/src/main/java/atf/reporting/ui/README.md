<a id="atf-reporting-ui---from-checkpoint-to-renderer-ports"></a>
<a id="atf-reporting-ui--de-la-checkpoint-la-porturile-rendererului"></a>
# ATF Reporting UI - from checkpoint to renderer ports

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

The package consists of UI / API already observed evidence. It has the lifecycle of the scenario, the checkpoints and the publication; the browser, endpoint and business decision belong to the consumer.

[Atlas](../README.md) · [Module contract](../../../../../../README.md#contractul-raportului)

| Reason for change | Reading input | Responsibility |
|---|---|---|
| aperture and profile solved | [UiExtentReportSession](UiExtentReportSession.java), [options](UiExtentReportOptions.java) | façade and compatibility of inputs |
| scenario and unification between phases | [UiScenarioLifecycle](UiScenarioLifecycle.java) | status of scenario and retained checkpoints |
| step, setup, timing and phase closure | [UiPhaseLifecycle](UiPhaseLifecycle.java) | lifecycle of a phase |
| projection and record ports | [UiEvidencePublisher](UiEvidencePublisher.java) | layout / checkpoint and flow compact |
| API exchanges of checkpoint | [UiApiValidationEvidence](validation/UiApiValidationEvidence.java), [publisher](UiEvidencePublisher.java) | `ApiEvidencePublisher` is private collaborator nested |
| the cause of the failure and the permitted catch | [publisher failure](failure/UiFailureEvidencePublisher.java) | diagnostic and publication, without new catch |

<a id="route-of-change"></a>
<a id="traseul-unei-modificări"></a>
## Route of change

- The consumer shall transmit shares, assertions and catch already available.
  `ReportEvidenceProfile` separates layout, screenshot policy and action disclosure.
- The facade keeps public entries; lifecycle manage the state, and
  Publishers write in `AtfReportPorts` ports.
- Compact flower can retain checkpoints until completion. Change
  The group also requires the context of the scenario, not only the method that publishes the current step.

<details>
<summary><strong>Invariants and limits that are easily lost from context</strong></summary>

- Cross- phase unified flow is selected prior to the first phase and asks for profile
  It doesn't change the order of facts or the verdict received from the consumer.
- `ReportPublicationScope` keeps the cause and tries each hold close;
  The delegated rail has its own demand and its own child validation.
- The API assertions of an exchange are not reassociated with the neighbor after the text
  A hollow channel and an exchange without responses are distinct situations.
- API collaborator is private in `UiEvidencePublisher` and publishes each
  valid exchange, including without responses. Boundary requires demand and, without replies, explicit verdict or assessed assertions; INFO alone does not provide it. Diagnosis and attempts remain in the performance evidence without the invented answers details.
- The terminal diagnosis is inspectable in Tests and indexed for the quest
  FAIL via [joint contract](../../../../../../README.md#execuție-api-terminată-fără-răspuns). Backlink opens original evidence execution without an identity borrowed from the neighbor.
- The Given / When / Then succession uses the context of the scenario; the AND continuations
  are not a global replacement of keywords in HTML.
- The execution and publication timings are separate facts; the renderer does not
  Perform the operation again to get durations or missing evidence.

</details>

<a id="active-evidence-and-criteria"></a>
<a id="probe-și-criterii-active"></a>
## Active evidence and criteria

- [UiApiValidationContractComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/ui/validation/UiApiValidationContractComponentTest.java): shape and association of API channel.
- [ReportingStandaloneVerdictComponentTest](../../../../../test/java/atf/reporting/standalone/ReportingStandaloneVerdictComponentTest.java) and [UiTerminalExchangeReportComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/execution/UiTerminalExchangeReportComponentTest.java): refusal
  Default success, all UI pathways and Extent / fallback diagnosis parity.
- [UiCrossPhaseCompactFlowComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/ui/profile/UiCrossPhaseCompactFlowComponentTest.java): group in / over phases.
- [UiPublicationFailureComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/ui/publication/UiPublicationFailureComponentTest.java): body, acquisition and close through the public facade.
- [UiPhaseTimingLifecycleComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/ui/timing/UiPhaseTimingLifecycleComponentTest.java) and [ReportGherkinSequenceTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/gherkin/ReportGherkinSequenceTest.java): timing and vocabulary.

The composition and browser samples are in [Reporting Tests](../../../../../../../atf-reporting-tests/README.md). The standalone boundary is checked in [Reporting library](../../../../../../src/test/java/atf/reporting/standalone/ReportingStandaloneVerdictComponentTest.java). API supported and pure projection extraction have [separate criteria](../../../../../../../docs/feedback/ATF_Reporting_Feedback.md).


<a id="neutral-observations-on-uiphase"></a>
<a id="observații-neutre-pe-uiphase"></a>
## Neutral observations on UiPhase

[UiPhase](UiExtentReportSession.java) exposes observation and supportsObservations through existing lifecycle.

- [UiPhaseLifecycle](UiPhaseLifecycle.java) validates the status, delegates the phase port and separates the publication from the measured timer.
- [ReportObservation](../model/ReportObservation.java) keeps facts typed, without demands or assertions invented.
- The observation body uses the configured body limit; expected / observed ACTION / OBSERVE have separated the preview 240 plus fully redacted cell.
