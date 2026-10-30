package atf.ui.playwright.download;

import atf.testkit.AtfComponentTest;
import atf.core.reporting.contract.AtfReportStatus;
import atf.ui.tests.support.download.UiDownloadPortFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
class PlaywrightDownloadComponentTest {
    @Test void successRequiresTheRealEventAndReleasesItsListener() {
        var fixture = new UiDownloadPortFixture("success");
        assertNotNull(fixture.run());
        assertEquals(1, fixture.clicks()); assertEquals(1, fixture.readiness()); assertEquals(1, fixture.removals());
        assertTrue(fixture.detached());
        assertEquals(1, fixture.records().size());
        assertEquals("Download", fixture.records().getFirst().operation());
        assertEquals(AtfReportStatus.PASS, fixture.records().getFirst().status());
    }
    @ParameterizedTest @ValueSource(strings = {"attach", "click", "readiness", "no-event", "same", "distinct", "cleanup"})
    void everyFailureKeepsItsCauseAndNeverReplaysTheActivation(String mode) {
        var fixture = new UiDownloadPortFixture(mode);
        Throwable observed = assertThrows(Throwable.class, fixture::run);
        assertSame(mode.equals("cleanup") ? fixture.cleanup() : fixture.primary(), observed);
        assertEquals(mode.equals("attach") ? 0 : 1, fixture.clicks());
        assertEquals(1, fixture.removals()); assertTrue(fixture.detached());
        assertEquals(mode.equals("distinct") ? java.util.List.of(fixture.cleanup()) : java.util.List.of(),
                java.util.List.of(observed.getSuppressed()));
        assertEquals(1, fixture.records().size());
        assertEquals(AtfReportStatus.FAIL, fixture.records().getFirst().status());
        assertTrue(fixture.records().getFirst().durationMillis() > 0);
    }
}
