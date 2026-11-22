<a id="built-and-safe-execution-for-atf"></a>
<a id="build-și-execuție-sigură-pentru-atf"></a>
# Built and Safe Execution for ATF

For Core and Reporting source in `atf_j0` and affected J1–J7 consumers, apply [workspace protocol](ATF_Workspace_Synchronization.md) before selective synchronization.

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

- the live checkout is the working context and synchronization source;
- builds and tests run in external staging;
- intermediate outputs are not promoted into the checkout;
- the final artifact is published only after verification in TAB MCP;
- existing processes and runs are not stopped.

<a id="isolated-workflow"></a>
<a id="fluxul-izolat"></a>
## Isolated workflow

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":8,"padding":6,"diagramPadding":20}}}%%
flowchart LR
    Live["Live"] -->|inspects| Inspect["Inspection"]
    Inspect -->|isolates| Stage["TMP stage"]
    Stage -->|synchronizes| Sync["Source sync"]
    Sync -->|runs| Build["Build"]
    Build -->|publishes| Report["Report"]
    Report -->|validates| MCP["TAB audit"]
    MCP -->|reviews| Review["Review"]
    Review -->|promotes| SyncBack["Promotion"]
```

<details>
<summary><strong>Legend — isolated workflow and anti-undo</strong></summary>

- live is the verified context, and staging is the temporary build location;
- generated output does not return to source or become an implicit baseline;
- review compares sources and evidence before any synchronization back;
- the final edge is selective: only approved and reconciled files are promoted.

The short labels shall be kept as follows:

| In the scheme | Full meaning |
|---|---|
| Live | Checkout live |
| Inspection | Readonly inspection |
| TMP stage | new staging area in a temporary directory |
| Source sync | source synchronization without outputs |
| Build | build and tests in staging |
| Report | report in staging |
| TAB audit | validation in TAB MCP |
| Review | diff and result review |
| Promotion | controlled synchronization of approved sources only |

</details>

<a id="actions-to-avoid"></a>
<a id="ce-nu-se-face"></a>
## Actions to avoid

- do not stop existing processes or runs;
- do not use `reset --hard`, `checkout --`, or `git clean`;
- do not delete `target`, `bin`, `obj`, `artifacts`, or another process's files;
- do not reuse staging that may belong to another execution;
- do not run two Maven builds in the same checkout or stage: both use `target`, and a concurrent `clean` can cause Surefire classloading false negatives; each execution gets its own staging area;
- do not overwrite a report opened by the user;
- do not open the Windows browser to validate a report; use TAB MCP for interaction.

<a id="safe-configuration-resolution"></a>
<a id="rezolvarea-sigură-a-configurației"></a>
## Safe configuration resolution

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":24,"padding":6,"diagramPadding":20}}}%%
flowchart LR
    Yaml["YAML"] -->|resolves| Base["Path bases"]
    Base -->|validates| Validate["Validation"]
    Validate -->|writes| Output["Output"]
```

<details>
<summary><strong>Legend — configuration captured at the composition root</strong></summary>

Reporting configuration is captured once at the composition root. A relative YAML path resolves against the captured `startDirectory`, while `reporting.extent.outputDirectory` resolves against the YAML directory. Paths that escape that directory through `..` are rejected before publication. This prevents two IntelliJ runs with different roots from accidentally sharing an output because `user.dir` changed or a relative path is ambiguous.

The short labels shall be kept as follows:

| In the diagram | Full meaning |
|---|---|
| YAML | atf.yaml · captured once |
| Path bases | startDirectory + YAML directory |
| Validation | validate boundaries · reject `..` escape |

</details>

<a id="change-workflow-for-every-change"></a>
<a id="regula-de-lucru-pentru-orice-schimbare"></a>
## Change workflow for every change

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":8,"padding":6,"diagramPadding":20}}}%%
flowchart LR
    Live["Live code"] -->|captures| Baseline["Baseline"]
    Baseline -->|isolates| Stage["Staging"]
    Stage -->|bounds| Change["Change"]
    Change -->|verifies| Verify["Verification"]
    Verify -->|audits| Audit["TAB audit"]
    Audit -->|reconciles| Reconcile["Reconciliation"]
    Reconcile -->|closes| Close["Documentation"]
