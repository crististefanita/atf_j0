package atf.reporting.ui.contract;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.EvidenceTableFormatter;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.model.ReportField;
import atf.reporting.semantic.AtfReportSemanticSnapshot.RequestSnapshot;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;

import static atf.reporting.markup.AtfReportEvidenceKeys.EVIDENCE_TYPE;
import static atf.reporting.markup.AtfReportEvidenceKeys.EVIDENCE_POLICY;
import static atf.reporting.markup.AtfReportEvidenceKeys.UI_VALIDATION;
import static atf.reporting.markup.AtfReportEvidenceKeys.API_TRACE;
import static atf.reporting.markup.AtfReportEvidenceKeys.UNIFIED_UI_FLOW;
import static atf.reporting.markup.AtfReportEvidenceKeys.UI_FAILURE;
import static atf.reporting.markup.AtfReportEvidenceKeys.REQUEST_ACTION;
import static atf.reporting.markup.AtfReportEvidenceKeys.REQUEST_TARGET;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Public transport evidence must not expose a UI checkpoint's internal identity. */
@AtfComponentTest
class UiCheckpointEvidenceProjectionTest {
    private static final String UI_TITLE = "AND catalog home renders product cards, pagination and footer";
    private static final String UI_TARGET = "https://ui.example.test/catalog";

    @Test
    void typedAndLegacyProjectionExcludeContextWithoutLosingRealTransportFields() {
        var context = List.of(
                new ReportField(REQUEST_ACTION, UI_TITLE),
                new ReportField(" REQUEST.TARGET ", UI_TARGET),
                new ReportField(EVIDENCE_TYPE, UI_VALIDATION),
                new ReportField(" EVIDENCE.POLICY ", "each-checkpoint"));
        var method = new ReportField("method", "POST");
        var url = new ReportField("url", "https://api.example.test/cart");
        var extension = new ReportField("request.custom", "public request extension");
        var response = new ReportField("HTTP STATUS", "201");
        var mixed = new ArrayList<>(context);
        mixed.addAll(List.of(method, url, extension, response));
        var legacy = mixed.stream().map(field -> new String[]{field.name(), field.value()}).toList();

        assertAll(
                () -> assertTrue(EvidenceTableFormatter.requestFields(context).isEmpty()),
                () -> assertTrue(EvidenceTableFormatter.technicalTraceFields(context).isEmpty()),
                () -> assertEquals(List.of(method, url, extension), EvidenceTableFormatter.requestFields(mixed)),
                () -> assertEquals(List.of(response), EvidenceTableFormatter.technicalTraceFields(mixed)),
                () -> assertEquals(List.of("method", "url", "request.custom"),
                        EvidenceTableFormatter.requestDetails(legacy).stream().map(row -> row[0]).toList()),
                () -> assertEquals(List.of("HTTP STATUS"),
                        EvidenceTableFormatter.technicalTraceDetails(legacy).stream().map(row -> row[0]).toList()),
                () -> assertEquals(UI_TITLE, mixed.get(0).value()),
                () -> assertEquals(UI_TARGET, mixed.get(1).value()));
    }

    @Test
    void explicitTraceAndUnifiedFlowContractsRemainPublic() {
        var publicTrace = List.of(new ReportField(EVIDENCE_TYPE, API_TRACE),
                new ReportField(EVIDENCE_TYPE, UNIFIED_UI_FLOW));
        var failureContext = List.of(new ReportField(EVIDENCE_TYPE, UI_FAILURE),
                new ReportField(EVIDENCE_POLICY, "screenshot disabled or unavailable"));
        assertAll(
                () -> assertEquals(publicTrace, EvidenceTableFormatter.technicalTraceFields(publicTrace)),
                () -> assertTrue(EvidenceTableFormatter.technicalTraceFields(failureContext).isEmpty()));
        var legacyTrace = publicTrace.stream().map(field -> new String[]{field.name(), field.value()}).toList();
        var legacyFailure = failureContext.stream().map(field -> new String[]{field.name(), field.value()}).toList();
        assertAll(
                () -> assertEquals(publicTrace.stream().map(ReportField::value).toList(),
                        EvidenceTableFormatter.technicalTraceDetails(legacyTrace).stream()
                                .map(row -> row[1]).toList()),
                () -> assertTrue(EvidenceTableFormatter.technicalTraceDetails(legacyFailure).isEmpty()));
    }

    @Test
    void fallbackRendersValidationWithoutSyntheticRequestOrResponseDisclosure(@TempDir Path directory)
            throws Exception {
        Path report = directory.resolve("ui-checkpoint-fallback.html");
        List<String> internalKeys;
        try (var session = new HtmlReportSession(report)) {
            var scenario = session.startScenario("UiCheckpointComponent", "UI evidence only", List.of("ui"));
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest(UI_TITLE, "", "");
            request.detail(REQUEST_ACTION, UI_TITLE);
            request.detail(REQUEST_TARGET, UI_TARGET);
            request.detail(EVIDENCE_TYPE, UI_VALIDATION);
            request.detail(EVIDENCE_POLICY, "each-checkpoint");
            request.startValidation("UI validation").complete(AtfReportStatus.FAIL);
            request.complete(AtfReportStatus.FAIL);
            phase.complete(AtfReportStatus.FAIL);
            scenario.complete(AtfReportStatus.FAIL);
            internalKeys = assertInstanceOf(RequestSnapshot.class,
                    session.semanticSnapshot().classes().getFirst().scenarios().getFirst()
                            .phases().getFirst().nodes().getFirst()).detailNames();
        }

        String html = Files.readString(report);

        assertAll(
                () -> assertTrue(html.contains(UI_TITLE)),
                () -> assertTrue(html.contains("UI validation")),
                () -> assertTrue(html.contains("status-fail")),
                () -> assertFalse(html.contains("Request details")),
                () -> assertFalse(html.contains("Response details")),
                () -> assertFalse(html.contains("REQUEST ACTION")),
                () -> assertFalse(html.contains("REQUEST TARGET")),
                () -> assertFalse(html.contains(UI_TARGET)),
                () -> assertFalse(html.contains("EVIDENCE TYPE")),
                () -> assertFalse(html.contains("EVIDENCE POLICY")),
                () -> assertEquals(List.of(REQUEST_ACTION, REQUEST_TARGET, EVIDENCE_TYPE, EVIDENCE_POLICY),
                        internalKeys));
    }
}
