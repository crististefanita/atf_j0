# Reporting J16 — screenshot viewport and audit context

J16 keeps the complete PNG visible in a shared Extent/fallback viewer and preserves the clicked checkpoint when a capture is deduplicated. Business setup, inputs and independent expected values remain producer-owned; this release does not reconstruct missing Android or component evidence.

## Published library

| Item | Verified value |
|---|---|
| coordinate | atf.api:atf-reporting:1.0.0-j16 |
| JAR SHA-256 | 02B798A5848D5167FECA16A0977F4F30B7047BB49EA666200AA7D0EA11731367 |
| POM SHA-256 | B2C0E6CE98F3DB07DF6EF52C540A4BE2F6BD27D94B73F3810C0E9BB3C2BC422E |
| Core | C3 unchanged; no new Core publication |
| native gate | 91 passed in each of two independent parentless builds; identical JAR bytes |
| producer inputs | 416 raw files; 17 native Java files; only the project version is transformed |
| compatibility | all 6,597 public/protected J15 entries retained; two asset enum constants added, none removed |
| layout | latest Reporting JAR/POM directly in each consumer lib; source and generic tests stay in J0 |

## Runtime behavior

| Surface | Contract |
|---|---|
| fit view | viewport-sized native dialog; complete portrait/landscape/square image, original aspect ratio and no default image scroll |
| original size | explicit inspection mode; source bytes remain unchanged |
| duplicates | resolve canonical bytes without clicking the canonical owner; preserve the local caption, including escaped literals |
| return | same active owner, scenario and disclosure state; reading checkpoint and keyboard focus restored |
| resize | preserve the reading anchor across layout reflow; numerical scroll is clamped only to the current legal range |
| accessibility | mouse, Enter, Space, Escape, labeled controls and keyboard focus containment |
| existing evidence | UI actions, API evidence, SQL observations, four-column assertions, status and preview 240 retain their contracts |

The viewer uses a report-owned browser dialog; it does not require browser/OS fullscreen permission. Existing screenshot anchor semantics are retained for compatibility. CSS is scoped to the viewer and the original PNG is not cropped, transcoded or replaced.

## Permanent regression evidence

| Gate | Scope and result |
|---|---|
| final source | 96 distinct verified cases; broad selection plus isolated affected reruns |
| physical JAR | 92 distinct verified cases; physical C3/J16 CodeSource and focused media/row reruns |
| component references | every one of 45 captures resolves to its original bytes; Extent: 3 canonical plus 42 references; fallback: 45 direct, all with exact local captions |
| browser journey | two owners, long scroll, three image orientations, resize, fit/original-size, click/Enter/Space, close/Escape and focus return |
| independent negative | a reset to zero is refused even when viewport reflow reduces the maximum legal scroll |
| observation-only media | explicit media route retains PNG plus OBSERVE with zero invented business assertions, in both renderers |
| SQL compatibility | 18 native Extent/fallback reports, 40 invocations and 8 controlled failures; independent artifact/mutant oracles pass |
| adjacent contracts | full values, redaction, literal text/JSON, typed row projection, assertion indicator, timing, disclosure and asset budgets |

The initial red viewer gate could not find the requested native dialog in J15. A later binary gate exposed an invalid pixel-only scroll assumption after resize; the corrected oracle separates unchanged-layout scroll from reflow and verifies the clicked checkpoint remains visible. That stronger check also exposed a real reflow return defect, which was fixed before the delivery build; failed receipts remain in TMP.

The asset contract rejected a regex form that retained source newlines during publication. The viewer now uses an explicit assignment that follows the existing compaction policy, with separately measured owner budgets; the fallback owner ceiling is unchanged. No assertion, preview or payload oracle was relaxed.

## Consumer acceptance

| Consumer | Final J16 acceptance | Boundary |
|---|---|---|
| J1 | 31 PASS in owned staging | Shop/AutoEx component sanity; no new live web journey claimed |
| J2 | 11 PASS in owned staging | SQL and MOCK/embedded Eventuate; no broker or Eventuate application acceptance |
| J3 | 8 PASS in owned staging | PCM and canonical preparation; no new live microphone capture |
| J4 | 6 PASS in owned staging | Core/Reporting, PNG adapter and inline contracts; no new device execution |

- The expanded browser matrices encountered unchanged five-second timeouts under concurrent browser runs; affected classes passed in isolation, without code or deadline relaxation. Failed runs and the exact repair gates remain in the acceptance receipts.

- Fresh copies of the delivered live consumers passed the same 31 / 11 / 8 / 6 selections after distribution. The verifier matches the JAR in live lib, copied lib and each physical Maven test classpath; all four contain exactly the C3 and J16 JAR/POM pairs.

- The candidate and post-distribution receipts are distinct. Each consumer retains C3 and the same J16 bytes, with its own source snapshot and acceptance; one consumer does not certify another.

## Documentation and governance

- J0: 156 cases; J1: 156; J2: 170, all passed. J3 uses its own governance selection: 69 cases passed; an earlier unmatched suite selector executed zero cases and is not acceptance evidence.
- The focused document audit passed nine owner documents after reconciling concurrent feedback. Incoming consumer run evidence remains in J4, with only neutral renderer follow-up in J0.
- TAB MCP checked the physical-JAR Extent/fallback viewer, standard UI ACTION/OBSERVE tables, SQL exact-decimal assertions and the standalone reader prototype.

## J4 reporting inputs

- A recorded corpus artifact has 1,137 evaluator-summary rows under request/validation nesting; PASS/PASS is not the full input/rule/expected audit of each retained game.
- J4 owns meaningful titles, effective setup, input/state facts, rule legends, captured comparisons, measured action timing and optional technical provenance.
- J0 owns neutral rendering and reusable mock/browser contracts; native component assertion groups must not require fabricated HTTP requests.

- The adaptive-detail policy is an implementation requirement in J4: AUTO / SUMMARY / FULL, positive optional-context budgets, explicit omissions and mandatory audit facts. Six current publication entrypoints override the configured body limit, including the new learning composition root; J16 does not implement or silently enable that consumer policy.

- The consumer feedback tracks implementation separately. The temporary reader prototype preserves 130 captured invocations and 425 source-attested comparisons, with missing legacy detail explicit; it is a composition review, not a fresh business or device execution.

- Detailed gate receipts are located through evidenceRoot in library-provenance.json. Existing J15 reports remain historical; the new viewer appears in reports generated with J16.
