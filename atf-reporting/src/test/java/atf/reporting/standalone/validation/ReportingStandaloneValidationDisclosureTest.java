package atf.reporting.standalone.validation;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.html.HtmlReportSession;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/** Standalone API ports publish one closed assertion disclosure, without any ATF integration dependency. */
class ReportingStandaloneValidationDisclosureTest {
    @TempDir Path directory;

    @ParameterizedTest
    @EnumSource(value = AtfReportStatus.class, names = {"PASS", "FAIL", "WARNING"})
    void nonemptyApiValidationUsesTheSharedClosedAssertionShell(AtfReportStatus status) {
        String html = render(status, List.of(new AtfAssertion("original comparison", "null", "{}",
                status == AtfReportStatus.PASS, status)));
        String opening = "<details class=\"step-detail assertion-evidence\"";
        assertEquals(1, html.split(java.util.regex.Pattern.quote(opening), -1).length - 1);
        int begin = html.indexOf(opening);
        assertFalse(html.substring(begin, html.indexOf('>', begin)).contains(" open"));
        int end = html.indexOf("</details>", begin);
        assertTrue(html.substring(begin, end).contains("<table class=\"assertions\""));
        assertTrue(html.substring(begin, end).contains("original comparison"));
        assertTrue(html.substring(begin, end).contains("status-" + status.name().toLowerCase(java.util.Locale.ROOT)));
    }

    @Test
    void emptyApiValidationDoesNotInventAnEmptyDisclosureOrTable() {
        String html = render(AtfReportStatus.INFO, List.of());
        assertFalse(html.contains("<details class=\"step-detail assertion-evidence\""));
        assertFalse(html.contains("<table class=\"assertions\""));
    }

    private String render(AtfReportStatus status, List<AtfAssertion> comparisons) {
        try (var session = new HtmlReportSession(directory.resolve("validation.html"))) {
            var scenario = session.startScenario("ApiMock", "standalone API contract", List.of());
            var phase = scenario.startPhase("TEST BODY");
            var request = phase.startRequest("GET /resource", "GET", "/resource");
            var validation = request.startValidation("Response contract");
            validation.assertions(comparisons);
            validation.complete(status);
            request.complete(status);
            phase.complete(status);
            scenario.complete(status);
            return session.render();
        }
    }
}
