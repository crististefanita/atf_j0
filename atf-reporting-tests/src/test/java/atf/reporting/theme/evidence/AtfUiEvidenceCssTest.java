package atf.reporting.theme.evidence;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ratchets the finite CSS contract used by inline Playwright evidence. */
@AtfUnitTest
class AtfUiEvidenceCssTest {
    @Test
    void rendersTypedSelectorsAndPaletteWithoutEmbeddingRuntimeData() {
        String css = AtfUiEvidenceCss.stylesheet(
                AtfReportHtmlVocabulary.ElementId.UI_EVIDENCE,
                AtfReportHtmlVocabulary.CssClass.ATF_UI_EVIDENCE_SCREENSHOT_LINK);

        assertAll(
                () -> assertTrue(css.contains("#atf-ui-exploratory-evidence{")),
                () -> assertTrue(css.contains("border:1px solid #2b3d5a")),
                () -> assertTrue(css.contains("background:#111a29;color:#dbe7ff")),
                () -> assertTrue(css.contains("summary{cursor:pointer;color:#80aaff}")),
                () -> assertTrue(css.contains(".atf-ui-evidence-screenshot-link{display:inline-flex")),
                () -> assertTrue(css.contains("background:#1a2940;text-decoration:underline}")),
                () -> assertFalse(css.contains("<style")),
                () -> assertFalse(css.contains("${")));
    }
}
