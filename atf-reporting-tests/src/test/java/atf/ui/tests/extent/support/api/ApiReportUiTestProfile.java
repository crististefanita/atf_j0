package atf.ui.tests.extent.support.api;
import atf.reporting.ui.audit.vocabulary.UiAuditGherkin;

import java.util.List;
import java.util.Objects;

/**
 * Reusable declarative catalog entry for an API-to-UI report contract.
 *
 * <p>This is intentionally production-side test infrastructure: it contains
 * no JUnit lifecycle and no browser implementation. Executable UI tests only
 * select a profile and provide their annotation-driven setup. Keeping the
 * catalog here prevents scenario metadata from being duplicated in every
 * {@code *ReportUiTest} class.</p>
 */
public final class ApiReportUiTestProfile {
    /** The only browser smoke profile: exact methods come from @ApiTestRef. */
    public static final ApiReportUiTestProfile SELECTED_SMOKE = profile(
            "selected-api-tests",
            "Selected API methods produce one mock-backed UI report",
            List.of("ui", "api", "mock-contract", "selected-tests"));

    private final String reportKey;
    private final String auditTitle;
    private final List<String> tags;

    private ApiReportUiTestProfile(String reportKey, String auditTitle,
                                   List<String> tags) {
        this.reportKey = Objects.requireNonNull(reportKey, "reportKey");
        this.auditTitle = Objects.requireNonNull(auditTitle, "auditTitle");
        this.tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
    }

    private static ApiReportUiTestProfile profile(String reportKey, String auditTitle,
                                                  List<String> tags) {
        return new ApiReportUiTestProfile(reportKey, auditTitle, tags);
    }

    public String reportKey() {
        return reportKey;
    }

    public String auditTitle() {
        return auditTitle;
    }

    public List<String> tags() {
        return tags;
    }

    public String setupDescription() {
        return UiAuditGherkin.GENERATED_API_REPORT_READY;
    }

    public String failureMessage() {
        return auditTitle + " failed";
    }

    public String screenTitle(String ownerName) {
        return ownerName + " matches mock JSON contracts";
    }

    public String reportLabel() {
        return "API Extent report";
    }
}