```

<details>
<summary><strong>Legend — required protocol for each increment</strong></summary>

- **Live**: reread the code, tests, READMEs, feedback, and relevant artifact.
- **Baseline**: preserve failures; do not delete, relax, or rewrite tests for a false green.
- **Staging**: use a unique directory; copy only sources and documents, without generated output.
- **Change**: bound responsibility and define nominal and failure-path tests; do not overwrite or reset on conflict.
- **Verify**: run the relevant unit, component, and artifact tests, then the affected suite.
- **Audit**: inspect the new artifact in TAB MCP, including the changed surface and a negative scenario.
- **Reconcile**: compare staging with live again; synchronize approved files only.
- **Close**: update the owner README; close feedback only when there is observable evidence.

<details>
<summary><strong>Operational details — staging, artifact, and publication</strong></summary>

Live code and existing tests are the current context; staging is only the safe working location and does not automatically become a new source of truth. Do not stop existing processes or reuse another process's directory.

On refresh, compare hashes: a copied source may have a timestamp older than the classes in `target`, and Maven may reuse those classes. Use a new stage or invalidate incremental output only in your own copy, after the previous build has finished; preserve evidence already produced in TMP.

The report must be new, timestamped, and kept in staging; do not overwrite an open report or a run's output. In TAB MCP, check tabs, filters, disclosures, deep links, request/validation, Fails, and Overview as applicable.

Before publication, compare staging with the current live checkout. Keep only approved sources and documents; do not propagate `target`, `artifacts`, caches, logs, or generated output. After synchronization, reread the touched files and check the links.

</details>

The short labels shall be kept as follows:

| In the diagram | Full meaning |
|---|---|
| Live code | live code · README + feedback |
| Baseline | baseline · inspect + capture |
| Staging | isolated staging · approved sources |
| Change | bounded increment · nominal + failure test |
| Verification | build + tests · new artifact |
| TAB audit | TAB MCP · UI + negative path |
| Reconciliation | live resync · drift check |
| Documentation | feedback + owner README · observable evidence |

</details>

<a id="software-guard-for-livestaging-sync"></a>
<a id="guard-software-pentru-sync-livestaging"></a>
### Software guard for live↔staging sync

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":24,"padding":6,"diagramPadding":20}}}%%
flowchart LR
    Manifest["baseline"] -->|feeds| Build["stage gate"]
    Build -->|triggers| Guard["drift check"]
    Guard -->|allows| Promote["selective sync"]
```

<details>
<summary><strong>Legend — manifest, drift, and promotion</strong></summary>

- **Baseline**: the manifest contains only approved files and relative paths.
- **Staging**: the build and tests run separately from the live checkout.
- **Guard**: drift found during baseline prevalidation stops the batch before the first file; the destination is also revalidated on every copy.
- **Sync**: replacement is atomic per approved file; the batch is not transactional and does not implement rollback or implicit deletion.

The protocol above also has an executable mechanism, not just an operational convention. Before editing, the composition root captures approved files with `AtfPromotionManifest.capture(liveRoot, approvedFiles)` and can write the JSON manifest for process auditing. After the staging gate, `AtfPromotionGuard.promote(stagingRoot, liveRoot, baseline)` checks for parallel drift and atomically replaces each file in the manifest. A change or creation detected during the initial check stops the batch before the first file; a path absent from the baseline is the only explicit approval to create a new file. There is no unapproved overwrite, reset, or implicit deletion. The manifest stores only relative paths, presence, sizes, and digests, not absolute local paths, and is not included in the HTML report.

