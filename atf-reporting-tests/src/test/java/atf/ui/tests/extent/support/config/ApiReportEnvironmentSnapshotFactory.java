package atf.ui.tests.extent.support.config;

import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;

import java.util.List;
import java.util.Objects;

/** Builds the bounded metadata owned by the API precondition composition root. */
public final class ApiReportEnvironmentSnapshotFactory {
    private static final String ENVIRONMENT = "ACCEPTANCE";
    private static final String SUITE = "UI API precondition";
    private static final String TRANSPORT = "ACCEPTANCE";
    private static final String PRECONDITION = "atf-mock.yaml";
    private static final String FLOW = "selected API collection";
    private static final String FIXTURE = "atf-reporting-tests";
    private static final String EVIDENCE = "request + response + execution";
    private static final String REPORTING = "Extent HTML";
    private static final String ARTIFACT = "API setup artifact";

    private ApiReportEnvironmentSnapshotFactory() {
    }

    public static ReportEnvironmentSnapshot precondition(Class<?> setupOwner,
                                                          List<String> families) {
        Objects.requireNonNull(setupOwner, "setupOwner");
        Objects.requireNonNull(families, "families");
        return ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, ENVIRONMENT)
                .put(ReportEnvironmentField.TEST_PACKAGE, setupOwner.getPackageName())
                .put(ReportEnvironmentField.SUITE, SUITE)
                .put(ReportEnvironmentField.PROFILE, String.join(", ", families))
                .put(ReportEnvironmentField.TRANSPORT_MODE, TRANSPORT)
                .put(ReportEnvironmentField.PRECONDITION, PRECONDITION)
                .put(ReportEnvironmentField.FLOW, FLOW)
                .put(ReportEnvironmentField.FIXTURE, FIXTURE)
                .put(ReportEnvironmentField.API_EVIDENCE, EVIDENCE)
                .put(ReportEnvironmentField.REPORTING, REPORTING)
                .put(ReportEnvironmentField.REPORT_ARTIFACT, ARTIFACT)
                .build();
    }
}
