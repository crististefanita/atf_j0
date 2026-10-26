package atf.reporting.model;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class ReportScreenshotTest {
    @Test
    void normalizesTitleAndPayloadAtTheSemanticBoundary() {
        ReportScreenshot screenshot = new ReportScreenshot("  login state  ", "  AQID  ");

        assertEquals("login state", screenshot.title());
        assertEquals("AQID", screenshot.base64Png());
    }

    @Test
    void usesTheSharedScreenshotLabelWhenTitleIsMissing() {
        assertEquals("UI screenshot", new ReportScreenshot(" ", "AQID").title());
    }

    @Test
    void rejectsMissingOrBlankPayload() {
        assertThrows(NullPointerException.class, () -> new ReportScreenshot("state", null));
        assertThrows(IllegalArgumentException.class, () -> new ReportScreenshot("state", "  "));
    }
}
