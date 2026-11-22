<a id="ui-network--capture-identity-and-listener-cleanup"></a>
<a id="ui-network--captură-identitate-și-eliberarea-listenerelor"></a>
# UI network — capture, identity, and listener cleanup

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

The package observes browser traffic and keeps a bounded journal. It does not
send requests, assign business verdicts, or publish Extent nodes. The browser
session owns the page; the inspector owns its listeners.

<a id="entry-points-and-responsibilities"></a>
<a id="intrări-și-responsabilități"></a>
## Entry points and responsibilities

| When investigating | Read first | Boundary |
|---|---|---|
| attach, capture, and close | [UiNetworkInspector](UiNetworkInspector.java) | coordination, reservation, retention, and delivery |
| request started before the marker | [UiRequestTimeline](identity/UiRequestTimeline.java) | direct owner of the start hook; scoped identity |
| identity value | [UiRequestIdentity](identity/UiRequestIdentity.java) | ordering within one scope; distinct from receipt sequence |
| data retained in the journal | [UiNetworkObservation](UiNetworkObservation.java) | snapshot and sanitization according to configuration |
| accepted traffic and capacity | [UiNetworkCapturePolicy](UiNetworkCapturePolicy.java) | capture policy, not a business criterion |
| compaction and repeats | [UiNetworkInspector.presentationObservations](UiNetworkInspector.java) | presentation view; the raw journal also retains a later failed response |

<a id="invariants-to-preserve"></a>
<a id="invariante-de-păstrat"></a>
## Invariants to preserve

- Capture is rejected during incomplete attachment and after close, before an
  expired Response is dereferenced. Closing is logical and irreversible.
- The timeline compensates for its own failed registration, including when used
  directly. Each owner attempts all of its removals while preserving the
  primary cause and distinct cleanup failures; it never suppresses an exception
  onto itself.
- Native callbacks left behind after an unsubscribe refusal are inert. Close
  does not cancel the subscriber snapshot of a delivery that has already begun.
- Request start order is not response arrival order. The consumer decides
  business membership; the retained journal remains inspectable after close.
- Query values are removed from URLs. Before redaction, capture computes an
  opaque SHA-256 identity fingerprint; re-sanitization preserves it without
  recovering values. Legacy input already redacted without a fingerprint remains
  unknown. The reporting adapter does not publish the digest or treat the
  placeholder as a query value.

<a id="tests-to-run-before-a-change"></a>
<a id="probe-înainte-de-schimbare"></a>
## Tests to run before a change

| Contract | Starting test |
|---|---|
| acquisition and cleanup, including Error/re-entrancy | [UiNetworkLifecycleComponentTest](../../../../../test/java/atf/ui/network/lifecycle/UiNetworkLifecycleComponentTest.java) |
| scoped identity | [UiRequestIdentityComponentTest](../../../../../test/java/atf/ui/network/identity/UiRequestIdentityComponentTest.java) |
| subscriber delivery | [UiNetworkDeliveryComponentTest](../../../../../test/java/atf/ui/network/delivery/UiNetworkDeliveryComponentTest.java) |
| identity without exposed query values | [UiObservedQueryFingerprintComponentTest](../../../../../test/java/atf/ui/network/UiObservedQueryFingerprintComponentTest.java) |
| lossless raw data and configurable compaction | [UiNetworkPresentationStreamComponentTest](../../../../../test/java/atf/ui/network/UiNetworkPresentationStreamComponentTest.java) |

Retention conditions and consumer examples are in the [module contract](../../../../../../README.md).

Continue in the [UI module guide](../../../../../../../atf-ui/README.md).
