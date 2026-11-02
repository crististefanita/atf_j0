# Producer audit — 2026-11-01

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
