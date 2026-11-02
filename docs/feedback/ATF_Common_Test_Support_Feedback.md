# Common test support — J2/J3 audit handoff

## At a glance

This is a feedback-only proposal from the J2/J3 live review of 2026-11-01.
Core C2 and Reporting J10 remain unchanged.

The earlier consumer-only review did not reopen Core criteria. The producer
audit now has distinct [console and cancellation findings](ATF_Core_Feedback.md);
they do not belong in Kit.

## KIT-ACCEPTANCE-DIAGNOSTICS-123 — reuse scope, preserve causes

**Priority: 78/100** = impact 29 + gap 21 + actionable 20 + documentation 8.
**Owner:** neutral test support in Test Kit at J0; launchers and domain oracles
remain with the consumers. This is not a new production process framework.

| Observed caller | Gap | Boundary to preserve |
|---|---|---|
| J2 SqlReportAcceptance | unexpected JUnit result is printed as an opaque DefaultFailure object; original SQL timeout is visible only in the HTML | SQL selection, expected counts, driver and SQL oracle stay in SQL test scope |
| J2 EventuateReportAcceptance | similar property save/restore and outcome checks are handwritten; this caller already prints original failures | renderer and Eventuate fixture/oracle stay with Eventuate |
| J3 SpeechAssertionRequirementTest | repeats directory/renderer overrides, JUnit launch and restoration | speech category and minimum-assertion oracle stay with Speech |
| J3 SpeechDiagnosticScriptsTest | failed temporary native logs disappear; timeout and child failures lack a retained phase bundle | PowerShell collector/host policy and audio semantics stay in J3 |

Test Kit already owns ScopedAcceptanceRun and FailureAwareCleanup. Prefer
adoption and a small demonstrated extension over parallel scope/cleanup APIs.
The current helper explicitly needs no launcher dependency; preserve that
boundary unless a separately reviewed contract justifies changing it.

- [ ] Define a neutral invocation/diagnostic result carrying original failure,
  container/test counts, explicit artifact selection and bounded diagnostic
  references. Reuse existing scope mechanics, with exact restoration for all
  overridden properties even if launch, publication or collection fails.
- [ ] Prove two concrete callers before extraction. Include a case with zero
  discovered tests, container failure, an expected controlled assertion, an
  unexpected exception, a failed artifact and a failed diagnostic sink.
- [ ] A real primary exception stays primary; secondary capture/cleanup failures
  remain available. A useful diagnostic cannot depend only on a deleted path
  or an object's default toString identity.
- [ ] Keep JUnit launching at its current test adapter and pass neutral callbacks
  where possible. No renderer, Kafka client, JDBC driver or audio engine enters
  Kit merely to share a loop. No universal mutable context or new module.

**Closure:** both consumer adapters use the same small neutral mechanism and
retain their independent expectations, exact JUnit outcomes and report oracles.
Individual/suite and Extent/fallback checks remain consumer acceptance gates.

## Consolidation limits

The J2/J3 Mock Java sources are byte-identical. Main Test Kit/Console sources
have no textual semantic differences after line-ending normalization; a
byte-different file is not automatically a refactor target. Keep guarded,
selective reconciliation and consumer-specific POMs rather than copying an
entire deployment or creating another source owner.

BoundedProcess is production code currently reused inside Speech. Moving it
to Test Kit would reverse the runtime/test dependency boundary. JDBC deadlines,
Kafka polling, native capture stop and host ETW control have distinct ownership
and cleanup guarantees; share proven mechanics only after contract comparison.

- [Current J2 audit](../../../atf_j2/docs/feedback/README.md#live-audit--2026-11-01) separates infrastructure from saga acceptance.
- [Current J3 audit](../../../atf_j3/docs/feedback/README.md#live-review--2026-11-01-feedback-only-checkpoint) separates fixture, process and physical audio evidence.

## KIT-UNICODE-PREVIEW-127 — preserve valid text at the configured boundary

**Priority: 84/100** = impact 31 + gap 24 + actionable 20 + documentation 9.
**Owner:** AssertionValueDisplay and its native Kit tests at J0; consumers
retain their configured limit and sanitizer. Rendering regression stays in J0.

A 238-character ASCII prefix followed by a supplementary Unicode character
crosses the 240-unit budget. Kit returns length 240 with an unpaired high
surrogate; Reporting's direct projection returns valid text of length 239.
The same configured limit is respected in both cases; the difference is fidelity.

- [ ] Keep the complete sanitizer-before-preview order and avoid splitting a
  surrogate pair. Explicitly document UTF-16 budget units versus code points;
  do not reinterpret or enlarge the existing 240 default.
- [ ] Cover both boundary positions, limit one, supplementary characters,
  literal replacement masks and sanitizer failure without a second callback.
- [ ] Preserve the original JUnit comparison and exception. Prove that a
  suffix mismatch still fails and the standard collapsible table remains intact.
- [ ] Characterize native Kit first, then composition in Reporting Tests and
  adoption in J2/J3. A renderer cannot reliably repair text already split upstream.

Proof: unicode-preview-probe.log under TMP j0-deep-audit-1057ca2f02ed.
No source changes or limit changes are part of this audit.

## Reuse decision matrix — producer review

| Responsibility | Owner / reuse decision | Required boundary |
|---|---|---|
| JUnit assertion capture and display | Kit: RecordedAssertions, AssertionValueDisplay, ComponentAuditExtension | no renderer, business oracle or second comparison |
| test acquisition/cleanup/property scope | Kit: FailureAwareCleanup and ScopedAcceptanceRun first | original failure identity and exact property restoration |
| production Core lifecycle cleanup | Core: CleanupRegistry | RuntimeException contract is different from Kit's Exception/Error aggregation |
| renderer publication cleanup | small local Reporting protocol | standalone Reporting cannot depend on Core or Kit for a short helper |
| SQL capture/failure budget | J2 atf-sql | JDK-only production contract; no new Core/Kit dependency merely to shorten a message |
| ASR, native process, PCM continuity, checkpoint | J3 Speech | no production dependency on test support |
| host collector / PowerShell diagnostics | J3 tools plus its controlled test adapter | no ETW/device policy in Kit |
| SQL/audio semantic report model | Reporting after neutral facts are specified | no JDBC/Kafka/ASR callbacks in the renderer |

### Scope the acceptance helper extension

- ScopedAcceptanceRun currently scopes one property and collects artifacts only
  after normal callback return. It does not promise a retained failed-attempt
  bundle or a complete multi-property transaction. Preserve that existing API.
- Extend only for two demonstrated callers. Keep outcome/diagnostic references
  neutral; JUnit discovery/count policy, expected controlled failures and domain
  artifact oracles remain in each test adapter.
- A failed attempt may retain artifact references without becoming a successful
  Result. Capture/cleanup failure remains secondary to the original exception.
- Production Core, standalone Reporting and JDK-only SQL cannot import Kit to
  share a timeout or a few cleanup lines. Similar syntax does not imply the
  same lifecycle, cancellation or publication guarantee.
