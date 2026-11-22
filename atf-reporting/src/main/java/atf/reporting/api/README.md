<a id="atf-reporting-api---transport-and-audit-models-without-executor-http"></a>
<a id="atf-reporting-api--modele-de-transport-și-audit-fără-executor-http"></a>
# ATF Reporting API - transport and audit models without executor HTTP

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

The package normalizes the facts of demand / response and verifies a API contract from the evidence already provided. Do not send requests, do not authenticate a client or resume operations; those responsibilities remain in the consumer.

[Atlas](../README.md) · [Query'](../../../../../../README.md#fidelitatea-contractului-query)

| Question | Main source | Boundary |
|---|---|---|
| What request / response is published? | [ReportRequest](ReportRequest.java), [ReportResponse](ReportResponse.java) | DTO-uri reporting |
| How does a legacy model get in? | [ReportModelAdapters](ReportModelAdapters.java) | delimited reflection, free from import Core |
| What does query mean completely? | [ReportQueryParameters](ReportQueryParameters.java) | pairs and multiplication, separate from the legacy view |
| What does the attempts measure? | [ReportTransportTiming](evidence/ReportTransportTiming.java), [summary](evidence/ReportExecutionSummary.java) | observed data, including unavailable response |
| What does the API artifact check? | [ApiReportContractAuditor](contract/ApiReportContractAuditor.java) | pure audit of contract without live endpoint |

<a id="data-flow"></a>
<a id="fluxul-datelor"></a>
## Data flow

- Typed input is preferred; the legacy adapter reads requested access and
  refuse malformed values with source type / field.
- [ReportExecutionEvidence](../model/ReportExecutionEvidence.java)
  Request, optional response by absence, provenance, attempts and termination.
- JSON contract and projection consume these facts; HTTP and verdict
  business are not recovered through a hidden client in Reporting.

<details>
<summary><strong>Distinctions essential for compatibility</strong></summary>

- `ReportQueryParameters.values()` keeps up; `asMap()` is view
  Legacy with the last value by name. The readable label is not the full identity.
- `ReportRequest.queryParameters()` keeps these pairs by Builder and
  The secure signature keeps the multiplication and order of the same name; the policy of masking the credentials remains explicit.
- When the catch policy has eliminated the values, `queryFingerprint()` maintains
  only opaque identity. It does not rebuild pairs; the digest does not appear in HTML. Changing the query through the building invalidates the footprint of the previous catch.
- `partialExecution(...)` explicitly requests the availability of the response from the
  external snack; history and termination do not produce a new response.
- The signature for the presentation and the identity of the request shall not authorise the reply.
- `ReportModelAdapters` is an existing public bridge; the elimination of reflection
  calls for consumer inventory and demonstrated migration.

</details>

<a id="samples-and-adjacent-context"></a>
<a id="probe-și-context-adiacent"></a>
## Samples and adjacent context

- [TerminalExecutionProjectionTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/api/execution/terminal/TerminalExecutionProjectionTest.java): adaptation of partial snapshot and terminal diagnosis.
- [ReportTransportTimingTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/api/timing/ReportTransportTimingTest.java): duration and availability, without invented time.
- Query and association contracts are linked to [Reporting Tests](../../../../../../../atf-reporting-tests/README.md#fidelitatea-query-ului-și-asocierea-mock).

The status of an attached UI exchange also reads [UiApiValidationEvidence](../ui/validation/UiApiValidationEvidence.java).

- Without replies, the verdict comes explicitly from the caller or from the assertions assessed;
  untried input is denied prior to publication.
- `ApiExchangeEvidence.publish` keeps the requester and execution evidence,
  with URL unknown empty and without assumed replay.
- The contract and evidence are at
  [terminal execution](../../../../../../README.md#execuție-api-terminată-fără-răspuns).

