package atf.ui.tests.extent.component.assertions;

import atf.reporting.audit.AssertionAuditReport;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.reporting.security.SensitiveDataRedactor;
import atf.testkit.AtfComponentTest;
import atf.testkit.assertions.AssertionValueDisplay;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class AssertionAuditPublicationComponentTest {
    @Test void skippedPhasePublishesItsAbortReasonWithoutInventingAssertions() throws Exception {
        var directory = Files.createTempDirectory("atf-aborted-assertion-audit-");
        for (boolean extent : List.of(true, false)) {
            var file = directory.resolve(extent ? "extent.html" : "fallback.html");
            AtfReportPorts.Session session = extent ? new ExtentReportSession(file) : new HtmlReportSession(file);
            try (var report = new AssertionAuditReport(session, new AssertionAuditReport.Identity(
                    "AbortedFixture", "fixture", "aborted setup", "checks", List.of("component")),
                    new SensitiveDataRedactor())) {
                report.phase("SETUP", List.of(), AtfReportStatus.SKIP, "controlled assumption reason");
                report.complete(AtfReportStatus.SKIP);
                var snapshot = extent ? ((ExtentReportSession) session).semanticSnapshot()
                        : ((HtmlReportSession) session).semanticSnapshot();
                var phase = snapshot.classes().getFirst().scenarios().getFirst().phases().getFirst();
                assertEquals(AtfReportStatus.SKIP, phase.status());
                assertTrue(phase.nodes().isEmpty());
            }
            String html = Files.readString(file);
            assertTrue(html.contains("Abort reason"));
            assertTrue(html.contains("controlled assumption reason"));
        }
    }

    @Test void safeBoundedRowsReachBothRenderersWithoutSecretFragments() throws Exception {
        var directory = Files.createTempDirectory("atf-safe-assertion-audit-");
        for (boolean extent : List.of(true, false)) {
            var file = directory.resolve(extent ? "extent.html" : "fallback.html");
            AtfReportPorts.Session session = extent ? new ExtentReportSession(file) : new HtmlReportSession(file);
            try (var report = new AssertionAuditReport(session, new AssertionAuditReport.Identity(
                    "AuditFixture", "fixture", "bounded secret", "checks", List.of("component")),
                    new SensitiveDataRedactor())) {
                var rows = new RecordedAssertions(new AssertionValueDisplay(240, report::redactField));
                String secret = "{\"password\":\"REPORT_SYNTHETIC_CANARY_" + "q".repeat(280) + "\"}";
                rows.equals("JSON contract", secret, secret);
                String escaped = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of(
                        "password", "prefix\"REPORT_ESCAPED_CANARY_" + "q".repeat(280)));
                rows.equals("Escaped JSON contract", escaped, escaped);
                assertThrows(AssertionError.class, () -> rows.equals("real mismatch", "expected", "actual"));
                report.phase("TEST BODY", rows.checks().stream().map(row ->
                        new AtfAssertion(row.name(), row.expected(), row.actual(), row.passed())).toList(),
                        AtfReportStatus.FAIL, "");
                report.complete(AtfReportStatus.FAIL);
            }
            String html = Files.readString(file);
            assertFalse(html.contains("REPORT_SYNTHETIC_CANARY_"));
            assertFalse(html.contains("REPORT_ESCAPED_CANARY_"));
            assertTrue(html.contains("REDACTED"));
            assertTrue(html.contains("Component assertions"));
            assertTrue(html.contains("real mismatch"));
            assertTrue(html.contains("expected") && html.contains("actual"));
        }
    }
}
