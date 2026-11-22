<a id="reporting--auditable-evidence-and-reports"></a>
<a id="reporting--dovezi-și-rapoarte-auditabile"></a>
# Reporting — auditable evidence and reports

<a id="at-a-glance"></a>
<a id="pe-scurt"></a>
## At a glance

Reporting publishes the caller's data, comparisons, and verdict to Extent/fallback, without Core or Kit.

| Need | API |
|---|---|
| Individual/class/suite run | `AssertionAuditRun` |
| Invocation | `run.open(Identity)` → `AssertionAuditReport` |
| Ports / fallback | `AtfReportPorts` / `HtmlReportSession` |
| Observed HTTP | `request.execution(ReportExecutionEvidence)`, with `ReportResponse` |
| Screenshots | `UiExtentReportSession`; full PNG |

<a id="minimal-example"></a>
<a id="exemplu-minim"></a>
## Minimal example

Individual run: `try (var run = open(output)) { publish(run, actual); }`. A suite keeps the run open and opens one scenario per invocation:

```java
import atf.reporting.audit.AssertionAuditRun;
import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.security.SensitiveDataRedactor;
import java.nio.file.Path;
import java.util.List;

public static AssertionAuditRun open(Path output) {
    var settings = ReportingSettings.defaults();
    var redactor = new SensitiveDataRedactor();
    return new AssertionAuditRun(new ExtentReportSession(output, settings, redactor), redactor, settings);
}

public static void publish(AssertionAuditRun run, String actual) {
    boolean passed = "ready".equals(actual);
    var status = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
    var identity = new AssertionAuditReport.Identity("HealthContract", "example", "state is ready", "state", List.of("component"));
    try (var report = run.open(identity)) {
        report.phase("TEST BODY", List.of(new AtfAssertion("State", "ready", String.valueOf(actual), passed)), status, "");
        report.complete(status);
    }
    if (!passed) throw new AssertionError("State differs; inspect the report");
}
```

<a id="scope-and-integration"></a>
<a id="scope-și-integrare"></a>
## Scope and integration

| Owner | Closing and limit |
|---|---|
| AssertionAuditRun | Owns the session; close publishes the final HTML. |
| run.open(identity) | Borrows the session; close closes the invocation's scenario. |
| AssertionAuditReport(session, ...) constructor | Owns and closes its own session. |

- Use the same settings/redactor in the run and renderer; the default preview is **240**, read through assertionMaxDisplayCharacters(). Full values precede the preview.
- The example follows the session thread; AssertionAuditRun.extent(...) serializes publication for concurrent callers.
- Kit collects the JUnit comparison, evaluated once; Reporting publishes it. The caller preserves the verdict, cause, and timing through phase(..., timing).
- Fails indexes the terminal cause on the failed ACTION or OBSERVE row. Tests also retains the collapsible diagnostic, while independent comparisons remain distinct.
- The PNG retains the full viewport/display; capture and zoom belong to the adapter. Identical screenshots from the same step retain one piece of evidence and the first caption.
- After close, ReportArtifactInspector.inspect(path, profile) qualifies the inventory and embedded resources; diagnostics identify external dependencies. Text paths remain valid. The inspector is static; rendering and interactions are verified separately in a browser.
- Titles are published as literal text in Extent and fallback. The profile accepts the original labels, including multiline labels. ReportArtifactProfile.visibleLabel(label) provides the same whitespace projection as the inspector.
- The browser belongs to the caller: separate JUnit discovery diagnostics from execution, close the context before the browser, and preserve the original cause during cleanup.

HTTP duration comes from `ReportResponse.duration()`; `detail(...)` preserves the caller's text without turning it into a transport measurement.

---

J0: [code, tests and feedback](README.md#mentenanță-și-feedback).

[Consumer integration guides](README.md#consumer-guides).
