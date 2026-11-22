<a id="atf-reporting-evidence---observed-data-format-and-dislocure"></a>
<a id="atf-reporting-evidence--date-observate-format-și-disclosure"></a>
<a id="atf-reporting-evidence---observed-data-format-and-disclosure"></a>
# ATF Reporting Evidence - observed data, format and disclosure

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

The package converts the evidence received in the tables and markup. It holds the display policy and the presentation of existing catches; it does not capture the screen and does not perform the operation to complete a missing row.

[Atlas](../README.md) · [UI profile](profile/ReportEvidenceProfile.java) · [Display contract](../../../../../../README.md#lizibilitatea-dovezilor-în-panouri-înguste)

| Change | Source | Responsibility |
|---|---|---|
| Display between UI and transport | [ExtentEvidenceRenderer](ExtentEvidenceRenderer.java) | coordinate specialized renderings without lifecycle of node |
| request/response/attempts | [ExtentTransportEvidenceRenderer](ExtentTransportEvidenceRenderer.java) | Technical tables and evidence JSON |
| shares, observations and assertions UI | [ExtentUiEvidenceRenderer](ExtentUiEvidenceRenderer.java), [ui](ui) | Owner and presentation UI |
| Redaction order / format / ceiling | [ReportEvidenceJsonFormatter](markup/ReportEvidenceJsonFormatter.java) | Extent / fallback policy |
| Lines and tables | [EvidenceTableFormatter](EvidenceTableFormatter.java), [ReportEvidenceTable](ReportEvidenceTable.java) | Display structure and data |
| exceptional summary | [ReportFailureDescription](diagnostic/ReportFailureDescription.java) | graph bounded, redaction before 240; no business decision |
| image attached to actions | [UiActionScreenshotPolicy](policy/UiActionScreenshotPolicy.java) | cardinality, without browser catch |

<a id="the-reading-pipelineum-together"></a>
<a id="pipeline-ul-de-citit-împreună"></a>
<a id="the-reading-pipeline-together"></a>
## The reading pipeline together

- The model keeps the facts complete for verdict and association; copy of
  Display applies redaction, formatting and budget configured.
- JSON formatter is used by both renderers. A correction only in HTML
  Extent may leave the fallback or another unaligned projection.
- Disclosureul holds the interaction; four columns of assertions and six of
  actions have different roles. A comparison time is not invented during action.

<details>
<summary><strong>Invariant for catches, status and safety</strong></summary>

- A sequence of actions shall have at most a distinct payload screen shot;
  Without action observations may have a gallery, according to existing policy.
- Screenshot policy, flow mode and action details are separate axes of the profile.
- `ExtentReportUiEvidence` can insert catches PNG already produced in an artifact;
  His name doesn't turn him into a Playwright provider.
- The verdict is kept from the input typed. Trunching text does not produce a
  new comparison and does not convert the expected / current equal visual into PASS.
- [Redactorul JSON](../security/JsonEvidenceRedactor.java) interprets key
  Semantics before value; also mask / array / object, keep the fields duplicated and does not other than the values used by the caller at the verdict.
- [Summary of the assertions](diagnostic/ReportAssertionSummary.java) is only
  Safe diagnosis, with 240 editorial per field and ceiling; do not reassess comparisons.

</details>

## Probe

[ReportEvidenceJsonFormatterTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/evidence/markup/ReportEvidenceJsonFormatterTest.java), [ReportEvidenceTableTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/evidence/ReportEvidenceTableTest.java) and [ReportEvidenceStyleContractTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/evidence/ReportEvidenceStyleContractTest.java) check the projection; renderer and browser controls are in [Reporting Tests](../../../../../../../atf-reporting-tests/README.md#tabele-publicate-și-inventar-independent).

[SensitiveDataRedactor](../security/SensitiveDataRedactor.java) and [ReportingSettings](../configuration/ReportingSettings.java) are also read for a secret / budget change; for geometry, the continuous context of [runtime and assets](../runtime/README.md).


<a id="action--observe-complete-values"></a>
<a id="valorile-complete-actionobserve"></a>
## ACTION / OBSERVE complete values

| Owner | Contract |
|---|---|
| [UiEvidenceValueMarkup](ui/UiEvidenceValueMarkup.java) | Only one value; preview 240 and complete dislocation in the same cell |
| [ACTION](ui/ExtentUiActionRenderer.java) and [OBSERVE](ui/ExtentUiObservationRenderer.java) | use the same projection in Extent and fallback, with ordinary and captured status |

SUMMARY_ONLY remains explicit; body display limit and assertions values have separate contracts.

<a id="a-single-owner-for-the-row-ui"></a>
<a id="un-singur-owner-pentru-rândul-ui"></a>
## A single owner for the row UI

[UiEvidenceRowMarkup](ui/UiEvidenceRowMarkup.java) designs operation, ordinary, target and captured values for ACTION and OBSERVE, with the same redaction policy. [UiEvidenceValueMarkup](ui/UiEvidenceValueMarkup.java) keeps the 240 preview and the full text, including the beginning and end of whitespace in DOM.

- ACTION retains the TIME column; OBSERVE retains the time-free structure.
- Fallback unlocks the original records; the title PNG uses renderer policy.
- The verdict and comparisons remain captured independently of the wording.
