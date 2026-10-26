package atf.reporting.standalone;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Component contract for the Extent evidence-row boundary.
 *
 * <p>The live Extent DOM uses {@code .test-contents}; this test prevents a
 * future CSS refactor from silently targeting only the legacy class and
 * reintroducing a separator below the status badge.</p>
 */
class ReportingStandaloneEvidenceLayoutTest {
    private static final String EVIDENCE_STACK_RESOURCE =
            "/atf/reporting/css/evidence-stack.css";

    @Test
    void liveExtentEvidenceRowsSuppressOnlyHorizontalStatusSeparators() {
        String css = readResource(EVIDENCE_STACK_RESOURCE);

        assertAll(
                () -> assertTrue(css.contains(".test-contents"),
                        "the active Extent detail container must be covered"),
                () -> assertTrue(css.contains("details.step-detail"),
                        "API/setup/execution evidence rows must be covered"),
                () -> assertTrue(css.contains("details.atf-ui-action-sequence"),
                        "Playwright action rows must be covered"),
                () -> assertTrue(css.contains("details.atf-ui-observations"),
                        "audit observation rows must be covered"),
                () -> assertTrue(css.contains("details.setup-evidence"),
                        "setup evidence must retain its own boundary"),
                () -> assertTrue(css.contains("details.assertion-evidence"),
                        "assertion evidence must retain its own boundary"),
                () -> assertTrue(css.contains("> .collapse"),
                        "active Extent card boundaries must use explicit DOM combinators"),
                () -> assertTrue(css.contains("table.helm-validation-table"),
                        "assertion rows must be covered"),
                () -> assertTrue(css.contains("border: 1px solid var(--atf-border)"),
                        "setup and assertion disclosures must keep their visible border"),
                () -> assertTrue(css.contains("border-bottom: 0 !important"),
                        "the row boundary must be removed"),
                () -> assertTrue(css.contains("border-top: 0 !important"),
                        "the row boundary must not double-render"),
                () -> assertFalse(css.matches("(?s).*\\.test-contents.*border\\s*:\\s*0\\s*!important.*"),
                        "the fix must preserve vertical table separators"));
    }

    private static String readResource(String resource) {
        try (InputStream stream = ReportingStandaloneEvidenceLayoutTest.class
                .getResourceAsStream(resource)) {
            if (stream == null) {
                throw new AssertionError("Missing reporting asset: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new AssertionError("Cannot read reporting asset: " + resource, error);
        }
    }
}
