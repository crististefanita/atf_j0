package atf.ui.playwright;
import atf.testkit.AtfComponentTest;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest

class UiScreenValidationTest {
    private static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Test
    void acceptsARealBase64PngPayload() {
        var evidence = new UiScreenValidation.Evidence("screen", ONE_PIXEL_PNG, true);

        assertTrue(UiScreenValidation.isValidPngPayload(ONE_PIXEL_PNG));
        assertTrue(evidence.hasScreenshotPayload());
        assertTrue(evidence.hasInlineImage());
    }

    @Test
    void rejectsBlankInvalidAndNonPngPayloads() {
        assertFalse(UiScreenValidation.isValidPngPayload(null));
        assertFalse(UiScreenValidation.isValidPngPayload("  "));
        assertFalse(UiScreenValidation.isValidPngPayload("not-base64"));
        assertFalse(UiScreenValidation.isValidPngPayload("AQID"));
        assertFalse(new UiScreenValidation.Evidence("screen", "AQID", true)
                .hasScreenshotPayload());
        assertFalse(new UiScreenValidation.Evidence("screen", ONE_PIXEL_PNG, false)
                .hasScreenshotPayload());
    }
}
