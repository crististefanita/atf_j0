# ATF J0 — registrul lacunelor active

## Pe scurt

J0 deține implementările Core/Reporting, testele native Core și regresiile generice Reporting; J1 deține scenariile AutoEx/Shop. Contractele livrate sunt la [ownerii documentați](../ATF_Context_Atlas.md), iar aici rămân numai lacunele și traseele de închidere.

[Workspace-urile](../ATF_Workspace_Synchronization.md) separă J0 sursă, J1 consumer web și J2 WIP pentru Kafka/mesagerie cu Setup în Podman. Un rezultat sau o prioritate nu se transferă automat între ele.

## Top feedback-uri active

Scorurile sunt priorități, nu note de calitate sau procente de acoperire. Formula este impact /40 + lacună /30 + lucrabil /20 + datorie documentară /10; definiția și dovada rămân la owner.

| Ordine | Owner și criteriu | Prioritate /100 | Restanță concretă |
|---:|---|---:|---|
| 1 | [Core console projection](ATF_Core_Feedback.md#core-console-redaction-124--sanitize-every-emitted-console-channel) | 92 | known credential remains in lifecycle names; explicit JSON-string policy |
| 2 | [Reporting Tests](ATF_Reporting_Tests_Feedback.md) | 89 | permanent source/JAR oracles and negative fixtures |
| 3 | [Core transport cancellation](ATF_Core_Feedback.md#core-transport-cancel-125--retain-ownership-of-the-upstream-http-future) | 88 | mapped future cancellation does not reach upstream operation |
| 4 | [SQL reporting](ATF_Reporting_Feedback.md#report-sql-steps-116--query-uri-și-rezultate-auditabile) | 88 | neutral facts, configurable SQL budgets, J2 adoption after release |
| 5 | [Android handoff](ATF_Android_UI_Reporting_Feedback.md) | 88 | preserve the separate J4 adoption scope |
| 6 | [Kit Unicode preview](ATF_Common_Test_Support_Feedback.md#kit-unicode-preview-127--preserve-valid-text-at-the-configured-boundary) | 84 | preserve surrogate pairs within the configured 240 limit |
| 7 | [Public API contract](ATF_Reporting_Feedback.md#report-public-api-43--suprafață-susținută-și-ownership) | 81 | supported surface, explicit capabilities and legacy compatibility |
| 8 | [Documentation](ATF_Documentation_Feedback.md) | 81 | current deployment facts and retained historical scope |
| 9 | [Common test support](ATF_Common_Test_Support_Feedback.md#kit-acceptance-diagnostics-123--reuse-scope-preserve-causes) | 78 | scope/diagnostic reuse without production dependencies on Kit |

Masca literală, MIME, probele deadline-ului, corelarea comenzii, factura și lifecycle-urile deja livrate nu sunt relistate ca defecte. Rămân deschise numai negativele distincte descrise în feedback.

## Contracte și controale verificate

[Contractul Core C2](../ATF_Core_Consumer.md) fixează sursa și nativele în J0,
consumul prin JAR în J1/J2/J3 și acceptanțele distincte. Criteriile rezolvate ale
distribuției și probele deterministe de timeout sunt în documentația ownerului
și în baseline; nu rămân defecte active în feedback.

[Atlasul](../ATF_Context_Atlas.md), [arhitectura](../ATF_Architecture.md) și [ghidul de testare](../ATF_Testing_Guide.md) dețin contractele stabile și probele de pornire. Controalele tehnice sunt în [baseline](../ATF_Test_Changes_Baseline.md#controale-tehnice-verificate), cu bazele reale ale proiectului.

[Disclosure-ul local și guvernanța Reporting J10](../ATF_Reporting_J10_Verification.md) sunt contracte verificate; criteriile 114 și 100 sunt eliminate din backlog.

[System și index fallback J11](../ATF_Reporting_J11_Verification.md) dețin contractele criteriilor 117 și 66; acestea nu mai sunt ToDo-uri. [Inspectorul, timing-ul și indicatorul J12](../ATF_Reporting_J12_Verification.md) au contracte și regresii permanente; handoff-ul Android rămâne distinct.

## Design AI — evaluare separată

[Evaluarea AI](README_AI_Evaluation.md) separă comparatorul existent de designul agentic propus. Backlog-ul de design nu extinde incrementul de mentenanță, iar [nota editorială](../README_Atf_DocumentationBestPractices.md#rubrica-reviziei-editoriale) nu certifică readiness-ul implementării.

## Păstrare, promovare și verificare

| Situație | Acțiune și autoritate |
|---|---|
| contract deja implementat | păstrează regula la README-ul ownerului; indexul trimite la ea prin [atlas](../ATF_Context_Atlas.md) |
| lacună reală | trigger, observație, owner, prioritate și criteriu de închidere în feedback-ul domeniului |
| rezultat parțial | precizează limita și restanța; un guard verde nu închide alt scenariu |
| control Git verificat | [baseline](../ATF_Test_Changes_Baseline.md); nu se rebazează ori prelungește implicit |

[Protocolul documentar](../README_Atf_DocumentationBestPractices.md#păstrarea-și-ștergerea-feedback-ului) și [build-ul în TMP](../ATF_Build_And_Execution_Safety.md) guvernează fiecare increment. Criteriul se elimină numai după probă și promovarea contractului stabil.

## Cross-consumer support review — 2026-11-01

[KIT-ACCEPTANCE-DIAGNOSTICS-123](ATF_Common_Test_Support_Feedback.md) records a
scoped Test Kit reuse candidate from J2/J3: property scope, retained original
failures and bounded diagnostic references. Launchers, renderer expectations
and domain oracles remain at their owners. No Core defect or library change
is inferred from this proposal; no implementation was modified.

## Producer audit — 2026-11-01

This feedback-only audit covers Core, Reporting, Reporting Tests and Test Kit:

- Implementation, thresholds, dependencies and delivered C2/J10 binaries are unchanged.
- Overall review grades: **code 90, documentation 91, quality attributes 88**.
- Grades are engineering judgements, not coverage/accuracy percentages or TODO priorities.

| Audited module | Code /100 | Owner README /100 | Quality synthesis /100 |
|---|---:|---:|---:|
| Core | 88 | 91 | 86 |
| Reporting | 89 | 90 | 87 |
| Reporting Tests | 91 | 92 | 90 |
| Test Kit | 92 | 91 | 90 |

Other J0 modules are not freshly rescored by this focused review. Core's new
console/cancellation cases and Kit's Unicode case explain the lower scores
relative to the earlier consumer-only assessment.

<details>
<summary><strong>README and quality evidence</strong></summary>

| Reviewed README / entry | Score /100 | Reason and remaining improvement |
|---|---:|---|
| root ATF README | 91 | clear ownership; current deployment facts still need selective reconciliation |
| Core security atlas | 92 | precise strategy boundaries; console channel policy needs closure |
| Reporting portable entry | 92 | module-local route; full portable normative/release closure remains open |
| Reporting production atlas | 94 | compact entry/source/proof map; maintain it with API changes |
| Reporting API package | 93 | legacy and typed boundaries stated; explicit optional capabilities pending |
| Reporting HTML package | 93 | direct flush/thread limitations honestly documented |

| Quality axis | Score /100 | Remaining proof |
|---|---:|---|
| Architecture / responsibilities | 92 | keep production boundaries and independent renderer |
| Correctness / contracts | 88 | upstream cancellation, valid Unicode and inspection-result invariants |
| Security / projection | 80 | safe lifecycle names and explicit public JSON-string policy |
| Failure diagnostics | 87 | retained primary causes and failed-attempt references |
| Audit / rendering | 87 | profile, metadata and captured/publication timing closure |
| Tests / governance | 90 | add permanent negatives for the newly reproduced boundaries |
| Configuration / determinism | 93 | preserve captured settings and effective preview 240 |
| Library release readiness | 83 | supported API/vendor matrix and portable release contracts |
| Documentation | 91 | preserve progressive disclosure, fix current claims selectively |

The equally weighted axis mean rounds to 88; module synthesis scores have
their own scope. A high test-support score is not ASR or saga acceptance.

| Executed proof | Result / limit |
|---|---|
| native reactor selection | Kit 133, Core 276, Reporting 67 pass; zero failure/error/skip |
| selected Reporting Tests | six classes / 16 tests pass; not the full browser/composition suite |
| controlled counterexamples | console channel leak, upstream future not cancelled, invalid Kit UTF-16 preview and permissive public inspection records |
| source artifact/browser | Extent/fallback controlled FAIL, exact Fails backlink, PASS neighbor, Enter/Space and complete JSON last row; System remains six versus one fields |
| binary consumers | earlier J2/J3 C2/J10 evidence is correlated, not rerun or replaced by source tests |

Proof root: TMP j0-deep-audit-1057ca2f02ed. No live build output is published.

</details>

J2 keeps its SQL/Kafka capture and real application acceptance criteria.
J3 keeps ASR, device continuity and host/guest trace acceptance; its latest
host feedback is assessed at the audio owner. A shared Core/Reporting fix
requires later separate consumer acceptance and does not close those domains.
