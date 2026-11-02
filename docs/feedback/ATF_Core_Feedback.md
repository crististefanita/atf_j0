# Core — active audit findings

## Delivered baseline

Literal masks, quoted authorization, complete JSON and C2 distribution retain
their delivered contracts in [Core Consumer](../ATF_Core_Consumer.md#document-json-complet-și-evidence-fidel).
The producer audit below found different boundaries; it does not reopen those
fixed implementations or rewrite the [verified baseline](../ATF_Test_Changes_Baseline.md).

## CORE-CONSOLE-REDACTION-124 — sanitize every emitted console channel

**Priority: 92/100** = impact 40 + gap 26 + actionable 18 + documentation 8.
**Owner:** ApiExecutor console projection and Core security policy; concrete
Console formatting remains in Console, without a second divergent redactor.

| Controlled input / channel | Current observed result |
|---|---|
| GET /token/ followed by a generated synthetic canary | START/COMPLETE event name retains the canary |
| URL detail for that same request | known path credential is masked |
| valid JSON with an authorization string inside a public message field | Core CODE event retains the canary; Reporting JSON projection masks it |

The probe uses one completed mock exchange and captures neutral console
events. It prints only booleans, never the canary; no network or real secret
is used. ConsoleFormatter formats these supplied events without another
redaction pass, so the safe URL detail does not protect the lifecycle name.

- [ ] Redact the complete request display name before START/COMPLETE emission.
  Preserve the actual request path, full comparison values and transport input.
- [ ] Specify the policy for known credentials embedded in otherwise public
  JSON string values. Core currently documents structural field selection;
  this is a channel-policy gap, not a failure of its literal-mask fix or parser.
- [ ] Cover lifecycle names, request/response CODE, known path/query/auth syntax,
  public-string credentials, disabled policy and literal masks. Preserve JSON
  token fidelity, duplicate fields, numeric precision and nonsensitive neighbors.
- [ ] Keep raw canaries out of stdout, assertion failure text and test receipts.
  Use independent expected projections and boolean absence checks.

**Closure:** every configured emitted console channel follows the declared
policy, while original execution input and verdict remain unchanged.
Proof: core-console-probe.log in TMP j0-deep-audit-1057ca2f02ed.

## CORE-TRANSPORT-CANCEL-125 — retain ownership of the upstream HTTP future

**Priority: 88/100** = impact 35 + gap 25 + actionable 20 + documentation 8.
**Owner:** HttpClientTransport and its native adapter tests. J2 owns the later
real HTTP convergence acceptance, not a copy of this Core implementation.

HttpClientTransport returns sendAsync(...).thenApply(...). Cancelling that
mapped future succeeds, but an instrumented HttpClient's original future
receives zero cancel calls and remains pending. This is a deterministic
adapter test with no network, not proof that a real server rolled back a write.

- [ ] Keep an explicit best-effort cancellation connection to the acquired
  upstream operation, including completion/cancel races and exactly one send.
- [ ] Test the adapter as well as ApiExecutor's acquired-future cancellation.
  The existing executor timeout/cancellation tests remain valid and closed.
- [ ] Prove late success/failure, cancel before/after completion and owned
  cleanup with a controlled client; add a bounded local HTTP probe where needed.
- [ ] Preserve TIMED_OUT/CANCELLED snapshots, response availability and primary
  diagnostics. Cancelling a future never establishes server-side rollback;
  an uncertain mutation must not be replayed automatically.

Proof: http-cancellation-probe.log in the same owned TMP root. The original
mapped-future result is retained; no timeout or retry policy was increased.

## Consumer correlation and safe reuse

- J2 LiveHttpClient consumes this adapter, so upstream cancellation needs Core
  acceptance followed by a separate J2 real-read probe. EVT-CONVERGENCE-CONTRACT-86
  remains open for that consumer evidence.
- J2's default LIVE executor uses a no-op console sink. Do not claim this review
  observed credential emission from that default composition. Its direct report-
  path Console bypass is a different consumer criterion, EVT-CONSOLE-PATH-79.
- J3's device discontinuity, ASR quality and WPR stop are different production
  boundaries. A Core HTTP fix cannot close those audio criteria.
- ThrowableDiagnostics is a bounded text formatter, not a redactor or an
  aggregate diagnostic-capture budget. Its 210 default and the 240 assertion
  preview are separate contracts; neither limits retained SQL exceptions.

No library version or binary delivery is changed by this feedback-only audit.
The permanent new Core regressions belong in Core; cross-projection security
vectors belong in Reporting Tests, without a Reporting runtime dependency on Core.
