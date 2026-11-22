<a id="atf-reporting-runtime---interactions-in-html-published"></a>
<a id="atf-reporting-runtime--interacțiuni-în-html-ul-publicat"></a>
# ATF Reporting Runtime - interactions in HTML published

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

The package consists of JavaScript controllers which run in the published report. `runtime/tests` holds HTML Test tab; it is a production code and does not contain JUnit scenarios or consumer business tests.

[Atlas](../README.md) · [Assets and vocabulary](../../../../../../README.md#contracte-și-feedback)

| Behavior | Entry Java | Actual source |
|---|---|---|
| Runtime module order | [ExtentReportRuntimeModule](core/ExtentReportRuntimeModule.java) | typed registry of the owners |
| injection into the document | [ExtentReportRuntimeBehaviour](core/ExtentReportRuntimeBehaviour.java) | runtime boot and compound scripts |
| navigation and the ancestral of failure | [section](section), [failure runtime](core/ExtentReportFailureRuntime.java) | `extent-navigation-*`, `extent-failure-*` |
| Tests/Categories | [tests](tests) | `extent-scenarios`, `extent-tests-filters`, `extent-categories` |
| durations and projections | [duration runtime](core/ExtentReportDurationRuntime.java) | `duration-*` |
| DOM vocabulary and tokens | [AtfReportHtmlVocabulary](../markup/vocabulary/AtfReportHtmlVocabulary.java) | markup / runtime common selectors and attributes |

<a id="from-native-source-to-artifact"></a>
<a id="de-la-sursa-nativă-la-artifact"></a>
## From native source to artifact

- [ReportAsset](../asset/ReportAsset.java) and the manifest hold the identity,
  the format, the pathology and order of the validated inventory.
- [Loaderul](../asset/ReportAssetLoader.java) read the classification;
  [composer](../asset/ReportAssetComposer.java) solves tokens and produces the copy of publication. Resources remain in [javascript](../../../../resources/atf/reporting/javascript) and [css](../../../../resources/atf/reporting/css).
- Runtime facades and [tema](../theme/AtfReportTheme.java) maintain order
  The renderer's pipeline includes the result in autonomous HTML.

<details>
<summary><strong>What checks beyond the existence of the script</strong></summary>

- Modification of a selector requires reading of the label that produces it and a
  Java vocabulary used for interpolation.
- Fails must resolve the exact identity of the comparison and its ancestral;
  the first validation of TEST BODY is not a general substitute for semantic path.
- Publication of delayed sailing depends on the current generation. An old callback
  shall not open the remaining scenario of a previous selection.
- Compaction is applied to the published copy; native sources remain legible.
  The presence of all resources in the source directory does not prove JAR content.
- The Runtime projects existing durations and does not measure business operation again.

</details>

<a id="minimum-evidence-and-context"></a>
<a id="probe-și-context-minim"></a>
## Minimum evidence and context

[ReportAssetContractTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/asset/ReportAssetContractTest.java) and [ReportAssetCompactorTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/asset/ReportAssetCompactorTest.java) check the resources; [ExtentReportRuntimeEvidenceComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/reporting/runtime/evidence/ExtentReportRuntimeEvidenceComponentTest.java) checks the composed runtime. [PublishedFailureNavigationComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/failurecontext/PublishedFailureNavigationComponentTest.java) checks comparison visibility after the backlink, including a negative control that hides an ancestor. [PublishedFailureNavigationEdgeComponentTest](../../../../../../../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/failurecontext/PublishedFailureNavigationEdgeComponentTest.java) adds comparison-free diagnostics, fallback and route/scroll retention checks.

These samples are in [Reporting Tests](../../../../../../../atf-reporting-tests/README.md). A change of interaction requires and fresh artifact in the browser; JavaScript text inspection does not demonstrate the click, geometry or complete expansion.

