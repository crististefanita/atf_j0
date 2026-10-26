package atf.ui.tests.extent.support.audit;

import atf.reporting.ui.audit.input.ReportAuditInput;
import atf.ui.tests.extent.support.config.UiConfiguration;

/** Resolves one immutable report input from the active UI configuration. */
@FunctionalInterface
public interface ReportAuditInputSource {
    ReportAuditInput resolve(UiConfiguration configuration);
}
