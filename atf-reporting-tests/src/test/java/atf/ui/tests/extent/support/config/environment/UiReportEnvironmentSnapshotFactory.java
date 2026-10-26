package atf.ui.tests.extent.support.config.environment;

import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.ui.tests.extent.support.config.UiConfiguration;

import java.util.Objects;

/** Builds bounded, typed Overview/System metadata for reporting test composition roots. */
public final class UiReportEnvironmentSnapshotFactory {
    private UiReportEnvironmentSnapshotFactory() {
    }

    public static ReportEnvironmentSnapshot audit(UiConfiguration configuration,
                                                  String suite,
                                                  String flow) {
        Objects.requireNonNull(configuration, "configuration");
        return ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "ACCEPTANCE")
                .put(ReportEnvironmentField.TEST_PACKAGE, "atf.ui.tests.extent")
                .put(ReportEnvironmentField.SUITE, suite)
                .put(ReportEnvironmentField.PROFILE, "report-audit")
                .put(ReportEnvironmentField.UI_HOST,
                        configuration.browser().baseUrl().toString())
                .put(ReportEnvironmentField.BROWSER, configuration.browser().browser())
                .put(ReportEnvironmentField.HEADLESS,
                        Boolean.toString(configuration.browser().headless()))
                .put(ReportEnvironmentField.VIEWPORT,
                        configuration.browser().viewportWidth() + "x"
                                + configuration.browser().viewportHeight() + " @ "
                                + configuration.browser().zoomPercent() + "%")
                .put(ReportEnvironmentField.UI_TIMEOUT,
                        duration(configuration.browser().timeout().toMillis()))
                .put(ReportEnvironmentField.NAVIGATION_TIMEOUT,
                        duration(configuration.browser().navigationTimeout().toMillis()))
                .put(ReportEnvironmentField.FLOW, flow)
                .put(ReportEnvironmentField.FIXTURE, "atf-reporting-tests")
                .put(ReportEnvironmentField.API_EVIDENCE, "generated API Extent report")
                .put(ReportEnvironmentField.NETWORK_INSPECTOR, "browser events")
                .put(ReportEnvironmentField.RETRY_POLICY,
                        configuration.browser().retryPolicy().enabled()
                                ? "read-only; max="
                                + configuration.browser().retryPolicy().maxAttempts()
                                : "disabled")
                .put(ReportEnvironmentField.SCREENSHOT_POLICY,
                        configuration.screenValidation().enabled()
                                ? (configuration.screenValidation().fullPage()
                                ? "enabled; full-page" : "enabled; viewport")
                                : "disabled")
                .put(ReportEnvironmentField.REPORTING, "Extent HTML")
                .put(ReportEnvironmentField.REPORT_ARTIFACT, "UI audit artifact")
                .build();
    }

    public static ReportEnvironmentSnapshot fixture(String suite, String flow) {
        return ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "ACCEPTANCE")
                .put(ReportEnvironmentField.TEST_PACKAGE, "atf.ui.tests.extent")
                .put(ReportEnvironmentField.SUITE, suite)
                .put(ReportEnvironmentField.PROFILE, "static-fixture")
                .put(ReportEnvironmentField.FLOW, flow)
                .put(ReportEnvironmentField.FIXTURE, "atf-reporting-tests")
                .put(ReportEnvironmentField.REPORTING, "Extent HTML")
                .put(ReportEnvironmentField.REPORT_ARTIFACT, "UI fixture artifact")
                .build();
    }

    private static String duration(long millis) {
        return millis + " ms";
    }
}
