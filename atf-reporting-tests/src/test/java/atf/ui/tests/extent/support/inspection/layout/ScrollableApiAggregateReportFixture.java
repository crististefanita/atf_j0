package atf.ui.tests.extent.support.inspection.layout;

import atf.reporting.configuration.ExtentReportConfiguration;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.network.ReportNetworkInspector;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Publishes the network-table fixture through the API report's real request tree. */
public final class ScrollableApiAggregateReportFixture {
    private static final String REPORT_FILE = "api-scroll-viewport-%s.html";
    private static final String OWNER = "ScrollableApiAggregateFixture";
    private static final String TEST_PACKAGE = "atf.ui.tests.extent.e2e";
    private static final String SCENARIO = "API aggregate scroll viewport %s";
    private static final String PHASE = "TEST BODY";
    private static final String REQUEST_TITLE = "GET /viewport/aggregate — network evidence";
    private static final String REQUEST_METHOD = "GET";
    private static final String REQUEST_PATH = "/viewport/aggregate";

    private ScrollableApiAggregateReportFixture() { }

    public static Path publish(Path directory, ScrollableTableReportFixture.Density density) {
        String densityName = density.name().toLowerCase();
        ExtentReportConfiguration configuration = new ExtentReportConfiguration(
                directory, REPORT_FILE.formatted(densityName), false,
                ExtentReportConfiguration.DEFAULT_TIMESTAMP_PATTERN,
                ExtentReportConfiguration.DEFAULT_TIMESTAMP_ZONE);
        List<ReportExecutionEvidence> exchanges = new ArrayList<>(
                ScrollableTableReportFixture.exchanges(density.signatureCount()));
        if (density.expectsLongContent() && !exchanges.isEmpty()) {
            exchanges.set(0, ScrollableTableReportFixture.longTraceExchange());
        }
        exchanges.addAll(ScrollableTableReportFixture.failedExchanges(density.diagnosticCount()));
        var aggregate = ReportNetworkInspector.aggregate(exchanges, List.of());

        try (ExtentReportSession session = ExtentReportSession.openConfigured(configuration)) {
            AtfReportPorts.Scenario scenario = session.startScenario(OWNER, TEST_PACKAGE,
                    SCENARIO.formatted(densityName), List.of("api", "reporting", "scroll-viewport"));
            AtfReportPorts.Phase phase = scenario.startPhase(PHASE);
            AtfReportPorts.Request request = phase.startRequest(REQUEST_TITLE,
                    REQUEST_METHOD, REQUEST_PATH);
            exchanges.forEach(request::execution);
            request.networkAggregate(aggregate);
            request.complete(AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
            return session.reportPath();
        }
    }
}
