# Reporting J18 — cold-start readiness acceptance

## Release identity

Reporting J18 is built from J0 in owned TMP staging. Core C3 is unchanged.

| Property | Value |
|---|---|
| coordinate | atf.api:atf-reporting:1.0.0-j18 |
| JAR SHA-256 | 63655536DE4EBA02A8AF6CA765D445BBCE7D89967979FF5BF9247FDC0F79AD02 |
| POM SHA-256 | 32A67E40F3A002C35F31696515562C5570E344522CEB0D9AA3E60888D48B1FFC |
| producer base | f1bfc261e40a08cdfc3e411a2fd4ef1525970147 plus reviewed working changes |
| reproducibility | two parentless builds with identical JAR bytes; 91 native tests pass in each |
| Java compatibility | 543 class entries: 542 byte-identical to J17; only the Spark presentation adapter changes, with identical public/protected API; three JavaScript assets change |
| retained evidence | supplied comparison cells/bodies and canonical PNG payloads/references; default preview 240 |

## Native timeline labels

The additional current J4 Learning rendering exposed a native Spark SyntaxError
for a title containing pretty JSON. The pinned dashboard macro now encodes
timeline keys as JSON strings; full labels and captured duration values remain.

- The old candidate fails all three neutral title cases; the repaired source and physical JAR execute the real embedded script and reject browser errors.
- Coverage includes quoted/multiline JSON, backslashes and Unicode; the five timestamp cases retain their independent clock/zone oracles.
- Current J4 Learning contracts and actual rendering are revalidated without changing the consumer’s independent business comparisons.

## Cause and correction

A Tests URL activated Overview at startup, then activated Tests from both
DOMContentLoaded and load. The load callback restarted readiness while
repeating expensive work on a dense document.

