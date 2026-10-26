package atf.ui.tests.extent.support.inspection.layout;

import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTraceMetadata;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.ui.UiExtentReportOptions;
import atf.reporting.ui.UiExtentReportSession;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static atf.reporting.ui.validation.UiApiValidationFixtures.observedExchanges;

/** Creates authentic Extent artifacts with bounded or overflowing table data for browser proof. */
public final class ScrollableTableReportFixture {
    private static final String REPORT_PREFIX = "scroll-viewport-";
    private static final String TEST_CLASS = "ScrollableTableFixture";
    private static final String TEST_PACKAGE = "atf.ui.tests.extent.e2e";
    private static final String SCREEN_TITLE = "GIVEN report tables have viewport data";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String BASE_URL = "https://api.example.test";
    private static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    private ScrollableTableReportFixture() { }

    public static Path publish(Path directory, Density density) {
        Map<String, String> systemInfo = systemInfo(density.systemRows());
        var options = UiExtentReportOptions.builder(directory)
                .fileName(REPORT_PREFIX + density.name().toLowerCase() + ".html")
                .timestampEnabled(false)
                .console(AtfConsoleSink.noOp())
                .systemInfo(systemInfo)
                .build();

        try (UiExtentReportSession session = UiExtentReportSession.openConfigured(options)) {
            var scenario = session.startScenario(TEST_CLASS, TEST_PACKAGE,
                    "scroll viewport matrix " + density.name().toLowerCase(), tags(density.categoryCount()));
            var phase = scenario.startPhase("TEST BODY");
            phase.screenValidation(SCREEN_TITLE, "#test-view", ONE_PIXEL_PNG,
                    assertions(density.assertionCount()), actions(density.actionCount()),
                    AtfReportStatus.PASS);
            phase.apiTrace(AtfGherkinKeyword.THEN,
                    "the checkpoint API exchange retains technical trace evidence",
                    "/viewport/ui-checkpoint",
                    observedExchanges(List.of(density.expectsLongContent() ? longTraceExchange() : exchanges(1).getFirst())),
                    AtfReportStatus.PASS);
            phase.complete(AtfReportStatus.PASS);
            scenario.complete(AtfReportStatus.PASS);
            return session.reportPath();
        }
    }

    private static Map<String, String> systemInfo(int count) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index <= count; index++) {
            values.put("Fixture system field %02d".formatted(index),
                    "scroll viewport value %02d".formatted(index));
        }
        return Map.copyOf(values);
    }

    private static List<String> tags(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> "viewport-category-%02d".formatted(index)).toList();
    }

    private static List<AtfUiActionRecord> actions(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> new AtfUiActionRecord(index, "Get text",
                        "#viewport-row-%02d".formatted(index), "row text is available",
                        "row %02d".formatted(index), 1, AtfReportStatus.PASS))
                .toList();
    }

    private static List<AtfAssertion> assertions(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> new AtfAssertion("Viewport assertion %02d".formatted(index),
                        "visible", "visible", true))
                .toList();
    }

    static List<ReportExecutionEvidence> exchanges(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(index -> {
            String path = "/viewport/items/%02d".formatted(index);
            var request = new ReportRequest("GET", path, Map.of(),
                    Map.of("Accept", JSON_CONTENT_TYPE), "none", "");
            var response = new ReportResponse(200, "{\"id\":%d}".formatted(index),
                    Map.of("Content-Type", JSON_CONTENT_TYPE), Duration.ofMillis(2), BASE_URL + path);
            return new ReportExecutionEvidence(response, 1, Duration.ofMillis(2), List.of(),
                    List.of(), request, ReportTransportProvenance.ACCEPTANCE_MOCK,
                    ReportTraceMetadata.from(request));
        }).toList();
    }

    static List<ReportExecutionEvidence> failedExchanges(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(index -> {
            String path = "/viewport/failures/%02d".formatted(index);
            var request = new ReportRequest("GET", path, Map.of(), Map.of(), "none", "");
            var response = new ReportResponse(-1, "", Map.of(), Duration.ofMillis(2), BASE_URL + path);
            var trace = ReportTraceMetadata.from(request).withCapture("F" + index, index, "viewport",
                    "ActionCaused", "viewport", "UI action", "UI_BROWSER_REQUEST_FAILED", "", "", "");
            return new ReportExecutionEvidence(response, 1, Duration.ofMillis(2), List.of(),
                    List.of(new ReportAttemptRecord(1, -1, Duration.ofMillis(2), "FAILURE",
                            "fixture transport failure %02d".formatted(index))),
                    request, ReportTransportProvenance.LIVE_HTTP, trace);
        }).toList();
    }

    static ReportExecutionEvidence longTraceExchange() {
        ReportExecutionEvidence exchange = exchanges(1).getFirst();
        ReportTraceMetadata trace = new ReportTraceMetadata(
                "VP-TRACE-1", 1, "viewport", "ActionCaused", "viewport-checkpoint",
                "UI checkpoint", "GET /viewport/items/1", "GET /viewport/items/1#body=none",
                "request digest present", "response digest present", "UI_BROWSER",
                "viewport fixture", "response metadata is complete",
                "status=200; " + "trace evidence remains readable while the table scrolls; ".repeat(18));
        return new ReportExecutionEvidence(exchange.response(), exchange.attempts(),
                exchange.totalDuration(), exchange.retryEvents(), exchange.attemptHistory(),
                exchange.request(), exchange.provenance(), trace);
    }

    public enum Density {
        SHORT(3, 1, 1, 1, 1),
        LONG(36, 36, 36, 30, 24);

        private final int systemRows;
        private final int categoryCount;
        private final int actionCount;
        private final int signatureCount;
        private final int diagnosticCount;

        Density(int systemRows, int categoryCount, int actionCount,
                int signatureCount, int diagnosticCount) {
            this.systemRows = systemRows;
            this.categoryCount = categoryCount;
            this.actionCount = actionCount;
            this.signatureCount = signatureCount;
            this.diagnosticCount = diagnosticCount;
        }

        public int systemRows() { return systemRows; }
        public int categoryCount() { return categoryCount; }
        public int actionCount() { return actionCount; }
        public int assertionCount() { return actionCount; }
        public int signatureCount() { return signatureCount; }
        public int diagnosticCount() { return diagnosticCount; }
        public boolean expectsLongContent() { return this == LONG; }
    }
}
