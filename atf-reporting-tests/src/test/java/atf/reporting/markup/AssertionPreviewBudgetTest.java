package atf.reporting.markup;

import atf.reporting.configuration.ReportingSettings;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class AssertionPreviewBudgetTest {
    @ParameterizedTest @ValueSource(ints = {1, 2, 40, 240})
    void theEntireValueIncludingTheMarkerFitsTheConfiguredBudget(int limit) {
        var settings = new ReportingSettings(limit);
        String exact = "x".repeat(limit);
        assertEquals(exact, ExtentReportNodeSupport.display(exact, settings));
        String longValue = ExtentReportNodeSupport.display("x".repeat(1358), settings);
        assertEquals(limit, longValue.length());
        assertTrue(longValue.endsWith("…"));
        assertEquals("", ExtentReportNodeSupport.display(null, settings));
    }

    @Test void cuttingAnEmojiDoesNotPublishHalfASurrogatePair() {
        String value = ExtentReportNodeSupport.display("x".repeat(238) + "😀tail", new ReportingSettings(240));
        assertEquals("x".repeat(238) + "…", value);
        assertFalse(value.chars().anyMatch(c -> Character.isSurrogate((char) c)));
    }
}