- **Initialization:** the [navigation contract](ATF_Large_Report_Navigation.md#cold-startup-and-readiness)
requires one initialization and one activation of the requested destination.
The load callback reconciles only the four navigation links rewritten by
native Spark; dynamic-content observers remain active.

- **Truthful state:** readiness is published only after normalization succeeds. A controlled
normalization exception remains unready until a real successful recovery;
page-load timing is recorded independently from a completed navigation entry.

| Deadline | Unchanged value |
|---|---|
| readiness and individual browser action | 10,000 ms |
| navigation | 45,000 ms |
| existing API disclosure matrix | 5,000 ms |

Whole publication, parsing and multi-action inspection intervals have different
scopes. No total cold-load or universal hardware guarantee is inferred from
an individual passing readiness/action deadline.

## Mutation work after readiness

Profiling the bootstrap-only candidate reproduced a 10 s timeout despite a
truthful ready flag. Repeated identical class/open writes invalidated table
content, and known hidden source views still entered geometry measurement.

| Correction | Permanent independent oracle |
|---|---|
| ignore same-value presentation writes | 200 identical class/open writes do not increase canvas text measurements; the old candidate increased 6 to 12 |
| coalesce real invalidation targets | real text changes and the native cell-font token still update measured columns |
| skip known hidden geometry | no table getClientRects call before a hidden view is revealed; complete values measure on reveal |

## Verification record

The final Reporting source/JAR gates pass. Consumer acceptance and retained
application failures are listed separately by scope. Logs, snapshots and
captures remain in owned TMP staging
ATF_REPORT_COLD_READY_ACTIVE_DO_NOT_CLEAN_k8QoAs.

| Gate | Tests / failures / errors / skips |
|---|---|
| native parentless A and B | 91 / 0 / 0 / 0 in each build |
| final source reactor | 70 / 0 / 0 / 0 |
| final physical-JAR adjacency | 92 / 0 / 0 / 0 |
| full Android fresh processes | 1 / 0 / 0 / 0; one permanent case launches three new processes |
| full Android bounded owners plus JAR identity | 3 / 0 / 0 / 0 |
| J1 component suites / independent AutoEx UI recheck | 47 / 0 / 0 / 0; 1 / 0 / 0 / 0 |
| J2 SQL / Eventuate MOCK | 11 / 0 / 0 / 0 |
| J3 PCM / canonical preparation | 8 / 0 / 0 / 0 |
| J4 component/reporting contracts | 55 / 0 / 0 / 0 |
| J1 / J2 / J3 deployment | 32 / 0 / 0 / 0; 42 / 0 / 0 / 0; 5 / 0 / 0 / 0 |
| current Learning rendering plus JAR identity | 2 / 0 / 0 / 0 |
| retained initial J1 UI witness | Shop: 1 / 0 / 0 / 0; AutoEx: 1 / 0 / 1 / 0 search postcondition error; independent unchanged AutoEx recheck passes |
| 14 fresh consumer renderings plus JAR identity | 15 / 0 / 0 / 0 |
| final VERIFIED-control governance | 1 / 0 / 0 / 0 |
| fresh post-distribution J1 / J2 / J3 / J4 | 32 / 0 / 0 / 0; 42 / 0 / 0 / 0; 6 / 0 / 0 / 0; 5 / 0 / 0 / 0 |

- The archived Android HTML is 55697010 bytes.
- A separate original-source comparison against J17 confirms five owners, 114,596 cells/bodies and 308 PNG references/payloads unchanged.

The external artifact is republished from archived receipts with the final J18
JAR. It is not a fresh Android execution; old reports and their receipts remain
unchanged. The user permits structural expansion of these two original tests,
while all other large-report paths remain targeted.

The final inventory compares original cells, complete supplied bodies, captions,
canonical PNG bytes and references before/after navigation. A one-character
canonical PNG mutation must invalidate it even when owner/link/caption match.

| Cold run | Navigation ms | Ready wait ms | Through first owner action ms |
|---|---|---|---|
| fresh process 1 | 4871 | 4891 | 16750 |
| fresh process 2 | 4714 | 5087 | 17226 |
| fresh process 3 | 4857 | 4957 | 16527 |

Each process is a new Chromium launch; the input is the complete archived
Android publication, without warm retries or an increased deadline. These
intervals have separate scopes and are not captured Android execution timing.

## Retained unsuccessful attempts

- AutoEx did not confirm the first Blue Top product after Search in the existing action budget. The failed Extent artifact is retained; a later HTTP 200 probe and independent unchanged UI recheck are separate witnesses. The consumer owns capture/root-cause follow-up.
- J3 documentation governance rejected a direct producer-source link; consumer documentation now links its local evidence instead, without relaxing workspace boundaries.


- The first source test-control gate rejected overlapping controls for an existing navigation test. The new control owns only the new cold-start class; the existing class remains under control 139.
- The first bootstrap candidate removed native load reconciliation and broke Fails navigation. All six permanent failure-navigation cases exposed it, along with two API cases; restoring lightweight href reconciliation fixes the cause without reactivating a view.
- The bootstrap-only J18 candidate also timed out at the unchanged 10 s readiness bound; a cold profile showed a true ready flag followed by mutation/layout work blocking observation. Its hash and failed logs remain in the rejected-candidate archive; it was not delivered.
- The initial font-mutation fixture used a CSS rule overridden by the native important cell-font rule. The final test uses the native font token and asserts its computed 32 px value before requiring new measurement.
- The clean plugin was unavailable in the offline cache; final parentless A/B builds used new directories with no generated target rather than downloading dependencies or reusing output.
- Earlier J17 cold timeouts and warm reruns remain in the historical [J17 record](ATF_Reporting_J17_Verification.md). They are not used as cold-start acceptance of J18.

## Scope and publication method

- Chromium interactions and actual captures use the previously user-approved alternative to unavailable TAB MCP controls; no fresh manual TAB acceptance is claimed.
- J1 real web execution, J2 SQL/Eventuate MOCK, J3 PCM/canonical preparation and J4 report components have independent acceptance gates.
- Eventuate LIVE, acoustic ASR and a fresh Android device run are separate; archived renderer acceptance does not certify them.
- Consumer deliveries retain one current Core pair and one Reporting pair directly in lib. Source implementation and permanent Reporting tests remain in J0.


## Local review

[Complete archived Android publication](http://127.0.0.1:59987/android-j18-final.html#test-view)
and the consumer routes are served from the retained TMP artifacts. The user
permits bulk expansion only for the two selected original Android tests.

## Delivered identity

Fresh copies from all four delivered live checkouts passed their independent
post-distribution checks. Each actual Surefire classpath resolves exactly one
Reporting J18 JAR with the delivered SHA-256; J3 includes a real canonical
preparation adapter case in addition to deployment governance.
