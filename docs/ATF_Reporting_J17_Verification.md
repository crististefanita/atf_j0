# Reporting J17 — retained-evidence navigation acceptance

## Release identity

Reporting J17 is built from J0 source in owned TMP staging. Core C3 is unchanged.

| Property | Value |
|---|---|
| coordinate | atf.api:atf-reporting:1.0.0-j17 |
| JAR SHA-256 | B7DF32027A91F29B3662D0DE179BC329AB3018006E4894FAD594401D6FDA9F0A |
| published POM SHA-256 | 4FF3CDC6F54043532FEE5857E2778BD720009B4840C49768AB3E82FBEC3B8D8C |
| producer base | f1bfc261e40a08cdfc3e411a2fd4ef1525970147, plus reviewed working changes |
| reproducibility | two parentless builds, identical JAR bytes; 91 native tests pass in each |
| Java compatibility | all 543 class entries are byte-identical to J16; four JavaScript assets change |
| evidence policy | original supplied comparisons/bodies/PNG remain; preview 240; native table formatting |

## Change and permanent regression

The [navigation contract](ATF_Large_Report_Navigation.md) defines deferred hidden-table
measurement, mutation-scoped layout invalidation, per-pass execution-marker cache
and shared source/scenario indexes for projected views.

| Regression | Independent proof |
|---|---|
| neutral dense fixture | four owners, 12,004 comparison rows and four original synthetic PNGs; bulk expansion and targeted paths |
| external large input | targeted assertion/PNG paths only; no Expand all |
| retained evidence | per-owner SHA-256 of original comparison cells, bodies and PNG references before/after navigation |
| visibility | closed/open/reopened evidence and direct text-node mutation |
| timing | removed/replaced/restored execution markers refresh between aggregation passes |
| shared allocator | exact complete allocator remains in fallback; J16 appends its viewer after it, so a suffix assertion was stale |
| asset budget | measured total ceilings, independent owner caps and a negative owner-cap test |

Verdicts, payload budgets and preview 240 are unchanged.

## Verified release and publication method

The frozen release passes the following gates. Logs, source snapshots and
rendering captures remain in owned TMP staging ATF_REPORT_NAVIGATION_ACTIVE_DO_NOT_CLEAN_AYBvNf.

| Gate | Tests / failures / errors / skips |
|---|---|
| native parentless A and B | 91 / 0 / 0 / 0 in each build |
| final affected source | 34 / 0 / 0 / 0 |
| source and test-change governance | 22 / 0 / 0 / 0; final VERIFIED-control rerun: 1 / 0 / 0 / 0 |
| physical-JAR browser contracts | 24 / 0 / 0 / 0 |
| physical-JAR asset contracts | 20 / 0 / 0 / 0 |
| complete archived Android plus JAR identity | 2 / 0 / 0 / 0 |
| strengthened canonical PNG inventory | source plus control: 2 / 0 / 0 / 0; dense JAR: 1 / 0 / 0 / 0; full Android JAR: 2 / 0 / 0 / 0 |
| J1 component suites / real web E2Es | 47 / 0 / 0 / 0; 2 / 0 / 0 / 0 |
| J2 SQL and Eventuate MOCK | 11 / 0 / 0 / 0 |
| J3 PCM and canonical preparation | 8 / 0 / 0 / 0 |
| J4 component/reporting contracts | 55 / 0 / 0 / 0 |
| separate J1 / J2 / J3 deployment gates | 32 / 0 / 0 / 0; 42 / 0 / 0 / 0; 5 / 0 / 0 / 0 |
| consumer rendering plus physical-JAR identity | 15 / 0 / 0 / 0 across 14 fresh artifacts |
| fresh post-distribution J1 / J2 / J3 / J4 | 32 / 0 / 0 / 0; 42 / 0 / 0 / 0; 6 / 0 / 0 / 0; 5 / 0 / 0 / 0 |

- The large publication is 55,696,482 bytes; captured comparisons, canonical PNG payloads and PNG references remain byte-identical across repeated owner switches and targeted opening.
- Observed large-artifact readiness was 21.912 s including cold load. Owner switches were 3.626 / 4.078 / 4.371 s; each browser action retained its 10 s deadline.
- Whole targeted assertion/PNG paths and captures took 19.508 / 16.794 / 29.477 s. Those multi-action browser intervals are not execution timing inside the Android report.
- Fresh consumer rendering covers Tests, Category, Fails and Overview; individual/combined component scopes, UI actions, API/SQL and Extent/fallback retain their native formatting.
- Chromium captures show the original Android display fully inside the shared viewer, including system bars; no crop or PNG substitution is used.
- Consumer DOM/rounded JS-heap samples are in the rendering log. They are point-in-time browser readings, not peak resident-memory guarantees.

TAB MCP controls became unavailable in the client after the earlier session.
Automated Chromium interactions and their actual PNG captures are verified;
a fresh manual TAB MCP acceptance is not claimed. On 3 November 2026 the user
explicitly authorized publication using these Chromium checks and visually
inspected captures for this delivery.

<details>
<summary><strong>Retained unsuccessful attempts</strong></summary>

- J16 and early candidates timed out in large-report acceptance; profiling identified repeated marker/view lookups and eager hidden-table work.
- An additional cold-start readiness rerun timed out at the unchanged 10 s bound. The isolated rerun passed with the same code/JAR and no deadline relaxation; host load remains an explicit performance limit.
- A temporary oracle selected a hidden Fails copy; the permanent test now scopes selectors to Tests and exercises targeted navigation on the neutral fixture.
- The first current J4 learning rerun refused an existing output path. A fresh output directory passed all 55 contracts; the no-overwrite guard was preserved.
- A temporary fallback probe assumed only one scenario was visible. Fallback intentionally retains multiple scenarios; selecting the exact original scenario ID passed the complete matrix.
- The interrupted bulk diagnostic is not passing release evidence, and bulk expansion on the real large report is excluded by the user.

</details>

The final source and physical-JAR inventory also rejects a one-character change
to a canonical PNG while its link, caption and owner stay unchanged. J3 post-checks
include five deployment cases and one actual adapter component; all four fresh
consumer classpaths resolve the same Reporting JAR SHA-256.

## Scope limits

- A republished Android receipt is not a new device execution; fixture checks do not certify ASR or a broker/saga.
- Cold parsing of a 55.7 MB artifact depends on the host; there is no universal browser-memory or elapsed-time guarantee.
- The user excluded bulk expansion on the real large report. Its earlier diagnostic measured about 16 seconds and did not complete; no image/assertion was removed to reduce that cost.

Historical J16 device, SQL and speech records retain their original identities.
The binary consumers keep exactly one current Core pair and one current Reporting
pair directly in lib; the producer source and harness remain in J0.
