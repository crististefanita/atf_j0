# ATF workspace rules

- Use the sibling workspace `atf_j0` (`../atf_j0`) as the current Core/Reporting/Console/Mock
  source and test reference.
  `demo2` is historical; do not edit or synchronize it for new work.
- Read `docs/ATF_Workspace_Synchronization.md` and
  `docs/ATF_Build_And_Execution_Safety.md` before changing or delivering code.
- Keep Reporting implementation/native tests in `atf_j0/atf-reporting` and
  permanent mock, contract and browser regressions in `atf_j0/atf-reporting-tests`.
- After relevant J0 changes, assess `atf_j1`, `atf_j2` and `atf_j3`. Deliver and
  validate each initialized consumer separately. Inspect J2's current bootstrap
  and messaging scope; do not infer its acceptance from J1's results.
- J2 keeps its existing Git and messaging scope. Its explicit Eventuate profile includes Setup and excludes Shop, AutoEx, UI and Debug UI; do not restore those modules or Playwright in J2.
- Keep Core implementation and native tests only in J0. Binary consumers
  retain the latest Core, Reporting, Console and Mock JAR/POM pairs directly in `lib`,
  with one root provenance file. Do not copy Reporting source or its harness
  into the live binary consumers.
- Reconcile common code and consumer adapters selectively in both directions.
  Preserve local work, reactor POMs, configurations and independent Git bases.
  A successful file promotion is not an atomic transaction across workspaces.
- Build/test only in owned TMP staging; keep generated reports/cache/output
  there. Preserve the 240-character default reporting preview and auditable,
  collapsible component/E2E assertions.
- Keep owner documentation and feedback current with the actual code and
  tests. Preserve historical build provenance; a directory rename alone does
  not require a new Reporting version or imply a fresh test execution.

- J3 is the five-module speech consumer. Deliver the same latest Core/Reporting flat JAR/POM pairs, preserve its local streaming work, and validate it separately.

- Keep active J0 feedback limited to Core/Reporting/Console/Mock, including generic Reporting tests. J1 coordinates other shared-module criteria and consumer handoffs; preserve domain ownership and independent acceptance in J2/J3/J4. Compatibility routes may remain in J0 without duplicated TODOs.

- Keep project README files and docs focused on current contracts. Retain only the current Reporting verification note beside its producer package in owned TMP; record its location in current consumer provenance. Do not accumulate per-release verification Markdown in docs or consumer lib. Preserve historical build/test provenance.

- Official documentation describes current contracts, not iterative Core/Reporting improvements. Keep library release chronology, dated acceptance counts and scorecards outside official README/atlas/contract documents; retain current packaging evidence beside the matching library. Report review scores in the conversation and keep actionable gaps at their owner.
