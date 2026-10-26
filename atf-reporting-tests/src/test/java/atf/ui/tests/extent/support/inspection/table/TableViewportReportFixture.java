package atf.ui.tests.extent.support.inspection.table;

import atf.reporting.api.ReportAttemptRecord;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.markup.AtfReportEvidenceKeys;
import atf.reporting.markup.AtfReportMarkup;
import atf.reporting.markup.table.ReportTableViewport;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.support.TestReportPaths;
import atf.reporting.theme.AtfReportTheme;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.IntStream;

/** Neutral public-port fixtures, never application E2E data or DOM-cloned table rows. */
public final class TableViewportReportFixture {
    public static final int LONG_ROWS = 40;
    public static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
    public static final List<String> FAMILIES = List.of("atf-setup-evidence-table", "execution-table",
            "helm-validation-table", "helm-request-table", "retry-history-table", "helm-trace-table",
            "future-consumer-table");

    private TableViewportReportFixture() { }

    public static String componentHtml(boolean fallback, int rows, boolean tallCell) {
        StringBuilder html = new StringBuilder("<!doctype html><meta charset='utf-8'><style>")
                .append(fallback ? AtfReportTheme.fallbackCss() : AtfReportTheme.css())
                .append("body{margin:0;padding:8px} .fixture{margin:12px 0} th,td{padding:8px;white-space:normal} </style>")
                .append("<div class='dashboard-view'><div class='sysenv-container'><div class='card'><div class='card-body'>")
                .append("<table><thead><tr><th>Name</th><th>Value</th></tr></thead><tbody><tr><td>Environment</td><td>LOCAL</td></tr></tbody></table>")
                .append("</div></div></div></div>");
        for (String phase : List.of("SETUP", "TEST BODY", "TEARDOWN")) {
            for (String role : List.of("request", "validation")) {
                String view = phase.equals("SETUP") ? "test-view"
                        : phase.equals("TEST BODY") ? "category-view" : "atf-fails-view";
                html.append("<section class='").append(view).append("' data-context='")
                        .append(phase).append("/").append(role)
                        .append("'><div class='test-content-detail'><div class='card' data-atf-node-role='")
                        .append(role).append("'>");
                for (String family : FAMILIES) {
                    html.append("<div class='fixture' data-family='").append(family).append("'>")
                            .append(AtfReportMarkup.tableHtml(family, rows(rows, tallCell))).append("</div>");
                }
                for (var kind : List.of(ReportTableViewport.ACTIONS, ReportTableViewport.ASSERTIONS)) {
                    html.append("<div class='fixture' data-family='").append(kind.name()).append("'>")
                            .append(kind.wrap(bareTable(rows, tallCell))).append("</div>");
                }
                html.append("</div></div></section>");
            }
        }
        return html.toString();
    }

    private static String[][] rows(int count, boolean tall) {
        String[][] rows = new String[count + 1][2];
        rows[0] = new String[]{"FIELD", "VALUE"};
        for (int i = 1; i <= count; i++) rows[i] = new String[]{
                tall ? ("multiline-value-" + i + " ").repeat(600) : "Field " + i, "value " + i};
        return rows;
    }

    private static String bareTable(int count, boolean tall) {
        String markup = AtfReportMarkup.tableHtml("helm-evidence-table", rows(count, tall));
        return markup.substring(markup.indexOf('>') + 1, markup.lastIndexOf("</div>"));
    }

    public static Path publish(boolean fallback) {
        Path path = TestReportPaths.report("table-viewport-" + (fallback ? "fallback" : "extent") + ".html");
        Map<String, String> system = new LinkedHashMap<>();
        IntStream.rangeClosed(1, LONG_ROWS).forEach(i -> system.put("Viewport environment " + i, "value " + i));
        try (AtfReportPorts.Session session = fallback ? new HtmlReportSession(path)
                : new ExtentReportSession(path, ReportingSettings.defaults(), new SensitiveDataRedactor(),
                AtfConsoleSink.noOp(), system)) {
            var scenario = session.startScenario("TableViewportComponent", "neutral viewport proof", List.of("viewport"));
            for (String phaseName : List.of("SETUP", "TEST BODY", "TEARDOWN")) {
                var phase = scenario.startPhase(phaseName);
                for (int count : List.of(1, LONG_ROWS)) {
                    var request = phase.startRequest("GIVEN " + phaseName + " viewport " + count, "", "");
                    request.detail(AtfReportEvidenceKeys.EVIDENCE_TYPE, AtfReportEvidenceKeys.REPORT_SETUP);
                    var actions = IntStream.rangeClosed(1, count).mapToObj(i -> new AtfUiActionRecord(i,
                            "Get text", "#neutral-" + i, "visible", "visible", 1, PASS)).toList();
                    var observations = IntStream.rangeClosed(1, count).mapToObj(i -> new AtfUiActionRecord(i,
                            "Audit screen contract", "checkpoint-" + i, "visible", "visible", 0, PASS,
                            AtfUiEvidenceSource.OBSERVE)).toList();
                    var allActions = new ArrayList<>(actions);
                    allActions.addAll(observations);
                    request.uiActions(allActions);
                    request.screenshot("terminal capture", PNG);
                    for (int i = 1; i <= count; i++) request.detail(AtfReportEvidenceKeys.setupCheck(i),
                            "Check " + i + " | expected=visible | actual=visible | result=PASS");
                    var validation = request.startValidation("Screen validation");
                    validation.uiActions(allActions);
                    validation.screenshot("terminal validation capture", PNG);
                    validation.assertions(IntStream.rangeClosed(1, count).mapToObj(i ->
                            new AtfAssertion("Assertion " + i, "visible", "visible", true)).toList());
                    validation.complete(PASS);
                    request.complete(PASS);

                    var api = phase.startRequest("QUERY /viewport — " + phaseName + " " + count, "QUERY", "/viewport");
                    for (int i = 1; i <= count; i++) {
                        api.detail("request.header." + i, "value " + i);
                        api.detail("response.detail." + i, "value " + i);
                    }
                    var rq = new ReportRequest("QUERY", "/viewport", Map.of(), Map.of(), "none", "{}");
                    var response = new ReportResponse(200, "{}", Map.of(), Duration.ofMillis(3), "https://api.example.test/viewport");
                    api.execution(new ReportExecutionEvidence(response, count, Duration.ofMillis(3), List.of(),
                            IntStream.rangeClosed(1, count).mapToObj(i -> new ReportAttemptRecord(i, 200,
                                    Duration.ofMillis(3), "PASS", "attempt " + i)).toList(), rq,
                            ReportTransportProvenance.ACCEPTANCE_MOCK));
                    api.json("RESPONSE JSON", "{\"unchanged\":true}");
                    api.complete(PASS);
                }
                phase.complete(PASS);
            }
            scenario.complete(PASS);
        }
        return path;
    }
}
