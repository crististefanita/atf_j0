package atf.reporting.api.request;

import atf.reporting.api.ApiManualReplay;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.api.evidence.ApiExchangeEvidence;
import atf.reporting.api.evidence.ReportBodyAvailability;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.semantic.AtfReportSemanticSnapshot.RequestSnapshot;
import atf.reporting.support.request.ObservedRequestFixture;
import atf.reporting.support.request.LiveClientHeaderProbe;
import atf.reporting.support.request.ObservedRequestFixture.Renderer;
import atf.reporting.support.request.ObservedRequestFixture.Sample;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.escapeHtml;

@AtfComponentTest
class ObservedRequestEvidenceComponentTest {
    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("cases")
    void realPublishersPreserveObservedHeadersAndBodyAvailability(Renderer renderer, Sample sample) throws Exception {
        var published = ObservedRequestFixture.publish(renderer, List.of(sample));
        var exchange = ObservedRequestFixture.exchange(published);
        var fields = new LinkedHashMap<String, String>();
        ApiExchangeEvidence.publishFields(sample.execution(), fields::put, (label, value) -> { });
        var request = sample.request();
        String headers = fields.get("request.headers");
        String replay = ApiManualReplay.describe(ReportTransportProvenance.LIVE_HTTP, request,
                sample.execution().response().url(), request.bodyText());
        boolean jsonRequest = sample.availability == ReportBodyAvailability.AVAILABLE;
        assertEquals(jsonRequest, exchange.jsonLabels().contains("REQUEST JSON"));
        assertEquals(jsonRequest, published.html().contains("REQUEST JSON"),
                "rendered payload availability must match the semantic snapshot");
        assertEquals(sample != Sample.OBSERVED_UNCAPTURED && sample != Sample.EMPTY_RESPONSE,
                exchange.jsonLabels().contains("RESPONSE JSON"));
        assertEquals(sample != Sample.OBSERVED_UNCAPTURED && sample != Sample.EMPTY_RESPONSE,
                published.html().contains("RESPONSE JSON"));
        assertTrue(published.html().contains("Request details"));
        assertFalse(published.html().contains("header-secret"));
        assertFalse(published.html().contains("request-secret"));
        assertEquals(ApiManualReplay.redactedHeaders(request), headers);
        assertTrue(published.html().contains(headers),
                "the actual publisher must render the same redacted headers as replay");
        assertTrue(published.html().contains(
                escapeHtml(headers)),
                "rendered Request details must contain the exact sanitized projection");
        if (sample == Sample.CONFIGURED_DEFAULTS) {
            assertTrue(published.html().contains("retry-history-table"));
            assertTrue(published.html().contains("retryable fixture"));
        }
        if (sample != Sample.OBSERVED_EXPLICIT && sample != Sample.CONFIGURED_DEFAULTS) {
            assertFalse(headers.contains("Content-Type"));
            assertFalse(headers.contains("Accept"));
            if (sample != Sample.OBSERVED_UNCAPTURED) assertFalse(replay.contains("Content-Type"));
        }
        if (sample.availability == ReportBodyAvailability.ABSENT) {
            assertTrue(fields.get("request.body.capture").startsWith("Absent"));
            assertFalse(replay.contains("--data-raw"));
        } else if (sample.availability == ReportBodyAvailability.NOT_CAPTURED) {
            assertTrue(fields.get("request.body.capture").startsWith("Not captured"));
            assertTrue(replay.startsWith("NOT REPLAYABLE"));
        } else {
            assertTrue(replay.contains("--data-raw"));
        }
        if (sample == Sample.OBSERVED_NULL) assertTrue(replay.contains("--data-raw 'null'"));
        if (renderer == Renderer.UI_API) {
            var checkpoint = (RequestSnapshot)
                    published.snapshot().classes().getFirst().scenarios().getFirst().phases().getFirst().nodes().getFirst();
            assertEquals(ReportEvidenceOwner.API, checkpoint.validations().getFirst().evidenceOwner());
            assertTrue(published.html().contains("data-atf-evidence-owner=\"api\""));
        }
    }

    @Test
    void configuredDefaultsMatchHeadersActuallyExecutedByTheClient() throws Exception {
        LiveClientHeaderProbe.verify();
    }

    @Test
    void publishesInspectableObservedRequestMatrix() throws Exception {
        var published = ObservedRequestFixture.publish(Renderer.UI_API, List.of(Sample.values()));
        assertEquals(Sample.values().length, published.snapshot().scenarioCount());
    }

    private static Stream<Arguments> cases() {
        return Stream.of(Renderer.values()).flatMap(renderer ->
                Stream.of(Sample.values()).map(sample -> Arguments.of(renderer, sample)));
    }
}
