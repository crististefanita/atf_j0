<a id="atf-best-practices--index-and-governance"></a>
<a id="bune-practici-atf--index-și-guvernanță"></a>
# ATF Best Practices — index and governance

This is the entry point for cross-cutting ATF rules. It does not duplicate coding or editorial rules; it points to the document that owns each contract. Stable contracts are protected by their tests and guards, while feedback remains the dynamic register of gaps.

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

The rules have three owners: code and architecture, documentation, and dynamic feedback. Each stable rule has a single owner and verifiable evidence.

<a id="choosing-a-document"></a>
<a id="cum-alegi-documentul"></a>
## Choosing a document

| Question | Owning document | Contents |
|---|---|---|
| How should I design modules, classes, and tests? | [README_Atf_CodeBestPractices.md](README_Atf_CodeBestPractices.md) | OOP, boundaries, models, constants, CSS/JavaScript, test layers |
| How should I choose a layer and prove a change? | [ATF_Testing_Guide.md](ATF_Testing_Guide.md) | module × layer matrix, UI/API proof, and test selection |
| How should I write and maintain documentation? | [README_Atf_DocumentationBestPractices.md](README_Atf_DocumentationBestPractices.md) | structure, diagrams, tables, naming, feedback, and links |
| Which gaps are still active? | [feedback/README.md](feedback/README.md) | ToDos, owners, and closure criteria |
| What is the module architecture? | [ATF_Architecture.md](ATF_Architecture.md) | modules, dependencies, and boundary direction |
| How are builds and synchronization verified? | [ATF_Build_And_Execution_Safety.md](ATF_Build_And_Execution_Safety.md) | staging, builds, browser gate, and live synchronization |

<a id="shared-rule-stable-contract-dynamic-feedback"></a>
<a id="regula-comună-contract-stabil-feedback-dinamic"></a>
## Shared rule: stable contract, dynamic feedback

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":40,"padding":10,"diagramPadding":16}}}%%
flowchart LR
    Live[Code + POM + live tests] -->|establishes| Contract[Stable contract]
    Contract -->|feeds| Gate[Test or guard]
    Gate -->|documents| Owner[Owner README]
    Gate -. reproducible gap .-> Feedback[Active feedback]
    Feedback -. new evidence .-> Live
```

<details>
<summary><strong>Legend — what stays stable and what gets updated</strong></summary>

- live code and tests are the source of truth;
- the owner's README describes stable behavior;
- the test or guard protects the claim;
- feedback records only what remains to be decided or implemented and can be
  updated, narrowed, or closed dynamically;
- existing normative READMEs and gate tests are not changed incidentally;
  changing them requires a requirement, approval, reason, and new evidence;
- run results do not become permanent documentation.

</details>

<a id="quick-checklist"></a>
<a id="checklist-rapid"></a>
## Quick checklist

- [ ] I identified the owner before changing code or documentation.
- [ ] The rule appears in exactly one normative document.
- [ ] A test, guard, or verifiable acceptance criterion exists.
- [ ] I did not change a normative README or existing gate without explicit
      scope and approval.
- [ ] I did not add an absolute path, timestamp, artifact name, or test count
      that could become stale.
- [ ] Feedback contains only the remaining gap, not a copy of a closed rule.
- [ ] Links are relative and survive checkout relocation.

For implementation, read the owner's document, then the affected module README. This index should remain short and stable.

<a id="verifying-the-rule-at-its-owner"></a>
<a id="verificarea-regulii-la-owner"></a>
## Verifying the rule at its owner

| What changes | Starting proof | Limitation |
|---|---|---|
| Code and boundary | [WholeAtfGovernanceSuiteTest](../atf-tests/src/test/java/atf/api/tests/governance/WholeAtfGovernanceSuiteTest.java) | verifies the governed rule, not every application behavior |
| README and legends | [Documentary validators](README_Atf_DocumentationBestPractices.md#verificarea-editorială-la-owner) | valid structure still requires checking against the live source |
| Protected test | [testChanges policy](../atf-tests/README.md#test-change-governance) | an editorial change does not authorize weakening the test |

Run the gate in [TMP](ATF_Build_And_Execution_Safety.md), using the command in [ATF Tests](../atf-tests/README.md#quick-verification). Do not copy the checklist into every module README; document only its concrete application there.