The guard prevalidates the entity plan before the first writing: regular, eligible and open sources for reading, with the steps reconciled with the manifest. A subsequent source absent or ineligible or an existing parent of the destination that is not the director refugees the lot without live changes. The pretending does not create missing parents; the directories are created Only for the executed file. The execution revalidates each destination; a subsequent I / O times drift may leave a partially promoted lot. remains per file, without the batch rollback; the permanent samples are at [Core](../atf-core/README.md#manifest-și-promovare-guarded).

- Verify and reread the actual state after any error; do not interpret
  an exception as proof that no files changed.
- Keep the digests and approved list for reconciliation; any retry
  requires a baseline that matches the current state, with no implicit overwrite.
- The lock coordinates promoters that use it; per-file revalidation does not
  make external editors participants in a transaction.

The evidence order is: live code → tests → HTML artifact → TAB MCP interaction → documentation. If two sources conflict, do not declare success until the difference has been investigated and reconciled.

PowerShell example, with paths supplied by the operator:

```powershell
$source = '.'
$stage = Join-Path $env:ATF_STAGING_ROOT 'atf-java-staging-<id>'

New-Item -ItemType Directory -Force $stage | Out-Null
robocopy $source $stage /E /XD '.git' 'target' 'artifacts' 'spark' 'bin' 'obj' /XF '*.log' 'hs_err_pid*.log'
mvn -o -f "$stage\pom.xml" test
```

Java is maintained in an external checkout setting, e.g. `${ATF_STAGING_ROOT}/Atf-java-feedback-<id>`. Sources resynchronise before the build; `target` reports and dumping remain out of sync. Reporting tests accept `-Datf.test.reportDirectory=<stage>\artifacts\reports` and the configured report uses `-Datf.report.config=<stage>\atf.yaml`; thus the report to be verified is deterministic. under `<stage>\artifacts\reports`, regardless of the IDE / Maven directory's working.

</details>

<a id="staging-for-test-change-governance"></a>
<a id="staging-pentru-guvernanța-schimbării-testelor"></a>
### Staging for test-change governance

`TestChangeGovernanceTest` requires a real Git checkout and the review baseline. A plain copy without history is not evidence for this gate and fails explicitly; do not disable the check to make staging pass.

- Staging starts without a checkout and takes a snapshot of live, including the absence of deleted or relocated files. Check the inventory against live; a clone with a checkout cannot implicitly reintroduce the old copy from `HEAD`.
- Identify the remote ref before fetch; do not assume it is named `main`. Fetch does not authorize pull, reset, or overwrite of local refactors; live state and the actual review base remain separate reconciliation inputs.


- Create an isolated local clone without checkout or hardlinks.
- Initialize the clone index from `HEAD`, then copy live sources without `.git`, `target`, `artifacts`, or local credential-bearing configuration.
- Preserve the source differences from the baseline; do not hide them by committing an artificial snapshot before verification.
- Run with `-Datf.governance.testChanges.baseRef=<review-base>`; the change check pins that base SHA. CI requires the base explicitly, and the live checkout is not reused for intermediate output.

This example supplements the source synchronization above:

```powershell
git clone --no-checkout --no-hardlinks . $stage
git -C $stage read-tree HEAD
robocopy . $stage /E /XD .git target artifacts spark bin obj /XF '*.log' '*.local.properties'
mvn -f "$stage\pom.xml" '-Dtest=WholeAtfGovernanceSuiteTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Datf.governance.testChanges.baseRef=<review-base>' test
```

<a id="the-reactor-is-mandatory-for-contracts-between-modules"></a>
<a id="reactorul-este-obligatoriu-pentru-contracte-între-module"></a>
### The reactor is mandatory for contracts between modules

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":24,"padding":6,"diagramPadding":20}}}%%
flowchart LR
    Changed["ATF change"] -->|trigger| Reactor["reactor gate"]
    Reactor -->|prove| SameSources["same-stage"]
    Standalone["reporting"] -->|trigger| Isolated["standalone gate"]
    Isolated -->|prove| IsolatedProof["isolated proof"]
```

<details>
<summary><strong>Legend - two independent geese</strong></summary>

- **Reactor gate**: `mvn -pl <consumer> -am test`; recommend the changed module with consumers on the same stage.
- **Standalone gate**: `mvn -f atf-reporting/pom.xml test`; check the isolated reporting without consumers.
- **Proof**: the result must come from the sources of the stage, not from an old local JAR.

When a test module consumes API exchanged into `atf-core`, `atf-ui` or other reactor modules, the check is run with `-am` (e.g. `mvn -pl atf-reporting-tests -am test`). For the standalone library, `mvn -f atf-reporting/pom.xml test` is the intentionally isolated gear; an isolated `mvn -pl atf-reporting-tests test` can solve a previously installed JAR from the local repository and can report missing builders / methods that do not exist in the source of the stagecoach. Isolated result is only acceptable after a green build reactor or if the dependencies used are explicitly constructed from the same stage; otherwise there is no evidence for live code.

After a re-sync at the same staging, the timestamps kept by copying may be older than already compiled classes, even if the signature of the source has changed. A Maven `Nothing to compile` message does not then prove source / class equivalence. Use a new staging or `clean test` only at private staging after completion of its own building; live checkout and The output of other executions is not being cleaned. The artifacts already produced remain separate from `target`.

- An interrupted compilation can leave incomplete `.class` files even if Maven considers them current. `ClassFormatError` requires checking the compiled output before changing the source or assertions.
- After your execution finishes, rebuild the affected output in new or private staging, then repeat the complete affected verification; tests run with incomplete bytecode do not certify the live sources.

</details>

<a id="periodic-feedback-and-build-sync-loop"></a>
<a id="bucla-periodică-de-sync-pentru-feedback-și-build"></a>
### Periodic feedback and build sync loop

```mermaid
%%{init: {"themeVariables":{"fontSize":"15px"},"themeCSS":".nodeLabel, .nodeLabel p { font-size: 15px !important; line-height: 1.35 !important; } .edgeLabel, .edgeLabel p { font-size: 12px !important; font-weight: 400 !important; line-height: 1.2 !important; } .edgeLabel .labelBkg, .edgeLabel p { background: transparent !important; } .edgeLabel foreignObject { transform: translateY(-18px); overflow: visible; } .node rect { rx: 6px; ry: 6px; }","flowchart":{"curve":"linear","nodeSpacing":24,"rankSpacing":24,"padding":6,"diagramPadding":20}}}%%
flowchart LR
    Live["Live"] -->|recreates| Recreate["New staging"]
    Recreate -->|verifies| Verify["Verification"]
    Verify -->|feeds| Feedback["Documentation"]
    Feedback -->|promotes| Promote["Promotion"]
```

<details>
<summary><strong>Legend — resuming a ToDo and isolating output</strong></summary>

Whenever resuming a ToDo, start from the live checkout, recreate staging, and copy sources only; `target`, HTML reports, captures, and dumps are not sources of truth and are not synchronized back. After the build, tie the evidence to the synchronized code using three identifiers: the working commit/snapshot, the timestamped artifact, and interactive verification in TAB MCP. If an agent works elsewhere, repeat the same sequence before each build; do not accept an old artifact as evidence for live code.

Publication to the same final path is serialized by a per-path lock in the process temporary directory; the lock is not written beside the report and does not change the session's private staging area.

Each Extent staging is marked by a file that is versioned and kept under a lease exclusively while the session is active. After a forced interruption, the operator can explicitly run the bounded sweeper in `atf.reporting.publication.ReportStagingWorkspace`, for example:

```java
var result = ReportStagingWorkspace.sweep(
        reportDirectory,
        Duration.ofHours(1));
```

The sweeter only inspects `.atf-extent-session-*` directories marked directly below `reportDirectory`, keeps the workspaces fresh, active, malformed or unmarked and returns the `scanned/deleted/skipped/failures` cardinality. Default processes no more 256 candidates; the `maxCandidates` overload can be used for operationalization, and `limitReached=true` makes observable limitation in the test and diagnosis.

<id> is unique to the current session. If there are locally modified files, check before synchronization so they are not overwritten. Any external project running in parallel redirects its output to its own staging directories; the ATF Java build uses its own `target` and artifacts separately.

The short labels shall be kept as follows:

| In the diagram | Full meaning |
|---|---|
| Live | live checkout |
| New staging | recreate staging |
| Verification | build + artifact + TAB MCP |
| Documentation | feedback / owner README |
| Promotion | selective sync |

</details>

<a id="jvm-memory-for-tests-and-publication"></a>
<a id="memoria-jvm-pentru-teste-și-publicare"></a>
## JVM memory for tests and publication

| Process | Configuration | Role |
|---|---|---|
| Maven | `.mvn/jvm.config`, `MAVEN_OPTS` | Runs the build; does not size the test fork |
| Surefire | `pom.xml`: `atf.test.heap.max=2g` | Maximum heap of 2 GiB per test JVM; initial heap is 64 MiB |
| Direct IntelliJ JUnit | Run Configuration → VM options | Explicitly sets `-Xms64m -Xmx2g -XX:MaxMetaspaceSize=128m` |

- Explicit Maven override: `mvn "-Datf.test.heap.max=3g" ... test`; the limit applies to each fork, not the entire reactor.
- `-DargLine=...` does not replace the `argLine` explicitly configured in this POM; use `atf.test.heap.max` to override the heap size.
- Keep the normal Surefire fork; `-DforkCount=0` changes execution mode and is not the recommended remedy for report memory usage.
- Reports with inline PNGs also need memory during finalization: the pipeline post-processes the entire HTML before atomic publication.
- An `OutOfMemoryError: Java heap space` during finalization invalidates publication, even if the test methods passed; also check the Maven exit code.
- Do not disable captures to hide insufficient heap. The 2 GiB is the default budget, not a guarantee for every volume; concurrent forks and browsers consume additional memory.

<a id="what-to-sync-and-what-to-exclude"></a>
<a id="ce-se-sincronizează-și-ce-nu"></a>
## What to Sync and What to Exclude

Only explicitly reviewed sources and documents may be propagated:

- projects, sources, and their tests;
- READMEs and Markdown documents;
- tracked configuration files, when the change requires them;
- validated and reproducible patches.

Do not propagate:

- target, bin, obj, artifacts, spark, or caches;
- generated reports and execution snapshots;
- logs, dumps, and temporary files;
- files created by another process in the live checkout.

Synchronization back is a selective operation after diff review. If there are concurrent changes in the same file, stop editing that file only and reread the live content; do not fix it through overwrite or reset.

<a id="preventing-accidental-rollback-when-returning-from-staging"></a>
<a id="regula-anti-undo-la-revenirea-din-staging"></a>
### Preventing accidental rollback when returning from staging

Staging is a working copy, not an authoritative source. When returning to live, do not mirror the directory or recursively copy staging over the checkout. Promote only files in the task area, following this sequence:

```text
current live → baseline for approved files
staging      → diff limited to the same files
reconciliation with parallel changes in live
selective promotion only if the baseline is unchanged
reread live and verify the owner/documentation
```

During reconciliation, live remains authoritative for changes made in parallel:

- keep new changes and moves and integrate them selectively;
- a file absent from staging does not authorize deleting it from live;
- make a conflict an explicit ToDo and stop promotion of that file;
- other approved files and active runs may continue.

<a id="the-staging-report"></a>
<a id="raportul-din-staging"></a>
## The staging report

The report generated in staging is the only source for visual verification of the change. In TAB MCP:

1. open the exact artifact from staging;
2. check Tests, Category, Fails, and Overview;
3. expand at least one positive and one negative test;
4. confirm request/validation ownership, the aggregated table, and scrollable JSON;
5. test the link from Fails to the original test;
6. verify that Overview is the default and evidence nodes are collapsed by default.

A static HTML check is useful, but it does not replace clicking in TAB MCP for changes to layout, selectors, collapse, tabs, or scrolling.

<a id="completion-criteria-for-a-safe-execution"></a>
<a id="criterii-de-finalizare-pentru-o-execuție-sigură"></a>
## Completion criteria for a safe execution

- the existing process has not been stopped;
- staging is separate and contains no output copied from old runs;
- the build and tests have run in staging;
- the artifact is self-contained and has not overwritten the live report;
- interactive verification was performed in TAB MCP when required;
- synchronization back contains only approved sources/documents;
- no files were deleted and no foreign checkouts were modified.

