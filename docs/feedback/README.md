<a id="atf-j0---register-of-active-loopholes"></a>
<a id="atf-j0--registrul-lacunelor-active"></a>
<a id="atf-j0--active-gap-registry"></a>
# ATF J0 — active gap registry

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

The J0 registry tracks Core, Reporting, Console, Mock and Test Kit gaps, including generic Reporting regressions. The [atlas](../ATF_Context_Atlas.md) routes delivered contracts to their owners; the [J1 registry](../../../atf_j1/docs/feedback/README.md) owns shared requirements outside the libraries and AutoEx/Shop scenarios.

[Workspaces](../ATF_Workspace_Synchronization.md) separates J0 source, J1 web consumer and J2 WIP for Kafka / Setup messaging in Podman. A result or priority shall not be transferred automatically to each other.

<a id="common-libraries"></a>
<a id="biblioteci-comune"></a>
## Common libraries

J0 owns Core, Reporting, Console, Mock and Test Kit implementation, public contracts, native tests, packaging and feedback, including findings from ATF consumers. The [maintenance guide](../../lib/README.md) defines one requirement per defect and independent consumer acceptance.

| Library / evidence | Canonical backlog |
|---|---|
| Core | [Core](ATF_Core_Feedback.md) |
| Reporting | [Reporting](ATF_Reporting_Feedback.md) |
| Reporting generic regressions | [Reporting Tests](ATF_Reporting_Tests_Feedback.md) |
| Console and Mock | [Console / Mock](ATF_Console_Mock_Feedback.md) |
| Test Kit | [Test Kit](ATF_Common_Test_Support_Feedback.md) |

<a id="maintenance-governance"></a>
<a id="guvernanța-mentenanței"></a>
## Maintenance governance

[Workspace feedback](ATF_Maintenance_Feedback.md) keeps the ownership of the whole- reactor gates and J0 / J1. The combined point entry, readonly checks and closing criteria are executable and related to process documentation.

Source-library editorial criteria belong to [documentation feedback](ATF_Documentation_Feedback.md); shared consumer criteria remain with J1.

<a id="active-maintenance-priorities"></a>
<a id="priorități-de-mentenanță-activă"></a>
## Active maintenance priorities

Scores express priority, rather than quality or coverage: impact/40 + gap/30 + workability/20 + documentation debt/10. The criterion and its evidence remain with the owner.

| Order | Owner and criterion | Priority / 100 | Actual retention |
|---:|---|---:|---|
| 1 | [Full UI values](ATF_Android_UI_Reporting_Feedback.md#report-ui-assertion-full-evidence-133--valori-lungi-capturate-înainte-de-preview) | 88 | capture before the caller’s preview and safe disclosure; ACTION / OBSERVE is already delivered |
| 2 | [Supported public API](ATF_Reporting_Feedback.md#report-public-api-43--suprafață-susținută-și-ownership) | 81 | delimitation of the surface, adaptor capabilities and compatibility |
| 3 | [Reporting test oracle](ATF_Reporting_Tests_Feedback.md#report-test-audit-60--o-dovadă-identificabilă-pentru-fiecare-scenariu) | 80 | exact identification of the evidence, selection of the owner in the external sample and remaining negative matrix |
| 4 | [release contract](ATF_Reporting_Feedback.md#report-release-contract-45--ce-conține-și-ce-promite-pachetul) | 73 | declared toolchain automatic gate and external distribution metadata |
| 5 | [UI Observation Profile](ATF_Reporting_Feedback.md#report-observation-contract--domeniul-validatorului-de-artefact) | 72 | PNG intent and its association to the action / owner declared |

The extension of SQL requested, [REPORT-SQL-STEPS-116](ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile), remains separate from the maintenance note of the delivered behaviour.

[Redaction of assertions labels](Whist_Assertion_Redaction_Feedback.md) follows the point case in which the business text with `basic` loses a word. The closing probe must also preserve masking of real Basic credentials.

Literal redaction, MIME, correlation and lifecycle contracts retain their evidence. Core separately verifies the cancellation of HttpClient and URLConnection; the acceptance of each consumer remains distinct.

Do not enter functionality just to achieve a note; prioritise the proven correctness, auditability and ownership.

<a id="checked-contracts-and-controls"></a>
<a id="contracte-și-controale-verificate"></a>
## Checked contracts and controls

| Boundary | Delivered contract and retained proof |
|---|---|
| Consumer guides | [Gate JAR/POM](../ATF_Platform_Libraries.md) |
| Core | [Consumer contract](../ATF_Core_Consumer.md): source / native ownership, body / deadline cancellation and independent acceptance on JAR |
| Disclosure and governance | [Display contract](../ATF_Reporting_Display_Contract.md): criteria 114/100 are delivered |
| Recovery in Maven reports | [Contract delivered](ATF_Reporting_Feedback.md#ui-recovery-audit-01--încercări-inspectabile-în-extent); IntelliJ verification remains delayed at the consumer |
| Extent navigation and starting the verification browser | [Navigation contract](../ATF_Large_Report_Navigation.md) and [sample set](../../atf-reporting-tests/README.md#setup-browser-și-verdict-terminal) |
| System, Inspector and Timing | [Display contract](../ATF_Reporting_Display_Contract.md) and [Reporting contract](../../atf-reporting/README.md): criteria 117/66/62/83/124 are delivered |
| Publication memory and ACTION / OBSERVE integral | [Reporting contract](../../atf-reporting/README.md) criteria 131/132 have been removed from active priorities; source sample / JAR remains |
| Documentation and controls | [Atlas](../ATF_Context_Atlas.md), [architecture](../ATF_Architecture.md), [test](../ATF_Testing_Guide.md) and [baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate) keep real Git contracts and bases |

<a id="requirements-transferred-to-j1"></a>
<a id="cerințe-transferate-în-j1"></a>
## Requirements transferred to J1

[J1 Registry](../../../atf_j1/docs/feedback/README.md) owns shared governance, AI design and consumer coordination. J0 owns Test Kit, consumed as a JAR in J1–J7 without parallel source. ID- identifiers and evidence are retained; moving the record does not complete the implementation.

<a id="preservation-promotion-and-verification"></a>
<a id="păstrare-promovare-și-verificare"></a>
## Preservation, promotion and verification

| Situation | Action and authority |
|---|---|
| contract already implemented | keep the rule at README; index sends to it via [atlas](../ATF_Context_Atlas.md) |
| actual gap | trigger, observation, owner, priority and closing criterion in domain feedback |
| partial result | specify limit and delay; a green guard does not close any other scenario |
| verified Git control | [baseline](../ATF_Test_Changes_Baseline.md); no implicit rebasing or extension |

The [documentation protocol](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului) and [TMP build process](../ATF_Build_And_Execution_Safety.md) govern every increment. Remove a criterion only after its probe passes and its durable contract is promoted.
