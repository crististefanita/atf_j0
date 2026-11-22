<a id="playwright--operations-deadlines-and-browser-session"></a>
<a id="playwright--operații-deadline-și-sesiune-de-browser"></a>
# Playwright — Operations, Deadlines, and Browser Session

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

This package is the generic browser adapter. It receives explicit configuration
and a sink, records operations, and exposes observations; it does not decide a
checkout outcome or own the renderer or JUnit runner.

<a id="inputs-and-responsibilities"></a>
<a id="intrări-și-responsabilități"></a>
## Inputs and responsibilities

| When investigating | Read first | Boundary |
|---|---|---|
| acquisition and release | [PlaywrightUiSession](PlaywrightUiSession.java) | owns Playwright/browser/context/page and the inspector |
| scale and iframe coordinates | [UiViewportScale](../config/geometry/UiViewportScale.java) → [PlaywrightUiSession](PlaywrightUiSession.java) | requested output, CSS viewport, and native DPR; no injected CSS |
| action vocabulary | [PlaywrightActionSequence](PlaywrightActionSequence.java) | public facade, not the implementation of every operation |
| download and observation cleanup | [PlaywrightBrowserActions](PlaywrightBrowserActions.java) | listener armed before click, shared deadline, and real event |
| readiness and budget | [PlaywrightBrowserActions](PlaywrightBrowserActions.java) | browser waits and observation within budget |
| explicit fixture preparation | [PlaywrightPagePreparation](PlaywrightPagePreparation.java) | load, current root attachment, and native PNG share the existing navigation budget; target actionability remains native |
| locator and interaction | [PlaywrightLocatorActions](PlaywrightLocatorActions.java) | locator operations |
| select replaced while waiting | [PlaywrightSelectAction](selection/PlaywrightSelectAction.java) | read-only observation before the single native call, with no replay |
| passive backend observation and unsubscribe | [UiBackendRecovery](recovery/UiBackendRecovery.java) and [UiRecoveryObserver](recovery/UiRecoveryObserver.java) | scoped metadata; no dispatch, replay, or captured body |
| partial attachment and release failures | [UiListenerScope](lifecycle/UiListenerScope.java) | active only after full attachment; attempts every release and retains distinct causes |
| cause, timing, and diagnostics | [UiActionRecorder](UiActionRecorder.java) | recording and operation retry policy |

<a id="invariants-to-preserve"></a>
<a id="invariante-de-păstrat"></a>
## Invariants to preserve

- Readiness receives the remaining budget. A new read does not automatically
  get another full timeout; the business expected value is not a synchronization predicate.
- Do not equate a successful click with completion of its effect. Download,
  navigation, and HTTP response are distinct observations that need their own context and time.
- Check the retry policy before changing an operation; do not add a local retry
  to a mutation to hide a missing postcondition.
- Select re-observes the strict locator and option without activating the old
  control. The native event is allowed once, within the remaining deadline; an
  empty result or exception after dispatch remains FAIL and does not authorize another selection.
- Diagnostics for a sensitive action remain safe. HTML projection and preview
  belong to the Reporting library, not this adapter.

Preparation for the [geometry fixture gate](../../../../../../README.md#geometry-fixture-gate)
belongs to the test fixture: initialized documents, stable layout, and capture
within the navigation budget, followed by a pointer at 850 ms. Runtime gets no implicit warm-up.

<a id="proofs-and-neighbors"></a>
<a id="probe-și-vecini"></a>
## Proofs and neighbors

| Contract | Starting proof |
|---|---|
| action vocabulary and recording | [PlaywrightActionSequenceTest](../../../../../test/java/atf/ui/playwright/PlaywrightActionSequenceTest.java) |
| first-frame preparation and its independent deadline | [clock contracts](../../../../../test/java/atf/ui/playwright/PlaywrightPagePreparationTest.java) / [native pixels and refusals](../../../../../test/java/atf/ui/playwright/PlaywrightPagePreparationComponentTest.java) |
| trusted pointer, focus, capture, and refusal at 70% | [PlaywrightPointerGeometryComponentTest](../../../../../test/java/atf/ui/playwright/geometry/PlaywrightPointerGeometryComponentTest.java) |
| download, event, and cleanup | [PlaywrightDownloadComponentTest](../../../../../test/java/atf/ui/playwright/download/PlaywrightDownloadComponentTest.java) |
| select continuity and replay refusal | [PlaywrightSelectContinuityComponentTest](../../../../../test/java/atf/ui/playwright/selection/PlaywrightSelectContinuityComponentTest.java) |
| cause, rollback, and close-once | [UiListenerScopeComponentTest](../../../../../test/java/atf/ui/playwright/lifecycle/UiListenerScopeComponentTest.java) and [UiRecoveryObserverLifecycleComponentTest](../../../../../test/java/atf/ui/playwright/recovery/UiRecoveryObserverLifecycleComponentTest.java) |

For ownership and screenshots, continue in the [UI README](../../../../../../README.md);
for business operations, continue in the consumer's page/flow. A local fixture
does not certify readiness of the entire external application.

The callback checks `active()` before accessing any request/response/frame.
The scope contract is in the [UI README](../../../../../../README.md#observation-listener-scope);
the network inspector keeps its ownership and release order separate.

Continue with the [UI module guide](../../../../../../../atf-ui/README.md).
