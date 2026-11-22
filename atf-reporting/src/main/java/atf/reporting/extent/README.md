<a id="atf-reporting-extent---purposes-and-publication-of-the-final-artifact"></a>
<a id="atf-reporting-extent--scope-uri-și-publicarea-artifactului-final"></a>
# ATF Reporting Extent - Purposes and publication of the final artifact

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

The package adapts the reporting ports to the Extent tree and holds the final publication. Lifecycle is separated from the post-processing HTML; none execute the consumer scenario.

[Atlas](../README.md) · [Publication](../../../../../../README.md#publicare-și-ieșire) · [Renderer fallback](../html/README.md)

| Change | Start source | Owner |
|---|---|---|
| input and thread of the session | [ExtentReportSession](session/ExtentReportSession.java) | path, configuration and completion |
| scenario, phase and hierarchy | [ExtentReportTree](session/ExtentReportTree.java), [lifecycle node](session/ExtentNodeLifecycle.java) | tree and transitions |
| request, validation, assertion-step | the searches in this package | ports over Extent nodes |
| workspace and commit | [SessionPublication](session/ExtentReportSessionPublication.java) | private staging and final destination |
| Spark transformation to autonomous HTML | [PublicationPipeline](session/ExtentReportPublicationPipeline.java) | Assets, navigation, Overview and deductions |
| 24 h class Categories | [TimestampReporter](session/ExtentReportTimestampReporter.java) | Point adaptor of the vendor template |

<a id="order-that-matters"></a>
<a id="ordinea-care-contează"></a>
## Order that matters

- `flush()` writes the vender output in staging. `close()` completes the nodes,
  Flush and publish the final result through the publication owner.
- The pipeline inlines the assets Spark, injects the theme / runtime ATF and
  complete the document before moving to the public path.
- The publication diagnostic event follows the confirmation of the final file;
  The scope holds the cleanup of the workspace.

<details>
<summary><strong>Lifecycline invariants</strong></summary>

- Extent session is limited to the thread that created it.
  demonstrate another renderer does not authorise competing access to this session.
- Starting a knot and rollback have their own evidence; validating the input and
  the acquisition of the node is not the same step.
- The repeated closure does not publish a duplicate event.
  diagnostic notification are distinct responsibilities.
- `ReportPublicationCoordinator` serializes moving to destination.
  a test schemer and does not reexecute the business operation.
- The Category Templetor comes from Extent; the Timestamp Adapter refuses the expression
  Vendor changed, instead of claiming a successful conversion.

</details>

<a id="prohibitions-and-consolidation"></a>
<a id="probe-și-consolidare"></a>
## Prohibitions and consolidation

[ReportingStandaloneVerdictComponentTest](../../../../../test/java/atf/reporting/standalone/ReportingStandaloneVerdictComponentTest.java) checks local contracts; [UiPublicationFailureComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/ui/publication/UiPublicationFailureComponentTest.java) and publication samples from [Reporting Tests](../../../../../../../atf-reporting-tests/README.md) complete the fault-paths and artifact.

Differences in flush, publication and thread against fallback are declared in [contract of the session](../../../../../../README.md#publicare-și-ieșire). The sustained API delimitation keeps [active criteria](../../../../../../../docs/feedback/ATF_Reporting_Feedback.md#report-public-api-43--suprafață-susținută-și-ownership). [assets](../asset/ReportAssetLoader.java) and [runtime](../runtime/README.md) are also read at a finalization change, as HTML includes these two boundaries.


<a id="publication-memory"></a>
<a id="memoria-publicării"></a>
## Publication memory

| Owner | Responsibility |
|---|---|
| [ExtentReportPublicationPipeline](session/ExtentReportPublicationPipeline.java) | Order the transformations, restoration and atomic replacement |
| [InlinePngPayloadStore](publication/InlinePngPayloadStore.java) | PNG private spool, stable identity in session, fixed buffers and cleanup to success / failure |

The Extent / Spark model and text remain in the heap; the reduction of PNG children does not define a universal ceiling.
