# Large report navigation — retained evidence, incremental work

## Contract

Large HTML reports retain every supplied comparison and original inline PNG.
Navigation changes when layout and timing metadata are prepared, not which
evidence is published or how a test verdict is evaluated.

| Responsibility | Invariant |
|---|---|
| assertion cells | native default preview remains 240; independent captured verdicts and complete supplied bodies remain unchanged |
| hidden tables | no eager wrapper, intrinsic measurement or resize subscription before visibility; known hidden views also skip geometry reads |
| layout changes | coalesce affected roots once per animation frame; descendants of a pending ancestor do not trigger another subtree scan |
| screenshot dialogs | unrelated appended UI does not rescan every table in the document; original PNG bytes, local captions and return context remain intact |
| timing | resolve a card's original execution marker once per aggregation pass; reset the cache at the next pass, including absent markers |
| projected views | capture the source-owner/scenario index once per projection pass and share it across Category, Fails and Overview |
| view selection | Tests, Category, Fails and Overview keep the visible pane, the sole current navigation link and the URL fragment in sync, including after native Spark rewrites the links |
| dynamic content | identical class/open writes are ignored; actual mutations are coalesced before content invalidation; real child/text mutations increment a content revision; new/reopened/resized tables receive native column allocation; removed tables retire resize observations |
| renderer scope | the table allocator is shared by Extent and fallback; duration aggregation belongs to Extent |

The report stays self-contained. This change does not move evidence to another
file, virtualize away owners, remove screenshots or substitute observations
for assertion rows.

## Permanent verification

| Owner | Proof |
|---|---|
| LargeNavigationFixture | four original owners, 600 comparison groups, 12,004 captured comparison rows and four complete synthetic PNG checkpoints |
| LargeReportNavigationBrowserComponentTest | repeated owner switches, targeted assertion/PNG paths and return; bulk expansion on the neutral fixture or at most two explicitly selected external owners; per-owner SHA-256 inventory of original cells/bodies and canonical PNG payloads/references before/after; a one-character payload mutation is rejected |
| EvidenceTableVisibilityComponentTest | identical presentation writes do not remeasure; actual cell-style changes do; known hidden views skip geometry; closed evidence remains structurally untouched; opening/reopening and direct text-node mutation receive full measured columns |
| NavigationSelectionBrowserComponentTest | Chromium checks the four view transitions and the sole accessible current link. The distribution fixture checks the four semantic targets and fragment listener on HTML from the physical JAR. |
| RecordedPublicationTimingComponentTest | execution and transport stay independent of publication; removed/replaced markers refresh at a subsequent aggregation |
| adjacent browser families | standard UI/API tables, fallback layout, disclosure state, source labels and whole-frame screenshot viewport/return contracts |

- The optional atf.navigation.input property defaults to targeted paths.
- An explicit atf.navigation.expandOwners accepts at most two distinct, zero-based owner indices; duplicates, negative/out-of-range indices and unbounded requests are rejected.
- Each selected owner is expanded once, with the same canonical evidence inventory before and after.
- This verifies publication/rendering, not a fresh business or device execution.

## Measurement and boundaries

- Browser readiness, owner interaction and publication clocks have different scopes.
- A passing screenshot contract does not certify a fresh Android execution or its business oracle.
- Very large required evidence still consumes parser, layout and image memory; a content budget is not a total HTML or browser-heap guarantee.
- Performance observations never turn a captured FAIL into PASS.

The producer owns permanent source/JAR regressions. Consumer provenance records the current binary and independent acceptance; archived Android rendering does not certify a fresh device run.

## Cold startup and readiness

| Lifecycle | Required behavior |
|---|---|
| DOMContentLoaded/load | initialize once and activate only the requested hash target; an empty hash opens Overview |
| native Spark load | reconcile the four navigation hrefs after the vendor rewrites them; do not reactivate or restart the readiness clock |
| readiness | run initial structural normalization in a coalesced task even when visual frames are deferred; publish only after success |
| page load | record the completed navigation timing independently; an unfinished load remains unknown rather than zero |
| dynamic content | retain observers, revision invalidation and scheduled normalization for later mutations |
| deadlines | readiness/action 10,000 ms; navigation 45,000 ms; existing API matrices retain 5,000 ms |

[ColdStartReadinessComponentTest](../atf-reporting-tests/src/test/java/atf/ui/tests/extent/component/navigation/ColdStartReadinessComponentTest.java)
checks five initial destinations, startup scenario deep links, native link
repair and repeated load. It verifies that readiness waits for real structural
normalization and that Tests controls work when animation frames are withheld.
An external atf.cold.input runs three fresh Chromium processes against the same complete artifact.

## Native timeline labels

| Boundary | Invariant |
|---|---|
| Spark template | encode the full timeline owner key as a JSON string; preserve quotes, line breaks, backslashes and Unicode |
| captured data | retain the exact caller label and original duration; no trimming or inferred verdict |
| vendor upgrade | fail explicitly when the pinned native key expression changes, instead of silently publishing invalid script |
| permanent proof | TimelineTitleEncodingComponentTest executes the real embedded script and rejects browser errors; timestamp regressions stay separate |

The producer owns implementation and permanent source/JAR tests. Consumers retain their independent application oracles; dated external witnesses do not become fresh acceptance.
