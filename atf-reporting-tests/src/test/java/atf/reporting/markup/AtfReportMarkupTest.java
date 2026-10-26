package atf.reporting.markup;
import atf.testkit.AtfUnitTest;
import atf.reporting.evidence.EvidenceTableFormatter;
import atf.reporting.support.ApiReportTestUrls;
import atf.mock.MockRoutePaths;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfReportMarkupTest {
    @Test
    void evidenceLabelsUseSingularOnlyForOneItem() {
        assertAll(
                () -> assertEquals("Request details · 1 field", AtfReportLabels.requestDetails(1)),
                () -> assertEquals("Response details · 2 fields", AtfReportLabels.responseDetails(2)),
                () -> assertEquals("Audit details · 1 field", AtfReportLabels.auditDetails(1)),
                () -> assertEquals("Setup evidence · 1 check", AtfReportLabels.setupEvidence(1)),
                () -> assertEquals("Execution evidence · 1 attempt", AtfReportLabels.executionEvidence(1)),
                () -> assertEquals("Playwright action sequence (1 entry)", AtfReportLabels.actionSequence(1)),
                () -> assertEquals("Playwright action sequence (0 entries)", AtfReportLabels.actionSequence(0)));
    }

    @Test
    void keyValueEvidenceTablesUseTheSharedKeyColumnWidth() {
        String html = AtfReportMarkup.tableHtml("helm-evidence-table helm-trace-table", new String[][]{
                {"FIELD", "VALUE"},
                {"AUTHENTICATION", "none"},
                {"ROUTE TEMPLATE", MockRoutePaths.userById("1")}
        });
        String execution = AtfReportMarkup.tableHtml("helm-evidence-table execution-table", new String[][]{
                {"FIELD", "VALUE"},
                {"response.duration.ms", "12"}
        });
        String request = AtfReportMarkup.tableHtml("helm-evidence-table helm-request-table", new String[][]{
                {"FIELD", "VALUE"},
                {"URL", ApiReportTestUrls.atf(MockRoutePaths.USERS)}
        });

        assertAll(
                () -> assertTrue(html.contains("class=\"atf-evidence-key-column\" style=\"width:var(--atf-evidence-key-column-width)\"")),
                () -> assertTrue(html.contains("class=\"atf-evidence-value-column\"")),
                () -> assertTrue(execution.contains("class=\"atf-evidence-key-column\" style=\"width:var(--atf-evidence-key-column-width)\"")),
                () -> assertTrue(request.contains("class=\"atf-evidence-key-column\" style=\"width:var(--atf-evidence-key-column-width)\"")),
                () -> assertTrue(html.contains("<th>FIELD</th><th>VALUE</th>")));
    }

    @Test
    void requestAndRetryTablesShareABoundedViewportWithoutChangingTraceClassification() {
        String request = AtfReportMarkup.tableHtml("helm-evidence-table helm-request-table",
                new String[][]{{"FIELD", "VALUE"}, {"URL", ApiReportTestUrls.atf("/resource")}});
        String retry = AtfReportMarkup.tableHtml("helm-evidence-table retry-history-table",
                new String[][]{{"ATTEMPT", "STATUS"}, {"1", "429"}});
        assertAll(
                () -> assertTrue(request.contains("<div class=\"atf-api-table-wrap\" tabindex=\"0\"><table")),
                () -> assertTrue(retry.contains("<div class=\"atf-api-table-wrap\" tabindex=\"0\"><table")),
                () -> assertFalse(request.contains("TECHNICAL TRACE")),
                () -> assertFalse(retry.contains("TECHNICAL TRACE")));
    }

    @Test
    void longFieldLabelsDoNotOverrideTheSharedColumnContract() {
        String html = AtfReportMarkup.tableHtml("helm-trace-table", new String[][]{
                {"FIELD", "VALUE"},
                {"A label deliberately long enough to require the half-table cap because it keeps growing", "value"}
        });

        assertAll(
                () -> assertTrue(html.contains("width:var(--atf-evidence-key-column-width)")),
                () -> assertFalse(html.contains("max-width:50%")),
                () -> assertFalse(html.contains("width:max-content")));
    }

    @Test
    void regularTablesDoNotReceiveTraceColumnStructure() {
        String html = AtfReportMarkup.tableHtml("helm-validation-table", new String[][]{
                {"ASSERTION", "EXPECTED"}, {"status", "200"}
        });

        assertFalse(html.contains("atf-trace-field-column"));
    }

    @Test
    void evidenceTablesReuseTheCanonicalHtmlEscaper() {
        String html = AtfReportMarkup.tableHtml("helm-validation-table", new String[][]{
                {"FIELD", "VALUE"}, {"<name>", "&\"expected\""}
        });

        assertTrue(html.contains("&lt;name&gt;"));
        assertTrue(html.contains("&amp;&quot;expected&quot;"));
    }

    @Test
    void traceMarkupUsesTheTestsTabLabelContract() {
        String html = AtfReportMarkup.tableHtml("helm-trace-table", new String[][]{
                {"FIELD", "VALUE"}, {"RESPONSE STATUS", "200"}
        });
        int label = html.indexOf("TECHNICAL TRACE");
        int wrapper = html.indexOf("class=\"atf-trace-table-wrap\"");
        int table = html.indexOf("<table class=\"helm-trace-table\">");
        int tableEnd = html.indexOf("</table>");
        int wrapperEnd = html.lastIndexOf("</div>");

        assertAll(
                () -> assertTrue(html.contains("helm-api-classification technical")),
                () -> assertTrue(html.contains("helm-api-classification-label\">" + AtfReportLabels.TECHNICAL_TRACE + "</span>")),
                () -> assertTrue(label >= 0 && label < wrapper && wrapper < table),
                () -> assertTrue(tableEnd > table && tableEnd < wrapperEnd),
                () -> assertTrue(html.contains("<table class=\"helm-trace-table\">")));
    }

    @Test
    void emptyTraceTableStillClosesItsScrollableWrapper() {
        String html = AtfReportMarkup.tableHtml("helm-trace-table", new String[0][]);

        assertAll(
                () -> assertEquals(1, occurrences(html, "class=\"atf-trace-table-wrap\"")),
                () -> assertEquals(2, occurrences(html, "</div>")),
                () -> assertTrue(html.endsWith("</table></div>")));
    }

    private static int occurrences(String value, String token) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    @Test
    void canonicalEvidenceLabelsKeepTheirExactCapitalization() {
        assertAll(
                () -> assertTrue(AtfReportLabels.responseDetails(8).equals("Response details · 8 fields")),
                () -> assertTrue(AtfReportLabels.assertions(3).equals("Assertions · 3")),
                () -> assertTrue(AtfReportLabels.REQUEST_JSON.equals("REQUEST JSON")),
                () -> assertTrue(AtfReportLabels.RESPONSE_JSON.equals("RESPONSE JSON")));
    }

    @Test
    void technicalTraceKeepsFullUrlAsUrlField() {
        String[][] rows = EvidenceTableFormatter.toExtentRows(
                java.util.Collections.singletonList(new String[]{"URL", ApiReportTestUrls.atf(MockRoutePaths.POSTS)}));

        assertAll(
                () -> assertTrue("URL".equals(rows[1][0])),
                () -> assertTrue(ApiReportTestUrls.atf(MockRoutePaths.POSTS).equals(rows[1][1])));
    }

    @Test
    void multiExchangeUiTraceReusesDirectApiVocabularyWithoutRequestPrefixNoise() {
        assertAll(
                () -> assertEquals("API 1 · METHOD", EvidenceTableFormatter.displayName("request.api.1.method")),
                () -> assertEquals("API 1 · AUTH TYPE",
                        EvidenceTableFormatter.displayName("request.api.1.auth.type")),
                () -> assertEquals("API 1 · PATH", EvidenceTableFormatter.displayName("request.api.1.path")),
                () -> assertEquals("API 1 · TRANSPORT PROVENANCE",
                        EvidenceTableFormatter.displayName("request.api.1.transport")),
                () -> assertEquals("API 2 · HTTP STATUS",
                        EvidenceTableFormatter.displayName("response.api.2.status")),
                () -> assertEquals("API 2 · CONTENT-TYPE",
                        EvidenceTableFormatter.displayName("response.api.2.content.type")));
    }

    @Test
    void authenticationModeAndTypeKeepDistinctReportLabels() {
        assertAll(
                () -> assertEquals("AUTHENTICATION", EvidenceTableFormatter.displayName("auth.mode")),
                () -> assertEquals("AUTH TYPE", EvidenceTableFormatter.displayName("auth.type")));
    }

    @Test
    void requestAndResponseDetailsKeepTheirOwnEvidence() {
        var allDetails = List.of(
                new String[]{"Duration", "12 ms"},
                new String[]{"URL", ApiReportTestUrls.atf(MockRoutePaths.postById("101"))},
                new String[]{"HTTP STATUS", "200"},
                new String[]{"Content-Type", "application/json"},
                new String[]{"Attempt", "1"},
                new String[]{"Failure cause", "none"});
        var requestDetails = EvidenceTableFormatter.requestDetails(allDetails);
        var responseDetails = EvidenceTableFormatter.technicalTraceDetails(allDetails);

        assertAll(
                () -> assertEquals(1, requestDetails.size()),
                () -> assertEquals("URL", requestDetails.get(0)[0]),
                () -> assertEquals(3, responseDetails.size()),
                () -> assertEquals("HTTP STATUS", responseDetails.get(0)[0]),
                () -> assertEquals("Content-Type", responseDetails.get(1)[0]),
                () -> assertEquals("Failure cause", responseDetails.get(2)[0]),
                () -> assertFalse(responseDetails.stream().anyMatch(detail -> "Duration".equals(detail[0]))),
                () -> assertFalse(responseDetails.stream().anyMatch(detail -> "Attempt".equals(detail[0]))));
    }

    @Test
    void requestMetadataStaysInTheRequestTable() {
        var details = List.of(
                new String[]{"URL", ApiReportTestUrls.atf(MockRoutePaths.SEARCH)},
                new String[]{"Auth profile", "bearer"},
                new String[]{"Auth type", "BEARER"},
                new String[]{"Query params", "{page=2}"},
                new String[]{"Request headers", "{Accept=application/json}"});

        var requestDetails = EvidenceTableFormatter.requestDetails(details);
        var responseDetails = EvidenceTableFormatter.technicalTraceDetails(details);

        assertAll(
                () -> assertEquals(5, requestDetails.size()),
                () -> assertTrue(responseDetails.isEmpty()));
    }

    @Test
    void normalizedReplayAndProvenanceAliasesRemainRequestMetadata() {
        var details = List.of(
                new String[]{"manual.replay", "curl example"},
                new String[]{"transport.provenance", "captured"},
                new String[]{"auth.profile", "shop-user"},
                new String[]{"response.content.type", "application/json"});
        assertAll(
                () -> assertEquals(3, EvidenceTableFormatter.requestDetails(details).size()),
                () -> assertEquals(1, EvidenceTableFormatter.technicalTraceDetails(details).size()),
                () -> assertEquals("MANUAL REPLAY", EvidenceTableFormatter.displayName("manual.replay")),
                () -> assertEquals("TRANSPORT PROVENANCE", EvidenceTableFormatter.displayName("transport.provenance")),
                () -> assertEquals("AUTH PROFILE", EvidenceTableFormatter.displayName("auth.profile")));
    }
}
