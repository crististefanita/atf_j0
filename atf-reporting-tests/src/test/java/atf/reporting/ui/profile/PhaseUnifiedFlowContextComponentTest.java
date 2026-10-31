package atf.reporting.ui.profile;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.semantic.AtfReportSemanticSnapshot;
import atf.reporting.support.flow.PhaseUnifiedFlowFixture;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class PhaseUnifiedFlowContextComponentTest {
    @ParameterizedTest
    @CsvSource({"false,UI", "true,UI", "false,UI_FAILURE", "true,UI_FAILURE",
            "false,API", "true,API", "false,API_FAILURE", "true,API_FAILURE"})
    void contextIsUiProvenanceWhileRealExchangesRetainHttpEvidence(boolean fallback,
            PhaseUnifiedFlowFixture.Variant variant, @TempDir Path root) throws Exception {
        var published = PhaseUnifiedFlowFixture.publish(root, fallback, 240, variant);
        var phase = published.snapshot().classes().getFirst().scenarios().getFirst().phases().getFirst();
        var request = (AtfReportSemanticSnapshot.RequestSnapshot) phase.nodes().getFirst();
        boolean api = variant == PhaseUnifiedFlowFixture.Variant.API || variant == PhaseUnifiedFlowFixture.Variant.API_FAILURE;
        assertEquals(1, phase.nodes().size());
        assertEquals(api ? 2 : 1, request.validations().size());
        assertFalse(request.detailNames().contains("flow.presentation"), "A phase-local flow is not cross-phase compact evidence");
        assertEquals(List.of("Product retained", "Order retained"), request.validations().getFirst().assertionNames());
        assertEquals(3, request.validations().getFirst().uiActionCount());
        assertEquals(1, request.validations().getFirst().screenshotCount());
        assertEquals(variant == PhaseUnifiedFlowFixture.Variant.UI_FAILURE || variant == PhaseUnifiedFlowFixture.Variant.API_FAILURE
                ? AtfReportStatus.FAIL : AtfReportStatus.PASS, phase.status());
        String markup = published.html().replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
        assertEquals(1L, java.util.regex.Pattern.compile("class=\"[^\"]*\\batf-ui-flow-provenance\\b[^\"]*\"")
                .matcher(markup).results().count(), "Phase context has its own UI disclosure");
        assertTrue(markup.contains("UI flow context · 4 fields"));
        assertFalse(markup.contains("UI flow provenance · 0 checkpoints"));
        assertTrue(markup.contains("<td>FLOW PHASE</td><td>TEST BODY</td>"));
        assertEquals(api ? 1 : 0, count(markup, ">Response details ·"), "Only the actual HTTP exchange owns response details");
        if (api) {
            var exchange = request.validations().get(1).requests().getFirst();
            assertEquals("GET", exchange.method());
            assertEquals("/demo", exchange.path());
            assertTrue(exchange.executionPresent());
            assertEquals(List.of("API status"), exchange.validations().getFirst().assertionNames());
            assertEquals(variant == PhaseUnifiedFlowFixture.Variant.API_FAILURE ? AtfReportStatus.FAIL : AtfReportStatus.PASS,
                    exchange.status());
        }
    }

    private static int count(String text, String marker) { return text.split(java.util.regex.Pattern.quote(marker), -1).length - 1; }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void conflictingCompactIdentityRetainsTheRealFailedExchange(boolean fallback, @TempDir Path root) throws Exception {
        var published = PhaseUnifiedFlowFixture.publishConflictingContext(root, fallback);
        var phase = published.snapshot().classes().getFirst().scenarios().getFirst().phases().getFirst();
        var request = (AtfReportSemanticSnapshot.RequestSnapshot) phase.nodes().getFirst();
        assertEquals(AtfReportStatus.FAIL, phase.status());
        assertEquals("GET", request.method());assertEquals("/demo", request.path());
        assertTrue(request.executionPresent());
        assertEquals(List.of("API status"), request.validations().getFirst().assertionNames());
        String markup = published.html().replaceAll("(?is)<script\\b[^>]*>.*?</script>|<style\\b[^>]*>.*?</style>", "");
        assertEquals(1, count(markup, ">Request details ·"));
        assertEquals(1, count(markup, ">Response details ·"));
        assertEquals(1, count(markup, ">Execution evidence ·"));
        assertFalse(markup.contains("UI flow context ·"));
        assertFalse(markup.contains("UI flow provenance ·"));
        assertTrue(markup.contains("500"));assertTrue(markup.contains("200"));
    }
}
