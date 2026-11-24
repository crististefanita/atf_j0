# Documentation feedback — source-owned contracts

## At a glance

J0 owns documentation for its source libraries and Reporting regressions. [Shared consumer criteria](https://github.com/crististefanita/atf_j1/blob/HEAD/docs/feedback/ATF_Documentation_Feedback.md) remain with J1; transferring a finding does not prove implementation.

<a id="doc-reporting-prose--remaining-detailed-section-review"></a>
<a id="doc-reporting-prose--revizia-rămasă-a-secțiunilor-detaliate"></a>
<a id="joint-documentation---owner-j1"></a>
<a id="documentație-comună--owner-j1"></a>
<a id="doc-context-deployment-111--prioritate-81"></a>
## Ownership and retained contracts

| Boundary | Owner and authority |
|---|---|
| source-library prose and examples | module README, code and native tests in J0 |
| house style and edition parity | [documentation standard](../README_Atf_DocumentationBestPractices.md#technical-prose-review), documentation governance and the commit-pinned edition comparator |
| consumer scenarios and shared nonlibrary criteria | J1; independent acceptance remains with each consumer |

## DOC-GUIDE-PROSE — remaining maintenance terminology

**Status: Open.** Review launcher/scope terminology in [active Test Kit feedback](ATF_Common_Test_Support_Feedback.md) and consumer/synchronization wording in the [workspace protocol](../ATF_Workspace_Synchronization.md) against Romanian peers and live owners.

- Preserve unresolved criteria, executable commands and original test-change bases; remove closed-result narration rather than translating it.
- Close after the selected passages read naturally, preserve the same contract in both editions and pass documentation governance.

<a id="source-language-english-2026-11-24"></a>
## SOURCE-LANGUAGE-EN-2026-11-24 — English source and comments

**Status: Verified and closed for J0 and J1.** J0 owns the policy and reusable Test Kit scanner; J1 consumes the selected binary and enforces it over its maintained Java/MJS inventory. Source and tests remain identical between `main` and `main_RO`; only governed documentation prose changes language.

- Developer-authored identifiers, comments/docstrings, test names, and diagnostics use English. Localized UI text, intentional localized fixtures/payloads, protocol/API names, chess notation, and generated source remain data or external contracts.
- J0 owns the canonical implementation; consumers own source-root discovery, localization exceptions, and suite enrollment. J1's gate exercises Java and MJS without carrying a scanner copy.
- Negative/positive fixtures, path/line/construct diagnostics, documentation language governance, and complete-suite enrollment passed in staged builds.

Remaining adoption work is tracked separately below; it does not invalidate the verified J0/J1 increment.

<a id="source-language-adoption-2026-11-24"></a>
## SOURCE-LANGUAGE-ADOPTION-2026-11-24 — remaining ATF consumers

**Status: Open.** J0/J1 establish the shared Java/MJS rule. Other ATF owners need a verified local gate before the rule can be called universal.

- Inventory maintained languages and source roots for each applicable module; exclude generated output explicitly.
- Adopt the Test Kit scanner where syntax is supported. For other languages, extend it with fixtures or define an equivalent parser-aware owner gate; do not apply a Java/MJS lexical check to unsupported syntax.
- Preserve the same EN/RO documentation meaning and byte-identical source between language branches.
- Add the gate to each module's normal governance suite with negative source examples and positive localized-data examples.

Close per consumer only after its source inventory, diagnostics, suite enrollment, and documentation governance pass.
