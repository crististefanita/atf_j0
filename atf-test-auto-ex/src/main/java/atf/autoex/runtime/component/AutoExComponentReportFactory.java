package atf.autoex.runtime.component;

import atf.reporting.audit.AssertionAuditRun;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.console.AtfConsoleSink;
import java.nio.file.Path;

/** Consumer metadata over the reusable report owner; no JUnit or test collection state. */
public final class AutoExComponentReportFactory {
    private AutoExComponentReportFactory() { }

    public static AssertionAuditRun open(Path directory, AtfConsoleSink console) {
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "COMPONENT")
                .put(ReportEnvironmentField.SUITE, "Automation Exercise component")
                .put(ReportEnvironmentField.REPORTING, "Extent HTML")
                .put(ReportEnvironmentField.REPORT_ARTIFACT, "Scoped component audit").build();
        return AssertionAuditRun.extent(directory, "component-atf-auto-ex", environment, console);
    }
}
